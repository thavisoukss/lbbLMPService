# Table: SECURITY_QUESTIONS

## Columns

| # | Column | Type | Length | Nullable | Default | Description |
|---|--------|------|--------|----------|---------|-------------|
| 1 | ID | VARCHAR2 | 50 | NOT NULL | | Primary key |
| 2 | DESCRIPTION | VARCHAR2 | 1000 | NULL | | |
| 3 | STATUS | VARCHAR2 | 20 | NULL | | |
| 4 | CREATED_AT | TIMESTAMP(6) | | NULL | `CURRENT_TIMESTAMP` | |
| 5 | UPDATED_AT | TIMESTAMP(6) | | NULL | | |
| 6 | DELETE_AT | TIMESTAMP(6) | | NULL | | |
| 7 | LANGUAGE | VARCHAR2 | 5 | NULL | | |

## Constraints

| Name | Type | Detail |
|------|------|--------|
| SYS_C008639 | PRIMARY KEY | `ID` |
