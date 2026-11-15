package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactSet}.
 *
 * <p>The tests are written as if three independent experts (one per method) had
 * contributed a single test case each, then merged their work into this file.
 * The suite therefore covers:
 *
 * <ul>
 *   <li>Construction (default, collection, builder with custom options, illegal config)</li>
 *   <li>All {@link Set} operations (size, isEmpty, contains, add, remove, clear, …)</li>
 *   <li>Iterator behaviour (including {@code remove()})</li>
 *   <li>Array conversion (both {@code toArray()} and {@code toArray(T[])} variants)</li>
 *   <li>Object contract methods ({@code equals}, {@code hashCode}, {@code toString})</li>
 *   <li>Special features of {@code CompactSet} – case‑insensitivity and ordering</li>
 * </ul>
 *
 * <p>Only public APIs that appear in the supplied source code (or in the
 * accompanying {@code CompactMap} API) are used – no mocking or subclassing of
 * library types.</p>
 */
public class CompactSetTest {

    private CompactSet<String> defaultSet;
    private CompactSet<Integer> insertionSet;
    private CompactSet<String> caseInsensitiveSet;
    private CompactSet<String> sortedSet;
    private CompactSet<String> reverseSet;

    @Before
    public void setUp() {
        // default (unordered, case‑sensitive, compactSize = 70)
        defaultSet = new CompactSet<>();

        // insertion order – useful for iterator order tests
        insertionSet = CompactSet.<Integer>builder()
                .insertionOrder()
                .compactSize(10)          // small size, still >=2
                .build();

        // case‑insensitive string set
        caseInsensitiveSet = CompactSet.<String>builder()
                .caseSensitive(false)
                .compactSize(10)
                .build();

        // natural sorted order
        sortedSet = CompactSet.<String>builder()
                .sortedOrder()
                .compactSize(10)
                .build();

        // reverse sorted order
        reverseSet = CompactSet.<String>builder()
                .reverseOrder()
                .compactSize(10)
                .build();
    }

    /* -------------------------------------------------------------
     *  Construction & illegal configuration
     * ------------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElements() {
        List<String> src = Arrays.asList("a", "b", "c");
        CompactSet<String> fromCollection = new CompactSet<>(src);
        assertEquals(3, fromCollection.size());
        assertTrue(fromCollection.containsAll(src));
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorNullThrowsNPE() {
        new CompactSet<String>((Collection<String>) null);
    }

    @Test(expected = IllegalStateException.class)
    public void testBuilderWithIllegalCompactSizeThrows() {
        // compactSize < 2 must trigger IllegalStateException in CompactSet ctor
        CompactSet.builder()
                .compactSize(1)   // illegal
                .build();
    }

    /* -------------------------------------------------------------
     *  Basic Set contract – size, isEmpty, clear, add, remove
     * ------------------------------------------------------------- */

    @Test
    public void testAddAndSizeBehaviour() {
        assertTrue(defaultSet.add("one"));
        assertTrue(defaultSet.add("two"));
        assertFalse(defaultSet.add("one")); // duplicate – no change
        assertEquals(2, defaultSet.size());
    }

    @Test
    public void testRemoveBehaviour() {
        defaultSet.add("x");
        defaultSet.add("y");
        assertTrue(defaultSet.remove("x"));
        assertFalse(defaultSet.remove("z")); // not present
        assertEquals(1, defaultSet.size());
        assertFalse(defaultSet.contains("x"));
    }

