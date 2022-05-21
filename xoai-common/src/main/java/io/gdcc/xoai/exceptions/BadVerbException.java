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
public class BadVerbException extends OAIException {

    private static final long serialVersionUID = 2748244610538429452L;
    
    @Override
    public Error.Code getErrorCode() {
        return Error.Code.BAD_VERB;
    }
    
    /**
     * Creates a new instance of <code>BadVerbException</code> without
     * detail message.
     */
    public BadVerbException() {
    }

    /**
     * Constructs an instance of <code>BadVerbException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public BadVerbException(String msg) {
        super(msg);
    }
}
