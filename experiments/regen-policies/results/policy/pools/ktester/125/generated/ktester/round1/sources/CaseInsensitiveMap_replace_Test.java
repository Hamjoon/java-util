package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

class CaseInsensitiveMap_replace_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intMap;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh maps for each test to avoid cross‑test interference
        stringMap = new CaseInsensitiveMap<>();
        intMap = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no lingering state
        stringMap = null;
        intMap = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        // Reset static configuration to default for any other test suites
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    /**
     * Data for the "basic success and failure cases" group.
     */
    private static Stream<ReplaceCase> basicSuccessFailureProvider() {
        return Stream.of(new ReplaceCase("Apple", "red", "green", true, "Key \"Apple\" exists with value \"red\". replace should return true and the value becomes \"green\"."), new ReplaceCase("Apple", "yellow", "green", false, "Key \"Apple\" exists but oldValue does not match. replace should return false and the original value stays \"red\"."));
    }

    /**
     * Data for the "case insensitivity" group.
     */
    private static Stream<ReplaceCase> caseInsensitivityProvider() {
        return Stream.of(new ReplaceCase("apple", "red", "green", true, "Map contains key \"Apple\" (mixed case) with value \"red\"; lower‑case key should succeed."), new ReplaceCase("ApPlE", "green", "yellow", true, "After previous replacement, entry for \"Apple\" holds \"green\"; mixed‑case key should update to \"yellow\"."));
    }

    /**
     * Data for the "key absent" group.
     */
    private static Stream<ReplaceCase> keyAbsentProvider() {
        return Stream.of(new ReplaceCase("Banana", "yellow", "green", false, "Map does not contain the key \"Banana\"; replace must return false."));
    }

    /**
     * Data for the "null oldValue" group.
     */
    private static Stream<ReplaceCase> nullOldValueProvider() {
        return Stream.of(new ReplaceCase("Apple", null, "green", false, "Existing value is non‑null; null oldValue does not match, replace returns false."));
    }

    /**
     * Data for the "null newValue" group.
     */
    private static Stream<ReplaceCase> nullNewValueProvider() {
        return Stream.of(new ReplaceCase("Apple", "red", null, true, "Replacing existing \"red\" with null is allowed; replace returns true."));
    }

    /**
     * Data for the "non‑String key type" group.
     */
    private static Stream<ReplaceCase> nonStringKeyProvider() {
        return Stream.of(new ReplaceCase(42, "value1", "value2", true, "Non‑String key 42 exists; replace succeeds."));
    }

    /**
     * Data for the "empty map" group.
     */
    private static Stream<ReplaceCase> emptyMapProvider() {
        return Stream.of(new ReplaceCase("Apple", "red", "green", false, "Calling replace on an empty map should return false."));
    }

    /**
     * Data for the "null key handling" group – expecting NPE.
     */
    private static Stream<ReplaceCase> nullKeyProvider() {
        return Stream.of(new ReplaceCase(null, "any", "new", NullPointerException.class, "Passing a null key should cause NullPointerException."));
    }

    @Test
    void replaceExistingKeyWithMatchingOldValue_ShouldReturnTrueAndUpdateValue() {
        // given
        stringMap.put("Key", "OldValue");
        // when
        boolean replaced = stringMap.replace("kEy", "OldValue", "NewValue");
        // then
        assertTrue(replaced, "replace should return true when old value matches");
        assertEquals("NewValue", stringMap.get("KEY"), "Value should be updated for case‑insensitive key");
    }

    @Test
    void replaceExistingKeyWithMismatchingOldValue_ShouldReturnFalseAndLeaveValueUnchanged() {
        // given
        stringMap.put("Key", "Original");
        // when
        boolean replaced = stringMap.replace("KEY", "WrongOld", "NewValue");
        // then
        assertFalse(replaced, "replace should return false when old value does not match");
        assertEquals("Original", stringMap.get("key"), "Original value must remain unchanged");
    }

    @Test
    void replaceNonExistingKey_ShouldReturnFalseAndLeaveMapUnchanged() {
        // given – map is empty
        // when
        boolean replaced = stringMap.replace("Missing", "any", "new");
        // then
        assertFalse(replaced, "replace should return false when key is absent");
        assertNull(stringMap.get("Missing"), "No entry should be created for a failed replace");
    }

    @Test
    void replaceWithNullKey_ShouldBehaveLikeUnderlyingMap() {
        // given
        stringMap.put(null, "nullValue");
        // when
        boolean replaced = stringMap.replace(null, "nullValue", "newNullValue");
        // then
        assertTrue(replaced, "replace should succeed for a present null key");
        assertEquals("newNullValue", stringMap.get(null), "Value for null key should be updated");
    }

    @Test
    void replaceWithNonStringKey_ShouldDelegateToBackingMap() {
        // given
        intMap.put(42, "Answer");
        // when
        boolean replaced = intMap.replace(42, "Answer", "Life");
        // then
        assertTrue(replaced, "replace should succeed for non‑String key when old value matches");
        assertEquals("Life", intMap.get(42), "Value should be updated for the integer key");
    }

    @Test
    void replaceWithNonStringKeyAndMismatchingOldValue_ShouldReturnFalse() {
        // given
        intMap.put(7, "Seven");
        // when
        boolean replaced = intMap.replace(7, "Wrong", "SevenAgain");
        // then
        assertFalse(replaced, "replace should return false when old value does not match for non‑String key");
        assertEquals("Seven", intMap.get(7), "Original value must remain unchanged");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicSuccessFailureProvider")
    void replace_BasicSuccessAndFailure(ReplaceCase rc) {
        // arrange
        // ensure the key is present with the expected initial value
        stringMap.put("Apple", "red");
        // act
        boolean result = stringMap.replace((String) rc.key(), (String) rc.oldValue(), (String) rc.newValue());
        // assert
        assertEquals(rc.expected(), result, rc.description());
        // verify final state only when replacement succeeded
        if (Boolean.TRUE.equals(rc.expected())) {
            assertEquals(rc.newValue(), stringMap.get("Apple"));
        } else {
            assertEquals("red", stringMap.get("Apple"));
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("caseInsensitivityProvider")
    void replace_CaseInsensitivity(ReplaceCase rc) {
        // first call may need an initial entry
        if (!stringMap.containsKey("Apple")) {
            stringMap.put("Apple", "red");
        }
        boolean result = stringMap.replace((String) rc.key(), (String) rc.oldValue(), (String) rc.newValue());
        assertEquals(rc.expected(), result, rc.description());
        assertEquals(rc.newValue(), stringMap.get("Apple"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("keyAbsentProvider")
    void replace_KeyAbsent(ReplaceCase rc) {
        boolean result = stringMap.replace((String) rc.key(), (String) rc.oldValue(), (String) rc.newValue());
        assertEquals(rc.expected(), result, rc.description());
        assertNull(stringMap.get(rc.key()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullOldValueProvider")
    void replace_NullOldValue(ReplaceCase rc) {
        stringMap.put("Apple", "red");
        boolean result = stringMap.replace((String) rc.key(), (String) rc.oldValue(), (String) rc.newValue());
        assertEquals(rc.expected(), result, rc.description());
        assertEquals("red", stringMap.get("Apple"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullNewValueProvider")
    void replace_NullNewValue(ReplaceCase rc) {
        stringMap.put("Apple", "red");
        boolean result = stringMap.replace((String) rc.key(), (String) rc.oldValue(), (String) rc.newValue());
        assertEquals(rc.expected(), result, rc.description());
        assertNull(stringMap.get("Apple"));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nonStringKeyProvider")
    void replace_NonStringKey(ReplaceCase rc) {
        // Cast to raw types because the map is typed with Integer keys
        @SuppressWarnings("unchecked")
        CaseInsensitiveMap<Integer, String> map = (CaseInsensitiveMap<Integer, String>) intMap;
        map.put((Integer) rc.key(), (String) rc.oldValue());
        boolean result = map.replace((Integer) rc.key(), (String) rc.oldValue(), (String) rc.newValue());
        assertEquals(rc.expected(), result, rc.description());
        assertEquals(rc.newValue(), map.get(rc.key()));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("emptyMapProvider")
    void replace_EmptyMap(ReplaceCase rc) {
        boolean result = stringMap.replace((String) rc.key(), (String) rc.oldValue(), (String) rc.newValue());
        assertEquals(rc.expected(), result, rc.description());
        assertTrue(stringMap.isEmpty(), "Map must stay empty after failed replace");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullKeyProvider")
    void replace_NullKeyThrowsException(ReplaceCase rc) {
        // arrange – map may already contain a null key, but we are testing the NPE from normalizeKey
        // act & assert
        assertThrows((Class<? extends Throwable>) rc.expected(), () -> stringMap.replace((String) rc.key(), (String) rc.oldValue(), (String) rc.newValue()), rc.description());
    }

    /**
     * Test that an UnsupportedOperationException from the backing map propagates.
     */
    @Test
    void replace_UnsupportedOperationOnBackingMap_ShouldPropagateException() {
        // Create a normal map and then replace its internal backing map with a Mockito mock.
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        @SuppressWarnings("unchecked")
        Map<String, String> mockBacking = mock(Map.class);
        // Configure the mock to throw when replace is called.
        when(mockBacking.replace(anyString(), anyString(), anyString())).thenThrow(new UnsupportedOperationException("replace not supported"));
        // Use reflection to inject the mock into the private 'map' field.
        try {
            Field field = CaseInsensitiveMap.class.getDeclaredField("map");
            field.setAccessible(true);
            field.set(map, mockBacking);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inject mock backing map via reflection", e);
        }
        // The call should propagate the UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> map.replace("Apple", "red", "green"), "replace must propagate UnsupportedOperationException from the backing map");
    }
}
