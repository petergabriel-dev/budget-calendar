# Spec Audit Report - Iteration #1

## Summary

- **Total Issues Found**: 14
- **Critical**: 5
- **Warnings**: 6
- **Info**: 3
- **Confidence Level**: YELLOW

The main specs provide a solid foundation but contain significant inconsistencies between files that would cause implementation conflicts. Most critical are database schema mismatches between DB_DESIGN.md and the entity definitions in ARCHITECTURE.md and FEATURES.md.

---

## Issues

### Critical

#### 1. ID Type Mismatch - UUID vs INTEGER

**Location**: ARCHITECTURE.md (lines 238, 258) vs DB_DESIGN.md (lines 29, 63)

**Description**: 
- ARCHITECTURE.md defines all entity IDs as UUID strings: `id: string; // UUID`
- DB_DESIGN.md uses INTEGER PRIMARY KEY AUTOINCREMENT for all tables

**Recommendation**: Choose one ID strategy consistently. For a multiplatform app with potential future sync, UUID strings are recommended. Update DB_DESIGN.md to use TEXT for IDs or add migration note.

---

#### 2. Missing Tables for Safe to Spend Feature

**Location**: FEATURES.md (lines 231-232) vs DB_DESIGN.md

**Description**: 
- FEATURES.md references `monthly_rollovers` table and `safe_to_spend_cache` table for Safe to Spend calculation
- DB_DESIGN.md does not define these tables

**Recommendation**: Add to DB_DESIGN.md:
```sql
CREATE TABLE monthly_rollovers (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    year INTEGER NOT NULL,
    month INTEGER NOT NULL,
    rollover_amount INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    UNIQUE(year, month)
);

CREATE TABLE safe_to_spend_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    calculated_amount INTEGER NOT NULL,
    calculated_at INTEGER NOT NULL,
    is_sandbox INTEGER NOT NULL DEFAULT 0
);
```

---

#### 3. Missing Tables for Credit Card Feature

**Location**: FEATURES.md (lines 339-340) vs DB_DESIGN.md

**Description**:
- FEATURES.md references `cc_reserved` table and `credit_card_settings` table
- DB_DESIGN.md does not define these tables

**Recommendation**: Add to DB_DESIGN.md:
```sql
CREATE TABLE credit_card_settings (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_id INTEGER NOT NULL REFERENCES accounts(id) ON DELETE CASCADE,
    credit_limit INTEGER,
    statement_balance INTEGER,
    due_date INTEGER,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);
```
Note: Reserved amounts can be calculated from transactions; if caching is needed, add `cc_reserved` table.

---

#### 4. Missing Transaction Fields for Transfers

**Location**: FEATURES.md (lines 105, 118) vs DB_DESIGN.md (transactions table)

**Description**:
- FEATURES.md requires `destination_account_id` for TRANSFER type transactions (line 105)
- FEATURES.md mentions `linked_transaction_id` for transfer linking (line 118)
- DB_DESIGN.md transactions table does not include these columns

**Recommendation**: Add columns to transactions table in DB_DESIGN.md:
```sql
destination_account_id INTEGER REFERENCES accounts(id),
linked_transaction_id INTEGER REFERENCES transactions(id),
```

---

#### 5. Missing Recurring Transaction Fields on Main Table

**Location**: FEATURES.md (line 118) vs DB_DESIGN.md

**Description**:
- FEATURES.md mentions `recurrence_pattern` and `recurrence_end_date` fields on transactions
- DB_DESIGN.md has separate `recurring_transactions` table but transactions table lacks these fields

**Recommendation**: Either:
a) Add `recurrence_pattern TEXT`, `recurrence_end_date INTEGER`, `is_recurring INTEGER` to transactions table, OR
b) Clarify in FEATURES.md that recurring transactions are managed entirely through `recurring_transactions` table and transactions are generated from them

---

### Warnings

#### 6. Date Format Inconsistency

**Location**: ARCHITECTURE.md (line 263) vs DB_DESIGN.md (line 66)

**Description**:
- ARCHITECTURE.md shows dates as ISO 8601 strings: `date: string; // ISO 8601 date`
- DB_DESIGN.md uses INTEGER for Unix timestamps in milliseconds

**Recommendation**: This is acceptable if consistent. Update ARCHITECTURE.md schema to show INTEGER for dates to match DB_DESIGN.md.

---

#### 7. is_sandbox Flag vs Separate Sandbox Table

**Location**: ARCHITECTURE.md (lines 375-383) vs DB_DESIGN.md

**Description**:
- ARCHITECTURE.md defines separate `sandbox_transactions` table (lines 375-383)
- DB_DESIGN.md has `is_sandbox` INTEGER flag on transactions table (line 71)

**Recommendation**: Choose one approach. The `is_sandbox` flag approach is simpler and avoids data duplication. Remove sandbox_transactions table from ARCHITECTURE.md or add justification for both.

---

#### 8. Missing kotlinx-serialization and Ktor in Dependencies

**Location**: ARCHITECTURE.md (lines 428-429) vs DEPENDENCIES.md

