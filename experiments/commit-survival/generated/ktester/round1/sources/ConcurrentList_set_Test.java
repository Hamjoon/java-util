package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_set_Test {

    private ConcurrentList<Integer> list;

    // used for the constructor that wraps an existing list
    private List<Integer> backingList;

    @BeforeEach
    void setupBeforeEach() {
        // fresh list for each test
        list = new ConcurrentList<>();
        // a separate backing list for tests that need to verify the wrapper behaviour
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // help GC – not strictly required but keeps the pattern consistent
        list = null;
        backingList = null;
    }

    /**
     * Provides normal (non‑exceptional) test data:
     * <ul>
     *   <li>replace first element</li>
     *   <li>replace last element</li>
     *   <li>replace with {@code null}</li>
     * </ul>
     */
    private static Stream<Arguments> normalSetProvider() {
        return Stream.of(Arguments.of(0, "first", "originalFirst"), Arguments.of(2, "last", "originalLast"), Arguments.of(1, null, "originalSecond"));
    }

    private static Stream<Arguments> exceptionSetProvider() {
        return Stream.of(// negative index
        Arguments.of(-1, 99, IndexOutOfBoundsException.class, "Negative index"), // index == size
        Arguments.of(3, 99, IndexOutOfBoundsException.class, "Index equal to size"), // immutable list
        Arguments.of(0, 99, UnsupportedOperationException.class, "Immutable list"), // custom list throws IllegalArgumentException
        Arguments.of(0, 99, IllegalArgumentException.class, "Custom list throws IAEx"), // custom list throws ClassCastException
        Arguments.of(0, 99, ClassCastException.class, "Custom list throws CCE"), // custom list throws NullPointerException on null element
        Arguments.of(0, null, NullPointerException.class, "Custom list throws NPE on null"));
    }

    @Test
    void setReturnsPreviousElement() {
        // given
        list.add(10);
        list.add(20);
        list.add(30);
        // when
        Integer previous = list.set(1, 99);
        // then
        assertEquals(20, previous, "set should return the element previously at the index");
    }

    @Test
    void setUpdatesElementAtIndex() {
        // given
        list.add(1);
        list.add(2);
        list.add(3);
        // when
        list.set(2, 42);
        // then
        assertEquals(42, list.get(2), "Element at index 2 should be replaced with the new value");
    }

    @Test
    void setThrowsIndexOutOfBoundsWhenIndexIsNegative() {
        // given
        list.add(5);
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(-1, 100), "Negative index should cause IndexOutOfBoundsException");
    }

    @Test
    void setThrowsIndexOutOfBoundsWhenIndexIsEqualToSize() {
        // given
        list.add(5);
        list.add(6);
        // when / then
        assertThrows(IndexOutOfBoundsException.class, () -> list.set(2, 100), "Index equal to size is out of bounds for set operation");
    }

    @Test
    void setWorksWhenConcurrentListWrapsExistingList() {
        // given: a backing list that we will wrap
        backingList.add(100);
        backingList.add(200);
        backingList.add(300);
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        // when
        Integer old = wrapped.set(0, 999);
        // then
        assertEquals(100, old, "set should return the original element from the backing list");
        assertEquals(999, wrapped.get(0), "wrapped list should reflect the new value");
        assertEquals(999, backingList.get(0), "underlying list must also be updated");
    }

    @Test
    void setDoesNotAffectOtherIndices() {
        // given
        list.add(11);
        list.add(22);
        list.add(33);
        list.add(44);
        // when
        list.set(2, 777);
        // then
        assertEquals(11, list.get(0));
        assertEquals(22, list.get(1));
        assertEquals(777, list.get(2));
        assertEquals(44, list.get(3));
    }

    @ParameterizedTest(name = "[{index}] set(index={0}, element={1}) → returns {2}")
    @MethodSource("normalSetProvider")
    void setNormalBehaviour(int index, Integer newValue, String expectedPrev) {
        // Prepare a backing list with known values
        List<Integer> backing = new ArrayList<>(Arrays.asList(10, 20, 30));
        // Remember the element that should be returned
        Integer expected = backing.get(index);
        // Wrap it
        ConcurrentList<Integer> cl = new ConcurrentList<>(backing);
        // Perform the operation
        Integer previous = cl.set(index, newValue);
        // Verify contract
        assertEquals(expected, previous, "set should return the previous element");
        assertEquals(newValue, cl.get(index), "element at the index must be replaced");
        assertEquals(newValue, backing.get(index), "underlying list must reflect the change");
    }

    @ParameterizedTest(name = "[{index}] set throws {3}")
    @MethodSource("exceptionSetProvider")
    void setExceptionalBehaviour(int index, Integer element, Class<? extends Throwable> expectedException, String description) throws Exception {
        ConcurrentList<Integer> cl;
        switch(description) {
            case "Immutable list":
                List<Integer> immutable = Collections.unmodifiableList(new ArrayList<>(Arrays.asList(1, 2, 3)));
                cl = new ConcurrentList<>(immutable);
                break;
            case "Custom list throws IAEx":
                @SuppressWarnings("unchecked")
                List<Integer> illegalList = mock(List.class);
                when(illegalList.size()).thenReturn(3);
                when(illegalList.set(anyInt(), any())).thenThrow(new IllegalArgumentException("bad value"));
                cl = new ConcurrentList<>(illegalList);
                break;
            case "Custom list throws CCE":
                @SuppressWarnings("unchecked")
                List<Integer> classCastList = mock(List.class);
                when(classCastList.size()).thenReturn(3);
                when(classCastList.set(anyInt(), any())).thenThrow(new ClassCastException("bad cast"));
                cl = new ConcurrentList<>(classCastList);
                break;
            case "Custom list throws NPE on null":
                @SuppressWarnings("unchecked")
                List<Integer> nullRejectingList = mock(List.class);
                when(nullRejectingList.size()).thenReturn(3);
                when(nullRejectingList.set(anyInt(), isNull())).thenThrow(new NullPointerException("null not allowed"));
                cl = new ConcurrentList<>(nullRejectingList);
                break;
            default:
                // default to a normal list for the index‑out‑of‑bounds cases
                cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
                break;
        }
        // The operation should propagate the expected exception
        assertThrows(expectedException, () -> cl.set(index, element), description);
    }

    @Test
    void setWithNullUnderlyingListThrowsNPE() throws Exception {
        // Create a normal ConcurrentList then replace its internal list with null via reflection
        ConcurrentList<Integer> cl = new ConcurrentList<>();
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        // now the internal list is null
        listField.set(cl, null);
        assertThrows(NullPointerException.class, () -> cl.set(0, 42), "Calling set when the internal list is null should throw NPE");
    }

    @Test
    void setAcquiresAndReleasesWriteLock(@Mock ReadWriteLock mockLock, @Mock Lock mockWriteLock) throws Exception {
        // Prepare a mock lock that returns a mock write lock
        when(mockLock.writeLock()).thenReturn(mockWriteLock);
        // Create a ConcurrentList with a normal backing list
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>(Arrays.asList(1, 2, 3)));
        // Inject the mock lock into the instance via reflection
        Field lockField = ConcurrentList.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(cl, mockLock);
        // Perform a set – this should trigger lock()/unlock()
        cl.set(1, 99);
        // Verify ordering: lock then unlock
        InOrder inOrder = inOrder(mockWriteLock);
        inOrder.verify(mockWriteLock).lock();
        inOrder.verify(mockWriteLock).unlock();
    }

    @Test
    void setReleasesLockEvenWhenUnderlyingListThrows() throws Exception {
        // Mock list that throws RuntimeException on set
        @SuppressWarnings("unchecked")
        List<Integer> exploding = mock(List.class);
        when(exploding.size()).thenReturn(1);
        when(exploding.set(anyInt(), any())).thenThrow(new RuntimeException("boom"));
        // Mock lock
        ReadWriteLock mockLock = mock(ReadWriteLock.class);
        Lock mockWriteLock = mock(Lock.class);
        when(mockLock.writeLock()).thenReturn(mockWriteLock);
        // Build ConcurrentList with the exploding list
        ConcurrentList<Integer> cl = new ConcurrentList<>(exploding);
        // Inject mock lock
        Field lockField = ConcurrentList.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(cl, mockLock);
        // Expect the RuntimeException to propagate, but the lock must still be released
        assertThrows(RuntimeException.class, () -> cl.set(0, 42));
        // Verify that unlock was called even after the exception
        verify(mockWriteLock).unlock();
    }

    @Test
    void setThenGetDoesNotDeadlock() {
        // Normal list – no mocking needed
        list.add(5);
        list.add(10);
        list.add(15);
        // set should succeed
        list.set(1, 99);
        // get should succeed immediately (no deadlock)
        assertEquals(99, list.get(1), "After set, get must return the new value without deadlock");
    }

    @Test
    void setPropagatesConcurrentModificationException() {
        // Create a list that throws CME when set is called
        @SuppressWarnings("unchecked")
        List<Integer> cmeList = mock(List.class);
        when(cmeList.size()).thenReturn(2);
        when(cmeList.set(anyInt(), any())).thenThrow(new ConcurrentModificationException("concurrent change"));
        ConcurrentList<Integer> cl = new ConcurrentList<>(cmeList);
        assertThrows(ConcurrentModificationException.class, () -> cl.set(0, 123), "ConcurrentModificationException from underlying list must propagate");
    }
}
