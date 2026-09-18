# ADR 0002: Authentication for the React SPA

**Status:** Accepted

## Decision

Users have normalized, unique email addresses and BCrypt password hashes. Registration creates an active user. Login errors use the same `401 INVALID_CREDENTIALS` response whether the email is unknown, the password is wrong, or the user is disabled.

The API issues random opaque tokens. It stores only their SHA-256 hashes in PostgreSQL. Access tokens expire after 15 minutes; refresh tokens expire after 7 days. The browser holds the access token only in JavaScript memory and sends it as a bearer token. It never uses localStorage or sessionStorage for tokens. The refresh token is sent in an HttpOnly, SameSite=Strict cookie scoped to `/api/v1/auth`. Secure is enabled by default; the local HTTP profile overrides it with `APP_AUTH_SECURE_COOKIE=false`. TLS is required in production.

On page load and after an expired access token, the SPA calls `POST /api/v1/auth/refresh` with credentials. Refresh rotates the refresh token and issues a new access token. Reusing an old refresh token fails. Logout deletes the current access and refresh token records and clears the cookie. Other active access tokens from separate sessions remain valid until expiration or their own logout.

The API is stateless at the HTTP session layer. Spring Security requires bearer authentication except for register, login, refresh, and actuator health. The frontend protects the assessment page and offers `/login` and `/register`. `GET /api/v1/auth/me` returns only public user fields.

## CORS and CSRF

Only configured SPA origins are allowed by CORS; credentials and the Authorization header are enabled. Refresh and logout require an Origin matching the configured allowed origins. The refresh cookie has SameSite=Strict. These controls guard cookie-based actions against cross-site requests. Register and login use request bodies without relying on existing cookies. Deploy frontend and API on the same site so SameSite=Strict permits refresh. If their origins differ across sites, revisit the cookie and CSRF design before deployment.

## Consequences

Token lookup adds a database read to authenticated requests. Hashed tokens can be revoked immediately, and no signing key or JWT library is required. Expired token rows should be removed by routine database maintenance as usage grows.
