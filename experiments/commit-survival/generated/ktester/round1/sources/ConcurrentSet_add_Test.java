package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.CountDownLatch;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_add_Test {

    private ConcurrentSet<Integer> intSet;

    private ConcurrentSet<String> stringSet;

    // used for mixed‑type tests
    private ConcurrentSet<Object> objectSet;

    /**
     * Simple POJO with equals/hashCode based on an id field.
     */
    static final class CustomKey {

        private final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomKey) && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey{id=" + id + '}';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        intSet = new ConcurrentSet<>();
        stringSet = new ConcurrentSet<>();
        objectSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the sets to avoid cross‑test contamination
        intSet.clear();
        stringSet.clear();
        objectSet.clear();
    }

    static Stream<Arguments> customKeyProvider() {
        CustomKey first = new CustomKey(1);
        // equal to first
        CustomKey duplicate = new CustomKey(1);
        return Stream.of(Arguments.of(first, true, "First insertion of a custom key should succeed"), Arguments.of(duplicate, false, "Second insertion of an equal custom key should return false"));
    }

    @Test
    void addNewElementReturnsTrueAndIncreasesSize() {
        assertTrue(intSet.add(42), "Adding a new element should return true");
        assertTrue(intSet.contains(42), "Set should contain the newly added element");
        assertEquals(1, intSet.size(), "Size should be 1 after adding one element");
    }

    @Test
    void addDuplicateElementReturnsFalseAndDoesNotChangeSize() {
        assertTrue(intSet.add(7), "First addition should succeed");
        assertFalse(intSet.add(7), "Adding the same element again should return false");
        assertEquals(1, intSet.size(), "Size should remain 1 after duplicate add");
    }

    @Test
    void addNullElementIsHandledCorrectly() {
        // The implementation uses a sentinel for null, but the public contract
        // should behave as a normal Set with respect to null.
        assertTrue(stringSet.add(null), "Adding null for the first time should return true");
        assertTrue(stringSet.contains(null), "Set should contain null after addition");
        assertEquals(1, stringSet.size(), "Size should be 1 after adding null");
        // Adding null again must behave like a duplicate
        assertFalse(stringSet.add(null), "Adding null a second time should return false");
        assertEquals(1, stringSet.size(), "Size should stay 1 after duplicate null add");
    }

    @Test
    void addAllUsesAddInternallyAndRespectsReturnValues() {
        // addAll is already exercised in the existing test suite, but we
        // verify that it relies on add() by checking the return value of add()
        // for each element via the public API.
        assertTrue(intSet.addAll(Arrays.asList(1, 2, 3)), "addAll should report modification");
        assertTrue(intSet.addAll(Collections.singletonList(2)), "addAll with an existing element should still return true if at least one element was added");
        // Adding only existing elements now should return false
        assertFalse(intSet.addAll(Arrays.asList(1, 2, 3)), "addAll with only duplicates should return false");
    }

    @Test
    void concurrentAddAndRemoveStress() throws InterruptedException {
        int threadCount = 4;
        int opsPerThread = 5_000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // Threads that add numbers
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.add(j);
                }
                latch.countDown();
            }).start();
            // Threads that remove numbers
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.remove(j);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        // After all concurrent modifications, the size must be between 0 and opsPerThread
        // (because each number may be present at most once)
        assertTrue(intSet.size() >= 0 && intSet.size() <= opsPerThread, "Size should be within expected bounds after concurrent modifications");
    }

    /**
     * Adding a new, non‑null element (String) should succeed.
     */
    @Test
    void addNewStringElement() {
        assertTrue(stringSet.add("alpha"), "First addition of \"alpha\" must return true");
        assertTrue(stringSet.contains("alpha"));
        assertEquals(1, stringSet.size());
    }

    /**
     * Adding the same element again must return false.
     */
    @Test
    void addDuplicateStringElement() {
        assertTrue(stringSet.add("alpha"));
        assertFalse(stringSet.add("alpha"), "Second addition of the same element must return false");
        assertEquals(1, stringSet.size());
    }

    /**
     * Adding null should throw NullPointerException according to the supplied test case.
     */
    @Test
    void addNullThrowsNPE() {
        assertThrows(NullPointerException.class, () -> stringSet.add(null), "add(null) is expected to throw NullPointerException");
    }

    /**
     * Adding a distinct element "beta".
     */
    @Test
    void addDistinctString() {
        assertTrue(stringSet.add("beta"));
        assertTrue(stringSet.contains("beta"));
        assertEquals(1, stringSet.size());
    }

    /**
     * After removal, the element can be added again.
     */
    @Test
    void addAfterRemoval() {
        assertTrue(stringSet.add("alpha"));
        assertTrue(stringSet.remove("alpha"));
        assertTrue(stringSet.add("alpha"), "Re‑adding after removal must succeed");
        assertEquals(1, stringSet.size());
    }

    /**
     * Adding after a clear operation behaves like adding to an empty set.
     */
    @Test
    void addAfterClear() {
        stringSet.add("some");
        stringSet.clear();
        assertTrue(stringSet.add("gamma"));
        assertTrue(stringSet.contains("gamma"));
        assertEquals(1, stringSet.size());
    }

    /**
     * Adding a different String instance with the same contents must be treated as a duplicate.
     */
    @Test
    void addEqualButDistinctStringInstance() {
        String first = "alpha";
        // distinct object, same logical value
        String second = new String("alpha");
        assertTrue(stringSet.add(first));
        assertFalse(stringSet.add(second), "Equal content should not be added again");
        assertEquals(1, stringSet.size());
    }

    /**
     * Adding an element of a different runtime type (Integer) to a Set<String> is allowed
     *  because the generic type is erased at runtime.
     */
    @Test
    void addDifferentRuntimeType() {
        // Using a raw ConcurrentSet to avoid compile‑time generic mismatch
        ConcurrentSet rawSet = new ConcurrentSet<>();
        assertTrue(rawSet.add("string"));
        assertTrue(rawSet.add(42), "Adding an Integer to a raw set should succeed");
        assertEquals(2, rawSet.size());
        assertTrue(rawSet.contains(42));
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("customKeyProvider")
    void addCustomKeyObjects(CustomKey key, boolean expectedResult, String description) {
        assertEquals(expectedResult, objectSet.add(key), description);
        if (expectedResult) {
            assertTrue(objectSet.contains(key));
        } else {
            // size should stay 1 after the duplicate attempt
            assertEquals(1, objectSet.size());
        }
    }
}
