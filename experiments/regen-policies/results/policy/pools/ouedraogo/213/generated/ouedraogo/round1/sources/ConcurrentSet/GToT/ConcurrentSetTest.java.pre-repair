package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link ConcurrentSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors.</li>
 *   <li>All public methods (including those inherited from {@link Set}).</li>
 *   <li>Typical usage scenarios, edge cases (null handling, duplicate elements, empty collections, etc.)</li>
 *   <li>Exception‑free behaviour for illegal arguments (the class does not throw checked exceptions, but we verify that no unexpected NPEs occur).</li>
 * </ul>
 */
public class ConcurrentSetTest {

    private ConcurrentSet<String> set;

    @Before
    public void setUp() {
        set = new ConcurrentSet<>();
    }

    /* -------------------------- Constructors -------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructorFromCollectionHandlesNulls() {
        List<String> source = Arrays.asList("A", null, "B", null);
        ConcurrentSet<String> fromCol = new ConcurrentSet<>(source);
        assertEquals(3, fromCol.size());                 // "A", "B", null
        assertTrue(fromCol.contains("A"));
        assertTrue(fromCol.contains("B"));
        assertTrue(fromCol.contains(null));
    }

    @Test
    public void testConstructorFromSetHandlesNulls() {
        Set<String> source = new HashSet<>(Arrays.asList(null, "X"));
        ConcurrentSet<String> fromSet = new ConcurrentSet<>(source);
        assertEquals(2, fromSet.size());
        assertTrue(fromSet.contains(null));
        assertTrue(fromSet.contains("X"));
    }

    /* -------------------------- Basic Mutating Operations -------------------------- */

    @Test
    public void testAddAndContainsIncludingNull() {
        assertTrue(set.add("one"));
        assertTrue(set.contains("one"));
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        // duplicate adds should return false
        assertFalse(set.add("one"));
        assertFalse(set.add(null));
        assertEquals(2, set.size());
    }

    @Test
    public void testRemoveAndContains() {
        set.add("alpha");
        set.add(null);
        assertTrue(set.remove("alpha"));
        assertFalse(set.contains("alpha"));
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        // removing non‑existent element returns false
        assertFalse(set.remove("beta"));
    }

    @Test
    public void testAddAllReturnsTrueOnlyIfModified() {
        List<String> list = Arrays.asList("a", "b", null);
        assertTrue(set.addAll(list));          // first time modifies
        assertFalse(set.addAll(list));         // second time no modification
        assertEquals(3, set.size());
        assertTrue(set.containsAll(list));
    }

    @Test
    public void testRemoveAll() {
        set.addAll(Arrays.asList("x", "y", null, "z"));
        assertTrue(set.removeAll(Arrays.asList("y", null)));
        assertFalse(set.contains("y"));
        assertFalse(set.contains(null));
        assertTrue(set.contains("x"));
        assertTrue(set.contains("z"));
        // removing a collection that has no effect returns false
        assertFalse(set.removeAll(Collections.singleton("non‑existent")));
    }

    @Test
    public void testRetainAll() {
        set.addAll(Arrays.asList("keep1", "keep2", "discard", null));
        Collection<String> toRetain = Arrays.asList("keep1", null);
        assertTrue(set.retainAll(toRetain));
        assertEquals(2, set.size());
        assertTrue(set.contains("keep1"));
        assertTrue(set.contains(null));
        assertFalse(set.contains("keep2"));
        assertFalse(set.contains("discard"));
        // retaining with same collection should return false (no change)
        assertFalse(set.retainAll(toRetain));
    }

    @Test
    public void testContainsAll() {
        set.addAll(Arrays.asList("a", "b", null));
        assertTrue(set.containsAll(Arrays.asList("a", null)));
        assertFalse(set.containsAll(Arrays.asList("a", "c")));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
        set.add("elem");
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
    }

