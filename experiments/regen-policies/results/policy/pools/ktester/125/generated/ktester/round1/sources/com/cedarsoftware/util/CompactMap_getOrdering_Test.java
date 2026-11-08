package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.*;
// import org.junit.Test;
// import org.junit.Before;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CompactMap_getOrdering_Test {

    /**
     * Cached reflective access to the protected {@code getOrdering()} method.
     */
    private Method getOrderingMethod;

//     @BeforeEach
    void setupBeforeEach() throws Exception {
        // The method is protected – make it accessible once for every test case.
        getOrderingMethod = CompactMap.class.getDeclaredMethod("getOrdering");
        getOrderingMethod.setAccessible(true);
    }

    /**
     * Helper that invokes the protected {@code getOrdering()} via reflection.
     */
    private String orderingOf(Map<?, ?> map) throws Exception {
        return (String) getOrderingMethod.invoke(map);
    }

//     @Test
//     void defaultCompactMapReturnsUnordered() throws Exception {
//         // The no‑arg constructor creates a map with the default configuration.
//         CompactMap<String, Object> map = new CompactMap<>();
//         assertEquals(CompactMap.UNORDERED, orderingOf(map), "Default CompactMap should report UNORDERED ordering");
//     }

//     @Test
//     void builderInsertionOrderReturnsInsertion() throws Exception {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().insertionOrder().build();
//         assertEquals(CompactMap.INSERTION, orderingOf(map), "Builder with insertionOrder() should report INSERTION ordering");
//     }

//     @Test
//     void builderSortedOrderReturnsSorted() throws Exception {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().build();
//         assertEquals(CompactMap.SORTED, orderingOf(map), "Builder with sortedOrder() should report SORTED ordering");
//     }

//     @Test
//     void builderReverseOrderReturnsReverse() throws Exception {
//         CompactMap<String, Object> map = CompactMap.<String, Object>builder().reverseOrder().build();
//         assertEquals(CompactMap.REVERSE, orderingOf(map), "Builder with reverseOrder() should report REVERSE ordering");
//     }

//     @Test
//     void customSubclassCanOverrideOrdering() throws Exception {
//         // Anonymous subclass that overrides getOrdering() to expose a custom value.
//         CompactMap<String, Object> map = new CompactMap<String, Object>() {
// 
//             @Override
//             protected String getOrdering() {
//                 return "custom";
//             }
// 
//             // Minimal implementations required by the abstract members.
//             @Override
//             protected int compactSize() {
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new HashMap<>();
//             }
//         };
//         assertEquals("custom", orderingOf(map), "Subclass overriding getOrdering() should return the overridden value");
//     }

//     @Test
//     void defaultCompactMapAlwaysReturnsUnordered() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<>();
//         assertEquals(CompactMap.UNORDERED, orderingOf(map), "CompactMap created with default constructor should report UNORDERED ordering");
//     }

//     @Test
//     void repeatedCallsReturnSameValue() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<>();
//         String first = orderingOf(map);
//         String second = orderingOf(map);
//         assertEquals(first, second, "Two successive calls to getOrdering() must return equal values");
//         // Also ensure they are the same object reference (strings are interned in this case).
//         assertSame(first, second, "Two successive calls to getOrdering() should return the same object reference");
//     }

//     @Test
//     void orderingReferenceEqualityWithConstant() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<>();
//         String ordering = orderingOf(map);
//         // The contract expects the exact same object as the public constant.
//         assertSame(CompactMap.UNORDERED, ordering, "Returned ordering should be the same object reference as CompactMap.UNORDERED");
//     }

//     @Test
//     void orderingIsNotEqualToOtherOrderingIdentifiers() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<>();
//         String ordering = orderingOf(map);
//         // The ordering must not match any of the other defined ordering constants.
//         assertNotEquals(CompactMap.INSERTION, ordering, "UNORDERED must not be equal to INSERTION");
//         assertNotEquals(CompactMap.SORTED, ordering, "UNORDERED must not be equal to SORTED");
//         assertNotEquals(CompactMap.REVERSE, ordering, "UNORDERED must not be equal to REVERSE");
//     }

//     @Test
//     void getOrderingNeverReturnsNull() throws Exception {
//         CompactMap<String, Object> map = new CompactMap<>();
//         String ordering = orderingOf(map);
//         assertNotNull(ordering, "getOrdering() must never return null");
//     }

//     @Test
//     void threadSafetyOfGetOrdering() throws Exception {
//         final CompactMap<String, Object> map = new CompactMap<>();
//         final int threadCount = 20;
//         ExecutorService executor = Executors.newFixedThreadPool(threadCount);
//         try {
//             // Submit a bunch of tasks that read the ordering concurrently.
//             List<Future<String>> futures = IntStream.range(0, threadCount).mapToObj(i -> executor.submit(() -> orderingOf(map))).collect(Collectors.toList());
//             // Verify every thread received the expected constant and no exception occurred.
//             for (Future<String> f : futures) {
//                 assertEquals(CompactMap.UNORDERED, f.get(), "All concurrent calls must return UNORDERED ordering");
//             }
//         } finally {
//             executor.shutdownNow();
//         }
//     }
}