package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactSet_clear_Test {

    /**
     * Instance used by most tests
     */
    private CompactSet<String> stringSet;

    /**
     * Instance used for generic empty‑set test
     */
    private CompactSet<Integer> intSet;

//     @BeforeEach
    void setupBeforeEach() {
        // Create a set with a few elements – this will be the subject of the clear() tests
        stringSet = new CompactSet<>();
        stringSet.add("alpha");
        stringSet.add("beta");
        stringSet.add("gamma");
        // Separate empty set instance for the “clear on empty” scenario
        intSet = new CompactSet<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the test clean
        stringSet = null;
        intSet = null;
    }

    /* ----------------------------------------------------------------------
     * Helper method to replace the private 'map' field of a CompactSet instance.
     * ---------------------------------------------------------------------- */
    private static void injectMap(CompactSet<?> set, CompactMap<?, ?> newMap) throws Exception {
        Field mapField = CompactSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, newMap);
    }

    /**
     * Verify that clear() removes all elements and leaves the set in a usable empty state.
     */
//     @Test
//     void testClearRemovesAllElements() {
//         // given – set already contains three elements (setupBeforeEach)
//         // when
//         stringSet.clear();
//         // then
//         assertTrue("Set should be empty after clear()", stringSet.isEmpty());
//         assertEquals("Size should be 0 after clear()", 0, stringSet.size());
//         assertFalse(stringSet.contains("alpha"));
//         assertFalse(stringSet.contains("beta"));
//         assertFalse(stringSet.contains("gamma"));
//     }

    /**
     * Verify that calling clear() on an already empty set does not cause errors or state changes.
     */
//     @Test
//     void testClearOnEmptySet() {
//         // given – intSet is empty (setupBeforeEach)
//         // when
//         intSet.clear();
//         // then
//         assertTrue("Empty set should remain empty after clear()", intSet.isEmpty());
//         assertEquals("Size should remain 0 after clear()", 0, intSet.size());
//     }

    /**
     * Verify that after clear() the iterator reports no elements.
     */
//     @Test
//     void testIteratorAfterClear() {
//         // given – stringSet has elements (setupBeforeEach)
//         // when
//         stringSet.clear();
//         // then
//         assertTrue("Set should be empty after clear()", stringSet.isEmpty());
//         Iterator<String> iterator = stringSet.iterator();
//         assertFalse("Iterator should have no elements after clear()", iterator.hasNext());
//     }

    /**
     * Clear after a partial removal – ensure remaining elements are also removed.
     */
//     @Test
//     void testClearAfterPartialRemoval() {
//         // given: remove one element
//         assertTrue("Pre‑condition: element \"beta\" must be removable", stringSet.remove("beta"));
//         assertEquals(2, stringSet.size());
//         // when
//         stringSet.clear();
//         // then
//         assertTrue("Set should be empty after clear()", stringSet.isEmpty());
//         assertEquals(0, stringSet.size());
//         assertFalse(stringSet.contains("alpha"));
//         assertFalse(stringSet.contains("beta"));
//         assertFalse(stringSet.contains("gamma"));
//     }

    /**
     * Clear on a case‑insensitive CompactSet – configuration should not affect clearing.
     */
//     @Test
//     void testClearOnCaseInsensitiveSet() {
//         // Build a case‑insensitive set via the builder API
//         CompactSet<String> ciSet = CompactSet.<String>builder().caseSensitive(false).build();
//         // add elements with varying case
//         ciSet.add("One");
//         ciSet.add("two");
//         ciSet.add("THREE");
//         // sanity checks before clear
//         assertTrue(ciSet.contains("one"));
//         assertTrue(ciSet.contains("TWO"));
//         assertTrue(ciSet.contains("three"));
//         // when
//         ciSet.clear();
//         // then
//         assertTrue("Case‑insensitive set should be empty after clear()", ciSet.isEmpty());
//         assertEquals(0, ciSet.size());
//         assertFalse(ciSet.contains("one"));
//     }

    /**
     * Verify that a set which previously contained a null element becomes empty after clear().
     */
//     @Test
//     void testClearWithNullElement() {
//         CompactSet<String> nullableSet = new CompactSet<>();
//         // Add a null element – CompactSet permits null according to Set contract
//         nullableSet.add(null);
//         assertTrue("Set should contain null before clear()", nullableSet.contains(null));
//         // when
//         nullableSet.clear();
//         // then
//         assertTrue("Set should be empty after clear()", nullableSet.isEmpty());
//         assertEquals(0, nullableSet.size());
//         assertFalse("Null element should be removed after clear()", nullableSet.contains(null));
//     }

    /**
     * After clear(), the set should behave like a fresh instance – adding new elements works.
     */
//     @Test
//     void testOperationsAfterClear() {
//         // when
//         stringSet.clear();
//         // then set behaves like new
//         assertTrue(stringSet.isEmpty());
//         // add a new element
//         assertTrue(stringSet.add("delta"));
//         assertEquals(1, stringSet.size());
//         assertTrue(stringSet.contains("delta"));
//     }

    /**
     * Verify that a NullPointerException is thrown when the internal map reference is null.
     */
//     @Test
//     void testClearWhenInternalMapIsNull() throws Exception {
//         // Set the private 'map' field to null via reflection
//         Field mapField = CompactSet.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(stringSet, null);
//         // Expect NPE because clear() tries to invoke map.clear()
//         try {
//             stringSet.clear();
//             fail("Expected NullPointerException when internal map is null");
//         } catch (NullPointerException expected) {
//             // success
//         }
//     }
}