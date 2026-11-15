/***************************************************************************************************
 * JUnit‑4 test suite for {@link com.cedarsoftware.util.CaseInsensitiveSet}.
 *
 * The tests are written from the perspective of three “experts”.  Each expert contributed a
 * test case for every public method, covering typical usage, edge‑cases and error handling.
 *
 * The suite validates:
 *   • Correct case‑insensitive behaviour for {@link String} elements while preserving the original
 *     case for iteration and output.
 *   • Proper handling of non‑String elements (they behave like a normal {@link Set}).
 *   • Constructor selection logic (default, collection‑based, capacity‑based and custom map).
 *   • Equality, hash‑code, iterator order, array conversion and the deprecated {@code plus}/{@code minus}
 *     convenience methods.
 *   • Exceptional situations such as illegal arguments and {@code NullPointerException}s.
 *
 * The test class can be compiled and executed with any JUnit‑4 runner.
 **************************************************************************************************/
package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

public class CaseInsensitiveSetTest {

    /* -------------------------------------------------------------------------
     *  Helper factories – used by several tests
     * ---------------------------------------------------------------------- */
    private CaseInsensitiveSet<String> stringSet;
    private CaseInsensitiveSet<Object> mixedSet;

    @Before
    public void setUp() {
        stringSet = new CaseInsensitiveSet<>();
        mixedSet  = new CaseInsensitiveSet<>();
    }

    /* -------------------------------------------------------------------------
     *  Constructor tests
     * ---------------------------------------------------------------------- */

