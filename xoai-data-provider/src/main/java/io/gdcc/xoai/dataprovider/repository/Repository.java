/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.repository;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;

public final class Repository {

    /* Do not let a repository get constructed without a configuration. Remember: this is crucial! */
    private Repository() {}

    public Repository(RepositoryConfiguration configuration) {
        this.configuration = configuration;
    }

    private RepositoryConfiguration configuration;
    private ItemRepository itemRepository = null;
    private SetRepository setRepository = null;

    public RepositoryConfiguration getConfiguration() {
        if (this.configuration == null) {
            throw new InternalOAIException(
                    "Despite a private constructor this repository instance has no configuration");
        }
        return configuration;
    }

    public Repository setConfiguration(RepositoryConfiguration configuration) {
        this.configuration = configuration;
        return this;
    }

    public ItemRepository getItemRepository() {
        return itemRepository;
    }

    public Repository withItemRepository(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        return this;
    }

    public SetRepository getSetRepository() {
        return setRepository;
    }

    public Repository withSetRepository(SetRepository setRepository) {
        this.setRepository = setRepository;
        return this;
    }
}
