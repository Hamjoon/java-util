package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite that exercises all public behaviour of {@link CompactCILinkedSet}
 * as well as the protected members that are specific to the case‑insensitive
 * implementation (accessed via reflection).
 *
 * The tests intentionally avoid any external library calls – they only use the
 * constructors and methods that are declared in {@code CompactCILinkedSet} or its
 * superclass {@code CompactSet}.
 */
public class CompactCILinkedSetTest {

    private CompactCILinkedSet<String> ciSet;

    @Before
    public void setUp() {
        ciSet = new CompactCILinkedSet<>();
    }

    /** -------------------------------------------------------------------- *
     *  Basic construction & emptiness checks
     *  -------------------------------------------------------------------- */
    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue("New set must be empty", ciSet.isEmpty());
        assertEquals("Size of a new set must be 0", 0, ciSet.size());
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorNullArgument() {
        new CompactCILinkedSet<String>((Collection<String>) null);
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        Collection<String> source = Arrays.asList("One", "Two", "Three");
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(source);
        assertEquals("All elements must be copied", 3, set.size());
        assertTrue(set.containsAll(source));
    }

    /** -------------------------------------------------------------------- *
     *  Case‑insensitive semantics
     *  -------------------------------------------------------------------- */
    @Test
    public void testCaseInsensitiveAddAndContains() {
        assertTrue(ciSet.add("Apple"));
        assertTrue("Contains must be case‑insensitive", ciSet.contains("APPLE"));
        assertTrue("Contains must be case‑insensitive (lower case)", ciSet.contains("apple"));
        assertFalse("Adding a duplicate with different case must return false",
                ciSet.add("aPPle"));
        assertEquals("Set size must remain 1 after duplicate add", 1, ciSet.size());
    }

    @Test
    public void testCaseInsensitiveRemove() {
        ciSet.add("Banana");
        assertTrue(ciSet.remove("BANANA"));
        assertFalse(ciSet.contains("banana"));
        assertEquals(0, ciSet.size());
    }

    /** -------------------------------------------------------------------- *
     *  Insertion order preservation (Linked behaviour)
     *  -------------------------------------------------------------------- */
    @Test
    public void testInsertionOrderIsPreserved() {
        List<String> items = Arrays.asList("first", "second", "third");
        for (String s : items) {
            ciSet.add(s);
        }

        Iterator<String> it = ciSet.iterator();
        for (String expected : items) {
            assertTrue("Iterator should have next element", it.hasNext());
            assertEquals(expected, it.next());
        }
        assertFalse("Iterator should be exhausted", it.hasNext());
    }

    /** -------------------------------------------------------------------- *
     *  Bulk operations – addAll, containsAll, retainAll, removeAll
     *  -------------------------------------------------------------------- */
    @Test
    public void testAddAllAndContainsAll() {
        Collection<String> toAdd = Arrays.asList("alpha", "BETA", "Gamma");
        assertTrue(ciSet.addAll(toAdd));
        assertTrue(ciSet.containsAll(Arrays.asList("ALPHA", "beta", "GAMMA")));
        // duplicate addAll must return false
        assertFalse(ciSet.addAll(toAdd));
        assertEquals(3, ciSet.size());
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        ciSet.addAll(Arrays.asList("one", "two", "three"));
        assertTrue(ciSet.retainAll(Arrays.asList("TWO", "FOUR")));
        assertEquals(1, ciSet.size());
        assertTrue(ciSet.contains("two"));
    }

    @Test
    public void testRemoveAllEliminatesSpecifiedElements() {
        ciSet.addAll(Arrays.asList("red", "green", "blue"));
        assertTrue(ciSet.removeAll(Arrays.asList("GREEN", "yellow")));
        assertEquals(2, ciSet.size());
        assertFalse(ciSet.contains("green"));
        assertTrue(ciSet.contains("red"));
        assertTrue(ciSet.contains("blue"));
    }

    /** -------------------------------------------------------------------- *
     *  toArray variants
     *  -------------------------------------------------------------------- */
    @Test
    public void testToArrayReturnsAllElementsInInsertionOrder() {
        ciSet.addAll(Arrays.asList("a", "b", "c"));
        Object[] array = ciSet.toArray();
        assertArrayEquals(new Object[]{"a", "b", "c"}, array);
    }

    @Test
    public void testToTypedArray() {
        ciSet.addAll(Arrays.asList("x", "y"));
        String[] typed = ciSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"x", "y"}, typed);
    }

    /** -------------------------------------------------------------------- *
     *  clear, isEmpty, size, equals, hashCode, toString
     *  -------------------------------------------------------------------- */
    @Test
    public void testClearAndIsEmpty() {
        ciSet.addAll(Arrays.asList("p", "q"));
        assertFalse(ciSet.isEmpty());
        ciSet.clear();
        assertTrue(ciSet.isEmpty());
        assertEquals(0, ciSet.size());
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCILinkedSet<String> other = new CompactCILinkedSet<>();
        ciSet.addAll(Arrays.asList("x", "y"));
        other.addAll(Arrays.asList("X", "Y"));
        assertEquals("Sets with same elements (case‑insensitive) must be equal", ciSet, other);
        assertEquals(ciSet.hashCode(), other.hashCode());
    }

    @Test
    public void testToStringContainsAllElements() {
        ciSet.addAll(Arrays.asList("foo", "bar"));
        String s = ciSet.toString();
        assertTrue(s.contains("foo"));
        assertTrue(s.contains("bar"));
    }

    /** -------------------------------------------------------------------- *
     *  Protected API verification via reflection
     *  -------------------------------------------------------------------- */
    @Test
    public void testIsCaseInsensitiveProtectedMethodReturnsTrue() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        boolean result = (boolean) m.invoke(ciSet);
        assertTrue("isCaseInsensitive() must return true for this subclass", result);
    }

    @Test
    public void testGetNewSetProtectedMethodReturnsNull() throws Exception {
        Method m = CompactCILinkedSet.class.getDeclaredMethod("getNewSet");
        m.setAccessible(true);
        Object result = m.invoke(ciSet);
        assertNull("Deprecated getNewSet() must return null", result);
    }

    /** -------------------------------------------------------------------- *
     *  Null handling – the underlying CompactSet permits a single null element.
     *  -------------------------------------------------------------------- */
    @Test
    public void testNullElementBehaviour() {
        assertTrue("First null add must succeed", ciSet.add(null));
        assertTrue("Set must contain null after add", ciSet.contains(null));
        assertFalse("Second null add must be reported as duplicate", ciSet.add(null));
        assertEquals(1, ciSet.size());

        assertTrue("Removing null must succeed", ciSet.remove(null));
        assertFalse(ciSet.contains(null));
        assertEquals(0, ciSet.size());
    }
}
