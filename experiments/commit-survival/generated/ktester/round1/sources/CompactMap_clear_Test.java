package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.Collection;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;

@ExtendWith(MockitoExtension.class)
class CompactMap_clear_Test {

    private CompactMap<String, Integer> mapStringInt;

    private CompactMap<Integer, String> mapIntString;

    /**
     * Functional interface mirroring JUnit's {@code Executable} to avoid a
     * direct dependency on JUnit's internal class in the helper method.
     */
    @FunctionalInterface
    private interface Executable {

        void execute() throws Throwable;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create fresh map instances for each test
        mapStringInt = new CompactMap<>();
        mapIntString = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references (helps GC in large test suites)
        mapStringInt = null;
        mapIntString = null;
    }

    /**
     * Executes the supplied {@code executable} and asserts that it throws
     * one of the {@code expectedExceptions}.  If no exception or an
     * unexpected type is thrown, the assertion fails.
     */
    private static void assertThrowsAnyOf(Class<? extends Throwable>[] expectedExceptions, Executable executable, String message) {
        try {
            executable.execute();
            fail(message + " (no exception thrown)");
        } catch (Throwable actual) {
            for (Class<? extends Throwable> expected : expectedExceptions) {
                if (expected.isInstance(actual)) {
                    // Expected exception – test passes
                    return;
                }
            }
            fail(message + " (unexpected exception: " + actual + ")");
        }
    }

    @Test
    @DisplayName("clear() on an empty CompactMap leaves it empty")
    void testClearOnEmptyMap() {
        assertTrue(mapStringInt.isEmpty(), "Map should start empty");
        assertEquals(0, mapStringInt.size(), "Initial size must be 0");
        mapStringInt.clear();
        assertTrue(mapStringInt.isEmpty(), "Map should still be empty after clear()");
        assertEquals(0, mapStringInt.size(), "Size must remain 0 after clear()");
        assertFalse(mapStringInt.containsKey("any"), "No key should be present after clear()");
    }

    @Test
    @DisplayName("clear() removes all entries after several puts")
    void testClearAfterAddingEntries() {
        mapStringInt.put("one", 1);
        mapStringInt.put("two", 2);
        mapStringInt.put("three", 3);
        assertEquals(3, mapStringInt.size(), "Map should contain three entries before clear()");
        assertTrue(mapStringInt.containsKey("two"));
        assertTrue(mapStringInt.containsValue(3));
        mapStringInt.clear();
        assertTrue(mapStringInt.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, mapStringInt.size(), "Size must be 0 after clear()");
        assertFalse(mapStringInt.containsKey("one"));
        assertFalse(mapStringInt.containsValue(2));
    }

    @Test
    @DisplayName("keySet().clear() delegates to CompactMap.clear()")
    void testKeySetClearUsesMapClear() {
        mapStringInt.put("alpha", 10);
        mapStringInt.put("beta", 20);
        Set<String> ks = mapStringInt.keySet();
        assertEquals(2, ks.size(), "Key set size before clear should be 2");
        // should invoke CompactMap.this.clear()
        ks.clear();
        assertTrue(mapStringInt.isEmpty(), "Map must be empty after keySet().clear()");
        assertEquals(0, ks.size(), "Key set must be empty after clear()");
    }

    @Test
    @DisplayName("values().clear() delegates to CompactMap.clear()")
    void testValuesClearUsesMapClear() {
        mapIntString.put(1, "one");
        mapIntString.put(2, "two");
        mapIntString.put(3, "three");
        Collection<String> vals = mapIntString.values();
        assertEquals(3, vals.size(), "Values collection size before clear should be 3");
        // should invoke CompactMap.this.clear()
        vals.clear();
        assertTrue(mapIntString.isEmpty(), "Map must be empty after values().clear()");
        assertEquals(0, vals.size(), "Values collection must be empty after clear()");
    }

    @Test
    @DisplayName("clear() after a remove operation leaves map empty")
    void testClearAfterRemove() {
        mapStringInt.put("temp", 99);
        assertEquals(1, mapStringInt.size());
        mapStringInt.remove("temp");
        assertTrue(mapStringInt.isEmpty(), "Map should be empty after remove of the only entry");
        // Re‑populate, remove, then clear again to ensure clear works in that state
        mapStringInt.put("a", 1);
        mapStringInt.put("b", 2);
        mapStringInt.remove("a");
        assertEquals(1, mapStringInt.size());
        mapStringInt.clear();
        assertTrue(mapStringInt.isEmpty(), "Map must be empty after clear following a remove");
    }

