package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactSet}.
 *
 * The tests are organised by the public API of {@code CompactSet}:
 * <ul>
 *   <li>Construction / configuration</li>
 *   <li>Set operations (add, remove, contains, bulk operations, iterator, etc.)</li>
 *   <li>Object overrides (equals, hashCode, toString)</li>
 *   <li>Builder and {@code withConfig} behaviour</li>
 *   <li>Edge cases and exception handling</li>
 * </ul>
 *
 * The test class deliberately creates real {@code CompactSet} instances
 * (via constructors or the builder) and never uses mocks or subclasses of
 * {@code CompactMap} – only the public API that is part of the supplied
 * source code is exercised.
 */
public class CompactSetTest {

    private CompactSet<String> defaultSet;

    @Before
    public void setUp() {
        defaultSet = new CompactSet<>();
    }

    /* -------------------------------------------------------------
     *  Construction / basic configuration
     * ------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new CompactSet<String>((Collection<String>) null);
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> list = Arrays.asList("a", "b", "c");
        CompactSet<String> set = new CompactSet<>(list);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(list));
    }

    @Test
    public void testIsDefaultCompactSetReturnsTrueForPlainDefault() {
        assertTrue(defaultSet.isDefaultCompactSet());
    }

    @Test
    public void testIsDefaultCompactSetReturnsFalseWhenCustomized() {
        CompactSet<String> custom = CompactSet.<String>builder()
                .caseSensitive(false)
                .sortedOrder()
                .compactSize(80)
                .build();
        assertFalse(custom.isDefaultCompactSet());
    }

    @Test(expected = IllegalStateException.class)
    public void testIllegalCompactSizeInSubclassThrows() {
        // Subclass overrides compactSize() to return 1 (illegal, must be >=2)
        new CompactSet<String>() {
            @Override
            protected int compactSize() {
                return 1;
            }
        };
    }

    /* -------------------------------------------------------------
     *  Core Set operations
     * ------------------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(defaultSet.add("hello"));
        assertTrue(defaultSet.contains("hello"));
        // duplicate add must return false and not change size
        assertFalse(defaultSet.add("hello"));
        assertEquals(1, defaultSet.size());
    }

    @Test
    public void testAddNullElement() {
        assertTrue(defaultSet.add(null));
        assertTrue(defaultSet.contains(null));
        assertEquals(1, defaultSet.size());
    }

    @Test
    public void testRemoveExistingAndNonExisting() {
        defaultSet.add("x");
        assertTrue(defaultSet.remove("x"));
        assertFalse(defaultSet.remove("x")); // already removed
        assertFalse(defaultSet.remove("non‑existent"));
    }

    @Test
    public void testClear() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty());
    }

    @Test
    public void testContainsAll() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(defaultSet.containsAll(Arrays.asList("a", "c")));
        assertFalse(defaultSet.containsAll(Arrays.asList("a", "z")));
    }

    @Test
    public void testAddAllAndAddAllWithDuplicates() {
        List<String> list = Arrays.asList("a", "b", "a", "c");
        assertTrue(defaultSet.addAll(list)); // at least one new element
        assertEquals(3, defaultSet.size());
        // Adding the same collection again must return false (no modification)
        assertFalse(defaultSet.addAll(list));
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllRejectsNullCollection() {
        defaultSet.addAll(null);
    }

    @Test
    public void testRetainAll() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(defaultSet.retainAll(Arrays.asList("b", "c", "d"))); // 'a' removed
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains("a"));
        assertTrue(defaultSet.containsAll(Arrays.asList("b", "c")));
    }

    @Test
    public void testRemoveAll() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(defaultSet.removeAll(Arrays.asList("b", "x"))); // 'b' removed
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains("b"));
        assertTrue(defaultSet.containsAll(Arrays.asList("a", "c")));
    }

    @Test
    public void testIteratorBehaviour() {
        defaultSet.addAll(Arrays.asList("one", "two", "three"));
        Iterator<String> it = defaultSet.iterator();
        Set<String> seen = new HashSet<>();
        while (it.hasNext()) {
            String v = it.next();
            assertTrue(seen.add(v)); // no duplicates while iterating
            if ("two".equals(v)) {
                it.remove(); // test iterator.remove()
            }
        }
        assertFalse(defaultSet.contains("two"));
        assertEquals(2, defaultSet.size());
    }

    @Test
    public void testToArray() {
        defaultSet.addAll(Arrays.asList("a", "b"));
        Object[] arr = defaultSet.toArray();
        assertEquals(2, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.containsAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testToArrayWithExactSizedArray() {
        defaultSet.addAll(Arrays.asList("x", "y"));
        String[] dest = new String[2];
        String[] result = defaultSet.toArray(dest);
        assertSame(dest, result);
        assertTrue(Arrays.asList(result).containsAll(Arrays.asList("x", "y")));
    }

    @Test
    public void testToArrayWithLargerArray() {
        defaultSet.addAll(Arrays.asList("p"));
        String[] dest = new String[5];
        String[] result = defaultSet.toArray(dest);
        assertSame(dest, result);
        assertEquals("p", result[0]);
        assertNull(result[1]); // per Collection.toArray contract
    }

    /* -------------------------------------------------------------
     *  Object overrides
     * ------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CompactSet<String> s1 = new CompactSet<>();
        CompactSet<String> s2 = new CompactSet<>();
        s1.addAll(Arrays.asList("a", "b"));
        s2.addAll(Arrays.asList("b", "a"));
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        // Different content
        s2.add("c");
        assertNotEquals(s1, s2);
    }

    @Test
    public void testEqualsWithOtherSetImplementations() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("x", "y"));
        CompactSet<String> cs = new CompactSet<>();
        cs.addAll(hashSet);
        assertEquals(hashSet, cs);
        assertEquals(cs, hashSet);
    }

    @Test
    public void testEqualsWithNullAndDifferentType() {
        assertFalse(defaultSet.equals(null));
        assertFalse(defaultSet.equals("not a set"));
    }

    @Test
    public void testToStringContainsElements() {
        defaultSet.addAll(Arrays.asList("alpha", "beta"));
        String s = defaultSet.toString();
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
    }

    /* -------------------------------------------------------------
     *  Builder and configuration API
     * ------------------------------------------------------------- */

