package com.cedarsoftware.util;

import java.util.*;
import java.util.stream.Stream;
// import org.junit.*;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactSet_equals_Test {

    /* --------------------------------------------------------------------- */
    private static class FaultySet<E> implements Set<E> {

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
        public Iterator<E> iterator() {
            return new Iterator<E>() {

                @Override
                public boolean hasNext() {
                    throw new RuntimeException("iterator failure");
                }

                @Override
                public E next() {
                    throw new RuntimeException("iterator failure");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean addAll(Collection<? extends E> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean equals(Object o) {
            return o == this;
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

//     private static Stream<Arguments> standardSetProvider() {
//         Set<String> hash = new HashSet<>(Arrays.asList("p", "q"));
//         Set<String> tree = new TreeSet<>(Arrays.asList("p", "q"));
//         return Arrays.asList(hash, tree);
//     }

//     private static Stream<Arguments> emptySetProvider() {
//         Set<String> hash = new HashSet<>();
//         Set<String> tree = new TreeSet<>();
//         Set<String> compact = new CompactSet<>();
//         return Arrays.asList(hash, tree, compact);
//     }

//     private static Stream<Arguments> linkedHashSetProvider() {
//         LinkedHashSet<String> sameOrder = new LinkedHashSet<>(Arrays.asList("p", "q"));
//         LinkedHashSet<String> reverseOrder = new LinkedHashSet<>(Arrays.asList("q", "p"));
//         return Arrays.asList(sameOrder, reverseOrder);
//     }

//     @Test
//     void equalsSelfReturnsTrue() {
//         Set<String> set = new CompactSet<>();
//         set.add("alpha");
//         set.add("beta");
//         assertTrue(set.equals(set));
//     }

//     @Test
//     void equalsAnotherCompactSetWithSameElements() {
//         Set<String> set1 = new CompactSet<>();
//         Set<String> set2 = new CompactSet<>();
//         set1.add("one");
//         set1.add("two");
//         set2.add("two");
//         set2.add("one");
//         assertTrue(set1.equals(set2));
//         assertTrue(set2.equals(set1));
//     }

//     @Test
//     void equalsAnotherSetImplementationWithSameElements() {
//         Set<String> compactSet = new CompactSet<>();
//         compactSet.add("x");
//         compactSet.add("y");
//         Set<String> hashSet = new HashSet<>();
//         hashSet.add("y");
//         hashSet.add("x");
//         assertTrue(compactSet.equals(hashSet));
//         assertTrue(hashSet.equals(compactSet));
//     }

//     @Test
//     void equalsAnotherSetImplementationWithDifferentElements() {
//         Set<String> compactSet = new CompactSet<>();
//         compactSet.add("a");
//         compactSet.add("b");
//         Set<String> otherSet = new HashSet<>();
//         otherSet.add("a");
//         otherSet.add("c");
//         assertFalse(compactSet.equals(otherSet));
//     }

//     @Test
//     void equalsEmptySets() {
//         Set<String> emptyCompact = new CompactSet<>();
//         Set<String> emptyHash = new HashSet<>();
//         assertTrue(emptyCompact.equals(emptyHash));
//         assertTrue(emptyHash.equals(emptyCompact));
//     }

//     @Test
//     void equalsNullReturnsFalse() {
//         Set<String> set = new CompactSet<>();
//         set.add("value");
//         assertFalse(set.equals(null));
//     }

//     @Test
//     void equalsNonSetObjectReturnsFalse() {
//         Set<String> set = new CompactSet<>();
//         set.add("foo");
//         String notASet = "foo";
//         assertFalse(set.equals(notASet));
//     }

//     @Test
//     void equalsTreeSetWithSameElementsAndDifferentOrdering() {
//         Set<String> compactSet = new CompactSet<>();
//         compactSet.add("c");
//         compactSet.add("a");
//         compactSet.add("b");
//         Set<String> treeSet = new TreeSet<>();
//         treeSet.add("b");
//         treeSet.add("c");
//         treeSet.add("a");
//         assertTrue(compactSet.equals(treeSet));
//         assertTrue(treeSet.equals(compactSet));
//     }

//     @Test
//     void selfComparisonSameInstance() {
//         CompactSet<String> set = new CompactSet<>();
//         set.addAll(Arrays.asList("x", "y"));
//         assertTrue(set.equals(set));
//     }

//     @Test
//     void equalityWithAnotherCompactSetIdenticalElements() {
//         CompactSet<String> set1 = new CompactSet<>();
//         CompactSet<String> set2 = new CompactSet<>();
//         set1.addAll(Arrays.asList("a", "b", "c"));
//         set2.addAll(Arrays.asList("a", "b", "c"));
//         assertTrue(set1.equals(set2));
//     }

//     @Test
//     void inequalityWithAnotherCompactSetDifferentElements() {
//         CompactSet<String> set1 = new CompactSet<>();
//         CompactSet<String> set2 = new CompactSet<>();
//         set1.addAll(Arrays.asList("a", "b", "c"));
//         set2.addAll(Arrays.asList("a", "b", "d"));
//         assertFalse(set1.equals(set2));
//     }

//     @ParameterizedTest(name = "{index} => other={0}")
//     @MethodSource("standardSetProvider")
//     void equalityWithStandardSetImplementations(Set<String> other) {
//         CompactSet<String> compact = new CompactSet<>();
//         compact.addAll(Arrays.asList("p", "q"));
//         for (Set<String> other : standardSetProvider()) {
//             assertTrue(compact.equals(other));
//             assertTrue(other.equals(compact));
//         }
//     }

//     @Test
//     void inequalityWhenOtherSetHasExtraElement() {
//         CompactSet<String> compact = new CompactSet<>();
//         compact.addAll(Arrays.asList("a", "b"));
//         Set<String> superset = new HashSet<>(Arrays.asList("a", "b", "c"));
//         assertFalse(compact.equals(superset));
//     }

//     @Test
//     void inequalityWhenOtherSetIsMissingElement() {
//         CompactSet<String> compact = new CompactSet<>();
//         compact.addAll(Arrays.asList("a", "b"));
//         Set<String> subset = new HashSet<>(Collections.singletonList("a"));
//         assertFalse(compact.equals(subset));
//     }

//     @Test
//     void equalityCaseInsensitiveCompactSets() {
//         CompactSet<String> caseSensitive = CompactSet.<String>builder().caseSensitive(true).build();
//         caseSensitive.addAll(Arrays.asList("a", "b"));
//         CompactSet<String> caseInsensitive = CompactSet.<String>builder().caseSensitive(false).build();
//         caseInsensitive.addAll(Arrays.asList("A", "b"));
//         CompactSet<String> ci1 = CompactSet.<String>builder().caseSensitive(false).build();
//         ci1.addAll(Arrays.asList("a", "b"));
//         CompactSet<String> ci2 = CompactSet.<String>builder().caseSensitive(false).build();
//         ci2.addAll(Arrays.asList("A", "b"));
//         assertTrue(ci1.equals(ci2));
//     }

//     @Test
//     void inequalityCaseSensitivityMismatch() {
//         CompactSet<String> caseSensitive = CompactSet.<String>builder().caseSensitive(true).build();
//         caseSensitive.add("a");
//         CompactSet<String> caseInsensitive = CompactSet.<String>builder().caseSensitive(false).build();
//         caseInsensitive.add("A");
//         assertFalse(caseSensitive.equals(caseInsensitive));
//     }

//     @ParameterizedTest(name = "{index} => other={0}")
//     @MethodSource("emptySetProvider")
//     void equalityWithEmptySets(Set<String> other) {
//         CompactSet<String> emptyCompact = new CompactSet<>();
//         for (Set<String> other : emptySetProvider()) {
//             assertTrue(emptyCompact.equals(other));
//             assertTrue(other.equals(emptyCompact));
//         }
//     }

//     @Test
//     void inequalityEmptyVsNonEmpty() {
//         CompactSet<String> emptyCompact = new CompactSet<>();
//         Set<String> nonEmpty = new HashSet<>(Collections.singletonList("z"));
//         assertFalse(emptyCompact.equals(nonEmpty));
//     }

//     @Test
//     void equalityWithNullElementPresence() {
//         CompactSet<String> compact = new CompactSet<>();
//         compact.add(null);
//         Set<String> hashWithNull = new HashSet<>();
//         hashWithNull.add(null);
//         assertTrue(compact.equals(hashWithNull));
//     }

//     @Test
//     void inequalityWhenOnlyOneContainsNull() {
//         CompactSet<String> compact = new CompactSet<>();
//         compact.add(null);
//         Set<String> hashWithoutNull = new HashSet<>();
//         assertFalse(compact.equals(hashWithoutNull));
//     }

//     @ParameterizedTest(name = "{index} => order={1}")
//     @MethodSource("linkedHashSetProvider")
//     void equalityWithLinkedHashSet(LinkedHashSet<String> other) {
//         CompactSet<String> compact = new CompactSet<>();
//         compact.addAll(Arrays.asList("p", "q"));
//         for (LinkedHashSet<String> other : linkedHashSetProvider()) {
//             assertTrue(compact.equals(other));
//             assertTrue(other.equals(compact));
//         }
//     }

//     @Test
//     void equalityWithUnmodifiableSet() {
//         Set<String> mutable = new HashSet<>(Arrays.asList("m", "n"));
//         Set<String> unmodifiable = Collections.unmodifiableSet(mutable);
//         CompactSet<String> compact = new CompactSet<>();
//         compact.addAll(Arrays.asList("m", "n"));
//         assertTrue(compact.equals(unmodifiable));
//         assertTrue(unmodifiable.equals(compact));
//     }

//     @Test
//     void classCastExceptionCaseInsensitiveComparatorMismatch() {
//         CompactSet<String> caseInsensitive = CompactSet.<String>builder().caseSensitive(false).build();
//         caseInsensitive.addAll(Arrays.asList("a", "b"));
//         Set<Integer> integerSet = new HashSet<>(Collections.singletonList(123));
//         try {
//             caseInsensitive.equals(integerSet);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }

//     @Test
//     void classCastExceptionNaturalOrderingComparatorMismatch() {
//         CompactSet<String> naturalOrdered = CompactSet.<String>builder().sortedOrder().build();
//         naturalOrdered.addAll(Arrays.asList("x", "y"));
//         Set<Integer> integerSet = new TreeSet<>(Collections.singleton(456));
//         try {
//             naturalOrdered.equals(integerSet);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//         }
//     }

//     @Test
//     void runtimeExceptionFromFaultySetIterator() {
//         CompactSet<String> compact = new CompactSet<>();
//         compact.addAll(Arrays.asList("a", "b"));
//         Set<String> faulty = new FaultySet<>();
//         try {
//             compact.equals(faulty);
//             fail("Expected RuntimeException");
//         } catch (RuntimeException ignored) {
//         }
//     }

//     @Test
//     void orderingConfigurationIndependence() {
//         CompactSet<String> insertionOrdered = CompactSet.<String>builder().insertionOrder().build();
//         insertionOrdered.addAll(Arrays.asList("x", "y"));
//         CompactSet<String> sortedOrdered = CompactSet.<String>builder().sortedOrder().build();
//         sortedOrdered.addAll(Arrays.asList("y", "x"));
//         assertTrue(insertionOrdered.equals(sortedOrdered));
//         assertTrue(sortedOrdered.equals(insertionOrdered));
//     }
}