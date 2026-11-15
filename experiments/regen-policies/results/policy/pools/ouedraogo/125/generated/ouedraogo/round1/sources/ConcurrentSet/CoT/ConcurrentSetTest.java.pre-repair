package com.cedarsoftware.util;

import org.junit.Test;
import static org.junit.Assert.*;

import java.util.*;

public class ConcurrentSetTest {

    /* -------------------------------------------------
       1. Constructors & basic mutators
       ------------------------------------------------- */

    @Test
    public void testEmptyConstructor() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testCollectionConstructor() {
        List<String> list = Arrays.asList("a", "b", null);
        ConcurrentSet<String> set = new ConcurrentSet<>(list);
        assertEquals(3, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains("b"));
        assertTrue(set.contains(null));
    }

    @Test
    public void testSetConstructor() {
        Set<Integer> src = new HashSet<>(Arrays.asList(1, 2, 3));
        ConcurrentSet<Integer> set = new ConcurrentSet<>(src);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(src));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollection() {
        new ConcurrentSet<String>((Collection<String>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullSet() {
        new ConcurrentSet<String>((Set<String>) null);
    }

    /* -------------------------------------------------
       2. add / contains / null handling
       ------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        assertTrue(set.add(10));
        assertTrue(set.contains(10));
        assertFalse(set.add(10)); // duplicate
    }

    @Test
    public void testAddNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertFalse(set.add(null));
    }

    @Test
    public void testAddAllWithDuplicatesAndNulls() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        List<String> src = Arrays.asList("a", null, "a", null);
        assertTrue(set.addAll(src));
        assertEquals(2, set.size());
        assertTrue(set.contains("a"));
        assertTrue(set.contains(null));
    }

    /* -------------------------------------------------
       3. remove / removeAll / retainAll
       ------------------------------------------------- */

    @Test
    public void testRemove() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add("x");
        assertTrue(set.remove("x"));
        assertFalse(set.contains("x"));
        assertFalse(set.remove("x"));
    }

    @Test
    public void testRemoveNull() {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        set.add(null);
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
    }

    @Test
    public void testRemoveAllWithAbsentElementsAndNull() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, null));
        List<Integer> toRemove = Arrays.asList(3, null, 4);
        assertTrue(set.removeAll(toRemove));
        assertEquals(2, set.size());
        assertTrue(set.contains(1));
        assertTrue(set.contains(2));
        assertFalse(set.contains(null));
    }

    @Test
    public void testRetainAllWithEmptyCollection() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("x", "y"));
        assertTrue(set.retainAll(Collections.emptyList()));
        assertTrue(set.isEmpty());
    }

    @Test
    public void testRetainAll() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, 3, 4));
        List<Integer> retain = Arrays.asList(2, 4, 6);
        assertTrue(set.retainAll(retain));
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList(2, 4)));
    }

    /* -------------------------------------------------
       4. clear
       ------------------------------------------------- */

    @Test
    public void testClear() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", "b"));
        set.clear();
        assertTrue(set.isEmpty());
    }

    /* -------------------------------------------------
       5. iterator behaviour
       ------------------------------------------------- */

    @Test
    public void testIteratorAndRemoveViaIterator() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a", "b", "c"));
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        String first = it.next();
        it.remove();
        assertFalse(set.contains(first));
        assertEquals(2, set.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testIteratorRemoveBeforeNext() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a"));
        Iterator<String> it = set.iterator();
        it.remove(); // illegal state
    }

    /* -------------------------------------------------
       6. toArray variants
       ------------------------------------------------- */

    @Test
    public void testToArray() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2, null));
        Object[] arr = set.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.contains(1));
        assertTrue(list.contains(2));
        assertTrue(list.contains(null));
    }

    @Test
    public void testToArrayWithTypedArray() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("x", null));
        // a is smaller than needed – method must allocate a new array
        String[] a = new String[1];
        String[] result = set.toArray(a);
        assertEquals(2, result.length);
        assertTrue(Arrays.asList(result).contains("x"));
        assertTrue(Arrays.asList(result).contains(null));

        // a is larger – extra slot must be set to null
        String[] big = new String[5];
        String[] bigResult = set.toArray(big);
        assertSame(big, bigResult);
        assertNull(bigResult[2]); // index after size
    }

    @Test(expected = ArrayStoreException.class)
    public void testToArrayWithIncompatibleArrayType() {
        ConcurrentSet<Integer> set = new ConcurrentSet<>(Arrays.asList(1, 2));
        set.toArray(new String[2]); // should throw
    }

    /* -------------------------------------------------
       7. equals / hashCode
       ------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        ConcurrentSet<String> s1 = new ConcurrentSet<>(Arrays.asList("a", null, "b"));
        Set<String> javaSet = new HashSet<>(Arrays.asList("b", "a", null));
        assertTrue(s1.equals(javaSet));
        assertEquals(s1.hashCode(), javaSet.hashCode());

        Set<String> other = new HashSet<>(Arrays.asList("a", "b"));
        assertFalse(s1.equals(other));
    }

    @Test
    public void testEqualsWithNonSetObject() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a"));
        assertFalse(set.equals("not a set"));
    }

    @Test
    public void testEqualsWithIncompatibleElementTypes() {
        ConcurrentSet<String> set = new ConcurrentSet<>(Arrays.asList("a"));
        Set<Integer> other = new HashSet<>(Arrays.asList(1));
        // internal contains check will throw ClassCastException; equals must catch and return false
        assertFalse(set.equals(other));
    }

    /* -------------------------------------------------
       8. toString edge cases
       ------------------------------------------------- */

    @Test
    public void testToStringEmptyAndSelfReference() {
        ConcurrentSet<Object> set = new ConcurrentSet<>();
        assertEquals("{}", set.toString());

        // add the set itself as an element – special handling in toString()
        set.add(set);
        assertTrue(set.toString().contains("(this Set)"));
    }
}
