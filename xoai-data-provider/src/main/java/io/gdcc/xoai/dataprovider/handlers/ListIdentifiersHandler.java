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
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.ResultsPage;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.results.record.Header;
import io.gdcc.xoai.model.oaipmh.verbs.ListIdentifiers;

import java.util.List;


public class ListIdentifiersHandler extends VerbHandler<ListIdentifiers> {

    private final ItemRepository itemRepository;
    
    public ListIdentifiersHandler(Context context, Repository repository) {
        super(context, repository);
        this.itemRepository = repository.getItemRepository();
    }
    
    @Override
    public ListIdentifiers handle(ResumptionToken.Value token) throws HandlerException {
        
        if (token == null || token.isEmpty())
            throw new InternalOAIException("Resumption token must not be null or empty - check your implementation!");
    
        // Check for set support if set argument is present (skip lot's of CPU cycles if not)
        verifySet(token);
        
        // Get the format
        MetadataFormat format = verifyFormat(token);
        
        // Create filters
        final List<ScopedFilter> filters = createFilters(token, format);
        
        // Execute the lookup with the repository
        ResultsPage<ItemIdentifier> results =
            itemRepository.getItemIdentifiers(
                filters,
                format,
                getConfiguration().getMaxListIdentifiers(),
                token);
    
        // If no results present, send error message
        if (results.getTotal() == 0)
            throw new NoMatchesException();

        final ListIdentifiers response = new ListIdentifiers();
        // TODO make the getHeaders an unmodifiable list and add withHeader() method to ListIdentifiers
        results.getList().forEach(
            item -> response.getHeaders().add(createHeader(item, format))
        );
        
        // Create the OAIPMH model for the <resumptionToken>
        ResumptionToken tokenResponse = results.getResponseToken();
        // TODO: add expiration date here, based on repository configuration

        return response.withResumptionToken(tokenResponse);
    }


    private Header createHeader(ItemIdentifier itemIdentifier, MetadataFormat format) {
        if (!itemIdentifier.isDeleted() && ! format.isItemShown(itemIdentifier))
            throw new InternalOAIException("The item repository is currently providing items which cannot be disseminated with format "+format.getPrefix());

        Header header = new Header();
        header.withDatestamp(itemIdentifier.getDatestamp());
        header.withIdentifier(itemIdentifier.getIdentifier());
        if (itemIdentifier.isDeleted())
            header.withStatus(Header.Status.DELETED);

        for (Set set : getContext().getSets())
            if (set.isItemShown(itemIdentifier))
                header.withSetSpec(set.getSpec());

        for (Set set : itemIdentifier.getSets())
            header.withSetSpec(set.getSpec());

        return header;
    }
}
