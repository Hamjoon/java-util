package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableSet_remove_Test {

    private SealableSet<Integer> set;

    private volatile boolean sealed = false;

    private Supplier<Boolean> sealedSupplier = () -> sealed;

    private SealableSet<Integer> intSet;

    private SealableSet<String> stringSet;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh SealableSet for each test and populate it with known values
        set = new SealableSet<>(sealedSupplier);
        set.add(10);
        set.add(20);
        set.add(null);
        // Ensure the set starts in an un‑sealed state
        sealed = false;
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; no external resources to clean up
        set = null;
    }

    /* Helper to replace the private backing Set via reflection                */
    private <T> void injectBackingSet(SealableSet<T> sealableSet, Set<T> backing) {
        try {
            Field f = SealableSet.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(sealableSet, backing);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Stream<Arguments> normalRemovalProvider() {
        return Stream.of(// existing element "a"
        Arguments.of("remove existing element \"a\"", Set.of("a", "b"), "a", true, 1, false), // non‑existent element "b" when set contains only "a"
        Arguments.of("remove non‑existent element \"b\"", Set.of("a"), "b", false, 1, false), // null present
        Arguments.of("remove null when present", Set.of((String) null, "x"), null, true, 1, false), // null absent
        Arguments.of("remove null when absent", Set.of("x"), null, false, 1, false), // removal from empty set
        Arguments.of("remove any element from empty set", Set.of(), "any", false, 0, false), // different type (String from Integer set)
        Arguments.of("remove String from Integer set", Set.of(1, 2), "any", false, 2, false), // idempotent removal
        Arguments.of("remove \"y\" twice (first true, second false)", Set.of("y"), "y", true, 0, // first call only; second call will be separate test
        false));
    }

    @Test
    void removeExistingElement_returnsTrue_andElementIsAbsent() {
        // given
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        assertTrue(intSet.contains(20));
        // when
        boolean removed = intSet.remove(20);
        // then
        assertTrue(removed, "remove should return true for an existing element");
        assertFalse(intSet.contains(20), "set should no longer contain the removed element");
    }

    @Test
    void removeNonExistingElement_returnsFalse_andSetUnchanged() {
        // given
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        int originalSize = intSet.size();
        assertFalse(intSet.contains(99));
        // when
        boolean removed = intSet.remove(99);
        // then
        assertFalse(removed, "remove should return false when the element is not present");
        assertEquals(originalSize, intSet.size(), "set size must stay the same after a failed remove");
    }

    @Test
    void removeNullElement_whenPresent_returnsTrue_andNullIsAbsent() {
        // given
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        assertTrue(intSet.contains(null));
        // when
        boolean removed = intSet.remove(null);
        // then
        assertTrue(removed, "remove should return true when null is present");
        assertFalse(intSet.contains(null), "null should no longer be present after removal");
    }

    @Test
    void removeNullElement_whenAbsent_returnsFalse() {
        // given
        intSet.add(10);
        intSet.add(20);
        // ensure null is absent
        assertFalse(intSet.contains(null));
        // when
        boolean removed = intSet.remove(null);
        // then
        assertFalse(removed, "remove should return false when null is not present");
    }

    @Test
    void removeWhenSealed_throwsUnsupportedOperationException() {
        // given
        intSet.add(10);
        // seal the set
        sealed = true;
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> intSet.remove(10));
    }

    @Test
    void removeMultipleElements_sequentially_behavesCorrectly() {
        // given
        intSet.add(10);
        intSet.add(20);
        intSet.add(null);
        assertTrue(intSet.contains(10));
        assertTrue(intSet.contains(20));
        // when
        boolean firstRemoval = intSet.remove(10);
        boolean secondRemoval = intSet.remove(20);
        // then
        assertTrue(firstRemoval);
        assertTrue(secondRemoval);
        assertFalse(intSet.contains(10));
        assertFalse(intSet.contains(20));
        // Only the null element should remain
        assertEquals(1, intSet.size());
        assertTrue(intSet.contains(null));
    }

    /* Sealed‑state handling (expected IllegalStateException)                */
    @Test
    void removeWhenSealed_throwsIllegalStateException_anyElement() {
        sealed = true;
        intSet.add(1);
        assertThrows(IllegalStateException.class, () -> intSet.remove(1));
    }

    @Test
    void removeWhenSealed_throwsIllegalStateException_nullArgument() {
        sealed = true;
        intSet.add(null);
        assertThrows(IllegalStateException.class, () -> intSet.remove(null));
    }

    /* Parameterised normal‑removal tests (unsealed)                         */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalRemovalProvider")
    void remove_unsealed_normalRemoval(String description, Set<?> initialElements, Object elementToRemove, boolean expectedResult, int expectedSizeAfter, boolean shouldContainAfter) {
        // Use a String set for non‑integer cases, otherwise an Integer set
        if (initialElements.iterator().next() instanceof String) {
            @SuppressWarnings("unchecked")
            Set<String> backing = new HashSet<>((Collection<String>) initialElements);
            injectBackingSet(stringSet, backing);
            boolean result = stringSet.remove((String) elementToRemove);
            assertEquals(expectedResult, result, description);
            assertEquals(expectedSizeAfter, stringSet.size(), description);
            assertEquals(shouldContainAfter, stringSet.contains(elementToRemove), description);
        } else {
            @SuppressWarnings("unchecked")
            Set<Integer> backing = new HashSet<>((Collection<Integer>) initialElements);
            injectBackingSet(intSet, backing);
            boolean result = intSet.remove(elementToRemove);
            assertEquals(expectedResult, result, description);
            assertEquals(expectedSizeAfter, intSet.size(), description);
            assertEquals(shouldContainAfter, intSet.contains(elementToRemove), description);
        }
    }

    @Test
    void remove_idempotentSecondCall_returnsFalse() {
        // Set up with a single element "y"
        Set<String> backing = new HashSet<>(Set.of("y"));
        injectBackingSet(stringSet, backing);
        // first removal succeeds
        assertTrue(stringSet.remove("y"));
        // second removal must return false
        assertFalse(stringSet.remove("y"));
    }

    /* Edge‑case removal tests                                               */
    @Test
    void remove_NumberWrapper_successful() {
        Set<Number> backing = new HashSet<>(Set.of(42, 99));
        // SealableSet is generic, we can use Integer as T (Number is assignable)
        @SuppressWarnings("unchecked")
        SealableSet<Integer> numberSet = new SealableSet<>(sealedSupplier);
        injectBackingSet(numberSet, (Set<Integer>) (Set<?>) backing);
        boolean removed = numberSet.remove(42);
        assertTrue(removed);
        assertEquals(1, numberSet.size());
        assertFalse(numberSet.contains(42));
    }

    @Test
    void remove_duplicateElement_firstTrueSecondFalse() {
        Set<String> backing = new HashSet<>(Set.of("duplicate"));
        injectBackingSet(stringSet, backing);
        assertTrue(stringSet.remove("duplicate"));
        assertFalse(stringSet.remove("duplicate"));
    }

    /* Underlying set exception propagation tests                            */
    @Test
    void remove_underlyingTreeSet_throwsClassCastException() {
        // TreeSet<Integer> will throw ClassCastException when a String is removed
        TreeSet<Integer> treeSet = new TreeSet<>(Set.of(1, 2, 3));
        injectBackingSet(intSet, treeSet);
        assertThrows(ClassCastException.class, () -> intSet.remove("stringElement"));
    }

    @Test
    void remove_underlyingTreeSet_throwsNullPointerException() {
        // Default TreeSet does not allow null
        TreeSet<Integer> treeSet = new TreeSet<>(Set.of(1, 2, 3));
        injectBackingSet(intSet, treeSet);
        assertThrows(NullPointerException.class, () -> intSet.remove(null));
    }

    @Test
    void remove_underlyingUnmodifiableSet_throwsUnsupportedOperationException() {
        Set<String> modifiable = new HashSet<>(Set.of("a", "b"));
        Set<String> unmodifiable = Collections.unmodifiableSet(modifiable);
        injectBackingSet(stringSet, unmodifiable);
        assertThrows(UnsupportedOperationException.class, () -> stringSet.remove("anyElement"));
    }
}
