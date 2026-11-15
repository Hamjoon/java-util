package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_remove_2_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intMap;

    @BeforeEach
    void setupBeforeEach() {
        // Default maps use the library's default backing map (LinkedHashMap).
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("test", "value1");
        stringMap.put("mixed", "mixedValue");
        stringMap.put("nullKey", null);
        stringMap.put("Alpha", "A");
        stringMap.put("Beta", "B");
        stringMap.put("Gamma", "C");
        intMap = new CaseInsensitiveMap<>();
        intMap.put(123, "intValue");
        intMap.put(1, "One");
        intMap.put(2, "Two");
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the maps to avoid any cross‑test contamination.
        stringMap.clear();
        intMap.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset the cache length to its default value for any other tests that might run later.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /* --------------------------------------------------------------------- */
    private static void setBackingMap(CaseInsensitiveMap<?, ?> ciMap, Map<?, ?> newBacking) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(ciMap, newBacking);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> stringKeyCases() {
        return Stream.of(// matching value -> true
        Arguments.of("TeSt", "value1", true), // non‑matching value -> false
        Arguments.of("TeSt", "differentValue", false));
    }

    private static Stream<Arguments> nonStringIntegerCases() {
        return Stream.of(// matching
        Arguments.of(123, "intValue", true), // wrong value
        Arguments.of(123, "different", false), // absent key
        Arguments.of(999, "any", false));
    }

    private static Stream<Arguments> nullKeyCases() {
        return Stream.of(// matching value
        Arguments.of("nullValue", true), // wrong value
        Arguments.of("other", false), // no null key present (map cleared before test)
        Arguments.of("any", false));
    }

    private static Stream<Arguments> nullValueCases() {
        return Stream.of(// matching null value
        Arguments.of("nullKey", true), // non‑null value attempted (handled in other test)
        Arguments.of("nullKey", false));
    }

    private static Stream<Arguments> charSequenceKeyCases() {
        StringBuilder sb = new StringBuilder("builderKey");
        return Stream.of(// matching value
        Arguments.of(sb, "builderValue", true), // wrong value
        Arguments.of(sb, "different", false));
    }

    private static Stream<Arguments> mixedCaseKeyCases() {
        return Stream.of(// matching
        Arguments.of("MiXeD", "mixedValue", true), // wrong value
        Arguments.of("MiXeD", "otherValue", false));
    }

    @Test
    void removeStringKeyMatchingValue_ShouldReturnTrueAndDeleteEntry() {
        // Act
        // different case
        boolean removed = stringMap.remove("ALPHA", "A");
        // Assert
        assertTrue(removed, "remove should return true when key (case‑insensitive) and value match");
        assertNull(stringMap.get("Alpha"), "Entry should no longer be accessible via original case");
        assertNull(stringMap.get("alpha"), "Entry should no longer be accessible via any case");
        assertEquals(2, stringMap.size(), "Map size should be decremented");
    }

    @Test
    void removeStringKeyWithWrongValue_ShouldReturnFalseAndLeaveMapUnchanged() {
        // Act
        boolean removed = stringMap.remove("beta", "WRONG");
        // Assert
        assertFalse(removed, "remove should return false when the value does not match");
        assertEquals("B", stringMap.get("Beta"), "Original entry must remain unchanged");
        assertEquals(3, stringMap.size(), "Map size must stay the same");
    }

    @Test
    void removeStringKeyWhenKeyAbsent_ShouldReturnFalse() {
        // Act
        boolean removed = stringMap.remove("Delta", "D");
        // Assert
        assertFalse(removed, "remove should return false when the key is not present");
        assertEquals(3, stringMap.size(), "Map size must stay the same");
    }

    @Test
    void removeStringKeyWithNullValue_ShouldBehaveLikeBackingMap() {
        // Prepare a null value entry
        stringMap.put("NullKey", null);
        // Act
        boolean removed = stringMap.remove("nullkey", null);
        // Assert
        assertTrue(removed, "remove should succeed when both key (case‑insensitive) and null value match");
        assertNull(stringMap.get("NullKey"), "Entry should be removed");
    }

    @Test
    void removeNonStringKeyMatchingValue_ShouldReturnTrueAndDeleteEntry() {
        // Act
        boolean removed = intMap.remove(1, "One");
        // Assert
        assertTrue(removed, "remove should return true for matching non‑String key/value");
        assertNull(intMap.get(1), "Entry should be removed");
        assertEquals(1, intMap.size(), "Map size should be decremented");
    }

    @Test
    void removeNonStringKeyWithWrongValue_ShouldReturnFalse() {
        // Act
        boolean removed = intMap.remove(2, "WRONG");
        // Assert
        assertFalse(removed, "remove should return false when value does not match for non‑String key");
        assertEquals("Two", intMap.get(2), "Original entry must remain");
    }

    @Test
    void removeNonStringKeyWhenKeyAbsent_ShouldReturnFalse() {
        // Act
        boolean removed = intMap.remove(99, "Anything");
        // Assert
        assertFalse(removed, "remove should return false when the non‑String key is absent");
        assertEquals(2, intMap.size(), "Map size must stay unchanged");
    }

    @Test
    void removeNullKeyAndValue_ShouldDelegateToBackingMap() {
        // Add a null key/value entry
        intMap.put(null, null);
        // Act
        boolean removed = intMap.remove(null, null);
        // Assert
        assertTrue(removed, "remove should succeed for null key/value pair");
        assertNull(intMap.get(null), "Entry should be removed");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={0}, value={1} => expected={2}")
    @MethodSource("stringKeyCases")
    void removeStringKeyCaseInsensitive(Object key, Object value, boolean expected) {
        // Act
        boolean result = stringMap.remove(key, value);
        // Assert
        assertEquals(expected, result);
        // Additional state checks for successful removal
        if (expected) {
            assertNull(stringMap.get(key), "Entry should be removed");
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={0}, value={1} => expected={2}")
    @MethodSource("nonStringIntegerCases")
    void removeNonStringIntegerKey(Object key, Object value, boolean expected) {
        boolean result = intMap.remove(key, value);
        assertEquals(expected, result);
        if (expected) {
            assertNull(intMap.get(key), "Entry should be removed");
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key=null, value={1} => expected={2}")
    @MethodSource("nullKeyCases")
    void removeNullKey(Object value, Object expectedResult) {
        // Prepare map with a null key entry for the first case
        if (value == null || "nullValue".equals(value)) {
            // Ensure a null key exists with value "nullValue"
            stringMap.put(null, "nullValue");
        }
        boolean result = stringMap.remove(null, value);
        if (expectedResult instanceof Boolean) {
            assertEquals(expectedResult, result);
            if ((Boolean) expectedResult) {
                assertNull(stringMap.get(null), "Null key entry should be removed");
            }
        } else {
            fail("Unexpected expectedResult type");
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={0}, value=null => expected={1}")
    @MethodSource("nullValueCases")
    void removeNullValue(Object key, boolean expected) {
        // Ensure the map contains the entry ("nullKey", null) – already added in setup
        boolean result = stringMap.remove(key, null);
        assertEquals(expected, result);
        if (expected) {
            assertNull(stringMap.get(key), "Entry with null value should be removed");
        }
    }

    // -----------------------------------------------------------------
    @Test
    void removeStringKeyNotPresent() {
        boolean removed = stringMap.remove("Absent", "someValue");
        assertFalse(removed, "Removal must return false when key is absent");
        assertEquals(6, stringMap.size(), "Map size must stay unchanged");
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key=StringBuilder({0}), value={1} => expected={2}")
    @MethodSource("charSequenceKeyCases")
    void removeCharSequenceKey(Object key, Object value, boolean expected) {
        // Populate map with a StringBuilder key
        StringBuilder sbKey = new StringBuilder("builderKey");
        stringMap.put(sbKey, "builderValue");
        boolean result = stringMap.remove(key, value);
        assertEquals(expected, result);
        if (expected) {
            assertNull(stringMap.get(sbKey), "Entry should be removed");
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] key={0}, value={1} => expected={2}")
    @MethodSource("mixedCaseKeyCases")
    void removeMixedCaseKey(Object key, Object value, boolean expected) {
        boolean result = stringMap.remove(key, value);
        assertEquals(expected, result);
        if (expected) {
            assertNull(stringMap.get("mixed"), "Entry should be removed irrespective of case");
        }
    }

    // -----------------------------------------------------------------
    @Test
    void removeNullKeyOnConcurrentHashMapThrowsNPE() {
        // Replace backing map with ConcurrentHashMap (disallows null keys)
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new ConcurrentHashMap<>());
        // Populate with a non‑null entry to avoid empty‑map shortcuts
        map.put("someKey", "someValue");
        assertThrows(NullPointerException.class, () -> map.remove(null, "someValue"));
    }

    // -----------------------------------------------------------------
    @Test
    void removeNullValueOnConcurrentHashMapThrowsNPE() {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, new ConcurrentHashMap<>());
        map.put("someKey", "someValue");
        assertThrows(NullPointerException.class, () -> map.remove("someKey", null));
    }

    // -----------------------------------------------------------------
    @Test
    void removeStringKeyOnTreeMapWithIntegerOrderingThrowsCCE() {
        // TreeMap expecting Integer keys (natural ordering)
        TreeMap<Integer, String> backing = new TreeMap<>();
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, backing);
        // Insert an Integer key to satisfy the TreeMap's type expectations
        backing.put(1, "one");
        assertThrows(ClassCastException.class, () -> map.remove("stringKey", "value"));
    }

    @Test
    void removeIntegerKeyOnTreeMapWithStringOrderingThrowsCCE() {
        // TreeMap expecting String keys (natural ordering)
        TreeMap<String, String> backing = new TreeMap<>();
        CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, backing);
        // Insert a String key to satisfy the TreeMap's type expectations
        backing.put("a", "alpha");
        assertThrows(ClassCastException.class, () -> map.remove(123, "value"));
    }

    // -----------------------------------------------------------------
    @Test
    void removeOnUnmodifiableMapThrowsUOE() {
        Map<String, String> modifiable = new LinkedHashMap<>();
        modifiable.put("anyKey", "anyValue");
        Map<String, String> unmodifiable = Collections.unmodifiableMap(modifiable);
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        setBackingMap(map, unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> map.remove("anyKey", "anyValue"));
    }
}
