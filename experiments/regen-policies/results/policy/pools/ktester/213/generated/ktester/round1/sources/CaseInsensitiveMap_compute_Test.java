package com.cedarsoftware.util;

import java.util.function.BiFunction;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
// import org.junit.*;
// import static org.junit.Assert.*;

class CaseInsensitiveMap_compute_Test {

    private CaseInsensitiveMap<String, String> map;

    // ---------- compute with key of type other than String that overrides equals/hashCode ----------
    static class CustomKey {

        final int id;

        CustomKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CustomKey && ((CustomKey) o).id == this.id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }

        @Override
        public String toString() {
            return "CustomKey(" + id + ")";
        }
    }

//     @BeforeEach
    void setupBeforeEach() {
        map = new CaseInsensitiveMap<>();
        map.put("One", "Alpha");
        map.put("Two", "Beta");
    }

//     @AfterEach
    void teardownAfterEach() {
        // Reset static cache to default values to avoid cross‑test interference
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @Test
//     void computeExistingKey_ShouldPassOriginalKeyToBiFunction_AndReplaceValue() {
//         @SuppressWarnings("unchecked")
//         BiFunction<String, String, String> mockFunction = mock(BiFunction.class);
//         when(mockFunction.apply(anyString(), anyString())).thenAnswer(invocation -> {
//             String key = invocation.getArgument(0);
//             String value = invocation.getArgument(1);
//             return value + "-" + key;
//         });
//         String result = map.compute("one", mockFunction);
//         assertEquals("Alpha-One", result);
//         assertEquals("Alpha-One", map.get("ONE"));
//         ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
//         ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
//         verify(mockFunction, times(1)).apply(keyCaptor.capture(), valueCaptor.capture());
//         assertEquals("One", keyCaptor.getValue());
//         assertEquals("Alpha", valueCaptor.getValue());
//     }

//     @Test
//     void computeAbsentKey_ShouldInsertNewEntryAndReturnFunctionResult() {
//         BiFunction<String, String, String> function = (k, v) -> "generated-" + k;
//         String result = map.compute("three", function);
//         assertEquals("generated-three", result);
//         assertEquals("generated-three", map.get("THREE"));
//         assertTrue(map.containsKey("Three"));
//     }

//     @Test
//     void computeWithNonStringKey_ShouldBehaveLikeRegularMap() {
//         CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
//         intMap.put(42, "Answer");
//         BiFunction<Integer, String, String> function = (k, v) -> v + "-updated";
//         String result = intMap.compute(42, function);
//         assertEquals("Answer-updated", result);
//         assertEquals("Answer-updated", intMap.get(42));
//     }

    // ---------- compute with null key ----------
//     @Test
//     void compute_NullKey_ShouldThrowNullPointerException() {
//         BiFunction<String, String, String> fn = (k, v) -> "any";
//         map.compute(null, fn);
//     }

    // ---------- compute with non‑String key (no conversion) ----------
//     @Test
//     void compute_NonStringKey_NoConversion_InsertAndUpdate() {
//         CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
//         String first = intMap.compute(42, (k, v) -> v == null ? "first" : v + "-updated");
//         assertEquals("first", first);
//         assertEquals("first", intMap.get(42));
//         String updated = intMap.compute(42, (k, v) -> v + "-again");
//         assertEquals("first-again", updated);
//         assertEquals("first-again", intMap.get(42));
//     }

    // ---------- compute with String key – case‑insensitive match ----------
//     @Test
//     void compute_StringKey_CaseInsensitiveMatch_ModifyAndRemove() {
//         map.put("foo", "bar");
//         String modified = map.compute("FOO", (k, v) -> v + "_X");
//         assertEquals("bar_X", modified);
//         assertEquals("bar_X", map.get("foo"));
//         assertEquals("bar_X", map.get("FOO"));
//         String removed = map.compute("FoO", (k, v) -> null);
//         assertNull(removed);
//         assertFalse(map.containsKey("foo"));
//         assertFalse(map.containsKey("FOO"));
//     }

    // ---------- compute with String key – absent entry ----------
//     @Test
//     void compute_StringKey_AbsentEntry_InsertsUpperCaseKeyResult() {
//         String result = map.compute("NeWKey", (k, v) -> k.toUpperCase());
//         assertEquals("NEWKEY", result);
//         assertEquals("NEWKEY", map.get("newkey"));
//         assertTrue(map.containsKey("NeWKey"));
//     }

    // ---------- compute when remappingFunction throws ----------
//     @Test
//     void compute_RemappingFunctionThrows_PropagatesException() {
//         BiFunction<String, String, String> throwingFn = (k, v) -> {
//             throw new IllegalStateException("boom");
//         };
//         map.compute("error", throwingFn);
//     }

    // ---------- compute with empty string key ----------
//     @Test
//     void compute_EmptyStringKey_InsertAndUpdate() {
//         String first = map.compute("", (k, v) -> v == null ? "empty" : v + "_again");
//         assertEquals("empty", first);
//         assertEquals("empty", map.get(""));
//         String second = map.compute("", (k, v) -> v + "_updated");
//         assertEquals("empty_updated", second);
//         assertEquals("empty_updated", map.get(""));
//     }

    // ---------- compute with mixed‑case String key after prior lower‑case insertion ----------
//     @Test
//     void compute_MixedCaseKey_AfterLowerCaseInsertion() {
//         String first = map.compute("MiXeD", (k, v) -> "value");
//         assertEquals("value", first);
//         assertEquals("value", map.get("mixed"));
//         String second = map.compute("MIXED", (k, v) -> v + "_again");
//         assertEquals("value_again", second);
//         assertEquals("value_again", map.get("mixed"));
//     }

    // ---------- compute with null remappingFunction ----------
//     @Test
//     void compute_NullRemappingFunction_ShouldThrowNullPointerException() {
//         map.compute("anyKey", null);
//     }

    // ---------- compute where remappingFunction returns same value ----------
//     @Test
//     void compute_ReturnsSameValue_NoChange() {
//         String first = map.compute("same", (k, v) -> v == null ? "orig" : v);
//         assertEquals("orig", first);
//         assertEquals("orig", map.get("same"));
//         String second = map.compute("SAME", (k, v) -> v);
//         assertEquals("orig", second);
//         assertEquals("orig", map.get("same"));
//     }

//     @Test
//     void compute_CustomKey_NoCaseConversion() {
//         CaseInsensitiveMap<CustomKey, String> customMap = new CaseInsensitiveMap<>();
//         CustomKey key1 = new CustomKey(1);
//         String first = customMap.compute(key1, (k, v) -> "first");
//         assertEquals("first", first);
//         assertEquals("first", customMap.get(key1));
//         CustomKey key2 = new CustomKey(1);
//         String second = customMap.compute(key2, (k, v) -> v + "_upd");
//         assertEquals("first_upd", second);
//         assertEquals("first_upd", customMap.get(key1));
//     }

    // ---------- compute on unmodifiable backing map ----------
//     @Test
//     void compute_UnmodifiableBackingMap_ShouldThrowUnsupportedOperationException() throws Exception {
//         CaseInsensitiveMap<String, String> unmodMap = new CaseInsensitiveMap<>();
//         Map<String, String> backing = Collections.unmodifiableMap(new HashMap<>());
//         Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(unmodMap, backing);
//         BiFunction<String, String, String> fn = (k, v) -> "value";
//         unmodMap.compute("anyKey", fn);
//     }

    // ---------- compute with incompatible return type ----------
//     @Test
//     void compute_IncompatibleReturnType_ShouldThrowClassCastException() {
//         CaseInsensitiveMap<String, Integer> intMap = new CaseInsensitiveMap<>();
//         BiFunction<String, Integer, Integer> fn = (k, v) -> (Integer) (Object) "notAnInteger";
//         intMap.compute("num", fn);
//     }
}