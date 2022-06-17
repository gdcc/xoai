/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package io.gdcc.xoai.dataprovider.repository;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.exceptions.handler.CannotDisseminateFormatException;
import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.IdDoesNotExistException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import java.io.InputStream;
import java.util.List;

/**
 * This class wraps the data source of items.
 *
 * @author Development @ Lyncode
 * @version 3.1.0
 */
public interface ItemRepository {
    /**
     * Gets an item from the data source without {@link Metadata}.
     *
     * @param identifier Unique identifier of the item
     * @return An {@link ItemIdentifier} to work with
     * @throws IdDoesNotExistException when the item does not exist
     * @throws InternalOAIException In case source internal errors happen
     * @see <a
     *     href="client://www.openarchives.org/OAI/openarchivesprotocol.html#UniqueIdentifier">Unique
     *     identifier definition</a>
     */
    ItemIdentifier getItem(String identifier) throws IdDoesNotExistException;

    /**
     * Gets an item from the data source, but indicate the metadata format we are seeking. This may
     * be used to return an {@link Item} already containing {@link Metadata}, which makes {@link
     * io.gdcc.xoai.dataprovider.handlers.GetRecordHandler} and {@link
     * io.gdcc.xoai.dataprovider.handlers.ListRecordsHandler} skip the build of such to compile the
     * reply.
     *
     * @see Metadata#needsProcessing()
     * @see Metadata#copyFromStream(InputStream)
     * @see io.gdcc.xoai.xml.CopyElement
     * @param identifier Unique identifier of the item
     * @return An {@link Item} to work with, probably containing {@link Metadata}
     * @throws IdDoesNotExistException In case there is no record within the source matching the
     *     identifier
     * @throws CannotDisseminateFormatException In case the item does not have metadata for the
     *     requested format
     * @throws InternalOAIException In case source internal errors happen
     * @see <a
     *     href="client://www.openarchives.org/OAI/openarchivesprotocol.html#UniqueIdentifier">Unique
     *     identifier definition</a>
     */
    Item getItem(String identifier, MetadataFormat format) throws HandlerException;

    /**
     * Gets a (paged) list of identifiers. The metadata prefix parameter is internally converted to
     * a list of filters. That is, when configuring XOAI, it is possible to associate to each
     * metadata format a list of filters.
     *
     * @param filters List of Filters, see <a
     *     href="https://github.com/lyncode/xoai/wiki/XOAI-Data-Provider-Architecture">details</a>
     * @param metadataFormat The intended {@link MetadataFormat} the repository shall return (may
     *     also be contained within the scoped filter for {@link
     *     io.gdcc.xoai.dataprovider.filter.Scope#MetadataFormat})
     * @param maxResponseLength The maximum count of identifiers to return (paged results). Is
     *     always > 0 from {@link RepositoryConfiguration}, enforced by {@link
     *     io.gdcc.xoai.dataprovider.handlers.IdentifyHandler}
     * @param resumptionToken A resumption token (element value) either freshly created from a new
     *     {@link io.gdcc.xoai.model.oaipmh.Request} or by loading it from a clients request
     *     argument. Contains information about offset (for paging), from/until dates and sets. You
     *     may assume any information in this parameter is verified. Will not be null and instances
     *     are immutable.
     * @return List of identifiers
     * @throws HandlerException In case no match can be found or something else related to the
     *     request goes wrong. Repository internal errors not triggered by a false client request
     *     must throw an internal error!
     * @throws InternalOAIException In case source internal errors happen
     * @see <a
     *     href="client://www.openarchives.org/OAI/openarchivesprotocol.html#ListIdentifiers">List
     *     Identifiers Definition</a>
     */
    ResultsPage<ItemIdentifier> getItemIdentifiers(
            final List<ScopedFilter> filters,
            final MetadataFormat metadataFormat,
            final int maxResponseLength,
            final ResumptionToken.Value resumptionToken)
            throws HandlerException;

    /**
     * Gets a (paged) list of items. The metadata prefix parameter is internally converted to a list
     * of filters. That is, when configuring XOAI, it is possible to associate to each metadata
     * format a list of filters.
     *
     * @param filters List of Filters <a
     *     href="https://github.com/lyncode/xoai/wiki/XOAI-Data-Provider-Architecture">details</a>
     * @param metadataFormat The intended {@link MetadataFormat} the repository shall return (may
     *     also be contained within the scoped filter for {@link
     *     io.gdcc.xoai.dataprovider.filter.Scope#MetadataFormat})
     * @param maxResponseLength The maximum count of identifiers to return (paged results). Is
     *     always > 0 from {@link RepositoryConfiguration}, enforced by {@link
     *     io.gdcc.xoai.dataprovider.handlers.IdentifyHandler}
     * @param resumptionToken A resumption token (element value) either freshly created from a new
     *     {@link io.gdcc.xoai.model.oaipmh.Request} or by loading it from a clients request
     *     argument. Contains information about offset (for paging), from/until dates and sets. You
     *     may assume any information in this parameter is verified. Will not be null and instances
     *     are immutable.
     * @return List of Items
     * @throws HandlerException In case no match can be found or something else related to the
     *     request goes wrong. Repository internal errors not triggered by a false client request
     *     must throw an internal error!
     * @throws InternalOAIException In case source internal errors happen
     * @see <a href="client://www.openarchives.org/OAI/openarchivesprotocol.html#ListRecords">List
     *     Records Definition</a>
     */
    ResultsPage<Item> getItems(
            final List<ScopedFilter> filters,
            final MetadataFormat metadataFormat,
            final int maxResponseLength,
            final ResumptionToken.Value resumptionToken)
            throws HandlerException;
}
