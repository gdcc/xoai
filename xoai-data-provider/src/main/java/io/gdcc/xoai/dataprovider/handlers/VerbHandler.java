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
import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.NoMatchesException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.Verb;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class VerbHandler<T extends Verb> {
    private final Context context;
    private final Repository repository;

    protected VerbHandler (Context context, Repository repository) {
        this.context = context;
        this.repository = repository;
    }

    protected Context getContext() {
        return context;
    }
    protected Repository getRepository() {
        return repository;
    }
    protected RepositoryConfiguration getConfiguration() {
        return repository.getConfiguration();
    }
    
    /**
     * Check for set support if set argument is present and check set existence (in both set repository and
     * context's static defined sets!).
     *
     * @param token The resumption token from the client
     * @throws HandlerException When sets aren't supported or the set does not exist.
     */
    protected void verifySet(ResumptionToken.Value token) throws HandlerException {
        String requestedSet = token.getSetSpec();
        if (requestedSet != null) {
            if (!getRepository().getSetRepository().supportSets())
                throw new DoesNotSupportSetsException();
            else if (!getRepository().getSetRepository().exists(requestedSet) && !getContext().hasSet(requestedSet))
                throw new NoMatchesException("Requested set '" + requestedSet + "' does not exist");
        }
    }
    
    protected MetadataFormat verifyFormat(ResumptionToken.Value token) throws HandlerException {
        // Get the metadata format or throw errors
        final String requestedFormat = Optional.ofNullable(token.getMetadataPrefix())
            .orElseThrow(() -> new CannotDisseminateFormatException("Missing required argument 'metadataPrefix'"));
        return Optional.ofNullable(getContext().formatForPrefix(requestedFormat))
            .orElseThrow(() -> new CannotDisseminateFormatException("Format '" + requestedFormat + "' not applicable in this context"));
    }
    
    protected List<ScopedFilter> createFilters(ResumptionToken.Value token, MetadataFormat format) {
        // Create empty filter list
        final List<ScopedFilter> filters = new ArrayList<>();
        
        // Create the filter from the context and add to list (will default to being transparent)
        filters.add(getContext().getScopedFilter());
        
        // Add the metadata formats condition to the list of filters (will default to being transparent)
        filters.add(format.getScopedFilter());
        
        // Add the sets condition if the requested set is contained within the context
        Optional.ofNullable(token.getSetSpec())
            .flatMap(setSpec -> getContext().getSet(setSpec))
            .map(Set::getScopedFilter)
            .ifPresent(filters::add);
        
        return filters;
    }
    
    /**
     * Handle an OAI-PMH {@link Request} for a {@link Verb} not making use of paged results and resumption tokens.
     *
     * Note: handlers not support this type of method may throw {@link io.gdcc.xoai.dataprovider.exceptions.InternalOAIException}
     *       to indicate lacking support. This should only happen when implementing applications override
     *       the handlers.
     *
     * @param request The request to work on
     * @return The OAI-PMH {@link Verb} response
     * @throws HandlerException When the request does not create a valid OAI-PMH response, triggering an error message.
     * @throws io.gdcc.xoai.dataprovider.exceptions.InternalOAIException When an implementation internal error happens
     *                                                                   which has a root cause independent from the
     *                                                                   clients request.
     */
    public T handle(final Request request) throws HandlerException {
        throw new InternalOAIException("Unsupported method for this handler!");
    }
    
    /**
     * Handle an OAI-PMH {@link Request} using a {@link Verb} accepting a resumption token, dealing with paged results.
     * If the client did not include a token, create a fake one via {@link ResumptionToken.ValueBuilder#build(Request)}.
     *
     * Note: handlers not support this type of method may throw {@link io.gdcc.xoai.dataprovider.exceptions.InternalOAIException}
     *       to indicate lacking support. This should only happen when implementing applications override the handlers.
     *
     * @param token   The token to start crafting response data from (defined offset, dates, set and metadata prefix).
     * @return The OAI-PMH {@link Verb} response
     * @throws HandlerException When the request does not create a valid OAI-PMH response, triggering an error message.
     * @throws io.gdcc.xoai.dataprovider.exceptions.InternalOAIException When an implementation internal error happens
     *                                                                   which has a root cause independent of the
     *                                                                   clients request.
     */
    public T handle(final ResumptionToken.Value token) throws HandlerException {
        throw new InternalOAIException("Unsupported method for this handler!");
    }
}
