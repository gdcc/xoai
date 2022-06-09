package io.gdcc.xoai.dataprovider.request;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.exceptions.BadArgumentException;
import io.gdcc.xoai.exceptions.BadVerbException;
import io.gdcc.xoai.exceptions.OAIException;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.verbs.Verb.Argument;
import io.gdcc.xoai.model.oaipmh.verbs.Verb.Type;
import io.gdcc.xoai.services.api.DateProvider;

import java.time.DateTimeException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RequestBuilder {
    
    // static methods only - you should never create an object of this
    private RequestBuilder() {}
    
    public static final class RawRequest {
        private Type verb;
        private final Map<Argument, String> arguments = new EnumMap<>(Argument.class);
        private final List<BadArgumentException> errors = new ArrayList<>();
        
        // the empty constructor is only allowed in this class (we know what we are doing)
        // or within package (so we can use it in tests, too)
        RawRequest() {}
    
        /**
         * Create a new raw request. We need to guarantee the verb is known, please provide it.
         * @param verb The {@link io.gdcc.xoai.model.oaipmh.verbs.Verb.Type} for this request,
         *             representing the OAI-PMH verb.
         * @throws NullPointerException when the verb is null.
         */
        public RawRequest(Type verb) {
            withVerb(verb);
        }
    
        // package private - everyone else must use the constructor!
        void withVerb(final Type verb) {
            Objects.requireNonNull(verb);
            this.verb = verb;
        }
        public RawRequest withArgument(final Argument argument, final String value) {
            Objects.requireNonNull(argument);
            Objects.requireNonNull(value);
            this.arguments.put(argument, value);
            return this;
        }
        public void withError(final BadArgumentException e) {
            Objects.requireNonNull(e);
            this.errors.add(e);
        }
        public boolean hasErrors() {
            return ! this.errors.isEmpty();
        }
        public Type getVerb() {
            return verb;
        }
        public Map<Argument, String> getArguments() {
            return Collections.unmodifiableMap(arguments);
        }
        public List<BadArgumentException> getErrors() {
            return Collections.unmodifiableList(errors);
        }
    }
    
    /**
     * Build a {@link RawRequest} containing all parameters necessary to build a {@link Request} object from another map.
     * This map is exactly what you receive from {@see jakarta.servlet.ServletRequest#getParameterMap}.
     * (This method is designed to be used with an OAI-PMH servlet.)
     *
     * The method validates the presence of a verb, the that sent arguments exist and are valid for the verb.
     * It also checks that the arguments aren't multivalued. It does, however, not validate the actual
     * arguments value or syntactic correctness. This is left as a task for the request building and handlers.
     *
     * When argument errors are found, this will be stored as errors within the raw request, to enable collecting
     * all errors first, which is more user-friendly (and the spec also requests it).
     *
     * @param parameters The map originating from your servlet
     * @return A {@link RawRequest} containing the parsed map, a {@link Type} and any errors from arguments
     * @throws BadVerbException When the parameters don't contain a valid verb mapping to a {@link Type}
     */
    public static RawRequest buildRawRequest(final Map<String, String[]> parameters) throws OAIException {
        Objects.requireNonNull(parameters, "The parameter Map<String,String[]> must be non-null");
        RawRequest rawRequest = new RawRequest();
        
        // First take a look of the verb is present (if not, this is a violation of the spec)
        if (! parameters.containsKey(Argument.Verb.toString())) {
            throw new BadVerbException("No argument '" + Argument.Verb + "' found");
        // Let's see if the parameter value is single and a valid verb
        } else {
            // Check length
            String[] verbParameter = parameters.get(Argument.Verb.toString());
            if (verbParameter == null || verbParameter.length != 1) {
                throw new BadVerbException("Verb must be singular, given: '" + Arrays.toString(verbParameter) + "'");
            }
            // Get verb, will throw BadVerbException if not found
            rawRequest.withVerb(Type.from(verbParameter[0]));
        }
        
        // For convenience...
        final Type verb = rawRequest.verb;
        
        // Get parameters but remove already checked verb (don't reiterate)
        Set<String> parameterKeys = new HashSet<>(parameters.keySet());
        parameterKeys.remove(Argument.Verb.toString());
        
        // Iterate all query parameters
        for (String parameter : parameterKeys) {
            // Check if the parameter actually exists (will throw BadArgumentException) and transform to argument
            Argument argument = Argument.from(parameter);
            
            try {
                // Check if the parameter is allowed for this verb at all or throw BadArgumentException
                if (!verb.reqArgs().contains(argument)
                    && !verb.optArgs().contains(argument)
                    && !verb.exclArgs().contains(argument)) {
                    throw new BadArgumentException("'" + argument + "' is not valid for verb '" + verb + "'");
                }
                
                // Check if the parameter value is a single item (all of OAI-PMH does not use multi-value arguments)
                String[] argParameter = parameters.get(parameter);
                if (argParameter == null || argParameter.length != 1) {
                    throw new BadArgumentException("Arguments must be singular, given: '" + Arrays.toString(argParameter) + "'");
                }
                
                // Otherwise, add to the map
                rawRequest.withArgument(argument, argParameter[0]);
            } catch (BadArgumentException e) {
                // Instead of throwing, let's iterate all the arguments and collect all errors.
                // Using code needs to check for errors before trying to use the raw request
                rawRequest.withError(e);
            }
        }
        return rawRequest;
    }
    
    /**
     * Build a {@link Request} from a {@link RawRequest}, while retrieving details from a {@link RepositoryConfiguration}.
     * It will parse any dates given via {@link Argument#From} or {@link Argument#Until} with {@link Granularity} set
     * in the {@link RepositoryConfiguration}. Any errors found during the process gets stored within the raw request.
     *
     * If the raw request contains (already) errors, this method ensures to return only the most basic request model,
     * containing the base URL only. This way the handlers cannot continue parsing the request until the most basic
     * errors have been fixed by the user in their next request to the endpoint.
     *
     * The validation of syntactic correctness and argument values of the request is left to the handlers.
     *
     * @param rawRequest The raw request, coming from e.g. {@link #buildRawRequest(Map)} or somewhere else.
     *                   More errors will be added inside this method on discovery.
     * @param configuration The repository configuration your are using for your {@link io.gdcc.xoai.dataprovider.DataProvider}
     * @return A request to work with (might be empty when errors are present within the raw request!)
     */
    public static Request buildRequest(final RawRequest rawRequest, final RepositoryConfiguration configuration) {
        // Create a new request model object and try to fill it below
        final Request request = new Request(configuration.getBaseUrl());
        final Granularity granularity = configuration.getGranularity();
    
        // Remember: raw request might already have errors at this point! We still try our best to find more.
        // Add the verb from the raw request (this must have been present and legal.)
        request.withVerb(rawRequest.getVerb());
        
        // Compile the raw arguments into usable options of the request
        compileRequestArgument(request, rawRequest.getArguments(), rawRequest.errors, granularity, configuration.getEarliestDate());
        // Validate the arguments with the associated verb for exclusive, required and optional arguments
        validateArgumentPresence(rawRequest.getVerb(), rawRequest.getArguments().keySet())
            .forEach(rawRequest::withError);
        
        // When we found any errors along the way, bail out early and prevent anything using the returned request
        // to do sth with it. The calling code possesses the raw request, to which we might have added more errors.
        if (rawRequest.hasErrors())
            return new Request(configuration.getBaseUrl());
    
        // TODO: add timestamp skewing for until here
        return request;
    }
    
    /**
     * Iterate all the raw arguments. For dates: try to convert and check for granularity & earliest allowed (as configured).
     * Will add any errors found to the list of errors, which is the list from the RawRequest, passed by reference.
     */
    static void compileRequestArgument(final Request request, final Map<Argument,String> arguments, final List<BadArgumentException> errorList,
                                       final Granularity granularity, final Instant earliestDate) {
        
        // Iterate all the arguments and add to the request, parsing the dates on the go.
        for (Map.Entry<Argument,String> entry : arguments.entrySet()) {
            Argument argument = entry.getKey();
            String value = entry.getValue();
        
            try {
                switch (argument) {
                    case MetadataPrefix:
                        request.withMetadataPrefix(value); break;
                    case From:
                        // Parse the date
                        Instant from = DateProvider.parse(value, granularity);
                        // Ensure from is not before the configured earliest date or throw BadArgumentException
                        if (from.isAfter(earliestDate))
                            request.withFrom(from);
                        else
                            throw new BadArgumentException("'" + argument + "' cannot be before " + earliestDate);
                        break;
                    case Until:
                        request.withUntil(DateProvider.parse(value, granularity)); break;
                    case Identifier:
                        request.withIdentifier(value); break;
                    case Set:
                        request.withSet(value); break;
                    case ResumptionToken:
                        request.withResumptionToken(value); break;
                    default:
                        throw new InternalOAIException("This should never happen - do not include the verb in the arguments!");
                }
            } catch (DateTimeException e) {
                errorList.add(new BadArgumentException("'" + value + "' is not a valid date for '" + argument +
                    "' requiring format '" + granularity + "'"));
            } catch (BadArgumentException e) {
                // Collect all errors instead of bailing out
                errorList.add(e);
            }
        }
    }
    
    /**
     * Validate for a type if all required arguments are there and if exclusive arguments given, nothing else is present
     */
    public static List<BadArgumentException> validateArgumentPresence(final Type verb, final Set<Argument> arguments) {
        Objects.requireNonNull(verb, "Verb may not be null");
        Objects.requireNonNull(arguments, "Arguments may not be null");
        
        List<BadArgumentException> errors = new ArrayList<>();
        // create a copy of the arguments - set operations on the view would otherwise change the map!
        Set<Argument> copy = new HashSet<>(arguments);
        
        // exclusive first - each argument in this set may only be present once and no other may be present!
        // (if this verb has no exclusive args, no iteration will be done)
        for (Argument argument : verb.exclArgs()) {
            // if this exclusive argument is contained in the arguments, but there are others, too, this is a
            // violation of the protocol.
            // Quote: "the argument may be included with request, but must be the only argument"
            if (copy.contains(argument) && copy.size() > 1) {
                errors.add(new BadArgumentException("Non-exclusive use of exclusive argument '" +argument+ "'"));
            }
        }
        
        // Exclusive options require that nothing else is present. If an exclusive argument is present but not alone,
        // stop validation here and return the errors early to inform the user.
        if (! errors.isEmpty())
            return errors;
        
        // now lets look for required arguments (empty means no checks done)
        for (Argument argument : verb.reqArgs()) {
            if (! copy.contains(argument)) {
                // add an error if the required argument is missing
                errors.add(new BadArgumentException("Missing required argument '" +argument+ "' for verb '" +verb+ "'"));
            }
            // if the required argument is present, remove it from the set, so we can detect
            // non-allowed arguments for the verb by simply checking on empty set.
            // (this is no-op for args not present)
            copy.remove(argument);
        }
    
        // lets look for optional arguments (empty means no checks done)
        for (Argument argument : verb.optArgs()) {
            // remove any optional arguments from the set, so we can detect
            // non-allowed arguments for the verb by simply checking on empty set.
            // (this is a no-op for args not present)
            copy.remove(argument);
        }
        
        // any non-allowed arguments?
        if (!copy.isEmpty()) {
            copy.forEach(arg -> errors.add(new BadArgumentException("Argument '" +arg+ "' not allowed for verb '" +verb+ "'")));
        }
        
        return errors;
    }
}
