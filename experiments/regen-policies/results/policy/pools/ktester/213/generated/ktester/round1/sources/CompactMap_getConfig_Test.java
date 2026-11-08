package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static com.cedarsoftware.util.CompactMap.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_getConfig_Test {

    private CompactMap<String, Integer> map;

    /**
     * Simple wrapper for a map that mimics the library's CaseInsensitiveMap.
     */
    private static class SimpleCaseInsensitiveMap<K, V> extends AbstractMap<K, V> {

        private final Map<K, V> wrapped;

        SimpleCaseInsensitiveMap(Map<K, V> wrapped) {
            this.wrapped = Objects.requireNonNull(wrapped);
        }

        Map<K, V> getWrappedMap() {
            return wrapped;
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return wrapped.entrySet();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        /*
         * Create a concrete CompactMap instance with deterministic configuration.
         * We override the protected methods that influence the configuration so that
         * the test can assert exact values without depending on the default implementation.
         */
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // custom compact size
                return 7;
            }

            @Override
            protected String getOrdering() {
                // insertion ordering
                return INSERTION;
            }

            @Override
            protected String getSingleValueKey() {
                // custom single‑value key
                return "myId";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // Use a LinkedHashMap to verify that MAP_TYPE reports this class.
                return new LinkedHashMap<>();
            }

            @Override
            protected boolean isCaseInsensitive() {
                // case‑sensitive (default)
                return false;
            }
        };
    }

    /**
     * Creates a CompactMap subclass with the supplied overrides.
     */
    private static CompactMap<String, Integer> createMap(Integer compactSize, Boolean caseInsensitive, String ordering, String singleKey, Map<String, Integer> newMap) {
        return new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return compactSize != null ? compactSize : super.compactSize();
            }

            @Override
            protected boolean isCaseInsensitive() {
                return caseInsensitive != null ? caseInsensitive : super.isCaseInsensitive();
            }

            @Override
            protected String getOrdering() {
                return ordering != null ? ordering : super.getOrdering();
            }

            @Override
            protected K getSingleValueKey() {
                //noinspection unchecked
                return (K) (singleKey != null ? singleKey : super.getSingleValueKey());
            }

            @Override
            protected Map<K, V> getNewMap() {
                //noinspection unchecked
                return (Map<K, V>) newMap;
            }
        };
    }

    @Test
    void getConfig_returnsCorrectValues_andIsUnmodifiable() {
        // Act
        Map<String, Object> config = map.getConfig();
        // Assert – map contains all expected keys
        assertEquals(5, config.size(), "Config map should contain exactly five entries");
        assertTrue(config.containsKey(COMPACT_SIZE), "Config must contain COMPACT_SIZE");
        assertTrue(config.containsKey(CASE_SENSITIVE), "Config must contain CASE_SENSITIVE");
        assertTrue(config.containsKey(ORDERING), "Config must contain ORDERING");
        assertTrue(config.containsKey(SINGLE_KEY), "Config must contain SINGLE_KEY");
        assertTrue(config.containsKey(MAP_TYPE), "Config must contain MAP_TYPE");
        // Assert – values match the overrides defined in setupBeforeEach()
        assertEquals(7, config.get(COMPACT_SIZE));
        // !isCaseInsensitive()
        assertEquals(Boolean.TRUE, config.get(CASE_SENSITIVE));
        assertEquals(INSERTION, config.get(ORDERING));
        assertEquals("myId", config.get(SINGLE_KEY));
        assertEquals(LinkedHashMap.class, config.get(MAP_TYPE));
        // Assert – the returned map is unmodifiable
        assertThrows(UnsupportedOperationException.class, () -> config.put("newKey", "newValue"), "Config map should be immutable");
    }

    @Test
    void getConfig_reflectsCaseInsensitiveSetting() {
        // Create a map where case‑insensitivity is enabled
        CompactMap<String, Integer> ciMap = new CompactMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        Map<String, Object> config = ciMap.getConfig();
        // CASE_SENSITIVE should be false when the map is case‑insensitive
        assertEquals(Boolean.FALSE, config.get(CASE_SENSITIVE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_underlyingMapIsCaseInsensitiveMap_wrappedHashMap() {
        // Build a CaseInsensitiveMap that wraps a HashMap
        SimpleCaseInsensitiveMap<String, Integer> ciMap = new SimpleCaseInsensitiveMap<>(new HashMap<>());
        CompactMap<String, Integer> map = createMap(// compactSize
        123, // isCaseInsensitive = false
        false, // ordering
        SORTED, // singleKey
        "myId", (Map<String, Integer>) ciMap);
        Map<String, Object> config = map.getConfig();
        assertEquals(123, config.get(COMPACT_SIZE));
        // !isCaseInsensitive()
        assertEquals(Boolean.TRUE, config.get(CASE_SENSITIVE));
        assertEquals(SORTED, config.get(ORDERING));
        assertEquals("myId", config.get(SINGLE_KEY));
        // The map instance is a CaseInsensitiveMap, but getConfig() should store the
        // class of the wrapped map (HashMap).
        assertEquals(HashMap.class, config.get(MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_defaultConfiguration() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> config = map.getConfig();
        assertEquals(DEFAULT_COMPACT_SIZE, config.get(COMPACT_SIZE));
        assertEquals(Boolean.valueOf(DEFAULT_CASE_SENSITIVE), config.get(CASE_SENSITIVE));
        assertEquals(UNORDERED, config.get(ORDERING));
        assertEquals(DEFAULT_SINGLE_KEY, config.get(SINGLE_KEY));
        // Default backing map is a HashMap
        assertEquals(HashMap.class, config.get(MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_caseInsensitiveMode() {
        CompactMap<String, Integer> map = createMap(// compactSize
        75, // isCaseInsensitive = true
        true, // default ordering
        null, // default singleKey
        null, // default map
        new HashMap<>());
        Map<String, Object> config = map.getConfig();
        assertEquals(75, config.get(COMPACT_SIZE));
        // !true = false
        assertEquals(Boolean.FALSE, config.get(CASE_SENSITIVE));
        assertEquals(UNORDERED, config.get(ORDERING));
        assertEquals(DEFAULT_SINGLE_KEY, config.get(SINGLE_KEY));
        assertEquals(HashMap.class, config.get(MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_customOrderingInsertion() {
        CompactMap<String, Integer> map = createMap(null, null, INSERTION, null, new HashMap<>());
        Map<String, Object> config = map.getConfig();
        assertEquals(DEFAULT_COMPACT_SIZE, config.get(COMPACT_SIZE));
        assertEquals(Boolean.TRUE, config.get(CASE_SENSITIVE));
        assertEquals(INSERTION, config.get(ORDERING));
        assertEquals(DEFAULT_SINGLE_KEY, config.get(SINGLE_KEY));
        assertEquals(HashMap.class, config.get(MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_mapTypeTreeMap() {
        CompactMap<String, Integer> map = createMap(null, null, SORTED, null, new TreeMap<>());
        Map<String, Object> config = map.getConfig();
        assertEquals(DEFAULT_COMPACT_SIZE, config.get(COMPACT_SIZE));
        assertEquals(Boolean.TRUE, config.get(CASE_SENSITIVE));
        assertEquals(SORTED, config.get(ORDERING));
        assertEquals(DEFAULT_SINGLE_KEY, config.get(SINGLE_KEY));
        assertEquals(TreeMap.class, config.get(MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_caseInsensitiveMapWrappingTreeMap() {
        SimpleCaseInsensitiveMap<String, Integer> ciMap = new SimpleCaseInsensitiveMap<>(new TreeMap<>());
        CompactMap<String, Integer> map = createMap(60, false, SORTED, "myId", (Map<String, Integer>) ciMap);
        Map<String, Object> config = map.getConfig();
        assertEquals(60, config.get(COMPACT_SIZE));
        assertEquals(Boolean.TRUE, config.get(CASE_SENSITIVE));
        assertEquals(SORTED, config.get(ORDERING));
        assertEquals("myId", config.get(SINGLE_KEY));
        // Unwrap to TreeMap class
        assertEquals(TreeMap.class, config.get(MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_isUnmodifiable() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> config = map.getConfig();
        assertThrows(UnsupportedOperationException.class, () -> config.put("newKey", "newValue"), "Config map must be immutable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_newMapReturnsNull_throwsNPE() {
        CompactMap<String, Integer> map = new CompactMap<String, Integer>() {

            @Override
            protected Map<K, V> getNewMap() {
                // deliberately break the contract
                return null;
            }
        };
        assertThrows(NullPointerException.class, map::getConfig, "Calling getConfig() when getNewMap() returns null should throw NPE");
    }
}
