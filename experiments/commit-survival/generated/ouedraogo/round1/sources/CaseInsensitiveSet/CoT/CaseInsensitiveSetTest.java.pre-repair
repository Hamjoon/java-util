package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Test suite for {@link CaseInsensitiveSet}.
 *
 * The test class follows the requested steps:
 * 1. Lists all public methods with signatures.
 * 2. Provides a basic test for each method.
 * 3. Identifies edge‑case / exception scenarios.
 * 4. Adds dedicated tests for those scenarios.
 * 5. Combines everything into a single JUnit‑4 test file.
 */
public class CaseInsensitiveSetTest {

    /* ----------------------------------------------------------------------
     * 1. PUBLIC METHOD SIGNATURES
     * ---------------------------------------------------------------------- */
    // Constructors
    //   CaseInsensitiveSet()
    //   CaseInsensitiveSet(Collection<? extends E> collection)
    //   CaseInsensitiveSet(Collection<? extends E> source, Map backingMap)
    //   CaseInsensitiveSet(int initialCapacity)
    //   CaseInsensitiveSet(int initialCapacity, float loadFactor)

    // Set interface methods (implemented)
    //   int size()
    //   boolean isEmpty()
    //   boolean contains(Object o)
    //   Iterator<E> iterator()
    //   Object[] toArray()
    //   <T> T[] toArray(T[] a)
    //   boolean add(E e)
    //   boolean remove(Object o)
    //   boolean containsAll(Collection<?> c)
    //   boolean addAll(Collection<? extends E> c)
    //   boolean retainAll(Collection<?> c)
    //   boolean removeAll(Collection<?> c)
    //   void clear()
    //   boolean equals(Object other)
    //   int hashCode()
    //   String toString()

    // Deprecated convenience methods
    //   Set<E> minus(Iterable<E> removeMe)
    //   Set<E> minus(E removeMe)
    //   Set<E> plus(Iterable<E> right)
    //   Set<E> plus(Object right)

    /* ----------------------------------------------------------------------
     * 2. BASIC TESTS FOR EACH METHOD
     * ---------------------------------------------------------------------- */
    private CaseInsensitiveSet<String> emptySet;
    private CaseInsensitiveSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CaseInsensitiveSet<>();

