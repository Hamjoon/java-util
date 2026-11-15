package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_getOrDefault_Test {

    private TestMap<String, String> map;

    /**
     * Simple concrete implementation used only for testing
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> backing) {
            super(backing);
        }
    }

    /**
     * Subclass that deliberately passes a {@code null} backing map to trigger NPE
     */
    private static class NullBackingMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullBackingMap() {
            super(null);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh map backed by a new ConcurrentHashMap
        ConcurrentMap<Object, Object> backing = new ConcurrentHashMap<>();
        map = new TestMap<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear the map to help GC and avoid cross‑test contamination
        map.clear();
    }

    private static Stream<Arguments> nullDefaultValueHandlingProvider() {
        return Stream.of(Arguments.of("Key absent and default value is null; method should return null.", "missingKey", null, null), Arguments.of("Key present; even if default is null, the stored value should be returned.", "existingKey", null, "value1"));
    }

    private static Stream<Arguments> defaultValueHandlingProvider() {
        return Stream.of(Arguments.of("Absent key returns supplied non‑null default.", "absentKey", "defaultVal", "defaultVal"), Arguments.of("Present key returns stored value, ignoring default.", "presentKey", "defaultVal", "storedVal"));
    }

    private static Stream<Arguments> nullKeyHandlingProvider() {
        return Stream.of(Arguments.of("A null key is present in the map (masked internally); the method should return its associated value \"nullKeyVal\".", null, "defaultForNullKey", "nullKeyVal"), Arguments.of("The map does not contain a null key; the method should return the provided default value.", null, "defaultForMissingNull", "defaultForMissingNull"), Arguments.of("No null key present and default value is null; the method should return null.", null, null, null));
    }

    private static Stream<Arguments> nullStoredValueHandlingProvider() {
        return Stream.of(Arguments.of("The key exists but its associated value is null (masked internally); the method must return null, not the default.", "keyWithNullValue", "nonNullDefault", null), Arguments.of("The key exists with a null value and the default is also null; the method should return null.", "keyWithNullValue", null, null));
    }

    /**
     * Invokes the protected {@code maskNullKey} method via reflection.
     */
    private static Object invokeMaskNullKey(AbstractConcurrentNullSafeMap<?, ?> map, Object key) throws Exception {
        var method = AbstractConcurrentNullSafeMap.class.getDeclaredMethod("maskNullKey", Object.class);
        method.setAccessible(true);
        return method.invoke(map, key);
    }

    @Test
    void getOrDefault_returnsExistingValue() {
        // given
        map.put("key1", "value1");
        // when
        String result = map.getOrDefault("key1", "default");
        // then
        assertEquals("value1", result, "Should return the stored value when key is present");
    }

    @Test
    void getOrDefault_returnsDefaultWhenKeyMissing() {
        // given – map is empty
        // when
        String result = map.getOrDefault("missing", "default");
        // then
        assertEquals("default", result, "Should return the supplied default for absent keys");
    }

    @Test
    void getOrDefault_returnsNullValueStoredInMap() {
        // given
        map.put("nullValueKey", null);
        // when
        String result = map.getOrDefault("nullValueKey", "default");
        // then
        assertNull(result, "Should return the actual null value stored, not the default");
    }

    @Test
    void getOrDefault_handlesNullKey() {
        // given
        map.put(null, "nullKeyValue");
        // when
        String result = map.getOrDefault(null, "default");
        // then
        assertEquals("nullKeyValue", result, "Should correctly handle a null key");
    }

    @Test
    void getOrDefault_returnsDefaultWhenNullKeyAbsent() {
        // given – map does not contain a null key
        // when
        String result = map.getOrDefault(null, "defaultForNull");
        // then
        assertEquals("defaultForNull", result, "Should return default when null key is not present");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullDefaultValueHandlingProvider")
    void getOrDefault_nullDefaultValueHandling(String description, Object key, String defaultValue, Object expected) {
        // arrange – put a value for the “existingKey” case
        if ("existingKey".equals(key)) {
            map.put("existingKey", "value1");
        }
        // act
        String result = map.getOrDefault(key, defaultValue);
        // assert
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("defaultValueHandlingProvider")
    void getOrDefault_defaultValueHandling(String description, Object key, String defaultValue, Object expected) {
        // arrange – set up the presentKey case
        if ("presentKey".equals(key)) {
            map.put("presentKey", "storedVal");
        }
        // act
        String result = map.getOrDefault(key, defaultValue);
        // assert
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullKeyHandlingProvider")
    void getOrDefault_nullKeyHandling(String description, Object key, String defaultValue, Object expected) {
        // arrange – put a mapping for the null‑key present case
        if (expected != null && !"defaultForMissingNull".equals(expected) && !"null".equals(expected)) {
            map.put(null, "nullKeyVal");
        }
        // act
        String result = map.getOrDefault(key, defaultValue);
        // assert
        if (expected == null) {
            assertNull(result, description);
        } else {
            assertEquals(expected, result, description);
        }
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullStoredValueHandlingProvider")
    void getOrDefault_nullStoredValueHandling(String description, Object key, String defaultValue, Object expected) {
        // arrange – store a null value for the key
        map.put("keyWithNullValue", null);
        // act
        String result = map.getOrDefault(key, defaultValue);
        // assert
        assertEquals(expected, result, description);
    }

    @Test
    void getOrDefault_typeMismatchKey_throwsClassCastException() {
        // map is declared as <String, String>
        // passing an Integer key should cause a ClassCastException during the cast to K
        assertThrows(ClassCastException.class, () -> map.getOrDefault(12345, "anyDefault"), "Passing a key of the wrong type must raise ClassCastException");
    }

    @Test
    void getOrDefault_typeMismatchValue_throwsClassCastException() throws Exception {
        // Bypass type‑safe put() by inserting a raw Integer value directly into the backing map.
        // The key is a proper String, but the stored value is an Integer.
        ConcurrentMap<Object, Object> backing = getInternalMap(map);
        // mask the key as the implementation would do
        Object maskedKey = invokeMaskNullKey(map, "badKey");
        // raw Integer, not a String
        backing.put(maskedKey, 42);
        // Now calling getOrDefault should attempt to cast the Integer to String and fail.
        assertThrows(ClassCastException.class, () -> map.getOrDefault("badKey", "defaultString"), "A stored value of incompatible type must raise ClassCastException");
    }

    @Test
    void getOrDefault_nullInternalMap_throwsNullPointerException() {
        NullBackingMap<String, String> nullMap = new NullBackingMap<>();
        assertThrows(NullPointerException.class, () -> nullMap.getOrDefault("anyKey", "anyDefault"), "If internalMap is null, getOrDefault must throw NPE");
    }

    /**
     * Retrieves the private {@code internalMap} field from a map instance.
     */
    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Object, Object> getInternalMap(AbstractConcurrentNullSafeMap<?, ?> map) throws Exception {
        Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        return (ConcurrentMap<Object, Object>) f.get(map);
    }
}
