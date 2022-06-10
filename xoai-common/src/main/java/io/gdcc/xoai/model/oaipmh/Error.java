/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.model.oaipmh;

import io.gdcc.xoai.xmlio.exceptions.XmlWriteException;
import io.gdcc.xoai.xml.XmlWritable;
import io.gdcc.xoai.xml.XmlWriter;

import javax.xml.stream.XMLStreamException;

public class Error implements XmlWritable {
    private final String value;
    private Code code;

    public Error (String message) {
        this.value = message;
    }

    public String getMessage() {
        return value;
    }

    public Code getCode() {
        return code;
    }

    public Error withCode(Code value) {
        this.code = value;
        return this;
    }

    @Override
    public void write(XmlWriter writer) throws XmlWriteException {
        try {
            if (this.code != null)
                writer.writeAttribute("code", this.code.toString());
    
            writer.writeCharacters(value);
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }
    
    public enum Code {
        CANNOT_DISSEMINATE_FORMAT("cannotDisseminateFormat", "Cannot disseminate item with the given format"),
        ID_DOES_NOT_EXIST("idDoesNotExist", "The given id does not exist"),
        BAD_ARGUMENT("badArgument", null),
        BAD_VERB("badVerb", "Illegal OAI verb"),
        NO_METADATA_FORMATS("noMetadataFormats", "The item does not have any metadata format available for dissemination"),
        NO_RECORDS_MATCH("noRecordsMatch", "No matches for the query"),
        BAD_RESUMPTION_TOKEN("badResumptionToken", "The resumption token is invalid"),
        NO_SET_HIERARCHY("noSetHierarchy", "This repository does not support sets");

        private final String id;
        private final String message;

        Code(String id, String message) {
            this.id = id;
            this.message = message;
        }

        public String id() {
            return id;
        }
        public String message() {
            return message;
        }

        public static Code from(String code) {
            for (Code c : Code.values()) {
                if (c.id.equals(code)) {
                    return c;
                }
            }
            throw new IllegalArgumentException(code);
        }

        @Override
        public String toString() {
            return id;
        }
    }
}
