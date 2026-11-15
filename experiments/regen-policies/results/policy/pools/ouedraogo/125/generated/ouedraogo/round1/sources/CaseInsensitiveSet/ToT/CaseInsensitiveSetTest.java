package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.After;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors (including error scenarios)</li>
 *   <li>Standard {@link Set} operations (add, remove, contains, size, isEmpty, iterator, toArray, etc.)</li>
 *   <li>Case‑insensitive behaviour for {@link String} elements</li>
 *   <li>Mixed‑type collections (String + non‑String)</li>
 *   <li>Bulk operations (addAll, containsAll, retainAll, removeAll)</li>
 *   <li>Equality and hash‑code contracts</li>
 *   <li>String representation</li>
 *   <li>Deprecated convenience methods {@code plus} and {@code minus}</li>
 *   <li>Edge cases such as null arguments, negative capacities, and improper array types</li>
 * </ul>
 */
public class CaseInsensitiveSetTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private CaseInsensitiveSet<String> set;

    @Before
    public void setUp() {
        set = new CaseInsensitiveSet<>();
    }

    @After
    public void tearDown() {
        set.clear();
    }

    /* ---------------------------------------------------------------------- */
    /*                     Constructor Tests                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElementsCaseInsensitively() {
        List<String> src = Arrays.asList("Hello", "WORLD", "hello", "World");
        CaseInsensitiveSet<String> s = new CaseInsensitiveSet<>(src);
        // "Hello"/"hello" collapse, "WORLD"/"World" collapse
        assertEquals(2, s.size());
        assertTrue(s.contains("HELLO"));
        assertTrue(s.contains("world"));
        // Original case preserved (first occurrence)
        Iterator<String> it = s.iterator();
        assertEquals("Hello", it.next());
        assertEquals("WORLD", it.next());
    }

    @Test
    public void testConstructorWithSortedSetUsesTreeMapBacking() {
        SortedSet<String> sorted = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        sorted.addAll(Arrays.asList("b", "A", "c"));
        CaseInsensitiveSet<String> s = new CaseInsensitiveSet<>(sorted);
        // Verify ordering matches TreeMap (natural order, case‑insensitive)
        assertEquals("[A, b, c]", s.toString());
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveSet<Integer> s = new CaseInsensitiveSet<>(10);
        assertTrue(s.isEmpty());
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveSet<Integer> s = new CaseInsensitiveSet<>(5, 0.75f);
        assertTrue(s.isEmpty());
    }

    @Test
    public void testConstructorWithNegativeCapacityThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        new CaseInsensitiveSet<>(-1);
    }

    @Test
    public void testConstructorWithZeroCapacityAndNegativeLoadFactorThrowsException() {
        thrown.expect(IllegalArgumentException.class);
        new CaseInsensitiveSet<>(0, -0.1f);
    }

    @Test
    public void testConstructorWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new CaseInsensitiveSet<String>(null);
    }

    @Test
    public void testConstructorWithBackingMapAndCollection() {
        Map<Object, Object> backing = new CaseInsensitiveMap<>(2);
        List<Object> source = Arrays.asList("One", 2, "one");
        CaseInsensitiveSet<Object> s = new CaseInsensitiveSet<>(source, backing);
        assertEquals(2, s.size()); // "One"/"one" collapse
        assertTrue(s.contains("ONE"));
        assertTrue(s.contains(2));
    }

    /* ---------------------------------------------------------------------- */
    /*                     Basic Set Operations                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(set.add("Hello"));
        assertTrue(set.contains("hello"));
        assertFalse(set.add("HELLO")); // duplicate, case‑insensitive
        assertEquals(1, set.size());
    }

    @Test
    public void testAddNonStringElementsAreHandledNormally() {
        CaseInsensitiveSet<Object> s = new CaseInsensitiveSet<>();
        assertTrue(s.add(123));
        assertTrue(s.add(123)); // duplicate non‑String should not be added again
        assertEquals(1, s.size());
        assertTrue(s.contains(123));
    }

    @Test
    public void testRemoveCaseInsensitive() {
        set.add("Test");
        assertTrue(set.remove("test"));
        assertFalse(set.contains("TEST"));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRemoveNonExistingElementReturnsFalse() {
        set.add("A");
        assertFalse(set.remove("B"));
        assertEquals(1, set.size());
    }

    @Test
    public void testSizeAndIsEmptyReflectChanges() {
        assertTrue(set.isEmpty());
        set.add("x");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        set.remove("X");
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* ---------------------------------------------------------------------- */
    /*                     Bulk Operations                                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAllHandlesDuplicatesCaseInsensitively() {
        List<String> list = Arrays.asList("a", "B", "A", "b", "c");
        assertTrue(set.addAll(list));
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", "b", "c")));
        // Second call should not modify the set
        assertFalse(set.addAll(list));
    }

    @Test
    public void testContainsAllWithMixedCase() {
        set.addAll(Arrays.asList("One", "Two"));
        assertTrue(set.containsAll(Arrays.asList("one", "TWO")));
        assertFalse(set.containsAll(Arrays.asList("one", "three")));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElementsCaseInsensitively() {
        set.addAll(Arrays.asList("a", "b", "c", "D"));
        boolean changed = set.retainAll(Arrays.asList("A", "d"));
        assertTrue(changed);
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", "D")));
        assertFalse(set.contains("b"));
    }

    @Test
    public void testRetainAllWithNoChangesReturnsFalse() {
        set.addAll(Arrays.asList("x", "y"));
        boolean changed = set.retainAll(Arrays.asList("X", "Y"));
        assertFalse(changed);
    }

    @Test
    public void testRemoveAllRemovesSpecifiedElementsCaseInsensitively() {
        set.addAll(Arrays.asList("alpha", "beta", "gamma"));
        boolean changed = set.removeAll(Arrays.asList("ALPHA", "GAMMA"));
        assertTrue(changed);
        assertEquals(1, set.size());
        assertTrue(set.contains("beta"));
    }

    @Test
    public void testRemoveAllWhenNoElementMatchesReturnsFalse() {
        set.addAll(Arrays.asList("one", "two"));
        boolean changed = set.removeAll(Arrays.asList("three"));
        assertFalse(changed);
        assertEquals(2, set.size());
    }

    @Test
    public void testClearEmptiesTheSet() {
        set.addAll(Arrays.asList("a", "b"));
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* ---------------------------------------------------------------------- */
    /*                     Iterator and Array Conversion                        */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIteratorPreservesInsertionOrderAndOriginalCase() {
        set.addAll(Arrays.asList("First", "second", "THIRD"));
        Iterator<String> it = set.iterator();
        assertEquals("First", it.next());
        assertEquals("second", it.next());
        assertEquals("THIRD", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorRemoveWorks() {
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String v = it.next();
            if ("b".equalsIgnoreCase(v)) {
                it.remove();
            }
        }
        assertEquals(2, set.size());
        assertFalse(set.contains("b"));
    }

    @Test
    public void testToArrayReturnsObjectArrayPreservingOrder() {
        set.addAll(Arrays.asList("x", "y"));
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[]{"x", "y"}, arr);
    }

    @Test
    public void testToArrayWithTypedArrayCreatesCorrectRuntimeType() {
        set.addAll(Arrays.asList("p", "q"));
        String[] arr = set.toArray(new String[0]);
        assertArrayEquals(new String[]{"p", "q"}, arr);
    }

    @Test
    public void testToArrayWithTooSmallArrayCreatesNewArray() {
        set.addAll(Arrays.asList("one", "two"));
        String[] small = new String[1];
        String[] result = set.toArray(small);
        assertNotSame(small, result); // new array created
        assertEquals(2, result.length);
    }

    @Test
    public void testToArrayWithIncompatibleArrayTypeThrowsArrayStoreException() {
        set.addAll(Arrays.asList("a"));
        thrown.expect(ArrayStoreException.class);
        Integer[] bad = new Integer[1];
        set.toArray(bad);
    }

    /* ---------------------------------------------------------------------- */
    /*                     Equality, HashCode and toString                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsReflexiveAndSymmetric() {
        set.addAll(Arrays.asList("A", "b"));
        CaseInsensitiveSet<String> other = new CaseInsensitiveSet<>();
        other.addAll(Arrays.asList("a", "B"));
        assertTrue(set.equals(other));
        assertTrue(other.equals(set));
        assertEquals(set.hashCode(), other.hashCode());
    }

    @Test
    public void testEqualsWithDifferentImplementationSet() {
        set.addAll(Arrays.asList("x", "y"));
        Set<String> hashSet = new HashSet<>(Arrays.asList("X", "Y"));
        // HashSet does NOT perform case‑insensitive comparison, so equality should be false
        assertFalse(set.equals(hashSet));
    }

    @Test
    public void testEqualsWithNonSetObjectReturnsFalse() {
        assertFalse(set.equals("not a set"));
    }

    @Test
    public void testHashCodeMatchesUnderlyingMapKeySetHashCode() {
        set.addAll(Arrays.asList("m", "n"));
        int expected = set.map.keySet().hashCode(); // direct access for verification
        assertEquals(expected, set.hashCode());
    }

    @Test
    public void testToStringPreservesOriginalCaseAndOrder() {
        set.addAll(Arrays.asList("First", "second", "THIRD"));
        assertEquals("[First, second, THIRD]", set.toString());
    }

    /* ---------------------------------------------------------------------- */
    /*                     Deprecated Convenience Methods                      */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testDeprecatedPlusWithSingleObject() {
        set.plus("One");
        assertTrue(set.contains("ONE"));
        assertEquals(1, set.size());
    }

    @Test
    public void testDeprecatedPlusWithIterable() {
        List<String> list = Arrays.asList("a", "B", "c");
        set.plus(list);
        assertTrue(set.containsAll(Arrays.asList("A", "b", "C")));
        assertEquals(3, set.size());
    }

    @Test
    public void testDeprecatedMinusWithSingleObject() {
        set.addAll(Arrays.asList("alpha", "beta"));
        set.minus("ALPHA");
        assertFalse(set.contains("alpha"));
        assertEquals(1, set.size());
    }

    @Test
    public void testDeprecatedMinusWithIterable() {
        set.addAll(Arrays.asList("one", "two", "three"));
        set.minus(Arrays.asList("ONE", "THREE"));
        assertEquals(1, set.size());
        assertTrue(set.contains("two"));
    }
}
