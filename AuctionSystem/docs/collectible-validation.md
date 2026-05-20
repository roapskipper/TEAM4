# Collectible category validation

Collectible items extend common item rules with category-specific required and optional fields.

## Enforcement layers

1. **Request layer** — `ItemService.createItem()` calls `Collectible.validateCollectibleFields()` when `category == COLLECTIBLE`, before `CollectibleFactory` runs.
2. **Factory layer** — `CollectibleFactory.createItem()` repeats the same static validation before constructing the entity.
3. **Entity layer** — `Collectible` constructors and setters call `validateCollectibleCategory()`, which delegates to `validateCollectibleFields()`.

Failures at the service layer surface as `BusinessException` with messages from `Collectible.ValidationMessages`.

## Required fields

| Field | Rule | Message constant |
|-------|------|------------------|
| Rarity level | Must be a non-null `Collectible.RarityLevel` | `RARITY_REQUIRED` |
| Condition grade | Must be a non-null `Collectible.ConditionGrade` | `CONDITION_REQUIRED` |

## Optional fields

| Field | Rule |
|-------|------|
| Year of origin | Optional; `0` means unknown. If set (non-zero), must be between -3000 and the current year. |
| Origin | Optional; blank/null defaults to `Unknown` via `Collectible.resolveOrigin()`. Max 120 characters when provided. |
| Has certificate | Optional boolean; defaults to `false` on requests. |

## Tests

- `ItemServiceTest.CollectibleValidationTests` — missing rarity, missing condition, successful create, blank origin → `Unknown`.
- `ItemFactoryTest.CollectibleFactoryTests` — factory-level missing rarity rejection.
