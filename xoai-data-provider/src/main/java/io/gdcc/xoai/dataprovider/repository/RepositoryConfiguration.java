/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.repository;

import static java.util.Arrays.asList;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.model.oaipmh.DeletedRecord;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.services.api.ResumptionTokenFormat;
import io.gdcc.xoai.services.impl.SimpleResumptionTokenFormat;
import io.gdcc.xoai.xml.WriterContext;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * RepositoryConfiguration is a class containing all settings relevant for OAI-PMH operation. A
 * configuration can only be built using the {@link RepositoryConfigurationBuilder} and is
 * non-modifiable. In case you need to change something, you need to build a new one.
 */
public class RepositoryConfiguration implements WriterContext {

    private final List<String> adminEmails = new ArrayList<>();
    private final List<String> descriptions = new ArrayList<>();
    private final List<String> compressions = new ArrayList<>();

    private final Granularity granularity;
    private final ResumptionTokenFormat resumptionTokenFormat;
    private final String repositoryName;
    private final String baseUrl;
    private final Instant earliestDate;
    private final Integer maxListIdentifiers;
    private final Integer maxListSets;
    private final Integer maxListRecords;
    private final DeletedRecord deleteMethod;

    private final boolean enableMetadataAttributes;

    RepositoryConfiguration(
            List<String> adminEmails,
            List<String> descriptions,
            List<String> compressions,
            Granularity granularity,
            ResumptionTokenFormat resumptionTokenFormat,
            String repositoryName,
            String baseUrl,
            Instant earliestDate,
            Integer maxListIdentifiers,
            Integer maxListSets,
            Integer maxListRecords,
            DeletedRecord deleteMethod,
            boolean enableMetadataAttributes) {
        this.adminEmails.addAll(List.copyOf(adminEmails));
        this.descriptions.addAll(List.copyOf(descriptions));
        this.compressions.addAll(List.copyOf(compressions));
        this.granularity = granularity;
        this.resumptionTokenFormat = resumptionTokenFormat;
        this.repositoryName = repositoryName;
        this.baseUrl = baseUrl;
        this.earliestDate = earliestDate;
        this.maxListIdentifiers = maxListIdentifiers;
        this.maxListSets = maxListSets;
        this.maxListRecords = maxListRecords;
        this.deleteMethod = deleteMethod;
        this.enableMetadataAttributes = enableMetadataAttributes;
    }

    /**
     * Transform an existing configuration back to a builder as a template for reconfiguration.
     *
     * @return A new configuration builder
     */
    public RepositoryConfigurationBuilder asTemplate() {
        var builder =
                new RepositoryConfigurationBuilder()
                        .setAdminEmails(this.adminEmails)
                        .withGranularity(this.granularity)
                        .withResumptionTokenFormat(this.resumptionTokenFormat)
                        .withRepositoryName(this.repositoryName)
                        .withBaseUrl(this.baseUrl)
                        .withEarliestDate(this.earliestDate)
                        .withMaxListIdentifiers(this.maxListIdentifiers)
                        .withMaxListSets(this.maxListSets)
                        .withMaxListRecords(this.maxListRecords)
                        .withDeleteMethod(this.deleteMethod)
                        .withEnableMetadataAttributes(this.enableMetadataAttributes);

        // Quick and hacky addition as no methods for bulk adding available
        builder.descriptions.clear();
        builder.descriptions.addAll(this.descriptions);
        builder.compressions.clear();
        builder.compressions.addAll(this.compressions);

        return builder;
    }

