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
public class BadArgumentException extends OAIException {

    private static final long serialVersionUID = 6436751364163509217L;

    @Override
    public Error.Code getErrorCode() {
        return Error.Code.BAD_ARGUMENT;
    }

    /**
     * Constructs an instance of <code>BadArgumentException</code> with the specified detail
     * message.
     *
     * @param msg the detail message.
     */
    public BadArgumentException(String msg) {
        super(msg);
    }
}
