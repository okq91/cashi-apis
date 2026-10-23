# cashi-api

Demo payment API: Kotlin + Spring Boot 3.5, Firebase Firestore storage, JWT access and refresh tokens.

## Run locally

1. Download a Firebase service account key (Firebase console > Project settings > Service accounts > Generate new private key)
   and save it **outside the repo** at `~/.config/cashi-api/firebase-service-account.json`
   (or point `FIREBASE_CREDENTIALS_PATH` at it). If the file is missing, Application Default Credentials are used
   (`GOOGLE_APPLICATION_CREDENTIALS` or `gcloud auth application-default login`).
2. Set a JWT signing key (base64, at least 32 bytes):

   ```bash
   export JWT_SECRET=$(openssl rand -base64 32)
   ./gradlew bootRun
   ```

| Env var | Default | Purpose |
| --- | --- | --- |
| `JWT_SECRET` | required | Base64 HMAC key for access tokens |
| `JWT_EXPIRATION` | `900000` (15 min) | Access token lifetime, ms |
| `JWT_REFRESH_EXPIRATION` | `604800000` (7 days) | Refresh token lifetime, ms |
| `FIREBASE_CREDENTIALS_PATH` | `~/.config/cashi-api/firebase-service-account.json` | Service account key |
| `FIREBASE_PROJECT_ID` | from the key | Firebase project |
| `PORT` | `8080` | HTTP port |

## Endpoints

| Method | Path | Auth | Body |
| --- | --- | --- | --- |
| POST | `/api/auth/register` | no | `{ "email", "password" (min 8), "fullName" }` |
| POST | `/api/auth/login` | no | `{ "email", "password" }` |
| POST | `/api/auth/refresh` | no | `{ "refreshToken" }` returns a new access **and** refresh token (old one is revoked) |
| POST | `/api/payments` | Bearer | `{ "amount": 25.50, "currency": "USD", "recipientEmail": "ali@example.com", "description": "optional" }` |
| GET | `/api/payments?cursor=` | Bearer | payment history for the caller, newest first, 10 per page. Returns `{ items, nextCursor }`; pass `nextCursor` as `cursor` for the next page (null on the last page) |

```bash
TOKEN=$(curl -s localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"email":"omar@example.com","password":"password123","fullName":"Omar"}' | jq -r .accessToken)

curl -s localhost:8080/api/payments -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
  -d '{"amount":25.50,"currency":"USD","recipientEmail":"ali@example.com"}'

curl -s localhost:8080/api/payments -H "Authorization: Bearer $TOKEN"
```

## Firestore layout

- `users/{userId}`: email, fullName, passwordHash (BCrypt), createdAt
- `user_emails/{email}`: userId (keeps emails unique, written in the same transaction as the user)
- `users/{userId}/payments/{paymentId}`: amount (decimal string), currency, recipientEmail, description, createdAt
- `refresh_tokens/{sha256(token)}`: userId, expiryDate (raw refresh tokens are never stored)

## Tests

`./gradlew test` runs the unit tests (MockK, no Spring context), so no Firebase project is needed.
