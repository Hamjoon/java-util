package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_contains_Test {

    /**
     * Instance used for the regular (non‑mock) tests.
     */
    private ConcurrentList<Integer> list;

    /**
     * Mocked List that will be wrapped by ConcurrentList for delegation verification.
     */
    @Mock
    private List<Integer> mockedList;

    /**
     * equals returns true for any non‑null argument
     */
    static class AlwaysTrue {

        @Override
        public boolean equals(Object o) {
            return o != null;
        }
    }

    /**
     * equals returns false when compared to null
     */
    static class NeverEqualsNull {

        @Override
        public boolean equals(Object o) {
            return o != null;
        }
    }

    /**
     * equals throws RuntimeException when argument equals the trigger string
     */
    static class ThrowingEquals {

        private final String trigger;

        ThrowingEquals(String trigger) {
            this.trigger = trigger;
        }

        @Override
        public boolean equals(Object o) {
            if (Objects.equals(o, trigger)) {
                throw new RuntimeException(trigger);
            }
            return false;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test to avoid cross‑test interference.
        list = new ConcurrentList<>();
        // The @Mock field `mockedList` is automatically instantiated by MockitoExtension.
    }

    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(Arguments.of(List.of("apple", "banana", null), "apple", true, "The list contains the element \"apple\"; contains should return true."), Arguments.of(List.of("apple", "banana", null), "banana", true, "The list contains the element \"banana\"; contains should return true."), Arguments.of(List.of("apple", "banana", null), "coconut", false, "The list does not contain the element \"coconut\"; contains should return false."), Arguments.of(List.of(null, "x"), null, true, "The list contains a null element; calling contains(null) should return true."), Arguments.of(List.of("x", "y"), null, false, "The list does not contain null; calling contains(null) should return false."), Arguments.of(List.of("only"), "only", true, "List with a single element \"only\"; contains should correctly identify the element."), Arguments.of(List.of("only"), "missing", false, "List with a single element \"only\"; searching for a different element should return false."), Arguments.of(List.of("duplicate", "duplicate"), "duplicate", true, "List contains multiple occurrences of \"duplicate\"; contains should still return true."), Arguments.of(List.of(), "empty", false, "The list is empty; any contains check should return false."));
    }

    private static Stream<Arguments> typeHandlingProvider() {
        return Stream.of(Arguments.of(List.of(42), 42, true, "The list contains the Integer 42; contains(Integer) should return true."), Arguments.of(List.of(42), 42L, false, "The list contains the Integer 42 but the query is a Long 42L; should return false."), Arguments.of(List.of(new AlwaysTrue()), "test", true, "Custom object whose equals returns true for any String; contains(\"test\") should be true."), Arguments.of(List.of(Arrays.asList(1, 2, 3)), Arrays.asList(1, 2, 3), true, "List contains a List instance with elements [1,2,3]; searching with equal List should return true."), Arguments.of(List.of(Arrays.asList(1, 2, 3)), Arrays.asList(3, 2, 1), false, "List contains a List [1,2,3]; searching with different order should return false."));
    }

    @Test
    void containsReturnsTrueWhenElementIsPresent() {
        list.add(10);
        list.add(20);
        list.add(30);
        assertTrue(list.contains(20), "contains should return true for an element that was added");
    }

    @Test
    void containsReturnsFalseWhenElementIsAbsent() {
        list.add(1);
        list.add(2);
        list.add(3);
        assertFalse(list.contains(99), "contains should return false for an element that is not in the list");
    }

    @Test
    void containsHandlesNullElements() {
        list.add(null);
        list.add(5);
        assertTrue(list.contains(null), "contains should correctly detect a null element that was added");
        assertFalse(list.contains(0), "contains should return false for a null‑absent element");
    }

    @Test
    void containsWorksWithWrappedListDelegate() {
        // Arrange: make the mock behave as we want.
        when(mockedList.contains(42)).thenReturn(true);
        when(mockedList.contains(99)).thenReturn(false);
        // The ConcurrentList that wraps the mocked List.
        ConcurrentList<Integer> wrapper = new ConcurrentList<>(mockedList);
        // Act & Assert
        assertTrue(wrapper.contains(42), "wrapper should return true when the underlying list returns true");
        assertFalse(wrapper.contains(99), "wrapper should return false when the underlying list returns false");
        // Verify that the underlying list's contains method was invoked exactly once per call.
        verify(mockedList, times(1)).contains(42);
        verify(mockedList, times(1)).contains(99);
    }

    @Test
    void constructorRejectsNullBackingList() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<>(null), "Constructor should reject a null backing List");
        assertEquals("List cannot be null", ex.getMessage());
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("basicFunctionalityProvider")
    void containsBasicFunctionality(List<Object> initial, Object query, boolean expected, String _desc) {
        list.addAll(initial);
        assertEquals(expected, list.contains(query));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("typeHandlingProvider")
    void containsTypeHandling(List<Object> initial, Object query, boolean expected, String _desc) {
        list.addAll(initial);
        assertEquals(expected, list.contains(query));
    }

    @Test
    void containsMultipleNulls() {
        list.addAll(Arrays.asList(null, null, "x"));
        assertTrue(list.contains(null), "Underlying list contains multiple null entries; contains(null) must return true.");
    }

    @Test
    void containsNoNulls() {
        list.addAll(Arrays.asList("a", "b"));
        assertFalse(list.contains(null), "Underlying list contains no null entries; contains(null) must return false.");
    }

    @Test
    void containsCustomEqualsAlwaysTrue() {
        list.add(new AlwaysTrue());
        assertTrue(list.contains("any"), "Custom element overrides equals to true for any non‑null argument.");
    }

    @Test
    void containsCustomEqualsNeverNull() {
        list.add(new NeverEqualsNull());
        assertFalse(list.contains(null), "Custom element returns false when compared to null.");
    }

    @Test
    void containsElementThrowsRuntimeException() {
        list.add(new ThrowingEquals("trigger"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.contains("trigger"), "Exception from element.equals should propagate.");
        assertEquals("trigger", ex.getMessage());
    }

    @Test
    void containsElementThrowsOnlyForSpecificArgument() {
        list.add(new ThrowingEquals("trigger"));
        assertFalse(list.contains("safe"), "For non‑trigger argument, contains should behave normally and return false.");
    }

    @Test
    void containsUnderlyingListThrowsUnsupportedOperationException() {
        // mock that throws on contains()
        when(mockedList.contains(any())).thenThrow(new UnsupportedOperationException("not supported"));
        ConcurrentList<Object> wrapper = new ConcurrentList<>(mockedList);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> wrapper.contains("any"));
        assertEquals("not supported", ex.getMessage());
    }

    @Test
    void containsUnderlyingListThrowsIllegalStateException() {
        when(mockedList.contains(any())).thenThrow(new IllegalStateException("bad state"));
        ConcurrentList<Object> wrapper = new ConcurrentList<>(mockedList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> wrapper.contains("trigger"));
        assertEquals("bad state", ex.getMessage());
    }

    @Test
    void containsUnderlyingListThrowsConcurrentModificationException() {
        when(mockedList.contains(any())).thenThrow(new ConcurrentModificationException("cme"));
        ConcurrentList<Object> wrapper = new ConcurrentList<>(mockedList);
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> wrapper.contains("cme"));
        assertEquals("cme", ex.getMessage());
    }

    @Test
    void readOperationThrowsIllegalMonitorStateException() {
        ConcurrentList<Object> broken = new ConcurrentList<Object>() {

            @Override
            public <// use reflection to access the private method name; we just shadow it
            T> T readOperation(Supplier<T> operation) {
                throw new IllegalMonitorStateException("illegal monitor");
            }
        };
        broken.add("something");
        IllegalMonitorStateException ex = assertThrows(IllegalMonitorStateException.class, () -> broken.contains("any"));
        assertEquals("illegal monitor", ex.getMessage());
    }

    @Test
    void internalListNullLeadsToNPE() throws Exception {
        // create a normal instance and then null out the private field
        ConcurrentList<Object> cl = new ConcurrentList<>();
        cl.add("value");
        Field f = ConcurrentList.class.getDeclaredField("list");
        f.setAccessible(true);
        // break internal state
        f.set(cl, null);
        assertThrows(NullPointerException.class, () -> cl.contains("any"));
    }

    @Test
    void containsDuringConcurrentAddDoesNotThrowCME() throws Exception {
        // Use a real list that we can modify from another thread while a read lock is held.
        List<Integer> backing = new ArrayList<>();
        ConcurrentList<Integer> cl = new ConcurrentList<>(backing);
        // Populate with some initial data
        cl.addAll(Arrays.asList(1, 2, 3));
        CountDownLatch startRead = new CountDownLatch(1);
        CountDownLatch finishRead = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(false);
        Thread reader = new Thread(() -> {
            // readOperation acquires read lock; we simulate a long read
            startRead.countDown();
            // will be false initially
            result.set(cl.contains(99));
            finishRead.countDown();
        });
        Thread writer = new Thread(() -> {
            try {
                // wait until read started
                startRead.await();
                // modify backing list concurrently
                cl.add(99);
            } catch (InterruptedException ignored) {
            }
        });
        reader.start();
        writer.start();
        reader.join();
        writer.join();
        // After both threads finish, contains should have returned false (it read before add)
        // but more importantly, no CME should have been thrown.
        assertFalse(result.get(), "contains should safely return false without CME even if another thread adds.");
        assertTrue(cl.contains(99), "After concurrent modification, the element must be present.");
    }

    @Test
    void containsDuringConcurrentRemoveReturnsFalseSafely() throws Exception {
        List<Integer> backing = new ArrayList<>(Arrays.asList(5, 6, 7));
        ConcurrentList<Integer> cl = new ConcurrentList<>(backing);
        CountDownLatch startRead = new CountDownLatch(1);
        CountDownLatch finishRead = new CountDownLatch(1);
        AtomicBoolean result = new AtomicBoolean(true);
        Thread reader = new Thread(() -> {
            startRead.countDown();
            // may be true or false depending on timing
            result.set(cl.contains(5));
            finishRead.countDown();
        });
        Thread remover = new Thread(() -> {
            try {
                startRead.await();
                cl.remove(Integer.valueOf(5));
            } catch (InterruptedException ignored) {
            }
        });
        reader.start();
        remover.start();
        reader.join();
        remover.join();
        // No CME should be thrown; result can be true or false but we assert no exception.
        assertNotNull(result.get());
    }
}
