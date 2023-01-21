/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider;

import static io.gdcc.xoai.model.oaipmh.verbs.Verb.Type.ListRecords;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;

import io.gdcc.xoai.dataprovider.handlers.AbstractHandlerTest;
import io.gdcc.xoai.dataprovider.request.RequestBuilder;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.Verb;
import io.gdcc.xoai.xml.XmlWritable;
import io.gdcc.xoai.xml.XmlWriter;
import io.gdcc.xoai.xmlio.exceptions.XmlWriteException;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.EvaluateXPathMatcher;
import org.xmlunit.matchers.HasXPathMatcher;

public class DataProviderTest extends AbstractHandlerTest {
    private static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";
    private final DataProvider dataProvider = new DataProvider(aContext(), theRepository());

    @Test
    public void handleFromServletQueryParameters() throws Exception {
        // when
        String result = write(dataProvider.handle(Map.of("verb", new String[] {"Identify"})));

        // then
        assertThat(
                result,
                xPath(
                        "//oai:Identify/oai:baseURL/text()",
                        equalTo(theRepository().getConfiguration().getBaseUrl())));
    }

    @Test
    public void missingVerbFromServletQueryParameters() throws Exception {
        // when
        String result = write(dataProvider.handle(Map.of("verb", new String[] {""})));

        // then
        assertThat(result, xPath("//oai:error/@code", equalTo("badVerb")));
    }

    @Test
    public void invalidVerbFromServletQueryParameters() throws Exception {
        // when
        String result = write(dataProvider.handle(Map.of("verb", new String[] {"hello"})));

        // then
        assertThat(result, xPath("//oai:error/@code", equalTo("badVerb")));
    }

    @Test
    public void missingMetadataFormat() throws Exception {
        // when
        String result = write(dataProvider.handle(new RequestBuilder.RawRequest(ListRecords)));

        // then
        assertThat(result, xPath("//oai:error/@code", equalTo("badArgument")));
    }

    @Test
    public void noMatchRecords() throws Exception {
        // when
        String result =
                write(
                        dataProvider.handle(
                                new RequestBuilder.RawRequest(ListRecords)
                                        .withArgument(
                                                Verb.Argument.MetadataPrefix,
                                                EXISTING_METADATA_FORMAT)));

        // then
        assertThat(result, xPath("//oai:error/@code", equalTo("noRecordsMatch")));
    }

    @Test
    public void oneRecordMatch() throws Exception {
        // given
        theItemRepository().withRandomItems(1);

        // when
        String result =
                write(
                        dataProvider.handle(
                                new RequestBuilder.RawRequest(ListRecords)
                                        .withArgument(
                                                Verb.Argument.MetadataPrefix,
                                                EXISTING_METADATA_FORMAT)));

        // then
        assertThat(result, xPath("count(//oai:record)", asInteger(equalTo(1))));
    }

    @Test
    public void incompleteResponseFirstPage() throws Exception {
        // given
        theItemRepository().withRandomItems(10);
        theRepository()
                .getConfiguration()
                .asTemplate()
                .withMaxListRecords(5)
                .build()
                .inject(theRepository());

        // when
        String result =
                write(
                        dataProvider.handle(
                                request()
                                        .withVerb(ListRecords)
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)));

        // then
        assertThat(result, xPath("count(//oai:record)", asInteger(equalTo(5))));
        assertThat(result, hasXPath("//oai:resumptionToken"));
    }

    @Test
    public void incompleteResponseLastPage() throws Exception {
        // given
        theItemRepository().withRandomItems(10);
        theRepository()
                .getConfiguration()
                .asTemplate()
                .withMaxListRecords(5)
                .build()
                .inject(theRepository());

        // when
        String result =
                write(
                        dataProvider.handle(
                                request()
                                        .withVerb(ListRecords)
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                        .withResumptionToken(
                                                valueOf(
                                                        new ResumptionToken.ValueBuilder()
                                                                .withMetadataPrefix(
                                                                        EXISTING_METADATA_FORMAT)
                                                                .withOffset(5)
                                                                .build()))));

        assertThat(result, xPath("count(//oai:record)", equalTo("5")));
        assertThat(result, xPath("//oai:resumptionToken", equalTo("")));
    }

    protected static Matcher<? super String> xPath(String xPath, Matcher<String> valueMatcher) {
        return EvaluateXPathMatcher.hasXPath(xPath, valueMatcher)
                .withNamespaceContext(Map.of("oai", OAI_NAMESPACE));
    }

    protected static Matcher<? super String> hasXPath(String xPath) {
        return HasXPathMatcher.hasXPath(xPath).withNamespaceContext(Map.of("oai", OAI_NAMESPACE));
    }

    @Override
    protected String write(final XmlWritable handle) throws XMLStreamException, XmlWriteException {
        return XmlWriter.toString(writer -> writer.write(handle));
    }

    @Test
    void readOaiXslt() {
        assertThat(DataProvider.getOaiXSLT(), not(emptyString()));
    }
}
