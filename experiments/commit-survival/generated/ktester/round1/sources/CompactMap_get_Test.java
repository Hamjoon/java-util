package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_get_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
    }

    /**
     * Empty map must return {@code null} for any key.
     */
    @Test
    void get_EmptyMap_ReturnsNull() {
        assertNull(map.get("anyKey"));
    }

    /**
     * Single entry stored via {@code put} should be retrievable.
     */
    @Test
    void get_SingleEntry_ReturnsValue() {
        map.put("one", 1);
        assertEquals(1, map.get("one"));
    }

    /**
     * Requesting a non‑existent key on a map with one entry returns {@code null}.
     */
    @Test
    void get_SingleEntry_KeyNotPresent_ReturnsNull() {
        map.put("one", 1);
        assertNull(map.get("missing"));
    }

    /**
     * Two entries (still below {@code compactSize}) are stored in an Object[].
     */
    @Test
    void get_TwoEntries_ArrayStorage_ReturnsCorrectValues() {
        map.put("first", 10);
        map.put("second", 20);
        assertEquals(10, map.get("first"));
        assertEquals(20, map.get("second"));
        assertNull(map.get("unknown"));
    }

    /**
     * When the number of entries exceeds {@code compactSize} the map delegates to a
     * regular {@link java.util.Map}.  We create a small subclass that forces the
     * threshold to 2 so that after inserting three entries the delegate map is used.
     */
    @Test
    void get_DelegatedMapStorage_ReturnsCorrectValues() {
        CompactMap<String, Integer> tinyMap = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }
        };
        // three entries forces a switch to the delegated map
        tinyMap.put("a", 100);
        tinyMap.put("b", 200);
        tinyMap.put("c", 300);
        assertEquals(100, tinyMap.get("a"));
        assertEquals(200, tinyMap.get("b"));
        assertEquals(300, tinyMap.get("c"));
        assertNull(tinyMap.get("missing"));
    }

    /**
     * Null key handling – CompactMap should behave like a normal map.
     */
    @Test
    void get_NullKey_ReturnsValueOrNull() {
        map.put(null, 42);
        assertEquals(42, map.get(null));
        map.clear();
        assertNull(map.get(null));
    }

    /**
     * Verify that {@code get} does not modify the map (no side‑effects).
     */
    @Test
    void get_DoesNotAlterMapState() {
        map.put("x", 1);
        map.put("y", 2);
        int sizeBefore = map.size();
        // call get several times
        assertEquals(1, map.get("x"));
        assertEquals(2, map.get("y"));
        assertNull(map.get("z"));
        assertEquals(sizeBefore, map.size(), "size must stay unchanged after get()");
    }

    /**
     * Empty map – null key must also return null.
     */
    @Test
    void get_EmptyMap_NullKey_ReturnsNull() {
        assertNull(map.get(null));
    }

    /**
     * Single entry – retrieve stored value.
     */
    @Test
    void get_SingleEntry_ReturnsStoredValue() {
        map.put("k1", "v1");
        assertEquals("v1", map.get("k1"));
    }

    /**
     * Single entry – non‑existent key returns null.
     */
    @Test
    void get_SingleEntry_NonExistingKey_ReturnsNull() {
        map.put("k1", "v1");
        assertNull(map.get("missing"));
    }

    /**
     * Single entry – lookup with null when stored key is non‑null returns null.
     */
    @Test
    void get_SingleEntry_NullLookupWhenKeyNonNull_ReturnsNull() {
        map.put("k1", "v1");
        assertNull(map.get(null));
    }

    /**
     * Compact array mode – three entries, verify lookups.
     */
    @Test
    void get_CompactArrayMode_ThreeEntries_Lookups() {
        // Force compactSize to a value >3 (default is 80) – no need to subclass.
        map.put("k1", "v1");
        map.put("k2", "v2");
        map.put("k3", "v3");
        assertEquals("v2", map.get("k2"));
        assertEquals("v3", map.get("k3"));
        assertNull(map.get("unknown"));
    }

    /**
     * Compact array mode – null key with non‑null value.
     */
    @Test
    void get_CompactArrayMode_NullKey_ReturnsValue() {
        map.put(null, "nullValue");
        // ensure we stay in array mode (size 2)
        map.put("k1", "v1");
        assertEquals("nullValue", map.get(null));
    }

    /**
     * Map mode – insert nine entries (default compactSize is 80, so we need a smaller threshold).
     */
    @Test
    void get_MapMode_NineEntries_Lookups() {
        CompactMap<String, String> bigMap = new CompactMap<String, String>() {

            // trigger map mode after 3 entries
            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new HashMap<>();
            }
        };
        // nine distinct entries
        for (int i = 1; i <= 9; i++) {
            bigMap.put("k" + i, "v" + i);
        }
        assertEquals("v9", bigMap.get("k9"));
        // put returns previous (null)
        assertEquals("v12", bigMap.put("k12", "v12"));
        assertEquals("v12", bigMap.get("k12"));
        assertNull(bigMap.get("missing"));
    }

    /**
     * Map mode – null key handling.
     */
    @Test
    void get_MapMode_NullKey_ReturnsValue() {
        CompactMap<String, String> mapMode = new CompactMap<String, String>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new HashMap<>();
            }
        };
        // force transition to map mode
        mapMode.put("a", "1");
        mapMode.put("b", "2");
        // now in delegate map
        mapMode.put("c", "3");
        mapMode.put(null, "nullMapValue");
        assertEquals("nullMapValue", mapMode.get(null));
    }

    /**
     * State transition – ensure earlier entries survive all transitions.
     */
    @Test
    void get_StateTransitions_PreserveEarlierEntry() {
        CompactMap<String, String> transMap = new CompactMap<String, String>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new HashMap<>();
            }
        };
        // single entry
        transMap.put("k1", "v1");
        assertEquals("v1", transMap.get("k1"));
        // add to reach array mode
        transMap.put("k2", "v2");
        transMap.put("k3", "v3");
        assertEquals("v1", transMap.get("k1"));
        // add more to force map mode
        transMap.put("k4", "v4");
        transMap.put("k5", "v5");
        assertEquals("v1", transMap.get("k1"));
        // clear and verify empty branch
        transMap.clear();
        assertNull(transMap.get("k1"));
    }

    /**
     * Single entry with null key – retrieval works.
     */
    @Test
    void get_SingleEntry_NullKeyValue_ReturnsStoredValue() {
        map.put(null, "nullKeyValue");
        assertEquals("nullKeyValue", map.get(null));
        assertNull(map.get("someKey"));
    }

    /**
     * Single entry with null value – get returns null (value is null).
     */
    @Test
    void get_SingleEntry_NullValue_ReturnsNull() {
        map.put("kNullVal", null);
        assertNull(map.get("kNullVal"));
    }

    /**
     * Duplicate key handling in compact array mode – overwrite works.
     */
    @Test
    void get_CompactArrayMode_DuplicateKey_Overwrite() {
        map.put("k1", "v1");
        map.put("dupKey", "oldValue");
        map.put("k3", "v3");
        // overwrite dupKey
        map.put("dupKey", "newValue");
        assertEquals("newValue", map.get("dupKey"));
        assertEquals("v1", map.get("k1"));
    }

    /**
     * Duplicate key handling in map mode – overwrite works.
     */
    @Test
    void get_MapMode_DuplicateKey_Overwrite() {
        CompactMap<String, String> dupMap = new CompactMap<String, String>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new HashMap<>();
            }
        };
        // fill to map mode
        dupMap.put("k1", "v1");
        dupMap.put("dupKey", "first");
        // now map mode
        dupMap.put("k3", "v3");
        dupMap.put("dupKey", "finalValue");
        assertEquals("finalValue", dupMap.get("dupKey"));
    }

    /**
     * Key type mismatch – stored as String, lookup with Integer returns null.
     */
    @Test
    void get_KeyTypeMismatch_ReturnsNull() {
        map.put("123", "stringVal");
        // Integer key should not match
        assertNull(map.get(123));
    }

    /**
     * Removal – after remove the key is no longer found.
     */
    @Test
    void get_Removal_KeyNoLongerPresent() {
        map.put("toRemove", "value");
        map.put("other", "keep");
        assertEquals("value", map.get("toRemove"));
        map.remove("toRemove");
        assertNull(map.get("toRemove"));
        assertEquals("keep", map.get("other"));
    }

    /**
     * putAll merges correctly – existing and new entries are present.
     */
    @Test
    void get_PutAll_MergesEntries() {
        map.put("x", "ex");
        Map<String, String> other = new HashMap<>();
        other.put("a", "alpha");
        other.put("b", "beta");
        map.putAll(other);
        assertEquals("alpha", map.get("a"));
        assertEquals("beta", map.get("b"));
        assertEquals("ex", map.get("x"));
    }

    /**
     * Case‑insensitive lookup – map configured to ignore case.
     */
    @Test
    void get_CaseInsensitiveLookup_ReturnsValue() {
        CompactMap<String, String> ciMap = new CompactMap<String, String>() {

            @Override
            protected boolean isCaseInsensitive() {
                return true;
            }

            @Override
            protected Map<String, String> getNewMap() {
                // TreeMap with case‑insensitive comparator
                return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            }
        };
        ciMap.put("key", "value");
        assertEquals("value", ciMap.get("KEY"));
        assertEquals("value", ciMap.get("key"));
    }

    /**
     * Corrupted internal array (odd length) must throw ArrayIndexOutOfBoundsException.
     */
    @Test
    void get_CorruptedArray_OddLength_ThrowsArrayIndexOutOfBoundsException() throws Exception {
        // Force internal representation to an odd‑length Object[]
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        // length 3
        Object[] broken = new Object[] { "k1", "v1", "orphanKey" };
        f.set(map, broken);
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> map.get("anyKey"));
    }

    /**
     * Incompatible key type in Map mode (TreeMap) causes ClassCastException.
     */
    @Test
    void get_MapMode_IncompatibleKeyType_ThrowsClassCastException() throws Exception {
        CompactMap<String, String> badMap = new CompactMap<String, String>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                // TreeMap expects keys that are mutually comparable
                return new TreeMap<>();
            }
        };
        // Populate with Integer key via reflection to bypass compile‑time checks
        // ensure map mode later
        badMap.put("k1", "v1");
        badMap.put("k2", "v2");
        // now in delegate map
        badMap.put("k3", "v3");
        // Use reflection to replace internal map with a TreeMap<Integer,String>
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        TreeMap<Integer, String> tree = new TreeMap<>();
        tree.put(1, "one");
        f.set(badMap, tree);
        // The get will trigger TreeMap's compare, leading to ClassCastException
        assertThrows(ClassCastException.class, () -> badMap.get("stringKey"));
    }

    /**
     * Null internal state (val == null) must result in NullPointerException.
     */
    @Test
    void get_NullInternalState_ThrowsNullPointerException() throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        // break internal invariant
        f.set(map, null);
        assertThrows(NullPointerException.class, () -> map.get("anyKey"));
    }
}
