package com.cedarsoftware.util;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;
import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_remove_Test {

    /* -------------------------------------------------- *
     *  Single‑entry removal tests                        *
     * -------------------------------------------------- */
    @Nested
    class SingleEntryRemoval {

        @Test
        void removeExistingKeyReturnsValueAndEmptiesMap() {
            CompactMap<String, String> map = new CompactMap<>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, String> getNewMap() {
                    return new LinkedHashMap<>();
                }

                @Override
                protected String getSingleValueKey() {
                    return "id";
                }
            };
            // populate the single‑entry storage
            assertNull(map.put("id", "value1"));
            assertEquals(1, map.size());
            String removed = map.remove("id");
            assertEquals("value1", removed);
            assertEquals(0, map.size());
            assertTrue(map.isEmpty());
        }

        @Test
        void removeNonExistingKeyReturnsNullAndLeavesMapIntact() {
            CompactMap<String, String> map = new CompactMap<>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, String> getNewMap() {
                    return new LinkedHashMap<>();
                }

                @Override
                protected String getSingleValueKey() {
                    return "id";
                }
            };
            assertNull(map.put("id", "value1"));
            assertEquals(1, map.size());
            assertNull(map.remove("unknown"));
            assertEquals(1, map.size());
            assertEquals("value1", map.get("id"));
        }

        @Test
        void removeWithIncompatibleKeyTypeThrowsClassCastException() {
            CompactMap<String, String> map = new CompactMap<>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, String> getNewMap() {
                    return new LinkedHashMap<>();
                }

                @Override
                protected String getSingleValueKey() {
                    return "id";
                }
            };
            assertNull(map.put("id", "value"));
            // The internal single‑entry handling will attempt to compare the supplied key
            // with the stored String key, causing a ClassCastException.
            assertThrows(ClassCastException.class, () -> map.remove(456));
        }
    }

    /* -------------------------------------------------- *
     *  Compact‑array removal tests                       *
     * -------------------------------------------------- */
    @Nested
    class CompactArrayRemoval {

        private CompactMap<String, String> createTwoEntryArrayMap() {
            CompactMap<String, String> map = compactArrayMap(5);
            assertNull(map.put("a", "alpha"));
            assertNull(map.put("b", "beta"));
            assertEquals(2, map.size());
            return map;
        }

        @Test
        void removeFirstKeyShrinksToSingleEntry() {
            CompactMap<String, String> map = createTwoEntryArrayMap();
            String removed = map.remove("a");
            assertEquals("alpha", removed);
            assertEquals(1, map.size());
            // After removal the map should have transitioned to the single‑entry state.
            assertEquals("beta", map.get("b"));
            assertNull(map.get("a"));
        }

        @Test
        void removeNonExistingKeyReturnsNullAndSizeUnchanged() {
            CompactMap<String, String> map = createTwoEntryArrayMap();
            assertNull(map.remove("c"));
            assertEquals(2, map.size());
        }

        @Test
        void removeWithIncompatibleKeyTypeThrowsClassCastException() {
            CompactMap<String, String> map = compactArrayMap(5);
            assertNull(map.put("x", "X"));
            assertNull(map.put("y", "Y"));
            assertEquals(2, map.size());
            // Boolean key cannot be compared to stored String keys via equals()
            assertThrows(ClassCastException.class, () -> map.remove(true));
        }
    }

    /* -------------------------------------------------- *
     *  Backing‑map removal tests                         *
     * -------------------------------------------------- */
    @Nested
    class BackingMapRemoval {

        @Test
        void removeExistingKeyDecrementsSize() {
            CompactMap<String, String> map = backingMapConfigured(2);
            assertNull(map.put("k10", "v10"));
            assertNull(map.put("k20", "v20"));
            // forces transition to backing map
            assertNull(map.put("k25", "v25"));
            assertEquals(3, map.size());
            String removed = map.remove("k25");
            assertEquals("v25", removed);
            assertEquals(2, map.size());
            assertNull(map.get("k25"));
        }

        @Test
        void removeNonExistingKeyReturnsNullAndSizeUnchanged() {
            CompactMap<String, String> map = backingMapConfigured(2);
            assertNull(map.put("a", "A"));
            assertNull(map.put("b", "B"));
            assertNull(map.put("c", "C"));
            assertEquals(3, map.size());
            assertNull(map.remove("missing"));
            assertEquals(3, map.size());
        }

        @Test
        void removeNullKeyWhenBackingMapDisallowsNullThrowsNPE() {
            CompactMap<String, String> map = concurrentHashMapBacked();
            // ConcurrentHashMap does not permit null keys; inserting null would already fail.
            // We only need to invoke remove(null) and verify the exception propagation.
            assertThrows(NullPointerException.class, () -> map.remove(null));
        }

        @Test
        void removeKeyOfWrongTypeFromTreeMapThrowsClassCastException() {
            CompactMap<String, String> map = treeMapBacked();
            assertNull(map.put("alpha", "A"));
            assertNull(map.put("beta", "B"));
            // TreeMap expects Comparable keys; passing an Integer triggers ClassCastException.
            assertThrows(ClassCastException.class, () -> map.remove(123));
        }

        @Test
        void removeFromUnmodifiableBackingMapThrowsUnsupportedOperationException() {
            CompactMap<String, String> map = unmodifiableBackingMap();
            assertThrows(UnsupportedOperationException.class, () -> map.remove("existingKey"));
        }
    }

    /* -------------------------------------------------- *
     *  Null‑key handling tests                           *
     * -------------------------------------------------- */
    @Nested
    class NullKeyHandling {

        @Test
        void removeNullKeyWhenEntryExistsReturnsValue() {
            CompactMap<String, String> map = new CompactMap<>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, String> getNewMap() {
                    return new LinkedHashMap<>();
                }

                @Override
                protected String getSingleValueKey() {
                    return "id";
                }
            };
            // HashMap (default backing) permits null keys.
            assertNull(map.put(null, "nullValue"));
            assertEquals(1, map.size());
            String removed = map.remove(null);
            assertEquals("nullValue", removed);
            assertEquals(0, map.size());
        }

        @Test
        void removeNullKeyWhenNotPresentReturnsNull() {
            CompactMap<String, String> map = new CompactMap<>() {

                @Override
                protected int compactSize() {
                    return 3;
                }

                @Override
                protected Map<String, String> getNewMap() {
                    return new LinkedHashMap<>();
                }

                @Override
                protected String getSingleValueKey() {
                    return "id";
                }
            };
            assertNull(map.remove(null));
            assertEquals(0, map.size());
        }
    }

    /* -------------------------------------------------- *
     *  Case‑insensitive removal tests                    *
     * -------------------------------------------------- */
    @Nested
    class CaseInsensitiveRemoval {

        @Test
        void removeWithDifferentCaseSucceedsWhenCaseSensitiveFalse() {
            CompactMap<String, String> map = caseInsensitiveMap();
            assertNull(map.put("Key", "mixedCaseValue"));
            assertEquals(1, map.size());
            // Removal should succeed regardless of case.
            String removed = map.remove("KEY");
            assertEquals("mixedCaseValue", removed);
            assertEquals(0, map.size());
        }

        @Test
        void removeNonExistingKeyReturnsNullEvenWhenCaseInsensitive() {
            CompactMap<String, String> map = caseInsensitiveMap();
            assertNull(map.put("Alpha", "A"));
            assertNull(map.remove("another"));
            assertEquals(1, map.size());
        }
    }

    /**
     * Creates a CompactMap that will stay in the compact‑array state for up to {@code size} entries.
     */
    private static <K, V> CompactMap<K, V> compactArrayMap(int compactSize) {
        return new CompactMap<>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected Map<K, V> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            @SuppressWarnings("unchecked")
            protected K getSingleValueKey() {
                return (K) "id";
            }
        };
    }

    /**
     * Creates a CompactMap that forces transition to a backing map after {@code compactSize} entries.
     */
    private static <K, V> CompactMap<K, V> backingMapConfigured(int compactSize) {
        return new CompactMap<>() {

            @Override
            protected int compactSize() {
                return compactSize;
            }

            @Override
            protected Map<K, V> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            @SuppressWarnings("unchecked")
            protected K getSingleValueKey() {
                return (K) "id";
            }
        };
    }

    /**
     * Creates a case‑insensitive CompactMap (builder style) for the relevant tests.
     */
    private static CompactMap<String, String> caseInsensitiveMap() {
        // Builder creates a case‑insensitive map.
        return CompactMap.<String, String>builder().caseSensitive(false).build();
    }

    /**
     * Creates a CompactMap backed by the supplied map (used for unmodifiable map test).
     */
    private static CompactMap<String, String> unmodifiableBackingMap() {
        Map<String, String> base = Collections.unmodifiableMap(new HashMap<>(Map.of("existingKey", "value")));
        return CompactMap.<String, String>builder().sourceMap(base).build();
    }

    /**
     * Creates a CompactMap that uses a ConcurrentHashMap as its backing map.
     */
    private static CompactMap<String, String> concurrentHashMapBacked() {
        return CompactMap.<String, String>builder().mapType(ConcurrentHashMap.class).build();
    }

    /**
     * Creates a CompactMap that uses a TreeMap (String keys) as its backing map.
     */
    private static CompactMap<String, String> treeMapBacked() {
        return CompactMap.<String, String>builder().sortedOrder().build();
    }

    /* -------------------------------------------------- *
     *  Compact‑array removal tests                       *
     * -------------------------------------------------- */
    private CompactMap<String, String> createTwoEntryArrayMap() {
        CompactMap<String, String> map = compactArrayMap(5);
        assertNull(map.put("a", "alpha"));
        assertNull(map.put("b", "beta"));
        assertEquals(2, map.size());
        return map;
    }

    /**
     * Verify that removing a key from an empty map returns {@code null}
     * and does not change the size.
     */
    @Test
    void removeFromEmptyMapReturnsNull() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertTrue(map.isEmpty());
        assertNull(map.remove("any"));
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    /**
     * Verify removal when the map contains exactly one entry.
     * The internal representation is the special single‑value storage.
     */
    @Test
    void removeSingleEntry() {
        CompactMap<String, Double> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Double> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }
        };
        assertNull(map.put("value", 10.0));
        assertEquals(1, map.size());
        Double removed = map.remove("value");
        assertEquals(10.0, removed);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    /**
     * Verify removal when the map is using the compact array representation
     * (size between 2 and {@code compactSize()} inclusive).
     */
    @Test
    void removeFromCompactArray() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertNull(map.put("a", 1));
        assertNull(map.put("b", 2));
        assertNull(map.put("c", 3));
        assertEquals(3, map.size());
        Integer removed = map.remove("b");
        assertEquals(Integer.valueOf(2), removed);
        assertEquals(2, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(3), map.get("c"));
    }

    /**
     * Verify removal when the map has grown beyond {@code compactSize()}
     * and therefore uses a backing {@link java.util.Map}.
     */
    @Test
    void removeFromBackingMap() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertNull(map.put("x", "X"));
        assertNull(map.put("y", "Y"));
        assertNull(map.put("z", "Z"));
        assertEquals(3, map.size());
        String removed = map.remove("y");
        assertEquals("Y", removed);
        assertEquals(2, map.size());
        assertNull(map.get("y"));
        assertNull(map.remove("nonexistent"));
        assertEquals(2, map.size());
    }

    @Test
    void removeFromEmptyCompactMap() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertTrue(map.isEmpty());
        assertNull(map.remove("anyKey"));
        assertEquals(0, map.size());
    }

    /* -------------------------------------------------- *
     *  Single‑entry removal tests                        *
     * -------------------------------------------------- */
    @Test
    public void removeExistingKeyReturnsValueAndEmptiesMap() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        // populate the single‑entry storage
        assertNull(map.put("id", "value1"));
        assertEquals(1, map.size());
        String removed = map.remove("id");
        assertEquals("value1", removed);
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    @Test
    public void removeNonExistingKeyReturnsNullAndLeavesMapIntact() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertNull(map.put("id", "value1"));
        assertEquals(1, map.size());
        assertNull(map.remove("unknown"));
        assertEquals(1, map.size());
        assertEquals("value1", map.get("id"));
    }

    @Test
    public void removeWithIncompatibleKeyTypeThrowsClassCastException() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertNull(map.put("id", "value"));
        try {
            map.remove(456);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
            // expected
        }
    }

    @Test
    public void removeFirstKeyShrinksToSingleEntry() {
        CompactMap<String, String> map = createTwoEntryArrayMap();
        String removed = map.remove("a");
        assertEquals("alpha", removed);
        assertEquals(1, map.size());
        // After removal the map should have transitioned to the single‑entry state.
        assertEquals("beta", map.get("b"));
        assertNull(map.get("a"));
    }

    @Test
    public void removeNonExistingKeyReturnsNullAndSizeUnchanged() {
        CompactMap<String, String> map = createTwoEntryArrayMap();
        assertNull(map.remove("c"));
        assertEquals(2, map.size());
    }

    @Test
    public void removeWithIncompatibleKeyTypeThrowsClassCastException_Array() {
        CompactMap<String, String> map = compactArrayMap(5);
        assertNull(map.put("x", "X"));
        assertNull(map.put("y", "Y"));
        assertEquals(2, map.size());
        try {
            map.remove(true);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
            // expected
        }
    }

    /* -------------------------------------------------- *
     *  Backing‑map removal tests                         *
     * -------------------------------------------------- */
    @Test
    public void removeExistingKeyDecrementsSize() {
        CompactMap<String, String> map = backingMapConfigured(2);
        assertNull(map.put("k10", "v10"));
        assertNull(map.put("k20", "v20"));
        // forces transition to backing map
        assertNull(map.put("k25", "v25"));
        assertEquals(3, map.size());
        String removed = map.remove("k25");
        assertEquals("v25", removed);
        assertEquals(2, map.size());
        assertNull(map.get("k25"));
    }

    @Test
    public void removeNonExistingKeyReturnsNullAndSizeUnchanged_Backing() {
        CompactMap<String, String> map = backingMapConfigured(2);
        assertNull(map.put("a", "A"));
        assertNull(map.put("b", "B"));
        assertNull(map.put("c", "C"));
        assertEquals(3, map.size());
        assertNull(map.remove("missing"));
        assertEquals(3, map.size());
    }

    @Test
    public void removeNullKeyWhenBackingMapDisallowsNullThrowsNPE() {
        CompactMap<String, String> map = concurrentHashMapBacked();
        try {
            map.remove(null);
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
            // expected
        }
    }

    @Test
    public void removeKeyOfWrongTypeFromTreeMapThrowsClassCastException() {
        CompactMap<String, String> map = treeMapBacked();
        assertNull(map.put("alpha", "A"));
        assertNull(map.put("beta", "B"));
        try {
            map.remove(123);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
            // expected
        }
    }

    @Test
    public void removeFromUnmodifiableBackingMapThrowsUnsupportedOperationException() {
        CompactMap<String, String> map = unmodifiableBackingMap();
        try {
            map.remove("existingKey");
            fail("Expected UnsupportedOperationException");
        } catch (UnsupportedOperationException ignored) {
            // expected
        }
    }

    /* -------------------------------------------------- *
     *  Null‑key handling tests                           *
     * -------------------------------------------------- */
    @Test
    public void removeNullKeyWhenEntryExistsReturnsValue() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        // HashMap (default backing) permits null keys.
        assertNull(map.put(null, "nullValue"));
        assertEquals(1, map.size());
        String removed = map.remove(null);
        assertEquals("nullValue", removed);
        assertEquals(0, map.size());
    }

    @Test
    public void removeNullKeyWhenNotPresentReturnsNull() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertNull(map.remove(null));
        assertEquals(0, map.size());
    }

    /* -------------------------------------------------- *
     *  Case‑insensitive removal tests                    *
     * -------------------------------------------------- */
    @Test
    public void removeWithDifferentCaseSucceedsWhenCaseSensitiveFalse() {
        CompactMap<String, String> map = caseInsensitiveMap();
        assertNull(map.put("Key", "mixedCaseValue"));
        assertEquals(1, map.size());
        // Removal should succeed regardless of case.
        String removed = map.remove("KEY");
        assertEquals("mixedCaseValue", removed);
        assertEquals(0, map.size());
    }

    @Test
    public void removeNonExistingKeyReturnsNullEvenWhenCaseInsensitive() {
        CompactMap<String, String> map = caseInsensitiveMap();
        assertNull(map.put("Alpha", "A"));
        assertNull(map.remove("another"));
        assertEquals(1, map.size());
    }
}
