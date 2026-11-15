package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * Unit tests for {@link CompactCIHashSet}.
 *
 * These tests exercise:
 * <ul>
 *   <li>Both constructors (empty and collection‑based)</li>
 *   <li>Case‑insensitive add / contains / remove behavior</li>
 *   <li>Handling of {@code null} elements</li>
 *   <li>Standard {@link Set} contract methods (size, iterator, toArray, equals, hashCode)</li>
 *   <li>Protected API via reflection (isCaseInsensitive)</li>
 * </ul>
 */
public class CompactCIHashSetTest {

    /* ---------------------------------------------------------------------- */
    /* Helper methods                                                         */
    /* ---------------------------------------------------------------------- */

    /** Calls the protected {@code isCaseInsensitive()} method via reflection. */
    private static boolean invokeIsCaseInsensitive(CompactCIHashSet<?> set) throws Exception {
        Method m = CompactSet.class.getDeclaredMethod("isCaseInsensitive");
        m.setAccessible(true);
        return (Boolean) m.invoke(set);
    }

    /** Builds a collection containing the supplied elements. */
    @SafeVarargs
    private static <T> Collection<T> coll(T... items) {
        return Arrays.asList(items);
    }

    /* ---------------------------------------------------------------------- */
    /* Constructor tests                                                       */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEmptyConstructorCreatesEmptySet() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        assertTrue("New set should be empty", set.isEmpty());
        assertEquals("Size of new set must be 0", 0, set.size());
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNull() {
        new CompactCIHashSet<String>(null);
    }

    @Test
    public void testCollectionConstructorCopiesElementsCaseInsensitively() {
        List<String> source = Arrays.asList("Alpha", "beta", "ALPHA", "Beta", "gamma");
        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);

        // Expect only three distinct values because of case‑insensitivity
        assertEquals("Set should contain 3 unique case‑insensitive values", 3, set.size());

        // Verify that the original casing of the *first* occurrence is retained
        assertTrue(set.contains("Alpha"));
        assertTrue(set.contains("beta"));
        assertTrue(set.contains("gamma"));
        assertFalse(set.contains("ALPHA")); // same value but different case already stored
    }

    /* ---------------------------------------------------------------------- */
    /* Core Set behavior (add / contains / remove)                           */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testAddAndContainsAreCaseInsensitive() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();

        assertTrue("Adding \"Hello\" should succeed", set.add("Hello"));
        assertTrue("Set must contain \"hello\" (case‑insensitive)", set.contains("hello"));
        assertTrue("Set must contain \"HELLO\" (case‑insensitive)", set.contains("HELLO"));

        // Adding a duplicate with different case must return false and not increase size
        assertFalse("Adding duplicate \"HELLO\" should return false", set.add("HELLO"));
        assertEquals("Size must remain 1 after duplicate add", 1, set.size());
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.add("World");

        assertTrue("Removing \"world\" (different case) must succeed", set.remove("world"));
        assertTrue("Set must be empty after removal", set.isEmpty());

        // Removing again should return false
        assertFalse("Removing again should return false", set.remove("WORLD"));
    }

    @Test
    public void testNullElementHandling() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();

        // Adding null is permitted (behaviour mirrors java.util.HashSet)
        assertTrue("First null add must succeed", set.add(null));
        assertTrue("Set must contain null after add", set.contains(null));
        assertEquals("Size must be 1 after adding null", 1, set.size());

        // Adding null a second time must return false
        assertFalse("Second null add must return false", set.add(null));
        assertEquals("Size must stay 1 after duplicate null add", 1, set.size());

        // Removing null must succeed
        assertTrue("Removing null must succeed", set.remove(null));
        assertFalse("Set must not contain null after removal", set.contains(null));
        assertTrue("Set must be empty after removing null", set.isEmpty());
    }

    /* ---------------------------------------------------------------------- */
    /* Iterator / toArray contract                                            */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIteratorProvidesAllElements() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(Arrays.asList("One", "TWO", "three", "ONE", "two"));

        // Expected distinct case‑insensitive values: "One", "TWO", "three"
        assertEquals(3, set.size());

        Iterator<String> it = set.iterator();
        int count = 0;
        while (it.hasNext()) {
            String val = it.next();
            assertNotNull("Iterator must not return null elements", val);
            count++;
        }
        assertEquals("Iterator must traverse exactly size() elements", set.size(), count);
    }

    @Test
    public void testToArrayMatchesSetContents() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(coll("a", "B", "c", "A", "b"));

        Object[] array = set.toArray();
        assertEquals("Array length must equal set size", set.size(), array.length);
        // All elements must be present (order not guaranteed)
        for (Object o : array) {
            assertTrue("Array element must be a String", o instanceof String);
            assertTrue("Set must contain the element from the array", set.contains(o));
        }
    }

    /* ---------------------------------------------------------------------- */
    /* Equality and hashCode                                                  */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testEqualsAndHashCodeAreCaseInsensitive() {
        CompactCIHashSet<String> set1 = new CompactCIHashSet<>();
        CompactCIHashSet<String> set2 = new CompactCIHashSet<>();

        set1.addAll(coll("x", "Y", "z"));
        set2.addAll(coll("X", "y", "Z"));

        assertEquals("Sets with same values (different case) must be equal", set1, set2);
        assertEquals("Equal sets must have identical hash codes", set1.hashCode(), set2.hashCode());
    }

    @Test
    public void testEqualsWithDifferentTypesAndNull() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.add("test");

        assertNotEquals("Set must not be equal to null", null, set);
        assertNotEquals("Set must not be equal to an object of another class", "test", set);
    }

    /* ---------------------------------------------------------------------- */
    /* Protected API validation via reflection                                 */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testIsCaseInsensitiveReturnsTrue() throws Exception {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        assertTrue("isCaseInsensitive() must return true for CompactCIHashSet", invokeIsCaseInsensitive(set));
    }

    /* ---------------------------------------------------------------------- */
    /* Miscellaneous behavior checks                                          */
    /* ---------------------------------------------------------------------- */

    @Test
    public void testClearEmptiesSet() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(coll("alpha", "beta"));
        assertFalse("Set should not be empty before clear()", set.isEmpty());

        set.clear();
        assertTrue("Set must be empty after clear()", set.isEmpty());
        assertEquals("Size must be 0 after clear()", 0, set.size());
    }

    @Test
    public void testRetainAllKeepsCaseInsensitiveMatches() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(coll("Dog", "cat", "BIRD"));

        // Retain only elements that match (case‑insensitively) "dog" and "bird"
        boolean changed = set.retainAll(coll("dog", "BIRD"));
        assertTrue("retainAll should report that the set changed", changed);
        assertEquals(2, set.size());
        assertTrue(set.contains("Dog"));
        assertTrue(set.contains("BIRD"));
        assertFalse(set.contains("cat"));
    }

    @Test
    public void testRemoveAllRemovesCaseInsensitiveMatches() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(coll("one", "Two", "THREE"));

        boolean changed = set.removeAll(coll("ONE", "three"));
        assertTrue("removeAll should report that the set changed", changed);
        assertEquals(1, set.size());
        assertTrue(set.contains("Two"));
        assertFalse(set.contains("one"));
        assertFalse(set.contains("THREE"));
    }

    @Test(expected = NoSuchElementException.class)
    public void testIteratorThrowsWhenExhausted() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.add("solo");
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        it.next(); // first element
        assertFalse(it.hasNext());
        it.next(); // should throw NoSuchElementException
    }
}
