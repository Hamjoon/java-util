package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_lastIndexOf_Test {

    /**
     * Instances used for the “stand‑alone” mode tests **
     */
    private ConcurrentList<Integer> standAloneList;

    /**
     * Mock and wrapper used for the “wrapper” mode tests **
     */
    @Mock
    private List<Integer> // injected by Mockito
    mockedBackingList;

    private ConcurrentList<Integer> wrapperList;

    /**
     * A list implementation whose lastIndexOf always throws UnsupportedOperationException
     */
    private static class ThrowingList<E> extends AbstractList<E> {

        @Override
        public E get(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public int lastIndexOf(Object o) {
            throw new UnsupportedOperationException("deliberate");
        }
    }

    /**
     * A class whose equals method throws a RuntimeException
     */
    private static class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("Bad equals");
        }
    }

    /**
     * Simple POJO used to test custom equals logic
     */
    private static class TestObj {

        final int id;

        TestObj(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof TestObj) && ((TestObj) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "TestObj{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // fresh stand‑alone list for each test
        standAloneList = new ConcurrentList<>();
        // wrapper list that delegates to the mocked backing list
        wrapperList = new ConcurrentList<>(mockedBackingList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(mockedBackingList);
    }

    private static Stream<Arguments> branchCoverageProvider() {
        return Stream.of(// "apple" appears three times at 0,1,2
        Arguments.of(List.of("apple", "apple", "apple"), "apple", 2), // "banana" not present
        Arguments.of(List.of("apple", "orange", "grape"), "banana", -1), // null present at 1 and 3 (list size 4)
        Arguments.of(Arrays.asList(10, null, 20, null), null, 3), // no nulls
        Arguments.of(List.of(1, 2, 3), null, -1), // empty list
        Arguments.of(Collections.emptyList(), "any", -1), // single Integer 42
        Arguments.of(List.of(42), 42, 0));
    }

    private static Stream<Arguments> additionalScenariosProvider() {
        return Stream.of(// "orange" at 0 and 4 in a 6‑element list
        Arguments.of(List.of("orange", "apple", "banana", "grape", "orange", "pear"), "orange", 4), // "pear" only at final position
        Arguments.of(List.of("a", "b", "c", "d", "e", "pear"), "pear", 5), // "grape" only at first position
        Arguments.of(List.of("grape", "a", "b", "c"), "grape", 0), // all nulls, size 3
        Arguments.of(Arrays.asList(null, null, null), null, 2), // type mismatch: Integer 42 vs Long 42L
        Arguments.of(List.of(42), 42L, -1), // custom objects with overridden equals (compare id)
        Arguments.of(List.of(new TestObj(1), new TestObj(2), new TestObj(1), new TestObj(1)), new TestObj(1), 3));
    }

    /**
     * Helper to inject a custom backing list via reflection
     */
    private void setBackingList(ConcurrentList<?> list, List<?> backing) throws Exception {
        Field f = ConcurrentList.class.getDeclaredField("list");
        f.setAccessible(true);
        f.set(list, backing);
    }

    @Test
    void lastIndexOf_returnsCorrectIndex_inStandAloneList() {
        // given
        standAloneList.add(1);
        standAloneList.add(2);
        standAloneList.add(3);
        // duplicate, should be the last occurrence
        standAloneList.add(2);
        // when
        int lastIdx = standAloneList.lastIndexOf(2);
        int notFound = standAloneList.lastIndexOf(99);
        // then
        assertEquals(3, lastIdx, "The last index of element 2 should be 3");
        assertEquals(-1, notFound, "Element that is not present must return -1");
    }

    @Test
    void lastIndexOf_handlesNullElements_inStandAloneList() {
        // given
        standAloneList.add(null);
        standAloneList.add(42);
        standAloneList.add(null);
        // when
        int lastIdxNull = standAloneList.lastIndexOf(null);
        int lastIdx42 = standAloneList.lastIndexOf(42);
        // then
        assertEquals(2, lastIdxNull, "The last index of null should be 2");
        assertEquals(1, lastIdx42, "The index of 42 should be 1");
    }

    @Test
    void lastIndexOf_delegatesToBackingList_inWrapperMode() {
        // given
        when(mockedBackingList.lastIndexOf("alpha")).thenReturn(7);
        when(mockedBackingList.lastIndexOf("beta")).thenReturn(-1);
        // when
        int idxAlpha = wrapperList.lastIndexOf("alpha");
        int idxBeta = wrapperList.lastIndexOf("beta");
        // then
        assertEquals(7, idxAlpha, "Wrapper should return the value supplied by the backing list");
        assertEquals(-1, idxBeta, "Wrapper should forward -1 when element is absent");
        // verify that the wrapper really called the backing list's method exactly once per call
        verify(mockedBackingList, times(1)).lastIndexOf("alpha");
        verify(mockedBackingList, times(1)).lastIndexOf("beta");
        verifyNoMoreInteractions(mockedBackingList);
    }

    @Test
    void lastIndexOf_multipleCalls_doNotMutateState_inWrapperMode() {
        // given
        when(mockedBackingList.lastIndexOf("gamma")).thenReturn(3);
        // when
        int first = wrapperList.lastIndexOf("gamma");
        int second = wrapperList.lastIndexOf("gamma");
        // then
        assertEquals(3, first);
        assertEquals(3, second);
        // The backing list should have been consulted twice – read‑only behaviour.
        verify(mockedBackingList, times(2)).lastIndexOf("gamma");
        verifyNoMoreInteractions(mockedBackingList);
    }

    @ParameterizedTest(name = "[{index}] lastIndexOf({1}) on {0} → {2}")
    @MethodSource("branchCoverageProvider")
    void lastIndexOf_branchCoverage(List<Object> initialData, Object search, int expected) {
        // populate the stand‑alone list
        initialData.forEach(e -> standAloneList.add((Integer) e));
        // perform the search
        int result = standAloneList.lastIndexOf(search);
        assertEquals(expected, result);
    }

    @ParameterizedTest(name = "[{index}] lastIndexOf({1}) → {2}")
    @MethodSource("additionalScenariosProvider")
    void lastIndexOf_additionalScenarios(List<Object> data, Object search, int expected) {
        // Populate stand‑alone list (generic type is Object for simplicity)
        data.forEach(e -> standAloneList.add((Integer) e));
        int result = standAloneList.lastIndexOf(search);
        assertEquals(expected, result);
    }

    @Test
    void lastIndexOf_throws_NullPointerException_whenInternalListIsNull() throws Exception {
        // Force the internal list reference to null
        setBackingList(wrapperList, null);
        assertThrows(NullPointerException.class, () -> wrapperList.lastIndexOf("any"), "When the internal list is null a NullPointerException should be propagated");
    }

    @Test
    void lastIndexOf_propagates_UnsupportedOperationException_fromBackingList() throws Exception {
        setBackingList(wrapperList, new ThrowingList<>());
        assertThrows(UnsupportedOperationException.class, () -> wrapperList.lastIndexOf("trigger"), "The exception from the backing list must be propagated");
    }

    @Test
    void lastIndexOf_propagates_RuntimeException_fromElementEquals() {
        // Build a stand‑alone list containing a BadEquals instance
        // cast to satisfy generic signature
        standAloneList.add((Integer) (Object) new BadEquals());
        // The search will trigger equals on the stored element
        assertThrows(RuntimeException.class, () -> standAloneList.lastIndexOf(new Object()), "RuntimeException from equals should propagate");
    }
}
