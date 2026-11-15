package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_equals_Test {

    /**
     * A ConcurrentSet whose iterator's {@code next()} throws {@link ConcurrentModificationException}.
     */
    private static class BadIteratorConcurrentSet<E> extends ConcurrentSet<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new ConcurrentModificationException("forced");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /**
     * A ConcurrentSet whose iterator's {@code next()} throws {@link NullPointerException}.
     */
    private static class NullPointerIteratorConcurrentSet<E> extends ConcurrentSet<E> {

        @Override
        public Iterator<E> iterator() {
            return new Iterator<>() {

                @Override
                public boolean hasNext() {
                    return true;
                }

                @Override
                public E next() {
                    throw new NullPointerException("forced");
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }

    /* -------------------------------------------------------------
     *  Basic identity / null / type checks (parameterized)
     * ------------------------------------------------------------- */
    private static Stream<IdentityCase> identityCases() {
        return Stream.of(new IdentityCase("same instance", set -> set, true), new IdentityCase("null argument", set -> null, false), new IdentityCase("non‑Set argument", set -> "not a set", false));
    }

    /**
     * The same instance must be equal to itself.
     */
    @Test
    void testEqualsSameInstance() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(set.equals(set), "A set should be equal to itself");
    }

    /**
     * Equality must fail when the other object is not a Set.
     */
    @Test
    void testEqualsDifferentType() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        set.add(1);
        assertFalse(set.equals("not a set"), "A set should not be equal to an object of another type");
    }

    /**
     * Equality must fail when the sizes differ.
     */
    @Test
    void testEqualsDifferentSize() {
        ConcurrentSet<Integer> set1 = new ConcurrentSet<>();
        set1.addAll(Arrays.asList(1, 2, 3));
        Set<Integer> set2 = new HashSet<>(Arrays.asList(1, 2));
        assertFalse(set1.equals(set2), "Sets with different sizes must not be equal");
    }

    /**
     * Equality must succeed when both sets contain the same elements, regardless of order.
     */
    @Test
    void testEqualsSameElementsDifferentOrder() {
        ConcurrentSet<String> concurrent = new ConcurrentSet<>();
        concurrent.addAll(Arrays.asList("x", "y", "z"));
        Set<String> hash = new LinkedHashSet<>(Arrays.asList("z", "x", "y"));
        assertTrue(concurrent.equals(hash), "Sets with the same elements should be equal");
        assertTrue(hash.equals(concurrent), "Equality should be symmetric");
    }

    /**
     * Equality must correctly handle null elements.
     */
    @Test
    void testEqualsWithNullElements() {
        ConcurrentSet<String> concurrent = new ConcurrentSet<>();
        concurrent.addAll(Arrays.asList(null, "a", "b"));
        Set<String> hash = new HashSet<>(Arrays.asList("b", null, "a"));
        assertTrue(concurrent.equals(hash), "Sets containing null should compare equal when elements match");
    }

    /**
     * If the other set throws a ClassCastException during contains(), equals must return false.
     */
    @Test
    void testEqualsOtherSetThrowsClassCastException() {
        ConcurrentSet<Integer> concurrent = new ConcurrentSet<>();
        concurrent.addAll(Arrays.asList(1, 2, 3));
        Set<Integer> badSet = new AbstractSet<Integer>() {

            @Override
            public Iterator<Integer> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 3;
            }

            @Override
            public boolean contains(Object o) {
                throw new ClassCastException("forced for test");
            }
        };
        assertFalse(concurrent.equals(badSet), "equals should return false when other.contains throws ClassCastException");
    }

    /**
     * If the other set throws a NullPointerException during contains(), equals must return false.
     */
    @Test
    void testEqualsOtherSetThrowsNullPointerException() {
        ConcurrentSet<String> concurrent = new ConcurrentSet<>();
        concurrent.addAll(Arrays.asList("a", "b"));
        Set<String> badSet = new AbstractSet<String>() {

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 2;
            }

            @Override
            public boolean contains(Object o) {
                throw new NullPointerException("forced for test");
            }
        };
        assertFalse(concurrent.equals(badSet), "equals should return false when other.contains throws NullPointerException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("identityCases")
    void testEqualsIdentityAndTypeChecks(IdentityCase tc) {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        Object other = tc.otherProvider.apply(set);
        assertEquals(tc.expected, set.equals(other), "Failed case: " + tc.description);
    }

    /* -------------------------------------------------------------
     *  Equality with other Set implementations
     * ------------------------------------------------------------- */
    @Test
    void testEqualsSameElementsDifferentImplementation() {
        ConcurrentSet<String> concurrent = new ConcurrentSet<>();
        concurrent.addAll(Arrays.asList("a", "b", "c"));
        Set<String> hash = new HashSet<>(Arrays.asList("c", "a", "b"));
        assertTrue(concurrent.equals(hash), "HashSet with same elements must be equal");
        assertTrue(hash.equals(concurrent), "Equality should be symmetric");
    }

    @Test
    void testEqualsSameElementsTreeSetDifferentOrder() {
        ConcurrentSet<Integer> concurrent = new ConcurrentSet<>();
        concurrent.addAll(Arrays.asList(1, 2, 3));
        Set<Integer> tree = new TreeSet<>(Arrays.asList(3, 2, 1));
        assertTrue(concurrent.equals(tree), "TreeSet with same elements must be equal");
    }

    @Test
    void testEqualsBothEmptySets() {
        ConcurrentSet<Object> concurrent = new ConcurrentSet<>();
        Set<Object> empty = Collections.emptySet();
        assertTrue(concurrent.equals(empty), "Two empty sets should be equal");
    }

    @Test
    void testEqualsTwoConcurrentSets() {
        ConcurrentSet<String> set1 = new ConcurrentSet<>(List.of("alpha", "beta"));
        ConcurrentSet<String> set2 = new ConcurrentSet<>(List.of("beta", "alpha"));
        assertTrue(set1.equals(set2), "Two ConcurrentSet instances with same elements must be equal");
    }

    /* -------------------------------------------------------------
     *  Size mismatch
     * ------------------------------------------------------------- */
    @Test
    void testEqualsSizeMismatch() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("x", "y"));
        // size 3
        Set<String> other = Set.of("x", "y", "z");
        assertFalse(set.equals(other), "Different sizes must result in false");
    }

    @Test
    void testEqualsOtherSetSizeThrowsException() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("a");
        Set<String> badSizeSet = new AbstractSet<>() {

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                throw new IllegalStateException("size broken");
            }
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> set.equals(badSizeSet), "size() exception should propagate");
        assertEquals("size broken", ex.getMessage());
    }

    /* -------------------------------------------------------------
     *  Content mismatch with same size
     * ------------------------------------------------------------- */
    @Test
    void testEqualsContentMismatchSameSize() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b"));
        Set<String> other = Set.of("a", "different");
        assertFalse(set.equals(other), "Same size but different content must be false");
    }

    /* -------------------------------------------------------------
     *  Null element handling
     * ------------------------------------------------------------- */
    @Test
    void testEqualsNullElementMatches() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(null, "a"));
        Set<String> other = new HashSet<>(Arrays.asList("a", null));
        assertTrue(set.equals(other), "Both sets contain null and same elements");
    }

    @Test
    void testEqualsNullElementMismatched() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList(null, "a"));
        // no null
        Set<String> other = Set.of("a", "b");
        assertFalse(set.equals(other), "Set with null should not equal set without null");
    }

    @Test
    void testEqualsOtherSetThrowsRuntimeExceptionFromContains() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a", "b"));
        Set<String> badSet = new AbstractSet<>() {

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 2;
            }

            @Override
            public boolean contains(Object o) {
                throw new IllegalArgumentException("bad arg");
            }
        };
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> set.equals(badSet), "IllegalArgumentException should propagate");
        assertEquals("bad arg", ex.getMessage());
    }

    @Test
    void testEqualsOtherSetThrowsErrorFromContains() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.addAll(Arrays.asList("a"));
        Set<String> badSet = new AbstractSet<>() {

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public boolean contains(Object o) {
                throw new AssertionError("assertion error");
            }
        };
        AssertionError err = assertThrows(AssertionError.class, () -> set.equals(badSet), "Error should propagate unchanged");
        assertEquals("assertion error", err.getMessage());
    }

    /* -------------------------------------------------------------
     *  This ConcurrentSet's iterator throws exceptions
     * ------------------------------------------------------------- */
    @Test
    void testEqualsIteratorThrowsConcurrentModificationException() {
        ConcurrentSet<String> badIterSet = new BadIteratorConcurrentSet<>();
        // element not really used; iterator will throw
        badIterSet.add("x");
        Set<String> other = Collections.emptySet();
        assertThrows(ConcurrentModificationException.class, () -> badIterSet.equals(other), "Iterator exception should propagate");
    }

    @Test
    void testEqualsIteratorThrowsNullPointerException() {
        ConcurrentSet<String> badIterSet = new NullPointerIteratorConcurrentSet<>();
        badIterSet.add("x");
        Set<String> other = Collections.emptySet();
        assertThrows(NullPointerException.class, () -> badIterSet.equals(other), "Iterator NPE should propagate");
    }
}
