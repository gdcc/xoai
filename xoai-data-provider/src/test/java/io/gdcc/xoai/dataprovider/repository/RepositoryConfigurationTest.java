package io.gdcc.xoai.dataprovider.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.services.api.DateProvider;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RepositoryConfigurationTest {

    private static Stream<Arguments> skews() {
        return Stream.of(
                Arguments.of(
                        LocalDateTime.of(2022, 5, 19, 10, 0, 0, 123).toInstant(ZoneOffset.UTC),
                        Granularity.Second,
                        "2022-05-19T10:00:01Z",
                        "2022-05-19T10:00:01Z"),
                Arguments.of(
                        LocalDateTime.of(2022, 5, 19, 10, 0, 0, 123).toInstant(ZoneOffset.UTC),
                        Granularity.Day,
                        "2022-05-19",
                        "2022-05-19T23:59:59Z"));
    }

    @ParameterizedTest
    @MethodSource("skews")
    void skewUntil(
            Instant timestamp,
            Granularity granularity,
            String expected,
            String expectedAtSecondGranularity) {
        // given
        RepositoryConfiguration config =
                RepositoryConfiguration.defaults().withGranularity(granularity);

        // when
        Instant skewedTime = config.skewUntil(timestamp);

        // then
        assertTrue(skewedTime.isAfter(timestamp));
        assertEquals(expected, DateProvider.format(skewedTime, granularity));
        assertEquals(
                expectedAtSecondGranularity, DateProvider.format(skewedTime, Granularity.Second));
    }

    @Test
    void setAdminEmails() {
        List<String> mails = List.of("test", "test2", "test3");

        RepositoryConfiguration subject = RepositoryConfiguration.defaults();
        assumeTrue(subject.getAdminEmails().size() == 1);

        subject.setAdminEmails(mails);
        assertEquals(mails.size(), subject.getAdminEmails().size());
        assertEquals(mails, subject.getAdminEmails());
    }
}
