/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.exceptions.handler.CannotDisseminateFormatException;
import io.gdcc.xoai.dataprovider.exceptions.handler.DoesNotSupportSetsException;
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMatchesException;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import io.gdcc.xoai.model.oaipmh.verbs.ListIdentifiers;
import io.gdcc.xoai.model.oaipmh.verbs.ListRecords;
import io.gdcc.xoai.model.oaipmh.verbs.ListSets;
import io.gdcc.xoai.model.oaipmh.verbs.Verb;
import io.gdcc.xoai.xml.EchoElement;
import io.gdcc.xoai.xml.XmlWriter;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.HasXPathMatcher;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static io.gdcc.xoai.dataprovider.model.InMemoryItem.randomItem;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

public class ListRecordsHandlerTest extends AbstractHandlerTest {
    private final ListRecordsHandler underTest = new ListRecordsHandler(aContext(), theRepository());
    
    @Test
    public void noEmptyOrNullToken () {
        ResumptionToken.Value subject = new ResumptionToken.ValueBuilder().build();
        assertThrows(InternalOAIException.class, () -> underTest.handle(subject));
        assertThrows(InternalOAIException.class, () -> underTest.handle((ResumptionToken.Value) null));
    }
    
    @Test
    public void metadataPrefixIsMandatory () {
        assertThrows(CannotDisseminateFormatException.class, () -> underTest.handle(
            new ResumptionToken.ValueBuilder().withOffset(0).build()
        ));
    }
    
    @Test
    public void cannotDisseminateFormat() {
        theItemRepository().withItem(randomItem().withIdentifier("1"));
        aContext().withMetadataFormat(EXISTING_METADATA_FORMAT, MetadataFormat.identity());
        assertThrows(CannotDisseminateFormatException.class,
            () -> underTest.handle(
                new ResumptionToken.ValueBuilder()
                    .withMetadataPrefix("abcd")
                    .build())
        );
    }
    
    @Test
    public void doesNotSupportSets() {
        theSetRepository().doesNotSupportSets();
        assertThrows(DoesNotSupportSetsException.class,
            () -> underTest.handle(
                new ResumptionToken.ValueBuilder()
                    .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                    .withSetSpec("hello")
                    .build())
        );
    }
    
    @Test
    public void responseWithoutItems()  {
        assertThrows(NoMatchesException.class,
            () -> underTest.handle(
                new ResumptionToken.ValueBuilder()
                    .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                    .build())
        );
    }

    @Test
    void validResponse() throws Exception {
        theItemRepository().withRandomItems(10);
        String result = write(underTest.handle(
            new ResumptionToken.ValueBuilder()
                .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                .build()
        ));
        
        assertThat(result, xPath("count(//record)", asInteger(equalTo(10))));
    }
    
    @Test
    public void validResponseWithOnlyOnePage() throws Exception {
        theRepositoryConfiguration().withMaxListSets(100);
        theItemRepository().withRandomItems(10);
        ListRecords handle = underTest.handle(
            new ResumptionToken.ValueBuilder()
                .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                .build()
        );
        String result = write(handle);
        
        assertThat(result, xPath("count(//record)", asInteger(equalTo(10))));
        assertThat(result, not(hasXPath("//resumptionToken")));
    }
    
    @Test
    public void firstPageOfValidResponseWithTwoPages() throws Exception {
        theRepositoryConfiguration().withMaxListRecords(5);
        theItemRepository().withRandomItems(10);
        ListRecords handle = underTest.handle(
            new ResumptionToken.ValueBuilder()
                .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                .build()
        );
        String result = write(handle);
        
        assertThat(result, xPath("count(//record)", asInteger(equalTo(5))));
        assertThat(result, hasXPath("//resumptionToken"));
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
    
        String result = write(underTest.handle(
            new ResumptionToken.ValueBuilder()
                .withMetadataPrefix("custom")
                .build()
        ));
        
        assertThat(result, xPath("count(//record)", asInteger(equalTo(11))));
        assertThat(result, HasXPathMatcher.hasXPath("//record/metadata/testdata"));
    }
    
    @Test
    void responseWithItemHavingMetadataAttributes() throws Exception {
        // given
        theItemRepository().withItem(
            randomItem()
                .withDeleted(false)
                .withIdentifier("attributes")
                .withMetadata(
                    new Metadata(new EchoElement("<test>I have Attributes!</test>"))
                        .withAttribute("test","foobar"))
        );
        RepositoryConfiguration configuration = RepositoryConfiguration.defaults().withEnableMetadataAttributes(true);
        aContext().withMetadataFormat("custom", MetadataFormat.identity());
        ListRecords handle = underTest.handle(
            new ResumptionToken.ValueBuilder()
                .withMetadataPrefix("custom")
                .build()
        );
        
        // when
        String result = XmlWriter.toString(handle, configuration);
        
        // then
        assertThat(result, HasXPathMatcher.hasXPath("//metadata/@test"));
        assertThat(result, xPath("//metadata/@test", is(equalTo("foobar"))));
    }
}
