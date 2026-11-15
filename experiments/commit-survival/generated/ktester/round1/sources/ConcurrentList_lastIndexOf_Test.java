package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_lastIndexOf_Test {

    /**
     * The list used in most tests – created fresh before each test.
     */
    private ConcurrentList<Integer> intList;

    /**
     * A list that wraps an existing mutable list – used for the constructor‑with‑list test.
     */
    private List<String> wrappedStringList;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise a fresh ConcurrentList for each test.
        intList = new ConcurrentList<>();
        // Populate it with a known pattern (including duplicates).
        intList.add(10);
        intList.add(20);
        intList.add(30);
        intList.add(20);
        intList.add(40);
        intList.add(null);
        intList.add(20);
        // Prepare a wrapped list for the constructor‑with‑list scenario.
        List<String> backing = new ArrayList<>();
        backing.add("alpha");
        backing.add("beta");
        backing.add("gamma");
        wrappedStringList = new ConcurrentList<>(backing);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly necessary but keeps the pattern clear.
        intList = null;
        wrappedStringList = null;
    }

    /**
     * Provides arguments for the basic‑functionality group.
     * The backing list contains: ["apple", "orange", "apple", null, "apple"]
     */
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of("apple", 4, "List contains multiple occurrences of \"apple\"; lastIndexOf should return the index of the last occurrence (4)."), Arguments.of("banana", -1, "Object not present in the list; lastIndexOf should return -1."), Arguments.of(null, 3, "List contains a null element at index 3; lastIndexOf(null) should return 3."));
    }

    @Test
    void testLastIndexOfSingleOccurrence() {
        // GIVEN a list where the element 30 appears only once
        // WHEN we ask for its last index
        int idx = intList.lastIndexOf(30);
        // THEN the index should be the position where it was added (2)
        assertEquals(2, idx, "lastIndexOf should return the correct index for a single occurrence");
    }

    @Test
    void testLastIndexOfMultipleOccurrences() {
        // GIVEN a list where the element 20 appears three times (indices 1, 3, 6)
        // WHEN we ask for its last index
        int idx = intList.lastIndexOf(20);
        // THEN the index should be the last occurrence (6)
        assertEquals(6, idx, "lastIndexOf should return the index of the last occurrence");
    }

    @Test
    void testLastIndexOfElementNotPresent() {
        // GIVEN a list that does not contain the value 99
        // WHEN we ask for its last index
        int idx = intList.lastIndexOf(99);
        // THEN the method should return -1
        assertEquals(-1, idx, "lastIndexOf should return -1 when the element is absent");
    }

    @Test
    void testLastIndexOfNullElement() {
        // GIVEN a list that contains a single null at index 5
        // WHEN we ask for the last index of null
        int idx = intList.lastIndexOf(null);
        // THEN the index should be 5
        assertEquals(5, idx, "lastIndexOf should correctly handle null values");
    }

    @Test
    void testLastIndexOfWithWrappedListConstructor() {
        // GIVEN a ConcurrentList that wraps an existing mutable list
        // WHEN we call lastIndexOf on an element that exists in the backing list
        int idx = wrappedStringList.lastIndexOf("beta");
        // THEN the index should match the backing list's index (1)
        assertEquals(1, idx, "lastIndexOf should work correctly when the list wraps an external list");
    }

    @Test
    void testLastIndexOfAfterModifications() {
        // GIVEN a list where we remove elements after initial population
        // removes first occurrence at index 1
        intList.remove(Integer.valueOf(20));
        // WHEN we ask for the last index of 20
        int idx = intList.lastIndexOf(20);
        // THEN the last occurrence should now be at index 5 (original index 6 shifted left)
        assertEquals(5, idx, "lastIndexOf should reflect the current state after modifications");
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicFunctionalityProvider")
    void testLastIndexOfBasicFunctionality(String search, int expected, String description) {
        // GIVEN a list with known elements (including duplicates and a null)
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("apple");
        list.add("orange");
        list.add("apple");
        list.add(null);
        list.add("apple");
        // WHEN we query lastIndexOf
        int actual = list.lastIndexOf(search);
        // THEN the result matches the expectation
        assertEquals(expected, actual, description);
    }

    @Test
    void testLastIndexOfEmptyList() {
        // GIVEN an empty ConcurrentList
        ConcurrentList<String> empty = new ConcurrentList<>();
        // WHEN we search for any value
        int idx = empty.lastIndexOf("anyValue");
        // THEN -1 is returned
        assertEquals(-1, idx, "When the list is empty, lastIndexOf should always return -1");
    }

    @Test
    void testLastIndexOfNullBackingList() {
        // GIVEN a ConcurrentList constructed with a null backing list
        ConcurrentList<String> listWithNullBacking = new ConcurrentList<>(null);
        // WHEN we invoke lastIndexOf we expect a NullPointerException to propagate
        assertThrows(NullPointerException.class, () -> listWithNullBacking.lastIndexOf("anyValue"), "Calling lastIndexOf on a list with a null internal list should throw NullPointerException");
    }

    @Test
    void testLastIndexOfUnsupportedOperationInBackingList(@Mock List<String> mockList) {
        // GIVEN a mocked backing list whose lastIndexOf throws UnsupportedOperationException
        when(mockList.lastIndexOf(any())).thenThrow(new UnsupportedOperationException("not supported"));
        ConcurrentList<String> wrapper = new ConcurrentList<>(mockList);
        // WHEN we call lastIndexOf we expect the same exception to propagate
        assertThrows(UnsupportedOperationException.class, () -> wrapper.lastIndexOf("test"), "UnsupportedOperationException from the backing list should propagate");
    }

    @Test
    void testLastIndexOfReadLockAcquisitionFailure() throws Exception {
        // GIVEN a normal ConcurrentList
        ConcurrentList<Integer> list = new ConcurrentList<>();
        list.add(1);
        list.add(2);
        // Mock a ReadWriteLock whose readLock().lock() throws IllegalStateException
        ReadWriteLock mockLock = mock(ReadWriteLock.class);
        Lock mockReadLock = mock(Lock.class);
        when(mockLock.readLock()).thenReturn(mockReadLock);
        doThrow(new IllegalStateException("cannot acquire read lock")).when(mockReadLock).lock();
        // Use reflection to replace the internal lock field (name guessed as "rwLock")
        Field lockField = Arrays.stream(ConcurrentList.class.getDeclaredFields()).filter(f -> ReadWriteLock.class.isAssignableFrom(f.getType())).findFirst().orElseThrow(() -> new IllegalStateException("ReadWriteLock field not found"));
        lockField.setAccessible(true);
        lockField.set(list, mockLock);
        // WHEN we call lastIndexOf we expect IllegalStateException to propagate
        assertThrows(IllegalStateException.class, () -> list.lastIndexOf(1), "If the read lock cannot be acquired, lastIndexOf should propagate IllegalStateException");
    }
}
