package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_informAdditionalUsage_2_Test {

    /**
     * Instance fields are created fresh for each test.
     */
    private TrackingMap<String, Integer> primaryMap;

    private TrackingMap<String, Integer> secondaryMap;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise two independent TrackingMap instances backed by simple HashMaps.
        Map<String, Integer> primaryBacking = new HashMap<>();
        primaryBacking.put("a", 1);
        primaryBacking.put("b", 2);
        primaryMap = new TrackingMap<>(primaryBacking);
        Map<String, Integer> secondaryBacking = new HashMap<>();
        secondaryBacking.put("c", 3);
        secondaryBacking.put("d", 4);
        secondaryMap = new TrackingMap<>(secondaryBacking);
    }

    /**
     * Verify that {@code informAdditionalUsage} merges the set of used keys
     * from the additional map into the primary map.
     */
    @Test
    void shouldMergeUsedKeysFromAdditionalTrackingMap() {
        // GIVEN – mark some keys as read in each map.
        // "a" becomes a used key in primaryMap
        primaryMap.get("a");
        // "c" becomes a used key in secondaryMap
        secondaryMap.get("c");
        // "d" becomes a used key in secondaryMap
        secondaryMap.get("d");
        Set<String> primaryBefore = primaryMap.keysUsed();
        assertEquals(Set.of("a"), primaryBefore, "Primary map should initially contain only key 'a' as used");
        // WHEN – inform the primary map about the additional usage.
        primaryMap.informAdditionalUsage(secondaryMap);
        // THEN – the primary map's used‑key set should be the union of both.
        Set<String> primaryAfter = primaryMap.keysUsed();
        assertEquals(Set.of("a", "c", "d"), primaryAfter, "Primary map should contain its original used key plus all keys used in the additional map");
    }

    /**
     * Verify that invoking the method with an empty additional map does not change the primary map.
     */
    @Test
    void shouldLeavePrimaryUnchangedWhenAdditionalIsEmpty() {
        // GIVEN – only one key is used in the primary map.
        primaryMap.get("b");
        Set<String> before = primaryMap.keysUsed();
        assertEquals(Set.of("b"), before);
        // AND – the secondary map has no used keys (no get() calls).
        // WHEN
        primaryMap.informAdditionalUsage(secondaryMap);
        // THEN – the primary map's used‑key set remains unchanged.
        Set<String> after = primaryMap.keysUsed();
        assertEquals(Set.of("b"), after);
    }

    /**
     * Verify that passing {@code null} as the additional map results in a {@link NullPointerException}.
     */
    @Test
    void shouldThrowWhenAdditionalMapIsNull() {
        // GIVEN
        // ensure primary map is in a valid state
        primaryMap.get("a");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> primaryMap.informAdditionalUsage(null), "Calling informAdditionalUsage with null should throw NullPointerException");
    }

    /**
     * Basic addition of distinct keys.
     *
     * trackingMap1 is pre‑populated with keys "a" and "b" via
     * {@code informAdditionalUsage(Collection)}.
     * trackingMap2 is pre‑populated with keys "c" and "d".
     * After the call the union of both key sets must be present.
     */
    @Test
    void shouldAddDistinctKeysFromAdditionalMap() {
        // pre‑populate primaryMap with "a" and "b"
        primaryMap.informAdditionalUsage(List.of("a", "b"));
        // pre‑populate secondaryMap with "c" and "d"
        secondaryMap.informAdditionalUsage(List.of("c", "d"));
        primaryMap.informAdditionalUsage(secondaryMap);
        assertEquals(Set.of("a", "b", "c", "d"), primaryMap.keysUsed(), "Primary map should contain the union of distinct keys from both maps");
    }

    /**
     * Self usage addition – calling the method on the same instance should leave the set unchanged.
     */
    @Test
    void shouldLeaveKeysUnchangedWhenAdditionalIsSelf() {
        primaryMap.informAdditionalUsage(List.of("x", "y"));
        Set<String> before = primaryMap.keysUsed();
        // calling with itself
        primaryMap.informAdditionalUsage(primaryMap);
        assertEquals(before, primaryMap.keysUsed(), "Calling informAdditionalUsage with the same instance must not alter the keysUsed set");
    }

    /**
     * Addition with overlapping keys.
     *
     * primaryMap has "x" and "y"; secondaryMap has "y" and "z".
     * The resulting set must be {"x","y","z"} without duplicates.
     */
    @Test
    void shouldMergeOverlappingKeysWithoutDuplicates() {
        primaryMap.informAdditionalUsage(List.of("x", "y"));
        secondaryMap.informAdditionalUsage(List.of("y", "z"));
        primaryMap.informAdditionalUsage(secondaryMap);
        assertEquals(Set.of("x", "y", "z"), primaryMap.keysUsed(), "Union should contain each key only once even when overlapping");
    }

    /**
     * Verify that changes to the additional map after the call do not affect the primary map.
     */
    @Test
    void shouldNotReflectLaterChangesInAdditionalMap() {
        // primaryMap has "p"
        primaryMap.informAdditionalUsage(List.of("p"));
        // secondaryMap initially has "q"
        secondaryMap.informAdditionalUsage(List.of("q"));
        primaryMap.informAdditionalUsage(secondaryMap);
        // mutate secondaryMap after the merge
        secondaryMap.informAdditionalUsage(List.of("r"));
        // primaryMap must still only contain "p" and "q"
        assertEquals(Set.of("p", "q"), primaryMap.keysUsed(), "Primary map must retain a snapshot of the additional map's keys at call time");
    }

    /**
     * Addition with a {@code null} key in the additional map.
     *
     * The additional map is pre‑populated with {@code null} and "alpha".
     * The resulting set must contain both.
     */
    @Test
    void shouldAcceptNullKeyFromAdditionalMap() {
        // primary map starts empty
        secondaryMap.informAdditionalUsage(Arrays.asList(null, "alpha"));
        primaryMap.informAdditionalUsage(secondaryMap);
        assertEquals(Set.of(null, "alpha"), primaryMap.keysUsed(), "keysUsed must contain null and \"alpha\" when they are present in the additional map");
    }

    /**
     * Additional map whose internal {@code readKeys} field has been set to {@code null}
     * should cause a {@link NullPointerException} when used.
     */
    @Test
    void shouldThrowWhenAdditionalMapHasNullReadKeys() throws Exception {
        // Ensure secondaryMap has a normal readKeys set first
        secondaryMap.informAdditionalUsage(List.of("z"));
        // Use reflection to corrupt the internal state
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        // simulate a broken instance
        readKeysField.set(secondaryMap, null);
        assertThrows(NullPointerException.class, () -> primaryMap.informAdditionalUsage(secondaryMap), "If the additional map's internal readKeys is null, a NullPointerException must be thrown");
    }

    /**
     * Verify that informing with an empty additional map leaves the primary map unchanged.
     * This is similar to an earlier test but uses a freshly created empty TrackingMap.
     */
    @Test
    void shouldLeavePrimaryUnchangedWhenAdditionalIsEmptyMap() {
        primaryMap.informAdditionalUsage(List.of("m", "n"));
        // no used keys
        TrackingMap<String, Integer> emptyMap = new TrackingMap<>(new HashMap<>());
        primaryMap.informAdditionalUsage(emptyMap);
        assertEquals(Set.of("m", "n"), primaryMap.keysUsed(), "Primary map's keysUsed must remain unchanged when additional map has no used keys");
    }
}
