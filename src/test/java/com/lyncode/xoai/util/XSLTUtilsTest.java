package com.lyncode.xoai.util;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import com.lyncode.xoai.dataprovider.core.ItemMetadata;
import com.lyncode.xoai.dataprovider.data.AbstractItem;
import com.lyncode.xoai.dataprovider.exceptions.XSLTransformationException;

import static org.mockito.Mockito.*;


public class XSLTUtilsTest {
    public static String SCHEMA_XSL = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" + 
    		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"" + 
    		"    xmlns:doc=\"http://www.lyncode.com/xoai\" version=\"1.0\">" + 
    		"    <xsl:output omit-xml-declaration=\"yes\" method=\"xml\" indent=\"yes\" />" + 
    		"    <!-- An identity transformation to show the internal XOAI generated XML -->" + 
    		"    <xsl:template match=\"/\">" + 
    		"        <dim:dim xmlns:dim=\"http://www.dspace.org/xmlns/dspace/dim\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
    		"            xsi:schemaLocation=\"http://www.dspace.org/xmlns/dspace/dim http://www.dspace.org/schema/dim.xsd\">" + 
    		"            <xsl:for-each select=\"doc:metadata/doc:element[@name='dc']/doc:element/doc:element\">" + 
    		"                <xsl:choose>" + 
    		"                    <xsl:when test=\"doc:element\">" + 
    		"                        <dim:field>" + 
    		"                            <xsl:attribute name=\"mdschema\">" + 
    		"                                <xsl:value-of select=\"../../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:attribute name=\"element\">" + 
    		"                                <xsl:value-of select=\"../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:attribute name=\"qualifier\">" + 
    		"                                <xsl:value-of select=\"@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:choose>" + 
    		"                                <xsl:when test=\"doc:element[@name='none']\"></xsl:when>" + 
    		"                                <xsl:otherwise>" + 
    		"                                    <xsl:attribute name=\"lang\">" + 
    		"                                        <xsl:value-of select=\"doc:element/@name\" />" + 
    		"                                    </xsl:attribute>" + 
    		"                                </xsl:otherwise>" + 
    		"                            </xsl:choose>" + 
    		"                            <xsl:if test=\"doc:element/doc:field[@name='authority']\">" + 
    		"                                <xsl:attribute name=\"authority\">" + 
    		"                                    <xsl:value-of select=\"doc:element/doc:field[@name='authority']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:if test=\"doc:element/doc:field[@name='confidence']\">" + 
    		"                                <xsl:attribute name=\"confidence\">" + 
    		"                                    <xsl:value-of select=\"doc:element/doc:field[@name='confidence']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:value-of select=\"doc:element/doc:field[@name='value']/text()\"></xsl:value-of>" + 
    		"                        </dim:field>" + 
    		"                    </xsl:when>" + 
    		"                    <xsl:otherwise>" + 
    		"                        <dim:field>" + 
    		"                            <xsl:attribute name=\"mdschema\">" + 
    		"                                <xsl:value-of select=\"../../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:attribute name=\"element\">" + 
    		"                                <xsl:value-of select=\"../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:choose>" + 
    		"                                <xsl:when test=\"@name='none'\"></xsl:when>" + 
    		"                                <xsl:otherwise>" + 
    		"                                    <xsl:attribute name=\"lang\">" + 
    		"                                        <xsl:value-of select=\"@name\" />" + 
    		"                                    </xsl:attribute>" + 
    		"                                </xsl:otherwise>" + 
    		"                            </xsl:choose>" + 
    		"                            <xsl:if test=\"doc:field[@name='authority']\">" + 
    		"                                <xsl:attribute name=\"authority\">" + 
    		"                                    <xsl:value-of select=\"doc:field[@name='authority']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:if test=\"doc:field[@name='confidence']\">" + 
    		"                                <xsl:attribute name=\"confidence\">" + 
    		"                                    <xsl:value-of select=\"doc:field[@name='confidence']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:value-of select=\"doc:field[@name='value']/text()\"></xsl:value-of>" + 
    		"                        </dim:field>" + 
    		"                    </xsl:otherwise>" + 
    		"                </xsl:choose>" + 
    		"            </xsl:for-each>" + 
    		"        </dim:dim>" + 
    		"    </xsl:template>" + 
    		"</xsl:stylesheet>";
    public static String METADATA_XSL = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>" +
    		"<xsl:stylesheet xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\"" + 
    		"    xmlns:doc=\"http://www.lyncode.com/xoai\" version=\"1.0\">" + 
    		"    <xsl:output omit-xml-declaration=\"yes\" method=\"xml\" indent=\"yes\" />" + 
    		"" + 
    		"    <!-- An identity transformation to show the internal XOAI generated XML -->" + 
    		"    <xsl:template match=\"/\">" + 
    		"        <dim:dim xmlns:dim=\"http://www.dspace.org/xmlns/dspace/dim\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " + 
    		"            xsi:schemaLocation=\"http://www.dspace.org/xmlns/dspace/dim http://www.dspace.org/schema/dim.xsd\">" + 
    		"            <xsl:for-each select=\"doc:metadata/doc:element[@name='dc']/doc:element/doc:element\">" + 
    		"                <xsl:choose>" + 
    		"                    <xsl:when test=\"doc:element\">" + 
    		"                        <dim:field>" + 
    		"                            <xsl:attribute name=\"mdschema\">" + 
    		"                                <xsl:value-of select=\"../../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:attribute name=\"element\">" + 
    		"                                <xsl:value-of select=\"../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:attribute name=\"qualifier\">" + 
    		"                                <xsl:value-of select=\"@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:choose>" + 
    		"                                <xsl:when test=\"doc:element[@name='none']\"></xsl:when>" + 
    		"                                <xsl:otherwise>" + 
    		"                                    <xsl:attribute name=\"lang\">" + 
    		"                                        <xsl:value-of select=\"doc:element/@name\" />" + 
    		"                                    </xsl:attribute>" + 
    		"                                </xsl:otherwise>" + 
    		"                            </xsl:choose>" + 
    		"                            <xsl:if test=\"doc:element/doc:field[@name='authority']\">" + 
    		"                                <xsl:attribute name=\"authority\">" + 
    		"                                    <xsl:value-of select=\"doc:element/doc:field[@name='authority']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:if test=\"doc:element/doc:field[@name='confidence']\">" + 
    		"                                <xsl:attribute name=\"confidence\">" + 
    		"                                    <xsl:value-of select=\"doc:element/doc:field[@name='confidence']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:value-of select=\"doc:element/doc:field[@name='value']/text()\"></xsl:value-of>" + 
    		"                        </dim:field>" + 
    		"                    </xsl:when>" + 
    		"                    <xsl:otherwise>" + 
    		"                        <dim:field>" + 
    		"                            <xsl:attribute name=\"mdschema\">" + 
    		"                                <xsl:value-of select=\"../../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:attribute name=\"element\">" + 
    		"                                <xsl:value-of select=\"../@name\" />" + 
    		"                            </xsl:attribute>" + 
    		"                            <xsl:choose>" + 
    		"                                <xsl:when test=\"@name='none'\"></xsl:when>" + 
    		"                                <xsl:otherwise>" + 
    		"                                    <xsl:attribute name=\"lang\">" + 
    		"                                        <xsl:value-of select=\"@name\" />" + 
    		"                                    </xsl:attribute>" + 
    		"                                </xsl:otherwise>" + 
    		"                            </xsl:choose>" + 
    		"                            <xsl:if test=\"doc:field[@name='authority']\">" + 
    		"                                <xsl:attribute name=\"authority\">" + 
    		"                                    <xsl:value-of select=\"doc:field[@name='authority']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:if test=\"doc:field[@name='confidence']\">" + 
    		"                                <xsl:attribute name=\"confidence\">" + 
    		"                                    <xsl:value-of select=\"doc:field[@name='confidence']/text()\" />" + 
    		"                                </xsl:attribute>" + 
    		"                            </xsl:if>" + 
    		"                            <xsl:value-of select=\"doc:field[@name='value']/text()\"></xsl:value-of>" + 
    		"                        </dim:field>" + 
    		"                    </xsl:otherwise>" + 
    		"                </xsl:choose>" + 
    		"            </xsl:for-each>" + 
    		"        </dim:dim>" + 
    		"    </xsl:template>" + 
    		"</xsl:stylesheet>";
    public static String SAMPLE_DATA = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?><metadata xmlns=\"http://www.lyncode.com/xoai\">" + 
    		"    <element name=\"dc\">" + 
    		"        <element name=\"creator\">" + 
    		"            <element name=\"en_US\">" + 
    		"                <field name=\"value\">Cat, Lily</field>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"date\">" + 
    		"            <element name=\"accessioned\">" + 
    		"                <element name=\"none\">" + 
    		"                    <field name=\"value\">1982-06-26T19:58:24Z</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"            <element name=\"available\">" + 
    		"                <element name=\"none\">" + 
    		"                    <field name=\"value\">1982-06-26T19:58:24Z</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"            <element name=\"created\">" + 
    		"                <element name=\"en_US\">" + 
    		"                    <field name=\"value\">1982-11-11</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"            <element name=\"issued\">" + 
    		"                <element name=\"none\">" + 
    		"                    <field name=\"value\">1982-11-11</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"identifier\">" + 
    		"            <element name=\"issn\">" + 
    		"                <element name=\"en_US\">" + 
    		"                    <field name=\"value\">123456789</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"            <element name=\"uri\">" + 
    		"                <element name=\"none\">" + 
    		"                    <field name=\"value\">http://hdl.handle.net/10673/4</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"description\">" + 
    		"            <element name=\"abstract\">" + 
    		"                <element name=\"en_US\">" + 
    		"                    <field name=\"value\">This is a Sample HTML webpage including several images and styles (CSS).</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"            <element name=\"provenance\">" + 
    		"                <element name=\"en\">" + 
    		"                    <field name=\"value\">Made available in DSpace on 2012-06-26T19:58:24Z (GMT). No. of bitstreams: 7&#13;" + 
    		"Lily-cat-of-day.htm: 39970 bytes, checksum: aae901336b56ae14070fdec8c79dd48e (MD5)&#13;" + 
    		"cl_style.css: 1181 bytes, checksum: 58178d41c221520d88333b9d482b31b8 (MD5)&#13;" + 
    		"kitty-1257950690.jpg: 83390 bytes, checksum: 729596bb3ad09ebe958a150787418212 (MD5)&#13;" + 
    		"kitty-1257950690_002.jpg: 86057 bytes, checksum: 37c07c8488042d064bd945765d9e2f98 (MD5)&#13;" + 
    		"logo.png: 4157 bytes, checksum: d64fb5f69c7820685d5ed3037b9670ed (MD5)&#13;" + 
    		"style.css: 49623 bytes, checksum: 89c2a0557993a8665574c0b52fc1582d (MD5)&#13;" + 
    		"stylesheet.css: 877 bytes, checksum: d2b580ac1c89ae88dc05dcd9108b002e (MD5)</field>" + 
    		"                    <field name=\"value\">Restored into DSpace on 2013-06-13T09:17:32Z (GMT).</field>" + 
    		"                    <field name=\"value\">Restored into DSpace on 2013-06-13T11:04:13Z (GMT).</field>" + 
    		"                    <field name=\"value\">Restored into DSpace on 2013-09-01T00:01:40Z (GMT).</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"language\">" + 
    		"            <element name=\"en_US\">" + 
    		"                <field name=\"value\">en</field>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"rights\">" + 
    		"            <element name=\"en_US\">" + 
    		"                <field name=\"value\">© EverCats.com</field>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"subject\">" + 
    		"            <element name=\"en_US\">" + 
    		"                <field name=\"value\">cat</field>" + 
    		"                <field name=\"value\">calico</field>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"title\">" + 
    		"            <element name=\"en_US\">" + 
    		"                <field name=\"value\">Test Webpage</field>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"type\">" + 
    		"            <element name=\"en_US\">" + 
    		"                <field name=\"value\">text</field>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"    </element>" + 
    		"    <element name=\"bundles\">" + 
    		"        <element name=\"bundle\">" + 
    		"            <field name=\"name\">ORIGINAL</field>" + 
    		"            <element name=\"bitstreams\">" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">Lily-cat-of-day.htm</field>" + 
    		"                    <field name=\"format\">text/html</field>" + 
    		"                    <field name=\"size\">39970</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/1/bitstream</field>" + 
    		"                    <field name=\"checksum\">aae901336b56ae14070fdec8c79dd48e</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">1</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">cl_style.css</field>" + 
    		"                    <field name=\"format\">text/css</field>" + 
    		"                    <field name=\"size\">1181</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/2/bitstream</field>" + 
    		"                    <field name=\"checksum\">58178d41c221520d88333b9d482b31b8</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">2</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">kitty-1257950690.jpg</field>" + 
    		"                    <field name=\"format\">image/jpeg</field>" + 
    		"                    <field name=\"size\">83390</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/3/bitstream</field>" + 
    		"                    <field name=\"checksum\">729596bb3ad09ebe958a150787418212</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">3</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">kitty-1257950690_002.jpg</field>" + 
    		"                    <field name=\"format\">image/jpeg</field>" + 
    		"                    <field name=\"size\">86057</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/4/bitstream</field>" + 
    		"                    <field name=\"checksum\">37c07c8488042d064bd945765d9e2f98</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">4</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">logo.png</field>" + 
    		"                    <field name=\"format\">image/png</field>" + 
    		"                    <field name=\"size\">4157</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/5/bitstream</field>" + 
    		"                    <field name=\"checksum\">d64fb5f69c7820685d5ed3037b9670ed</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">5</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">style.css</field>" + 
    		"                    <field name=\"format\">text/css</field>" + 
    		"                    <field name=\"size\">49623</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/6/bitstream</field>" + 
    		"                    <field name=\"checksum\">89c2a0557993a8665574c0b52fc1582d</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">6</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">stylesheet.css</field>" + 
    		"                    <field name=\"format\">text/css</field>" + 
    		"                    <field name=\"size\">877</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/7/bitstream</field>" + 
    		"                    <field name=\"checksum\">d2b580ac1c89ae88dc05dcd9108b002e</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">7</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"bundle\">" + 
    		"            <field name=\"name\">LICENSE</field>" + 
    		"            <element name=\"bitstreams\">" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">license.txt</field>" + 
    		"                    <field name=\"originalName\">license.txt</field>" + 
    		"                    <field name=\"format\">text/plain; charset=utf-8</field>" + 
    		"                    <field name=\"size\">1748</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/8/bitstream</field>" + 
    		"                    <field name=\"checksum\">8a4605be74aa9ea9d79846c1fba20a33</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">8</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"bundle\">" + 
    		"            <field name=\"name\">TEXT</field>" + 
    		"            <element name=\"bitstreams\">" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">Lily-cat-of-day.htm.txt</field>" + 
    		"                    <field name=\"originalName\">Lily-cat-of-day.htm.txt</field>" + 
    		"                    <field name=\"format\">text/plain</field>" + 
    		"                    <field name=\"size\">5026</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/9/bitstream</field>" + 
    		"                    <field name=\"checksum\">fe0041b3efc9c21308752f4e51dbf4f9</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">9</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"        <element name=\"bundle\">" + 
    		"            <field name=\"name\">THUMBNAIL</field>" + 
    		"            <element name=\"bitstreams\">" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">kitty-1257950690.jpg.jpg</field>" + 
    		"                    <field name=\"originalName\">kitty-1257950690.jpg.jpg</field>" + 
    		"                    <field name=\"format\">image/jpeg</field>" + 
    		"                    <field name=\"size\">2271</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/10/bitstream</field>" + 
    		"                    <field name=\"checksum\">ca818144119599efeab8915b406c5584</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">10</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">kitty-1257950690_002.jpg.jpg</field>" + 
    		"                    <field name=\"originalName\">kitty-1257950690_002.jpg.jpg</field>" + 
    		"                    <field name=\"format\">image/jpeg</field>" + 
    		"                    <field name=\"size\">1772</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/11/bitstream</field>" + 
    		"                    <field name=\"checksum\">40d67d58a7cc47c3e37c56a5c2e48479</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">11</field>" + 
    		"                </element>" + 
    		"                <element name=\"bitstream\">" + 
    		"                    <field name=\"name\">logo.png.jpg</field>" + 
    		"                    <field name=\"originalName\">logo.png.jpg</field>" + 
    		"                    <field name=\"format\">image/jpeg</field>" + 
    		"                    <field name=\"size\">1279</field>" + 
    		"                    <field name=\"url\">http://demo.dspace.org/xmlui/bitstream/10673%2F4/12/bitstream</field>" + 
    		"                    <field name=\"checksum\">4a88efd091aaa0b4619011bd0dff5f00</field>" + 
    		"                    <field name=\"checksumAlgorithm\">MD5</field>" + 
    		"                    <field name=\"sid\">12</field>" + 
    		"                </element>" + 
    		"            </element>" + 
    		"        </element>" + 
    		"    </element>" + 
    		"    <element name=\"others\">" + 
    		"        <field name=\"handle\">10673/4</field>" + 
    		"        <field name=\"identifier\">oai:demo.dspace.org:10673/4</field>" + 
    		"        <field name=\"lastModifyDate\">2013-09-01 02:00:26.763</field>" + 
    		"    </element>" + 
    		"    <element name=\"repository\">" + 
    		"        <field name=\"name\">DSpace Demo Repository</field>" + 
    		"        <field name=\"mail\">dspacedemo@gmail.com</field>" + 
    		"    </element>" + 
    		"    <element name=\"license\">" + 
    		"        <field name=\"bin\">Tk9URTogUExBQ0UgWU9VUiBPV04gTElDRU5TRSBIRVJFClRoaXMgc2FtcGxlIGxpY2Vuc2UgaXMgcHJvdmlkZWQgZm9yIGluZm9ybWF0aW9uYWwgcHVycG9zZXMgb25seS4KCk5PTi1FWENMVVNJVkUgRElTVFJJQlVUSU9OIExJQ0VOU0UKCkJ5IHNpZ25pbmcgYW5kIHN1Ym1pdHRpbmcgdGhpcyBsaWNlbnNlLCB5b3UgKHRoZSBhdXRob3Iocykgb3IgY29weXJpZ2h0Cm93bmVyKSBncmFudHMgdG8gRFNwYWNlIFVuaXZlcnNpdHkgKERTVSkgdGhlIG5vbi1leGNsdXNpdmUgcmlnaHQgdG8gcmVwcm9kdWNlLAp0cmFuc2xhdGUgKGFzIGRlZmluZWQgYmVsb3cpLCBhbmQvb3IgZGlzdHJpYnV0ZSB5b3VyIHN1Ym1pc3Npb24gKGluY2x1ZGluZwp0aGUgYWJzdHJhY3QpIHdvcmxkd2lkZSBpbiBwcmludCBhbmQgZWxlY3Ryb25pYyBmb3JtYXQgYW5kIGluIGFueSBtZWRpdW0sCmluY2x1ZGluZyBidXQgbm90IGxpbWl0ZWQgdG8gYXVkaW8gb3IgdmlkZW8uCgpZb3UgYWdyZWUgdGhhdCBEU1UgbWF5LCB3aXRob3V0IGNoYW5naW5nIHRoZSBjb250ZW50LCB0cmFuc2xhdGUgdGhlCnN1Ym1pc3Npb24gdG8gYW55IG1lZGl1bSBvciBmb3JtYXQgZm9yIHRoZSBwdXJwb3NlIG9mIHByZXNlcnZhdGlvbi4KCllvdSBhbHNvIGFncmVlIHRoYXQgRFNVIG1heSBrZWVwIG1vcmUgdGhhbiBvbmUgY29weSBvZiB0aGlzIHN1Ym1pc3Npb24gZm9yCnB1cnBvc2VzIG9mIHNlY3VyaXR5LCBiYWNrLXVwIGFuZCBwcmVzZXJ2YXRpb24uCgpZb3UgcmVwcmVzZW50IHRoYXQgdGhlIHN1Ym1pc3Npb24gaXMgeW91ciBvcmlnaW5hbCB3b3JrLCBhbmQgdGhhdCB5b3UgaGF2ZQp0aGUgcmlnaHQgdG8gZ3JhbnQgdGhlIHJpZ2h0cyBjb250YWluZWQgaW4gdGhpcyBsaWNlbnNlLiBZb3UgYWxzbyByZXByZXNlbnQKdGhhdCB5b3VyIHN1Ym1pc3Npb24gZG9lcyBub3QsIHRvIHRoZSBiZXN0IG9mIHlvdXIga25vd2xlZGdlLCBpbmZyaW5nZSB1cG9uCmFueW9uZSdzIGNvcHlyaWdodC4KCklmIHRoZSBzdWJtaXNzaW9uIGNvbnRhaW5zIG1hdGVyaWFsIGZvciB3aGljaCB5b3UgZG8gbm90IGhvbGQgY29weXJpZ2h0LAp5b3UgcmVwcmVzZW50IHRoYXQgeW91IGhhdmUgb2J0YWluZWQgdGhlIHVucmVzdHJpY3RlZCBwZXJtaXNzaW9uIG9mIHRoZQpjb3B5cmlnaHQgb3duZXIgdG8gZ3JhbnQgRFNVIHRoZSByaWdodHMgcmVxdWlyZWQgYnkgdGhpcyBsaWNlbnNlLCBhbmQgdGhhdApzdWNoIHRoaXJkLXBhcnR5IG93bmVkIG1hdGVyaWFsIGlzIGNsZWFybHkgaWRlbnRpZmllZCBhbmQgYWNrbm93bGVkZ2VkCndpdGhpbiB0aGUgdGV4dCBvciBjb250ZW50IG9mIHRoZSBzdWJtaXNzaW9uLgoKSUYgVEhFIFNVQk1JU1NJT04gSVMgQkFTRUQgVVBPTiBXT1JLIFRIQVQgSEFTIEJFRU4gU1BPTlNPUkVEIE9SIFNVUFBPUlRFRApCWSBBTiBBR0VOQ1kgT1IgT1JHQU5JWkFUSU9OIE9USEVSIFRIQU4gRFNVLCBZT1UgUkVQUkVTRU5UIFRIQVQgWU9VIEhBVkUKRlVMRklMTEVEIEFOWSBSSUdIVCBPRiBSRVZJRVcgT1IgT1RIRVIgT0JMSUdBVElPTlMgUkVRVUlSRUQgQlkgU1VDSApDT05UUkFDVCBPUiBBR1JFRU1FTlQuCgpEU1Ugd2lsbCBjbGVhcmx5IGlkZW50aWZ5IHlvdXIgbmFtZShzKSBhcyB0aGUgYXV0aG9yKHMpIG9yIG93bmVyKHMpIG9mIHRoZQpzdWJtaXNzaW9uLCBhbmQgd2lsbCBub3QgbWFrZSBhbnkgYWx0ZXJhdGlvbiwgb3RoZXIgdGhhbiBhcyBhbGxvd2VkIGJ5IHRoaXMKbGljZW5zZSwgdG8geW91ciBzdWJtaXNzaW9uLgo=</field>" + 
    		"    </element>" + 
    		"</metadata>";
    
