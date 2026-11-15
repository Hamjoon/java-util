package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

class CaseInsensitiveMap_containsValue_Test {

    private CaseInsensitiveMap<String, String> ciMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor – it creates an internal LinkedHashMap
        ciMap = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination
        ciMap = null;
    }

    private static Stream<Arguments> valuePresentProvider() {
        return Stream.of(// String present
        Arguments.of("Apple", true), // Integer present
        Arguments.of(123, true));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// null not present → false
        Arguments.of(null, false), // empty string present → true
        Arguments.of("", true), // new Object not present → false
        Arguments.of(new Object(), false), // new Integer(123) equal to stored Integer → true
        Arguments.of(new Integer(123), true), // duplicate Apple (case‑sensitive) → true
        Arguments.of("Apple", true), // Long 123L different type from stored Integer → false
        Arguments.of(123L, false));
    }

    /**
     * Uses reflection to set the {@code map} field of {@link CaseInsensitiveMap}
     * to a custom implementation. This is required for the null‑value‑rejection
     * scenarios because the public API does not expose a way to inject a map.
     *
     * @param target the CaseInsensitiveMap instance whose internal map should be replaced
     * @param newMap the map implementation to inject
     * @throws Exception if reflection fails
     */
    private static void setInternalMap(CaseInsensitiveMap<?, ?> target, Map<?, ?> newMap) throws Exception {
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        // Remove the 'final' modifier (required for JDK 12+)
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(mapField, mapField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        mapField.set(target, newMap);
    }

    @Test
    void containsValue_ShouldReturnTrue_WhenValueExists() {
        // given
        ciMap.put("One", "Alpha");
        ciMap.put("Two", "Beta");
        ciMap.put("Three", "Gamma");
        // when
        boolean result = ciMap.containsValue("Beta");
        // then
        assertTrue(result, "containsValue should return true for an existing value");
    }

    @Test
    void containsValue_ShouldReturnFalse_WhenValueDoesNotExist() {
        // given
        ciMap.put("One", "Alpha");
        ciMap.put("Two", "Beta");
        // when
        boolean result = ciMap.containsValue("Delta");
        // then
        assertFalse(result, "containsValue should return false when the value is not present");
    }

    @Test
    void containsValue_ShouldHandleNullValues() {
        // given
        ciMap.put("One", null);
        ciMap.put("Two", "Beta");
        // when
        boolean containsNull = ciMap.containsValue(null);
        boolean containsNonNull = ciMap.containsValue("Beta");
        boolean doesNotContain = ciMap.containsValue("Alpha");
        // then
        assertTrue(containsNull, "containsValue should return true for a stored null value");
        assertTrue(containsNonNull, "containsValue should return true for an existing non‑null value");
        assertFalse(doesNotContain, "containsValue should return false for a value that is not stored");
    }

    @Test
    void containsValue_ShouldBeCaseSensitiveForValues() {
        // given
        ciMap.put("Key", "Value");
        // same letters, different case
        ciMap.put("Another", "value");
        // when
        boolean exactMatch = ciMap.containsValue("Value");
        boolean differentCase = ciMap.containsValue("value");
        boolean notPresent = ciMap.containsValue("VaLuE");
        // then
        assertTrue(exactMatch, "Exact case match should be found");
        assertTrue(differentCase, "Different case match should also be found because values are compared using standard equals");
        // Note: Map values are compared using Object.equals, which is case‑sensitive for Strings.
        // Therefore "VaLuE" is not equal to either stored value.
        assertFalse(notPresent, "Mixed‑case value not stored should not be found");
    }

    /**
     * Test cases where the value is present in the underlying map.
     */
    @ParameterizedTest(name = "[{index}] value={0} → expected={1}")
    @MethodSource("valuePresentProvider")
    void containsValue_ValuePresent(Object value, boolean expected) {
        // Populate the map with known entries
        ciMap.put("fruit", "Apple");
        ciMap.put("number", 123);
        boolean actual = ciMap.containsValue(value);
        assertEquals(expected, actual, () -> "containsValue should return " + expected + " for value: " + value);
    }

    /**
     * Edge‑case and special‑scenario tests.
     */
    @ParameterizedTest(name = "[{index}] value={0} → expected={1}")
    @MethodSource("edgeCaseProvider")
    void containsValue_EdgeCases(Object value, Object expectedResult) {
        // Prepare the map according to the description of each case
        // empty string
        ciMap.put("empty", "");
        // Integer 123
        ciMap.put("int", 123);
        // No null entry is added – used to test null‑absence case
        if (expectedResult instanceof Boolean) {
            boolean expected = (Boolean) expectedResult;
            boolean actual = ciMap.containsValue(value);
            assertEquals(expected, actual, () -> "containsValue should return " + expected + " for value: " + value);
        } else if (expectedResult instanceof Class) {
            // Expected an exception type
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expectedResult;
            assertThrows(exClass, () -> ciMap.containsValue(value), () -> "containsValue should throw " + exClass.getSimpleName() + " for value: " + value);
        }
    }

    @Test
    void containsValue_NullPointerException_WhenWrappedMapIsConcurrentHashMap() throws Exception {
        // Replace the internal map with a ConcurrentHashMap (does not allow null values)
        Map<String, Object> concurrentMap = new ConcurrentHashMap<>();
        setInternalMap(ciMap, concurrentMap);
        // The map is empty; calling containsValue(null) should throw NPE per Map contract
        assertThrows(NullPointerException.class, () -> ciMap.containsValue(null), "ConcurrentHashMap.containsValue(null) must throw NullPointerException");
    }

    @Test
    void containsValue_NullPointerException_WhenWrappedMapIsConcurrentSkipListMap() throws Exception {
        // Replace the internal map with a ConcurrentSkipListMap (does not allow null values)
        Map<String, Object> skipListMap = new ConcurrentSkipListMap<>();
        setInternalMap(ciMap, skipListMap);
        // The map is empty; calling containsValue(null) should throw NPE
        assertThrows(NullPointerException.class, () -> ciMap.containsValue(null), "ConcurrentSkipListMap.containsValue(null) must throw NullPointerException");
    }
}
