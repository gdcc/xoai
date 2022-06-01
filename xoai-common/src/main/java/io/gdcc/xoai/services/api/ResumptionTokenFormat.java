/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.services.api;

import io.gdcc.xoai.exceptions.BadResumptionTokenException;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;

public interface ResumptionTokenFormat {
    ResumptionTokenFormat withGranularity(Granularity granularity);
    String format(ResumptionToken.Value value);
    ResumptionToken.Value parse(String value) throws BadResumptionTokenException;
}
