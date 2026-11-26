# Nutrition data sources and licensing

This is a source assessment, not a grant of permission. Re-check each upstream source's current terms before distributing data or shipping a product.

## USDA FoodData Central

- Official API guide: https://fdc.nal.usda.gov/api-guide/
- USDA states FoodData Central data are public domain and not copyrighted, under CC0 1.0 Universal.
- API access uses a data.gov API key. The key belongs on a backend, not in the Android binary.
- Suitable as an international reference source, especially for generic ingredients and branded foods where covered.
- The source should be attributed in the app and in documentation as requested by USDA.

## Indian Food Composition Tables 2017 (IFCT)

- Official publication: https://www.nin.res.in/ebooks/IFCT2017.pdf
- Publisher: ICMR–National Institute of Nutrition.
- The publication's copyright page says personal-use reproduction is permitted with full acknowledgment, but electronic storage or reproduction for creating a product requires prior written permission from NIN, Hyderabad.
- Therefore NutriGuide must **not** bundle an extracted IFCT database, copy its tables into Room, or distribute a derived electronic dataset until written permission is obtained and its exact scope is recorded.
- The data can be evaluated as a candidate reference source once permission and update/version handling are resolved.

## Indian recipes and prepared dishes

Raw-ingredient datasets are not a sufficient source for every cooked Indian dish. Oil, water loss, recipe proportions, regional preparation and serving vessels materially affect a cooked dish's nutrient density.

Potential sources such as the Indian Nutrient Databank or recently published community datasets need an explicit licence review, source provenance, recipe/version checks and a data-quality assessment before ingestion. Do not assume that an openly accessible spreadsheet or website grants redistribution rights.

## Source-selection policy

- Never silently choose the first search result when multiple foods are plausible.
- Distinguish raw ingredient, cooked ingredient, restaurant dish, home recipe and branded product.
- Keep data provenance and units attached to every nutrient record.
- State when a source is missing, mismatched or not licensed for product use.
- Permit a user correction without overwriting source records.

## Useful references

- USDA FDC API guide and licence: https://fdc.nal.usda.gov/api-guide/
- IFCT 2017 official publication: https://www.nin.res.in/ebooks/IFCT2017.pdf
- FAO overview of India's 2017 food composition tables: https://www.fao.org/food-composition/tables-and-databases/detail/(country--date)-title-9/en
