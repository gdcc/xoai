/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.xml;

import io.gdcc.xoai.xmlio.exceptions.XmlWriteException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLStreamException;

/**
 * Create a {@link XmlWritable} element that reads from the given {@link InputStream} at the very
 * last moment: when the XML writer is asked to write the content of the element. The input is
 * copied and an XML declaration is removed if present.
 *
 * <p>Obviously, the stream should send XML - although we write anything we receive. No input check
 * is done for speed reasons - it's the using applications responsibility to send valid XML that
 * also withstands namespace checks!
 *
 * <p>Note: you cannot write at the root level with this element, as the StAX writer needs at least
 * one wrapping element.
 */
public class CopyElement implements XmlWritable {

    protected final InputStream xmlInputStream;

    /**
     * Create the element and associate with the InputStream
     *
     * @param xmlInputStream The stream to write when this element gets written.
     */
    public CopyElement(final InputStream xmlInputStream) {
        Objects.requireNonNull(xmlInputStream);
        this.xmlInputStream = xmlInputStream;
    }

    @Override
    public void write(XmlWriter writer) throws XmlWriteException {
        try {
            // Make the XmlWriter think we want to write a value, so it prints ">" of the containing
            // element to stream
            // This is somewhat hacky, but there is no other possibility to trick the StAX API into
            // this.
            writer.writeCharacters("");
            // Flush the XmlWriter to make sure any preceding tags are written out
            writer.flush();

            // Now let's write the actual content
            writeXml(writer);

            // And flush stream & writer after the operation - again
            writer.flush();
        } catch (XMLStreamException | IOException e) {
            throw new XmlWriteException(e);
        }
    }

    /**
     * A matcher, created only once, reusable to match the XML declaration with any attributes.
     * Non-greedy, so we do not interfere with any XML processing instructions following.
     */
    private static final Matcher xmlDeclaration = Pattern.compile("<\\?xml .*?\\?>").matcher("");

    protected void writeXml(XmlWriter writer) throws IOException {

        /*
         * The - optional but recommended - xml declaration, if present, MUST be the first and only element of the input.
         * It may not be preceded by a comment or similar. No matter how long it is exactly, the limited number of
         * attributes inside <?xml version='1.0' encoding='UTF-8' standalone='yes' ?> make it VERY unlikely it is going
         * to exceed 1024 characters.
         *
         * If we don't find the declaration within this first section, but it's still present - that's invalid XML
         * and the application must take care of it.
         */

        try (xmlInputStream;
                // create a buffered stream to allow for rewind
                BufferedInputStream bufferedXmlInputStream =
                        new BufferedInputStream(xmlInputStream)) {

            final int n = 1024;
            // mark the start of the stream, leave 1 byte headroom for reading ahead
            bufferedXmlInputStream.mark(n + 1);
            // read the first bytes
            byte[] bytes = bufferedXmlInputStream.readNBytes(n);

            // broken UTF-8 multibyte char only possible when >= 1024 chars available
            if (bytes.length == n) {
                // analyze how far we can read without risk of broken char
                final int maxN = maxBytesWithCompleteUTF8Chars(bytes);
                // if we detected a (potentially broken) mbchar at the end, re-read.
                if (maxN < n) {
                    // reset the stream to begin
                    bufferedXmlInputStream.reset();
                    // replace bytes by reading again, but only so far as maxN
                    bytes = bufferedXmlInputStream.readNBytes(maxN);
                }
            }

            String firstChars = new String(bytes, StandardCharsets.UTF_8);
            // match the start with the compiled regex and replace with nothing when matching.
            firstChars = xmlDeclaration.reset(firstChars).replaceFirst("");

            // write the chars to the output stream
            writer.getOutputStream().write(firstChars.getBytes(StandardCharsets.UTF_8));

            // now send the rest of the stream
            bufferedXmlInputStream.transferTo(writer.getOutputStream());
        }
    }

    public static int maxBytesWithCompleteUTF8Chars(final byte[] buffer) {
        final int n = buffer.length;

        if (Byte.toUnsignedInt(buffer[n - 1])
                > 127) { // 0b0xxxxxxx are all 1-byte chars, so only seek for 0b1xxxxxxx
            for (int i = n - 1;
                    i > n - 5;
                    i--) { // go max four bytes back (max UTF-8 mbchar length)
                if (Byte.toUnsignedInt(buffer[i])
                        > 191) { // 0b110xxxxx / 0b1110xxxx / 0b11110xxx are the UTF-8 multibyte
                    // char start bytes
                    // -> this is a multibyte start char => use "i" as perfect new length
                    //    (array index starts at 0, so is already -1 for read length)
                    return i;
                }
                // -> this is a byte somewhere inside the mbchar, go -1 byte and try again
            }
        }

        return n;
    }
}
