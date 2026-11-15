package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Test;

/**
 * JUnit 4 test suite for {@link CompactLinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Both constructors (default and collection‑based)</li>
 *   <li>Overridden {@code isCaseInsensitive()} and deprecated {@code getNewSet()}</li>
 *   <li>Standard {@link Set} operations (add, contains, remove, clear, iteration order)</li>
 *   <li>Bulk operations (addAll, retainAll, removeAll, containsAll)</li>
 *   <li>Array conversion, equals, and hashCode</li>
 *   <li>Exception handling for a {@code null} collection argument</li>
 * </ul>
 */
public class CompactLinkedSetTest {

    /*** Constructor & basic Set API ******************************************/

    @Test
    public void testDefaultConstructorAndBasicOperations() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();

        // freshly created set is empty
        assertTrue("Set should be empty", set.isEmpty());
        assertEquals("Size should be 0", 0, set.size());

        // contract: overridden method reports case‑insensitivity
        assertTrue("isCaseInsensitive() must return true", set.isCaseInsensitive());

        // deprecated method should return null (as documented)
        assertNull("getNewSet() is deprecated and should return null", set.getNewSet());

        // add first element
        assertTrue("First add must return true", set.add("Apple"));
        assertEquals(1, set.size());
        assertTrue(set.contains("Apple"));
        assertTrue("Case‑insensitive contains must succeed", set.contains("APPLE"));

        // duplicate (different case) must be ignored
        assertFalse("Adding duplicate (different case) must return false", set.add("APPLE"));
        assertEquals(1, set.size());

        // add a second distinct element
        assertTrue(set.add("Banana"));
        assertEquals(2, set.size());

        // verify insertion order via iterator
        Iterator<String> it = set.iterator();
        assertEquals("Apple", it.next());
        assertEquals("Banana", it.next());
        try {
            it.next();
            fail("Iterator should be exhausted");
        } catch (NoSuchElementException ignored) {
        }

        // remove using different case
        assertTrue("Removal should be case‑insensitive", set.remove("apple"));
        assertEquals(1, set.size());
        assertFalse(set.contains("Apple"));

        // clear the set
        set.clear();
        assertTrue(set.isEmpty());
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> src = Arrays.asList("One", "Two", "ONE");
        CompactLinkedSet<String> set = new CompactLinkedSet<>(src);

        // "One" and "ONE" are considered the same element
        assertEquals("Duplicate (case‑insensitive) should be collapsed", 2, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));

        // verify insertion order – the first occurrence wins
        Iterator<String> it = set.iterator();
        assertEquals("One", it.next());   // from first element
        assertEquals("Two", it.next());   // second distinct element
        assertFalse(it.hasNext());
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrows() {
        new CompactLinkedSet<String>((Collection<String>) null);
    }

    /*** Bulk operations *******************************************************/

    @Test
    public void testAddAllRetainAllRemoveAll() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        assertTrue(set.addAll(Arrays.asList("A", "B", "C")));
        assertEquals(3, set.size());

        // retain only A and B (case‑insensitive)
        assertTrue(set.retainAll(Arrays.asList("a", "b")));
        assertEquals(2, set.size());
        assertTrue(set.contains("A"));
        assertTrue(set.contains("B"));
        assertFalse(set.contains("C"));

        // remove B and C (C already absent)
        assertTrue(set.removeAll(Arrays.asList("B", "c")));
        assertEquals(1, set.size());
        assertTrue(set.contains("A"));
        assertFalse(set.contains("B"));
    }

    @Test
    public void testContainsAllAndAddAllIdempotence() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        Collection<String> col = Arrays.asList("x", "y", "X");
        assertTrue(set.addAll(col));
        // "x" and "X" are the same, so only two distinct elements
        assertEquals(2, set.size());

        // containsAll should be true irrespective of case
        assertTrue(set.containsAll(Arrays.asList("X", "y")));
        assertTrue(set.containsAll(Arrays.asList("x", "Y")));

        // adding the same collection again must not change the set
        assertFalse(set.addAll(col));
        assertEquals(2, set.size());
    }

    /*** Array conversion, equals, hashCode *************************************/

    @Test
    public void testToArray() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.addAll(Arrays.asList("first", "second", "third"));

        Object[] arr = set.toArray();
        assertArrayEquals(new Object[]{"first", "second", "third"}, arr);

        String[] typed = set.toArray(new String[0]);
        assertArrayEquals(new String[]{"first", "second", "third"}, typed);
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedSet<String> set1 = new CompactLinkedSet<>(Arrays.asList("x", "y"));
        CompactLinkedSet<String> set2 = new CompactLinkedSet<>(Arrays.asList("Y", "X"));
        CompactLinkedSet<String> set3 = new CompactLinkedSet<>(Arrays.asList("x", "z"));

        assertEquals("Sets with same elements (case‑insensitive) must be equal", set1, set2);
        assertEquals("Equal sets must have same hashCode", set1.hashCode(), set2.hashCode());

        assertNotEquals("Different sets must not be equal", set1, set3);
    }

    /*** toString sanity check *************************************************/

    @Test
    public void testToStringFormat() {
        CompactLinkedSet<String> set = new CompactLinkedSet<>();
        set.addAll(Arrays.asList("alpha", "beta"));
        String s = set.toString();

        // The exact format is implementation‑dependent, but it must contain the elements
        assertTrue("toString should contain 'alpha'", s.contains("alpha"));
        assertTrue("toString should contain 'beta'", s.contains("beta"));
    }
}
