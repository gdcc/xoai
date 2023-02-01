package io.gdcc.xoai.testapp;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SmokeTest {

    @Autowired private OaiController oai;

    @Test
    void contextLoads() {
        assertNotNull(oai);
    }
}
