package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_hashCode_Test {

    class FixedHash {

        private final int value;

        FixedHash(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof FixedHash && ((FixedHash) o).value == value;
        }
    }

    class FixedHash {

        private final int value;

        FixedHash(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof FixedHash && ((FixedHash) o).value == value;
        }
    }

    /**
     * Supplies the test data for {@link #hashCodeParameterized(String, List, int)}.
     *
     * <p>The data mirrors the JSON test cases provided in the specification.</p>
     */
    static Stream<Arguments> hashCodeTestCases() {
        // Helper custom objects with deterministic hashCode values
        class FixedHash {

            private final int value;

            FixedHash(int value) {
                this.value = value;
            }

            @Override
            public int hashCode() {
                return value;
            }

            @Override
            public boolean equals(Object o) {
                return o instanceof FixedHash && ((FixedHash) o).value == value;
            }
        }
        return Stream.of(// 1. Empty ConcurrentSet – expected 0
        Arguments.of("Empty ConcurrentSet – the loop does not execute, so the returned hash code should be 0.", Collections.emptyList(), 0), // 2. Set contains a single null element – expected 0
        Arguments.of("ConcurrentSet contains a single null element. The ternary expression yields 0 for the null item, therefore the total hash code remains 0.", Collections.singletonList(null), 0), // 3. Single non‑null Integer value 5 – expected 5
        Arguments.of("ConcurrentSet contains a single non‑null Integer value 5. Integer.hashCode() returns the int value itself, so the method should return 5.", Collections.singletonList(5), 5), // 4. Three Integer values 1, 2 and 3 – expected 6
        Arguments.of("ConcurrentSet contains three Integer values 1, 2 and 3. The sum of their hash codes is 1+2+3 = 6.", Arrays.asList(1, 2, 3), 6), // 5. Null element and Integer 7 – expected 7
        Arguments.of("ConcurrentSet contains a null element and an Integer 7. Null contributes 0, Integer 7 contributes 7, total = 7.", Arrays.asList(null, 7), 7), // 6. Custom object whose hashCode() returns 42 – expected 42
        Arguments.of("ConcurrentSet contains a single custom object whose hashCode() method is overridden to return 42. The method should return 42.", Collections.singletonList(new FixedHash(42)), 42), // 7. Duplicate Integer 5 added twice – expected 5 (duplicate ignored)
        Arguments.of("ConcurrentSet attempts to add duplicate Integer 5 twice. As a Set, only one instance is stored, so the hash code is 5.", Arrays.asList(5, 5), 5), // 8. Custom object whose hashCode() returns -3 – expected -3
        Arguments.of("ConcurrentSet contains a custom object whose hashCode() returns -3. The method should correctly handle negative hash codes and return -3.", Collections.singletonList(new FixedHash(-3)), -3));
    }

    /**
     * Verify that the hashCode of an empty ConcurrentSet is 0,
     * which matches the contract of the implementation (sum of element hash codes).
     */
    @Test
    void hashCodeEmptySet() {
        ConcurrentSet<Integer> empty = new ConcurrentSet<>();
        assertEquals(0, empty.hashCode(), "Hash code of an empty set should be 0");
    }

    /**
     * Verify that hashCode correctly aggregates the hash codes of non‑null elements.
     */
    @Test
    void hashCodeWithNonNullElements() {
        Set<Integer> source = new HashSet<>(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> set = new ConcurrentSet<>(source);
        int expected = source.stream().mapToInt(i -> i == null ? 0 : i.hashCode()).sum();
        assertEquals(expected, set.hashCode(), "Hash code should be the sum of the hash codes of the contained elements");
    }

    /**
     * Verify that null elements contribute 0 to the hash code, as defined in the implementation.
     */
    @Test
    void hashCodeWithNullElements() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", null, "b"));
        // Expected sum: "a".hashCode() + 0 (null) + "b".hashCode()
        int expected = "a".hashCode() + "b".hashCode();
        assertEquals(expected, set.hashCode(), "Null elements must contribute 0 to the hash code");
    }

    /**
     * Verify that two sets with the same logical elements (including null) have identical hash codes,
     * even when they are constructed via different constructors.
     */
    @Test
    void hashCodeConsistencyAcrossConstructors() {
        // Construct via collection constructor
        ConcurrentSet<String> viaCollection = new ConcurrentSet<>(Arrays.asList("x", null, "y"));
        // Construct via default constructor and add elements manually
        ConcurrentSet<String> viaAdd = new ConcurrentSet<>();
        viaAdd.add("x");
        viaAdd.add(null);
        viaAdd.add("y");
        assertEquals(viaCollection.hashCode(), viaAdd.hashCode(), "Hash codes should be equal for sets with identical contents regardless of construction path");
    }

    /**
     * Verify that the hash code changes when the set's contents change.
     */
    @Test
    void hashCodeChangesWhenContentChanges() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(10);
        int hashAfterFirstAdd = set.hashCode();
        set.add(20);
        int hashAfterSecondAdd = set.hashCode();
        assertNotEquals(hashAfterFirstAdd, hashAfterSecondAdd, "Hash code must change when new elements are added");
        set.remove(10);
        int hashAfterRemove = set.hashCode();
        assertNotEquals(hashAfterSecondAdd, hashAfterRemove, "Hash code must change when elements are removed");
    }

    /**
     * Parameterized test that covers the JSON‑driven scenarios.
     *
     * <p>Each argument consists of:
     * <ul>
     *   <li>a description (for readability in test reports),</li>
     *   <li>a list of elements to insert into the set (may contain {@code null}),</li>
     *   <li>the expected hash code according to the contract (sum of individual hash codes, {@code null}=0).</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("hashCodeTestCases")
    void hashCodeParameterized(String description, List<Object> elements, int expectedHash) {
        // Build the set using the default constructor and add each element.
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        for (Object e : elements) {
            // The add method returns false for duplicates – we deliberately add each element once.
            set.add(e);
        }
        assertEquals(expectedHash, set.hashCode(), () -> "Hash code mismatch for case: " + description);
    }
}
