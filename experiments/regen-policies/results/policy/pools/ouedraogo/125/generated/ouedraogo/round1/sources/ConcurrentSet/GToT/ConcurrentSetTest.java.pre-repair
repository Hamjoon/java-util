package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class ConcurrentSetTest {

    private ConcurrentSet<String> emptySet;
    private ConcurrentSet<String> setWithElements;

    @Before
    public void setUp() {
        emptySet = new ConcurrentSet<>();
        setWithElements = new ConcurrentSet<>();
        setWithElements.add("alpha");
        setWithElements.add("beta");
        setWithElements.add(null);
    }

    // ----- Constructors -----------------------------------------------------

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        ConcurrentSet<Integer> cs = new ConcurrentSet<>();
        assertTrue(cs.isEmpty());
        assertEquals(0, cs.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsIncludingNull() {
        List<String> src = Arrays.asList("x", null, "y", "x"); // duplicate "x"
        ConcurrentSet<String> cs = new ConcurrentSet<>(src);
        assertEquals(3, cs.size()); // x, y, null
        assertTrue(cs.contains("x"));
        assertTrue(cs.contains("y"));
        assertTrue(cs.contains(null));
    }

    @Test
    public void testSetConstructorCopiesElementsIncludingNull() {
        Set<String> src = new HashSet<>(Arrays.asList(null, "a"));
        ConcurrentSet<String> cs = new ConcurrentSet<>(src);
        assertEquals(2, cs.size());
        assertTrue(cs.contains(null));
        assertTrue(cs.contains("a"));
    }

    // ----- equals & hashCode ------------------------------------------------

    @Test
    public void testEqualsSameElements() {
        ConcurrentSet<String> other = new ConcurrentSet<>();
        other.add("beta");
        other.add(null);
        other.add("alpha");
        assertTrue(setWithElements.equals(other));
        assertTrue(other.equals(setWithElements));
    }

    @Test
    public void testEqualsDifferentSize() {
        ConcurrentSet<String> other = new ConcurrentSet<>();
        other.add("alpha");
        other.add("beta");
        // missing null
        assertFalse(setWithElements.equals(other));
    }

    @Test
    public void testEqualsWithNonSetObject() {
        assertFalse(setWithElements.equals("not a set"));
        assertFalse(setWithElements.equals(null));
    }

    @Test
    public void testEqualsHandlesClassCastExceptionGracefully() {
        // Create a set containing an Integer; the iteration will try to cast to String
        Set raw = new HashSet();
        raw.add(42);
        ConcurrentSet rawSet = new ConcurrentSet(raw);
        assertFalse(setWithElements.equals(rawSet));
    }

    @Test
    public void testHashCodeWithNulls() {
        // hash = "alpha".hashCode() + "beta".hashCode() + 0
        int expected = "alpha".hashCode() + "beta".hashCode();
        assertEquals(expected, setWithElements.hashCode());
    }

    @Test
    public void testHashCodeEmptySet() {
        assertEquals(0, emptySet.hashCode());
    }

    // ----- toString ---------------------------------------------------------

    @Test
    public void testToStringEmpty() {
        assertEquals("{}", emptySet.toString());
    }

    @Test
    public void testToStringContainsElementsAndNull() {
        String s = setWithElements.toString();
        // Order is unspecified, so we check for presence of each token
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
        assertTrue(s.contains("null"));
    }

    @Test
    public void testToStringSelfReference() {
        ConcurrentSet<Object> cs = new ConcurrentSet<>();
        cs.add(cs); // add the set itself
        assertEquals("{(this Set)}", cs.toString());
    }

    // ----- size & isEmpty ---------------------------------------------------

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());

        assertFalse(setWithElements.isEmpty());
        assertEquals(3, setWithElements.size());
    }

    // ----- contains ---------------------------------------------------------

    @Test
    public void testContains() {
        assertTrue(setWithElements.contains("alpha"));
        assertTrue(setWithElements.contains(null));
        assertFalse(setWithElements.contains("gamma"));
    }

    // ----- iterator ---------------------------------------------------------

    @Test
    public void testIteratorIteratesAllElements() {
        Set<String> seen = new HashSet<>();
        for (String s : setWithElements) {
            seen.add(s);
        }
        assertEquals(setWithElements.size(), seen.size());
        assertTrue(seen.contains("alpha"));
        assertTrue(seen.contains("beta"));
        assertTrue(seen.contains(null));
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNextThrows() {
        Iterator<String> it = setWithElements.iterator();
        it.remove(); // should throw IllegalStateException
    }

    @Test
    public void testIteratorRemoveWorks() {
        Iterator<String> it = setWithElements.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("alpha".equals(val)) {
                it.remove();
            }
        }
        assertFalse(setWithElements.contains("alpha"));
        assertEquals(2, setWithElements.size());
    }

    // ----- toArray ----------------------------------------------------------

    @Test
    public void testToArrayReturnsAllElements() {
        Object[] arr = setWithElements.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains("alpha"));
        assertTrue(list.contains("beta"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayTypedExactSize() {
        String[] a = new String[3];
        String[] result = setWithElements.toArray(a);
        assertSame(a, result);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("alpha"));
        assertTrue(list.contains("beta"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayTypedLargerArrayGetsTrailingNull() {
        String[] a = new String[5];
        String[] result = setWithElements.toArray(a);
        assertSame(a, result);
        assertNull(result[3]); // first extra slot
        assertNull(result[4]); // second extra slot
    }

    @Test
    public void testToArrayTypedSmallerArrayCreatesNewOne() {
        String[] a = new String[1];
        String[] result = setWithElements.toArray(a);
        assertNotSame(a, result);
        assertEquals(3, result.length);
    }

    // ----- containsAll -----------------------------------------------------

    @Test
    public void testContainsAllTrue() {
        List<String> sub = Arrays.asList("alpha", null);
        assertTrue(setWithElements.containsAll(sub));
    }

    @Test
    public void testContainsAllFalse() {
        List<String> sub = Arrays.asList("alpha", "gamma");
        assertFalse(setWithElements.containsAll(sub));
    }

    // ----- add --------------------------------------------------------------

    @Test
    public void testAddNewElement() {
        assertTrue(emptySet.add("new"));
        assertTrue(emptySet.contains("new"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testAddDuplicateReturnsFalse() {
        assertTrue(setWithElements.add("alpha"));
        assertFalse(setWithElements.add("alpha"));
    }

    @Test
    public void testAddNullOnlyOnce() {
        assertTrue(emptySet.add(null));
        assertFalse(emptySet.add(null));
        assertEquals(1, emptySet.size());
    }

    // ----- remove -----------------------------------------------------------

    @Test
    public void testRemoveExistingElement() {
        assertTrue(setWithElements.remove("beta"));
        assertFalse(setWithElements.contains("beta"));
        assertEquals(2, setWithElements.size());
    }

    @Test
    public void testRemoveNonExistingElement() {
        assertFalse(setWithElements.remove("gamma"));
    }

    @Test
    public void testRemoveNull() {
        assertTrue(setWithElements.remove(null));
        assertFalse(setWithElements.contains(null));
        assertEquals(2, setWithElements.size());
    }

    // ----- addAll -----------------------------------------------------------

    @Test
    public void testAddAllAddsAllMissingElements() {
        List<String> toAdd = Arrays.asList("alpha", "gamma", null);
        assertTrue(setWithElements.addAll(toAdd)); // at least "gamma" is new
        assertTrue(setWithElements.contains("gamma"));
        assertEquals(3, setWithElements.size()); // null and alpha already present
    }

    @Test
    public void testAddAllReturnsFalseWhenNoChange() {
        List<String> same = Arrays.asList("alpha", "beta", null);
        assertFalse(setWithElements.addAll(same));
    }

    // ----- removeAll --------------------------------------------------------

    @Test
    public void testRemoveAllRemovesExistingElements() {
        List<String> toRemove = Arrays.asList("alpha", "nonexistent", null);
        assertTrue(setWithElements.removeAll(toRemove));
        assertFalse(setWithElements.contains("alpha"));
        assertFalse(setWithElements.contains(null));
        assertEquals(1, setWithElements.size()); // only "beta" left
    }

    @Test
    public void testRemoveAllReturnsFalseWhenNothingRemoved() {
        List<String> toRemove = Arrays.asList("x", "y");
        assertFalse(setWithElements.removeAll(toRemove));
    }

    // ----- retainAll --------------------------------------------------------

    @Test
    public void testRetainAllKeepsIntersection() {
        List<String> keep = Arrays.asList("beta", null, "extra");
        assertTrue(setWithElements.retainAll(keep));
        assertTrue(setWithElements.contains("beta"));
        assertTrue(setWithElements.contains(null));
        assertFalse(setWithElements.contains("alpha"));
        assertEquals(2, setWithElements.size());
    }

    @Test
    public void testRetainAllWithEmptyCollectionEmptiesSet() {
        assertTrue(setWithElements.retainAll(Collections.emptyList()));
        assertTrue(setWithElements.isEmpty());
    }

    @Test
    public void testRetainAllReturnsFalseWhenNoChange() {
        List<String> same = Arrays.asList("alpha", "beta", null);
        assertFalse(setWithElements.retainAll(same));
    }

    // ----- clear ------------------------------------------------------------

    @Test
    public void testClearEmptiesSet() {
        setWithElements.clear();
        assertTrue(setWithElements.isEmpty());
        assertEquals(0, setWithElements.size());
    }

    @Test
    public void testClearOnAlreadyEmptySet() {
        emptySet.clear(); // should be a no‑op
        assertTrue(emptySet.isEmpty());
    }
}
