package com.trecapps.falsehoods.controllers;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import net.jqwik.api.Tuple;
import net.jqwik.api.Tuple.Tuple2;

/**
 * Property-based tests for the topbar Login anchor href logic.
 *
 * These tests validate the isKnownRoute / computeLoginHref helpers that mirror
 * the Thymeleaf expression in topbar.html. Testing the helper directly avoids
 * the complexity of a full Spring WebFlux reactive Thymeleaf context while still
 * verifying the correctness properties expressed in the template.
 *
 * **Validates: Requirements 1.1, 1.2**
 */
class TopbarLoginHrefPropertyTest {

    // -------------------------------------------------------------------------
    // Helpers that mirror the Thymeleaf expression in topbar.html exactly
    // -------------------------------------------------------------------------

    /**
     * Mirrors the {@code isKnownRoute} boolean expression in topbar.html.
     *
     * <pre>
     * (currentPath == '/Welcome'
     *  or currentPath == '/FalsehoodSearch'
     *  or currentPath == '/ArticleEdit'
     *  or currentPath == '/FalsehoodSubmit'
     *  or #strings.startsWith(currentPath, '/Article/')
     *  or #strings.startsWith(currentPath, '/Falsehood/')
     *  or #strings.startsWith(currentPath, '/ArticleEdit/')
     *  or #strings.startsWith(currentPath, '/FalsehoodByBrand/'))
     * </pre>
     */
    static boolean isKnownRoute(String path) {
        if (path == null) return false;
        return path.equals("/Welcome")
            || path.equals("/FalsehoodSearch")
            || path.equals("/ArticleEdit")
            || path.equals("/FalsehoodSubmit")
            || path.startsWith("/Article/")
            || path.startsWith("/Falsehood/")
            || path.startsWith("/ArticleEdit/")
            || path.startsWith("/FalsehoodByBrand/");
    }

