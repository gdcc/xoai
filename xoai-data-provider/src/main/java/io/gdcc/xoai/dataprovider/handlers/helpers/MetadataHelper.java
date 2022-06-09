package io.gdcc.xoai.dataprovider.handlers.helpers;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.model.oaipmh.results.record.Metadata;
import io.gdcc.xoai.xml.EchoElement;
import io.gdcc.xoai.xml.XSLPipeline;
import io.gdcc.xoai.xml.XmlWriter;

import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MetadataHelper {
    public static Metadata process(Metadata metadata, MetadataFormat format, Context context) {
        try (
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            XmlWriter writer = new XmlWriter(outStream);
        ) {
            metadata.write(writer);
            writer.flush();
            
            XSLPipeline pipeline = new XSLPipeline(new ByteArrayInputStream(outStream.toByteArray()), true);
            
            EchoElement element = new EchoElement(
                pipeline.apply(context.getTransformer())
                    .apply(format.getTransformer())
                    .process());
            
            return new Metadata(element);
        } catch (XMLStreamException | TransformerException | IOException e) {
            throw new InternalOAIException(e);
        }
    }
}
