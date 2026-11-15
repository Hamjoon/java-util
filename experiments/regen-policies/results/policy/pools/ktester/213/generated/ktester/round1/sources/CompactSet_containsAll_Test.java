package com.cedarsoftware.util;

import java.util.*;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CompactSet_containsAll_Test {

    private CompactSet<String> set;

    /**
     * Simple collection that throws RuntimeException when its iterator is used.
     */
    private static class ThrowingCollection<E> implements Collection<E> {

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
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new RuntimeException("Iterator failure");
                }

                @Override
                public E next() {
                    throw new RuntimeException("Iterator failure");
                }
            };
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

//     @BeforeEach
    void setupBeforeEach() {
        // Create a fresh CompactSet for each test
        set = new CompactSet<>();
        // Populate with a known collection of elements
        set.addAll(Arrays.asList("alpha", "beta", "gamma", "delta"));
        // default set contains two known elements
        set = new CompactSet<>();
        set.addAll(Arrays.asList("apple", "banana"));
    }

//     @AfterEach
    void teardownAfterEach() {
        // Clear the set to ensure no side‑effects between tests
        set.clear();
    }

    private CompactSet<String> createCaseInsensitiveSet() {
        // builder() API inferred from documentation
        return CompactSet.<String>builder().caseSensitive(false).build();
    }

//     @Test
//     void containsAll_WithAllElementsPresent_ReturnsTrue() {
//         Collection<String> subset = Arrays.asList("alpha", "gamma");
//         boolean result = set.containsAll(subset);
//         assertTrue("set should contain all elements of the subset", result);
//     }

//     @Test
//     void containsAll_WithMissingElement_ReturnsFalse() {
//         Collection<String> superset = Arrays.asList("alpha", "beta", "epsilon");
//         boolean result = set.containsAll(superset);
//         assertFalse("set does not contain 'epsilon', should return false", result);
//     }

//     @Test
//     void containsAll_WithEmptyCollection_ReturnsTrue() {
//         Collection<String> empty = Collections.emptyList();
//         boolean result = set.containsAll(empty);
//         assertTrue("containsAll on empty collection should always return true", result);
//     }

//     @Test
//     void containsAll_WithNullElementInCollection_ThrowsNPE() {
//         Collection<String> withNull = Arrays.asList("alpha", null);
//         set.containsAll(withNull);
//     }

//     @Test
//     void containsAll_WithDifferentCollectionImplementation_ReturnsCorrectResult() {
//         Set<String> hashSet = new HashSet<>(Arrays.asList("beta", "delta"));
//         Set<String> treeSet = new TreeSet<>(Arrays.asList("alpha", "gamma", "theta"));
//         assertTrue("All elements of hashSet are present", set.containsAll(hashSet));
//         assertFalse("Element 'theta' is not present in the set", set.containsAll(treeSet));
//     }

//     @Test
//     void containsAll_AfterModificationsReflectsCurrentState() {
//         set.remove("beta");
//         Collection<String> afterRemoval = Arrays.asList("alpha", "beta");
//         assertFalse("After removing 'beta', containsAll should reflect missing element", set.containsAll(afterRemoval));
//     }

//     @Test
//     void containsAll_BasicPositive_ReturnsTrue() {
//         Collection<String> subset = Arrays.asList("apple", "banana");
//         assertTrue("CompactSet contains all elements of the subset, should return true", set.containsAll(subset));
//     }

//     @Test
//     void containsAll_BasicNegative_ReturnsFalse() {
//         Collection<String> superset = Arrays.asList("apple", "cherry");
//         assertFalse("CompactSet does not contain 'cherry', should return false", set.containsAll(superset));
//     }

//     @Test
//     void containsAll_EmptyCollection_ReturnsTrue() {
//         Collection<String> empty = Collections.emptyList();
//         assertTrue("containsAll on an empty collection must always return true", set.containsAll(empty));
//     }

//     @Test
//     void containsAll_EmptySet_NonEmptyCollection_ReturnsFalse() {
//         set.clear();
//         Collection<String> nonEmpty = Arrays.asList("any");
//         assertFalse("Empty CompactSet cannot contain any element of a non‑empty collection", set.containsAll(nonEmpty));
//     }

