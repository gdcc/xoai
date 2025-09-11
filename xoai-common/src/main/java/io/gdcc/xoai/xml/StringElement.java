package io.gdcc.xoai.xml;

import io.gdcc.xoai.xmlio.exceptions.XmlWriteException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLStreamException;

/**
 * This class implements the {@link XmlWritable} interface, enabling it to be serialized to an
 * {@link XmlWriter} as part of the XML writing process. It represents an XML element in the form of
 * an unparsed string, allowing raw XML content to be directly written without prior parsing or
 * modification.
 *
 * <p>Note: you cannot write at the root level with this element, as the StAX writer needs at least
 * one wrapping element.
 */
public class StringElement implements XmlWritable {
    private final String xmlString;

    public StringElement(final String xmlString) {
        this.xmlString = xmlString;
    }

    public String asUnparsedString() {
        return xmlString;
    }

    @Override
    public void write(final XmlWriter writer) throws XmlWriteException {
        if (xmlString != null) {
            // This replicates the same approach used in the CopyElement:
            // we transfer the raw, unparsed string unmodified into the output
            // stream of the writer
            try {
                // Make the XmlWriter think we want to write a value, so it prints ">" of the
                // containing
                // element to stream
                // This is somewhat hacky, but there is no other possibility to trick the StAX API
                // into
                // this.
                writer.writeCharacters("");
                // Flush the XmlWriter to make sure any preceding tags are written out
                writer.flush();
                // Now let's write the actual content:
                writer.getOutputStream().write(xmlString.getBytes(StandardCharsets.UTF_8));
                // And flush stream & writer after the operation - again
                writer.flush();
            } catch (XMLStreamException | IOException e) {
                throw new XmlWriteException(e);
            }
        } else {
            throw new XmlWriteException("Cannot write XML since the string is null");
        }
    }
}
