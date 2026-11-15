package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_get_Test {

    /**
     * The concrete instance under test – we use an anonymous subclass because the class is abstract.
     */
    private AbstractConcurrentNullSafeMap<String, String> mapUnderTest;

    /**
     * The real internal map that backs the abstract implementation.
     */
    private ConcurrentMap<Object, Object> internalMap;

    /* --------------------------------------------------------------------- *
     *                     Helper holder class for parameterisation          *
     * --------------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final Object key;

        // null indicates expected null result
        final String expected;

        final String description;

        ArgumentsHolder(Object key, String expected, String description) {
            this.key = key;
            this.expected = expected;
            this.description = description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a real ConcurrentHashMap so that mask/unmask logic can be exercised.
        internalMap = new ConcurrentHashMap<>();
        mapUnderTest = new AbstractConcurrentNullSafeMap<String, String>(internalMap) {
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure a clean state for the next test.
        internalMap.clear();
    }

    // ---------- Null key handling ----------
    private static Stream<ArgumentsHolder> nullKeyProvider() {
        return Stream.of(// case 1 – null key present with non‑null value
        new ArgumentsHolder(null, "valueForNullKey", "When the supplied key is null and the map contains an entry for the null key with a non‑null value, get(null) should return that value."), // case 2 – null key present with null value
        new ArgumentsHolder(null, null, "When the supplied key is null and the map contains an entry for the null key whose value is null, get(null) should return null."), // case 3 – null key absent
        new ArgumentsHolder(null, null, "When the supplied key is null and the map does NOT contain a mapping for the null key, get(null) should return null."));
    }

    // ---------- Non‑null key handling ----------
    private static Stream<ArgumentsHolder> nonNullKeyProvider() {
        return Stream.of(// existing key
        new ArgumentsHolder("existingKey", "valueForExistingKey", "When the key is a non‑null object present in the map, get(key) should return the associated value."), // missing key
        new ArgumentsHolder("missingKey", null, "When the key is non‑null but not present in the map, get(key) should return null."), // key with null value
        new ArgumentsHolder("keyWithNullValue", null, "When the key is present and its associated value is null (masked internally), get(key) should correctly unmask and return null."), // distinct but equal key (different instance)
        new ArgumentsHolder(new String("newStringKey"), "valueForEqualKey", "When a distinct but equal key object (different instance) is used, get should locate the mapping based on equals and return the correct value."));
    }

    @Test
    void get_returnsValueForExistingNonNullKey() {
        // given
        String key = "foo";
        String value = "bar";
        // uses the implementation that ultimately stores a masked entry
        mapUnderTest.put(key, value);
        // when
        String result = mapUnderTest.get(key);
        // then
        assertEquals(value, result, "get() should return the value previously put for a non‑null key");
        // Verify that the internal map indeed contains a masked entry (the key stored is not the original key)
        assertTrue(internalMap.containsKey(mapUnderTest.maskNullKey(key)), "Internal map should contain the masked version of the key");
    }

    @Test
    void get_returnsValueForExistingNullKey() {
        // given
        String value = "nullKeyValue";
        // null key is allowed by the null‑safe wrapper
        mapUnderTest.put(null, value);
        // when
        String result = mapUnderTest.get(null);
        // then
        assertEquals(value, result, "get() should return the value associated with a null key");
        // The internal map must contain a sentinel object rather than a literal null key.
        Object maskedKey = mapUnderTest.maskNullKey(null);
        assertNotNull(maskedKey, "maskNullKey(null) must return a non‑null sentinel");
        assertTrue(internalMap.containsKey(maskedKey), "Internal map should store the sentinel key for a null key");
    }

    @Test
    void get_returnsNullWhenKeyIsAbsent() {
        // given
        String absentKey = "doesNotExist";
        // when
        String result = mapUnderTest.get(absentKey);
        // then
        assertNull(result, "get() should return null when the key is not present in the map");
    }

    @Test
    void get_returnsNullWhenStoredValueIsNull() {
        // given
        String key = "someKey";
        // store a null value
        mapUnderTest.put(key, null);
        // when
        String result = mapUnderTest.get(key);
        // then
        assertNull(result, "get() should return null for a key whose associated value is null");
        // Ensure the internal map holds the sentinel for the null value.
        Object maskedKey = mapUnderTest.maskNullKey(key);
        Object stored = internalMap.get(maskedKey);
        assertEquals(mapUnderTest.maskNullValue(null), stored, "Internal map should contain the masked sentinel for a null value");
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nullKeyProvider")
    void get_nullKeyHandling(ArgumentsHolder holder) {
        // Arrange
        if (holder.expected != null || holder.description.contains("contains an entry")) {
            // Put an entry for the null key (value may be null)
            mapUnderTest.put(null, holder.expected);
        }
        // Act
        String result = mapUnderTest.get(null);
        // Assert
        assertEquals(holder.expected, result);
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("nonNullKeyProvider")
    void get_nonNullKeyHandling(ArgumentsHolder holder) {
        // Arrange
        if (holder.expected != null) {
            // Store the mapping using a canonical key instance
            String canonicalKey = holder.key instanceof String ? (String) holder.key : holder.key.toString();
            mapUnderTest.put(canonicalKey, holder.expected);
        }
        // Act
        String result = mapUnderTest.get(holder.key);
        // Assert
        assertEquals(holder.expected, result);
    }

    // ---------- Numeric key handling (different generic types) ----------
    @Test
    void get_numericKeyHandling() {
        // Use a map with Object keys/values to allow Integer keys.
        ConcurrentMap<Object, Object> numericInternal = new ConcurrentHashMap<>();
        AbstractConcurrentNullSafeMap<Object, Object> numericMap = new AbstractConcurrentNullSafeMap<Object, Object>(numericInternal) {
        };
        Integer key = 12345;
        String value = "numericKeyValue";
        numericMap.put(key, value);
        Object result = numericMap.get(key);
        assertEquals(value, result, "get() should return the value associated with an Integer key");
    }

    // ---------- Exception scenarios ----------
    @Test
    void get_throwsClassCastExceptionForIncompatibleKey() {
        // The map is typed <String, String>; passing an Object that cannot be cast to String should fail.
        Object badKey = new Object();
        assertThrows(ClassCastException.class, () -> mapUnderTest.get(badKey), "Calling get() with a key of incompatible type should throw ClassCastException");
    }

    @Test
    void get_throwsNullPointerExceptionWhenInternalMapIsNull() throws Exception {
        // Create a map instance and then use reflection to null out the internalMap field.
        AbstractConcurrentNullSafeMap<String, String> brokenMap = new AbstractConcurrentNullSafeMap<String, String>(new ConcurrentHashMap<>()) {
        };
        // Use reflection to set the final field to null.
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(brokenMap, null);
        assertThrows(NullPointerException.class, () -> brokenMap.get("anyKey"), "If internalMap is null, get() should propagate NullPointerException");
    }
}
