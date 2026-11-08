package com.cedarsoftware.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

class CompactMap_remove_Test {

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void}, otherwise {@code false}
     */
    public static boolean returnsVoid(java.lang.reflect.Method method) {
        return method.getReturnType() == void.class;
    }

    // Provider for the compact‑array removal tests
    private static Stream<Arguments> compactArrayRemovalProvider() {
        // key present → expected value, key absent → expected null
        return Stream.of(Arguments.of("alpha", "A"), Arguments.of("missing", null));
    }

    // Provider for the backing‑map removal tests
    private static Stream<Arguments> backingMapRemovalProvider() {
        return Stream.of(Arguments.of("key25", "value25"), Arguments.of("nonexistent", null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeFromEmptyMap_returnsNull() {
        // Create a map with a tiny compact size so we can force the other states later.
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
        Assertions.assertTrue(map.isEmpty(), "Map should start empty");
        Assertions.assertNull(map.remove("anyKey"), "Removing from an empty map must return null");
        Assertions.assertTrue(map.isEmpty(), "Map must stay empty after a failed remove");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeSingleEntry_returnsValueAndEmptiesMap() {
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
        // put a single entry – this triggers the “single entry” storage path
        Assertions.assertNull(map.put("value", 10.0d));
        Assertions.assertEquals(1, map.size());
        // remove it
        Double removed = map.remove("value");
        Assertions.assertEquals(10.0d, removed);
        Assertions.assertEquals(0, map.size());
        Assertions.assertTrue(map.isEmpty());
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeFromCompactArray_decrementsSizeAndKeepsIntegrity() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // keep us in the array zone
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
        // Fill the map with three entries – this will be stored as an Object[]
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        Assertions.assertEquals(3, map.size());
        // Remove middle entry
        Integer removed = map.remove("b");
        Assertions.assertEquals(2, removed);
        Assertions.assertEquals(2, map.size());
        Assertions.assertNull(map.get("b"));
        Assertions.assertEquals(1, map.get("a"));
        Assertions.assertEquals(3, map.get("c"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeFromBackingMap_behavesLikeStandardMap() {
        // Use the builder to obtain a concrete generated subclass.
        // Force a transition to a backing map after 3 entries.
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(3).mapType(HashMap.class).build();
        // Populate beyond the compact size
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        // now internal storage is a Map
        map.put("k4", "v4");
        Assertions.assertTrue(map.size() > 3);
        Assertions.assertEquals("v2", map.remove("k2"));
        Assertions.assertEquals(3, map.size());
        Assertions.assertFalse(map.containsKey("k2"));
        Assertions.assertNull(map.remove("non‑existent"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeNonExistentKey_alwaysReturnsNull() {
        // Empty map
        CompactMap<Integer, String> empty = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<Integer, String> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected Integer getSingleValueKey() {
                // any Integer works; using 0 as a placeholder
                return 0;
            }
        };
        Assertions.assertNull(empty.remove(42));
        // Single entry map
        CompactMap<Integer, String> single = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<Integer, String> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected Integer getSingleValueKey() {
                return 0;
            }
        };
        single.put(1, "one");
        Assertions.assertNull(single.remove(2));
        // Compact array (2 entries)
        CompactMap<Integer, String> array = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<Integer, String> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected Integer getSingleValueKey() {
                return 0;
            }
        };
        array.put(10, "ten");
        array.put(20, "twenty");
        Assertions.assertNull(array.remove(30));
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: remove(''{0}'' ) from compact array")
    @MethodSource("compactArrayRemovalProvider")
    void removeFromCompactArray_variousKeys(String key, String expected) {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                // ensure array storage
                return 4;
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
        // Populate with two known entries
        map.put("alpha", "A");
        map.put("beta", "B");
        Assertions.assertEquals(2, map.size());
        // Perform removal
        String result = map.remove(key);
        if (expected == null) {
            Assertions.assertNull(result, "Removing non‑existent key must return null");
            Assertions.assertEquals(2, map.size(), "Size must stay unchanged when key missing");
        } else {
            Assertions.assertEquals(expected, result);
            Assertions.assertEquals(1, map.size(), "Size must decrement after successful removal");
        }
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: remove(''{0}'' ) from backing map")
    @MethodSource("backingMapRemovalProvider")
    void removeFromBackingMap_variousKeys(String key, String expected) {
        // small compact size to force map storage quickly
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(2).mapType(LinkedHashMap.class).build();
        // Insert three entries → internal storage is a Map
        map.put("key24", "value24");
        map.put("key25", "value25");
        map.put("key26", "value26");
        Assertions.assertTrue(map.size() > 2);
        String removed = map.remove(key);
        if (expected == null) {
            Assertions.assertNull(removed, "Non‑existent key must return null");
            Assertions.assertTrue(map.containsKey("key25"), "Map should stay unchanged");
        } else {
            Assertions.assertEquals(expected, removed);
            Assertions.assertFalse(map.containsKey(key), "Key must be removed from the map");
        }
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeNullKey_fromEmptyMap_returnsNull() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        Assertions.assertNull(map.remove(null), "Removing null from an empty map should return null");
    }

    @Test
    void removeNullKey_fromMapContainingNull_returnsValue() throws Exception {
        // force map storage after first put
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(1).mapType(HashMap.class).build();
        // Populate with a null key
        map.put(null, "nullValue");
        map.put("other", "otherValue");
        Assertions.assertEquals(2, map.size());
        String removed = map.remove(null);
        Assertions.assertEquals("nullValue", removed);
        Assertions.assertEquals(1, map.size());
        Assertions.assertFalse(map.containsKey(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeFromUnmodifiableMap_throwsUnsupportedOperationException() throws Exception {
        // Build a normal CompactMap with a few entries so that it uses a backing map.
        CompactMap<String, String> map = CompactMap.<String, String>builder().compactSize(1).mapType(HashMap.class).build();
        map.put("someKey", "someValue");
        map.put("another", "anotherValue");
        // Replace the internal 'val' with an unmodifiable view via reflection.
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, String> underlying = (Map<String, String>) valField.get(map);
        Map<String, String> unmodifiable = Collections.unmodifiableMap(underlying);
        valField.set(map, unmodifiable);
        // The remove operation should now delegate to the unmodifiable map and fail.
        Assertions.assertThrows(UnsupportedOperationException.class, () -> map.remove("someKey"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void removeSingleEntry_nonMatchingKey_returnsNullAndLeavesEntry() {
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
                return "single";
            }
        };
        // Insert the single entry using the special single‑value key.
        map.put("single", "onlyValue");
        Assertions.assertEquals(1, map.size());
        // Attempt to remove a different key.
        Assertions.assertNull(map.remove("other"));
        Assertions.assertEquals(1, map.size(), "Size must stay 1 when key does not match");
        Assertions.assertEquals("onlyValue", map.get("single"));
    }
}