        List<String> list = Arrays.asList("Alpha", "Bravo", "Charlie");
        populatedSet = new CaseInsensitiveSet<>(list);
    }

    // ---- Constructors ----------------------------------------------------
    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        Collection<String> src = Arrays.asList("One", "Two", "Three");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(src));
    }

    @Test
    public void testCollectionAndBackingMapConstructor() {
        Map<String, Object> map = new HashMap<>();
        Collection<String> src = Arrays.asList("X", "Y");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src, map);
        assertEquals(2, set.size());
        assertTrue(set.contains("x")); // case‑insensitive check
        assertTrue(map == ((CaseInsensitiveSet<String>) set).map); // internal map is the supplied one
    }

    @Test
    public void testInitialCapacityConstructor() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testInitialCapacityAndLoadFactorConstructor() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10, 0.75f);
        assertTrue(set.isEmpty());
    }

    // ---- size / isEmpty --------------------------------------------------
    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(0, emptySet.size());
        assertTrue(emptySet.isEmpty());

        assertEquals(3, populatedSet.size());
        assertFalse(populatedSet.isEmpty());
    }

    // ---- contains --------------------------------------------------------
    @Test
    public void testContainsCaseInsensitive() {
        assertTrue(populatedSet.contains("alpha"));
        assertTrue(populatedSet.contains("BRAVO"));
        assertFalse(populatedSet.contains("delta"));
    }

    // ---- iterator ---------------------------------------------------------
    @Test
    public void testIteratorReturnsOriginalCase() {
        Set<String> observed = new HashSet<>();
        for (String s : populatedSet) {
            observed.add(s);
        }
        // The iterator should give back the original strings as they were inserted
        assertTrue(observed.containsAll(Arrays.asList("Alpha", "Bravo", "Charlie")));
    }

    // ---- toArray ----------------------------------------------------------
    @Test
    public void testToArray() {
        Object[] arr = populatedSet.toArray();
        assertEquals(3, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("Alpha", "Bravo", "Charlie")));
    }

    @Test
    public void testToArrayTyped() {
        String[] arr = populatedSet.toArray(new String[0]);
        assertEquals(3, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("Alpha", "Bravo", "Charlie")));
    }

    // ---- add --------------------------------------------------------------
    @Test
    public void testAddNewElement() {
        assertTrue(emptySet.add("Delta"));
        assertTrue(emptySet.contains("delta"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testAddDuplicateIsNoOp() {
        assertFalse(populatedSet.add("alpha")); // already present (case‑insensitive)
        assertEquals(3, populatedSet.size());
    }

    // ---- remove -----------------------------------------------------------
    @Test
    public void testRemoveExisting() {
        assertTrue(populatedSet.remove("bravo"));
        assertFalse(populatedSet.contains("BRAVO"));
        assertEquals(2, populatedSet.size());
    }

    @Test
    public void testRemoveNonExisting() {
        assertFalse(populatedSet.remove("delta"));
        assertEquals(3, populatedSet.size());
    }

    // ---- containsAll ------------------------------------------------------
    @Test
    public void testContainsAllTrue() {
        List<String> subset = Arrays.asList("alpha", "CHARLIE");
        assertTrue(populatedSet.containsAll(subset));
    }

    @Test
    public void testContainsAllFalse() {
        List<String> superset = Arrays.asList("alpha", "delta");
        assertFalse(populatedSet.containsAll(superset));
    }

    // ---- addAll -----------------------------------------------------------
    @Test
    public void testAddAllAddsMissingElementsOnly() {
        List<String> more = Arrays.asList("delta", "ALPHA");
        assertTrue(populatedSet.addAll(more));
        assertTrue(populatedSet.containsAll(Arrays.asList("Alpha", "Bravo", "Charlie", "delta")));
        assertEquals(4, populatedSet.size());
    }

    // ---- retainAll --------------------------------------------------------
    @Test
    public void testRetainAllKeepsIntersection() {
        List<String> keep = Arrays.asList("alpha", "delta"); // only "alpha" is present
        assertTrue(populatedSet.retainAll(keep));
        assertTrue(populatedSet.contains("Alpha"));
        assertFalse(populatedSet.contains("Bravo"));
        assertEquals(1, populatedSet.size());
    }

    @Test
    public void testRetainAllNoChangeWhenAllPresent() {
        List<String> all = Arrays.asList("alpha", "bravo", "charlie");
        assertFalse(populatedSet.retainAll(all));
        assertEquals(3, populatedSet.size());
    }

    // ---- removeAll --------------------------------------------------------
    @Test
    public void testRemoveAllRemovesIntersection() {
        List<String> toRemove = Arrays.asList("bravo", "delta");
        assertTrue(populatedSet.removeAll(toRemove));
        assertFalse(populatedSet.contains("Bravo"));
        assertEquals(2, populatedSet.size());
    }

    @Test
    public void testRemoveAllNoChangeWhenNoMatch() {
        List<String> none = Arrays.asList("delta", "echo");
        assertFalse(populatedSet.removeAll(none));
        assertEquals(3, populatedSet.size());
    }

    // ---- clear ------------------------------------------------------------
    @Test
    public void testClearEmptiesSet() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    // ---- equals -----------------------------------------------------------
    @Test
    public void testEqualsSameInstance() {
        assertTrue(populatedSet.equals(populatedSet));
    }

    @Test
    public void testEqualsDifferentSetSameElements() {
        Set<String> other = new HashSet<>(Arrays.asList("alpha", "BRAVO", "charlie"));
        assertTrue(populatedSet.equals(other));
    }

    @Test
    public void testEqualsDifferentSize() {
        Set<String> other = new HashSet<>(Arrays.asList("alpha", "bravo"));
        assertFalse(populatedSet.equals(other));
    }

    @Test
    public void testEqualsWithNonSetObject() {
        assertFalse(populatedSet.equals("not a set"));
    }

    // ---- hashCode ---------------------------------------------------------
    @Test
    public void testHashCodeIsCaseInsensitive() {
        Set<String> same = new HashSet<>(Arrays.asList("ALPHA", "bravo", "CHARLIE"));
        assertEquals(populatedSet.hashCode(), same.hashCode());
    }

    // ---- toString ---------------------------------------------------------
    @Test
    public void testToStringContainsElements() {
        String s = populatedSet.toString();
        assertTrue(s.contains("Alpha"));
        assertTrue(s.contains("Bravo"));
        assertTrue(s.contains("Charlie"));
    }

    // ---- Deprecated plus / minus -----------------------------------------
    @Test
    public void testMinusIterable() {
        populatedSet.minus(Arrays.asList("alpha", "delta"));
        assertFalse(populatedSet.contains("Alpha"));
        assertEquals(2, populatedSet.size());
    }

    @Test
    public void testMinusSingle() {
        populatedSet.minus("bravo");
        assertFalse(populatedSet.contains("Bravo"));
        assertEquals(2, populatedSet.size());
    }

    @Test
    public void testPlusIterable() {
        populatedSet.plus(Arrays.asList("delta", "echo"));
        assertTrue(populatedSet.containsAll(Arrays.asList("delta", "echo")));
        assertEquals(5, populatedSet.size());
    }

    @Test
    public void testPlusObject() {
        populatedSet.plus("FoO");
        assertTrue(populatedSet.contains("foo"));
        assertEquals(4, populatedSet.size());
    }

    /* ----------------------------------------------------------------------
     * 3. EDGE CASES & EXCEPTION SCENARIOS
     *
     * The implementation does not throw checked exceptions, but we can test:
     *   • Adding null values (null is allowed – it is stored as a key)
     *   • Removing null
     *   • Collections containing null
     *   • Using the constructor that accepts a backing map with pre‑populated data
     *   • Verifying that case‑insensitivity works for non‑String objects (they are compared normally)
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddNull() {
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertEquals(1, emptySet.size());

        // Adding another null should not increase size
        assertFalse(emptySet.add(null));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testRemoveNull() {
        emptySet.add(null);
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testCollectionWithNullsInConstructor() {
        List<String> listWithNull = Arrays.asList("A", null, "B");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(listWithNull);
        assertEquals(3, set.size());
        assertTrue(set.contains(null));
        assertTrue(set.contains("a"));
    }

    @Test
    public void testAddAllWithNulls() {
        List<String> list = Arrays.asList("X", null, "Y");
        assertTrue(emptySet.addAll(list));
        assertTrue(emptySet.contains("x"));
        assertTrue(emptySet.contains(null));
        assertEquals(3, emptySet.size());
    }

    @Test
    public void testRetainAllWithNulls() {
        emptySet.addAll(Arrays.asList("a", null, "b"));
        assertTrue(emptySet.retainAll(Arrays.asList(null, "B"))); // keep null and "b"
        assertEquals(2, emptySet.size());
        assertTrue(emptySet.contains(null));
        assertTrue(emptySet.contains("b"));
    }

    @Test
    public void testRemoveAllWithNulls() {
        emptySet.addAll(Arrays.asList("a", null, "b"));
        assertTrue(emptySet.removeAll(Arrays.asList(null)));
        assertFalse(emptySet.contains(null));
        assertEquals(2, emptySet.size());
    }

    @Test
    public void testNonStringElementsAreComparedNormally() {
        CaseInsensitiveSet<Object> mixed = new CaseInsensitiveSet<>();
        Integer i1 = 42;
        Integer i2 = new Integer(42);
        mixed.add(i1);
        // Adding another Integer with same value should be considered duplicate (hashCode/equals)
        assertFalse(mixed.add(i2));
        assertEquals(1, mixed.size());

        // Adding a distinct object with same toString but different type should be distinct
        mixed.add("42");
        assertEquals(2, mixed.size());
        assertTrue(mixed.contains("42"));
        assertTrue(mixed.contains(42));
    }

    @Test
    public void testBackingMapPrepopulated() {
        Map<String, Object> backing = new HashMap<>();
        backing.put("pre", new Object());
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(Collections.emptyList(), backing);
        // The set should see the pre‑populated entry
        assertTrue(set.contains("PRE"));
        assertEquals(1, set.size());
    }

    /* ----------------------------------------------------------------------
     * 4. COMBINED TESTS (already merged above)
     * ---------------------------------------------------------------------- */
}
