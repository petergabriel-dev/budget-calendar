# 13 — Account Description Field

Adds an optional `description` field to the `Account` domain model, database schema, and create/edit form. This is a prerequisite for the Accounts Screen Redesign (14), which renders descriptions on account cards.

Shadow spec: `specs/shadow/Account.shadow.md`
Design file: `specs/DESIGN_SYSTEM.pen`

---

## Tasks

- [x] Add SQLDelight migration `6.sqm` at `src/commonMain/sqldelight/com/petergabriel/budgetcalendar/core/database/6.sqm`. Content: `ALTER TABLE accounts ADD COLUMN description TEXT;` — no table rebuild needed, nullable column with no default is valid. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [x] Update `accounts.sq` to include `description` everywhere. In all `SELECT *`-style queries add `description` to the column list. Add `:description` parameter to `insertAccount` and `updateAccount`. The `getAllAccounts`, `getAccountById`, and `getSpendingPoolAccounts` queries must return `description`. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [x] Update `Account` data class at `features/accounts/domain/model/Account.kt` to add `description: String?` as the last field. Update `CreateAccountRequest` to add `description: String?`. Update `UpdateAccountRequest` to add `description: String?`. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [x] Update `AccountMapper` to map the new `description` column from the SQLDelight-generated row type to the `Account` domain model. Update `AccountRepositoryImpl` to thread `description` through `insertAccount` and `updateAccount` calls. Use `cocoindex-code` to locate both files before editing — search `"AccountMapper accounts"` and `"AccountRepositoryImpl insert"`. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)

- [x] Update `AccountFormSheet` to add an optional description field. Use `BcInputGroup(label = "Description", value = descriptionInput, onValueChange = { descriptionInput = it }, placeholder = "e.g. Primary funding source")` with a 100-character soft cap (show char count, do not hard-block). Wire `descriptionInput` into the `CreateAccountRequest` and `UpdateAccountRequest`. Pre-populate from `initialData?.description` in edit mode. Use `cocoindex-code` to locate `AccountFormSheet` before editing — search `"AccountFormSheet create account form"`. (Use Skill: kmp) (Reference: specs/shadow/Account.shadow.md)
