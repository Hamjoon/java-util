package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

class CompactMap_put_Test {

    /**
     * Supplies the different value objects required for the EMPTY_MAP tests.
     */
//     private static Stream<Arguments> emptyMapStateProvider() {
//         return Stream.of(Arguments.of("value1", "Simple String value"), Arguments.of(new HashMap<>(), "HashMap value – should fall back to entry"), Arguments.of(new Object[0], "Empty Object[] – should fall back to entry"), Arguments.of(null, "Null value – allowed, stored directly"), Arguments.of(new ArrayList<>(), "ArrayList (non‑Map/Object[]) – stored directly"));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void}
     * return type.
     *
     * @param method the method to examine
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && method.getReturnType() == void.class;
    }

    /**
     * Checks whether the given method is annotated with the supplied
     * annotation type.
     *
     * @param method     the method to check
     * @param annotation the annotation type
     * @return {@code true} if the method is annotated with {@code annotation}
     */
//     public static boolean isAnnotated(Method method, Class<? extends Annotation> annotation) {
//         return method != null && method.isAnnotationPresent(annotation);
//     }

    /**
     * Finds a declared method with the given name and parameter types.
     *
     * @param clazz      the class to search
     * @param name       the method name
     * @param paramTypes the parameter types (may be empty)
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        if (clazz == null || name == null) {
            return null;
        }
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    /**
     * Determines whether a method with the given signature is present on the
     * supplied class.
     *
     * @param clazz      the class to inspect
     * @param name       the method name
     * @param paramTypes the parameter types (may be empty)
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... paramTypes) {
        return findMethod(clazz, name, paramTypes) != null;
    }

    public static boolean isPublic(Method method) {
        return method != null && Modifier.isPublic(method.getModifiers());
    }

    public static boolean isPublic(Constructor<?> ctor) {
        return ctor != null && Modifier.isPublic(ctor.getModifiers());
    }

    public static boolean isPublic(Field field) {
        return field != null && Modifier.isPublic(field.getModifiers());
    }

    public static boolean isPublic(Class<?> clazz) {
        return clazz != null && Modifier.isPublic(clazz.getModifiers());
    }

    public static boolean isStatic(Method method) {
        return method != null && Modifier.isStatic(method.getModifiers());
    }

    public static boolean isStatic(Constructor<?> ctor) {
        return ctor != null && Modifier.isStatic(ctor.getModifiers());
    }

    public static boolean isStatic(Field field) {
        return field != null && Modifier.isStatic(field.getModifiers());
    }

    public static boolean isAbstract(Method method) {
        return method != null && Modifier.isAbstract(method.getModifiers());
    }

    public static boolean isAbstract(Class<?> clazz) {
        return clazz != null && Modifier.isAbstract(clazz.getModifiers());
    }

    public static boolean isPublicStatic(Method method) {
        return isPublic(method) && isStatic(method);
    }

    public static boolean isPublicStaticFinal(Method method) {
        return isPublicStatic(method) && Modifier.isFinal(method.getModifiers());
    }

    public static boolean isPublicStaticOrAbstract(Method method) {
        return isPublicStatic(method) || isAbstract(method);
    }

    public static boolean isPublicStaticOrFinal(Method method) {
        return isPublicStatic(method) || Modifier.isFinal(method.getModifiers());
    }

    public static boolean isPublicFinal(Method method) {
        return method != null && Modifier.isPublic(method.getModifiers()) && Modifier.isFinal(method.getModifiers());
    }

    public static boolean isPublicOrProtected(Method method) {
        if (method == null)
            return false;
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods);
    }

    public static boolean isPublicOrProtectedOrPackage(Method method) {
        if (method == null)
            return false;
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) || Modifier.isProtected(mods) || (!Modifier.isPrivate(mods) && !Modifier.isProtected(mods) && !Modifier.isPublic(mods));
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivate(Method method) {
        return method != null && (Modifier.isPublic(method.getModifiers()) || Modifier.isProtected(method.getModifiers()) || Modifier.isPrivate(method.getModifiers()) || (!Modifier.isPublic(method.getModifiers()) && !Modifier.isProtected(method.getModifiers()) && !Modifier.isPrivate(method.getModifiers())));
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrStatic(Method method) {
        return isPublicOrProtectedOrPackageOrPrivate(method) || isStatic(method);
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinal(Method method) {
        return isPublicOrProtectedOrPackageOrPrivateOrStatic(method) || Modifier.isFinal(method.getModifiers());
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrStaticOrAbstract(Method method) {
        return isPublicOrProtectedOrPackageOrPrivateOrStatic(method) || isAbstract(method);
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrFinalOrAbstract(Method method) {
        return isPublicOrProtectedOrPackageOrPrivate(method) || Modifier.isFinal(method.getModifiers()) || isAbstract(method);
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinalOrAbstract(Method method) {
        return isPublicOrProtectedOrPackageOrPrivateOrStatic(method) || Modifier.isFinal(method.getModifiers()) || isAbstract(method);
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinalOrAbstractOrTransient(Method method) {
        return isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinalOrAbstract(method) || Modifier.isTransient(method.getModifiers());
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinalOrAbstractOrTransientOrVolatile(Method method) {
        return isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinalOrAbstractOrTransient(method) || Modifier.isVolatile(method.getModifiers());
    }

    public static boolean isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinalOrAbstractOrTransientOrVolatileOrSynchronized(Method method) {
        return isPublicOrProtectedOrPackageOrPrivateOrStaticOrFinalOrAbstractOrTransientOrVolatile(method) || Modifier.isSynchronized(method.getModifiers());
    }

    public static boolean isPublicStatic(Constructor<?> ctor) {
        return isPublic(ctor) && isStatic(ctor);
    }

    public static boolean isPublicStaticFinal(Constructor<?> ctor) {
        return isPublicStatic(ctor) && Modifier.isFinal(ctor.getModifiers());
    }

    public static boolean isPublicStaticOrAbstract(Constructor<?> ctor) {
        return isPublicStatic(ctor) || isAbstract(ctor.getDeclaringClass());
    }

    public static boolean isPublicStaticOrFinal(Constructor<?> ctor) {
        return isPublicStatic(ctor) || Modifier.isFinal(ctor.getModifiers());
    }

    public static boolean isPublicFinal(Constructor<?> ctor) {
        return isPublic(ctor) && Modifier.isFinal(ctor.getModifiers());
    }

//     public static boolean isPublicOrProtectedOrPackageOrPrivate(Constructor<?> ctor) {
//         return isPublicOrProtectedOrPackageOrPrivate(ctor.getDeclaringClass());
//     }

//     public static boolean isPublicOrProtectedOrPackageOrPrivateOrStatic(Constructor<?> ctor) {
//         return isPublicOrProtectedOrPackageOrPrivateOrStatic(ctor.getDeclaringClass());
//     }

    // Similar helpers for {@link Field}
    public static boolean isPublicStatic(Field field) {
        return isPublic(field) && isStatic(field);
    }

    public static boolean isPublicStaticFinal(Field field) {
        return isPublicStatic(field) && Modifier.isFinal(field.getModifiers());
    }

    public static boolean isPublicStaticOrFinal(Field field) {
        return isPublicStatic(field) || Modifier.isFinal(field.getModifiers());
    }

    public static boolean isPublicFinal(Field field) {
        return isPublic(field) && Modifier.isFinal(field.getModifiers());
    }

//     public static boolean isPublicOrProtectedOrPackageOrPrivate(Field field) {
//         return isPublicOrProtectedOrPackageOrPrivate(field.getDeclaringClass());
//     }

//     public static boolean isPublicOrProtectedOrPackageOrPrivateOrStatic(Field field) {
//         return isPublicOrProtectedOrPackageOrPrivateOrStatic(field.getDeclaringClass());
//     }

    /**
     * When the map is empty and the supplied key equals the configured
     * {@code singleValueKey} and the value is *not* a {@code Map} or an
     * {@code Object[]} the implementation stores the value directly (no
     * {@link CompactMapEntry} wrapper).  The method must return {@code null}
     * and the map size becomes 1.
     */
