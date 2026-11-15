package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_keysUsed_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private TrackingMap<String, String> trackingMap;

    /**
     * A simple backing map used to construct the {@link TrackingMap}.
     */
    private Map<String, String> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a plain HashMap as the backing implementation – it supports null keys/values.
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear references – helps the GC when many tests run.
        trackingMap = null;
        backingMap = null;
    }

    /**
     * Supplies arguments for {@link #keysUsedBasicFunctionality(String, List, Set, boolean)}.
     */
    private static Stream<Arguments> basicFunctionalityProvider() {
        return Stream.of(// 1. freshly created map – empty set
        Arguments.of("Initially empty set", Collections.emptyList(), Collections.emptySet(), false), // 2. single get() access
        Arguments.of("Single key accessed via get()", List.of("alpha"), Set.of("alpha"), false), // 3. multiple distinct keys accessed via get()
        Arguments.of("Multiple distinct keys accessed via get()", List.of("beta", "gamma"), Set.of("beta", "gamma"), false), // 4. repeated calls to keysUsed() should return the same mutable Set instance
        Arguments.of("Repeated calls to keysUsed() return same mutable Set instance", List.of("delta"), Set.of("delta"), true));
    }

    @Test
    @DisplayName("keysUsed() is empty for a freshly constructed TrackingMap")
    void keysUsedInitiallyEmpty() {
        Set<String> used = trackingMap.keysUsed();
        assertNotNull(used, "keysUsed() must never return null");
        assertTrue(used.isEmpty(), "A new TrackingMap should have no used keys");
    }

    @Test
    @DisplayName("keysUsed() records a key accessed via get()")
    void keysUsedAfterGetExistingKey() {
        backingMap.put("alpha", "A");
        backingMap.put("beta", "B");
        // Access one of the keys
        assertEquals("A", trackingMap.get("alpha"));
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Only the accessed key should be recorded");
        assertTrue(used.contains("alpha"), "The accessed key must be present in keysUsed()");
    }

    @Test
    @DisplayName("keysUsed() records a non‑existent key accessed via get()")
    void keysUsedAfterGetNonExistingKey() {
        backingMap.put("x", "X");
        // returns null, but should still be recorded
        trackingMap.get("nonexistent");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "A missing key accessed via get() must be recorded");
        assertTrue(used.contains("nonexistent"));
    }

    @Test
    @DisplayName("keysUsed() records a key accessed via containsKey()")
    void keysUsedAfterContainsKeyExisting() {
        backingMap.put("one", "1");
        backingMap.put("two", "2");
        assertTrue(trackingMap.containsKey("two"));
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size());
        assertTrue(used.contains("two"));
    }

    @Test
    @DisplayName("keysUsed() records a non‑existent key accessed via containsKey()")
    void keysUsedAfterContainsKeyNonExisting() {
        backingMap.put("a", "A");
        trackingMap.containsKey("missing");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Missing key accessed via containsKey() must be recorded");
        assertTrue(used.contains("missing"));
    }

    @Test
    @DisplayName("keysUsed() does not duplicate entries when the same key is accessed repeatedly")
    void keysUsedNoDuplicatesOnRepeatedAccess() {
        backingMap.put("dup", "D");
        // Access the same key three times using different capitalisations (case‑insensitive map not used here)
        trackingMap.get("dup");
        trackingMap.get("dup");
        trackingMap.containsKey("dup");
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Repeated accesses of the same key must not create duplicates");
        assertTrue(used.contains("dup"));
    }

    @Test
    @DisplayName("keysUsed() records a null key when the underlying map permits it")
    void keysUsedAfterNullKeyAccess() {
        // HashMap allows null keys – put a null key/value pair.
        backingMap.put(null, "nullValue");
        // Access the null key via get()
        assertEquals("nullValue", trackingMap.get(null));
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "Null key access must be recorded");
        assertTrue(used.contains(null), "The recorded key set must contain the null key");
    }

    @Test
    @DisplayName("keysUsed() records a key whose value is null")
    void keysUsedWhenValueIsNull() {
        backingMap.put("empty", null);
        // Access the key – value is null but access counts.
        assertNull(trackingMap.get("empty"));
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size());
        assertTrue(used.contains("empty"));
    }

    @Test
    @DisplayName("keysUsed() reflects accesses made before expungeUnused()")
    void keysUsedUnaffectedByExpungeUnused() {
        backingMap.put("keep", "yes");
        backingMap.put("drop", "no");
        // Access only "keep"
        trackingMap.get("keep");
        // Expunge unused entries – should remove "drop"
        trackingMap.expungeUnused();
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size(), "keysUsed() should still contain only the accessed key after expunge");
        assertTrue(used.contains("keep"));
        assertFalse(used.contains("drop"));
        // Verify the map state matches expectation
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("keep"));
    }

    /**
     * Parameterised test that covers the "basic functionality" group.
     *
     * <p>The arguments are:
     * <ul>
     *   <li>description – a human‑readable explanation (used only for the display name)</li>
     *   <li>keysToAccess – a list of keys that will be accessed via {@code get()}</li>
     *   <li>expectedKeys – the set of keys that {@code keysUsed()} is expected to contain after the accesses</li>
     *   <li>checkReferenceEquality – when true, the test also asserts that two successive calls to {@code keysUsed()}
     *       return the exact same {@link Set} instance (reference equality)</li>
     * </ul>
     * </p>
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("basicFunctionalityProvider")
    void keysUsedBasicFunctionality(String description, List<String> keysToAccess, Set<String> expectedKeys, boolean checkReferenceEquality) {
        // Populate the backing map with dummy values for every key we will access.
        keysToAccess.forEach(k -> backingMap.put(k, "value-" + k));
        // Perform the accesses.
        keysToAccess.forEach(trackingMap::get);
        // First call to keysUsed()
        Set<String> firstCall = trackingMap.keysUsed();
        assertNotNull(firstCall, "keysUsed() must never return null");
        assertEquals(expectedKeys, firstCall, "keysUsed() should contain exactly the accessed keys");
        // If required, verify that the returned Set instance is the same on subsequent calls.
        if (checkReferenceEquality) {
            Set<String> secondCall = trackingMap.keysUsed();
            assertSame(firstCall, secondCall, "keysUsed() must return the same Set instance on successive calls");
        }
        // Additional sanity check: the internal set must be mutable – add a new key via get()
        // and ensure the previously obtained reference reflects the change.
        if (!keysToAccess.isEmpty()) {
            String newKey = "newKey";
            backingMap.put(newKey, "newValue");
            trackingMap.get(newKey);
            assertTrue(firstCall.contains(newKey), "The Set returned earlier should reflect later accesses (mutability)");
        }
    }

    /**
     * Verifies that calling {@code put()} does **not** record usage.
     *
     * <p>This corresponds to the JSON group
     * <em>"TrackingMap.keysUsed() - no reads after puts"</em>.</p>
     */
    @Test
    @DisplayName("keysUsed() remains empty after put() without any reads")
    void keysUsedRemainsEmptyAfterPutOnly() {
        // Put a key/value pair – no read operation performed.
        trackingMap.put("x", "123");
        Set<String> used = trackingMap.keysUsed();
        assertNotNull(used, "keysUsed() must never return null");
        assertTrue(used.isEmpty(), "put() should not be considered a read; keysUsed() must stay empty");
    }

    /**
     * Additional test for {@link TrackingMap#informAdditionalUsage(Collection)}.
     *
     * <p>While not part of the original JSON, this scenario ensures that the
     * {@code keysUsed()} set can be extended via the public API that merges
     * usage information from another source.</p>
     */
    @Test
    @DisplayName("informAdditionalUsage(Collection) correctly merges external keys")
    void informAdditionalUsageMergesKeys() {
        // Access a couple of keys via the map itself.
        backingMap.put("a", "A");
        backingMap.put("b", "B");
        trackingMap.get("a");
        // Create an external collection of keys that were used elsewhere.
        // "c" does not exist in the backing map
        Collection<String> external = List.of("b", "c");
        // Merge the external usage.
        trackingMap.informAdditionalUsage(external);
        Set<String> used = trackingMap.keysUsed();
        // Expected set now contains "a" (from get), "b" and "c" (from informAdditionalUsage)
        assertEquals(Set.of("a", "b", "c"), used);
    }
}
