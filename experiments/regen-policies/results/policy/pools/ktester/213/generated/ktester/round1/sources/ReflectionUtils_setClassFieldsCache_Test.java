package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.util.Collection;
import java.lang.reflect.Field;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.ConcurrentHashMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import static org.junit.jupiter.api.Assertions.*;

class ReflectionUtils_setClassFieldsCache_Test {

    /**
     * Simple class used for reflection tests.
     */
    private static class Sample {

        private int intField;

        private String stringField;
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the default cache is restored before each test so that tests are independent.
        // An empty map is sufficient – the implementation will wrap it in a thread‑safe wrapper.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        // Ensure each test starts with a fresh (empty) cache.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        // Start each test with a fresh (empty) cache.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the default (empty) cache after each test to avoid side effects.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        // Reset after each test to avoid side effects.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
        // Restore the default (empty) cache after each test.
        ReflectionUtils.setClassFieldsCache(new HashMap<>());
    }

    /**
     * Finds a method with the given name and parameter types on the supplied
     * class (including inherited methods).  Returns {@code null} if no such
     * method exists.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types (may be empty)
     * @return the {@link Method} or {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Objects.requireNonNull(clazz, "Class must not be null");
        Objects.requireNonNull(name, "Method name must not be null");
        try {
            return clazz.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            // continue with declared methods (including private/protected)
        }
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException ignored) {
            // search super‑classes recursively
        }
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null) {
            return findMethod(superClass, name, parameterTypes);
        }
        return null;
    }

    /**
     * Returns {@code true} if a method with the supplied signature is present
     * on the given class (including its super‑classes), {@code false} otherwise.
     *
     * @param clazz          the class to inspect
     * @param name           the method name
     * @param parameterTypes the method parameter types
     * @return {@code true} if the method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    /**
     * Determines whether the supplied method returns {@code void}.
     *
     * @param method the method to examine; must not be {@code null}
     * @return {@code true} if the method's return type is {@code void}
     */
    public static boolean returnsVoid(Method method) {
        Objects.requireNonNull(method, "Method must not be null");
        return void.class.equals(method.getReturnType());
    }

    /**
     * Verify that {@code setClassFieldsCache} accepts a non‑null map and that the
     * subsequent call to {@code getAllDeclaredFields} still returns the correct fields.
     */
    @Test
    void setClassFieldsCache_ReplacesCacheAndPreservesFunctionality() {
        Map<Object, Collection<Field>> customCache = new HashMap<>();
        customCache.put("dummyKey", Collections.emptyList());
        assertDoesNotThrow(() -> ReflectionUtils.setClassFieldsCache(customCache));
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(Sample.class);
        assertNotNull(fields);
        assertEquals(2, fields.size());
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertTrue(names.contains("intField"));
        assertTrue(names.contains("stringField"));
    }

