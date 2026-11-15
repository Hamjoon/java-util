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
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_contains_Test {

    /**
     * Mocked backing list used for wrapper‑mode tests **
     */
    @Mock
    private List<String> mockedBackingList;

    /**
     * Instance under test – will be created per test **
     */
    private ConcurrentList<String> listWithMock;

    /**
     * Instance under test – real underlying list (stand‑alone mode) **
     */
    private ConcurrentList<Integer> standaloneList;

    /**
     * Instance under test – standalone mode (real internal list) **
     */
    private ConcurrentList<Integer> standaloneIntList;

    /**
     * Instance under test – standalone mode (real internal list of Strings) **
     */
    private ConcurrentList<String> standaloneStringList;

    /**
     * Simple immutable point class with value‑based equals & hashCode
     */
    private static final class Point {

        final int x, y;

        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Point))
                return false;
            Point p = (Point) o;
            return x == p.x && y == p.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * Object whose equals method always throws a RuntimeException
     */
    private static final class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals failure");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Wrapper mode – use the mocked list
        listWithMock = new ConcurrentList<>(mockedBackingList);
        // Stand‑alone mode – use a real ArrayList internally
        standaloneIntList = new ConcurrentList<>();
        standaloneStringList = new ConcurrentList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset mocks to avoid cross‑test interference
        reset(mockedBackingList);
    }

    private static Stream<Arguments> provideStandaloneScenarios() {
        // Each Arguments: (list to populate, element to search, expectedResult, description)
        List<Object> empty = Collections.emptyList();
        return Stream.of(// element present
        Arguments.of(Arrays.asList("apple", "orange"), "apple", true, "The list contains the string \"apple\"; contains should return true."), // element absent
        Arguments.of(Arrays.asList("apple", "orange"), "banana", false, "The list does not contain \"banana\"; contains should return false."), // null element stored in list
        Arguments.of(Arrays.asList(null, "x"), null, true, "The list has a null entry; contains(null) must return true."), // null argument with no null in list
        Arguments.of(Arrays.asList("a", "b"), null, false, "The list does not contain null; contains(null) should be false."), // empty list
        Arguments.of(empty, "any", false, "When the list is empty, any contains check must return false."), // different object type
        Arguments.of(Arrays.asList("a", "b"), 123, false, "The list holds only strings; checking for an Integer should return false."), // duplicate elements
        Arguments.of(Arrays.asList("apple", "apple", "banana"), "apple", true, "The list contains \"apple\" twice; contains should still return true."), // element present via equals (different Integer instance)
        Arguments.of(Arrays.asList(42, 7), 42, true, "List holds Integer 42; a different Integer instance with same value should be equal."), // empty list with null argument
        Arguments.of(empty, null, false, "Empty list with null argument must return false."));
    }

    @Test
    void contains_DelegatesToBackingList_WhenElementIsPresent() {
        // Arrange
        when(mockedBackingList.contains("present")).thenReturn(true);
        // Act
        boolean result = listWithMock.contains("present");
        // Assert
        assertTrue(result, "contains should return true when backing list reports true");
        verify(mockedBackingList, times(1)).contains("present");
    }

    @Test
    void contains_DelegatesToBackingList_WhenElementIsAbsent() {
        // Arrange
        when(mockedBackingList.contains("absent")).thenReturn(false);
        // Act
        boolean result = listWithMock.contains("absent");
        // Assert
        assertFalse(result, "contains should return false when backing list reports false");
        verify(mockedBackingList, times(1)).contains("absent");
    }

    @Test
    void contains_DelegatesToBackingList_WithNullElement() {
        // Arrange
        when(mockedBackingList.contains(null)).thenReturn(true);
        // Act
        boolean result = listWithMock.contains(null);
        // Assert
        assertTrue(result, "contains should correctly handle null elements if backing list does");
        verify(mockedBackingList, times(1)).contains(null);
    }

    @Test
    void contains_ReturnsTrue_ForExistingElement_InStandaloneMode() {
        // Arrange
        standaloneList.add(42);
        // Act & Assert
        assertTrue(standaloneList.contains(42), "Standalone list should contain an element that was added");
    }

    @Test
    void contains_ReturnsFalse_ForMissingElement_InStandaloneMode() {
        // Arrange
        standaloneList.add(1);
        standaloneList.add(2);
        // Act & Assert
        assertFalse(standaloneList.contains(3), "Standalone list should not contain an element that was never added");
    }

    @Test
    void contains_ReturnsFalse_OnEmptyStandaloneList() {
        // No elements added – list is empty
        assertFalse(standaloneList.contains("anything"), "Empty list should not contain any element");
    }

    @Test
    void contains_HandlesNullElement_WhenUnderlyingListAllowsNulls() {
        // Arrange
        standaloneList.add(null);
        standaloneList.add("value");
        // Act & Assert
        assertTrue(standaloneList.contains(null), "Standalone list should correctly report containment of a null element");
        assertTrue(standaloneList.contains("value"), "Standalone list should still correctly report non‑null elements");
    }

    @Test
    void constructor_ThrowsIllegalArgumentException_WhenBackingListIsNull() {
        // Verify that the wrapper constructor validates its argument
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<>(null), "Constructor should reject a null backing list");
        assertEquals("List cannot be null", ex.getMessage());
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("provideStandaloneScenarios")
    void contains_StandaloneMode_VariousScenarios(List<Object> initialElements, Object element, boolean expected, String description) {
        // Populate the standalone list with the given elements
        ConcurrentList<Object> list = new ConcurrentList<>();
        initialElements.forEach(list::add);
        assertEquals(expected, list.contains(element), description);
    }

    @Test
    void contains_CustomObjectEquality() {
        ConcurrentList<Point> pointList = new ConcurrentList<>();
        pointList.add(new Point(1, 2));
        pointList.add(new Point(3, 4));
        assertTrue(pointList.contains(new Point(1, 2)), "A separate but equal Point(1,2) should make contains return true.");
    }

    @Test
    void contains_ConcurrentModificationSafety() throws Exception {
        ConcurrentList<String> concurrentList = new ConcurrentList<>();
        // Pre‑populate with some data
        concurrentList.add("initial");
        ExecutorService exec = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        // Thread 1: performs contains
        Future<Boolean> containsFuture = exec.submit(() -> {
            // wait for both threads to be ready
            startLatch.await();
            boolean result = concurrentList.contains("concurrent");
            doneLatch.countDown();
            return result;
        });
        // Thread 2: adds the element concurrently
        exec.submit(() -> {
            startLatch.await();
            concurrentList.add("concurrent");
            doneLatch.countDown();
        });
        // Release both threads
        startLatch.countDown();
        // Wait for both to finish and ensure no exception was thrown
        assertDoesNotThrow(() -> doneLatch.await(2, TimeUnit.SECONDS), "Both operations should complete without throwing.");
        // The result may be true or false depending on timing – we only assert no exception.
        // re‑throw any unexpected exception
        containsFuture.get();
        exec.shutdownNow();
    }

    @Test
    void contains_PropagatesRuntimeException_FromArgumentEquals() {
        ConcurrentList<BadEquals> list = new ConcurrentList<>();
        // element type matches
        list.add(new BadEquals());
        BadEquals arg = new BadEquals();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> list.contains(arg));
        assertEquals("equals failure", ex.getMessage());
    }

    @Test
    void contains_PropagatesException_FromBackingListContains() {
        // Backing list whose contains throws IllegalStateException
        List<String> throwingList = mock(List.class);
        when(throwingList.contains("trigger")).thenThrow(new IllegalStateException("triggered"));
        ConcurrentList<String> wrapper = new ConcurrentList<>(throwingList);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> wrapper.contains("trigger"));
        assertEquals("triggered", ex.getMessage());
    }

    @Test
    void contains_PropagatesError_FromBackingListContains() {
        // Backing list whose contains throws AssertionError
        List<String> errorList = mock(List.class);
        when(errorList.contains("error")).thenThrow(new AssertionError("assertion error"));
        ConcurrentList<String> wrapper = new ConcurrentList<>(errorList);
        AssertionError err = assertThrows(AssertionError.class, () -> wrapper.contains("error"));
        assertEquals("assertion error", err.getMessage());
    }

    @Test
    void contains_ThrowsNullPointerException_WhenInternalListIsNull() throws Exception {
        // Create a valid wrapper first, then nullify its private 'list' field via reflection
        ConcurrentList<String> wrapper = new ConcurrentList<>(new ArrayList<>());
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        // simulate corrupted internal state
        listField.set(wrapper, null);
        assertThrows(NullPointerException.class, () -> wrapper.contains("any"));
    }
}
