package io.gdcc.xoai.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

public class StringElementTest {
    private final String xmlString =
            "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\""
                    + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\""
                    + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
                    + " xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/ "
                    + " http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n"
                    + "	<dc:title>Article Title-additional CDATA</dc:title>\n"
                    + "</oai_dc:dc>";

    @Test
    public void rawUnparsedMetadata() throws Exception {
        assertEquals(xmlString, new StringElement(xmlString).asUnparsedString());
    }

    @Test
    public void stringElementWrite() throws Exception {

        StringElement stringElement = new StringElement(xmlString);

        final ByteArrayOutputStream resultStream = new ByteArrayOutputStream();

        XmlWriter writer = new XmlWriter(resultStream);
        writer.writeStartDocument();
        writer.writeStartElement("metadata");
        writer.write(stringElement);
        writer.writeEndElement();
        writer.writeEndDocument();

        String resultString = resultStream.toString();

        String expectedOutput =
                "<?xml version='1.0' encoding='UTF-8'?><metadata>" + xmlString + "</metadata>";

        assertEquals(expectedOutput, resultString);
    }
}
