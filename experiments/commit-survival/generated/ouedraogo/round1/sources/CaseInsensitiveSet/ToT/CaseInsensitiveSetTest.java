package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CaseInsensitiveSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>All public constructors</li>
 *   <li>Standard {@link Set} operations (add, remove, contains, iterator, toArray, clear, etc.)</li>
 *   <li>Case‑insensitive behavior for {@link String} elements</li>
 *   <li>Interaction with non‑String elements</li>
 *   <li>Edge cases (nulls, empty collections, duplicate case variants)</li>
 *   <li>Deprecated convenience methods {@code plus} / {@code minus}</li>
 *   <li>Correct {@code equals} and {@code hashCode} contracts</li>
 *   <li>String representation via {@code toString}</li>
 * </ul>
 */
public class CaseInsensitiveSetTest {

    private CaseInsensitiveSet<String> stringSet;
    private CaseInsensitiveSet<Integer> intSet;

    @Before
    public void setUp() {
        stringSet = new CaseInsensitiveSet<>();
        intSet = new CaseInsensitiveSet<>();
    }

    /* -------------------------------------------------------
     * Constructor tests
     * ------------------------------------------------------- */

    @Test
    public void testDefaultConstructorCreatesEmptySet() {
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    @Test
    public void testConstructorWithCollection_SortedSet() {
        SortedSet<String> source = new TreeSet<>();
        source.addAll(Arrays.asList("One", "Two", "Three"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
    }

    @Test
    public void testConstructorWithCollection_ConcurrentSkipListSet() {
        ConcurrentSkipListSet<String> source = new ConcurrentSkipListSet<>();
        source.addAll(Arrays.asList("Alpha", "Beta"));
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(2, set.size());
        assertTrue(set.contains("alpha"));
        assertTrue(set.contains("BETA"));
    }

    @Test
    public void testConstructorWithCollection_GenericCollection() {
        List<String> source = Arrays.asList("A", "B", "C");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source);
        assertEquals(3, set.size());
        assertTrue(set.containsAll(source));
    }

    @Test
    public void testConstructorWithBackingMap() {
        Map<String, Object> backing = new HashMap<>();
        List<String> source = Arrays.asList("X", "Y");
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(source, backing);
        // The backing map should now contain the two entries
        assertEquals(2, backing.size());
        assertTrue(set.contains("x"));
        assertTrue(set.contains("Y"));
    }

    @Test
    public void testConstructorWithInitialCapacity() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(10);
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testConstructorWithCapacityAndLoadFactor() {
        CaseInsensitiveSet<String> set = new CaseInsensitiveSet<>(5, 0.75f);
        assertTrue(set.isEmpty());
    }

    /* -------------------------------------------------------
     * Basic add / remove / contains behavior
     * ------------------------------------------------------- */

    @Test
    public void testAddReturnsTrueWhenElementIsNew() {
        assertTrue(stringSet.add("Hello"));
        assertTrue(stringSet.contains("Hello"));
        assertEquals(1, stringSet.size());
    }

    @Test
    public void testAddReturnsFalseWhenElementAlreadyExists_IgnoringCase() {
        assertTrue(stringSet.add("World"));
        assertFalse(stringSet.add("world")); // same ignoring case
        assertEquals(1, stringSet.size());
    }

    @Test
    public void testAddNullElement() {
        assertTrue(stringSet.add(null));
        assertTrue(stringSet.contains(null));
        assertEquals(1, stringSet.size());
        // Adding null again should not change size
        assertFalse(stringSet.add(null));
        assertEquals(1, stringSet.size());
    }

    @Test
    public void testRemoveExistingElement() {
        stringSet.add("Foo");
        assertTrue(stringSet.remove("foo")); // case‑insensitive removal
        assertFalse(stringSet.contains("Foo"));
        assertEquals(0, stringSet.size());
    }

    @Test
    public void testRemoveNonExistingElementReturnsFalse() {
        stringSet.add("Bar");
        assertFalse(stringSet.remove("baz"));
        assertEquals(1, stringSet.size());
    }

    @Test
    public void testRemoveNullElement() {
        stringSet.add(null);
        assertTrue(stringSet.remove(null));
        assertFalse(stringSet.contains(null));
        assertEquals(0, stringSet.size());
    }

    /* -------------------------------------------------------
     * contains / containsAll
     * ------------------------------------------------------- */

    @Test
    public void testContainsWorksCaseInsensitive() {
        stringSet.addAll(Arrays.asList("Apple", "Banana"));
        assertTrue(stringSet.contains("apple"));
        assertTrue(stringSet.contains("BANANA"));
        assertFalse(stringSet.contains("Cherry"));
    }

    @Test
    public void testContainsAllWithMixedCaseAndNulls() {
        stringSet.addAll(Arrays.asList("One", null, "Two"));
        List<String> query = Arrays.asList("one", null, "TWO");
        assertTrue(stringSet.containsAll(query));
        query = Arrays.asList("one", "three");
        assertFalse(stringSet.containsAll(query));
    }

    /* -------------------------------------------------------
     * iterator and toArray
     * ------------------------------------------------------- */

    @Test
    public void testIteratorPreservesOriginalCase() {
        stringSet.addAll(Arrays.asList("CaseOne", "caseTwo"));
        Iterator<String> it = stringSet.iterator();
        Set<String> observed = new HashSet<>();
        while (it.hasNext()) {
            observed.add(it.next());
        }
        // The iterator returns the exact strings that were added
        assertTrue(observed.contains("CaseOne"));
        assertTrue(observed.contains("caseTwo"));
    }

    @Test
    public void testToArrayReturnsAllElements() {
        stringSet.addAll(Arrays.asList("A", "B", "C"));
        Object[] array = stringSet.toArray();
        assertEquals(3, array.length);
        assertTrue(Arrays.asList(array).containsAll(Arrays.asList("A", "B", "C")));
    }

    @Test
    public void testToArrayTyped() {
        stringSet.addAll(Arrays.asList("X", "Y"));
        String[] typed = stringSet.toArray(new String[0]);
        assertEquals(2, typed.length);
        assertTrue(Arrays.asList(typed).containsAll(Arrays.asList("X", "Y")));
    }

    /* -------------------------------------------------------
     * retainAll / removeAll
     * ------------------------------------------------------- */

    @Test
    public void testRetainAllKeepsOnlySpecifiedElements() {
        stringSet.addAll(Arrays.asList("keep1", "keep2", "discard"));
        boolean changed = stringSet.retainAll(Arrays.asList("KEEP1", "keep2"));
        assertTrue(changed);
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.contains("keep1"));
        assertTrue(stringSet.contains("keep2"));
        assertFalse(stringSet.contains("discard"));
    }

    @Test
    public void testRetainAllNoChangeWhenAllPresent() {
        stringSet.addAll(Arrays.asList("a", "b"));
        boolean changed = stringSet.retainAll(Arrays.asList("A", "B"));
        assertFalse(changed);
        assertEquals(2, stringSet.size());
    }

    @Test
    public void testRemoveAllDeletesSpecifiedElements() {
        stringSet.addAll(Arrays.asList("one", "two", "three"));
        boolean changed = stringSet.removeAll(Arrays.asList("ONE", "THREE"));
        assertTrue(changed);
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("two"));
        assertFalse(stringSet.contains("one"));
        assertFalse(stringSet.contains("three"));
    }

