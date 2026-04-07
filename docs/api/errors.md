# Global Error Response Format

All error responses across every controller and exception handler follow this structure:

```json
{
    "status": "error",
    "error": {
        "code": "<ERROR_CODE>"
    },
    "message": "<human-readable description>"
}
```

## Fields

| Field | Type | Description |
|-------|------|-------------|
| `status` | `string` | Always `"error"` for error responses |
| `error.code` | `string` | Machine-readable error code (e.g. `ER_INVALID_TOKEN`) |
| `message` | `string` | Human-readable description of the error |

---

## Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `ER_INVALID_TOKEN` | `401` | Missing or invalid JWT token |
| `ER_TOKEN_EXPIRED` | `401` | JWT token has expired |
| `ER_MSMART_ERROR`  | `200` | m-smart rejected the request (business error) |

---

## Example

```json
{
    "status": "error",
    "error": {
        "code": "ER_INVALID_TOKEN"
    },
    "message": "missing or invalid token"
}
```