//     @Test
//     void putIntoEmptyMap_optimisedSingleValueStorage() {
//         CompactMap<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // any value >= 2
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         assertTrue(map.isEmpty());
//         // first insertion, returns null
//         assertNull(map.put("id", 42));
//         assertEquals(1, map.size());
//         assertEquals(42, map.get("id"));
//     }

    /**
     * When the map is empty but the key does **not** match the configured
     * {@code singleValueKey}, the implementation creates a {@link CompactMapEntry}
     * to hold the first key/value pair.
     */
//     @Test
//     void putIntoEmptyMap_createsEntryWhenKeyDiffers() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         assertNull(map.put("name", "John"));
//         assertEquals(1, map.size());
//         assertEquals("John", map.get("name"));
//     }

    /**
     * Overwrite an existing entry while the map is still in the *single‑entry*
     * state.  The previous value must be returned.
     */
//     @Test
//     void putOverwriteSingleEntry_returnsPreviousValue() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "key";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         assertNull(map.put("key", 1));
//         assertEquals(1, map.size());
//         // second put with the same key – should replace and return the old value
//         Integer previous = map.put("key", 2);
//         assertEquals(Integer.valueOf(1), previous);
//         assertEquals(1, map.size());
//         assertEquals(Integer.valueOf(2), map.get("key"));
//     }

    /**
     * Transition from *empty* → *single entry* → *compact array*.
     * With a {@code compactSize} CompactMap_put_Test3, the map stores the first two entries
     * in an internal {@code Object[]} array.
     */
