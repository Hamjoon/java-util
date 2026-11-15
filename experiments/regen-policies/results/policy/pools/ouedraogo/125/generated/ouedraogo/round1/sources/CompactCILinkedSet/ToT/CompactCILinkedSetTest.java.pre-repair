package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 test suite for {@link CompactCILinkedSet}.
 *
 * The tests cover:
 * <ul>
 *   <li>Construction (default and collection‑based)</li>
 *   <li>Case‑insensitive behaviour</li>
 *   <li>Insertion‑order preservation (linked behaviour)</li>
 *   <li>Standard {@link Set} operations (add, addAll, remove, retainAll, clear, iterator)</li>
 *   <li>Edge cases (null elements, duplicate elements with different case, null collection argument)</li>
 *   <li>Deprecated API contract (isCaseInsensitive, getNewSet)</li>
 * </ul>
 */
public class CompactCILinkedSetTest {

    private CompactCILinkedSet<String> emptySet;
    private CompactCILinkedSet<String> populatedSet;

    @Before
    public void setUp() {
        emptySet = new CompactCILinkedSet<>();
        populatedSet = new CompactCILinkedSet<>(Arrays.asList("Apple", "Banana", "APPLE"));
        // After construction, "Apple" and "APPLE" should be considered the same element.
    }

    /*** -------------------- Constructor Tests -------------------- ***/

    @Test
    public void defaultConstructorCreatesEmptySet() {
        assertTrue("Default constructor should create an empty set", emptySet.isEmpty());
        assertEquals("Size must be zero", 0, emptySet.size());
    }

    @Test
    public void collectionConstructorDeduplicatesCaseInsensitive() {
        // The source collection has three entries, two of which differ only by case.
        assertEquals("Duplicate values differing only by case must be collapsed", 2, populatedSet.size());
        assertTrue("Set must contain 'Apple' (any case)", populatedSet.contains("Apple"));
        assertTrue("Set must contain 'APPLE' (any case)", populatedSet.contains("APPLE"));
        assertTrue("Set must contain 'Banana'", populatedSet.contains("Banana"));
    }

    @Test(expected = NullPointerException.class)
    public void collectionConstructorRejectsNullArgument() {
        new CompactCILinkedSet<String>(null);
    }

    /*** -------------------- Behavioural Tests -------------------- ***/

    @Test
    public void isCaseInsensitiveReturnsTrue() {
        assertTrue("isCaseInsensitive() must always return true for this class", emptySet.isCaseInsensitive());
    }

    @Test
    public void getNewSetIsDeprecatedAndReturnsNull() {
        // The method is protected; we can reach it via a subclass defined inside the test.
        class Accessor extends CompactCILinkedSet<String> {
            Set<String> callGetNewSet() {
                return super.getNewSet();
            }
        }
        Accessor accessor = new Accessor();
        assertNull("Deprecated getNewSet() must return null", accessor.callGetNewSet());
    }

    @Test
    public void addRespectsCaseInsensitivity() {
        assertTrue(emptySet.add("Orange"));
        assertFalse("Adding the same value with different case must not increase size", emptySet.add("ORANGE"));
        assertEquals("Size must be 1 after attempting to add duplicate (different case)", 1, emptySet.size());
        assertTrue("Set must contain value regardless of case", emptySet.contains("orange"));
    }

    @Test
    public void addNullElementIsAllowed() {
        assertTrue("First addition of null should succeed", emptySet.add(null));
        assertFalse("Second addition of null should be rejected as duplicate", emptySet.add(null));
        assertEquals("Size must be 1 after adding null twice", 1, emptySet.size());
        assertTrue("Set must report containing null", emptySet.contains(null));
    }

    @Test
    public void addAllCollapsesCaseInsensitiveDuplicates() {
        List<String> toAdd = Arrays.asList("Pear", "PEAR", "peAr", "Grape");
        assertTrue("addAll should report changes", emptySet.addAll(toAdd));
        assertEquals("Only two distinct values (case‑insensitive) should be stored", 2, emptySet.size());
        assertTrue(emptySet.contains("pear"));
        assertTrue(emptySet.contains("GRAPE"));
    }

    @Test
    public void iteratorPreservesInsertionOrder() {
        List<String> insertionOrder = Arrays.asList("first", "Second", "THIRD");
        CompactCILinkedSet<String> orderedSet = new CompactCILinkedSet<>(insertionOrder);
        Iterator<String> it = orderedSet.iterator();

        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) {
            iterated.add(it.next());
        }

        // Because of case‑insensitivity, "Second" and "SECOND" would collapse,
        // but our list has unique values, so order must be identical.
        assertEquals("Iterator must preserve insertion order", insertionOrder, iterated);
    }

    @Test
    public void retainAllKeepsOnlySpecifiedElementsCaseInsensitively() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(Arrays.asList("Alpha", "Beta", "Gamma"));
        Collection<String> toRetain = Arrays.asList("ALPHA", "delta");
        assertTrue(set.retainAll(toRetain));
        assertEquals("Only 'Alpha' (case‑insensitive) should remain", 1, set.size());
        assertTrue(set.contains("alpha"));
        assertFalse(set.contains("Beta"));
    }

    @Test
    public void removeAllRemovesElementsCaseInsensitively() {
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>(Arrays.asList("One", "Two", "Three"));
        Collection<String> toRemove = Arrays.asList("ONE", "four");
        assertTrue(set.removeAll(toRemove));
        assertEquals("Only 'Two' and 'Three' should remain", 2, set.size());
        assertFalse(set.contains("one"));
        assertTrue(set.contains("Two"));
        assertTrue(set.contains("Three"));
    }

    @Test
    public void clearEmptiesTheSet() {
        populatedSet.clear();
        assertTrue("After clear(), set must be empty", populatedSet.isEmpty());
        assertEquals("Size must be zero after clear()", 0, populatedSet.size());
    }

    @Test
    public void toArrayReturnsAllElementsInInsertionOrder() {
        Object[] arr = populatedSet.toArray();
        // Expected order: "Apple" (first occurrence) then "Banana"
        assertArrayEquals("toArray must reflect insertion order", new Object[]{"Apple", "Banana"}, arr);
    }

    @Test
    public void toArrayWithTypedArrayWorks() {
        String[] a = new String[0];
        String[] result = populatedSet.toArray(a);
        assertArrayEquals("Typed toArray must contain the same elements in order", new String[]{"Apple", "Banana"}, result);
    }

    @Test
    public void equalsAndHashCodeContract() {
        CompactCILinkedSet<String> s1 = new CompactCILinkedSet<>(Arrays.asList("X", "Y"));
        CompactCILinkedSet<String> s2 = new CompactCILinkedSet<>(Arrays.asList("x", "y"));
        assertEquals("Sets with same elements (case‑insensitive) must be equal", s1, s2);
        assertEquals("Equal sets must have identical hash codes", s1.hashCode(), s2.hashCode());
    }

    @Test
    public void toStringProvidesReadableRepresentation() {
        String str = populatedSet.toString();
        // The exact format is implementation dependent; we only assert that it contains the elements.
        assertTrue("String representation must contain 'Apple'", str.contains("Apple"));
        assertTrue("String representation must contain 'Banana'", str.contains("Banana"));
    }
}
