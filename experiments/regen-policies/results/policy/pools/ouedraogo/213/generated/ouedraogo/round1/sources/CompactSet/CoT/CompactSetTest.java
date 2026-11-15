package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Test suite for {@link CompactSet}.
 *
 * The test class is generated in several steps:
 * 1. List of all public methods (including inherited from Set) with signatures.
 * 2. Basic functionality tests for each method.
 * 3. Edge‑case / exception tests identified from the source code.
 * 4. All tests merged into a single JUnit‑4 test file.
 */
public class CompactSetTest {

    /* -------------------------------------------------------------
     * 1. Public methods of CompactSet (including those inherited from Set)
     * -------------------------------------------------------------
     *
     * CompactSet()
     * CompactSet(Collection<? extends E> c)
     * protected CompactSet(CompactMap<E,Object> map)
     *
     * boolean isDefaultCompactSet()
     * int size()
     * boolean isEmpty()
     * boolean contains(Object o)
     * boolean add(E e)
     * boolean remove(Object o)
     * void clear()
     * boolean containsAll(Collection<?> c)
     * boolean addAll(Collection<? extends E> c)
     * boolean retainAll(Collection<?> c)
     * boolean removeAll(Collection<?> c)
     * Iterator<E> iterator()
     * Object[] toArray()
     * <T> T[] toArray(T[] a)
     * boolean equals(Object o)
     * int hashCode()
     * String toString()
     * static <E> Builder<E> builder()
     * Map<String,Object> getConfig()
     * CompactSet<E> withConfig(Map<String,Object> config)
     *
     * Builder<E> methods (used in tests):
     *   Builder<E> caseSensitive(boolean)
     *   Builder<E> compactSize(int)
     *   Builder<E> sortedOrder()
     *   Builder<E> reverseOrder()
     *   Builder<E> insertionOrder()
     *   Builder<E> noOrder()
     *   CompactSet<E> build()
     */

    private CompactSet<String> defaultSet;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() {
        // default constructor uses the library defaults (compactSize = 50, case‑sensitive, unordered)
        defaultSet = new CompactSet<>();
    }

