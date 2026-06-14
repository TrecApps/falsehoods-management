# Implementation Plan: Login Redirect on Return

## Overview

This feature adds context-aware post-login redirects by threading a `target` query parameter from the topbar through the `LoginRouter` server handler into the `Login.html` client-side redirect logic. The change touches exactly three files: `topbar.html` (Thymeleaf fragment), `LoginRouter.java` (WebFlux handler), and `Login.html` (JavaScript function). All existing behavior is preserved as the fallback when `target` is absent.

## Tasks

- [x] 1. Add jqwik dependency for property-based testing
  - Add `net.jqwik:jqwik:1.8.5` to `testImplementation` in `build.gradle`
  - This is the Java PBT library required by the design's testing strategy
  - _Requirements: 2.1_

- [x] 2. Update `LoginRouter.java` to pass `redirectTarget` to the template model
  - [x] 2.1 Implement target parameter extraction in `loginPage`
    - Read the `target` query parameter from `ServerRequest` using `request.queryParam("target")`
    - URL-decode the value with `URLDecoder.decode(t, StandardCharsets.UTF_8)` inside a try/catch; fall back to `""` on `IllegalArgumentException`
    - Cap the decoded string at 2048 characters with `substring(0, 2048)` when length exceeds the cap
    - Add the result to `dataMap` under key `"redirectTarget"` before the `render` call; use `""` when the param is absent
    - Add imports: `java.net.URLDecoder`, `java.nio.charset.StandardCharsets`
    - _Requirements: 2.1, 2.2, 2.3_

  - [x] 2.2 Write property test for `loginPage` target decoding and capping (Property 3)
    - Create `src/test/java/com/trecapps/falsehoods/controllers/LoginRouterPropertyTest.java`
    - Use jqwik `@Property(tries = 100)` with `@ForAll @StringLength(min = 1, max = 4096) String rawInput`
    - URL-encode the input, build a mock `ServerRequest` with `target` set to the encoded value, invoke `loginPage`, and assert `model["redirectTarget"].equals(rawInput.substring(0, Math.min(rawInput.length(), 2048)))`
    - Tag: `// Feature: login-redirect-on-return, Property 3: LoginRouter decodes and caps the target parameter`
    - **Property 3: LoginRouter decodes and caps the target parameter**
    - **Validates: Requirements 2.1**

  - [x] 2.3 Write unit tests for `loginPage` edge cases
    - No `target` param → `"redirectTarget"` is `""`
    - `target` exactly 2048 chars → value not truncated
    - `target` 2049 chars → truncated to 2048
    - Malformed percent-encoding → falls back to `""`
    - Create `src/test/java/com/trecapps/falsehoods/controllers/LoginRouterTest.java`
    - _Requirements: 2.1, 2.2_

- [x] 3. Checkpoint — Ensure all Java tests pass
  - Ensure all tests pass, ask the user if questions arise.

- [x] 4. Update `Login.html` to implement allowlist-based redirect in `transferToPage()`
  - [x] 4.1 Replace the `transferToPage` function body with allowlist-validated redirect logic
    - Define `ALLOWLIST` array with the seven internal path prefixes: `/Welcome`, `/FalsehoodSearch`, `/Falsehood/`, `/Article/`, `/ArticleEdit`, `/FalsehoodSubmit`, `/FalsehoodByBrand/`
    - Read `redirectTarget` from the Thymeleaf inline expression `"[[${redirectTarget}]]"` (not from `URLSearchParams`)
    - Guard 1: if `!redirectTarget` → redirect to `gatewayPath + "/Welcome"` and return
    - Guard 2: if `redirectTarget.indexOf('://') !== -1` → redirect to `gatewayPath + "/Welcome"` and return
    - Guard 3: if `!redirectTarget.startsWith('/')` → redirect to `gatewayPath + "/Welcome"` and return
    - Allowlist check: `ALLOWLIST.some(prefix => redirectTarget.startsWith(prefix))` → if false, redirect to `/Welcome`
    - On pass: `document.location.href = gatewayPath + redirectTarget`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3_

  - [x] 4.2 Write property test for `transferToPage` redirect logic (Property 4)
    - Add `fast-check` as a dev dependency if a JS test harness is available, or document the test as a standalone script under `src/test/js/`
    - Extract the redirect decision into a pure helper function `computeRedirect(redirectTarget, gatewayPath)` in a separate module so it can be unit-tested without a DOM
    - Use `fc.assert(fc.property(fc.string(), ...))` with `numRuns: 100` to verify: valid targets (non-empty, starts with `/`, no `://`, on allowlist) → `gatewayPath + redirectTarget`; all others → `gatewayPath + "/Welcome"`
    - Tag: `// Feature: login-redirect-on-return, Property 4: transferToPage redirects correctly based on redirectTarget`
    - **Property 4: transferToPage redirects correctly based on redirectTarget**
    - **Validates: Requirements 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3**

  - [x] 4.3 Write unit tests for `transferToPage` example cases
    - `redirectTarget = ""` → navigates to `gatewayPath + "/Welcome"`
    - `redirectTarget = "http://evil.com"` → navigates to `gatewayPath + "/Welcome"`
    - `redirectTarget = "//evil.com"` → navigates to `gatewayPath + "/Welcome"`
    - `redirectTarget = "/FalsehoodSearch"` → navigates to `gatewayPath + "/FalsehoodSearch"`
    - `redirectTarget = "/Falsehood/42"` → navigates to `gatewayPath + "/Falsehood/42"`
    - `redirectTarget = "/unknownRoute"` → navigates to `gatewayPath + "/Welcome"`
    - _Requirements: 3.1, 3.2, 3.3, 3.4, 4.1, 4.2, 4.3_

