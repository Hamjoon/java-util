package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactSet}.
 *
 * <p>
 * The tests are organized by the public methods of {@code CompactSet}.
 * For each method we provide:
 * <ul>
 *   <li>Typical usage (happy path)</li>
 *   <li>Edge cases (empty collections, null values, duplicate elements, etc.)</li>
 *   <li>Error scenarios (illegal arguments, mis‑configuration, etc.)</li>
 * </ul>
 * The suite also validates the builder, configuration handling and the
 * {@code isDefaultCompactSet()} contract.
 * </p>
 *
 * <p>All objects are created through the public constructors / factory
 * methods supplied in the source – no mocks or subclassing of library
 * types is performed.</p>
 */
public class CompactSetTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private CompactSet<String> defaultSet;
    private CompactSet<Integer> intSet;

    @Before
    public void setUp() {
        defaultSet = new CompactSet<>();
        intSet = new CompactSet<>();
    }

    /* --------------------------------------------------------------------- */
    /*  Constructor tests                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());
    }

    @Test
    public void testConstructorFromCollectionCopiesElements() {
        List<String> source = Arrays.asList("a", "b", "c", "a");
        CompactSet<String> set = new CompactSet<>(source);

        // duplicates are ignored
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
    }

    @Test
    public void testConstructorFromNullCollectionThrowsNPE() {
        thrown.expect(NullPointerException.class);
        new CompactSet<String>(null);
    }

    @Test
    public void testProtectedConstructorRejectsInvalidCompactSize() {
        // Build a CompactMap with compactSize = 1 (illegal, must be >=2)
        CompactMap<String, Object> badMap = CompactSet.createSimpleMap(true, 1, CompactMap.UNORDERED);
        thrown.expect(IllegalStateException.class);
        new CompactSet<>(badMap);
    }

    /* --------------------------------------------------------------------- */
    /*  Builder and configuration handling                                   */
    /* --------------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesCaseInsensitiveSortedSet() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(80)
                .build();

        assertTrue(set.add("Apple"));
        // case‑insensitive duplicate should not be added
        assertFalse(set.add("apple"));
        assertEquals(1, set.size());

        // ordering test – iterator should return in natural order
        set.clear();
        set.addAll(Arrays.asList("banana", "Apple", "cherry"));
        Iterator<String> it = set.iterator();
        assertEquals("Apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
    }

    @Test
    public void testBuilderInsertionOrderPreservesInsertionSequence() {
        CompactSet<String> set = CompactSet.<String>builder()
                .insertionOrder()
                .build();

        set.addAll(Arrays.asList("first", "second", "third"));
        Iterator<String> it = set.iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
        assertEquals("third", it.next());
    }

    @Test
    public void testGetConfigReflectsUnderlyingMapSettings() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)
                .compactSize(70)
                .reverseOrder()
                .build();

        Map<String, Object> cfg = set.getConfig();

        assertEquals(70, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(false, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.REVERSE, cfg.get(CompactMap.ORDERING));
    }

    @Test
    public void testWithConfigOverridesAndRetainsUnspecifiedValues() {
        CompactSet<String> original = CompactSet.<String>builder()
                .caseSensitive(true)
                .compactSize(50)
                .insertionOrder()
                .build();

        original.addAll(Arrays.asList("a", "b", "c"));

        Map<String, Object> newCfg = new HashMap<>();
        newCfg.put(CompactMap.COMPACT_SIZE, 100);          // override
        // leave caseSensitive & ordering unspecified

        CompactSet<String> modified = original.withConfig(newCfg);

        // Verify overridden compactSize
        assertEquals(100, modified.getConfig().get(CompactMap.COMPACT_SIZE));

        // Verify the other settings stayed the same
        assertEquals(true, modified.getConfig().get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.INSERTION, modified.getConfig().get(CompactMap.ORDERING));

        // All original elements must be present
        assertTrue(modified.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testWithConfigNullArgumentThrows() {
        thrown.expect(IllegalArgumentException.class);
        defaultSet.withConfig(null);
    }

    /* --------------------------------------------------------------------- */
    /*  Basic Set operations                                                 */
    /* --------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsBehaviour() {
        assertTrue(defaultSet.add("x"));
        assertTrue(defaultSet.contains("x"));
        // duplicate add returns false
        assertFalse(defaultSet.add("x"));
        assertEquals(1, defaultSet.size());
    }

    @Test
    public void testAddNullElementSupported() {
        // CompactMap permits null keys, so the set should as well
        assertTrue(defaultSet.add(null));
        assertTrue(defaultSet.contains(null));
        assertFalse(defaultSet.add(null)); // duplicate null
        assertEquals(1, defaultSet.size());
    }

    @Test
    public void testRemoveBehaviour() {
        defaultSet.addAll(Arrays.asList("one", "two"));
        assertTrue(defaultSet.remove("one"));
        assertFalse(defaultSet.contains("one"));
        assertEquals(1, defaultSet.size());

        // removing non‑existent element returns false
        assertFalse(defaultSet.remove("three"));
    }

    @Test
    public void testRemoveNullElement() {
        defaultSet.add(null);
        assertTrue(defaultSet.remove(null));
        assertFalse(defaultSet.contains(null));
    }

    @Test
    public void testClearEmptiesSet() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());
    }

    @Test
    public void testContainsAllWithEmptyCollection() {
        assertTrue(defaultSet.containsAll(Collections.emptyList()));
    }

    @Test
    public void testContainsAllWithMissingElement() {
        defaultSet.addAll(Arrays.asList("x", "y"));
        assertFalse(defaultSet.containsAll(Arrays.asList("x", "z")));
    }

    @Test
    public void testAddAllReturnsTrueIfSetChanged() {
        List<String> list = Arrays.asList("a", "b", "c");
        assertTrue(defaultSet.addAll(list));
        // adding the same elements again should return false
        assertFalse(defaultSet.addAll(list));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        defaultSet.addAll(Arrays.asList("keep", "discard", "alsoKeep"));
        assertTrue(defaultSet.retainAll(Arrays.asList("keep", "alsoKeep")));
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.containsAll(Arrays.asList("keep", "alsoKeep")));
    }

    @Test
    public void testRetainAllWithNoMatchClearsSet() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        assertTrue(defaultSet.retainAll(Collections.emptyList()));
        assertTrue(defaultSet.isEmpty());
    }

    @Test
    public void testRemoveAllRemovesSpecifiedElements() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(defaultSet.removeAll(Arrays.asList("b", "c", "nonexistent")));
        assertEquals(1, defaultSet.size());
        assertTrue(defaultSet.contains("a"));
    }

    @Test
    public void testIteratorReflectsCurrentState() {
        defaultSet.addAll(Arrays.asList("x", "y", "z"));
        Iterator<String> it = defaultSet.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            seen.add(it.next());
        }
        assertEquals(new HashSet<>(Arrays.asList("x", "y", "z")), seen);
    }

    @Test
    public void testToArrayProducesCorrectContents() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        Object[] arr = defaultSet.toArray();
        assertEquals(2, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testToArrayWithTypedArray() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        String[] arr = defaultSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"a", "b"}, arr);
    }

    @Test
    public void testToArrayWithLargerTypedArrayKeepsExtraSlotNull() {
        defaultSet.addAll(Arrays.asList("a"));
        String[] dest = new String[2];
        dest[1] = "preserve";
        String[] result = defaultSet.toArray(dest);
        assertEquals(2, result.length);
        assertEquals("a", result[0]);
        assertNull(result[1]); // per Collection.toArray(T[]) contract
    }

    /* --------------------------------------------------------------------- */
    /*  Object contract (equals, hashCode, toString)                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeAgainstAnotherSet() {
        CompactSet<String> s1 = new CompactSet<>();
        CompactSet<String> s2 = new CompactSet<>();

        s1.addAll(Arrays.asList("a", "b"));
        s2.addAll(Arrays.asList("b", "a"));

        assertTrue(s1.equals(s2));
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void testEqualsWithDifferentImplementation() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("a", "b"));
        defaultSet.addAll(Arrays.asList("a", "b"));
        assertTrue(defaultSet.equals(hashSet));
        assertTrue(hashSet.equals(defaultSet));
    }

    @Test
    public void testEqualsWithNullAndOtherTypes() {
        defaultSet.add("x");
        assertFalse(defaultSet.equals(null));
        assertFalse(defaultSet.equals("not a set"));
    }

    @Test
    public void testToStringContainsAllElements() {
        defaultSet.addAll(Arrays.asList("one", "two"));
        String s = defaultSet.toString();
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        assertTrue(s.contains("one"));
        assertTrue(s.contains("two"));
    }

    /* --------------------------------------------------------------------- */
    /*  isDefaultCompactSet() verification                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testIsDefaultCompactSetReturnsTrueForLibraryDefault() {
        // The default constructor should match the library defaults:
        // compactSize = 50, caseSensitive = true, ordering = "unordered",
        // single key = CompactMap.DEFAULT_SINGLE_KEY, backing map = HashMap.
        CompactSet<String> set = new CompactSet<>();
        assertTrue(set.isDefaultCompactSet());
    }

    @Test
    public void testIsDefaultCompactSetReturnsFalseWhenConfigurationDiffers() {
        CompactSet<String> set = CompactSet.<String>builder()
                .caseSensitive(false)   // differs
                .compactSize(60)        // differs
                .sortedOrder()          // differs
                .build();

        assertFalse(set.isDefaultCompactSet());
    }

    /* --------------------------------------------------------------------- */
    /*  Edge cases with large number of elements (triggering backing map)    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testLargeNumberOfElementsBeyondCompactSize() {
        // Use a compactSize of 5 to force the internal map to switch to a
        // regular HashMap after a few inserts.
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .compactSize(5)
                .build();

        for (int i = 0; i < 20; i++) {
            assertTrue(set.add(i));
        }
        assertEquals(20, set.size());

        // Verify that all inserted values are present
        for (int i = 0; i < 20; i++) {
            assertTrue(set.contains(i));
        }
    }

    /* --------------------------------------------------------------------- */
    /*  Miscellaneous internal helpers (createSimpleMap)                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void testCreateSimpleMapProducesExpectedBackingMapTypes() {
        // unordered, case‑sensitive -> HashMap
        CompactMap<String, Object> map1 = CompactSet.createSimpleMap(true, 10, CompactMap.UNORDERED);
        assertTrue(map1.getNewMap() instanceof HashMap);

        // insertion order, case‑insensitive -> CaseInsensitiveMap wrapping LinkedHashMap
        CompactMap<String, Object> map2 = CompactSet.createSimpleMap(false, 10, CompactMap.INSERTION);
        assertTrue(map2.getNewMap() instanceof CaseInsensitiveMap);

        // sorted order, case‑sensitive -> TreeMap
        CompactMap<String, Object> map3 = CompactSet.createSimpleMap(true, 10, CompactMap.SORTED);
        assertTrue(map3.getNewMap() instanceof TreeMap);
    }
}