    /* -------------------------------------------------------------
     * 2. Basic functionality tests
     * ------------------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(defaultSet.add("a"));
        assertTrue(defaultSet.contains("a"));
        // adding duplicate must return false
        assertFalse(defaultSet.add("a"));
    }

    @Test
    public void testRemove() {
        defaultSet.add("x");
        assertTrue(defaultSet.remove("x"));
        assertFalse(defaultSet.contains("x"));
        // removing non‑existent element returns false
        assertFalse(defaultSet.remove("nonexistent"));
    }

    @Test
    public void testSizeAndIsEmpty() {
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());

        defaultSet.add("one");
        defaultSet.add("two");
        assertFalse(defaultSet.isEmpty());
        assertEquals(2, defaultSet.size());
    }

    @Test
    public void testClear() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertEquals(3, defaultSet.size());
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty());
    }

    @Test
    public void testAddAllAndContainsAll() {
        List<String> list = Arrays.asList("alpha", "beta", "gamma");
        assertTrue(defaultSet.addAll(list));
        assertEquals(3, defaultSet.size());

        assertTrue(defaultSet.containsAll(list));

        // adding a collection that contains already‑present elements should return false
        assertFalse(defaultSet.addAll(Arrays.asList("alpha", "beta")));
    }

    @Test
    public void testRemoveAll() {
        defaultSet.addAll(Arrays.asList("a", "b", "c", "d"));
        assertTrue(defaultSet.removeAll(Arrays.asList("b", "d", "x"))); // x not present
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains("b"));
        assertFalse(defaultSet.contains("d"));
    }

    @Test
    public void testRetainAll() {
        defaultSet.addAll(Arrays.asList("1", "2", "3"));
        assertTrue(defaultSet.retainAll(Arrays.asList("2", "3", "4")));
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.contains("2"));
        assertTrue(defaultSet.contains("3"));
        assertFalse(defaultSet.contains("1"));
    }

    @Test
    public void testIterator() {
        defaultSet.addAll(Arrays.asList("x", "y", "z"));
        Set<String> seen = new HashSet<>();
        for (String s : defaultSet) {
            seen.add(s);
        }
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z")), seen);
    }

    @Test
    public void testToArray() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        Object[] arr = defaultSet.toArray();
        assertArrayEquals(new Object[]{"a", "b"}, arr);
    }

    @Test
    public void testToArrayTyped() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        String[] arr = defaultSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b"}, arr);
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactSet<String> other = new CompactSet<>();
        defaultSet.addAll(Arrays.asList("one", "two"));
        other.addAll(Arrays.asList("one", "two"));

        assertTrue(defaultSet.equals(other));
        assertEquals(defaultSet.hashCode(), other.hashCode());

        other.add("three");
        assertFalse(defaultSet.equals(other));
    }

    @Test
    public void testToString() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        String str = defaultSet.toString();
        // Order is not guaranteed for unordered sets, so just check content
        assertTrue(str.contains("a"));
        assertTrue(str.contains("b"));
        assertTrue(str.startsWith("[") && str.endsWith("]"));
    }

    @Test
    public void testBuilderCustomConfig() {
        CompactSet<String> ordered = CompactSet.<String>builder()
                .insertionOrder()
                .caseSensitive(false)
                .compactSize(10)
                .build();

        ordered.addAll(Arrays.asList("A", "a", "B"));
        // case‑insensitive: "A" and "a" are considered the same, so only one should be stored
        assertEquals(2, ordered.size());
        assertTrue(ordered.contains("a"));
        assertTrue(ordered.contains("B"));
    }

    @Test
    public void testGetConfig() {
        Map<String, Object> cfg = defaultSet.getConfig();
        assertNotNull(cfg);
        assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.UNORDERED, cfg.get(CompactMap.ORDERING));
    }

    @Test
    public void testWithConfigOverrides() {
        // start with a case‑sensitive unordered set
        CompactSet<String> base = CompactSet.<String>builder()
                .caseSensitive(true)
                .noOrder()
                .compactSize(5)
                .build();

        base.addAll(Arrays.asList("a", "b", "c"));

        // new config: make it case‑insensitive and sorted
        Map<String, Object> newCfg = new HashMap<>();
        newCfg.put(CompactMap.CASE_SENSITIVE, false);
        newCfg.put(CompactMap.ORDERING, CompactMap.SORTED);
        // omit compactSize to keep original (5)

        CompactSet<String> transformed = base.withConfig(newCfg);
        // Verify ordering is sorted (iteration should be natural order)
        List<String> iterated = new ArrayList<>();
        for (String s : transformed) {
            iterated.add(s);
        }
        assertEquals(Arrays.asList("a", "b", "c"), iterated); // already sorted

        // Verify case‑insensitivity: adding "A" should not increase size
        assertFalse(transformed.add("A"));
        assertEquals(3, transformed.size());
    }

    /* -------------------------------------------------------------
     * 3. Edge‑case / exception tests
     * ------------------------------------------------------------- */

    @Test
    public void testConstructorWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new CompactSet<String>((Collection<String>) null);
    }

    @Test
    public void testBuilderCompactSizeBelowTwoThrowsIllegalState() {
        // The builder itself does not throw, but the resulting CompactSet will.
        thrown.expect(IllegalStateException.class);
        CompactSet<String> set = CompactSet.<String>builder()
                .compactSize(1)            // illegal, must be >=2
                .build();                  // should trigger IllegalStateException in CompactSet ctor
    }

    @Test
    public void testWithConfigNullArgumentThrowsNPE() {
        thrown.expect(NullPointerException.class);
        defaultSet.withConfig(null);
    }

    @Test
    public void testAddNullElement() {
        // CompactSet (via CompactMap) permits null keys – verify behaviour
        assertTrue(defaultSet.add(null));
        assertTrue(defaultSet.contains(null));
        assertEquals(1, defaultSet.size());

        // Adding null again must return false
        assertFalse(defaultSet.add(null));
    }

    @Test
    public void testRemoveNullElement() {
        defaultSet.add(null);
        assertTrue(defaultSet.remove(null));
        assertFalse(defaultSet.contains(null));
    }

    @Test
    public void testContainsAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        defaultSet.containsAll(null);
    }

    @Test
    public void testAddAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        defaultSet.addAll(null);
    }

    @Test
    public void testRetainAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        defaultSet.retainAll(null);
    }

    @Test
    public void testRemoveAllWithNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        defaultSet.removeAll(null);
    }

    @Test
    public void testIsDefaultCompactSetOnDefaultInstance() {
        // The default constructor should produce the library “default” configuration
        assertTrue(defaultSet.isDefaultCompactSet());
    }

    @Test
    public void testIsDefaultCompactSetFalseWhenCustomized() {
        CompactSet<String> custom = CompactSet.<String>builder()
                .compactSize(20)
                .caseSensitive(false)
                .sortedOrder()
                .build();
        assertFalse(custom.isDefaultCompactSet());
    }
}
