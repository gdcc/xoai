/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.model.oaipmh.verbs;

import io.gdcc.xoai.exceptions.BadArgumentException;
import io.gdcc.xoai.exceptions.BadVerbException;
import io.gdcc.xoai.xml.XmlWritable;

import java.util.Collections;
import java.util.Set;

public interface Verb extends XmlWritable {
    
    /**
     * An enumerated list of verbs (queries you can ask a data provider for results)
     */
    enum Type {
        GetRecord(
            "GetRecord",
            Set.of(Argument.Identifier, Argument.MetadataPrefix),
            Set.of(),
            Set.of()),
        Identify(
            "Identify",
            Set.of(),
            Set.of(),
            Set.of()),
        ListIdentifiers(
            "ListIdentifiers",
            Set.of(Argument.MetadataPrefix),
            Set.of(Argument.From, Argument.Until, Argument.Set),
            Set.of(Argument.ResumptionToken)),
        ListMetadataFormats(
            "ListMetadataFormats",
            Set.of(),
            Set.of(Argument.Identifier),
            Set.of()),
        ListRecords(
            "ListRecords",
            Set.of(Argument.MetadataPrefix),
            Set.of(Argument.From, Argument.Until, Argument.Set),
            Set.of(Argument.ResumptionToken)),
        ListSets(
            "ListSets",
            Set.of(),
            Set.of(),
            Set.of(Argument.ResumptionToken));

        private final String verb;
        private final Set<Argument> required;
        private final Set<Argument> optional;
        private final Set<Argument> exclusive;

        Type(String value, Set<Argument> required, Set<Argument> optional, Set<Argument> exclusive) {
            this.verb = value;
            this.required = required;
            this.optional = optional;
            this.exclusive = exclusive;
        }

        public String displayName() {
            return verb;
        }
        
        public static Type from(String verb) throws BadVerbException {
            for (Type c : Type.values())
                if (c.verb.equals(verb))
                    return c;
            
            throw new BadVerbException(verb);
        }
    
        /**
         * Return a set of Arguments that must be present in a request
         * @return The set - may be empty, but not null!
         */
        public Set<Argument> reqArgs() {
            return Collections.unmodifiableSet(required);
        }
    
        /**
         * Return a set of Arguments that may be present in a request
         * @return The set - may be empty, but not null!
         */
        public Set<Argument> optArgs() {
            return Collections.unmodifiableSet(optional);
        }
    
        /**
         * Return a set of Arguments that are mutually exclusive.
         * If one of these is used, nothing else may be present beside the "verb"
         * @return The list of exclusive options
         */
        public Set<Argument> exclArgs() {
            return Collections.unmodifiableSet(exclusive);
        }
    }
    
    /**
     * Receive the type of query this Verb represents
     * @return The verb type
     */
    Type getType();
    
    enum Argument {
        From("from"),
        Until("until"),
        Identifier("identifier"),
        MetadataPrefix("metadataPrefix"),
        ResumptionToken("resumptionToken"),
        Set("set"),
        Verb("verb");
        
        private final String representation;
    
        Argument (String rep) {
            this.representation = rep;
        }
    
        @Override
        public String toString () {
            return representation;
        }
        
        public static Argument from(String representation) throws BadArgumentException {
            for (Argument param : Argument.values())
                if (param.representation.equals(representation))
                    return param;
            
            throw new BadArgumentException("Given argument '" + representation + "' is not valid.");
        }
    }
}
