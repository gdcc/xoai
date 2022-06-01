package io.gdcc.xoai.services.impl;

import io.gdcc.xoai.exceptions.BadResumptionTokenException;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class SimpleResumptionTokenFormatTest {
    
    final SimpleResumptionTokenFormat format = new SimpleResumptionTokenFormat();
    
    @Test
    void cycleFullToken() throws BadResumptionTokenException {
        // given
        ResumptionToken.Value expected = new ResumptionToken.ValueBuilder()
            .withOffset(1)
            .withSetSpec("test")
            .withMetadataPrefix("oai_dc")
            .withFrom(Instant.now().truncatedTo(ChronoUnit.MINUTES))
            .withUntil(Instant.now().truncatedTo(ChronoUnit.MINUTES))
            .build();
        
        // when
        ResumptionToken.Value result = format.parse(format.format(expected));
        
        // then
        assertEquals(expected, result);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
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
    @ValueSource(strings = {
        "    ",
        "\t\t\t",
        "offset::1|",
        "until::2022-05-13T10:00:00Z"
    })
    void validParse(String token) {
        String encoded = SimpleResumptionTokenFormat.base64Encode(token);
        assertDoesNotThrow(() -> format.parse(encoded));
    }

}