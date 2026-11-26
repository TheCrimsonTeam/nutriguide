from __future__ import annotations

import asyncio
import base64
import hashlib
import hmac
import io
import json
import logging
import os
import time
from collections import defaultdict, deque
from difflib import SequenceMatcher
from typing import Any

import httpx
from fastapi import Depends, FastAPI, File, Header, HTTPException, Request, UploadFile
from PIL import Image, ImageOps, UnidentifiedImageError
from pydantic import BaseModel, ConfigDict, Field, ValidationError

logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger("nutriguide.food_analysis")
app = FastAPI(title="NutriGuide food analysis", version="0.3.1")

OPENAI_URL = "https://api.openai.com/v1/responses"
USDA_SEARCH_URL = "https://api.nal.usda.gov/fdc/v1/foods/search"
VISION_MODEL = os.getenv("OPENAI_VISION_MODEL", "gpt-6.2-sol")
MAX_UPLOAD_BYTES = int(os.getenv("MAX_UPLOAD_BYTES", "10485760"))
MAX_IMAGE_PIXELS = 20_000_000
MAX_EDGE = 1600
RATE_LIMIT_PER_MINUTE = int(os.getenv("RATE_LIMIT_PER_MINUTE", "10"))
CACHE_SECONDS = 90
Image.MAX_IMAGE_PIXELS = MAX_IMAGE_PIXELS

class IdentifiedFood(BaseModel):
    model_config = ConfigDict(extra="forbid")
    name: str = Field(min_length=1, max_length=120)
    alternatives: list[str] = Field(default_factory=list, max_length=5)
    portionGrams: float = Field(gt=0, le=5000)
    servingDescription: str = Field(max_length=120)
    pieceCount: float | None = Field(default=None, ge=0, le=1000)
    confidenceLabel: str
    portionUncertainty: str
    visualObservations: list[str] = Field(default_factory=list, max_length=6)

class NutritionCandidate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    fdcId: int
    description: str
    dataType: str
    matchScore: float
    caloriesKcalPer100g: float | None
    proteinGPer100g: float | None
    carbsGPer100g: float | None
    fatGPer100g: float | None
    fiberGPer100g: float | None
    sugarGPer100g: float | None
    source: str = "USDA FoodData Central"

class AnalysisItem(IdentifiedFood):
    nutritionCandidates: list[NutritionCandidate] = Field(default_factory=list)
    caloriesKcal: float | None = None
    proteinG: float | None = None
    carbsG: float | None = None
    fatG: float | None = None
    fiberG: float | None = None
    sugarG: float | None = None
    nutritionSource: str | None = None

class AnalysisResponse(BaseModel):
    items: list[AnalysisItem]
    message: str
    requiresUserConfirmation: bool = True

FOOD_SCHEMA: dict[str, Any] = {
    "type": "object",
    "properties": {
        "items": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "alternatives": {"type": "array", "items": {"type": "string"}},
                    "portionGrams": {"type": "number"},
                    "servingDescription": {"type": "string"},
                    "pieceCount": {"type": ["number", "null"]},
                    "confidenceLabel": {"type": "string", "enum": ["low", "medium", "high"]},
                    "portionUncertainty": {"type": "string", "enum": ["low", "medium", "high"]},
                    "visualObservations": {"type": "array", "items": {"type": "string"}},
                },
                "required": [
                    "name", "alternatives", "portionGrams", "servingDescription",
                    "pieceCount", "confidenceLabel", "portionUncertainty", "visualObservations"
                ],
                "additionalProperties": False,
            },
        },
        "message": {"type": "string"},
    },
    "required": ["items", "message"],
    "additionalProperties": False,
}
NUTRIENT_IDS = {
    1008: "caloriesKcalPer100g",
    1003: "proteinGPer100g",
    1005: "carbsGPer100g",
    1004: "fatGPer100g",
    1079: "fiberGPer100g",
    2000: "sugarGPer100g",
}
_rate_events: dict[str, deque[float]] = defaultdict(deque)
_rate_lock = asyncio.Lock()
_cache: dict[str, tuple[float, dict[str, Any]]] = {}
_inflight: dict[str, asyncio.Task[dict[str, Any]]] = {}
_cache_lock = asyncio.Lock()

def normalize_image(raw: bytes, content_type: str | None) -> bytes:
    if content_type not in {"image/jpeg", "image/png", "image/webp"}:
        raise ValueError("Upload a JPEG, PNG or WebP food photo.")
    try:
        with Image.open(io.BytesIO(raw)) as image:
            if image.width * image.height > MAX_IMAGE_PIXELS:
                raise ValueError("Image dimensions are too large.")
            image.verify()
        with Image.open(io.BytesIO(raw)) as source:
            image = ImageOps.exif_transpose(source).convert("RGB")
            image.thumbnail((MAX_EDGE, MAX_EDGE), Image.Resampling.LANCZOS)
            output = io.BytesIO()
            image.save(output, format="JPEG", quality=84, optimize=True)
            return output.getvalue()
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError) as exc:
        raise ValueError("The uploaded image could not be decoded.") from exc

