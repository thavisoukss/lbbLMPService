# Table: ACCOUNT

## Columns

| # | Column | Type | Length/Precision | Nullable | Default | Description |
|---|--------|------|-----------------|----------|---------|-------------|
| 1 | ID | NUMBER | 22 | NOT NULL | | Primary key |
| 2 | CUSTOMER_ID | VARCHAR2 | 50 | NULL | | |
| 3 | ACCOUNT_NO | VARCHAR2 | 50 | NOT NULL | | |
| 4 | ACCOUNT_NAME | VARCHAR2 | 100 | NULL | | |
| 5 | ACCOUNT_TYPE | VARCHAR2 | 50 | NULL | | |
| 6 | ACCOUNT_CURRENCY | VARCHAR2 | 10 | NULL | `'LAK'\|\|'LBI'` | |
| 7 | DESCRIPTION | VARCHAR2 | 100 | NULL | | |
| 8 | STATUS | VARCHAR2 | 20 | NULL | `'ACTIVE'` | |
| 9 | CREATED_AT | TIMESTAMP(6) | | NULL | `CURRENT_TIMESTAMP` | |
| 10 | UPDATED_AT | TIMESTAMP(6) | | NULL | | |
| 11 | DELETE_AT | TIMESTAMP(6) | | NULL | | |
| 12 | BALANCE | NUMBER | 30,2 | NULL | | |
| 13 | MIGRATE_STATUS | VARCHAR2 | 20 | NULL | | For user migrate. Set to `PENDING` for old user and `COMPLETED` when user migrated |

## Constraints

| Name | Type | Detail |
|------|------|--------|
| SYS_C008156 | PRIMARY KEY | `ID` |
| UQ_BANK_ACCOUNTS_ACCOUNT_NO | UNIQUE | `ACCOUNT_NO` |
| FK_ACCOUT_ACCOUNT_NO | FOREIGN KEY | References `SYS_C007827` |
| CK_ACCOUNT_MIGRATE_STATUS | CHECK | `MIGRATE_STATUS IN ('PENDING', 'COMPLETED')` |
| SYS_C008863 | CHECK | `ACCOUNT_NO IS NOT NULL` |
