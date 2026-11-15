package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors and methods (signature verification is implicit by compilation).</li>
 *   <li>Basic functional behavior.</li>
 *   <li>Edge‑case handling such as null values, case‑insensitive duplicate detection, mixed‑type collections,
 *       empty collections, and illegal arguments.</li>
 *   <li>Equality, hash‑code contract and deprecated convenience methods.</li>
 * </ul>
 */
public class CaseInsensitiveSetTest {

    private CaseInsensitiveSet<String> emptySet;
    private CaseInsensitiveSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CaseInsensitiveSet<>();
        populatedSet = new CaseInsensitiveSet<>(Arrays.asList("Apple", "banana", "Cherry"));
    }

    /* -------------------------------------------------
     *  Constructor tests
     * ------------------------------------------------- */

    @Test
    public void defaultConstructorCreatesEmptySet() {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void collectionConstructorCopiesElementsCaseInsensitively() {
        List<String> source = Arrays.asList("One", "TWO", "three", "ONE", "Two");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        // "One"/"ONE" and "TWO"/"Two" are considered the same element
        assertEquals(3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));
    }

    @Test(expected = NullPointerException.class)
    public void collectionConstructorThrowsNPEWhenCollectionIsNull() {
        new CaseInsensitiveSet<String>((Collection<String>) null);
    }

    @Test
    public void collectionAndBackingMapConstructorUsesProvidedMap() {
        Map<String, Object> backing = new TreeMap<>();
        List<String> src = Arrays.asList("Alpha", "beta");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(src, backing);
        assertEquals(2, set.size());
        assertSame(backing, getInternalMap(set));
    }

    @Test
    public void intCapacityConstructorCreatesEmptySet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
    }

    @Test
    public void intCapacityAndLoadFactorConstructorCreatesEmptySet() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10, 0.75f);
        assertTrue(set.isEmpty());
    }

    /* -------------------------------------------------
     *  Basic Set operations
     * ------------------------------------------------- */

    @Test
    public void addIncreasesSizeAndIsCaseInsensitive() {
        assertTrue(emptySet.add("Hello"));
        assertEquals(1, emptySet.size());
        // Adding same word with different case does not increase size
        assertFalse(emptySet.add("hello"));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void addNullIsAllowedAndCountsAsSingleElement() {
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertFalse(emptySet.add(null)); // duplicate null
        assertEquals(1, emptySet.size());
    }

    @Test
    public void removeDecreasesSizeAndIsCaseInsensitive() {
        populatedSet.add("DURiAn");
        assertTrue(populatedSet.remove("durian"));
        assertFalse(populatedSet.contains("DURiAn"));
        assertEquals(3, populatedSet.size()); // original 3 + added 1 - removed 1
    }

    @Test
    public void removeNullWorks() {
        emptySet.add(null);
        assertTrue(emptySet.remove(null));
        assertFalse(emptySet.contains(null));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void containsIsCaseInsensitive() {
        assertTrue(populatedSet.contains("apple"));
        assertTrue(populatedSet.contains("BANANA"));
        assertTrue(populatedSet.contains("cherry"));
        assertFalse(populatedSet.contains("date"));
    }

    @Test
    public void iteratorReturnsOriginalCaseValues() {
        List<String> iterated = new ArrayList<>();
        for (String s : populatedSet) {
            iterated.add(s);
        }
        // The iterator must return the exact objects that were added
        assertTrue(iterated.containsAll(Arrays.asList("Apple", "banana", "Cherry")));
    }

    @Test
    public void toArrayReturnsAllElements() {
        Object[] arr = populatedSet.toArray();
        assertEquals(3, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("Apple", "banana", "Cherry")));
    }

    @Test
    public void toArrayTypedReturnsAllElementsAndRespectsArraySize() {
        String[] target = new String[5];
        String[] result = populatedSet.toArray(target);
        assertSame(target, result); // same array is returned when it is large enough
        assertEquals(5, result.length);
        // The first three entries are the set elements; the remaining are null per spec
        List<String> firstThree = Arrays.asList(result[0], result[1], result[2]);
        assertTrue(firstThree.containsAll(Arrays.asList("Apple", "banana", "Cherry")));
        assertNull(result[3]);
        assertNull(result[4]);
    }

    @Test
    public void addAllAddsAllElementsCaseInsensitively() {
        List<String> more = Arrays.asList("date", "ELDERBERRY", "apple");
        assertTrue(populatedSet.addAll(more));
        assertEquals(5, populatedSet.size()); // "apple" already present
        assertTrue(populatedSet.containsAll(Arrays.asList("date", "elderberry")));
    }

    @Test
    public void containsAllWorksWithMixedCaseAndNonStringElements() {
        CaseInsensitiveSet<Object> mixed = new CaseInsensitiveSet<>();
        mixed.addAll(Arrays.asList("One", 2, "THREE"));
        assertTrue(mixed.containsAll(Arrays.asList("one", 2, "three")));
        assertFalse(mixed.containsAll(Arrays.asList("one", 2, "four")));
    }

    @Test
    public void retainAllKeepsOnlySpecifiedElementsCaseInsensitively() {
        List<String> keep = Arrays.asList("BANANA", "cherry");
        assertTrue(populatedSet.retainAll(keep));
        assertEquals(2, populatedSet.size());
        assertTrue(populatedSet.contains("banana"));
        assertTrue(populatedSet.contains("Cherry"));
        assertFalse(populatedSet.contains("Apple"));
    }

    @Test
    public void removeAllRemovesSpecifiedElementsCaseInsensitively() {
        List<String> remove = Arrays.asList("APPLE", "banana");
        assertTrue(populatedSet.removeAll(remove));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Cherry"));
        assertFalse(populatedSet.contains("Apple"));
        assertFalse(populatedSet.contains("banana"));
    }

    @Test
    public void clearEmptiesSet() {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    /* -------------------------------------------------
     *  Equality & hashCode
     * ------------------------------------------------- */

    @Test
    public void equalsSameReference() {
        assertTrue(populatedSet.equals(populatedSet));
    }

    @Test
    public void equalsDifferentSetSameElementsCaseInsensitively() {
        Set<String> other = new HashSet<>(Arrays.asList("apple", "BANANA", "cherry"));
        assertTrue(populatedSet.equals(other));
    }

    @Test
    public void equalsFailsWhenOtherIsNotASet() {
        assertFalse(populatedSet.equals("not a set"));
    }

    @Test
    public void equalsFailsWhenSizesDiffer() {
        Set<String> other = new HashSet<>(Arrays.asList("apple", "banana"));
        assertFalse(populatedSet.equals(other));
    }

    @Test
    public void hashCodeIsConsistentWithEquals() {
        Set<String> other = new HashSet<>(Arrays.asList("APPLE", "Banana", "CHERRY"));
        assertTrue(populatedSet.equals(other));
        assertEquals(populatedSet.hashCode(), other.hashCode());
    }

    /* -------------------------------------------------
     *  Deprecated convenience methods
     * ------------------------------------------------- */

    @Test
    public void plusAddsSingleElement() {
        emptySet.plus("X");
        assertTrue(emptySet.contains("X"));
    }

    @Test
    public void plusIterableAddsAllElements() {
        emptySet.plus(Arrays.asList("a", "B", "c"));
        assertTrue(emptySet.containsAll(Arrays.asList("A", "b", "C")));
    }

    @Test
    public void minusRemovesSingleElement() {
        populatedSet.minus("APPLE");
        assertFalse(populatedSet.contains("Apple"));
    }

    @Test
    public void minusIterableRemovesAllSpecifiedElements() {
        populatedSet.minus(Arrays.asList("banana", "CHERRY"));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("Apple"));
    }

    /* -------------------------------------------------
     *  toString representation
     * ------------------------------------------------- */

    @Test
    public void toStringShowsElementsInBracketedCommaSeparatedForm() {
        String s = populatedSet.toString();
        // The exact order is unspecified, but the format must start with '[' and end with ']'
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        // All original case strings must appear somewhere inside
        assertTrue(s.contains("Apple"));
        assertTrue(s.contains("banana"));
        assertTrue(s.contains("Cherry"));
    }

    /* -------------------------------------------------
     *  Helper – reflectively obtain the backing map for verification
     * ------------------------------------------------- */

    @SuppressWarnings("unchecked")
    private static <K, V> Map<K, V> getInternalMap(CaseInsensitiveSet<K> set) {
        try {
            java.lang.reflect.Field f = CaseInsensitiveSet.class.getDeclaredField("map");
            f.setAccessible(true);
            return (Map<K, V>) f.get(set);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
