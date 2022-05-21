/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider;

import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.handlers.ErrorHandler;
import io.gdcc.xoai.dataprovider.handlers.GetRecordHandler;
import io.gdcc.xoai.dataprovider.handlers.IdentifyHandler;
import io.gdcc.xoai.dataprovider.handlers.ListIdentifiersHandler;
import io.gdcc.xoai.dataprovider.handlers.ListMetadataFormatsHandler;
import io.gdcc.xoai.dataprovider.handlers.ListRecordsHandler;
import io.gdcc.xoai.dataprovider.handlers.ListSetsHandler;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.request.RequestBuilder;
import io.gdcc.xoai.dataprovider.request.RequestBuilder.RawRequest;
import io.gdcc.xoai.exceptions.BadVerbException;
import io.gdcc.xoai.exceptions.OAIException;
import io.gdcc.xoai.model.oaipmh.OAIPMH;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.verbs.Verb.Type;
import io.gdcc.xoai.services.api.DateProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class DataProvider {
    private static final Logger log = LoggerFactory.getLogger(DataProvider.class);

    public static DataProvider dataProvider(final Context context, final Repository repository) {
        return new DataProvider(context, repository);
    }
    
    private final Repository repository;
    
    protected final IdentifyHandler identifyHandler;
    protected final GetRecordHandler getRecordHandler;
    protected final ListSetsHandler listSetsHandler;
    protected final ListRecordsHandler listRecordsHandler;
    protected final ListIdentifiersHandler listIdentifiersHandler;
    protected final ListMetadataFormatsHandler listMetadataFormatsHandler;
    protected final ErrorHandler errorsHandler;
    
    /**
     * Create a new data provider with a context and a repository, linked to your item and set repository implementations.
     * In case you need to get any custom handlers in place, extend the class and either override this constructor
     * or add some method to manipulate the instance fields.
     *
     * @param context Your context
     * @param repository Your repository
     */
    public DataProvider(final Context context, final Repository repository) {
        this.repository = repository;
        this.identifyHandler = new IdentifyHandler(context, repository);
        this.listSetsHandler = new ListSetsHandler(context, repository);
        this.listMetadataFormatsHandler = new ListMetadataFormatsHandler(context, repository);
        this.listRecordsHandler = new ListRecordsHandler(context, repository);
        this.listIdentifiersHandler = new ListIdentifiersHandler(context, repository);
        this.getRecordHandler = new GetRecordHandler(context, repository);
        this.errorsHandler = new ErrorHandler(repository.getConfiguration());
    }
    
    public OAIPMH handle(Map<String, String[]> queryParameters) {
        OAIPMH oaipmh = new OAIPMH();
        
        try {
            RawRequest rawRequest = RequestBuilder.buildRawRequest(queryParameters);
            // process the raw request further, even if it has errors - we might discover more errors in the next step
            return handle(oaipmh, rawRequest);
        } catch (OAIException e) {
            log.debug(e.getMessage(), e);
            return this.errorsHandler.handle(oaipmh, e);
        }
    }
    
    public OAIPMH handle(final OAIPMH oaipmh, final RawRequest rawRequest) {
        try {
            // build the request first
            final Request request = RequestBuilder.buildRequest(rawRequest, this.repository.getConfiguration());
            
            // if there are errors, stop here, build errors and return.
            if (rawRequest.hasErrors()) {
                return this.errorsHandler.handle(oaipmh, rawRequest);
            }
            // hand down the request, now validated for the most basic things, to the real verb handlers
            return handle(oaipmh, request);
        } catch (OAIException e) {
            log.debug(e.getMessage(), e);
            return this.errorsHandler.handle(oaipmh, e);
        }
    }

    public OAIPMH handle(OAIPMH oaipmh, Request request) throws OAIException {
        log.debug("Starting handling OAI request");

        oaipmh.withRequest(request).withResponseDate(DateProvider.now());
        
        try {
            Type verb = request.getType().orElseThrow(BadVerbException::new);
            
            switch (verb) {
                case Identify:
                    return oaipmh.withVerb(identifyHandler.handle(request));
                case ListSets:
                    return oaipmh.withVerb(listSetsHandler.handle(request));
                case ListMetadataFormats:
                    return oaipmh.withVerb(listMetadataFormatsHandler.handle(request));
                case GetRecord:
                    return oaipmh.withVerb(getRecordHandler.handle(request));
                case ListIdentifiers:
                    return oaipmh.withVerb(listIdentifiersHandler.handle(request));
                case ListRecords:
                    return oaipmh.withVerb(listRecordsHandler.handle(request));
                default:
                    throw new BadVerbException("Illegal verb " + verb);
            }
        } catch (HandlerException e) {
            log.debug(e.getMessage(), e);
            return this.errorsHandler.handle(oaipmh, e);
        }
    }
}
