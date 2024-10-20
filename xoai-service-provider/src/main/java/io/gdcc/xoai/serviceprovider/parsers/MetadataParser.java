/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.serviceprovider.parsers;

import static io.gdcc.xoai.xmlio.matchers.QNameMatchers.localPart;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.aStartElement;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.anEndElement;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.elementName;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.text;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.theEndOfDocument;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.AllOf.allOf;

import io.gdcc.xoai.model.xoai.Element;
import io.gdcc.xoai.model.xoai.Field;
import io.gdcc.xoai.model.xoai.XOAIMetadata;
import io.gdcc.xoai.xmlio.XmlReader;
import io.gdcc.xoai.xmlio.exceptions.XmlReaderException;
import java.io.InputStream;
import javax.xml.namespace.QName;
import javax.xml.stream.events.XMLEvent;
import org.hamcrest.Matcher;

public class MetadataParser {
    public XOAIMetadata parse(InputStream input) throws XmlReaderException {
        XOAIMetadata metadata = new XOAIMetadata();
        XmlReader reader = new XmlReader(input);
        reader.next(elementName(localPart(equalTo("metadata"))));

        while (reader.next(theEndOfDocument(), anEndElement(), startElement())
                .current(startElement())) {
            metadata.withElement(parseElement(reader));
        }

        return metadata;
    }

    private Element parseElement(XmlReader reader) throws XmlReaderException {
        Element element = new Element(reader.getAttributeValue(name()));
        while (reader.next(startElement(), startField(), endOfMetadata()).current(startElement())) {
            element.withElement(parseElement(reader));
        }

        while (reader.current(startField())) {
            Field field = new Field().withName(reader.getAttributeValue(name()));

            if (reader.next(anEndElement(), text()).current(text()))
                field.withValue(reader.getText());

            element.withField(field);
            reader.next(startField(), endElement());
        }

        return element;
    }

    private Matcher<XMLEvent> startField() {
        return allOf(aStartElement(), elementName(localPart(equalTo("field"))));
    }

    private Matcher<XMLEvent> endOfMetadata() {
        return allOf(anEndElement(), elementName(localPart(equalTo("metadata"))));
    }

    private Matcher<QName> name() {
        return localPart(equalTo("name"));
    }

    private Matcher<XMLEvent> startElement() {
        return allOf(aStartElement(), elementName(localPart(equalTo("element"))));
    }

    private Matcher<XMLEvent> endElement() {
        return allOf(anEndElement(), elementName(localPart(equalTo("element"))));
    }
}
