/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.repository;

public final class Repository {
    public static Repository repository () {
        return new Repository();
    }

    private RepositoryConfiguration configuration = RepositoryConfiguration.defaults();
    private ItemRepository itemRepository = null;
    private SetRepository setRepository = null;

    public RepositoryConfiguration getConfiguration() {
        return configuration;
    }

    public Repository withConfiguration(RepositoryConfiguration configuration) {
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
