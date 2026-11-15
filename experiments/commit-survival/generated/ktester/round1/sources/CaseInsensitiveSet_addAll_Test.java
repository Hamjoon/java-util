package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.util.stream.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_addAll_Test {

    private CaseInsensitiveSet<String> set;

    /**
     * A collection whose iterator throws a {@link RuntimeException} when {@code next()}
     * is called. Used to verify that {@code addAll} propagates runtime exceptions from
     * the supplied collection.
     */
    private static class FaultyCollection<E> implements Collection<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new RuntimeException("Faulty iterator");
                }

                @Override
                public void remove() {
                    /* no‑op */
                }
            };
        }

        // The remaining methods are not used by addAll; they can be simple stubs.
        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Object[] toArray() {
            return new Object[0];
        }

        @Override
        public <T> T[] toArray(T[] a) {
            return a;
        }

        @Override
        public boolean add(E e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test to avoid cross‑test contamination
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent
        set = null;
    }

    /**
     * Supplies a stream of arguments for the {@link #addAll_branchCoverage} test.
     * Each argument contains:
     * <ul>
     *   <li>the collection to add</li>
     *   <li>the expected boolean result (or {@code null} when an exception is expected)</li>
     *   <li>the expected exception class (or {@code null} when no exception)</li>
     * </ul>
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// empty collection → false
        Arguments.of(Collections.emptyList(), false, null), // new elements → true
        Arguments.of(Arrays.asList("a", "b", "c"), true, null), // case‑variant elements are treated as distinct keys (raw key stored) → true
        Arguments.of(Arrays.asList("a", "A"), true, null), // duplicate of existing elements → false
        Arguments.of(Arrays.asList("x", "y"), false, null), // null collection → NPE
        Arguments.of(null, null, NullPointerException.class), // collection containing null and a new string → true (null key allowed for String set)
        Arguments.of(Arrays.asList(null, "test"), true, null));
    }

    /**
     * Additional scenarios that are not covered by the first parameterised test.
     */
    private static Stream<Arguments> additionalScenariosProvider() {
        return Stream.of(// duplicate elements only, set initially empty → true (first unique element adds)
        Arguments.of(Arrays.asList("dup", "dup"), true, null), // duplicate element already present → false
        Arguments.of(Arrays.asList("x", "x"), false, null), // mix of existing and new elements → true
        Arguments.of(Arrays.asList("a", "b", "c"), true, null), // collection with only null, empty set → true
        Arguments.of(Collections.singletonList(null), true, null), // collection with only null, null already present → false
        Arguments.of(Collections.singletonList(null), false, null), // case‑variant where existing element already present → false
        Arguments.of(Arrays.asList("a", "A"), false, null), // HashSet containing case‑variant strings, empty set → true
        Arguments.of(new HashSet<>(Arrays.asList("a", "A")), true, null), // Faulty collection that throws during iteration → RuntimeException
        Arguments.of(new FaultyCollection<>(), null, RuntimeException.class));
    }

    @Test
    void addAllAddsNewElementsAndReturnsTrue() {
        // given
        Collection<String> toAdd = Arrays.asList("Apple", "Banana", "Cherry");
        // when
        boolean changed = set.addAll(toAdd);
        // then
        assertTrue(changed, "addAll should report a change when new elements are added");
        assertEquals(3, set.size(), "Set size should reflect all added elements");
        assertTrue(set.containsAll(toAdd), "Set should contain all newly added elements");
    }

    @Test
    void addAllWithAllExistingElementsReturnsFalse() {
        // given
        set.addAll(Arrays.asList("Dog", "Elephant"));
        // different case but should be considered equal
        Collection<String> duplicate = Arrays.asList("dog", "elephant");
        // when
        boolean changed = set.addAll(duplicate);
        // then
        assertFalse(changed, "addAll should return false when no new elements are introduced");
        assertEquals(2, set.size(), "Set size must remain unchanged");
    }

    @Test
    void addAllIsCaseInsensitiveButPreservesOriginalCase() {
        // given
        set.addAll(Arrays.asList("Foo", "Bar"));
        Collection<String> mixedCase = Arrays.asList("foo", "BAR", "Baz");
        // when
        boolean changed = set.addAll(mixedCase);
        // then
        assertTrue(changed, "Adding a genuinely new element ('Baz') should cause a change");
        assertEquals(3, set.size(), "Set should now contain three distinct logical elements");
        // The original case of the first occurrence must be retained
        assertTrue(set.contains("Foo"));
        assertTrue(set.contains("Bar"));
        assertTrue(set.contains("Baz"));
        // The lower‑case variants should not appear as separate entries
        assertFalse(set.contains("foo"));
        assertFalse(set.contains("BAR"));
    }

    @Test
    void addAllWithEmptyCollectionReturnsFalseAndLeavesSetUnchanged() {
        // given
        set.addAll(Arrays.asList("One", "Two"));
        Collection<String> empty = Collections.emptyList();
        // when
        boolean changed = set.addAll(empty);
        // then
        assertFalse(changed, "Adding an empty collection must not modify the set");
        assertEquals(2, set.size(), "Set size should remain the same after adding empty collection");
    }

    @Test
    void addAllNullCollectionThrowsNullPointerException() {
        // given
        Collection<String> nullColl = null;
        // when / then
        assertThrows(NullPointerException.class, () -> set.addAll(nullColl), "Passing null to addAll should result in NPE");
    }

    @ParameterizedTest(name = "[{index}] addAll({0}) → expected: {1} / exception: {2}")
    @MethodSource("branchCoverageProvider")
    void addAll_branchCoverage(Collection<String> c, Boolean expectedResult, Class<? extends Throwable> expectedException) {
        if (expectedException != null) {
            // Expecting an exception (e.g., NPE)
            assertThrows(expectedException, () -> set.addAll(c));
        } else {
            // Normal execution – verify the boolean result and the size change
            int before = set.size();
            boolean changed = set.addAll(c);
            assertEquals(expectedResult, changed, "Unexpected boolean result from addAll");
            // Verify size semantics: changed == true → size increased, else unchanged
            int after = set.size();
            if (changed) {
                assertTrue(after > before, "Size should have increased when addAll returns true");
            } else {
                assertEquals(before, after, "Size must stay the same when addAll returns false");
            }
        }
    }

    @ParameterizedTest(name = "[{index}] addAll({0}) → expected: {1} / exception: {2}")
    @MethodSource("additionalScenariosProvider")
    void addAll_additionalScenarios(Collection<String> c, Boolean expectedResult, Class<? extends Throwable> expectedException) {
        // Prepare the set for the cases that require prior content
        if (expectedResult != null && !expectedResult && c != null && c.contains(null) && set.size() == 0) {
            // For the "null already present" case we need to add null first
            set.addAll(Collections.singletonList(null));
        }
        if (expectedResult != null && !expectedResult && c != null && c.contains("a") && set.size() == 0) {
            // For the "case‑variant already present" case we need to add "a" first
            set.addAll(Collections.singletonList("a"));
        }
        if (expectedException != null) {
            assertThrows(expectedException, () -> set.addAll(c));
        } else {
            int before = set.size();
            boolean changed = set.addAll(c);
            assertEquals(expectedResult, changed, "Unexpected boolean result from addAll");
            int after = set.size();
            if (changed) {
                assertTrue(after > before, "Size should increase when addAll returns true");
            } else {
                assertEquals(before, after, "Size must stay unchanged when addAll returns false");
            }
        }
    }

    @Test
    void addAll_withNullElementInIntegerSet_throwsNullPointerException() {
        // The Integer version uses a map implementation that does NOT allow null keys
        CaseInsensitiveSet<Integer> intSet = new CaseInsensitiveSet<>();
        Collection<Integer> withNull = Collections.singletonList(null);
        assertThrows(NullPointerException.class, () -> intSet.addAll(withNull), "Adding null to an Integer set (backed by ConcurrentSkipListMap) should throw NPE");
    }

    @Test
    void addAll_withIncompatibleType_throwsClassCastException() {
        // Attempt to add an Integer to a Set<String>
        // raw collection to bypass compile‑time generics
        Collection raw = Arrays.asList(123);
        assertThrows(ClassCastException.class, () -> set.addAll(raw), "Adding an element of the wrong type should cause a ClassCastException");
    }
}