//     @Test
//     void putTransitionsToCompactArray() {
//         CompactMap<String, String> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // 2‑3 entries stay in compact array
//                 return 3;
//             }
// 
//             @Override
//             protected Map<String, String> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         // first entry – single entry state
//         assertNull(map.put("id", "first"));
//         assertEquals(1, map.size());
//         // second entry – should cause transition to compact array
//         assertNull(map.put("a", "alpha"));
//         assertEquals(2, map.size());
//         // verify both entries are present
//         assertEquals("first", map.get("id"));
//         assertEquals("alpha", map.get("a"));
//     }

    /**
     * Transition from *compact array* to *backing map*.
     * With a {@code compactSize} CompactMap_put_Test2, adding a third entry forces the
     * implementation to replace the internal array with a real {@link Map}
     * (the type returned by {@code getNewMap()}).
     */
//     @Test
//     void putTransitionsToBackingMap() {
//         CompactMap<String, Integer> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // >2 entries trigger backing map
//                 return 2;
//             }
// 
//             @Override
//             protected Map<String, Integer> getNewMap() {
//                 // use a LinkedHashMap so iteration order is predictable
//                 return new LinkedHashMap<>();
//             }
//         };
//         // fill up to compact size (2 entries)
//         assertNull(map.put("id", 10));
//         assertNull(map.put("a", 1));
//         assertEquals(2, map.size());
//         // third entry – should switch to backing map
//         assertNull(map.put("b", 2));
//         assertEquals(3, map.size());
//         // all entries must be accessible
//         assertEquals(Integer.valueOf(10), map.get("id"));
//         assertEquals(Integer.valueOf(1), map.get("a"));
//         assertEquals(Integer.valueOf(2), map.get("b"));
//     }

    /**
     * Verify that a {@code Map} value is **not** stored directly in the
     * *single‑entry* optimisation path.  When the key matches the
     * {@code singleValueKey} but the value is a {@link Map}, the implementation
     * must fall back to the regular entry representation.
     */
//     @Test
//     void putMapValueWhenKeyMatchesSingleValueKeyFallsBackToEntry() {
//         CompactMap<String, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 return "id";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<String, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         Map<String, String> inner = new HashMap<>();
//         inner.put("x", "y");
//         // because the value is a Map, the optimisation is not applied
//         assertNull(map.put("id", inner));
//         assertEquals(1, map.size());
//         assertSame(inner, map.get("id"));
//     }

    /**
     * Various values inserted into an *empty* map where the key equals the
     * configured {@code singleValueKey} (which is {@code null} for this test).
     * The map should store the value directly unless it is a {@code Map} or
     * {@code Object[]} – in those cases a {@link CompactMapEntry} is created.
     */
//     @ParameterizedTest(name = "{index}: value = {1}")
//     @MethodSource("emptyMapStateProvider")
//     void putIntoEmptyMap_EmptyState_VariousValues(Object value, String description) {
//         CompactMap<Object, Object> map = new CompactMap<>() {
// 
//             @Override
//             protected Object getSingleValueKey() {
//                 // singleValueKey is null for this group
//                 return null;
//             }
// 
//             @Override
//             protected int compactSize() {
//                 return 5;
//             }
// 
//             @Override
//             protected Map<Object, Object> getNewMap() {
//                 return new LinkedHashMap<>();
//             }
//         };
//         // key is null, matching the singleValueKey
//         assertNull(map.put(null, value), "put should return null for first insertion");
//         assertEquals(1, map.size(), "size must be 1 after insertion");
//         // The map must return the exact instance we stored (reference equality)
//         assertSame(value, map.get(null), description);
//     }

    /**
     * A {@link CompactMap} built with sorted ordering uses a {@link TreeMap}
     * as its backing map.  Inserting a key that does not implement {@link Comparable}
     * must result in {@link ClassCastException}.
     *
     * The test first inserts a comparable key to force the map out of the
     * EMPTY/SINGLE‑ENTRY state into the backing {@code TreeMap}.  The second
     * insertion uses a non‑comparable key and verifies the exception.
     */
//     @Test
//     void putSortedMap_NonComparableKey_throwsClassCastException() {
//         CompactMap<Object, String> map = new CompactMap<>() {
// 
//             @Override
//             protected String getSingleValueKey() {
//                 // use a key that will not match the first insertion
//                 return "id";
//             }
// 
//             @Override
//             protected int compactSize() {
//                 // any value >= 2, we will trigger map after the second put
//                 return 2;
//             }
// 
//             @Override
//             protected String getOrdering() {
//                 // force sorted ordering; using the constant defined in CompactMap
//                 return CompactMap.SORTED;
//             }
// 
//             @Override
//             protected Map<Object, String> getNewMap() {
//                 // TreeMap provides the sorted behaviour
//                 return new TreeMap<>();
//             }
//         };
//         // First, insert a normal comparable key to move the map into the backing Map state.
//         assertNull(map.put("a", "first"));
//         assertEquals(1, map.size());
//         // Second insertion uses a non‑Comparable key – expect ClassCastException.
//         assertThrows(ClassCastException.class, () -> map.put(new Object(), "someValue"));
//     }
}