package io.gdcc.xoai.dataprovider;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;

import io.gdcc.xoai.testapp.OaiController;
import io.gdcc.xoai.testapp.TestApp;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(
        classes = TestApp.class,
        webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Testcontainers
class DataProviderTCK {

    GenericContainer<?> validator =
            new GenericContainer<>(
                            new ImageFromDockerfile("gdcc/oaipmh-validator", false)
                                    .withDockerfileFromBuilder(
                                            builder ->
                                                    builder.from("perl:5")
                                                            // 1) required force install because of
                                                            // some missing ssl whatever, ignore for
                                                            // http-only testing
                                                            // 2) also adding missing testing
                                                            // dependency or install fails
                                                            .run(
                                                                    "cpanm -f Crypt::SSLeay"
                                                                            + " Test::Exception")
                                                            .run("cpanm HTTP::OAIPMH::Validator")
                                                            .build()))
                    .withStartupCheckStrategy(
                            new OneShotStartupCheckStrategy().withTimeout(Duration.ofSeconds(3)))
                    .withClasspathResourceMapping(
                            "validator.pl", "/validator.pl", BindMode.READ_ONLY);

    @Test
    void validate() {
        // Expose the Spring Boot App Port into the container
        org.testcontainers.Testcontainers.exposeHostPorts(OaiController.HOST_PORT);

        // See
        // https://www.testcontainers.org/features/networking/#exposing-host-ports-to-the-container
        // for details on the URL
        validator.withCommand("perl /validator.pl " + OaiController.BASE_URL).start();

        String logs = validator.getLogs();
        System.out.println(logs);

        assertThat(
                logs,
                containsString(
                        "Validation status of data provider "
                                + OaiController.BASE_URL
                                + " is COMPLIANT"));
    }
}
