/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider;

import io.gdcc.xoai.dataprovider.handlers.ErrorHandler;
import io.gdcc.xoai.dataprovider.handlers.GetRecordHandler;
import io.gdcc.xoai.dataprovider.handlers.IdentifyHandler;
import io.gdcc.xoai.dataprovider.handlers.ListIdentifiersHandler;
import io.gdcc.xoai.dataprovider.handlers.ListMetadataFormatsHandler;
import io.gdcc.xoai.dataprovider.handlers.ListRecordsHandler;
import io.gdcc.xoai.dataprovider.handlers.ListSetsHandler;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.dataprovider.request.RequestBuilder;
import io.gdcc.xoai.dataprovider.request.RequestBuilder.RawRequest;
import io.gdcc.xoai.exceptions.BadVerbException;
import io.gdcc.xoai.exceptions.OAIException;
import io.gdcc.xoai.model.oaipmh.OAIPMH;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.Verb.Type;
import io.gdcc.xoai.services.api.DateProvider;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * When looking to implement an OAI-PMH data provider, you should create a servlet on your own and
 * use an instance of this class (preferably within a stateless bean to make the application server
 * scale instances if necessary) to handle the incoming requests.
 *
 * <p>The only state to keep track is the configuration. The repositories need to interface with the
 * rest of your application anyway - they probably should be instances within the same service bean,
 * alongside a provider.
 */
public class DataProvider {
    private static final Logger log = LoggerFactory.getLogger(DataProvider.class);

    public static DataProvider dataProvider(final Context context, final Repository repository) {
        return new DataProvider(context, repository);
    }

    private final RepositoryConfiguration configuration;

    protected final IdentifyHandler identifyHandler;
    protected final GetRecordHandler getRecordHandler;
    protected final ListSetsHandler listSetsHandler;
    protected final ListRecordsHandler listRecordsHandler;
    protected final ListIdentifiersHandler listIdentifiersHandler;
    protected final ListMetadataFormatsHandler listMetadataFormatsHandler;
    protected final ErrorHandler errorsHandler;

    /**
     * Create a new data provider with a context and a repository, linked to your item and set
     * repository implementations. In case you need to get any custom handlers in place, extend the
     * class and either override this constructor or add some method to manipulate the instance
     * fields.
     *
     * @param context Your context
     * @param repository Your repository
     */
    public DataProvider(final Context context, final Repository repository) {
        Objects.requireNonNull(context, "Context for data provider may not be null");
        Objects.requireNonNull(repository, "Repository for data provider may not be null");
        Objects.requireNonNull(
                repository.getItemRepository(),
                "Repository must contain a non-null item repository");
        Objects.requireNonNull(
                repository.getSetRepository(),
                "Repository must contain a non-null sets repository");

        this.configuration = repository.getConfiguration();
        Objects.requireNonNull(this.configuration, "Repository configuration may not be null");

        this.identifyHandler = new IdentifyHandler(context, repository);
        this.listSetsHandler = new ListSetsHandler(context, repository);
        this.listMetadataFormatsHandler = new ListMetadataFormatsHandler(context, repository);
        this.listRecordsHandler = new ListRecordsHandler(context, repository);
        this.listIdentifiersHandler = new ListIdentifiersHandler(context, repository);
        this.getRecordHandler = new GetRecordHandler(context, repository);
        this.errorsHandler = new ErrorHandler(repository.getConfiguration());
    }

    /**
     * Handle a request by passing along a {@see jakarta.servlet.ServletRequest#getParameters} map.
     *
     * @param queryParameters The requests parameter map
     * @return The response to send to the user (you need an {@link io.gdcc.xoai.xml.XmlWriter}).
     *     Might contain errors!
     * @throws io.gdcc.xoai.dataprovider.exceptions.InternalOAIException in case of serverside
     *     errors.
     */
    public OAIPMH handle(Map<String, String[]> queryParameters) {
        OAIPMH oaipmh = new OAIPMH();
        try {
            RawRequest rawRequest = RequestBuilder.buildRawRequest(queryParameters);
            // process the raw request further, even if it has errors - we might discover more
            // errors in
            // the next step
            return handle(rawRequest);
        } catch (OAIException e) {
            log.debug(e.getMessage(), e);
            return this.errorsHandler.handle(oaipmh, e);
        }
    }

