/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.results.Description;
import io.gdcc.xoai.model.oaipmh.verbs.Identify;
import io.gdcc.xoai.xml.XmlWritable;
import io.gdcc.xoai.xml.XmlWriter;
import io.gdcc.xoai.xmlio.exceptions.XmlWriteException;
import java.util.List;
import javax.xml.stream.XMLStreamException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class IdentifyHandler extends VerbHandler<Identify> {
    private static final Logger log = LoggerFactory.getLogger(IdentifyHandler.class);

    private static final String PROTOCOL_VERSION = "2.0";
    private static final String XOAI_DESC = "XOAI: OAI-PMH Java Toolkit";

    public IdentifyHandler(Context context, Repository repository) {
        super(context, repository);
    }

    @Override
    public Identify handle(Request request) throws HandlerException {
        Identify identify = new Identify();
        RepositoryConfiguration configuration = getRepository().getConfiguration();
        identify.withBaseURL(configuration.getBaseUrl());
        identify.withRepositoryName(configuration.getRepositoryName());
        for (String mail : configuration.getAdminEmails()) {
            identify.getAdminEmails().add(mail);
        }
        identify.withEarliestDatestamp(configuration.getEarliestDate());
        identify.withDeletedRecord(DeletedRecord.valueOf(configuration.getDeleteMethod().name()));

        identify.withGranularity(configuration.getGranularity());
        identify.withProtocolVersion(PROTOCOL_VERSION);
        if (configuration.hasCompressions())
            for (String com : configuration.getCompressions()) identify.getCompressions().add(com);

        List<String> descriptions = configuration.getDescription();
        if (descriptions == null) {
            try {
                identify.withDescription(
                        new Description(
                                XmlWriter.toString(new XOAIDescription().withValue(XOAI_DESC))));
            } catch (XMLStreamException e) {
                log.warn("Description not added", e);
            }
        } else {
            for (String description : descriptions) {
                identify.getDescriptions().add(new Description().withMetadata(description));
            }
        }

        return identify;
    }

    public static class XOAIDescription implements XmlWritable {
        protected String value;
        protected String type;

        public String getValue() {
            return value;
        }

        public XOAIDescription withValue(String value) {
            this.value = value;
            return this;
        }

        public String getType() {
            return type;
        }

        public XOAIDescription withType(String value) {
            this.type = value;
            return this;
        }

        @Override
        public void write(XmlWriter writer) throws XmlWriteException {
            try {
                writer.writeStartElement("XOAIDescription");
                writer.writeAttribute("type", getType());
                writer.writeCharacters(getValue());
                writer.writeEndElement();
            } catch (XMLStreamException e) {
                throw new XmlWriteException(e);
            }
        }
    }
}
