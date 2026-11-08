package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static com.cedarsoftware.util.CompactMap.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.HashMap;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_getOrdering_Test {

    /**
     * Provides configuration maps together with the expected ordering constant.
     *
     * <p>The data mirrors the JSON test cases supplied in the task description.
     */
    private static Stream<Arguments> orderingConfigProvider() {
        return Stream.of(// {"ordering":"sorted"} -> SORTED
        Arguments.of(Map.of(ORDERING, SORTED), SORTED), // {"ordering":"insertion"} -> INSERTION
        Arguments.of(Map.of(ORDERING, INSERTION), INSERTION), // {"ordering":"reverse"} -> REVERSE
        Arguments.of(Map.of(ORDERING, REVERSE), REVERSE), // {"ordering":"unknown"} -> fallback to UNORDERED
        Arguments.of(Map.of(ORDERING, "unknown"), UNORDERED), // {} (no ordering key) -> default UNORDERED
        Arguments.of(Map.of(), UNORDERED));
    }

    /**
     * The default CompactMap (created with the no‑arg constructor) should report
     * {@link CompactMap#UNORDERED} as its ordering.
     */
    @Test
    void defaultOrderingIsUnordered() {
        CompactMap<String, Object> map = new CompactMap<>();
        // getOrdering() is protected but we are in the same package, so it is accessible.
        String ordering = map.getOrdering();
        assertEquals(UNORDERED, ordering, "Default CompactMap should have ordering UNORDERED");
    }

    /**
     * When the builder is used with {@code sortedOrder()}, the resulting map must
     * return {@link CompactMap#SORTED} from {@code getOrdering()}.
     */
    @Test
    void sortedOrderingIsReturned() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().build();
        String ordering = map.getOrdering();
        assertEquals(SORTED, ordering, "Builder with sortedOrder() should produce ordering SORTED");
    }

    /**
     * When the builder is used with {@code insertionOrder()}, the resulting map must
     * return {@link CompactMap#INSERTION} from {@code getOrdering()}.
     */
    @Test
    void insertionOrderingIsReturned() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        String ordering = map.getOrdering();
        assertEquals(INSERTION, ordering, "Builder with insertionOrder() should produce ordering INSERTION");
    }

    /**
     * When the builder is used with {@code reverseOrder()}, the resulting map must
     * return {@link CompactMap#REVERSE} from {@code getOrdering()}.
     */
    @Test
    void reverseOrderingIsReturned() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().reverseOrder().build();
        String ordering = map.getOrdering();
        assertEquals(REVERSE, ordering, "Builder with reverseOrder() should produce ordering REVERSE");
    }

    /**
     * Verify that the ordering constant is not accidentally changed by other
     * configuration options (e.g., case‑sensitivity). The map is built with
     * case‑insensitive and sorted ordering; the ordering should still be SORTED.
     */
    @Test
    void caseInsensitiveSortedOrderingIsSorted() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder().build();
        String ordering = map.getOrdering();
        assertEquals(SORTED, ordering, "Case‑insensitive sorted map should still report ordering SORTED");
    }

    /**
     * The {@code getOrdering()} method must never return {@code null}.
     */
    @Test
    void orderingNeverNull() {
        Map<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
        assertNotNull(((CompactMap<?, ?>) map).getOrdering(), "getOrdering() should never return null");
    }

    /**
     * Validates that {@link CompactMap#newMap(java.util.Map)} respects the
     * {@code ordering} entry in the supplied configuration map.
     *
     * @param config   configuration map passed to {@code newMap}
     * @param expected expected ordering constant returned by {@code getOrdering()}
     */
    @ParameterizedTest(name = "config={0} => expected ordering={1}")
    @MethodSource("orderingConfigProvider")
    void orderingRespectsConfigurationMap(Map<String, Object> config, String expected) {
        // The static factory creates a CompactMap based on the raw options map.
        CompactMap<String, Object> map = CompactMap.newMap(config);
        String actual = map.getOrdering();
        assertEquals(expected, actual, () -> "With config " + config + " expected ordering " + expected + " but got " + actual);
    }
}
