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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.xmlunit.matchers.HasXPathMatcher.hasXPath;

import java.io.InputStream;
import org.junit.jupiter.api.Test;

public class XmlReaderTest {
    private InputStream inputStream =
            XmlReaderTest.class.getClassLoader().getResourceAsStream("example.xml");

    @Test
    public void testRetrieveCurrentAsString() throws Exception {
        XmlReader reader = new XmlReader(inputStream);
        reader.next(aStartElement()).next(aStartElement());
        String string = reader.retrieveCurrentAsString();

        assertThat(string, hasXPath("/one/two"));
    }
}
