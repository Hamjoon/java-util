package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_containsKey_Test {

    /**
     * A key whose {@code equals} always throws a {@link RuntimeException}.
     */
    private static final class BadEqualsKey {

        @Override
        public boolean equals(Object obj) {
            throw new RuntimeException("equals failure");
        }

        @Override
        public int hashCode() {
            return 42;
        }
    }

    /**
     * A key whose {@code hashCode} throws {@link IllegalStateException}.
     * The implementation of {@code areKeysEqual} may call {@code hashCode}
     * as part of its comparison, so we test that the exception propagates.
     */
    private static final class BadHashCodeKey {

        @Override
        public boolean equals(Object obj) {
            return obj != null && obj.equals(this);
        }

        @Override
        public int hashCode() {
            throw new IllegalStateException("hashCode failure");
        }
    }

    /**
     * Helper that uses reflection to replace the private {@code val} field.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) {
        try {
            Field f = CompactMap.class.getDeclaredField("val");
            f.setAccessible(true);
            f.set(map, newVal);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ---------- Empty map ----------
    private static Stream<Arguments> emptyMapProvider() {
        return Stream.of(Arguments.of(null, false, "null key on empty map"), Arguments.of("anyKey", false, "non‑null key on empty map"));
    }

    // ---------- Single entry ----------
    private static Stream<Arguments> singleEntryProvider() {
        return Stream.of(Arguments.of("alpha", true, "matching key"), Arguments.of("beta", false, "non‑matching key"), Arguments.of(null, true, "null key when stored key is null"), Arguments.of(null, false, "null query when stored key is non‑null"));
    }

    // ---------- Compact array ----------
    private static Stream<Arguments> compactArrayProvider() {
        return Stream.of(Arguments.of("k1", true, "first key in array"), Arguments.of("k3", true, "later key in array"), Arguments.of("missing", false, "absent key"), Arguments.of(null, true, "null key present in array"), Arguments.of(null, false, "null key absent in array"));
    }

    // ---------- Backing map ----------
    private static Stream<Arguments> backingMapProvider() {
        return Stream.of(Arguments.of("first", true, "existing key 1"), Arguments.of("third", true, "existing key 2"), Arguments.of("absent", false, "non‑existent key"), Arguments.of(null, true, "null key present in map"), Arguments.of(null, false, "null key absent in map"));
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void}
     * return type.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method returns {@code void},
     *         {@code false} otherwise
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_EmptyMap_ReturnsFalse() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertTrue(map.isEmpty(), "Map must be empty");
        assertFalse(map.containsKey("any"), "Empty map must not contain any key");
        assertFalse(map.containsKey(null), "Empty map must not contain null key");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_SingleEntry_ReturnsTrueOrFalse() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // first entry – triggers the single‑value optimisation
        map.put("alpha", 1);
        assertTrue(map.containsKey("alpha"));
        assertFalse(map.containsKey("beta"));
        assertFalse(map.containsKey(null));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_CompactArray_ReturnsCorrectResult() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Fill the map with three entries – still below compactSize → array storage
        map.put("k1", 10);
        map.put("k2", 20);
        map.put("k3", 30);
        assertEquals(3, map.size());
        assertTrue(map.containsKey("k1"));
        assertTrue(map.containsKey("k2"));
        assertTrue(map.containsKey("k3"));
        assertFalse(map.containsKey("missing"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_BackingMap_ReturnsCorrectResult() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Add four entries – the fourth insertion should cause the transition
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // > compactSize → backing map
        map.put("d", 4);
        assertEquals(4, map.size());
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertTrue(map.containsKey("d"));
        assertFalse(map.containsKey("e"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_NullKey_BehavesAccordingToUnderlyingMap() {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // put a null key – LinkedHashMap (the backing map) permits null keys
        map.put(null, 99);
        map.put("x", 1);
        // still compact array (size == 3)
        map.put("y", 2);
        assertTrue(map.containsKey(null));
        assertTrue(map.containsKey("x"));
        assertTrue(map.containsKey("y"));
        assertFalse(map.containsKey("z"));
    }

    /* --------------------------------------------------------------------- */
    @Test
    void containsKey_CaseInsensitive_ReturnsTrueWhenCaseDiffers() {
        // Builder creates a map with case‑insensitive string keys
        CompactMap<String, Integer> map = CompactMap.<String, Integer>builder().caseSensitive(false).compactSize(10).build();
        map.put("key", 123);
        assertTrue(map.containsKey("KEY"), "Case‑insensitive map should match different case");
        assertFalse(map.containsKey("different"), "Non‑matching key must return false");
    }

    @Test
    void containsKey_UnderlyingMapThrowsNullPointerException_Propagates() {
        // Mock a Map that throws NPE when containsKey(null) is invoked
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.containsKey(null)).thenThrow(new NullPointerException("null not allowed"));
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Force the map into the "backing map" state
        map.put("a", 1);
        map.put("b", 2);
        // Replace internal storage with the mock that throws
        setInternalVal(map, mockMap);
        assertThrows(NullPointerException.class, () -> map.containsKey(null), "NullPointerException from underlying map must propagate");
    }

    @Test
    void containsKey_UnderlyingMapThrowsClassCastException_Propagates() {
        @SuppressWarnings("unchecked")
        Map<String, Integer> mockMap = mock(Map.class);
        when(mockMap.containsKey("incompatibleKey")).thenThrow(new ClassCastException("bad type"));
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 2;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Populate enough entries to be in map mode
        map.put("x", 1);
        map.put("y", 2);
        setInternalVal(map, mockMap);
        assertThrows(ClassCastException.class, () -> map.containsKey("incompatibleKey"), "ClassCastException from underlying map must propagate");
    }

    @Test
    void containsKey_ArrayKeyEqualsThrowsRuntimeException_Propagates() {
        CompactMap<Object, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<Object, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Manually inject an Object[] with a BadEqualsKey as the first key
        Object[] entries = new Object[] { new BadEqualsKey(), 999, "goodKey", 1 };
        setInternalVal(map, entries);
        // The call will iterate over the array and invoke equals on BadEqualsKey,
        // which throws RuntimeException
        assertThrows(RuntimeException.class, () -> map.containsKey("any"), "RuntimeException from key.equals must propagate");
    }

    @Test
    void containsKey_ArrayKeyHashCodeThrowsIllegalStateException_Propagates() {
        CompactMap<Object, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<Object, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        Object[] entries = new Object[] { new BadHashCodeKey(), 111, "another", 2 };
        setInternalVal(map, entries);
        // Trigger containsKey – if areKeysEqual uses hashCode, the exception will surface
        assertThrows(IllegalStateException.class, () -> map.containsKey("trigger"), "IllegalStateException from key.hashCode must propagate");
    }

    @ParameterizedTest(name = "Empty map – key={2}")
    @MethodSource("emptyMapProvider")
    void containsKey_EmptyMap_Parameterized(Object key, boolean expected, String _desc) {
        CompactMap<String, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        assertEquals(expected, map.containsKey(key));
    }

    @ParameterizedTest(name = "Single entry – key={2}")
    @MethodSource("singleEntryProvider")
    void containsKey_SingleEntry_Parameterized(Object key, boolean expected, String _desc) {
        CompactMap<Object, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<Object, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Populate with a single entry; for the "null key stored" case we insert null
        if (Objects.equals(key, null) && expected) {
            map.put(null, 42);
        } else if (!Objects.equals(key, null) && expected) {
            map.put(key, 1);
        } else {
            // store a non‑null key "alpha" for the negative cases
            map.put("alpha", 1);
        }
        assertEquals(expected, map.containsKey(key));
    }

    @ParameterizedTest(name = "Compact array – key={2}")
    @MethodSource("compactArrayProvider")
    void containsKey_CompactArray_Parameterized(Object key, boolean expected, String _desc) {
        CompactMap<Object, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 5;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<Object, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Build a known array state: k1, k2, k3 with optional null key
        map.put("k1", 10);
        map.put("k2", 20);
        map.put("k3", 30);
        if (Objects.equals(key, null) && expected) {
            map.put(null, 99);
        }
        assertEquals(expected, map.containsKey(key));
    }

    @ParameterizedTest(name = "Backing map – key={2}")
    @MethodSource("backingMapProvider")
    void containsKey_BackingMap_Parameterized(Object key, boolean expected, String _desc) {
        CompactMap<Object, Integer> map = new CompactMap<>() {

            @Override
            protected int compactSize() {
                return 3;
            }

            @Override
            protected String getSingleValueKey() {
                return "value";
            }

            @Override
            protected Map<Object, Integer> getNewMap() {
                return new LinkedHashMap<>();
            }
        };
        // Populate > compactSize to force map mode
        map.put("first", 1);
        map.put("second", 2);
        map.put("third", 3);
        map.put("fourth", 4);
        if (Objects.equals(key, null) && expected) {
            map.put(null, 0);
        }
        assertEquals(expected, map.containsKey(key));
    }
}
