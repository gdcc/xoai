/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.exceptions;

import io.gdcc.xoai.model.oaipmh.Error;

/**
 * @author Development @ Lyncode
 * @version 3.1.0
 */
public abstract class OAIException extends Exception {

    private static final long serialVersionUID = -3229816947775660398L;

    /**
     * Retrieve the error code from the enumerated list
     *
     * @return The matching error code
     */
    public abstract Error.Code getErrorCode();

    /**
     * Generate a more meaningfull error message sent to users/logging/... from the error code and
     * the exception message.
     *
     * @return The error message
     */
    public String getErrorMessage() {
        String errorCodeMessage = getErrorCode().message();

        return (errorCodeMessage != null ? errorCodeMessage + ": " : "")
                + (getMessage() != null ? getMessage() : "");
    }

    /** Creates a new instance of <code>OAIException</code> without detail message. */
    protected OAIException() {}

    /**
     * Constructs an instance of <code>OAIException</code> with the specified detail message.
     *
     * @param msg the detail message.
     */
    protected OAIException(String msg) {
        super(msg);
    }

    protected OAIException(Throwable ex) {
        super(ex.getMessage(), ex);
    }

    protected OAIException(String message, Throwable ex) {
        super(message, ex);
    }
}
