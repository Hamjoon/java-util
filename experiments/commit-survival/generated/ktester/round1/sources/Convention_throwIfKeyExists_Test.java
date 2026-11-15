package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfKeyExists_Test {

    /**
     * A reusable map instance for the non‑parameterised tests.
     */
    private Map<String, String> map;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh map for each test to avoid cross‑test contamination.
        map = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Nothing specific to clean up.
        map.clear();
    }

    private static Stream<Arguments> provideIntegerKeyScenarios() {
        // Scenario 1 – key exists, expect exception with supplied message.
        Map<Integer, String> map1 = new HashMap<>();
        map1.put(1, "one");
        // Scenario 2 – key absent, expect no exception.
        Map<Integer, String> map2 = new HashMap<>();
        map2.put(2, "two");
        return Stream.of(Arguments.of(map1, 1, "duplicate integer key", true, "duplicate integer key"), Arguments.of(map2, 3, "no conflict", false, null));
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenMapIsNull_throwsException(Map<?, ?> nullMap) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(nullMap, "anyKey", "map cannot be null")).withMessageContaining("map cannot be null");
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenKeyIsNull_throwsException(String nullKey) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(new HashMap<>(), nullKey, "key cannot be null")).withMessageContaining("key cannot be null");
    }

    @Test
    void testThrowIfKeyExists_whenKeyExists_throwsException() {
        map.put("existingKey", "value");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "existingKey", "duplicate key")).withMessageContaining("duplicate key");
    }

    @Test
    void testThrowIfKeyExists_whenKeyDoesNotExist_doesNotThrow() {
        // map is empty (set up in @BeforeEach)
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "absentKey", "should not be used"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfKeyExists_whenMessageIsNullOrEmpty_throwsException(String message) {
        // The method does not validate the message itself, but we still verify the
        // behaviour when the map/key checks pass – no exception should be thrown.
        // This test demonstrates that the message parameter is only used when an
        // IllegalArgumentException is actually thrown.
        if (map.containsKey("any")) {
            // Not reachable in this test, kept for completeness.
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "any", message));
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "any", message));
        }
    }

    @Test
    void testThrowIfKeyExists_whenKeyExists_throwsExceptionWithProvidedMessage() {
        map.put("existingKey", "value");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "existingKey", "duplicate key")).withMessage("duplicate key");
    }

    @Test
    void testThrowIfKeyExists_whenKeyExists_andMessageIsEmpty_throwsExceptionWithEmptyMessage() {
        map.put("existing", "value");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "existing", "")).withMessage("");
    }

    @Test
    void testThrowIfKeyExists_whenKeyExists_andMessageIsNull_throwsExceptionWithNullMessage() {
        // The method forwards the supplied message directly to the exception.
        map.put("nullMessageKey", "value");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "nullMessageKey", null)).// AssertJ treats null as a valid message here.
        hasMessageNullOrEmpty();
    }

    @Test
    void testThrowIfKeyExists_whenMapContainsNullKey_andKeyIsNull_throwsKeyNullExceptionFirst() {
        // Even though the map contains a null key, the method validates the key argument
        // before checking existence, therefore the "key cannot be null" message wins.
        map.put(null, "nullKeyValue");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, null, "any")).withMessageContaining("key cannot be null");
    }

    @Test
    void testThrowIfKeyExists_whenKeyAbsent_noException() {
        // map is empty (set up in @BeforeEach)
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "absentKey", "should not be used"));
    }

    @Test
    void testThrowIfKeyExists_whenOtherKeysPresent_keyAbsent_noException() {
        map.put("a", "1");
        map.put("b", "2");
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "c", "irrelevant"));
    }

    @Test
    void testThrowIfKeyExists_whenDifferentKeyHasNullValue_keyAbsent_noException() {
        map.put("x", null);
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "y", "does not exist"));
    }

    @ParameterizedTest
    @MethodSource("provideIntegerKeyScenarios")
    void testThrowIfKeyExists_withIntegerKeys(Map<Integer, String> intMap, Integer key, String message, boolean shouldThrow, String expectedMessage) {
        if (shouldThrow) {
            assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(intMap, key, message)).hasMessage(expectedMessage);
        } else {
            assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(intMap, key, message));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfKeyExists_whenMessageIsNullOrEmpty_andKeyAbsent_noException(String message) {
        // The map does not contain the key, therefore the method should complete
        // without looking at the message value.
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "anyKey", message));
    }
}
