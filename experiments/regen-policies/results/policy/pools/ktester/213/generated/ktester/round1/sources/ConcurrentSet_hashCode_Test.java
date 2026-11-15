package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_hashCode_Test {

    private ConcurrentSet<Object> set;

    // ---------- Helper classes for custom hashCode / equals behavior ----------
    private static class Foo {

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Foo;
        }
    }

    private static class Bar {

        @Override
        public int hashCode() {
            return 7;
        }

        @Override
        public boolean equals(Object o) {
            // Distinguish by identity
            return this == o;
        }
    }

    private static class MutableInt {

        private int value;

        MutableInt(int value) {
            this.value = value;
        }

        void setValue(int v) {
            this.value = v;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof MutableInt && ((MutableInt) o).value == this.value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid any accidental state leakage
        set.clear();
    }

    static Stream<Arguments> nonNullElementsProvider() {
        return Stream.of(Arguments.of(Arrays.asList(5, 10), 15), Arguments.of(Arrays.asList(-3, -5), -8));
    }

    static Stream<Arguments> mixedNullProvider() {
        return Stream.of(Arguments.of(Arrays.asList(null, 7), 7), // second null ignored by Set semantics
        Arguments.of(Arrays.asList(null, null), 0));
    }

    @Test
    void testHashCodeEmptySet() {
        assertEquals(0, set.hashCode(), "Hash code of an empty set should be 0");
    }

    @Test
    void testHashCodeSingleElement() {
        String element = "test";
        set.add(element);
        // null contributes 0, so just the element's hash
        int expected = element.hashCode();
        assertEquals(expected, set.hashCode(), "Hash code should equal the element's hash code");
    }

    @Test
    void testHashCodeMultipleElementsIncludingNull() {
        // Elements: "a", null, "b"
        set.addAll(Arrays.asList("a", null, "b"));
        int expected = "a".hashCode() + 0 + "b".hashCode();
        assertEquals(expected, set.hashCode(), "Hash code should be sum of element hash codes, null counted as 0");
    }

    @Test
    void testHashCodeConsistencyAcrossCalls() {
        set.addAll(Arrays.asList(1, 2, 3));
        int first = set.hashCode();
        int second = set.hashCode();
        assertEquals(first, second, "Hash code should be consistent across multiple invocations");
    }

    @Test
    void testHashCodeOrderIndependence() {
        // Create two sets with same elements but added in different order
        ConcurrentSet<Integer> setA = new ConcurrentSet<>();
        setA.addAll(Arrays.asList(5, 1, 3));
        ConcurrentSet<Integer> setB = new ConcurrentSet<>();
        setB.addAll(Arrays.asList(3, 5, 1));
        assertEquals(setA.hashCode(), setB.hashCode(), "Hash code should be independent of insertion order because it sums element hash codes");
    }

    @Test
    void testHashCodeMatchesStandardSetImplementation() {
        // Verify that our implementation behaves like a typical Set hashCode (sum of element hash codes)
        Set<String> reference = new HashSet<>(Arrays.asList("x", "y", null));
        ConcurrentSet<String> concurrent = new ConcurrentSet<>();
        concurrent.addAll(reference);
        int expected = 0;
        for (String s : reference) {
            expected += (s == null) ? 0 : s.hashCode();
        }
        assertEquals(expected, concurrent.hashCode(), "ConcurrentSet hashCode should match manual sum of element hash codes");
    }

    // ---------- Empty Set ----------
    @Test
    void testEmptySetHashCode() {
        assertEquals(0, set.hashCode(), "A newly created ConcurrentSet with no elements should return a hash code of 0.");
    }

    // ---------- Set Containing Only Null ----------
    @Test
    void testSetContainingOnlyNull() {
        set.add(null);
        assertEquals(0, set.hashCode(), "A set that contains a single null element should treat the null as contributing 0.");
    }

    // ---------- Set Containing Only Non‑Null Elements ----------
    @ParameterizedTest(name = "{index}: elements={0}, expected={1}")
    @MethodSource("nonNullElementsProvider")
    void testSetContainingOnlyNonNullElements(List<Integer> elements, int expected) {
        set.addAll(elements);
        assertEquals(expected, set.hashCode(), "Hash code should be the sum of the integer hash codes.");
    }

    // ---------- Mixed Null and Non‑Null ----------
    @ParameterizedTest(name = "{index}: elements={0}, expected={1}")
    @MethodSource("mixedNullProvider")
    void testMixedNullAndNonNullElements(List<Object> elements, int expected) {
        set.addAll(elements);
        assertEquals(expected, set.hashCode(), "Null contributes 0, non‑null contributes its hashCode.");
    }

    // ---------- Custom objects Foo ----------
    @Test
    void testCustomObjectsFooTwoInstances() {
        Foo f1 = new Foo();
        Foo f2 = new Foo();
        set.add(f1);
        set.add(f2);
        assertEquals(84, set.hashCode(), "Two Foo instances each hash to 42, sum should be 84.");
    }

    @Test
    void testCustomObjectsFooAndNull() {
        Foo f = new Foo();
        set.add(f);
        set.add(null);
        assertEquals(42, set.hashCode(), "Foo (42) + null (0) = 42.");
    }

    // ---------- Duplicate Additions ----------
    @Test
    void testDuplicateAdditions() {
        set.add(4);
        set.add(4);
        // duplicates ignored
        set.add(4);
        set.add(8);
        assertEquals(12, set.hashCode(), "Duplicates are ignored; hash = 4 + 8 = 12.");
    }

    // ---------- HashCode Overflow ----------
    @Test
    void testOverflowWithMaxValueAndOne() {
        set.add(Integer.MAX_VALUE);
        set.add(1);
        // Expected overflow: MAX_VALUE + 1 == Integer.MIN_VALUE
        assertEquals(Integer.MIN_VALUE, set.hashCode(), "Overflow should wrap to Integer.MIN_VALUE.");
    }

    @Test
    void testOverflowWithMaxValueTwice() {
        set.add(Integer.MAX_VALUE);
        set.add(Integer.MAX_VALUE);
        // MAX_VALUE + MAX_VALUE == -2 (because 2*2147483647 = 4294967294 -> 0xFFFFFFFE -> -2)
        assertEquals(-2, set.hashCode(), "Overflow of two MAX_VALUEs should result in -2.");
    }

    // ---------- HashCode After Removals ----------
    @Test
    void testHashCodeAfterRemovalOfElement() {
        set.add(10);
        set.add(20);
        set.remove(10);
        assertEquals(20, set.hashCode(), "After removing 10, only 20 remains.");
    }

    @Test
    void testHashCodeAfterRemovalOfNull() {
        set.add(null);
        set.add(5);
        set.remove(null);
        assertEquals(5, set.hashCode(), "After removing null, only 5 remains.");
    }

    // ---------- Distinct objects with same hashCode ----------
    @Test
    void testDistinctObjectsSameHashCode() {
        Bar b1 = new Bar();
        Bar b2 = new Bar();
        set.add(b1);
        set.add(b2);
        assertEquals(14, set.hashCode(), "Two distinct Bar instances each hash to 7, sum = 14.");
    }

    // ---------- Mutating element ----------
    @Test
    void testMutableElementHashCodeChanges() {
        MutableInt mutable = new MutableInt(3);
        set.add(mutable);
        // Change internal state after insertion
        mutable.setValue(5);
        // The iterator will see the updated hashCode value
        assertEquals(5, set.hashCode(), "Mutable element's hashCode should reflect its current state.");
    }

    // ---------- Large set ----------
    @Test
    void testLargeSetHashCode() {
        for (int i = 1; i <= 1000; i++) {
            set.add(i);
        }
        // Sum of 1..1000 = 1000*1001/2 = 500500
        assertEquals(500_500, set.hashCode(), "Hash code of integers 1..1000 should equal arithmetic series sum.");
    }

    // ---------- Element hashCode throws RuntimeException ----------
    @Test
    void testElementHashCodeThrowsRuntimeException() {
        Object bad = new Object() {

            @Override
            public int hashCode() {
                throw new RuntimeException("boom");
            }
        };
        set.add(bad);
        RuntimeException ex = assertThrows(RuntimeException.class, set::hashCode, "RuntimeException from element's hashCode should propagate.");
        assertEquals("boom", ex.getMessage());
    }

    // ---------- Element hashCode throws Error ----------
    @Test
    void testElementHashCodeThrowsError() {
        Object bad = new Object() {

            @Override
            public int hashCode() {
                throw new AssertionError("assertion failed");
            }
        };
        set.add(bad);
        AssertionError err = assertThrows(AssertionError.class, set::hashCode, "Error from element's hashCode should propagate.");
        assertEquals("assertion failed", err.getMessage());
    }
}
