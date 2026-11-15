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

    private TrackingMap<String, String> primaryMap;

    private TrackingMap<String, String> secondaryMap;

    @BeforeEach
    void setupBeforeEach() {
        // Each test gets a fresh pair of TrackingMap instances backed by simple HashMaps
        primaryMap = new TrackingMap<>(new HashMap<>());
        secondaryMap = new TrackingMap<>(new HashMap<>());
        // Populate both maps with distinct keys/values
        primaryMap.put("A", "alpha");
        primaryMap.put("B", "bravo");
        secondaryMap.put("X", "xray");
        secondaryMap.put("Y", "yankee");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references – not strictly required but keeps the pattern consistent
        primaryMap = null;
        secondaryMap = null;
    }

    private static Stream<Arguments> emptyOrFilledProvider() {
        return Stream.of(// empty additional map
        Arguments.of(new TrackingMap<String, String>(new HashMap<>()), Collections.emptySet()), // additional map with read keys ["x","y"]
        Arguments.of(mapWithReadKeys("x", "y"), Set.of("x", "y")), // additional map with read keys ["a","b"] where "a" already present in primary
        Arguments.of(mapWithReadKeys("a", "b"), Set.of("a", "b")));
    }

    private static TrackingMap<String, String> mapWithReadKeys(String... keys) {
        TrackingMap<String, String> tm = new TrackingMap<>(new HashMap<>());
        for (String k : keys) {
            // triggers a read (value will be null, but key is recorded)
            tm.get(k);
        }
        return tm;
    }

    @Test
    void testInformAdditionalUsageMergesReadKeys() {
        // Access keys in the secondary map – these become its "read keys"
        secondaryMap.get("X");
        secondaryMap.containsKey("Y");
        // Verify secondary map has recorded the accesses before merging
        Set<String> secondaryRead = secondaryMap.keysUsed();
        assertEquals(2, secondaryRead.size());
        assertTrue(secondaryRead.containsAll(Set.of("X", "Y")));
        // Merge the secondary map's read keys into the primary map
        primaryMap.informAdditionalUsage(secondaryMap);
        // Primary map should now report the merged keys as its own read keys
        Set<String> primaryRead = primaryMap.keysUsed();
        assertEquals(2, primaryRead.size());
        assertTrue(primaryRead.containsAll(Set.of("X", "Y")));
        // Ensure that primary map's own entries are untouched
        assertEquals("alpha", primaryMap.get("A"));
        assertEquals("bravo", primaryMap.get("B"));
    }

    @Test
    void testInformAdditionalUsageDoesNotAffectPrimaryEntries() {
        // No accesses on secondary map – its readKeys set is empty
        // no‑op, just to be explicit
        secondaryMap.informAdditionalUsage(secondaryMap);
        // Merge (should be a no‑op)
        primaryMap.informAdditionalUsage(secondaryMap);
        // Primary map's read keys should still be empty
        assertTrue(primaryMap.keysUsed().isEmpty());
        // The underlying entries must remain intact
        assertEquals(2, primaryMap.size());
        assertEquals("alpha", primaryMap.get("A"));
        assertEquals("bravo", primaryMap.get("B"));
    }

    @Test
    void testInformAdditionalUsageWithEmptyAdditionalMap() {
        // Clear any reads on secondary (none were performed)
        assertTrue(secondaryMap.keysUsed().isEmpty());
        // Merge empty read set
        primaryMap.informAdditionalUsage(secondaryMap);
        // Primary map should still have no read keys
        assertTrue(primaryMap.keysUsed().isEmpty());
    }

    @Test
    void testInformAdditionalUsageNullArgumentThrows() {
        // The contract of informAdditionalUsage does not accept null; we expect NPE
        assertThrows(NullPointerException.class, () -> primaryMap.informAdditionalUsage(null));
    }

    @Test
    void testInformAdditionalUsageAddsOnlyNewReadKeys() {
        // Primary already has read key "A"
        primaryMap.get("A");
        // Secondary reads "A" and "B"
        // different key, just to have a read
        secondaryMap.get("X");
        secondaryMap.get("Y");
        // duplicate read, should not matter
        secondaryMap.get("X");
        // Simulate that secondary's readKeys are ["X","Y"]
        secondaryMap.get("X");
        secondaryMap.containsKey("Y");
        // Merge
        primaryMap.informAdditionalUsage(secondaryMap);
        // Expected read keys: original "A" plus "X" and "Y"
        Set<String> expected = Set.of("A", "X", "Y");
        assertEquals(expected, primaryMap.keysUsed());
    }

    @Test
    void testInformAdditionalUsageDoesNotAddWhenAdditionalHasNoReadsButHasEntries() {
        // Secondary has entries but we do NOT read any of them
        secondaryMap.put("Z", "zulu");
        // Verify secondary's read set is still empty
        assertTrue(secondaryMap.keysUsed().isEmpty());
        // Merge
        primaryMap.informAdditionalUsage(secondaryMap);
        // Primary's read keys should remain unchanged (empty)
        assertTrue(primaryMap.keysUsed().isEmpty());
    }

    @Test
    void testInformAdditionalUsageCopiesSnapshotOfReadKeys() {
        // Secondary reads "X"
        secondaryMap.get("X");
        // Merge while only "X" is read
        primaryMap.informAdditionalUsage(secondaryMap);
        // After merge, read an additional key in secondary
        secondaryMap.get("Y");
        // Primary's read keys must still be only the snapshot taken at merge time
        Set<String> primaryRead = primaryMap.keysUsed();
        assertEquals(Set.of("X"), primaryRead);
        // Ensure secondary now has both "X" and "Y"
        assertEquals(Set.of("X", "Y"), secondaryMap.keysUsed());
    }

    @Test
    void testInformAdditionalUsageThrowsClassCastExceptionOnIncompatibleKeyType() throws Exception {
        // Create an additional map whose internal readKeys set contains an Integer
        TrackingMap<String, String> additional = new TrackingMap<>(new HashMap<>());
        // Use reflection to inject an Integer into its private readKeys set
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Set<Object> rawSet = (Set<Object>) readKeysField.get(additional);
        // incompatible type
        rawSet.add(123);
        // Primary is a normal TrackingMap<String, String>
        assertThrows(ClassCastException.class, () -> primaryMap.informAdditionalUsage(additional));
    }

    @Test
    void testInformAdditionalUsageThrowsUnsupportedOperationExceptionWhenReadKeysUnmodifiable() throws Exception {
        // Replace primary's readKeys with an unmodifiable set via reflection
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        Set<String> unmodifiable = Collections.unmodifiableSet(new HashSet<>());
        readKeysField.set(primaryMap, unmodifiable);
        // Prepare a secondary map with a read key
        secondaryMap.get("X");
        // The call should attempt to add to the unmodifiable set and thus throw
        assertThrows(UnsupportedOperationException.class, () -> primaryMap.informAdditionalUsage(secondaryMap));
    }

    @ParameterizedTest(name = "informAdditionalUsage with additional map having read keys {1}")
    @MethodSource("emptyOrFilledProvider")
    void testInformAdditionalUsageParameterized(TrackingMap<String, String> additional, Set<String> expectedReadKeys) {
        // For the third case we need to pre‑populate primary with "a"
        if (expectedReadKeys.contains("a")) {
            // mark "a" as already read
            primaryMap.get("a");
        }
        primaryMap.informAdditionalUsage(additional);
        assertEquals(expectedReadKeys, primaryMap.keysUsed());
    }
}
