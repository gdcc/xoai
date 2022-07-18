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
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMetadataFormatsException;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.repository.ItemRepository;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.verbs.ListMetadataFormats;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ListMetadataFormatsHandler extends VerbHandler<ListMetadataFormats> {
    private final ItemRepository itemRepository;

    public ListMetadataFormatsHandler(Context context, Repository repository) {
        super(context, repository);
        itemRepository = repository.getItemRepository();

        // Static validation
        if (getContext().getMetadataFormats() == null
                || getContext().getMetadataFormats().isEmpty())
            throw new InternalOAIException("The context must expose at least one metadata format");
    }

    @Override
    public ListMetadataFormats handle(Request request) throws HandlerException {
        final List<MetadataFormat> formats = new ArrayList<>();

        // This verb allows to specify an identifier to return the available metadata formats for
        // this
        // particular item
        Optional<String> identifier = request.getIdentifier();
        if (identifier.isPresent()) {
            ItemIdentifier item = itemRepository.getItemIdentifier(identifier.get());

            // Lookup the formats available from the context. When the metadata formats registered
            // within
            // the context
            // do not define any condition, this will be a simple copy of all formats. When items
            // might
            // have not every
            // format available, it is up to the implementing application to provide a suiting
            // condition
            // to lookup
            // supported formats for the item!
            formats.addAll(getContext().formatFor(item));
        } else {
            formats.addAll(getContext().getMetadataFormats());
        }

        // If no formats could be found, send an error message
        if (formats.isEmpty()) {
            throw new NoMetadataFormatsException();
        }

        // Create the response
        ListMetadataFormats result = new ListMetadataFormats();
        formats.forEach(format -> result.withMetadataFormat(format.toOAIPMH()));
        return result;
    }
}