    @Test
    public void testRemoveAllNoChangeWhenNoMatch() {
        stringSet.addAll(Arrays.asList("alpha", "beta"));
        boolean changed = stringSet.removeAll(Collections.singleton("gamma"));
        assertFalse(changed);
        assertEquals(2, stringSet.size());
    }

    /* -------------------------------------------------------
     * clear, isEmpty, size
     * ------------------------------------------------------- */

    @Test
    public void testClearEmptiesSet() {
        stringSet.addAll(Arrays.asList("x", "y"));
        assertFalse(stringSet.isEmpty());
        stringSet.clear();
        assertTrue(stringSet.isEmpty());
        assertEquals(0, stringSet.size());
    }

    /* -------------------------------------------------------
     * equals and hashCode contracts
     * ------------------------------------------------------- */

    @Test
    public void testEqualsWithSameElementsDifferentCase() {
        CaseInsensitiveSet<String> other = new CaseInsensitiveSet<>();
        stringSet.addAll(Arrays.asList("Same", "Elements"));
        other.addAll(Arrays.asList("same", "ELEMENTS"));
        assertTrue(stringSet.equals(other));
        assertTrue(other.equals(stringSet));
    }

    @Test
    public void testEqualsWithDifferentSizes() {
        CaseInsensitiveSet<String> other = new CaseInsensitiveSet<>();
        stringSet.add("only");
        other.addAll(Arrays.asList("only", "extra"));
        assertFalse(stringSet.equals(other));
    }

