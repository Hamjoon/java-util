package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_isEmpty_Test {

    /**
     * Instance under test – created fresh before each test method.
     */
    private ConcurrentSet<Integer> set;

    @BeforeEach
    void setupBeforeEach() {
        // Create a new, empty ConcurrentSet for every test.
        set = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set and release the reference.
        if (set != null) {
            set.clear();
        }
        set = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> behaviorProvider() {
        return Stream.of(arguments("A newly created ConcurrentSet should be empty; isEmpty() must return true.", true, BehaviorScenario.NEW_SET), arguments("After adding a single element to the set, isEmpty() must return false.", false, BehaviorScenario.AFTER_ADD_SINGLE), arguments("After adding elements and then clearing the set, isEmpty() must return true again.", true, BehaviorScenario.AFTER_ADD_CLEAR), arguments("After adding a collection of elements via addAll(), isEmpty() must return false.", false, BehaviorScenario.AFTER_ADDALL), arguments("After adding a collection then removing all elements via removeAll(), isEmpty() must return true.", true, BehaviorScenario.AFTER_REMOVEALL), arguments("After adding elements and retaining a subset via retainAll(), the set is still non‑empty, so isEmpty() returns false.", false, BehaviorScenario.AFTER_RETAINALL));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> edgeProvider() {
        return Stream.of(arguments("Calling addAll() with an empty collection leaves the set empty; isEmpty() should return true.", true, EdgeScenario.ADDALL_EMPTY), arguments("addAll() with a non‑empty collection makes the set non‑empty; isEmpty() returns false.", false, EdgeScenario.ADDALL_NONEMPTY), arguments("removeAll() with an empty collection on a non‑empty set does not change the set; isEmpty() stays false.", false, EdgeScenario.REMOVEALL_EMPTY), arguments("removeAll() with a collection that contains all current elements empties the set; isEmpty() returns true.", true, EdgeScenario.REMOVEALL_ALL), arguments("Adding two distinct elements, then removing one with remove(Object) leaves one element; isEmpty() returns false.", false, EdgeScenario.REMOVE_ONE_LEFT), arguments("After the previous step, removing the remaining element with remove(Object) makes the set empty; isEmpty() returns true.", true, EdgeScenario.REMOVE_LAST), arguments("retainAll() with a collection that shares no elements with the set clears the set; isEmpty() returns true.", true, EdgeScenario.RETAINALL_NONE), arguments("Using an iterator to traverse a single‑element set and calling iterator.remove() removes that element; isEmpty() returns true.", true, EdgeScenario.ITERATOR_REMOVE), arguments("Calling clear() on an already empty set leaves it empty; isEmpty() returns true.", true, EdgeScenario.CLEAR_ALREADY_EMPTY), arguments("Attempting to add(null) throws NullPointerException; the set remains empty, so subsequent isEmpty() would be true.", true, EdgeScenario.ADD_NULL), arguments("Attempting to addAll() a collection containing null throws NullPointerException; the set remains unchanged (empty).", true, EdgeScenario.ADDALL_WITH_NULL), arguments("Calling remove(Object) with an element that is not present does not modify an empty set; isEmpty() stays true.", true, EdgeScenario.REMOVE_NONEXISTENT));
    }

    @Test
    @DisplayName("isEmpty() returns true for a newly created set")
    void testIsEmptyOnNewSet() {
        Assertions.assertTrue(set.isEmpty(), "A newly created set must be empty");
        Assertions.assertEquals(0, set.size(), "Size of a new set must be zero");
    }

    @Test
    @DisplayName("isEmpty() returns false after adding an element")
    void testIsEmptyAfterAdd() {
        set.add(42);
        Assertions.assertFalse(set.isEmpty(), "Set should not be empty after adding an element");
        Assertions.assertEquals(1, set.size(), "Size should be 1 after one addition");
    }

    @Test
    @DisplayName("isEmpty() returns true after removing the only element")
    void testIsEmptyAfterRemove() {
        set.add(7);
        Assertions.assertFalse(set.isEmpty(), "Set should not be empty after addition");
        set.remove(7);
        Assertions.assertTrue(set.isEmpty(), "Set should be empty after removing the only element");
        Assertions.assertEquals(0, set.size(), "Size should be zero after removal");
    }

    @Test
    @DisplayName("isEmpty() returns true after clear()")
    void testIsEmptyAfterClear() {
        set.addAll(Arrays.asList(1, 2, 3));
        Assertions.assertFalse(set.isEmpty(), "Set should not be empty before clear");
        set.clear();
        Assertions.assertTrue(set.isEmpty(), "Set should be empty after clear");
        Assertions.assertEquals(0, set.size(), "Size must be zero after clear");
    }

    @Test
    @DisplayName("isEmpty() works correctly under concurrent add/remove")
    void testIsEmptyConcurrent() throws InterruptedException {
        final int threadCount = 4;
        final int opsPerThread = 5_000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // Threads that add numbers
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    set.add(j);
                }
                latch.countDown();
            }).start();
            // Threads that remove numbers
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    set.remove(j);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        // After all concurrent activity, the set may be empty or contain some
        // residual elements, but the contract of isEmpty() must still be consistent
        // with size().
        Assertions.assertEquals(set.isEmpty(), set.size() == 0, "isEmpty() must agree with size() after concurrent modifications");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("behaviorProvider")
    void testBehaviorScenarios(String description, boolean expected, BehaviorScenario scenario) {
        // Arrange & Act according to the scenario
        switch(scenario) {
            case NEW_SET:
                // nothing to do – set is already new and empty
                break;
            case AFTER_ADD_SINGLE:
                set.add(1);
                break;
            case AFTER_ADD_CLEAR:
                set.addAll(Arrays.asList(10, 20, 30));
                set.clear();
                break;
            case AFTER_ADDALL:
                set.addAll(Arrays.asList(5, 6, 7));
                break;
            case AFTER_REMOVEALL:
                set.addAll(Arrays.asList(8, 9));
                set.removeAll(Arrays.asList(8, 9));
                break;
            case AFTER_RETAINALL:
                set.addAll(Arrays.asList(11, 12, 13));
                set.retainAll(Collections.singleton(11));
                break;
            default:
                throw new IllegalArgumentException("Unhandled scenario: " + scenario);
        }
        // Assert
        assertEquals(expected, set.isEmpty(), "Scenario failed: " + description);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeProvider")
    void testEdgeScenarios(String description, boolean expectedAfter, EdgeScenario scenario) {
        switch(scenario) {
            case ADDALL_EMPTY:
                set.addAll(Collections.emptyList());
                break;
            case ADDALL_NONEMPTY:
                set.addAll(Arrays.asList(100, 200));
                break;
            case REMOVEALL_EMPTY:
                set.addAll(Arrays.asList(1, 2, 3));
                set.removeAll(Collections.emptyList());
                break;
            case REMOVEALL_ALL:
                set.addAll(Arrays.asList(4, 5));
                set.removeAll(Arrays.asList(4, 5));
                break;
            case REMOVE_ONE_LEFT:
                set.addAll(Arrays.asList(6, 7));
                set.remove(6);
                break;
            case REMOVE_LAST:
                set.addAll(Arrays.asList(8, 9));
                set.remove(8);
                set.remove(9);
                break;
            case RETAINALL_NONE:
                set.addAll(Arrays.asList(10, 11));
                // no common elements
                set.retainAll(Collections.singleton(99));
                break;
            case ITERATOR_REMOVE:
                set.add(12);
                Iterator<Integer> it = set.iterator();
                assertTrue(it.hasNext(), "Iterator should have an element");
                it.next();
                it.remove();
                break;
            case CLEAR_ALREADY_EMPTY:
                // already empty
                set.clear();
                break;
            case ADD_NULL:
                assertThrows(NullPointerException.class, () -> set.add(null), "Adding null should throw NullPointerException");
                break;
            case ADDALL_WITH_NULL:
                List<Integer> listWithNull = new ArrayList<>();
                listWithNull.add(null);
                assertThrows(NullPointerException.class, () -> set.addAll(listWithNull), "addAll with null element should throw NullPointerException");
                break;
            case REMOVE_NONEXISTENT:
                // set is empty; remove a non‑present element
                set.remove(12345);
                break;
            default:
                throw new IllegalArgumentException("Unhandled edge scenario: " + scenario);
        }
        // For the two exception scenarios we already verified the exception; now
        // we simply assert that the set is still empty.
        assertEquals(expectedAfter, set.isEmpty(), "Scenario failed: " + description);
    }
}
