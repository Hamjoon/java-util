package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.NavigableSet;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_remove_Test {

    /**
     * The set under test.
     */
    private NavigableSet<String> set;

    private static class RemoveCase {

        final String input;

        final boolean expected;

        final String description;

        RemoveCase(String input, boolean expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Create a fresh instance before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    /**
     * Clean up after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Ensure the set is cleared for the next test.
        set.clear();
    }

    private static Stream<RemoveCase> basicRemoveCases() {
        return Stream.of(new RemoveCase("apple", true, "Remove an element that is present in the set. The internal set initially contains \"apple\" and \"orange\"; after calling remove(\"apple\") the method should return true and the element should no longer be in the set."), new RemoveCase("banana", false, "Attempt to remove an element that is not present. The internal set contains \"apple\" and \"orange\" only; remove(\"banana\") should return false and leave the set unchanged."));
    }

    /**
     * Removing an existing element should return true and actually remove it.
     */
    @Test
    void removeExistingElement() {
        // given
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertEquals(3, set.size());
        // when
        boolean removed = set.remove("banana");
        // then
        assertTrue(removed, "remove should return true for an existing element");
        assertEquals(2, set.size(), "size should decrease after removal");
        assertFalse(set.contains("banana"), "set should no longer contain the removed element");
    }

    /**
     * Removing a null element should behave the same as any other element.
     */
    @Test
    void removeNullElement() {
        // given
        assertTrue(set.add(null));
        assertTrue(set.add("apple"));
        assertEquals(2, set.size());
        // when
        boolean removed = set.remove(null);
        // then
        assertTrue(removed, "remove should return true when null is present");
        assertEquals(1, set.size(), "size should decrease after removing null");
        assertFalse(set.contains(null), "set should no longer contain null");
    }

    /**
     * Removing an element that is not present should return false and leave the set unchanged.
     */
    @Test
    void removeNonExistingElement() {
        // given
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertEquals(2, set.size());
        // when
        boolean removed = set.remove("cherry");
        // then
        assertFalse(removed, "remove should return false for a missing element");
        assertEquals(2, set.size(), "size should remain unchanged when removal fails");
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("banana"));
    }

    /**
     * Removing from an empty set should return false.
     */
    @Test
    void removeFromEmptySet() {
        // given: set is empty
        assertTrue(set.isEmpty());
        // when
        boolean removed = set.remove("anything");
        // then
        assertFalse(removed, "remove should return false when the set is empty");
        assertTrue(set.isEmpty(), "set should remain empty");
    }

    /**
     * Removing the same element twice: first removal succeeds, second fails.
     */
    @Test
    void removeSameElementTwice() {
        // given
        assertTrue(set.add("duplicate"));
        assertEquals(1, set.size());
        // first removal
        assertTrue(set.remove("duplicate"), "first removal should succeed");
        assertEquals(0, set.size());
        // second removal
        assertFalse(set.remove("duplicate"), "second removal should fail because element is gone");
        assertEquals(0, set.size());
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicRemoveCases")
    void removeBasicParameterized(RemoveCase rc) {
        // initialise the set with the elements required by the description
        set.add("apple");
        set.add("orange");
        boolean result = set.remove(rc.input);
        assertEquals(rc.expected, result, "Unexpected result for input: " + rc.input);
        if (rc.expected) {
            assertFalse(set.contains(rc.input), "Element should have been removed");
        } else {
            assertTrue(set.contains("apple"));
            assertTrue(set.contains("orange"));
        }
    }

    @Test
    void removeNullWhenAbsent() {
        // given – set does NOT contain null
        assertTrue(set.add("apple"));
        assertEquals(1, set.size());
        // when
        boolean removed = set.remove(null);
        // then
        assertFalse(removed, "remove should return false when null is absent");
        assertEquals(1, set.size(), "size must stay unchanged");
        assertTrue(set.contains("apple"));
    }

    /**
     * Passing an object of an incompatible type should cause a {@link ClassCastException}
     * when the method attempts to cast the argument to the generic type {@code E}.
     */
    @Test
    void removeIncompatibleTypeThrowsClassCastException() {
        // set is a ConcurrentNavigableSetNullSafe<String>
        Object incompatible = new Object();
        assertThrows(ClassCastException.class, () -> set.remove(incompatible), "Removing an object of wrong type must throw ClassCastException");
    }

    /**
     * If the internal {@code NavigableSet} reference is {@code null},
     * a {@link NullPointerException} is expected when {@code remove} delegates to it.
     */
    @Test
    void removeWhenInternalSetIsNullThrowsNullPointerException() throws Exception {
        // Create a fresh instance and then null‑out its internalSet via reflection.
        ConcurrentNavigableSetNullSafe<String> unsafe = new ConcurrentNavigableSetNullSafe<>();
        Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalSetField.setAccessible(true);
        // force internalSet to be null
        internalSetField.set(unsafe, null);
        assertThrows(NullPointerException.class, () -> unsafe.remove("anything"), "Calling remove when internalSet is null should throw NPE");
    }

    /**
     * When the underlying {@code NavigableSet} implementation does not support removal,
     * the delegated call should propagate {@link UnsupportedOperationException}.
     */
    @Test
    void removeWhenUnderlyingSetDoesNotSupportRemovalThrowsUnsupportedOperationException(@Mock NavigableSet<Object> mockSet) {
        // Configure the mock to throw UOE on remove(...)
        when(mockSet.remove(any())).thenThrow(new UnsupportedOperationException("remove not supported"));
        // Inject the mock into a new ConcurrentNavigableSetNullSafe instance via reflection.
        ConcurrentNavigableSetNullSafe<String> unsafe = new ConcurrentNavigableSetNullSafe<>();
        try {
            Field internalSetField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
            internalSetField.setAccessible(true);
            internalSetField.set(unsafe, mockSet);
        } catch (ReflectiveOperationException e) {
            fail("Failed to inject mock internalSet: " + e);
        }
        // The call should propagate the UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> unsafe.remove("any"), "Removal on an unmodifiable underlying set must throw UnsupportedOperationException");
    }
}
