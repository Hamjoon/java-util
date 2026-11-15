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
class ConcurrentSet_hashCode_Test {

    /**
     * Instance used by most tests – created fresh for each test method.
     */
    private ConcurrentSet<Integer> intSet;

    /**
     * Instance that holds a mixture of objects, including null, for more exhaustive checks.
     */
    private ConcurrentSet<Object> mixedSet;

    class Dummy {

        private final int id;

        Dummy(int id) {
            this.id = id;
        }

        @Override
        public int hashCode() {
            return id * 31;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Dummy && ((Dummy) o).id == id;
        }
    }

    /**
     * Helper class whose {@code hashCode()} deliberately throws a {@link RuntimeException}.
     */
    static class BadHash {

        @Override
        public int hashCode() {
            throw new RuntimeException("BadHash triggered");
        }
    }

    /**
     * Helper class whose {@code hashCode()} deliberately throws a {@link NullPointerException}.
     */
    static class NullHash {

        @Override
        public int hashCode() {
            throw new NullPointerException("NullHash triggered");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        mixedSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to help the garbage collector; not strictly required.
        intSet.clear();
        mixedSet.clear();
    }

    private static Stream<Arguments> basicHashCodeProvider() {
        return Stream.of(Arguments.of("Empty set should return hash code 0 because the loop does not execute.", Collections.emptyList(), 0), Arguments.of("Set containing integers 1,2,3 should sum their hash codes (1+2+3).", Arrays.asList(1, 2, 3), 1 + 2 + 3), Arguments.of("String hash codes are \"a\".hashCode()=97, \"b\".hashCode()=98, sum = 195.", Arrays.asList("a", "b"), "a".hashCode() + "b".hashCode()), Arguments.of("Null contributes 0, \"c\".hashCode()=99, total 99.", Arrays.asList(null, "c"), 0 + "c".hashCode()));
    }

    @Test
    void hashCodeEmptySet_isZero() {
        // An empty set should produce a hash code of 0 according to the implementation.
        assertEquals(0, intSet.hashCode(), "Hash code of an empty ConcurrentSet must be 0");
    }

    @Test
    void hashCodeSingleElement() {
        intSet.add(42);
        // Integer hashCode returns the int value itself.
        assertEquals(42, intSet.hashCode(), "Hash code should equal the element's hash code when only one element is present");
    }

    @Test
    void hashCodeMultipleElements_sumOfHashCodes() {
        // Use a deterministic collection of values.
        Set<Integer> source = new HashSet<>(Arrays.asList(1, 2, 3, 5));
        intSet.addAll(source);
        // Expected sum: 1 + 2 + 3 + 5 = 11
        int expected = source.stream().mapToInt(Integer::hashCode).sum();
        assertEquals(expected, intSet.hashCode(), "Hash code must be the sum of the hash codes of all elements in the set");
    }

    @Test
    void hashCodeIgnoresNullElements() {
        // Add a null and a non‑null element.
        mixedSet.add(null);
        mixedSet.add("abc");
        // "abc".hashCode() is a known value; null contributes 0.
        int expected = "abc".hashCode();
        assertEquals(expected, mixedSet.hashCode(), "Null elements must contribute 0 to the hash code");
    }

    @Test
    void hashCodeConsistentAcrossInvocations() {
        intSet.addAll(Arrays.asList(7, 8, 9));
        int first = intSet.hashCode();
        int second = intSet.hashCode();
        assertEquals(first, second, "hashCode must be consistent when the set is not modified");
    }

    @Test
    void hashCodeReflectsModification() {
        intSet.addAll(Arrays.asList(10, 20));
        int before = intSet.hashCode();
        // modify the set
        intSet.add(30);
        int after = intSet.hashCode();
        assertNotEquals(before, after, "hashCode should change when the set's contents change");
    }

    @Test
    void hashCodeWithCustomObjects() {
        // Custom objects with deterministic hashCode values.
        class Dummy {

            private final int id;

            Dummy(int id) {
                this.id = id;
            }

            @Override
            public int hashCode() {
                return id * 31;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof Dummy && ((Dummy) o).id == id;
            }
        }
        Dummy d1 = new Dummy(1);
        Dummy d2 = new Dummy(2);
        ConcurrentSet<Dummy> customSet = new ConcurrentSet<>();
        customSet.addAll(Arrays.asList(d1, d2));
        int expected = d1.hashCode() + d2.hashCode();
        assertEquals(expected, customSet.hashCode(), "Hash code must correctly sum custom objects' hash codes");
    }

    /**
     * Basic functionality test cases:
     * - empty set → 0
     * - [1,2,3] → 6
     * - ["a","b"] → 195 (97 + 98)
     * - [null,"c"] → 99 (null contributes 0, "c".hashCode() = 99)
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("basicHashCodeProvider")
    void hashCode_basicFunctionality(String description, Collection<?> elements, int expected) {
        // Use a raw ConcurrentSet to hold heterogeneous elements.
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.addAll(elements);
        assertEquals(expected, set.hashCode(), () -> "Failed basic case: " + description);
    }

    @Test
    void hashCode_propagatesRuntimeExceptionFromElement() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.add(new BadHash());
        RuntimeException ex = assertThrows(RuntimeException.class, set::hashCode, "RuntimeException from an element's hashCode() must be propagated");
        assertEquals("BadHash triggered", ex.getMessage());
    }

    @Test
    void hashCode_propagatesNullPointerExceptionFromElement() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        set.add(new NullHash());
        NullPointerException ex = assertThrows(NullPointerException.class, set::hashCode, "NullPointerException from an element's hashCode() must be propagated");
        assertEquals("NullHash triggered", ex.getMessage());
    }
}
