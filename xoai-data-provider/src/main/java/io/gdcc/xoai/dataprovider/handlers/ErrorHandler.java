/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.request.RequestBuilder;
import io.gdcc.xoai.exceptions.OAIException;
import io.gdcc.xoai.model.oaipmh.Error;
import io.gdcc.xoai.model.oaipmh.OAIPMH;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.services.api.DateProvider;
import java.util.Objects;

public class ErrorHandler {

    private final RepositoryConfiguration configuration;

    public ErrorHandler(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    /**
     * Create an OAI-PMH response with an emtpy request, a response date and add an error element.
     * The handler will override anything present (except other errors) to ensure a proper message.
     *
     * @param oaipmh The pre-existing model instance, content will be overridden
     * @param ex The error to handle
     * @return The OAI-PMH model with the error message
     */
    public OAIPMH handle(final OAIPMH oaipmh, final OAIException ex) {
        Objects.requireNonNull(oaipmh);
        Objects.requireNonNull(ex);

        return oaipmh.withRequest(new Request(configuration.getBaseUrl()))
                .withResponseDate(DateProvider.now())
                .withVerb(null)
                .withError(handle(ex));
    }

    /**
     * Create an OAI-PMH response with an emtpy request, a response date and a (potentially
     * error-carrying) raw request. This is here for convenience - the handler will override
     * anything present to ensure a proper message if and only if the raw request does actually
     * contain errors. Will return unchanged otherwise.
     *
     * @param oaipmh The pre-existing model instance, content will be overridden
     * @param rawRequest The error to handle
     * @return The OAI-PMH model with the error message
     */
    public OAIPMH handle(final OAIPMH oaipmh, final RequestBuilder.RawRequest rawRequest) {
        Objects.requireNonNull(oaipmh);
        Objects.requireNonNull(rawRequest);

        if (rawRequest.hasErrors()) {
            oaipmh.withRequest(new Request(configuration.getBaseUrl()))
                    .withResponseDate(DateProvider.now())
                    .withVerb(null);

            rawRequest.getErrors().forEach(ex -> oaipmh.withError(handle(ex)));
        }
        return oaipmh;
    }

    public Error handle(final OAIException ex) {
        Objects.requireNonNull(ex);
        return new Error(ex.getMessage()).withCode(ex.getErrorCode());
    }
}
