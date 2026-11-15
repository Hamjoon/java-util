package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableSet_first_Test {

    private SealableNavigableSet<Integer> set;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        // Default set used by the original tests – populated with three integers.
        set = new SealableNavigableSet<>(sealedSupplier);
        set.add(30);
        set.add(10);
        set.add(20);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and reset mutable state
        set = null;
        sealedState = false;
    }

    private static Stream<Arguments> stringFirstProvider() {
        return Stream.of(Arguments.of(Arrays.asList("banana", "apple", "cherry"), "apple"), Arguments.of(Collections.singletonList("A"), "A"));
    }

    @Test
    void firstReturnsLowestElement() {
        // Natural ordering: 10 is the smallest element
        assertEquals(10, set.first());
    }

    @Test
    void firstWorksWhenSealed() {
        // simulate sealed (read‑only) state
        sealedState = true;
        assertEquals(10, set.first(), "first() should still work when the set is sealed");
    }

    @Test
    void firstThrowsWhenSetIsEmpty() {
        SealableNavigableSet<Integer> empty = new SealableNavigableSet<>(sealedSupplier);
        assertThrows(NoSuchElementException.class, empty::first, "first() must throw NoSuchElementException on an empty set");
    }

    @Test
    void firstRespectsCustomComparator() {
        // Reverse order comparator: highest value becomes the first element
        SealableNavigableSet<Integer> reverseSet = new SealableNavigableSet<>(Comparator.reverseOrder(), sealedSupplier);
        reverseSet.add(5);
        reverseSet.add(10);
        reverseSet.add(1);
        assertEquals(10, reverseSet.first(), "With a reverse order comparator, the greatest element should be returned by first()");
    }

    /**
     * Group: first() behavior – strings **
     */
    @Test
    void firstReturnsSmallestStringInNaturalOrder() {
        // Set contains ["banana", "apple", "cherry"]; expected "apple"
        SealableNavigableSet<String> stringSet = new SealableNavigableSet<>(sealedSupplier);
        stringSet.addAll(Arrays.asList("banana", "apple", "cherry"));
        assertEquals("apple", stringSet.first(), "first() should return the smallest string according to natural order");
    }

    @Test
    void firstThrowsWhenStringSetIsEmpty() {
        SealableNavigableSet<String> empty = new SealableNavigableSet<>(sealedSupplier);
        assertThrows(NoSuchElementException.class, empty::first, "first() must propagate NoSuchElementException when the underlying set is empty");
    }

    @Test
    void firstUsesReverseStringComparator() {
        // Reverse comparator: highest natural order becomes first
        SealableNavigableSet<String> reverse = new SealableNavigableSet<>(Comparator.reverseOrder(), sealedSupplier);
        reverse.addAll(Arrays.asList("zebra", "lion", "tiger"));
        assertEquals("zebra", reverse.first(), "With a reverse comparator, the greatest element (\"zebra\") should be returned");
    }

    @Test
    void firstReturnsOnlyElementWhenSingleton() {
        SealableNavigableSet<String> singleton = new SealableNavigableSet<>(sealedSupplier);
        singleton.add("A");
        assertEquals("A", singleton.first(), "When the set contains a single element, first() must return that element");
    }

    /**
     * Group: first() after element removal **
     */
    @Test
    void firstAfterRemovingSmallestElement() {
        SealableNavigableSet<String> set = new SealableNavigableSet<>(sealedSupplier);
        set.addAll(Arrays.asList("b", "a", "c"));
        // Remove the current smallest element "a"
        assertTrue(set.remove("a"));
        assertEquals("b", set.first(), "After removing \"a\", the new smallest element should be \"b\"");
    }

    /**
     * Group: first() with integer natural ordering **
     */
    @Test
    void firstReturnsSmallestInteger() {
        SealableNavigableSet<Integer> intSet = new SealableNavigableSet<>(sealedSupplier);
        intSet.addAll(Arrays.asList(5, 3, 1, 4));
        assertEquals(1, intSet.first(), "first() should return the smallest integer (1) according to natural ordering");
    }

    /**
     * Group: first() with comparator handling null values **
     */
    @Test
    void firstWithNullFirstComparator() {
        Comparator<String> nullFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        SealableNavigableSet<String> set = new SealableNavigableSet<>(nullFirst, sealedSupplier);
        set.addAll(Arrays.asList(null, "beta"));
        assertNull(set.first(), "When a null‑first comparator is used, first() should return null");
    }

    /**
     * Group: first() with length‑based comparator **
     */
    @Test
    void firstWithLengthBasedComparator() {
        Comparator<String> byLength = Comparator.comparingInt(String::length);
        SealableNavigableSet<String> set = new SealableNavigableSet<>(byLength, sealedSupplier);
        set.addAll(Arrays.asList("aa", "b", "cccc"));
        assertEquals("b", set.first(), "Length‑based comparator should make the shortest string (\"b\") the first element");
    }

    /**
     * Group: first() after adding elements post‑construction **
     */
    @Test
    void firstAfterAddingElementsLater() {
        SealableNavigableSet<String> set = new SealableNavigableSet<>(sealedSupplier);
        // Add elements in non‑sorted order
        set.add("gamma");
        set.add("beta");
        set.add("alpha");
        assertEquals("alpha", set.first(), "After adding elements post‑construction, first() must return the smallest (\"alpha\")");
    }

    /**
     * Group: first() null underlying set **
     */
    @Test
    void firstThrowsWhenUnderlyingNavSetIsNull() throws Exception {
        // Construct a normal instance then use reflection to null out the delegate.
        SealableNavigableSet<String> set = new SealableNavigableSet<>(sealedSupplier);
        Field navSetField = SealableNavigableSet.class.getDeclaredField("navSet");
        navSetField.setAccessible(true);
        // simulate a null delegate
        navSetField.set(set, null);
        assertThrows(NullPointerException.class, set::first, "If the delegate NavigableSet is null, first() should throw NullPointerException");
    }

    @ParameterizedTest(name = "first() on {0} → {1}")
    @MethodSource("stringFirstProvider")
    void firstParameterizedStringTest(List<String> elements, String expected) {
        SealableNavigableSet<String> set = new SealableNavigableSet<>(sealedSupplier);
        set.addAll(elements);
        assertEquals(expected, set.first(), "Parameterized test: expected first() to be \"" + expected + "\"");
    }
}
