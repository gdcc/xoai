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
    
    /**
     * This is used for Dataverse 4/5 backward compatibility, because they added an attribute to the
     * <code>&lt;record&gt;&lt;metadata&gt;</code> element, containing the API URL of a record in their
     * special metadata format "dataverse_json".
     *
     * The data provider repository configuration uses this to make the behaviour configurable.
     *
     * @deprecated Remove when Dataverse 6 is old enough that no ones uses this workaround anymore.
     * @return true when {@link io.gdcc.xoai.model.oaipmh.results.Record} should add attributes to <code>&lt;metadata&gt;</code>,
     *         false otherwise (default)
     */
    @Deprecated(since = "5.0")
    default boolean isMetadataAttributesEnabled() {
        return false;
    }
    
}
