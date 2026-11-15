package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.*;
import java.util.stream.Stream;

// Mockito is not required for these tests but kept from the template
@ExtendWith(MockitoExtension.class)
class TTLCache_hashCode_Test {

    private TTLCache<Integer, String> ttlCache;

    /**
     * Simple key type whose {@code hashCode()} deliberately throws.
     */
    private static final class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("BadKey hashCode failure");
        }
    }

    private static final class ArgumentsHolder {

        final List<Map.Entry<?, ?>> entries;

        final int expected;

        final String description;

        ArgumentsHolder(List<Map.Entry<?, ?>> entries, int expected, String description) {
            this.entries = entries;
            this.expected = expected;
            this.description = description;
        }
    }

    /**
     * Initialise a fresh cache before each test.  The cleanup interval is set
     * to a large value so the background purge thread does not interfere with
     * the deterministic expectations of the hash‑code calculation.
     */
    @BeforeEach
    void setupBeforeEach() {
        ttlCache = new TTLCache<>(10_000L, -1, 60_000L);
    }

    /**
     * Clear the cache after each test to avoid any side‑effects.
     */
    @AfterEach
    void teardownAfterEach() {
        ttlCache.clear();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Shut down the shared background thread used by all TTLCache instances.
        TTLCache.shutdown();
    }

    /**
     * Helper that reproduces the algorithm used inside {@code TTLCache.hashCode()}
     * using a list of entries in the order they would appear in the internal
     * doubly‑linked list (insertion order for a cache without LRU activity).
     */
    private static int computeExpectedHash(List<Map.Entry<Integer, String>> entries) {
        int hash = 1;
        for (Map.Entry<Integer, String> e : entries) {
            Object key = e.getKey();
            Object value = e.getValue();
            hash = 31 * hash + (key == null ? 0 : key.hashCode());
            hash = 31 * hash + (value == null ? 0 : value.hashCode());
        }
        return hash;
    }

    /**
     * Data for the "empty and single entry scenarios" group.
     */
    private static Stream<ArgumentsHolder> emptyAndSingleEntryProvider() {
        return Stream.of(// empty cache – expected 1 (already covered, but kept for completeness)
        new ArgumentsHolder(List.of(), 1, "Empty cache should yield seed value 1"), // single entry ("a", 1)
        new ArgumentsHolder(List.of(new AbstractMap.SimpleEntry<>("a", 1)), 3969, "Single entry with key \"a\" and value 1"), // two entries ("a",1), ("b",2)
        new ArgumentsHolder(List.of(new AbstractMap.SimpleEntry<>("a", 1), new AbstractMap.SimpleEntry<>("b", 2)), 3819109, "Two entries in insertion order"));
    }

    /**
     * Data for the "additional scenarios" group (null handling, etc.).
     */
    private static Stream<ArgumentsHolder> additionalScenariosProvider() {
        return Stream.of(// single entry (null, null) → hash stays 1
        new ArgumentsHolder(List.of(new AbstractMap.SimpleEntry<>(null, null)), 1, "Single entry with both key and value null"), // single entry (null, Integer 5) → hash = 31
        new ArgumentsHolder(List.of(new AbstractMap.SimpleEntry<>(null, 5)), 31, "Single entry with null key and non‑null value 5"), // single entry ("key", null) – we compute expected dynamically because
        // the hash of the string depends on the JDK implementation.
        new ArgumentsHolder(List.of(new AbstractMap.SimpleEntry<>("key", null)), computeExpectedHash(List.of(new AbstractMap.SimpleEntry<>("key", null))), "Single entry with non‑null key \"key\" and null value"));
    }

    @Test
    void testHashCodeEmptyCache() {
        // An empty cache should return the seed value (1) because the loop body never executes.
        assertEquals(1, ttlCache.hashCode(), "hashCode of an empty TTLCache must be the initial seed value 1");
    }

    @Test
    void testHashCodeSingleEntry() {
        ttlCache.put(1, "A");
        List<Map.Entry<Integer, String>> entries = new ArrayList<>();
        entries.add(new AbstractMap.SimpleEntry<>(1, "A"));
        int expected = computeExpectedHash(entries);
        assertEquals(expected, ttlCache.hashCode(), "hashCode does not match the expected value for a single entry");
    }

    @Test
    void testHashCodeMultipleEntriesPreservesInsertionOrder() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        List<Map.Entry<Integer, String>> entries = new ArrayList<>();
        entries.add(new AbstractMap.SimpleEntry<>(1, "A"));
        entries.add(new AbstractMap.SimpleEntry<>(2, "B"));
        entries.add(new AbstractMap.SimpleEntry<>(3, "C"));
        int expected = computeExpectedHash(entries);
        assertEquals(expected, ttlCache.hashCode(), "hashCode should reflect insertion order when no LRU activity occurs");
    }

    @Test
    void testHashCodeAfterRemoval() {
        ttlCache.put(1, "A");
        ttlCache.put(2, "B");
        ttlCache.put(3, "C");
        // Remove the middle entry.
        ttlCache.remove(2);
        List<Map.Entry<Integer, String>> entries = new ArrayList<>();
        entries.add(new AbstractMap.SimpleEntry<>(1, "A"));
        entries.add(new AbstractMap.SimpleEntry<>(3, "C"));
        int expected = computeExpectedHash(entries);
        assertEquals(expected, ttlCache.hashCode(), "hashCode must be recomputed correctly after an entry is removed");
    }

    @Test
    void testHashCodeWithNullKeyAndValue() {
        ttlCache.put(null, null);
        ttlCache.put(1, null);
        ttlCache.put(null, "X");
        List<Map.Entry<Integer, String>> entries = new ArrayList<>();
        entries.add(new AbstractMap.SimpleEntry<>(null, null));
        entries.add(new AbstractMap.SimpleEntry<>(1, null));
        entries.add(new AbstractMap.SimpleEntry<>(null, "X"));
        int expected = computeExpectedHash(entries);
        assertEquals(expected, ttlCache.hashCode(), "hashCode should correctly handle null keys and/or null values");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("emptyAndSingleEntryProvider")
    void testHashCodeEmptyAndSingleEntryScenarios(ArgumentsHolder holder) {
        // Populate the cache using the generic Object types from the holder.
        holder.entries.forEach(e -> ttlCache.put((Integer) e.getKey(), (String) e.getValue()));
        // The expected value is either pre‑computed (numeric) or we compute it on‑the‑fly.
        int actual = ttlCache.hashCode();
        assertEquals(holder.expected, actual, "hashCode mismatch for scenario: " + holder.description);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("additionalScenariosProvider")
    void testAdditionalScenarios(ArgumentsHolder holder) {
        holder.entries.forEach(e -> ttlCache.put((Integer) e.getKey(), (String) e.getValue()));
        assertEquals(holder.expected, ttlCache.hashCode(), "hashCode mismatch for additional scenario: " + holder.description);
    }

    @Test
    void testHashCodePropagatesExceptionFromKeyHashCode() {
        // Insert a key that will throw when its hashCode() is invoked.
        ttlCache.put((Integer) (Object) new BadKey(), "value");
        // The call to TTLCache.hashCode() should propagate the IllegalStateException.
        IllegalStateException ex = assertThrows(IllegalStateException.class, ttlCache::hashCode, "Exception from key.hashCode() must propagate");
        assertEquals("BadKey hashCode failure", ex.getMessage());
    }

    @Test
    void testHashCodeIsThreadSafeAgainstConcurrentModification() throws Exception {
        // Populate the cache with many entries to make hashCode take a noticeable amount of time.
        int entryCount = 10_000;
        for (int i = 0; i < entryCount; i++) {
            ttlCache.put(i, "V" + i);
        }
        // Latch to make the mutating thread wait until hashCode has started.
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        // This thread will attempt to put a new entry while the main thread computes hashCode.
        executor.submit(() -> {
            try {
                // Signal that we are ready and then wait for the main thread to start hashing.
                startLatch.await();
                // Attempt a mutation – this will block on the cache's lock.
                ttlCache.put(entryCount + 1, "extra");
            } catch (InterruptedException ignored) {
            } finally {
                doneLatch.countDown();
            }
        });
        // Main thread: compute hashCode while the other thread is waiting.
        int hash;
        try {
            // Release the other thread after we have entered hashCode (the lock is held now).
            startLatch.countDown();
            hash = ttlCache.hashCode();
        } finally {
            // Ensure the mutating thread finishes before we finish the test.
            doneLatch.await(5, TimeUnit.SECONDS);
            executor.shutdownNow();
        }
        // No ConcurrentModificationException should have been thrown; hash must be a valid int.
        // Verify that the hash matches the expected value computed from the original entries.
        List<Map.Entry<Integer, String>> originalEntries = new ArrayList<>();
        for (int i = 0; i < entryCount; i++) {
            originalEntries.add(new AbstractMap.SimpleEntry<>(i, "V" + i));
        }
        int expected = computeExpectedHash(originalEntries);
        assertEquals(expected, hash, "hashCode should reflect the snapshot taken before the concurrent mutation");
    }
}
