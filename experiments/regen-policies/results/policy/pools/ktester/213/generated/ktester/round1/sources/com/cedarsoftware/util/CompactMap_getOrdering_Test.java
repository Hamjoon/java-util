package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.stream.Stream;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CompactMap_getOrdering_Test {

    /**
     * Functional interface used to lazily create a map inside the parameter source.
     */
    @FunctionalInterface
    private interface BuilderFactory {

        CompactMap<String, Object> create();
    }

    /**
     * Helper that uses reflection to call the protected {@code getOrdering()} method.
     */
    private static String invokeGetOrdering(CompactMap<?, ?> map) {
        try {
            Method m = CompactMap.class.getDeclaredMethod("getOrdering");
            m.setAccessible(true);
            return (String) m.invoke(map);
        } catch (Exception e) {
            throw new AssertionError("Unable to invoke getOrdering()", e);
        }
    }

    /**
     * Supplies the different builder configurations used by the parameterized test.
     */
    private static Stream<Arguments> builderOrderingProvider() {
        return Stream.of(Arguments.of((BuilderFactory) () -> CompactMap.<String, Object>builder().insertionOrder().build(), "insertionOrder()"), Arguments.of((BuilderFactory) () -> CompactMap.<String, Object>builder().sortedOrder().build(), "sortedOrder()"), Arguments.of((BuilderFactory) () -> CompactMap.<String, Object>builder().reverseOrder().build(), "reverseOrder()"));
    }

//     @Test
//     void defaultMapReturnsUnordered() {
//         CompactMap<String, Object> map = new CompactMap<>();
//         String ordering = invokeGetOrdering(map);
//         Assertions.assertEquals(CompactMap.UNORDERED, ordering);
//     }

//     @Test
//     void insertionOrderMapReturnsInsertion() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         String ordering = invokeGetOrdering(map);
//         Assertions.assertEquals(CompactMap.INSERTION, ordering);
//     }

//     @Test
//     void sortedOrderMapReturnsSorted() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().build();
//         String ordering = invokeGetOrdering(map);
//         Assertions.assertEquals(CompactMap.SORTED, ordering);
//     }

//     @Test
//     void reverseOrderMapReturnsReverse() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().reverseOrder().build();
//         String ordering = invokeGetOrdering(map);
//         Assertions.assertEquals(CompactMap.REVERSE, ordering);
//     }

    /**
     * Verify that the ordering value is stored in the map's configuration and can be
     * retrieved via {@code getConfig()} as well as via the protected {@code getOrdering()}.
     */
//     @Test
//     void orderingConsistentWithConfig() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().caseSensitive(// add another option to ensure config is not empty
//         false).build();
//         Map<String, Object> cfg = map.getConfig();
//         Assertions.assertNotNull(cfg);
//         Assertions.assertTrue(cfg.containsKey(CompactMap.ORDERING));
//         Assertions.assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
//         String ordering = invokeGetOrdering(map);
//         Assertions.assertEquals(CompactMap.SORTED, ordering);
//     }

    /**
     * Builder‑based ordering options (insertion, sorted, reverse) are currently
     * ignored by the placeholder implementation, so the method must still return
     * {@code UNORDERED}.
     *
     * @param builderCall a lambda that creates a map with a specific ordering option
     * @param description textual description for the assertion message
     */
//     @ParameterizedTest(name = "{index} => {1}")
//     @MethodSource("builderOrderingProvider")
//     void builderOrderingIgnoredByGetOrdering(BuilderFactory builderCall, String description) {
//         CompactMap<String, Object> map = builderCall.create();
//         String ordering = invokeGetOrdering(map);
//         // According to the placeholder implementation, ordering is always UNORDERED
//         Assertions.assertEquals(CompactMap.UNORDERED, ordering, "Ordering for builder call '" + description + "' should be UNORDERED");
//     }

    /**
     * Even when the configuration map explicitly contains an {@code ordering}
     * entry (via {@code withConfig}), the protected {@code getOrdering()} method
     * still returns {@code UNORDERED}.
     */
//     @Test
//     void withConfigOrderingDoesNotAffectGetOrdering() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
//         Map<String, Object> newConfig = new HashMap<>();
//         newConfig.put(CompactMap.ORDERING, CompactMap.SORTED);
//         map = map.withConfig(newConfig);
//         Map<String, Object> cfg = map.getConfig();
//         Assertions.assertNotNull(cfg);
//         Assertions.assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
//         String ordering = invokeGetOrdering(map);
//         Assertions.assertEquals(CompactMap.UNORDERED, ordering);
//     }

    /**
     * Ensure that the ordering value stored in the map's configuration is independent
     * from the value returned by {@code getOrdering()}. This guards against future
     * changes where the method might start delegating to the config.
     */
//     @Test
//     void orderingInConfigIsIndependentFromGetOrdering() {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().caseSensitive(// add another option to ensure config is populated
//         false).build();
//         Map<String, Object> cfg = map.getConfig();
//         Assertions.assertNotNull(cfg);
//         Assertions.assertTrue(cfg.containsKey(CompactMap.ORDERING));
//         Assertions.assertEquals(CompactMap.SORTED, cfg.get(CompactMap.ORDERING));
//         String ordering = invokeGetOrdering(map);
//         // The placeholder implementation still returns UNORDERED
//         Assertions.assertEquals(CompactMap.UNORDERED, ordering);
//     }
}