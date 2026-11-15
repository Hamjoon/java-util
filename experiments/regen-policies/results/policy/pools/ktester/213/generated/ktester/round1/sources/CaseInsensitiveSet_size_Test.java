package com.cedarsoftware.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_size_Test {

    /**
     * A set created with the default constructor – used by most tests.
     */
    private CaseInsensitiveSet<Object> defaultSet;

    /**
     * A set created with a custom backing map – used to verify that size works with any map implementation.
     */
    private CaseInsensitiveSet<String> customMapSet;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh default set before each test.
        defaultSet = new CaseInsensitiveSet<>();
        // Initialise a set that uses a LinkedHashMap as the backing map.
        // The map is wrapped in a CaseInsensitiveMap to keep the case‑insensitive contract.
        Map<String, Object> backing = new LinkedHashMap<>();
        CaseInsensitiveMap<String, Object> ciMap = new CaseInsensitiveMap<>(Collections.emptyMap(), backing);
        customMapSet = new CaseInsensitiveSet<>(Collections.emptySet(), ciMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the GC in long test suites.
        defaultSet = null;
        customMapSet = null;
    }

    /**
     * Verify size with the default constructor and normal add/remove operations.
     */
    @Test
    void sizeDefaultConstructor() {
        // given
        defaultSet.add(42);
        defaultSet.add("Hello");
        // when
        int sizeAfterAdds = defaultSet.size();
        // then
        assertEquals(2, sizeAfterAdds, "Size should reflect two distinct elements");
        // when – remove an integer
        defaultSet.remove(42);
        // then
        assertEquals(1, defaultSet.size(), "Size should decrement after removing an element");
        // when – remove a string using a different case
        defaultSet.remove("hello");
        // then
        assertEquals(0, defaultSet.size(), "Case‑insensitive removal must affect size");
    }

    /**
     * Verify size when the set is created from an existing collection.
     */
    @Test
    void sizeFromCollectionConstructor() {
        // given a collection containing duplicate strings with different cases
        Collection<String> source = Arrays.asList("One", "ONE", "Two", "three");
        CaseInsensitiveSet<String> setFromCollection = new CaseInsensitiveSet<>(source);
        // when
        int size = setFromCollection.size();
        // then
        // "One"/"ONE" count as one element, others are distinct → total 3
        assertEquals(3, size, "Duplicate strings differing only by case must be collapsed");
    }

    /**
     * Verify size when the set is created with an initial capacity.
     */
    @Test
    void sizeWithInitialCapacityConstructor() {
        // given
        CaseInsensitiveSet<String> capacitySet = new CaseInsensitiveSet<>(10);
        capacitySet.addAll(Arrays.asList("Alpha", "Beta", "alpha"));
        // when
        int size = capacitySet.size();
        // then
        // "Alpha"/"alpha" collapse → 2 distinct elements
        assertEquals(2, size, "Initial capacity should not affect logical size");
    }

    /**
     * Verify size when the set is created with an initial capacity and load factor.
     */
    @Test
    void sizeWithInitialCapacityAndLoadFactorConstructor() {
        // given
        CaseInsensitiveSet<String> loadFactorSet = new CaseInsensitiveSet<>(5, 0.75f);
        loadFactorSet.addAll(Arrays.asList("X", "Y", "x"));
        // when
        int size = loadFactorSet.size();
        // then
        // "X"/"x" collapse → 2 distinct elements
        assertEquals(2, size, "Load factor constructor must still respect case‑insensitive semantics");
    }

    /**
     * Verify that size works correctly with a custom backing map (LinkedHashMap) and that order does not affect the count.
     */
    @Test
    void sizeWithCustomBackingMap() {
        // given
        customMapSet.addAll(Arrays.asList("Apple", "Banana", "apple"));
        // when
        int size = customMapSet.size();
        // then
        // "Apple"/"apple" collapse → 2 distinct elements
        assertEquals(2, size, "Custom backing map must still provide correct size");
    }

    /**
     * Verify that size returns zero for a freshly created set.
     */
    @Test
    void sizeOnEmptySet() {
        assertEquals(0, defaultSet.size(), "Newly constructed set must be empty");
    }

    /**
     * Verify that size reflects the number of elements after a bulk removal.
     */
    @Test
    void sizeAfterBulkRemove() {
        // given
        defaultSet.addAll(Arrays.asList("One", "Two", "Three", 100));
        assertEquals(4, defaultSet.size());
        // when
        defaultSet.removeAll(Arrays.asList("ONE", "two", 100));
        // then
        assertEquals(1, defaultSet.size(), "Bulk removal must correctly update size");
    }

    /**
     * size on newly created empty set (JSON group).
     */
    @Test
    void sizeOnNewlyCreatedEmptySet() {
        // The defaultSet is already empty thanks to @BeforeEach.
        assertEquals(0, defaultSet.size(), "A freshly instantiated CaseInsensitiveSet should report size 0.");
    }

    /**
     * size after adding distinct elements (JSON group).
     */
    @Test
    void sizeAfterAddingDistinctElements() {
        defaultSet.add("One");
        defaultSet.add("Two");
        defaultSet.add("Three");
        assertEquals(3, defaultSet.size(), "After adding three different strings the set size must be 3.");
    }

    /**
     * size after adding duplicate elements (case‑insensitive) (JSON group).
     */
    @Test
    void sizeAfterAddingDuplicateElementsCaseInsensitive() {
        defaultSet.add("Apple");
        // duplicate, different case
        defaultSet.add("apple");
        defaultSet.add("Banana");
        assertEquals(2, defaultSet.size(), "Adding \"Apple\" and then \"apple\" should not increase the size; final size must be 2 after also adding \"Banana\".");
    }

    /**
     * size after removing an existing element (JSON group).
     */
    @Test
    void sizeAfterRemovingExistingElement() {
        defaultSet.add("Cat");
        defaultSet.add("Dog");
        defaultSet.add("Elephant");
        // remove "Dog"
        defaultSet.remove("Dog");
        assertEquals(2, defaultSet.size(), "Removing \"Dog\" should decrease the size to 2.");
        // remove using different case
        defaultSet.remove("cat");
        assertEquals(1, defaultSet.size(), "Further removing \"cat\" (different case) should leave size 1.");
    }

    /**
     * size after attempting to remove a non‑existent element (JSON group).
     */
    @Test
    void sizeAfterRemovingNonExistentElement() {
        defaultSet.add("Red");
        defaultSet.add("Blue");
        // attempt to remove "Green" which is not present
        defaultSet.remove("Green");
        assertEquals(2, defaultSet.size(), "When the set contains \"Red\" and \"Blue\", trying to remove \"Green\" must leave the size unchanged at 2.");
    }

    /**
     * size after clear() (JSON group).
     */
    @Test
    void sizeAfterClear() {
        defaultSet.addAll(Arrays.asList("A", "B", "C"));
        assertEquals(3, defaultSet.size());
        defaultSet.clear();
        assertEquals(0, defaultSet.size(), "Calling clear() on a set that previously held elements must reset the size to 0.");
    }

    /**
     * size after bulk addAll() with overlapping elements (JSON group).
     */
    @Test
    void sizeAfterBulkAddAllWithOverlappingElements() {
        // first addAll
        defaultSet.addAll(Arrays.asList("One", "Two", "Three", "Two"));
        assertEquals(3, defaultSet.size(), "First bulk add should result in size 3 (duplicate \"Two\" ignored).");
        // second addAll
        defaultSet.addAll(Arrays.asList("Three", "Four"));
        assertEquals(4, defaultSet.size(), "Second bulk add should increase size to 4.");
    }

    /**
     * size after retainAll() filtering (JSON group).
     */
    @Test
    void sizeAfterRetainAllFiltering() {
        defaultSet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        // retain only beta and gamma (different cases)
        defaultSet.retainAll(Arrays.asList("beta", "gamma"));
        assertEquals(2, defaultSet.size(), "retainAll with case‑insensitive matches should keep two elements.");
    }

    /**
     * size after removeAll() with mixed case elements (JSON group).
     */
    @Test
    void sizeAfterRemoveAllMixedCaseElements() {
        defaultSet.addAll(Arrays.asList("One", "Two", "Three"));
        defaultSet.removeAll(Arrays.asList("two", "FOUR"));
        assertEquals(2, defaultSet.size(), "removeAll should remove only \"Two\" (case‑insensitive), leaving two elements.");
    }

    /**
     * size after addAll with empty collection (JSON group).
     */
    @Test
    void sizeAfterAddAllEmptyCollection() {
        defaultSet.addAll(Collections.emptyList());
        assertEquals(0, defaultSet.size(), "Calling addAll() with an empty collection on a newly created set must not change the set; size() should remain 0.");
    }

    /**
     * size after retainAll with empty collection (JSON group).
     */
    @Test
    void sizeAfterRetainAllEmptyCollection() {
        defaultSet.addAll(Arrays.asList("A", "B", "C"));
        defaultSet.retainAll(Collections.emptyList());
        assertEquals(0, defaultSet.size(), "When the set contains \"A\", \"B\", \"C\" and retainAll() is invoked with an empty collection, all elements are removed; size() must be 0.");
    }

    /**
     * size after removeAll with empty collection (JSON group).
     */
    @Test
    void sizeAfterRemoveAllEmptyCollection() {
        defaultSet.addAll(Arrays.asList("X", "Y"));
        defaultSet.removeAll(Collections.emptyList());
        assertEquals(2, defaultSet.size(), "Starting with \"X\" and \"Y\", calling removeAll() with an empty collection must leave the set unchanged; size() stays 2.");
    }

    /**
     * size after adding non‑String elements (JSON group).
     */
    @Test
    void sizeAfterAddingNonStringElements() {
        defaultSet.add(1);
        defaultSet.add(2);
        defaultSet.add(3);
        assertEquals(3, defaultSet.size(), "Adding three distinct non‑String objects should increase the size to 3 because case‑insensitivity only affects CharSequence keys.");
    }

    /**
     * size after adding duplicate non‑String elements (JSON group).
     */
    @Test
    void sizeAfterAddingDuplicateNonStringElements() {
        Integer value = Integer.valueOf(42);
        defaultSet.add(value);
        // duplicate reference
        defaultSet.add(value);
        assertEquals(1, defaultSet.size(), "Adding the same Integer object twice must result in a single entry; size() should be 1.");
    }

    /**
     * size after iterator remove (JSON group).
     */
    @Test
    void sizeAfterIteratorRemove() {
        defaultSet.addAll(Arrays.asList("Alpha", "Beta", "Gamma"));
        assertEquals(3, defaultSet.size());
        Iterator<Object> it = defaultSet.iterator();
        while (it.hasNext()) {
            Object cur = it.next();
            if ("Beta".equals(cur)) {
                // remove Beta
                it.remove();
                break;
            }
        }
        assertEquals(2, defaultSet.size(), "Iterator.remove() should reduce size to 2 after removing \"Beta\".");
        // second removal via iterator (remove \"Gamma\")
        it = defaultSet.iterator();
        while (it.hasNext()) {
            Object cur = it.next();
            if ("Gamma".equals(cur)) {
                it.remove();
                break;
            }
        }
        assertEquals(1, defaultSet.size(), "Second iterator.remove() should bring size down to 1.");
    }

    /**
     * size after attempting to add null element (JSON group).
     */
    @Test
    void sizeAfterAttemptingToAddNullElement() {
        assertThrows(NullPointerException.class, () -> defaultSet.add(null), "Attempting to add a null element should throw NullPointerException.");
        // after the exception the set should still be empty
        assertEquals(0, defaultSet.size(), "Size remains 0 after failed null insertion.");
    }

    /**
     * size when internal map is null (JSON group).
     */
    @Test
    void sizeWhenInternalMapIsNull() throws Exception {
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        // Use reflection to force the private 'map' field to null.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        assertThrows(NullPointerException.class, set::size, "Invoking size() when the internal map is null should throw NullPointerException.");
    }

    /**
     * size when internal map throws RuntimeException (JSON group).
     */
    @Test
    void sizeWhenInternalMapThrowsRuntimeException() throws Exception {
        // Create a mock Map whose size() method throws a RuntimeException.
        @SuppressWarnings("unchecked")
        Map<Object, Object> explodingMap = mock(Map.class);
        when(explodingMap.size()).thenThrow(new RuntimeException("boom"));
        CaseInsensitiveSet<Object> set = new CaseInsensitiveSet<>();
        // Inject the mock via reflection.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, explodingMap);
        RuntimeException ex = assertThrows(RuntimeException.class, set::size, "Calling size() should propagate the RuntimeException thrown by the backing map.");
        assertEquals("boom", ex.getMessage());
    }
}
