package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_merge_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intKeyMap;

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure the default cache length is restored for the whole test suite.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instances for every test to avoid cross‑test contamination.
        stringMap = new CaseInsensitiveMap<>();
        intKeyMap = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // No mutable static state is created in these tests, but we reset the cache
        // length in case a test changes it.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    private static Stream<Arguments> basicMergeProvider() {
        return Stream.of(// Insert when absent (remappingFunction is null → treated as default behavior)
        Arguments.of("Key does not exist – insert and return value", "Apple", "Red", (BiFunction<String, String, String>) null, "Red"), // Combine when present
        Arguments.of("Key exists – combine old and new values", "Apple", "Green", (oldVal, newVal) -> oldVal + "," + newVal, "Red,Green"));
    }

    @Test
    void mergeAddsNewEntryWhenKeyAbsent() {
        // GIVEN – an empty map
        assertTrue(stringMap.isEmpty());
        // WHEN – merge is called with a new key
        String result = stringMap.merge("Key", "Initial", (oldVal, newVal) -> oldVal + newVal);
        // THEN – the returned value is the supplied value and the entry exists
        assertEquals("Initial", result);
        // case‑insensitive lookup
        assertEquals("Initial", stringMap.get("key"));
        assertEquals(1, stringMap.size());
    }

    @Test
    void mergeCombinesValuesWhenKeyPresent() {
        // GIVEN – a map containing a key (different case)
        stringMap.put("TeSt", "First");
        assertEquals("First", stringMap.get("test"));
        // WHEN – merge is called with the same logical key but different case
        String combined = stringMap.merge("test", "Second", (oldVal, newVal) -> oldVal + "-" + newVal);
        // THEN – the remapping function is applied and the map reflects the new value
        assertEquals("First-Second", combined);
        assertEquals("First-Second", stringMap.get("TEST"));
        assertEquals(1, stringMap.size());
    }

    @Test
    void mergeUsesRemappingFunctionResultEvenIfNull() {
        // GIVEN – a map with an existing entry
        stringMap.put("Alpha", "A");
        assertEquals("A", stringMap.get("alpha"));
        // WHEN – merge returns null from the remapping function
        String result = stringMap.merge("ALPHA", "B", (oldVal, newVal) -> null);
        // THEN – the entry is removed from the map (standard Map.merge contract)
        assertNull(result);
        assertFalse(stringMap.containsKey("alpha"));
        assertTrue(stringMap.isEmpty());
    }

    @Test
    void mergeThrowsWhenRemappingFunctionIsNull() {
        // GIVEN – a map with a pre‑existing entry
        stringMap.put("X", "x");
        // WHEN / THEN – a NullPointerException is thrown as per Map.merge spec
        assertThrows(NullPointerException.class, () -> stringMap.merge("X", "y", null));
    }

    @Test
    void mergeWorksWithNonStringKey() {
        // GIVEN – a map that uses Integer keys (no case‑insensitivity needed)
        intKeyMap.put(42, "Answer");
        assertEquals("Answer", intKeyMap.get(42));
        // WHEN – merge on an existing integer key
        String result = intKeyMap.merge(42, "Plus", (oldVal, newVal) -> oldVal + newVal);
        // THEN – the value is updated correctly
        assertEquals("AnswerPlus", result);
        assertEquals("AnswerPlus", intKeyMap.get(42));
    }

    @Test
    void mergeAddsNonStringKeyWhenAbsent() {
        // GIVEN – an empty integer‑key map
        assertTrue(intKeyMap.isEmpty());
        // WHEN – merge on a key that does not exist
        String result = intKeyMap.merge(7, "Seven", // oldVal will be null
        (oldVal, newVal) -> oldVal + newVal);
        // THEN – the supplied value is stored
        assertEquals("Seven", result);
        assertEquals("Seven", intKeyMap.get(7));
        assertEquals(1, intKeyMap.size());
    }

    /**
     * Basic merge behavior **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicMergeProvider")
    void basicMergeBehavior(String description, String key, String value, BiFunction<String, String, String> remap, String expected) {
        // No prior entry – the map is empty.
        String result = stringMap.merge(key, value, remap);
        assertEquals(expected, result);
        assertEquals(expected, stringMap.get(key));
        assertEquals(1, stringMap.size());
    }

    /**
     * Case‑insensitivity handling **
     */
    @Test
    void caseInsensitivityInsertAndCombine() {
        // Insert with mixed case
        String r1 = stringMap.merge("Apple", "Red", null);
        assertEquals("Red", r1);
        assertEquals("Red", stringMap.get("apple"));
        // Combine with different case
        String r2 = stringMap.merge("apple", "Green", (o, n) -> o + "," + n);
        assertEquals("Red,Green", r2);
        assertEquals("Red,Green", stringMap.get("APPLE"));
        // Replace with upper‑case key using a remapping function that returns newVal
        String r3 = stringMap.merge("APPLE", "Yellow", (o, n) -> n);
        assertEquals("Yellow", r3);
        assertEquals("Yellow", stringMap.get("Apple"));
    }

    /**
     * Null key handling **
     */
    @Test
    void mergeWithNullKeyWhenAbsent() {
        // HashMap permits null keys – the entry should be added.
        String result = stringMap.merge(null, "NullValue", null);
        assertEquals("NullValue", result);
        assertEquals("NullValue", stringMap.get(null));
        assertEquals(1, stringMap.size());
    }

    @Test
    void mergeWithNullKeyWhenPresent() {
        // Pre‑populate the null key.
        stringMap.put(null, "NullValue");
        // Merge using a remapping function that concatenates.
        String result = stringMap.merge(null, "AnotherValue", (oldVal, newVal) -> oldVal + "+" + newVal);
        assertEquals("NullValue+AnotherValue", result);
        assertEquals("NullValue+AnotherValue", stringMap.get(null));
        assertEquals(1, stringMap.size());
    }

    /**
     * Null value handling **
     */
    @Test
    void mergeInsertWithNonNullValue() {
        String result = stringMap.merge("Banana", "Yellow", null);
        assertEquals("Yellow", result);
        assertEquals("Yellow", stringMap.get("Banana"));
    }

    @Test
    void mergeWithNullValueAndRemappingFunction() {
        // Insert baseline entry.
        stringMap.put("Banana", "Yellow");
        // Merge where the new value is null.
        String result = stringMap.merge("Banana", null, (oldVal, newVal) -> oldVal + "-null");
        assertEquals("Yellow-null", result);
        assertEquals("Yellow-null", stringMap.get("Banana"));
    }

    /**
     * Remapping function returns null (entry removal) **
     */
    @Test
    void remapReturnsNullRemovesEntry() {
        // Create entry that will later be removed.
        stringMap.put("Cherry", "Red");
        // Merge with a remapping function that returns null.
        String result = stringMap.merge("Cherry", "DarkRed", (o, n) -> null);
        assertNull(result);
        assertFalse(stringMap.containsKey("Cherry"));
        assertTrue(stringMap.isEmpty());
    }

    /**
     * Remapping function throws exception **
     */
    @Test
    void remapThrowsExceptionPropagatesAndLeavesMapUnchanged() {
        // Insert baseline entry.
        stringMap.put("Date", "Sweet");
        // Attempt merge with a remapping function that throws.
        RuntimeException ex = assertThrows(RuntimeException.class, () -> stringMap.merge("Date", "Dry", (oldVal, newVal) -> {
            throw new RuntimeException("boom");
        }));
        assertEquals("boom", ex.getMessage());
        // Verify map unchanged.
        assertEquals("Sweet", stringMap.get("Date"));
        assertEquals(1, stringMap.size());
    }

    /**
     * Non‑String key (no normalization) **
     */
    @Test
    void nonStringKeyInsertAndReplace() {
        // Insert an Integer key.
        intKeyMap.put(5, "Five");
        assertEquals("Five", intKeyMap.get(5));
        // Merge on the same Integer key, replacing the value.
        String result = intKeyMap.merge(5, "FivePlus", (oldV, newV) -> newV);
        assertEquals("FivePlus", result);
        assertEquals("FivePlus", intKeyMap.get(5));
    }

    /**
     * Remapping function null with existing key – should throw **
     */
    @Test
    void mergeNullRemappingFunctionWithExistingKeyThrows() {
        // Setup existing entry.
        stringMap.put("Mango", "Green");
        // Null remapping function must cause NPE according to Map.merge contract.
        assertThrows(NullPointerException.class, () -> stringMap.merge("Mango", "Yellow", null));
    }
}
