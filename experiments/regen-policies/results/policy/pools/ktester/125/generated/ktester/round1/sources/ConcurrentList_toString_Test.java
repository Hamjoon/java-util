package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentModificationException;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_toString_Test {

    private ConcurrentList<Integer> concurrentList;

    private List<Integer> backingList;

    class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("element failure");
        }
    }

    class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("element failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh ConcurrentList for each test (stand‑alone mode)
        concurrentList = new ConcurrentList<>();
        // Also prepare a backing list for wrapper‑mode tests
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required but keeps the pattern consistent
        concurrentList = null;
        backingList = null;
    }

    @Test
    void toString_EmptyList_ReturnsEmptyBrackets() {
        // Given: a newly created ConcurrentList (empty)
        // When
        String result = concurrentList.toString();
        // Then
        assertEquals("[]", result, "toString() of an empty list should be []");
    }

    @Test
    void toString_PopulatedList_ReturnsUnderlyingListString() {
        // Given
        concurrentList.add(10);
        concurrentList.add(20);
        concurrentList.add(30);
        // When
        String result = concurrentList.toString();
        // Then
        assertEquals("[10, 20, 30]", result, "toString() should reflect the elements in insertion order");
    }

    @Test
    void toString_WrapperMode_ReflectsBackingListChanges() {
        // Given: a backing list with some elements
        backingList.add(1);
        backingList.add(2);
        backingList.add(3);
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        // When: we call toString on the wrapper
        String first = wrapped.toString();
        // Then: it should match the backing list's toString
        assertEquals(backingList.toString(), first, "Initial toString() must match the backing list's representation");
        // When: we modify the backing list directly
        backingList.add(4);
        String second = wrapped.toString();
        // Then: the wrapper's toString reflects the change because it delegates to the backing list
        assertEquals(backingList.toString(), second, "toString() after modifying the backing list should reflect the new state");
    }

    @Test
    void toString_NullElementsHandledCorrectly() {
        // Given: list containing null values
        concurrentList.add(null);
        concurrentList.add(5);
        concurrentList.add(null);
        // When
        String result = concurrentList.toString();
        // Then
        assertEquals("[null, 5, null]", result, "toString() should correctly represent null elements");
    }

    @Test
    void toString_ConcurrentModificationException_Propagates() throws Exception {
        // Arrange: replace the internal list with a mock that throws CME on toString()
        @SuppressWarnings("unchecked")
        List<Integer> mockedList = mock(List.class);
        when(mockedList.toString()).thenThrow(new ConcurrentModificationException("simulated CME"));
        // Use reflection to inject the mock into the private final field `list`
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(concurrentList, mockedList);
        // Act & Assert: the exception should propagate unchanged
        assertThrows(ConcurrentModificationException.class, () -> concurrentList.toString(), "ConcurrentModificationException from the underlying list should propagate");
    }

    @Test
    void toString_ElementToStringThrowsRuntimeException_Propagates() {
        // Arrange: a custom element whose toString throws RuntimeException
        class BadToString {

            @Override
            public String toString() {
                throw new RuntimeException("element failure");
            }
        }
        // The list can hold Object, so we need a ConcurrentList<Object>
        ConcurrentList<Object> list = new ConcurrentList<>();
        list.add(new BadToString());
        list.add("good");
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, list::toString, "Exception from an element's toString() must propagate");
        assertEquals("element failure", ex.getMessage(), "The propagated exception should retain the original message");
    }

    @Test
    void toString_InternalListIsNull_ThrowsNullPointerException() throws Exception {
        // Arrange: force the private `list` field to null via reflection
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(concurrentList, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> concurrentList.toString(), "Calling toString() when internal list is null should throw NPE");
    }
}
