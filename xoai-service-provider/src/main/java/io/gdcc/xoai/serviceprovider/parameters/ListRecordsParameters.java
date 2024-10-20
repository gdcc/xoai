/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.serviceprovider.parameters;

import java.time.Instant;

public class ListRecordsParameters {
    public static ListRecordsParameters request() {
        return new ListRecordsParameters();
    }

    private String metadataPrefix;
    private String setSpec;
    private Instant from;
    private Instant until;
    private String granularity;

    public String getMetadataPrefix() {
        return metadataPrefix;
    }

    public ListRecordsParameters withMetadataPrefix(String metadataPrefix) {
        this.metadataPrefix = metadataPrefix;
        return this;
    }

    public String getSetSpec() {
        return setSpec;
    }

    public ListRecordsParameters withSetSpec(String setSpec) {
        this.setSpec = setSpec;
        return this;
    }

    public Instant getFrom() {
        return from;
    }

    public ListRecordsParameters withFrom(Instant from) {
        this.from = from;
        return this;
    }

    public Instant getUntil() {
        return until;
    }

    public ListRecordsParameters withUntil(Instant until) {
        this.until = until;
        return this;
    }

    public boolean areValid() {
        return metadataPrefix != null;
    }

    public void withGranularity(String granularity) {
        this.granularity = granularity;
    }

    public String getGranularity() {
        return granularity;
    }
}
