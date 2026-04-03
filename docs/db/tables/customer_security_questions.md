# Table: CUSTOMER_SECURITY_QUESTIONS

## Columns

| # | Column | Type | Length | Nullable | Default | Description |
|---|--------|------|--------|----------|---------|-------------|
| 1 | ID | NUMBER | 22 | NOT NULL | | Primary key |
| 2 | CUSTOMER_ID | VARCHAR2 | 50 | NULL | | FK → CUSTOMER |
| 3 | SECURITY_QUESTIONS_ID | VARCHAR2 | 50 | NULL | | FK → SECURITY_QUESTIONS.ID |
| 4 | ANSWER | VARCHAR2 | 250 | NULL | | |
| 5 | STATUS | VARCHAR2 | 20 | NULL | | |
| 6 | CREATED_AT | TIMESTAMP(6) | | NULL | `CURRENT_TIMESTAMP` | |
| 7 | UPDATED_AT | TIMESTAMP(6) | | NULL | | |
| 8 | DELETE_AT | TIMESTAMP(6) | | NULL | | |
| 9 | TYPE | VARCHAR2 | 25 | NULL | | |

## Constraints

| Name | Type | Detail |
|------|------|--------|
| SYS_C008640 | PRIMARY KEY | `ID` |
| FK_CUSTOMER_SECURITY_QUESTIONS_CUS_ID | FOREIGN KEY | `CUSTOMER_ID` → CUSTOMER |
| FK_CUSTOMER_SECURITY_QUESTIONS_SECURITY_QUESTIONS_ID | FOREIGN KEY | `SECURITY_QUESTIONS_ID` → SECURITY_QUESTIONS(`ID`) |