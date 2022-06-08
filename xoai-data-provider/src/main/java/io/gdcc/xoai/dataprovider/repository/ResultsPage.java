package io.gdcc.xoai.dataprovider.repository;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;

import java.util.List;
import java.util.Objects;

/**
 * A mapping POJO carrying a list of elements returned from application repositories.
 * Contains also information about the total number of results. Represents a page of
 * results only - indicates the presence of more data via a boolean flag.
 *
 * This item or sections of it might be used to create a cache of results within the application.
 *
 * @param <T> The type of results. See {@link io.gdcc.xoai.dataprovider.model.Item},
 *            {@link io.gdcc.xoai.dataprovider.model.ItemIdentifier} and {@link io.gdcc.xoai.dataprovider.model.Set}
 */
public final class ResultsPage<T> {
    
    private final boolean hasMore;
    private final List<T> resultsList;
    private final int totalResults;
    private final ResumptionToken.Value requestToken;
    
    /**
     * Create a page of results. Will verify nonsense combinations of parameters.
     *
     * @param requestToken The token a client (would) send in a request (might be precalculated to cache pages)
     * @param hasMoreResults A flag to indicate if a client may ask for more results by sending the response token value
     * @param resultsList The actual results for this page
     * @param totalResults The number of all results (sum of pages)
     * @throws NullPointerException when token or results are null
     * @throws InternalOAIException when semantic values of parameters don't make sense
     */
    public ResultsPage(ResumptionToken.Value requestToken, boolean hasMoreResults, List<T> resultsList, int totalResults) {
        Objects.requireNonNull(resultsList, "List of result may be empty but not null");
        
        if (totalResults < 0) {
            throw new InternalOAIException("Number of results may not be negative " + totalResults);
        }
        if (resultsList.size() > totalResults) {
            throw new InternalOAIException("Number of results (" +totalResults+ ") may not be smaller than the list size " + resultsList.size());
        }
        if (resultsList.isEmpty() && totalResults > 0 ) {
            throw new InternalOAIException("Number of results (" +totalResults+ ") may not be larger 0 with an empty result list");
        }
        if (resultsList.isEmpty() && hasMoreResults) {
            throw new InternalOAIException("Cannot indicate more results and have an empty result list");
        }
    
        Objects.requireNonNull(requestToken, "Resumption token may not be null");
        if (requestToken.isEmpty()) {
            throw new InternalOAIException("Result may not contain an empty resumption token");
        }
        
        this.requestToken = requestToken;
        this.hasMore = hasMoreResults;
        this.resultsList = resultsList;
        this.totalResults = totalResults;
    }
    
    public boolean hasMore() {
        return hasMore;
    }
    
    public List<T> getList() {
        return List.copyOf(resultsList);
    }
    
    public int getTotal() {
        return this.totalResults;
    }
    
    /**
     * Access the resumption token of this request (this is the token the client sent us)
     * @return The token the client sent us before
     */
    public ResumptionToken.Value getRequestTokenValue() {
        return this.requestToken;
    }
    
    /**
     * Create a new resumption token value from this result. Will either carry the details and have the offset
     * shifted with the number of results or be empty to indicate this is the last page of results.
     *
     * The token value is independent of whether this was an initial request (offset would be 0).
     * Case 1) No more results: return empty token value
     * Case 2) More results: return token value with number of results in this page added to former offset
     *
     * @return The resumption token value to be sent to the client
     */
    public ResumptionToken.Value getResponseTokenValue() {
        return hasMore()
            ? requestToken.next(resultsList.size())
            : new ResumptionToken.ValueBuilder().build();
    }
    
    /**
     * Create a new OAI-PMH response token model instance, already adding the cursor and totalResults attributes
     * and the appropriate token value, encoding this pages state of data retrieval.
     *
     * The token is independent of whether this was an initial request, see {@link #getResponseTokenValue()}
     * We simply add the cursor attribute (which is the offset encoded in the request token value) and the total
     * number of results attribute (given during page creation).
     *
     * @return A <code>&lt;resumptionToken&gt;</code> to return to the harvesting client.
     */
    public ResumptionToken getResponseToken() {
        return new ResumptionToken(getResponseTokenValue())
            .withCompleteListSize(totalResults)
            .withCursor(requestToken.getOffset());
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResultsPage)) return false;
        ResultsPage<?> that = (ResultsPage<?>) o;
        return hasMore == that.hasMore && totalResults == that.totalResults && resultsList.equals(that.resultsList) && requestToken.equals(that.requestToken);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(hasMore, resultsList, totalResults, requestToken);
    }
}
