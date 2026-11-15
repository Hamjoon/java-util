package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_remove_Test {

    /**
     * The instance under test – created fresh for each test method.
     */
    private ConcurrentSet<String> set;

    /**
     * Custom key class with proper {@code equals} and {@code hashCode}
     * to test that {@code remove} works with equal but distinct instances.
     */
    static final class CustomKey {

        private final String id;

        CustomKey(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomKey))
                return false;
            CustomKey that = (CustomKey) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{id='" + id + "'}";
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a clean ConcurrentSet instance.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to help the GC and to make the
        // test intent obvious (not strictly required for this class).
        set.clear();
    }

    private static Stream<Arguments> basicRemovalProvider() {
        return Stream.of(// Remove an existing element that was previously added; should return true.
        Arguments.of("element1", true, "previously added"), // Attempt to remove an element that is not present; should return false.
        Arguments.of("nonexistent", false, "not present"), // Remove null when null is not in the set; should return false.
        Arguments.of(null, false, "null not present"));
    }

    @Test
    @DisplayName("remove(Object) returns true for an existing element and actually removes it")
    void removeExistingElement() {
        // given
        assertTrue(set.add("alpha"), "add should succeed for a new element");
        assertTrue(set.contains("alpha"), "set must contain the added element before removal");
        // when
        boolean removed = set.remove("alpha");
        // then
        assertTrue(removed, "remove should return true for an element that was present");
        assertFalse(set.contains("alpha"), "set must no longer contain the element after removal");
        assertTrue(set.isEmpty(), "set should be empty after removing the sole element");
    }

    @Test
    @DisplayName("remove(Object) returns false when the element is not present")
    void removeNonExistingElement() {
        // given – the set is empty
        assertFalse(set.contains("beta"), "pre‑condition: set does not contain the element");
        // when
        boolean removed = set.remove("beta");
        // then
        assertFalse(removed, "remove should return false when the element is absent");
        assertTrue(set.isEmpty(), "set should remain empty");
    }

    @Test
    @DisplayName("remove(Object) correctly handles null values")
    void removeNullElement() {
        // given
        assertTrue(set.add(null), "add should accept null (sentinel handling)");
        assertTrue(set.contains(null), "set must contain null after addition");
        // when
        boolean removed = set.remove(null);
        // then
        assertTrue(removed, "remove should return true for a present null element");
        assertFalse(set.contains(null), "set must no longer contain null after removal");
        assertTrue(set.isEmpty(), "set should be empty after removing null");
    }

    @Test
    @DisplayName("remove(Object) works correctly after bulk addition via addAll")
    void removeAfterAddAll() {
        // given
        set.addAll(Arrays.asList("x", "y", "z"));
        assertEquals(3, set.size(), "set should contain three elements after addAll");
        // when
        boolean removedY = set.remove("y");
        boolean removedMissing = set.remove("missing");
        // then
        assertTrue(removedY, "remove should return true for an element that existed");
        assertFalse(removedMissing, "remove should return false for a non‑existent element");
        assertEquals(2, set.size(), "size should reflect the removal of one element");
        assertTrue(set.containsAll(Arrays.asList("x", "z")), "remaining elements must be present");
        assertFalse(set.contains("y"), "removed element must no longer be present");
    }

    @Test
    @DisplayName("remove(Object) is idempotent – removing the same element twice")
    void removeIdempotent() {
        // given
        set.add("once");
        assertTrue(set.remove("once"), "first removal should succeed");
        // when
        boolean secondRemoval = set.remove("once");
        // then
        assertFalse(secondRemoval, "second removal of the same element should return false");
        assertTrue(set.isEmpty(), "set should remain empty after the second removal");
    }

    @Test
    @DisplayName("remove(Object) works correctly when the set contains duplicate attempts via add")
    void removeAfterDuplicateAddAttempt() {
        // given
        assertTrue(set.add("dup"), "first add should succeed");
        assertFalse(set.add("dup"), "second add of same element should return false");
        assertEquals(1, set.size(), "set size must be 1 after duplicate add attempt");
        // when
        boolean removed = set.remove("dup");
        // then
        assertTrue(removed, "remove should succeed for the present element");
        assertTrue(set.isEmpty(), "set should be empty after removal");
    }

    @Test
    @DisplayName("remove(Object) interacts correctly with removeAll (which delegates to remove)")
    void removeAllDelegatesToRemove() {
        // given
        set.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(3, set.size());
        // when
        boolean changed = set.removeAll(Collections.singleton("b"));
        // then
        assertTrue(changed, "removeAll should report a modification when an element is removed");
        assertFalse(set.contains("b"), "element 'b' must be removed");
        assertEquals(2, set.size(), "size must reflect the removal");
        assertTrue(set.containsAll(Arrays.asList("a", "c")), "remaining elements must stay");
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for the basic functionality group.
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} ⇒ remove({0}) returns {1}")
    @MethodSource("basicRemovalProvider")
    @DisplayName("remove(Object) – basic functionality")
    void removeBasicFunctionality(Object elementToRemove, boolean expectedResult, String preconditionDescription) {
        // Arrange – set up pre‑condition described in the JSON test case
        if (preconditionDescription.contains("previously added")) {
            assertTrue(set.add("element1"), "setup: element1 must be added");
        } else if (preconditionDescription.contains("null element was added")) {
            assertTrue(set.add(null), "setup: null must be added");
        }
        // Act
        boolean result = set.remove(elementToRemove);
        // Assert
        assertEquals(expectedResult, result, () -> "remove(" + elementToRemove + ") should return " + expectedResult);
    }

    @Test
    @DisplayName("remove(Object) correctly handles null values when present")
    void removeNullElementWhenPresent() {
        // given
        assertTrue(set.add(null), "add should accept null (sentinel handling)");
        assertTrue(set.contains(null), "set must contain null after addition");
        // when
        boolean removed = set.remove(null);
        // then
        assertTrue(removed, "remove should return true for a present null element");
        assertFalse(set.contains(null), "set must no longer contain null after removal");
        assertTrue(set.isEmpty(), "set should be empty after removing null");
    }

    @Test
    @DisplayName("remove(Object) returns false after the set has been cleared")
    void removeAfterClear() {
        // given
        assertTrue(set.add("cleared"), "element must be added before clear");
        set.clear();
        assertTrue(set.isEmpty(), "set must be empty after clear");
        // when
        boolean removed = set.remove("cleared");
        // then
        assertFalse(removed, "removing after clear should return false");
    }

    @Test
    @DisplayName("remove(Object) decrements size correctly")
    void sizeDecrementAfterRemoval() {
        // given
        set.add("first");
        set.add("second");
        assertEquals(2, set.size(), "two elements should be present");
        // when
        boolean removed = set.remove("second");
        // then
        assertTrue(removed, "removal should succeed");
        assertEquals(1, set.size(), "size should decrease to 1 after removal");
        assertTrue(set.contains("first"), "remaining element must still be present");
    }

    @Test
    @DisplayName("remove(Object) throws ClassCastException when type mismatched")
    void removeTypeMismatchThrowsClassCastException() {
        // given
        // valid element for ConcurrentSet<String>
        set.add("string");
        // when / then
        assertThrows(ClassCastException.class, () -> set.remove(123), "Removing an Integer from a ConcurrentSet<String> should throw ClassCastException");
    }

    @Test
    @DisplayName("remove(Object) succeeds with a different instance that is equal")
    void removeCustomKeyEquality() {
        // given a set of CustomKey
        ConcurrentSet<CustomKey> keySet = new ConcurrentSet<>();
        CustomKey first = new CustomKey("id");
        // equal to first but distinct instance
        CustomKey second = new CustomKey("id");
        assertTrue(keySet.add(first), "first custom key should be added");
        assertEquals(1, keySet.size(), "size should be 1 after addition");
        // when
        boolean removed = keySet.remove(second);
        // then
        assertTrue(removed, "remove should succeed for an equal key");
        assertTrue(keySet.isEmpty(), "set should be empty after removal");
    }
}
