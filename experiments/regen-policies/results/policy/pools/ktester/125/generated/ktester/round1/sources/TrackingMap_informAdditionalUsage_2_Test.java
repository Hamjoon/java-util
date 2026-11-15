package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_informAdditionalUsage_2_Test {

    private TrackingMap<String, Integer> primary;

    private TrackingMap<String, Integer> secondary;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh pair of maps backed by independent HashMaps
        primary = new TrackingMap<>(new HashMap<>());
        secondary = new TrackingMap<>(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no test leaks references
        primary = null;
        secondary = null;
    }

    // -----------------------------------------------------------------------
    private static Stream<MergeScenario> mergeScenarios() {
        return Stream.of(// 1. empty additional – already covered by a dedicated test, but kept for completeness
        new MergeScenario(// primary accessed keys before
        Set.of("a"), // additional accessed keys
        Set.of(), // expected after merge
        Set.of("a"), "empty additional map should not change primary"), // 2. additional with {"b","c"} – no overlap
        new MergeScenario(Set.of("a"), Set.of("b", "c"), Set.of("a", "b", "c"), "merge non‑overlapping keys"), // 3. additional with {"b","c"} – overlapping "b"
        new MergeScenario(Set.of("a", "b"), Set.of("b", "c"), Set.of("a", "b", "c"), "merge overlapping keys without duplication"));
    }

    /**
     * Verify that {@code informAdditionalUsage} merges the set of accessed keys
     * from the supplied {@link TrackingMap} into the calling instance.
     */
    @Test
    void testInformAdditionalUsageMergesReadKeys() {
        // Populate both maps
        primary.put("a", 1);
        primary.put("b", 2);
        secondary.put("c", 3);
        secondary.put("d", 4);
        // Access a subset of keys in each map
        // marks "a"
        primary.get("a");
        // marks "b"
        primary.containsKey("b");
        // marks "c"
        secondary.get("c");
        // Before merging, each map only knows its own accessed keys
        Set<String> primaryKeysBefore = primary.keysUsed();
        Set<String> secondaryKeysBefore = secondary.keysUsed();
        assertEquals(Set.of("a", "b"), primaryKeysBefore, "Primary map should have recorded accesses to 'a' and 'b' only");
        assertEquals(Set.of("c"), secondaryKeysBefore, "Secondary map should have recorded access to 'c' only");
        // Merge secondary's usage into primary
        primary.informAdditionalUsage(secondary);
        // After merging, primary should contain the union of both key sets
        Set<String> primaryKeysAfter = primary.keysUsed();
        assertEquals(Set.of("a", "b", "c"), primaryKeysAfter, "Primary map should have merged accessed keys from secondary");
        // Ensure secondary remains unchanged
        assertEquals(Set.of("c"), secondary.keysUsed(), "Secondary map's recorded keys must remain unchanged");
    }

    /**
     * When the additional {@link TrackingMap} has not recorded any key usage,
     * the calling instance's usage set must stay unchanged.
     */
    @Test
    void testInformAdditionalUsageWithEmptyAdditionalDoesNotAlterPrimary() {
        primary.put("x", 10);
        primary.put("y", 20);
        // Access only one key in primary
        primary.get("x");
        // Secondary has no accesses at all
        secondary.put("z", 30);
        // (no get/containsKey on secondary)
        Set<String> before = primary.keysUsed();
        assertEquals(Set.of("x"), before, "Primary should have only 'x' recorded before merge");
        primary.informAdditionalUsage(secondary);
        Set<String> after = primary.keysUsed();
        assertEquals(Set.of("x"), after, "Primary's recorded keys must stay the same when merging an empty usage map");
    }

    /**
     * Passing {@code null} to {@code informAdditionalUsage} must result in a
     * {@link NullPointerException}. The method contract does not allow a null argument.
     */
    @Test
    void testInformAdditionalUsageNullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> primary.informAdditionalUsage(null), "Calling informAdditionalUsage(null) should throw NullPointerException");
    }

    /**
     * Verify that calling {@code informAdditionalUsage} with the same instance
     * (self‑reference) does not change the set and does not throw.
     */
    @Test
    void testInformAdditionalUsageSelfReferenceIsNoOp() {
        primary.put("a", 1);
        // record "a"
        primary.get("a");
        Set<String> before = primary.keysUsed();
        assertEquals(Set.of("a"), before, "Pre‑condition: only 'a' is recorded");
        // Self‑reference – should be a no‑op
        assertDoesNotThrow(() -> primary.informAdditionalUsage(primary), "Self‑reference should not throw");
        Set<String> after = primary.keysUsed();
        assertEquals(Set.of("a"), after, "Self‑reference must leave keysUsed unchanged");
    }

    /**
     * Primary map starts empty; after informing usage from a map that has read keys
     * {@code {"x","y"}}, the primary should contain exactly those keys.
     */
    @Test
    void testInformAdditionalUsageIntoEmptyPrimary() {
        // secondary will have accessed keys "x" and "y"
        secondary.put("x", 100);
        secondary.put("y", 200);
        secondary.get("x");
        secondary.containsKey("y");
        assertTrue(primary.keysUsed().isEmpty(), "Primary should start with no recorded keys");
        primary.informAdditionalUsage(secondary);
        assertEquals(Set.of("x", "y"), primary.keysUsed(), "Primary should now contain the keys recorded in secondary");
    }

    /**
     * Simulate a primary map whose internal {@code readKeys} set is wrapped with
     * {@link Collections#unmodifiableSet}. Adding additional keys should trigger
     * {@link UnsupportedOperationException}.
     */
    @Test
    void testInformAdditionalUsageWithUnmodifiablePrimaryReadKeysThrowsUOE() throws Exception {
        // Prepare secondary with a single accessed key
        secondary.put("k", 1);
        secondary.get("k");
        // Replace primary's private readKeys with an unmodifiable set via reflection
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> modifiable = new HashSet<>();
        readKeysField.set(primary, Collections.unmodifiableSet(modifiable));
        // The call should attempt to addAll to the unmodifiable set and fail
        assertThrows(UnsupportedOperationException.class, () -> primary.informAdditionalUsage(secondary), "Adding to an unmodifiable readKeys set must throw UnsupportedOperationException");
    }

    /**
     * The argument map is non‑null but its internal {@code readKeys} field has been
     * set to {@code null} via reflection. The method dereferences
     * {@code additional.readKeys} and therefore should throw {@link NullPointerException}.
     */
    @Test
    void testInformAdditionalUsageAdditionalWithNullReadKeysThrowsNPE() throws Exception {
        // Use reflection to null‑out secondary's readKeys field
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(secondary, null);
        // Primary has some usage already – not important for the exception
        primary.put("a", 1);
        primary.get("a");
        assertThrows(NullPointerException.class, () -> primary.informAdditionalUsage(secondary), "If additional.readKeys is null, a NullPointerException must be thrown");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("mergeScenarios")
    void testInformAdditionalUsageParameterized(MergeScenario scenario) {
        // Populate primary with its keys and mark them as accessed
        for (String k : scenario.primaryKeys) {
            primary.put(k, k.length());
            // mark as accessed
            primary.get(k);
        }
        // Populate secondary (the additional map) similarly
        for (String k : scenario.additionalKeys) {
            secondary.put(k, k.length());
            // mark as accessed
            secondary.get(k);
        }
        // Perform the merge
        primary.informAdditionalUsage(secondary);
        // Verify the resulting set matches expectation
        assertEquals(scenario.expectedKeys, primary.keysUsed(), "After merge: " + scenario.description);
    }
}