    @Test
    public void testEqualsWithNonSetObject() {
        assertFalse(stringSet.equals("not a set"));
    }

    @Test
    public void testHashCodeIsCaseInsensitiveForStrings() {
        stringSet.addAll(Arrays.asList("AbC", "def"));
        int expected = StringUtilities.hashCodeIgnoreCase("AbC") + StringUtilities.hashCodeIgnoreCase("def");
        assertEquals(expected, stringSet.hashCode());
    }

    @Test
    public void testHashCodeIncludesNonStringElementsNormally() {
        int intVal = 42;
        intSet.add(intVal);
        int expected = Integer.valueOf(intVal).hashCode();
        assertEquals(expected, intSet.hashCode());
    }

    @Test
    public void testHashCodeIgnoresNulls() {
        stringSet.add(null);
        stringSet.add("NonNull");
        int expected = StringUtilities.hashCodeIgnoreCase("NonNull"); // null contributes 0
        assertEquals(expected, stringSet.hashCode());
    }

    /* -------------------------------------------------------
     * Deprecated plus / minus methods
     * ------------------------------------------------------- */

    @Test
    @SuppressWarnings("deprecation")
    public void testPlusIterableAddsAllElements() {
        List<String> toAdd = Arrays.asList("first", "second");
        stringSet.plus(toAdd);
        assertEquals(2, stringSet.size());
        assertTrue(stringSet.containsAll(toAdd));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testMinusIterableRemovesElements() {
        stringSet.addAll(Arrays.asList("keep", "remove"));
        List<String> toRemove = Collections.singletonList("REMOVE");
        stringSet.minus(toRemove);
        assertEquals(1, stringSet.size());
        assertTrue(stringSet.contains("keep"));
        assertFalse(stringSet.contains("remove"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testPlusSingleObjectAddsElement() {
        stringSet.plus((Object) "single");
        assertTrue(stringSet.contains("single"));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testMinusSingleObjectRemovesElement() {
        stringSet.add("gone");
        stringSet.minus("GONE");
        assertFalse(stringSet.contains("gone"));
    }

    /* -------------------------------------------------------
     * toString representation
     * ------------------------------------------------------- */

    @Test
    public void testToStringMatchesUnderlyingKeySet() {
        stringSet.addAll(Arrays.asList("A", "B"));
        String rep = stringSet.toString();
        // The representation should be exactly the one from the underlying keySet
        assertEquals(stringSet.iterator().next() + ", " + stringSet.iterator().next(), rep.replace("[", "").replace("]", "").replace(" ", ""));
        // Simpler sanity check: it must start with '[' and end with ']'
        assertTrue(rep.startsWith("["));
        assertTrue(rep.endsWith("]"));
    }
}
