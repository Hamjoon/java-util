package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods inherited from {@link CompactSet} (size, isEmpty, contains, add, remove,
 *       clear, containsAll, addAll, retainAll, removeAll, iterator, toArray, toArray(T[]), equals,
 *       hashCode, toString).</li>
 *   <li>Both public constructors of {@code CompactLinkedSet}.</li>
 *   <li>Edge‑case and exceptional behaviours such as null arguments, duplicate elements,
 *       case‑insensitivity expectations, and iterator ordering.</li>
 * </ul>
 */
public class CompactLinkedSetTest {

    private CompactLinkedSet<String> emptySet;
    private CompactLinkedSet<String> populatedSet;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        emptySet = new CompactLinkedSet<>();
        populatedSet = new CompactLinkedSet<>(Arrays.asList("Apple", "Banana", "Cherry"));
    }

    /* ----------------------------------------------------------------------
     *  Constructor tests
     * ---------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertNotNull(emptySet);
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        Collection<String> source = Arrays.asList("One", "Two", "Three");
        CompactLinkedSet<String> set = new CompactLinkedSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
        // verify insertion order is preserved
        Iterator<String> it = set.iterator();
        assertEquals("One", it.next());
        assertEquals("Two", it.next());
        assertEquals("Three", it.next());
    }

    @Test
    public void testCollectionConstructorThrowsNPEWhenNull() {
        thrown.expect(NullPointerException.class);
        new CompactLinkedSet<String>(null);
    }

    /* ----------------------------------------------------------------------
     *  size() / isEmpty()
     * ---------------------------------------------------------------------- */

    @Test
    public void testSizeAndIsEmpty() {
        assertEquals(0, emptySet.size());
        assertTrue(emptySet.isEmpty());

        assertEquals(3, populatedSet.size());
        assertFalse(populatedSet.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  add(E) and contains(Object)
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(emptySet.add("Alpha"));
        assertTrue(emptySet.contains("Alpha"));
        assertEquals(1, emptySet.size());

        // adding duplicate should return false and not increase size
        assertFalse(emptySet.add("Alpha"));
        assertEquals(1, emptySet.size());

        // null element handling (CompactSet allows null, but we verify behaviour)
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertFalse(emptySet.add(null)); // duplicate null
    }

    @Test
    public void testCaseInsensitiveBehavior() throws Exception {
        // The class claims to be case‑insensitive; we test that both cases are considered equal.
        // Because the underlying map is built with caseSensitive(true) this may not hold,
        // but the contract of CompactLinkedSet says it is case‑insensitive, so we verify it.
        emptySet.add("Test");
        assertTrue(emptySet.contains("TEST"));
        assertTrue(emptySet.contains("test"));
        // removal should also be case‑insensitive
        assertTrue(emptySet.remove("tEsT"));
        assertFalse(emptySet.contains("Test"));
    }

    /* ----------------------------------------------------------------------
     *  remove(Object)
     * ---------------------------------------------------------------------- */

    @Test
    public void testRemove() {
        assertTrue(populatedSet.remove("Banana"));
        assertFalse(populatedSet.contains("Banana"));
        assertEquals(2, populatedSet.size());

        // removing non‑existent element returns false
        assertFalse(populatedSet.remove("Durian"));
    }

    /* ----------------------------------------------------------------------
     *  clear()
     * ---------------------------------------------------------------------- */

    @Test
    public void testClear() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    /* ----------------------------------------------------------------------
     *  containsAll(Collection<?>)
     * ---------------------------------------------------------------------- */

    @Test
    public void testContainsAll() {
        Collection<String> subset = Arrays.asList("Apple", "Cherry");
        assertTrue(populatedSet.containsAll(subset));

        Collection<String> notSubset = Arrays.asList("Apple", "Durian");
        assertFalse(populatedSet.containsAll(notSubset));
    }

    /* ----------------------------------------------------------------------
     *  addAll(Collection<? extends E>)
     * ---------------------------------------------------------------------- */

    @Test
    public void testAddAll() {
        Collection<String> more = Arrays.asList("Durian", "Elderberry", "Apple"); // Apple already present
        assertTrue(populatedSet.addAll(more));
        assertEquals(5, populatedSet.size()); // only two new elements added
        assertTrue(populatedSet.containsAll(Arrays.asList("Durian", "Elderberry")));
    }

    @Test
    public void testAddAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        populatedSet.addAll(null);
    }

    /* ----------------------------------------------------------------------
     *  retainAll(Collection<?>)
     * ---------------------------------------------------------------------- */

    @Test
    public void testRetainAll() {
        Collection<String> retain = Arrays.asList("Apple", "Banana");
        assertTrue(populatedSet.retainAll(retain));
        assertEquals(2, populatedSet.size());
        assertTrue(populatedSet.containsAll(retain));
        assertFalse(populatedSet.contains("Cherry"));
    }

    @Test
    public void testRetainAllWithEmptyCollectionEmptiesSet() {
        assertTrue(populatedSet.retainAll(Collections.emptyList()));
        assertTrue(populatedSet.isEmpty());
    }

    /* ----------------------------------------------------------------------
     *  removeAll(Collection<?>)
     * ---------------------------------------------------------------------- */

    @Test
    public void testRemoveAll() {
        Collection<String> toRemove = Arrays.asList("Apple", "Cherry", "NonExisting");
        assertTrue(populatedSet.removeAll(toRemove));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Banana"));
    }

    @Test
    public void testRemoveAllWithEmptyCollectionDoesNothing() {
        assertFalse(populatedSet.removeAll(Collections.emptyList()));
        assertEquals(3, populatedSet.size());
    }

    /* ----------------------------------------------------------------------
     *  iterator()
     * ---------------------------------------------------------------------- */

    @Test
    public void testIteratorOrderPreservesInsertionOrder() {
        List<String> expected = Arrays.asList("Apple", "Banana", "Cherry");
        List<String> actual = new ArrayList<>();
        for (String s : populatedSet) {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testIteratorRemove() {
        Iterator<String> it = populatedSet.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("Banana".equals(val)) {
                it.remove();
            }
        }
        assertFalse(populatedSet.contains("Banana"));
        assertEquals(2, populatedSet.size());
    }

    /* ----------------------------------------------------------------------
     *  toArray()
     * ---------------------------------------------------------------------- */

    @Test
    public void testToArray() {
        Object[] array = populatedSet.toArray();
        assertArrayEquals(new Object[]{"Apple", "Banana", "Cherry"}, array);
    }

    @Test
    public void testToArrayTyped() {
        String[] a = new String[0];
        String[] array = populatedSet.toArray(a);
        assertArrayEquals(new String[]{"Apple", "Banana", "Cherry"}, array);
    }

    /* ----------------------------------------------------------------------
     *  equals(Object) and hashCode()
     * ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        Set<String> other = new LinkedHashSet<>(Arrays.asList("Apple", "Banana", "Cherry"));
        assertTrue(populatedSet.equals(other));
        assertEquals(populatedSet.hashCode(), other.hashCode());

        // different order should still be equal because insertion order is preserved in both sets
        Set<String> differentOrder = new LinkedHashSet<>(Arrays.asList("Cherry", "Banana", "Apple"));
        assertFalse(populatedSet.equals(differentOrder));
    }

    @Test
    public void testEqualsWithSelfAndNull() {
        assertTrue(populatedSet.equals(populatedSet));
        assertFalse(populatedSet.equals(null));
        assertFalse(populatedSet.equals("some string"));
    }

    /* ----------------------------------------------------------------------
     *  toString()
     * ---------------------------------------------------------------------- */

    @Test
    public void testToStringContainsElements() {
        String s = populatedSet.toString();
        assertTrue(s.contains("Apple"));
        assertTrue(s.contains("Banana"));
        assertTrue(s.contains("Cherry"));
        // typical Set toString format: [Apple, Banana, Cherry]
        assertTrue(s.startsWith("[") && s.endsWith("]"));
    }

    /* ----------------------------------------------------------------------
     *  Protected methods accessed via reflection (edge‑case verification)
     * ---------------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveViaReflection() throws Exception {
        java.lang.reflect.Method m = CompactLinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (Boolean) m.invoke(populatedSet);
        assertTrue("CompactLinkedSet should report case‑insensitive", result);
    }

    @Test
    public void testGetNewSetViaReflectionReturnsNull() throws Exception {
        java.lang.reflect.Method m = CompactLinkedSet.class.getDeclaredMethod("getNewSet");
        m.setAccessible(true);
        Object result = m.invoke(populatedSet);
        assertNull("Deprecated getNewSet() should return null", result);
    }

    /* ----------------------------------------------------------------------
     *  compactSize() edge case (protected method)
     * ---------------------------------------------------------------------- */

    @Test
    public void testCompactSizeIsAtLeastTwo() throws Exception {
        java.lang.reflect.Method m = CompactSet.class.getDeclaredMethod("compactSize");
        m.setAccessible(true);
        int size = (Integer) m.invoke(populatedSet);
        assertTrue("compactSize should be >= 2 for a valid set", size >= 2);
    }
}
