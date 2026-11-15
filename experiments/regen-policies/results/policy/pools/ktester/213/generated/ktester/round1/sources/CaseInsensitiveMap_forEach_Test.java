package com.cedarsoftware.util;

import org.mockito.InOrder;
import org.mockito.Mock;
import java.util.function.BiConsumer;
import static org.mockito.Mockito.*;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.Set;
// import org.junit.*;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class CaseInsensitiveMap_forEach_Test {

    /**
     * The map under test – created fresh for each test method.
     */
    private CaseInsensitiveMap<String, String> stringMap;

    /**
     * A map that mixes String and non‑String keys.
     */
    private CaseInsensitiveMap<Object, String> mixedMap;

//     @BeforeAll
    static void setupBeforeAll() {
        // Ensure the global cache is in a known state for all tests.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @BeforeEach
    void setupBeforeEach() {
        // Simple map with only String keys – the primary focus of the forEach test.
        stringMap = new CaseInsensitiveMap<>();
        stringMap.put("One", "Alpha");
        stringMap.put("Two", "Beta");
        stringMap.put("Three", "Gamma");
        // Map that contains both String and non‑String keys.
        mixedMap = new CaseInsensitiveMap<>();
        mixedMap.put("Key", "Value");
        mixedMap.put(42, "Answer");
    }

//     @AfterAll
    static void teardownAfterAll() {
        // Restore defaults – mirrors the cleanup performed in the original test suite.
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

//     @Test
//     void forEach_UnwrapsCaseInsensitiveStringKeys() {
//         // Arrange
//         @SuppressWarnings("unchecked")
//         BiConsumer<String, String> consumer = mock(BiConsumer.class);
//         // Act
//         stringMap.forEach(consumer);
//         // Assert – verify order (LinkedHashMap preserves insertion order)
//         InOrder inOrder = inOrder(consumer);
//         inOrder.verify(consumer).accept("One", "Alpha");
//         inOrder.verify(consumer).accept("Two", "Beta");
//         inOrder.verify(consumer).accept("Three", "Gamma");
//         inOrder.verifyNoMoreInteractions();
//     }

//     @Test
//     void forEach_MixedKeyTypes_PassesOriginalKeys() {
//         // Arrange
//         @SuppressWarnings("unchecked")
//         BiConsumer<Object, String> consumer = mock(BiConsumer.class);
//         // Act
//         mixedMap.forEach(consumer);
//         // Assert – order is insertion order
//         InOrder inOrder = inOrder(consumer);
//         inOrder.verify(consumer).accept("Key", "Value");
//         inOrder.verify(consumer).accept(42, "Answer");
//         inOrder.verifyNoMoreInteractions();
//     }

//     @Test
//     void forEach_NullAction_ThrowsNPE() {
//         stringMap.forEach(null);
//     }

//     @Test
//     void forEach_MixedKeyTypes_StandardBehavior() {
//         // Map with two CaseInsensitiveString keys (different case) and a non‑String key.
//         CaseInsensitiveMap<Object, Object> map = new CaseInsensitiveMap<>();
//         map.put("Alpha", 1);
//         map.put("beta", 2);
//         map.put(42, "answer");
//         List<Map.Entry<Object, Object>> collected = new ArrayList<>();
//         BiConsumer<Object, Object> collector = (k, v) -> collected.add(new AbstractMap.SimpleEntry<>(k, v));
//         map.forEach(collector);
//         assertEquals(3, collected.size());
//         assertEquals(new AbstractMap.SimpleEntry<>("Alpha", 1), collected.get(0));
//         assertEquals(new AbstractMap.SimpleEntry<>("beta", 2), collected.get(1));
//         assertEquals(new AbstractMap.SimpleEntry<>(42, "answer"), collected.get(2));
//     }

//     @Test
//     void forEach_MixedKeyTypes_DistinctCaseInsensitiveKeysPreserveOriginalCase() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("TEST", "upper");
//         // different case, distinct entry because map is case‑insensitive (second overwrites first)
//         map.put("test", "lower");
//         List<Map.Entry<String, String>> collected = new ArrayList<>();
//         map.forEach((k, v) -> collected.add(new AbstractMap.SimpleEntry<>(k, v)));
//         // The second put overwrites the first, so only one entry should be present with the original case of the stored key ("test").
//         assertEquals(1, collected.size());
//         assertEquals(new AbstractMap.SimpleEntry<>("test", "lower"), collected.get(0));
//     }

//     @Test
//     void forEach_EmptyMap_NoInvocations() {
//         CaseInsensitiveMap<String, String> empty = new CaseInsensitiveMap<>();
//         @SuppressWarnings("unchecked")
//         BiConsumer<String, String> consumer = mock(BiConsumer.class);
//         empty.forEach(consumer);
//         verifyNoInteractions(consumer);
//     }

//     @Test
//     void forEach_OnlyNonStringKeys() {
//         CaseInsensitiveMap<Object, String> map = new CaseInsensitiveMap<>();
//         map.put(100, "hundred");
//         map.put(3.14, "pi");
//         map.put(true, "false");
//         List<Map.Entry<Object, String>> collected = new ArrayList<>();
//         map.forEach((k, v) -> collected.add(new AbstractMap.SimpleEntry<>(k, v)));
//         assertEquals(3, collected.size());
//         assertTrue(collected.contains(new AbstractMap.SimpleEntry<>(100, "hundred")));
//         assertTrue(collected.contains(new AbstractMap.SimpleEntry<>(3.14, "pi")));
//         assertTrue(collected.contains(new AbstractMap.SimpleEntry<>(true, "false")));
//     }

//     @Test
//     void forEach_OnlyCaseInsensitiveStringKeys() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("First", 1);
//         map.put("second", 2);
//         List<Map.Entry<String, Integer>> collected = new ArrayList<>();
//         map.forEach((k, v) -> collected.add(new AbstractMap.SimpleEntry<>(k, v)));
//         assertEquals(2, collected.size());
//         assertTrue(collected.contains(new AbstractMap.SimpleEntry<>("First", 1)));
//         assertTrue(collected.contains(new AbstractMap.SimpleEntry<>("second", 2)));
//     }

//     @Test
//     void forEach_NullKeyHandling() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put(null, "nullValue");
//         List<Map.Entry<String, String>> collected = new ArrayList<>();
//         map.forEach((k, v) -> collected.add(new AbstractMap.SimpleEntry<>(k, v)));
//         assertEquals(1, collected.size());
//         assertNull(collected.get(0).getKey());
//         assertEquals("nullValue", collected.get(0).getValue());
//     }

//     @Test
//     void forEach_NullValueHandling() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("KeyOne", null);
//         map.put("KeyTwo", null);
//         List<Map.Entry<String, String>> collected = new ArrayList<>();
//         map.forEach((k, v) -> collected.add(new AbstractMap.SimpleEntry<>(k, v)));
//         assertEquals(2, collected.size());
//         assertTrue(collected.stream().anyMatch(e -> "KeyOne".equals(e.getKey()) && e.getValue() == null));
//         assertTrue(collected.stream().anyMatch(e -> "KeyTwo".equals(e.getKey()) && e.getValue() == null));
//     }

//     @Test
//     void forEach_BiConsumerThrowsException() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("A", 1);
//         map.put("B", 2);
//         map.put("C", 3);
//         BiConsumer<String, Integer> throwingConsumer = (k, v) -> {
//             if ("A".equals(k)) {
//                 throw new RuntimeException("first entry failure");
//             }
//         };
//         try {
//             map.forEach(throwingConsumer);
//             fail("Expected RuntimeException");
//         } catch (RuntimeException ex) {
//             assertEquals("first entry failure", ex.getMessage());
//         }
//     }

//     @Test
//     void forEach_DuplicateCaseInsensitiveKeys_SingleEntry() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("Duplicate", 1);
//         // overwrites the first entry
//         map.put("duplicate", 42);
//         List<Map.Entry<String, Integer>> collected = new ArrayList<>();
//         map.forEach((k, v) -> collected.add(new AbstractMap.SimpleEntry<>(k, v)));
//         assertEquals(1, collected.size());
//         // The stored key should retain the original case of the *last* put ("duplicate").
//         assertEquals(new AbstractMap.SimpleEntry<>("duplicate", 42), collected.get(0));
//     }

//     @Test
//     void forEach_NullBackingMap_ThrowsNPE() throws Exception {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         // Use reflection to set the private 'map' field to null.
//         java.lang.reflect.Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(map, null);
//         @SuppressWarnings("unchecked")
//         BiConsumer<String, String> consumer = mock(BiConsumer.class);
//         try {
//             map.forEach(consumer);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//             // expected
//         }
//     }

//     @Test
//     void forEach_KeyTypeMismatch_ClassCastException() throws Exception {
//         // Map declared with Integer keys, but we will inject a CaseInsensitiveString key.
//         CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>();
//         // Build a raw backing map containing a CIS key.
//         Map raw = new HashMap<>();
//         raw.put(CaseInsensitiveMap.CaseInsensitiveString.of("MISMATCH"), "value");
//         // Inject the raw map via reflection.
//         java.lang.reflect.Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(map, raw);
//         @SuppressWarnings("unchecked")
//         BiConsumer<Integer, String> consumer = mock(BiConsumer.class);
//         try {
//             map.forEach(consumer);
//             fail("Expected ClassCastException");
//         } catch (ClassCastException ignored) {
//             // expected
//         }
//     }

//     @Test
//     void forEach_UnderlyingMapThrowsRuntimeException() throws Exception {
//         // Custom map whose forEach method always throws.
//         Map<String, String> explodingMap = new AbstractMap<>() {
// 
//             @Override
//             public Set<Entry<String, String>> entrySet() {
//                 return Collections.emptySet();
//             }
// 
//             @Override
//             public void forEach(java.util.function.BiConsumer<? super String, ? super String> action) {
//                 throw new RuntimeException("map failure");
//             }
//         };
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         // Inject the exploding map.
//         java.lang.reflect.Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(map, explodingMap);
//         try {
//             map.forEach((k, v) -> {
//                 // no‑op
//             });
//             fail("Expected RuntimeException");
//         } catch (RuntimeException ex) {
//             assertEquals("map failure", ex.getMessage());
//         }
//     }
}