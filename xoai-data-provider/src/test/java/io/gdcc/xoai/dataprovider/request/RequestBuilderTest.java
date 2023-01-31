package io.gdcc.xoai.dataprovider.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfigurationTest;
import io.gdcc.xoai.exceptions.BadArgumentException;
import io.gdcc.xoai.exceptions.BadVerbException;
import io.gdcc.xoai.exceptions.OAIException;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.verbs.Verb;
import io.gdcc.xoai.services.api.DateProvider;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RequestBuilderTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class RawRequestTests {
        Stream<Arguments> throwingRawQueries() {
            return Stream.of(
                    Arguments.of(NullPointerException.class, null),
                    Arguments.of(BadVerbException.class, Map.of()),
                    Arguments.of(BadVerbException.class, Map.of("", A())),
                    Arguments.of(BadVerbException.class, Map.of("", A(""))),
                    Arguments.of(BadVerbException.class, Map.of("verb", A(""))),
                    Arguments.of(BadVerbException.class, Map.of("verb", A("doesNotExist"))),
                    Arguments.of(BadVerbException.class, Map.of("verb", A("foobar", "hello"))));
        }

        @ParameterizedTest
        @MethodSource("throwingRawQueries")
        void buildRawRequestThrowsException(
                Class<? extends OAIException> expected, Map<String, String[]> queryMap) {
            assertThrows(expected, () -> RequestBuilder.buildRawRequest(queryMap));
        }

        Stream<Arguments> errornousRawQueries() {
            return Stream.of(
                    Arguments.of(Map.of("verb", A("Identify"), "foobarParam", "")),
                    Arguments.of(Map.of("verb", A("Identify"), "foobarParam", A())),
                    Arguments.of(Map.of("verb", A("Identify"), "foobarParam", A("test"))),
                    Arguments.of(Map.of("verb", A("GetRecord"), "set", A("test"))),
                    Arguments.of(Map.of("verb", A("ListIdentifiers"), "resumptionToken", A())),
                    Arguments.of(
                            Map.of(
                                    "verb",
                                    A("ListIdentifiers"),
                                    "resumptionToken",
                                    A("test1", "test2"))));
        }

        @ParameterizedTest
        @MethodSource("errornousRawQueries")
        void buildRawRequestHasErrors(Map<String, String[]> queryMap) throws OAIException {
            // when
            RequestBuilder.RawRequest rawRequest = RequestBuilder.buildRawRequest(queryMap);

            // then
            assertTrue(rawRequest.hasErrors());
            assertEquals(1, rawRequest.getErrors().size());
        }

        @Test
        void buildRawRequest() throws OAIException {
            // given
            Map<String, String[]> query =
                    Map.of("verb", A("GetRecord"), "metadataPrefix", A("test"));
            // when
            RequestBuilder.RawRequest rawRequest = RequestBuilder.buildRawRequest(query);
            // then
            assertFalse(rawRequest.hasErrors());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class CompileRequestTests {

        Request request = new Request("https://localhost");

        Stream<Arguments> errorFreeArguments() {
            return Stream.of(
                    Arguments.of(Verb.Argument.MetadataPrefix, "test"),
                    Arguments.of(Verb.Argument.Identifier, "test"),
                    Arguments.of(Verb.Argument.Set, "test"),
                    Arguments.of(Verb.Argument.ResumptionToken, "test"),
                    Arguments.of(Verb.Argument.From, "2021-05-01"),
                    Arguments.of(Verb.Argument.From, "2021-05-01T00:00:01Z"),
                    Arguments.of(Verb.Argument.Until, "2021-05-01"),
                    Arguments.of(Verb.Argument.Until, "2021-05-01T00:00:01Z"));
        }

        @ParameterizedTest
        @MethodSource("errorFreeArguments")
        void compileRequest(Verb.Argument argument, String value) {
            // given
            Granularity granularity = Granularity.Lenient;
            Map<Verb.Argument, String> arguments = new EnumMap<>(Verb.Argument.class);
            arguments.put(argument, value);

            // when
            List<BadArgumentException> errors =
                    RequestBuilder.compileRequestArgument(request, arguments, granularity);

            // then
            assertTrue(errors.isEmpty());
        }

        @Test
        void compileRequestThrows() {
            Map<Verb.Argument, String> arguments = Map.of(Verb.Argument.Verb, "test");
            assertThrows(
                    InternalOAIException.class,
                    () ->
                            RequestBuilder.compileRequestArgument(
                                    request, arguments, Granularity.Second));
        }

        Stream<Arguments> failingTimeArguments() {
            return Stream.of(
                    Arguments.of(Map.of(Verb.Argument.From, "2021-05-01"), Granularity.Second),
                    Arguments.of(
                            Map.of(Verb.Argument.From, "2021-05-01T00:00:01Z"), Granularity.Day),
                    Arguments.of(Map.of(Verb.Argument.Until, "2021-05-01"), Granularity.Second),
                    Arguments.of(
                            Map.of(Verb.Argument.Until, "2021-05-01T00:00:01Z"), Granularity.Day),
                    Arguments.of(
                            Map.of(
                                    Verb.Argument.From,
                                    "2021-05-01",
                                    Verb.Argument.Until,
                                    "2021-05-01T00:00:01Z"),
                            Granularity.Day),
                    Arguments.of(
                            Map.of(
                                    Verb.Argument.From,
                                    "2021-05-01",
                                    Verb.Argument.Until,
                                    "2021-05-01T00:00:01Z"),
                            Granularity.Second),
                    Arguments.of(
                            Map.of(
                                    Verb.Argument.From,
                                    "2021-05-01T00:00:01Z",
                                    Verb.Argument.Until,
                                    "2021-05-01"),
                            Granularity.Day),
                    Arguments.of(
                            Map.of(
                                    Verb.Argument.From,
                                    "2021-05-01T00:00:01Z",
                                    Verb.Argument.Until,
                                    "2021-05-01"),
                            Granularity.Second));
        }

        @ParameterizedTest
        @MethodSource("failingTimeArguments")
        void compileRequest(Map<Verb.Argument, String> arguments, Granularity granularity) {
            // when
            List<BadArgumentException> errors =
                    RequestBuilder.compileRequestArgument(request, arguments, granularity);
            // then
            assertFalse(errors.isEmpty());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class VerifyTimeArgumentsTests {

        Granularity sec = Granularity.Second;
        Granularity day = Granularity.Day;
        Granularity len = Granularity.Lenient;

        String earliest_raw = "2022-05-01";
        Instant earliest = LocalDate.of(2022, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

        Instant sec_after_now = Instant.now().plus(1, ChronoUnit.SECONDS);
        String sec_after_now_raw = DateProvider.format(sec_after_now, sec);

        Instant after_now = Instant.now().plus(5, ChronoUnit.SECONDS);
        String after_now_raw = DateProvider.format(after_now, sec);

        Instant after_day = Instant.now().plus(1, ChronoUnit.DAYS);
        String after_day_raw = DateProvider.format(after_day, day);

        Boolean requireFAE = true;

        class Wrapper {
            Instant from;
            String rawFrom;
            Instant until;
            String rawUntil;
            Instant earliestDate;
            Granularity granularity;
            Boolean requireFromAfterEarliest;

            Wrapper(
                    Instant from,
                    String rawFrom,
                    Instant until,
                    String rawUntil,
                    Instant earliestDate,
                    Granularity granularity,
                    Boolean requireFromAfterEarliest) {
                this.from = from;
                this.rawFrom = rawFrom;
                this.until = until;
                this.rawUntil = rawUntil;
                this.earliestDate = earliestDate;
                this.granularity = granularity;
                this.requireFromAfterEarliest = requireFromAfterEarliest;
            }

            Arguments asArg() {
                return Arguments.of(this);
            }

            @Override
            public String toString() {
                return "Wrapper{"
                        + "from="
                        + from
                        + ", rawFrom='"
                        + rawFrom
                        + '\''
                        + ", until="
                        + until
                        + ", rawUntil='"
                        + rawUntil
                        + '\''
                        + ", earliestDate="
                        + earliestDate
                        + ", granularity="
                        + granularity
                        + ", requireFromAfterEarliest="
                        + requireFromAfterEarliest
                        + '}';
            }
        }

        Stream<Arguments> errorFreeArguments() {
            String may_second_raw = "2022-05-02";
            Instant may_second = DateProvider.parse(may_second_raw, day);
            String april_thirty_raw = "2022-04-30";
            Instant april_thirty = DateProvider.parse(april_thirty_raw, day);

            Boolean requireFAE = true;

            return Stream.of(
                    new Wrapper(null, null, null, null, earliest, sec, requireFAE).asArg(),
                    new Wrapper(null, null, null, null, earliest, day, requireFAE).asArg(),
                    new Wrapper(null, null, null, null, earliest, len, requireFAE).asArg(),
                    new Wrapper(null, null, null, null, earliest, sec, requireFAE).asArg(),
                    new Wrapper(may_second, may_second_raw, null, null, earliest, day, requireFAE)
                            .asArg(),
                    new Wrapper(null, null, may_second, may_second_raw, earliest, day, requireFAE)
                            .asArg(),
                    new Wrapper(
                                    earliest,
                                    earliest_raw,
                                    may_second,
                                    may_second_raw,
                                    earliest,
                                    day,
                                    requireFAE)
                            .asArg(),
                    new Wrapper(
                                    april_thirty,
                                    april_thirty_raw,
                                    may_second,
                                    may_second_raw,
                                    earliest,
                                    day,
                                    !requireFAE)
                            .asArg(),
                    new Wrapper(
                                    earliest,
                                    earliest_raw,
                                    earliest,
                                    earliest_raw,
                                    earliest,
                                    day,
                                    requireFAE)
                            .asArg(),
                    new Wrapper(after_now, after_now_raw, null, null, earliest, day, requireFAE)
                            .asArg(),
                    new Wrapper(
                                    null,
                                    null,
                                    sec_after_now,
                                    sec_after_now_raw,
                                    earliest,
                                    sec,
                                    requireFAE)
                            .asArg());
        }

        @ParameterizedTest
        @MethodSource("errorFreeArguments")
        void verifyTimeArguments(Wrapper w) {
            // given
            Request request = new Request("https://localhost").withFrom(w.from).withUntil(w.until);
            request.saveRawFrom(w.rawFrom);
            request.saveRawUntil(w.rawUntil);

            // when
            List<BadArgumentException> errors =
                    RequestBuilder.verifyTimeArguments(
                            request, w.earliestDate, w.granularity, w.requireFromAfterEarliest);

            // then
            if (!errors.isEmpty()) {
                errors.forEach(e -> System.out.println(e.getMessage()));
            }
            assertTrue(errors.isEmpty());
        }

        Stream<Arguments> wrongTimes() {
            return Stream.of(
                    // from and until have different granularity
                    new Wrapper(
                                    DateProvider.parse("2022-01-01", day),
                                    "2022-01-01",
                                    DateProvider.parse("2022-04-30T10:00:00Z", sec),
                                    "2022-04-30T10:00:00Z",
                                    earliest,
                                    day,
                                    requireFAE)
                            .asArg(),
                    new Wrapper(
                                    DateProvider.parse("2022-01-01", day),
                                    "2022-01-01",
                                    DateProvider.parse("2022-04-30T10:00:00Z", sec),
                                    "2022-04-30T10:00:00Z",
                                    earliest,
                                    sec,
                                    requireFAE)
                            .asArg(),
                    new Wrapper(
                                    DateProvider.parse("2022-01-01", day),
                                    "2022-01-01",
                                    DateProvider.parse("2022-04-30T10:00:00Z", sec),
                                    "2022-04-30T10:00:00Z",
                                    earliest,
                                    len,
                                    requireFAE)
                            .asArg(),

                    // from before earliest
                    new Wrapper(
                                    DateProvider.parse("2022-01-01", day),
                                    "2022-01-01",
                                    null,
                                    null,
                                    earliest,
                                    day,
                                    requireFAE)
                            .asArg(),
                    new Wrapper(
                                    DateProvider.parse("2022-04-30T10:00:00Z", sec),
                                    "2022-04-30T10:00:00Z",
                                    null,
                                    null,
                                    earliest,
                                    sec,
                                    requireFAE)
                            .asArg(),
                    new Wrapper(
                                    DateProvider.parse("2022-01-01", day),
                                    "2022-01-01",
                                    null,
                                    null,
                                    earliest,
                                    len,
                                    requireFAE)
                            .asArg(),

                    // from after now
                    new Wrapper(after_now, after_now_raw, null, null, earliest, sec, requireFAE)
                            .asArg(),
                    new Wrapper(after_day, after_day_raw, null, null, earliest, day, requireFAE)
                            .asArg(),
                    new Wrapper(after_day, after_day_raw, null, null, earliest, len, requireFAE)
                            .asArg(),

                    // until after now
                    new Wrapper(null, null, after_now, after_now_raw, earliest, sec, requireFAE)
                            .asArg(),
                    new Wrapper(null, null, after_day, after_day_raw, earliest, day, requireFAE)
                            .asArg(),
                    new Wrapper(null, null, after_day, after_day_raw, earliest, len, requireFAE)
                            .asArg(),

                    // from after until
                    new Wrapper(
                                    DateProvider.parse("2022-05-20", day),
                                    "2022-05-20",
                                    DateProvider.parse("2022-05-10", day),
                                    "2022-05-10",
                                    earliest,
                                    day,
                                    requireFAE)
                            .asArg(),
                    new Wrapper(
                                    DateProvider.parse("2022-05-10T10:00:00Z", sec),
                                    "2022-05-10T10:00:00Z",
                                    DateProvider.parse("2022-05-10T09:00:00Z", sec),
                                    "2022-05-10T09:00:00Z",
                                    earliest,
                                    sec,
                                    requireFAE)
                            .asArg());
        }

        @ParameterizedTest
        @MethodSource("wrongTimes")
        void verifyTimeArgumentsFailing(Wrapper w) {
            // given
            Request request = new Request("https://localhost").withFrom(w.from).withUntil(w.until);
            request.saveRawFrom(w.rawFrom);
            request.saveRawUntil(w.rawUntil);

            // when
            List<BadArgumentException> errors =
                    RequestBuilder.verifyTimeArguments(
                            request, w.earliestDate, w.granularity, w.requireFromAfterEarliest);

            // then

            if (!errors.isEmpty()) {
                errors.forEach(e -> System.out.println(e.getMessage()));
            }

            assertFalse(errors.isEmpty());
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class VerifyArgumentsPresenceTests {

        Stream<Arguments> failingArguments() {
            return Stream.of(
                    Arguments.of(
                            Verb.Type.Identify,
                            Set.of(Verb.Argument.From, Verb.Argument.Identifier)),
                    Arguments.of(
                            Verb.Type.GetRecord, Set.of(Verb.Argument.From, Verb.Argument.Set)),
                    Arguments.of(
                            Verb.Type.ListIdentifiers,
                            Set.of(Verb.Argument.ResumptionToken, Verb.Argument.Set)),
                    Arguments.of(
                            Verb.Type.ListIdentifiers,
                            Set.of(
                                    Verb.Argument.MetadataPrefix,
                                    Verb.Argument.Set,
                                    Verb.Argument.From,
                                    Verb.Argument.Identifier)),
                    Arguments.of(
                            Verb.Type.ListRecords,
                            Set.of(Verb.Argument.ResumptionToken, Verb.Argument.Set)),
                    Arguments.of(
                            Verb.Type.ListSets,
                            Set.of(Verb.Argument.ResumptionToken, Verb.Argument.Set)),
                    Arguments.of(Verb.Type.ListSets, Set.of(Verb.Argument.From)));
        }

        @ParameterizedTest
        @MethodSource("failingArguments")
        void verifyTimeArguments(Verb.Type verb, Set<Verb.Argument> arguments) {
            // when
            List<BadArgumentException> errors =
                    RequestBuilder.validateArgumentPresence(verb, arguments);
            // then
            assertFalse(errors.isEmpty());
        }

        Stream<Arguments> validArguments() {
            return Stream.of(
                    Arguments.of(Verb.Type.Identify, Set.of()),
                    Arguments.of(
                            Verb.Type.GetRecord,
                            Set.of(Verb.Argument.Identifier, Verb.Argument.MetadataPrefix)),
                    Arguments.of(Verb.Type.ListIdentifiers, Set.of(Verb.Argument.ResumptionToken)),
                    Arguments.of(
                            Verb.Type.ListIdentifiers,
                            Set.of(
                                    Verb.Argument.MetadataPrefix,
                                    Verb.Argument.Set,
                                    Verb.Argument.From,
                                    Verb.Argument.Until)),
                    Arguments.of(
                            Verb.Type.ListRecords,
                            Set.of(Verb.Argument.MetadataPrefix, Verb.Argument.From)));
        }

        @ParameterizedTest
        @MethodSource("validArguments")
        void verifyValidTimeArguments(Verb.Type verb, Set<Verb.Argument> arguments) {
            // when
            List<BadArgumentException> errors =
                    RequestBuilder.validateArgumentPresence(verb, arguments);

            // then
            assertTrue(errors.isEmpty());
        }
    }

    static Stream<Arguments> skewing() {
        return Stream.of(
                // Day precision - until will be skewed to end of day
                Arguments.of(
                        DateProvider.parse("2022-05-22", Granularity.Day),
                        LocalDateTime.of(2022, 5, 22, 23, 59, 59, 999999999)
                                .toInstant(ZoneOffset.UTC),
                        Granularity.Day),
                // Second precision - until skewed by 1 seconds
                Arguments.of(
                        DateProvider.parse("2022-05-22T10:59:59Z", Granularity.Second),
                        LocalDateTime.of(2022, 5, 22, 11, 0, 0).toInstant(ZoneOffset.UTC),
                        Granularity.Second),
                // Lenient precision - until skewed by 1 seconds
                Arguments.of(
                        DateProvider.parse("2022-05-22T10:59:59Z", Granularity.Lenient),
                        LocalDateTime.of(2022, 5, 22, 23, 59, 59, 999999999)
                                .toInstant(ZoneOffset.UTC),
                        Granularity.Lenient));
    }

    @ParameterizedTest
    @MethodSource("skewing")
    void testForTimeSkewingForDay(Instant until, Instant expectedSkew, Granularity granularity) {
        // given
        RepositoryConfiguration configuration =
                RepositoryConfigurationTest.defaults()
                        .withGranularity(granularity)
                        .withEarliestDate(
                                LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC))
                        .build();

        RequestBuilder.RawRequest rawRequest =
                new RequestBuilder.RawRequest(Verb.Type.ListRecords)
                        .withArgument(Verb.Argument.MetadataPrefix, "oai_dc")
                        .withArgument(Verb.Argument.Until, DateProvider.format(until, granularity));

        // when
        Request request = RequestBuilder.buildRequest(rawRequest, configuration);
        Instant skewedUntil = request.getUntil().get();

        // then
        assertTrue(until.isBefore(skewedUntil));
        assertEquals(expectedSkew, skewedUntil);
    }

    static String[] A(String... strings) {
        return strings;
    }
}
