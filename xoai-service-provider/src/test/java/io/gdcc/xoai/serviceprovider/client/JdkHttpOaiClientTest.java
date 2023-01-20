package io.gdcc.xoai.serviceprovider.client;

import static io.gdcc.xoai.serviceprovider.client.JdkHttpOaiClient.JdkHttpBuilder;
import static io.gdcc.xoai.serviceprovider.client.OAIClient.Builder;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;

class JdkHttpOaiClientTest {

    private static Stream<Arguments> getNpeHeaders() {
        var nullKeyMap = new HashMap<String, String>();
        nullKeyMap.put(null, "test");
        var nullValueMap = new HashMap<String, String>();
        nullValueMap.put("test", null);

        return Stream.of(Arguments.of(nullKeyMap), Arguments.of(nullValueMap));
    }

    @ParameterizedTest
    @NullSource
    @MethodSource("getNpeHeaders")
    void testBuilderWithCustomHeadersThrowsNPE(Map<String, String> headers) {
        // given
        Builder builder = new JdkHttpBuilder();

        // when & then
        assertThrows(NullPointerException.class, () -> builder.withCustomHeaders(headers));
    }

    private static Stream<Arguments> getIaeHeaders() {
        var emptyHeaderMap = new HashMap<String, String>();
        emptyHeaderMap.put("", "test");

        return Stream.of(Arguments.of(emptyHeaderMap));
    }

    @ParameterizedTest
    @MethodSource("getIaeHeaders")
    void testBuilderWithCustomHeadersThrowsIAE(Map<String, String> headers) {
        // given
        Builder builder = new JdkHttpBuilder();

        // when & then
        assertThrows(IllegalArgumentException.class, () -> builder.withCustomHeaders(headers));
    }

    private static Stream<Arguments> getHeaders() {
        var goodMap = new HashMap<String, String>();
        goodMap.put("test", "test");

        return Stream.of(Arguments.of(goodMap));
    }

    @ParameterizedTest
    @MethodSource("getHeaders")
    void testBuilderWithCustomHeaders(Map<String, String> headers) {
        // given
        Builder builder = new JdkHttpBuilder();

        // when & then
        assertEquals(builder, builder.withCustomHeaders(headers));
    }
}