def extract_nutrients(nutrients: list[dict[str, Any]]) -> dict[str, float | None]:
    values: dict[str, float | None] = {key: None for key in NUTRIENT_IDS.values()}
    legacy_numbers = {208: 1008, 203: 1003, 205: 1005, 204: 1004, 291: 1079, 269: 2000}
    for item in nutrients:
        nutrient_id = item.get("nutrientId")
        if nutrient_id is None:
            try:
                nutrient_id = legacy_numbers.get(int(str(item.get("nutrientNumber", ""))))
            except ValueError:
                nutrient_id = None
        field = NUTRIENT_IDS.get(nutrient_id)
        if field is None:
            continue
        raw = item.get("value", item.get("amount"))
        try:
            values[field] = float(raw) if raw is not None else None
        except (TypeError, ValueError):
            values[field] = None
    return values

def build_candidate(food: dict[str, Any], query: str) -> dict[str, Any]:
    def normalize(value: str) -> str:
        return " ".join("".join(ch.lower() if ch.isalnum() else " " for ch in value).split())
    description = str(food.get("description") or "Unlabelled food")
    score = SequenceMatcher(None, normalize(query), normalize(description)).ratio()
    return {
        "fdcId": int(food.get("fdcId") or 0),
        "description": description,
        "dataType": str(food.get("dataType") or "Unknown"),
        "matchScore": round(score, 3),
        **extract_nutrients(food.get("foodNutrients") or []),
        "source": "USDA FoodData Central",
    }

async def post_json_with_retry(
    client: httpx.AsyncClient, url: str, *,
    headers: dict[str, str] | None = None,
    params: dict[str, str] | None = None,
    body: dict[str, Any] | None = None,
) -> httpx.Response:
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            response = await client.post(url, headers=headers, params=params, json=body)
            if (response.status_code == 429 or response.status_code >= 500) and attempt < 2:
                await asyncio.sleep(0.5 * (2 ** attempt))
                continue
            response.raise_for_status()
            return response
        except (httpx.TimeoutException, httpx.TransportError, httpx.HTTPStatusError) as exc:
            last_error = exc
            if attempt == 2 or (
                isinstance(exc, httpx.HTTPStatusError)
                and exc.response.status_code not in (429, 500, 502, 503, 504)
            ):
                break
            await asyncio.sleep(0.5 * (2 ** attempt))
    raise RuntimeError("An upstream service could not complete the request.") from last_error

async def identify_foods(image_jpeg: bytes) -> tuple[list[IdentifiedFood], str]:
    key = os.getenv("OPENAI_API_KEY", "").strip()
    if not key:
        raise HTTPException(status_code=503, detail="Food recognition is not configured on the server.")
    prompt = (
        "Identify only food components that are visibly supported by this photograph. Return separate "
        "components for mixed plates. Do not invent hidden ingredients, sauces, oils, or preparation methods. "
        "Estimate visible edible portion weight conservatively. If scale is unclear, mark portionUncertainty "
        "high and describe uncertainty. Give no calories or nutrient values. If no food is clearly visible, "
        "return an empty items array."
    )
    data_url = "data:image/jpeg;base64," + base64.b64encode(image_jpeg).decode("ascii")
    body = {
        "model": VISION_MODEL,
        "store": False,
        "max_output_tokens": 2500,
        "input": [{
            "role": "user",
            "content": [
                {"type": "input_text", "text": prompt},
                {"type": "input_image", "image_url": data_url, "detail": "high"},
            ],
        }],
        "text": {
            "format": {
                "type": "json_schema",
                "name": "nutriguide_food_components",
                "strict": True,
                "schema": FOOD_SCHEMA,
            }
        },
    }
    async with httpx.AsyncClient(timeout=httpx.Timeout(45.0, connect=10.0)) as client:
        response = await post_json_with_retry(
            client, OPENAI_URL,
            headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
            body=body,
        )
    payload = response.json()
    output_text = payload.get("output_text")
    if not output_text:
        for output in payload.get("output", []):
            if output.get("type") == "message":
                for item in output.get("content", []):
                    if item.get("type") == "output_text":
                        output_text = item.get("text")
                        break
            if output_text:
                break
    if not output_text:
        raise RuntimeError("Vision model returned no structured output.")
    parsed = json.loads(output_text)
    return [IdentifiedFood.model_validate(item) for item in parsed.get("items", [])], str(parsed.get("message") or "Review each food and portion estimate.")

