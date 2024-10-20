/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

public class XMLUtils {
    static final TransformerFactory factory = TransformerFactory.newInstance();

    public static String format(String unformattedXml) {
        final OutputStream out = new ByteArrayOutputStream();

        try (out;
                InputStream in = new ByteArrayInputStream(unformattedXml.getBytes())) {
            Transformer transformer = factory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");

            // initialize StreamResult with File object to save to file
            StreamResult result = new StreamResult(out);
            StreamSource source = new StreamSource(in);

            transformer.transform(source, result);
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not format given XML input", e);
        }

        return out.toString();
    }
}
