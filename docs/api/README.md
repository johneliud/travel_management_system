# API Conventions

## Base URL

```
/api/{resource}
```

All API endpoints follow the `/api/{resource}` path convention.

## Standard Error Response

All error responses use a consistent JSON shape:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "timestamp": "2026-01-15T10:30:00Z",
  "path": "/api/resource",
  "correlationId": "optional-correlation-id",
  "errors": [
    {
      "field": "fieldName",
      "message": "Field-level error message"
    }
  ]
}
```

### Fields

| Field           | Type     | Required | Description                                    |
|-----------------|----------|----------|------------------------------------------------|
| `code`          | string   | yes      | Machine-readable error code (e.g., `NOT_FOUND`)|
| `message`       | string   | yes      | Human-readable error message                   |
| `timestamp`     | datetime | yes      | ISO-8601 timestamp of when the error occurred  |
| `path`          | string   | yes      | The request path that caused the error         |
| `correlationId` | string   | no       | Reserved for distributed tracing (future use)  |
| `errors`        | array    | no       | Field-level validation errors (400 only)       |

## HTTP Status Codes

| Code | Meaning           | When Used                                      |
|------|-------------------|------------------------------------------------|
| 400  | Bad Request       | Validation errors, business rule violations     |
| 404  | Not Found         | Resource does not exist                         |
| 409  | Conflict          | Resource already exists, state conflict         |
| 500  | Internal Error    | Unhandled server error (generic message)        |

## Error Codes

| Code                  | HTTP | Description                           |
|-----------------------|------|---------------------------------------|
| `VALIDATION_ERROR`    | 400  | Request validation failed             |
| `BUSINESS_ERROR`      | 400  | Business rule violation               |
| `NOT_FOUND`           | 404  | Resource not found                    |
| `CONFLICT`            | 409  | Resource conflict                     |
| `INTERNAL_ERROR`      | 500  | Unexpected server error               |

## Security Notes

- Error responses never include stack traces
- Error responses never include secrets or internal identifiers
- 500 errors return a generic message; full details are logged server-side only
- Validation errors include field names but never field values (to avoid leaking sensitive data)

## Example Errors

### 400 - Validation Error

```json
{
  "code": "VALIDATION_ERROR",
  "message": "email: must not be blank, name: must be at least 2 characters",
  "timestamp": "2026-01-15T10:30:00Z",
  "path": "/api/users",
  "errors": [
    { "field": "email", "message": "must not be blank" },
    { "field": "name", "message": "must be at least 2 characters" }
  ]
}
```

### 404 - Not Found

```json
{
  "code": "USER_NOT_FOUND",
  "message": "User not found with id: 123",
  "timestamp": "2026-01-15T10:30:00Z",
  "path": "/api/users/123"
}
```

### 500 - Internal Error

```json
{
  "code": "INTERNAL_ERROR",
  "message": "An unexpected error occurred",
  "timestamp": "2026-01-15T10:30:00Z",
  "path": "/api/users"
}
```
