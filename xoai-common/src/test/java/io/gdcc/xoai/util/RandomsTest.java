package io.gdcc.xoai.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.regex.Pattern;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class RandomsTest {
    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 10, 64})
    void createRandomAlphabetic(int length) {
        for (int i = 0; i < 10; i++) {
            String random = Randoms.randomAlphabetic(length);
            assertNotNull(random);
            assertTrue(Pattern.matches("[a-zA-Z]{" + length + "}", random));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 3, 10, 64})
    void createRandomNumeric(int length) {
        for (int i = 0; i < 10; i++) {
            String random = Randoms.randomNumeric(length);
            assertNotNull(random);
            assertTrue(Pattern.matches("\\d{" + length + "}", random));
        }
    }
}
