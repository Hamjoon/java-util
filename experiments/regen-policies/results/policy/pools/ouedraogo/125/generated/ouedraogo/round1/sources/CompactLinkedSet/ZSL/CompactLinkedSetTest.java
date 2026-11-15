package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.*;

public class CompactLinkedSetTest {

    private CompactLinkedSet<String> set;

    @Before
    public void setUp() {
        set = new CompactLinkedSet<>();
    }

    /*** Basic Set contract ***/

    @Test
    public void testAddAndContains() {
        assertTrue(set.add("Apple"));
        assertTrue(set.contains("Apple"));
        // case‑insensitive check
        assertTrue(set.contains("apple"));
        // adding the same element (different case) should return false
        assertFalse(set.add("apple"));
        assertEquals(1, set.size());
    }

    @Test
    public void testRemove() {
        set.add("Banana");
        assertTrue(set.remove("BANANA")); // case‑insensitive removal
        assertFalse(set.contains("banana"));
        assertEquals(0, set.size());
    }

    @Test
    public void testIsEmptyAndClear() {
        assertTrue(set.isEmpty());
        set.add("Cherry");
        assertFalse(set.isEmpty());
        set.clear();
        assertTrue(set.isEmpty());
        assertEquals(0, set.size());
    }

    /*** Collection based operations ***/

    @Test
    public void testAddAllAndSize() {
        Collection<String> toAdd = Arrays.asList("Dog", "cat", "dog", "CAT");
        assertTrue(set.addAll(toAdd));
        // "Dog"/"dog" and "cat"/"CAT" are duplicates because of case‑insensitivity
        assertEquals(2, set.size());
        assertTrue(set.contains("DOG"));
        assertTrue(set.contains("Cat"));
    }

    @Test(expected = NullPointerException.class)
    public void testConstructorWithNullCollectionThrows() {
        new CompactLinkedSet<String>((Collection<String>) null);
    }

    @Test
    public void testConstructorWithCollectionPopulatesSet() {
        Collection<String> source = Arrays.asList("Eagle", "eagle", "Falcon");
        CompactLinkedSet<String> constructed = new CompactLinkedSet<>(source);
        // "Eagle"/"eagle" collapse to a single entry
        assertEquals(2, constructed.size());
        assertTrue(constructed.contains("EAGLE"));
        assertTrue(constructed.contains("falcon"));
    }

    @Test
    public void testContainsAll() {
        set.addAll(Arrays.asList("Giraffe", "Hippo"));
        assertTrue(set.containsAll(Arrays.asList("giraffe", "HIPPO")));
        assertFalse(set.containsAll(Arrays.asList("giraffe", "lion")));
    }

    @Test
    public void testRetainAll() {
        set.addAll(Arrays.asList("Iguana", "Jaguar", "Koala"));
        // retain only "iguana" and "koala" (case‑insensitive)
        assertTrue(set.retainAll(Arrays.asList("IGUANA", "koala")));
        assertEquals(2, set.size());
        assertTrue(set.contains("iguana"));
        assertTrue(set.contains("KOALA"));
        assertFalse(set.contains("jaguar"));
    }

    @Test
    public void testRemoveAll() {
        set.addAll(Arrays.asList("Llama", "Mongoose", "Newt"));
        assertTrue(set.removeAll(Arrays.asList("llama", "NEWt")));
        assertEquals(1, set.size());
        assertTrue(set.contains("mongoose"));
        assertFalse(set.contains("llama"));
        assertFalse(set.contains("newt"));
    }

    /*** Iterator order (insertion order) ***/

    @Test
    public void testIteratorPreservesInsertionOrder() {
        List<String> items = Arrays.asList("alpha", "bravo", "charlie");
        set.addAll(items);
        Iterator<String> it = set.iterator();
        for (String expected : items) {
            assertTrue(it.hasNext());
            assertEquals(expected, it.next());
        }
        assertFalse(it.hasNext());
    }

    /*** toArray variants ***/

    @Test
    public void testToArrayObjectArray() {
        set.addAll(Arrays.asList("Delta", "Echo"));
        Object[] array = set.toArray();
        assertArrayEquals(new Object[]{"Delta", "Echo"}, array);
    }

    @Test
    public void testToArrayTypedArray() {
        set.addAll(Arrays.asList("Foxtrot", "Golf"));
        String[] array = set.toArray(new String[0]);
        assertArrayEquals(new String[]{"Foxtrot", "Golf"}, array);
    }

    /*** Equality and hash code ***/

    @Test
    public void testEqualsAndHashCode() {
        CompactLinkedSet<String> other = new CompactLinkedSet<>();
        set.addAll(Arrays.asList("Hotel", "India"));
        other.addAll(Arrays.asList("hotel", "INDIA"));
        assertEquals(set, other);
        assertEquals(other, set);
        assertEquals(set.hashCode(), other.hashCode());

        // Different content should not be equal
        other.clear();
        other.add("Juliet");
        assertNotEquals(set, other);
    }

    /*** toString representation ***/

    @Test
    public void testToStringContainsElementsInOrder() {
        set.addAll(Arrays.asList("Kilo", "Lima"));
        String s = set.toString();
        // The exact format is not defined, but it must contain the elements in order
        assertTrue(s.startsWith("["));
        assertTrue(s.endsWith("]"));
        int first = s.indexOf("Kilo");
        int second = s.indexOf("Lima");
        assertTrue(first >= 0 && second > first);
    }

    /*** Adding duplicate elements (same case) does not change set ***/

    @Test
    public void testAddDuplicateSameCase() {
        assertTrue(set.add("Mike"));
        assertFalse(set.add("Mike")); // duplicate, same case
        assertEquals(1, set.size());
    }

    /*** Adding null element (if supported) ***/

    @Test
    public void testAddNullElement() {
        // The underlying implementation may allow null; we verify behavior
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
        // Adding another null should be considered a duplicate
        assertFalse(set.add(null));
        assertEquals(1, set.size());
        // Removal of null
        assertTrue(set.remove(null));
        assertFalse(set.contains(null));
        assertTrue(set.isEmpty());
    }
}