    @Test
    public void testClearEmptiesSet() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(defaultSet.isEmpty());
        defaultSet.clear();
        assertTrue(defaultSet.isEmpty());
        assertEquals(0, defaultSet.size());
    }

    /* -------------------------------------------------------------
     *  contains / containsAll
     * ------------------------------------------------------------- */

    @Test
    public void testContainsAndContainsAll() {
        defaultSet.addAll(Arrays.asList("alpha", "beta"));
        assertTrue(defaultSet.contains("alpha"));
        assertFalse(defaultSet.contains("gamma"));

        List<String> sub = Arrays.asList("alpha", "beta");
        List<String> sup = Arrays.asList("alpha", "beta", "gamma");
        assertTrue(defaultSet.containsAll(sub));
        assertFalse(defaultSet.containsAll(sup));
    }

    /* -------------------------------------------------------------
     *  addAll, retainAll, removeAll
     * ------------------------------------------------------------- */

    @Test
    public void testAddAllReturnsTrueOnlyIfModified() {
        assertFalse(defaultSet.addAll(Collections.emptyList()));
        assertTrue(defaultSet.addAll(Arrays.asList("a", "b")));
        // second call adds nothing new – should return false
        assertFalse(defaultSet.addAll(Arrays.asList("a", "b")));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        defaultSet.addAll(Arrays.asList("one", "two", "three"));
        assertTrue(defaultSet.retainAll(Arrays.asList("two", "three", "four")));
        assertEquals(2, defaultSet.size());
        assertTrue(defaultSet.contains("two"));
        assertTrue(defaultSet.contains("three"));
        assertFalse(defaultSet.contains("one"));
    }

    @Test
    public void testRemoveAllRemovesSpecifiedElements() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        assertTrue(defaultSet.removeAll(Arrays.asList("b", "x"))); // b exists, x does not
        assertEquals(2, defaultSet.size());
        assertFalse(defaultSet.contains("b"));
        assertTrue(defaultSet.contains("a"));
        assertTrue(defaultSet.contains("c"));
    }

    /* -------------------------------------------------------------
     *  Iterator behaviour (including remove)
     * ------------------------------------------------------------- */

    @Test
    public void testIteratorOrderInsertionSet() {
        // insertion order: 10, 20, 30
        insertionSet.addAll(Arrays.asList(10, 20, 30));
        Iterator<Integer> it = insertionSet.iterator();
        assertEquals(Integer.valueOf(10), it.next());
        assertEquals(Integer.valueOf(20), it.next());
        assertEquals(Integer.valueOf(30), it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void testIteratorRemoveWorks() {
        defaultSet.addAll(Arrays.asList("x", "y", "z"));
        Iterator<String> it = defaultSet.iterator();
        while (it.hasNext()) {
            String val = it.next();
            if ("y".equals(val)) {
                it.remove(); // remove via iterator
            }
        }
        assertFalse(defaultSet.contains("y"));
        assertEquals(2, defaultSet.size());
    }

    /* -------------------------------------------------------------
     *  Ordering specific tests (sorted / reverse)
     * ------------------------------------------------------------- */

    @Test
    public void testSortedOrderIteration() {
        sortedSet.addAll(Arrays.asList("delta", "alpha", "charlie", "bravo"));
        List<String> expected = Arrays.asList("alpha", "bravo", "charlie", "delta");
        List<String> actual = new ArrayList<>();
        for (String s : sortedSet) {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void testReverseOrderIteration() {
        reverseSet.addAll(Arrays.asList("delta", "alpha", "charlie", "bravo"));
        List<String> expected = Arrays.asList("delta", "charlie", "bravo", "alpha");
        List<String> actual = new ArrayList<>();
        for (String s : reverseSet) {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }

    /* -------------------------------------------------------------
     *  Case‑insensitivity
     * ------------------------------------------------------------- */

    @Test
    public void testCaseInsensitiveSetTreatsDifferentCasesAsEqual() {
        caseInsensitiveSet.add("Test");
        assertTrue(caseInsensitiveSet.contains("test"));
        assertFalse(caseInsensitiveSet.add("TEST")); // duplicate under case‑insensitive view
        assertEquals(1, caseInsensitiveSet.size());
    }

    /* -------------------------------------------------------------
     *  toArray() variants
     * ------------------------------------------------------------- */

    @Test
    public void testToArrayReturnsAllElements() {
        defaultSet.addAll(Arrays.asList("a", "b", "c"));
        Object[] arr = defaultSet.toArray();
        assertEquals(3, arr.length);
        List<Object> list = Arrays.asList(arr);
        assertTrue(list.containsAll(Arrays.asList("a", "b", "c")));
    }

    @Test
    public void testToArrayTypedCreatesCorrectRuntimeType() {
        defaultSet.addAll(Arrays.asList("x", "y"));
        String[] a = new String[0];
        String[] result = defaultSet.toArray(a);
        assertArrayEquals(new String[]{"x", "y"}, result);
    }

    @Test
    public void testToArrayTypedWithLargerDestinationArray() {
        defaultSet.addAll(Arrays.asList("p", "q"));
        String[] dest = new String[5];
        dest[2] = "shouldBeOverwritten";
        String[] result = defaultSet.toArray(dest);
        // The returned array should be the same instance (dest) and contain the elements,
        // followed by a null sentinel per the Collection contract.
        assertSame(dest, result);
        assertEquals("p", result[0]);
        assertEquals("q", result[1]);
        assertNull(result[2]); // first extra slot must be null
    }

    /* -------------------------------------------------------------
     *  equals, hashCode, toString
     * ------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeMatchHashSetBehaviour() {
        Set<String> hashSet = new HashSet<>(Arrays.asList("one", "two"));
        defaultSet.addAll(hashSet);
        assertEquals(hashSet, defaultSet);
        assertEquals(hashSet.hashCode(), defaultSet.hashCode());
    }

    @Test
    public void testEqualsWithDifferentImplementation() {
        List<String> list = Arrays.asList("a", "b");
        CompactSet<String> cs = new CompactSet<>(list);
        assertFalse(cs.equals(list)); // different collection type
    }

    @Test
    public void testToStringReflectsElementSet() {
        defaultSet.addAll(Arrays.asList("alpha", "beta"));
        String s = defaultSet.toString();
        // The string must start with '[' and end with ']' and contain both elements
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        assertTrue(s.contains("alpha"));
        assertTrue(s.contains("beta"));
    }

    /* -------------------------------------------------------------
     *  Builder customisation tests (caseSensitive flag & ordering)
     * ------------------------------------------------------------- */

    @Test
    public void testBuilderCaseSensitiveFlagAffectsBehaviour() {
        CompactSet<String> caseSensitiveSet = CompactSet.<String>builder()
                .caseSensitive(true)
                .compactSize(10)
                .build();
        caseSensitiveSet.add("Hello");
        assertTrue(caseSensitiveSet.contains("Hello"));
        assertFalse(caseSensitiveSet.contains("hello"));
    }

    @Test
    public void testBuilderInsertionOrderPreservesInsertionSequence() {
        CompactSet<Integer> set = CompactSet.<Integer>builder()
                .insertionOrder()
                .compactSize(5)
                .build();
        set.addAll(Arrays.asList(3, 1, 4, 2));
        Iterator<Integer> it = set.iterator();
        assertEquals(Integer.valueOf(3), it.next());
        assertEquals(Integer.valueOf(1), it.next());
        assertEquals(Integer.valueOf(4), it.next());
        assertEquals(Integer.valueOf(2), it.next());
    }

    /* -------------------------------------------------------------
     *  Null handling (CompactMap permits null keys – we verify the contract)
     * ------------------------------------------------------------- */

    @Test
    public void testAddNullElement() {
        // According to Map contract, null keys are allowed unless the underlying map forbids them.
        // CompactMap (as used here) supports null keys, so the set should as well.
        defaultSet.add(null);
        assertTrue(defaultSet.contains(null));
        assertEquals(1, defaultSet.size());
        assertTrue(defaultSet.remove(null));
        assertFalse(defaultSet.contains(null));
    }

    /* -------------------------------------------------------------
     *  Defensive copy of collection argument in addAll / constructor
     * ------------------------------------------------------------- */

    @Test
    public void testAddAllDoesNotRetainReferenceToInputCollection() {
        List<String> source = new ArrayList<>(Arrays.asList("a", "b"));
        defaultSet.addAll(source);
        source.add("c"); // modify after addAll
        // The set must not contain "c" because addAll should have copied the elements
        assertFalse(defaultSet.contains("c"));
    }

    @Test
    public void testCollectionConstructorDoesNotRetainReference() {
        List<String> source = new ArrayList<>(Arrays.asList("x", "y"));
        CompactSet<String> cs = new CompactSet<>(source);
        source.clear();
        // The set must still contain the original elements
        assertTrue(cs.contains("x"));
        assertTrue(cs.contains("y"));
        assertEquals(2, cs.size());
    }
}
