package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
class ConcurrentList_hashCode_Test {

    /**
     * Instance that uses the default no‑arg constructor (wraps an internal ArrayList).
     */
    private ConcurrentList<Integer> defaultList;

    /**
     * Instance that wraps a Mockito mock List – used to verify delegation.
     */
    private ConcurrentList<Integer> mockWrappedList;

    /**
     * Mocked underlying List.
     */
    @Mock
    private List<Integer> mockList;

    private static final class FixedHash {

        private final int value;

        FixedHash(int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            // deterministic hash
            return value;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof FixedHash) && ((FixedHash) o).value == value;
        }
    }

    private static class IllegalStateConcurrentList<E> extends ConcurrentList<E> {

        IllegalStateConcurrentList(List<E> delegate) {
            super(delegate);
        }

        @Override
        protected <T> T readOperation(Supplier<T> operation) {
            throw new IllegalStateException("forced");
        }
    }

    private static class AssertionErrorConcurrentList<E> extends ConcurrentList<E> {

        AssertionErrorConcurrentList(List<E> delegate) {
            super(delegate);
        }

        @Override
        protected <T> T readOperation(Supplier<T> operation) {
            throw new AssertionError("forced assertion");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Fresh list for each test to avoid cross‑test interference.
        defaultList = new ConcurrentList<>();
        // The mock is injected by MockitoExtension; we wrap it here.
        mockWrappedList = new ConcurrentList<>(mockList);
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stale references linger.
        defaultList = null;
        mockWrappedList = null;
    }

    @Test
    void hashCodeEmptyListMatchesUnderlying() {
        // An empty ConcurrentList should have the same hashCode as an empty ArrayList.
        List<Integer> reference = new ArrayList<>();
        assertEquals(reference.hashCode(), defaultList.hashCode(), "hashCode of an empty ConcurrentList must equal hashCode of an empty underlying list");
    }

    @Test
    void hashCodeWithElementsMatchesUnderlying() {
        // Populate both the reference list and the ConcurrentList with identical elements.
        List<Integer> reference = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            reference.add(i);
            defaultList.add(i);
        }
        assertEquals(reference.hashCode(), defaultList.hashCode(), "hashCode should reflect the contents of the list");
    }

    @Test
    void hashCodeChangesWhenUnderlyingListChanges() {
        // Initial state
        defaultList.add(1);
        defaultList.add(2);
        int firstHash = defaultList.hashCode();
        // Mutate the list
        defaultList.add(3);
        int secondHash = defaultList.hashCode();
        assertNotEquals(firstHash, secondHash, "hashCode must change after the list content changes");
    }

    @Test
    void hashCodeDelegatesToWrappedList() {
        // Arrange: stub the mock to return a deterministic value.
        int expectedHash = 123456;
        when(mockList.hashCode()).thenReturn(expectedHash);
        // Act
        int actualHash = mockWrappedList.hashCode();
        // Assert: the value comes directly from the wrapped list.
        assertEquals(expectedHash, actualHash, "ConcurrentList.hashCode() must delegate to the wrapped List's hashCode()");
        verify(mockList, times(1)).hashCode();
    }

    @Test
    void hashCodeThrowsWhenWrappedListIsNull() {
        // The constructor validates null arguments, but we double‑check behavior.
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> new ConcurrentList<Integer>(null));
        assertEquals("List cannot be null", ex.getMessage(), "Constructor should reject a null underlying list");
    }

    @Test
    void hashCodePropagatesRuntimeExceptionFromUnderlying() {
        // Simulate a faulty underlying list whose hashCode throws.
        when(mockList.hashCode()).thenThrow(new RuntimeException("boom"));
        RuntimeException ex = assertThrows(RuntimeException.class, () -> mockWrappedList.hashCode(), "hashCode() should propagate RuntimeException from the wrapped list");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void hashCodeAfterAddRemoveMatchesUnderlying() {
        // Add ["a", "b", "c"], remove element at index 1 ("b").
        defaultList.add(0, 10);
        defaultList.add(1, 20);
        defaultList.add(2, 30);
        // removes 20
        defaultList.remove(1);
        List<Integer> reference = new ArrayList<>(Arrays.asList(10, 30));
        assertEquals(reference.hashCode(), defaultList.hashCode(), "hashCode must reflect list after add and remove operations");
    }

    @Test
    void hashCodeAfterClearMatchesUnderlying() {
        defaultList.addAll(Arrays.asList(1, 2, 3, 4));
        defaultList.clear();
        List<Integer> empty = new ArrayList<>();
        assertEquals(empty.hashCode(), defaultList.hashCode(), "After clear() the hashCode must equal that of an empty underlying list");
    }

    @Test
    void hashCodeHandlesNullElements() {
        defaultList.add(null);
        defaultList.add(42);
        defaultList.add(null);
        List<Integer> reference = new ArrayList<>();
        reference.add(null);
        reference.add(42);
        reference.add(null);
        assertEquals(reference.hashCode(), defaultList.hashCode(), "hashCode must correctly handle null elements inside the list");
    }

    @Test
    void hashCodeReflectsCustomElementHashCodes() {
        FixedHash a = new FixedHash(42);
        FixedHash b = new FixedHash(42);
        FixedHash c = new FixedHash(42);
        defaultList.addAll(Arrays.asList(a, b, c));
        List<FixedHash> reference = new ArrayList<>(Arrays.asList(a, b, c));
        assertEquals(reference.hashCode(), defaultList.hashCode(), "hashCode must incorporate custom element hashCode implementations");
    }

    @Test
    void hashCodeIdempotentWhenUnchanged() {
        defaultList.addAll(Arrays.asList(1, 2, 3));
        int first = defaultList.hashCode();
        int second = defaultList.hashCode();
        int third = defaultList.hashCode();
        assertEquals(first, second, "Repeated hashCode calls must return the same value");
        assertEquals(second, third, "Repeated hashCode calls must return the same value");
    }

    @Test
    void hashCodeConcurrentAccessSafety() throws InterruptedException {
        final ConcurrentList<Integer> shared = new ConcurrentList<>();
        final int threadCount = 20;
        final int operationsPerThread = 1_000;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicBoolean failed = new AtomicBoolean(false);
        List<Future<?>> futures = new ArrayList<>();
        // Writer threads (add/remove)
        for (int i = 0; i < 5; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    Random rand = new Random();
                    for (int j = 0; j < operationsPerThread; j++) {
                        if (rand.nextBoolean()) {
                            shared.add(rand.nextInt());
                        } else {
                            if (!shared.isEmpty()) {
                                int idx = rand.nextInt(shared.size());
                                shared.remove(idx);
                            }
                        }
                    }
                } catch (Exception e) {
                    failed.set(true);
                }
            }));
        }
        // Reader threads (hashCode)
        for (int i = 0; i < 15; i++) {
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    for (int j = 0; j < operationsPerThread; j++) {
                        // The call itself must not throw.
                        shared.hashCode();
                    }
                } catch (Exception e) {
                    failed.set(true);
                }
            }));
        }
        // Start all threads simultaneously.
        startLatch.countDown();
        // Wait for completion.
        for (Future<?> f : futures) {
            // re‑throws any exception
            f.get();
        }
        executor.shutdownNow();
        assertFalse(failed.get(), "Concurrent access should not cause any exception");
    }

    @Test
    void hashCodeReadOperationThrowsIllegalStateException() {
        IllegalStateConcurrentList<Integer> list = new IllegalStateConcurrentList<>(new ArrayList<>());
        IllegalStateException ex = assertThrows(IllegalStateException.class, list::hashCode, "hashCode must propagate IllegalStateException from readOperation");
        assertEquals("forced", ex.getMessage());
    }

    @Test
    void hashCodeReadOperationThrowsAssertionError() {
        AssertionErrorConcurrentList<Integer> list = new AssertionErrorConcurrentList<>(new ArrayList<>());
        AssertionError err = assertThrows(AssertionError.class, list::hashCode, "hashCode must propagate AssertionError from readOperation");
        assertEquals("forced assertion", err.getMessage());
    }

    @Test
    void hashCodeThrowsWhenInternalListIsNull() throws Exception {
        // Use reflection to set the private 'list' field to null.
        Field listField = ConcurrentList.class.getDeclaredField("list");
        listField.setAccessible(true);
        listField.set(defaultList, null);
        // Expect a NullPointerException when hashCode tries to delegate.
        assertThrows(NullPointerException.class, defaultList::hashCode, "hashCode should throw NPE if internal list reference is null");
    }
}
