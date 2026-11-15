package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toString_Test {

    // Instance used for the “real” list tests
    private ConcurrentList<String> realList;

    // Mocked underlying list to verify delegation
    @Mock
    private List<String> mockedUnderlyingList;

    private ConcurrentList<String> listWithMock;

    /**
     * Simple POJO used for the custom‑object test.
     */
    static final class Person {

        private final String name;

        Person(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "Person{name='" + name + "'}";
        }
    }

    /**
     * List implementation that ignores its contents and always returns a constant string.
     */
    static final class CustomToStringList<E> extends AbstractList<E> {

        @Override
        public E get(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String toString() {
            return "CUSTOM";
        }
    }

    /**
     * Subclass that forces readOperation to throw an IllegalStateException.
     */
    static class FaultyReadConcurrentList<E> extends ConcurrentList<E> {

        @Override
        protected <T> T readOperation(Supplier<T> operation) {
            throw new IllegalStateException("simulated read lock failure");
        }
    }

    /**
     * List whose toString() throws a generic RuntimeException.
     */
    static final class ThrowingToStringList<E> extends AbstractList<E> {

        @Override
        public E get(int index) {
            throw new IndexOutOfBoundsException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public String toString() {
            throw new RuntimeException("simulated toString failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Real list – starts empty for each test
        realList = new ConcurrentList<>();
        // Prepare the mock to return a known string when its toString() is invoked
        when(mockedUnderlyingList.toString()).thenReturn("[mocked]");
        // Wrap the mock with ConcurrentList
        listWithMock = new ConcurrentList<>(mockedUnderlyingList);
    }

    @Test
    void toString_EmptyList_ReturnsEmptyBrackets() {
        // Given an empty ConcurrentList
        // When
        String result = realList.toString();
        // Then
        assertEquals("[]", result, "toString() of an empty list should be \"[]\"");
    }

    @Test
    void toString_NonEmptyList_ReturnsUnderlyingListToString() {
        // Given a ConcurrentList with several elements
        realList.add("a");
        realList.add("b");
        realList.add("c");
        // When
        String result = realList.toString();
        // Then
        // The format must match java.util.ArrayList's toString()
        assertEquals("[a, b, c]", result, "toString() should reflect the underlying list's string representation");
    }

    @Test
    void toString_DelegatesToUnderlyingList_WhenWrappedListIsMocked() {
        // Given a ConcurrentList that wraps a mocked List
        // When
        String result = listWithMock.toString();
        // Then
        // Verify that the mock's toString() was invoked exactly once
        verify(mockedUnderlyingList, times(1)).toString();
        // And the result should be whatever the mock returned
        assertEquals("[mocked]", result, "toString() should return the value supplied by the underlying list's toString()");
    }

    @Test
    void toString_ConcurrentModificationDoesNotThrow() {
        // This test ensures that toString() does not throw when the list is modified
        // concurrently from another thread. The implementation uses a read lock, so
        // the call should be safe.
        // Populate the list
        for (int i = 0; i < 100; i++) {
            realList.add("val" + i);
        }
        // Start a thread that modifies the list while toString() is executed
        Thread modifier = new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                realList.add("extra" + i);
                realList.remove(0);
            }
        });
        modifier.start();
        // The toString call should complete without throwing an exception
        assertDoesNotThrow(() -> {
            String s = realList.toString();
            assertNotNull(s);
        }, "toString() should not throw even if the list is being modified concurrently");
        // Ensure the modifier thread finishes before the test ends
        try {
            modifier.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail("Test interrupted while waiting for modifier thread");
        }
    }

    @Test
    void toString_UnderlyingToStringThrowsIllegalStateException_Propagates() {
        // Underlying list that throws when toString() is called
        List<String> badList = mock(List.class);
        when(badList.toString()).thenThrow(new IllegalStateException("simulated failure"));
        ConcurrentList<String> cl = new ConcurrentList<>(badList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, cl::toString, "Exception from underlying list's toString() must be propagated");
        assertEquals("simulated failure", ex.getMessage());
    }

    @Test
    void toString_ListWithNullElements_ProducesStandardRepresentation() {
        // Backing list contains a null followed by a non‑null element
        List<String> backing = new ArrayList<>();
        backing.add(null);
        backing.add("a");
        ConcurrentList<String> cl = new ConcurrentList<>(backing);
        assertEquals("[null, a]", cl.toString(), "toString() should correctly render null elements");
    }

    @Test
    void toString_CustomObjectElements_UsesObjectToString() {
        ConcurrentList<Person> cl = new ConcurrentList<>();
        cl.add(new Person("John"));
        assertEquals("[Person{name='John'}]", cl.toString(), "toString() must include the custom object's toString() output");
    }

    @Test
    void toString_AddThenRemove_ElementIsNoLongerPresent() {
        realList.add("x");
        realList.remove("x");
        assertEquals("[]", realList.toString(), "After adding and removing the same element, toString() should show an empty list");
    }

    @Test
    void toString_UnderlyingListOverridesToString_ReturnsCustomValue() {
        List<String> custom = new CustomToStringList<>();
        ConcurrentList<String> cl = new ConcurrentList<>(custom);
        assertEquals("CUSTOM", cl.toString(), "ConcurrentList should return exactly what the underlying list's toString() returns");
    }

    @Test
    void toString_NullBackingList_ThrowsNullPointerException() throws Exception {
        // Create a normal instance then null out its internal 'list' field via reflection
        ConcurrentList<String> cl = new ConcurrentList<>();
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        // now the backing list is null
        listField.set(cl, null);
        assertThrows(NullPointerException.class, cl::toString, "Calling toString() with a null backing list should throw NPE");
    }

    @Test
    void toString_ReadOperationThrowsIllegalStateException_Propagates() {
        FaultyReadConcurrentList<String> cl = new FaultyReadConcurrentList<>();
        IllegalStateException ex = assertThrows(IllegalStateException.class, cl::toString, "IllegalStateException from readOperation must be propagated");
        assertEquals("simulated read lock failure", ex.getMessage());
    }

    @Test
    void toString_UnderlyingToStringThrowsRuntimeException_Propagates() {
        List<String> bad = new ThrowingToStringList<>();
        ConcurrentList<String> cl = new ConcurrentList<>(bad);
        RuntimeException ex = assertThrows(RuntimeException.class, cl::toString, "RuntimeException from underlying toString() must be propagated");
        assertEquals("simulated toString failure", ex.getMessage());
    }
}
