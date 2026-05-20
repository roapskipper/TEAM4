# Category default normalization

Optional category fields receive backend defaults before entity construction and persistence. Required fields are still validated after defaults are applied.

## Flow

1. `ItemService.createItem()` validates common and category-specific rules.
2. `ItemRequestDefaults.apply(itemRequest)` mutates the request with category defaults.
3. Category factories build entities; constructors apply the same `resolve*` helpers for consistency.

## Defaults by category

| Category | Field | When blank / missing | Default |
|----------|-------|----------------------|---------|
| Art | `artist` | null or whitespace | `Unknown` |
| Art | `creationYear` | not provided (int `0`) | `0` (unknown year) |
| Electronics | `brand`, `model` | null or whitespace | `Unknown` |
| Fashion | `gender` | null | `UNISEX` |
| Vehicle | `transmission` | null | `OTHER` |

## Implementation

- Shared helper: `Item.normalizeDefaultString(value, defaultValue)`
- Request layer: `ItemRequestDefaults` in `com.team4.factory`
- Model layer: `Art.resolveArtist`, `Electronics.resolveBrand` / `resolveModel`, `Fashion.resolveGender`, `Vehicle.resolveTransmission`

## Validation

Defaults do not satisfy required enum fields. For example, blank `artist` becomes `Unknown`, but `medium == null` still fails with `Art medium is required.`

## Tests

- `DefaultNormalizationTest` — constructor-level defaults
- `ItemRequestDefaultsTest` — request mutation
- `ItemServiceTest.CategoryDefaultNormalizationTests` — end-to-end create path and required-field guard tests
