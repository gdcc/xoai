/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.model;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.dataprovider.filter.Condition;
import io.gdcc.xoai.dataprovider.filter.Scope;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;

import javax.xml.transform.Transformer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Context {
    public static Context context () {
        return new Context();
    }

    private Transformer metadataTransformer;
    private final List<MetadataFormat> metadataFormats = new ArrayList<>();
    private final List<Set> sets = new ArrayList<>();
    private Condition condition;

    public List<Set> getSets() {
        return sets;
    }

    public Context withSet(Set set) {
        if (!set.hasCondition())
            throw new InternalOAIException("Context sets must have a condition");
        this.sets.add(set);
        return this;
    }
    
    public boolean hasSet(String setSpec) {
        return isStaticSet(setSpec);
    }
    public boolean isStaticSet(String setSpec) {
        return this.sets.stream().anyMatch(set -> set.getSpec().equals(setSpec));
    }
    
    public Optional<Set> getSet(String setSpec) {
        return this.sets.stream()
            .filter(set -> set.getSpec().equals(setSpec))
            .findAny();
    }
    
    public Stream<Set> getSetsForItem(ItemIdentifier item) {
        return this.sets.stream()
            .filter(set -> set.isItemShown(item));
    }

    public Transformer getTransformer() {
        return metadataTransformer;
    }

    public Context withTransformer(Transformer metadataTransformer) {
        this.metadataTransformer = metadataTransformer;
        return this;
    }

    public List<MetadataFormat> getMetadataFormats() {
        return List.copyOf(metadataFormats);
    }

    public Context withMetadataFormat(MetadataFormat metadataFormat) {
        int remove = -1;
        for (int i = 0;i<metadataFormats.size();i++)
            if (metadataFormats.get(i).getPrefix().equals(metadataFormat.getPrefix()))
                remove = i;
        if (remove >= 0)
            this.metadataFormats.remove(remove);
        this.metadataFormats.add(metadataFormat);
        return this;
    }

    public Context withCondition(Condition condition) {
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
        return new ScopedFilter(condition != null ? condition : Condition.ALWAYS_TRUE, Scope.Context);
    }

    public MetadataFormat formatForPrefix(String metadataPrefix) {
        for (MetadataFormat format : this.metadataFormats)
            if (format.getPrefix().equals(metadataPrefix))
                return format;

        return null;
    }

    public boolean hasTransformer() {
        return metadataTransformer != null;
    }

    public Context withMetadataFormat(String prefix, Transformer transformer) {
        withMetadataFormat(new MetadataFormat().withNamespace(prefix).withPrefix(prefix).withSchemaLocation(prefix).withTransformer(transformer));
        return this;
    }

    public Context withMetadataFormat(String prefix, Transformer transformer, Condition condition) {
        withMetadataFormat(
                new MetadataFormat()
                        .withNamespace(prefix)
                        .withPrefix(prefix)
                        .withSchemaLocation(prefix)
                        .withTransformer(transformer)
                        .withCondition(condition)
        );
        return this;
    }

    public Context withoutMetadataFormats() {
        metadataFormats.clear();
        return this;
    }

    public List<MetadataFormat> formatFor(ItemIdentifier item) {
        List<MetadataFormat> result = new ArrayList<>();
        for (MetadataFormat format : this.metadataFormats)
            if (format.isItemShown(item))
                result.add(format);
        return result;
    }
}
