/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.exceptions.handler.DoesNotSupportSetsException;
import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.Set;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.ResultsPage;
import io.gdcc.xoai.dataprovider.repository.SetRepository;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.ListSets;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class ListSetsHandler extends VerbHandler<ListSets> {
    private final SetRepository setRepository;

    public ListSetsHandler(Context context, Repository repository) {
        super(context, repository);
        this.setRepository = getRepository().getSetRepository();
    }
    
    @Override
    public ListSets handle(Request request) throws HandlerException {
        throw new InternalOAIException("Method ListSets.handle not allowed without resumption token");
    }

    @Override
    public ListSets handle(Request request, ResumptionToken.Value token) throws HandlerException {
    
        if (token == null || token.isEmpty())
            throw new InternalOAIException("Resumption token must not be null or empty - check your implementation!");
        
        if (!setRepository.supportSets()) {
            throw new DoesNotSupportSetsException();
        }
    
        // Execute the lookup with the repository, get all sets
        List<Set> repositorySets = setRepository.getSets();
        List<Set> contextSets = getContext().getSets();
        
        int totalResults = repositorySets.size() + contextSets.size();
        int maxResults = getConfiguration().getMaxListSets();
    
        // Create an ordered stream of sets (as both are coming from lists, they are ordered by design)
        // and create a slice for the paginated result
        List<Set> pagedSetList = Stream
            .concat(repositorySets.stream(), contextSets.stream())
                .skip(token.getOffset())
                .limit(maxResults)
                .collect(Collectors.toUnmodifiableList());
        
        // Create the paged result
        ResultsPage<Set> results = new ResultsPage<>(
            token,
            // more results available when page size == maxlength - but only when this is not also the end of the list (edge case where maxlength is a multiple of total size)
            pagedSetList.size() == maxResults && totalResults != maxResults + token.getOffset(),
            pagedSetList,
            totalResults
        );
        
    
        final ListSets response = new ListSets();
        // TODO make the getSets an unmodifiable list and add withSet() method to ListSets
        results.getList().forEach(
            item -> response.getSets().add(item.toOAIPMH())
        );
    
        // Create the OAIPMH model for the <resumptionToken>
        ResumptionToken tokenResponse = results.getResponseToken();
        // TODO: add expiration date here, based on repository configuration
    
        return response.withResumptionToken(tokenResponse);
    }

}
