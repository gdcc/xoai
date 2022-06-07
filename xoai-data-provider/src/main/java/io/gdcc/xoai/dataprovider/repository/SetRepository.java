/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package io.gdcc.xoai.dataprovider.repository;

import io.gdcc.xoai.dataprovider.model.Set;

import java.util.List;

/**
 * API for implementing a repository of sets.
 * It is possible to have a data provider without sets.
 *
 * @author Development @ Lyncode
 * @version 3.1.0
 */
public interface SetRepository {

    /**
     * Checks if the actual data source supports sets.
     *
     * @return Supports sets?
     */
    default boolean supportSets() {
        return false;
    };

    /**
     * Returns a list of all sets that exist within the repository context. This will usually be not very large and
     * is not very memory intense. The repository may cache the result to avoid unnecessary database calls.
     *
     * The result will be enriched with the {@link io.gdcc.xoai.dataprovider.model.Context} static sets and returned
     * as a paged result to the client.
     *
     * @return List of Sets
     */
    List<Set> getSets();

    /**
     * Checks if a specific sets exists in the data source.
     *
     * @param setSpec Set spec
     * @return Set exists
     * @see <a href="client://www.openarchives.org/OAI/openarchivesprotocol.html#Set">Set definition</a>
     */
    default boolean exists(String setSpec) {
        return false;
    };
}
