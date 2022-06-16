package io.gdcc.xoai.services.impl;

import static org.junit.jupiter.api.Assertions.*;

import io.gdcc.xoai.exceptions.BadResumptionTokenException;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.services.api.ResumptionTokenFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SimpleResumptionTokenFormatTest {

    static final ResumptionTokenFormat format = new SimpleResumptionTokenFormat();

    static Stream<Arguments> fullCycleExamples() {
        return Stream.of(
                Arguments.of(ChronoUnit.MINUTES, format),
                Arguments.of(
                        ChronoUnit.DAYS,
                        new SimpleResumptionTokenFormat().withGranularity(Granularity.Day)),
                Arguments.of(
                        ChronoUnit.MINUTES,
                        new SimpleResumptionTokenFormat().withGranularity(Granularity.Lenient)),
                Arguments.of(
                        ChronoUnit.DAYS,
                        new SimpleResumptionTokenFormat().withGranularity(Granularity.Lenient)));
    }

    @ParameterizedTest
    @MethodSource("fullCycleExamples")
    void cycleFullToken(ChronoUnit limit, ResumptionTokenFormat format)
            throws BadResumptionTokenException {
        // given
        ResumptionToken.Value expected =
                new ResumptionToken.ValueBuilder()
                        .withOffset(1)
                        .withSetSpec("test")
                        .withMetadataPrefix("oai_dc")
                        .withFrom(Instant.now().truncatedTo(limit))
                        .withUntil(Instant.now().truncatedTo(limit))
                        .build();

        // when && then
        assertEquals(expected, format.parse(format.format(expected)));
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "asdasdas",
                "offset::1|set::",
                "offset::1|hellooooooo",
                "offset::1|hellooooooo::foobar",
                "offset::1|set::foo::bar",
                "until::foobar"
            })
    void failingParse(String token) {
        String encoded = SimpleResumptionTokenFormat.base64Encode(token);
        assertThrows(BadResumptionTokenException.class, () -> format.parse(encoded));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"    ", "\t\t\t", "offset::1|", "until::2022-05-13T10:00:00Z"})
    void validParse(String token) {
        String encoded = SimpleResumptionTokenFormat.base64Encode(token);
        assertDoesNotThrow(() -> format.parse(encoded));
    }
}
