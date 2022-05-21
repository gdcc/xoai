/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.model.Context;
import io.gdcc.xoai.dataprovider.repository.Repository;
import io.gdcc.xoai.dataprovider.repository.RepositoryConfiguration;
import io.gdcc.xoai.model.oaipmh.Request;
import io.gdcc.xoai.model.oaipmh.verbs.Verb;

public abstract class VerbHandler<T extends Verb> {
    private final Context context;
    private final Repository repository;

    protected VerbHandler (Context context, Repository repository) {
        this.context = context;
        this.repository = repository;
    }

    protected Context getContext() {
        return context;
    }
    protected Repository getRepository() {
        return repository;
    }
    protected RepositoryConfiguration getConfiguration() {
        return repository.getConfiguration();
    }

    public abstract T handle(Request request) throws HandlerException;
}
