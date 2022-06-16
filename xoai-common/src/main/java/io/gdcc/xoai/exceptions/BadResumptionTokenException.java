/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.exceptions;

import io.gdcc.xoai.model.oaipmh.Error;

public class BadResumptionTokenException extends OAIException {
    @Override
    public Error.Code getErrorCode() {
        return Error.Code.BAD_RESUMPTION_TOKEN;
    }

    public BadResumptionTokenException() {}

    public BadResumptionTokenException(String message) {
        super(message);
    }

    public BadResumptionTokenException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadResumptionTokenException(Throwable cause) {
        super(cause);
    }
}
