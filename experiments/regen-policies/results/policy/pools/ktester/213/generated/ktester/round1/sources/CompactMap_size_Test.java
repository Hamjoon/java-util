package com.cedarsoftware.util;

import java.lang.reflect.Field;
// import org.junit.*;
// import static org.junit.Assert.*;
// import java.util.concurrent.ConcurrentModificationException;
import java.util.concurrent.ConcurrentModificationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_size_Test {

    /**
     * Simple {@link Map} implementation that throws a supplied exception from {@code size()}.
     */
    private static class ThrowingMap implements Map<Object, Object> {

        private final RuntimeException toThrow;

        ThrowingMap(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public int size() {
            throw toThrow;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object k) {
            return false;
        }

        @Override
        public boolean containsValue(Object v) {
            return false;
        }

        @Override
        public Object get(Object k) {
            return null;
        }

        @Override
        public Object put(Object k, Object v) {
            return null;
        }

        @Override
        public Object remove(Object k) {
            return null;
        }

        @Override
        public void putAll(Map<?, ?> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public java.util.Set<Object> keySet() {
            return Collections.emptySet();
        }

        @Override
        public java.util.Collection<Object> values() {
            return Collections.emptyList();
        }

        @Override
        public java.util.Set<Entry<Object, Object>> entrySet() {
            return Collections.emptySet();
        }
    }

    /* --------------------------------------------------------------------- *
     * Helper to set the private {@code val} field via reflection.
     * --------------------------------------------------------------------- */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Verify that a freshly‑created map reports size 0 and {@code isEmpty()} true.
     */
    @Test
    void emptyMapSize() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertEquals(0, map.size());
        assertTrue(map.isEmpty());
    }

    /**
     * When the map contains exactly one entry it stores the value directly.
     * The size method must return 1.
     */
    @Test
    void singleEntrySize() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertEquals(0, map.size());
        map.put("value", 42);
        assertEquals(1, map.size());
        assertFalse(map.isEmpty());
    }

    /**
     * Add entries up to (and including) the configured {@code compactSize}.
     * The internal representation should be an Object[] (compact array) and
     * {@code size()} must equal the number of key/value pairs.
     */
    @Test
    void compactArraySize() {
        final int compactLimit = 4;
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return compactLimit;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        for (int i = 0; i < compactLimit; i++) {
            map.put("k" + i, i);
        }
        assertEquals(compactLimit, map.size());
        assertFalse(map.isEmpty());
        map.remove("k0");
        assertEquals(compactLimit - 1, map.size());
    }

    /**
     * Adding more entries than {@code compactSize} forces a transition to a backing
     * {@link java.util.Map}. The {@code size()} method must delegate to that map.
     */
    @Test
    void backingMapSize() {
        final int compactLimit = 3;
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return compactLimit;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        for (int i = 0; i < compactLimit + 2; i++) {
            map.put("k" + i, i);
        }
        int expected = compactLimit + 2;
        assertEquals(expected, map.size());
        map.remove("k0");
        assertEquals(expected - 1, map.size());
    }

    @ParameterizedTest(name = "Object[] length {0} → expected size {1}")
    @CsvSource({ // even length, 4>>1 = 2
    "4,2", // odd length, 3>>1 = 1
    "3,1", // empty array
    "0,0", // length 1 → 1>>1 = 0
    "1,0" })
    void sizeWithObjectArrayBranch(int arrayLength, int expectedSize) {
        CompactMap<String, Object> map = new CompactMap<>();
        Object[] arr = new Object[arrayLength];
        // Fill array with dummy values to avoid null‑array edge cases (not required for size())
        for (int i = 0; i < arrayLength; i++) {
            arr[i] = i;
        }
        setInternalVal(map, arr);
        assertEquals(expectedSize, map.size(), "size() should return length>>1 for Object[] storage");
    }

    @Test
    void sizeWithHashMapBranch() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Integer> backing = new HashMap<>();
        backing.put("a", 1);
        backing.put("b", 2);
        backing.put("c", 3);
        setInternalVal(map, backing);
        assertEquals(3, map.size());
    }

    @Test
    void sizeWithEmptyMapBranch() {
        CompactMap<String, Integer> map = new CompactMap<>();
        Map<String, Integer> empty = new HashMap<>();
        setInternalVal(map, empty);
        assertEquals(0, map.size());
    }

    @Test
    void sizeWithTreeMapBranch() {
        CompactMap<String, Integer> map = new CompactMap<>();
        TreeMap<String, Integer> tree = new TreeMap<>();
        for (int i = 0; i < 5; i++) tree.put("k" + i, i);
        setInternalVal(map, tree);
        assertEquals(5, map.size());
    }

    @Test
    void sizeWithEmptySentinelBranch() throws Exception {
        CompactMap<String, Object> map = new CompactMap<>();
        Field valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
        Field sentinelField = CompactMap.class.getDeclaredField("EMPTY_MAP");
        sentinelField.setAccessible(true);
        Object emptySentinel = sentinelField.get(null);
        valField.set(map, emptySentinel);
        assertEquals(0, map.size());
    }

    @Test
    void sizeWithSingleFallbackBranch() {
        CompactMap<String, Object> map = new CompactMap<>();
        setInternalVal(map, "single");
        assertEquals(1, map.size());
    }

    @Test
    void sizeWithNullFallbackBranch() {
        CompactMap<String, Object> map = new CompactMap<>();
        setInternalVal(map, null);
        assertEquals(1, map.size());
    }

    @Test
    void sizePropagatesIllegalStateException() {
        CompactMap<String, Object> map = new CompactMap<>();
        setInternalVal(map, new ThrowingMap(new IllegalStateException("boom")));
        IllegalStateException ex = assertThrows(IllegalStateException.class, map::size);
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void sizePropagatesConcurrentModificationException() {
        CompactMap<String, Object> map = new CompactMap<>();
        setInternalVal(map, new ThrowingMap(new ConcurrentModificationException("concurrent!")));
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, map::size);
        assertEquals("concurrent!", ex.getMessage());
    }

    @Test
    public void sizeWithObjectArrayLength4() {
        CompactMap<String, Object> map = new CompactMap<>();
        Object[] arr = new Object[4];
        for (int i = 0; i < arr.length; i++) arr[i] = i;
        setInternalVal(map, arr);
        assertEquals(2, map.size());
    }

    @Test
    public void sizeWithObjectArrayLength3() {
        CompactMap<String, Object> map = new CompactMap<>();
        Object[] arr = new Object[3];
        for (int i = 0; i < arr.length; i++) arr[i] = i;
        setInternalVal(map, arr);
        assertEquals(1, map.size());
    }

    @Test
    public void sizeWithObjectArrayLength0() {
        CompactMap<String, Object> map = new CompactMap<>();
        setInternalVal(map, new Object[0]);
        assertEquals(0, map.size());
    }

    @Test
    public void sizeWithObjectArrayLength1() {
        CompactMap<String, Object> map = new CompactMap<>();
        setInternalVal(map, new Object[1]);
        assertEquals(0, map.size());
    }
}
