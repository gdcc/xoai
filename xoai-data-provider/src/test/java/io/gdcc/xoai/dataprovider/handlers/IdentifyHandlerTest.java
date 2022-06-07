/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package io.gdcc.xoai.dataprovider.handlers;

import io.gdcc.xoai.dataprovider.exceptions.InternalOAIException;
import io.gdcc.xoai.model.oaipmh.verbs.Identify;
import org.junit.jupiter.api.Test;

import static io.gdcc.xoai.model.oaipmh.verbs.Verb.Type.Identify;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class IdentifyHandlerTest extends AbstractHandlerTest {
    @Test
    public void validResponse() throws Exception {
        Identify handle = new IdentifyHandler(aContext(), theRepository()).handle(request().withVerb(Identify));
        String result = write(handle);

        assertThat(result, xPath("//repositoryName", is(equalTo(theRepositoryConfiguration().getRepositoryName()))));
    }

    @Test
    public void internalExceptionForInvalidConfiguration () {
        theRepositoryConfiguration().withMaxListSets(0);
        assertThrows(InternalOAIException.class, () -> new IdentifyHandler(aContext(), theRepository()));
    }
}
