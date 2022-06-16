/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.exceptions.handler;

import io.gdcc.xoai.exceptions.OAIException;

public abstract class HandlerException extends OAIException {
    private static final long serialVersionUID = 3141316350056438361L;

    protected HandlerException() {
        super();
    }

    protected HandlerException(String message, Throwable cause) {
        super(message, cause);
    }

    protected HandlerException(String message) {
        super(message);
    }

    protected HandlerException(Throwable cause) {
        super(cause);
    }
}
