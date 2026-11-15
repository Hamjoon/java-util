package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.ConcurrentSkipListSet;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_clear_Test {

    private CaseInsensitiveSet<Object> set;

    class BadMap<K, V> extends AbstractMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public void clear() {
            throw new IllegalStateException("Deliberate exception from BadMap.clear()");
        }
    }

    class BadMap<K, V> extends AbstractMap<K, V> {

        @Override
        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }

        @Override
        public void clear() {
            throw new IllegalStateException("Deliberate exception from BadMap.clear()");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh set before each test
        set = new CaseInsensitiveSet<>();
        set.add("Alpha");
        set.add("beta");
        set.add(123);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear reference to help GC (not strictly required)
        set = null;
    }

    @Test
    void testClearRemovesAllElements() {
        // Verify initial state
        assertEquals(3, set.size(), "Set should contain three elements before clear");
        assertFalse(set.isEmpty(), "Set should not be empty before clear");
        // Invoke the method under test
        set.clear();
        // Verify post‑conditions
        assertEquals(0, set.size(), "Set size should be zero after clear");
        assertTrue(set.isEmpty(), "Set should be empty after clear");
        assertFalse(set.contains("Alpha"), "Cleared set must not contain previous elements");
        assertFalse(set.contains("beta"), "Cleared set must not contain previous elements");
        assertFalse(set.contains(123), "Cleared set must not contain previous elements");
    }

    @Test
    @DisplayName("clear() on an already empty set leaves it empty and throws no exception")
    void testClearOnEmptySet() {
        // Empty the set first
        set.clear();
        assertTrue(set.isEmpty());
        // Call clear again – should be a no‑op
        assertDoesNotThrow(() -> set.clear(), "Calling clear on an empty set must not throw");
        assertEquals(0, set.size(), "Size must remain zero after second clear");
    }

    @Test
    @DisplayName("subsequent add operations work normally after clear()")
    void testAddAfterClear() {
        set.clear();
        assertTrue(set.isEmpty());
        // Add new elements
        assertTrue(set.add("Gamma"));
        assertTrue(set.add(456));
        // Verify normal behaviour
        assertEquals(2, set.size());
        assertTrue(set.contains("Gamma"));
        assertTrue(set.contains(456));
    }

    @Test
    @DisplayName("iterator obtained after clear() reports no elements")
    void testIteratorAfterClear() {
        set.clear();
        Iterator<Object> it = set.iterator();
        assertFalse(it.hasNext(), "Iterator should have no elements after clear");
    }

    @Test
    @DisplayName("size() returns 0 after clear()")
    void testSizeAfterClear() {
        set.clear();
        assertEquals(0, set.size(), "size() must be zero after clear");
    }

    @Test
    @DisplayName("clear() removes a previously stored null element")
    void testClearRemovesNullElement() {
        // The set permits null – add it explicitly
        set.add(null);
        assertTrue(set.contains(null));
        set.clear();
        assertEquals(0, set.size());
        assertFalse(set.contains(null), "Set must not contain null after clear");
    }

    @Test
    @DisplayName("iterator obtained before clear() becomes fail‑fast")
    void testIteratorFailFastAfterClear() {
        // obtain before clear
        Iterator<Object> it = set.iterator();
        // modify the collection
        set.clear();
        // Any subsequent call to next() must throw CME
        assertThrows(ConcurrentModificationException.class, it::next, "Iterator should throw ConcurrentModificationException after clear");
    }

    @Test
    @DisplayName("multiple consecutive clear() calls are idempotent")
    void testRepeatedClearIdempotent() {
        for (int i = 0; i < 3; i++) {
            assertDoesNotThrow(() -> set.clear(), "clear() must not throw on repetition " + i);
            assertEquals(0, set.size(), "size must be zero after clear call " + i);
        }
    }

    @Test
    @DisplayName("clear() empties the set after addAll() with duplicate elements")
    void testClearAfterAddAllWithDuplicates() {
        List<String> dupes = Arrays.asList("x", "X", "x", "Y", "y");
        // addAll should treat "x"/"X" as same element (case‑insensitive)
        // start from a clean slate
        set.clear();
        set.addAll(dupes);
        assertTrue(set.contains("x"));
        assertTrue(set.contains("Y"));
        assertEquals(2, set.size(), "Duplicates should not increase size");
        set.clear();
        assertEquals(0, set.size());
        assertTrue(set.isEmpty());
    }

    @Test
    @DisplayName("clear() efficiently empties a set containing 10,000 distinct elements")
    void testClearLargeSet() {
        // start empty
        set.clear();
        for (int i = 0; i < 10_000; i++) {
            set.add(i);
        }
        assertEquals(10_000, set.size(), "Pre‑condition: set must contain 10,000 elements");
        set.clear();
        assertEquals(0, set.size(), "Size must be zero after clear");
        assertTrue(set.isEmpty(), "Set must be empty after clear");
        assertArrayEquals(new Object[0], set.toArray(), "toArray() should return an empty array");
    }

    @Test
    @DisplayName("clear() only affects the instance it is invoked on")
    void testClearDoesNotAffectOtherSet() {
        CaseInsensitiveSet<Object> otherSet = new CaseInsensitiveSet<>();
        otherSet.add("Alpha");
        otherSet.add("beta");
        otherSet.add(123);
        // Clear only the first set
        set.clear();
        // Verify otherSet unchanged
        assertEquals(3, otherSet.size());
        assertTrue(otherSet.contains("Alpha"));
        assertTrue(otherSet.contains("beta"));
        assertTrue(otherSet.contains(123));
    }

    @Test
    @DisplayName("clear() throws NullPointerException when internal map is null")
    void testClearWithNullMap() throws Exception {
        // Inject a null map via reflection
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        // Expect NPE when clear() tries to invoke map.clear()
        assertThrows(NullPointerException.class, () -> set.clear(), "clear() should propagate NullPointerException when internal map is null");
    }

    @Test
    @DisplayName("clear() propagates UnsupportedOperationException from an unmodifiable backing map")
    void testClearWithUnmodifiableMap() throws Exception {
        // Replace internal map with an unmodifiable map
        Map<Object, Object> unmodifiable = Collections.unmodifiableMap(new TreeMap<>());
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, unmodifiable);
        // clear() should throw UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> set.clear(), "clear() must propagate UnsupportedOperationException from unmodifiable map");
    }

    @Test
    @DisplayName("clear() propagates IllegalStateException from a custom map implementation")
    void testClearWithCustomMapThrowingException() throws Exception {
        // Custom map that throws IllegalStateException on clear()
        class BadMap<K, V> extends AbstractMap<K, V> {

            @Override
            public Set<Entry<K, V>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public void clear() {
                throw new IllegalStateException("Deliberate exception from BadMap.clear()");
            }
        }
        BadMap<Object, Object> badMap = new BadMap<>();
        // Populate the map so that the set believes it has elements
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, badMap);
        // clear() should let the IllegalStateException bubble up
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.clear(), "clear() must propagate IllegalStateException from custom map");
        assertEquals("Deliberate exception from BadMap.clear()", ex.getMessage());
    }
}
