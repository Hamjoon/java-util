package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_containsValue_Test {

    private TestMap<String, String> map;

    /**
     * Simple concrete implementation used only for testing.
     */
    private static final class TestMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    /**
     * Subclass that deliberately passes a {@code null} internal map to the super‑class.
     */
    private static final class NullInternalMap<K, V> extends AbstractConcurrentNullSafeMap<K, V> {

        NullInternalMap() {
            super(null);
        }
    }

    class Custom {

        final String id;

        Custom(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Custom) && Objects.equals(id, ((Custom) o).id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    class Custom {

        final String id;

        Custom(String id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Custom) && Objects.equals(id, ((Custom) o).id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh ConcurrentHashMap for each test to avoid cross‑test interference.
        ConcurrentMap<Object, Object> internal = new ConcurrentHashMap<>();
        map = new TestMap<>(internal);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to aid GC and ensure no leftover state.
        map.clear();
    }

    /**
     * Inserts a sentinel directly into the backing map (bypassing any masking logic).
     */
    private static void putSentinel(AbstractConcurrentNullSafeMap<?, ?> mapInstance, AbstractConcurrentNullSafeMap.NullSentinel sentinel) {
        getInternalMap(mapInstance).put(sentinel, sentinel);
    }

    private static Stream<Arguments> provideStandardCases() {
        return Stream.of(// value = null, sentinel present -> true
        Arguments.of(null, true, "null value, sentinel present"), // value = null, sentinel absent -> false
        Arguments.of(null, false, "null value, sentinel absent"), // non‑null present
        Arguments.of("apple", true, "non‑null present"), // non‑null absent
        Arguments.of("banana", false, "non‑null absent"), // sentinel passed as non‑null, present
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, true, "sentinel as argument, present"), // sentinel passed as non‑null, absent
        Arguments.of(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE, false, "sentinel as argument, absent"));
    }

    @Test
    void containsValue_returnsTrueWhenValuePresent() {
        // given
        map.put("key1", "value1");
        map.put("key2", "value2");
        // when
        boolean result = map.containsValue("value1");
        // then
        assertTrue(result, "containsValue should return true for an existing value");
    }

    @Test
    void containsValue_returnsFalseWhenValueAbsent() {
        // given
        map.put("key1", "value1");
        // when
        boolean result = map.containsValue("nonexistent");
        // then
        assertFalse(result, "containsValue should return false when the value is not present");
    }

    @Test
    void containsValue_handlesNullValueUsingSentinel() {
        // given
        // null value is stored as sentinel internally
        map.put("nullKey", null);
        // when
        boolean result = map.containsValue(null);
        // then
        assertTrue(result, "containsValue should correctly detect a stored null value");
    }

    @Test
    void containsValue_returnsFalseWhenMapIsEmpty() {
        // given – map is freshly created and empty
        // when
        boolean result = map.containsValue("any");
        // then
        assertFalse(result, "containsValue should return false for an empty map");
    }

    /**
     * Direct access to the protected {@code internalMap} field via reflection.
     */
    @SuppressWarnings("unchecked")
    private static ConcurrentMap<Object, Object> getInternalMap(AbstractConcurrentNullSafeMap<?, ?> mapInstance) {
        try {
            Field f = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
            f.setAccessible(true);
            return (ConcurrentMap<Object, Object>) f.get(mapInstance);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @ParameterizedTest(name = "containsValue({1}) → {2} ({0})")
    @MethodSource("provideStandardCases")
    void containsValue_standardScenarios(Object value, boolean expected, String description) {
        // Arrange ---------------------------------------------------------
        // Populate the internal map according to the scenario description.
        if (value == null) {
            // For the "sentinel present" case we pre‑populate the sentinel.
            if (description.contains("present")) {
                putSentinel(map, AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE);
            }
        } else if (value == AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE) {
            // Directly insert the sentinel when the test expects it to be present.
            if (description.contains("present")) {
                putSentinel(map, AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE);
            }
        } else {
            // Normal value insertion.
            map.put("key", value);
        }
        // Act -------------------------------------------------------------
        boolean result = map.containsValue(value);
        // Assert ----------------------------------------------------------
        assertEquals(expected, result, () -> "Unexpected result for case: " + description);
    }

    @Test
    void containsValue_equalitySemantics() {
        // given – map contains a distinct String instance with same characters
        String stored = new String("apple");
        map.put("k1", stored);
        // different object, same content
        String query = new String("apple");
        // when
        boolean result = map.containsValue(query);
        // then
        assertTrue(result, "containsValue should rely on equals(), not identity");
    }

    @Test
    void containsValue_nonStringValue() {
        map.put("intKey", 42);
        assertTrue(map.containsValue(42), "containsValue must work for non‑String values");
    }

    @Test
    void containsValue_customObjectEquality() {
        // custom object with proper equals/hashCode
        class Custom {

            final String id;

            Custom(String id) {
                this.id = id;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Custom) && Objects.equals(id, ((Custom) o).id);
            }

            @Override
            public int hashCode() {
                return Objects.hash(id);
            }
        }
        Custom stored = new Custom("id-123");
        map.put("cKey", stored);
        // equal but distinct instance
        Custom query = new Custom("id-123");
        assertTrue(map.containsValue(query), "containsValue should return true for equal custom objects");
    }

    @Test
    void containsValue_absentValueWhenOnlySentinelPresent() {
        // internal map contains only the null sentinel
        putSentinel(map, AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE);
        assertFalse(map.containsValue("apple"), "When only the sentinel is stored, a regular value must not be found");
    }

    @Test
    void containsValue_emptyMapReturnsFalse() {
        assertFalse(map.containsValue("any"), "containsValue should return false for an empty map");
    }

    @Test
    void containsValue_nullInternalMap_throwsNPE_forNullArgument() {
        NullInternalMap<String, String> nullMap = new NullInternalMap<>();
        assertThrows(NullPointerException.class, () -> nullMap.containsValue(null), "Calling containsValue when internalMap is null must throw NPE");
    }

    @Test
    void containsValue_nullInternalMap_throwsNPE_forNonNullArgument() {
        NullInternalMap<String, String> nullMap = new NullInternalMap<>();
        assertThrows(NullPointerException.class, () -> nullMap.containsValue("any"), "Calling containsValue with non‑null argument when internalMap is null must throw NPE");
    }

    @Test
    void containsValue_propagatesUnsupportedOperationException_nonNull() {
        // Mock a ConcurrentMap that throws UOE on containsValue
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockMap = mock(ConcurrentMap.class);
        when(mockMap.containsValue("any")).thenThrow(new UnsupportedOperationException("unsupported"));
        TestMap<String, String> customMap = new TestMap<>(mockMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> customMap.containsValue("any"));
        assertEquals("unsupported", ex.getMessage());
    }

    @Test
    void containsValue_propagatesUnsupportedOperationException_nullArgument() {
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> mockMap = mock(ConcurrentMap.class);
        when(mockMap.containsValue(AbstractConcurrentNullSafeMap.NullSentinel.NULL_VALUE)).thenThrow(new UnsupportedOperationException("unsupported"));
        TestMap<String, String> customMap = new TestMap<>(mockMap);
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> customMap.containsValue(null));
        assertEquals("unsupported", ex.getMessage());
    }

    @Test
    void containsValue_propagatesClassCastException_forInvalidType() {
        // Map that only accepts String values; any other type causes CCE
        ConcurrentMap<Object, Object> restrictiveMap = new ConcurrentHashMap<>() {

            @Override
            public boolean containsValue(Object value) {
                if (!(value instanceof String)) {
                    throw new ClassCastException("Only String values allowed");
                }
                return super.containsValue(value);
            }

            @Override
            public Object put(Object key, Object value) {
                if (!(value instanceof String)) {
                    throw new ClassCastException("Only String values allowed");
                }
                return super.put(key, value);
            }
        };
        // Insert a legal entry so the map is not empty
        restrictiveMap.put("k", "legal");
        TestMap<String, Object> customMap = new TestMap<>(restrictiveMap);
        assertThrows(ClassCastException.class, () -> customMap.containsValue(123), "containsValue should propagate ClassCastException for disallowed value types");
    }

    @Test
    void containsValue_propagatesClassCastException_whenNullArgumentAndSentinelRejected() {
        // Same restrictive map, but it treats the sentinel as invalid
        ConcurrentMap<Object, Object> restrictiveMap = new ConcurrentHashMap<>() {

            @Override
            public boolean containsValue(Object value) {
                if (!(value instanceof String)) {
                    throw new ClassCastException("Only String values allowed");
                }
                return super.containsValue(value);
            }
        };
        TestMap<String, Object> customMap = new TestMap<>(restrictiveMap);
        assertThrows(ClassCastException.class, () -> customMap.containsValue(null), "containsValue(null) should propagate ClassCastException when the sentinel is rejected");
    }
}
