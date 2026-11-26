#!/usr/bin/env python3
"""Evaluate food-component identification, portion, and nutrition errors on a labelled holdout CSV."""

from __future__ import annotations

import argparse
import csv
import json
import math
import re
from pathlib import Path
from typing import Iterable


def number(value: str | None) -> float | None:
    if value is None or not value.strip():
        return None
    try:
        parsed = float(value)
    except ValueError:
        return None
    return parsed if math.isfinite(parsed) else None


def normalise(value: str | None) -> str:
    return re.sub(r"[^a-z0-9]+", " ", (value or "").lower()).strip()


def mean(values: Iterable[float]) -> float | None:
    data = list(values)
    return sum(data) / len(data) if data else None


def evaluate(rows: list[dict[str, str]]) -> dict:
    tp = fp = fn = 0
    exact = expected_count = 0
    portion_errors: list[float] = []
    kcal_errors: list[float] = []
    kcal_percent_errors: list[float] = []
    macro_percent_errors: dict[str, list[float]] = {
        "protein": [], "carbs": [], "fat": []
    }

    for row in rows:
        expected = normalise(row.get("expected_food"))
        predicted = normalise(row.get("predicted_food"))
        if expected:
            expected_count += 1
        if expected and predicted and expected == predicted:
            tp += 1
            exact += 1
        else:
            if predicted:
                fp += 1
            if expected:
                fn += 1

        reference = number(row.get("reference_grams"))
        estimate = number(row.get("predicted_grams"))
        if reference is not None and estimate is not None:
            portion_errors.append(abs(estimate - reference))

        for suffix, errors, percent_errors in [
            ("kcal", kcal_errors, kcal_percent_errors)
        ]:
            reference_value = number(row.get(f"reference_{suffix}"))
            predicted_value = number(row.get(f"predicted_{suffix}"))
            if reference_value is not None and predicted_value is not None:
                errors.append(abs(predicted_value - reference_value))
                if reference_value > 0:
                    percent_errors.append(abs(predicted_value - reference_value) / reference_value * 100)

        for nutrient in ("protein", "carbs", "fat"):
            ref = number(row.get(f"reference_{nutrient}_g"))
            pred = number(row.get(f"predicted_{nutrient}_g"))
            if ref is not None and pred is not None and ref > 0:
                macro_percent_errors[nutrient].append(abs(pred - ref) / ref * 100)

    precision = tp / (tp + fp) if tp + fp else None
    recall = tp / (tp + fn) if tp + fn else None
    f1 = (
        2 * precision * recall / (precision + recall)
        if precision is not None and recall is not None and precision + recall > 0
        else None
    )

    return {
        "rows": len(rows),
        "expected_components": expected_count,
        "identification": {
            "true_positive": tp,
            "false_positive": fp,
            "false_negative": fn,
            "exact_label_rate": exact / expected_count if expected_count else None,
            "precision": precision,
            "recall": recall,
            "f1": f1,
        },
        "portion_grams": {
            "n": len(portion_errors),
            "mae": mean(portion_errors),
        },
        "energy_kcal": {
            "n": len(kcal_errors),
            "mae": mean(kcal_errors),
            "mape_percent": mean(kcal_percent_errors),
        },
        "macro_mape_percent": {
            nutrient: {
                "n": len(values),
                "mape": mean(values),
            }
            for nutrient, values in macro_percent_errors.items()
        },
        "notes": [
            "Exact string matching is intentionally conservative; use a reviewed synonym map before final evaluation.",
            "Metrics exclude missing numeric pairs and report their sample counts.",
            "Rows are treated as pre-aligned component pairs; annotate unmatched detections/misses explicitly.",
        ],
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--input", required=True, type=Path)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    if not args.input.is_file():
        parser.error(f"Input file does not exist: {args.input}")

    with args.input.open(newline="", encoding="utf-8") as handle:
        rows = list(csv.DictReader(handle))

    if not rows:
        parser.error("Input CSV has no data rows.")

    result = evaluate(rows)
    rendered = json.dumps(result, indent=2)
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered + "\n", encoding="utf-8")
    print(rendered)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
