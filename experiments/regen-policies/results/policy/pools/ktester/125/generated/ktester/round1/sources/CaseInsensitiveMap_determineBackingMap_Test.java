package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Function;
import java.util.Map.Entry;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_determineBackingMap_Test {

    /**
     * Holds the original registry so we can restore it after each test.
     */
    private static List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> originalRegistry;

    /**
     * A thin subclass that exposes the protected {@code determineBackingMap} method for testing.
     */
    private static class TestableCaseInsensitiveMap<K, V> extends CaseInsensitiveMap<K, V> {

        Map<K, V> invokeDetermineBackingMap(Map<K, V> source) {
            return super.determineBackingMap(source);
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original registry (if any) – this uses the public API to replace it later.
        // The library does not expose a getter, so we simply remember that we will reset it to an empty list.
        originalRegistry = Collections.emptyList();
    }

    @BeforeEach
    void setupBeforeEach() {
        // Start each test with a clean (empty) registry.
        CaseInsensitiveMap.replaceRegistry(new ArrayList<>());
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the registry to its original (empty) state to avoid cross‑test contamination.
        CaseInsensitiveMap.replaceRegistry(new ArrayList<>(originalRegistry == null ? Collections.emptyList() : originalRegistry));
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original registry (including possible null) after all tests.
        setRegistryViaReflection(originalRegistry);
    }

    private static void setRegistryViaReflection(List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
            f.setAccessible(true);
            f.set(null, registry);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void determineBackingMap_throwsOnIdentityHashMap() {
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        IdentityHashMap<String, String> source = new IdentityHashMap<>();
        source.put("Key", "Value");
        assertThrows(IllegalArgumentException.class, () -> map.invokeDetermineBackingMap(source), "IdentityHashMap should cause IllegalArgumentException");
    }

    @Test
    void determineBackingMap_usesRegistryMatch() {
        // Prepare a registry entry that matches TreeMap and creates a new TreeMap instance.
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> entry = new AbstractMap.SimpleEntry<>(TreeMap.class, size -> new TreeMap<>());
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(entry);
        CaseInsensitiveMap.replaceRegistry(registry);
        // Source map is a TreeMap with a few entries.
        TreeMap<String, Integer> source = new TreeMap<>();
        source.put("One", 1);
        source.put("Two", 2);
        TestableCaseInsensitiveMap<String, Integer> map = new TestableCaseInsensitiveMap<>();
        Map<String, Integer> backing = map.invokeDetermineBackingMap(source);
        // Verify that the returned map is a *new* TreeMap containing the same entries.
        assertTrue(backing instanceof TreeMap, "Backing map should be a TreeMap");
        assertNotSame(source, backing, "Backing map should be a copy, not the original instance");
        assertEquals(source, backing, "Backing map should contain the same entries as the source");
    }

    @Test
    void determineBackingMap_defaultsToLinkedHashMapWhenNoRegistryMatch() {
        // Ensure the registry is empty – no type will match the source.
        CaseInsensitiveMap.replaceRegistry(new ArrayList<>());
        // Use a HashMap as source; it does not match any registry entry.
        HashMap<String, String> source = new HashMap<>();
        source.put("Alpha", "A");
        source.put("Beta", "B");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        Map<String, String> backing = map.invokeDetermineBackingMap(source);
        // The default backing map type is LinkedHashMap.
        assertTrue(backing instanceof LinkedHashMap, "When no registry entry matches, the default backing map should be a LinkedHashMap");
        assertNotSame(source, backing, "Backing map should be a copy, not the original instance");
        assertEquals(source, backing, "Backing map should contain the same entries as the source");
    }

    /* --------------------------------------------------------------
     * Helper methods for manipulating the private static mapRegistry
     * -------------------------------------------------------------- */
    @SuppressWarnings("unchecked")
    private static List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> getCurrentRegistry() {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("mapRegistry");
            f.setAccessible(true);
            return (List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>>) f.get(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testDetermineBackingMap_NullSource_ThrowsNullPointerException() {
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        // Passing null should cause a NPE when source.size() is accessed.
        assertThrows(NullPointerException.class, () -> map.invokeDetermineBackingMap(null), "Null source map should throw NullPointerException");
    }

    @Test
    void testDetermineBackingMap_NullRegistry_ThrowsNullPointerException() throws Exception {
        // Force the static registry to null via reflection.
        setRegistryViaReflection(null);
        HashMap<String, String> source = new HashMap<>();
        source.put("a", "1");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> map.invokeDetermineBackingMap(source), "Null mapRegistry should cause NullPointerException during iteration");
    }

    @Test
    void testDetermineBackingMap_RegistryContainsNullEntry_ThrowsNullPointerException() {
        // Registry with a single null entry.
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(null);
        CaseInsensitiveMap.replaceRegistry(registry);
        HashMap<String, String> source = new HashMap<>();
        source.put("x", "y");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> map.invokeDetermineBackingMap(source), "Iterating over a null entry should throw NullPointerException");
    }

    @Test
    void testDetermineBackingMap_EntryWithNullKey_ThrowsNullPointerException() {
        // Entry whose key (Class) is null.
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> badEntry = new AbstractMap.SimpleEntry<>(null, size -> new HashMap<>());
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(badEntry);
        CaseInsensitiveMap.replaceRegistry(registry);
        HashMap<String, String> source = new HashMap<>();
        source.put("k", "v");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> map.invokeDetermineBackingMap(source), "Entry with null key should cause NullPointerException when getKey().isInstance(...) is called");
    }

    @Test
    void testDetermineBackingMap_EntryWithNullFactory_ThrowsNullPointerException() {
        // Entry with a valid Class key but a null factory function.
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> badEntry = new AbstractMap.SimpleEntry<>(HashMap.class, null);
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(badEntry);
        CaseInsensitiveMap.replaceRegistry(registry);
        HashMap<String, String> source = new HashMap<>();
        source.put("a", "b");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> map.invokeDetermineBackingMap(source), "Entry with null factory should cause NullPointerException when factory.apply(...) is invoked");
    }

    @Test
    void testDetermineBackingMap_FactoryReturnsNull_ThrowsNullPointerException() {
        // Factory that returns null.
        Function<Integer, Map<String, String>> nullFactory = size -> null;
        Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>> entry = new AbstractMap.SimpleEntry<>(HashMap.class, nullFactory);
        List<Entry<Class<?>, Function<Integer, ? extends Map<?, ?>>>> registry = new ArrayList<>();
        registry.add(entry);
        CaseInsensitiveMap.replaceRegistry(registry);
        HashMap<String, String> source = new HashMap<>();
        source.put("k1", "v1");
        TestableCaseInsensitiveMap<String, String> map = new TestableCaseInsensitiveMap<>();
        assertThrows(NullPointerException.class, () -> map.invokeDetermineBackingMap(source), "Factory returning null should cause NullPointerException when copy(source, null) is called");
    }
}
