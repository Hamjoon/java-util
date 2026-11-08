package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import java.util.*;
import static com.cedarsoftware.util.CompactMap.*;

class CompactMap_withConfig_Test {

    /**
     * A CompactMap that reports a specific ordering and compact size.
     */
    private static class OrderingMap extends CompactMap<String, Integer> {

        private final String ordering;

        private final int compactSize;

        OrderingMap(String ordering, int compactSize) {
            this.ordering = ordering;
            this.compactSize = compactSize;
        }

        @Override
        protected String getOrdering() {
            return ordering;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected Map<String, Integer> getNewMap() {
            return new LinkedHashMap<>();
        }
    }

    /**
     * A CompactMap that returns a custom single‑value key (or null).
     */
    private static class SingleKeyMap extends CompactMap<String, Integer> {

        private final String singleKey;

        SingleKeyMap(String singleKey) {
            this.singleKey = singleKey;
        }

        @Override
        protected String getSingleValueKey() {
            return singleKey;
        }

        @Override
        protected int compactSize() {
            return 10;
        }

        @Override
        protected Map<String, Integer> getNewMap() {
            return new HashMap<>();
        }
    }

    /**
     * A CompactMap whose backing map is a {@code CaseInsensitiveMap}.
     */
    @SuppressWarnings("unchecked")
    private static class CaseInsensitiveBackingMap extends CompactMap<String, Integer> {

        private final CaseInsensitiveMap<String, Integer> ciMap;

        CaseInsensitiveBackingMap() {
            // mock the CaseInsensitiveMap – we only need its class and wrapped map
            ciMap = (CaseInsensitiveMap<String, Integer>) Mockito.mock(CaseInsensitiveMap.class);
            Mockito.when(ciMap.getWrappedMap()).thenReturn(new LinkedHashMap<>());
        }

        @Override
        protected Map<String, Integer> getNewMap() {
            return ciMap;
        }

        @Override
        protected boolean isCaseInsensitive() {
            return true;
        }

        @Override
        protected int compactSize() {
            return 10;
        }
    }

    /**
     * Verify that a {@code null} configuration map is rejected.
     */
    @Test
    void testWithConfigNullThrows() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertThrows(IllegalArgumentException.class, () -> map.withConfig(null));
    }

    /**
     * Verify that the original entries are copied to the newly created map.
     */
    @Test
    void testWithConfigCopiesExistingEntries() {
        CompactMap<String, Integer> original = new CompactMap<>();
        original.put("one", 1);
        original.put("two", 2);
        // empty config – defaults are used
        Map<String, Object> cfg = new HashMap<>();
        CompactMap<String, Integer> copy = original.withConfig(cfg);
        assertEquals(2, copy.size());
        assertEquals(1, copy.get("one"));
        assertEquals(2, copy.get("two"));
    }