    /**
     * Second entrypoint. You can also build the {@link RawRequest} yourself and start here. Please
     * look at {@link RequestBuilder#buildRawRequest(Map)} or {@link RequestBuilder.RawRequest} to
     * learn about creating these.
     *
     * @param rawRequest The minimal parsed and validated request
     * @return The response to send to the user (you need an {@link io.gdcc.xoai.xml.XmlWriter}).
     *     Might contain errors!
     * @throws io.gdcc.xoai.dataprovider.exceptions.InternalOAIException in case of serverside
     *     errors.
     */
    public OAIPMH handle(final RawRequest rawRequest) {
        OAIPMH oaipmh = new OAIPMH();
        // build the request first
        final Request request = RequestBuilder.buildRequest(rawRequest, this.configuration);

        // if there are errors, stop here, build errors and return.
        if (rawRequest.hasErrors()) {
            return this.errorsHandler.handle(oaipmh, rawRequest);
        }
        // hand down the request, now validated for the most basic things, to the real verb handlers
        return handle(request);
    }

    /**
     * Third entry point. You provide a full-fledged {@link Request}, compiled, validated etc to
     * work with. Please look at {@link RequestBuilder#buildRequest(RawRequest,
     * RepositoryConfiguration)} or {@link Request} to learn about creating these.
     *
     * @param request The validated full-fledged request to be worked on.
     * @return The response to send to the user (you need an {@link io.gdcc.xoai.xml.XmlWriter}).
     *     Might contain errors!
     * @throws io.gdcc.xoai.dataprovider.exceptions.InternalOAIException in case of serverside
     *     errors.
     */
    public OAIPMH handle(Request request) {
        // Build a response model instance
        OAIPMH oaipmh = new OAIPMH().withRequest(request).withResponseDate(DateProvider.now());

        try {
            // Try to retrieve a token sent by the client
            Optional<ResumptionToken.Value> clientSentToken =
                    configuration.getResumptionTokenFormat().parse(request);

            // TODO: validate the token when present!

            // When not present, create a new token value for this initial request
            // Remember: an initial token never be sent back to the client. (Empty when result is
            // small,
            // additional offset otherwise)
            ResumptionToken.Value token =
                    clientSentToken.orElse(ResumptionToken.ValueBuilder.build(request));

            // Execute the request with a matching handler
            Type verb = request.getType().orElseThrow(BadVerbException::new);
            switch (verb) {
                case Identify:
                    return oaipmh.withVerb(identifyHandler.handle(request));
                case ListSets:
                    return oaipmh.withVerb(listSetsHandler.handle(token));
                case ListMetadataFormats:
                    return oaipmh.withVerb(listMetadataFormatsHandler.handle(request));
                case GetRecord:
                    return oaipmh.withVerb(getRecordHandler.handle(request));
                case ListIdentifiers:
                    return oaipmh.withVerb(listIdentifiersHandler.handle(token));
                case ListRecords:
                    return oaipmh.withVerb(listRecordsHandler.handle(token));
                default:
                    throw new BadVerbException("Illegal verb " + verb);
            }
        } catch (OAIException e) {
            log.debug(e.getMessage(), e);
            return this.errorsHandler.handle(oaipmh, e);
        }
    }

    /**
     * Let an implementing application retrieve the OAI XSLT for nicer UI of results. The app needs
     * to create some endpoint to serve the content and reference that endpoint in {@link
     * io.gdcc.xoai.xml.XmlWriter#writeStylesheet(String)}. The app might also choose to cache the
     * String instead of re-reading it.
     *
     * @return The XSLT document as complete String
     */
    public static final String getOaiXSLT() {
        // get resource from classpath
        ClassLoader classLoader = DataProvider.class.getClassLoader();
        URL oaiXsltResource = classLoader.getResource("oai2.xsl");

        // Prevent errors if file not found or could not be loaded
        if (oaiXsltResource == null) {
            log.warn("Could not find or load OAI XSLT file, class loader returned null");
            return "";
        }

        try (InputStream inStream = oaiXsltResource.openStream()) {
            return new String(inStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Could not read OAI XSLT file", e);
            return "";
        }
    }
}
