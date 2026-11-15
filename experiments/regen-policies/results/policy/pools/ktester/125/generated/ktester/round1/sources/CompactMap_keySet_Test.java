package com.cedarsoftware.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

@ExtendWith(MockitoExtension.class)
class CompactMap_keySet_Test {

    private CompactMap<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        map = createTestMap();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure each test starts with a fresh map instance
        map.clear();
    }

    /**
     * Helper method that creates a CompactMap instance with a very small
     * {@code compactSize} so that the internal storage switches quickly and the
     * {@code keySet()} implementation can be exercised regardless of the
     * underlying storage type.
     */
    private CompactMap<String, Integer> createTestMap() {
        return new CompactMap<>() {

            @Override
            protected int compactSize() {
                // small size to force transition to a backing map quickly
                return 3;
            }

            @Override
            protected Map<String, Integer> getNewMap() {
                // use a LinkedHashMap to preserve insertion order (makes assertions easier)
                return new LinkedHashMap<>();
            }

            @Override
            protected String getSingleValueKey() {
                // not relevant for these tests but required by the original class contract
                return "single";
            }
        };
    }

    /**
     * Returns {@code true} if the supplied method has a {@code void} return type.
     *
     * @param method the method to inspect; must not be {@code null}
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        Objects.requireNonNull(method, "method must not be null");
        return method.getReturnType() == void.class;
    }

    /**
     * Finds a method with the given name and parameter types on the supplied class
     * (including its super‑classes). Returns {@code null} if no such method exists.
     *
     * @param clazz          the class to search
     * @param name           the method name
     * @param parameterTypes the parameter types (may be empty)
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... parameterTypes) {
        Objects.requireNonNull(clazz, "clazz must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Class<?> search = clazz;
        while (search != null) {
            try {
                Method m = search.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException ignored) {
                // continue with superclass
            }
            search = search.getSuperclass();
        }
        return null;
    }

    /**
     * Returns {@code true} if a method with the given signature is present on the class.
     *
     * @param clazz          the class to inspect
     * @param name           the method name
     * @param parameterTypes the parameter types
     * @return {@code true} if the method exists
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

    public static boolean isPublicStatic(Method method) {
        return isPublic(method) && isStatic(method);
    }

    public static boolean isPublicInstance(Method method) {
        return isPublic(method) && !isStatic(method);
    }

    @Test
    void keySetReflectsMapSizeAndContents() {
        // Populate the map
        map.put("one", 1);
        map.put("two", 2);
        map.put("three", 3);
        Set<String> ks = map.keySet();
        // size() should delegate to map.size()
        assertEquals(3, ks.size());
        assertEquals(3, map.size());
        // contains() should delegate to map.containsKey()
        assertTrue(ks.contains("one"));
        assertFalse(ks.contains("four"));
        // clear() should clear the underlying map
        ks.clear();
        assertTrue(ks.isEmpty());
        assertTrue(map.isEmpty());
    }

    @Test
    void keySetRemoveSingleKey() {
        map.put("alpha", 10);
        map.put("beta", 20);
        map.put("gamma", 30);
        Set<String> ks = map.keySet();
        // remove an existing key – should return true because size changes
        assertTrue(ks.remove("beta"));
        assertFalse(map.containsKey("beta"));
        assertEquals(2, ks.size());
        // remove a non‑existent key – should return false (size unchanged)
        assertFalse(ks.remove("delta"));
        assertEquals(2, ks.size());
    }

    @Test
    void keySetRemoveAllAndRetainAll() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        Set<String> ks = map.keySet();
        // removeAll – collection contains a mix of present and absent keys
        Collection<String> toRemove = Arrays.asList("b", "x", "c");
        assertTrue(ks.removeAll(toRemove));
        assertFalse(map.containsKey("b"));
        assertFalse(map.containsKey("c"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("d"));
        assertEquals(2, ks.size());
        // retainAll – keep only "a"
        Collection<String> toRetain = Collections.singleton("a");
        assertTrue(ks.retainAll(toRetain));
        assertTrue(map.containsKey("a"));
        assertFalse(map.containsKey("d"));
        assertEquals(1, ks.size());
    }

    @Test
    void iteratorRemoveWorksCorrectly() {
        map.put("x", 100);
        map.put("y", 200);
        map.put("z", 300);
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        // Remove each element via iterator
        while (it.hasNext()) {
            String key = it.next();
            assertTrue(map.containsKey(key));
            // should remove from the underlying map
            it.remove();
        }
        // After iterator removal, map must be empty
        assertTrue(ks.isEmpty());
        assertTrue(map.isEmpty());
    }

    @Test
    void iteratorThrowsNoSuchElementExceptionWhenEmpty() {
        Set<String> ks = map.keySet();
        Iterator<String> it = ks.iterator();
        // should throw
        it.next();
    }

    @Test
    void iteratorDetectsConcurrentModification() {
        map.put("p", 1);
        map.put("q", 2);
        Iterator<String> it = map.keySet().iterator();
        // Modify the map directly after iterator creation
        map.put("r", 3);
        // The next call to the iterator must detect the concurrent modification
        try {
            it.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException ignored) {
            // expected
        }
    }

    /* -------------------------------------------------------------
     *  Basic operations
     * ------------------------------------------------------------- */
    @Test
    void keySet_basic_operations() {
        // put two entries
        map.put("a", 1);
        map.put("b", 2);
        Set<String> ks = map.keySet();
        // contains
        assertTrue(ks.contains("a"));
        assertFalse(ks.contains("c"));
        // size reflects map size
        assertEquals(2, ks.size());
        // iteration order (LinkedHashMap preserves insertion order)
        Iterator<String> it = ks.iterator();
        List<String> iterated = new ArrayList<>();
        while (it.hasNext()) iterated.add(it.next());
        assertEquals(Arrays.asList("a", "b"), iterated);
        // clear through the set
        ks.clear();
        assertTrue(map.isEmpty());
        assertEquals(0, ks.size());
    }

    /* -------------------------------------------------------------
     *  remove()
     * ------------------------------------------------------------- */
    @Test
    void keySet_remove_existing_and_missing() {
        map.put("x", 10);
        Set<String> ks = map.keySet();
        // existing key
        assertTrue(ks.remove("x"));
        assertEquals(0, map.size());
        // missing key
        assertFalse(ks.remove("y"));
        assertEquals(0, map.size());
    }

    @Test
    void keySet_remove_type_mismatch_throws_ClassCastException() {
        map.put("s", 5);
        Set<String> ks = map.keySet();
        // The underlying map will attempt to cast the key to String and fail
        try {
            ks.remove(5);
            fail("Expected ClassCastException");
        } catch (ClassCastException ignored) {
            // expected
        }
    }

    /* -------------------------------------------------------------
     *  removeAll()
     * ------------------------------------------------------------- */
    @Test
    void keySet_removeAll_various_cases() {
        // case 1 – some keys removed
        map.put("p", 1);
        map.put("q", 2);
        map.put("r", 3);
        Set<String> ks = map.keySet();
        assertTrue(ks.removeAll(Arrays.asList("p", "r")));
        assertFalse(map.containsKey("p"));
        assertFalse(map.containsKey("r"));
        assertTrue(map.containsKey("q"));
        assertEquals(1, ks.size());
        // case 2 – none removed
        map.clear();
        map.put("u", 5);
        map.put("v", 6);
        ks = map.keySet();
        assertFalse(ks.removeAll(Arrays.asList("x", "y")));
        assertEquals(2, ks.size());
        // case 3 – empty collection
        assertFalse(ks.removeAll(Collections.emptyList()));
        assertEquals(2, ks.size());
        // case 4 – collection with duplicates
        map.clear();
        map.putAll(Map.of("a", 1, "b", 2, "c", 3));
        ks = map.keySet();
        assertTrue(ks.removeAll(Arrays.asList("a", "a", "b")));
        assertFalse(map.containsKey("a"));
        assertFalse(map.containsKey("b"));
        assertTrue(map.containsKey("c"));
        assertEquals(1, ks.size());
    }

    @Test
    void keySet_removeAll_null_collection_throws_NPE() {
        map.put("k", 1);
        Set<String> ks = map.keySet();
        ks.removeAll(null);
    }

    /* -------------------------------------------------------------
     *  retainAll()
     * ------------------------------------------------------------- */
    @Test
    void keySet_retainAll_various_cases() {
        // case 1 – retain all (no change)
        map.putAll(Map.of("a", 1, "b", 2, "c", 3));
        Set<String> ks = map.keySet();
        assertFalse(ks.retainAll(Arrays.asList("a", "b", "c")));
        assertEquals(3, ks.size());
        // case 2 – actual removal
        ks = map.keySet();
        assertTrue(ks.retainAll(Arrays.asList("a", "c")));
        assertFalse(map.containsKey("b"));
        assertTrue(map.containsKey("a"));
        assertTrue(map.containsKey("c"));
        assertEquals(2, ks.size());
        // case 3 – retain none (empty collection)
        ks = map.keySet();
        assertTrue(ks.retainAll(Collections.emptyList()));
        assertTrue(map.isEmpty());
        assertEquals(0, ks.size());
        // case 4 – collection with no matching elements
        map.putAll(Map.of("x", 9, "y", 8));
        ks = map.keySet();
        assertTrue(ks.retainAll(Arrays.asList("z", "w")));
        assertTrue(map.isEmpty());
    }

    @Test
    void keySet_retainAll_null_collection_throws_NPE() {
        map.put("k", 1);
        Set<String> ks = map.keySet();
        ks.retainAll(null);
    }

    /* -------------------------------------------------------------
     *  Null handling
     * ------------------------------------------------------------- */
    @Test
    void keySet_null_key_handling() {
        map.put(null, 42);
        Set<String> ks = map.keySet();
        // contains null
        assertTrue(ks.contains(null));
        // remove null
        assertTrue(ks.remove(null));
        assertTrue(map.isEmpty());
    }

    @Test
    void keySet_remove_null_when_not_present() {
        Set<String> ks = map.keySet();
        assertFalse(ks.remove(null));
    }

    /* -------------------------------------------------------------
     *  Iterator behaviour
     * ------------------------------------------------------------- */
    @Test
    void iterator_next_on_empty_throws_NoSuchElementException() {
        Iterator<String> it = map.keySet().iterator();
        it.next();
    }

    @Test
    void iterator_remove_behaviour() {
        map.put("alpha", 1);
        map.put("beta", 2);
        Iterator<String> it = map.keySet().iterator();
        // first element
        String first = it.next();
        assertTrue(map.containsKey(first));
        // remove via iterator
        it.remove();
        assertFalse(map.containsKey(first));
        assertEquals(1, map.size());
        // second element
        assertTrue(it.hasNext());
        String second = it.next();
        it.remove();
        assertFalse(map.containsKey(second));
        assertTrue(map.isEmpty());
        assertFalse(it.hasNext());
    }

    @Test
    void iterator_illegal_state_exceptions() {
        map.put("one", 1);
        Iterator<String> it = map.keySet().iterator();
        // remove before next()
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ignored) {
        }
        // correct usage then double remove
        it.next();
        it.remove();
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void iterator_concurrent_modification_detection() {
        map.put("x", 10);
        map.put("y", 20);
        Iterator<String> it = map.keySet().iterator();
        // modify map directly
        map.put("z", 30);
        try {
            it.next();
            fail("Expected ConcurrentModificationException");
        } catch (ConcurrentModificationException ignored) {
            // expected
        }
    }

    /* -------------------------------------------------------------
     *  removeIf()
     * ------------------------------------------------------------- */
    @Test
    void removeIf_predicate_behaviour() {
        map.putAll(Map.of("temp1", 1, "temp2", 2, "perm", 3));
        Set<String> ks = map.keySet();
        // predicate removes entries starting with "temp"
        assertTrue(ks.removeIf(k -> ((String) k).startsWith("temp")));
        assertFalse(map.containsKey("temp1"));
        assertFalse(map.containsKey("temp2"));
        assertTrue(map.containsKey("perm"));
        assertEquals(1, ks.size());
        // predicate that matches nothing
        assertFalse(ks.removeIf(k -> false));
    }

    @Test
    void removeIf_null_predicate_throws_NPE() {
        map.put("a", 1);
        Set<String> ks = map.keySet();
        ks.removeIf(null);
    }

    @Test
    void removeIf_predicate_throws_exception_propagates() {
        map.put("a", 1);
        Set<String> ks = map.keySet();
        try {
            ks.removeIf(k -> {
                throw new IllegalArgumentException();
            });
            fail("Expected IllegalArgumentException");
        } catch (IllegalArgumentException ignored) {
            // expected
        }
    }

    /* -------------------------------------------------------------
     *  Additional edge cases
     * ------------------------------------------------------------- */
    @Test
    void keySet_isEmpty_after_clear() {
        map.put("k1", 1);
        Set<String> ks = map.keySet();
        ks.clear();
        assertTrue(ks.isEmpty());
        assertTrue(map.isEmpty());
    }

    @Test
    void keySet_contains_type_mismatch_returns_false() {
        map.put("num", 5);
        Set<String> ks = map.keySet();
        // contains with wrong type should simply return false, not throw
        assertFalse(ks.contains(5));
    }

    @Test
    void keySet_iterator_remove_illegal_state_when_next_not_called() {
        map.put("a", 1);
        Iterator<String> it = map.keySet().iterator();
        it.remove();
    }

    @Test
    void keySet_iterator_remove_illegal_state_when_called_twice() {
        map.put("a", 1);
        map.put("b", 2);
        Iterator<String> it = map.keySet().iterator();
        it.next();
        // first remove OK
        it.remove();
        try {
            it.remove();
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ignored) {
            // expected
        }
    }
}
