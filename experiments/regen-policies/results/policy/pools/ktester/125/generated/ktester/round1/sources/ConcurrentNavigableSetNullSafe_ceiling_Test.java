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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_ceiling_Test {

    /**
     * Set used in the majority of tests – created with the default constructor.
     */
    private NavigableSet<String> naturalSet;

    /**
     * Set created with a custom comparator (by string length).
     */
    private NavigableSet<String> lengthComparatorSet;

    @BeforeEach
    void setupBeforeEach() {
        // Natural‑ordering set
        naturalSet = new ConcurrentNavigableSetNullSafe<>();
        // Comparator that orders strings by length, treating {@code null} as length 0.
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        lengthComparatorSet = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a clean set.
        naturalSet.clear();
        lengthComparatorSet.clear();
    }

    /**
     * Provides arguments for the {@link #ceiling_BranchCoverage(String, Object, Object)} test.
     *
     * Each {@code Arguments} consists of:
     * <ul>
     *   <li>a description (used as the test name),</li>
     *   <li>the query element {@code e},</li>
     *   <li>the expected result (either a value or {@code null}).</li>
     * </ul>
     */
    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// 1. argument null, set contains null → null
        Arguments.of("null argument, set contains null", null, null), // 2. argument null, set does NOT contain null → smallest element
        Arguments.of("null argument, set without null", null, "alpha"), // 3. argument non‑null, no element ≥ argument → null
        Arguments.of("non‑null argument with no ceiling", "gamma", null), // 4. argument present in set → same element
        Arguments.of("argument present in set", "beta", "beta"), // 5. argument not present but higher element exists → next higher
        Arguments.of("argument not present, higher exists", "beta", "delta"));
    }

    private static Stream<Arguments> additionalCasesProvider() {
        return Stream.of(// Empty set & null argument → null
        Arguments.of("empty set, null argument", Collections.emptyList(), null, null), // Empty set & non‑null argument → null
        Arguments.of("empty set, non‑null argument", Collections.emptyList(), "any", null), // Set contains only masked null, non‑null argument → null
        Arguments.of("set only null sentinel, non‑null argument", Collections.singletonList(null), "zeta", null), // Set ["beta","delta"], argument "alpha" → "beta"
        Arguments.of("argument less than smallest element", Arrays.asList("beta", "delta"), "alpha", "beta"), // Set ["beta","delta"], argument "epsilon" → null
        Arguments.of("argument greater than greatest element", Arrays.asList("beta", "delta"), "epsilon", null), // Reverse comparator for Integer, set [1,3,5] (ordered 5,3,1), ceiling(4) → 3
        Arguments.of("reverse integer comparator, ceiling 4", Arrays.asList(1, 3, 5), 4, 3), // Type mismatch: set of Strings, query Integer → ClassCastException
        Arguments.of("type mismatch, expect ClassCastException", Arrays.asList("a", "b"), 42, ClassCastException.class));
    }

    @Test
    void ceiling_NaturalOrder_ElementPresent() {
        // given
        naturalSet.addAll(Arrays.asList("apple", "banana", "cherry", null));
        // when / then
        // exact match
        assertEquals("banana", naturalSet.ceiling("banana"));
        // next higher element
        assertEquals("banana", naturalSet.ceiling("bana"));
        // higher than "blue"
        assertEquals("cherry", naturalSet.ceiling("blue"));
        // null is the smallest element
        assertEquals(null, naturalSet.ceiling(null));
    }

    @Test
    void ceiling_NaturalOrder_ElementGreaterThanAll() {
        // given
        naturalSet.addAll(Arrays.asList("apple", "banana", "cherry", null));
        // when
        String result = naturalSet.ceiling("zoo");
        // then
        assertNull(result, "Ceiling of an element greater than any present should be null");
    }

    @Test
    void ceiling_NaturalOrder_EmptySet() {
        // given – naturalSet is empty
        // when
        String result = naturalSet.ceiling("any");
        // then
        assertNull(result, "Ceiling on an empty set must return null");
    }

    @Test
    void ceiling_LengthComparator_NullAndLengths() {
        // given
        lengthComparatorSet.addAll(Arrays.asList("a", "bb", "ccc", null));
        // when / then
        // null (length 0) is the smallest, so its ceiling is null itself
        assertNull(lengthComparatorSet.ceiling(null));
        // ceiling of empty string (length 0) should be the null sentinel
        assertNull(lengthComparatorSet.ceiling(""));
        // ceiling of "aa" (length 2) should be "bb" (length 2)
        assertEquals("bb", lengthComparatorSet.ceiling("aa"));
        // ceiling of "dddd" (length 4) should be "ccc" (length 3) is too short,
        // so the next higher length is null – but there is none, thus null
        assertNull(lengthComparatorSet.ceiling("dddd"));
    }

    @Test
    void ceiling_LengthComparator_ElementGreaterThanAll() {
        // given
        lengthComparatorSet.addAll(Arrays.asList("a", "bb", null));
        // when
        // length 4, greater than any present
        String result = lengthComparatorSet.ceiling("dddd");
        // then
        assertNull(result);
    }

    @Test
    void ceiling_LengthComparator_EmptySet() {
        // given – lengthComparatorSet is empty
        // when
        String result = lengthComparatorSet.ceiling("any");
        // then
        assertNull(result);
    }

    @Test
    void ceiling_NullElementInSet_AndQueryNull() {
        // given
        naturalSet.addAll(Arrays.asList(null, "apple"));
        // when
        String result = naturalSet.ceiling(null);
        // then
        assertNull(result, "When null is present, its own ceiling must be null");
    }

    @Test
    void ceiling_QueryNullWhenSetContainsOnlyNonNulls() {
        // given
        naturalSet.addAll(Arrays.asList("apple", "banana"));
        // when
        String result = naturalSet.ceiling(null);
        // then
        // The sentinel representing null is the smallest element, so the
        // ceiling of null should be the first (smallest) real element.
        assertEquals("apple", result);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("branchCoverageProvider")
    void ceiling_BranchCoverage(String description, Object query, Object expected) throws Exception {
        // Build the appropriate set for each scenario
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        switch(description) {
            case "null argument, set contains null":
                set.add(null);
                break;
            case "null argument, set without null":
                set.add("alpha");
                break;
            case "non‑null argument with no ceiling":
                set.addAll(Arrays.asList("alpha", "beta"));
                break;
            case "argument present in set":
                set.addAll(Arrays.asList("beta", "delta"));
                break;
            case "argument not present, higher exists":
                set.addAll(Arrays.asList("alpha", "delta"));
                break;
            default:
                fail("Unexpected description: " + description);
        }
        @SuppressWarnings("unchecked")
        String result = (String) set.ceiling((String) query);
        assertEquals(expected, result, description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("additionalCasesProvider")
    void ceiling_AdditionalCases(String description, List<?> initialElements, Object query, Object expected) throws Exception {
        // Determine the element type from the initialElements list
        if (initialElements.isEmpty() || initialElements.get(0) instanceof String) {
            @SuppressWarnings("unchecked")
            ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
            //noinspection unchecked
            set.addAll((Collection<String>) initialElements);
            if (expected instanceof Class) {
                // Expecting an exception
                assertThrows((Class<? extends Throwable>) expected, () -> set.ceiling((String) query), description);
            } else {
                String result = set.ceiling((String) query);
                assertEquals(expected, result, description);
            }
        } else if (initialElements.get(0) instanceof Integer) {
            @SuppressWarnings("unchecked")
            Comparator<Integer> reverse = Comparator.reverseOrder();
            @SuppressWarnings("unchecked")
            ConcurrentNavigableSetNullSafe<Integer> set = new ConcurrentNavigableSetNullSafe<>(reverse);
            //noinspection unchecked
            set.addAll((Collection<Integer>) initialElements);
            if (expected instanceof Class) {
                assertThrows((Class<? extends Throwable>) expected, () -> set.ceiling((Integer) query), description);
            } else {
                Integer result = set.ceiling((Integer) query);
                assertEquals(expected, result, description);
            }
        } else {
            fail("Unsupported element type in test data");
        }
    }

    @Test
    void ceiling_NullInternalSet_ThrowsNullPointerException() throws Exception {
        // given – a normal set with some data
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        set.add("any");
        // Force internalSet to null via reflection
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(set, null);
        // when / then
        assertThrows(NullPointerException.class, () -> set.ceiling("any"), "Calling ceiling when internalSet is null must throw NPE");
    }

    @Test
    void ceiling_InternalSetThrowsIllegalStateException() throws Exception {
        // given – a mock NavigableSet that throws IllegalStateException on ceiling()
        @SuppressWarnings("unchecked")
        NavigableSet<Object> mockSet = mock(NavigableSet.class);
        when(mockSet.ceiling(any())).thenThrow(new IllegalStateException("boom"));
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        // Inject the mock via reflection
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(set, mockSet);
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.ceiling("any"), "IllegalStateException from internalSet must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void ceiling_InternalSetThrowsUnsupportedOperationException() throws Exception {
        // given – a custom NavigableSet implementation that always throws UOE
        NavigableSet<Object> throwingSet = new NavigableSet<Object>() {

            @Override
            public Object ceiling(Object e) {
                throw new UnsupportedOperationException("not supported");
            }

            // The rest of the methods are not needed for this test; they can delegate to an empty set.
            @Override
            public Comparator<? super Object> comparator() {
                return null;
            }

            @Override
            public Object first() {
                return null;
            }

            @Override
            public Object last() {
                return null;
            }

            @Override
            public Object lower(Object e) {
                return null;
            }

            @Override
            public Object floor(Object e) {
                return null;
            }

            @Override
            public Object higher(Object e) {
                return null;
            }

            @Override
            public Object pollFirst() {
                return null;
            }

            @Override
            public Object pollLast() {
                return null;
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<Object> iterator() {
                return Collections.emptyIterator();
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
            public boolean add(Object o) {
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
            public boolean addAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
            }

            @Override
            public Object lower(Object e) {
                return null;
            }

            @Override
            public Object floor(Object e) {
                return null;
            }

            @Override
            public Object higher(Object e) {
                return null;
            }

            @Override
            public Object ceiling(Object e) {
                throw new UnsupportedOperationException("not supported");
            }

            @Override
            public Object pollFirst() {
                return null;
            }

            @Override
            public Object pollLast() {
                return null;
            }

            @Override
            public NavigableSet<Object> descendingSet() {
                return this;
            }

            @Override
            public Iterator<Object> descendingIterator() {
                return iterator();
            }

            @Override
            public NavigableSet<Object> subSet(Object fromElement, boolean fromInclusive, Object toElement, boolean toInclusive) {
                return this;
            }

            @Override
            public NavigableSet<Object> headSet(Object toElement, boolean inclusive) {
                return this;
            }

            @Override
            public NavigableSet<Object> tailSet(Object fromElement, boolean inclusive) {
                return this;
            }

            @Override
            public SortedSet<Object> subSet(Object fromElement, Object toElement) {
                return this;
            }

            @Override
            public SortedSet<Object> headSet(Object toElement) {
                return this;
            }

            @Override
            public SortedSet<Object> tailSet(Object fromElement) {
                return this;
            }
        };
        ConcurrentNavigableSetNullSafe<String> set = new ConcurrentNavigableSetNullSafe<>();
        Field field = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        field.setAccessible(true);
        field.set(set, throwingSet);
        // when / then
        assertThrows(UnsupportedOperationException.class, () -> set.ceiling("any"), "UnsupportedOperationException from internalSet must propagate");
    }
}
