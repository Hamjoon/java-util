package com.cedarsoftware.util;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
// import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_put_Test {

    /* ---------- Helper class for parameterized tests ---------- */
    private static final class Arguments {

        final Object a, b, c;

        private Arguments(Object a, Object b, Object c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        static Arguments of(Object a, Object b, Object c) {
            return new Arguments(a, b, c);
        }
    }

    private static Stream<Arguments> compactArrayProvider() {
        return Stream.of(Arguments.of("alpha", "first", null), Arguments.of("beta", "second", null), Arguments.of("alpha", "updated", "first"));
    }

    private static Stream<Arguments> backingMapProvider() {
        return Stream.of(Arguments.of("x", 123, null), Arguments.of("x", 456, 123));
    }

    /**
     * Retrieves the private {@code val} field via reflection. Used only for
     * debugging purposes; test assertions rely on the public API.
     */
    @SuppressWarnings("unchecked")
    private static Object getInternalVal(CompactMap<?, ?> map) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        return f.get(map);
    }

    @Test
    void putIntoEmptyMapOptimizedSingleValue() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("id", 42));
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(42), map.get("id"));
    }

    @Test
    void putIntoEmptyMapCreatesEntry() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("other", "value"));
        assertEquals(1, map.size());
        assertEquals("value", map.get("other"));
    }

    @Test
    void putOverwriteSingleEntryReturnsOldValue() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("id", "first");
        assertEquals("first", map.get("id"));
        assertEquals("first", map.put("id", "second"));
        assertEquals(1, map.size());
        assertEquals("second", map.get("id"));
    }

    @Test
    void transitionToCompactArray() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("a", 1));
        assertNull(map.put("b", 2));
        assertNull(map.put("c", 3));
        assertEquals(3, map.size());
        assertEquals(Integer.valueOf(1), map.get("a"));
        assertEquals(Integer.valueOf(2), map.get("b"));
        assertEquals(Integer.valueOf(3), map.get("c"));
        assertEquals(Integer.valueOf(2), map.put("b", 20));
        assertEquals(Integer.valueOf(20), map.get("b"));
    }

    @Test
    void transitionToBackingMap() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("k1", "v1"));
        assertNull(map.put("k2", "v2"));
        assertEquals(2, map.size());
        assertNull(map.put("k3", "v3"));
        assertEquals(3, map.size());
        assertEquals("v1", map.get("k1"));
        assertEquals("v2", map.get("k2"));
        assertEquals("v3", map.get("k3"));
    }

    @ParameterizedTest(name = "[{index}] put({0}, {1}) expects {2}")
    @MethodSource("compactArrayProvider")
    void compactArrayStorage(String key, String value, String expected) {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // prime the map: two entries -> compact array state
        map.put("alpha", "first");
        map.put("beta", "second");
        String returned = map.put(key, value);
        assertEquals(expected, returned);
        assertEquals(value, map.get(key));
        // size stays 2 unless we overwrote an existing key (still 2)
        assertEquals(2, map.size());
    }

    @ParameterizedTest(name = "[{index}] put({0}, {1}) expects {2}")
    @MethodSource("backingMapProvider")
    void backingMapStorage(String key, Integer value, Integer expected) {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 1;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        Integer returned = map.put(key, value);
        assertEquals(expected, returned);
        assertEquals(value, map.get(key));
    }

    @Test
    void emptyMapFirstEntryCreatesCompactMapEntry() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("onlyKey", "onlyValue"));
        assertEquals(1, map.size());
        assertEquals("onlyValue", map.get("onlyKey"));
    }

    @Test
    void emptyMapOptimizedSingleValueFirstInsertion() throws Exception {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "singleKey";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("singleKey", "singleValue"));
        assertEquals(1, map.size());
        assertEquals("singleValue", map.get("singleKey"));
        assertEquals("singleValue", map.put("singleKey", "newSingleValue"));
        assertEquals("newSingleValue", map.get("singleKey"));
    }

    @Test
    void singleEntryOverwriteOrTransition_NewKeyTriggersArray() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("firstKey", "firstVal");
        assertNull(map.put("secondKey", "secondVal"));
        assertEquals(2, map.size());
        assertEquals("firstVal", map.get("firstKey"));
        assertEquals("secondVal", map.get("secondKey"));
    }

    @Test
    void singleEntryOverwriteOrTransition_SameKeyOverwrite() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        map.put("firstKey", "firstVal");
        assertEquals("firstVal", map.put("firstKey", "updatedVal"));
        assertEquals(1, map.size());
        assertEquals("updatedVal", map.get("firstKey"));
    }

    @Test
    void emptyMapValueIsMapCreatesEntry() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        Map<String, String> inner = new HashMap<>();
        inner.put("a", "b");
        assertNull(map.put("kMap", inner));
        assertEquals(1, map.size());
        assertSame(inner, map.get("kMap"));
    }

    @Test
    void emptyMapValueIsArrayCreatesEntry() {
        CompactMap<String, Object> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, Object> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        Object[] arr = new Object[] { "a", "b" };
        assertNull(map.put("kArray", arr));
        assertEquals(1, map.size());
        assertSame(arr, map.get("kArray"));
    }

    @Test
    void putNullKeyCreatesEntry() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put(null, "someValue"));
        assertEquals(1, map.size());
        assertEquals("someValue", map.get(null));
    }

    @Test
    void putNullValueCreatesEntry() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("someKey", null));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("someKey"));
        assertNull(map.get("someKey"));
    }

    @Test
    void compactArrayToBackingMapTransition() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("a", "v1"));
        assertNull(map.put("b", "v2"));
        assertNull(map.put("c", "v3"));
        assertEquals(3, map.size());
        assertEquals("v1", map.get("a"));
        assertEquals("v2", map.get("b"));
        assertEquals("v3", map.get("c"));
        assertEquals("v2", map.put("b", "v2Updated"));
        assertEquals("v2Updated", map.get("b"));
    }

    @Test
    void overwriteInBackingMapAfterTransition() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("x", 1));
        assertNull(map.put("y", 2));
        assertNull(map.put("z", 3));
        assertEquals(Integer.valueOf(1), map.put("x", 10));
        assertEquals(Integer.valueOf(10), map.get("x"));
        assertEquals(3, map.size());
    }

    @Test
    void putNullKeyInSingleEntryStateTransitionsToArray() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "id";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("onlyKey", "onlyValue"));
        assertEquals(1, map.size());
        assertNull(map.put(null, "nullKeyValue"));
        assertEquals(2, map.size());
        assertEquals("onlyValue", map.get("onlyKey"));
        assertEquals("nullKeyValue", map.get(null));
    }

    @Test
    void putNullValueInOptimizedSingleValue() {
        CompactMap<String, String> map = new CompactMap<>() {

            @Override
            protected String getSingleValueKey() {
                return "singleKey";
            }

            @Override
            protected int compactSize() {
                return 4;
            }

            @Override
            protected Map<String, String> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertNull(map.put("singleKey", "singleValue"));
        assertEquals("singleValue", map.get("singleKey"));
        assertEquals("singleValue", map.put("singleKey", null));
        assertNull(map.get("singleKey"));
    }
}