    @Test
    public void constructor_default_createsEmptySet() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void constructor_collectionArrayList_preservesInsertionOrder() {
        List<String> list = Arrays.asList("Apple", "banana", "CHERRY");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(list);
        // Insertion order must be preserved (LinkedHashMap backing)
        Iterator<String> it = set.iterator();
        assertEquals("Apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("CHERRY", it.next());
    }

    @Test
    public void constructor_collectionSortedSet_usesTreeMapBackedMap() {
        SortedSet<String> sorted = new TreeSet<>(Arrays.asList("c", "b", "a"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(sorted);
        // TreeMap provides natural ordering – case‑insensitive, so order is a,b,c
        Iterator<String> it = set.iterator();
        assertEquals("a", it.next());
        assertEquals("b", it.next());
        assertEquals("c", it.next());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_collectionNull_throwsNPE() {
        new CaseInsensitiveSet<String>(null);
    }

    @Test
    public void constructor_initialCapacity_positiveCreatesEmptySet() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_initialCapacity_negativeThrowsIAE() {
        new CaseInsensitiveSet<>(-5);
    }

    @Test
    public void constructor_initialCapacityAndLoadFactor_positiveCreatesEmptySet() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>(10, 0.75f);
        assertTrue(set.isEmpty());
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_initialCapacityAndLoadFactor_negativeCapacityThrowsIAE() {
        new CaseInsensitiveSet<>(-1, 0.75f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_initialCapacityAndLoadFactor_nonPositiveLoadFactorThrowsIAE() {
        new CaseInsensitiveSet<>(10, 0.0f);
    }

    @Test
    public void constructor_collectionAndCustomMap_respectsProvidedMap() {
        // Use a TreeMap as the custom backing map – it gives sorted iteration order.
        Map<String, Object> backing = new CaseInsensitiveMap<>(new TreeMap<>());
        List<String> source = Arrays.asList("z", "y", "x");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source, backing);
        Iterator<String> it = set.iterator();
        assertEquals("x", it.next()); // TreeMap sorts ascending
        assertEquals("y", it.next());
        assertEquals("z", it.next());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_collectionAndCustomMap_nullCollection_throwsNPE() {
        new CaseInsensitiveSet<>(null, new CaseInsensitiveMap<>());
    }

    @Test(expected = NullPointerException.class)
    public void constructor_collectionAndCustomMap_nullMap_throwsNPE() {
        new CaseInsensitiveSet<>(Collections.emptyList(), null);
    }

    /* -------------------------------------------------------------------------
     *  Basic Set operations – add / contains / remove / size / isEmpty
     * ---------------------------------------------------------------------- */

    @Test
    public void add_stringCaseInsensitive_addsOnlyOnce() {
        assertTrue(stringSet.add("Hello"));
        assertFalse(stringSet.add("HELLO")); // duplicate, different case
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("hello")); // case‑insensitive lookup
    }

    @Test
    public void add_nonStringElements_behaveNormally() {
        assertTrue(mixedSet.add(123));
        assertTrue(mixedSet.add(456));
        assertFalse(mixedSet.add(123)); // duplicate integer
        assertEquals(2, mixedSet.size());
        assertTrue(mixedSet.contains(456));
    }

    @Test
    public void remove_stringCaseInsensitive_removesRegardlessOfCase() {
        stringSet.add("World");
        assertTrue(stringSet.remove("world"));
        assertFalse(stringSet.contains("WORLD"));
        assertTrue(stringSet.isEmpty());
    }

    @Test
    public void remove_nonStringElement() {
        mixedSet.add(99);
        assertTrue(mixedSet.remove(99));
        assertFalse(mixedSet.contains(99));
    }

    @Test
    public void contains_mixedTypes_caseInsensitiveForStrings() {
        mixedSet.add("Apple");
        mixedSet.add(42);
        assertTrue(mixedSet.contains("apple"));
        assertTrue(mixedSet.contains(42));
        assertFalse(mixedSet.contains("banana"));
    }

    @Test
    public void size_and_isEmpty_reflectCurrentState() {
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
        stringSet.add("One");
        assertFalse(stringSet.isEmpty());
        assertEquals(1, stringSet.size());
    }

    /* -------------------------------------------------------------------------
     *  Bulk operations – addAll, containsAll, removeAll, retainAll
     * ---------------------------------------------------------------------- */

    @Test
    public void addAll_collectionWithDuplicates_caseInsensitive() {
        List<String> list = Arrays.asList("a", "A", "b");
        assertTrue(stringSet.addAll(list));
        assertEquals(2, stringSet.size()); // "a" and "b"
        assertTrue(stringSet.containsAll(Arrays.asList("A", "b")));
    }

    @Test(expected = NullPointerException.class)
    public void addAll_nullCollection_throwsNPE() {
        stringSet.addAll(null);
    }

    @Test
    public void containsAll_mixedCollection_respectsCaseInsensitivity() {
        stringSet.addAll(Arrays.asList("x", "Y"));
        assertTrue(stringSet.containsAll(Arrays.asList("X", "y")));
        assertFalse(stringSet.containsAll(Arrays.asList("X", "z")));
    }

    @Test
    public void removeAll_collectionRemovesCaseInsensitive() {
        stringSet.addAll(Arrays.asList("foo", "Bar", "baz"));
        boolean changed = stringSet.removeAll(Arrays.asList("FOO", "baz"));
        assertTrue(changed);
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("Bar"));
    }

    @Test
    public void retainAll_keepsOnlySpecifiedElements_caseInsensitive() {
        stringSet.addAll(Arrays.asList("alpha", "Beta", "GAMMA"));
        boolean changed = stringSet.retainAll(Arrays.asList("ALPHA", "gamma"));
        assertTrue(changed);
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains("alpha"));
        assertTrue(stringSet.contains("GAMMA"));
        assertFalse(stringSet.contains("Beta"));
    }

    /* -------------------------------------------------------------------------
     *  Iterator and array conversion
     * ---------------------------------------------------------------------- */

    @Test
    public void iterator_preservesOriginalCaseAndInsertionOrder() {
        stringSet.add("First");
        stringSet.add("second");
        stringSet.add("THIRD");
        Iterator<String> it = stringSet.iterator();
        assertEquals("First", it.next());
        assertEquals("second", it.next());
        assertEquals("THIRD", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    public void toArray_returnsObjectArrayPreservingCase() {
        stringSet.addAll(Arrays.asList("One", "TWO"));
        Object[] arr = stringSet.toArray();
        assertArrayEquals(new Object[]{"One", "TWO"}, arr);
    }

    @Test
    public void toArray_TypedArray_fitsExactly() {
        stringSet.addAll(Arrays.asList("a", "b"));
        String[] dest = new String[2];
        String[] result = stringSet.toArray(dest);
        assertSame(dest, result);
        assertArrayEquals(new String[]{"a", "b"}, result);
    }

    @Test
    public void toArray_TypedArray_largerCreatesNewArray() {
        stringSet.addAll(Arrays.asList("x", "y"));
        String[] dest = new String[5];
        String[] result = stringSet.toArray(dest);
        assertNotSame(dest, result); // because dest is larger than needed
        assertEquals(2, result.length);
        assertArrayEquals(new String[]{"x", "y"}, result);
    }

    @Test(expected = NullPointerException.class)
    public void toArray_TypedArray_nullArray_throwsNPE() {
        stringSet.toArray((String[]) null);
    }

    @Test(expected = ArrayStoreException.class)
    public void toArray_TypedArray_wrongComponentType_throwsArrayStoreException() {
        stringSet.add("hello");
        // Attempt to store a String into an Integer[] array
        Integer[] dest = new Integer[1];
        stringSet.toArray(dest);
    }

    /* -------------------------------------------------------------------------
     *  Equality and hashCode
     * ---------------------------------------------------------------------- */

    @Test
    public void equals_sameElementsDifferentCase_returnsTrue() {
        CaseInsensitiveSet<String> s1 = new CaseInsensitiveSet<>();
        CaseInsensitiveSet<String> s2 = new CaseInsensitiveSet<>();
        s1.add("Java");
        s2.add("java");
        assertTrue(s1.equals(s2));
        assertTrue(s2.equals(s1));
        assertEquals(s1.hashCode(), s2.hashCode());
    }

    @Test
    public void equals_differentTypesOrSizes_returnsFalse() {
        CaseInsensitiveSet<String> s1 = new CaseInsensitiveSet<>();
        s1.add("one");
        Set<String> other = new HashSet<>(Collections.singletonList("one"));
        assertFalse(s1.equals(other)); // other is not a CaseInsensitiveSet but a Set – still should be false because of type check
        CaseInsensitiveSet<String> s2 = new CaseInsensitiveSet<>();
        s2.add("two");
        assertFalse(s1.equals(s2));
    }

    @Test
    public void hashCode_consistentWithUnderlyingMapKeySet() {
        stringSet.addAll(Arrays.asList("a", "B"));
        int expected = stringSet.map.keySet().hashCode(); // package‑private access allowed inside same package
        assertEquals(expected, stringSet.hashCode());
    }

    /* -------------------------------------------------------------------------
     *  clear() operation
     * ---------------------------------------------------------------------- */

    @Test
    public void clear_emptiesTheSet() {
        stringSet.addAll(Arrays.asList("x", "y"));
        assertFalse(stringSet.isEmpty());
        stringSet.clear();
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    /* -------------------------------------------------------------------------
     *  Deprecated plus/minus convenience methods
     * ---------------------------------------------------------------------- */

    @Test
    public void plus_iterableAddsElements() {
        List<String> toAdd = Arrays.asList("alpha", "BETA");
        stringSet.plus(toAdd);
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains("ALPHA"));
        assertTrue(stringSet.contains("beta"));
    }

    @Test
    public void plus_singleObjectAddsElement() {
        stringSet.plus("Gamma");
        assertTrue(stringSet.contains("gamma"));
        assertEquals(1, stringSet.size());
    }

    @Test
    public void minus_iterableRemovesElements() {
        stringSet.addAll(Arrays.asList("one", "two", "three"));
        List<String> toRemove = Arrays.asList("ONE", "THREE");
        stringSet.minus(toRemove);
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("two"));
    }

    @Test
    public void minus_singleObjectRemovesElement() {
        stringSet.addAll(Arrays.asList("a", "b"));
        stringSet.minus("A");
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("b"));
    }

    /* -------------------------------------------------------------------------
     *  toString() – ensures original case is shown and order matches iteration order
     * ---------------------------------------------------------------------- */

    @Test
    public void toString_preservesOriginalCaseAndOrder() {
        stringSet.addAll(Arrays.asList("First", "second", "THIRD"));
        assertEquals("[First, second, THIRD]", stringSet.toString());
    }
}
