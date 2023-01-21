package io.gdcc.xoai.dataprovider.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration.RepositoryConfigurationBuilder;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.services.api.DateProvider;
import io.gdcc.xoai.services.impl.SimpleResumptionTokenFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class RepositoryConfigurationTest {

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
                RepositoryConfigurationTest.defaults().withGranularity(granularity).build();

        // when
        Instant skewedTime = config.skewUntil(timestamp);

        // then
        assertTrue(skewedTime.isAfter(timestamp));
        assertEquals(expected, DateProvider.format(skewedTime, granularity));
        assertEquals(
                expectedAtSecondGranularity, DateProvider.format(skewedTime, Granularity.Second));
    }

    @Test
    void setAdminEmailsWithNull() {
        RepositoryConfigurationBuilder subject = RepositoryConfigurationTest.defaults();
        assertThrows(
                IllegalArgumentException.class, () -> subject.setAdminEmails((List<String>) null));
        assertThrows(IllegalArgumentException.class, () -> subject.setAdminEmails((String[]) null));
        assertThrows(
                IllegalArgumentException.class, () -> subject.withAdminEmails((String[]) null));
        assertThrows(IllegalArgumentException.class, () -> subject.withAdminEmail(null));
    }

    @Test
    void setAdminEmailsWithNullOrEmptyElement() {
        RepositoryConfigurationBuilder subject = RepositoryConfigurationTest.defaults();

        List<String> nullList = new ArrayList<>();
        nullList.add(null);

        List<String> emptyList = new ArrayList<>();

        List<String> listWithEmptyElement = List.of("");

        assertThrows(IllegalArgumentException.class, () -> subject.setAdminEmails(nullList));
        assertThrows(IllegalArgumentException.class, () -> subject.setAdminEmails(emptyList));
        assertThrows(
                IllegalArgumentException.class, () -> subject.setAdminEmails(listWithEmptyElement));
    }

    @Test
    void setAdminEmailsFromList() {
        List<String> mails = List.of("test", "test2", "test3");

        RepositoryConfigurationBuilder subject = RepositoryConfigurationTest.defaults();
        assumeTrue(subject.adminEmails.size() == 1);

        subject.setAdminEmails(mails);
        assertEquals(mails.size(), subject.adminEmails.size());
        assertEquals(mails, subject.adminEmails);
    }

    @Test
    void setAdminEmailsFromArray() {
        String[] mails = List.of("test", "test2", "test3").toArray(String[]::new);

        RepositoryConfigurationBuilder subject = RepositoryConfigurationTest.defaults();
        assumeTrue(subject.adminEmails.size() == 1);

        subject.setAdminEmails(mails);
        assertEquals(mails.length, subject.adminEmails.size());
        assertArrayEquals(mails, subject.adminEmails.toArray(String[]::new));
    }

    @Test
    void setCompression() {
        String compression = "zip";
    }

    /**
     * Helper function to retrieve an almost done config, only call build() or alter a bit before
     * that...
     *
     * @return
     */
    public static RepositoryConfigurationBuilder defaults() {
        return new RepositoryConfigurationBuilder()
                .withGranularity(Granularity.Second)
                .withRepositoryName("Repository")
                .withEarliestDate(DateProvider.now())
                .withAdminEmail("sample@test.com")
                .withBaseUrl("http://localhost")
                .withMaxListRecords(100)
                .withMaxListIdentifiers(100)
                .withMaxListSets(100)
                .withDeleteMethod(DeletedRecord.NO)
                .withResumptionTokenFormat(
                        new SimpleResumptionTokenFormat().withGranularity(Granularity.Second))
                .withEnableMetadataAttributes(false);
    }
}