    @Before
    public void setUp() throws Exception {

    }

    @Test
    public void shouldTransformItRight() throws IOException, XSLTransformationException {
        InputStream xmlSchemaTransform = IOUtils.toInputStream(SCHEMA_XSL);
        InputStream xmlMetadataTransform = IOUtils.toInputStream(METADATA_XSL);
        
        AbstractItem item = mock(AbstractItem.class);
        ItemMetadata metadata = new ItemMetadata(SAMPLE_DATA);
        
        when(item.getMetadata()).thenReturn(metadata);
        
        String result = XSLTUtils.transform(xmlMetadataTransform, xmlSchemaTransform, item);

        //System.out.println(result);
        //
        
        //assertEquals(result, "<dim");
    }

    @Test
    public void shouldTransformItRight2() throws IOException, XSLTransformationException {
        InputStream xmlSchemaTransform = IOUtils.toInputStream(SCHEMA_XSL);
        
        AbstractItem item = mock(AbstractItem.class);
        ItemMetadata metadata = new ItemMetadata(SAMPLE_DATA);
        
        when(item.getMetadata()).thenReturn(metadata);
        
        String result = XSLTUtils.transform(xmlSchemaTransform, item);
        //System.out.println(result);
        //assertEquals(result, "<dim");
    }

}
