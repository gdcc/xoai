/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.services.impl.metadataSearchImpl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import io.gdcc.xoai.model.xoai.Element;
import io.gdcc.xoai.model.xoai.XOAIMetadata;
import io.gdcc.xoai.services.impl.MetadataSearchImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class MetadataSearchImplTest {

    private XOAIMetadata metadata;
    private Element creatorElement;
    private Element subjectElement;

    @BeforeEach
    public void setUp() {
        metadata = new XOAIMetadata();

        Element parentElement = new Element("dc");

        creatorElement = new Element("creator");
        creatorElement.withField("value", "Sousa, Jesus Maria Angélica Fernandes");

        subjectElement = new Element("subject");
        subjectElement.withField(null, "Ciências da Educação");

        parentElement.withElement(creatorElement);
        parentElement.withElement(subjectElement);

        metadata.withElement(parentElement);
    }

    @Test
    public void metadataSearchImplConstructorTest() {
        MetadataSearchImpl searcher = new MetadataSearchImpl(metadata);

        assertThat(2, equalTo(searcher.index().size()));
        assertThat(
                searcher.findOne("dc.creator"), equalTo("Sousa, Jesus Maria Angélica Fernandes"));
        assertThat(searcher.findOne("dc.subject"), equalTo("Ciências da Educação"));
    }

    /**
     * Example to showcase that a field with a ":" will be added to the MetadataSearchImpl's index
     */
    @Test
    public void langPropertyIsAdded() {
        creatorElement.withField("xml:lang", "pt-PT");
        subjectElement.withField("xml:lang", "pt-PT");
        MetadataSearchImpl searcher = new MetadataSearchImpl(metadata);

        assertThat(4, equalTo(searcher.index().size()));
        assertThat(searcher.findOne("dc.creator:xml:lang"), equalTo("pt-PT"));
        assertThat(searcher.findOne("dc.subject:xml:lang"), equalTo("pt-PT"));
    }
}
