package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class SealableList_toArray_2_Test {

    private SealableList<Integer> list;

    private volatile boolean sealedState = false;

    private Supplier<Boolean> sealedSupplier = () -> sealedState;

    private SealableList<Integer> intList;

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealedState = false;
        intList = new SealableList<>(new ArrayList<>(), sealedSupplier);
        intList.add(10);
        intList.add(20);
        intList.add(30);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required
        list = null;
    }

    /**
     * Helper to create a SealableList with a given backing list.
     */
    private <T> SealableList<T> createSealableList(List<T> backing) {
        return new SealableList<>(backing, sealedSupplier);
    }

    @Test
    void testToArrayExactSizeArray() {
        Integer[] target = new Integer[3];
        Integer[] result = list.toArray(target);
        // When the supplied array is exactly the right size, the same array is returned
        assertSame(target, result, "Returned array should be the same instance when size matches");
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result);
    }

    @Test
    void testToArrayLargerArray() {
        Integer[] target = new Integer[5];
        Integer[] result = list.toArray(target);
        // Same instance is returned, extra slots must be null per List.toArray contract
        assertSame(target, result, "Returned array should be the same instance when larger");
        assertEquals(10, result[0]);
        assertEquals(20, result[1]);
        assertEquals(30, result[2]);
        assertNull(result[3], "First extra element must be null");
        assertNull(result[4], "Second extra element must be null");
    }

    @Test
    void testToArraySmallerArray() {
        Integer[] target = new Integer[2];
        Integer[] result = list.toArray(target);
        // A new array must be created because the supplied one is too small
        assertNotSame(target, result, "Returned array should be a new instance when too small");
        assertEquals(3, result.length, "Resulting array length must match list size");
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result);
    }

    @Test
    void testToArrayWhenSealed() {
        // Seal the list – toArray should still succeed because it does not modify the list
        sealedState = true;
        Integer[] target = new Integer[3];
        Integer[] result = list.toArray(target);
        assertSame(target, result);
        assertArrayEquals(new Integer[] { 10, 20, 30 }, result);
    }

    @Test
    void testToArrayWithDifferentComponentType() {
        // Provide an array of a supertype (Number) – List.toArray should return a Number[]
        Number[] target = new Number[3];
        Number[] result = list.toArray(target);
        assertSame(target, result);
        assertArrayEquals(new Number[] { 10, 20, 30 }, result);
    }

    @Test
    void testToArrayOnEmptyList() {
        // Create an empty SealableList and request an array
        SealableList<String> empty = new SealableList<>(new ArrayList<>(), sealedSupplier);
        String[] arr = empty.toArray(new String[0]);
        assertNotNull(arr);
        assertEquals(0, arr.length);
    }

    @Test
    void testToArrayWithZeroLengthStringArray() {
        // Underlying list: ["alpha", "beta"]
        List<String> backing = new ArrayList<>();
        backing.add("alpha");
        backing.add("beta");
        SealableList<String> list = createSealableList(backing);
        // should allocate new array
        String[] result = list.toArray(new String[0]);
        assertNotSame(new String[0], result, "A new array must be allocated");
        assertArrayEquals(new String[] { "alpha", "beta" }, result);
    }

    @Test
    void testToArrayWithLargerStringArray() {
        // Underlying list: ["alpha", "beta"]
        List<String> backing = new ArrayList<>();
        backing.add("alpha");
        backing.add("beta");
        SealableList<String> list = createSealableList(backing);
        String[] supplied = new String[5];
        String[] result = list.toArray(supplied);
        assertSame(supplied, result, "The supplied larger array should be returned");
        assertEquals("alpha", result[0]);
        assertEquals("beta", result[1]);
        assertNull(result[2], "Element after last must be null");
        // Remaining slots are unspecified but must stay null per contract
        assertNull(result[3]);
        assertNull(result[4]);
    }

    @Test
    void testToArrayClassCastExceptionWhenArrayTypeIncompatible() {
        // List holds Strings, caller supplies Integer[]
        List<String> backing = new ArrayList<>();
        backing.add("x");
        SealableList<String> list = createSealableList(backing);
        assertThrows(ClassCastException.class, () -> list.toArray(new Integer[0]), "Storing a String into an Integer[] must throw ClassCastException");
    }

    @Test
    void testExactSizeStringArrayReturned() {
        // List: ["alpha", "beta"]
        List<String> backing = new ArrayList<>();
        backing.add("alpha");
        backing.add("beta");
        SealableList<String> list = createSealableList(backing);
        String[] supplied = new String[2];
        String[] result = list.toArray(supplied);
        assertSame(supplied, result, "Exact‑size array should be returned unchanged");
        assertArrayEquals(new String[] { "alpha", "beta" }, result);
    }

    @Test
    void testSmallerArrayCausesNewAllocation() {
        // List: ["one","two","three"]
        List<String> backing = new ArrayList<>();
        backing.add("one");
        backing.add("two");
        backing.add("three");
        SealableList<String> list = createSealableList(backing);
        // too small
        String[] supplied = new String[2];
        String[] result = list.toArray(supplied);
        assertNotSame(supplied, result, "A new array must be allocated when supplied one is too small");
        assertEquals(3, result.length);
        assertArrayEquals(new String[] { "one", "two", "three" }, result);
    }

    @Test
    void testToArrayNullArrayArgument() {
        List<String> backing = new ArrayList<>();
        backing.add("anything");
        SealableList<String> list = createSealableList(backing);
        assertThrows(NullPointerException.class, () -> list.toArray((String[]) null), "Passing null as the array argument must throw NullPointerException");
    }

    @Test
    void testToArrayOnEmptyListWithZeroLengthArray() {
        SealableList<String> empty = new SealableList<>(new ArrayList<>(), sealedSupplier);
        String[] supplied = new String[0];
        String[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Empty list + zero‑length array should return the supplied array");
        assertEquals(0, result.length);
    }

    @Test
    void testToArrayOnEmptyListWithLargerArray() {
        SealableList<String> empty = new SealableList<>(new ArrayList<>(), sealedSupplier);
        String[] supplied = new String[5];
        String[] result = empty.toArray(supplied);
        assertSame(supplied, result, "Empty list + larger array should return the supplied array");
        assertNull(result[0], "First element must be set to null as the marker");
    }

    @Test
    void testToArrayListContainingNullElements() {
        // List: [null, "gamma"]
        List<String> backing = new ArrayList<>();
        backing.add(null);
        backing.add("gamma");
        SealableList<String> list = createSealableList(backing);
        String[] supplied = new String[2];
        String[] result = list.toArray(supplied);
        assertSame(supplied, result, "Exact‑size array should be returned");
        assertArrayEquals(new String[] { null, "gamma" }, result);
    }

    @Test
    void testToArraySupertypeArray() {
        // List of Integer values
        List<Integer> backing = new ArrayList<>();
        backing.add(1);
        backing.add(2);
        SealableList<Integer> list = createSealableList(backing);
        Number[] supplied = new Number[0];
        Number[] result = list.toArray(supplied);
        assertNotSame(supplied, result, "A new Number[] must be allocated because supplied length is 0");
        assertArrayEquals(new Number[] { 1, 2 }, result);
    }

    @Test
    void testToArrayLargerObjectArray() {
        // List: ["delta"]
        List<String> backing = new ArrayList<>();
        backing.add("delta");
        SealableList<String> list = createSealableList(backing);
        Object[] supplied = new Object[3];
        Object[] result = list.toArray(supplied);
        assertSame(supplied, result, "Larger Object[] should be returned unchanged");
        assertEquals("delta", result[0]);
        assertNull(result[1], "Marker null after last element");
        assertNull(result[2]);
    }

    @Test
    void testToArrayWhenInternalListIsNull() throws Exception {
        // Simulate corrupted internal state by null‑ing the private 'list' field.
        SealableList<String> list = createSealableList(new ArrayList<>());
        Field f = SealableList.class.getDeclaredField("list");
        f.setAccessible(true);
        // now internal list is null
        f.set(list, null);
        assertThrows(NullPointerException.class, () -> list.toArray(new String[0]), "Delegating to a null internal list must result in NullPointerException");
    }

    @Test
    void testToArrayArrayStoreExceptionWhenIncompatibleArray() {
        // List holds Strings, caller supplies Integer[] – should throw ArrayStoreException.
        List<String> backing = new ArrayList<>();
        backing.add("hello");
        SealableList<String> list = createSealableList(backing);
        assertThrows(ArrayStoreException.class, () -> list.toArray(new Integer[0]), "Storing a String into an Integer[] must throw ArrayStoreException");
    }
}
