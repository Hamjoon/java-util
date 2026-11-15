package com.cedarsoftware.util.convert;

// import java.util.concurrent.ConcurrentModificationException;
@ExtendWith(MockitoExtension.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MapConversions_toString_Test {

    private static Converter converter;

    @Mock
    Converter stubConverter;

    @Mock
    Converter throwingIllegalArgumentConverter;

    @Mock
    Converter throwingNullPointerConverter;

    @Mock
    Converter throwingIllegalStateConverter;

    /* --------------------------------------------------------------------- */
    class BadToString {

        @Override
        public String toString() {
            throw new RuntimeException("bad toString");
        }
    }

    class ThrowingMap implements Map<String, Object> {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return true;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            throw new UnsupportedOperationException("get not supported");
        }

        @Override
        public Object put(String key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<String> keySet() {
            return Set.of("value");
        }

        @Override
        public Collection<Object> values() {
            return List.of("x");
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return Set.of(new AbstractMap.SimpleEntry<>("value", "x"));
        }
    }

    class CmeMap implements Map<String, Object> {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean containsKey(Object key) {
            return true;
        }

        @Override
        public boolean containsValue(Object value) {
            return false;
        }

        @Override
        public Object get(Object key) {
            return "x";
        }

        @Override
        public Object put(String key, Object value) {
            return null;
        }

        @Override
        public Object remove(Object key) {
            return null;
        }

        @Override
        public void putAll(Map<? extends String, ?> m) {
        }

        @Override
        public void clear() {
        }

        @Override
        public Set<String> keySet() {
            return Set.of("value");
        }

        @Override
        public Collection<Object> values() {
            return List.of("x");
        }

        @Override
        public Set<Entry<String, Object>> entrySet() {
            return new AbstractSet<>() {

                @Override
                public Iterator<Entry<String, Object>> iterator() {
                    return new Iterator<>() {

                        boolean first = true;

                        @Override
                        public boolean hasNext() {
                            return true;
                        }

                        @Override
                        public Entry<String, Object> next() {
                            if (first) {
                                first = false;
                                throw new ConcurrentModificationException("iteration broken");
                            }
                            return null;
                        }
                    };
                }

                @Override
                public int size() {
                    return 1;
                }
            };
        }
    }

    @BeforeEach
    void setUpStubs() {
        when(stubConverter.convert(any(), eq(String.class))).thenAnswer(invocation -> {
            Object src = invocation.getArgument(0);
            return "converted:" + toJsonString(src);
        });
        when(throwingIllegalArgumentConverter.convert(any(), eq(String.class))).thenThrow(new IllegalArgumentException("forced"));
        when(throwingNullPointerConverter.convert(any(), eq(String.class))).thenThrow(new NullPointerException("forced"));
        when(throwingIllegalStateConverter.convert(any(), eq(String.class))).thenThrow(new IllegalStateException("forced"));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /* --------------------------------------------------------------------- */
    private static String toJsonString(Object obj) {
        if (obj instanceof Map) {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Object e : ((Map<?, ?>) obj).entrySet()) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) e;
                if (!first)
                    sb.append(",");
                sb.append('"').append(entry.getKey()).append("\":");
                Object v = entry.getValue();
                if (v instanceof String) {
                    sb.append('"').append(v).append('"');
                } else if (v instanceof Map) {
                    sb.append(toJsonString(v));
                } else if (v instanceof Collection) {
                    sb.append(v.toString());
                } else {
                    sb.append(String.valueOf(v));
                }
                first = false;
            }
            sb.append('}');
            return sb.toString();
        }
        return String.valueOf(obj);
    }

    /**
     * Simple helper to build a map from alternating key/value objects.
     */
    private static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            String key = String.valueOf(kv[i]);
            Object value = kv[i + 1];
            m.put(key, value);
        }
        return m;
    }

    private Stream<Arguments> additionalCases() {
        return Stream.of(Arguments.of(map(), stubConverter, "converted:{}", "Empty map – no VALUE or _v keys, fallback to converter"), Arguments.of(map("value", null, "other", "x"), stubConverter, "null", "Map contains the VALUE key but its associated value is null"), Arguments.of(map("value", null, "_v", 999), stubConverter, "null", "Both VALUE and _v keys present, VALUE null takes precedence"), Arguments.of(map("value", map("inner", 1)), stubConverter, "{inner=1}", "VALUE key maps to another Map object"), Arguments.of(map("value", 42, "extra", "ignore"), stubConverter, "42", "VALUE key holds a non‑String primitive wrapper"), Arguments.of(map("_v", true, "something", "else"), stubConverter, "true", "Only the alternate key _v is present, Boolean value"), Arguments.of(map("value", "", "another", "field"), stubConverter, "", "VALUE key exists with an empty string"), Arguments.of(new StringBuilder("StringBuilder content"), stubConverter, "converted:StringBuilder content", "Source object implements CharSequence but is not a String"), Arguments.of(new LinkedHashMap<String, Object>() {

            {
                put("class", "java.util.LinkedHashMap");
                put("value", "linked");
            }
        }, stubConverter, "linked", "Map is a LinkedHashMap subclass, VALUE key detected"), Arguments.of(map("alpha", "a", "beta", "b"), stubConverter, "converted:{\"alpha\":\"a\",\"beta\":\"b\"}", "Map without VALUE/_v keys, converter produces JSON‑style string"), Arguments.of(map("toString", "should not be used"), stubConverter, "converted:{\"toString\":\"should not be used\"}", "Map entry named \"toString\" does not affect conversion"), Arguments.of(map("value", "first", "_v", "second", "extra", "ignore"), stubConverter, "first", "Both VALUE and _v keys present, VALUE takes precedence"), Arguments.of(map("value", map("nested", map("deep", 5))), stubConverter, "{nested={deep=5}}", "VALUE maps to a nested map"), Arguments.of(map("_v", null), stubConverter, "null", "Only the alternate key _v is present and its value is null"), Arguments.of(3.1415, stubConverter, "converted:3.1415", "Non‑String, non‑Map numeric value (Double)"), Arguments.of(map("value", "   leading and trailing   "), stubConverter, "   leading and trailing   ", "VALUE contains whitespace – should be returned unchanged"), Arguments.of(map("value", "こんにちは"), stubConverter, "こんにちは", "VALUE holds Unicode characters"), Arguments.of(map("value", "nullString"), stubConverter, "nullString", "VALUE contains the literal text \"nullString\""), Arguments.of(map("value", "123", "_v", "456"), stubConverter, "123", "Both keys present with string values – VALUE preferred"), Arguments.of(map("_v", "onlyV"), stubConverter, "onlyV", "Only the alternate key _v is present"), Arguments.of(map("value", 0), stubConverter, "0", "VALUE maps to integer zero"), Arguments.of(map("value", false), stubConverter, "false", "VALUE maps to Boolean false"), Arguments.of(map("value", Arrays.asList(1, 2, 3)), stubConverter, "[1, 2, 3]", "VALUE holds a List – default toString representation"), Arguments.of(map("value", map("a", 1), "_v", map("b", 2)), stubConverter, "{a=1}", "Both keys present with map values – VALUE's map toString returned"), Arguments.of("any", null, "NullPointerException", "Converter argument is null – should throw NPE"));
    }

    /**
     * Returns {@code true} if the supplied method returns {@code void}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method's return type is {@code void}
     */
    public static boolean returnsVoid(java.lang.reflect.Method method) {
        return method.getReturnType() == void.class;
    }

    @Test
    void toString_nonMapDelegatesToConverter() {
        // For a non‑map source the method should fall back to the generic conversion
        // path which, for a String source, simply returns the same string.
        String source = "already a string";
        String result = MapConversions.toString(source, converter);
        assertEquals(source, result, "Non‑map values should be returned unchanged");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("additionalCases")
    void toString_additionalCases(Object from, Converter conv, String expected, String description) {
        if ("NullPointerException".equals(expected)) {
            assertThrows(NullPointerException.class, () -> MapConversions.toString(from, conv), description);
            return;
        }
        String result = MapConversions.toString(from, conv);
        assertEquals(expected, result, description);
    }

    @Test
    void toString_converterThrowsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toString(123, throwingIllegalArgumentConverter), "Converter throws IllegalArgumentException for non‑Map, non‑String input");
    }

    @Test
    void toString_valueObjectToStringThrowsRuntimeException() {
        Map<String, Object> map = new HashMap<>();
        map.put("value", new BadToString());
        RuntimeException ex = assertThrows(RuntimeException.class, () -> MapConversions.toString(map, stubConverter), "RuntimeException from value's toString should propagate");
        assertEquals("bad toString", ex.getMessage());
    }

    @Test
    void toString_mapGetThrowsUnsupportedOperationException() {
        Map<String, Object> throwingMap = new ThrowingMap();
        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, () -> MapConversions.toString(throwingMap, stubConverter), "Map.get should throw UnsupportedOperationException and be propagated");
        assertEquals("get not supported", ex.getMessage());
    }

    @Test
    void toString_mapIterationThrowsConcurrentModificationException() {
        Map<String, Object> cmeMap = new CmeMap();
        ConcurrentModificationException ex = assertThrows(ConcurrentModificationException.class, () -> MapConversions.toString(cmeMap, stubConverter), "Iterating over map entries should propagate ConcurrentModificationException");
        assertEquals("iteration broken", ex.getMessage());
    }

    @Test
    void toString_charSequenceWithNullPointerThrowingConverter() {
        assertThrows(NullPointerException.class, () -> MapConversions.toString(new StringBuilder("test"), throwingNullPointerConverter), "Converter throws NullPointerException for CharSequence source");
    }

    @Test
    void toString_arrayWithIllegalStateThrowingConverter() {
        int[] array = new int[] { 1, 2, 3 };
        assertThrows(IllegalStateException.class, () -> MapConversions.toString(array, throwingIllegalStateConverter), "Converter throws IllegalStateException for array source");
    }
}
