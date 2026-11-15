package com.cedarsoftware.util;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.junit.Test;

/**
 * Comprehensive JUnit 4 tests for {@link CompactCIHashSet}.
 * These tests verify:
 * <ul>
 *   <li>Correct case‑insensitive behavior for add, contains, and remove.</li>
 *   <li>Construction via the default constructor and the collection constructor.</li>
 *   <li>Standard {@link Set} contract methods (size, isEmpty, clear, iterator, toArray, equals, hashCode, toString).</li>
 *   <li>Protected methods accessed via reflection (isCaseInsensitive, getNewSet).</li>
 *   <li>Exception handling (null collection argument).</li>
 * </ul>
 */
public class CompactCIHashSetTest {

    /** Helper to invoke a protected method via reflection. */
    private Object invokeProtected(Object target, String methodName, Class<?>... paramTypes) throws Exception {
        Method m = target.getClass().getDeclaredMethod(methodName, paramTypes);
        m.setAccessible(true);
        return m.invoke(target);
    }

    @Test
    public void testDefaultConstructorAndCaseInsensitiveAddContains() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();

        // Initially empty
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());

        // Add a value with mixed case
        assertTrue(set.add("Apple"));
        assertEquals(1, set.size());

        // Adding the same value with different case should not increase size
        assertFalse(set.add("apple"));
        assertEquals(1, set.size());

        // contains should be case‑insensitive
        assertTrue(set.contains("APPLE"));
        assertTrue(set.contains("apple"));
        assertTrue(set.contains("ApPlE"));

        // The set's toString should contain the original stored form (implementation may keep first case)
        String stringRepr = set.toString();
        assertTrue(stringRepr.contains("Apple"));
    }

    @Test
    public void testConstructorWithCollection() {
        List<String> source = Arrays.asList("One", "TWO", "three", "ONE");
        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);

        // "One" and "ONE" are considered the same element (case‑insensitive)
        assertEquals(3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));
        assertTrue(set.contains("THREE"));

        // Verify that all original elements are present (order not important)
        assertTrue(set.containsAll(source));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrowsNPE() {
        new CompactCIHashSet<String>((Collection<String>) null);
    }

    @Test
    public void testRemoveCaseInsensitive() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.add("Banana");

        // remove using different case
        assertTrue(set.remove("BANANA"));
        assertFalse(set.contains("banana"));
        assertEquals(0, set.size());

        // removing a non‑existent element should return false
        assertFalse(set.remove("banana"));
    }

    @Test
    public void testClearAndIsEmpty() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(Arrays.asList("a", "b", "c"));
        assertFalse(set.isEmpty());
        assertEquals(3, set.size());

        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    @Test
    public void testIteratorAndToArray() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(Arrays.asList("x", "Y", "z"));

        // Iterator should traverse all elements
        List<String> iterated = new ArrayList<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            iterated.add(it.next());
        }
        assertEquals(3, iterated.size());
        assertTrue(iterated.containsAll(Arrays.asList("x", "Y", "z")));

        // toArray (Object[])
        Object[] arr = set.toArray();
        assertEquals(3, arr.length);
        assertTrue(Arrays.asList(arr).containsAll(Arrays.asList("x", "Y", "z")));

        // toArray(T[])
        String[] typed = set.toArray(new String[0]);
        assertEquals(3, typed.length);
        assertTrue(Arrays.asList(typed).containsAll(Arrays.asList("x", "Y", "z")));
    }

    @Test
    public void testEqualsAndHashCode() {
        CompactCIHashSet<String> set1 = new CompactCIHashSet<>();
        CompactCIHashSet<String> set2 = new CompactCIHashSet<>();

        set1.addAll(Arrays.asList("Alpha", "beta"));
        set2.addAll(Arrays.asList("ALPHA", "BETA"));

        assertEquals(set1, set2);
        assertEquals(set1.hashCode(), set2.hashCode());

        // Different content
        set2.add("gamma");
        assertNotEquals(set1, set2);
    }

    @Test
    public void testToStringContainsElements() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        set.addAll(Arrays.asList("dog", "cat"));
        String s = set.toString();

        // The default Set toString includes elements surrounded by brackets and separated by commas
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        assertTrue(s.contains("dog"));
        assertTrue(s.contains("cat"));
    }

    @Test
    public void testAddAllAndContainsAllCaseInsensitive() {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();
        List<String> toAdd = Arrays.asList("One", "ONE", "two", "TWO");
        set.addAll(toAdd);

        // Duplicates (case‑insensitive) should be ignored
        assertEquals(2, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("two"));

        // containsAll should succeed regardless of case
        assertTrue(set.containsAll(Arrays.asList("ONE", "two")));
    }

    @Test
    public void testProtectedMethodsViaReflection() throws Exception {
        CompactCIHashSet<String> set = new CompactCIHashSet<>();

        // isCaseInsensitive() is protected; ensure it returns true
        Boolean caseInsensitive = (Boolean) invokeProtected(set, "isCaseInsensitive");
        assertTrue(caseInsensitive);

        // getNewSet() is deprecated and should return null
        Object newSet = invokeProtected(set, "getNewSet");
        assertNull(newSet);
    }
}
