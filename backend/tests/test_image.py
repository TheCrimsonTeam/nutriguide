import io

import pytest
from fastapi.testclient import TestClient
from PIL import Image

from app import main
from app.main import normalize_image


def sample_image():
    buffer = io.BytesIO()
    Image.new("RGB", (240, 120), color=(220, 220, 220)).save(buffer, format="JPEG")
    return buffer.getvalue()


def test_valid_jpeg_is_normalized():
    result = normalize_image(sample_image(), "image/jpeg")
    with Image.open(io.BytesIO(result)) as image:
        assert image.format == "JPEG"
        assert image.size == (240, 120)


def test_unsupported_mime_is_rejected():
    with pytest.raises(ValueError):
        normalize_image(sample_image(), "image/gif")


def test_corrupt_image_is_rejected():
    with pytest.raises(ValueError):
        normalize_image(b"not a jpeg", "image/jpeg")


def test_health_reports_provider_configuration(monkeypatch):
    monkeypatch.setenv("OPENAI_API_KEY", "configured-test-key")
    monkeypatch.setenv("USDA_API_KEY", "configured-test-key")

    response = TestClient(main.app).get("/health")

    assert response.status_code == 200
    assert response.json() == {
        "status": "ok",
        "visionConfigured": True,
        "nutritionConfigured": True,
    }


def test_analysis_rejects_unconfigured_client_gate(monkeypatch):
    monkeypatch.delenv("NUTRIGUIDE_CLIENT_TOKEN", raising=False)

    response = TestClient(main.app).post(
        "/food-analysis",
        files={"photo": ("meal.jpg", sample_image(), "image/jpeg")},
    )

    assert response.status_code == 503


def test_analysis_rejects_wrong_client_token(monkeypatch):
    monkeypatch.setenv("NUTRIGUIDE_CLIENT_TOKEN", "a" * 40)

    response = TestClient(main.app).post(
        "/food-analysis",
        headers={"X-NutriGuide-Client-Token": "wrong-token"},
        files={"photo": ("meal.jpg", sample_image(), "image/jpeg")},
    )

    assert response.status_code == 401


def test_analysis_rejects_unsupported_media_type(monkeypatch):
    monkeypatch.setenv("NUTRIGUIDE_CLIENT_TOKEN", "a" * 40)

    response = TestClient(main.app).post(
        "/food-analysis",
        headers={"X-NutriGuide-Client-Token": "a" * 40},
        files={"photo": ("meal.gif", b"GIF89a", "image/gif")},
    )

    assert response.status_code == 415


def test_analysis_rejects_oversized_upload(monkeypatch):
    monkeypatch.setenv("NUTRIGUIDE_CLIENT_TOKEN", "a" * 40)
    monkeypatch.setattr(main, "MAX_UPLOAD_BYTES", 10)

    response = TestClient(main.app).post(
        "/food-analysis",
        headers={"X-NutriGuide-Client-Token": "a" * 40},
        files={"photo": ("meal.jpg", sample_image(), "image/jpeg")},
    )

    assert response.status_code == 413