    /**
     * Verify that the {@code CASE_SENSITIVE} flag in the config overrides the
     * map's intrinsic case‑sensitivity setting.
     */
    @Test
    void testWithConfigCaseSensitivityOverrides() {
        // Subclass that reports itself as case‑insensitive (i.e. !isCaseInsensitive() == false)
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                // default would be case‑insensitive
                return true;
            }

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        // Config forces case‑sensitive handling
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CASE_SENSITIVE, true);
        CompactMap<String, Integer> map = base.withConfig(cfg);
        map.put("A", 1);
        // distinct because we forced case‑sensitivity
        map.put("a", 2);
        assertEquals(2, map.size(), "Both keys should be stored separately");
    }

    /**
     * Verify that the {@code ORDERING} option is honoured.
     */
    @Test
    void testWithConfigOrderingSorted() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // The backing map does not matter for ordering; the builder will enforce it.
                return new LinkedHashMap<>();
            }
        };
        Map<String, Object> cfg = new HashMap<>();
        // request sorted order
        cfg.put(ORDERING, SORTED);
        CompactMap<String, Integer> map = base.withConfig(cfg);
        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);
        // The iterator should return keys in natural order: a, b, c
        String[] expected = { "a", "b", "c" };
        int idx = 0;
        for (String key : map.keySet()) {
            assertEquals(expected[idx++], key);
        }
        assertEquals(3, idx, "All three keys should have been iterated");
    }

    /**
     * Verify that the {@code MAP_TYPE} option is applied.
     * The test checks observable behaviour that results from using a {@link TreeMap}
     * as the backing map (i.e., natural ordering without explicitly setting ORDERING).
     */
    @Test
    void testWithConfigMapTypeTreeMap() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // Default backing map – will be overridden by the config
                return new LinkedHashMap<>();
            }
        };
        Map<String, Object> cfg = new HashMap<>();
        // request TreeMap backing
        cfg.put(MAP_TYPE, TreeMap.class);
        CompactMap<String, Integer> map = base.withConfig(cfg);
        map.put("b", 2);
        map.put("c", 3);
        map.put("a", 1);
        // Because the backing map is a TreeMap, iteration order is sorted
        String[] expected = { "a", "b", "c" };
        int idx = 0;
        for (String key : map.keySet()) {
            assertEquals(expected[idx++], key);
        }
        assertEquals(3, idx);
    }

    /**
     * Verify that the {@code COMPACT_SIZE} option is honoured.
     * We cannot directly inspect the internal threshold, but we can ensure that
     * the map still works correctly when the configured size is smaller than the
     * number of inserted elements.
     */
    @Test
    void testWithConfigCompactSize() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // original threshold (default)
                return 50;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        Map<String, Object> cfg = new HashMap<>();
        // lower the threshold
        cfg.put(COMPACT_SIZE, 5);
        CompactMap<String, Integer> map = base.withConfig(cfg);
        // Insert more than the new threshold and ensure map behaves normally
        for (int i = 0; i < 10; i++) {
            map.put("k" + i, i);
        }
        assertEquals(10, map.size());
        assertEquals(9, map.get("k9"));
    }

    // -----------------------------------------------------------------
    @Test
    void testCompactSizeDefaultWhenMissing() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 30;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        CompactMap<String, Integer> result = base.withConfig(new HashMap<>());
        assertEquals(30, result.getConfig().get(COMPACT_SIZE));
    }

    @Test
    void testCompactSizeOverridesConfig() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 30;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(COMPACT_SIZE, 70);
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertEquals(70, result.getConfig().get(COMPACT_SIZE));
    }

    // -----------------------------------------------------------------
    @Test
    void testCaseSensitiveDefaultWhenMissing() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                return false;
            }

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        CompactMap<String, Integer> result = base.withConfig(new HashMap<>());
        // isCaseInsensitive() == false → caseSensitive = true
        assertTrue((Boolean) result.getConfig().get(CASE_SENSITIVE));
    }

    @Test
    void testCaseSensitiveOverridesConfig() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CASE_SENSITIVE, false);
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertFalse((Boolean) result.getConfig().get(CASE_SENSITIVE));
    }

    // -----------------------------------------------------------------
    @Test
    void testOrderingDefaultWhenMissing() {
        CompactMap<String, Integer> base = new OrderingMap(INSERTION, 10);
        CompactMap<String, Integer> result = base.withConfig(new HashMap<>());
        assertEquals(INSERTION, result.getConfig().get(ORDERING));
    }

    @Test
    void testOrderingSorted() {
        CompactMap<String, Integer> base = new OrderingMap(INSERTION, 10);
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ORDERING, SORTED);
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertEquals(SORTED, result.getConfig().get(ORDERING));
    }

    @Test
    void testOrderingReverse() {
        CompactMap<String, Integer> base = new OrderingMap(INSERTION, 10);
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ORDERING, REVERSE);
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertEquals(REVERSE, result.getConfig().get(ORDERING));
        // verify that iteration is reverse sorted
        result.put("a", 1);
        result.put("b", 2);
        result.put("c", 3);
        List<String> iterated = new ArrayList<>();
        result.keySet().forEach(iterated::add);
        assertEquals(Arrays.asList("c", "b", "a"), iterated);
    }

    @Test
    void testOrderingInsertion() {
        CompactMap<String, Integer> base = new OrderingMap(INSERTION, 10);
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ORDERING, INSERTION);
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertEquals(INSERTION, result.getConfig().get(ORDERING));
        result.put("first", 1);
        result.put("second", 2);
        result.put("third", 3);
        List<String> iterated = new ArrayList<>();
        result.keySet().forEach(iterated::add);
        assertEquals(Arrays.asList("first", "second", "third"), iterated);
    }

    @Test
    void testOrderingUnknownFallsBackToUnordered() {
        CompactMap<String, Integer> base = new OrderingMap(INSERTION, 10);
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ORDERING, "unknown");
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertEquals(UNORDERED, result.getConfig().get(ORDERING));
    }

    // -----------------------------------------------------------------
    @Test
    void testSingleKeyDefaultFromInstance() {
        CompactMap<String, Integer> base = new SingleKeyMap("myId");
        CompactMap<String, Integer> result = base.withConfig(new HashMap<>());
        assertEquals("myId", result.getConfig().get(SINGLE_KEY));
    }

    @Test
    void testSingleKeyOverridesConfig() {
        CompactMap<String, Integer> base = new SingleKeyMap("myId");
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(SINGLE_KEY, "customKey");
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertEquals("customKey", result.getConfig().get(SINGLE_KEY));
    }

    @Test
    void testSingleKeyFallsBackToDefaultWhenBothNull() {
        CompactMap<String, Integer> base = new SingleKeyMap(null) {

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        CompactMap<String, Integer> result = base.withConfig(new HashMap<>());
        assertEquals(DEFAULT_SINGLE_KEY, result.getConfig().get(SINGLE_KEY));
    }

    // -----------------------------------------------------------------
    @Test
    void testMapTypeDefaultFromBackingMap() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new TreeMap<>();
            }
        };
        CompactMap<String, Integer> result = base.withConfig(new HashMap<>());
        assertEquals(TreeMap.class, result.getConfig().get(MAP_TYPE));
    }

    @Test
    void testMapTypeOverridesConfig() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 10;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(MAP_TYPE, LinkedHashMap.class);
        CompactMap<String, Integer> result = base.withConfig(cfg);
        assertEquals(LinkedHashMap.class, result.getConfig().get(MAP_TYPE));
    }

    @Test
    void testMapTypeWhenBackingIsCaseInsensitiveMap() {
        CompactMap<String, Integer> base = new CaseInsensitiveBackingMap();
        CompactMap<String, Integer> result = base.withConfig(new HashMap<>());
        // The builder should unwrap the CaseInsensitiveMap and use the wrapped map's class
        assertEquals(LinkedHashMap.class, result.getConfig().get(MAP_TYPE));
    }

    // -----------------------------------------------------------------
    @Test
    void testFullConfigIntegration() {
        CompactMap<String, Integer> base = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 20;
            }

            @Override
            protected boolean isCaseInsensitive() {
                return false;
            }

            @Override
            protected String getOrdering() {
                return INSERTION;
            }

            @Override
            protected String getSingleValueKey() {
                return "oldKey";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(COMPACT_SIZE, 80);
        cfg.put(CASE_SENSITIVE, false);
        cfg.put(ORDERING, SORTED);
        cfg.put(SINGLE_KEY, "uuid");
        cfg.put(MAP_TYPE, LinkedHashMap.class);
        CompactMap<String, Integer> result = base.withConfig(cfg);
        Map<String, Object> conf = result.getConfig();
        assertAll(() -> assertEquals(80, conf.get(COMPACT_SIZE)), () -> assertEquals(false, conf.get(CASE_SENSITIVE)), () -> assertEquals(SORTED, conf.get(ORDERING)), () -> assertEquals("uuid", conf.get(SINGLE_KEY)), () -> assertEquals(LinkedHashMap.class, conf.get(MAP_TYPE)));
    }

    // -----------------------------------------------------------------
    @Test
    void testInvalidCompactSizeType() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(COMPACT_SIZE, "notAnInteger");
        assertThrows(ClassCastException.class, () -> map.withConfig(cfg));
    }

    @Test
    void testInvalidCaseSensitiveType() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CASE_SENSITIVE, "yes");
        assertThrows(ClassCastException.class, () -> map.withConfig(cfg));
    }

    @Test
    void testInvalidOrderingType() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(ORDERING, 123);
        assertThrows(ClassCastException.class, () -> map.withConfig(cfg));
    }

    @Test
    void testInvalidMapTypeClass() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> cfg = new HashMap<>();
        // not a Map
        cfg.put(MAP_TYPE, String.class);
        // Builder.mapType will throw IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> map.withConfig(cfg));
    }

    @Test
    void testInvalidSingleKeyType() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(SINGLE_KEY, 42);
        assertThrows(ClassCastException.class, () -> map.withConfig(cfg));
    }

    // -----------------------------------------------------------------
    @Test
    void testCompilerUnavailableThrowsIllegalState() {
        try (MockedStatic<ReflectionUtils> mock = Mockito.mockStatic(ReflectionUtils.class)) {
            mock.when(ReflectionUtils::isJavaCompilerAvailable).thenReturn(false);
            CompactMap<String, Integer> map = new CompactMap<>();
            Map<String, Object> cfg = new HashMap<>();
            IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.withConfig(cfg));
            assertTrue(ex.getMessage().contains("CompactMap builder pattern requires the Java Compiler"));
        }
    }
}