- [x] 5. Update `topbar.html` to append `target` query parameter on the Login anchor
  - [x] 5.1 Replace the static `href="Login"` anchor with a conditional `th:href`
    - Wrap the existing `<a th:if="${list == null}" href="Login">` anchor with a `th:with` block to capture `currentPath=${#request.requestURI}` and `isKnownRoute`
    - Set `isKnownRoute` using a compound boolean expression covering: `currentPath == '/Welcome'`, `currentPath == '/FalsehoodSearch'`, `currentPath == '/ArticleEdit'`, `currentPath == '/FalsehoodSubmit'`, and `#strings.startsWith(currentPath, '/Article/')`, `#strings.startsWith(currentPath, '/Falsehood/')`, `#strings.startsWith(currentPath, '/ArticleEdit/')`, `#strings.startsWith(currentPath, '/FalsehoodByBrand/')`
    - Set `th:href` to `${isKnownRoute} ? @{/Login(target=${currentPath})} : @{/Login}`
    - Do not include `/Login` in the `isKnownRoute` check (prevents self-referential redirect loop)
    - _Requirements: 1.1, 1.2, 1.3_

  - [x] 5.2 Write property test for topbar Login href (Property 1 and Property 2)
    - Create a Spring WebFlux integration test that renders the `topbar` fragment with mock request URIs
    - For Property 1: for each known route path (parameterized), assert `href` contains `target=<URL-encoded-path>`
    - For Property 2: for `/Login`, empty string, and unrecognized paths, assert `href` equals `Login` with no `target` param
    - Use Jsoup or similar HTML parser to extract the anchor's `href` attribute
    - Tag: `// Feature: login-redirect-on-return, Property 1: Topbar appends target for known routes`
    - Tag: `// Feature: login-redirect-on-return, Property 2: Topbar omits target for unknown/excluded paths`
    - **Property 1: Topbar appends target for known routes**
    - **Property 2: Topbar omits target for unknown/excluded paths**
    - **Validates: Requirements 1.1, 1.2**

  - [ ]* 5.3 Write integration tests for topbar Login href example cases
    - `requestURI = "/FalsehoodSearch"` → `href` contains `target=%2FFalsehoodSearch`
    - `requestURI = "/Article/99"` → `href` contains `target=%2FArticle%2F99`
    - `requestURI = "/Login"` → `href` equals `Login` (no target)
    - `requestURI = "/SomethingElse"` → `href` equals `Login` (no target)
    - `requestURI = ""` → `href` equals `Login` (no target)
    - `requestURI = "/Welcome"` → `href` contains `target=%2FWelcome`
    - _Requirements: 1.1, 1.2_

- [x] 6. Final checkpoint — Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties (Properties 1–4 from the design document)
- Unit tests validate specific examples and edge cases
- The JavaScript property test (4.2) requires extracting `computeRedirect` into a pure function before testing — this is captured in task 4.2 itself
- No new routes, endpoints, or data models are introduced; this is a purely additive change

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "2.1"] },
    { "id": 1, "tasks": ["2.2", "2.3", "4.1"] },
    { "id": 2, "tasks": ["4.2", "4.3", "5.1"] },
    { "id": 3, "tasks": ["5.2", "5.3"] }
  ]
}
```
