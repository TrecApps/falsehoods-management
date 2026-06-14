package com.trecapps.falsehoods.controllers;

import net.jqwik.api.*;
import net.jqwik.api.constraints.StringLength;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Property-based tests for LoginRouter.
 *
 * **Validates: Requirements 2.1**
 */
class LoginRouterPropertyTest {

    // Feature: login-redirect-on-return, Property 3: LoginRouter decodes and caps the target parameter
    @Property(tries = 100)
    void redirectTargetIsDecodedAndCapped(
            @ForAll @StringLength(min = 1, max = 4096) String rawInput) throws Exception {

        // Arrange: instantiate LoginRouter and inject field values via reflection
        LoginRouter router = new LoginRouter();
        setField(router, "loginUrl", "http://test-login.example.com");
        setField(router, "falsehoodsUrl", "http://test-gateway.example.com");
        setField(router, "falsehoodsPath", "/falsehoods");

        // URL-encode the raw input (mirroring what the browser/Thymeleaf sends)
        String encoded = URLEncoder.encode(rawInput, StandardCharsets.UTF_8);

        // Build a mock ServerRequest with target = encoded
        ServerRequest mockRequest = mock(ServerRequest.class);
        when(mockRequest.queryParam("target")).thenReturn(Optional.of(encoded));

        // Act: invoke loginPage
        Mono<ServerResponse> responseMono = router.loginPage(mockRequest);

        // Extract the rendered model from the ServerResponse via its EntityResponse context
        ServerResponse response = responseMono.block();
        assertThat(response).isNotNull();

        // ServerResponse.render() stores model attributes accessible via the rendering context.
        // We reach them through the package-private RenderingResponse API.
        // Cast to RenderingResponse to access the model map.
        org.springframework.web.reactive.function.server.RenderingResponse renderingResponse =
                (org.springframework.web.reactive.function.server.RenderingResponse) response;
        Map<String, Object> model = (Map<String, Object>) renderingResponse.model();

        // Assert: redirectTarget equals the decoded input, capped at 2048 chars
        String expectedTarget = rawInput.substring(0, Math.min(rawInput.length(), 2048));
        assertThat(model.get("redirectTarget"))
                .as("redirectTarget should be the decoded, capped rawInput")
                .isEqualTo(expectedTarget);
    }

    // ---------------------------------------------------------------------------
    // Helper: set a private/package-private field on the target object
    // ---------------------------------------------------------------------------
    private static void setField(Object target, String fieldName, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
