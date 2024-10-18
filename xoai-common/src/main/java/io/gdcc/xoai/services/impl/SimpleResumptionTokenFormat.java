/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.services.impl;

import io.gdcc.xoai.exceptions.BadResumptionTokenException;
import io.gdcc.xoai.model.oaipmh.Granularity;
import io.gdcc.xoai.model.oaipmh.ResumptionToken;
import io.gdcc.xoai.services.api.DateProvider;
import io.gdcc.xoai.services.api.ResumptionTokenFormat;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.util.Base64;
import java.util.regex.Pattern;

public class SimpleResumptionTokenFormat implements ResumptionTokenFormat {

    private static final String partSeparator = "|";
    private static final String valueSeparator = "::";
    private static final String offset = "offset";
    private static final String set = "set";
    private static final String from = "from";
    private static final String until = "until";
    private static final String metadataPrefix = "prefix";

    private Granularity granularity = Granularity.Second;

    @Override
    public ResumptionToken.Value parse(String resumptionToken) throws BadResumptionTokenException {

        ResumptionToken.ValueBuilder tokenBuilder = new ResumptionToken.ValueBuilder();
        String decodedToken = base64Decode(resumptionToken);

        if (decodedToken == null || decodedToken.isBlank()) {
            return tokenBuilder.build();
        }

        for (String part : decodedToken.split(Pattern.quote(partSeparator))) {
            String[] keyValue = part.split(valueSeparator);
            if (keyValue.length != 2 || keyValue[1].isEmpty()) {
                throw new BadResumptionTokenException("Invalid token part '" + part + "'");
            }

            try {
                switch (keyValue[0]) {
                    case offset:
                        tokenBuilder.withOffset(Integer.parseInt(keyValue[1]));
                        break;
                    case set:
                        tokenBuilder.withSetSpec(keyValue[1]);
                        break;
                    case from:
                        tokenBuilder.withFrom(DateProvider.parse(keyValue[1], this.granularity));
                        break;
                    case until:
                        tokenBuilder.withUntil(DateProvider.parse(keyValue[1], this.granularity));
                        break;
                    case metadataPrefix:
                        tokenBuilder.withMetadataPrefix(keyValue[1]);
                        break;
                    default:
                        throw new BadResumptionTokenException(
                                "Unknown key '" + keyValue[0] + "' found");
                }
            } catch (DateTimeException e) {
                throw new BadResumptionTokenException(e);
            }
        }

        return tokenBuilder.build();
    }

    @Override
    public ResumptionTokenFormat withGranularity(Granularity granularity) {
        this.granularity = granularity;
        return this;
    }

    @Override
    public String format(ResumptionToken.Value resumptionToken) {
        String token = "";

        token +=
                resumptionToken.hasOffset()
                        ? offset + valueSeparator + resumptionToken.getOffset()
                        : "";
        token +=
                resumptionToken.hasSetSpec()
                        ? partSeparator + set + valueSeparator + resumptionToken.getSetSpec()
                        : "";
        token +=
                resumptionToken.hasFrom()
                        ? partSeparator
                                + from
                                + valueSeparator
                                + DateProvider.format(resumptionToken.getFrom(), this.granularity)
                        : "";
        token +=
                resumptionToken.hasUntil()
                        ? partSeparator
                                + until
                                + valueSeparator
                                + DateProvider.format(resumptionToken.getUntil(), this.granularity)
                        : "";
        token +=
                resumptionToken.hasMetadataPrefix()
                        ? partSeparator
                                + metadataPrefix
                                + valueSeparator
                                + resumptionToken.getMetadataPrefix()
                        : "";

        return base64Encode(token);
    }

    /**
     * Simple decoding of a Base64 encoded String assuming UTF-8 usage
     *
     * @param value The Base64 encoded string
     * @return A decoded String (may be empty)
     */
    static String base64Decode(String value) throws BadResumptionTokenException {
        if (value == null) {
            return null;
        }
        try {
            byte[] decodedValue = Base64.getDecoder().decode(value);
            return new String(decodedValue, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException e) {
            throw new BadResumptionTokenException("Token has no valid base64 encoding", e);
        }
    }

    static String base64Encode(String value) {
        if (value == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
