/**
 * Copyright 2012 Lyncode
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gdcc.xoai.xmlio;

import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.aStartElement;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.text;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.not;

import io.gdcc.xoai.xmlio.exceptions.XmlReaderException;
import io.gdcc.xoai.xmlio.matchers.extractor.ExtractFunction;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.Namespace;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.codehaus.stax2.XMLInputFactory2;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class XmlReader implements AutoCloseable {

    /**
     * A thread-local holder for `XMLInputFactory` instances, ensuring a secure factory is created
     * for each thread using the `createSecureXMLInputFactory` method in the containing `XmlReader`
     * class.
     *
     * <p>The `XMLInputFactory` instances are configured with security parameters to disable
     * potentially dangerous features, such as support for external entities, DTDs, and automatic
     * entity replacement, mitigating common XML-related security vulnerabilities.
     *
     * <p>This approach avoids contention issues in multi-threaded contexts, as `XMLInputFactory` is
     * not inherently thread-safe and must be instantiated separately for each thread.
     *
     * <p>In regard to suppressing the Sonar warning about not calling remove(): 1. XMLInputFactory
     * is lightweight, the factory itself is a small, stateless object that doesn't hold significant
     * resources 2. No resource leaks, as XMLInputFactory doesn't maintain open connections, file
     * handles, or other system resources 3. ThreadPool reuse is beneficial, as keeping the factory
     * in ThreadLocal allows efficient reuse across multiple XML parsing operations on the same
     * thread
     */
    @SuppressWarnings("java:S5164")
    private static final ThreadLocal<XMLInputFactory> XML_INPUT_FACTORY =
            ThreadLocal.withInitial(XmlReader::createSecureXMLInputFactory);

    private static XMLInputFactory createSecureXMLInputFactory() {
        try {
            // Using the STaX2 API here, but hiding behind STaX1
            // Ignore SonarCloud warning here - it's used the way the library says we must use it.
            // https://sonarcloud.io/organizations/gdcc/rules?open=java%3AS3252&rule_key=java%3AS3252
            @SuppressWarnings("java:S3252")
            XMLInputFactory factory = XMLInputFactory2.newFactory();

            // Disable dangerous features: disallow loading external resources to avoid XXEs
            // See
            // https://github.com/FasterXML/woodstox/wiki/Configuring-Woodstox-I-%E2%80%90-Basic-Stax-Properties#standard-stax-properties
            factory.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
            factory.setProperty(XMLInputFactory.SUPPORT_DTD, false);
            factory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);

            return factory;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to create secure XMLInputFactory", e);
        }
    }

    private final XMLEventReader xmlEventParser;

    public XmlReader(final InputStream stream) throws XmlReaderException {
        try {
            // The EventParser is not thread-safe, so create one for every operation.
            this.xmlEventParser = XML_INPUT_FACTORY.get().createXMLEventReader(stream);
        } catch (XMLStreamException e) {
            throw new XmlReaderException(e);
        }
    }

    public boolean current(Matcher<XMLEvent> matcher) throws XmlReaderException {
        return matcher.matches(peek());
    }

    public void close() throws XmlReaderException {
        try {
            xmlEventParser.close();
        } catch (XMLStreamException e) {
            throw new XmlReaderException(e);
        }
    }

    public QName getName() throws XmlReaderException {
        if (peek().isStartElement()) return peek().asStartElement().getName();
        else if (peek().isEndElement()) return peek().asEndElement().getName();
        else throw new XmlReaderException("Current event has no name");
    }

    public String getText() throws XmlReaderException {
        if (current(text())) return peek().asCharacters().getData();
        else throw new XmlReaderException("Current element is not text");
    }

    public String getAttributeValue(Matcher<QName> nameMatcher) throws XmlReaderException {
        if (peek().isStartElement()) {
            Iterator<Attribute> attributes = peek().asStartElement().getAttributes();
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                if (nameMatcher.matches(attribute.getName())) return attribute.getValue();
            }
        }
        return null;
    }

    public <T> Map<T, String> getAttributes(ExtractFunction<QName, T> extractFunction)
            throws XmlReaderException {
        HashMap<T, String> map = new HashMap<>();
        if (peek().isStartElement()) {
            Iterator<Attribute> attributes = peek().asStartElement().getAttributes();
            while (attributes.hasNext()) {
                Attribute attribute = attributes.next();
                map.put(extractFunction.apply(attribute.getName()), attribute.getValue());
            }
        }
        return map;
    }

    public boolean hasAttribute(final Matcher<Attribute> matcher) throws XmlReaderException {
        return hasAttributeMatcher(matcher).matches(peek().asStartElement().getAttributes());
    }

    private TypeSafeMatcher<Iterator<Attribute>> hasAttributeMatcher(
            final Matcher<Attribute> matcher) {
        return new TypeSafeMatcher<>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("has attribute");
            }

            @Override
            protected boolean matchesSafely(Iterator item) {
                while (item.hasNext()) if (matcher.matches(item.next())) return true;
                return false;
            }
        };
    }

    @SafeVarargs
    public final XmlReader next(Matcher<XMLEvent>... possibleEvents) throws XmlReaderException {
        try {
            xmlEventParser.nextEvent();
            while (!anyOf(possibleEvents).matches(peek())) xmlEventParser.nextEvent();

            return this;
        } catch (XMLStreamException e) {
            throw new XmlReaderException(e);
        }
    }

    public <T> T get(IslandParser<T> islandParser) throws XmlReaderException {
        return islandParser.parse(this);
    }

    private XMLEvent peek() throws XmlReaderException {
        try {
            return this.xmlEventParser.peek();
        } catch (XMLStreamException e) {
            throw new XmlReaderException(e);
        }
    }

    public XMLEvent nextEvent() throws XmlReaderException {
        try {
            return this.xmlEventParser.nextEvent();
        } catch (XMLStreamException e) {
            throw new XmlReaderException(e);
        }
    }

    public boolean hasNext() {
        return this.xmlEventParser.hasNext();
    }

    public String retrieveCurrentAsString() throws XmlReaderException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            if (current(not(aStartElement())))
                throw new XmlReaderException("Can only retrieve from starting elements");
            XmlIoWriter writer = new XmlIoWriter(outputStream);
            int depth = 0;
            while (xmlEventParser.peek() != null) {
                XMLEvent event = xmlEventParser.peek();
                if (event.isStartElement()) {
                    depth++;
                    StartElement start = event.asStartElement();
                    writer.writeStartElement(
                            start.getName().getPrefix(),
                            start.getName().getLocalPart(),
                            start.getName().getNamespaceURI());

                    Iterator<Namespace> it = start.getNamespaces();
                    while (it.hasNext()) {
                        Namespace n = it.next();
                        writer.writeNamespace(n.getPrefix(), n.getNamespaceURI());
                    }

                    Iterator<Attribute> attrs = start.getAttributes();
                    while (attrs.hasNext()) {
                        Attribute attr = attrs.next();
                        writer.writeAttribute(
                                attr.getName().getPrefix(),
                                attr.getName().getNamespaceURI(),
                                attr.getName().getLocalPart(),
                                attr.getValue());
                    }

                } else if (event.isEndElement()) {
                    /*
                    This was the original content. It lead to a breaking test in xoai-service-provider/RecordParserTest,
                    where the end element of an <oai_dc:dc> element was not closed because the end element was
                    not written due to count already being 0 when reaching the if clause. Leaving this here
                    when we see any troubles arising from this change.

                    count--;
                    if (count == 0) {
                        break;
                    } else writer.writeEndElement();
                     */

                    // write the closing tag and decrease the depth
                    writer.writeEndElement();
                    depth -= 1;

                    // if this was the end element of the outermost XML layer, break the loop here
                    if (depth == 0) {
                        break;
                    }
                } else if (event.isCharacters()) {
                    writer.writeCharacters(event.asCharacters().getData());
                }

                if (xmlEventParser.hasNext()) xmlEventParser.nextEvent();
                else break;
            }

            // when there wasn't enough closing elements, we're doomed.
            if (depth > 0) throw new XmlReaderException("Unterminated structure");

            writer.flush();
            writer.close();

            return outputStream.toString();
        } catch (XMLStreamException e) {
            throw new XmlReaderException(e);
        }
    }

    public interface IslandParser<T> {
        T parse(XmlReader reader) throws XmlReaderException;
    }
}
