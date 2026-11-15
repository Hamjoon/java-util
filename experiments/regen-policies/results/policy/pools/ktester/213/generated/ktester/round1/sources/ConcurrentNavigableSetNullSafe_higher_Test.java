package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_higher_Test {

    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // a fresh set for each test – natural ordering
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid accidental cross‑test contamination
        set.clear();
    }

    /* -------------------------------------------------------------
     *  Parameterized tests for the first group "higher(E) method tests"
     * ------------------------------------------------------------- */
    private static Stream<Arguments> naturalOrderCases() {
        return Stream.of(// null argument – should return the smallest actual element
        Arguments.of(null, "apple", "When the argument is null, higher should return the smallest element after unmasking."), // standard case
        Arguments.of("apple", "banana", "Standard case: higher(\"apple\") returns \"banana\"."), // greatest element
        Arguments.of("cherry", null, "Argument is the greatest element, higher should return null."), // greater than any element
        Arguments.of("zzz", null, "Argument greater than any element, higher should return null."), // less than smallest element
        Arguments.of("0", "apple", "Argument less than the smallest element, higher should return the smallest element."), // literal string "null"
        Arguments.of("null", "apple", "String literal \"null\" is treated as a normal element."), // incomparable type (UUID) – will be cast to Object, higher returns null
        Arguments.of(new UUID(0L, 0L), null, "Incomparable type should result in null."));
    }

    /**
     * Verify higher(E) with natural ordering and a mixture of
     * non‑null and null elements.
     */
    @Test
    void higher_NaturalOrder_MixedElements() {
        // arrange
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // act & assert
        assertEquals("banana", set.higher("apple"), "higher should return the next element");
        assertEquals("cherry", set.higher("banana"), "higher should return the next element");
        assertNull(set.higher("cherry"), "higher of the greatest element should be null");
        assertNull(set.higher(null), "higher of null (the greatest element) should be null");
    }

    /**
     * Verify higher(E) on an empty set always returns {@code null}.
     */
    @Test
    void higher_EmptySet_ReturnsNull() {
        assertTrue(set.isEmpty());
        assertNull(set.higher("any"), "higher on empty set must return null");
        assertNull(set.higher(null), "higher on empty set with null must return null");
    }

    /**
     * Verify higher(E) when a custom comparator is supplied.
     * The comparator orders strings by length, treating {@code null}
     * as length 0 (the smallest element).
     */
    @Test
    void higher_CustomComparator_LengthBased() {
        // arrange – replace the default set with one that uses a length comparator
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        assertTrue(set.add(null));
        assertTrue(set.add("kiwi"));
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        // act & assert – order should be: null, kiwi, apple, banana
        assertEquals("kiwi", set.higher(null), "null is the smallest, higher should be the shortest non‑null");
        assertEquals("apple", set.higher("kiwi"), "next after kiwi (length 4) is apple (length 5)");
        assertEquals("banana", set.higher("apple"), "next after apple (length 5) is banana (length 6)");
        assertNull(set.higher("banana"), "banana is the greatest element");
    }

    /**
     * Verify higher(E) when the argument itself is not present in the set.
     * The method should return the least element greater than the argument
     * according to the set's ordering.
     */
    @Test
    void higher_ElementNotPresent_ReturnsNextHigher() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // "banana" is not in the set, but it lies between "apple" and "cherry"
        assertEquals("cherry", set.higher("banana"), "higher should return the smallest element greater than the argument even if the argument is absent");
        // a value greater than any element should yield null
        assertNull(set.higher("zoo"));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("naturalOrderCases")
    void higher_NaturalOrder_VariousInputs(Object e, String expected, String _desc) {
        // arrange – populate the set with three strings and a null element
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertTrue(set.add("cherry"));
        assertTrue(set.add(null));
        // act
        @SuppressWarnings("unchecked")
        String result = (String) ((NavigableSet<Object>) set).higher(e);
        // assert
        assertEquals(expected, result);
    }

    /* -------------------------------------------------------------
     *  Edge cases where the set contains only null or a mixture
     * ------------------------------------------------------------- */
    @Test
    void higher_OnlyNullElement_ReturnsNull() {
        assertTrue(set.add(null));
        // the internal placeholder for null is considered the greatest element
        assertNull(set.higher(null));
    }

    @Test
    void higher_MixedNullAndValues() {
        assertTrue(set.add(null));
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        // higher(null) should give the smallest non‑null element
        assertEquals("apple", set.higher(null));
        // higher("apple") should skip equal element and give next
        assertEquals("banana", set.higher("apple"));
    }

    /* -------------------------------------------------------------
     *  Integer set tests (different generic type)
     * ------------------------------------------------------------- */
    @Test
    void higher_IntegerSet_VariousScenarios() {
        NavigableSet<Integer> intSet = new ConcurrentNavigableSetNullSafe<>();
        assertTrue(intSet.addAll(Arrays.asList(1, 3, 5, 7)));
        assertEquals(7, intSet.higher(5));
        assertEquals(7, intSet.higher(6));
        assertNull(intSet.higher(7));
    }

    /* -------------------------------------------------------------
     *  Reverse order comparator tests (group 2)
     * ------------------------------------------------------------- */
    @Test
    void higher_ReverseOrderComparator() {
        Comparator<String> reverse = Comparator.reverseOrder();
        set = new ConcurrentNavigableSetNullSafe<>(reverse);
        assertTrue(set.addAll(Arrays.asList("apple", "banana", "cherry")));
        // In reverse order the "greater" element is actually the smaller string
        assertEquals("apple", set.higher("banana"), "In reverse order, higher(\"banana\") should return \"apple\".");
        assertEquals("banana", set.higher("cherry"), "In reverse order, higher(\"cherry\") should return \"banana\".");
        assertNull(set.higher("apple"), "In reverse order, \"apple\" is the greatest element, so higher returns null.");
    }

    /* -------------------------------------------------------------
     *  Exception scenarios (group 3)
     * ------------------------------------------------------------- */
    @Test
    void higher_IncompatibleArgument_ThrowsClassCastException() {
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        // Passing an Integer to a String‑based set should trigger ClassCastException
        assertThrows(ClassCastException.class, () -> ((NavigableSet<Object>) set).higher(42));
    }

    @Test
    void higher_ComparatorThrowsRuntimeException() {
        Comparator<String> badComparator = (o1, o2) -> {
            throw new RuntimeException("Comparator failure");
        };
        set = new ConcurrentNavigableSetNullSafe<>(badComparator);
        assertTrue(set.add("x"));
        assertTrue(set.add("y"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> set.higher("x"));
        assertEquals("Comparator failure", ex.getMessage());
    }

    @Test
    void higher_InternalSetNull_ThrowsNullPointerException() throws Exception {
        // Use reflection to null out the internalSet field after construction
        Field internal = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internal.setAccessible(true);
        internal.set(set, null);
        assertThrows(NullPointerException.class, () -> set.higher("any"));
    }
}
