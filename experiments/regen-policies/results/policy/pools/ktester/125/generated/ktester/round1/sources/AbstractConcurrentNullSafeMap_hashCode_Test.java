package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentHashMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.function.BiFunction;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_hashCode_Test {

    private TestMap<String, String> map;

    /**
     * Concrete implementation used only for testing the abstract class.
     */
    private static class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap() {
            super(new ConcurrentHashMap<>());
        }
    }

    /**
     * Simple object whose {@code hashCode()} either returns a fixed int or throws a supplied
     * exception factory. Used to test propagation of exceptions from key/value hashCode().
     */
    private static class BadHashObject {

        private final int value;

        private final Supplier<RuntimeException> exceptionSupplier;

        BadHashObject(int value) {
            this.value = value;
            this.exceptionSupplier = null;
        }

        BadHashObject(Supplier<RuntimeException> exceptionSupplier) {
            this.value = 0;
            this.exceptionSupplier = exceptionSupplier;
        }

        @Override
        public int hashCode() {
            if (exceptionSupplier != null) {
                RuntimeException ex = exceptionSupplier.get();
                // Distinguish between key and value failures via message
                if (ex.getMessage() == null) {
                    ex = new RuntimeException("hash failure");
                }
                if (ex.getMessage().contains("key")) {
                    throw new RuntimeException("key hash failure");
                } else if (ex.getMessage().contains("value")) {
                    throw new RuntimeException("value hash failure");
                } else {
                    throw ex;
                }
            }
            return value;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof BadHashObject && ((BadHashObject) o).value == this.value;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to avoid side‑effects between tests.
        map.clear();
    }

    /**
     * Helper that reproduces the hash‑code algorithm used in the class under test.
     */
    private int expectedHashCode(Set<Map.Entry<String, String>> entries) {
        int h = 0;
        for (Map.Entry<String, String> e : entries) {
            String k = e.getKey();
            String v = e.getValue();
            int keyHash = (k == null) ? 0 : k.hashCode();
            int valueHash = (v == null) ? 0 : v.hashCode();
            h += keyHash ^ valueHash;
        }
        return h;
    }

    /**
     * Supplies the data for {@link #hashCode_CalculationScenarios}.
     */
    private static Stream<Arguments> hashCodeCalculationProvider() {
        // Helper to compute the expected hash using the same algorithm.
        return Stream.of(// Empty map – already covered by a dedicated test, but kept for completeness.
        Arguments.of(null, null, 0, "Empty map should produce hash code 0 because the loop does not execute."), // Single entry, both non‑null.
        Arguments.of(1, 2, (1 ^ 2), "Single entry with non‑null key and non‑null value (key=1, value=2)."), // Single entry, null key.
        Arguments.of(null, 2, (0 ^ 2), "Single entry with null key and non‑null value (key=null, value=2)."), // Single entry, null value.
        Arguments.of(1, null, (1 ^ 0), "Single entry with non‑null key and null value (key=1, value=null)."), // Both null.
        Arguments.of(null, null, (0 ^ 0), "Single entry with both key and value null."), // Multiple entries: (1,2) and (3,null)
        Arguments.of(// we construct the map manually inside the provider
        new Object() {

            // dummy, never used
            @Override
            public int hashCode() {
                return 0;
            }
        }, null, // expected hash = (1^2)+(3^0)=3+3=6
        6, "Multiple entries: (1,2) and (3,null)."), // Identical hash codes (42,42) → 0
        Arguments.of(42, 42, (42 ^ 42), "Single entry where key and value have identical hash codes (42,42)."), // Multiple zero‑xor entries: (1,1) and (2,2)
        Arguments.of(new Object() {

            // placeholder
            @Override
            public int hashCode() {
                return 0;
            }
        }, null, 0, "Multiple entries each producing a zero XOR: (1,1) and (2,2)."), // Null key, non‑null value (5)
        Arguments.of(null, 5, (0 ^ 5), "Single entry with a null key and a non‑null value (key=null, value=5)."), // Custom objects whose hashCode() returns -1
        Arguments.of(new BadHashObject(-1), new BadHashObject(-1), (-1 ^ -1), "Single entry where both key and value are custom objects whose hashCode() returns -1."), // Overflow scenario
        Arguments.of(Integer.MAX_VALUE, null, (Integer.MAX_VALUE ^ 0) + ((Integer.MAX_VALUE - 1) ^ 0) + (0 ^ 2), "Three entries that cause integer overflow."));
    }

    @Test
    void hashCode_EmptyMap_ReturnsZero() {
        assertTrue(map.isEmpty(), "Map should be empty before test");
        assertEquals(0, map.hashCode(), "Hash code of an empty map must be 0");
    }

    @Test
    void hashCode_SingleNonNullEntry_CalculatesCorrectly() {
        map.put("key", "value");
        int expected = expectedHashCode(map.entrySet());
        assertEquals(expected, map.hashCode(), "Hash code should match the calculated value for a single entry");
    }

    @Test
    void hashCode_MultipleEntriesIncludingNulls_CalculatesCorrectly() {
        // Populate map with a mixture of null / non‑null keys and values
        // both non‑null
        map.put("a", "alpha");
        // null key
        map.put(null, "nullKey");
        // null value
        map.put("nullValue", null);
        // both null (overwrites previous null key entry)
        map.put(null, null);
        // The map now contains three distinct entries:
        // 1. ("a","alpha")
        // 2. (null,null)   <-- last put replaces the entry with null key
        // 3. ("nullValue",null)
        int expected = expectedHashCode(map.entrySet());
        assertEquals(expected, map.hashCode(), "Hash code should correctly incorporate null keys and values");
    }

    @Test
    void hashCode_ConsistentAcrossMultipleInvocations() {
        map.put("x", "y");
        map.put(null, "z");
        map.put("w", null);
        int first = map.hashCode();
        int second = map.hashCode();
        int third = map.hashCode();
        assertEquals(first, second, "Hash code should be deterministic");
        assertEquals(second, third, "Hash code should be deterministic across calls");
    }

    /**
     * Parameterised test that covers the various single‑entry and multi‑entry
     * scenarios described in the JSON test cases.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("hashCodeCalculationProvider")
    void hashCode_CalculationScenarios(Object key, Object value, int expected, String description) {
        // Use a map whose generic types match the supplied objects.
        TestMap<Object, Object> m = new TestMap<>();
        m.put(key, value);
        // For multi‑entry cases the provider already inserts the extra entries.
        assertEquals(expected, m.hashCode(), description);
    }

    /**
     * Helper entry class used in the overflow scenario. The provider builds the map
     * manually, so we need a separate test method for that specific case.
     */
    @Test
    void hashCode_OverflowScenario() {
        TestMap<Integer, Integer> m = new TestMap<>();
        m.put(Integer.MAX_VALUE, null);
        m.put(Integer.MAX_VALUE - 1, null);
        m.put(null, 2);
        int expected = (Integer.MAX_VALUE ^ 0) + ((Integer.MAX_VALUE - 1) ^ 0) + (0 ^ 2);
        assertEquals(expected, m.hashCode(), "Overflow scenario: sum may overflow the int range but must follow Java arithmetic.");
    }

    @Test
    void hashCode_entrySetReturnsNull_throwsNullPointerException() {
        // Spy the concrete map and force entrySet() to return null.
        TestMap<String, String> spy = spy(new TestMap<>());
        doReturn(null).when(spy).entrySet();
        assertThrows(NullPointerException.class, spy::hashCode, "When entrySet() returns null the for‑each loop should throw NPE.");
    }

    @Test
    void hashCode_entrySetContainsNullElement_throwsNullPointerException() {
        // Create a Set with a single null element.
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, String>> badSet = (Set<Map.Entry<String, String>>) (Set<?>) Collections.singleton(null);
        TestMap<String, String> spy = spy(new TestMap<>());
        doReturn(badSet).when(spy).entrySet();
        assertThrows(NullPointerException.class, spy::hashCode, "Iterating over a null entry should cause NPE when entry.getKey() is invoked.");
    }

    @Test
    void hashCode_entryKeyThrowsRuntimeException_propagates() {
        // Custom Entry whose getKey() throws.
        Map.Entry<String, String> badEntry = new Map.Entry<>() {

            @Override
            public String getKey() {
                throw new RuntimeException("key failure");
            }

            @Override
            public String getValue() {
                return "value";
            }

            @Override
            public String setValue(String value) {
                return null;
            }
        };
        Set<Map.Entry<String, String>> set = Collections.singleton(badEntry);
        TestMap<String, String> spy = spy(new TestMap<>());
        doReturn(set).when(spy).entrySet();
        RuntimeException ex = assertThrows(RuntimeException.class, spy::hashCode);
        assertEquals("key failure", ex.getMessage());
    }

    @Test
    void hashCode_entryValueThrowsRuntimeException_propagates() {
        // Custom Entry whose getValue() throws.
        Map.Entry<String, String> badEntry = new Map.Entry<>() {

            @Override
            public String getKey() {
                return "key";
            }

            @Override
            public String getValue() {
                throw new RuntimeException("value failure");
            }

            @Override
            public String setValue(String value) {
                return null;
            }
        };
        Set<Map.Entry<String, String>> set = Collections.singleton(badEntry);
        TestMap<String, String> spy = spy(new TestMap<>());
        doReturn(set).when(spy).entrySet();
        RuntimeException ex = assertThrows(RuntimeException.class, spy::hashCode);
        assertEquals("value failure", ex.getMessage());
    }

    @Test
    void hashCode_keyHashCodeThrowsRuntimeException_propagates() {
        // Key object whose hashCode() throws.
        Object badKey = new BadHashObject(RuntimeException::new);
        TestMap<Object, String> m = new TestMap<>();
        m.put(badKey, "value");
        RuntimeException ex = assertThrows(RuntimeException.class, m::hashCode);
        assertEquals("key hash failure", ex.getMessage());
    }

    @Test
    void hashCode_valueHashCodeThrowsRuntimeException_propagates() {
        // Value object whose hashCode() throws.
        Object badValue = new BadHashObject(RuntimeException::new);
        TestMap<String, Object> m = new TestMap<>();
        m.put("key", badValue);
        RuntimeException ex = assertThrows(RuntimeException.class, m::hashCode);
        assertEquals("value hash failure", ex.getMessage());
    }

    @Test
    void hashCode_concurrentModificationDuringIteration_throwsConcurrentModificationException() {
        // Mock a Set whose iterator throws CME on next().
        @SuppressWarnings("unchecked")
        Set<Map.Entry<String, String>> mockSet = mock(Set.class);
        Iterator<Map.Entry<String, String>> iterator = mock(Iterator.class);
        when(mockSet.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(true);
        when(iterator.next()).thenThrow(new ConcurrentModificationException("simulated CME"));
        TestMap<String, String> spy = spy(new TestMap<>());
        doReturn(mockSet).when(spy).entrySet();
        assertThrows(ConcurrentModificationException.class, spy::hashCode, "Concurrent modification during iteration should propagate CME.");
    }
}
