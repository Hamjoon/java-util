package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

// @ExtendWith(MockitoExtension.class)
// allows non‑static @MethodSource methods
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CompactMap_isEmpty_Test {

    private CompactMap<String, Object> map;

    @BeforeEach
    void setupBeforeEach() {
        // create a fresh map for every test – this guarantees that the map is never null
        map = new CompactMap<>();
        // just in case the constructor gave us a non‑empty sentinel
        map.clear();
    }

    @AfterEach
    void teardownAfterEach() {
        // clear the map to guarantee isolation between tests
        map.clear();
    }

    private static Stream<Arguments> putAllOperationProvider() {
        return Stream.of(// Empty source map – should stay empty.
        Arguments.arguments("empty source map", Collections.emptyMap(), true), // Non‑empty source map – map becomes populated.
        Arguments.arguments("non‑empty source map", new HashMap<String, Object>() {

            {
                put("a", 1);
                put("b", 2);
            }
        }, false));
    }

    /**
     * Scenarios for remove‑operations.
     */
//     private static Stream<Arguments> removeOperationProvider() {
//         return Stream.of(// 1. Removing from an empty map – sentinel stays unchanged.
//         Arguments.arguments("remove on an empty CompactMap", (Runnable) () -> {
//             // map is freshly created and empty; just call remove.
//             // The lambda runs against the test instance's map field.
//         }, true), // 2. Insert a single entry, then remove it – map should revert to empty sentinel.
//         Arguments.arguments("insert one entry then remove it", (Runnable) () -> {
//             map.put("key", "value");
//             map.remove("key");
//         }, true), // 3. Insert two entries, remove only one – map remains non‑empty.
//         Arguments.arguments("insert two entries then remove one", (Runnable) () -> {
//             map.put("k1", "v1");
//             map.put("k2", "v2");
//             map.remove("k1");
//         }, false));
//     }

    @Test
    void testIsEmptyOnNewMap() {
        assertTrue(map.isEmpty(), "A newly created CompactMap should be empty");
        assertEquals(0, map.size(), "Size of a new CompactMap must be 0");
    }

    @Test
    void testIsEmptyAfterPutAndRemove() {
        // Insert a single entry – the map should no longer be empty.
        assertNull(map.put("value", 10.0), "First insertion should return null");
        assertFalse(map.isEmpty(), "Map should not be empty after a put");
        assertEquals(1, map.size(), "Size should be 1 after one insertion");
        // Remove the entry – the map should become empty again.
        assertEquals(10.0, map.remove("value"), "Removed value should match inserted value");
        assertTrue(map.isEmpty(), "Map should be empty after removing the only entry");
        assertEquals(0, map.size(), "Size should be 0 after removal");
    }

    @Test
    void testIsEmptyAfterClear() {
        map.put("value", 1);
        map.put("alpha", "beta");
        assertFalse(map.isEmpty(), "Map should not be empty after adding entries");
        map.clear();
        assertTrue(map.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, map.size(), "Size should be 0 after clear()");
    }

    /**
     * Parameterized test covering three scenarios:
     * <ul>
     *   <li>remove on an already empty map</li>
     *   <li>remove the sole entry</li>
     *   <li>remove one of two entries</li>
     * </ul>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("removeOperationProvider")
    void testIsEmptyAfterRemoveOperation(String description, Runnable setup, boolean expectedEmpty) {
        // Perform the custom setup (populate map, call remove, etc.)
        setup.run();
        // Verify the expectation.
        assertEquals(expectedEmpty, map.isEmpty(), "After operation: " + description + " – isEmpty() should be " + expectedEmpty);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("putAllOperationProvider")
    void testIsEmptyAfterPutAll(String description, Map<String, Object> source, boolean expectedEmpty) {
        map.putAll(source);
        assertEquals(expectedEmpty, map.isEmpty(), "After putAll with " + description + " – isEmpty() should be " + expectedEmpty);
    }

    @Test
    void testClearOnAlreadyEmptyMap() {
        // The map is fresh and empty; calling clear should not change state.
        map.clear();
        assertTrue(map.isEmpty(), "Calling clear() on an already empty CompactMap must keep it empty");
    }

    @Test
    void testIsEmptyAfterNullKeyAndValue() {
        // Both key and value are null – this still creates an internal map.
        map.put(null, null);
        assertFalse(map.isEmpty(), "Putting a null key/value pair must make the map non‑empty");
        assertEquals(1, map.size(), "Size should be 1 after inserting null key/value");
    }

    @Test
    void testIsEmptyAfterOverwritingExistingKey() {
        map.put("dup", "first");
        // Overwrite the same key with a new value.
        map.put("dup", "second");
        assertFalse(map.isEmpty(), "Overwriting an existing key must not revert the map to empty");
        assertEquals(1, map.size(), "Size should remain 1 after overwriting the key");
        assertEquals("second", map.get("dup"), "Value should be updated to the latest put");
    }
}