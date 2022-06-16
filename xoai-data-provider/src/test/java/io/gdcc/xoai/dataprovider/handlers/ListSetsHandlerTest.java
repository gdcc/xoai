/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import static io.gdcc.xoai.dataprovider.model.Set.set;
import static io.gdcc.xoai.model.oaipmh.verbs.Verb.Type.ListSets;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.exceptions.handler.DoesNotSupportSetsException;
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMatchesException;
import io.gdcc.xoai.dataprovider.filter.Condition;
import io.gdcc.xoai.dataprovider.repository.InMemorySetRepository;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.ListSets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ListSetsHandlerTest extends AbstractHandlerTest {
    protected ListSetsHandler underTest;
    protected InMemorySetRepository setRepository;

    @BeforeEach
    public void setup() {
        this.setRepository = new InMemorySetRepository();
        theRepository().withSetRepository(setRepository);
        underTest = new ListSetsHandler(aContext(), theRepository());
    }

    @Test
    public void noEmptyOrNullToken() {
        ResumptionToken.Value subject = new ResumptionToken.ValueBuilder().build();
        assertThrows(InternalOAIException.class, () -> underTest.handle(subject));
        assertThrows(
                InternalOAIException.class, () -> underTest.handle((ResumptionToken.Value) null));
    }

    @Test
    public void doesNotSupportSets() {
        setRepository.doesNotSupportSets();
        assertThrows(
                DoesNotSupportSetsException.class,
                () ->
                        underTest.handle(
                                ResumptionToken.ValueBuilder.build(request().withVerb(ListSets))));
    }

    @Test
    public void emptyRepositoryShouldGiveNoMatches() throws Exception {
        assertThrows(
                NoMatchesException.class,
                () ->
                        underTest.handle(
                                ResumptionToken.ValueBuilder.build(request().withVerb(ListSets))));
    }

    @Test
    public void validResponseWithOnlyOnePage() throws Exception {
        theRepositoryConfiguration().withMaxListSets(100);
        setRepository.withRandomSets(10);
        ListSets handle =
                underTest.handle(ResumptionToken.ValueBuilder.build(request().withVerb(ListSets)));
        String result = write(handle);

        assertThat(result, xPath("count(//set)", asInteger(equalTo(10))));
        assertThat(result, not(hasXPath("//resumptionToken")));
    }

    @Test
    public void showsVirtualSetsFirst() throws Exception {
        setRepository.withSet("set", "hello");
        theContext().withSet(set("virtual").withName("new").withCondition(Condition.ALWAYS_FALSE));

        ListSets handle =
                underTest.handle(ResumptionToken.ValueBuilder.build(request().withVerb(ListSets)));
        String result = write(handle);

        assertThat(result, xPath("count(//set)", asInteger(equalTo(2))));
        assertThat(result, xPath("//set[1]/setSpec", equalTo("virtual")));
        assertThat(result, xPath("//set[2]/setSpec", equalTo("hello")));
    }

    @Test
    public void firstPageOfValidResponseWithTwoPages() throws Exception {
        theRepositoryConfiguration().withMaxListSets(5);
        setRepository.withRandomSets(10);
        ListSets handle =
                underTest.handle(ResumptionToken.ValueBuilder.build(request().withVerb(ListSets)));
        String result = write(handle);

        assertThat(result, xPath("count(//set)", asInteger(equalTo(5))));
        assertThat(result, hasXPath("//resumptionToken"));
    }

    @Test
    public void lastPageOfVResponseWithTwoPages() throws Exception {
        theRepositoryConfiguration().withMaxListSets(5);
        setRepository.withRandomSets(10);
        ListSets handle =
                underTest.handle(new ResumptionToken.ValueBuilder().withOffset(5).build());
        String result = write(handle);

        assertThat(result, xPath("count(//set)", asInteger(equalTo(5))));
        assertThat(result, xPath("//resumptionToken", is(equalTo(""))));
    }
}
