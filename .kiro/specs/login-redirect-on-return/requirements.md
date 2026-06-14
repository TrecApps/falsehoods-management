# Requirements Document

## Introduction

This feature adds a "redirect back" capability to the login flow of the Falsehoods Management application. Currently, after a successful login, users are always redirected to `/Welcome`. The desired behavior is context-aware: when a user clicks "Log In" from the topbar while browsing a page, a successful login should return them to that originating page. When a user navigates directly to `/Login` with no originating context, the fallback redirect to `/Welcome` is preserved.

The change touches three layers:
1. **Topbar** (`topbar.html`) — the Login anchor must append the originating page path as a `target` query parameter.
2. **LoginRouter** — the server-side handler must pass the `target` query parameter through to the template model.
3. **Login.html** — the `transferToPage()` JavaScript function must read the server-provided target value and redirect accordingly.

## Glossary

- **Topbar**: The `topbar.html` Thymeleaf fragment rendered at the top of every authenticated/unauthenticated page, containing navigation links and the Login button.
- **LoginRouter**: The Spring WebFlux `@Component` (`LoginRouter.java`) that handles `GET /Login` and renders the `Login.html` template.
- **Login_Page**: The `Login.html` Thymeleaf template that presents the authentication UI and performs the post-login redirect via `transferToPage()`.
- **Target_Param**: The URL query parameter named `target` that carries the path of the originating page (e.g., `?target=/FalsehoodSearch`).
- **transferToPage**: The JavaScript function in `Login.html` responsible for redirecting the browser after a successful login.
- **gatewayPath**: A server-side variable injected into `Login.html` representing the base URL of the application gateway.
- **Allowlist**: A predefined set of application-internal path prefixes that are considered safe redirect destinations.

---

## Requirements

### Requirement 1: Topbar Login Link Encodes the Originating Page

**User Story:** As a user browsing the application, I want the Login button to remember the page I was on, so that after I log in I am returned to that page without having to navigate back manually.

#### Acceptance Criteria

1. WHEN the Topbar renders the Login anchor and the current request path matches one of the known application routes (`/Welcome`, `/Article/{id}`, `/ArticleEdit`, `/ArticleEdit/{id}`, `/FalsehoodSearch`, `/Falsehood/{id}`, `/FalsehoodSubmit`, `/FalsehoodByBrand/{id}`), THE Topbar SHALL append a `target` query parameter to the `/Login` href containing the URL-encoded path of the current page (e.g., `href="Login?target=%2FFalsehoodSearch"`), and SHALL NOT include `/Login` itself as a valid target.
2. WHEN the Topbar renders the Login anchor and the current request path is absent, empty, equals `/Login`, or does not match any of the known application routes, THE Topbar SHALL render the Login anchor with `href="Login"` and no `target` parameter.
3. IF the Topbar fails to include a `target` parameter for any reason, THEN THE Login_Page SHALL treat the missing parameter as equivalent to an empty string and redirect to `/Welcome` after a successful login.

---

### Requirement 2: LoginRouter Passes the Target Parameter to the Template

**User Story:** As a developer, I want the server to pass the `target` query parameter from the incoming request through to the rendered template, so that the client-side redirect logic can access it.

#### Acceptance Criteria

1. WHEN a `GET /Login` request is received with a `target` query parameter, THE LoginRouter SHALL add the URL-decoded value of the `target` parameter (capped at 2048 characters) to the template model under the key `"redirectTarget"`.
2. WHEN a `GET /Login` request is received without a `target` query parameter, THE LoginRouter SHALL add an empty string to the template model under the key `"redirectTarget"`.
3. THE LoginRouter SHALL pass the `"redirectTarget"` value to the `Login.html` template on every invocation of `loginPage`.

---

### Requirement 3: Login Page Redirects to the Target After Successful Login

**User Story:** As a user who arrived at the Login page from another page, I want to be redirected back to that page after I log in successfully, so that my workflow is not interrupted.

#### Acceptance Criteria

1. WHEN `transferToPage` is called and the server-provided `redirectTarget` is a non-empty string that begins with `/` and contains no `://`, THE Login_Page SHALL set `document.location.href` to `gatewayPath` concatenated with `redirectTarget`.
2. WHEN `transferToPage` is called and the server-provided `redirectTarget` is an empty string, THE Login_Page SHALL set `document.location.href` to `gatewayPath` concatenated with `/Welcome`.
3. WHEN `transferToPage` is called and the server-provided `redirectTarget` does not begin with `/` or contains `://`, THE Login_Page SHALL set `document.location.href` to `gatewayPath` concatenated with `/Welcome`.
4. WHEN `transferToPage` is called and `redirectTarget` is null (absent from the server-provided model), THE Login_Page SHALL treat it as an empty string and redirect to `gatewayPath` concatenated with `/Welcome`.

---

### Requirement 4: Allowed Redirect Destinations Are Restricted to Internal Routes

**User Story:** As a security-conscious developer, I want the redirect target to be validated against a known set of safe paths, so that the login flow cannot be used as an open redirector to external or malicious URLs.

#### Acceptance Criteria

1. THE Login_Page SHALL define an Allowlist of valid internal path prefixes: `/Welcome`, `/FalsehoodSearch`, `/Falsehood/`, `/Article/`, `/ArticleEdit`, `/FalsehoodSubmit`, `/FalsehoodByBrand/`.
2. WHEN `transferToPage` is called and the `redirectTarget` does not start with any entry in the Allowlist, THE Login_Page SHALL redirect to `/Welcome` regardless of the value of `redirectTarget`.
3. IF `redirectTarget` contains a protocol scheme (e.g., `http://` or `https://`), THEN THE Login_Page SHALL redirect to `/Welcome` and SHALL NOT follow the provided value.
