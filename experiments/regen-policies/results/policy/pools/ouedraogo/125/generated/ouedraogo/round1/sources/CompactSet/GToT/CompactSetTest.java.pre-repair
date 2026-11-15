package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public methods (including those inherited from {@link Set})</li>
 *   <li>Typical usage scenarios</li>
 *   <li>Edge‑cases such as duplicate adds, null handling, empty collections</li>
 *   <li>Exception handling (illegal compact size, null arguments)</li>
 *   <li>Builder‑configured ordering and case‑sensitivity</li>
 * </ul>
 */
public class CompactSetTest {

    private CompactSet<String> defaultSet;

    @Before
    public void setUp() {
        defaultSet = new CompactSet<>();
    }

    /* ----------------------------------------------------------------- */
    /*  Basic Set operations (size, isEmpty, add, remove, clear)        */
    /* ----------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorAndBasicOperations() {
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());

        // add
        assertTrue(defaultSet.add("one"));
        assertTrue(defaultSet.contains("one"));
        assertEquals(1, defaultSet.size());

        // duplicate add returns false, size unchanged
        assertFalse(defaultSet.add("one"));
        assertEquals(1, defaultSet.size());

        // remove existing element
        assertTrue(defaultSet.remove("one"));
        assertFalse(defaultSet.contains("one"));
        assertEquals(0, defaultSet.size());

        // remove non‑existing element
        assertFalse(defaultSet.remove("missing"));

        // clear on empty set (no exception)
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty());
    }

    @Test
    public void testAddAllAndContainsAll() {
        List<String> list = Arrays.asList("a", "b", "c", "a"); // duplicate "a"
        assertTrue(defaultSet.addAll(list));
        assertEquals(3, defaultSet.size());
        assertTrue(defaultSet.containsAll(Arrays.asList("a", "b", "c")));

        // addAll with an already‑present collection returns false
        assertFalse(defaultSet.addAll(list));
    }

    @Test
    public void testRetainAll() {
        defaultSet.addAll(Arrays.asList("x", "y", "z"));
        assertTrue(defaultSet.retainAll(Arrays.asList("y", "z", "w"))); // keep y & z
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.containsAll(Arrays.asList("y", "z")));
        assertFalse(defaultSet.contains("x"));
    }

    @Test
    public void testRemoveAll() {
        defaultSet.addAll(Arrays.asList("p", "q", "r"));
        assertTrue(defaultSet.removeAll(Arrays.asList("q", "nonexistent")));
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains("q"));
        assertTrue(defaultSet.containsAll(Arrays.asList("p", "r")));
    }

    @Test
    public void testIteratorOrderInsertion() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .insertionOrder()
                .build();
        set.addAll(Arrays.asList(5, 3, 9, 1));

        Iterator<Integer> it = set.iterator();
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(Arrays.asList(5, 3, 9, 1), iterated);
    }

    @Test
    public void testIteratorOrderSorted() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .sortedOrder()
                .build();
        set.addAll(Arrays.asList(8, 2, 5, 1));

        Iterator<Integer> it = set.iterator();
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(Arrays.asList(1, 2, 5, 8), iterated);
    }

    @Test
    public void testIteratorOrderReverse() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .reverseOrder()
                .build();
        set.addAll(Arrays.asList(3, 7, 4, 9));

        Iterator<Integer> it = set.iterator();
        List<Integer> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(Arrays.asList(9, 7, 4, 3), iterated);
    }

    @Test
    public void testCaseInsensitiveBuilder() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false) // case‑insensitive
                .build();

        assertTrue(set.add("Apple"));
        // Different case should be considered equal
        assertFalse(set.add("apple"));
        assertTrue(set.contains("APPLE"));
        assertEquals(1, set.size());
    }

    /* ----------------------------------------------------------------- */
    /*  toArray variants                                                    */
    /* ----------------------------------------------------------------- */

    @Test
    public void testToArray() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        Object[] arr = defaultSet.toArray();
        assertArrayEquals(new Object[]{"a", "b", "c"}, arr);
    }

    @Test
    public void testToArrayWithProvidedArray() {
        defaultSet.addAll(Arrays.asList("x", "y"));
        String[] a = new String[5];
        a[2] = "filled";
        String[] result = defaultSet.toArray(a);

        // Result should be the same array (per java.util.Collection contract)
        assertSame(a, result);
        // First two positions contain the set elements (order not guaranteed for default)
        List<String> firstTwo = Arrays.asList(result[0], result[1]);
        assertTrue(firstTwo.containsAll(Arrays.asList("x", "y")));
        // The element after the last should be null per spec
        assertNull(result[2]);
    }

    /* ----------------------------------------------------------------- */
    /*  equals, hashCode and toString                                      */
    /* ----------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactSet<String> s1 = new CompactSet<>();
        CompactSet<String> s2 = new CompactSet<>();

        s1.addAll(Arrays.asList("a", "b"));
        s2.addAll(Arrays.asList("b", "a")); // different insertion order

        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        // Equality with a standard HashSet should also succeed
        Set<String> hashSet = new HashSet<>(Arrays.asList("a", "b"));
        assertEquals(s1, hashSet);
    }

    @Test
    public void testToStringRepresentation() {
        defaultSet.addAll(Arrays.asList("one", "two"));
        String str = defaultSet.toString();
        // The string must start with '[' and end with ']'
        assertTrue(str.startsWith("["));
        assertTrue(str.endsWith("]"));
        // It must contain both elements (order not important for default)
        assertTrue(str.contains("one"));
        assertTrue(str.contains("two"));
    }

    /* ----------------------------------------------------------------- */
    /*  Builder edge cases                                                  */
    /* ----------------------------------------------------------------- */

    @Test(expected = IllegalStateException.class)
    public void testBuilderInvalidCompactSize() {
        // compactSize < 2 should trigger IllegalStateException in CompactSet ctor
        CompactSet.<String>builder()
                .compactSize(1)
                .build();
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorNullCollection() {
        new CompactSet<String>((Collection<String>) null);
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllNullCollection() {
        defaultSet.addAll(null);
    }

    /* ----------------------------------------------------------------- */
    /*  Null element handling (depends on CompactMap implementation)      */
    /* ----------------------------------------------------------------- */

    @Test
    public void testAddNullElement() {
        // CompactMap permits null keys (the original library does); verify behaviour
        assertTrue(defaultSet.add(null));
        assertTrue(defaultSet.contains(null));
        assertEquals(1, defaultSet.size());

        // Adding null again should return false (duplicate)
        assertFalse(defaultSet.add(null));
        assertEquals(1, defaultSet.size());

        // Remove null
        assertTrue(defaultSet.remove(null));
        assertFalse(defaultSet.contains(null));
        assertTrue(defaultSet.isEmpty());
    }
}
