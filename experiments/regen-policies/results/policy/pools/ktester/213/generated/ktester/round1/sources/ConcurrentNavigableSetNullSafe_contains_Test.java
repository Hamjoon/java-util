package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_contains_Test {

    /**
     * Set used in most tests – created fresh before each test.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural‑ordering) constructor for a clean set.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared so that a failing test does not affect the next one.
        if (set != null) {
            set.clear();
        }
    }

    /* --------------------------------------------------------------------- *
     *  Basic contains checks with non‑null elements
     * --------------------------------------------------------------------- */
    @Test
    void containsShouldReturnTrueForAddedElements() {
        assertTrue(set.add("alpha"));
        assertTrue(set.add("beta"));
        assertTrue(set.add("gamma"));
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("beta"));
        assertTrue(set.contains("gamma"));
        assertFalse(set.contains("delta"));
    }

    /* --------------------------------------------------------------------- *
     *  Null handling – the class is designed to accept null values.
     * --------------------------------------------------------------------- */
    @Test
    void containsShouldHandleNullElement() {
        assertTrue(set.add(null));
        assertTrue(set.add("apple"));
        assertTrue(set.contains(null));
        assertTrue(set.contains("apple"));
        assertFalse(set.contains("banana"));
    }

    /* --------------------------------------------------------------------- *
     *  Removal should affect subsequent contains calls.
     * --------------------------------------------------------------------- */
    @Test
    void containsReflectsRemoval() {
        set.addAll(Arrays.asList("one", "two", "three", null));
        assertTrue(set.contains("two"));
        assertTrue(set.contains(null));
        assertTrue(set.remove("two"));
        assertFalse(set.contains("two"));
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
    }

    /* --------------------------------------------------------------------- *
     *  Constructor with a custom comparator (by string length).
     * --------------------------------------------------------------------- */
    @Test
    void containsWorksWithComparatorConstructor() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> lengthSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        assertTrue(lengthSet.add("a"));
        assertTrue(lengthSet.add("abcd"));
        // length 0
        assertTrue(lengthSet.add(null));
        assertTrue(lengthSet.add("abc"));
        assertTrue(lengthSet.contains("a"));
        assertTrue(lengthSet.contains("abcd"));
        assertTrue(lengthSet.contains(null));
        // not added
        assertFalse(lengthSet.contains("zzzz"));
    }

    /* --------------------------------------------------------------------- *
     *  Constructor that copies an existing collection.
     * --------------------------------------------------------------------- */
    @Test
    void containsWorksWithCollectionConstructor() {
        Collection<String> source = Arrays.asList("x", "y", null, "z");
        NavigableSet<String> copy = new ConcurrentNavigableSetNullSafe<>(source);
        assertEquals(4, copy.size());
        assertTrue(copy.containsAll(source));
        // Verify that a non‑present element is correctly reported as absent.
        assertFalse(copy.contains("nonexistent"));
    }

    /* --------------------------------------------------------------------- *
     *  Subset view that includes null bounds – contains should delegate correctly.
     * --------------------------------------------------------------------- */
    @Test
    void containsOnSubsetIncludingNull() {
        set.addAll(Arrays.asList("apple", "banana", "cherry", null));
        // Subset from "banana" (inclusive) to null (inclusive)
        NavigableSet<String> sub = set.subSet("banana", true, null, true);
        assertEquals(3, sub.size());
        assertTrue(sub.contains("banana"));
        assertTrue(sub.contains("cherry"));
        assertTrue(sub.contains(null));
        assertFalse(sub.contains("apple"));
    }

    /* --------------------------------------------------------------------- *
     *  Edge case – empty set should report false for any contains query.
     * --------------------------------------------------------------------- */
    @Test
    void containsOnEmptySet() {
        assertTrue(set.isEmpty());
        assertFalse(set.contains("anything"));
        assertFalse(set.contains(null));
    }

    /* --------------------------------------------------------------------- *
     *  Invalid type – passing an object of a different generic type should
     *  trigger a ClassCastException because of the unchecked cast inside
     *  {@code contains(Object)}.
     * --------------------------------------------------------------------- */
    @Test
    void containsInvalidTypeShouldThrowClassCastException() {
        // The set is declared as NavigableSet<String>. Passing an Integer forces the cast to fail.
        assertThrows(ClassCastException.class, () -> set.contains(12345));
    }

    /* --------------------------------------------------------------------- *
     *  Null handling – two scenarios driven by a parameterised test:
     *  1) set contains a null element → contains(null) == true
     *  2) set does NOT contain a null element → contains(null) == false
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: setContainsNull={0} => expected={1}")
    @CsvSource({ // null was added beforehand, expect true
    "true, true", // null not added, expect false
    "false, false" })
    void containsNullHandlingParameterized(boolean addNullFirst, boolean expected) {
        if (addNullFirst) {
            assertTrue(set.add(null));
        }
        assertEquals(expected, set.contains(null));
    }

    /* --------------------------------------------------------------------- *
     *  Internal set being {@code null}. By using reflection we replace the
     *  private {@code internalSet} field with {@code null} and verify that
     *  a NullPointerException bubbles up when {@code contains} tries to delegate.
     * --------------------------------------------------------------------- */
    @Test
    void containsWhenInternalSetIsNullShouldThrowNPE() throws Exception {
        // Obtain the private field and set it to null.
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        internalSetField.set(set, null);
        assertThrows(NullPointerException.class, () -> set.contains("sample"));
    }

    /* --------------------------------------------------------------------- *
     *  Underlying NavigableSet that does not permit {@code null} (e.g., TreeSet).
     *  We replace the internal set with a TreeSet, then call {@code contains(null)}.
     *  TreeSet throws NPE for null arguments, which should be propagated.
     * --------------------------------------------------------------------- */
    @Test
    void containsNullWhenUnderlyingSetDisallowsNullShouldThrowNPE() throws Exception {
        // Replace internalSet with a TreeSet (does not allow nulls).
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        @SuppressWarnings("unchecked")
        NavigableSet<Object> treeSet = new TreeSet<>();
        internalSetField.set(set, treeSet);
        // The set itself does not contain null, but we are testing the exception path.
        assertThrows(NullPointerException.class, () -> set.contains(null));
    }
}
