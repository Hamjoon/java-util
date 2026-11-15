package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@ExtendWith(MockitoExtension.class)
class CompactMap_entrySet_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Create a CompactMap with a tiny compactSize so we can test the
        // transition from empty → single → compact array → backing map.
        map = new CompactMap<String, Integer>() {

            @Override
            protected int compactSize() {
                // small enough for our tests
                return 4;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                return new HashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                return "singleKey";
            }
        };
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a fresh map.
        map.clear();
    }

    /**
     * Helper to create a mutable {@link Map.Entry}.
     */
    private static <K, V> Map.Entry<K, V> entry(K k, V v) {
        return new AbstractMap.SimpleEntry<>(k, v);
    }

    private static Stream<Arguments> provideContainsCases() {
        return // entry key exists and stored value (100) is non‑null and equals the entry's value
        Stream.// entry key exists and stored value (100) is non‑null and equals the entry's value
        of(// entry key exists with a null value in the map
        Arguments.of(new AbstractMap.SimpleEntry<>("key1", 100), true), // entry key does not exist in the map
        Arguments.of(new AbstractMap.SimpleEntry<>("keyNull", null), true), // parameter is not a Map.Entry instance
        Arguments.of(new AbstractMap.SimpleEntry<>("missing", 999), false), Arguments.of("just a string", false));
    }

    /**
     * Returns {@code true} if the supplied method returns {@code void}.
     */
    public static boolean returnsVoid(Method method) {
        return method.getReturnType() == void.class;
    }

    /**
     * Find a method with the given name and parameter types on the supplied class
     * (including its super‑classes).  Returns {@code null} if no such method exists.
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                Method m = current.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // continue searching in the superclass
                current = current.getSuperclass();
            }
        }
        return null;
    }

    /**
     * Convenience wrapper around {@link #findMethod(Class, String, Class[])}.
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... parameterTypes) {
        return findMethod(clazz, name, parameterTypes) != null;
    }

    public static boolean isPublic(Method method) {
        return Modifier.isPublic(method.getModifiers());
    }

    public static boolean isStatic(Method method) {
        return Modifier.isStatic(method.getModifiers());
    }

    public static boolean isAbstract(Method method) {
        return Modifier.isAbstract(method.getModifiers());
    }

    public static boolean isSynthetic(Method method) {
        return method.isSynthetic();
    }

    public static boolean isBridge(Method method) {
        return method.isBridge();
    }

    public static boolean isDefaultMethod(Method method) {
        return method.isDefault();
    }

    public static boolean isPublicStatic(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) && Modifier.isStatic(mods);
    }

    public static boolean isPublicStaticFinal(Method method) {
        int mods = method.getModifiers();
        return Modifier.isPublic(mods) && Modifier.isStatic(mods) && Modifier.isFinal(mods);
    }

    @Test
    void entrySetSizeAndContains() {
        // empty map
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertEquals(0, es.size());
        assertFalse(es.contains(entry("a", 1)));
        // add a few entries
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // get a fresh view
        es = map.entrySet();
        assertEquals(3, es.size());
        // present entries
        assertTrue(es.contains(entry("a", 1)));
        assertTrue(es.contains(entry("b", 2)));
        assertTrue(es.contains(entry("c", 3)));
        // wrong value – should be false
        assertFalse(es.contains(entry("a", 99)));
        // non‑existent key
        assertFalse(es.contains(entry("d", 4)));
    }

    @Test
    void entrySetRemove() {
        map.put("x", 10);
        map.put("y", 20);
        map.put("z", 30);
        assertEquals(3, map.size());
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        // remove an existing entry via the set
        assertTrue(es.remove(entry("y", 20)));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("y"));
        assertFalse(es.contains(entry("y", 20)));
        // attempt to remove a non‑present entry – should return false
        assertFalse(es.remove(entry("y", 20)));
        assertEquals(2, map.size());
    }

    @Test
    void entrySetRemoveAll() {
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        map.put("four", 4);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Collection<Map.Entry<String, Integer>> toRemove = // not present – should be ignored
        Arrays.// not present – should be ignored
        asList(// not present – should be ignored
        entry("two", 2), // not present – should be ignored
        entry("four", 4), entry("absent", 99));
        assertTrue(es.removeAll(toRemove));
        assertEquals(2, map.size());
        assertFalse(map.containsKey("two"));
        assertFalse(map.containsKey("four"));
        assertTrue(map.containsKey("one"));
        assertTrue(map.containsKey("three"));
    }

    @Test
    void entrySetRetainAll() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        map.put("e", 5);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        // Keep only a subset of entries
        Collection<Map.Entry<String, Integer>> toKeep = // ignored because key not present
        Arrays.// ignored because key not present
        asList(// ignored because key not present
        entry("b", 2), // ignored because key not present
        entry("d", 4), entry("nonexistent", 99));
        assertTrue(es.retainAll(toKeep));
        assertEquals(2, map.size());
        assertTrue(map.containsKey("b"));
        assertTrue(map.containsKey("d"));
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("c"));
        assertFalse(map.containsKey("e"));
    }

    @Test
    void entrySetClear() {
        map.put("k1", 100);
        map.put("k2", 200);
        assertFalse(map.isEmpty());
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        es.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, es.size());
    }

    @Test
    void entrySetIteratorRemove() {
        map.put("alpha", 1);
        map.put("beta", 2);
        map.put("gamma", 3);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Integer> e = it.next();
            if ("beta".equals(e.getKey())) {
                // use iterator's remove
                it.remove();
            }
        }
        assertEquals(2, map.size());
        assertFalse(map.containsKey("beta"));
        assertTrue(map.containsKey("alpha"));
        assertTrue(map.containsKey("gamma"));
    }

    // ---------- contains ----------
    @ParameterizedTest(name = "{index}: contains({0}) → {1}")
    @MethodSource("provideContainsCases")
    void testContains(Object input, boolean expected) {
        // Populate map with the two entries required for the tests
        map.put("key1", 100);
        map.put("keyNull", null);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        assertEquals(expected, es.contains(input));
    }

    // ---------- remove (non‑entry argument) ----------
    @Test
    void testRemoveNonEntryArgument() {
        map.put("key1", 100);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        // Passing a non‑Map.Entry should return false and not modify the map
        assertFalse(es.remove("not an entry"));
        assertEquals(1, map.size());
        assertTrue(map.containsKey("key1"));
    }

    // ---------- removeAll with empty collection ----------
    @Test
    void testRemoveAllEmptyCollection() {
        map.put("key1", 100);
        map.put("key2", 200);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        boolean changed = es.removeAll(Collections.emptyList());
        assertFalse(changed, "removeAll on an empty collection should return false");
        assertEquals(2, map.size(), "Map size must stay unchanged");
    }

    // ---------- retainAll exact match ----------
    @Test
    void testRetainAllExactMatchNoChange() {
        map.put("key1", 100);
        map.put("key2", 200);
        map.put("keyNull", null);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Collection<Map.Entry<String, Integer>> exact = Arrays.asList(entry("key1", 100), entry("key2", 200), entry("keyNull", null));
        boolean changed = es.retainAll(exact);
        assertFalse(changed, "retainAll with an exact match should return false");
        assertEquals(3, map.size(), "Map size must remain unchanged");
    }

    // ---------- iterator basic ----------
    @Test
    void testIteratorBasicBehaviour() {
        map.put("key1", 100);
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = es.iterator();
        assertTrue(it.hasNext(), "Iterator should have next before first call");
        Map.Entry<String, Integer> e = it.next();
        assertEquals("key1", e.getKey());
        assertEquals(100, e.getValue());
        assertFalse(it.hasNext(), "Iterator should report no more elements after consuming all");
        // Next on exhausted iterator must throw NoSuchElementException
        assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    void testIteratorNoSuchElementExceptionOnEmptyMap() {
        // map is empty
        Set<Map.Entry<String, Integer>> es = map.entrySet();
        Iterator<Map.Entry<String, Integer>> it = es.iterator();
        assertFalse(it.hasNext());
        assertThrows(NoSuchElementException.class, it::next);
    }

    // ---------- iterator remove illegal state ----------
    @Test
    void testIteratorRemoveIllegalStateBeforeNext() {
        map.put("a", 1);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // remove before any next() should fail
        assertThrows(IllegalStateException.class, it::remove);
    }

    @Test
    void testIteratorRemoveCalledTwice() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
        // first next + remove – legal
        assertTrue(it.hasNext());
        it.next();
        it.remove();
        // second remove without another next – illegal
        assertThrows(IllegalStateException.class, it::remove);
    }
}
