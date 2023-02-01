package io.gdcc.xoai.testapp;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Map;
import org.hamcrest.Matcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.xmlunit.matchers.EvaluateXPathMatcher;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
class OaiControllerTest {

    private static final String OAI_NAMESPACE = "http://www.openarchives.org/OAI/2.0/";
    private static final String BASE_URL = "http://localhost:" + OaiController.HOST_PORT + "/oai";

    @Autowired private TestRestTemplate restTemplate;

    @Test
    void errorResponseForEmptyRequest() throws Exception {
        String result = this.restTemplate.getForObject(BASE_URL, String.class);
        // System.out.println(result);
        assertThat(result, xPath("//oai:error/@code", equalTo("badVerb")));
    }

    @Test
    void identifyResponseForIdentifyResponse() throws Exception {
        String result = this.restTemplate.getForObject(BASE_URL + "?verb=Identify", String.class);
        // System.out.println(result);
        assertThat(result, xPath("//oai:repositoryName", equalTo(OaiController.repoName)));
    }

    protected static Matcher<? super String> xPath(String xPath, Matcher<String> valueMatcher) {
        return EvaluateXPathMatcher.hasXPath(xPath, valueMatcher)
                .withNamespaceContext(Map.of("oai", OAI_NAMESPACE));
    }
}
