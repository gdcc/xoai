/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.exceptions.BadArgumentException;
import io.gdcc.xoai.dataprovider.exceptions.handler.CannotDisseminateFormatException;
import io.gdcc.xoai.dataprovider.exceptions.handler.DoesNotSupportSetsException;
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMatchesException;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.HasXPathMatcher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static io.gdcc.xoai.dataprovider.model.InMemoryItem.randomItem;
import static io.gdcc.xoai.model.oaipmh.verbs.Verb.Type.ListRecords;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ListRecordsHandlerTest extends AbstractHandlerTest {
    private final ListRecordsHandler underTest = new ListRecordsHandler(aContext(), theRepository());

    @Test
    void missingMetadataFormat() {
        assertThrows(BadArgumentException.class,
            () -> underTest.handle(request().withVerb(ListRecords)));
    }

    @Test
    void cannotDisseminateFormat() {
        theItemRepository().withItem(randomItem().withIdentifier("1"));
        aContext().withMetadataFormat(EXISTING_METADATA_FORMAT, MetadataFormat.identity());
    
        assertThrows(CannotDisseminateFormatException.class,
            () -> underTest.handle(request().withVerb(ListRecords).withMetadataPrefix("abcd")));
    }

    @Test
    void noMatchRecords() {
        assertThrows(NoMatchesException.class,
            () -> underTest.handle(request()
                    .withVerb(ListRecords)
                    .withMetadataPrefix(EXISTING_METADATA_FORMAT)));
    }

    @Test
    void setRequestAndSetsNotSupported() {
        theSetRepository().doesNotSupportSets();
        assertThrows(DoesNotSupportSetsException.class,
            () -> underTest.handle(request()
                    .withVerb(ListRecords)
                    .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                    .withSet("sad")));
    }

    @Test
    void validResponse() throws Exception {
        theItemRepository().withRandomItems(10);
        String result = write(underTest.handle(request().withVerb(ListRecords).withMetadataPrefix(EXISTING_METADATA_FORMAT)));
        assertThat(result, xPath("count(//record)", asInteger(equalTo(10))));
    }
    
    @Test
    void validResponseCopyElement() throws Exception {
        // given
        theItemRepository()
            .withRandomItems(10)
            .withItem(
            randomItem()
                .withDeleted(false)
                .withIdentifier("copy")
                .withMetadata(Metadata.copyFromStream(new ByteArrayInputStream("<testdata>Test1234</testdata>".getBytes(StandardCharsets.UTF_8))))
        );
        aContext().withMetadataFormat("custom", MetadataFormat.identity());
        String result = write(underTest.handle(request().withVerb(ListRecords).withMetadataPrefix("custom")));
        assertThat(result, xPath("count(//record)", asInteger(equalTo(11))));
        assertThat(result, HasXPathMatcher.hasXPath("//record/metadata/testdata"));
    }
}
