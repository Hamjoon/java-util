package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit‑4 test suite for {@link CompactCIHashSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (empty and from a collection)</li>
 *   <li>Case‑insensitive behaviour for {@code add}, {@code contains},
 *       {@code remove} and bulk operations.</li>
 *   <li>Edge cases such as {@code null} arguments and duplicate values
 *       differing only by case.</li>
 *   <li>Utility methods specific to this subclass:
 *       {@code isCaseInsensitive()} and {@code getNewSet()}.</li>
 *   <li>Standard {@link Set} contract aspects (size, iterator, toArray).</li>
 * </ul>
 */
public class CompactCIHashSetTest {

    private CompactCIHashSet<String> emptySet;

    @Before
    public void setUp() {
        emptySet = new CompactCIHashSet<>();
    }

    /*** Constructor tests ***/

    @Test
    public void testEmptyConstructorCreatesEmptySet() {
        assertTrue("A newly constructed set must be empty", emptySet.isEmpty());
        assertEquals("Size of a newly constructed set must be 0", 0, emptySet.size());
    }

    @Test
    public void testCollectionConstructorCopiesElementsCaseInsensitively() {
        List<String> source = Arrays.asList("One", "Two", "ONE", "three");
        CompactCIHashSet<String> set = new CompactCIHashSet<>(source);

        // "One" and "ONE" collapse into a single entry
        assertEquals("Duplicate values differing only by case must be collapsed", 3, set.size());
        assertTrue(set.contains("one"));
        assertTrue(set.contains("TWO"));
        assertTrue(set.contains("Three"));
    }

    @Test(expected = NullPointerException.class)
    public void testCollectionConstructorRejectsNullArgument() {
        new CompactCIHashSet<String>(null);
    }

    /*** Behavioural tests – case‑insensitivity ***/

    @Test
    public void testAddIsCaseInsensitive() {
        assertTrue(emptySet.add("Apple"));
        // Adding same word with different case must not increase size
        assertFalse("Adding a duplicate (case‑insensitive) must return false", emptySet.add("apple"));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("APPLE"));
    }

    @Test
    public void testContainsIsCaseInsensitive() {
        emptySet.add("Banana");
        assertTrue(emptySet.contains("banana"));
        assertTrue(emptySet.contains("BANANA"));
        assertFalse(emptySet.contains("bana")); // not a full match
    }

    @Test
    public void testRemoveIsCaseInsensitive() {
        emptySet.add("Cherry");
        assertTrue(emptySet.remove("CHERRY"));
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testAddAllRespectsCaseInsensitivity() {
        List<String> items = Arrays.asList("Dog", "cat", "DOG", "Cat", "Elephant");
        assertTrue(emptySet.addAll(items));
        // "Dog"/"DOG" and "cat"/"Cat" collapse
        assertEquals(3, emptySet.size());
        assertTrue(emptySet.containsAll(Arrays.asList("dog", "CAT", "elephant")));
    }

    @Test(expected = NullPointerException.class)
    public void testAddAllRejectsNullCollection() {
        emptySet.addAll(null);
    }

    @Test
    public void testRetainAllIsCaseInsensitive() {
        emptySet.addAll(Arrays.asList("Fox", "fox", "Giraffe"));
        // Set now contains "Fox" and "Giraffe"
        assertTrue(emptySet.retainAll(Arrays.asList("FOX")));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("fox"));
        assertFalse(emptySet.contains("giraffe"));
    }

    @Test
    public void testRemoveAllIsCaseInsensitive() {
        emptySet.addAll(Arrays.asList("Hippo", "hippo", "Iguana"));
        // Should remove both "Hippo"/"hippo"
        assertTrue(emptySet.removeAll(Arrays.asList("HIPPO")));
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("iguana"));
    }

    /*** Standard Set contract tests ***/

    @Test
    public void testIteratorReturnsElementsInInsertionOrderPreservingOriginalCase() {
        emptySet.addAll(Arrays.asList("Jack", "Jill", "jack"));
        Iterator<String> it = emptySet.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        // "jack" is duplicate, so only two distinct elements should appear
        assertEquals(2, iterated.size());
        // The original case of the first occurrence must be preserved
        assertTrue(iterated.contains("Jack"));
        assertTrue(iterated.contains("Jill"));
    }

    @Test
    public void testToArrayContainsAllElements() {
        emptySet.addAll(Arrays.asList("Kite", "Lemon"));
        Object[] array = emptySet.toArray();
        assertEquals(2, array.length);
        List<Object> list = Arrays.asList(array);
        assertTrue(list.contains("Kite"));
        assertTrue(list.contains("Lemon"));
    }

    @Test
    public void testToArrayWithTypedArray() {
        emptySet.addAll(Arrays.asList("Mango", "Nectarine"));
        String[] typed = emptySet.toArray(new String[0]);
        assertEquals(2, typed.length);
        List<String> list = Arrays.asList(typed);
        assertTrue(list.contains("Mango"));
        assertTrue(list.contains("Nectarine"));
    }

    /*** Subclass‑specific methods ***/

    @Test
    public void testIsCaseInsensitiveReturnsTrue() {
        assertTrue("CompactCIHashSet must report case‑insensitive behaviour", emptySet.isCaseInsensitive());
    }

    @Test
    public void testGetNewSetReturnsNull() {
        // The overridden method deliberately returns null; we verify that contract.
        assertNull("getNewSet() is overridden to return null", emptySet.getNewSet());
    }

    /*** Miscellaneous edge cases ***/

    @Test
    public void testAddNullElement() {
        // CompactSet (the superclass) allows null values; we verify the same here.
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertEquals(1, emptySet.size());

        // Adding null again must not increase size
        assertFalse(emptySet.add(null));
        assertEquals(1, emptySet.size());
    }

    @Test
    public void testClearEmptiesTheSet() {
        emptySet.addAll(Arrays.asList("Orange", "Papaya"));
        assertFalse(emptySet.isEmpty());
        emptySet.clear();
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testEqualsAndHashCodeConsistency() {
        CompactCIHashSet<String> setA = new CompactCIHashSet<>();
        CompactCIHashSet<String> setB = new CompactCIHashSet<>();

        setA.addAll(Arrays.asList("Quince", "Raspberry"));
        setB.addAll(Arrays.asList("quince", "raspberry"));

        assertEquals("Sets with same case‑insensitive elements must be equal", setA, setB);
        assertEquals("Equal sets must have identical hash codes", setA.hashCode(), setB.hashCode());
    }

    @Test
    public void testToStringContainsAllElements() {
        emptySet.addAll(Arrays.asList("Strawberry", "Tomato"));
        String s = emptySet.toString();
        assertTrue(s.contains("Strawberry"));
        assertTrue(s.contains("Tomato"));
    }
}
