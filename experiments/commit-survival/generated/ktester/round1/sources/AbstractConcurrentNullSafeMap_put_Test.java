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
class AbstractConcurrentNullSafeMap_put_Test {

    /**
     * The map instance used in each test.
     */
    private AbstractConcurrentNullSafeMap<String, String> map;

    /**
     * A fresh internal map for each test.
     */
    private ConcurrentMap<Object, Object> internalMap;

    /**
     * Simple concrete implementation of the abstract class for testing purposes.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internalMap) {
            super(internalMap);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        internalMap = new ConcurrentHashMap<>();
        map = new TestMap<>(internalMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid cross‑test contamination.
        internalMap.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Insert a new entry – previous should be null.
        Arguments.of("foo", "bar", null, "bar"), // Replace the existing entry – previous should be the old value.
        Arguments.of("foo", "baz", "bar", "baz"));
    }

    private static Stream<Arguments> nullKeyHandlingProvider() {
        return Stream.of(// First insertion with null key.
        Arguments.of("valueForNullKey", null, "valueForNullKey"), // Replacement of existing null‑key entry.
        Arguments.of("secondValueForNullKey", "valueForNullKey", "secondValueForNullKey"));
    }

    private static Stream<Arguments> nullValueHandlingProvider() {
        return Stream.of(// Insert a null value first.
        Arguments.of("keyWithNullValue", null, "newValue"));
    }

    private static Stream<Arguments> bothNullHandlingProvider() {
        return Stream.of(// Replace null value with a non‑null value.
        Arguments.of("nonNullAfterNull", null, "nonNullAfterNull"));
    }

    @Test
    void putNonNullKeyAndValue_returnsPreviousValue() {
        // given
        map.put("a", "first");
        // when
        String previous = map.put("a", "second");
        // then
        assertEquals("first", previous, "put should return the previous value associated with the key");
        assertEquals("second", map.get("a"), "the map should contain the new value after put");
    }

    @Test
    void putNewKey_returnsNull() {
        // when
        String previous = map.put("newKey", "value");
        // then
        assertNull(previous, "put should return null when the key was not previously present");
        assertEquals("value", map.get("newKey"));
    }

    @Test
    void putNullKeyAndNullValue_handledByMasking() {
        // when
        String previous = map.put(null, null);
        // then
        assertNull(previous, "previous value should be null for first insertion of null key");
        assertNull(map.get(null), "retrieving a null key should yield null value");
        // put again to ensure masking works for replacement
        String previous2 = map.put(null, "notNull");
        assertNull(previous2, "previous should be null because the stored value was masked null");
        assertEquals("notNull", map.get(null));
    }

    @Test
    void putNullKeyWithNonNullValue() {
        // when
        String previous = map.put(null, "value");
        // then
        assertNull(previous);
        assertEquals("value", map.get(null));
    }

    @Test
    void putNonNullKeyWithNullValue() {
        // when
        String previous = map.put("key", null);
        // then
        assertNull(previous);
        assertNull(map.get("key"));
    }

    @Test
    void putMultipleEntries_maintainsAllMappings() {
        // given
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put(null, "nullKey");
        map.put("nullValue", null);
        // when / then
        assertEquals("v1", map.get("k1"));
        assertEquals("v2", map.get("k2"));
        assertEquals("nullKey", map.get(null));
        assertNull(map.get("nullValue"));
        assertEquals(4, map.size());
    }

    @ParameterizedTest(name = "[{index}] key={0}, value={1}")
    @MethodSource("basicFunctionalityProvider")
    void putBasicFunctionality(String key, String value, String expectedPrev, String expectedCurrent) {
        // First insertion – map should be empty, therefore previous is null.
        String prev = map.put(key, value);
        assertEquals(expectedPrev, prev, "The previous value returned by put() does not match the expectation.");
        // Verify the new mapping is present.
        assertEquals(expectedCurrent, map.get(key), "The map does not contain the expected value after put().");
    }

    @ParameterizedTest(name = "[{index}] key=null, value={1}")
    @MethodSource("nullKeyHandlingProvider")
    void putNullKeyHandling(String value, String expectedPrev, String expectedCurrent) {
        String prev = map.put(null, value);
        assertEquals(expectedPrev, prev, "Previous value for null key does not match expectation.");
        assertEquals(expectedCurrent, map.get(null), "Value retrieved with null key is not as expected.");
        assertTrue(map.containsKey(null), "Map should report containing the null key after insertion.");
    }

    @ParameterizedTest(name = "[{index}] key={0}, value=null")
    @MethodSource("nullValueHandlingProvider")
    void putNullValueHandling(String key, String expectedPrev, String expectedCurrent) {
        // Insert a null value.
        String prev = map.put(key, null);
        assertEquals(expectedPrev, prev, "Previous value when inserting null should be as expected.");
        assertNull(map.get(key), "Retrieving the key after inserting a null value must return null.");
        assertTrue(map.containsKey(key), "Key should be present even when its associated value is null.");
        // Now replace the null value with a non‑null one.
        String prev2 = map.put(key, expectedCurrent);
        assertEquals(expectedPrev, prev2, "Previous value when replacing a null should be null.");
        assertEquals(expectedCurrent, map.get(key), "After replacement, the map should contain the new non‑null value.");
    }

    @ParameterizedTest(name = "[{index}] both null, secondValue={1}")
    @MethodSource("bothNullHandlingProvider")
    void putBothNullHandling(String secondValue, String expectedPrev, String expectedCurrent) {
        // First insertion with both key and value null.
        String prev = map.put(null, null);
        assertEquals(expectedPrev, prev, "First insertion with both null should return null.");
        assertNull(map.get(null), "Retrieving null key after first insertion must return null.");
        assertTrue(map.containsKey(null), "Map must contain the null key after first insertion.");
        // Second insertion – replace the null value with a non‑null one.
        String prev2 = map.put(null, secondValue);
        assertEquals(expectedPrev, prev2, "Previous value when replacing a null entry should be null.");
        assertEquals(expectedCurrent, map.get(null), "After replacement, the map should return the new non‑null value.");
    }

    @Test
    void replaceExistingNullMapping() {
        // Step 1 – insert a key with a null value.
        String prev1 = map.put("replaceNullKey", null);
        assertNull(prev1, "First insertion should return null.");
        assertNull(map.get("replaceNullKey"), "Value should be null after first insertion.");
        assertTrue(map.containsKey("replaceNullKey"), "Key must be present after inserting null value.");
        // Step 2 – replace the null value with a real value.
        String prev2 = map.put("replaceNullKey", "actualValue");
        assertNull(prev2, "Previous value (null) should be returned as null.");
        assertEquals("actualValue", map.get("replaceNullKey"), "The map must contain the new non‑null value after replacement.");
    }

    @Test
    void sizeAdjustments() {
        // Insert first distinct key.
        String p1 = map.put("sizeKey1", "v1");
        assertNull(p1, "First insertion should return null.");
        assertEquals(1, map.size(), "Size should be 1 after first insertion.");
        // Insert second distinct key.
        String p2 = map.put("sizeKey2", "v2");
        assertNull(p2, "Second insertion should return null.");
        assertEquals(2, map.size(), "Size should be 2 after second insertion.");
        // Replace value for an existing key – size must stay unchanged.
        String p3 = map.put("sizeKey1", "v1Updated");
        assertEquals("v1", p3, "Previous value must be returned when updating an existing key.");
        assertEquals(2, map.size(), "Size must remain 2 after updating an existing entry.");
    }

    @Test
    void putWithNullInternalMapThrowsNPE() throws Exception {
        // Create a map instance whose internalMap field is deliberately set to null.
        AbstractConcurrentNullSafeMap<String, String> brokenMap = new TestMap<>(null);
        // Use reflection to ensure the field really is null (defensive, not strictly required).
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(brokenMap, null);
        // The call must throw NPE because the implementation dereferences internalMap.
        assertThrows(NullPointerException.class, () -> brokenMap.put("anyKey", "anyValue"), "Calling put() when internalMap is null should throw NullPointerException.");
    }
}
