# Design Document: Login Redirect on Return

## Overview

This feature adds a context-aware post-login redirect to the Falsehoods Management application. Today, `transferToPage()` in `Login.html` always navigates to `/Welcome` after a successful login. The goal is to return the user to the page they were browsing when they clicked "Log in" — without compromising security against open-redirect attacks.

The change is confined to three files and follows a simple request-scoped data flow:

```
[topbar.html]  -->  href="Login?target=/FalsehoodSearch"
    |
    v
[LoginRouter.java]  -->  model["redirectTarget"] = "/FalsehoodSearch"
    |
    v
[Login.html]  -->  transferToPage() validates allowlist, then navigates
```

No new dependencies, no new endpoints, no database changes. The feature is purely additive: every existing behavior is preserved as the fallback when `target` is absent.

---

## Architecture

The application is a **Spring WebFlux** service using **Thymeleaf** for server-side rendering. Pages are served via `RouterFunction` beans wired in `RouteConfig`. There is no MVC dispatcher — routers are plain `@Component` classes whose handler methods return `Mono<ServerResponse>`.

Relevant routing:

| Route | Handler |
|---|---|
| `GET /Login` | `LoginRouter::loginPage` |
| `GET /Welcome` | `BrandsRouter::welcomePage` |
| `GET /FalsehoodSearch` | `FalsehoodsRouter::falsehoodSearchPage` |
| … (others) | various routers |

The topbar is a Thymeleaf fragment (`th:fragment="bar"`) included in every page. It is rendered server-side, so the current request URI is available via Thymeleaf's `#request` utility object (specifically `#request.requestURI` in Spring WebFlux's Thymeleaf integration).

Security model: all redirect validation happens client-side in `transferToPage()`. The server performs no redirect — it only passes a string to the template. The client validates that string against a hardcoded allowlist before using it.

---

## Components and Interfaces

### 1. `topbar.html` — Login anchor (`th:href`)

**Current state:**
```html
<a th:if="${list == null}" href="Login">
    <button class="btn btn-primary desktop-login" style="height: 100%">Log in</button>
</a>
```

**Required change:**  
Replace the static `href="Login"` with a conditional `th:href` that appends `?target=<encoded-path>` when the current path is a recognized application route.

The Thymeleaf expression must:
1. Retrieve the current request URI via `${#request.requestURI}` (available in Spring WebFlux Thymeleaf context as the servlet-equivalent request object).
2. Check whether it matches a known route.
3. Emit `@{/Login(target=${#request.requestURI})}` when it matches; emit `@{/Login}` otherwise.

Because the match list is long and Thymeleaf lacks a native `contains(list)` expression, the cleanest approach is a single compound boolean expression using `or`:

```html
<a th:if="${list == null}"
   th:with="currentPath=${#request.requestURI},
            isKnownRoute=(${currentPath} == '/Welcome'
                       or ${currentPath} == '/FalsehoodSearch'
                       or ${currentPath} == '/ArticleEdit'
                       or ${currentPath} == '/FalsehoodSubmit'
                       or #strings.startsWith(${currentPath}, '/Article/')
                       or #strings.startsWith(${currentPath}, '/Falsehood/')
                       or #strings.startsWith(${currentPath}, '/FalsehoodByBrand/')
                       or #strings.startsWith(${currentPath}, '/ArticleEdit/'))"
   th:href="${isKnownRoute} ? @{/Login(target=${currentPath})} : @{/Login}">
    <button class="btn btn-primary desktop-login" style="height: 100%">Log in</button>
</a>
```

**Design notes:**
- `/Login` is intentionally excluded from the `isKnownRoute` check — a user on `/Login` clicking "Log in" again should not create a self-referential redirect loop.
- `#strings.startsWith` handles the parameterized routes (`/Article/{id}`, `/Falsehood/{id}`, `/ArticleEdit/{id}`, `/FalsehoodByBrand/{id}`) without needing regex.
- Thymeleaf's `@{/Login(target=${currentPath})}` URL-encodes the value automatically.
- In Spring WebFlux, `#request.requestURI` returns the path without query string (e.g., `/FalsehoodSearch`), which is exactly what we want.