    @Test
    public void testBuilderCreatesConfiguredSet() {
        CompactSet<String> ordered = CompactSet.<String>builder()
                .caseSensitive(false)
                .insertionOrder()
                .compactSize(70)
                .build();

        Map<String, Object> cfg = ordered.getConfig();
        assertEquals(70, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.INSERTION, cfg.get(CompactMap.ORDERING));
    }

    @Test
    public void testWithConfigOverridesExistingConfiguration() {
        CompactSet<String> original = CompactSet.<String>builder()
                .caseSensitive(true)
                .sortedOrder()
                .compactSize(60)
                .build();
        original.addAll(Arrays.asList("c", "a", "b"));

        Map<String, Object> newConfig = new HashMap<>();
        newConfig.put(CompactMap.CASE_SENSITIVE, Boolean.FALSE);
        newConfig.put(CompactMap.ORDERING, CompactMap.REVERSE);
        newConfig.put(CompactMap.COMPACT_SIZE, 30);

        CompactSet<String> revised = original.withConfig(newConfig);
        Map<String, Object> cfg = revised.getConfig();

        assertEquals(30, cfg.get(CompactMap.COMPACT_SIZE));
        assertEquals(Boolean.FALSE, cfg.get(CompactMap.CASE_SENSITIVE));
        assertEquals(CompactMap.REVERSE, cfg.get(CompactMap.ORDERING));

        // The elements must be preserved
        assertEquals(original.size(), revised.size());
        assertTrue(revised.containsAll(original));

        // Verify ordering effect by checking iterator order (reverse of natural order)
        Iterator<String> it = revised.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) iterated.add(it.next());
        List<String> expected = new ArrayList<>(Arrays.asList("c", "b", "a")); // reverse of sorted
        assertEquals(expected, iterated);
    }

    @Test(expected = RuntimeException.class) // Convention.throwIfNull throws a RuntimeException
    public void testWithConfigRejectsNullArgument() {
        defaultSet.withConfig(null);
    }

    /* -------------------------------------------------------------
     *  Edge cases for bulk operations
     * ------------------------------------------------------------- */

    @Test
    public void testRetainAllWithEmptyCollectionLeavesSetEmpty() {
        defaultSet.addAll(Arrays.asList("x", "y"));
        assertTrue(defaultSet.retainAll(Collections.emptyList()));
        assertTrue(defaultSet.isEmpty());
    }

    @Test
    public void testRemoveAllWithEmptyCollectionLeavesSetUnchanged() {
        defaultSet.addAll(Arrays.asList("p", "q"));
        assertFalse(defaultSet.removeAll(Collections.emptyList()));
        assertEquals(2, defaultSet.size());
    }

    @Test
    public void testContainsAllWithEmptyCollectionAlwaysTrue() {
        assertTrue(defaultSet.containsAll(Collections.emptyList()));
    }

    @Test
    public void testAddAllWithEmptyCollectionDoesNotModifySet() {
        assertFalse(defaultSet.addAll(Collections.emptyList()));
        assertTrue(defaultSet.isEmpty());
    }
}
