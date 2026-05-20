# Common item validation

All product categories share the same base fields. Validation is enforced in two layers:

1. **Request layer** — `ItemService.createItem()` calls `Item.validateCommonFields()` before factory construction and database insert. Failures surface as `BusinessException` with messages from `Item.ValidationMessages`.
2. **Entity layer** — Item subclasses invoke `validateBaseItem()` from constructors and setters, delegating to the same `validateCommonFields()` rules.

## Required fields

| Field | Rule | Constant / message |
|-------|------|-------------------|
| Name | Non-blank after trim; max 255 characters (DB limit; UI may use 100) | `NAME_REQUIRED`, `NAME_TOO_LONG` |
| Starting price | Required; numeric `BigDecimal`; must be ≥ 0 | `STARTING_PRICE_REQUIRED`, `STARTING_PRICE_NON_NEGATIVE` |
| Category | Required; must be a valid `Item.ItemCategory` enum value | `CATEGORY_REQUIRED` |
| Description | Non-blank after trim; max 2000 characters (DB limit; UI may use 500) | `DESCRIPTION_REQUIRED`, `DESCRIPTION_TOO_LONG` |
| Owner id | Non-blank after trim; max 36 characters (UUID) | `OWNER_ID_REQUIRED`, `OWNER_ID_TOO_LONG` |

## API behavior

- Invalid create requests fail before `ItemDAO.insert()`.
- Category-specific rules (Art medium, Vehicle odometer, etc.) are validated separately in factories and subtype constructors.

## Tests

`ItemServiceTest.CommonItemValidationTests` covers blank/oversized name, null/negative price, missing category, blank/oversized description, blank owner id, and zero starting price success path.
