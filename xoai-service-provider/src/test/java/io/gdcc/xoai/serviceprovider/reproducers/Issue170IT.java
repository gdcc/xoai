package io.gdcc.xoai.serviceprovider.reproducers;

import io.gdcc.xoai.model.oaipmh.results.record.Header;
import io.gdcc.xoai.serviceprovider.ServiceProvider;
import io.gdcc.xoai.serviceprovider.client.OAIClient;
import io.gdcc.xoai.serviceprovider.exceptions.BadArgumentException;
import io.gdcc.xoai.serviceprovider.model.Context;
import io.gdcc.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;

/**
 * This is a test class to verify behavior described in <a
 * href="https://github.com/gdcc/xoai/issues/170">GitHub Issue #170</a>
 */
class Issue170IT {

    static final String baseUrl = "http://dlibra.umcs.lublin.pl/dlibra/oai-pmh-repository.xml";
    static final String metadataPrefix = "mets";
    // earliest timestamp via Identify
    // static final Instant from = LocalDate.of(2008, 05,
    // 07).atStartOfDay().toInstant(ZoneOffset.UTC);
    static final Instant from = LocalDate.of(2023, 06, 01).atStartOfDay().toInstant(ZoneOffset.UTC);
    // static final Instant until = LocalDate.of(2012, 12,
    // 31).atStartOfDay().toInstant(ZoneOffset.UTC);
    static final Instant until = Instant.now();
    static final OAIClient client = OAIClient.newBuilder().withBaseUrl(baseUrl).build();

    @BeforeAll
    static void setUp() {}

    @RepeatedTest(5)
    void testHarvest() throws BadArgumentException {
        Context context = new Context().withOAIClient(client).withBaseUrl(baseUrl);

        ServiceProvider svcProvider = new ServiceProvider(context);

        Iterator<Header> result =
                svcProvider.listIdentifiers(
                        ListIdentifiersParameters.request()
                                .withMetadataPrefix(metadataPrefix)
                                .withFrom(from)
                                .withUntil(until));

        List<Header> headers = new ArrayList<>();
        result.forEachRemaining(headers::add);

        Assertions.assertFalse(headers.isEmpty());
    }
}
