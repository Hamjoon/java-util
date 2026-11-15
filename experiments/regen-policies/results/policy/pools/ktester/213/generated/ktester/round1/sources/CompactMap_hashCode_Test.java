package com.cedarsoftware.util;

import java.util.*;
import java.lang.reflect.Field;
// import org.junit.*;
import org.junit.jupiter.api.Test;
// import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.*;

class CompactMap_hashCode_Test {

    /**
     * Custom map that returns a constant hash code.
     */
    private static class ConstantHashMap<K, V> extends HashMap<K, V> {

        private final int constant;

        ConstantHashMap(int constant) {
            this.constant = constant;
        }

        @Override
        public int hashCode() {
            return constant;
        }
    }

    /**
     * Custom map whose {@code hashCode()} throws an exception.
     */
    private static class ExplodingMap<K, V> extends HashMap<K, V> {

        @Override
        public int hashCode() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * Creates a CompactMap that will stay in the “compact array” state while its size
     * is ≤ {@code maxArraySize}.  The map uses a LinkedHashMap as the backing map
     * when it finally grows beyond that size.
     */
    private static <K, V> CompactMap<K, V> newCompactArrayMap(int maxArraySize) {
        return new CompactMap<K, V>() {

            @Override
            protected int compactSize() {
                return maxArraySize;
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
     * Creates a CompactMap that will switch to a backing Map as soon as the size
     * exceeds {@code threshold}.
     */
    private static <K, V> CompactMap<K, V> newBackingMap(int threshold) {
        return new CompactMap<K, V>() {

            @Override
            protected int compactSize() {
                return threshold;
            }

            @Override
            protected Map<K, V> getNewMap() {
                return new HashMap<>();
            }

            @Override
            @SuppressWarnings("unchecked")
            protected K getSingleValueKey() {
                return (K) "id";
            }
        };
    }

    /**
     * Helper to set the private field {@code val} via reflection.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, newVal);
    }

    @Test
    void testHashCodeEmptyMap() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        assertTrue(map.isEmpty());
        assertEquals(0, map.hashCode());
    }

    @Test
    void testHashCodeSingleEntry() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
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
        map.put("key", "value");
        int expected = Objects.hashCode("key") ^ Objects.hashCode("value");
        assertEquals(expected, map.hashCode());
    }

    @Test
    void testHashCodeCompactArray() {
        CompactMap<String, Integer> map = newCompactArrayMap(5);
        map.put("a", 1);
        map.put("b", 2);
        int expected = (Objects.hashCode("a") ^ Objects.hashCode(1)) + (Objects.hashCode("b") ^ Objects.hashCode(2));
        assertEquals(expected, map.hashCode());
    }

    @Test
    void testHashCodeBackingMap() {
        CompactMap<String, Integer> map = newBackingMap(2);
        map.put("x", 10);
        map.put("y", 20);
        // triggers transition
        map.put("z", 30);
        Map<String, Integer> reference = new HashMap<>();
        reference.put("x", 10);
        reference.put("y", 20);
        reference.put("z", 30);
        assertEquals(reference.hashCode(), map.hashCode());
    }

    @Test
    void testHashCodeArrayRepresentationMultipleEntriesWithNullValue() throws Exception {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        Object[] internal = new Object[] { "a", 1, "b", null };
        setInternalVal(map, internal);
        int expected = (Objects.hashCode("a") ^ Objects.hashCode(1)) + (Objects.hashCode("b") ^ 0);
        assertEquals(expected, map.hashCode());
    }

    @Test
    void testHashCodeArrayRepresentationNullKey() throws Exception {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        Object[] internal = new Object[] { null, "x", "y", 2 };
        setInternalVal(map, internal);
        int expected = (0 ^ Objects.hashCode("x")) + (Objects.hashCode("y") ^ Objects.hashCode(2));
        assertEquals(expected, map.hashCode());
    }

    @Test
    void testHashCodeMapRepresentationUnderlyingHash() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        Map<String, Integer> backing = new HashMap<>();
        backing.put("k1", 10);
        backing.put("k2", 20);
        setInternalVal(map, backing);
        assertEquals(backing.hashCode(), map.hashCode());
    }

    @Test
    void testHashCodeMapRepresentationEmptyMap() throws Exception {
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
        Map<String, Integer> empty = new LinkedHashMap<>();
        setInternalVal(map, empty);
        assertEquals(0, map.hashCode());
    }

    @Test
    void testHashCodeEmptySentinelAfterClear() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        map.put("a", 1);
        map.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, map.hashCode());
    }

    @Test
    void testHashCodeSingleEntryNullKeyAndValue() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
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
        map.put(null, null);
        assertEquals(0, map.hashCode());
    }

    @Test
    void testHashCodeSingleEntryNullKeyNonNullValue() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
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
        map.put(null, "value");
        int expected = 0 ^ Objects.hashCode("value");
        assertEquals(expected, map.hashCode());
    }

    @Test
    void testHashCodeArrayRepresentationThreeEntries() throws Exception {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        Object[] internal = new Object[] { "a", 1, "b", 2, "c", 3 };
        setInternalVal(map, internal);
        int expected = (Objects.hashCode("a") ^ Objects.hashCode(1)) + (Objects.hashCode("b") ^ Objects.hashCode(2)) + (Objects.hashCode("c") ^ Objects.hashCode(3));
        assertEquals(expected, map.hashCode());
    }

    @Test
    void testHashCodeArrayRepresentationDuplicateKeys() throws Exception {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        Object[] internal = new Object[] { "dup", 10, "dup", 20 };
        setInternalVal(map, internal);
        int expected = (Objects.hashCode("dup") ^ Objects.hashCode(10)) + (Objects.hashCode("dup") ^ Objects.hashCode(20));
        assertEquals(expected, map.hashCode());
    }

    @Test
    void testHashCodeCustomMapConstantHash() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        ConstantHashMap<String, Integer> custom = new ConstantHashMap<>(12345);
        custom.put("any", 1);
        setInternalVal(map, custom);
        assertEquals(12345, map.hashCode());
    }

    @Test
    void testHashCodeTransitionArrayToMap() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 50;
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
        for (int i = 0; i < 51; i++) {
            map.put("k" + i, i);
        }
        Map<String, Integer> reference = new LinkedHashMap<>();
        for (int i = 0; i < 51; i++) {
            reference.put("k" + i, i);
        }
        assertEquals(reference.hashCode(), map.hashCode());
    }

    @Test
    void testHashCodeIdempotence() {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        map.put("x", 5);
        map.put("y", null);
        int first = map.hashCode();
        int second = map.hashCode();
        assertEquals(first, second);
    }

    @Test
    void testHashCodeMalformedArrayOddLength() throws Exception {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        Object[] malformed = new Object[] { "k1", 1, "k2" };
        setInternalVal(map, malformed);
        // The method should handle the malformed internal representation gracefully.
        // If it throws, the test will fail, which is the intended verification.
        map.hashCode();
    }

    @Test
    void testHashCodeUnderlyingMapThrowsException() throws Exception {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "id";
            }
        };
        setInternalVal(map, new ExplodingMap<>());
        // The hashCode implementation should catch the exception and not propagate it.
        map.hashCode();
    }

    @Test
    void testHashCodeInternalValNull() throws Exception {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        setInternalVal(map, null);
        map.hashCode();
    }

    @Test
    void testHashCodeInternalValNonMapNonArray() throws Exception {
        CompactMap<String, Integer> map = newCompactArrayMap(10);
        setInternalVal(map, "just a string");
        map.hashCode();
    }
}
