package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;

import java.util.*;

import static org.junit.Assert.*;

public class CaseInsensitiveSetTest {

    private CaseInsensitiveSet<String> set;

    @Before
    public void setUp() {
        set = new CaseInsensitiveSet<>();
    }

    /*** Basic add / contains / size behavior with case‑insensitivity ***/
    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(set.add("Hello"));
        // duplicate with different case must not be added
        assertFalse(set.add("hello"));
        assertTrue(set.contains("HELLO"));
        assertEquals(1, set.size());

        // non‑String element should behave normally
        CaseInsensitiveSet<Object> mixed = new CaseInsensitiveSet<>();
        assertTrue(mixed.add(123));
        assertTrue(mixed.add("Apple"));
        assertTrue(mixed.add("apple")); // duplicate string (case‑insensitive)
        assertFalse(mixed.add("APPLE"));
        assertEquals(2, mixed.size());
        assertTrue(mixed.contains(123));
        assertTrue(mixed.contains("APPLE"));
    }

    /*** remove operation is case‑insensitive for strings ***/
    @Test
    public void testRemoveCaseInsensitive() {
        set.add("World");
        assertTrue(set.remove("world"));
        assertFalse(set.contains("WORLD"));
        assertTrue(set.isEmpty());

        // removing non‑String works as usual
        CaseInsensitiveSet<Object> mixed = new CaseInsensitiveSet<>();
        mixed.add(456);
        assertTrue(mixed.remove(456));
        assertFalse(mixed.remove(456));
    }

    /*** iterator preserves original case ***/
    @Test
    public void testIteratorPreservesCase() {
        set.add("First");
        set.add("second");
        set.add("THIRD");
        List<String> iterated = new ArrayList<>();
        for (String s : set) {
            iterated.add(s);
        }
        // order is insertion order (LinkedHashMap backing)
        assertEquals(Arrays.asList("First", "second", "THIRD"), iterated);
    }

    /*** toArray methods ***/
    @Test
    public void testToArray() {
        set.add("One");
        set.add("Two");
        Object[] arr = set.toArray();
        assertArrayEquals(new Object[]{"One", "Two"}, arr);

        String[] a = new String[0];
        String[] result = set.toArray(a);
        assertArrayEquals(new String[]{"One", "Two"}, result);
    }

    /*** equals and hashCode contract ***/
    @Test
    public void testEqualsAndHashCode() {
        CaseInsensitiveSet<String> s1 = new CaseInsensitiveSet<>();
        CaseInsensitiveSet<String> s2 = new CaseInsensitiveSet<>();
        s1.add("Alpha");
        s2.add("ALPHA");
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        // different element
        s2.add("Beta");
        assertNotEquals(s1, s2);
    }

    /*** containsAll, addAll, retainAll, removeAll ***/
    @Test
    public void testBulkOperations() {
        List<String> list = Arrays.asList("A", "b", "C");
        assertTrue(set.addAll(list));
        assertEquals(3, set.size());

        // containsAll should be case‑insensitive
        assertTrue(set.containsAll(Arrays.asList("a", "B")));

        // retainAll keeps only specified (case‑insensitive)
        assertTrue(set.retainAll(Arrays.asList("a", "x")));
        assertEquals(1, set.size());
        assertTrue(set.contains("A"));

        // removeAll removes matching elements irrespective of case
        set.addAll(Arrays.asList("d", "E"));
        assertTrue(set.removeAll(Arrays.asList("D", "e")));
        assertFalse(set.contains("d"));
        assertFalse(set.contains("E"));
        assertEquals(1, set.size()); // only "A" remains
    }

    /*** clear ***/
    @Test
    public void testClear() {
        set.addAll(Arrays.asList("one", "two"));
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
    }

    /*** constructors with collection and backing map ***/
    @Test
    public void testConstructorFromCollection() {
        List<String> src = Arrays.asList("x", "Y", "x");
        CaseInsensitiveSet<String> fromList = new CaseInsensitiveSet<>(src);
        assertEquals(2, fromList.size());
        assertTrue(fromList.containsAll(Arrays.asList("X", "y")));
    }

    @Test
    public void testConstructorWithBackingMap() {
        Map<String, Object> backing = new TreeMap<>();
        CaseInsensitiveSet<String> custom = new CaseInsensitiveSet<>(Arrays.asList("foo", "BAR"), backing);
        // TreeMap orders keys naturally (case‑sensitive), but set still treats case‑insensitively
        assertEquals(2, custom.size());
        assertTrue(custom.contains("FOO"));
        assertTrue(custom.contains("bar"));
        // iteration order follows TreeMap's natural order (Foo < bar)
        Iterator<String> it = custom.iterator();
        assertEquals("bar", it.next()); // "bar" (lowercase) sorts before "foo" in natural order
        assertEquals("foo", it.next());
    }

    /*** deprecated plus/minus methods behave like addAll/removeAll ***/
    @Test
    public void testDeprecatedPlusMinus() {
        set.plus(Arrays.asList("a", "B"));
        assertEquals(2, set.size());
        set.minus(Arrays.asList("A"));
        assertFalse(set.contains("a"));
        assertTrue(set.contains("B"));
    }

    /*** toString representation preserves original case and order ***/
    @Test
    public void testToString() {
        set.add("One");
        set.add("two");
        set.add("THREE");
        assertEquals("[One, two, THREE]", set.toString());
    }

    /*** isEmpty on a newly created set ***/
    @Test
    public void testIsEmpty() {
        assertTrue(set.isEmpty());
        set.add("x");
        assertFalse(set.isEmpty());
    }

    /*** handling of null elements (allowed by underlying map) ***/
    @Test
    public void testNullElement() {
        // The underlying CaseInsensitiveMap permits null keys; verify set respects that
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
    }
}
