from app.main import build_candidate, extract_nutrients

def test_extracts_usda_nutrient_ids():
    result = extract_nutrients([
        {"nutrientId": 1008, "value": 130.0},
        {"nutrientId": 1003, "value": 2.7},
        {"nutrientId": 1005, "value": 28.0},
        {"nutrientId": 1004, "value": 0.3},
        {"nutrientId": 1079, "value": 0.4},
        {"nutrientId": 2000, "value": 0.1},
    ])
    assert result == {
        "caloriesKcalPer100g": 130.0,
        "proteinGPer100g": 2.7,
        "carbsGPer100g": 28.0,
        "fatGPer100g": 0.3,
        "fiberGPer100g": 0.4,
        "sugarGPer100g": 0.1,
    }

def test_missing_values_stay_unknown():
    result = extract_nutrients([{"nutrientId": 1003, "value": 5.0}])
    assert result["proteinGPer100g"] == 5.0
    assert result["caloriesKcalPer100g"] is None
    assert result["fiberGPer100g"] is None

def test_candidate_has_source_and_rank_hint():
    result = build_candidate({
        "fdcId": 123,
        "description": "Cooked white rice",
        "dataType": "SR Legacy",
        "foodNutrients": [{"nutrientId": 1008, "value": 130.0}],
    }, "white rice cooked")
    assert result["fdcId"] == 123
    assert result["source"] == "USDA FoodData Central"
    assert 0 <= result["matchScore"] <= 1
    assert result["caloriesKcalPer100g"] == 130.0
