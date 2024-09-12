package io.gdcc.xoai.tests.util;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import io.gdcc.xoai.xml.EchoElement;
import io.gdcc.xoai.xml.XmlWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.Test;

public class EchoElementTest {

    /*
     * Namespace declarations (such as 'dc' on the root element <oai_dc:dc> below) should be kept when provided, as they
     * are likely to be used later.
     */
    @Test
    public void handleEarlyNamespaceDeclarations() throws XMLStreamException {
        String xml =
                "<?xml version='1.0' encoding='UTF-8'?><oai_dc:dc"
                        + " xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\""
                        + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
                        + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                        + " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/"
                        + " http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
                        + "\t<dc:title>Invasive Lithobates catesbeianus - American bullfrog"
                        + " occurrences in Flanders</dc:title>\n"
                        + "\t<dc:subject>Occurrence</dc:subject>\n"
                        + "\t<dc:subject>Observation</dc:subject>\n"
                        + "</oai_dc:dc>";

        String result = echoXml(xml);

        assertThat("EchoElement handles nested namespaces", result, equalTo(xml));
    }

    /*
     * Namespace declarations must be tracked according to the current context.  The sibling dc: elements below all need
     * a namespace declaration.
     */
    @Test
    public void repeatingNamespaceDeclarations() throws XMLStreamException {
        String xml =
                "<?xml version='1.0' encoding='UTF-8'?><oai_dc:dc"
                    + " xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\""
                    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                    + " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/"
                    + " http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
                    + "\t<dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Invasive"
                    + " Lithobates catesbeianus - American bullfrog occurrences in"
                    + " Flanders</dc:title>\n"
                    + "\t<dc:subject"
                    + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Occurrence</dc:subject>\n"
                    + "\t<dc:subject"
                    + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Observation</dc:subject>\n"
                    + "</oai_dc:dc>";

        String result = echoXml(xml);

        assertThat("EchoElement handles nested namespaces", result, equalTo(xml));
    }

    @Test
    public void copyFromInputStream() throws XMLStreamException, IOException {
        // given
        String xml =
                "<?xml version='1.0' encoding='UTF-8'?><oai_dc:dc"
                    + " xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\""
                    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                    + " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/"
                    + " http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
                    + "\t<dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Invasive"
                    + " Lithobates catesbeianus - American bullfrog occurrences in"
                    + " Flanders</dc:title>\n"
                    + "\t<dc:subject"
                    + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Occurrence</dc:subject>\n"
                    + "\t<dc:subject"
                    + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\">Observation</dc:subject>\n"
                    + "</oai_dc:dc>";
        final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();

        // when
        try (resultStream;
                InputStream stream =
                        new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8));
                XmlWriter writer = new XmlWriter(resultStream)) {
            writer.writeStartDocument();
            writer.write(new EchoElement(stream));
            writer.writeEndDocument();
        }
        String result = resultStream.toString();

        // then
        assertThat("EchoElement handles InputStream", result, equalTo(xml));
    }

    @Test
    public void defaultNamespaceDeclaration() throws XMLStreamException {
        // given
        String xml =
                "<?xml version='1.0' encoding='UTF-8'?>"
                        + "<TEI xmlns=\"http://www.tei-c.org/ns/1.0\">\n"
                        + "  <teiHeader xmlns:xml=\"http://www.w3.org/XML/1998/namespace\""
                        + " xml:lang=\"de\">\n"
                        + "    <fileDesc>\n"
                        + "      <titleStmt>\n"
                        + "        <title>Titel\n"
                        + "        </title>\n"
                        + "        <respStmt>\n"
                        + "          <resp>Published by</resp>\n"
                        + "          <name type=\"org\">Organisation</name>\n"
                        + "        </respStmt>\n"
                        + "      </titleStmt>\n"
                        + "      <publicationStmt>\n"
                        + "        <publisher>\n"
                        + "          <name type=\"org\">Organisation</name>\n"
                        + "          <ptr target=\"http://www.organisation.org\"/>\n"
                        + "        </publisher>\n"
                        + "        <date when=\"2022-11-30\" type=\"issued\">2022-11-30</date>\n"
                        + "        <distributor>Handschriftenportal</distributor>\n"
                        + "        <availability status=\"free\">\n"
                        + "          <licence"
                        + " target=\"https://creativecommons.org/publicdomain/zero/1.0/deed.de\">\n"
                        + "          </licence>\n"
                        + "        </availability>\n"
                        + "        <pubPlace>\n"
                        + "        </pubPlace>\n"
                        + "      </publicationStmt>\n"
                        + "    </fileDesc>\n"
                        + "  </teiHeader>\n"
                        + "</TEI>";

        String result = echoXml(xml);

        assertThat("EchoElement does not add empty namespaces", result, equalTo(xml));
    }

    private static String echoXml(String xml) throws XMLStreamException {

        final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();

        try (resultStream;
                XmlWriter writer = new XmlWriter(resultStream)) {
            writer.writeStartDocument();
            writer.write(new EchoElement(xml));
            writer.writeEndDocument();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resultStream.toString();
    }
}
