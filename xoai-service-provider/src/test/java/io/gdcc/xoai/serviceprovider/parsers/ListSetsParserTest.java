/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.serviceprovider.parsers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import io.gdcc.xoai.model.oaipmh.results.Set;
import io.gdcc.xoai.xmlio.XmlReader;
import io.gdcc.xoai.xmlio.exceptions.XmlReaderException;
import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ListSetsParserTest {

    private static final String OAI_DC_SETS_CDATA_XML =
            "test/oai_dc/listsets/oai_dc-sets-CDATA.xml";

    @Test
    public void normalParsing() {
        InputStream inputStream =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream("test/oai_dc/listsets/oai_dc-sets.xml");

        List<Set> sets = parseXML(inputStream);
        assertEquals(2, sets.size());
        assertEquals("setOne", sets.get(0).getSpec());
        assertEquals("Set One", sets.get(0).getName());
        assertEquals("setTwo", sets.get(1).getSpec());
        assertEquals("Set Two", sets.get(1).getName());
    }

    @Test
    public void cdataIsParsed() {
        InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream(OAI_DC_SETS_CDATA_XML);

        List<Set> sets = parseXML(inputStream);
        assertEquals("cdataSPEC", sets.get(0).getSpec());
        assertEquals("Set with CDATA", sets.get(0).getName());
    }

    @Test
    public void multipleCDATAParsed() {
        InputStream inputStream =
                getClass().getClassLoader().getResourceAsStream(OAI_DC_SETS_CDATA_XML);

        List<Set> sets = parseXML(inputStream);
        assertEquals(2, sets.size());
        assertEquals("First CDATA, Set 2 with CDATA", sets.get(1).getName());
    }

    private List<Set> parseXML(InputStream inputStream) {
        XmlReader reader;
        try {
            reader = new XmlReader(inputStream);
            ListSetsParser parser = new ListSetsParser(reader);
            List<Set> sets = parser.parse();
            return sets;

        } catch (XmlReaderException e) {
            fail("unexpected exception reading the xml: " + e.getCause().toString());
        }
        return null;
    }
}
