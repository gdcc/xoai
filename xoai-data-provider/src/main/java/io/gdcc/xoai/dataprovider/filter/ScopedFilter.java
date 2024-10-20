/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.filter;

/**
 * @author Development @ Lyncode
 * @version 3.1.0
 */
public final class ScopedFilter {
    private final Condition condition;
    private final Scope scope;

    public ScopedFilter(Condition condition, Scope scope) {
        this.condition = condition;
        this.scope = scope;
    }

    public Condition getCondition() {
        return condition;
    }

    public Scope getScope() {
        return scope;
    }
}
