package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_replace_2_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intMap;

    @BeforeEach
    void setupBeforeEach() {
        // Map with String keys – default backing LinkedHashMap
        stringMap = new CaseInsensitiveMap<>();
        // lower‑case stored key
        stringMap.put("keyone", "oldValue");
        stringMap.put("keythree", "oldValue");
        stringMap.put("mixedcase", "oldMixed");
        stringMap.put("samekey", "sameValue");
        stringMap.put("alpha", "A");
        stringMap.put("beta", "B");
        stringMap.put("gamma", "C");
        // Map with non‑String keys to verify that replace works unchanged
        intMap = new CaseInsensitiveMap<>();
        intMap.put(123, "intOld");
        intMap.put(1, "One");
        intMap.put(2, "Two");
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any static configuration that other tests might have altered
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Data provider for the “basic functionality” group.
     */
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// Replace existing entry (case‑insensitive)
        Arguments.of("KeyOne", "newValue", "oldValue", true, "Replace existing entry with matching case‑insensitive key."), // Replace non‑existent key
        Arguments.of("KEYTWO", "value2", null, false, "Attempt to replace a non‑existent key."), // Null key – we expect NPE from normalizeKey (the implementation treats null specially)
        Arguments.of(null, "any", NullPointerException.class, false, "Pass a null key; normalizeKey should throw NPE."));
    }

    /**
     * Data provider for “null value handling”.
     */
    private static Stream<Arguments> nullValueHandlingProvider() {
        return Stream.of(// Existing key, new value is null
        Arguments.of("KeyThree", null, "oldValue", true, "Replace an existing entry with a null value."), // Non‑existent key, null value
        Arguments.of("KeyFour", null, null, false, "Attempt to replace a non‑existent key with a null value."));
    }

    /**
     * Data provider for “non‑String key types”.
     */
    private static Stream<Arguments> nonStringKeyProvider() {
        return Stream.of(// Integer key present
        Arguments.of(123, "numericValue", "intOld", true, "Replace an entry where the key is an Integer."), // Boolean key absent
        Arguments.of(true, "boolNew", null, false, "Attempt to replace a Boolean key that does not exist."));
    }

    @Test
    void replaceExistingStringKey_ShouldReturnOldValueAndUpdateMapping() {
        // Act
        String old = stringMap.replace("ALPHA", "NewA");
        // Assert
        assertEquals("A", old, "replace should return the previous value");
        assertEquals("NewA", stringMap.get("alpha"), "new value must be stored (case‑insensitive)");
        assertEquals("NewA", stringMap.get("ALPHA"), "retrieval must be case‑insensitive");
        // other entries must stay untouched
        assertEquals("B", stringMap.get("beta"));
        assertEquals("C", stringMap.get("GAMMA"));
    }

    @Test
    void replaceNonExistingStringKey_ShouldReturnNullAndLeaveMapUnchanged() {
        // Act
        String result = stringMap.replace("Delta", "D");
        // Assert
        assertNull(result, "replace on a missing key must return null");
        assertNull(stringMap.get("Delta"), "no new entry should be created");
        // existing entries must remain the same
        assertEquals("A", stringMap.get("Alpha"));
        assertEquals("B", stringMap.get("Beta"));
        assertEquals("C", stringMap.get("Gamma"));
    }

    @Test
    void replaceWithNullKey_ShouldBehaveLikeStandardMap() {
        // Arrange
        stringMap.put(null, "nullValue");
        // Act
        String previous = stringMap.replace(null, "newNull");
        // Assert
        assertEquals("nullValue", previous, "previous value for null key should be returned");
        assertEquals("newNull", stringMap.get(null), "null key should now map to the new value");
    }

    @Test
    void replaceExistingNonStringKey_ShouldWorkWithoutCaseLogic() {
        // Act
        String old = intMap.replace(1, "Uno");
        // Assert
        assertEquals("One", old, "replace must return the previous value for non‑String keys");
        assertEquals("Uno", intMap.get(1), "new value must be stored for the integer key");
        // other entry untouched
        assertEquals("Two", intMap.get(2));
    }

    @Test
    void replaceNonExistingNonStringKey_ShouldReturnNullAndNotCreateEntry() {
        // Act
        String result = intMap.replace(3, "Three");
        // Assert
        assertNull(result, "replace on missing non‑String key must return null");
        assertNull(intMap.get(3), "no new entry should be added for the missing key");
    }

    /**
     * Helper to inject a custom backing map via reflection.
     */
    @SuppressWarnings("unchecked")
    private static <K, V> void setBackingMap(CaseInsensitiveMap<K, V> cim, Map<K, V> backing) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(cim, backing);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("basicFunctionalityProvider")
    void replaceBasicFunctionality(String key, String newValue, Object expected, boolean expectSuccess, String description) {
        if (expected instanceof Class && ((Class<?>) expected).equals(NullPointerException.class)) {
            assertThrows(NullPointerException.class, () -> stringMap.replace(key, newValue), description);
            return;
        }
        String old = stringMap.replace(key, newValue);
        if (expectSuccess) {
            assertEquals(expected, old, description);
            assertEquals(newValue, stringMap.get(key), description);
        } else {
            assertNull(old, description);
            assertNull(stringMap.get(key), description);
        }
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("nullValueHandlingProvider")
    void replaceNullValueHandling(String key, String newValue, String expectedOld, boolean expectSuccess, String description) {
        String old = stringMap.replace(key, newValue);
        if (expectSuccess) {
            assertEquals(expectedOld, old, description);
            assertTrue(stringMap.containsKey(key), "Key should still be present");
            assertNull(stringMap.get(key), "Value should now be null");
        } else {
            assertNull(old, description);
            assertFalse(stringMap.containsKey(key), "Map must not contain the missing key");
        }
    }

    @ParameterizedTest(name = "{4}")
    @MethodSource("nonStringKeyProvider")
    void replaceNonStringKeys(Object key, String newValue, String expectedOld, boolean expectSuccess, String description) {
        // intMap already contains the Integer entry; for Boolean we use the same map (it will simply miss)
        String old = intMap.replace(key, newValue);
        if (expectSuccess) {
            assertEquals(expectedOld, old, description);
            assertEquals(newValue, intMap.get(key), description);
        } else {
            assertNull(old, description);
            assertNull(intMap.get(key), description);
        }
    }

    /**
     * Empty map behavior
     */
    @Test
    void replaceOnEmptyMap_ShouldReturnNullAndLeaveMapEmpty() {
        CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
        String result = empty.replace("anyKey", "anyValue");
        assertNull(result, "replace on empty map must return null");
        assertTrue(empty.isEmpty(), "map must remain empty");
    }

    /**
     * Replace with same value
     */
    @Test
    void replaceWithSameValue_ShouldReturnPreviousValue() {
        String old = stringMap.replace("SameKey", "sameValue");
        assertEquals("sameValue", old, "replace should return the previous (identical) value");
        assertEquals("sameValue", stringMap.get("samekey"), "value must stay unchanged");
    }

    /**
     * Whitespace and case‑mix handling
     */
    @Test
    void replaceKeyWithWhitespace_ShouldReturnNull() {
        // No entry with leading/trailing spaces exists
        String result = stringMap.replace("  SpAcE  ", "trimmed");
        assertNull(result, "Whitespace‑laden key does not match any entry");
        assertNull(stringMap.get("  SpAcE  "), "Map must not contain the whitespace key");
    }

    @Test
    void replaceMixedCaseKey_ShouldMatchLowercaseEntry() {
        String old = stringMap.replace("MiXeDCase", "newMixed");
        assertEquals("oldMixed", old, "Mixed‑case key should match stored lower‑case key");
        assertEquals("newMixed", stringMap.get("mixedcase"), "New value must be stored");
    }

    /**
     * Unsupported backing map – unmodifiable map
     */
    @Test
    void replaceWithUnmodifiableBackingMap_ShouldThrowUnsupportedOperationException() {
        Map<String, String> source = Collections.unmodifiableMap(new HashMap<>());
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>(source);
        // Populate via reflection because put would already throw UOE
        setBackingMap(map, source);
        assertThrows(UnsupportedOperationException.class, () -> map.replace("anyKey", "newValue"), "replace must propagate UnsupportedOperationException from the backing map");
    }

    /**
     * Null value with ConcurrentHashMap (null values not allowed)
     */
    @Test
    void replaceNullValueOnConcurrentHashMap_ShouldThrowNullPointerException() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new ConcurrentHashMap<>());
        map.put("someKey", "present");
        assertThrows(NullPointerException.class, () -> map.replace("someKey", null), "ConcurrentHashMap does not allow null values");
    }

    /**
     * ClassCastException with TreeMap (String comparator vs Integer key)
     */
    @Test
    void replaceWithIncompatibleKeyOnTreeMap_ShouldThrowClassCastException() {
        CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
        // TreeMap expects comparable keys (String by default)
        setBackingMap(map, new TreeMap<>());
        map.put("stringKey", "value");
        assertThrows(ClassCastException.class, () -> map.replace(123, "numericValue"), "TreeMap with String comparator should throw ClassCastException for Integer key");
    }

    /**
     * Null value with Hashtable (null values not allowed)
     */
    @Test
    void replaceNullValueOnHashtable_ShouldThrowNullPointerException() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new Hashtable<>());
        map.put("hashKey", "present");
        assertThrows(NullPointerException.class, () -> map.replace("hashKey", null), "Hashtable does not allow null values");
    }
}