    @Test
    @DisplayName("clear() on a populated map makes it reusable")
    void testClearThenReuseMap() {
        // Populate the map
        mapStringInt.put("x", 10);
        mapStringInt.put("y", 20);
        mapStringInt.put("z", 30);
        assertEquals(3, mapStringInt.size());
        // Clear it
        mapStringInt.clear();
        assertTrue(mapStringInt.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, mapStringInt.size());
        // Re‑use: put a new entry and verify return value is null (no previous mapping)
        Integer previous = mapStringInt.put("newKey", 99);
        assertNull(previous, "put() after clear should return null because there was no previous value");
        assertEquals(99, mapStringInt.get("newKey"));
        assertEquals(1, mapStringInt.size());
    }

    @ParameterizedTest(name = "clear() after inserting null key (nullKey={0})")
    @NullSource
    @DisplayName("clear() removes a null key")
    void testClearAfterNullKey(String nullKey) {
        // CompactMap allows null keys (the generic type is String, null is acceptable)
        mapStringInt.put(nullKey, 123);
        assertEquals(1, mapStringInt.size());
        assertTrue(mapStringInt.containsKey(nullKey));
        assertEquals(123, mapStringInt.get(nullKey));
        mapStringInt.clear();
        assertTrue(mapStringInt.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, mapStringInt.size());
        assertFalse(mapStringInt.containsKey(nullKey));
        assertNull(mapStringInt.get(nullKey));
    }

    @Test
    @DisplayName("clear() removes a null value")
    void testClearAfterNullValue() {
        mapStringInt.put("keyWithNull", null);
        assertEquals(1, mapStringInt.size());
        assertTrue(mapStringInt.containsKey("keyWithNull"));
        assertTrue(mapStringInt.containsValue(null));
        assertNull(mapStringInt.get("keyWithNull"));
        mapStringInt.clear();
        assertTrue(mapStringInt.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, mapStringInt.size());
        assertFalse(mapStringInt.containsKey("keyWithNull"));
        assertFalse(mapStringInt.containsValue(null));
    }

    @Test
    @DisplayName("clear() while iterating over entrySet")
    void testClearDuringIteration() {
        // Populate map with several entries
        mapStringInt.put("a", 1);
        mapStringInt.put("b", 2);
        mapStringInt.put("c", 3);
        Iterator<Map.Entry<String, Integer>> iterator = mapStringInt.entrySet().iterator();
        // Advance the iterator once to ensure it is not at the start
        assertTrue(iterator.hasNext());
        // consume first entry
        iterator.next();
        // Clear the map while the iterator is still alive
        mapStringInt.clear();
        // The iterator must now reflect the structural modification.
        // The contract of Map states that the iterator should either
        // report no further elements or throw a CME/NoSuchElementException.
        assertFalse(iterator.hasNext(), "Iterator should report no remaining elements after clear()");
        // The next() call is expected to throw either NoSuchElementException
        // (if the iterator simply reports end) or ConcurrentModificationException
        // (if it detects the modification).  Both behaviours are acceptable.
        assertThrowsAnyOf(new Class[] { NoSuchElementException.class, ConcurrentModificationException.class }, iterator::next, "Iterator.next() after clear() should throw NoSuchElementException or ConcurrentModificationException");
        // Finally, the map itself must be empty
        assertTrue(mapStringInt.isEmpty(), "Map must be empty after clear()");
        assertEquals(0, mapStringInt.size());
    }

    @Test
    @DisplayName("clear() after a singleton (single‑entry) CompactMap")
    void testClearAfterSingletonState() {
        // Insert exactly one entry – this may trigger the internal singleton optimisation
        mapStringInt.put("onlyKey", 42);
        assertEquals(1, mapStringInt.size());
        assertTrue(mapStringInt.containsKey("onlyKey"));
        assertEquals(42, mapStringInt.get("onlyKey"));
        // Clear the map
        mapStringInt.clear();
        // Verify that the map behaves like a fresh instance
        assertTrue(mapStringInt.isEmpty(), "Map should be empty after clear()");
        assertEquals(0, mapStringInt.size());
        assertFalse(mapStringInt.containsKey("onlyKey"));
        assertNull(mapStringInt.get("onlyKey"));
        // Ensure we can reuse the map after clearing the singleton state
        mapStringInt.put("newKey", 100);
        assertEquals(1, mapStringInt.size());
        assertEquals(100, mapStringInt.get("newKey"));
    }
}