**Description**:
- ARCHITECTURE.md lists `kotlinx-serialization` and `Ktor Client` as external dependencies for future API
- DEPENDENCIES.md does not include these packages

**Recommendation**: Add to DEPENDENCIES.md:
| Package | Version | Purpose |
|---------|---------|---------|
| kotlinx-serialization-json | 1.6.2 | JSON serialization |
| ktor-client-core | 2.3.7 | HTTP client |
| ktor-client-content-negotiation | 2.3.7 | Content negotiation |

---

#### 9. Missing BudgetViewModel in DI Module

**Location**: ARCHITECTURE.md (line 332)

**Description**: 
- ARCHITECTURE.md lists `BudgetViewModel` in viewModelModule (line 332)
- Package structure (lines 150-154) shows `budget/` feature but no `presentation/` subdirectory with ViewModel

**Recommendation**: Ensure budget feature follows consistent package structure with presentation layer, or remove BudgetViewModel from DI if budget calculations are read-only.

---

#### 10. Recurring Rule Format Mismatch

**Location**: ARCHITECTURE.md (line 266) vs DB_DESIGN.md

**Description**:
- ARCHITECTURE.md shows `recurringRule?: string; // RFC 5545 RRULE` on Transaction
- DB_DESIGN.md `recurring_transactions` table uses `day_of_month INTEGER` (simpler recurrence)

**Recommendation**: Clarify recurrence approach. RFC 5545 RRULE is more powerful but complex. Current DB schema supports simple monthly recurrence only.

---

#### 11. Sandbox Expiration Not in Database

**Location**: FEATURES.md (line 266) vs DB_DESIGN.md

**Description**:
- FEATURES.md states "Sandbox expires after 30 days of inactivity" (SAN-010)
- DB_DESIGN.md sandbox_snapshots table lacks `last_accessed_at` or expiration tracking

**Recommendation**: Add to sandbox_snapshots table:
```sql
last_accessed_at INTEGER NOT NULL,
```

---

### Info

#### 12. Account Deletion Rule Inconsistency

**Location**: FEATURES.md (line 27) vs DB_DESIGN.md

**Description**:
- FEATURES.md says "Account deletion is only allowed if no transactions reference it"
- DB_DESIGN.md has `ON DELETE CASCADE` on account_id foreign key

**Recommendation**: If cascade delete is intended, FEATURES.md rule ACC-007 should be updated to reflect automatic cleanup behavior.

---

#### 13. Missing Currency Symbol Configuration

**Location**: FEATURES.md vs DESIGN_SYSTEM.md

**Description**:
- FEATURES.md mentions "Currency symbol is user-configurable" (SHR-003)
- DESIGN_SYSTEM.md uses hardcoded `$` in all examples

**Recommendation**: Add currency configuration to Account Management feature or remove from shared rules.

---

#### 14. Transaction State Transition Timing

**Location**: FEATURES.md (line 82) vs DB_DESIGN.md

**Description**:
- FEATURES.md: "Pending transactions automatically transition to OVERDUE when current_date > transaction_date + 1 day"
- No database job/scheduler defined in DB_DESIGN.md for this transition

**Recommendation**: Document that overdue transition is handled at app runtime (on fetch) or add scheduled job documentation.

---

## Spec Completeness Check

| Section | Status | Notes |
|---------|--------|-------|
| ARCHITECTURE.md | ⚠️ Incomplete | Package structure needs budget presentation; ID types mismatch |
| DB_DESIGN.md | ⚠️ Incomplete | Missing tables (monthly_rollovers, credit_card_settings); missing fields |
| DESIGN_SYSTEM.md | ✅ Complete | Well-structured, comprehensive |
| FEATURES.md | ✅ Complete | Detailed business rules, but some DB references missing |
| DEPENDENCIES.md | ⚠️ Incomplete | Missing future API dependencies |

---

## Recommendations

### Immediate Actions (Before Implementation)

1. **Resolve ID type**: Choose UUID strings for IDs, update DB_DESIGN.md schema
2. **Add missing tables**: monthly_rollovers, credit_card_settings (or document as calculated)
3. **Add missing columns**: destination_account_id, linked_transaction_id on transactions
4. **Sync dependencies**: Add kotlinx-serialization and Ktor to DEPENDENCIES.md

### Documentation Improvements

1. **Clarify recurrence approach**: Simple day_of_month vs RFC 5545 RRULE
2. **Document sandbox architecture**: is_sandbox flag vs separate table
3. **Update ARCHITECTURE.md schemas**: Match date types to DB (INTEGER timestamps)

### Design Decisions Needed

1. Should budget calculations be cached in database or computed on-demand?
2. How to handle credit card reserved amounts - calculate or store?
3. Offline-first: How to handle future sync when backend is added?

---

## Conclusion

The spec suite is comprehensive and well-organized, providing clear business logic and UI specifications. However, the database schema needs alignment with the feature requirements and architecture definitions before implementation begins. The critical issues (ID types, missing tables/columns) would cause significant rework if implemented as-is.

**Confidence: YELLOW** - Proceed with implementation after addressing Critical issues.