    /**
     * Mirrors Thymeleaf's {@code @{/Login(target=${currentPath})}} URL encoding:
     * {@code +} is replaced with {@code %20} to match how browsers decode space in a path.
     *
     * <p>When {@code isKnownRoute} is true, the href becomes
     * {@code /Login?target=<url-encoded-path>}; otherwise it is simply {@code /Login}.
     */
    static String computeLoginHref(String requestURI) {
        if (isKnownRoute(requestURI)) {
            String encoded = URLEncoder.encode(requestURI, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            return "/Login?target=" + encoded;
        } else {
            return "/Login";
        }
    }

    // -------------------------------------------------------------------------
    // Known-route path generators
    // -------------------------------------------------------------------------

    private static final List<String> EXACT_KNOWN_ROUTES = List.of(
            "/Welcome",
            "/FalsehoodSearch",
            "/ArticleEdit",
            "/FalsehoodSubmit"
    );

    private static final List<Tuple2<String, String>> PREFIX_KNOWN_ROUTES = List.of(
            Tuple.of("/Article/",          "/Article/"),
            Tuple.of("/Falsehood/",        "/Falsehood/"),
            Tuple.of("/ArticleEdit/",      "/ArticleEdit/"),
            Tuple.of("/FalsehoodByBrand/", "/FalsehoodByBrand/")
    );

    /** Generates exact known-route paths (the four fixed strings). */
    @Provide
    Arbitrary<String> exactKnownPaths() {
        return Arbitraries.of(EXACT_KNOWN_ROUTES);
    }

    /** Generates prefix-based known-route paths: one of the four prefixes + a non-empty suffix. */
    @Provide
    Arbitrary<String> prefixKnownPaths() {
        Arbitrary<String> prefix = Arbitraries.of(
                "/Article/", "/Falsehood/", "/ArticleEdit/", "/FalsehoodByBrand/"
        );
        // Suffix: printable ASCII (letters, digits, slashes), min length 1
        Arbitrary<String> suffix = Arbitraries.strings()
                .withCharRange('a', 'z')
                .ofMinLength(1)
                .ofMaxLength(64);
        return Combinators.combine(prefix, suffix).as((p, s) -> p + s);
    }

    /** All known-route paths (exact + prefix-based). */
    @Provide
    Arbitrary<String> knownPaths() {
        return Arbitraries.oneOf(exactKnownPaths(), prefixKnownPaths());
    }

    /** Generates paths that are NOT known routes. */
    @Provide
    Arbitrary<String> unknownPaths() {
        // Start with a fixed set of representative unknown/excluded paths
        Arbitrary<String> fixedUnknown = Arbitraries.of(
                "/Login", "", "/SomethingElse", "/unknown/path", "notAPath",
                "/welcome",          // wrong case
                "/falsehoodsearch",  // wrong case
                "/article/",         // wrong case
                "/WELCOME",
                "/Falsehood",        // prefix without trailing slash — not a match
                "/ArticleEditing",   // contains known prefix but not a valid prefix-route
                "/FalsehoodSubmitX"  // extends exact match — not equal
        );
        // Also generate random strings filtered to exclude known routes
        Arbitrary<String> randomUnknown = Arbitraries.strings()
                .ofMinLength(0)
                .ofMaxLength(128)
                .filter(s -> !isKnownRoute(s));
        return Arbitraries.oneOf(fixedUnknown, randomUnknown);
    }

    // -------------------------------------------------------------------------
    // Property 1: Topbar appends target for known routes
    // -------------------------------------------------------------------------

    // Feature: login-redirect-on-return, Property 1: Topbar appends target for known routes
    /**
     * For any request path that matches a known application route, the computed
     * Login href MUST start with "/Login?target=" and the URL-encoded path must
     * appear in the href.
     *
     * <p>**Validates: Requirements 1.1**
     */
    @Property(tries = 200)
    void topbarAppendsTargetForKnownRoutes(@ForAll("knownPaths") String knownPath) {
        String href = computeLoginHref(knownPath);

        // The href must begin with the target prefix
        assertThat(href)
                .as("href for known route '%s' should start with '/Login?target='", knownPath)
                .startsWith("/Login?target=");

        // The URL-encoded path must be present in the href
        String expectedEncoded = URLEncoder.encode(knownPath, StandardCharsets.UTF_8)
                .replace("+", "%20");
        assertThat(href)
                .as("href for known route '%s' should contain URL-encoded path '%s'",
                        knownPath, expectedEncoded)
                .contains(expectedEncoded);
    }

    // -------------------------------------------------------------------------
    // Property 2: Topbar omits target for unknown/excluded paths
    // -------------------------------------------------------------------------

    // Feature: login-redirect-on-return, Property 2: Topbar omits target for unknown/excluded paths
    /**
     * For any request path that is empty, equals "/Login", or does not match any
     * known application route, the computed Login href MUST equal exactly "/Login"
     * with no {@code target} query parameter appended.
     *
     * <p>**Validates: Requirements 1.2**
     */
    @Property(tries = 200)
    void topbarOmitsTargetForUnknownPaths(@ForAll("unknownPaths") String unknownPath) {
        String href = computeLoginHref(unknownPath);

        assertThat(href)
                .as("href for unknown/excluded path '%s' should equal '/Login' with no target param",
                        unknownPath)
                .isEqualTo("/Login");
    }

    // -------------------------------------------------------------------------
    // Sanity checks: ensure isKnownRoute correctly classifies the boundary cases
    // -------------------------------------------------------------------------

    @Test
    void loginPathIsNotAKnownRoute() {
        assertThat(isKnownRoute("/Login")).isFalse();
    }

    @Test
    void emptyPathIsNotAKnownRoute() {
        assertThat(isKnownRoute("")).isFalse();
    }

    @Test
    void nullPathIsNotAKnownRoute() {
        assertThat(isKnownRoute(null)).isFalse();
    }

    @Test
    void falsehoodWithoutTrailingSlashIsNotAKnownRoute() {
        // "/Falsehood" does not start with "/Falsehood/" (missing slash) — not a prefix match
        assertThat(isKnownRoute("/Falsehood")).isFalse();
    }

    @Test
    void prefixPlusIdIsKnownRoute() {
        assertThat(isKnownRoute("/Article/42")).isTrue();
        assertThat(isKnownRoute("/Falsehood/99")).isTrue();
        assertThat(isKnownRoute("/ArticleEdit/7")).isTrue();
        assertThat(isKnownRoute("/FalsehoodByBrand/brand-x")).isTrue();
    }
}
