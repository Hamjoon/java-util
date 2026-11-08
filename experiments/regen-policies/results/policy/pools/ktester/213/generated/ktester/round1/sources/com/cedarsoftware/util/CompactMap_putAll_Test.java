package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.concurrent.ConcurrentModificationException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CompactMap_putAll_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private CompactMap<String, Integer> map;

    class CmeMap extends AbstractMap<String, Integer> {

        private final Map<String, Integer> delegate = new HashMap<>();

        CmeMap() {
            delegate.put("a", 1);
            delegate.put("b", 2);
            delegate.put("c", 3);
        }

        @Override
        public Set<Entry<String, Integer>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<String, Integer>> iterator() {
                    return new Iterator<>() {

                        private final Iterator<Entry<String, Integer>> it = delegate.entrySet().iterator();

                        private boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<String, Integer> next() {
                            if (first) {
                                first = false;
                                throw new ConcurrentModificationException("simulated");
                            }
                            return it.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }
    }

    class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadHashValue {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad value hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class CmeMap extends AbstractMap<String, Integer> {

        private final Map<String, Integer> delegate = new HashMap<>();

        CmeMap() {
            delegate.put("a", 1);
            delegate.put("b", 2);
            delegate.put("c", 3);
        }

        @Override
        public Set<Entry<String, Integer>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<String, Integer>> iterator() {
                    return new Iterator<>() {

                        private final Iterator<Entry<String, Integer>> it = delegate.entrySet().iterator();

                        private boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return it.hasNext();
                        }

                        @Override
                        public Entry<String, Integer> next() {
                            if (first) {
                                first = false;
                                throw new ConcurrentModificationException("simulated");
                            }
                            return it.next();
                        }
                    };
                }

                @Override
                public int size() {
                    return delegate.size();
                }
            };
        }
    }

    class BadHashKey {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    class BadHashValue {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad value hash");
        }

        @Override
        public boolean equals(Object o) {
            return this == o;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor; the generated subclass will be the
        // plain CompactMap because we do not supply any special configuration.
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no stray state leaks between tests.
        map = null;
    }

    private static Stream<Map<String, Integer>> nullOrEmptyProvider() {
        return Stream.of(null, Collections.emptyMap());
    }

    @Test
    void putAll_NullMap_DoesNothing() {
        map.put("existing", 42);
        int originalSize = map.size();
        // should be a no‑op, no exception
        map.putAll(null);
        assertEquals(originalSize, map.size(), "Size must stay unchanged when putAll receives null");
        assertEquals(42, map.get("existing"));
    }

    @Test
    void putAll_EmptyMap_DoesNothing() {
        map.put("foo", 1);
        int originalSize = map.size();
        // empty source map
        map.putAll(new HashMap<>());
        assertEquals(originalSize, map.size(), "Size must stay unchanged when putAll receives an empty map");
        assertEquals(1, map.get("foo"));
    }

    @Test
    void putAll_AddsAllEntries() {
        Map<String, Integer> source = new HashMap<>();
        source.put("a", 10);
        source.put("b", 20);
        source.put("c", 30);
        map.putAll(source);
        assertEquals(3, map.size(), "All entries from source must be added");
        assertEquals(10, map.get("a"));
        assertEquals(20, map.get("b"));
        assertEquals(30, map.get("c"));
    }

    @Test
    void putAll_OverridesExistingKeys() {
        // Existing content
        map.put("x", 1);
        map.put("y", 2);
        assertEquals(2, map.size());
        // Source map contains a key that already exists ("x") and a new key ("z")
        Map<String, Integer> source = new HashMap<>();
        // should replace the old value
        source.put("x", 99);
        // new entry
        source.put("z", 3);
        map.putAll(source);
        assertEquals(3, map.size(), "Size must reflect the new total number of distinct keys");
        assertEquals(99, map.get("x"), "Existing key should be overwritten");
        assertEquals(2, map.get("y"), "Unchanged key must retain its original value");
        assertEquals(3, map.get("z"));
    }

    @Test
    void putAll_OnBuilderGeneratedMap_WorksCorrectly() {
        // Build a CompactMap with a custom configuration (case‑insensitive, sorted)
        CompactMap<String, Integer> customMap = CompactMap.<String, Integer>builder().caseSensitive(false).sortedOrder().compactSize(// small compact size to force internal transitions
        5).build();
        // Populate the map using putAll
        Map<String, Integer> source = new HashMap<>();
        source.put("Alpha", 1);
        source.put("beta", 2);
        source.put("GAMMA", 3);
        customMap.putAll(source);
        // Because the map is case‑insensitive, keys differing only by case should be treated as equal.
        // Verify that the map contains exactly three entries and that look‑ups are case‑insensitive.
        assertEquals(3, customMap.size());
        assertEquals(1, customMap.get("alpha"));
        assertEquals(2, customMap.get("BETA"));
        assertEquals(3, customMap.get("gamma"));
    }

    @ParameterizedTest
    @MethodSource("nullOrEmptyProvider")
    @DisplayName("putAll with null or empty map must be a no‑op")
    void putAll_NullOrEmptyMap_DoesNothing(Map<String, Integer> src) {
        // Seed the map with a known entry so we can verify that it stays unchanged.
        map.put("seed", 123);
        int originalSize = map.size();
        // The method under test – should not throw and must not modify the map.
        map.putAll(src);
        assertEquals(originalSize, map.size(), "Size must stay unchanged when putAll receives null or empty map");
        assertEquals(123, map.get("seed"));
    }

    @Test
    @DisplayName("putAll accepts null keys and null values")
    void putAll_NullKeyAndValue() {
        Map<String, Integer> source = new HashMap<>();
        // null value
        source.put("nullKey", null);
        // null key (allowed by default HashMap backing)
        source.put(null, 777);
        map.putAll(source);
        assertEquals(2, map.size());
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("nullKey"));
        assertEquals(777, map.get(null));
        assertNull(map.get("nullKey"));
    }

    @Test
    @DisplayName("putAll works with an unmodifiable source map")
    void putAll_UnmodifiableSource() {
        Map<String, Integer> mutable = new HashMap<>();
        mutable.put("uKey", 42);
        mutable.put("vKey", 99);
        Map<String, Integer> source = Collections.unmodifiableMap(mutable);
        map.putAll(source);
        assertEquals(2, map.size());
        assertEquals(42, map.get("uKey"));
        assertEquals(99, map.get("vKey"));
        // Ensure the source map is still unmodified.
        assertTrue(source instanceof Collections.UnmodifiableMap);
        assertEquals(2, source.size());
    }

    @Test
    @DisplayName("putAll handles a map whose size equals the default compact size")
    void putAll_LargeMapBoundary() {
        Map<String, Integer> source = new HashMap<>();
        for (int i = 0; i < 50; i++) {
            source.put("k" + i, i);
        }
        map.putAll(source);
        assertEquals(50, map.size(), "All 50 entries must be present");
        for (int i = 0; i < 50; i++) {
            assertEquals(i, map.get("k" + i));
        }
    }

    @Test
    @DisplayName("putAll with TreeMap backing throws NullPointerException for null key")
    void putAll_NullKeyTreeMap() {
        // Build a CompactMap that uses TreeMap as its backing map.
        Map<String, Object> cfg = new HashMap<>();
        cfg.put(CompactMap.MAP_TYPE, TreeMap.class);
        CompactMap<String, Integer> treeMapBacked = CompactMap.<String, Integer>builder().withConfig(cfg).build();
        Map<String, Integer> source = new HashMap<>();
        // illegal for TreeMap
        source.put(null, 1);
        assertThrows(NullPointerException.class, () -> treeMapBacked.putAll(source));
    }

    @Test
    @DisplayName("putAll with incompatible key type throws ClassCastException")
    void putAll_IncompatibleKeyType() {
        CompactMap<String, Integer> cm = new CompactMap<>();
        // Source map with an Integer key, but the CompactMap expects String keys.
        Map raw = new HashMap<>();
        // raw map to bypass compile‑time generics
        raw.put(123, 456);
        assertThrows(ClassCastException.class, () -> cm.putAll(raw));
    }

    @Test
    @DisplayName("putAll with incompatible value type throws ClassCastException")
    void putAll_IncompatibleValueType() {
        CompactMap<String, Integer> cm = new CompactMap<>();
        // Source map with a String value where Integer is expected.
        Map raw = new HashMap<>();
        raw.put("key", "notAnInteger");
        assertThrows(ClassCastException.class, () -> cm.putAll(raw));
    }

    @Test
    @DisplayName("putAll propagates ConcurrentModificationException from source map")
    void putAll_ConcurrentModification() {
        // Custom map whose entrySet iterator throws CME on first call.
        class CmeMap extends AbstractMap<String, Integer> {

            private final Map<String, Integer> delegate = new HashMap<>();

            CmeMap() {
                delegate.put("a", 1);
                delegate.put("b", 2);
                delegate.put("c", 3);
            }

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return new AbstractSet<>() {

                    @Override
                    public Iterator<Entry<String, Integer>> iterator() {
                        return new Iterator<>() {

                            private final Iterator<Entry<String, Integer>> it = delegate.entrySet().iterator();

                            private boolean first = true;

                            @Override
                            public boolean hasNext() {
                                return it.hasNext();
                            }

                            @Override
                            public Entry<String, Integer> next() {
                                if (first) {
                                    first = false;
                                    throw new ConcurrentModificationException("simulated");
                                }
                                return it.next();
                            }
                        };
                    }

                    @Override
                    public int size() {
                        return delegate.size();
                    }
                };
            }
        }
        CompactMap<String, Integer> cm = new CompactMap<>();
        Map<String, Integer> source = new CmeMap();
        assertThrows(ConcurrentModificationException.class, () -> cm.putAll(source));
    }

    @Test
    @DisplayName("putAll propagates exception from key.hashCode()")
    void putAll_KeyHashCodeException() {
        class BadHashKey {

            @Override
            public int hashCode() {
                throw new IllegalStateException("bad hash");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        CompactMap<Object, String> cm = new CompactMap<>();
        Map<Object, String> source = new HashMap<>();
        source.put(new BadHashKey(), "value");
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cm.putAll(source));
        assertEquals("bad hash", ex.getMessage());
    }

    @Test
    @DisplayName("putAll propagates exception from value.hashCode()")
    void putAll_ValueHashCodeException() {
        class BadHashValue {

            @Override
            public int hashCode() {
                throw new IllegalStateException("bad value hash");
            }

            @Override
            public boolean equals(Object o) {
                return this == o;
            }
        }
        CompactMap<String, Object> cm = new CompactMap<>();
        Map<String, Object> source = new HashMap<>();
        source.put("key", new BadHashValue());
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> cm.putAll(source));
        assertEquals("bad value hash", ex.getMessage());
    }
}
