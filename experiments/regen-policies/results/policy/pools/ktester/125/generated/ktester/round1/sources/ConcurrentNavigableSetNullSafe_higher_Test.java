package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_higher_Test {

    private NavigableSet<String> naturalSet;

    private NavigableSet<String> lengthComparatorSet;

    private NavigableSet<String> emptySet;

    @BeforeEach
    void setupBeforeEach() {
        // Natural ordering set (contains null and three strings)
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        naturalSet.add(null);
        naturalSet.add("apple");
        naturalSet.add("banana");
        naturalSet.add("cherry");
        // Comparator that orders by string length (null considered length 0)
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        lengthComparatorSet.add(null);
        lengthComparatorSet.add("kiwi");
        lengthComparatorSet.add("apple");
        lengthComparatorSet.add("banana");
        // Empty set for edge‑case testing
        emptySet = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        naturalSet.clear();
        lengthComparatorSet.clear();
        emptySet.clear();
    }

    private static Stream<Arguments> provideNaturalOrderingCases() {
        return Stream.of(Arguments.of("a", "b"), Arguments.of("c", null), Arguments.of("z", null));
    }

    @Test
    void higher_NaturalOrdering_WithNullAndElements() {
        // null is the smallest element
        assertEquals("apple", naturalSet.higher(null), "higher(null) should return the first non‑null element");
        // regular ordering checks
        assertEquals("banana", naturalSet.higher("apple"));
        assertEquals("cherry", naturalSet.higher("banana"));
        assertNull(naturalSet.higher("cherry"), "higher of the greatest element should be null");
    }

    @Test
    void higher_NaturalOrdering_ElementNotPresent() {
        // Element not present but lower than existing elements
        assertEquals("apple", naturalSet.higher("aardvark"));
        // Element greater than any present element
        assertNull(naturalSet.higher("zoo"));
    }

    @Test
    void higher_WithLengthComparator() {
        // Ordering: null (len 0), kiwi (4), apple (5), banana (6)
        assertEquals("kiwi", lengthComparatorSet.higher(null), "higher(null) should return the shortest non‑null string");
        assertEquals("apple", lengthComparatorSet.higher("kiwi"));
        assertEquals("banana", lengthComparatorSet.higher("apple"));
        assertNull(lengthComparatorSet.higher("banana"));
    }

    @Test
    void higher_EmptySetReturnsNull() {
        assertNull(emptySet.higher(null));
        assertNull(emptySet.higher("any"));
    }

    @Test
    void higher_NullElementWhenSetContainsOnlyNull() {
        NavigableSet<String> onlyNull = new ConcurrentNavigableSetNullSafe<>();
        onlyNull.add(null);
        assertNull(onlyNull.higher(null), "When the set contains only null, higher(null) must be null");
    }

    /**
     * Parameterized test for simple natural‑ordering scenarios with Strings.
     */
    @ParameterizedTest
    @MethodSource("provideNaturalOrderingCases")
    void higher_NaturalOrdering_Parameterized(String argument, String expected) {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add("b");
        set.add("c");
        // also add a null to verify that null does not interfere with higher()
        set.add(null);
        if (expected == null) {
            assertNull(set.higher(argument));
        } else {
            assertEquals(expected, set.higher(argument));
        }
    }

    /**
     * Verify that {@code higher} works with a non‑String type (UUID) using the
     * natural ordering of {@link java.util.UUID}. The set contains only a single
     * UUID, therefore the result must be {@code null}.
     */
    @Test
    void higher_UUID_NoHigherElement() {
        NavigableSet<UUID> uuidSet = new ConcurrentNavigableSetNullSafe<>();
        UUID only = new UUID(0L, 0L);
        uuidSet.add(only);
        assertNull(uuidSet.higher(only), "Only element present, higher should be null");
    }

    /**
     * Set contains duplicate elements masked as null (i.e., a real {@code null}
     * and normal strings). Ensure that {@code higher("b")} returns {@code "c"}.
     */
    @Test
    void higher_SkipsMaskedNull() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add(null);
        set.add("b");
        set.add("c");
        assertEquals("c", set.higher("b"));
    }

    /**
     * Argument smaller than the smallest element – should return the first element.
     */
    @Test
    void higher_ElementSmallerThanSmallest() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add("b");
        set.add("c");
        assertEquals("a", set.higher("0"));
    }

    /**
     * Custom comparator that orders strings in reverse (descending) order.
     */
    @Test
    void higher_ReverseComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableSet<String> revSet = new ConcurrentNavigableSetNullSafe<>(reverse);
        revSet.add("c");
        revSet.add("b");
        revSet.add("a");
        // In reverse order the element after "c" is "b"
        assertEquals("b", revSet.higher("c"));
        // higher(null) should return the greatest element according to the comparator ("c")
        assertEquals("c", revSet.higher(null));
    }

    /**
     * Case‑insensitive comparator test.
     */
    @Test
    void higher_CaseInsensitiveComparator() {
        Comparator<String> ci = String.CASE_INSENSITIVE_ORDER;
        NavigableSet<String> ciSet = new ConcurrentNavigableSetNullSafe<>(ci);
        ciSet.add("Apple");
        ciSet.add("Banana");
        ciSet.add("Cherry");
        assertEquals("Banana", ciSet.higher("apple"));
    }

    /**
     * Supplying an argument of an incompatible type should propagate a
     * {@link ClassCastException} from the underlying {@link java.util.concurrent.ConcurrentSkipListSet}.
     */
    @Test
    void higher_IncompatibleTypeThrowsClassCastException() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("a");
        set.add("b");
        // The set expects Strings; passing an Integer must fail.
        assertThrows(ClassCastException.class, () -> set.higher(123));
    }

    /**
     * Comparator based on element length – verify that a shorter argument
     * receives the next longer element.
     */
    @Test
    void higher_LengthComparator_CustomElements() {
        Comparator<String> lengthComp = Comparator.comparingInt(String::length);
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComp);
        // length 2
        set.add("aa");
        // length 3
        set.add("bbb");
        // "x" has length 1, so the next greater element by length is "aa"
        assertEquals("aa", set.higher("x"));
    }

    /**
     * When the internal set uses natural ordering (no comparator) the sentinel
     * used to mask {@code null} does not implement {@link Comparable}. Calling
     * {@code higher(null)} therefore triggers a {@link ClassCastException}.
     */
    @Test
    void higher_MaskedNullNotComparableThrowsClassCastException() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        // Only a real null is stored; the sentinel will be compared with the
        // natural ordering of Strings, which leads to ClassCastException.
        set.add(null);
        assertThrows(ClassCastException.class, () -> set.higher(null));
    }

    /**
     * Defensive programming test – forcibly set the private {@code internalSet}
     * field to {@code null} via reflection and verify that {@code higher}
     * throws a {@link NullPointerException}.
     */
    @Test
    void higher_InternalSetIsNull_ThrowsNullPointerException() throws Exception {
        ConcurrentNavigableSetNullSafe<String> corrupted = new ConcurrentNavigableSetNullSafe<>();
        // Use reflection to nullify the internalSet field
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(corrupted, null);
        assertThrows(NullPointerException.class, () -> corrupted.higher("any"));
    }

    /**
     * Comparator that explicitly throws {@link ClassCastException} when it
     * receives a non‑String argument (including the internal null sentinel).
     */
    @Test
    void higher_ComparatorThrowsOnNullArgument() {
        Comparator<String> throwingComparator = (o1, o2) -> {
            // Force a ClassCastException for any non‑String (the sentinel is Object)
            if (!(o1 instanceof String) || !(o2 instanceof String)) {
                throw new ClassCastException("Comparator only accepts Strings");
            }
            return o1.compareTo(o2);
        };
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>(throwingComparator);
        set.add("a");
        set.add("b");
        // The sentinel for null will be passed to the comparator -> exception
        assertThrows(ClassCastException.class, () -> set.higher(null));
    }
}
