package io.gdcc.xoai.xml;

import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.services.api.ResumptionTokenFormat;
import io.gdcc.xoai.services.impl.SimpleResumptionTokenFormat;

public interface WriterContext {
    
    default Granularity getGranularity() {
        return Granularity.Second;
    }
    
    default ResumptionTokenFormat getResumptionTokenFormat() {
        return new SimpleResumptionTokenFormat().withGranularity(getGranularity());
    }
    
}