    @Test
    public void testClear() {
        set.addAll(Arrays.asList("x", null, "y"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /* -------------------------- Iterator Behaviour -------------------------- */

    @Test
    public void testIteratorTraversesAllElementsIncludingNull() {
        set.addAll(Arrays.asList("first", null, "second"));
        Set<String> visited = new HashSet<>();
        for (String s : set) {
            visited.add(s);
        }
        assertEquals(3, visited.size());
        assertTrue(visited.contains("first"));
        assertTrue(visited.contains("second"));
        assertTrue(visited.contains(null));
    }

    @Test
    public void testIteratorRemove() {
        set.addAll(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("b".equals(val)) {
                it.remove();
            }
        }
        assertEquals(2, set.size());
        assertFalse(set.contains("b"));
    }

    /* -------------------------- Array Conversions -------------------------- */

    @Test
    public void testToArrayWithoutParameter() {
        set.addAll(Arrays.asList("one", null, "two"));
        Object[] array = set.toArray();
        assertEquals(3, array.length);
        List<Object> list = Arrays.asList(array);
        assertTrue(list.contains("one"));
        assertTrue(list.contains("two"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayWithExactSizedArray() {
        set.addAll(Arrays.asList("x", null));
        String[] target = new String[2];
        String[] result = set.toArray(target);
        assertSame(target, result);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("x"));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayWithLargerArray() {
        set.addAll(Arrays.asList("p", "q"));
        String[] target = new String[5];
        String[] result = set.toArray(target);
        // The same array instance is returned because it was large enough
        assertSame(target, result);
        assertEquals("p", result[0]); // order is undefined, just check presence
        assertEquals("q", result[1]);
        assertNull(result[2]); // first null after the last element per contract
        // remaining positions stay untouched (could be null or previous data)
    }

    @Test
    public void testToArrayWithSmallerArray() {
        set.addAll(Arrays.asList("alpha", "beta", null));
        String[] small = new String[2];
        String[] result = set.toArray(small);
        // A new array is allocated because the supplied one was too small
        assertNotSame(small, result);
        assertEquals(3, result.length);
        List<String> list = Arrays.asList(result);
        assertTrue(list.contains("alpha"));
        assertTrue(list.contains("beta"));
        assertTrue(list.contains(null));
    }

    /* -------------------------- Equality, HashCode & toString -------------------------- */

    @Test
    public void testEqualsAndHashCodeWithSameElements() {
        Set<String> reference = new HashSet<>(Arrays.asList("a", null, "b"));
        set.addAll(reference);
        assertEquals(reference, set);
        assertEquals(set, reference);
        // hashCode must be sum of individual hash codes (null contributes 0)
        int expectedHash = "a".hashCode() + "b".hashCode(); // null adds 0
        assertEquals(expectedHash, set.hashCode());
    }

    @Test
    public void testEqualsWithDifferentSizesOrTypes() {
        set.addAll(Arrays.asList("x", "y"));
        assertFalse(set.equals(null));
        assertFalse(set.equals("not a set"));
        Set<String> other = new HashSet<>(Collections.singletonList("x"));
        assertFalse(set.equals(other)); // size differs
    }

    @Test
    public void testToStringFormatting() {
        // Empty set
        assertEquals("{}", set.toString());

        // Non‑empty set (order not guaranteed)
        set.addAll(Arrays.asList("first", null, "second"));
        String s = set.toString();
        assertTrue(s.startsWith("{"));
        assertTrue(s.endsWith("}"));
        // Remove surrounding braces and split by ", "
        String inner = s.substring(1, s.length() - 1);
        Set<String> parsed = new HashSet<>(Arrays.asList(inner.split(", ")));
        // The string "null" is used for the actual null element
        assertTrue(parsed.contains("first"));
        assertTrue(parsed.contains("second"));
        assertTrue(parsed.contains("null"));
    }

    /* -------------------------- Additional Edge Cases -------------------------- */

    @Test
    public void testAddNullMultipleTimes() {
        assertTrue(set.add(null));
        assertFalse(set.add(null)); // second add of null returns false
        assertEquals(1, set.size());
        assertTrue(set.contains(null));
    }

    @Test
    public void testRemoveNullWhenNotPresent() {
        assertFalse(set.remove(null));
        set.add("value");
        assertFalse(set.remove(null));
    }

    @Test
    public void testRetainAllWithEmptyCollection() {
        set.addAll(Arrays.asList("a", "b", null));
        assertTrue(set.retainAll(Collections.emptyList()));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testContainsWithIncompatibleType() {
        set.addAll(Arrays.asList("string"));
        // Passing an Integer should simply return false, not throw ClassCastException
        assertFalse(set.contains(123));
    }
}
