/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.serviceprovider;

import static io.gdcc.xoai.dataprovider.model.InMemoryItem.randomItem;
import static io.gdcc.xoai.dataprovider.model.MetadataFormat.identity;
import static io.gdcc.xoai.model.oaipmh.DeletedRecord.PERSISTENT;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.gdcc.xoai.dataprovider.filter.Condition;
import io.gdcc.xoai.dataprovider.filter.Filter;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.model.oaipmh.results.MetadataFormat;
import io.gdcc.xoai.model.oaipmh.verbs.Identify;
import io.gdcc.xoai.serviceprovider.exceptions.CannotDisseminateFormatException;
import io.gdcc.xoai.serviceprovider.exceptions.IdDoesNotExistException;
import io.gdcc.xoai.serviceprovider.exceptions.NoSetHierarchyException;
import io.gdcc.xoai.serviceprovider.parameters.GetRecordParameters;
import io.gdcc.xoai.serviceprovider.parameters.ListIdentifiersParameters;
import io.gdcc.xoai.serviceprovider.parameters.ListMetadataParameters;
import io.gdcc.xoai.serviceprovider.parameters.ListRecordsParameters;
import java.util.Iterator;
import org.junit.jupiter.api.Test;

public class ServiceProviderTest extends AbstractServiceProviderTest {
    private final ServiceProvider underTest = new ServiceProvider(theContext());

    @Test
    public void validIdentifyResponse() throws Exception {
        theDataRepositoryConfiguration()
                .asTemplate()
                .withRepositoryName("NAME")
                .withDeleteMethod(PERSISTENT)
                .build()
                .inject(theDataRepository());
        Identify identify = underTest.identify();
        assertThat(identify.getRepositoryName(), equalTo("NAME"));
        assertThat(identify.getDeletedRecord(), equalTo(PERSISTENT));
    }

    @Test
    public void validListMetadataFormatsResponse() throws Exception {
        Iterator<MetadataFormat> metadataFormatIterator = underTest.listMetadataFormats();

        assertThat(metadataFormatIterator.hasNext(), is(true));
        MetadataFormat metadataFormat = metadataFormatIterator.next();
        assertThat(metadataFormat.getMetadataPrefix(), equalTo(FORMAT));
    }

    @Test
    public void recordNotFoundForListMetadataFormats() throws Exception {
        assertThrows(
                IdDoesNotExistException.class,
                () ->
                        underTest.listMetadataFormats(
                                ListMetadataParameters.request().withIdentifier("asd")));
    }

    @Test
    public void recordNotFoundForGetRecord() throws Exception {
        assertThrows(
                IdDoesNotExistException.class,
                () ->
                        underTest.getRecord(
                                GetRecordParameters.request()
                                        .withIdentifier("asd")
                                        .withMetadataFormatPrefix(FORMAT)));
    }

    @Test
    public void recordDoesNotSupportFormatForGetRecord() throws Exception {
        theDataProviderContext().withMetadataFormat(FORMAT, identity(), alwaysFalseCondition());
        theDataItemRepository().withItem(randomItem().withIdentifier("asd").withSet("one"));
        assertThrows(
                CannotDisseminateFormatException.class,
                () ->
                        underTest.getRecord(
                                GetRecordParameters.request()
                                        .withIdentifier("asd")
                                        .withMetadataFormatPrefix(FORMAT)));
    }

    @Test
    public void listSetsWithNoSupportForSets() throws Exception {
        theDataSetRepository().doesNotSupportSets();
        assertThrows(NoSetHierarchyException.class, underTest::listSets);
    }

    @Test
    public void listSetsWithNoSets() throws Exception {
        assertThat(underTest.listSets().hasNext(), is(false));
    }

    @Test
    public void listSetsWithSetsOnePage() throws Exception {
        theDataSetRepository().withRandomSets(5);
        assertThat(count(underTest.listSets()), equalTo(5));
    }

    @Test
    public void listSetsWithSetsTwoPages() throws Exception {
        theDataRepositoryConfiguration()
                .asTemplate()
                .withMaxListSets(5)
                .build()
                .inject(theDataRepository());
        theDataSetRepository().withRandomSets(10);
        assertThat(count(underTest.listSets()), equalTo(10));
    }

    @Test
    public void listIdentifiersWithNoItems() throws Exception {
        assertThat(
                underTest
                        .listIdentifiers(
                                ListIdentifiersParameters.request().withMetadataPrefix(FORMAT))
                        .hasNext(),
                is(false));
    }

    @Test
    public void listIdentifiersWithOnePage() throws Exception {
        theDataItemRepository().withRandomItems(5);
        assertThat(
                count(
                        underTest.listIdentifiers(
                                ListIdentifiersParameters.request().withMetadataPrefix(FORMAT))),
                equalTo(5));
    }

    @Test
    public void listIdentifiersWithTwoPages() throws Exception {
        theDataRepositoryConfiguration()
                .asTemplate()
                .withMaxListIdentifiers(5)
                .build()
                .inject(theDataRepository());
        theDataItemRepository().withRandomItems(10);
        assertThat(
                count(
                        underTest.listIdentifiers(
                                ListIdentifiersParameters.request().withMetadataPrefix(FORMAT))),
                equalTo(10));
    }

    @Test
    public void listRecordsWithNoItems() throws Exception {
        assertThat(
                underTest
                        .listRecords(ListRecordsParameters.request().withMetadataPrefix(FORMAT))
                        .hasNext(),
                is(false));
    }

    @Test
    public void listRecordsWithOnePage() throws Exception {
        theDataItemRepository().withRandomItems(5);
        assertThat(
                count(
                        underTest.listRecords(
                                ListRecordsParameters.request().withMetadataPrefix(FORMAT))),
                equalTo(5));
    }

    @Test
    public void listRecordsWithTwoPages() throws Exception {
        theDataRepositoryConfiguration()
                .asTemplate()
                .withMaxListRecords(5)
                .build()
                .inject(theDataRepository());
        theDataItemRepository().withRandomItems(10);
        assertThat(
                count(
                        underTest.listRecords(
                                ListRecordsParameters.request().withMetadataPrefix(FORMAT))),
                equalTo(10));
    }

    private int count(Iterator<?> iterator) {
        int i = 0;
        while (iterator.hasNext()) {
            iterator.next();
            i++;
        }
        return i;
    }

    private Condition alwaysFalseCondition() {
        return new Condition() {
            @Override
            public Filter getFilter() {
                return new Filter() {
                    @Override
                    public boolean isItemShown(ItemIdentifier item) {
                        return false;
                    }
                };
            }
        };
    }
}
