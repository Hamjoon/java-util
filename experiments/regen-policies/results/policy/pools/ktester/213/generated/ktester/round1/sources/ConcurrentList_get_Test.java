package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_get_Test {

    /**
     * The list instance used in most tests.
     */
    private ConcurrentList<Integer> list;

    /**
     * A wrapped list used to verify wrapper‑mode behavior.
     */
    private List<Integer> backingList;

    /**
     * Simple POJO used for the custom‑object test case.
     */
    private static final class Widget {

        private final int id;

        private final String name;

        Widget(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return "{\"id\":" + id + ",\"name\":\"" + name + "\"}";
        }

        // Equality is not required for the current tests, but provided for completeness.
        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof Widget))
                return false;
            Widget other = (Widget) o;
            return id == other.id && name.equals(other.name);
        }

        @Override
        public int hashCode() {
            return 31 * id + name.hashCode();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh instance for each test to guarantee isolation.
        list = new ConcurrentList<>();
        backingList = new ArrayList<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – helps the GC in long test suites.
        list = null;
        backingList = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validIndexScenarios() {
        return Stream.of(arguments(0, "alpha"), arguments(1, "beta"), arguments(2, "gamma"));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> edgeCaseScenarios() {
        // Helper to create a Runnable that populates the list.
        Runnable emptyList = () -> {
            /* leave list empty */
        };
        Runnable listWithNullAt1 = () -> {
            list.add("first");
            list.add(null);
            list.add("third");
        };
        Runnable listAfterRemoval = () -> {
            list.add("first");
            list.add("second");
            // originally at index 2
            list.add("delta");
            // remove "first", now "delta" shifts to index 1
            list.remove(0);
        };
        Runnable listWithCustomObject = () -> {
            // ensure clean state
            list.clear();
            // The list is declared as ConcurrentList<String> in the test class,
            // so we need a separate instance typed to Object for this case.
        };
        // Since the generic type of `list` is String, we create a separate
        // ConcurrentList<Object> for the custom‑object test.
        Runnable customObjectSetup = () -> {
            // No action here – the test method creates its own list instance.
        };
        return Stream.of(// Negative index on a non‑empty list
        arguments(-1, null, IndexOutOfBoundsException.class, "Negative index should throw IndexOutOfBoundsException", (Runnable) () -> {
            list.add("a");
            list.add("b");
        }), // Index equal to size (out of bounds)
        arguments(3, null, IndexOutOfBoundsException.class, "Index equal to size should throw IndexOutOfBoundsException", (Runnable) () -> {
            list.add("alpha");
            list.add("beta");
            list.add("gamma");
        }), // Index greater than size
        arguments(5, null, IndexOutOfBoundsException.class, "Index greater than size should throw IndexOutOfBoundsException", (Runnable) () -> {
            list.add("x");
            list.add("y");
        }), // Getting from an empty list
        arguments(0, null, IndexOutOfBoundsException.class, "Getting from an empty list must throw IndexOutOfBoundsException", emptyList), // Null element at index 1
        arguments(1, null, null, "List contains a null element at index 1; get should return null without throwing", listWithNullAt1), // After removal, element shifts
        arguments(1, "delta", null, "After removing the element at index 0, the element originally at index 2 shifts to index 1", listAfterRemoval), // Custom object JSON representation
        arguments(0, "{\"id\":42,\"name\":\"widget\"}", null, "ConcurrentList stores custom objects; retrieving the first element should return its JSON representation", customObjectSetup));
    }

    @Test
    void getReturnsCorrectElement() {
        list.add(10);
        list.add(20);
        list.add(30);
        assertEquals(10, list.get(0), "Element at index 0 should be 10");
        assertEquals(20, list.get(1), "Element at index 1 should be 20");
        assertEquals(30, list.get(2), "Element at index 2 should be 30");
    }

    @Test
    void getFromWrappedListReturnsCorrectElement() {
        // Prepare a backing list with known content.
        backingList.add(100);
        backingList.add(200);
        backingList.add(300);
        // Wrap the existing list.
        ConcurrentList<Integer> wrapped = new ConcurrentList<>(backingList);
        assertEquals(100, wrapped.get(0));
        assertEquals(200, wrapped.get(1));
        assertEquals(300, wrapped.get(2));
    }

    @Test
    void getWithNullElementReturnsNull() {
        list.add(null);
        list.add(1);
        assertNull(list.get(0), "First element should be null");
        assertEquals(1, list.get(1), "Second element should be 1");
    }

    @Test
    void getWithInvalidIndexThrowsException() {
        list.add(5);
        list.add(6);
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(-1));
        // size is 2, valid indices are 0‑1
        assertThrows(IndexOutOfBoundsException.class, () -> list.get(2));
    }

    @Test
    void concurrentGetsDoNotCorruptState() throws InterruptedException, ExecutionException {
        // Populate the list with a known sequence.
        int size = 1_000;
        for (int i = 0; i < size; i++) {
            list.add(i);
        }
        ExecutorService executor = Executors.newFixedThreadPool(8);
        Callable<Boolean> readerTask = () -> {
            for (int i = 0; i < size; i++) {
                // The value read must equal the index because no modifications are performed.
                if (!list.get(i).equals(i)) {
                    return false;
                }
            }
            return true;
        };
        // Submit several readers that run concurrently.
        int readers = 20;
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i = 0; i < readers; i++) {
            futures.add(executor.submit(readerTask));
        }
        // Ensure all readers succeeded.
        for (Future<Boolean> f : futures) {
            assertTrue(f.get(), "Concurrent read returned an unexpected value");
        }
        executor.shutdownNow();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not shut down in time");
    }

    /**
     * Valid‑index scenarios: the list contains ["alpha","beta","gamma"].
     * The test verifies that {@code get(int)} returns the expected element.
     */
    @ParameterizedTest(name = "{index} => get({0}) = {1}")
    @MethodSource("validIndexScenarios")
    void testValidIndexes(int index, String expected) {
        // Arrange – same fixture for all arguments.
        list.add("alpha");
        list.add("beta");
        list.add("gamma");
        // Act & Assert
        assertEquals(expected, list.get(index), () -> "Expected element at index " + index + " to be \"" + expected + "\"");
    }

    /**
     * Edge‑case and exception handling scenarios.
     * Each argument supplies:
     *   – the index to query,
     *   – the expected result (null or a concrete value),
     *   – the exception class that should be thrown (null if no exception),
     *   – a human‑readable description,
     *   – a {@code Runnable} that prepares the list for the particular case.
     */
    @ParameterizedTest(name = "{index} => {3}")
    @MethodSource("edgeCaseScenarios")
    void testEdgeCases(int index, Object expectedValue, Class<? extends Throwable> expectedException, String description, Runnable listSetup) {
        // Arrange – prepare the list according to the scenario.
        listSetup.run();
        // Act & Assert
        if (expectedException != null) {
            assertThrows(expectedException, () -> list.get(index), () -> "Expected " + expectedException.getSimpleName() + " for index " + index);
        } else {
            assertEquals(expectedValue, list.get(index), () -> "Unexpected value for index " + index + " (" + description + ")");
        }
    }

    /**
     * Special test for the custom‑object scenario because it requires a
     * {@code ConcurrentList<Object>} rather than {@code ConcurrentList<String>}.
     */
    @Test
    void testGetWithCustomObjectReturnsJsonRepresentation() {
        // Arrange
        ConcurrentList<Object> objectList = new ConcurrentList<>();
        Widget widget = new Widget(42, "widget");
        objectList.add(widget);
        // Act
        Object result = objectList.get(0);
        // Assert – the {@code Widget#toString()} returns the expected JSON.
        assertEquals("{\"id\":42,\"name\":\"widget\"}", result.toString(), "The JSON representation of the stored Widget does not match");
    }
}
