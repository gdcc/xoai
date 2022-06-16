/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.model.oaipmh.results;

import io.gdcc.xoai.model.xoai.XOAIMetadata;
import io.gdcc.xoai.xml.EchoElement;
import io.gdcc.xoai.xml.XmlWritable;
import io.gdcc.xoai.xml.XmlWriter;
import io.gdcc.xoai.xmlio.exceptions.XmlWriteException;

public class Description implements XmlWritable {
    public static Description description(XOAIMetadata metadata) {
        return new Description(metadata);
    }

    protected String value;
    private XOAIMetadata xoaiMetadata;

    public Description() {}

    public Description(XOAIMetadata xoaiMetadata) {
        this.xoaiMetadata = xoaiMetadata;
    }

    public Description(String compiledMetadata) {
        value = compiledMetadata;
    }

    public Description withMetadata(XOAIMetadata xoaiMetadata) {
        this.xoaiMetadata = xoaiMetadata;
        return this;
    }

    public Description withMetadata(String metadata) {
        this.value = metadata;
        return this;
    }

    @Override
    public void write(XmlWriter writer) throws XmlWriteException {
        if (xoaiMetadata != null) {
            this.xoaiMetadata.write(writer);
        } else if (this.value != null) {
            EchoElement echo = new EchoElement(value);
            echo.write(writer);
        }
    }
}
