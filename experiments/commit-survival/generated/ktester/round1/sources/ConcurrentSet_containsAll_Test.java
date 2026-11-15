package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Constructor;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_containsAll_Test {

    private ConcurrentSet<Integer> set;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh set for each test (used by the original simple tests)
        set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(1, 2, 3, null));
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the set to avoid cross‑test contamination
        set.clear();
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// empty collection -> true
        Arguments.of(createSetFrom(Arrays.asList(1, 2, 3)), Collections.emptyList(), true, "An empty collection should always be considered contained, regardless of the set contents."), // all present
        Arguments.of(createSetFrom(Arrays.asList("a", "b", "c")), Arrays.asList("a", "b"), true, "All elements of the argument collection are present in the set."), // missing element
        Arguments.of(createSetFrom(Arrays.asList(10, 20, 30)), Arrays.asList(10, 99), false, "Second element (99) is not in the set, should return false."), // null present
        Arguments.of(createSetFrom(Arrays.asList("x", null, "y")), Collections.singletonList(null), true, "Set contains null and collection asks for null."), // null absent
        Arguments.of(createSetFrom(Arrays.asList(5, 6, 7)), Arrays.asList(5, null, 6), false, "Collection contains null which is not present in the set."), // empty set, non‑empty collection
        Arguments.of(createSetFrom(Collections.emptySet()), Collections.singletonList(1), false, "Set is empty while collection has one element."), // large collection with many duplicates
        Arguments.of(createSetFrom(Arrays.asList(1, 2, 3, 4, 5)), Collections.nCopies(1000, 3), true, "Large collection with many duplicate entries, all present."), // anonymous AbstractCollection
        Arguments.of(createSetFrom(Arrays.asList(1, 2, 3)), new AbstractCollection<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return Arrays.asList(1, 2, 3).iterator();
            }

            @Override
            public int size() {
                return 3;
            }
        }, true, "Anonymous AbstractCollection verifies enhanced‑for works with any Iterable."));
    }

    @Test
    void containsAll_AllElementsPresent_ReturnsTrue() {
        // given
        Collection<Integer> toCheck = Arrays.asList(1, 2, 3);
        // when
        boolean result = set.containsAll(toCheck);
        // then
        assertTrue(result, "containsAll should return true when every element is present");
    }

    @Test
    void containsAll_MissingElement_ReturnsFalse() {
        // given
        // 4 is not in the set
        Collection<Integer> toCheck = Arrays.asList(1, 4);
        // when
        boolean result = set.containsAll(toCheck);
        // then
        assertFalse(result, "containsAll should return false when at least one element is missing");
    }

    @Test
    void containsAll_EmptyCollection_ReturnsTrue() {
        // given
        Collection<Integer> empty = Collections.emptyList();
        // when
        boolean result = set.containsAll(empty);
        // then
        assertTrue(result, "containsAll should return true for an empty collection (vacuous truth)");
    }

    @Test
    void containsAll_WithNullElement_Present_ReturnsTrue() {
        // given
        Collection<Integer> withNull = Arrays.asList(null, 2);
        // when
        boolean result = set.containsAll(withNull);
        // then
        assertTrue(result, "containsAll should correctly handle null elements when they are present");
    }

    @Test
    void containsAll_WithNullElement_Absent_ReturnsFalse() {
        // given
        // ensure null is not in the set
        set.remove(null);
        Collection<Integer> withNull = Arrays.asList(null, 1);
        // when
        boolean result = set.containsAll(withNull);
        // then
        assertFalse(result, "containsAll should return false when null is required but not present");
    }

    @Test
    void containsAll_DifferentCollectionImplementation_ReturnsTrue() {
        // given
        Set<Integer> hashSet = new HashSet<>(Arrays.asList(1, 2, 3));
        // when
        boolean result = set.containsAll(hashSet);
        // then
        assertTrue(result, "containsAll should work with any Collection implementation (HashSet in this case)");
    }

    @Test
    void containsAll_ModifyOriginalCollectionAfterCall_DoesNotAffectResult() {
        // given
        List<Integer> mutable = new ArrayList<>(Arrays.asList(1, 2));
        boolean before = set.containsAll(mutable);
        // mutate the collection after the call
        mutable.add(99);
        // when
        boolean after = set.containsAll(mutable);
        // then
        assertTrue(before, "Initial call should succeed because 1 and 2 are present");
        assertFalse(after, "Subsequent call should fail because 99 is not present");
    }

    /**
     * Helper that tries to instantiate {@code ConcurrentSet} with a collection
     * constructor (if present).  If such a constructor does not exist, it falls
     * back to the no‑arg constructor and populates the set via {@code addAll}.
     */
    @SuppressWarnings("unchecked")
    private static <T> ConcurrentSet<T> createSetFrom(Collection<T> source) {
        try {
            Constructor<ConcurrentSet> ctor = ConcurrentSet.class.getConstructor(Collection.class);
            return (ConcurrentSet<T>) ctor.newInstance(source);
        } catch (Exception ignored) {
            // No (Collection) constructor – use default and addAll
            ConcurrentSet<T> s = new ConcurrentSet<>();
            s.addAll(source);
            return s;
        }
    }

    @ParameterizedTest(name = "[{index}] {3}")
    @MethodSource("basicFunctionalityProvider")
    void containsAll_BasicFunctionality(ConcurrentSet<?> setUnderTest, Collection<?> collection, boolean expected, String description) {
        assertEquals(expected, setUnderTest.containsAll(collection), () -> "Failed: " + description);
    }

    @Test
    void containsAll_NullCollection_ThrowsNullPointerException() {
        ConcurrentSet<Integer> s = createSetFrom(Arrays.asList(1, 2, 3));
        assertThrows(NullPointerException.class, () -> s.containsAll(null), "Passing a null collection should cause NullPointerException");
    }

    @Test
    void containsAll_MixedElementTypes_ReturnsFalse() {
        // Set contains Number types, collection mixes a String
        ConcurrentSet<Number> numberSet = createSetFrom(Arrays.asList(1, 2L, 3.0));
        Collection<Object> mixed = Arrays.asList(1, "two", 3.0);
        assertFalse(numberSet.containsAll(mixed), "Collection contains an element of an incompatible type; should return false");
    }

    @Test
    void containsAll_MultipleNulls_AllPresent_ReturnsTrue() {
        ConcurrentSet<String> s = createSetFrom(Arrays.asList(null, "a", "b"));
        Collection<String> col = Arrays.asList(null, null, "a");
        assertTrue(s.containsAll(col), "Set contains null; collection asks for several nulls plus a present non‑null");
    }

    @Test
    void containsAll_MultipleNulls_NonePresent_ReturnsFalse() {
        ConcurrentSet<String> s = createSetFrom(Arrays.asList("a", "b"));
        Collection<String> col = Arrays.asList(null, null);
        assertFalse(s.containsAll(col), "Collection contains only nulls but set does not contain null");
    }

    @Test
    void containsAll_CollectionIsAnotherConcurrentSetWithSameElements_ReturnsTrue() {
        ConcurrentSet<Integer> s1 = createSetFrom(Arrays.asList(4, 5, 6));
        ConcurrentSet<Integer> s2 = createSetFrom(Arrays.asList(4, 5, 6));
        assertTrue(s1.containsAll(s2), "Argument collection is another ConcurrentSet with exactly the same elements");
    }

    @Test
    void containsAll_HashSet_AllElementsPresent_ReturnsTrue() {
        ConcurrentSet<String> s = createSetFrom(Arrays.asList("x", "y", "z"));
        Set<String> hashSet = new HashSet<>(Arrays.asList("x", "y"));
        assertTrue(s.containsAll(hashSet), "All elements of the HashSet are present in the ConcurrentSet");
    }

    @Test
    void containsAll_HashSet_MissingElement_ReturnsFalse() {
        ConcurrentSet<String> s = createSetFrom(Arrays.asList("x", "y", "z"));
        Set<String> hashSet = new HashSet<>(Arrays.asList("x", "q"));
        assertFalse(s.containsAll(hashSet), "HashSet contains an element not present in the ConcurrentSet");
    }

    @Test
    void containsAll_CustomCollectionWithSideEffectIterator_EarlyTermination() {
        // Set contains 1,2,3,4
        ConcurrentSet<Integer> s = createSetFrom(Arrays.asList(1, 2, 3, 4));
        // Custom collection that counts iterations and contains a missing element (5)
        AbstractCollection<Integer> custom = new AbstractCollection<Integer>() {

            private int iterationCount = 0;

            @Override
            public Iterator<Integer> iterator() {
                return new Iterator<Integer>() {

                    private final Iterator<Integer> it = Arrays.asList(1, 5, 3).iterator();

                    @Override
                    public boolean hasNext() {
                        return it.hasNext();
                    }

                    @Override
                    public Integer next() {
                        iterationCount++;
                        return it.next();
                    }
                };
            }

            @Override
            public int size() {
                return 3;
            }

            public int getIterationCount() {
                return iterationCount;
            }
        };
        // The method should stop after checking 1 (present) and 5 (missing) → 2 iterations
        boolean result = s.containsAll(custom);
        assertFalse(result, "containsAll should return false because element 5 is missing");
        assertEquals(2, ((AbstractCollection<Integer>) custom).getIterationCount(), "Iterator should have been invoked only until the missing element was detected");
    }
}
