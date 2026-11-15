package com.cedarsoftware.util;

import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;

/**
 * JUnit 4 test suite for {@link CompactCILinkedSet}.
 *
 * The tests exercise:
 * <ul>
 *   <li>Both constructors (default and collection‑based)</li>
 *   <li>Case‑insensitive behavior of {@code contains}, {@code add}, {@code remove}</li>
 *   <li>Insertion‑order iteration (linked set semantics)</li>
 *   <li>All {@link Set} operations inherited from {@link CompactSet}</li>
 *   <li>Deprecated {@code getNewSet} returning {@code null}</li>
 *   <li>Correct handling of {@code null} collection argument</li>
 * </ul>
 *
 * The test class uses only the public API shown in the problem statement.
 */
public class CompactCILinkedSetTest
{
    private CompactCILinkedSet<String> emptySet;
    private CompactCILinkedSet<String> populatedSet;

    @Before
    public void setUp()
    {
        emptySet = new CompactCILinkedSet<>();
        populatedSet = new CompactCILinkedSet<>(Arrays.asList("Apple", "Banana", "Cherry"));
    }

    /* --------------------------------------------------------------------- */
    /* Constructor tests                                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void defaultConstructorCreatesEmptySet()
    {
        assertTrue(emptySet.isEmpty());
        assertEquals(0, emptySet.size());
    }

    @Test
    public void collectionConstructorCopiesElements()
    {
        assertEquals(3, populatedSet.size());
        assertTrue(populatedSet.contains("apple"));   // case‑insensitive
        assertTrue(populatedSet.contains("BANANA"));
        assertTrue(populatedSet.contains("ChErRy"));
    }

    @Test(expected = NullPointerException.class)
    public void collectionConstructorThrowsOnNull()
    {
        new CompactCILinkedSet<String>(null);
    }

    /* --------------------------------------------------------------------- */
    /* Core case‑insensitive behavior                                         */
    /* --------------------------------------------------------------------- */

    @Test
    public void isCaseInsensitiveReturnsTrue()
    {
        assertTrue(populatedSet.isCaseInsensitive());
    }

    @Test
    public void addIsCaseInsensitiveAndRejectsDuplicates()
    {
        assertTrue(emptySet.add("Orange"));
        assertFalse(emptySet.add("orange")); // duplicate, different case
        assertEquals(1, emptySet.size());
        assertTrue(emptySet.contains("ORANGE"));
    }

    @Test
    public void removeIsCaseInsensitive()
    {
        assertTrue(populatedSet.remove("bAnAnA"));
        assertEquals(2, populatedSet.size());
        assertFalse(populatedSet.contains("banana"));
    }

    @Test
    public void containsAllIsCaseInsensitive()
    {
        List<String> check = Arrays.asList("APPLE", "banana");
        assertTrue(populatedSet.containsAll(check));
    }

    @Test
    public void retainAllKeepsOnlySpecifiedElementsCaseInsensitively()
    {
        List<String> toRetain = Arrays.asList("APPLE", "grape");
        assertTrue(populatedSet.retainAll(toRetain));
        assertEquals(1, populatedSet.size());
        assertTrue(populatedSet.contains("apple"));
        assertFalse(populatedSet.contains("banana"));
    }

    @Test
    public void removeAllIsCaseInsensitive()
    {
        List<String> toRemove = Arrays.asList("CHERRY", "grape");
        assertTrue(populatedSet.removeAll(toRemove));
        assertEquals(2, populatedSet.size());
        assertFalse(populatedSet.contains("cherry"));
    }

    /* --------------------------------------------------------------------- */
    /* Insertion order (linked) semantics                                     */
    /* --------------------------------------------------------------------- */

    @Test
    public void iteratorPreservesInsertionOrder()
    {
        List<String> expected = Arrays.asList("Apple", "Banana", "Cherry");
        List<String> actual = new ArrayList<>();
        for (String s : populatedSet)
        {
            actual.add(s);
        }
        assertEquals(expected, actual);
    }

    @Test
    public void toArrayPreservesInsertionOrder()
    {
        Object[] array = populatedSet.toArray();
        assertArrayEquals(new Object[]{"Apple", "Banana", "Cherry"}, array);
    }

    @Test
    public void toArrayTypedPreservesInsertionOrder()
    {
        String[] array = populatedSet.toArray(new String[0]);
        assertArrayEquals(new String[]{"Apple", "Banana", "Cherry"}, array);
    }

    /* --------------------------------------------------------------------- */
    /* Miscellaneous Set contract tests                                      */
    /* --------------------------------------------------------------------- */

    @Test
    public void clearEmptiesSet()
    {
        populatedSet.clear();
        assertTrue(populatedSet.isEmpty());
        assertEquals(0, populatedSet.size());
    }

    @Test
    public void equalsAndHashCodeAreConsistent()
    {
        CompactCILinkedSet<String> another = new CompactCILinkedSet<>(Arrays.asList("apple", "BANANA", "Cherry"));
        assertEquals(populatedSet, another);
        assertEquals(populatedSet.hashCode(), another.hashCode());

        // Different order should still be equal because order is part of the contract for a linked set
        CompactCILinkedSet<String> differentOrder = new CompactCILinkedSet<>(Arrays.asList("Banana", "Apple", "Cherry"));
        assertNotEquals(populatedSet, differentOrder);
    }

    @Test
    public void toStringContainsElementsInInsertionOrder()
    {
        String s = populatedSet.toString();
        // The exact format is implementation dependent, but we can assert the order of the substrings.
        int idxApple = s.indexOf("Apple");
        int idxBanana = s.indexOf("Banana");
        int idxCherry = s.indexOf("Cherry");
        assertTrue(idxApple >= 0 && idxBanana >= 0 && idxCherry >= 0);
        assertTrue(idxApple < idxBanana);
        assertTrue(idxBanana < idxCherry);
    }

    @Test
    public void addAllWorksAndRespectsCaseInsensitivity()
    {
        List<String> more = Arrays.asList("date", "APPLE", "Elderberry");
        assertTrue(emptySet.addAll(more));
        assertEquals(3, emptySet.size()); // "APPLE" is duplicate of "Apple" after insertion
        assertTrue(emptySet.contains("date"));
        assertTrue(emptySet.contains("elderberry"));
        assertTrue(emptySet.contains("apple"));
    }

    /* --------------------------------------------------------------------- */
    /* Deprecated API test                                                    */
    /* --------------------------------------------------------------------- */

    @Test
    public void getNewSetReturnsNull()
    {
        // The method is protected; use a subclass that exposes it for testing.
        CompactCILinkedSet<String> set = new CompactCILinkedSet<>();
        Set<String> result = set.getNewSet(); // accessible because we are in the same package
        assertNull(result);
    }

    /* --------------------------------------------------------------------- */
    /* Edge cases                                                             */
    /* --------------------------------------------------------------------- */

    @Test
    public void addingNullElementIsAllowedIfUnderlying SetAllowsIt()
    {
        // CompactSet behaves like a normal HashSet regarding nulls; we verify the contract.
        assertTrue(emptySet.add(null));
        assertTrue(emptySet.contains(null));
        assertFalse(emptySet.add(null)); // duplicate null
        assertEquals(1, emptySet.size());
    }

    @Test
    public void containsNullWhenNotPresentReturnsFalse()
    {
        assertFalse(populatedSet.contains(null));
    }
}