---

### 2. `LoginRouter.java` — `loginPage` handler

**Current model keys:** `userServiceUrl`, `gatewayPath`, `baseUrl`

**Required change:** Read the `target` query parameter, decode and cap it, then add it to the model as `"redirectTarget"`.

```java
public Mono<ServerResponse> loginPage(ServerRequest request) {
    Map<String, Object> dataMap = new HashMap<>();

    dataMap.put("userServiceUrl", loginUrl);
    dataMap.put("gatewayPath", falsehoodsUrl);
    dataMap.put("baseUrl", falsehoodsPath);

    String rawTarget = request.queryParam("target")
            .map(t -> URLDecoder.decode(t, StandardCharsets.UTF_8))
            .orElse("");

    // Cap at 2048 characters to prevent oversized inputs
    if (rawTarget.length() > 2048) {
        rawTarget = rawTarget.substring(0, 2048);
    }

    dataMap.put("redirectTarget", rawTarget);

    return ServerResponse.ok().render("Login", dataMap);
}
```

**Design notes:**
- `request.queryParam("target")` returns `Optional<String>`. `.orElse("")` ensures `"redirectTarget"` is always a non-null string in the model, simplifying template and client code.
- `URLDecoder.decode` with `StandardCharsets.UTF_8` is used (not the deprecated `String` charset variant). The topbar emits a Thymeleaf `@{...}` URL which percent-encodes the value; this decode step recovers the original path.
- The 2048-character cap prevents oversized values from being injected into the page. Legitimate paths are far shorter.
- No imports beyond `java.net.URLDecoder` and `java.nio.charset.StandardCharsets` are needed.

---

### 3. `Login.html` — `transferToPage()` function

**Current state:** Reads `target` from URL query params and has an empty `switch` statement. This approach is incorrect — the authoritative value must come from the server-injected `[[${redirectTarget}]]`, not from the browser URL (which could be manipulated client-side independently of the server-validated value).

**Required change:** Replace the entire `transferToPage` body with allowlist-based logic reading the server-injected value:

```javascript
function transferToPage() {
    const ALLOWLIST = [
        '/Welcome',
        '/FalsehoodSearch',
        '/Falsehood/',
        '/Article/',
        '/ArticleEdit',
        '/FalsehoodSubmit',
        '/FalsehoodByBrand/'
    ];

    let gatewayPath = "[[${gatewayPath}]]";
    let redirectTarget = "[[${redirectTarget}]]";

    // Reject protocol-relative and absolute URLs
    if (!redirectTarget || redirectTarget.indexOf('://') !== -1 || !redirectTarget.startsWith('/')) {
        document.location.href = `${gatewayPath}/Welcome`;
        return;
    }

    // Validate against the internal-routes allowlist
    const isAllowed = ALLOWLIST.some(prefix => redirectTarget.startsWith(prefix));

    if (!isAllowed) {
        document.location.href = `${gatewayPath}/Welcome`;
        return;
    }

    document.location.href = `${gatewayPath}${redirectTarget}`;
}
```

**Design notes:**
- `[[${redirectTarget}]]` is Thymeleaf inline text — the server writes the value directly into the JavaScript string literal at render time. This is intentional and idiomatic for this codebase (the existing code already uses `[[${gatewayPath}]]` and `[[${userServiceUrl}]]` this way).
- Reading from the server-injected value (not `URLSearchParams`) ensures the client cannot be tricked by a manipulated query string that differs from what the server validated and recorded.
- The `indexOf('://')` check handles both `http://` and `https://` schemes (and any custom scheme).
- The `startsWith('/')` check rejects relative paths, bare hostnames, and protocol-relative URLs (`//evil.com`).
- The allowlist check uses `Array.prototype.some` with `startsWith` to handle both exact paths (`/Welcome`) and prefix-matched paths (`/Falsehood/123`).
- If `redirectTarget` is the empty string (server default), the first guard (`!redirectTarget`) is truthy → falls through to `/Welcome`. Correct behavior.

---

## Data Models

No new data models. The only state crossing component boundaries is a plain string:

| Field | Type | Source | Destination | Constraints |
|---|---|---|---|---|
| `target` | `String` (query param) | Browser URL | `LoginRouter` | URL-encoded; may be absent |
| `redirectTarget` | `String` (model key) | `LoginRouter` | `Login.html` template | URL-decoded; max 2048 chars; never null (empty string when absent) |

The topbar does not receive or pass `redirectTarget` — it only produces the `target` query parameter as part of the Login href.

---

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

Before listing final properties, redundancy analysis:

- **1.1 and 1.2** cover complementary halves of the same decision (known route → target appended; unknown/absent → no target). They cannot be merged since they test different branches, but together form a complete partition. Both are kept.
- **2.1 and 2.2** are complementary (param present vs absent). 2.2 is a single-example test, not a property; it won't appear as a PBT property. 2.1 remains.
- **3.1 and 4.2** overlap: 3.1 says valid targets are redirected to, 4.2 says non-allowlisted targets go to /Welcome. Together they describe the full decision. They can be unified into one comprehensive property covering all inputs.
- **3.3 and 4.3** both cover the "contains ://" case. 4.3 is fully subsumed by 3.3. Removed 4.3 as standalone.
- **3.2 and 3.4** are single-example/edge-case tests, not PBT properties. Not listed as properties.

Final properties after reflection: **4 properties**.

---

### Property 1: Topbar appends target for known routes

*For any* request path that matches one of the known application routes (`/Welcome`, `/FalsehoodSearch`, `/ArticleEdit`, `/FalsehoodSubmit`, or any path starting with `/Article/`, `/Falsehood/`, `/ArticleEdit/`, `/FalsehoodByBrand/`), the rendered Login anchor href SHALL contain a `target` query parameter equal to the URL-encoded form of that path.

**Validates: Requirements 1.1**

---

### Property 2: Topbar omits target for unknown/excluded paths

*For any* request path that is empty, equals `/Login`, or does not match any known application route prefix, the rendered Login anchor href SHALL equal `Login` with no `target` query parameter appended.

**Validates: Requirements 1.2**

---

### Property 3: LoginRouter decodes and caps the target parameter

*For any* non-empty URL-encoded string provided as the `target` query parameter on `GET /Login`, the model key `"redirectTarget"` SHALL contain the URL-decoded form of that string, truncated to at most 2048 characters.

**Validates: Requirements 2.1**

---

### Property 4: transferToPage redirects correctly based on redirectTarget

*For any* value of `redirectTarget` injected by the server:
- If `redirectTarget` is non-empty, starts with `/`, contains no `://`, and starts with an allowlist prefix → `document.location.href` is set to `gatewayPath + redirectTarget`.
- Otherwise (empty, no leading `/`, contains `://`, or not on the allowlist) → `document.location.href` is set to `gatewayPath + "/Welcome"`.

**Validates: Requirements 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3**

---

## Error Handling

| Scenario | Handling |
|---|---|
| `target` param absent from `GET /Login` | `LoginRouter` sets `"redirectTarget"` to `""`. No error. |
| `target` param is a URL over 2048 chars | Truncated to 2048 chars server-side before injection into model. |
| `URLDecoder.decode` throws (malformed encoding) | Wrap in try/catch; on exception, fall back to `""` for `redirectTarget`. |
| `redirectTarget` is empty string in JS | `!redirectTarget` guard is truthy → redirect to `/Welcome`. |
| `redirectTarget` contains `://` | `indexOf('://')` guard fires → redirect to `/Welcome`. |
| `redirectTarget` does not start with `/` | `startsWith('/')` guard fires → redirect to `/Welcome`. |
| `redirectTarget` is valid but not on allowlist | `ALLOWLIST.some(...)` returns false → redirect to `/Welcome`. |
| `#request.requestURI` unavailable in Thymeleaf | `isKnownRoute` evaluates to false → href rendered as `Login` with no target. Graceful degradation. |

For the `URLDecoder` exception case, the recommended implementation detail in `LoginRouter`:

```java
String rawTarget;
try {
    rawTarget = request.queryParam("target")
            .map(t -> URLDecoder.decode(t, StandardCharsets.UTF_8))
            .orElse("");
} catch (IllegalArgumentException e) {
    rawTarget = "";
}
if (rawTarget.length() > 2048) {
    rawTarget = rawTarget.substring(0, 2048);
}
```

