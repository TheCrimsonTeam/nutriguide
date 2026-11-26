# Food-photo accuracy protocol

## No accuracy claim yet

NutriGuide currently has no labelled photo dataset, independent reference meal measurements, or completed benchmark run. Therefore there is **no measured accuracy result** and no 95% accuracy claim.

## Evaluate separate tasks

Report each layer separately; a strong food label score does not prove portion or nutrient accuracy.

1. **Food identification:** per-component precision, recall and F1; exact-label match rate; meal-level component recall for multi-item meals.
2. **Portion estimation:** grams mean absolute error (MAE), median absolute error and percentage error, stratified by food type and serving-size band.
3. **Nutrition estimation:** absolute kcal MAE and percentage error; protein/carbohydrate/fat MAE and percentage error; report coverage only for source-resolved components.
4. **Calibration:** expected calibration error or reliability bins for the model's confidence values, if the provider supplies calibrated confidence.
5. **Abstention:** proportion of meals sent to user clarification and error rate among accepted results.

## Dataset protocol

Create a rights-cleared, representative test set containing:
- Indian home-cooked multi-component plates and regional dishes.
- Separate ingredients, mixed curries, dals, rice, breads, snacks and breakfast dishes.
- International foods.
- Multiple portion sizes, cookware and lighting conditions.
- Visually similar foods and hidden preparation differences.
- Multiple photographs per meal only if all images remain in a single train/validation/test partition.

For every reference meal, record weighed component grams, exact recipe/ingredient quantities, cooking method, and the composition database identifier used to compute reference nutrients. Use a held-out test set that was not used to prompt-tune or train the model. Have labels checked independently by two reviewers and resolve disagreements before scoring.

## Running the benchmark

The script expects one component per CSV row. Blank predicted names count as missed detections; rows with a prediction but no expected name count as false positives.

```bash
python3 tools/benchmark_food_accuracy.py   --input benchmarks/labelled_holdout.csv   --output benchmarks/results.json
```

See `benchmarks/food_accuracy_template.csv` for the required columns. The script reports identification precision/recall/F1, exact label rate, portion MAE, kcal MAE/MAPE and macro MAPE where reference values are positive.

Do not publish one overall "95%" number without the dataset size, sampling description, confidence intervals, task-specific metrics and error distribution.