async def search_nutrition(name: str) -> list[dict[str, Any]]:
    key = os.getenv("USDA_API_KEY", "").strip()
    if not key:
        return []
    body = {"query": name, "pageSize": 8, "dataType": ["Foundation", "SR Legacy", "Survey (FNDDS)"]}
    try:
        async with httpx.AsyncClient(timeout=httpx.Timeout(12.0, connect=5.0)) as client:
            response = await post_json_with_retry(client, USDA_SEARCH_URL, params={"api_key": key}, body=body)
        candidates = [
            build_candidate(food, name)
            for food in response.json().get("foods", [])
            if food.get("fdcId") is not None
        ]
        return sorted(candidates, key=lambda item: item["matchScore"], reverse=True)[:5]
    except Exception as exc:
        logger.warning("USDA lookup unavailable error_type=%s", type(exc).__name__)
        return []

async def perform_analysis(image: bytes) -> dict[str, Any]:
    foods, _ = await identify_foods(image)
    semaphore = asyncio.Semaphore(3)
    async def enrich(food: IdentifiedFood) -> dict[str, Any]:
        async with semaphore:
            candidates = await search_nutrition(food.name)
        result = food.model_dump()
        result["nutritionCandidates"] = candidates
        # No macros are assigned until the user selects a real database record.
        result.update({
            "caloriesKcal": None, "proteinG": None, "carbsG": None, "fatG": None,
            "fiberG": None, "sugarG": None, "nutritionSource": None,
        })
        return result
    items = await asyncio.gather(*(enrich(food) for food in foods))
    if not items:
        message = "No food components could be identified with enough visual evidence. Add foods manually."
    elif not os.getenv("USDA_API_KEY", "").strip():
        message = "Foods identified. Nutrition matching is unavailable until FoodData Central is configured."
    elif not any(item["nutritionCandidates"] for item in items):
        message = "Foods identified, but no database matches were found. Nutrients remain unknown."
    else:
        message = "Choose the closest USDA match for each food, then review its serving amount."
    return {"items": items, "message": message, "requiresUserConfirmation": True}

async def coalesced_analysis(key: str, image: bytes) -> dict[str, Any]:
    now = time.monotonic()
    async with _cache_lock:
        cached = _cache.get(key)
        if cached and now - cached[0] < CACHE_SECONDS:
            return cached[1]
        task = _inflight.get(key)
        owner = task is None
        if task is None:
            task = asyncio.create_task(perform_analysis(image))
            _inflight[key] = task
    try:
        result = await task
        if owner:
            async with _cache_lock:
                _cache[key] = (time.monotonic(), result)
        return result
    finally:
        if owner:
            async with _cache_lock:
                _inflight.pop(key, None)
                for cache_key, (created, _) in list(_cache.items()):
                    if time.monotonic() - created > CACHE_SECONDS:
                        _cache.pop(cache_key, None)

async def verify_client(token: str | None = Header(default=None, alias="X-NutriGuide-Client-Token")) -> None:
    expected = os.getenv("NUTRIGUIDE_CLIENT_TOKEN", "").strip()
    if len(expected) < 32:
        raise HTTPException(status_code=503, detail="Analysis access is not configured.")
    if not token or not hmac.compare_digest(token, expected):
        raise HTTPException(status_code=401, detail="This app installation is not authorised for analysis.")

async def enforce_rate_limit(request: Request) -> None:
    address = request.client.host if request.client else "unknown"
    now = time.monotonic()
    async with _rate_lock:
        events = _rate_events[address]
        while events and now - events[0] > 60:
            events.popleft()
        if len(events) >= RATE_LIMIT_PER_MINUTE:
            raise HTTPException(status_code=429, detail="Too many scans. Try again shortly.")
        events.append(now)

@app.get("/health")
async def health() -> dict[str, Any]:
    return {
        "status": "ok",
        "visionConfigured": bool(os.getenv("OPENAI_API_KEY", "").strip()),
        "nutritionConfigured": bool(os.getenv("USDA_API_KEY", "").strip()),
    }

@app.post("/food-analysis", response_model=AnalysisResponse, dependencies=[Depends(verify_client), Depends(enforce_rate_limit)])
async def analyze_food(photo: UploadFile = File(...)) -> AnalysisResponse:
    content_type = (photo.content_type or "").lower()
    if content_type not in {"image/jpeg", "image/png", "image/webp"}:
        raise HTTPException(status_code=415, detail="Upload a JPEG, PNG or WebP image.")
    raw = await photo.read(MAX_UPLOAD_BYTES + 1)
    if len(raw) > MAX_UPLOAD_BYTES:
        raise HTTPException(status_code=413, detail="Image exceeds the 10 MB limit.")
    if not raw:
        raise HTTPException(status_code=400, detail="The uploaded photo is empty.")
    try:
        normalized = normalize_image(raw, content_type)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    digest = hashlib.sha256(normalized).hexdigest()
    try:
        data = await coalesced_analysis(digest, normalized)
        return AnalysisResponse.model_validate(data)
    except HTTPException:
        raise
    except (httpx.HTTPError, RuntimeError, ValueError, ValidationError) as exc:
        logger.error("food analysis failed error_type=%s", type(exc).__name__)
        raise HTTPException(status_code=502, detail="Food analysis is unavailable. Add foods manually.") from exc
