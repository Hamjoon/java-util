package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_size_Test {

    /**
     * Instance used by the tests that need a fresh set for each execution.
     */
    private CaseInsensitiveSet<Object> set;

    /**
     * Creates a new, empty {@link CaseInsensitiveSet} before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        set = new CaseInsensitiveSet<>();
        set = new CaseInsensitiveSet<>();
    }

    /**
     * Clears the reference after each test – helps the garbage collector and isolates tests.
     */
    @AfterEach
    void teardownAfterEach() {
        set = null;
        set = null;
    }

    /**
     * Supplies different map implementations for the parameterized test above.
     */
    private static Stream<Map<Object, Object>> backingMapProvider() {
        return Stream.of(new TreeMap<>(), new ConcurrentSkipListMap<>());
    }

    /**
     * Verifies that a newly created set reports size 0.
     */
    @Test
    void sizeOfNewSetIsZero() {
        assertEquals(0, set.size(), "A freshly created set must be empty");
    }

    /**
     * Checks size after a mixture of adds and removes, including case‑insensitive removal of strings.
     */
    @Test
    void sizeReflectsAddsAndRemoves() {
        // add two distinct elements
        assertTrue(set.add(42));
        assertTrue(set.add("Hello"));
        assertEquals(2, set.size());
        // adding a duplicate (different case) must not increase size
        assertFalse(set.add("hello"));
        assertEquals(2, set.size());
        // remove by value (different case) – size should decrease
        assertTrue(set.remove("HELLO"));
        assertEquals(1, set.size());
        // remove the remaining element
        assertTrue(set.remove(42));
        assertEquals(0, set.size());
    }

    /**
     * Validates that the constructor with an initial capacity still yields a correctly sized set.
     */
    @Test
    void sizeWithInitialCapacityConstructor() {
        CaseInsensitiveSet<String> sizedSet = new CaseInsensitiveSet<>(10);
        assertEquals(0, sizedSet.size(), "Set created with initial capacity should start empty");
        sizedSet.add("One");
        sizedSet.add("Two");
        assertEquals(2, sizedSet.size());
        sizedSet.remove("ONE");
        assertEquals(1, sizedSet.size());
    }

    /**
     * Ensures that a set built from an existing collection reports the correct size.
     */
    @Test
    void sizeFromExistingCollection() {
        List<Object> source = Arrays.asList("Apple", "Banana", "apple", 100, 100);
        // "Apple" and "apple" are considered the same element (case‑insensitive)
        CaseInsensitiveSet<Object> fromCollection = new CaseInsensitiveSet<>(source);
        assertEquals(3, fromCollection.size(), "Duplicate strings (different case) and duplicate non‑strings must be collapsed");
    }

    /**
     * Tests size when a custom backing map (LinkedHashMap) is supplied – order does not affect size.
     */
    @Test
    void sizeWithCustomBackingMap() {
        Map<Object, Object> backing = new LinkedHashMap<>();
        CaseInsensitiveSet<Object> customSet = new CaseInsensitiveSet<>(Collections.emptySet(), backing);
        assertEquals(0, customSet.size());
        customSet.add("X");
        customSet.add("y");
        // duplicate, should be ignored
        customSet.add("X");
        assertEquals(2, customSet.size());
        // Verify that the backing map indeed contains the expected number of entries
        assertEquals(2, backing.size(), "Backing map size must stay in sync with set size");
    }

    /**
     * Confirms that {@code size()} works correctly after bulk operations.
     */
    @Test
    void sizeAfterAddAllAndClear() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            many.add("Item" + i);
        }
        // addAll should respect case‑insensitivity as well
        assertTrue(set.addAll(many));
        assertEquals(50, set.size());
        // addAll again with same elements (different case) must not change size
        List<String> mixedCase = new ArrayList<>();
        for (int i = 0; i < 50; i++) {
            // lower‑case version
            mixedCase.add("item" + i);
        }
        set.addAll(mixedCase);
        assertEquals(50, set.size());
        // clear should reset size to zero
        set.clear();
        assertEquals(0, set.size());
    }

    /**
     * Adds three distinct elements and verifies the size is 3.
     */
    @Test
    void sizeAfterAddingDistinctElements() {
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertTrue(set.add("c"));
        assertEquals(3, set.size(), "Size should be 3 after adding three distinct elements");
    }

    /**
     * Adds duplicate strings differing only by case and checks that size reflects case‑insensitivity.
     */
    @Test
    void sizeAfterAddingDuplicateElementsCaseInsensitive() {
        assertTrue(set.add("Apple"));
        // duplicate, should not increase size
        assertFalse(set.add("apple"));
        assertTrue(set.add("Banana"));
        assertEquals(2, set.size(), "Duplicate case‑insensitive strings must not increase size");
    }

    /**
     * Removes elements step‑by‑step and checks that size updates correctly.
     */
    @Test
    void sizeAfterRemovingElements() {
        // initialise with three elements
        assertTrue(set.add("one"));
        assertTrue(set.add("two"));
        assertTrue(set.add("three"));
        assertEquals(3, set.size());
        // first removal
        assertTrue(set.remove("two"));
        assertEquals(2, set.size());
        // second removal
        assertTrue(set.remove("three"));
        assertEquals(1, set.size());
    }

    /**
     * Populates the set, clears it, and verifies size returns to zero.
     */
    @Test
    void sizeAfterClearOperation() {
        set.add("alpha");
        set.add("beta");
        set.add("gamma");
        assertEquals(3, set.size());
        set.clear();
        assertEquals(0, set.size(), "Size must be 0 after clear()");
    }

    /**
     * Parameterized test that checks size consistency when using different backing map implementations.
     */
    @ParameterizedTest
    @MethodSource("backingMapProvider")
    void sizeConsistencyWithUnderlyingMapImplementation(Map<Object, Object> backingMap) {
        // The constructor (Collection, Map) is used to inject the custom map.
        CaseInsensitiveSet<Object> customSet = new CaseInsensitiveSet<>(Collections.emptySet(), backingMap);
        // add five distinct elements
        customSet.add("One");
        customSet.add("Two");
        customSet.add("Three");
        customSet.add(4);
        customSet.add(5L);
        assertEquals(5, customSet.size(), "Set size should match number of distinct elements added");
        assertEquals(5, backingMap.size(), "Backing map size must stay in sync with set size");
    }

    /**
     * Adding a single {@code null} element should increase size to 1.
     */
    @Test
    void sizeAfterAddingSingleNullElement() {
        assertTrue(set.add(null), "Adding null should return true on first insertion");
        assertEquals(1, set.size(), "Size must be 1 after adding a null element");
    }

    /**
     * Simulates a corrupted instance where the internal map is {@code null} and expects NPE on size().
     */
    @Test
    void sizeWhenInternalMapIsNull() throws Exception {
        // Create a normal instance first.
        CaseInsensitiveSet<Object> corrupted = new CaseInsensitiveSet<>();
        // Use reflection to set the private 'map' field to null.
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(corrupted, null);
        // Verify that calling size() now throws NullPointerException.
        assertThrows(NullPointerException.class, corrupted::size, "size() should throw NullPointerException when internal map is null");
    }
}
