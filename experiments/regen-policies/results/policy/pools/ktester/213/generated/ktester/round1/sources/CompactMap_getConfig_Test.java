package com.cedarsoftware.util;

import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
// import org.junit.*;
import org.junit.jupiter.params.ParameterizedTest;
// import org.junit.runner.RunWith;
import org.junit.jupiter.params.provider.MethodSource;

// import org.junit.runners.Parameterized;
// import org.junit.runners.Parameterized.Parameters;
// @ExtendWith(MockitoExtension.class)
class CompactMap_getConfig_Test {

    //     @Parameterized.Parameter(0)
    public CompactMap<String, Object> paramMap;

    // Parameter provider for the parameterized test above
    private static Stream<CompactMap<String, Object>> unmodifiableConfigProvider() {
        return Stream.of(CompactMap.<String, Object>builder().build(), CompactMap.<String, Object>builder().caseSensitive(false).build(), CompactMap.<String, Object>builder().sortedOrder().build());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_DefaultBuilder_ReturnsExpectedValues() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().build();
        Map<String, Object> config = map.getConfig();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> config.put("anyKey", "anyValue"), "Config map should be unmodifiable");
        Assertions.assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.UNORDERED, config.get(CompactMap.ORDERING));
        Assertions.assertEquals(CompactMap.DEFAULT_SINGLE_KEY, config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(CompactMap.DEFAULT_MAP_TYPE, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CustomAnonymousSubclass_ReturnsConfiguredValues() {
        CompactMap<String, Integer> map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 7;
            }

            @Override
            protected boolean isCaseInsensitive() {
                // case‑insensitive => CASE_SENSITIVE = false
                return true;
            }

            @Override
            protected String getOrdering() {
                return CompactMap.SORTED;
            }

            @Override
            protected String getSingleValueKey() {
                return "myKey";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new TreeMap<>();
            }
        };
        Map<String, Object> config = map.getConfig();
        Assertions.assertEquals(7, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(Boolean.FALSE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.SORTED, config.get(CompactMap.ORDERING));
        Assertions.assertEquals("myKey", config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(TreeMap.class, config.get(CompactMap.MAP_TYPE));
        Assertions.assertThrows(UnsupportedOperationException.class, () -> config.put("new", "value"), "Config map should be unmodifiable");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CaseInsensitiveMap_UnwrapsToWrappedMapClass() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(false).sortedOrder().build();
        Map<String, Object> config = map.getConfig();
        // The underlying map is a CaseInsensitiveMap that wraps a TreeMap.
        Assertions.assertEquals(TreeMap.class, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CustomCompactSize_ReturnsSuppliedValue() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().compactSize(10).build();
        Map<String, Object> config = map.getConfig();
        Assertions.assertEquals(10, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.INSERTION, config.get(CompactMap.ORDERING));
        Assertions.assertEquals(CompactMap.DEFAULT_SINGLE_KEY, config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(HashMap.class, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CustomMapType_TreeMap_ReturnsTreeMapClass() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().mapType(TreeMap.class).build();
        Map<String, Object> config = map.getConfig();
        Assertions.assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.INSERTION, config.get(CompactMap.ORDERING));
        Assertions.assertEquals(CompactMap.DEFAULT_SINGLE_KEY, config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(TreeMap.class, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CaseInsensitive_DefaultOrdering_ReturnsFalseFlag() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(false).build();
        Map<String, Object> config = map.getConfig();
        Assertions.assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(Boolean.FALSE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.INSERTION, config.get(CompactMap.ORDERING));
        Assertions.assertEquals(CompactMap.DEFAULT_SINGLE_KEY, config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(HashMap.class, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_SortedOrdering_UsesTreeMap() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().sortedOrder().build();
        Map<String, Object> config = map.getConfig();
        Assertions.assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.SORTED, config.get(CompactMap.ORDERING));
        Assertions.assertEquals(CompactMap.DEFAULT_SINGLE_KEY, config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(TreeMap.class, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CustomSingleKey_ReturnsProvidedKey() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().singleValueKey("uid").build();
        Map<String, Object> config = map.getConfig();
        Assertions.assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(CompactMap.DEFAULT_CASE_SENSITIVE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.INSERTION, config.get(CompactMap.ORDERING));
        Assertions.assertEquals("uid", config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(HashMap.class, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CaseInsensitive_WithLinkedHashMap_ReturnsLinkedHashMapClass() {
        CompactMap<String, Object> map = CompactMap.<String, Object>builder().caseSensitive(false).mapType(LinkedHashMap.class).build();
        Map<String, Object> config = map.getConfig();
        Assertions.assertEquals(CompactMap.DEFAULT_COMPACT_SIZE, config.get(CompactMap.COMPACT_SIZE));
        Assertions.assertEquals(Boolean.FALSE, config.get(CompactMap.CASE_SENSITIVE));
        Assertions.assertEquals(CompactMap.INSERTION, config.get(CompactMap.ORDERING));
        Assertions.assertEquals(CompactMap.DEFAULT_SINGLE_KEY, config.get(CompactMap.SINGLE_KEY));
        Assertions.assertEquals(LinkedHashMap.class, config.get(CompactMap.MAP_TYPE));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_NullUnderlyingMap_ThrowsNullPointerException() {
        CompactMap<String, Object> map = new CompactMap<String, Object>() {

            @Override
            protected Map<String, Object> getNewMap() {
                // break the contract deliberately
                return null;
            }
        };
        Assertions.assertThrows(NullPointerException.class, map::getConfig, "Calling getConfig() when getNewMap() returns null should throw NPE");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void getConfig_CaseInsensitiveMapWithNullWrappedMap_ThrowsNullPointerException() {
        CompactMap<String, Object> map = new CompactMap<String, Object>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                // Create a CaseInsensitiveMap that wraps a null map.
                return new CaseInsensitiveMap<>(null);
            }
        };
        Assertions.assertThrows(NullPointerException.class, map::getConfig, "When the wrapped map inside CaseInsensitiveMap is null, getConfig() should NPE");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest
    @MethodSource("unmodifiableConfigProvider")
    void getConfig_UnmodifiableMap_ThrowsOnModification(CompactMap<String, Object> map) {
        Map<String, Object> config = map.getConfig();
        Assertions.assertThrows(UnsupportedOperationException.class, () -> config.put("anyKey", "anyValue"), "Config map should be unmodifiable");
    }
}