//     @Test
//     void containsAll_NullCollectionArgument_ThrowsNPE() {
//         set.containsAll(null);
//     }

//     @Test
//     void containsAll_CollectionWithNullElement_ReturnsFalse() {
//         Collection<String> withNull = Arrays.asList("apple", null);
//         assertFalse("Collection containing null should result in false because the set has no null element", set.containsAll(withNull));
//     }

//     @Test
//     void containsAll_CollectionWithMixedNullAndValidElements_ReturnsFalse() {
//         Collection<String> mixed = Arrays.asList("apple", null);
//         assertFalse("Presence of a null element that is not in the set causes containsAll to return false", set.containsAll(mixed));
//     }

//     @Test
//     void containsAll_CollectionWithDuplicateElements_ReturnsTrue() {
//         Collection<String> dupes = Arrays.asList("apple", "apple");
//         assertTrue("Duplicates in the collection do not affect the result as long as the element exists in the set", set.containsAll(dupes));
//     }

//     @Test
//     void containsAll_CollectionLargerThanSet_ReturnsFalse() {
//         Collection<String> larger = Arrays.asList("apple", "banana", "cherry", "date");
//         assertFalse("Not all elements are present, therefore containsAll must return false", set.containsAll(larger));
//     }

//     @Test
//     void containsAll_CaseInsensitive_ElementsMatchIgnoringCase_ReturnsTrue() {
//         CompactSet<String> ciSet = createCaseInsensitiveSet();
//         ciSet.addAll(Arrays.asList("apple", "banana"));
//         Collection<String> upperCase = Arrays.asList("APPLE", "BANANA");
//         assertTrue("Case‑insensitive set should treat different case as equal", ciSet.containsAll(upperCase));
//     }

//     @Test
//     void containsAll_CaseInsensitive_ElementMissing_ReturnsFalse() {
//         CompactSet<String> ciSet = createCaseInsensitiveSet();
//         ciSet.addAll(Arrays.asList("apple", "banana"));
//         Collection<String> mixed = Arrays.asList("APPLE", "CHERRY");
//         assertFalse("Even case‑insensitive set must return false for elements not present", ciSet.containsAll(mixed));
//     }

//     @Test
//     void containsAll_CaseSensitive_MixedCaseElement_ReturnsFalse() {
//         Collection<String> mixed = Arrays.asList("Apple");
//         assertFalse("Case‑sensitive set should not match different case", set.containsAll(mixed));
//     }

//     @Test
//     void containsAll_CollectionExactlyMatchesSet_ReturnsTrue() {
//         Collection<String> same = Arrays.asList("apple", "banana");
//         assertTrue("When the collection equals the set, containsAll must return true", set.containsAll(same));
//     }

//     @Test
//     void containsAll_CollectionOfDifferentType_ReturnsFalse() {
//         Collection<Integer> ints = Arrays.asList(1, 2);
//         assertFalse("Set of Strings cannot contain Integer elements", set.containsAll(ints));
//     }

//     @Test
//     void containsAll_UnmodifiableCollection_ReturnsTrue() {
//         Set<String> unmodifiable = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("apple")));
//         assertTrue("containsAll must work with any Collection implementation, including unmodifiable ones", set.containsAll(unmodifiable));
//     }

//     @Test
//     void containsAll_SortedOrdering_WithIncompatibleElementTypes_ThrowsClassCastException() {
//         CompactSet<String> sortedSet = CompactSet.<String>builder().sortedOrder().build();
//         sortedSet.addAll(Arrays.asList("apple", "banana"));
//         Collection<Integer> ints = Arrays.asList(1, 2);
//         sortedSet.containsAll(ints);
//     }

//     @Test
//     void containsAll_CollectionIteratorThrowsException_PropagatesRuntimeException() {
//         Collection<String> throwing = new ThrowingCollection<>();
//         set.containsAll(throwing);
//     }

//     @Test
//     void containsAll_DifferentCollectionImplementations_ReturnCorrectResults() {
//         Set<String> hashSet = new HashSet<>(Arrays.asList("apple"));
//         Set<String> treeSet = new TreeSet<>(Arrays.asList("banana", "cherry"));
//         assertTrue("HashSet elements are present", set.containsAll(hashSet));
//         assertFalse("Set does not contain 'cherry'", set.containsAll(treeSet));
//     }
}