---

## Testing Strategy

This feature's core logic lives in three well-isolated places: a Thymeleaf expression, a Java handler method, and a JavaScript function. The testing approach reflects that.

### Property-Based Testing

Property-based testing (PBT) is applicable here because:
- `LoginRouter.loginPage` is a pure data-transformation step (query param → model entry) with a large, interesting input space (arbitrary strings, edge lengths, encoding variants).
- `transferToPage()` is pure decision logic on a string with a large input space (arbitrary path strings).

**Library:** [jqwik](https://jqwik.net/) for Java (Property 3), and [fast-check](https://fast-check.io/) for JavaScript (Property 4).

Each property test runs a **minimum of 100 iterations**.

**Tag format:** `// Feature: login-redirect-on-return, Property {N}: {property_text}`

#### Property 3 — Java (jqwik)

```java
// Feature: login-redirect-on-return, Property 3: LoginRouter decodes and caps the target parameter
@Property(tries = 100)
void redirectTargetIsDecodedAndCapped(@ForAll @StringLength(min = 1, max = 4096) String rawInput) {
    String encoded = URLEncoder.encode(rawInput, StandardCharsets.UTF_8);
    // construct mock ServerRequest with target = encoded
    // invoke loginPage
    // assert model["redirectTarget"].equals(rawInput.substring(0, Math.min(rawInput.length(), 2048)))
}
```

#### Property 4 — JavaScript (fast-check)

```javascript
// Feature: login-redirect-on-return, Property 4: transferToPage redirects correctly based on redirectTarget
fc.assert(fc.property(fc.string(), (redirectTarget) => {
    const ALLOWLIST = ['/Welcome', '/FalsehoodSearch', '/Falsehood/', '/Article/',
                       '/ArticleEdit', '/FalsehoodSubmit', '/FalsehoodByBrand/'];
    const result = computeRedirect(redirectTarget, 'https://gateway');
    const isValid = redirectTarget
        && !redirectTarget.includes('://')
        && redirectTarget.startsWith('/')
        && ALLOWLIST.some(p => redirectTarget.startsWith(p));
    const expected = isValid
        ? `https://gateway${redirectTarget}`
        : 'https://gateway/Welcome';
    return result === expected;
}), { numRuns: 100 });
```

### Unit / Example-Based Tests

| Test | What it verifies |
|---|---|
| `loginPage` with no `target` param | `"redirectTarget"` == `""` in model (Req 2.2) |
| `loginPage` with `target=""` | `"redirectTarget"` == `""` in model |
| `loginPage` with `target` exactly 2048 chars | value not truncated |
| `loginPage` with `target` 2049 chars | truncated to 2048 |
| `loginPage` with malformed percent-encoding | falls back to `""` |
| `transferToPage` with `redirectTarget = ""` | navigates to `gatewayPath + "/Welcome"` |
| `transferToPage` with `redirectTarget = null` | navigates to `gatewayPath + "/Welcome"` |
| `transferToPage` with `redirectTarget = "http://evil.com"` | navigates to `gatewayPath + "/Welcome"` |
| `transferToPage` with `redirectTarget = "/FalsehoodSearch"` | navigates to `gatewayPath + "/FalsehoodSearch"` |
| `transferToPage` with `redirectTarget = "/Falsehood/42"` | navigates to `gatewayPath + "/Falsehood/42"` |

### Topbar Template Tests

The topbar rendering is best covered by Spring WebFlux integration tests that render the fragment with a mock model and assert the `href` attribute value via HTML parsing (e.g., Jsoup):

| Test | Expected href |
|---|---|
| `requestURI = "/FalsehoodSearch"` | `Login?target=%2FFalsehoodSearch` |
| `requestURI = "/Article/99"` | `Login?target=%2FArticle%2F99` |
| `requestURI = "/Login"` | `Login` (no target) |
| `requestURI = "/SomethingElse"` | `Login` (no target) |
| `requestURI = ""` | `Login` (no target) |
| `requestURI = "/Welcome"` | `Login?target=%2FWelcome` |
