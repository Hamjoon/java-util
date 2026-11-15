package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Collection;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Set;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactSet_add_Test {

    /**
     * Helper subclass that allows us to control the {@code compactSize}.
     */
    private static final class TestCompactSet<E> extends CompactSet<E> {

        private final int threshold;

        TestCompactSet(int threshold) {
            this.threshold = threshold;
        }

        @Override
        protected int compactSize() {
            return threshold;
        }
    }

    /**
     * Utility to read the private {@code val} field of {@link CompactSet}.
     */
    private static Object getInternalVal(CompactSet<?> set) throws Exception {
        Field f = CompactSet.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(set);
    }

    private static Stream<Arguments> provideAddAfterSwitchCases() {
        return Stream.of(// Adding a new element after transition
        Arguments.of("fourth", true, 4, true), // Adding a duplicate element after transition
        Arguments.of("second", false, 3, true));
    }

    private static Stream<Arguments> provideAddNullCases() {
        return Stream.of(// First null addition succeeds
        Arguments.of(true, true, 1), // Second null addition (duplicate) fails
        Arguments.of(false, false, 1));
    }

    @Test
    void addToEmptySet_returnsTrueAndStoresElement() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add("first"), "First add must return true");
        assertEquals(1, set.size(), "Size must be 1 after first add");
        assertTrue(set.contains("first"), "Set must contain the added element");
        // Verify internal representation is an Object[] of length 1 containing "first"
        Object internal = getInternalVal(set);
        assertTrue(internal instanceof Object[], "Internal storage should be an Object[]");
        Object[] arr = (Object[]) internal;
        assertEquals(1, arr.length, "Array length must be 1");
        assertEquals("first", arr[0], "Array element must be the added value");
    }

    @Test
    void addDuplicateElement_returnsFalseAndDoesNotChangeSize() {
        CompactSet<Integer> set = new CompactSet<>();
        assertTrue(set.add(42));
        assertFalse(set.add(42), "Adding a duplicate should return false");
        assertEquals(1, set.size(), "Size must stay unchanged after duplicate add");
    }

    @Test
    void addNullElement_isSupported() {
        CompactSet<Object> set = new CompactSet<>();
        assertTrue(set.add(null), "First null add must succeed");
        assertTrue(set.contains(null), "Set must contain null after addition");
        assertEquals(1, set.size());
        // Adding null again should be considered a duplicate
        assertFalse(set.add(null), "Adding null a second time must return false");
        assertEquals(1, set.size(), "Size must remain 1 after duplicate null add");
    }

    @Test
    void transitionFromArrayToSetOccursWhenSizeExceedsCompactSize() {
        // Use a tiny compactSize (2) so the transition happens after the third add.
        TestCompactSet<Integer> set = new TestCompactSet<>(2);
        // First two adds stay in the internal array representation.
        assertTrue(set.add(1));
        assertTrue(set.add(2));
        assertEquals(2, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        // Third add triggers the switch to a Set.
        assertTrue(set.add(3), "Third add should succeed and cause transition");
        assertEquals(3, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertTrue(set.contains(3));
        // After transition, adding a duplicate must still return false.
        assertFalse(set.add(2), "Duplicate after transition must return false");
        assertEquals(3, set.size());
        // Adding a new element after transition must succeed.
        assertTrue(set.add(4));
        assertEquals(4, set.size());
        assertTrue(set.contains(4));
    }

    /* --------------------------------------------------------------------- *
     *  Group: add(E) - adding to array representation before compactSize
     * --------------------------------------------------------------------- */
    @Test
    void addSecondElement_beforeCompactSize_expandsArray() throws Exception {
        // Use a large compactSize so we stay in array mode.
        TestCompactSet<String> set = new TestCompactSet<>(10);
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        assertEquals(2, set.size(), "Size must be 2 after two distinct adds");
        assertTrue(set.contains("first"));
        assertTrue(set.contains("second"));
        // Verify internal storage is still an Object[] of length 2
        Object internal = getInternalVal(set);
        assertTrue(internal instanceof Object[], "Internal storage should still be an Object[]");
        Object[] arr = (Object[]) internal;
        assertEquals(2, arr.length, "Array length must be 2");
        // Order is not guaranteed for contains, but the implementation puts new entry at the end.
        assertEquals("first", arr[0]);
        assertEquals("second", arr[1]);
    }

    /* --------------------------------------------------------------------- *
     *  Group: add(E) - duplicate in array representation
     * --------------------------------------------------------------------- */
    @Test
    void addDuplicateInArray_returnsFalseAndLeavesArrayUnchanged() throws Exception {
        TestCompactSet<String> set = new TestCompactSet<>(10);
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        // Attempt duplicate
        assertFalse(set.add("first"), "Duplicate add must return false");
        assertEquals(2, set.size(), "Size must stay 2 after duplicate attempt");
        // Verify internal array unchanged (still length 2, same contents)
        Object internal = getInternalVal(set);
        assertTrue(internal instanceof Object[], "Internal storage should be an Object[]");
        Object[] arr = (Object[]) internal;
        assertEquals(2, arr.length);
        assertEquals("first", arr[0]);
        assertEquals("second", arr[1]);
    }

    /* --------------------------------------------------------------------- *
     *  Group: add(E) - switch from array to Set when reaching compactSize
     * --------------------------------------------------------------------- */
    @Test
    void addThirdElement_triggersSwitchToSet() throws Exception {
        // compactSize = 2, so after two elements the next add switches to Set
        TestCompactSet<String> set = new TestCompactSet<>(2);
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        // At this point we are still in array mode
        Object before = getInternalVal(set);
        assertTrue(before instanceof Object[], "Before third add we should have an Object[]");
        // Third distinct element triggers transition
        assertTrue(set.add("third"));
        assertEquals(3, set.size(), "Size must be 3 after transition");
        assertTrue(set.contains("first"));
        assertTrue(set.contains("second"));
        assertTrue(set.contains("third"));
        // Verify internal storage is now a Set
        Object after = getInternalVal(set);
        assertTrue(after instanceof Set, "After transition internal storage should be a Set");
        @SuppressWarnings("unchecked")
        Set<String> internalSet = (Set<String>) after;
        assertTrue(internalSet.containsAll(Set.of("first", "second", "third")));
    }

    /* --------------------------------------------------------------------- *
     *  Group: add(E) - duplicate after switch to Set
     * --------------------------------------------------------------------- */
    @Test
    void addDuplicateAfterSwitch_returnsFalseAndSetUnchanged() throws Exception {
        TestCompactSet<String> set = new TestCompactSet<>(2);
        // Fill to cause transition
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        // triggers switch
        assertTrue(set.add("third"));
        // Duplicate after switch
        assertFalse(set.add("second"), "Duplicate after switch must return false");
        assertEquals(3, set.size(), "Size must stay 3 after duplicate attempt");
        // Verify internal storage is still a Set and unchanged
        Object internal = getInternalVal(set);
        assertTrue(internal instanceof Set, "Internal storage should remain a Set");
        @SuppressWarnings("unchecked")
        Set<String> internalSet = (Set<String>) internal;
        assertEquals(3, internalSet.size());
        assertTrue(internalSet.containsAll(Set.of("first", "second", "third")));
    }

    /* --------------------------------------------------------------------- *
     *  Group: add(E) - adding to Set after switch (multiple cases)
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: add {0} => expectedResult={1}")
    @MethodSource("provideAddAfterSwitchCases")
    void addAfterSwitchToSet_cases(String item, boolean expectedResult, int expectedSize, boolean expectInternalSet) throws Exception {
        TestCompactSet<String> set = new TestCompactSet<>(2);
        // Prepare a set that has already switched to Set (size 3)
        assertTrue(set.add("first"));
        assertTrue(set.add("second"));
        // transition occurs here
        assertTrue(set.add("third"));
        // Perform the test add
        boolean result = set.add(item);
        assertEquals(expectedResult, result, "add() return value mismatch");
        assertEquals(expectedSize, set.size(), "Size after add mismatch");
        assertTrue(set.contains(item) == expectedResult || set.contains(item), "contains() should reflect presence");
        // Verify internal representation stays a Set
        Object internal = getInternalVal(set);
        if (expectInternalSet) {
            assertTrue(internal instanceof Set, "Internal storage must remain a Set");
        } else {
            fail("Internal representation expectation not met");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Group: add(E) - adding null element (multiple cases)
     * --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: add null ({0}) => expectedResult={1}")
    @MethodSource("provideAddNullCases")
    void addNullElement_cases(boolean firstAdd, boolean expectedResult, int expectedSize) throws Exception {
        CompactSet<Object> set = new CompactSet<>();
        if (!firstAdd) {
            // Pre‑populate with a null element so the first call is a duplicate
            assertTrue(set.add(null));
        }
        boolean result = set.add(null);
        assertEquals(expectedResult, result, "add(null) return value mismatch");
        assertEquals(expectedSize, set.size(), "Size after adding null mismatch");
        assertTrue(set.contains(null), "Set must contain null after operation");
        // Verify internal representation holds null correctly
        Object internal = getInternalVal(set);
        if (internal instanceof Object[]) {
            Object[] arr = (Object[]) internal;
            assertTrue(arr.length >= 1);
            // At least one entry must be null
            boolean foundNull = false;
            for (Object o : arr) {
                if (o == null) {
                    foundNull = true;
                    break;
                }
            }
            assertTrue(foundNull, "Array representation must contain null");
        } else if (internal instanceof Set) {
            @SuppressWarnings("unchecked")
            Set<Object> s = (Set<Object>) internal;
            assertTrue(s.contains(null), "Set representation must contain null");
        } else {
            fail("Unexpected internal representation");
        }
    }

    /* --------------------------------------------------------------------- *
     *  Group: add(E) - add after clear resets to empty
     * --------------------------------------------------------------------- */
    @Test
    void addAfterClear_resetsToEmptyAndStoresElement() throws Exception {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add("alpha"));
        assertEquals(1, set.size());
        set.clear();
        assertEquals(0, set.size(), "Size must be 0 after clear");
        // After clear, internal representation should be the sentinel EMPTY_SET (not directly accessible,
        // but we can verify that a subsequent add behaves like the first add on an empty set)
        assertTrue(set.add("reset"));
        assertEquals(1, set.size());
        assertTrue(set.contains("reset"));
        // Verify internal storage is an Object[] of length 1 containing "reset"
        Object internal = getInternalVal(set);
        assertTrue(internal instanceof Object[], "After clear and add, internal storage should be an Object[]");
        Object[] arr = (Object[]) internal;
        assertEquals(1, arr.length);
        assertEquals("reset", arr[0]);
    }
}
