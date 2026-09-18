# Login

`POST /api/auth/login` authenticates with email and password and returns a session, not a JWT.
All request and response DTOs use Lombok.

Send the JSON below directly with `Content-Type: application/json`.
Keep the returned `JSESSIONID` cookie for subsequent requests.
The controller and service each accept only `LoginRequest`; Spring supplies the request-specific servlet objects to the service for session handling.
No CSRF token endpoint, token cookie or token header is required.

CSRF protection is disabled at the user's request for local learning.
Do not deploy cookie-based authentication with this configuration; restore CSRF protection before production.

```json
{
  "email": "shipper@example.com",
  "password": "your-password"
}
```

Successful login returns this `data` inside `BaseResponse`:

```json
{
  "userId": "user-uuid",
  "userName": "Delivery staff",
  "email": "shipper@example.com",
  "roles": ["SHIPPER"]
}
```

Only active USER, ADMIN and SHIPPER roles assigned in `user_roles` are accepted.
The client does not choose its role; role fields in a request cannot grant permissions.
Accounts may have multiple supported roles.
Incorrect credentials, deleted accounts and accounts with no active supported role return 401 without revealing which check failed.
Invalid input returns 400; authenticated accounts without the required role receive 403 on protected APIs.
Passwords must match the existing BCrypt `password_hash`, with a maximum of 72 UTF-8 bytes.
No password or hash is returned.

SHIPPER can log in but does not inherit USER or ADMIN access.
Existing order-state updates remain ADMIN-only until shipper-specific permissions are defined.
HTTP Basic access also requires email in its Username field; display names are not accepted for authentication.
Spring calls its login identifier `username`, but this application stores the account's unique email there.
The required `UserDetailsService.loadUserByUsername(String email)` method receives the email extracted from `LoginRequest`; it does not take the whole request DTO.
`Authentication.getName()` returns that email, and services find the current account by email.
`userId` remains the database key used for order ownership and relationships, not the login identifier.
Duplicate display names do not mix accounts because emails are unique.
Invalidate existing sessions and log in again if an account's email changes.

## Database and deployment

Create the SHIPPER row in `roles` if it does not already exist, then assign its ID to the intended users through `user_roles`.
No account receives SHIPPER automatically.
Existing accounts need BCrypt hashes, not plaintext passwords.
Use HTTPS in production and keep browser requests same-origin, or explicitly configure trusted origins and credentialed requests when adding a separate frontend.
Roles in a session reflect login time; invalidate active sessions when revoking permissions.
Spring's existing `POST /logout` ends the session.

The implementation uses Spring Security's [explicit session persistence](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html).
