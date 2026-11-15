package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_add_Test {

    /**
     * List under test – created fresh for each test method
     */
    private ConcurrentList<Integer> list;

    /**
     * A mock backing list used to verify delegation when the wrapper constructor is employed
     */
    @Mock
    private List<Integer> mockBackingList;

    @BeforeEach
    void setupBeforeEach() {
        // Default (stand‑alone) list for the majority of tests
        list = new ConcurrentList<>();
        // When we need a wrapper, we will instantiate it inside the test method
        // using the already‑injected mockBackingList.
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps GC when many tests run
        list = null;
        // Mockito will reset the mock automatically between tests
    }

    @Test
    void addSingleElement_returnsTrueAndIncreasesSize() {
        // given
        Integer element = 42;
        // when
        boolean result = list.add(element);
        // then
        assertTrue(result, "add(E) should return true when element is added");
        assertEquals(1, list.size(), "Size should be 1 after adding a single element");
        assertEquals(element, list.get(0), "The added element should be retrievable at index 0");
    }

    @Test
    void addMultipleElements_accumulatesCorrectly() {
        // given
        Integer first = 1;
        Integer second = 2;
        // when
        list.add(first);
        list.add(second);
        // then
        assertEquals(2, list.size(), "Size should reflect the number of added elements");
        assertEquals(first, list.get(0), "First element should be at index 0");
        assertEquals(second, list.get(1), "Second element should be at index 1");
    }

    @Test
    void addNullElement_isSupportedWhenBackingListAllowsNull() {
        // given – use the wrapper constructor with a real ArrayList that permits nulls
        List<Integer> backing = new ArrayList<>();
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backing);
        // when
        boolean result = wrapped.add(null);
        // then
        assertTrue(result, "add(null) should return true when underlying list permits nulls");
        assertEquals(1, wrapped.size(), "Size should be 1 after adding null");
        assertTrue(wrapped.contains(null), "List should contain the null element");
    }

    @Test
    void addDelegatesToBackingList_whenWrapped() {
        // given – the mock is already injected by Mockito
        // make the mock behave like a normal list for the add operation
        when(mockBackingList.add(anyInt())).thenReturn(true);
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(mockBackingList);
        Integer element = 99;
        // when
        boolean result = wrapped.add(element);
        // then
        assertTrue(result, "add(E) should return the value supplied by the backing list");
        verify(mockBackingList, times(1)).add(element);
    }

    @Test
    void addThrowsIllegalArgumentException_whenWrapperIsCreatedWithNullList() {
        // given / when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<Integer>(null), "Constructor should reject a null backing list");
        assertEquals("List cannot be null", ex.getMessage(), "Exception message should match the contract");
    }

    @Test
    void addDuplicateValue_allowsDuplicates() {
        // given
        String duplicate = "first";
        ConcurrentList<String> stringList = new ConcurrentList<>();
        // first occurrence
        stringList.add(duplicate);
        // when
        // second occurrence
        boolean result = stringList.add(duplicate);
        // then
        assertTrue(result, "Adding a duplicate element should return true");
        assertEquals(2, stringList.size(), "Size should increase even when element is a duplicate");
        assertEquals(duplicate, stringList.get(0));
        assertEquals(duplicate, stringList.get(1));
    }

    @Test
    void addAfterClear_resetsSizeAndContainsElement() {
        // given
        ConcurrentList<String> stringList = new ConcurrentList<>();
        stringList.add("old");
        stringList.clear();
        // when
        boolean result = stringList.add("new");
        // then
        assertTrue(result, "add after clear should return true");
        assertEquals(1, stringList.size(), "Size should be 1 after clear + add");
        assertEquals("new", stringList.get(0));
    }

    @Test
    void addManyPriorAdds_dynamicResizing() {
        // given
        ConcurrentList<String> stringList = new ConcurrentList<>();
        for (int i = 0; i < 1000; i++) {
            stringList.add("elem-" + i);
        }
        assertEquals(1000, stringList.size(), "Pre‑condition: list should contain 1000 elements");
        // when
        boolean result = stringList.add("elem-1000");
        // then
        assertTrue(result, "Adding after many prior adds should return true");
        assertEquals(1001, stringList.size(), "Size should reflect the newly added element");
        assertEquals("elem-1000", stringList.get(1000));
    }

    @Test
    void addWhileAnotherThreadReads_doesNotThrowConcurrentModificationException() throws Exception {
        // given
        ConcurrentList<Integer> shared = new ConcurrentList<>();
        // Populate with some initial data
        for (int i = 0; i < 10; i++) {
            shared.add(i);
        }
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        AtomicReference<Exception> readerException = new AtomicReference<>();
        // Reader thread repeatedly calls size()
        Thread reader = new Thread(() -> {
            try {
                // wait until main thread is ready to add
                startLatch.await();
                for (int i = 0; i < 1000; i++) {
                    // should never throw CME
                    shared.size();
                }
            } catch (Exception e) {
                readerException.set(e);
            } finally {
                doneLatch.countDown();
            }
        });
        reader.start();
        // when
        // let the reader run
        startLatch.countDown();
        // concurrent write
        boolean result = shared.add(999);
        // then
        // ensure reader finished
        doneLatch.await();
        assertTrue(result, "add should succeed while another thread reads");
        assertNull(readerException.get(), "Reader thread must not encounter any exception");
        assertTrue(shared.contains(999), "Element added during concurrent read must be present");
    }

    @Test
    void addToUnmodifiableBackingList_propagatesUnsupportedOperationException() {
        // given
        List<Integer> unmodifiable = Collections.unmodifiableList(new ArrayList<>());
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(unmodifiable);
        // when / then
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> wrapped.add(1), "Adding to an unmodifiable backing list should propagate UnsupportedOperationException");
        // message is implementation‑dependent, so we only assert the type
    }

    @Test
    void addWhenInternalBackingListIsNull_throwsNullPointerException() throws Exception {
        // given
        ConcurrentList<String> target = new ConcurrentList<>();
        // Use reflection to nullify the private 'list' field
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(target, null);
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> target.add("test"), "Invoking add after the internal list is null should throw NPE");
        // No specific message contract
    }

    @Test
    void addWhenBackingListThrowsCustomRuntimeException_propagatesIt() {
        // given a custom List implementation that always throws IllegalStateException on add
        List<String> faulty = new List<String>() {

            @Override
            public int size() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public boolean contains(Object o) {
                return false;
            }

            @Override
            public Iterator<String> iterator() {
                return Collections.emptyIterator();
            }

            @Override
            public Object[] toArray() {
                return new Object[0];
            }

            @Override
            public <T> T[] toArray(T[] a) {
                return a;
            }

            @Override
            public boolean add(String s) {
                throw new IllegalStateException("deliberate failure");
            }

            @Override
            public boolean remove(Object o) {
                return false;
            }

            @Override
            public boolean containsAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean addAll(Collection<? extends String> c) {
                return false;
            }

            @Override
            public boolean addAll(int index, Collection<? extends String> c) {
                return false;
            }

            @Override
            public boolean removeAll(Collection<?> c) {
                return false;
            }

            @Override
            public boolean retainAll(Collection<?> c) {
                return false;
            }

            @Override
            public void clear() {
            }

            @Override
            public String get(int index) {
                return null;
            }

            @Override
            public String set(int index, String element) {
                return null;
            }

            @Override
            public void add(int index, String element) {
            }

            @Override
            public String remove(int index) {
                return null;
            }

            @Override
            public int indexOf(Object o) {
                return -1;
            }

            @Override
            public int lastIndexOf(Object o) {
                return -1;
            }

            @Override
            public ListIterator<String> listIterator() {
                return Collections.emptyListIterator();
            }

            @Override
            public ListIterator<String> listIterator(int index) {
                return Collections.emptyListIterator();
            }

            @Override
            public List<String> subList(int fromIndex, int toIndex) {
                return Collections.emptyList();
            }
        };
        ConcurrentList<String> wrapped = new ConcurrentList<>(faulty);
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> wrapped.add("trigger"), "Exception from backing list should be propagated");
        assertEquals("deliberate failure", ex.getMessage(), "Exception message should match the one thrown by the backing list");
    }

    @ParameterizedTest(name = "{index} => element={0}")
    @ValueSource(strings = { "first", "" })
    void addNormalBehavior_variousValues(String value) {
        // given
        ConcurrentList<String> stringList = new ConcurrentList<>();
        // when
        boolean result = stringList.add(value);
        // then
        assertTrue(result, "add should return true for value: " + value);
        assertEquals(1, stringList.size());
        assertEquals(value, stringList.get(0));
    }

    @Test
    void addNullNormalBehavior() {
        // given
        ConcurrentList<String> stringList = new ConcurrentList<>();
        // when
        boolean result = stringList.add(null);
        // then
        assertTrue(result, "add(null) should return true when underlying list permits nulls");
        assertEquals(1, stringList.size());
        assertNull(stringList.get(0));
    }
}
