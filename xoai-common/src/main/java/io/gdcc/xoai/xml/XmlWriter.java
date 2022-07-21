/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.xml;

import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.oaipmh.verbs.Verb;
import io.gdcc.xoai.services.api.DateProvider;
import io.gdcc.xoai.xmlio.XmlIoWriter;
import io.gdcc.xoai.xmlio.exceptions.XmlWriteException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.Optional;
import javax.xml.stream.XMLStreamException;

public class XmlWriter extends XmlIoWriter implements AutoCloseable {

    public static String toString(XmlWritable writable) throws XMLStreamException {
        return toString(writable, defaultContext());
    }

    public static String toString(XmlWritable writable, WriterContext context)
            throws XMLStreamException {
        final OutputStream out = new ByteArrayOutputStream();

        try (out;
                XmlWriter writer = new XmlWriter(out, context)) {
            writable.write(writer);
        } catch (IOException e) {
            throw new XmlWriteException(e);
        }

        // the try-with-resources above will take care that writer and stream are closed before
        // reading
        // back
        // the data, making sure everything has been flushed.
        return out.toString();
    }

    public static WriterContext defaultContext() {
        return new WriterContext() {};
    }

    private final WriterContext writerContext;

    public XmlWriter(OutputStream output) throws XMLStreamException {
        super(output);
        this.writerContext = defaultContext();
    }

    public XmlWriter(OutputStream output, WriterContext writerContext) throws XMLStreamException {
        super(output);
        this.writerContext = writerContext;
    }

    public WriterContext getWriterContext() {
        return this.writerContext;
    }

    public void writeDate(Instant date) throws XmlWriteException {
        try {
            this.writeCharacters(DateProvider.format(date, writerContext.getGranularity()));
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    public void writeDate(Instant date, Granularity granularity) throws XmlWriteException {
        try {
            this.writeCharacters(DateProvider.format(date, granularity));
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    public void writeElement(String elementName, String elementValue) throws XmlWriteException {
        try {
            this.writeStartElement(elementName);
            this.writeCharacters(elementValue);
            this.writeEndElement();
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    public void writeElement(String elementName, XmlWritable writable) throws XmlWriteException {
        try {
            if (writable != null) {
                this.writeStartElement(elementName);
                writable.write(this);
                this.writeEndElement();
            }
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    public void writeElement(String elementName, Instant date, Granularity granularity)
            throws XmlWriteException {
        this.writeElement(elementName, DateProvider.format(date, granularity));
    }

    public void writeElement(String elementName, Instant date) throws XmlWriteException {
        this.writeElement(elementName, DateProvider.format(date, writerContext.getGranularity()));
    }

    public void writeAttribute(String name, Instant date) throws XmlWriteException {
        try {
            this.writeAttribute(name, DateProvider.format(date, writerContext.getGranularity()));
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    public void writeAttribute(String name, Instant value, Granularity granularity)
            throws XmlWriteException {
        try {
            this.writeAttribute(name, DateProvider.format(value, granularity));
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    public <T> void writeAttribute(Verb.Argument argument, Optional<T> optional)
            throws XMLStreamException {
        if (optional.isPresent()) {
            T value = optional.get();
            if (value instanceof String) writeAttribute(argument.toString(), (String) value);
            else if (value instanceof Instant) writeAttribute(argument.toString(), (Instant) value);
        }
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        if (value != null) super.writeAttribute(localName, value);
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        if (text != null) super.writeCharacters(text);
    }

    public void write(XmlWritable writable) throws XmlWriteException {
        if (writable != null) writable.write(this);
    }

    public void write(ResumptionToken.Value value) throws XmlWriteException {
        try {
            if (!value.isEmpty())
                writeCharacters(writerContext.getResumptionTokenFormat().format(value));
        } catch (XMLStreamException e) {
            throw new XmlWriteException(e);
        }
    }

    public void writeStylesheet(String href) throws XMLStreamException {
        if (href == null) {
            throw new XMLStreamException(
                    "May not pass a null hyper reference to the XSLT processing instruction");
        }
        super.writeProcessingInstruction(
                "xml-stylesheet", "type=\"text/xsl\" href=\"" + href + "\"");
    }
}
