package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveSet_toArray_Test {

    private CaseInsensitiveSet<Object> set;

    @BeforeEach
    void setupBeforeEach() {
        // fresh instance for each test method
        set = new CaseInsensitiveSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // No per‑test cleanup required
        set.clear();
    }

    private static Stream<Arguments> simpleCasesProvider() {
        return Stream.of(// empty set
        Arguments.of(Collections.emptyList(), new Object[] {}), // single element "apple"
        Arguments.of(List.of("apple"), new Object[] { "apple" }), // three distinct elements, alphabetical order expected because default map is insertion‑order,
        // but the JSON assumes TreeMap ordering; we assert insertion order here.
        Arguments.of(List.of("apple", "banana", "cherry"), new Object[] { "apple", "banana", "cherry" }), // case‑insensitive duplicate: add "apple" then "Apple"
        Arguments.of(List.of("apple", "Apple"), // first inserted case retained
        new Object[] { "apple" }));
    }

    @Test
    void toArray_EmptySet_ReturnsEmptyArray() {
        Object[] result = set.toArray();
        assertNotNull(result, "Resulting array should never be null");
        assertEquals(0, result.length, "Empty set should produce an empty array");
    }

    @Test
    void toArray_StringElements_CaseInsensitiveDeduplicationAndOrderPreserved() {
        // Add strings with varying case – duplicates should be ignored
        set.add("Hello");
        set.add("world");
        // duplicate of "Hello" (different case)
        set.add("HELLO");
        Object[] result = set.toArray();
        // Expected order: insertion order of the first occurrence of each distinct element
        Object[] expected = { "Hello", "world" };
        assertArrayEquals(expected, result, "toArray should return elements in insertion order without case‑duplicate entries");
    }

    @Test
    void toArray_MixedTypes_CorrectElementsAndOrder() {
        set.add(5);
        set.add("Apple");
        // duplicate of "Apple" (different case) – should be ignored
        set.add("apple");
        set.add(3.14);
        set.add("Banana");
        Object[] result = set.toArray();
        Object[] expected = { 5, "Apple", 3.14, "Banana" };
        assertArrayEquals(expected, result, "toArray must preserve insertion order and handle heterogeneous element types");
    }

    @Test
    void toArray_ReturnsObjectArray_NotSpecificTypedArray() {
        set.add("One");
        set.add(2);
        set.add("Three");
        Object[] result = set.toArray();
        assertTrue(result instanceof Object[], "toArray should return an Object[] (the Set interface contract)");
        assertEquals(3, result.length);
    }

    @Test
    void toArray_ModifyingReturnedArrayDoesNotAffectSet() {
        set.add("Alpha");
        set.add("Beta");
        Object[] result = set.toArray();
        result[0] = "Modified";
        // The set itself must remain unchanged
        assertTrue(set.contains("Alpha"));
        assertTrue(set.contains("Beta"));
        assertFalse(set.contains("Modified"));
    }

    @ParameterizedTest(name = "toArray simple case #{index}")
    @MethodSource("simpleCasesProvider")
    void toArray_SimpleCases(List<Object> elementsToAdd, Object[] expected) {
        elementsToAdd.forEach(set::add);
        Object[] result = set.toArray();
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_AfterRemoval_ExcludesRemovedElement() {
        set.add("apple");
        set.add("banana");
        set.add("cherry");
        assertTrue(set.remove("banana"));
        Object[] result = set.toArray();
        Object[] expected = { "apple", "cherry" };
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_AfterClear_ReturnsEmptyArray() {
        set.add("x");
        set.add("y");
        set.clear();
        Object[] result = set.toArray();
        assertEquals(0, result.length);
    }

    @Test
    void toArray_AddAllWithDuplicates_CaseInsensitiveDeduplication() {
        Collection<String> source = List.of("apple", "banana", "apple", "Apple");
        set.addAll(source);
        Object[] result = set.toArray();
        // Expected distinct elements, preserving first insertion case
        Object[] expected = { "apple", "banana" };
        assertArrayEquals(expected, result);
    }

    @Test
    void toArray_SnapshotIndependenceAfterMutation() {
        set.add("apple");
        // snapshot should contain only "apple"
        Object[] snapshot = set.toArray();
        // mutate the set after snapshot
        set.add("banana");
        assertArrayEquals(new Object[] { "apple" }, snapshot, "Array returned earlier must remain unchanged after set mutation");
    }

    @Test
    void toArray_LargeSetSnapshot() {
        // Populate with 1000 distinct strings
        for (int i = 0; i < 1000; i++) {
            set.add("item" + i);
        }
        Object[] result = set.toArray();
        assertEquals(1000, result.length);
        // Verify first and last elements to ensure ordering
        assertEquals("item0", result[0]);
        assertEquals("item999", result[999]);
    }

    @Test
    void toArray_NullBackingMap_ThrowsNullPointerException() throws Exception {
        // Use reflection to set the private 'map' field to null
        Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(set, null);
        assertThrows(NullPointerException.class, () -> set.toArray(), "Calling toArray() when the backing map is null should throw NPE");
    }

    @Test
    void toArray_ConcurrentModification_ThrowsConcurrentModificationException() {
        // Populate the set
        for (int i = 0; i < 100; i++) {
            set.add("val" + i);
        }
        // Obtain the iterator from the underlying keySet and start iterating in a separate thread
        // while modifying the set to provoke CME.
        Iterator<Object> iterator = ((Set<?>) getKeySet(set)).iterator();
        // Advance iterator once
        assertTrue(iterator.hasNext());
        iterator.next();
        // Modify the set concurrently
        set.add("newElement");
        // The next iterator operation should detect the modification
        assertThrows(ConcurrentModificationException.class, iterator::next, "Concurrent modification should result in CME");
    }

    @Test
    void toArray_ArrayStoreException_WithIncompatibleElement() {
        // Insert an element that is a primitive array (e.g., int[])
        // This is legal as an Object, but copying into an Object[] works fine.
        // To provoke ArrayStoreException we need a scenario where the underlying
        // map returns a type that cannot be stored in Object[] – which is impossible.
        // Therefore we assert that no exception is thrown for such elements.
        set.add(new int[] { 1, 2, 3 });
        assertDoesNotThrow(() -> {
            Object[] arr = set.toArray();
            assertEquals(1, arr.length);
            assertTrue(arr[0] instanceof int[]);
        });
    }

    @SuppressWarnings("unchecked")
    private static Set<Object> getKeySet(CaseInsensitiveSet<Object> cis) {
        try {
            Field mapField = CaseInsensitiveSet.class.getDeclaredField("map");
            mapField.setAccessible(true);
            Map<Object, Object> map = (Map<Object, Object>) mapField.get(cis);
            return map.keySet();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
