package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests are organised as follows:
 * <ul>
 *   <li>Verification of the public API (constructors and methods) – basic happy‑path behaviour.</li>
 *   <li>Edge‑case and exception tests (null arguments, illegal capacities, array store errors, etc.).</li>
 *   <li>Specific case‑insensitive semantics for {@link String} elements.</li>
 *   <li>Coverage of the deprecated {@code plus}/{@code minus} helpers.</li>
 * </ul>
 */
public class CaseInsensitiveSetTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private CaseInsensitiveSet<String> stringSet;
    private CaseInsensitiveSet<Object> mixedSet;

    @Before
    public void setUp() {
        stringSet = new CaseInsensitiveSet<>();
        mixedSet = new CaseInsensitiveSet<>();
    }

    /* -------------------------------------------------
     *  Public constructors
     * ------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    @Test
    public void testConstructorWithCollectionCopiesElements() {
        List<String> src = Arrays.asList("One", "Two", "Three");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(src));
    }

    @Test
    public void testConstructorWithCollectionAndBackingMap() {
        List<String> src = Arrays.asList("A", "B");
        Map<String, Object> backing = new LinkedHashMap<>();
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src, backing);
        assertEquals(2, set.size());
        assertSame(backing, getInternalMap(set));
    }

    @Test
    public void testConstructorWithNegativeCapacityThrows() {
        thrown.expect(IllegalArgumentException.class);
        new CaseInsensitiveSet<>(-1);
    }

    @Test
    public void testConstructorWithZeroCapacityIsAllowed() {
        // Zero capacity is legal (behaviour defined by underlying map)
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(0);
        assertTrue(set.isEmpty());
    }

    @Test
    public void testConstructorWithInvalidLoadFactorThrows() {
        thrown.expect(IllegalArgumentException.class);
        new CaseInsensitiveSet<>(10, 0.0f);
    }

    @Test
    public void testConstructorWithNullCollectionThrows() {
        thrown.expect(NullPointerException.class);
        new CaseInsensitiveSet<String>(null);
    }

    @Test
    public void testConstructorWithNullCollectionAndBackingMapThrows() {
        thrown.expect(NullPointerException.class);
        new CaseInsensitiveSet<String>(null, new HashMap<>());
    }

    @Test
    public void testConstructorWithNullBackingMapThrows() {
        thrown.expect(NullPointerException.class);
        new CaseInsensitiveSet<String>(Collections.emptyList(), null);
    }

    /* -------------------------------------------------
     *  Basic Set operations
     * ------------------------------------------------- */

    @Test
    public void testAddAndContains() {
        assertTrue(stringSet.add("Hello"));
        assertTrue(stringSet.contains("Hello"));
        // case‑insensitive duplicate – add returns false, contains still true
        assertFalse(stringSet.add("HELLO"));
        assertTrue(stringSet.contains("hello"));
        assertEquals(1, stringSet.size());
    }

    @Test
    public void testRemove() {
        stringSet.add("World");
        assertTrue(stringSet.remove("world")); // case‑insensitive removal
        assertFalse(stringSet.contains("World"));
        assertEquals(0, stringSet.size());
    }

    @Test
    public void testAddAllAndContainsAll() {
        List<String> list = Arrays.asList("One", "Two", "Three");
        assertTrue(stringSet.addAll(list));
        assertTrue(stringSet.containsAll(list));
        // adding the same collection again must return false (no change)
        assertFalse(stringSet.addAll(list));
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        stringSet.addAll(Arrays.asList("A", "B", "C"));
        assertTrue(stringSet.retainAll(Arrays.asList("b", "c", "d"))); // case‑insensitive retain
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.containsAll(Arrays.asList("B", "C")));
        assertFalse(stringSet.contains("A"));
    }

    @Test
    public void testRemoveAllEliminatesSpecifiedElements() {
        stringSet.addAll(Arrays.asList("X", "Y", "Z"));
        assertTrue(stringSet.removeAll(Arrays.asList("y", "z"))); // case‑insensitive removal
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("X"));
    }

    @Test
    public void testClearEmptiesSet() {
        stringSet.addAll(Arrays.asList("a", "b"));
        stringSet.clear();
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    @Test
    public void testIteratorPreservesOriginalCase() {
        stringSet.addAll(Arrays.asList("First", "second"));
        Iterator<String> it = stringSet.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // Insertion order is preserved by the default backing map
        assertEquals(Arrays.asList("First", "second"), iterated);
    }

    @Test
    public void testToArrayReturnsAllElements() {
        stringSet.addAll(Arrays.asList("alpha", "beta"));
        Object[] arr = stringSet.toArray();
        assertArrayEquals(new Object[]{"alpha", "beta"}, arr);
    }

    @Test
    public void testToArrayWithTypedArray() {
        stringSet.addAll(Arrays.asList("x", "y"));
        String[] a = new String[0];
        String[] result = stringSet.toArray(a);
        assertArrayEquals(new String[]{"x", "y"}, result);
    }

    @Test
    public void testToArrayWithTooSmallTypedArray() {
        stringSet.addAll(Arrays.asList("one", "two"));
        String[] a = new String[1];
        String[] result = stringSet.toArray(a);
        // The method must allocate a new array of the same runtime type
        assertNotSame(a, result);
        assertArrayEquals(new String[]{"one", "two"}, result);
    }

    @Test
    public void testToArrayWithNullArrayThrows() {
        thrown.expect(NullPointerException.class);
        stringSet.toArray((String[]) null);
    }

    @Test
    public void testToArrayWithIncompatibleArrayThrows() {
        thrown.expect(ArrayStoreException.class);
        // Set contains Strings, but we pass an Integer[] – should trigger ArrayStoreException
        Integer[] a = new Integer[0];
        stringSet.toArray(a);
    }

    /* -------------------------------------------------
     *  Equality, hashCode and toString
     * ------------------------------------------------- */

    @Test
    public void testEqualsAndHashCode() {
        CaseInsensitiveSet<String> s1 = new CaseInsensitiveSet<>();
        CaseInsensitiveSet<String> s2 = new CaseInsensitiveSet<>();
        s1.add("One");
        s2.add("ONE"); // same element case‑insensitively
        assertEquals(s1, s2);
        assertEquals(s1.hashCode(), s2.hashCode());

        // different type – should be false
        assertNotEquals(s1, Collections.singleton("One"));
    }

    @Test
    public void testEqualsWithNonSetObject() {
        assertFalse(stringSet.equals("not a set"));
    }

    @Test
    public void testToStringPreservesOriginalCaseAndOrder() {
        stringSet.addAll(Arrays.asList("Apple", "banana"));
        assertEquals("[Apple, banana]", stringSet.toString());
    }

    /* -------------------------------------------------
     *  Mixed‑type handling
     * ------------------------------------------------- */

    @Test
    public void testMixedTypesAreHandledCorrectly() {
        mixedSet.add("String");
        mixedSet.add(100);
        mixedSet.add("string"); // duplicate case‑insensitive, should not be added
        assertEquals(2, mixedSet.size());
        assertTrue(mixedSet.contains("STRING"));
        assertTrue(mixedSet.contains(100));
    }

    /* -------------------------------------------------
     *  Deprecated plus/minus helpers
     * ------------------------------------------------- */

    @Test
    public void testDeprecatedPlusAndMinusMethods() {
        // plus(Iterable)
        List<String> extra = Arrays.asList("a", "b");
        stringSet.plus(extra);
        assertTrue(stringSet.containsAll(extra));

        // plus(Object)
        stringSet.plus("c");
        assertTrue(stringSet.contains("C"));

        // minus(Iterable)
        stringSet.minus(Arrays.asList("A", "b"));
        assertFalse(stringSet.contains("a"));
        assertFalse(stringSet.contains("B"));
        assertTrue(stringSet.contains("c"));

        // minus(Object)
        stringSet.minus("c");
        assertFalse(stringSet.contains("c"));
    }

    /* -------------------------------------------------
     *  Helper to expose the internal map for a couple of tests.
     * ------------------------------------------------- */

    @SuppressWarnings("unchecked")
    private static <K> Map<K, Object> getInternalMap(CaseInsensitiveSet<K> set) {
        try {
            java.lang.reflect.Field f = CaseInsensitiveSet.class.getDeclaredField("map");
            f.setAccessible(true);
            return (Map<K, Object>) f.get(set);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
