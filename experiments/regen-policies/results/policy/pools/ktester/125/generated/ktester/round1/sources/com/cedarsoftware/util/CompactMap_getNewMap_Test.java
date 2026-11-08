package com.cedarsoftware.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.HashSet;
import java.io.Serializable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.HashMap;
import java.util.Map;
import java.io.ObjectOutputStream;
import java.io.ObjectInputStream;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class CompactMap_getNewMap_Test {

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isBridge(Method method) {
        return method.isBridge();
    }

    public static boolean isSynthetic(Method method) {
        return method.isSynthetic();
    }

    public static boolean returnsVoid(Method method) {
        return void.class.equals(method.getReturnType());
    }

    public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
        return method.isAnnotationPresent(annotation);
    }

    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    public static Method[] getDeclaredMethods(Class<?> clazz) {
        return clazz.getDeclaredMethods();
    }

    public static boolean isPublic(Class<?> clazz) {
        return Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isAbstract(Class<?> clazz) {
        return Modifier.isAbstract(clazz.getModifiers());
    }

    public static boolean isAnnotationPresent(Class<?> clazz, Class<? extends Annotation> annotation) {
        return clazz.isAnnotationPresent(annotation);
    }

    public static boolean isAssignable(Class<?> from, Class<?> to) {
        return to.isAssignableFrom(from);
    }

    public static Class<?>[] getDeclaredClasses(Class<?> clazz) {
        return clazz.getDeclaredClasses();
    }

    public static Field[] getDeclaredFields(Class<?> clazz) {
        return clazz.getDeclaredFields();
    }

    public static boolean isAssignableFrom(Class<?> superClass, Class<?> subClass) {
        return superClass.isAssignableFrom(subClass);
    }

    /**
     * -----------------------------------------------------------------
     *  Test that the default implementation of getNewMap() returns a
     *  fresh HashMap instance.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void defaultGetNewMapReturnsHashMap() {
//         CompactMap<String, Integer> map = new CompactMap<>();
//         Map<String, Integer> newMap = map.getNewMap();
//         assertTrue(newMap instanceof HashMap);
//         assertTrue(newMap.isEmpty());
//         newMap.put("one", 1);
//         assertEquals(1, newMap.size());
//     }

    /**
     * -----------------------------------------------------------------
     *  Test that successive calls to getNewMap() produce distinct
     *  instances (no caching of the same map).
     *  -----------------------------------------------------------------
     */
//     @Test
//     void getNewMapProducesDistinctInstances() {
//         CompactMap<String, String> map = new CompactMap<>();
//         Map<String, String> first = map.getNewMap();
//         Map<String, String> second = map.getNewMap();
//         assertNotSame(first, second);
//     }

    /**
     * -----------------------------------------------------------------
     *  Test that a subclass overriding getNewMap() is honoured.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void overriddenGetNewMapIsUsed() {
//         CompactMap<String, Integer> customMap = new CompactMap<String, Integer>() {
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         Map<String, Integer> result = customMap.getNewMap();
//         assertTrue(result instanceof LinkedHashMap);
//         result.put("a", 10);
//         result.put("b", 20);
//         assertEquals(2, result.size());
//         assertEquals(Integer.valueOf(10), result.get("a"));
//     }

    /**
     * -----------------------------------------------------------------
     *  Test that the overridden map respects insertion order – a
     *  behavioural check that confirms we really got a LinkedHashMap.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void overriddenMapPreservesInsertionOrder() {
//         CompactMap<String, Integer> orderedMap = new CompactMap<String, Integer>() {
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         Map<String, Integer> map = orderedMap.getNewMap();
//         map.put("first", 1);
//         map.put("second", 2);
//         map.put("third", 3);
//         StringBuilder sb = new StringBuilder();
//         for (String key : map.keySet()) {
//             sb.append(key).append(',');
//         }
//         assertEquals("first,second,third,", sb.toString());
//     }

    /**
     * -----------------------------------------------------------------
     *  Verify that the internal 'val' field of CompactMap stays at its
     *  sentinel EMPTY_MAP after calling getNewMap().
     *  -----------------------------------------------------------------
     */
//     @Test
//     void internalValRemainsEmptyMapAfterGetNewMap() throws Exception {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         Class<?> clazz = CompactMap.class;
//         Field emptyMapField = clazz.getDeclaredField("EMPTY_MAP");
//         emptyMapField.setAccessible(true);
//         Object emptyMapSentinel = emptyMapField.get(null);
//         cm.getNewMap();
//         Field valField = clazz.getDeclaredField("val");
//         valField.setAccessible(true);
//         Object val = valField.get(cm);
//         assertSame(emptyMapSentinel, val);
//     }

    /**
     * -----------------------------------------------------------------
     *  Null handling – HashMap should accept null keys and values.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void getNewMapAcceptsNullKeyAndNullValue() {
//         CompactMap<String, String> cm = new CompactMap<>();
//         Map<String, String> map = cm.getNewMap();
//         map.put(null, null);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey(null));
//         assertNull(map.get(null));
//     }

//     @Test
//     void getNewMapAcceptsNullKeyWithNonNullValue() {
//         CompactMap<String, String> cm = new CompactMap<>();
//         Map<String, String> map = cm.getNewMap();
//         map.put(null, "value");
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey(null));
//         assertEquals("value", map.get(null));
//     }

//     @Test
//     void getNewMapAcceptsNonNullKeyWithNullValue() {
//         CompactMap<String, String> cm = new CompactMap<>();
//         Map<String, String> map = cm.getNewMap();
//         map.put("key", null);
//         assertEquals(1, map.size());
//         assertTrue(map.containsKey("key"));
//         assertNull(map.get("key"));
//     }

    /**
     * -----------------------------------------------------------------
     *  Independence after CompactMap mutation – the fresh map must be
     *  empty regardless of the owning CompactMap's state.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void newMapIsEmptyEvenAfterCompactMapIsPopulated() {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         cm.put("existing", 42);
//         Map<String, Integer> fresh = cm.getNewMap();
//         assertTrue(fresh.isEmpty());
//     }

//     @Test
//     void modificationsToReturnedMapDoNotAffectCompactMap() {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         cm.put("a", 1);
//         cm.put("b", 2);
//         int originalSize = cm.size();
//         Map<String, Integer> fresh = cm.getNewMap();
//         fresh.put("x", 99);
//         fresh.put("y", 100);
//         assertEquals(originalSize, cm.size());
//     }

    /**
     * -----------------------------------------------------------------
     *  Iterator behavior – entrySet iterator must contain all entries and
     *  throw NoSuchElementException when exhausted.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void iteratorOverEntriesContainsAllKeys() {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         Map<String, Integer> map = cm.getNewMap();
//         map.put("one", 1);
//         map.put("two", 2);
//         map.put("three", 3);
//         Set<String> iteratedKeys = new HashSet<>();
//         for (Map.Entry<String, Integer> e : map.entrySet()) {
//             iteratedKeys.add(e.getKey());
//         }
//         assertEquals(map.keySet(), iteratedKeys);
//     }

//     @Test
//     void iteratorThrowsNoSuchElementExceptionAfterExhaustion() {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         Map<String, Integer> map = cm.getNewMap();
//         map.put("only", 1);
//         Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
//         assertTrue(it.hasNext());
//         it.next();
//         assertFalse(it.hasNext());
//         try {
//             it.next();
//             fail("Expected NoSuchElementException");
//         } catch (NoSuchElementException ignored) {
//             // expected
//         }
//     }

    /**
     * -----------------------------------------------------------------
     *  Serialization compatibility – the map returned by getNewMap()
     *  must be serializable and retain its contents after a round‑trip.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void mapCanBeSerializedAndDeserializedPreservingContents() throws Exception {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         Map<String, Integer> original = cm.getNewMap();
//         original.put("a", 10);
//         original.put("b", 20);
//         byte[] bytes;
//         try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
//             ObjectOutputStream out = new ObjectOutputStream(bos)) {
//             out.writeObject(original);
//             out.flush();
//             bytes = bos.toByteArray();
//         }
//         Map<String, Integer> deserialized;
//         try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
//             ObjectInputStream in = new ObjectInputStream(bis)) {
//             @SuppressWarnings("unchecked")
//             Map<String, Integer> tmp = (Map<String, Integer>) in.readObject();
//             deserialized = tmp;
//         }
//         assertEquals(original, deserialized);
//     }

    /**
     * -----------------------------------------------------------------
     *  Exact type verification – ensure the concrete class is exactly
     *  java.util.HashMap and that it implements Map and Serializable.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void returnedObjectClassNameIsExactlyHashMap() {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         Map<String, Integer> map = cm.getNewMap();
//         assertEquals("java.util.HashMap", map.getClass().getName());
//     }

//     @Test
//     void returnedMapImplementsMapAndSerializable() {
//         CompactMap<String, Integer> cm = new CompactMap<>();
//         Map<String, Integer> map = cm.getNewMap();
//         assertTrue(map instanceof Map);
//         assertTrue(map instanceof Serializable);
//     }

    /**
     * -----------------------------------------------------------------
     *  Independence between multiple maps created by getNewMap()
     *  – modifications to one must not affect the other.
     *  -----------------------------------------------------------------
     */
//     @Test
//     void modificationsToOneMapDoNotAffectAnother() {
//         CompactMap<String, String> cm = new CompactMap<>();
//         Map<String, String> first = cm.getNewMap();
//         Map<String, String> second = cm.getNewMap();
//         first.put("k1", "v1");
//         second.put("k2", "v2");
//         assertEquals(Set.of("k1"), first.keySet());
//         assertEquals(Set.of("k2"), second.keySet());
//     }
}