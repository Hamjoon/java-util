package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.IntStream;

@ExtendWith(MockitoExtension.class)
class CompactSet_builder_Test {

    /**
     * Verify that the static builder() method returns a fresh Builder instance
     * and that the resulting CompactSet is a functional Set.
     */
    @Test
    void testBuilderCreatesNonNullCompactSet() {
        CompactSet<String> set = CompactSet.<String>builder().build();
        assertNotNull(set, "Builder should produce a non‑null CompactSet");
        assertTrue(set.isEmpty(), "Newly built set should be empty");
        assertEquals(0, set.size(), "Size of a new set must be zero");
    }

    /**
     * Verify that two consecutive calls to builder() produce independent builders.
     */
    @Test
    void testBuilderInstancesAreIndependent() {
        CompactSet.Builder<String> b1 = CompactSet.<String>builder().caseSensitive(false).sortedOrder().compactSize(30);
        // default builder
        CompactSet.Builder<String> b2 = CompactSet.<String>builder();
        CompactSet<String> set1 = b1.build();
        CompactSet<String> set2 = b2.build();
        // set1 is case‑insensitive, set2 is case‑sensitive (default)
        set1.add("foo");
        set2.add("foo");
        assertTrue(set1.contains("FOO"), "Case‑insensitive set should contain upper‑case variant");
        assertFalse(set2.contains("FOO"), "Case‑sensitive set should not contain upper‑case variant");
    }

    /**
     * Verify that a case‑insensitive, sorted CompactSet respects case‑insensitivity
     * and returns its elements in sorted order regardless of insertion order.
     */
    @Test
    void testCaseInsensitiveSortedBuilderBehavior() {
        CompactSet<String> set = CompactSet.<String>builder().caseSensitive(// make it case‑insensitive
        false).// request sorted ordering
        sortedOrder().compactSize(// custom compact size (behavior is internal)
        50).build();
        // Add elements in a non‑sorted order
        set.add("zebra");
        set.add("apple");
        set.add("monkey");
        // Case‑insensitivity check
        assertTrue(set.contains("ZEBRA"), "Set should be case‑insensitive and contain upper‑case key");
        assertTrue(set.contains("APPLE"));
        assertTrue(set.contains("MONKEY"));
        // Verify iteration order is sorted (lexicographically)
        List<String> iterationOrder = new ArrayList<>();
        for (Iterator<String> it = set.iterator(); it.hasNext(); ) {
            iterationOrder.add(it.next());
        }
        List<String> expectedOrder = List.of("apple", "monkey", "zebra");
        assertEquals(expectedOrder, iterationOrder, "Iterator should return elements in sorted order for a sorted CompactSet");
    }

    /**
     * Verify that the insertion‑order configuration preserves the order of insertion.
     */
    @Test
    void testInsertionOrderBuilderBehavior() {
        CompactSet<String> set = CompactSet.<String>builder().// request insertion ordering
        insertionOrder().compactSize(60).build();
        set.add("first");
        set.add("second");
        set.add("third");
        List<String> iterationOrder = new ArrayList<>();
        for (String s : set) {
            iterationOrder.add(s);
        }
        List<String> expectedOrder = List.of("first", "second", "third");
        assertEquals(expectedOrder, iterationOrder, "Iterator should preserve insertion order when configured for insertionOrder()");
    }

    /**
     * Verify that the builder can be used to create a CompactSet from an existing collection.
     */
    @Test
    void testBuilderWithInitialCollection() {
        List<String> source = List.of("alpha", "beta", "gamma");
        CompactSet<String> set = CompactSet.<String>builder().caseSensitive(true).insertionOrder().build();
        // Populate via addAll (the Set interface provides this)
        set.addAll(source);
        assertEquals(3, set.size(), "Set should contain all elements from the source collection");
        assertTrue(set.containsAll(source), "Set should contain each element from the source collection");
    }

    /**
     * Basic contract: {@code CompactSet.builder()} must never return {@code null}
     * and must return an instance of the nested {@code Builder} class.
     */
    @Test
    void testBuilderReturnsNonNullBuilderInstance() {
        CompactSet.Builder<Object> builder = CompactSet.builder();
        assertNotNull(builder, "builder() should return a non‑null Builder instance");
        assertTrue(builder instanceof CompactSet.Builder, "Returned object must be an instance of CompactSet$Builder");
    }

    /**
     * Two successive invocations must yield distinct Builder objects (different references).
     */
    @Test
    void testSuccessiveBuilderCallsReturnDistinctInstances() {
        CompactSet.Builder<String> first = CompactSet.builder();
        CompactSet.Builder<String> second = CompactSet.builder();
        assertNotSame(first, second, "Each call to builder() should return a fresh Builder instance");
    }

    /**
     * Generic type inference – explicit type arguments should produce a Builder
     * whose compile‑time type matches the requested generic type.
     *
     * Runtime verification is limited to ensuring the instance is non‑null;
     * the compile‑time check is performed by the Java compiler.
     */
    @Test
    void testBuilderWithExplicitStringGeneric() {
        CompactSet.Builder<String> stringBuilder = CompactSet.<String>builder();
        assertNotNull(stringBuilder, "Builder<String> should not be null");
    }

    @Test
    void testBuilderWithExplicitIntegerGeneric() {
        CompactSet.Builder<Integer> intBuilder = CompactSet.<Integer>builder();
        assertNotNull(intBuilder, "Builder<Integer> should not be null");
    }

    /**
     * Concurrency test – when multiple threads invoke {@code builder()} concurrently,
     * each thread must receive its own distinct Builder instance.
     */
    @Test
    void testBuilderIsThreadSafeAndReturnsDistinctInstances() throws Exception {
        final int THREAD_COUNT = 20;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        // Use a thread‑safe set to collect the Builder references
        Set<CompactSet.Builder<?>> builderSet = ConcurrentHashMap.newKeySet();
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(THREAD_COUNT);
        IntStream.range(0, THREAD_COUNT).forEach(i -> executor.submit(() -> {
            try {
                // wait until all threads are ready
                startLatch.await();
                CompactSet.Builder<?> b = CompactSet.builder();
                builderSet.add(b);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        }));
        // Release all threads simultaneously
        startLatch.countDown();
        // Wait for all tasks to finish
        assertTrue(doneLatch.await(5, TimeUnit.SECONDS), "All builder calls should complete in time");
        // Verify we have exactly THREAD_COUNT distinct Builder instances
        assertEquals(THREAD_COUNT, builderSet.size(), "Each thread should receive a distinct Builder instance");
        executor.shutdownNow();
    }
}
