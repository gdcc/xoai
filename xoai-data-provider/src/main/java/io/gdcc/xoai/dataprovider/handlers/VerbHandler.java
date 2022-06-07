/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.Verb;

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
     * Handle an OAI-PMH {@link Request} without a resumption token.
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
    public abstract T handle(final Request request) throws HandlerException;
    
    /**
     * Handle an OAI-PMH {@link Request} with an optional response token.
     *
     * * Note: handlers not support this type of method may throw {@link io.gdcc.xoai.dataprovider.exceptions.InternalOAIException}
     *         to indicate lacking support. This should only happen when implementing applications override
     *         the handlers.
     *
     * @param request The request to work on
     * @param token   The token to start crafting response data from (defined offset, dates and metadata prefix).
     *                May be null in case of handlers not supporting resumption tokens.
     * @return The OAI-PMH {@link Verb} response
     * @throws HandlerException When the request does not create a valid OAI-PMH response, triggering an error message.
     * @throws io.gdcc.xoai.dataprovider.exceptions.InternalOAIException When an implementation internal error happens
     *                                                                   which has a root cause independent from the
     *                                                                   clients request.
     */
    public abstract T handle(final Request request, final ResumptionToken.Value token) throws HandlerException;
}