    /**
     * Verify that passing {@code null} to {@code setClassFieldsCache} results in a
     * {@link NullPointerException}. The implementation calls {@code ensureThreadSafe}
     * which expects a non‑null argument.
     */
    @Test
    void setClassFieldsCache_NullArgumentThrowsNPE() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.setClassFieldsCache(null));
    }

    /**
     * Helper that extracts the internal cache map from {@link ReflectionUtils}.
     */
    @SuppressWarnings("unchecked")
    private static Map<Object, Collection<Field>> getInternalCache() {
        try {
            var field = ReflectionUtils.class.getDeclaredField("FIELDS_CACHE");
            field.setAccessible(true);
            var ref = (AtomicReference<Map<?, ?>>) field.get(null);
            return (Map<Object, Collection<Field>>) ref.get();
        } catch (Exception e) {
            throw new RuntimeException("Unable to access internal cache", e);
        }
    }

    /* --------------------------------------------------------------
     *  2. Empty HashMap – should be wrapped with a synchronized map
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_EmptyHashMap_WrappedSynchronously() {
        Map<Object, Collection<Field>> empty = new HashMap<>();
        ReflectionUtils.setClassFieldsCache(empty);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertNotSame(empty, internal, "Internal cache should not be the original map");
        assertTrue(internal.isEmpty(), "Internal cache must be empty");
        // The wrapper class name contains "SynchronizedMap"
        assertTrue(internal.getClass().getName().contains("SynchronizedMap"), "Internal cache should be a synchronized wrapper");
    }

    /* --------------------------------------------------------------
     *  3. Map with a concrete entry
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_MapWithEntries_ContainsSameData() throws NoSuchFieldException {
        Map<Object, Collection<Field>> source = new HashMap<>();
        String key = "sampleKey";
        // known field
        Field stringValueField = String.class.getDeclaredField("value");
        Collection<Field> value = Collections.singletonList(stringValueField);
        source.put(key, value);
        ReflectionUtils.setClassFieldsCache(source);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertTrue(internal.containsKey(key), "Internal cache must contain the supplied key");
        assertSame(value, internal.get(key), "Collection instance should be preserved");
    }

    /* --------------------------------------------------------------
     *  4. Already thread‑safe map (ConcurrentHashMap)
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_ConcurrentHashMap_NoWrapping() {
        Map<Object, Collection<Field>> concurrent = new ConcurrentHashMap<>();
        ReflectionUtils.setClassFieldsCache(concurrent);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertSame(concurrent, internal, "ConcurrentHashMap should be stored unchanged");
    }

    /* --------------------------------------------------------------
     *  5. Replacement behaviour (first then second map)
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_ReplacementBehaviour_SwapsReferences() {
        // first map with an entry
        Map<Object, Collection<Field>> first = new HashMap<>();
        first.put("firstKey", Collections.emptyList());
        // second map, empty
        Map<Object, Collection<Field>> second = new HashMap<>();
        // set first
        ReflectionUtils.setClassFieldsCache(first);
        Map<Object, Collection<Field>> afterFirst = getInternalCache();
        assertTrue(afterFirst.containsKey("firstKey"), "First map entry must be present after first set");
        // set second
        ReflectionUtils.setClassFieldsCache(second);
        Map<Object, Collection<Field>> afterSecond = getInternalCache();
        assertFalse(afterSecond.containsKey("firstKey"), "After replacement, entries from the first map must be gone");
        assertTrue(afterSecond.isEmpty(), "Second map is empty, internal cache should be empty");
    }

    /* --------------------------------------------------------------
     *  6. Already synchronized map
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_AlreadySynchronizedMap_NoAdditionalWrapping() {
        Map<Object, Collection<Field>> sync = Collections.synchronizedMap(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(sync);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertSame(sync, internal, "Already synchronized map should be stored as‑is");
    }

    /* --------------------------------------------------------------
     *  7. Unmodifiable map – should be wrapped synchronously
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_UnmodifiableMap_WrappedSynchronously() {
        Map<Object, Collection<Field>> unmod = Collections.unmodifiableMap(new HashMap<>());
        ReflectionUtils.setClassFieldsCache(unmod);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertNotSame(unmod, internal, "Unmodifiable map must be wrapped");
        assertTrue(internal.getClass().getName().contains("SynchronizedMap"), "Wrapper should be a synchronized map");
        // Verify that the wrapper still forwards to the unmodifiable map (modification throws)
        assertThrows(UnsupportedOperationException.class, () -> internal.put("k", Collections.emptyList()));
    }

    /* --------------------------------------------------------------
     *  8. Map containing null key and null value collection
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_MapWithNullKeyAndNullValue_Preserved() {
        Map<Object, Collection<Field>> map = new HashMap<>();
        map.put(null, null);
        ReflectionUtils.setClassFieldsCache(map);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertTrue(internal.containsKey(null), "Cache must contain the null key");
        assertNull(internal.get(null), "Cache must map null key to null value");
    }

    /* --------------------------------------------------------------
     *  9. Map with immutable collection value
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_MapWithImmutableCollection_ValuePreserved() {
        Map<Object, Collection<Field>> map = new HashMap<>();
        // immutable collection
        Collection<Field> immutable = Collections.emptySet();
        map.put("k", immutable);
        ReflectionUtils.setClassFieldsCache(map);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertSame(immutable, internal.get("k"), "Immutable collection instance must be preserved");
    }

    /* --------------------------------------------------------------
     * 10. LinkedHashMap preserving insertion order
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_LinkedHashMap_PreservesOrder() {
        Map<Object, Collection<Field>> linked = new LinkedHashMap<>();
        linked.put("first", Collections.emptyList());
        linked.put("second", Collections.emptyList());
        ReflectionUtils.setClassFieldsCache(linked);
        Map<Object, Collection<Field>> internal = getInternalCache();
        // Verify that the internal map is still a LinkedHashMap (or at least preserves order)
        Iterator<Object> it = internal.keySet().iterator();
        assertEquals("first", it.next());
        assertEquals("second", it.next());
    }

    /* --------------------------------------------------------------
     * 11. ConcurrentSkipListMap (thread‑safe sorted map)
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_ConcurrentSkipListMap_NoWrapping() {
        Map<Object, Collection<Field>> skipList = new ConcurrentSkipListMap<>();
        ReflectionUtils.setClassFieldsCache(skipList);
        Map<Object, Collection<Field>> internal = getInternalCache();
        assertSame(skipList, internal, "ConcurrentSkipListMap should be stored unchanged (already thread‑safe)");
    }

    /* --------------------------------------------------------------
     * 12. Sanity check – public API still works after cache replacement
     * -------------------------------------------------------------- */
    @Test
    void setClassFieldsCache_AfterReplacement_GetAllDeclaredFieldsWorks() {
        Map<Object, Collection<Field>> custom = new HashMap<>();
        ReflectionUtils.setClassFieldsCache(custom);
        // The API must still return the correct fields for Sample
        Collection<Field> fields = ReflectionUtils.getAllDeclaredFields(Sample.class);
        assertNotNull(fields);
        assertEquals(2, fields.size());
        Set<String> names = new HashSet<>();
        for (Field f : fields) {
            names.add(f.getName());
        }
        assertTrue(names.containsAll(Set.of("intField", "stringField")));
    }
}
