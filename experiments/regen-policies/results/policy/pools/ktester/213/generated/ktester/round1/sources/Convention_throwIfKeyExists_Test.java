package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.provider.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Convention_throwIfKeyExists_Test {

    private Map<String, String> stringMap;

    private Map<Integer, String> intMap;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh maps for each test to avoid cross‑test contamination.
        stringMap = new HashMap<>();
        intMap = new HashMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly necessary but keeps the intent clear.
        stringMap = null;
        intMap = null;
    }

    private static Stream<Arguments> provideMessageEdgeCases() {
        return Stream.of(// null message
        Arguments.of(null, null), // empty string
        Arguments.of("", ""), // regular non‑empty
        Arguments.of("explicit", "explicit"));
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenMapIsNull_throwsException(Map<?, ?> map) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, "anyKey", "map cannot be null")).withMessageContaining("map cannot be null");
    }

    @ParameterizedTest
    @NullSource
    void testThrowIfKeyExists_whenKeyIsNull_throwsException(String key) {
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(new HashMap<>(), key, "key cannot be null")).withMessageContaining("key cannot be null");
    }

    @Test
    void testThrowIfKeyExists_whenKeyExists_throwsException() {
        stringMap.put("existingKey", "value");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(stringMap, "existingKey", "duplicate key")).withMessageContaining("duplicate key");
    }

    @Test
    void testThrowIfKeyExists_whenKeyDoesNotExist_doesNotThrow() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(stringMap, "absentKey", "should not be used"));
    }

    @Test
    void testThrowIfKeyExists_genericTypes_keyExists_throwsException() {
        intMap.put(42, "answer");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(intMap, 42, "int key already present")).withMessageContaining("int key already present");
    }

    @Test
    void testThrowIfKeyExists_genericTypes_keyDoesNotExist_noException() {
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(intMap, 99, "should not matter"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testThrowIfKeyExists_invalidMessage_throwsException(String message) {
        // The method does not validate the message itself, but we verify that the
        // supplied message propagates when the key is present.
        stringMap.put("k", "v");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(stringMap, "k", message)).withMessageContaining(message == null ? "null" : message);
    }

    @Test
    void testThrowIfKeyExists_nullMessage_whenKeyExists_throwsExceptionWithNullMessage() {
        stringMap.put("k", "v");
        // The supplied message is null; the thrown exception should have a null message.
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(stringMap, "k", null)).satisfies(e -> assertThat(e.getMessage()).isNull());
    }

    @Test
    void testThrowIfKeyExists_emptyMessage_whenKeyExists_throwsExceptionWithEmptyMessage() {
        stringMap.put("x", "0");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(stringMap, "x", "")).hasMessage("");
    }

    @Test
    void testThrowIfKeyExists_differentStringInstancesSameLogicalKey_throwsException() {
        // Two distinct String objects that are equal via equals()
        String key1 = new String("a");
        String key2 = new String("a");
        Map<String, Integer> map = new HashMap<>();
        map.put(key1, 10);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, key2, "same logical key")).withMessageContaining("same logical key");
    }

    @Test
    void testThrowIfKeyExists_integerKeyDifferentInstances_throwsException() {
        // Integer objects with same int value but different instances
        Integer key1 = Integer.valueOf(123);
        Integer key2 = new Integer(123);
        Map<Integer, Integer> map = new HashMap<>();
        map.put(key1, 1);
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(map, key2, "numeric key duplicate")).withMessageContaining("numeric key duplicate");
    }

    @Test
    void testThrowIfKeyExists_emptyMap_keyAbsent_noException() {
        Map<String, Integer> emptyMap = new HashMap<>();
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(emptyMap, "anyKey", "irrelevant"));
    }

    @Test
    void testThrowIfKeyExists_mapWithOtherKeys_keyAbsent_noException() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        assertThatNoException().isThrownBy(() -> Convention.throwIfKeyExists(map, "c", "irrelevant"));
    }

    @ParameterizedTest
    @MethodSource("provideMessageEdgeCases")
    void testThrowIfKeyExists_messageEdgeCases(String message, String expectedMessage) {
        stringMap.put("k", "v");
        assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> Convention.throwIfKeyExists(stringMap, "k", message)).satisfies(e -> assertThat(e.getMessage()).isEqualTo(expectedMessage));
    }
}
