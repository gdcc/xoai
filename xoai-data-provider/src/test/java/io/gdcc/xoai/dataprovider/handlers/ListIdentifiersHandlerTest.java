/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import static io.gdcc.xoai.dataprovider.model.InMemoryItem.randomItem;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.exceptions.handler.CannotDisseminateFormatException;
import io.gdcc.xoai.dataprovider.exceptions.handler.DoesNotSupportSetsException;
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMatchesException;
import io.gdcc.xoai.dataprovider.filter.Condition;
import io.gdcc.xoai.dataprovider.model.InMemoryItem;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.ListIdentifiers;
import org.junit.jupiter.api.Test;

public class ListIdentifiersHandlerTest extends AbstractHandlerTest {
    private final ListIdentifiersHandler underTest =
            new ListIdentifiersHandler(aContext(), theRepository());

    @Test
    public void noEmptyOrNullToken() {
        ResumptionToken.Value subject = new ResumptionToken.ValueBuilder().build();
        assertThrows(InternalOAIException.class, () -> underTest.handle(subject));
        assertThrows(
                InternalOAIException.class, () -> underTest.handle((ResumptionToken.Value) null));
    }

    @Test
    public void metadataPrefixIsMandatory() {
        assertThrows(
                CannotDisseminateFormatException.class,
                () -> underTest.handle(new ResumptionToken.ValueBuilder().withOffset(0).build()));
    }

    @Test
    public void cannotDisseminateFormat() {
        theItemRepository().withItem(randomItem().withIdentifier("1"));
        aContext().withMetadataFormat(EXISTING_METADATA_FORMAT, MetadataFormat.identity());
        assertThrows(
                CannotDisseminateFormatException.class,
                () ->
                        underTest.handle(
                                new ResumptionToken.ValueBuilder()
                                        .withMetadataPrefix("abcd")
                                        .build()));
    }

    @Test
    public void doesNotSupportSets() {
        theSetRepository().doesNotSupportSets();
        assertThrows(
                DoesNotSupportSetsException.class,
                () ->
                        underTest.handle(
                                new ResumptionToken.ValueBuilder()
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                        .withSetSpec("hello")
                                        .build()));
    }

    @Test
    public void setDoesNotExist() {
        theSetRepository().withSet("test", "abcd");
        assertThrows(
                NoMatchesException.class,
                () ->
                        underTest.handle(
                                new ResumptionToken.ValueBuilder()
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                        .withSetSpec("hello")
                                        .build()));
    }

    @Test
    public void responseWithoutItems() {
        assertThrows(
                NoMatchesException.class,
                () ->
                        underTest.handle(
                                new ResumptionToken.ValueBuilder()
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                        .build()));
    }

    @Test
    public void responseWithItems() throws Exception {
        theItemRepository().withRandomItems(10);
        String result =
                write(
                        underTest.handle(
                                new ResumptionToken.ValueBuilder()
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                        .build()));

        assertThat(result, xPath("count(//header)", asInteger(equalTo(10))));
    }

    @Test
    public void responseWithItemHavingSet() throws Exception {
        InMemoryItem item = InMemoryItem.randomItem();
        theItemRepository().withItem(item);

        String setSpec = item.getSets().get(0).getSpec();
        theSetRepository().withSet("test", setSpec);

        String result =
                write(
                        underTest.handle(
                                new ResumptionToken.ValueBuilder()
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                        .withSetSpec(setSpec)
                                        .build()));

        assertThat(result, xPath("count(//header)", asInteger(equalTo(1))));
        assertThat(result, xPath("//header/setSpec/text()", equalTo(setSpec)));
    }

    @Test
    public void responseWithItemHavingVirtualSet() throws Exception {
        InMemoryItem item = InMemoryItem.randomItem();
        theItemRepository().withItem(item);

        String setSpec = "virtualset";
        theContext().withSet(new Set(setSpec).withCondition(Condition.ALWAYS_TRUE));

        String result =
                write(
                        underTest.handle(
                                new ResumptionToken.ValueBuilder()
                                        .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                        .build()));

        assertThat(result, xPath("count(//header)", asInteger(equalTo(1))));
        assertThat(result, xPath("//header/setSpec/text()", equalTo(setSpec)));
    }

    @Test
    public void validResponseWithOnlyOnePage() throws Exception {
        theRepository()
                .getConfiguration()
                .asTemplate()
                .withMaxListIdentifiers(100)
                .build()
                .inject(theRepository());
        theItemRepository().withRandomItems(10);
        ListIdentifiers handle =
                underTest.handle(
                        new ResumptionToken.ValueBuilder()
                                .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                .build());
        String result = write(handle);

        assertThat(result, xPath("count(//header)", asInteger(equalTo(10))));
        assertThat(result, not(hasXPath("//resumptionToken")));
    }

    @Test
    public void firstPageOfValidResponseWithTwoPages() throws Exception {
        theRepository()
                .getConfiguration()
                .asTemplate()
                .withMaxListIdentifiers(5)
                .build()
                .inject(theRepository());
        theItemRepository().withRandomItems(10);
        ListIdentifiers handle =
                underTest.handle(
                        new ResumptionToken.ValueBuilder()
                                .withMetadataPrefix(EXISTING_METADATA_FORMAT)
                                .build());
        String result = write(handle);

        assertThat(result, xPath("count(//header)", asInteger(equalTo(5))));
        assertThat(result, hasXPath("//resumptionToken"));
    }
}
