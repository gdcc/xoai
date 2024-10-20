/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.exceptions.handler;

import io.gdcc.xoai.model.oaipmh.Error;

/**
 * @author Development @ Lyncode
 * @version 3.1.0
 */
public class DoesNotSupportSetsException extends HandlerException {

    private static final long serialVersionUID = -7008970964208110045L;

    @Override
    public Error.Code getErrorCode() {
        return Error.Code.NO_SET_HIERARCHY;
    }

    /**
     * Creates a new instance of <code>DoesNotSupportSetsException</code> without detail message.
     */
    public DoesNotSupportSetsException() {}

    /**
     * Constructs an instance of <code>DoesNotSupportSetsException</code> with the specified detail
     * message.
     *
     * @param msg the detail message.
     */
    public DoesNotSupportSetsException(String msg) {
        super(msg);
    }
}
