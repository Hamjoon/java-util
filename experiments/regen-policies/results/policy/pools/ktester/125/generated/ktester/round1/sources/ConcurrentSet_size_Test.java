package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentSet_size_Test {

    /**
     * Instance used by the majority of tests.
     */
    private ConcurrentSet<Integer> intSet;

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh, empty set before each test.
        intSet = new ConcurrentSet<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set in case a test mutates it and an exception prevents normal cleanup.
        if (intSet != null) {
            intSet.clear();
        }
    }

    /**
     * Helper to create a fresh ConcurrentSet populated with given elements.
     */
    private ConcurrentSet<String> createStringSet(String... elements) {
        ConcurrentSet<String> set = new ConcurrentSet<>();
        for (String e : elements) {
            set.add(e);
        }
        return set;
    }

    private static Stream<Arguments> provideAddElements() {
        return Stream.of(Arguments.of(1, 1, "After adding a single element, size() must return 1."), Arguments.of(3, 3, "After adding three distinct elements, size() must return 3."), // duplicate scenario: add a,b,a => size 2
        // handled specially below
        Arguments.of(0, 2, "Adding duplicate elements does not increase the set size; after adding \"a\", \"b\", then \"a\" again, size() should be 2."));
    }

    @Test
    @DisplayName("size() on a newly created set returns 0")
    void testSizeOnEmptySet() {
        // given – a fresh ConcurrentSet (created in @BeforeEach)
        // when
        int size = intSet.size();
        // then
        assertEquals(0, size, "Empty set should have size 0");
    }

    @Test
    @DisplayName("size() reflects number of distinct elements after adds")
    void testSizeAfterAdds() {
        // given
        assertTrue(intSet.add(10));
        assertTrue(intSet.add(20));
        // when
        int size = intSet.size();
        // then
        assertEquals(2, size, "Set should contain two distinct elements");
    }

    @Test
    @DisplayName("size() does not increase when adding duplicate elements")
    void testSizeIgnoresDuplicates() {
        // given
        assertTrue(intSet.add(5));
        assertFalse(intSet.add(5), "Adding a duplicate should return false");
        // when
        int size = intSet.size();
        // then
        assertEquals(1, size, "Size should remain 1 after attempting to add a duplicate");
    }

    @Test
    @DisplayName("size() after addAll reflects the collection size")
    void testSizeAfterAddAll() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        // when
        int size = intSet.size();
        // then
        assertEquals(4, size, "addAll should result in size equal to the source collection");
    }

    @Test
    @DisplayName("size() updates correctly after removals")
    void testSizeAfterRemovals() {
        // given
        intSet.addAll(Arrays.asList(1, 2, 3));
        assertTrue(intSet.remove(2));
        // when
        int size = intSet.size();
        // then
        assertEquals(2, size, "Size should decrease after a successful removal");
    }

    @Test
    @DisplayName("size() works with the constructor that accepts a Collection")
    void testSizeWithCollectionConstructor() {
        // given
        Set<Integer> source = new HashSet<>(Arrays.asList(7, 8, 9));
        ConcurrentSet<Integer> setFromCollection = new ConcurrentSet<>(source);
        // when
        int size = setFromCollection.size();
        // then
        assertEquals(3, size, "Constructor with Collection should initialise set with correct size");
    }

    @Test
    @DisplayName("size() works with the constructor that accepts an existing Set")
    void testSizeWithSetConstructor() {
        // given
        Set<Integer> source = new HashSet<>(Arrays.asList(11, 12));
        ConcurrentSet<Integer> setFromSet = new ConcurrentSet<>(source);
        // when
        int size = setFromSet.size();
        // then
        assertEquals(2, size, "Constructor with Set should initialise set with correct size");
    }

    @Test
    @DisplayName("size() under concurrent add/remove operations stays within expected bounds")
    void testSizeConcurrentAddRemove() throws InterruptedException {
        // given
        int threadCount = 4;
        int opsPerThread = 5_000;
        CountDownLatch latch = new CountDownLatch(threadCount * 2);
        // when – start adding threads
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.add(j);
                }
                latch.countDown();
            }).start();
            // and start removing threads
            new Thread(() -> {
                for (int j = 0; j < opsPerThread; j++) {
                    intSet.remove(j);
                }
                latch.countDown();
            }).start();
        }
        // wait for all threads to finish
        latch.await();
        // then – size must be between 0 and opsPerThread (inclusive)
        int finalSize = intSet.size();
        assertTrue(finalSize >= 0 && finalSize <= opsPerThread, "Final size should be within [0, " + opsPerThread + "], but was " + finalSize);
    }

    // ---------- size when set is empty ----------
    @Test
    @DisplayName("size() on a freshly constructed set is 0 (JSON group)")
    void jsonSizeWhenEmpty() {
        assertEquals(0, intSet.size(), "A newly created ConcurrentSet should report size 0 because it contains no elements.");
    }

    // ---------- size after adding elements ----------
    @ParameterizedTest(name = "{index}: add {0} elements, expect size {1}")
    @MethodSource("provideAddElements")
    @DisplayName("size after adding elements (JSON group)")
    void jsonSizeAfterAdding(int elementsToAdd, int expectedSize, String description) {
        for (int i = 0; i < elementsToAdd; i++) {
            intSet.add(i);
        }
        assertEquals(expectedSize, intSet.size(), description);
    }

    @Test
    @DisplayName("size after adding duplicates (JSON specific case)")
    void jsonSizeAfterAddingDuplicates() {
        ConcurrentSet<String> set = createStringSet();
        assertTrue(set.add("a"));
        assertTrue(set.add("b"));
        assertFalse(set.add("a"));
        assertEquals(2, set.size(), "Adding duplicate elements does not increase the set size; after adding \"a\", \"b\", then \"a\" again, size() should be 2.");
    }

    // ---------- size after removing elements ----------
    @Test
    @DisplayName("size after removing one of two elements (JSON group)")
    void jsonSizeAfterRemovingOne() {
        intSet.add(1);
        intSet.add(2);
        assertTrue(intSet.remove(1));
        assertEquals(1, intSet.size(), "Add two elements, remove one, then size() should reflect the remaining element count (1).");
    }

    @Test
    @DisplayName("size after removing all elements (JSON group)")
    void jsonSizeAfterRemovingAll() {
        intSet.add(1);
        intSet.add(2);
        intSet.remove(1);
        intSet.remove(2);
        assertEquals(0, intSet.size(), "Remove all elements that were added; size() must be 0.");
    }

    // ---------- size after clear operation ----------
    @Test
    @DisplayName("size after clear (JSON group)")
    void jsonSizeAfterClear() {
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        intSet.clear();
        assertEquals(0, intSet.size(), "After adding several elements and invoking clear(), size() must return 0.");
    }

    // ---------- size under concurrent modifications (basic thread safety check) ----------
    @Test
    @DisplayName("size after concurrent modifications returns non‑negative integer (JSON group)")
    void jsonSizeConcurrentBasic() throws InterruptedException {
        int threadCount = 8;
        int ops = 2_000;
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                for (int j = 0; j < ops; j++) {
                    intSet.add(j);
                    intSet.remove(j);
                }
                latch.countDown();
            }).start();
        }
        latch.await();
        int size = intSet.size();
        assertTrue(size >= 0, "Size must never be negative after concurrent modifications.");
    }

    // ---------- size after addAll with distinct elements ----------
    @Test
    @DisplayName("size after addAll distinct elements (JSON group)")
    void jsonSizeAddAllDistinct() {
        intSet.addAll(Arrays.asList(10, 20, 30, 40, 50));
        assertEquals(5, intSet.size(), "Add a collection containing five distinct elements via addAll; size() should return 5.");
    }

    // ---------- size after addAll with duplicate elements ----------
    @Test
    @DisplayName("size after addAll duplicate elements (JSON group)")
    void jsonSizeAddAllDuplicates() {
        intSet.addAll(Arrays.asList(1, 2, 2, 3, 1));
        assertEquals(3, intSet.size(), "Add a collection that contains duplicates via addAll; size() should count only unique elements and return 3.");
    }

    // ---------- size after removeAll with existing and non‑existing elements ----------
    @Test
    @DisplayName("size after removeAll (JSON group)")
    void jsonSizeAfterRemoveAll() {
        ConcurrentSet<String> set = createStringSet("a", "b", "c");
        set.removeAll(Arrays.asList("b", "d"));
        assertEquals(2, set.size(), "Start with a set {\"a\",\"b\",\"c\"}, call removeAll with {\"b\",\"d\"}; only \"b\" is removed, size() should be 2.");
    }

    // ---------- size after retainAll reduces set ----------
    @Test
    @DisplayName("size after retainAll (JSON group)")
    void jsonSizeAfterRetainAll() {
        ConcurrentSet<String> set = createStringSet("a", "b", "c");
        set.retainAll(Arrays.asList("b", "c", "d"));
        assertEquals(2, set.size(), "Start with a set {\"a\",\"b\",\"c\"}, call retainAll with {\"b\",\"c\",\"d\"}; only \"b\" and \"c\" remain, size() should be 2.");
    }

    // ---------- size after iterator remove ----------
    @Test
    @DisplayName("size after iterator.remove() (JSON group)")
    void jsonSizeAfterIteratorRemove() {
        intSet.addAll(Arrays.asList(1, 2, 3));
        Iterator<Integer> it = intSet.iterator();
        assertTrue(it.hasNext());
        // consume first element
        it.next();
        // remove the element just returned
        it.remove();
        assertEquals(2, intSet.size(), "Add three elements, obtain an iterator, call next() once and then iterator.remove(); the removed element is gone, size() should be 2.");
    }

    // ---------- size after clear then add again ----------
    @Test
    @DisplayName("size after clear then add (JSON group)")
    void jsonSizeAfterClearThenAdd() {
        intSet.addAll(Arrays.asList(5, 6, 7));
        intSet.clear();
        intSet.add(42);
        assertEquals(1, intSet.size(), "Add several elements, clear the set, then add a single new element; size() should be 1.");
    }

    // ---------- size after attempting to add null element ----------
    @Test
    @DisplayName("size after adding null throws NullPointerException (JSON group)")
    void jsonSizeAddNull() {
        assertThrows(NullPointerException.class, () -> intSet.add(null), "Calling add(null) on a ConcurrentSet should throw NullPointerException.");
        assertEquals(0, intSet.size(), "Size must remain 0 after failed null insertion.");
    }

    // ---------- size with large number of elements ----------
    @Test
    @DisplayName("size with 10,000 distinct elements (JSON group)")
    void jsonSizeLargeNumber() {
        for (int i = 0; i < 10_000; i++) {
            intSet.add(i);
        }
        assertEquals(10_000, intSet.size(), "Add 10,000 distinct integers; size() must correctly report 10,000.");
    }

    // ---------- size idempotence ----------
    @Test
    @DisplayName("size idempotence (JSON group)")
    void jsonSizeIdempotence() {
        intSet.addAll(Arrays.asList(1, 2, 3, 4));
        int first = intSet.size();
        int second = intSet.size();
        int third = intSet.size();
        assertEquals(4, first, "First size call should be 4.");
        assertEquals(first, second, "Subsequent size calls must be identical.");
        assertEquals(first, third, "Subsequent size calls must be identical.");
    }

    // ---------- size when internal set is null ----------
    @Test
    @DisplayName("size when internal set is null throws NullPointerException (JSON group)")
    void jsonSizeInternalNull() throws Exception {
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        // Use reflection to null out the private 'set' field
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(set, null);
        assertThrows(NullPointerException.class, set::size, "Invoking size() when internal set is null should throw NullPointerException.");
    }

    // ---------- size when underlying set throws UnsupportedOperationException ----------
    @Test
    @DisplayName("size propagates UnsupportedOperationException from underlying set (JSON group)")
    void jsonSizeUnderlyingThrows() throws Exception {
        // Create a mock Set that throws on size()
        @SuppressWarnings("unchecked")
        Set<Object> mockSet = mock(Set.class);
        when(mockSet.size()).thenThrow(new UnsupportedOperationException("size not supported"));
        ConcurrentSet<Integer> set = new ConcurrentSet<>();
        // Inject the mock via reflection
        Field field = ConcurrentSet.class.getDeclaredField("set");
        field.setAccessible(true);
        field.set(set, mockSet);
        assertThrows(UnsupportedOperationException.class, set::size, "ConcurrentSet.size() should propagate UnsupportedOperationException from the underlying set.");
    }
}
