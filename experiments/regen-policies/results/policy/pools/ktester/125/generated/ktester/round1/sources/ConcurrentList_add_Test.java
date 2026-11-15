package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_add_Test {

    /**
     * Instance used for the “stand‑alone” tests.
     */
    private ConcurrentList<Integer> standAloneList;

    /**
     * Backing list used for the “wrapper” tests.
     */
    @Mock
    private List<Integer> mockBackingList;

    /**
     * Instance created with the mocked backing list.
     */
    private ConcurrentList<Integer> wrapperList;

    /**
     * Initialise a fresh test fixture before each test method.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Initialise the stand‑alone list (uses its own internal ArrayList).
        standAloneList = new ConcurrentList<>();
        // Initialise the wrapper list – the mock is injected by MockitoExtension.
        // Mockito creates a fresh mock for every test method, so we can safely reuse it.
        wrapperList = new ConcurrentList<>(mockBackingList);
        standAloneList = new ConcurrentList<>();
        wrapperList = new ConcurrentList<>(mockBackingList);
    }

    /**
     * Clean up after each test method.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references – helps the garbage collector and makes
        // the intent of the test fixture clear.
        standAloneList = null;
        wrapperList = null;
        standAloneList = null;
        wrapperList = null;
    }

    /**
     * Utility to replace the private {@code list} field of a {@link ConcurrentList}
     * instance with a custom implementation.
     */
    private void setInternalList(ConcurrentList<?> target, List<?> replacement) throws Exception {
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(target, replacement);
    }

    @Test
    void addSingleElement_Standalone() {
        assertTrue(standAloneList.isEmpty(), "List should start empty");
        boolean result = standAloneList.add(42);
        assertTrue(result, "add(E) must return true on successful insertion");
        assertEquals(1, standAloneList.size(), "Size should be 1 after one add");
        assertTrue(standAloneList.contains(42), "List must contain the added element");
    }

    @Test
    void addMultipleElements_Standalone() {
        standAloneList.add(1);
        standAloneList.add(2);
        standAloneList.add(3);
        assertEquals(3, standAloneList.size(), "Size should reflect the number of added elements");
        assertTrue(standAloneList.containsAll(List.of(1, 2, 3)), "All added elements must be present");
    }

    @Test
    void addNullElement_Standalone() {
        // ArrayList (the default backing list) permits null values.
        boolean result = standAloneList.add(null);
        assertTrue(result, "add(null) should return true");
        assertEquals(1, standAloneList.size(), "Size should be 1 after adding null");
        assertTrue(standAloneList.contains(null), "List must contain the null element");
    }

    @Test
    void addSingleElement_Wrapper_DelegatesToBackingList() {
        // Arrange – define the behaviour of the mock for the add operation.
        when(mockBackingList.add(7)).thenReturn(true);
        // Act
        boolean result = wrapperList.add(7);
        // Assert
        assertTrue(result, "add(E) should return the value supplied by the backing list");
        verify(mockBackingList, times(1)).add(7);
    }

    @Test
    void addNullElement_Wrapper_DelegatesToBackingList() {
        // Arrange – the mock should accept a null argument.
        when(mockBackingList.add(null)).thenReturn(true);
        // Act
        boolean result = wrapperList.add(null);
        // Assert
        assertTrue(result, "add(null) must forward the call and return the backing list's result");
        verify(mockBackingList, times(1)).add(null);
    }

    @Test
    void addElement_Wrapper_WhenBackingListRejects() {
        // Simulate a backing list that refuses the addition (e.g., a fixed‑size list).
        when(mockBackingList.add(99)).thenReturn(false);
        boolean result = wrapperList.add(99);
        assertFalse(result, "add(E) must propagate the false result from the backing list");
        verify(mockBackingList, times(1)).add(99);
    }

    @Test
    void addElement_Wrapper_ThrowsExceptionFromBackingList() {
        // Simulate a backing list that throws an exception (e.g., UnsupportedOperationException).
        when(mockBackingList.add(5)).thenThrow(new UnsupportedOperationException("read‑only list"));
        assertThrows(UnsupportedOperationException.class, () -> wrapperList.add(5), "add(E) should propagate exceptions thrown by the backing list");
        verify(mockBackingList, times(1)).add(5);
    }

    /**
     * Normal behavior – successful additions (parameterized).
     */
    @ParameterizedTest(name = "[{index}] add({0}) → true")
    @CsvSource({ "'alpha', true", "null, true" })
    void normalAdditions(String element, boolean expected) {
        boolean result = standAloneList.add(element);
        assertEquals(expected, result);
        assertTrue(standAloneList.contains(element));
    }

    /**
     * Duplicate element addition.
     */
    @Test
    void duplicateElementAddition() {
        assertTrue(standAloneList.add("duplicate"));
        // second insertion
        assertTrue(standAloneList.add("duplicate"));
        assertEquals(2, standAloneList.size());
        // both entries should be present
        assertEquals("duplicate", standAloneList.get(0));
        assertEquals("duplicate", standAloneList.get(1));
    }

    /**
     * Sequential additions to a non‑empty list.
     */
    @Test
    void sequentialAdditions() {
        assertTrue(standAloneList.add("first"));
        assertTrue(standAloneList.add("second"));
        assertEquals(2, standAloneList.size());
        assertTrue(standAloneList.containsAll(List.of("first", "second")));
    }

    /**
     * Add after clear.
     */
    @Test
    void addAfterClear() {
        standAloneList.add("temp");
        standAloneList.clear();
        assertTrue(standAloneList.isEmpty());
        assertTrue(standAloneList.add("afterClear"));
        assertEquals(1, standAloneList.size());
        assertTrue(standAloneList.contains("afterClear"));
    }

    /**
     * Underlying list returns false – we inject a custom List implementation.
     */
    @Test
    void underlyingListReturnsFalse() throws Exception {
        // custom list that always returns false on add()
        List<String> falseList = new AbstractList<>() {

            @Override
            public String get(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String e) {
                return false;
            }
        };
        setInternalList(standAloneList, falseList);
        assertFalse(standAloneList.add("ignored"));
    }

    /**
     * Concurrent additions – 10 threads each adding a distinct element.
     */
    @Test
    void concurrentAdditions() throws Exception {
        int threadCount = 10;
        ExecutorService exec = Executors.newFixedThreadPool(threadCount);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final String value = "thread-" + i;
            futures.add(exec.submit(() -> standAloneList.add(value)));
        }
        exec.shutdown();
        assertTrue(exec.awaitTermination(5, TimeUnit.SECONDS));
        // all futures must be true
        for (Future<Boolean> f : futures) {
            assertTrue(f.get());
        }
        assertEquals(threadCount, standAloneList.size());
        for (int i = 0; i < threadCount; i++) {
            assertTrue(standAloneList.contains("thread-" + i));
        }
    }

    /**
     * Large number of sequential additions (boundary test).
     */
    @Test
    void largeSequentialAdditions() {
        int total = 10_000;
        for (int i = 0; i < total; i++) {
            assertTrue(standAloneList.add("element-" + i));
        }
        assertEquals(total, standAloneList.size());
        // spot‑check a few entries
        assertEquals("element-0", standAloneList.get(0));
        assertEquals("element-9999", standAloneList.get(total - 1));
    }

    /**
     * Null underlying list – simulate via reflection.
     */
    @Test
    void nullUnderlyingListThrowsNPE() throws Exception {
        // Force the private 'list' field to null
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(standAloneList, null);
        assertThrows(NullPointerException.class, () -> standAloneList.add("any"), "Calling add when internal list is null should throw NPE");
    }

    /**
     * Exception from writeOperation lock acquisition.
     */
    @Test
    void lockAcquisitionThrowsIllegalStateException() throws Exception {
        // Mock a ReadWriteLock whose writeLock throws IllegalStateException on lock()
        ReadWriteLock mockLock = mock(ReadWriteLock.class);
        Lock mockWriteLock = mock(Lock.class);
        doThrow(new IllegalStateException("lock acquisition failed")).when(mockWriteLock).lock();
        when(mockLock.writeLock()).thenReturn(mockWriteLock);
        // Inject the mock lock via reflection (field name guessed as 'lock')
        Field lockField = ConcurrentList.class.getDeclaredField("lock");
        lockField.setAccessible(true);
        lockField.set(standAloneList, mockLock);
        assertThrows(IllegalStateException.class, () -> standAloneList.add("test"), "add should propagate IllegalStateException from lock acquisition");
    }

    /**
     * Supplier (underlying list) throws RuntimeException.
     */
    @Test
    void underlyingListThrowsRuntimeException() throws Exception {
        List<String> explodingList = new AbstractList<>() {

            @Override
            public String get(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String e) {
                throw new RuntimeException("boom");
            }
        };
        setInternalList(standAloneList, explodingList);
        assertThrows(RuntimeException.class, () -> standAloneList.add("trigger"), "RuntimeException from underlying list should propagate");
    }

    /**
     * Concurrent modification during write lock – simulate by throwing CME from add().
     */
    @Test
    void concurrentModificationDuringWriteLock() throws Exception {
        List<String> cmeList = new AbstractList<>() {

            private boolean firstCall = true;

            @Override
            public String get(int index) {
                throw new IndexOutOfBoundsException();
            }

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean add(String e) {
                if (firstCall) {
                    firstCall = false;
                    // first add succeeds
                    return true;
                }
                throw new ConcurrentModificationException("simulated conflict");
            }
        };
        setInternalList(standAloneList, cmeList);
        // first add succeeds
        assertTrue(standAloneList.add("first"));
        // second add should see the simulated CME
        assertThrows(ConcurrentModificationException.class, () -> standAloneList.add("conflict"));
    }
}
