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
import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.IdDoesNotExistException;
import io.gdcc.xoai.dataprovider.handlers.helpers.MetadataHelper;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.results.Record;
import io.gdcc.xoai.model.oaipmh.results.record.About;
import io.gdcc.xoai.model.oaipmh.results.record.Header;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import io.gdcc.xoai.model.oaipmh.verbs.GetRecord;

import java.util.Optional;


public final class GetRecordHandler extends VerbHandler<GetRecord> {
    public GetRecordHandler(Context context, Repository repository) {
        super(context, repository);
    }
    
    @Override
    public GetRecord handle(final Request request) throws HandlerException {
        // Get the metadata format or throw errors
        String requestedFormat = request.getMetadataPrefix()
            .orElseThrow(() -> new CannotDisseminateFormatException("Missing required argument 'metadataPrefix'"));
        MetadataFormat format = Optional.ofNullable(getContext().formatForPrefix(requestedFormat))
            .orElseThrow(() -> new CannotDisseminateFormatException("Format '" + requestedFormat + "' not applicable in this context"));

        // Retrieve the item from our source repository, indicating the metadata format to enable prefilled metadata
        String identifier = request.getIdentifier().orElseThrow(IdDoesNotExistException::new);
        Item item = getRepository().getItemRepository().getItem(identifier, format);
        
        // Check the item for existence, filter with context and format conditions
        if (item == null)
            throw new IdDoesNotExistException();
        if (! getContext().isItemShown(item))
            throw new IdDoesNotExistException("This context does not include this item");
        if (! format.isItemShown(item))
            throw new CannotDisseminateFormatException("Format " + format.getPrefix() + " not applicable to this item");
    
        // Construct the OAI PMH <result> model to be filled with data from the repository
        Header header = new Header();
        Record record = new Record().withHeader(header);
        GetRecord result = new GetRecord(record);

        // Build the <header> part of the response
        header.withIdentifier(item.getIdentifier());
        header.withDatestamp(item.getDatestamp());

        // Add set specifications containing this item.
        // Start with the sets from the context by checking if they contain the item
        getContext().getSets().stream()
            .filter(set -> set.isItemShown(item))
            .forEach(set -> header.withSetSpec(set.getSpec()));
        
        for (Set set : item.getSets())
            header.withSetSpec(set.getSpec());

        // No <metadata> or <about> may be present if this item is deleted
        if (item.isDeleted()) {
            header.withStatus(Header.Status.DELETED);
        } else {
            // Next up: <metadata> response part. Skip the pipeline on request by the source.
            // Skip the metadata transformation on request by the source repository.
            Metadata metadata = item.getMetadata();
            if (! metadata.needsProcessing())
                record.withMetadata(metadata);
            else
                record.withMetadata(MetadataHelper.process(metadata, format, getContext()));
            
            // Last add the <about> section if present (protocol spec says: optional and repeatable)
            for (About about : item.getAbout())
                record.withAbout(about);
        }
        return result;
    }
}
