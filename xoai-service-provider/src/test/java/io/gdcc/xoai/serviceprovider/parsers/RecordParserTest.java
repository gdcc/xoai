/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.serviceprovider.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.gdcc.xoai.model.oaipmh.results.Record;
import io.gdcc.xoai.serviceprovider.model.Context;
import io.gdcc.xoai.serviceprovider.model.Context.KnownTransformer;
import io.gdcc.xoai.xmlio.XmlReader;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class RecordParserTest {

    private InputStream input;
    private Context context;
    private RecordParser parser;

    @BeforeEach
    public void setUp() {
        input = getClass().getClassLoader().getResourceAsStream("test/oai_dc-valid.xml");

        context = new Context().withMetadataTransformer("oai_dc", KnownTransformer.OAI_DC);
    }

    @Test
    public void multipleElementsFound() throws Exception {

        parser = new RecordParser(context, "oai_dc");
        XmlReader reader = new XmlReader(input);
        Record record = parser.parse(reader);
        assertEquals(
                2, record.getMetadata().getXoaiMetadata().searcher().findAll("dc.rights").size());
    }

    @Test
    public void xmlLangIsParsed() throws Exception {

        parser = new RecordParser(context, "oai_dc");
        XmlReader reader = new XmlReader(input);
        Record record = parser.parse(reader);
        assertEquals(
                2,
                record.getMetadata()
                        .getXoaiMetadata()
                        .searcher()
                        .findAll("dc.rights:xml:lang")
                        .size());
    }

    @Test
    public void cdataParsing() throws Exception {
        input = getClass().getClassLoader().getResourceAsStream("test/oai_dc-CDATA.xml");

        parser = new RecordParser(context, "oai_dc");
        XmlReader reader = new XmlReader(input);
        Record record = parser.parse(reader);
        assertEquals(
                1, record.getMetadata().getXoaiMetadata().searcher().findAll("dc.title").size());
        assertEquals(
                "Article Title-additional CDATA",
                record.getMetadata().getXoaiMetadata().searcher().findOne("dc.title"));
    }
    
    @Test
    public void rawUnparsedMetadata() throws Exception {
        parser = new RecordParser(context, "oai_dc");
        XmlReader reader = new XmlReader(input);
        Record record = parser.parse(reader);
        
        assertNull(record.getMetadata().asUnparsedString());
        
        context = context.withSaveUnparsedMetadata(); 
        parser = new RecordParser(context, "");
        input = getClass().getClassLoader().getResourceAsStream("test/oai_dc-CDATA.xml");
        
        reader = new XmlReader(input);
        record = parser.parse(reader); 
        
        assertNull(record.getMetadata().getXoaiMetadata());
        
        assertEquals(
            "<oai_dc:dc xmlns:oai_dc=\"http://www.openarchives.org/OAI/2.0/oai_dc/\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.openarchives.org/OAI/2.0/oai_dc/  http://www.openarchives.org/OAI/2.0/oai_dc.xsd\">\n" +
            "	<dc:title>Article Title-additional CDATA</dc:title>\n" +
            "</oai_dc:dc>",
            record.getMetadata().asUnparsedString());
        
    }
}
