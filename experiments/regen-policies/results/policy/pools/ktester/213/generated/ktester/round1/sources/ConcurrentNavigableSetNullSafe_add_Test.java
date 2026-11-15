package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableSetNullSafe_add_Test {

    /**
     * The set under test – created fresh for each test method.
     */
    private NavigableSet<String> set;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default (natural ordering) constructor for most tests.
        set = new ConcurrentNavigableSetNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the set to aid GC and to guarantee isolation between tests.
        if (set != null) {
            set.clear();
        }
    }

    /**
     * Supplies the basic‑functionality test cases.
     */
    private static Stream<AddCase> basicFunctionalityCases() {
        return Stream.of(new AddCase("alpha", true, "Add a non‑null element that is not yet present – should be inserted and return true."), new AddCase("alpha", false, "Add the same non‑null element again – the underlying set already contains it, so add should return false."), new AddCase(null, true, "Add a null element for the first time – maskNull converts null to a sentinel object, which is not yet in the set, so add returns true."), new AddCase(null, false, "Add null a second time – the sentinel object is already stored, therefore add returns false."), new AddCase(UUID.fromString("00000000-0000-0000-0000-000000000000"), true, "Add a non‑null element of a type that implements Comparable (UUID) – should be added successfully."), new AddCase(new Object(), ClassCastException.class, "Add an element that does not implement Comparable to a set that uses natural ordering – internalSet.add will throw ClassCastException."));
    }

    /**
     * Cases for “add after removal”.
     */
    private static Stream<AddCase> addAfterRemovalCases() {
        return Stream.of(new AddCase("alpha", true, "Remove an existing element and then add it again; the set no longer contains the element, so add should return true."), new AddCase("alpha", false, "Add the same element a second time after it has just been added; now the element is present, so add returns false."));
    }

    /**
     * Cases for “add after clear”.
     */
    private static Stream<AddCase> addAfterClearCases() {
        return Stream.of(new AddCase("beta", true, "Clear the set, then add a new element; because the set is empty, add should succeed and return true."), new AddCase("beta", false, "Add the same element again after the clear; the element is already present, so add returns false."));
    }

    /**
     * Cases for “null after removal of sentinel”.
     */
    private static Stream<AddCase> nullAfterRemovalCases() {
        return Stream.of(new AddCase(null, true, "Add a null element for the first time; maskNull creates a sentinel that is not yet in the set, so add returns true."), new AddCase(null, true, "Remove the previously added null (sentinel) and then add null again; the sentinel is no longer present, so add returns true."), new AddCase(null, false, "Add null a third time without removal; sentinel is already stored, so add returns false."));
    }

    /**
     * Cases for “unsupported operation on underlying set”.
     */
    private static Stream<AddCase> unsupportedOperationCases() {
        return Stream.of(new AddCase("test", UnsupportedOperationException.class, "The internal NavigableSet is an unmodifiable view (e.g., created via Collections.unmodifiableNavigableSet). Invoking add should propagate UnsupportedOperationException."));
    }

    /**
     * Cases for “out‑of‑range element for subSet view”.
     */
    private static Stream<AddCase> outOfRangeCases() {
        return Stream.of(new AddCase(10, IllegalArgumentException.class, "The internal NavigableSet is a bounded subSet view (e.g., internalSet.subSet(1, true, 5, true)). Adding an element outside the defined range (10) must cause IllegalArgumentException."));
    }

    @Test
    void addNonNullElements_returnsTrueAndIncreasesSize() {
        assertTrue(set.add("apple"));
        assertTrue(set.add("banana"));
        assertEquals(2, set.size());
        // Adding a duplicate must return false and not change the size.
        assertFalse(set.add("apple"));
        assertEquals(2, set.size());
    }

    @Test
    void addNullElement_isSupportedAndIsOrderedAccordingToMasking() {
        // The implementation masks null internally, but from the API perspective null is a legitimate element.
        assertTrue(set.add(null));
        assertTrue(set.contains(null));
        assertEquals(1, set.size());
        // Adding another null must return false (duplicate).
        assertFalse(set.add(null));
        assertEquals(1, set.size());
        // Verify ordering – null is considered the smallest element when natural ordering is used.
        set.add("apple");
        Iterator<String> it = set.iterator();
        // null first
        assertNull(it.next());
        assertEquals("apple", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void addMixedNullAndNonNullElements_preservesAllElements() {
        assertTrue(set.add("banana"));
        assertTrue(set.add(null));
        assertTrue(set.add("apple"));
        assertTrue(set.add("cherry"));
        assertEquals(4, set.size());
        assertTrue(set.containsAll(Arrays.asList("banana", null, "apple", "cherry")));
        // Natural ordering: null, apple, banana, cherry
        Iterator<String> it = set.iterator();
        assertNull(it.next());
        assertEquals("apple", it.next());
        assertEquals("banana", it.next());
        assertEquals("cherry", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void addWithLengthComparator_ordersByStringLength_andHandlesNull() {
        Comparator<String> lengthComparator = Comparator.comparingInt(s -> s == null ? 0 : s.length());
        set = new ConcurrentNavigableSetNullSafe<>(lengthComparator);
        // length 4
        assertTrue(set.add("kiwi"));
        // length 5
        assertTrue(set.add("apple"));
        // length 0
        assertTrue(set.add(null));
        // length 6
        assertTrue(set.add("banana"));
        // Verify ordering according to the comparator.
        Iterator<String> it = set.iterator();
        // length 0
        assertNull(it.next());
        // length 4
        assertEquals("kiwi", it.next());
        // length 5
        assertEquals("apple", it.next());
        // length 6
        assertEquals("banana", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void collectionConstructor_usesAddToMaskNullElements() {
        Collection<String> source = Arrays.asList("alpha", null, "beta", "gamma");
        set = new ConcurrentNavigableSetNullSafe<>(source);
        assertEquals(4, set.size());
        assertTrue(set.containsAll(source));
        // Natural ordering: null, alpha, beta, gamma
        Iterator<String> it = set.iterator();
        assertNull(it.next());
        assertEquals("alpha", it.next());
        assertEquals("beta", it.next());
        assertEquals("gamma", it.next());
        assertFalse(it.hasNext());
    }

    @Test
    void addConcurrently_allElementsArePresentAndNoDuplicates() throws Exception {
        final int threadCount = 10;
        final int perThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger duplicateCount = new AtomicInteger(0);
        for (int i = 0; i < threadCount; i++) {
            final int threadId = i;
            executor.submit(() -> {
                try {
                    // ensure all threads start at the same moment
                    startLatch.await();
                    for (int j = 0; j < perThread; j++) {
                        int value = threadId * perThread + j;
                        boolean added = set.add(String.valueOf(value));
                        if (!added) {
                            duplicateCount.incrementAndGet();
                        }
                    }
                } catch (InterruptedException ignored) {
                } finally {
                    doneLatch.countDown();
                }
            });
        }
        // Release all workers
        startLatch.countDown();
        // Wait for completion
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "Threads did not finish in time");
        executor.shutdownNow();
        // Expect exactly threadCount * perThread distinct elements.
        assertEquals(threadCount * perThread, set.size(), "Set size does not match expected number of unique elements");
        assertEquals(0, duplicateCount.get(), "Unexpected duplicate additions detected");
    }

    @Test
    void addDuplicateElements_returnsFalseAndDoesNotChangeSet() {
        assertTrue(set.add("delta"));
        assertTrue(set.add(null));
        // Attempt to add duplicates
        assertFalse(set.add("delta"));
        assertFalse(set.add(null));
        // Size must remain unchanged
        assertEquals(2, set.size());
    }

    @Test
    void addWithCaseInsensitiveComparator_handlesEqualValues() {
        // Comparator that treats strings case‑insensitively.
        Comparator<String> ciComparator = String.CASE_INSENSITIVE_ORDER;
        set = new ConcurrentNavigableSetNullSafe<>(ciComparator);
        assertTrue(set.add("Test"));
        // "test" is considered equal to "Test" → duplicate → false
        assertFalse(set.add("test"));
        assertEquals(1, set.size());
        assertTrue(set.contains("Test"));
        assertTrue(set.contains("test"));
    }

    @Test
    void addWithComparatorThatThrowsClassCastException_onIncompatibleType() {
        // Comparator that blindly casts to String – will throw ClassCastException for non‑String.
        Comparator<Object> badComparator = (a, b) -> ((String) a).compareTo((String) b);
        // Use raw type to bypass generic checks.
        @SuppressWarnings("unchecked")
        NavigableSet<Object> rawSet = new ConcurrentNavigableSetNullSafe<>(badComparator);
        // The set should reject an Integer.
        assertThrows(ClassCastException.class, () -> rawSet.add(42));
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("basicFunctionalityCases")
    void addBasicFunctionality(AddCase tc) {
        if (tc.expected instanceof Class<?>) {
            // Expected exception
            Class<?> ex = (Class<?>) tc.expected;
            assertThrows(ex, () -> set.add((String) tc.element));
        } else {
            boolean expected = (Boolean) tc.expected;
            assertEquals(expected, set.add((String) tc.element), tc.description);
        }
    }

    @Test
    void addAfterRemoval() {
        // Prepare: add then remove
        assertTrue(set.add("alpha"));
        assertTrue(set.remove("alpha"));
        // Run parameterised checks
        for (AddCase tc : (Iterable<AddCase>) addAfterRemovalCases()::iterator) {
            assertEquals(tc.expected, set.add((String) tc.element), tc.description);
        }
    }

    @Test
    void addAfterClear() {
        set.add("beta");
        set.clear();
        // First addition after clear
        assertTrue(set.add("beta"));
        // Duplicate after clear
        assertFalse(set.add("beta"));
    }

    @Test
    void nullAfterRemovalOfSentinel() {
        // First add
        assertTrue(set.add(null));
        // Remove sentinel via remove(null)
        assertTrue(set.remove(null));
        // Add again – should succeed
        assertTrue(set.add(null));
        // Add once more – duplicate → false
        assertFalse(set.add(null));
    }

    @Test
    void addUnsupportedOperationPropagation() throws Exception {
        // Build an unmodifiable NavigableSet and inject it via reflection.
        NavigableSet<Object> base = new ConcurrentSkipListSet<>();
        NavigableSet<Object> unmodifiable = Collections.unmodifiableNavigableSet(base);
        ConcurrentNavigableSetNullSafe<String> custom = new ConcurrentNavigableSetNullSafe<>();
        // Use reflection to replace the private internalSet field.
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(custom, unmodifiable);
        // The add should now throw UnsupportedOperationException.
        assertThrows(UnsupportedOperationException.class, () -> custom.add("test"));
    }

    @Test
    void addOutOfRangeElementThrowsIllegalArgumentException() throws Exception {
        // Create a bounded sub‑set view of integers [1,5].
        NavigableSet<Integer> base = new ConcurrentSkipListSet<>(Arrays.asList(1, 2, 3, 4, 5));
        NavigableSet<Integer> bounded = base.subSet(1, true, 5, true);
        // Wrap it with ConcurrentNavigableSetNullSafe via reflection.
        @SuppressWarnings("unchecked")
        ConcurrentNavigableSetNullSafe<Integer> wrapper = new ConcurrentNavigableSetNullSafe<>();
        Field f = ConcurrentNavigableSetNullSafe.class.getDeclaredField("internalSet");
        f.setAccessible(true);
        f.set(wrapper, bounded);
        // Adding 10 is outside the range → IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> wrapper.add(10));
    }
}
