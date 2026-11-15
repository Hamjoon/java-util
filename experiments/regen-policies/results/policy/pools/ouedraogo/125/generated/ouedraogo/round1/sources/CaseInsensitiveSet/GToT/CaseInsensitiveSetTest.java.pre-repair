package com.cedarsoftware.util;

import org.junit.Test;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.ExpectedException;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit 4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>Basic functionality of each Set operation</li>
 *   <li>Case‑insensitive behavior for {@link String} elements</li>
 *   <li>Mixed‑type handling (String + non‑String)</li>
 *   <li>Edge‑cases and exception handling (null arguments, illegal capacities, array store issues)</li>
 *   <li>Deprecated {@code plus} / {@code minus} methods</li>
 *   <li>Equality and hash‑code contracts</li>
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

    /*----------------------------------------------------
     * Constructor Tests
     *----------------------------------------------------*/

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
    public void testConstructorWithCollectionIsCaseInsensitive() {
        List<String> src = Arrays.asList("Apple", "apple", "APPLE");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src);
        // All three map to a single entry
        assertEquals(1, set.size());
        assertTrue(set.contains("Apple"));
        assertTrue(set.contains("aPPle"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new CaseInsensitiveSet<>(null);
    }

    @Test
    public void testConstructorWithBackingMapPreservesProvidedMap() {
        Map<String, Object> backing = new LinkedHashMap<>();
        List<String> src = Arrays.asList("A", "B");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src, backing);
        // Backing map should now contain the two entries
        assertEquals(2, backing.size());
        assertTrue(set.containsAll(src));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullBackingMapThrowsNPE() {
        List<String> src = Collections.singletonList("test");
        new CaseInsensitiveSet<>(src, null);
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
        set.add(1);
        assertEquals(1, set.size());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeInitialCapacityThrowsIAE() {
        new CaseInsensitiveSet<>(-5);
    }

    @Test
    public void testConstructorWithInitialCapacityAndLoadFactor() {
        CaseInsensitiveSet<Integer> set = new CaseInsensitiveSet<>(5, 0.75f);
        assertTrue(set.isEmpty());
        set.add(42);
        assertTrue(set.contains(42));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithNegativeLoadFactorThrowsIAE() {
        new CaseInsensitiveSet<>(5, -0.1f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConstructorWithZeroLoadFactorThrowsIAE() {
        new CaseInsensitiveSet<>(5, 0.0f);
    }

    /*----------------------------------------------------
     * Basic Set Operations
     *----------------------------------------------------*/

    @Test
    public void testAddAndContainsCaseInsensitive() {
        assertTrue(stringSet.add("Hello"));
        assertFalse(stringSet.add("HELLO")); // duplicate (case‑insensitive)
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("hello"));
        assertTrue(stringSet.contains("HeLLo"));
    }

    @Test
    public void testAddNullElementThrowsNPE() {
        thrown.expect(NullPointerException.class);
        stringSet.add(null);
    }

    @Test
    public void testRemoveCaseInsensitive() {
        stringSet.add("World");
        assertTrue(stringSet.remove("world"));
        assertFalse(stringSet.contains("World"));
        assertEquals(0, stringSet.size());
    }

    @Test
    public void testRemoveNonExistingElementReturnsFalse() {
        assertFalse(stringSet.remove("nothing"));
    }

    @Test
    public void testMixedTypeSetBehaviour() {
        mixedSet.add("Apple");
        mixedSet.add(123);
        mixedSet.add("apple"); // duplicate string (case‑insensitive)
        assertEquals(2, mixedSet.size());
        assertTrue(mixedSet.contains("APPLE"));
        assertTrue(mixedSet.contains(123));
    }

    @Test
    public void testContainsAll() {
        List<String> list = Arrays.asList("One", "Two", "three");
        stringSet.addAll(list);
        assertTrue(stringSet.containsAll(list));

        // case‑insensitive check
        List<String> differentCase = Arrays.asList("ONE", "two", "THREE");
        assertTrue(stringSet.containsAll(differentCase));
    }

    @Test(expected = NullPointerException.class)
    public void testContainsAllWithNullCollectionThrowsNPE() {
        stringSet.containsAll(null);
    }

    @Test
    public void testAddAllReturnsTrueWhenSetChanged() {
        List<String> src = Arrays.asList("a", "b");
        assertTrue(stringSet.addAll(src));
        // Adding the same collection again should return false (no change)
        assertFalse(stringSet.addAll(src));
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllWithNullCollectionThrowsNPE() {
        stringSet.addAll(null);
    }

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        stringSet.addAll(Arrays.asList("A", "B", "C"));
        boolean changed = stringSet.retainAll(Arrays.asList("b", "c", "D"));
        assertTrue(changed);
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.containsAll(Arrays.asList("B", "c")));
        assertFalse(stringSet.contains("A"));
    }

    @Test(expected = NullPointerException.class)
    public void testRetainAllWithNullCollectionThrowsNPE() {
        stringSet.retainAll(null);
    }

    @Test
    public void testRemoveAllRemovesSpecifiedElements() {
        stringSet.addAll(Arrays.asList("X", "Y", "Z"));
        boolean changed = stringSet.removeAll(Arrays.asList("y", "nonexistent"));
        assertTrue(changed);
        assertEquals(2, stringSet.size());
        assertFalse(stringSet.contains("Y"));
        assertTrue(stringSet.containsAll(Arrays.asList("X", "Z")));
    }

    @Test(expected = NullPointerException.class)
    public void testRemoveAllWithNullCollectionThrowsNPE() {
        stringSet.removeAll(null);
    }

    @Test
    public void testClearEmptiesTheSet() {
        stringSet.addAll(Arrays.asList("a", "b"));
        assertFalse(stringSet.isEmpty());
        stringSet.clear();
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    /*----------------------------------------------------
     * Iterator & toArray Tests
     *----------------------------------------------------*/

    @Test
    public void testIteratorOrderPreservesInsertionOrder() {
        List<String> elements = Arrays.asList("first", "second", "third");
        stringSet.addAll(elements);
        Iterator<String> it = stringSet.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertEquals(elements, iterated);
    }

    @Test
    public void testToArrayReturnsAllElements() {
        stringSet.addAll(Arrays.asList("x", "y"));
        Object[] arr = stringSet.toArray();
        assertArrayEquals(new Object[]{"x", "y"}, arr);
    }

    @Test
    public void testToArrayWithTypedArrayExactSize() {
        stringSet.addAll(Arrays.asList("p", "q"));
        String[] target = new String[2];
        String[] result = stringSet.toArray(target);
        assertSame(target, result);
        assertArrayEquals(new String[]{"p", "q"}, result);
    }

    @Test
    public void testToArrayWithTypedArrayLargerThanNeeded() {
        stringSet.addAll(Arrays.asList("m"));
        String[] target = new String[3];
        String[] result = stringSet.toArray(target);
        assertSame(target, result);
        assertEquals("m", result[0]);
        assertNull(result[1]); // per java.util.Collection contract
    }

    @Test(expected = NullPointerException.class)
    public void testToArrayWithNullArrayThrowsNPE() {
        stringSet.toArray((String[]) null);
    }

    @Test(expected = ArrayStoreException.class)
    public void testToArrayWithIncompatibleArrayThrowsArrayStoreException() {
        stringSet.add("test");
        // Attempt to store a String into an Integer[] array
        Integer[] badArray = new Integer[1];
        stringSet.toArray(badArray);
    }

    /*----------------------------------------------------
     * Equality & HashCode Tests
     *----------------------------------------------------*/

    @Test
    public void testEqualsAndHashCodeWithSameElements() {
        CaseInsensitiveSet<String> set1 = new CaseInsensitiveSet<>();
        CaseInsensitiveSet<String> set2 = new CaseInsensitiveSet<>();

        set1.addAll(Arrays.asList("Alpha", "beta"));
        set2.addAll(Arrays.asList("ALPHA", "BETA"));

        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());
    }

    @Test
    public void testEqualsWithDifferentTypes() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>();
        set.add("one");
        assertFalse(set.equals("not a set"));
    }

    @Test
    public void testEqualsWithSelf() {
        assertTrue(stringSet.equals(stringSet));
    }

    /*----------------------------------------------------
     * Deprecated plus/minus Methods
     *----------------------------------------------------*/

    @Test
    public void testDeprecatedPlusWithIterable() {
        List<String> toAdd = Arrays.asList("a", "b");
        @SuppressWarnings("deprecation")
        Set<String> result = stringSet.plus(toAdd);
        assertSame(stringSet, result);
        assertTrue(stringSet.containsAll(toAdd));
    }

    @Test
    public void testDeprecatedPlusWithObject() {
        @SuppressWarnings("deprecation")
        Set<String> result = stringSet.plus("single");
        assertSame(stringSet, result);
        assertTrue(stringSet.contains("single"));
    }

    @Test
    public void testDeprecatedMinusWithIterable() {
        stringSet.addAll(Arrays.asList("x", "y", "z"));
        List<String> toRemove = Arrays.asList("Y", "nonexistent");
        @SuppressWarnings("deprecation")
        Set<String> result = stringSet.minus(toRemove);
        assertSame(stringSet, result);
        assertFalse(stringSet.contains("y"));
        assertTrue(stringSet.contains("x"));
    }

    @Test
    public void testDeprecatedMinusWithSingleElement() {
        stringSet.addAll(Arrays.asList("foo", "bar"));
        @SuppressWarnings("deprecation")
        Set<String> result = stringSet.minus("FOO");
        assertSame(stringSet, result);
        assertFalse(stringSet.contains("foo"));
        assertTrue(stringSet.contains("bar"));
    }

    /*----------------------------------------------------
     * toString Test
     *----------------------------------------------------*/

    @Test
    public void testToStringPreservesOriginalCaseAndOrder() {
        stringSet.addAll(Arrays.asList("First", "second", "THIRD"));
        String s = stringSet.toString();
        // Expected format: [First, second, THIRD]
        assertEquals("[First, second, THIRD]", s);
    }
}
