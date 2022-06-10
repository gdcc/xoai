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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
    
    private boolean enableMetadataAttributes = false;
    
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
    
    /**
     * Skew an instant to end of day (granularity Day or Lenient) or by a second (granularity Second).
     * This is necessary for two reasons:
     * 1. Day granularity must be inclusive, so we can't leave an "until" at start of day.
     * 2. Lenient granularity must work like day in this case, as we cannot be sure which is meant.
     * 3. Second granularity needs skipping a second to avoid not returning {@link io.gdcc.xoai.dataprovider.model.Item}
     *    from the repository that use an SQL timestamp with nanosecond granularity (so ...:00.5829 would not be
     *    found when asking for all until ...:00.000)
     *
     * @param timestamp The timestamp to skew a little
     * @return The skewed timestamp
     */
    public Instant skewUntil(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Skewing an 'until' date must not be used with null");
        switch (getGranularity()) {
            case Day:
            case Lenient: return LocalDate.ofInstant(timestamp, ZoneId.of("UTC")).atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
            case Second: return timestamp.plusSeconds(1);
            default: return timestamp;
        }
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
    
    /**
     * This is here for Dataverse 4/5 backward compatibility.
     *
     * They added an attribute to the <code>&gt;record&lt;&lt;metadata&gt;</code> element,
     * containing the API URL of a record in their special metadata format "dataverse_json".
     *
     * @deprecated Remove when Dataverse 6 is old enough that no ones uses this workaround anymore.
     */
    @Deprecated(since = "5.0")
    public RepositoryConfiguration withEnableMetadataAttributes(boolean enable) {
        this.enableMetadataAttributes = enable;
        return this;
    }
    
    @Override
    public boolean isMetadataAttributesEnabled() {
        return this.enableMetadataAttributes;
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
            .withResumptionTokenFormat(new SimpleResumptionTokenFormat().withGranularity(Granularity.Second))
            .withEnableMetadataAttributes(false);
    }
}
