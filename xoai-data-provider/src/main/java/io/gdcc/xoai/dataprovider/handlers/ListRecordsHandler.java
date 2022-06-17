/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMatchesException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.handlers.helpers.MetadataHelper;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.ResultsPage;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.results.Record;
import io.gdcc.xoai.model.oaipmh.results.record.About;
import io.gdcc.xoai.model.oaipmh.results.record.Header;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import io.gdcc.xoai.model.oaipmh.verbs.ListRecords;
import java.util.List;
import java.util.stream.Stream;

public class ListRecordsHandler extends VerbHandler<ListRecords> {
    private final ItemRepository itemRepository;

    public ListRecordsHandler(Context context, Repository repository) {
        super(context, repository);
        this.itemRepository = repository.getItemRepository();
    }

    @Override
    public ListRecords handle(ResumptionToken.Value token) throws HandlerException {

        if (token == null || token.isEmpty())
            throw new InternalOAIException(
                    "Resumption token must not be null or empty - check your implementation!");

        // Check for set support if set argument is present (skip lot's of CPU cycles if not)
        verifySet(token);

        // Get the format
        MetadataFormat format = verifyFormat(token);

        // Create filters
        final List<ScopedFilter> filters = createFilters(token, format);

        // Execute the lookup with the repository
        ResultsPage<Item> results =
                itemRepository.getItems(
                        filters, format, getConfiguration().getMaxListRecords(), token);

        // If no results present, send error message
        if (results.getTotal() == 0) throw new NoMatchesException();

        final ListRecords response = new ListRecords();
        // TODO make the getHeaders an unmodifiable list and add withHeader() method to
        // ListIdentifiers
        results.getList().forEach(item -> response.withRecord(createRecord(item, format)));

        // Create the OAIPMH model for the <resumptionToken>
        results.getResponseToken(getConfiguration().getMaxListRecords())
                // TODO: add expiration date here, based on repository configuration
                .ifPresent(response::withResumptionToken);

        return response;
    }

    private Record createRecord(Item item, MetadataFormat format) {

        // Create the most basic result
        final Header header = new Header();
        final Record record = new Record().withHeader(header);
        header.withIdentifier(item.getIdentifier());
        header.withDatestamp(item.getDatestamp());

        // Lookup and add any sets to the records header
        Stream.concat(
                        // Start with the static sets from the context (checks for visibility)
                        getContext().getSetsForItem(item),
                        // Add the sets from the item itself
                        item.getSets().stream())
                .map(Set::getSpec)
                .forEach(header::withSetSpec);

        // Flag deletion status
        if (item.isDeleted()) header.withStatus(Header.Status.DELETED);

        // Non-deleted items have a <metadata> and <about> part
        if (!item.isDeleted()) {
            // Next up: <metadata> response part. Skip the pipeline on request by the source.
            // Skip the metadata transformation on request by the source repository.
            Metadata metadata = item.getMetadata();
            if (!metadata.needsProcessing()) {
                record.withMetadata(metadata);
            } else {
                record.withMetadata(MetadataHelper.process(metadata, format, getContext()));
            }

            // Last add the <about> section if present (protocol spec says: optional and repeatable)
            for (About about : item.getAbout()) {
                record.withAbout(about);
            }
        }

        return record;
    }
}
