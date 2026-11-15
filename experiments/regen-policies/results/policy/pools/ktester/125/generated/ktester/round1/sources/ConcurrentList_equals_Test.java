package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_equals_Test {

    private static Stream<Arguments> provideFalseEqualityScenarios() {
        return Stream.of(// same elements, different order
        Arguments.of(Arrays.asList(3, 2, 1), false), // different collection type
        Arguments.of(new HashSet<>(Arrays.asList(1, 2, 3)), false), // completely unrelated type
        Arguments.of("some string", false));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsSameElements() {
        List<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        List<Integer> other = Arrays.asList(1, 2, 3);
        assertTrue(list.equals(other), "ConcurrentList should be equal to a List with the same elements");
        assertTrue(other.equals(list), "Standard List should be equal to ConcurrentList with same elements");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsDifferentElements() {
        List<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        List<Integer> other = Arrays.asList(1, 2, 4);
        assertFalse(list.equals(other), "ConcurrentList should not be equal when elements differ");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsSelf() {
        List<Integer> list = new ConcurrentList<>(Arrays.asList(5, 6, 7));
        assertTrue(list.equals(list), "ConcurrentList must be equal to itself");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsNull() {
        List<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        assertFalse(list.equals(null), "ConcurrentList should not be equal to null");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsDifferentType() {
        List<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        String notAList = "I am not a list";
        assertFalse(list.equals(notAList), "ConcurrentList should not be equal to an object of a different type");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsDelegatesToBackingList(@Mock List<Integer> mockedBacking) {
        // Arrange – configure the mock to return true for a specific argument
        List<Integer> other = Arrays.asList(10, 20, 30);
        when(mockedBacking.equals(other)).thenReturn(true);
        // The ConcurrentList wraps the mocked backing list
        ConcurrentList<Integer> concurrent = new ConcurrentList<>(mockedBacking);
        // Act
        boolean result = concurrent.equals(other);
        // Assert – the result must match the mock's configured return value
        assertTrue(result, "Result should reflect the mocked backing list's equals() outcome");
        // Verify that the backing list's equals method was indeed called once with the exact argument
        verify(mockedBacking, times(1)).equals(other);
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsBetweenConcurrentLists() {
        List<Integer> backing1 = new ArrayList<>(Arrays.asList(1, 2, 3));
        List<Integer> backing2 = new ArrayList<>(Arrays.asList(1, 2, 3));
        ConcurrentList<Integer> cl1 = new ConcurrentList<>(backing1);
        ConcurrentList<Integer> cl2 = new ConcurrentList<>(backing2);
        assertTrue(cl1.equals(cl2), "Two ConcurrentList instances with equal contents should be equal");
        assertTrue(cl2.equals(cl1), "Equality must be symmetric");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsWithSameBackingInstance() {
        List<Integer> shared = new ArrayList<>(Arrays.asList(7, 8, 9));
        ConcurrentList<Integer> cl1 = new ConcurrentList<>(shared);
        ConcurrentList<Integer> cl2 = new ConcurrentList<>(shared);
        assertTrue(cl1.equals(cl2), "ConcurrentLists sharing the same backing list should be equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsDifferentOrder() {
        List<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        List<Integer> other = Arrays.asList(3, 2, 1);
        assertFalse(list.equals(other), "Lists with same elements in different order must not be equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsHashSet() {
        List<Integer> list = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        Set<Integer> other = new HashSet<>(Arrays.asList(1, 2, 3));
        assertFalse(list.equals(other), "ConcurrentList should not be equal to a Set even if it contains same elements");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsTwoEmptyConcurrentLists() {
        ConcurrentList<Integer> cl1 = new ConcurrentList<>(Collections.emptyList());
        ConcurrentList<Integer> cl2 = new ConcurrentList<>(Collections.emptyList());
        assertTrue(cl1.equals(cl2), "Two empty ConcurrentList instances must be equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsEmptyMutableList() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(new ArrayList<>());
        List<Integer> empty = Collections.emptyList();
        assertTrue(cl.equals(empty), "Empty ConcurrentList should be equal to an empty standard List");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsBothNullElementsConcurrent() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(Arrays.asList(null, null));
        List<Integer> other = Arrays.asList(null, null);
        assertTrue(cl.equals(other), "ConcurrentList with only nulls must equal a List with same nulls");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsStandardListWithNulls() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(Arrays.asList(null, null));
        List<Integer> other = Arrays.asList(null, null);
        assertTrue(cl.equals(other), "ConcurrentList containing nulls should equal standard List with same nulls");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsNullElementMismatch() {
        ConcurrentList<Integer> cl = new ConcurrentList<>(Arrays.asList(1, null, 3));
        List<Integer> other = Arrays.asList(1, 2, 3);
        assertFalse(cl.equals(other), "Lists that differ because of a null element must not be equal");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsSubListEqual() {
        List<Integer> backing = Arrays.asList(1, 2, 3, 4);
        ConcurrentList<Integer> cl = new ConcurrentList<>(backing);
        // [1,2,3]
        List<Integer> sub = cl.subList(0, 3);
        assertTrue(cl.equals(sub), "ConcurrentList should be equal to a subList view with identical elements");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void equalsSubListDifferent() {
        List<Integer> backing = Arrays.asList(1, 2, 3);
        ConcurrentList<Integer> cl = new ConcurrentList<>(backing);
        // [2,3]
        List<Integer> sub = cl.subList(1, 3);
        // The original list is [1,2,3]; subList is [2,3] → not equal
        assertFalse(cl.equals(sub), "ConcurrentList must not be equal to a subList with different range");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index} => other={0}, expected={1}")
    @MethodSource("provideFalseEqualityScenarios")
    void equalsVariousFalseScenarios(Object other, boolean expected) {
        ConcurrentList<Integer> cl = new ConcurrentList<>(Arrays.asList(1, 2, 3));
        assertEquals(expected, cl.equals(other), "Equality result does not match expectation for input: " + other);
    }
}
