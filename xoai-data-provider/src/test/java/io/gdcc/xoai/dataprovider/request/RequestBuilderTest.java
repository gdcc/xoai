package io.gdcc.xoai.dataprovider.request;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
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

        Stream<Arguments> errorFreeArguments() {
            Granularity sec = Granularity.Second;
            Granularity day = Granularity.Day;
            Granularity len = Granularity.Lenient;
            Instant earliest = LocalDate.of(2022, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

            return Stream.of(
                    Arguments.of(null, null, earliest, sec),
                    Arguments.of(null, null, earliest, day),
                    Arguments.of(null, null, earliest, len),
                    Arguments.of(DateProvider.parse("2022-05-02", day), null, earliest, day),
                    Arguments.of(null, DateProvider.parse("2022-05-02", day), earliest, day),
                    Arguments.of(earliest, DateProvider.parse("2022-05-02", day), earliest, day),
                    Arguments.of(earliest, earliest, earliest, day),
                    Arguments.of(Instant.now().plus(2, ChronoUnit.SECONDS), null, earliest, day),
                    Arguments.of(null, Instant.now().plus(1, ChronoUnit.SECONDS), earliest, sec));
        }

        @ParameterizedTest
        @MethodSource("errorFreeArguments")
        void verifyTimeArguments(
                Instant from, Instant until, Instant earliestDate, Granularity granularity) {
            // given
            Request request = new Request("https://localhost").withFrom(from).withUntil(until);

            // when
            List<BadArgumentException> errors =
                    RequestBuilder.verifyTimeArguments(request, earliestDate, granularity);

            // then
            assertTrue(errors.isEmpty());
        }

        Stream<Arguments> wrongTimes() {
            Granularity sec = Granularity.Second;
            Granularity day = Granularity.Day;
            Granularity len = Granularity.Lenient;
            Instant earliest = LocalDate.of(2022, 5, 1).atStartOfDay().toInstant(ZoneOffset.UTC);

            return Stream.of(
                    // from before earliest
                    Arguments.of(DateProvider.parse("2022-01-01", day), null, earliest, day),
                    Arguments.of(
                            DateProvider.parse("2022-05-22T10:00:00Z", sec),
                            null,
                            DateProvider.parse("2022-05-22T11:00:00Z", sec),
                            sec),
                    Arguments.of(DateProvider.parse("2022-01-01", day), null, earliest, len),

                    // from after now
                    Arguments.of(Instant.now().plus(2, ChronoUnit.SECONDS), null, earliest, sec),
                    Arguments.of(Instant.now().plus(1, ChronoUnit.DAYS), null, earliest, day),
                    Arguments.of(Instant.now().plus(1, ChronoUnit.DAYS), null, earliest, len),

                    // until after now
                    Arguments.of(null, Instant.now().plus(5, ChronoUnit.SECONDS), earliest, sec),
                    Arguments.of(null, Instant.now().plus(1, ChronoUnit.DAYS), earliest, day),
                    Arguments.of(null, Instant.now().plus(1, ChronoUnit.DAYS), earliest, len),

                    // from after until
                    Arguments.of(
                            DateProvider.parse("2022-05-20", day),
                            DateProvider.parse("2022-05-10", day),
                            earliest,
                            day),
                    Arguments.of(
                            DateProvider.parse("2022-05-10T10:00:00Z", sec),
                            DateProvider.parse("2022-05-10T09:00:00Z", sec),
                            earliest,
                            sec));
        }

        @ParameterizedTest
        @MethodSource("wrongTimes")
        void verifyTimeArgumentsFailing(
                Instant from, Instant until, Instant earliestDate, Granularity granularity) {
            // given
            Request request = new Request("https://localhost").withFrom(from).withUntil(until);

            // when
            List<BadArgumentException> errors =
                    RequestBuilder.verifyTimeArguments(request, earliestDate, granularity);

            // then
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
                RepositoryConfiguration.defaults()
                        .withGranularity(granularity)
                        .withEarliestDate(
                                LocalDate.of(2022, 1, 1).atStartOfDay().toInstant(ZoneOffset.UTC));

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
