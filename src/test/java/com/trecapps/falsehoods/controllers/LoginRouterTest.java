package com.trecapps.falsehoods.controllers;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.web.reactive.function.server.RenderingResponse;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import reactor.core.publisher.Mono;

/**
 * Unit tests for {@link LoginRouter#loginPage(ServerRequest)} edge cases.
 *
 * Validates: Requirements 2.1, 2.2
 */
class LoginRouterTest {

    private LoginRouter router;

    @BeforeEach
    void setUp() throws Exception {
        router = new LoginRouter();
        setField(router, "loginUrl", "http://test-login.example.com");
        setField(router, "falsehoodsUrl", "http://test-gateway.example.com");
        setField(router, "falsehoodsPath", "/falsehoods");
    }

    /**
     * When no {@code target} query param is present, {@code "redirectTarget"} must be {@code ""}.
     * Validates: Requirement 2.2
     */
    @Test
    void noTargetParam_redirectTargetIsEmpty() throws Exception {
        ServerRequest mockRequest = mock(ServerRequest.class);
        when(mockRequest.queryParam("target")).thenReturn(Optional.empty());

        Mono<ServerResponse> responseMono = router.loginPage(mockRequest);
        Map<String, Object> model = extractModel(responseMono);

        assertThat(model.get("redirectTarget"))
                .as("redirectTarget should be empty string when no target param is present")
                .isEqualTo("");
    }

    /**
     * When {@code target} is exactly 2048 chars, the value must not be truncated.
     * Validates: Requirement 2.1
     */
    @Test
    void targetExactly2048Chars_notTruncated() throws Exception {
        String exactly2048 = "a".repeat(2048);

        ServerRequest mockRequest = mock(ServerRequest.class);
        when(mockRequest.queryParam("target")).thenReturn(Optional.of(exactly2048));

        Mono<ServerResponse> responseMono = router.loginPage(mockRequest);
        Map<String, Object> model = extractModel(responseMono);

        assertThat(model.get("redirectTarget"))
                .as("redirectTarget should equal the full 2048-char string")
                .isEqualTo(exactly2048);
    }

    /**
     * When {@code target} is 2049 chars, the value must be truncated to exactly 2048 chars.
     * Validates: Requirement 2.1
     */
    @Test
    void target2049Chars_truncatedTo2048() throws Exception {
        String exactly2048 = "b".repeat(2048);
        String input2049 = exactly2048 + "X";

        ServerRequest mockRequest = mock(ServerRequest.class);
        when(mockRequest.queryParam("target")).thenReturn(Optional.of(input2049));

        Mono<ServerResponse> responseMono = router.loginPage(mockRequest);
        Map<String, Object> model = extractModel(responseMono);

        assertThat(model.get("redirectTarget"))
                .as("redirectTarget should be truncated to 2048 chars")
                .isEqualTo(exactly2048);
    }

    /**
     * When {@code target} contains malformed percent-encoding, {@code "redirectTarget"} must fall back to {@code ""}.
     * Validates: Requirement 2.1
     */
    @Test
    void malformedPercentEncoding_redirectTargetIsEmpty() throws Exception {
        // "%%invalid%%" is malformed percent-encoding that URLDecoder will reject
        String malformed = "%%invalid%%";

        ServerRequest mockRequest = mock(ServerRequest.class);
        when(mockRequest.queryParam("target")).thenReturn(Optional.of(malformed));

        Mono<ServerResponse> responseMono = router.loginPage(mockRequest);
        Map<String, Object> model = extractModel(responseMono);

        assertThat(model.get("redirectTarget"))
                .as("redirectTarget should be empty string when target has malformed percent-encoding")
                .isEqualTo("");
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Map<String, Object> extractModel(Mono<ServerResponse> responseMono) {
        ServerResponse response = responseMono.block();
        assertThat(response).isNotNull();
        RenderingResponse renderingResponse = (RenderingResponse) response;
        return (Map<String, Object>) renderingResponse.model();
    }

    private static void setField(Object target, String fieldName, String value) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(target, value);
    }
}
