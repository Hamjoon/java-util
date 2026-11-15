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
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_equals_Test {

    @Mock
    private List<Integer> // mocked underlying list
    mockBackingList;

    // class under test
    private ConcurrentList<Integer> concurrentList;

    @BeforeEach
    void setupBeforeEach() {
        // Create a ConcurrentList that wraps the mocked backing list for the first three tests
        concurrentList = new ConcurrentList<>(mockBackingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset interactions with the mock to keep each test isolated
        reset(mockBackingList);
    }

    /**
     * Provides arguments for the basic & edge case scenarios.
     *
     * Each {@code Arguments} consists of:
     * <ul>
     *   <li>description – for readability in test reports</li>
     *   <li>listUnderTest – the content of the ConcurrentList under test</li>
     *   <li>other – the object passed to {@code equals}</li>
     *   <li>expected – the expected boolean result</li>
     * </ul>
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> basicAndEdgeCases() {
        return Stream.of(// null comparison
        arguments("null other", Arrays.asList("a", "b", "c"), null, false), // reflexive (same reference)
        arguments("self reference", Arrays.asList("a", "b", "c"), "self", true), // equal plain ArrayList
        arguments("plain ArrayList same elements", Arrays.asList("a", "b", "c"), new ArrayList<>(Arrays.asList("a", "b", "c")), true), // different size
        arguments("different size", Arrays.asList("a", "b", "c"), new ArrayList<>(Arrays.asList("a", "b")), false), // same elements, different order
        arguments("different order", Arrays.asList("a", "b", "c"), new ArrayList<>(Arrays.asList("c", "b", "a")), false), // non‑list object
        arguments("non‑list object", Arrays.asList("a", "b", "c"), "some string", false), // equal ConcurrentList
        arguments("other ConcurrentList same elements", Arrays.asList("a", "b", "c"), new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c"))), true), // other ConcurrentList with a differing element
        arguments("other ConcurrentList different element", Arrays.asList("a", "b", "c"), new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "x", "c"))), false), // null element at index 0
        arguments("null element matches", Arrays.asList(null, "b", "c"), new ArrayList<>(Arrays.asList(null, "b", "c")), true), // mismatching null positions
        arguments("null element mismatches", Arrays.asList(null, "b", "c"), new ArrayList<>(Arrays.asList(null, "b", null)), false));
    }

    /**
     * Additional scenarios covering empty lists, other List implementations,
     * duplicate elements, unmodifiable lists, nested lists and sub‑list views.
     */
    private static Stream<org.junit.jupiter.params.provider.Arguments> additionalScenarios() {
        return Stream.of(// empty ConcurrentList vs empty ArrayList
        arguments("empty list equals empty ArrayList", Collections.emptyList(), new ArrayList<>(), true), // empty ConcurrentList vs non‑empty ArrayList
        arguments("empty list vs non‑empty ArrayList", Collections.emptyList(), new ArrayList<>(Collections.singletonList("x")), false), // non‑empty ConcurrentList vs empty ArrayList
        arguments("non‑empty list vs empty ArrayList", Arrays.asList("a", "b", "c"), new ArrayList<>(), false), // LinkedList with same elements
        arguments("LinkedList same elements", Arrays.asList("a", "b", "c"), new LinkedList<>(Arrays.asList("a", "b", "c")), true), // duplicate elements
        arguments("duplicate elements match", Arrays.asList("a", "a", "b"), new ArrayList<>(Arrays.asList("a", "a", "b")), true), // unmodifiable list
        arguments("unmodifiable list same elements", Arrays.asList("a", "b", "c"), Collections.unmodifiableList(Arrays.asList("a", "b", "c")), true), // both lists contain only nulls
        arguments("both only nulls", Arrays.asList(null, null), new ArrayList<>(Arrays.asList(null, null)), true), // same number of nulls but different positions
        arguments("nulls different positions", Arrays.asList(null, "a", null), new ArrayList<>(Arrays.asList(null, null, "a")), false), // nested lists equal
        arguments("nested lists equal", Arrays.asList(Arrays.asList(1, 2), Collections.singletonList(3)), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Collections.singletonList(3)))), true), // nested lists differ
        arguments("nested lists differ", Arrays.asList(Arrays.asList(1, 2), Collections.singletonList(3)), new ArrayList<>(Arrays.asList(new ArrayList<>(Arrays.asList(1, 2)), new ArrayList<>(Collections.singletonList(4)))), false), // subList view equal
        arguments("subList view equal", Arrays.asList("a", "b", "c"), new ArrayList<>(Arrays.asList("a", "b", "c")).subList(0, 3), true));
    }

    @Test
    void equals_delegatesToBackingList_andReturnsTrue() {
        Object other = new Object();
        // Stub the backing list to return true for equals(other)
        when(mockBackingList.equals(other)).thenReturn(true);
        assertTrue(concurrentList.equals(other), "ConcurrentList should return true when backing list reports true");
        // Verify that the backing list's equals method was invoked exactly once with the same argument
        verify(mockBackingList, times(1)).equals(other);
    }

    @Test
    void equals_delegatesToBackingList_andReturnsFalse() {
        Object other = new Object();
        // Stub the backing list to return false for equals(other)
        when(mockBackingList.equals(other)).thenReturn(false);
        assertFalse(concurrentList.equals(other), "ConcurrentList should return false when backing list reports false");
        verify(mockBackingList, times(1)).equals(other);
    }

    @Test
    void equals_withNullOther_returnsFalse() {
        // The contract of List.equals(null) is to return false
        when(mockBackingList.equals(null)).thenReturn(false);
        assertFalse(concurrentList.equals(null), "ConcurrentList should return false when compared with null");
        verify(mockBackingList, times(1)).equals(null);
    }

    @Test
    void equals_whenOtherIsAnotherConcurrentList() {
        // Create a second ConcurrentList wrapping a different mock list
        List<Integer> otherMock = mock(List.class);
        ConcurrentList<Integer> otherConcurrent = new ConcurrentList<>(otherMock);
        // Stub the first backing list to compare equal to the second ConcurrentList instance
        when(mockBackingList.equals(otherConcurrent)).thenReturn(true);
        // optional symmetry check
        when(otherMock.equals(concurrentList)).thenReturn(true);
        assertTrue(concurrentList.equals(otherConcurrent), "ConcurrentList should delegate equals to its backing list even when other is a ConcurrentList");
        verify(mockBackingList, times(1)).equals(otherConcurrent);
    }

    @Test
    void equals_isReflexive() {
        // Stub the backing list to return true when compared with itself
        when(mockBackingList.equals(concurrentList)).thenReturn(true);
        assertTrue(concurrentList.equals(concurrentList), "ConcurrentList should be reflexive (a.equals(a) == true)");
        verify(mockBackingList, times(1)).equals(concurrentList);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("basicAndEdgeCases")
    void equals_basicAndEdgeScenarios(String description, List<String> listUnderTest, Object other, boolean expected) {
        // Build the ConcurrentList under test with a real backing list
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(listUnderTest));
        // Resolve the special "self" keyword
        Object argument = "self".equals(other) ? list : other;
        assertEquals(expected, list.equals(argument), () -> "Failed case: " + description);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("additionalScenarios")
    void equals_additionalScenarios(String description, List<String> listUnderTest, Object other, boolean expected) {
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(listUnderTest));
        assertEquals(expected, list.equals(other), () -> "Failed case: " + description);
    }

    @Test
    void equals_nonListObject_returnsFalseWithoutException() {
        // Using a real backing list to avoid mocking complications
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));
        Object nonList = new Object();
        // According to List.equals contract, a non‑List should simply return false
        assertFalse(list.equals(nonList), "equals should return false when the argument does not implement List");
    }

    @Test
    void equals_nullBackingList_propagatesNullPointerException() throws Exception {
        // Create a ConcurrentList with a non‑null backing list first
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b")));
        // Use reflection to set the private 'list' field to null
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(list, null);
        // Any call to equals should now throw NPE because the delegate is null
        assertThrows(NullPointerException.class, () -> list.equals("anything"), "Calling equals when the internal list is null must throw NullPointerException");
    }

    @Test
    void equals_subListWithInvalidRange_throwsIndexOutOfBoundsException() {
        // The ConcurrentList implementation declares subList as unsupported,
        // but we still verify the documented behaviour for an invalid range.
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a", "b", "c")));
        // Create a subList with an invalid range (start > end) – this triggers IOBE
        List<String> invalidSubList = assertThrows(IndexOutOfBoundsException.class, () -> list.subList(2, 1), "subList with start > end should throw IndexOutOfBoundsException");
        // The exception is thrown before equals is evaluated, but we assert the type
        assertNotNull(invalidSubList);
    }

    @Test
    void equals_listIteratorWithNegativeIndex_throwsIndexOutOfBoundsException() {
        // listIterator(int) is documented to throw IndexOutOfBoundsException for negative indices
        ConcurrentList<String> list = new ConcurrentList<>(new ArrayList<>(Arrays.asList("a")));
        assertThrows(IndexOutOfBoundsException.class, () -> list.listIterator(-1), "listIterator with a negative index must throw IndexOutOfBoundsException");
    }
}
