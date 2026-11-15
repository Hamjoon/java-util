package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_add_Test {

    /**
     * Fields used for the tests that rely on a mocked backing list **
     */
    @Mock
    private List<Integer> mockedBackingList;

    private ConcurrentList<Integer> listWithMock;

    /**
     * Fields used for the tests that rely on the default constructor **
     */
    private ConcurrentList<Integer> listWithDefaultCtor;

    static class Person {

        final String name;

        final int age;

        Person(String name, int age) {
            this.name = name;
            this.age = age;
        }
    }

    class BadConcurrentList<E> extends ConcurrentList<E> {

        BadConcurrentList(List<E> l) {
            super(l);
        }

        @Override
        protected <T> T writeOperation(Supplier<T> operation) {
            throw new IllegalStateException("lockFail");
        }
    }

    class BadConcurrentList<E> extends ConcurrentList<E> {

        BadConcurrentList(List<E> l) {
            super(l);
        }

        @Override
        protected <T> T writeOperation(Supplier<T> operation) {
            throw new IllegalStateException("lockFail");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Instance that wraps a mocked List – used to verify delegation.
        listWithMock = new ConcurrentList<>(mockedBackingList);
        // Instance that uses the no‑arg constructor – used for functional checks.
        listWithDefaultCtor = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(mockedBackingList);
    }

    private static Stream<Arguments> dummyProvider() {
        // placeholder for potential future parameterized tests
        return Stream.empty();
    }

    @Test
    void add_delegatesToUnderlyingList_andReturnsTrue() {
        // GIVEN – the mocked backing list will report a successful add
        when(mockedBackingList.add(42)).thenReturn(true);
        // WHEN
        boolean result = listWithMock.add(42);
        // THEN
        assertTrue(result, "add(E) should return true when the underlying list returns true");
        verify(mockedBackingList, times(1)).add(42);
    }

    @Test
    void add_delegatesToUnderlyingList_andReturnsFalse() {
        // GIVEN – the mocked backing list reports that the element was not added
        when(mockedBackingList.add(99)).thenReturn(false);
        // WHEN
        boolean result = listWithMock.add(99);
        // THEN
        assertFalse(result, "add(E) should return false when the underlying list returns false");
        verify(mockedBackingList, times(1)).add(99);
    }

    @Test
    void add_nullElement_isHandledByUnderlyingList() {
        // GIVEN – the underlying list accepts null and returns true
        when(mockedBackingList.add(null)).thenReturn(true);
        // WHEN
        boolean result = listWithMock.add(null);
        // THEN
        assertTrue(result, "add(null) should propagate the underlying list's result");
        verify(mockedBackingList, times(1)).add(null);
    }

    @Test
    void add_usingDefaultConstructor_behavesCorrectly() {
        // The default constructor creates an internal ArrayList, so we can test functional behaviour
        assertTrue(listWithDefaultCtor.isEmpty(), "New list should be empty");
        assertTrue(listWithDefaultCtor.add(10), "add should return true for a successful insertion");
        assertEquals(1, listWithDefaultCtor.size(), "Size should be 1 after one add");
        assertEquals(10, listWithDefaultCtor.get(0), "Element at index 0 should be the one added");
        assertTrue(listWithDefaultCtor.add(20));
        assertEquals(2, listWithDefaultCtor.size(), "Size should be 2 after two adds");
        assertEquals(20, listWithDefaultCtor.get(1), "Element at index 1 should be the second added value");
    }

    @Test
    void constructor_withNullList_throwsIllegalArgumentException() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<Integer>(null), "Passing null to the List‑wrapping constructor must throw IAE");
        assertEquals("List cannot be null", ex.getMessage());
    }

    @Test
    void add_normalAdditionEmptyList() {
        // Empty list created via default ctor
        assertTrue(listWithDefaultCtor.add("alpha"));
        assertEquals(1, listWithDefaultCtor.size());
        assertEquals("alpha", listWithDefaultCtor.get(0));
    }

    @Test
    void add_normalAdditionWithExistingElements() {
        List<Integer> backing = new ArrayList<>(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> list = new ConcurrentList<>(backing);
        assertTrue(list.add(42));
        assertEquals(4, list.size());
        assertEquals(42, list.get(3));
    }

    @Test
    void add_nullElementOnDefaultList() {
        assertTrue(listWithDefaultCtor.add(null));
        assertEquals(1, listWithDefaultCtor.size());
        assertNull(listWithDefaultCtor.get(0));
    }

    @Test
    void add_unmodifiableUnderlyingList_throwsUnsupportedOperationException() {
        List<String> unmod = Collections.unmodifiableList(new ArrayList<>());
        ConcurrentList<String> list = new ConcurrentList<>(unmod);
        assertThrows(UnsupportedOperationException.class, () -> list.add("test"));
    }

    @Test
    void add_underlyingListReturnsFalse() {
        List<String> falseList = new AbstractList<String>() {

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
        ConcurrentList<String> list = new ConcurrentList<>(falseList);
        assertFalse(list.add("duplicate"));
        assertEquals(0, list.size());
    }

    @Test
    void add_concurrentAccess_noDeadlock() throws Exception {
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>());
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        AtomicInteger successCount = new AtomicInteger();
        Runnable task = () -> {
            try {
                startLatch.await();
                if (list.add("concurrent")) {
                    successCount.incrementAndGet();
                }
            } catch (Exception e) {
                fail("Unexpected exception: " + e);
            } finally {
                doneLatch.countDown();
            }
        };
        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start();
        t2.start();
        // release both threads
        startLatch.countDown();
        doneLatch.await();
        assertEquals(2, successCount.get(), "Both adds should succeed");
        assertEquals(2, list.size());
        assertTrue(list.contains("concurrent"));
    }

    @Test
    void add_duplicateElementAllowed() {
        List<String> backing = new ArrayList<>(Collections.singletonList("duplicate"));
        ConcurrentList<String> list = new ConcurrentList<>(backing);
        assertTrue(list.add("duplicate"));
        assertEquals(2, list.size());
        assertEquals("duplicate", list.get(0));
        assertEquals("duplicate", list.get(1));
    }

    @Test
    void add_toCopyOnWriteArrayList() {
        CopyOnWriteArrayList<Integer> cow = new CopyOnWriteArrayList<>();
        ConcurrentList<Integer> list = new ConcurrentList<>(cow);
        assertTrue(list.add(123));
        assertEquals(1, list.size());
        assertEquals(123, list.get(0));
        assertTrue(cow.contains(123));
    }

    @Test
    void add_underlyingThrowsRuntimeException_propagates() {
        List<String> badList = new AbstractList<String>() {

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
                throw new IllegalStateException("boom");
            }
        };
        ConcurrentList<String> list = new ConcurrentList<>(badList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.add("boom"));
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void add_underlyingThrowsCheckedExceptionWrappedInRuntime() {
        List<String> weirdList = new AbstractList<String>() {

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
                throw new RuntimeException(new IOException("io"));
            }
        };
        ConcurrentList<String> list = new ConcurrentList<>(weirdList);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.add("checked"));
        assertTrue(ex.getCause() instanceof IOException);
        assertEquals("io", ex.getCause().getMessage());
    }

    @Test
    void add_synchronizedUnderlyingList() {
        List<String> sync = Collections.synchronizedList(new ArrayList<>());
        ConcurrentList<String> list = new ConcurrentList<>(sync);
        assertTrue(list.add("sync"));
        assertEquals(1, list.size());
        assertEquals("sync", list.get(0));
    }

    @Test
    void add_customObjectReferenceEquality() {
        Person alice = new Person("Alice", 30);
        ConcurrentList<Person> list = new ConcurrentList<>(new ArrayList<>());
        assertTrue(list.add(alice));
        assertSame(alice, list.get(0), "Stored reference should be the exact same object");
    }

    @Test
    void add_nullUnderlyingListField_throwsNullPointerException() throws Exception {
        // Create instance via reflection with null internal list
        Constructor<ConcurrentList> ctor = ConcurrentList.class.getDeclaredConstructor(List.class);
        ctor.setAccessible(true);
        ConcurrentList<String> list = ctor.newInstance((List<String>) null);
        // Force the private field to null (defensive, in case ctor checks)
        Field f = ConcurrentList.class.getDeclaredField("list");
        f.setAccessible(true);
        f.set(list, null);
        assertThrows(NullPointerException.class, () -> list.add("any"));
    }

    @Test
    void add_writeLockAcquisitionFailure_propagatesException() {
        // Subclass that overrides writeOperation to throw before invoking supplier
        class BadConcurrentList<E> extends ConcurrentList<E> {

            BadConcurrentList(List<E> l) {
                super(l);
            }

            @Override
            protected <T> T writeOperation(Supplier<T> operation) {
                throw new IllegalStateException("lockFail");
            }
        }
        BadConcurrentList<String> list = new BadConcurrentList<>(new ArrayList<>());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> list.add("lockFail"));
        assertEquals("lockFail", ex.getMessage());
    }

    @Test
    void add_genericViolation_classCastException() throws Exception {
        // Create a raw List that accepts Object, then cast to ConcurrentList<String>
        List raw = new ArrayList();
        // raw element of wrong type
        raw.add(new Object());
        // Use reflection to set the internal list
        Constructor<ConcurrentList> ctor = ConcurrentList.class.getDeclaredConstructor(List.class);
        ctor.setAccessible(true);
        ConcurrentList<String> list = ctor.newInstance(raw);
        // Now attempt to add a proper String – the underlying list already holds an Object,
        // which will cause a ClassCastException when accessed via get()
        assertTrue(list.add("ok"));
        // Trigger the cast by calling get(0)
        assertThrows(ClassCastException.class, () -> list.get(0));
    }
}
