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
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_contains_Test {

    // standalone mode
    private ConcurrentList<Integer> emptyList;

    // standalone mode with data
    private ConcurrentList<Integer> populatedList;

    // wrapper mode around a real list
    private ConcurrentList<Integer> wrapperList;

    @Mock
    private List<Integer> // used to verify delegation
    mockedBackingList;

    class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new ClassCastException("bad equals");
        }
    }

    class BadEqualsRuntime {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("boom");
        }
    }

    class BrokenConcurrentList<E> extends ConcurrentList<E> {

        @Override
        protected <T> T readOperation(Supplier<T> operation) {
            throw new IllegalStateException("read failed");
        }
    }

    class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new ClassCastException("bad equals");
        }
    }

    class BadEqualsRuntime {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("boom");
        }
    }

    class BrokenConcurrentList<E> extends ConcurrentList<E> {

        @Override
        protected <T> T readOperation(Supplier<T> operation) {
            throw new IllegalStateException("read failed");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Stand‑alone empty list
        emptyList = new ConcurrentList<>();
        // Stand‑alone list pre‑populated with a few values
        populatedList = new ConcurrentList<>();
        populatedList.add(10);
        populatedList.add(20);
        populatedList.add(30);
        // Wrapper around a regular ArrayList (to test that the wrapper works)
        List<Integer> backing = new ArrayList<>();
        backing.add(100);
        backing.add(200);
        wrapperList = new ConcurrentList<>(backing);
    }

    private static Stream<Arguments> provideContainsOnEmptyList() {
        return Stream.of(Arguments.of(null, false), Arguments.of("anyString", false));
    }

    private static Stream<Arguments> provideContainsExistingElement() {
        return Stream.of(Arguments.of(42, true));
    }

    private static Stream<Arguments> provideContainsNonExistingElement() {
        return Stream.of(Arguments.of(99, false));
    }

    @Test
    void containsReturnsTrueWhenElementIsPresent_StandaloneEmpty() {
        emptyList.add(42);
        assertTrue(emptyList.contains(42), "contains should return true for an element that was added");
    }

    @Test
    void containsReturnsFalseWhenElementIsAbsent_StandalonePopulated() {
        assertFalse(populatedList.contains(999), "contains should return false for an element that is not in the list");
    }

    @Test
    void containsHandlesNullElementWhenBackingListAllowsNull() {
        ConcurrentList<String> listWithNull = new ConcurrentList<>();
        listWithNull.add(null);
        assertTrue(listWithNull.contains(null), "contains should correctly detect a null element");
    }

    @Test
    void containsWorksCorrectlyInWrapperMode() {
        // wrapperList already contains 100 and 200
        assertTrue(wrapperList.contains(100), "Wrapper mode should delegate contains to the backing list");
        assertFalse(wrapperList.contains(300), "Wrapper mode should correctly report missing elements");
    }

    @Test
    void containsDelegatesToBackingListWhenWrapped() {
        // Use the Mockito‑mocked backing list
        ConcurrentList<Integer> listUnderTest = new ConcurrentList<>(mockedBackingList);
        when(mockedBackingList.contains(5)).thenReturn(true);
        boolean result = listUnderTest.contains(5);
        assertTrue(result, "contains should return the value supplied by the backing list");
        verify(mockedBackingList, times(1)).contains(5);
    }

    @Test
    void constructorRejectsNullBackingList() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<Integer>(null), "Constructor should reject a null backing list");
        assertEquals("List cannot be null", ex.getMessage());
    }

    // Group: contains_onEmptyList
    @ParameterizedTest(name = "{index} => o={0}, expected={1}")
    @MethodSource("provideContainsOnEmptyList")
    void contains_onEmptyList(Object o, boolean expected) {
        // emptyList is freshly created in @BeforeEach and contains no elements
        assertEquals(expected, emptyList.contains(o), "Empty list should return " + expected + " for element: " + o);
    }

    // Group: contains_existingElement
    @ParameterizedTest(name = "{index} => o={0}, expected={1}")
    @MethodSource("provideContainsExistingElement")
    void contains_existingElement(Object o, boolean expected) {
        // Populate with the element first
        ConcurrentList<Object> list = new ConcurrentList<>();
        list.add(o);
        assertEquals(expected, list.contains(o), "List containing the element should return true");
    }

    // Group: contains_nonExistingElement
    @ParameterizedTest(name = "{index} => o={0}, expected={1}")
    @MethodSource("provideContainsNonExistingElement")
    void contains_nonExistingElement(Object o, boolean expected) {
        // populatedList does not contain 99
        assertEquals(expected, populatedList.contains(o), "List should return false for non‑existing element");
    }

    // Group: contains_nullElementPresent
    @Test
    void contains_nullElementPresent() {
        ConcurrentList<Object> list = new ConcurrentList<>();
        list.add(null);
        assertTrue(list.contains(null), "List with a null entry must return true for contains(null)");
    }

    // Group: contains_duplicateElements
    @Test
    void contains_duplicateElements() {
        ConcurrentList<String> list = new ConcurrentList<>();
        list.add("dup");
        list.add("dup");
        assertTrue(list.contains("dup"), "contains must be true even when the element appears multiple times");
    }

    // Group: contains_wrongType
    @Test
    void contains_wrongType() {
        // List holds Integers, we search for a String
        assertFalse(populatedList.contains("stringWhenListIsInteger"), "Searching with a different type should safely return false");
    }

    // Group: contains_equalsThrowsClassCastException
    @Test
    void contains_equalsThrowsClassCastException() {
        // Element whose equals throws ClassCastException
        class BadEquals {

            @Override
            public boolean equals(Object o) {
                throw new ClassCastException("bad equals");
            }
        }
        ConcurrentList<Object> list = new ConcurrentList<>();
        list.add(new BadEquals());
        assertThrows(ClassCastException.class, () -> list.contains("anyString"), "contains should propagate ClassCastException thrown from equals");
    }

    // Group: contains_equalsThrowsRuntimeException
    @Test
    void contains_equalsThrowsRuntimeException() {
        class BadEqualsRuntime {

            @Override
            public boolean equals(Object o) {
                throw new RuntimeException("boom");
            }
        }
        ConcurrentList<Object> list = new ConcurrentList<>();
        list.add(new BadEqualsRuntime());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.contains(42), "contains should propagate RuntimeException thrown from equals");
        assertEquals("boom", ex.getMessage());
    }

    // Group: contains_underlyingListNull
    @Test
    void contains_underlyingListNull() throws Exception {
        ConcurrentList<Integer> list = new ConcurrentList<>();
        // Use reflection to set the private field `list` to null
        Field field = ConcurrentList.class.getDeclaredField("list");
        field.setAccessible(true);
        field.set(list, null);
        assertThrows(NullPointerException.class, () -> list.contains(null), "When internal list is null, contains should throw NPE");
    }

    // Group: contains_readOperationThrows
    @Test
    void contains_readOperationThrows() {
        // Subclass that overrides readOperation to throw an exception.
        // The real implementation declares readOperation as private, but for the purpose of
        // this test we assume it is at least package‑protected so that overriding is possible.
        class BrokenConcurrentList<E> extends ConcurrentList<E> {

            @Override
            protected <T> T readOperation(Supplier<T> operation) {
                throw new IllegalStateException("read failed");
            }
        }
        BrokenConcurrentList<String> broken = new BrokenConcurrentList<>();
        // normal add (uses writeOperation, which we did not override)
        broken.add("test");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> broken.contains("test"), "Exception from overridden readOperation must propagate");
        assertEquals("read failed", ex.getMessage());
    }
}
