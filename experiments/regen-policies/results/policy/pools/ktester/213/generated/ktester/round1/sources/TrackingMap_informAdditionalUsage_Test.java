package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_informAdditionalUsage_Test {

    private TrackingMap<String, Integer> trackingMap;

    private Map<String, Integer> backingMap;

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the backing map for deterministic behaviour
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the test tidy
        trackingMap = null;
        backingMap = null;
    }

    /**
     * Supplies arguments for {@link #informAdditionalUsageParameterized}.
     */
    private static Stream<Arguments> provideCollectionsForInformAdditionalUsage() {
        return Stream.of(// distinct keys
        Arguments.of("non‑empty collection with distinct keys", Arrays.asList("A", "B", "C"), new HashSet<>(Arrays.asList("A", "B", "C"))), // empty collection – expect empty set (no change)
        Arguments.of("empty collection", Collections.emptyList(), Collections.emptySet()), // duplicate elements – Set should deduplicate
        Arguments.of("collection with duplicate elements", Arrays.asList("X", "Y", "X", "Y"), new HashSet<>(Arrays.asList("X", "Y"))), // null element inside collection – HashSet permits null
        Arguments.of("collection containing null element", Arrays.asList(null, "Z"), new HashSet<>(Arrays.asList(null, "Z"))));
    }

    /**
     * Verify that keys supplied via {@code informAdditionalUsage(Collection)} are recorded
     * in the {@code keysUsed()} set.
     */
    @Test
    void informAdditionalUsageAddsKeysToKeysUsed() {
        Collection<String> toMark = Arrays.asList("alpha", "beta");
        trackingMap.informAdditionalUsage(toMark);
        Set<String> used = trackingMap.keysUsed();
        assertEquals(2, used.size(), "Two keys should be recorded as used");
        assertTrue(used.containsAll(toMark), "Both supplied keys must be present");
    }

    /**
     * Supplying an empty collection must not change the {@code keysUsed()} set.
     */
    @Test
    void informAdditionalUsageWithEmptyCollectionLeavesKeysUsedUnchanged() {
        // Prime the set with a known key
        trackingMap.informAdditionalUsage(Collections.singleton("existing"));
        Set<String> before = new HashSet<>(trackingMap.keysUsed());
        // Call with empty collection
        trackingMap.informAdditionalUsage(Collections.emptyList());
        Set<String> after = trackingMap.keysUsed();
        assertEquals(before, after, "keysUsed should remain unchanged after empty collection");
    }

    /**
     * Duplicate keys in the supplied collection must not result in duplicate entries
     * in the {@code keysUsed()} set.
     */
    @Test
    void informAdditionalUsageDeduplicatesKeys() {
        Collection<String> duplicates = Arrays.asList("dup", "dup", "unique");
        trackingMap.informAdditionalUsage(duplicates);
        Set<String> used = trackingMap.keysUsed();
        assertEquals(2, used.size(), "Duplicate keys should be stored only once");
        assertTrue(used.containsAll(Arrays.asList("dup", "unique")));
    }

    /**
     * Keys that are not present in the backing map are still recorded as used.
     * This mirrors the behaviour verified in other tests for {@code get} and {@code containsKey}.
     */
    @Test
    void informAdditionalUsageRecordsNonexistentKeys() {
        // No entries are added to the backing map
        Collection<String> unknown = Collections.singletonList("ghost");
        trackingMap.informAdditionalUsage(unknown);
        Set<String> used = trackingMap.keysUsed();
        assertEquals(1, used.size());
        assertTrue(used.contains("ghost"));
    }

    /**
     * After calling {@code informAdditionalUsage}, the marked keys are considered
     * accessed by {@code expungeUnused()}, i.e., they are retained even if they
     * have never been accessed via {@code get} or {@code containsKey}.
     */
    @Test
    void informAdditionalUsagePreventsExpungeOfMarkedEntries() {
        // Populate the map with two entries
        trackingMap.put("keep", 1);
        trackingMap.put("remove", 2);
        // Mark only "keep" as used via informAdditionalUsage
        trackingMap.informAdditionalUsage(Collections.singleton("keep"));
        // Expunge should remove the unmarked entry
        trackingMap.expungeUnused();
        assertEquals(1, trackingMap.size(), "Only one entry should remain after expunge");
        assertTrue(trackingMap.containsKey("keep"));
        assertFalse(trackingMap.containsKey("remove"));
    }

    /**
     * Parameterized test that covers a variety of collection inputs:
     * <ul>
     *   <li>normal distinct keys</li>
     *   <li>empty collection</li>
     *   <li>duplicate elements</li>
     *   <li>collection containing a null element</li>
     * </ul>
     * The expected set is compared against {@code keysUsed()} after invocation.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideCollectionsForInformAdditionalUsage")
    void informAdditionalUsageParameterized(String description, Collection<String> input, Set<String> expected) {
        // Pre‑condition: ensure the map starts with an empty usage set
        assertTrue(trackingMap.keysUsed().isEmpty(), "Pre‑condition: keysUsed should be empty");
        trackingMap.informAdditionalUsage(input);
        Set<String> actual = trackingMap.keysUsed();
        assertEquals(expected, actual, "keysUsed set does not match expected for case: " + description);
    }

    /**
     * Passing a {@code null} reference as the collection argument must result in a
     * {@link NullPointerException}, because {@code Set.addAll(null)} throws NPE.
     */
    @Test
    void informAdditionalUsageWithNullCollectionThrowsNPE() {
        assertThrows(NullPointerException.class, () -> trackingMap.informAdditionalUsage(null), "Calling informAdditionalUsage with null should throw NullPointerException");
    }
}
