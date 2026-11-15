package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactSet_remove_Test {

    /**
     * A CompactSet that uses the default {@code compactSize()} (80).
     */
    private CompactSet<Integer> defaultSet;

    /**
     * A CompactSet with a very small {@code compactSize()} to force the Set‑backed representation quickly.
     */
    private CompactSet<Integer> tinyCompactSet;

    /**
     * A CompactSet that is case‑insensitive (uses a TreeSet with CASE_INSENSITIVE_ORDER).
     */
    private CompactSet<String> caseInsensitiveSet;

    /**
     * A CompactSet whose backing Set is unmodifiable – used to provoke {@link UnsupportedOperationException}.
     */
    private CompactSet<String> unmodifiableSet;

    /**
     * Cached reflective access to the private {@code val} field.
     */
    private static Field valField;

    /**
     * Cached reflective access to the private static {@code EMPTY_SET} sentinel.
     */
    private static String emptySentinel;

    @BeforeAll
    static void initReflection() throws Exception {
        valField = CompactSet.class.getDeclaredField("val");
        valField.setAccessible(true);
        Field emptyField = CompactSet.class.getDeclaredField("EMPTY_SET");
        emptyField.setAccessible(true);
        emptySentinel = (String) emptyField.get(null);
    }

    @BeforeEach
    void setupBeforeEach() {
        defaultSet = new CompactSet<>();
        // Subclass that overrides compactSize() to 3 so that after 4 elements the Set implementation is used.
        tinyCompactSet = new CompactSet<Integer>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Set<Integer> getNewSet() {
                return new HashSet<>();
            }
        };
        // Case‑insensitive variant – overrides isCaseInsensitive() and supplies a TreeSet with the proper comparator.
        caseInsensitiveSet = new CompactSet<String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            @Override
            protected Set<String> getNewSet() {
                return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            }
        };
        // Unmodifiable Set variant – we force the backing store to be an unmodifiable Set via reflection.
        unmodifiableSet = new CompactSet<String>() {

            @Override
            protected Set<String> getNewSet() {
                // Normal mutable set; we will replace it later with an unmodifiable wrapper.
                return new HashSet<>();
            }
        };
        // Populate with a single element and then replace the internal Set with an unmodifiable version.
        unmodifiableSet.add("A");
        try {
            Set<String> modifiable = (Set<String>) valField.get(unmodifiableSet);
            Set<String> unmod = Collections.unmodifiableSet(modifiable);
            valField.set(unmodifiableSet, unmod);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required.
        defaultSet = null;
        tinyCompactSet = null;
    }

    /**
     * Removing from an empty CompactSet must return {@code false}.
     */
    @Test
    void testRemoveFromEmptySet() {
        assertFalse(defaultSet.remove("any"));
        assertEquals(0, defaultSet.size());
        assertTrue(defaultSet.isEmpty());
    }

    /**
     * Removing the only element stored in the internal {@code Object[]} representation.
     */
    @Test
    void testRemoveSingleElementArray() {
        assertTrue(defaultSet.add(42));
        assertEquals(1, defaultSet.size());
        assertTrue(defaultSet.contains(42));
        // Remove the element – should succeed and leave the set empty.
        assertTrue(defaultSet.remove(42));
        assertEquals(0, defaultSet.size());
        assertFalse(defaultSet.contains(42));
        assertTrue(defaultSet.isEmpty());
        // Verify internal sentinel has been restored.
        Object internal = valField.get(defaultSet);
        assertEquals(emptySentinel, internal);
    }

    /**
     * Removing an element from a multi‑element {@code Object[]} representation.
     */
    @Test
    void testRemoveFromArrayMultipleElements() {
        defaultSet.add(1);
        defaultSet.add(2);
        defaultSet.add(3);
        assertEquals(3, defaultSet.size());
        assertTrue(defaultSet.contains(2));
        // Remove the middle element.
        assertTrue(defaultSet.remove(2));
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains(2));
        assertTrue(defaultSet.contains(1));
        assertTrue(defaultSet.contains(3));
    }

    /**
     * Removing a non‑existent element must return {@code false} and not alter the set.
     */
    @Test
    void testRemoveNonExistingElement() {
        defaultSet.add(10);
        defaultSet.add(20);
        assertEquals(2, defaultSet.size());
        // 30 was never added
        assertFalse(defaultSet.remove(30));
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.contains(10));
        assertTrue(defaultSet.contains(20));
    }

    /**
     * When the set grows beyond {@code compactSize()} it switches to a {@code Set} implementation.
     * Removing an element that brings the size back down to {@code compactSize()} should cause a
     * transition back to the compact {@code Object[]} representation.
     */
    @Test
    void testRemoveFromSetAndTransitionBackToArray() {
        // Fill the tinyCompactSet until it uses a Set internally (size > 3).
        tinyCompactSet.add(1);
        tinyCompactSet.add(2);
        tinyCompactSet.add(3);
        // triggers Set backing
        tinyCompactSet.add(4);
        assertTrue(tinyCompactSet.size() > 3);
        assertTrue(tinyCompactSet.contains(4));
        // Remove one element; size becomes 3 which equals compactSize().
        assertTrue(tinyCompactSet.remove(4));
        assertEquals(3, tinyCompactSet.size());
        assertFalse(tinyCompactSet.contains(4));
        // At this point the internal representation should have switched back to Object[].
        // We cannot inspect the private field directly, but we can verify behaviour:
        // Adding a new element should still work and size becomes 4 again (switches back to Set).
        assertTrue(tinyCompactSet.add(5));
        assertEquals(4, tinyCompactSet.size());
        assertTrue(tinyCompactSet.contains(5));
    }

    /**
     * Verify that removal works correctly when the internal representation is a Set (size > compactSize).
     */
    @Test
    void testRemoveFromSetWithoutTransition() {
        // Use the tinyCompactSet, add enough elements to stay in Set mode after removal.
        tinyCompactSet.add(1);
        tinyCompactSet.add(2);
        tinyCompactSet.add(3);
        // now Set-backed
        tinyCompactSet.add(4);
        // still Set-backed after removal
        tinyCompactSet.add(5);
        assertTrue(tinyCompactSet.remove(2));
        assertEquals(4, tinyCompactSet.size());
        assertFalse(tinyCompactSet.contains(2));
        // Ensure other elements are still present.
        assertTrue(tinyCompactSet.contains(1));
        assertTrue(tinyCompactSet.contains(3));
        assertTrue(tinyCompactSet.contains(4));
        assertTrue(tinyCompactSet.contains(5));
    }

    /**
     * Removing {@code null} from an array‑backed CompactSet when {@code null} is present.
     */
    @Test
    void testRemoveNullFromArrayPresent() throws Exception {
        defaultSet.add(null);
        defaultSet.add(1);
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.contains(null));
        assertTrue(defaultSet.remove(null));
        assertEquals(1, defaultSet.size());
        assertFalse(defaultSet.contains(null));
        assertTrue(defaultSet.contains(1));
        // Verify internal representation is now a single‑element array (not the sentinel)
        Object internal = valField.get(defaultSet);
        assertTrue(internal instanceof Object[]);
        Object[] arr = (Object[]) internal;
        assertEquals(1, arr.length);
        assertEquals(1, arr[0]);
    }

    /**
     * Removing {@code null} from an array‑backed CompactSet when {@code null} is absent must return false.
     */
    @Test
    void testRemoveNullFromArrayAbsent() {
        defaultSet.add(1);
        defaultSet.add(2);
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains(null));
        assertFalse(defaultSet.remove(null));
        assertEquals(2, defaultSet.size());
    }

    /**
     * Removing {@code null} from a Set‑backed CompactSet.
     */
    @Test
    void testRemoveNullFromSet() throws Exception {
        // Force Set backing using tinyCompactSet (compactSize = 3)
        tinyCompactSet.add(1);
        tinyCompactSet.add(2);
        tinyCompactSet.add(3);
        // triggers Set mode
        tinyCompactSet.add(null);
        assertTrue(tinyCompactSet.contains(null));
        assertTrue(tinyCompactSet.remove(null));
        assertFalse(tinyCompactSet.contains(null));
        assertEquals(3, tinyCompactSet.size());
        // Verify the internal representation is still a Set (size > compactSize? now size == 3 == compactSize)
        Object internal = valField.get(tinyCompactSet);
        assertTrue(internal instanceof Set);
    }

    /**
     * Removing the first element from a three‑element backing array – order must be preserved.
     */
    @Test
    void testRemoveFirstElementFromArray() throws Exception {
        defaultSet.add("A");
        defaultSet.add("B");
        defaultSet.add("C");
        assertEquals(3, defaultSet.size());
        assertTrue(defaultSet.remove("A"));
        assertEquals(2, defaultSet.size());
        Object internal = valField.get(defaultSet);
        assertTrue(internal instanceof Object[]);
        Object[] arr = (Object[]) internal;
        assertArrayEquals(new Object[] { "B", "C" }, arr);
    }

    /**
     * Removing the middle element from a three‑element backing array – order must be preserved.
     */
    @Test
    void testRemoveMiddleElementFromArray() throws Exception {
        defaultSet.add("A");
        defaultSet.add("B");
        defaultSet.add("C");
        assertTrue(defaultSet.remove("B"));
        assertEquals(2, defaultSet.size());
        Object internal = valField.get(defaultSet);
        Object[] arr = (Object[]) internal;
        assertArrayEquals(new Object[] { "A", "C" }, arr);
    }

    /**
     * Removing the last element from a three‑element backing array – order must be preserved.
     */
    @Test
    void testRemoveLastElementFromArray() throws Exception {
        defaultSet.add("A");
        defaultSet.add("B");
        defaultSet.add("C");
        assertTrue(defaultSet.remove("C"));
        assertEquals(2, defaultSet.size());
        Object internal = valField.get(defaultSet);
        Object[] arr = (Object[]) internal;
        assertArrayEquals(new Object[] { "A", "B" }, arr);
    }

    /**
     * Case‑insensitive removal – the backing set stays Set‑backed and removal succeeds.
     */
    @Test
    void testCaseInsensitiveRemoval() throws Exception {
        caseInsensitiveSet.add("test");
        assertTrue(caseInsensitiveSet.contains("test"));
        // contains works case‑insensitively but we check directly
        assertFalse(caseInsensitiveSet.contains("TEST"));
        assertTrue(caseInsensitiveSet.remove("TEST"));
        assertEquals(0, caseInsensitiveSet.size());
        assertFalse(caseInsensitiveSet.contains("test"));
        // Internal representation after removal should be the EMPTY_SET sentinel (since it is empty)
        Object internal = valField.get(caseInsensitiveSet);
        assertEquals(emptySentinel, internal);
    }

    /**
     * Removing from an unmodifiable Set should propagate UnsupportedOperationException.
     */
    @Test
    void testRemoveFromUnmodifiableSetThrows() {
        assertThrows(UnsupportedOperationException.class, () -> unmodifiableSet.remove("A"));
    }

    /**
     * Removing a non‑String from a case‑insensitive CompactSet backed by an Object[] triggers ClassCastException.
     */
    @Test
    void testRemoveTypeMismatchInCaseInsensitiveMode() throws Exception {
        // Force array backing with a single String element while keeping case‑insensitive flag true.
        caseInsensitiveSet = new CompactSet<String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            @Override
            protected Set<String> getNewSet() {
                return new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            }
        };
        // Directly set the internal val to a one‑element Object[]
        Object[] arr = new Object[] { "test" };
        valField.set(caseInsensitiveSet, arr);
        // Attempt to remove an Integer – should cause ClassCastException inside compareItems
        assertThrows(ClassCastException.class, () -> caseInsensitiveSet.remove(123));
    }
}
