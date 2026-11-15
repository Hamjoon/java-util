package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_tailSet_Test {

    /* --------------------------------------------------------------------- */
    private static class TailSetCase {

        final String description;

        // null literal means actual null
        final String fromElement;

        final boolean inclusive;

        // null means an exception is expected
        final List<String> expected;

        // null if no exception
        final Class<? extends Throwable> expectedException;

        TailSetCase(String description, String fromElement, boolean inclusive, List<String> expected, Class<? extends Throwable> expectedException) {
            this.description = description;
            this.fromElement = fromElement;
            this.inclusive = inclusive;
            this.expected = expected;
            this.expectedException = expectedException;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /* --------------------------------------------------------------------- */
    private static Stream<TailSetCase> basicFunctionalityProvider() {
        return Stream.of(// null fromElement – should return whole set irrespective of inclusive flag
        new TailSetCase("null fromElement, inclusive=true", null, true, Arrays.asList("a", "b", "c"), null), new TailSetCase("null fromElement, inclusive=false", null, false, Arrays.asList("a", "b", "c"), null), // fromElement present in set
        new TailSetCase("\"b\" inclusive", "b", true, Arrays.asList("b", "c"), null), new TailSetCase("\"b\" exclusive", "b", false, Collections.singletonList("c"), null), // fromElement not present – inclusive true
        new TailSetCase("\"aa\" inclusive (not present)", "aa", true, Arrays.asList("b", "c"), null), // fromElement not present – inclusive false (same result)
        new TailSetCase("\"aa\" exclusive (not present)", "aa", false, Arrays.asList("b", "c"), null), // fromElement greater than any element – empty result
        new TailSetCase("\"d\" inclusive (beyond range)", "d", true, Collections.emptyList(), null), new TailSetCase("\"d\" exclusive (beyond range)", "d", false, Collections.emptyList(), null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_Inclusive_NaturalOrder() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        // natural order: null is considered greater than any non‑null element
        set.addAll(Arrays.asList("apple", "banana", "cherry", null));
        NavigableSet<String> tail = set.tailSet("banana", true);
        // Expected: banana, cherry, null
        List<String> expected = Arrays.asList("banana", "cherry", null);
        assertEquals(expected.size(), tail.size(), "Tail set size mismatch");
        assertIterableEquals(expected, tail, "Tail set elements mismatch (inclusive)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_Exclusive_NaturalOrder() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("apple", "banana", "cherry", null));
        NavigableSet<String> tail = set.tailSet("banana", false);
        // Expected: cherry, null
        List<String> expected = Arrays.asList("cherry", null);
        assertEquals(expected.size(), tail.size());
        assertIterableEquals(expected, tail);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_FromElementBeyondRange() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("apple", "banana", "cherry"));
        NavigableSet<String> tail = set.tailSet("zebra", true);
        assertTrue(tail.isEmpty(), "Tail set should be empty when fromElement > max element");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_OnEmptySet() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        NavigableSet<String> tail = set.tailSet("any", true);
        assertTrue(tail.isEmpty(), "Tail set of an empty set must be empty");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_WithCustomComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        set.addAll(Arrays.asList(null, "kiwi", "apple", "banana", "cherry"));
        // Order by length: null (0), kiwi (4), apple (5), cherry (6), banana (6) – tie broken by natural order
        // Inclusive from "apple" (length 5) → apple, cherry, banana
        NavigableSet<String> inclusive = set.tailSet("apple", true);
        List<String> expectedInc = Arrays.asList("apple", "cherry", "banana");
        assertIterableEquals(expectedInc, inclusive);
        // Exclusive from "apple" → cherry, banana
        NavigableSet<String> exclusive = set.tailSet("apple", false);
        List<String> expectedExc = Arrays.asList("cherry", "banana");
        assertIterableEquals(expectedExc, exclusive);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_FromNull_WithComparator() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        set.addAll(Arrays.asList(null, "a", "bb", "ccc"));
        // Inclusive – should contain everything
        NavigableSet<String> inc = set.tailSet(null, true);
        List<String> expectedInc = Arrays.asList(null, "a", "bb", "ccc");
        assertIterableEquals(expectedInc, inc);
        // Exclusive – should skip the null sentinel
        NavigableSet<String> exc = set.tailSet(null, false);
        List<String> expectedExc = Arrays.asList("a", "bb", "ccc");
        assertIterableEquals(expectedExc, exc);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicFunctionalityProvider")
    void tailSet_BasicFunctionality(TailSetCase tc) {
        // Arrange – a set with natural ordering containing a, b, c
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        // Act & Assert
        if (tc.expectedException != null) {
            assertThrows(tc.expectedException, () -> set.tailSet(tc.fromElement, tc.inclusive));
        } else {
            NavigableSet<String> tail = set.tailSet(tc.fromElement, tc.inclusive);
            assertIterableEquals(tc.expected, tail, () -> "Tail set mismatch for case: " + tc.description);
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_EmptySet() {
        NavigableSet<String> empty = new ConcurrentNavigableSetNullSafe<>();
        // null fromElement – still empty
        NavigableSet<String> tail1 = empty.tailSet(null, true);
        assertTrue(tail1.isEmpty(), "Tail set of empty set should be empty (null, inclusive)");
        // non‑null fromElement – still empty
        NavigableSet<String> tail2 = empty.tailSet("any", false);
        assertTrue(tail2.isEmpty(), "Tail set of empty set should be empty (non‑null, exclusive)");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_NullInternalSet_ThrowsNPE() throws Exception {
        // Create an instance via the public default constructor
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        // Use reflection to set the internalSet field to null
        Field internalField = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        internalField.setAccessible(true);
        internalField.set(set, null);
        // The call must dereference internalSet and therefore throw NPE
        assertThrows(NullPointerException.class, () -> set.tailSet("any", true), "Calling tailSet on a set with null internalSet should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_IncompatibleElementType_ThrowsClassCastException() {
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        // Pass an Integer where a String is expected
        assertThrows(ClassCastException.class, () -> set.tailSet((String) (Object) Integer.valueOf(123), false), "Passing an element of incompatible type should result in ClassCastException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_ComparatorRejectsElement_ThrowsIllegalArgumentException() {
        Comparator<String> rejectingComparator = (s1, s2) -> {
            if ("bad".equals(s1) || "bad".equals(s2)) {
                throw new IllegalArgumentException("Comparator does not accept \"bad\"");
            }
            // natural order fallback for other values
            return Comparator.naturalOrder().compare(s1, s2);
        };
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(rejectingComparator);
        set.addAll(Arrays.asList("good", "bad", "ugly"));
        // The tailSet call will trigger the comparator with "bad"
        assertThrows(IllegalArgumentException.class, () -> set.tailSet("bad", true), "Comparator rejecting element should propagate IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void tailSet_WithLengthComparator_InclusiveAndExclusive() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length()).thenComparing(Comparator.naturalOrder());
        NavigableSet<String> set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        set.addAll(Arrays.asList(null, "kiwi", "apple", "banana", "cherry"));
        // Inclusive from "apple" (length 5) → apple, cherry, banana
        NavigableSet<String> inclusive = set.tailSet("apple", true);
        List<String> expectedInc = Arrays.asList("apple", "cherry", "banana");
        assertIterableEquals(expectedInc, inclusive, "Inclusive tailSet with length comparator failed");
        // Exclusive from "apple" → cherry, banana
        NavigableSet<String> exclusive = set.tailSet("apple", false);
        List<String> expectedExc = Arrays.asList("cherry", "banana");
        assertIterableEquals(expectedExc, exclusive, "Exclusive tailSet with length comparator failed");
    }
}
