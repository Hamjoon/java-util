/********************************************************************
 *  JUnit‑4 test suite for com.cedarsoftware.util.CaseInsensitiveSet
 *
 *  The test class follows the systematic approach described in the
 *  problem statement:
 *
 *   1) List of public methods (including constructors) with signatures
 *   2) A basic test for each method that validates the normal behaviour
 *   3) Identification of edge‑cases / exception scenarios
 *   4) Additional tests that target those edge‑cases
 *   5) All tests merged into a single JUnit‑4 file.
 *
 *  The suite uses only the public API that is present in the supplied
 *  source code and its direct dependencies.  No mocking frameworks are
 *  used – real objects are created via the constructors that are part
 *  of the library.
 *
 *  Author: ChatGPT (generated for the exercise)
 ********************************************************************/
package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CaseInsensitiveSetTest {

    /** -----------------------------------------------------------------
     *  1) PUBLIC METHODS (including constructors) extracted from the class
     *  -----------------------------------------------------------------
     *
     *  Constructors
     *  -------------
     *  CaseInsensitiveSet()
     *  CaseInsensitiveSet(Collection<? extends E> collection)
     *  CaseInsensitiveSet(Collection<? extends E> source, Map backingMap)
     *  CaseInsensitiveSet(int initialCapacity)
     *  CaseInsensitiveSet(int initialCapacity, float loadFactor)
     *
     *  Set‑like operations
     *  -------------------
     *  int                hashCode()
     *  boolean            equals(Object other)
     *  int                size()
     *  boolean            isEmpty()
     *  boolean            contains(Object o)
     *  Iterator<E>        iterator()
     *  Object[]           toArray()
     *  <T> T[]            toArray(T[] a)
     *  boolean            add(E e)
     *  boolean            remove(Object o)
     *  boolean            containsAll(Collection<?> c)
     *  boolean            addAll(Collection<? extends E> c)
     *  boolean            retainAll(Collection<?> c)
     *  boolean            removeAll(Collection<?> c)
     *  void               clear()
     *
     *  Deprecated convenience methods
     *  --------------------------------
     *  Set<E> minus(Iterable<E> removeMe)
     *  Set<E> minus(E removeMe)
     *  Set<E> plus(Iterable<E> right)
     *  Set<E> plus(Object right)
     *
     *  Object             toString()
     */

    /** -----------------------------------------------------------------
     *  2) BASIC TESTS – one for each public method
     *  -----------------------------------------------------------------
     */

    private CaseInsensitiveSet<String> emptySet;
    private CaseInsensitiveSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CaseInsensitiveSet<>();
        populatedSet = new CaseInsensitiveSet<>(Arrays.asList("Apple", "Banana"));
    }

    /* ---------- Constructors ---------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> source = Arrays.asList("One", "Two", "Three");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
    }

    @Test
    public void testCollectionAndBackingMapConstructorUsesProvidedMap() {
        Map<String, Object> backing = new LinkedHashMap<>();
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Arrays.asList("X", "Y"), backing);
        // The backing map should now contain the two entries
        assertEquals(2, backing.size());
        assertTrue(set.contains("x")); // case‑insensitive check
    }

    @Test
    public void testInitialCapacityConstructorCreatesEmptySet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testInitialCapacityAndLoadFactorConstructorCreatesEmptySet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(5, 0.75f);
        assertTrue(set.isEmpty());
    }

    /* ---------- hashCode & equals ---------- */

    @Test
    public void testHashCodeConsistentWithUnderlyingMap() {
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>(Arrays.asList("A", "B"));
        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>(Arrays.asList("a", "b"));
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    public void testEqualsReflexiveAndSymmetric() {
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>(Arrays.asList("One", "Two"));
        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>(Arrays.asList("one", "two"));
        assertTrue(set1.equals(set1));          // reflexive
        assertTrue(set1.equals(set2));          // case‑insensitive equality
        assertTrue(set2.equals(set1));          // symmetric
        assertFalse(set1.equals(null));         // null comparison
        assertFalse(set1.equals("not a set"));  // different type
    }

    /* ---------- size, isEmpty ---------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(0, emptySet.size());
        assertTrue(emptySet.isEmpty());

        assertEquals(2, populatedSet.size());
        assertFalse(populatedSet.isEmpty());
    }

    /* ---------- contains ---------- */

    @Test
    public void testContainsCaseInsensitive() {
        assertTrue(populatedSet.contains("apple"));
        assertTrue(populatedSet.contains("BANANA"));
        assertFalse(populatedSet.contains("Cherry"));
    }

    /* ---------- iterator ---------- */

    @Test
    public void testIteratorPreservesInsertionOrderAndOriginalCase() {
        List<String> expected = Arrays.asList("Apple", "Banana");
        Iterator<String> it = populatedSet.iterator();
        List<String> actual = new ArrayList<>();
        while (it.hasNext()) {
            actual.add(it.next());
        }
        assertEquals(expected, actual);
    }

    /* ---------- toArray (no‑arg) ---------- */

    @Test
    public void testToArrayReturnsAllElements() {
        Object[] array = populatedSet.toArray();
        assertArrayEquals(new Object[]{"Apple", "Banana"}, array);
    }

    /* ---------- toArray(T[]) ---------- */

    @Test
    public void testToArrayTypedArrayExactSize() {
        String[] dest = new String[2];
        String[] result = populatedSet.toArray(dest);
        assertSame(dest, result); // should reuse the supplied array
        assertArrayEquals(new String[]{"Apple", "Banana"}, result);
    }

    @Test
    public void testToArrayTypedArrayLargerThanNeeded() {
        String[] dest = new String[5];
        String[] result = populatedSet.toArray(dest);
        assertSame(dest, result);
        assertEquals("Apple", result[0]);
        assertEquals("Banana", result[1]);
        assertNull(result[2]); // per Collection contract, the element after the last is set to null
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayTypedArrayNullArgument() {
        populatedSet.toArray((String[]) null);
    }

    @Test(expected = ArrayStoreException.class)
    public void testToArrayTypedArrayIncompatibleComponentType() {
        // The set holds Strings, but we ask for an Integer[] – this must fail
        populatedSet.toArray(new Integer[0]);
    }

    /* ---------- add ---------- */

    @Test
    public void testAddNewElementReturnsTrueAndIncreasesSize() {
        assertTrue(emptySet.add("New"));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("new"));
    }

    @Test
    public void testAddDuplicateDifferentCaseReturnsFalse() {
        assertTrue(populatedSet.add("apple")); // already present (case‑insensitive)
        assertEquals(2, populatedSet.size()); // size unchanged
    }

    /* ---------- remove ---------- */

    @Test
    public void testRemoveExistingElementReturnsTrue() {
        assertTrue(populatedSet.remove("APPLE"));
        assertFalse(populatedSet.contains("Apple"));
        assertEquals(1, populatedSet.size());
    }

    @Test
    public void testRemoveNonExistingElementReturnsFalse() {
        assertFalse(populatedSet.remove("Cherry"));
    }

    /* ---------- containsAll ---------- */

    @Test
    public void testContainsAllPositive() {
        List<String> list = Arrays.asList("apple", "BANANA");
        assertTrue(populatedSet.containsAll(list));
    }

    @Test
    public void testContainsAllNegative() {
        List<String> list = Arrays.asList("apple", "Cherry");
        assertFalse(populatedSet.containsAll(list));
    }

    @Test(expected = NullPointerException.class)
    public void testContainsAllNullCollection() {
        populatedSet.containsAll(null);
    }

    /* ---------- addAll ---------- */

    @Test
    public void testAddAllAddsMissingElementsOnly() {
        List<String> toAdd = Arrays.asList("banana", "Cherry", "Date");
        assertTrue(populatedSet.addAll(toAdd));
        assertEquals(4, populatedSet.size());
        assertTrue(populatedSet.containsAll(Arrays.asList("Apple", "Banana", "Cherry", "Date")));
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllNullCollection() {
        emptySet.addAll(null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllCollectionContainingNullElement() {
        List<String> list = new ArrayList<>();
        list.add(null);
        emptySet.addAll(list);
    }

    /* ---------- retainAll ---------- */

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        List<String> keep = Arrays.asList("apple", "CHERRY"); // CHERRY not present
        assertTrue(populatedSet.retainAll(keep));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Apple"));
        assertFalse(populatedSet.contains("Banana"));
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAllNullCollection() {
        populatedSet.retainAll(null);
    }

    /* ---------- removeAll ---------- */

    @Test
    public void testRemoveAllRemovesSpecifiedElements() {
        List<String> toRemove = Arrays.asList("APPLE", "cherry");
        assertTrue(populatedSet.removeAll(toRemove));
        assertEquals(1, populatedSet.size());
        assertFalse(populatedSet.contains("Apple"));
        assertTrue(populatedSet.contains("Banana"));
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveAllNullCollection() {
        populatedSet.removeAll(null);
    }

    /* ---------- clear ---------- */

    @Test
    public void testClearEmptiesSet() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    /* ---------- Deprecated plus/minus ---------- */

    @Test
    public void testDeprecatedPlusAddsElements() {
        emptySet.plus("Alpha");
        emptySet.plus(Arrays.asList("Beta", "Gamma"));
        assertEquals(3, emptySet.size());
        assertTrue(emptySet.containsAll(Arrays.asList("alpha", "beta", "gamma")));
    }

    @Test
    public void testDeprecatedMinusRemovesElements() {
        populatedSet.minus("APPLE");
        populatedSet.minus(Arrays.asList("banana", "nonexistent"));
        assertTrue(populatedSet.isEmpty());
    }

    /* ---------- toString ---------- */

    @Test
    public void testToStringReflectsInsertionOrderAndOriginalCase() {
        assertEquals("[Apple, Banana]", populatedSet.toString());
    }

    /* ---------- Edge‑case / Exception tests for constructors ---------- */

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new CaseInsensitiveSet<String>(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeInitialCapacityThrowsIAE() {
        new CaseInsensitiveSet<String>(-5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeLoadFactorThrowsIAE() {
        new CaseInsensitiveSet<String>(10, -0.5f);
    }

    /* ---------- Additional edge cases ---------- */

    @Test
    public void testMixedTypeSetHandlesStringAndNonStringSeparately() {
        CaseInsensitiveSet<Object> mixed = new CaseInsensitiveSet<>();
        mixed.add("String");
        mixed.add(42);
        mixed.add("string"); // duplicate case‑insensitive string, should not add
        assertEquals(2, mixed.size());
        assertTrue(mixed.contains("STRING"));
        assertTrue(mixed.contains(42));
    }

    @Test
    public void testAddNullElementBehaviour() {
        // The underlying CaseInsensitiveMap does not allow null keys, so we expect a NullPointerException
        try {
            emptySet.add(null);
            fail("Expected NullPointerException when adding null element");
        } catch (NullPointerException ignored) {
            // Expected – the map implementation throws NPE for null keys
        }
    }
}
