/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.repository;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.services.api.DateProvider;
import io.gdcc.xoai.services.api.ResumptionTokenFormat;
import io.gdcc.xoai.services.impl.SimpleResumptionTokenFormat;
import io.gdcc.xoai.xml.WriterContext;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;

public class RepositoryConfiguration implements WriterContext {
    
    private final List<String> adminEmails = new ArrayList<>();
    private final List<String> descriptions = new ArrayList<>();
    private final List<String> compressions = new ArrayList<>();
    
    private Granularity granularity;
    private ResumptionTokenFormat resumptionTokenFormat;
    private String repositoryName;
    private String baseUrl;
    private Instant earliestDate;
    private Integer maxListIdentifiers;
    private Integer maxListSets;
    private Integer maxListRecords;
    private DeletedRecord deleteMethod;
    
    private RepositoryConfiguration() {}

    public String getRepositoryName() {
        if (repositoryName == null)
            throw new InternalOAIException("Repository name has not been configured");
        return repositoryName;
    }

    public List<String> getAdminEmails() {
        return Collections.unmodifiableList(adminEmails);
    }

    public String getBaseUrl() {
        if (baseUrl == null)
            throw new InternalOAIException("Repository base URL has not been configured");
        return baseUrl;
    }

    public Instant getEarliestDate() {
        if (earliestDate == null)
            throw new InternalOAIException("Earliest date has not been configured");
        return earliestDate;
    }

    public int getMaxListIdentifiers() {
        if (maxListIdentifiers == null)
            throw new InternalOAIException("Maximum number of identifiers has not been configured");
        return maxListIdentifiers;
    }

    public int getMaxListSets() {
        if (maxListSets == null)
            throw new InternalOAIException("Maximum number of sets has not been configured");
        return maxListSets;
    }

    public int getMaxListRecords() {
        if (maxListRecords == null)
            throw new InternalOAIException("Maximum number of records has not been configured");
        return maxListRecords;
    }

    @Override
    public Granularity getGranularity() {
        if (granularity == null)
            throw new InternalOAIException("Granularity has not been configured");
        return granularity;
    }

    public DeletedRecord getDeleteMethod() {
        if (deleteMethod == null)
            throw new InternalOAIException("Delete method has not been configured");
        return deleteMethod;
    }

    public List<String> getDescription() {
        return Collections.unmodifiableList(descriptions);
    }

    public List<String> getCompressions () {
        return Collections.unmodifiableList(compressions);
    }
    
    public boolean hasCompressions() {
        return !compressions.isEmpty();
    }
    
    @Override
    public ResumptionTokenFormat getResumptionTokenFormat() {
        if (resumptionTokenFormat == null)
            throw new InternalOAIException("Resumption token format has not been configured");
        return this.resumptionTokenFormat;
    }
    
    public RepositoryConfiguration and() {
        return this;
    }
    
    public RepositoryConfiguration withGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    public RepositoryConfiguration withRepositoryName(String repositoryName) {
        this.repositoryName = repositoryName;
        return this;
    }

    public RepositoryConfiguration withAdminEmails(String... emails) {
        this.adminEmails.addAll(asList(emails));
        return this;
    }

    public RepositoryConfiguration withAdminEmail(String email) {
        this.adminEmails.add(email);
        return this;
    }
    public RepositoryConfiguration withDeleteMethod(DeletedRecord deleteMethod) {
        this.deleteMethod = deleteMethod;
        return this;
    }

    public RepositoryConfiguration withDescription(String description) {
        descriptions.add(description);
        return this;
    }

    public RepositoryConfiguration withBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
        return this;
    }

    public RepositoryConfiguration withEarliestDate(Instant earliestDate) {
        this.earliestDate = earliestDate;
        return this;
    }

    public RepositoryConfiguration withCompression(String compression) {
        compressions.add(compression);
        return this;
    }

    public RepositoryConfiguration withMaxListRecords(int maxListRecords) {
        this.maxListRecords = maxListRecords;
        return this;
    }
    
    public RepositoryConfiguration withMaxListIdentifiers(int maxListIdentifiers) {
        this.maxListIdentifiers = maxListIdentifiers;
        return this;
    }
    
    public RepositoryConfiguration withMaxListSets(int maxListSets) {
        this.maxListSets = maxListSets;
        return this;
    }
    
    public RepositoryConfiguration withResumptionTokenFormat(ResumptionTokenFormat format) {
        this.resumptionTokenFormat = format;
        return this;
    }
    
    public static RepositoryConfiguration defaults () {
        return new RepositoryConfiguration()
            .withGranularity(Granularity.Second)
            .withRepositoryName("Repository")
            .withEarliestDate(DateProvider.now())
            .withAdminEmail("sample@test.com")
            .withBaseUrl("http://localhost")
            .withMaxListRecords(100)
            .withMaxListIdentifiers(100)
            .withMaxListSets(100)
            .withDeleteMethod(DeletedRecord.NO)
            .withResumptionTokenFormat(new SimpleResumptionTokenFormat().withGranularity(Granularity.Second));
    }
}