    public void inject(Repository repository) {
        repository.setConfiguration(this);
    }

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
     * Skew an instant to end of day (granularity Day or Lenient) or by a second (granularity
     * Second). This is necessary for two reasons: 1. Day granularity must be inclusive, so we can't
     * leave an "until" at start of day. 2. Lenient granularity must work like day in this case, as
     * we cannot be sure which is meant. 3. Second granularity needs skipping a second to avoid not
     * returning {@link io.gdcc.xoai.dataprovider.model.Item} from the repository that use an SQL
     * timestamp with nanosecond granularity (so ...:00.5829 would not be found when asking for all
     * until ...:00.000)
     *
     * @param timestamp The timestamp to skew a little
     * @return The skewed timestamp
     */
    public Instant skewUntil(Instant timestamp) {
        Objects.requireNonNull(timestamp, "Skewing an 'until' date must not be used with null");
        switch (getGranularity()) {
            case Day:
            case Lenient:
                return LocalDate.ofInstant(timestamp, ZoneId.of("UTC"))
                        .atTime(LocalTime.MAX)
                        .toInstant(ZoneOffset.UTC);
            case Second:
                return timestamp.plusSeconds(1);
            default:
                return timestamp;
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

    public List<String> getCompressions() {
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

    @Override
    public boolean isMetadataAttributesEnabled() {
        return this.enableMetadataAttributes;
    }

    public static final class RepositoryConfigurationBuilder {

        /* All field below package private to access in tests */
        final List<String> adminEmails = new ArrayList<>();
        final List<String> descriptions = new ArrayList<>();
        final List<String> compressions = new ArrayList<>();

        Granularity granularity = Granularity.Second;
        ResumptionTokenFormat resumptionTokenFormat =
                new SimpleResumptionTokenFormat().withGranularity(Granularity.Second);
        String repositoryName;
        String baseUrl;
        Instant earliestDate;
        DeletedRecord deleteMethod;
        Integer maxListIdentifiers = 100;
        Integer maxListSets = 100;
        Integer maxListRecords = 100;
        Boolean enableMetadataAttributes = false;

        public RepositoryConfigurationBuilder withGranularity(Granularity granularity) {
            this.requireNotNull(granularity, "Granularity must not be null");

            this.granularity = granularity;
            return this;
        }

        public RepositoryConfigurationBuilder withRepositoryName(String repositoryName) {
            this.requireNotNullNotEmpty(
                    repositoryName, "Repository name must not be null or empty");

            this.repositoryName = repositoryName;
            return this;
        }

        public RepositoryConfigurationBuilder setAdminEmails(List<String> emails) {
            this.requireNotNull(emails, "Admin emails list must not be null");
            if (emails.isEmpty()) {
                throw new IllegalArgumentException("Admin emails list must not be empty");
            }
            return this.setAdminEmails(emails.toArray(String[]::new));
        }

        public RepositoryConfigurationBuilder setAdminEmails(String... emails) {
            // Backup first, then clear and add. In case the verification fails, restore backup
            var backup = List.copyOf(this.adminEmails);
            this.adminEmails.clear();

            try {
                return this.withAdminEmails(emails);
            } catch (IllegalArgumentException e) {
                this.adminEmails.addAll(backup);
                throw e;
            }
        }

        public RepositoryConfigurationBuilder withAdminEmails(String... emails) {
            this.requireNotNull(emails, "Admin email list must not be null");
            if (emails.length == 0) {
                throw new IllegalArgumentException("Admin emails list must not be empty");
            }
            for (String s : emails) {
                this.requireNotNullNotEmpty(
                        s,
                        "Admin email must not be null or empty in list ('"
                                + String.join("', '", emails)
                                + "')");
                // TODO: one might add mail verification here
            }

            this.adminEmails.addAll(asList(emails));
            return this;
        }

        public RepositoryConfigurationBuilder withAdminEmail(String email) {
            this.requireNotNullNotEmpty(email, "Admin email must not be null or empty");

            this.adminEmails.add(email);
            return this;
        }

        public RepositoryConfigurationBuilder withDeleteMethod(DeletedRecord deleteMethod) {
            this.requireNotNull(deleteMethod, "Deletion Method must not be null");

            this.deleteMethod = deleteMethod;
            return this;
        }

        public RepositoryConfigurationBuilder withDescription(String description) {
            this.requireNotNullNotEmpty(description, "Description must not be null or empty");

            descriptions.add(description);
            return this;
        }

        public RepositoryConfigurationBuilder withBaseUrl(String baseUrl) {
            this.requireNotNullNotEmpty(baseUrl, "Base URL must not be null or empty");

            this.baseUrl = baseUrl;
            return this;
        }

        public RepositoryConfigurationBuilder withEarliestDate(Instant earliestDate) {
            this.requireNotNull(earliestDate, "Earliest date must not be null");
            if (earliestDate.isAfter(Instant.now())) {
                throw new IllegalArgumentException(
                        "Earliest date cannot lie in the future (given: "
                                + earliestDate.truncatedTo(ChronoUnit.SECONDS).toString()
                                + ")");
            }

            this.earliestDate = earliestDate;
            return this;
        }

        public RepositoryConfigurationBuilder withCompression(String compression) {
            this.requireNotNullNotEmpty(compression, "Compression must not be null or empty");
            compressions.add(compression);
            return this;
        }

        public RepositoryConfigurationBuilder withMaxListRecords(int maxListRecords) {
            if (maxListRecords < 1) {
                throw new IllegalArgumentException(
                        "Maximum ListRecords response size must be greater 0");
            }

            this.maxListRecords = maxListRecords;
            return this;
        }

        public RepositoryConfigurationBuilder withMaxListIdentifiers(int maxListIdentifiers) {
            if (maxListIdentifiers < 1) {
                throw new IllegalArgumentException(
                        "Maximum ListIdentifiers response size must be greater 0");
            }

            this.maxListIdentifiers = maxListIdentifiers;
            return this;
        }

        public RepositoryConfigurationBuilder withMaxListSets(int maxListSets) {
            if (maxListSets < 1) {
                throw new IllegalArgumentException(
                        "Maximum ListSets response size must be greater 0");
            }

            this.maxListSets = maxListSets;
            return this;
        }

        public RepositoryConfigurationBuilder withResumptionTokenFormat(
                ResumptionTokenFormat format) {
            this.requireNotNull(format, "Resumption Token Format must not be null");

            this.resumptionTokenFormat = format;
            return this;
        }

        /**
         * This is here for Dataverse 4/5 backward compatibility.
         *
         * <p>They added an attribute to the <code>&gt;record&lt;&lt;metadata&gt;</code> element,
         * containing the API URL of a record in their special metadata format "dataverse_json".
         *
         * @deprecated Remove when Dataverse 6 is old enough that no ones uses this workaround
         *     anymore.
         */
        @Deprecated(since = "5.0")
        public RepositoryConfigurationBuilder withEnableMetadataAttributes(boolean enable) {
            this.enableMetadataAttributes = enable;
            return this;
        }

        public RepositoryConfiguration build() {
            // Basic validation of configuration that is still missing
            // 1. At least 1 admin mail present?
            if (adminEmails.isEmpty()) {
                throw new IllegalArgumentException("Missing admin email address/es");
            }
            // 2. Parameters without a default should have been set
            this.requireNotNullNotEmpty(baseUrl, "Missing base URL");
            this.requireNotNullNotEmpty(repositoryName, "Missing repository name");
            this.requireNotNull(
                    earliestDate,
                    "Missing 'earliest date', which is the date of the first inserted item");

            this.requireNotNull(deleteMethod, "Missing delete method");

            return new RepositoryConfiguration(
                    adminEmails,
                    descriptions,
                    compressions,
                    granularity,
                    resumptionTokenFormat,
                    repositoryName,
                    baseUrl,
                    earliestDate,
                    maxListIdentifiers,
                    maxListSets,
                    maxListRecords,
                    deleteMethod,
                    enableMetadataAttributes);
        }

        /**
         * Check for null and throw an IllegalArgumentException with a message when found.
         *
         * @param o Subject to test
         * @param message The message if subject is null
         * @throws IllegalArgumentException If subject is null
         */
        public void requireNotNull(Object o, String message) {
            if (o == null) {
                throw new IllegalArgumentException(message);
            }
        }

        /**
         * Check for null or an empty String and throw an IllegalArgumentException with a message
         * when found.
         *
         * @param s Subject to test
         * @param message The message if subject is null
         * @throws IllegalArgumentException If subject is null or empty String
         */
        public void requireNotNullNotEmpty(String s, String message) {
            if (s == null || s.isEmpty()) {
                throw new IllegalArgumentException(message);
            }
        }
    }
}
