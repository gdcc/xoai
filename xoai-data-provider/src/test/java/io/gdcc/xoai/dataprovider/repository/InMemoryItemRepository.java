/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.repository;

import static io.gdcc.xoai.dataprovider.model.InMemoryItem.randomItem;
import static java.util.Arrays.asList;

import io.gdcc.xoai.dataprovider.exceptions.handler.HandlerException;
import io.gdcc.xoai.dataprovider.exceptions.handler.IdDoesNotExistException;
import io.gdcc.xoai.dataprovider.filter.ScopedFilter;
import io.gdcc.xoai.dataprovider.model.InMemoryItem;
import io.gdcc.xoai.dataprovider.model.Item;
import io.gdcc.xoai.dataprovider.model.ItemIdentifier;
import io.gdcc.xoai.dataprovider.model.MetadataFormat;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryItemRepository implements ItemRepository {
    private final List<InMemoryItem> list = new ArrayList<>();

    public InMemoryItemRepository withNoItems() {
        return this;
    }

    public InMemoryItemRepository withItem(InMemoryItem item) {
        list.add(item);
        return this;
    }

    public InMemoryItemRepository withItems(InMemoryItem... item) {
        list.addAll(asList(item));
        return this;
    }

    public InMemoryItemRepository withRandomItems(int number) {
        for (int i = 0; i < number; i++) list.add(randomItem());
        return this;
    }

    @Override
    public Item getItem(String identifier) throws IdDoesNotExistException {
        for (InMemoryItem item : this.list) {
            if (item.getIdentifier().equals(identifier)) return item;
        }
        throw new IdDoesNotExistException();
    }

    @Override
    public Item getItem(String identifier, MetadataFormat format) throws HandlerException {
        return getItem(identifier);
    }

    @Override
    public ResultsPage<ItemIdentifier> getItemIdentifiers(
            List<ScopedFilter> filters,
            MetadataFormat metadataFormat,
            int maxResponseLength,
            ResumptionToken.Value resumptionToken)
            throws HandlerException {
        List<ItemIdentifier> pagedResults =
                this.list.stream()
                        .skip(resumptionToken.getOffset())
                        .limit(maxResponseLength)
                        .collect(Collectors.toUnmodifiableList());

        return new ResultsPage<>(
                resumptionToken,
                // more only when page size = maxlength - but only when this is not also the end of
                // the list
                // (edge case where maxlength is a multiple of total size)
                pagedResults.size() == maxResponseLength
                        && this.list.size() != resumptionToken.getOffset() + maxResponseLength,
                pagedResults,
                list.size());
    }

    @Override
    public ResultsPage<Item> getItems(
            List<ScopedFilter> filters,
            MetadataFormat metadataFormat,
            int maxResponseLength,
            ResumptionToken.Value resumptionToken)
            throws HandlerException {
        List<Item> pagedResults =
                this.list.stream()
                        .skip(resumptionToken.getOffset())
                        .limit(maxResponseLength)
                        .collect(Collectors.toUnmodifiableList());

        return new ResultsPage<>(
                resumptionToken,
                // more only when page size = maxlength - but only when this is not also the end of
                // the list
                // (edge case where maxlength is a multiple of total size)
                pagedResults.size() == maxResponseLength
                        && this.list.size() != resumptionToken.getOffset() + maxResponseLength,
                pagedResults,
                list.size());
    }
}
