/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.model;

import io.gdcc.xoai.dataprovider.filter.Condition;
import io.gdcc.xoai.dataprovider.filter.Scope;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.model.xoai.XOAIMetadata;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * @author Development @ Lyncode
 * @version 3.1.0
 */
public class Set {
    public static Set set(String spec) {
        return new Set(spec);
    }

    private final String spec;
    private final List<XOAIMetadata> descriptions = new ArrayList<>();
    private String name;
    private Condition condition;

    public Set(String spec) {
        this.spec = spec;
    }

    public String getName() {
        return name;
    }

    public Set withName(String name) {
        this.name = name;
        return this;
    }

    public List<XOAIMetadata> getDescriptions() {
        return descriptions;
    }

    public Set withDescription(XOAIMetadata description) {
        descriptions.add(description);
        return this;
    }

    public boolean hasDescription() {
        return (!this.descriptions.isEmpty());
    }

    public boolean hasCondition() {
        return condition != null;
    }

    public Set withCondition(Condition condition) {
        this.condition = condition;
        return this;
    }
    
    public boolean isItemShown(ItemIdentifier item) {
        // null item means false (not shown), otherwise true (no condition), when condition present check filter
        return item != null && condition == null || condition.isItemShown(item);
    }
    
    /**
     * Create a scoped {@link io.gdcc.xoai.dataprovider.filter.Filter} to hide items not matching the {@link Condition}.
     *
     * @return The scoped filter used with {@link io.gdcc.xoai.dataprovider.repository.ItemRepository#getItems(List, MetadataFormat, int, ResumptionToken.Value)}
     *         or {@link io.gdcc.xoai.dataprovider.repository.ItemRepository#getItemIdentifiers(List, MetadataFormat, int, ResumptionToken.Value)}.
     *         Will default to a transparent filter by using {@link Condition#ALWAYS_TRUE}.
     */
    public ScopedFilter getScopedFilter() {
        // if no condition is present, make the filter transparent by using always true
        return new ScopedFilter(this.condition == null ? Condition.ALWAYS_TRUE : this.condition, Scope.Set);
    }

    public String getSpec() {
        return spec;
    }

    public io.gdcc.xoai.model.oaipmh.results.Set toOAIPMH () {
        var set = new io.gdcc.xoai.model.oaipmh.results.Set()
            .withName(getName())
            .withSpec(getSpec());
        for (XOAIMetadata description : descriptions)
            set.withDescription(description);
        return set;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Set)) return false;
        Set set = (Set) o;
        return Objects.equals(getSpec(), set.getSpec());
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getName());
    }
}
