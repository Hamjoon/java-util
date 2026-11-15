package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_indexOf_Test {

    /**
     * List used when we want a real, thread‑safe list.
     */
    private ConcurrentList<Integer> realList;

    /**
     * Mocked backing list used to verify delegation.
     */
    @Mock
    private List<Integer> mockedBackingList;

    /**
     * ConcurrentList that wraps the {@code mockedBackingList}.
     */
    private ConcurrentList<Integer> listWithMock;

    /* -------------------------------------------------------------
     * Helper class for custom equality test
     * ------------------------------------------------------------- */
    private static final class Foo {

        private final int id;

        Foo(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Foo))
                return false;
            Foo foo = (Foo) o;
            return id == foo.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "Foo{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Real list for functional tests.
        realList = new ConcurrentList<>();
        // Prepare the list that delegates to a mock.
        // The mock will be injected by MockitoExtension.
        listWithMock = new ConcurrentList<>(mockedBackingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the garbage collector in long test suites.
        realList = null;
        listWithMock = null;
    }

    private static Stream<Arguments> functionalBranchProvider() {
        return Stream.of(// List contains the element "apple" at index 0
        Arguments.of(List.of("apple", "banana", "cherry"), "apple", 0, "List contains the element \"apple\" at index 0; indexOf should return the first matching index."), // Duplicate "banana" at indices 2 and 4 (we construct such a list)
        Arguments.of(List.of("x", "y", "banana", "z", "banana"), "banana", 2, "List contains duplicate \"banana\" values at indices 2 and 4; indexOf must return the first occurrence (index 2)."), // Missing element
        Arguments.of(List.of("a", "b", "c"), "missing", -1, "Element \"missing\" is not present in the list; indexOf should return -1."), // Null present at index 1
        Arguments.of(Arrays.asList("first", null, "third"), null, 1, "List contains a null element at index 1; indexOf(null) must return that index."), // Null absent (empty list)
        Arguments.of(Collections.emptyList(), null, -1, "List does not contain null (empty list scenario); indexOf(null) should return -1."), // Any search on empty list
        Arguments.of(Collections.emptyList(), "any", -1, "The underlying list is empty; any search must yield -1."));
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(// Multiple nulls, first at index 0
        Arguments.of(Arrays.asList(null, "x", null), null, 0, "The list contains multiple null elements (e.g., [null, \"x\", null]); indexOf(null) must return the first occurrence at index 0."), // Element "c" at last position
        Arguments.of(Arrays.asList("a", "b", "c"), "c", 2, "The element \"c\" is present only at the last position of the list (e.g., [\"a\", \"b\", \"c\"]); indexOf should return the last index 2."), // Type mismatch: searching Integer in a list of Strings
        Arguments.of(Arrays.asList("1", "2", "3"), 5, -1, "The list holds String objects (e.g., [\"1\", \"2\", \"3\"]). Searching for an Integer value 5, which is a different type, must return -1."), // Custom Foo objects with overridden equals
        Arguments.of(Arrays.asList(new Foo(1), new Foo(2)), new Foo(1), 0, "The list contains a custom object Foo(1) that overrides equals to compare by its id field. Searching with a different Foo instance having the same id should be considered equal and return index 0."), // Long objects distinct instances but equal value
        Arguments.of(Arrays.asList(Long.valueOf(5L), Long.valueOf(10L), Long.valueOf(15L)), Long.valueOf(10L), 1, "The list holds Long objects [5L, 10L, 15L]. Searching with a distinct Long instance representing 10L must match via equals and return index 1."), // Duplicate strings "duplicate"
        Arguments.of(Arrays.asList("duplicate", "x", "y", "duplicate"), "duplicate", 0, "The list contains duplicate strings \"duplicate\" at indices 0 and 3; indexOf must return the first occurrence (index 0)."));
    }

    @Test
    void indexOfReturnsCorrectIndexForExistingElement() {
        // given
        realList.add(10);
        realList.add(20);
        realList.add(30);
        // duplicate value
        realList.add(20);
        // when / then
        assertEquals(1, realList.indexOf(20), "Should return the first occurrence index");
        assertEquals(0, realList.indexOf(10), "Should return index 0 for the first element");
        assertEquals(2, realList.indexOf(30), "Should return index 2 for the third element");
    }

    @Test
    void indexOfReturnsMinusOneWhenElementIsAbsent() {
        // given
        realList.addAll(List.of(1, 2, 3));
        // when / then
        assertEquals(-1, realList.indexOf(99), "Non‑existent element must yield -1");
    }

    @Test
    void indexOfHandlesNullElement() {
        // given
        realList.add(null);
        realList.add(1);
        realList.add(null);
        // when / then
        assertEquals(0, realList.indexOf(null), "First null should be reported at index 0");
        // remove the first null
        realList.remove(0);
        assertEquals(1, realList.indexOf(null), "After removal, the remaining null should be at index 1");
    }

    @Test
    void indexOfDelegatesToUnderlyingListWhenWrapped() {
        // given
        when(mockedBackingList.indexOf("test")).thenReturn(7);
        // when
        int result = listWithMock.indexOf("test");
        // then
        assertEquals(7, result, "Result must be whatever the backing list returns");
        verify(mockedBackingList, times(1)).indexOf("test");
    }

    @Test
    void indexOfDelegatesCorrectlyForNullWhenWrapped() {
        // given
        when(mockedBackingList.indexOf(null)).thenReturn(-1);
        // when
        int result = listWithMock.indexOf(null);
        // then
        assertEquals(-1, result);
        verify(mockedBackingList, times(1)).indexOf(null);
    }

    /* -------------------------------------------------------------
     * Parameterized functional branch coverage tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("functionalBranchProvider")
    void indexOfFunctionalBranchCoverage(List<String> initialElements, Object search, int expected, String description) {
        // Arrange: populate a fresh ConcurrentList with the supplied elements
        ConcurrentList<String> list = new ConcurrentList<>();
        list.addAll(initialElements);
        // Act
        int actual = list.indexOf(search);
        // Assert
        assertEquals(expected, actual, description);
    }

    /* -------------------------------------------------------------
     * Parameterized additional edge‑case tests
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {3}")
    @MethodSource("edgeCaseProvider")
    void indexOfAdditionalEdgeCases(List<Object> initialElements, Object search, int expected, String description) {
        // Arrange: use a raw ConcurrentList to hold heterogeneous objects
        @SuppressWarnings("unchecked")
        ConcurrentList<Object> list = new ConcurrentList<>();
        list.addAll(initialElements);
        // Act
        int actual = list.indexOf(search);
        // Assert
        assertEquals(expected, actual, description);
    }
}
