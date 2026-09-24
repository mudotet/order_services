# Email login and SHIPPER

- Provide anonymous `POST /api/auth/login` accepting only email and password as credentials, using Lombok DTOs.
- Authenticate against active users and stored BCrypt hashes using Spring Security.
- Accept active database-assigned USER, ADMIN and SHIPPER roles, returning all supported roles without accepting client-provided permissions.
- Return userId, userName, email and roles without exposing credentials.
- Use native HTTP sessions by default, persisting authentication and changing the session ID on login.
- Disable CSRF for local learning at the user's request; remove token bootstrapping and document that this configuration must not be used in production.
- Controller and service login methods accept only LoginRequest; the service receives request-specific servlet objects through Spring injection.
- Keep existing USER/ADMIN permissions unchanged; SHIPPER login does not automatically grant access to those APIs.
- Use email as Spring's username and resolve authenticated users by their unique email; retain userId only for database relationships and response data.
- HTTP Basic and JSON login both authenticate by email only; do not fall back to display names.
- Reject invalid credentials, deleted accounts and accounts with no supported active role with a generic 401.
- Do not add JWT dependencies, registration, password reset or shipper assignment workflows.

## Verification

Authentication and security tests cover all three roles, multiple roles, duplicate display names, session persistence and rotation, token-free login and writes, rejected credentials, deleted accounts and roles, and unchanged USER/ADMIN authorization.
