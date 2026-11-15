package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SealableList_toArray_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    @BeforeEach
    void setupBeforeEach() {
        sealedState = false;
        list = new SealableList<>(new ArrayList<>(), sealedSupplier);
        // default list used by a few tests – individual tests will re‑populate as needed
        list.add(10);
        list.add(20);
        list.add(30);
    }

    @Test
    void testToArrayReturnsCorrectElements() {
        Object[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(10, array[0]);
        assertEquals(20, array[1]);
        assertEquals(30, array[2]);
    }

    @Test
    void testToArrayReturnsCopyNotReference() {
        Object[] array = list.toArray();
        // mutate the returned array
        array[0] = 99;
        assertEquals(10, list.get(0), "Original list must remain unchanged");
    }

    @Test
    void testToArrayOnEmptyList() {
        SealableList<String> empty = new SealableList<>(sealedSupplier);
        Object[] array = empty.toArray();
        assertEquals(0, array.length);
    }

    @Test
    void testTypedToArrayExactSize() {
        Integer[] supplied = new Integer[3];
        Integer[] result = list.toArray(supplied);
        assertSame(supplied, result, "When supplied array is exact size, it should be returned");
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result);
    }

    @Test
    void testTypedToArrayLargerArray() {
        Integer[] supplied = new Integer[5];
        Integer[] result = list.toArray(supplied);
        assertSame(supplied, result, "When supplied array is larger, it should be returned");
        assertArrayEquals(new Integer[] { 10, 20, 30, null, null }, result);
    }

    @Test
    void testTypedToArraySmallerArray() {
        Integer[] supplied = new Integer[2];
        Integer[] result = list.toArray(supplied);
        assertNotSame(supplied, result, "When supplied array is too small, a new array must be created");
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result);
    }

    @Test
    void testToArrayWhenSealedDoesNotThrow() {
        // seal the list
        sealedState = true;
        // toArray should still work because it does not modify the list
        Object[] array = list.toArray();
        assertEquals(3, array.length);
        assertEquals(10, array[0]);
    }

    @Test
    void testToArrayEmptyListFromJson() {
        // JSON case: empty SealableList should return empty Object[]
        SealableList<Object> empty = new SealableList<>(sealedSupplier);
        Object[] result = empty.toArray();
        assertEquals(0, result.length, "Empty list must produce an empty array");
    }

    @Test
    void testToArrayPreservesInsertionOrder() {
        // JSON case: list with 1,2,3 must preserve order
        SealableList<Integer> intList = new SealableList<>(sealedSupplier);
        intList.add(1);
        intList.add(2);
        intList.add(3);
        Object[] result = intList.toArray();
        assertArrayEquals(new Object[] { 1, 2, 3 }, result);
    }

    @Test
    void testToArrayIncludesNullElement() {
        // JSON case: null followed by "test"
        SealableList<Object> mixed = new SealableList<>(sealedSupplier);
        mixed.add(null);
        mixed.add("test");
        Object[] result = mixed.toArray();
        assertEquals(2, result.length);
        assertNull(result[0], "First element should be null");
        assertEquals("test", result[1]);
    }

    @Test
    void testToArrayProducesDistinctInstancesOnSuccessiveCalls() {
        // JSON case: two distinct Object[] instances on successive calls
        Object[] first = list.toArray();
        Object[] second = list.toArray();
        assertNotSame(first, second, "Each call must return a new array instance");
        // modify first and ensure second is unaffected
        first[0] = 999;
        assertEquals(10, second[0], "Second array must reflect original list, not mutation of first");
    }

    @Test
    void testToArrayAfterClearReturnsEmpty() {
        // JSON case: after clear, toArray returns empty array
        SealableList<String> sl = new SealableList<>(sealedSupplier);
        sl.add("x");
        sl.add("y");
        sl.clear();
        Object[] result = sl.toArray();
        assertEquals(0, result.length);
    }

    @Test
    void testToArrayAfterRemoval() {
        // JSON case: remove element at index 1 and verify remaining order
        SealableList<String> sl = new SealableList<>(sealedSupplier);
        sl.add("A");
        sl.add("B");
        sl.add("C");
        // remove "B"
        sl.remove(1);
        Object[] result = sl.toArray();
        assertArrayEquals(new Object[] { "A", "C" }, result);
    }

    @Test
    void testToArrayWithDuplicates() {
        // JSON case: three identical values
        SealableList<Integer> sl = new SealableList<>(sealedSupplier);
        sl.add(5);
        sl.add(5);
        sl.add(5);
        Object[] result = sl.toArray();
        assertArrayEquals(new Object[] { 5, 5, 5 }, result);
    }

    @Test
    void testToArrayLargeCollection() {
        // JSON case: 1000 sequential integers
        SealableList<Integer> sl = new SealableList<>(sealedSupplier);
        for (int i = 0; i < 1000; i++) {
            sl.add(i);
        }
        Object[] result = sl.toArray();
        assertEquals(1000, result.length);
        for (int i = 0; i < 1000; i++) {
            assertEquals(i, result[i]);
        }
    }

    @Test
    void testToArraySelfReference() {
        // JSON case: list contains itself
        SealableList<Object> sl = new SealableList<>(sealedSupplier);
        // self‑reference
        sl.add(sl);
        Object[] result = sl.toArray();
        assertEquals(1, result.length);
        assertSame(sl, result[0], "The element must be the same instance as the original list");
    }

    @Test
    void testToArrayMixedTypes() {
        // JSON case: heterogeneous elements including null
        SealableList<Object> sl = new SealableList<>(sealedSupplier);
        sl.add(1);
        sl.add("two");
        sl.add(3.0);
        sl.add(null);
        Object[] result = sl.toArray();
        assertArrayEquals(new Object[] { 1, "two", 3.0, null }, result);
    }

    @Test
    void testToArrayArrayIndependenceAfterMutation() {
        // JSON case: mutate first returned array, second call must be unchanged
        Object[] first = list.toArray();
        first[0] = "changed";
        Object[] second = list.toArray();
        assertArrayEquals(new Object[] { 10, 20, 30 }, second, "Second call must reflect original list contents, not the mutation of the first array");
    }

    @Test
    void testToArrayThrowsWhenInternalListIsNull() throws Exception {
        // JSON case: simulate faulty construction where backing list is null.
        // We create a valid instance then use reflection to null out the private field.
        SealableList<Object> sl = new SealableList<>(sealedSupplier);
        // Use reflection to set the private 'list' field to null
        Field listField = SealableList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(sl, null);
        // Expect a NullPointerException when delegating to list.toArray()
        assertThrows(NullPointerException.class, sl::toArray, "Calling toArray() with a null backing list should throw NPE");
    }
}
