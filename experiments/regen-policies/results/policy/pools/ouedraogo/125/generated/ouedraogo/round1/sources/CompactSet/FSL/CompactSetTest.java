package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Test;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All {@link Set} operations (add, remove, contains, iterator, etc.)</li>
 *   <li>Bulk operations (addAll, retainAll, removeAll, containsAll)</li>
 *   <li>Builder‑driven configurations:
 *       <ul>
 *         <li>Insertion order</li>
 *         <li>Sorted order</li>
 *         <li>Reverse order</li>
 *         <li>Case‑insensitivity</li>
 *         <li>Invalid compactSize handling</li>
 *       </ul>
 *   </li>
 *   <li>Object contract methods (equals, hashCode, toString)</li>
 *   <li>Array conversion methods (toArray())</li>
 * </ul>
 */
public class CompactSetTest {

    /* ------------------------------------------------------------ */
    /*  Basic Set behaviour using the default constructor          */
    /* ------------------------------------------------------------ */
    @Test
    public void testBasicAddRemoveContains() {
        CompactSet<String> set = new CompactSet<>();

        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // add a new element – should return true
        assertTrue(set.add("alpha"));
        assertFalse(set.isEmpty());
        assertEquals(1, set.size());
        assertTrue(set.contains("alpha"));

        // add duplicate – should return false and size unchanged
        assertFalse(set.add("alpha"));
        assertEquals(1, set.size());

        // add another element
        assertTrue(set.add("beta"));
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList("alpha", "beta")));

        // remove existing element – returns true
        assertTrue(set.remove("alpha"));
        assertEquals(1, set.size());
        assertFalse(set.contains("alpha"));

        // remove non‑existing element – returns false
        assertFalse(set.remove("gamma"));
        assertEquals(1, set.size());

        // clear the set
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testAddAllAndContainsAll() {
        CompactSet<Integer> set = new CompactSet<>();

        List<Integer> source = Arrays.asList(1, 2, 3, 2, 1);
        assertTrue(set.addAll(source));               // at least one new element
        assertEquals(3, set.size());                  // duplicates ignored
        assertTrue(set.containsAll(Arrays.asList(1, 2, 3)));

        // adding the same collection again should return false (no modification)
        assertFalse(set.addAll(source));
    }

    @Test
    public void testRetainAll() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("a", "b", "c", "d"));

        // retain only a subset
        boolean changed = set.retainAll(Arrays.asList("b", "c", "x"));
        assertTrue(changed);
        assertEquals(2, set.size());
        assertTrue(set.containsAll(Arrays.asList("b", "c")));
        assertFalse(set.contains("a"));
        assertFalse(set.contains("d"));
    }

    @Test
    public void testRemoveAll() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("red", "green", "blue"));

        // remove a subset
        boolean changed = set.removeAll(Arrays.asList("green", "yellow"));
        assertTrue(changed);
        assertEquals(2, set.size());
        assertFalse(set.contains("green"));
        assertTrue(set.containsAll(Arrays.asList("red", "blue")));
    }

    @Test
    public void testIteratorOrderInsertion() {
        CompactSet<String> set = CompactSet.<String>builder()
                .insertionOrder()
                .build();

        set.addAll(Arrays.asList("first", "second", "third"));
        Iterator<String> it = set.iterator();

        assertTrue(it.hasNext());
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorOrderSorted() {
        CompactSet<String> set = CompactSet.<String>builder()
                .sortedOrder()
                .build();

        set.addAll(Arrays.asList("delta", "alpha", "charlie", "bravo"));
        List<String> iterated = new ArrayList<>();
        for (String s : set) {
            iterated.add(s);
        }
        // natural order should be alphabetical
        assertEquals(Arrays.asList("alpha", "bravo", "charlie", "delta"), iterated);
    }

    @Test
    public void testIteratorOrderReverse() {
        CompactSet<String> set = CompactSet.<String>builder()
                .reverseOrder()
                .build();

        set.addAll(Arrays.asList("one", "two", "three"));
        List<String> iterated = new ArrayList<>();
        for (String s : set) {
            iterated.add(s);
        }
        // reverse alphabetical order
        assertEquals(Arrays.asList("two", "three", "one"), iterated);
    }

    /* ------------------------------------------------------------ */
    /*  Case‑sensitivity variations                                 */
    /* ------------------------------------------------------------ */
    @Test
    public void testCaseInsensitiveBehavior() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)   // case‑insensitive
                .build();

        assertTrue(set.add("Hello"));
        // duplicate with different case – should not be added
        assertFalse(set.add("hello"));
        assertEquals(1, set.size());
        assertTrue(set.contains("HELLO"));
        assertTrue(set.contains("hello"));
    }

    @Test
    public void testCaseSensitiveDefaultBehavior() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(true)    // explicit case‑sensitive
                .build();

        assertTrue(set.add("World"));
        assertTrue(set.add("world"));
        assertEquals(2, set.size());
        assertTrue(set.contains("World"));
        assertTrue(set.contains("world"));
        assertFalse(set.contains("WORLD"));
    }

    /* ------------------------------------------------------------ */
    /*  toArray() variations                                         */
    /* ------------------------------------------------------------ */
    @Test
    public void testToArrayObject() {
        CompactSet<Integer> set = new CompactSet<>();
        set.addAll(Arrays.asList(10, 20, 30));

        Object[] arr = set.toArray();
        assertEquals(3, arr.length);
        assertTrue(Arrays.asList(10, 20, 30).containsAll(Arrays.asList(arr)));
    }

    @Test
    public void testToArrayGeneric() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("x", "y"));

        // array exactly the right size
        String[] exact = set.toArray(new String[2]);
        assertArrayEquals(new String[]{"x", "y"}, exact);

        // array larger than needed – extra element should be null
        String[] larger = set.toArray(new String[5]);
        assertEquals(5, larger.length);
        assertNull(larger[2]); // first unused slot must be null per Collection contract
    }

    /* ------------------------------------------------------------ */
    /*  Equality, hashCode and toString                              */
    /* ------------------------------------------------------------ */
    @Test
    public void testEqualsAndHashCode() {
        CompactSet<Integer> a = new CompactSet<>();
        CompactSet<Integer> b = CompactSet.<Integer>builder().noOrder().build();

        a.addAll(Arrays.asList(1, 2, 3));
        b.addAll(Arrays.asList(3, 2, 1));

        assertEquals(a, b);
        assertEquals(b, a);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    public void testToStringRepresentation() {
        CompactSet<String> set = new CompactSet<>();
        set.addAll(Arrays.asList("apple", "banana"));
        String s = set.toString();
        // Order is not guaranteed for the default (unordered) set,
        // but the string must start with '[' and end with ']'
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        // Both elements must appear somewhere inside
        assertTrue(s.contains("apple"));
        assertTrue(s.contains("banana"));
    }

    /* ------------------------------------------------------------ */
    /*  Constructor that accepts a Collection                       */
    /* ------------------------------------------------------------ */
    @Test
    public void testConstructorFromCollection() {
        List<String> source = Arrays.asList("a", "b", "c", "a");
        CompactSet<String> set = new CompactSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(Arrays.asList("a", "b", "c")));
    }

    /* ------------------------------------------------------------ */
    /*  Invalid compactSize – should throw IllegalStateException   */
    /* ------------------------------------------------------------ */
    @Test(expected = IllegalStateException.class)
    public void testInvalidCompactSizeThrows() {
        // compactSize < 2 is illegal – builder should propagate the exception
        CompactSet.<String>builder()
                .compactSize(1)   // too small
                .build();          // should throw IllegalStateException
    }

    /* ------------------------------------------------------------ */
    /*  Null handling – CompactMap permits null keys (assumed)     */
    /* ------------------------------------------------------------ */
    @Test
    public void testNullElementSupport() {
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());

        // adding null again should return false (no change)
        assertFalse(set.add(null));
        assertEquals(1, set.size());

        // removal of null
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }
}
