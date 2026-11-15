package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toArray_Test {

    /**
     * Instance used for the “stand‑alone” tests (no external dependencies).
     */
    private ConcurrentList<Integer> list;

    /**
     * Mocked backing list used to verify delegation when the wrapper constructor is employed.
     */
    @Mock
    private List<Integer> mockedBackingList;

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test ensures isolation.
        list = new ConcurrentList<>();
    }

    @Test
    void toArray_EmptyList_ReturnsEmptyArray() {
        Object[] array = list.toArray();
        assertNotNull(array, "Resulting array must not be null");
        assertEquals(0, array.length, "Array length should be zero for an empty list");
    }

    @Test
    void toArray_PopulatedList_ReturnsArrayWithSameElementsInOrder() {
        list.add(10);
        list.add(20);
        list.add(30);
        Object[] array = list.toArray();
        assertNotNull(array, "Resulting array must not be null");
        assertEquals(3, array.length, "Array length should match list size");
        assertArrayEquals(new Object[] { 10, 20, 30 }, array, "Array contents must reflect the list order and values");
    }

    @Test
    void toArray_ListContainsNulls_HandlesNullElementsCorrectly() {
        list.add(null);
        list.add(42);
        list.add(null);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[] { null, 42, null }, array, "Array must preserve null elements exactly as they appear in the list");
    }

    @Test
    void toArray_Generic_WithExactSizeArray_ReturnsSameArrayInstance() {
        list.add(1);
        list.add(2);
        list.add(3);
        Integer[] supplied = new Integer[3];
        Integer[] result = list.toArray(supplied);
        // According to List contract, when the supplied array is large enough,
        // the same instance must be returned.
        assertSame(supplied, result, "When the supplied array fits, the same array instance should be returned");
        assertArrayEquals(new Integer[] { 1, 2, 3 }, result);
    }

    @Test
    void toArray_Generic_WithSmallerArray_ReturnsNewArray() {
        list.add(7);
        list.add(8);
        // too small
        Integer[] supplied = new Integer[1];
        Integer[] result = list.toArray(supplied);
        // Must be a new array (different instance) with the correct size.
        assertNotSame(supplied, result, "A new array should be allocated when the supplied one is too small");
        assertEquals(2, result.length);
        assertArrayEquals(new Integer[] { 7, 8 }, result);
    }

    @Test
    void toArray_WrapperMode_DelegatesToBackingList() {
        // Arrange – make the mock behave like a normal list for toArray()
        when(mockedBackingList.toArray()).thenReturn(new Object[] { "a", "b", "c" });
        // Use the wrapper constructor
        ConcurrentList<String> wrapper = new ConcurrentList<>(mockedBackingList);
        // Act
        Object[] result = wrapper.toArray();
        // Assert – result must be exactly what the mock returned
        assertArrayEquals(new Object[] { "a", "b", "c" }, result);
        // Verify that the underlying list's toArray() was invoked exactly once
        verify(mockedBackingList, times(1)).toArray();
    }

    @Test
    void toArray_Generic_WrapperMode_DelegatesCorrectly() {
        // Arrange – generic overload should also be delegated
        String[] supplied = new String[0];
        when(mockedBackingList.toArray(supplied)).thenReturn(new String[] { "x", "y" });
        ConcurrentList<String> wrapper = new ConcurrentList<>(mockedBackingList);
        // Act
        String[] result = wrapper.toArray(supplied);
        // Assert
        assertArrayEquals(new String[] { "x", "y" }, result);
        verify(mockedBackingList, times(1)).toArray(supplied);
    }

    @Test
    void toArray_ModifyingReturnedArrayDoesNotAffectList() {
        list.add(1);
        list.add(2);
        Object[] array = list.toArray();
        // Mutate the returned array
        array[0] = 99;
        // The list must still contain the original values
        assertEquals(1, list.get(0));
        assertEquals(2, list.get(1));
    }

    @Test
    void toArray_TwoCallsReturnDistinctArrayInstances() {
        list.add(5);
        list.add(6);
        Object[] first = list.toArray();
        Object[] second = list.toArray();
        assertNotSame(first, second, "Each call must return a fresh array instance");
        // Mutate first and verify second is unaffected
        first[0] = 100;
        assertEquals(5, second[0]);
    }

    @Test
    void toArray_SingletonList_ReturnsArrayOfLengthOne() {
        list.add(777);
        Object[] array = list.toArray();
        assertEquals(1, array.length);
        assertEquals(777, array[0]);
    }

    @Test
    void toArray_AllNullElements_ReturnsArrayWithAllNulls() {
        list.add(null);
        list.add(null);
        list.add(null);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[] { null, null, null }, array);
    }

    @Test
    void toArray_LargeList_PreservesOrderAndSize() {
        int size = 10_000;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        Object[] array = list.toArray();
        assertEquals(size, array.length);
        for (int i = 0; i < size; i++) {
            assertEquals(i, array[i]);
        }
    }

    @Test
    void toArray_AfterAdditionsReflectCurrentState() {
        list.add(1);
        list.add(2);
        list.add(3);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[] { 1, 2, 3 }, array);
    }

    @Test
    void toArray_AfterRemovalReflectsRemovedElement() {
        list.add(10);
        list.add(20);
        list.add(30);
        // remove element at index 1 (value 20)
        list.remove(1);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[] { 10, 30 }, array);
    }

    @Test
    void toArray_AfterClearReturnsEmptyArray() {
        list.add(1);
        list.add(2);
        list.clear();
        Object[] array = list.toArray();
        assertEquals(0, array.length);
    }

    @Test
    void toArray_WithDuplicates_PreservesAllOccurrences() {
        list.add(5);
        list.add(5);
        list.add(7);
        Object[] array = list.toArray();
        assertArrayEquals(new Object[] { 5, 5, 7 }, array);
    }

    @Test
    void toArray_ConcurrentWriterDoesNotThrowCME() throws Exception {
        // Fill the list with initial data.
        for (int i = 0; i < 100; i++) {
            list.add(i);
        }
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch writerDone = new CountDownLatch(1);
        // Writer thread that attempts to add while toArray() is executing.
        Thread writer = new Thread(() -> {
            try {
                // wait until the main thread starts toArray()
                startLatch.await();
                // this will block until the read lock is released
                list.add(9999);
            } catch (InterruptedException ignored) {
            } finally {
                writerDone.countDown();
            }
        });
        writer.start();
        // Main thread: start toArray() while writer is ready.
        startLatch.countDown();
        // should not throw CME
        Object[] result = list.toArray();
        // Ensure writer finishes after toArray()
        writerDone.await(2, TimeUnit.SECONDS);
        // The result must contain only the original 100 elements (no 9999)
        assertEquals(100, result.length);
        assertFalse(Arrays.asList(result).contains(9999));
    }

    @Test
    void toArray_NullBackingList_ThrowsNullPointerException() throws Exception {
        // Create a fresh instance via the default constructor.
        ConcurrentList<Integer> broken = new ConcurrentList<>();
        // Use reflection to set the private 'list' field to null.
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(broken, null);
        assertThrows(NullPointerException.class, broken::toArray, "Calling toArray() with a null internal list should throw NPE");
    }

    @Test
    void toArray_ReadLockFailure_PropagatesIllegalStateException() throws Exception {
        // Create a spy so we can replace the internal lock via reflection.
        ConcurrentList<Integer> spy = new ConcurrentList<>();
        // Find the ReadWriteLock field (its name is not part of the public API).
        Field lockField = null;
        for (Field f : ConcurrentList.class.getDeclaredFields()) {
            if (ReadWriteLock.class.isAssignableFrom(f.getType())) {
                lockField = f;
                break;
            }
        }
        assertNotNull(lockField, "Expected a ReadWriteLock field in ConcurrentList");
        // Mock a ReadWriteLock whose readLock().lock() throws IllegalStateException.
        ReadWriteLock mockedLock = mock(ReadWriteLock.class);
        Lock mockedReadLock = mock(Lock.class);
        doThrow(new IllegalStateException("cannot acquire read lock")).when(mockedReadLock).lock();
        when(mockedLock.readLock()).thenReturn(mockedReadLock);
        when(mockedLock.writeLock()).thenReturn(mock(Lock.class));
        // Inject the mocked lock.
        lockField.setAccessible(true);
        lockField.set(spy, mockedLock);
        // The call should propagate the IllegalStateException.
        assertThrows(IllegalStateException.class, spy::toArray, "If acquiring the read lock fails, toArray() must propagate the exception");
    }

    @Test
    void toArray_UnderlyingListThrowsRuntimeException_PropagatesIt() {
        // Create a list implementation that throws from toArray().
        List<Integer> badList = new AbstractList<>() {

            @Override
            public Integer get(int index) {
                return null;
            }

            @Override
            public int size() {
                return 1;
            }

            @Override
            public Object[] toArray() {
                throw new RuntimeException("boom");
            }
        };
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(badList);
        RuntimeException ex = assertThrows(RuntimeException.class, wrapper::toArray);
        assertEquals("boom", ex.getMessage());
    }
}
