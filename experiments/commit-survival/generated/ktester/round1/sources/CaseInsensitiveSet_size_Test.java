package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;

class CaseInsensitiveSet_size_Test {

    private CaseInsensitiveSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor for a fresh set before each test.
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern consistent.
        set = null;
    }

    @Test
    void size_isZeroForNewSet() {
        assertEquals(0, set.size(), "A newly created CaseInsensitiveSet should have size 0.");
    }

    @Test
    void size_reflectsNumberOfUniqueElements() {
        assertTrue(set.add("Apple"));
        assertEquals(1, set.size());
        // Adding a different string increases size.
        assertTrue(set.add("Banana"));
        assertEquals(2, set.size());
        // Adding the same string with different case does NOT increase size.
        assertFalse(set.add("apple"));
        assertEquals(2, set.size(), "Case‑insensitive duplicate should not change the size.");
    }

    @Test
    void size_afterRemovingElements() {
        set.addAll(Arrays.asList("One", "Two", "Three"));
        assertEquals(3, set.size());
        assertTrue(set.remove("Two"));
        assertEquals(2, set.size());
        // Removing a non‑existent element does not affect size.
        assertFalse(set.remove("Four"));
        assertEquals(2, set.size());
    }

    @Test
    void size_withCollectionConstructor() {
        List<String> source = Arrays.asList("Alpha", "Beta", "ALPHA", "gamma");
        // "Alpha" and "ALPHA" are case‑insensitive duplicates.
        CaseInsensitiveSet<String> fromCollection = new CaseInsensitiveSet<>(source);
        assertEquals(3, fromCollection.size(), "Constructor should deduplicate case‑insensitively.");
    }

    @Test
    void size_withInitialCapacityConstructor() {
        CaseInsensitiveSet<String> withCapacity = new CaseInsensitiveSet<>(10);
        assertEquals(0, withCapacity.size(), "Set created with an initial capacity should start empty.");
    }

    @Test
    void size_withInitialCapacityAndLoadFactorConstructor() {
        CaseInsensitiveSet<String> withCapacityAndLoad = new CaseInsensitiveSet<>(10, 0.75f);
        assertEquals(0, withCapacityAndLoad.size(), "Set created with capacity and load factor should start empty.");
    }

    @Test
    void size_afterAddingAllFromCollection() {
        Collection<String> coll = Arrays.asList("X", "Y", "Z", "x");
        // "X" and "x" are duplicates case‑insensitively.
        set.addAll(coll);
        assertEquals(3, set.size(), "addAll should respect case‑insensitive uniqueness.");
    }

    @Test
    void size_afterAddingSingleElement() {
        assertTrue(set.add("Apple"));
        assertEquals(1, set.size(), "After adding one element the size should be 1.");
    }

    @Test
    void size_afterAddingThreeDistinctElements() {
        set.addAll(Arrays.asList("One", "Two", "Three"));
        assertEquals(3, set.size(), "Three case‑insensitively distinct elements should give size 3.");
    }

    @Test
    void size_duplicateCaseInsensitiveDoesNotIncrease() {
        assertTrue(set.add("Apple"));
        // duplicate, different case
        assertFalse(set.add("apple"));
        assertEquals(1, set.size());
        // add another distinct element
        assertTrue(set.add("Banana"));
        assertEquals(2, set.size(), "Duplicate (case‑insensitive) should not affect size, distinct adds should.");
    }

    @Test
    void size_afterClearReturnsZero() {
        set.addAll(Arrays.asList("a", "b"));
        assertEquals(2, set.size());
        set.clear();
        assertEquals(0, set.size(), "After clear() the size must be 0.");
    }

    @Test
    void size_addAllWithMixedDuplicates() {
        // collection contains duplicates only differing by case
        Collection<String> coll = Arrays.asList("a", "b", "c", "A", "d", "e");
        // unique case‑insensitive values: a,b,c,d,e => 5
        set.addAll(coll);
        assertEquals(5, set.size(), "addAll should count only case‑insensitive unique entries.");
    }

    @Test
    void size_afterRemovalOfExistingElement() {
        set.addAll(Arrays.asList("One", "Two", "Three"));
        assertTrue(set.remove("Two"));
        assertEquals(2, set.size(), "Removing an existing element should decrement size.");
    }

    @Test
    void size_removeNonexistentElementDoesNotChangeSize() {
        set.addAll(Arrays.asList("One", "Two", "Three"));
        assertFalse(set.remove("Four"));
        assertEquals(3, set.size(), "Removing a non‑existent element must not affect size.");
    }

    @Test
    void size_afterRetainAllKeepsOnlySpecifiedElements() {
        set.addAll(Arrays.asList("a", "b", "c", "d"));
        boolean changed = set.retainAll(Arrays.asList("b", "c", "e"));
        assertTrue(changed);
        assertEquals(2, set.size(), "retainAll should leave only the intersecting elements.");
    }

    @Test
    void size_afterRemoveAllEliminatesSpecifiedElements() {
        // duplicate "x" ignored
        set.addAll(Arrays.asList("x", "y", "z", "x"));
        boolean changed = set.removeAll(Arrays.asList("x", "z"));
        assertTrue(changed);
        assertEquals(1, set.size(), "removeAll should remove the specified elements, leaving only 'y'.");
    }

    @Test
    void size_addingNullCountsAsOneElement() {
        assertTrue(set.add(null));
        assertEquals(1, set.size(), "Adding null to an empty set should increase size to 1.");
        // Adding null again should be considered a duplicate
        assertFalse(set.add(null));
        assertEquals(1, set.size(), "Adding null a second time must not increase size.");
    }

    @Test
    void size_massiveAdditionOfDistinctElements() {
        for (int i = 0; i < 1000; i++) {
            set.add("item" + i);
        }
        assertEquals(1000, set.size(), "Adding 1,000 distinct strings should result in size 1000.");
    }

    @Test
    void size_afterIteratorRemoval() {
        set.addAll(Arrays.asList("alpha", "beta", "gamma"));
        Iterator<String> it = set.iterator();
        assertTrue(it.hasNext());
        // point to first element
        it.next();
        // remove via iterator
        it.remove();
        assertEquals(2, set.size(), "Iterator.remove() must correctly update the set size.");
    }

    @Test
    void size_addAllOnNonEmptySet() {
        set.addAll(Arrays.asList("a", "b"));
        // collection contains one duplicate ("b") and two new elements
        set.addAll(Arrays.asList("b", "c", "d"));
        assertEquals(4, set.size(), "addAll on a non‑empty set should only count new case‑insensitive values.");
    }

    @Test
    void size_whenInternalMapIsNull_throwsNullPointerException() throws Exception {
        // Force the private 'map' field to null.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        assertThrows(NullPointerException.class, () -> set.size(), "If the internal map is null, size() should throw NPE.");
    }

    @Test
    void size_whenMapSizeThrows_throwsIllegalStateException() throws Exception {
        // Create a mock Map whose size() method throws IllegalStateException.
        @SuppressWarnings("unchecked")
        Map<String, Object> mockMap = mock(Map.class);
        when(mockMap.size()).thenThrow(new IllegalStateException("boom"));
        // Inject the mock into the set via reflection.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, mockMap);
        assertThrows(IllegalStateException.class, () -> set.size(), "size() should propagate IllegalStateException thrown by the internal map.");
    }
}
