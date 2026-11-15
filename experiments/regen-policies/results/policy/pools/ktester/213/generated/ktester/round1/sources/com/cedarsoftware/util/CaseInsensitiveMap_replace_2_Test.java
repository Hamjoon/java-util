package com.cedarsoftware.util;

import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_replace_2_Test {

    /**
     * Reset the global cache after each test so that other tests are not affected.
     */
//     @AfterEach
    void teardownAfterEach() {
        // Restore defaults that other tests rely on
        CaseInsensitiveMap.setMaxCacheLengthString(100);
        CaseInsensitiveMap.replaceCache(new LRUCache<>(5000, LRUCache.StrategyType.THREADED));
    }

    /**
     * Data for the {@code replaceExistingKey} group.
     */
//     static Stream<Arguments> replaceExistingKeyProvider() {
//         return // first replacement – key "Foo" (any case) exists with value "oldValue"
//         Stream.// first replacement – key "Foo" (any case) exists with value "oldValue"
//         of(// second replacement – after previous call the logical key holds "newValue"
//         Arguments.of("Foo", "newValue", "oldValue", Map.of("foo", "oldValue")), Arguments.of("FOO", "anotherValue", "newValue", Map.of("foo", "newValue")));
//     }

    /**
     * Data for the {@code replaceNonExistingKey} group.
     */
//     static Stream<Arguments> replaceNonExistingKeyProvider() {
//         return // key does not exist, attempt to replace with an Integer value
//         Stream.// key does not exist, attempt to replace with an Integer value
//         of(Arguments.of("Bar", 3, null, Map.of("alpha", 1, "beta", 2)));
//     }

    /**
     * Data for the {@code replaceWithNullValue} group.
     */
//     static Stream<Arguments> replaceWithNullValueProvider() {
//         return // replace existing value with null
//         Stream.// replace existing value with null
//         of(// replace null with a concrete value
//         Arguments.of("Foo", null, "anotherValue", Map.of("foo", "anotherValue")), Arguments.of("Foo", "finalValue", null, Map.of("foo", null)));
//     }

    /**
     * Data for the {@code replaceKeyCaseNormalization} group.
     */
//     static Stream<Arguments> replaceKeyCaseNormalizationProvider() {
//         return // first replace after a put – should return null because the key was not added via replace
//         Stream.// first replace after a put – should return null because the key was not added via replace
//         of(// second replace – now the logical key holds "mixedValue"
//         Arguments.of("MiXeD", "mixedValue", null, Map.of("mixed", "orig")), Arguments.of("MIXED", "newMixed", "mixedValue", Map.of("mixed", "mixedValue")));
//     }

    /**
     * Data for the {@code replaceNonStringKey} group.
     */
//     static Stream<Arguments> replaceNonStringKeyProvider() {
//         return Stream.of(Arguments.of(42, "newAnswer", "answer", Map.of(42, "answer", 7, "seven")));
//     }

    /**
     * Data for the {@code replaceSameValue} group.
     */
//     static Stream<Arguments> replaceSameValueProvider() {
//         return Stream.of(Arguments.of("ALPHA", "value", "value", Map.of("alpha", "value")));
//     }

    /**
     * Data for the {@code replaceEmptyKey} group.
     */
//     static Stream<Arguments> replaceEmptyKeyProvider() {
//         return Stream.of(Arguments.of("", "newEmpty", "empty", Map.of("", "empty")));
//     }

    /**
     * Data for the {@code replaceKeyWithSpaces} group.
     */
//     static Stream<Arguments> replaceKeyWithSpacesProvider() {
//         return Stream.of(Arguments.of("  SPACED  ", "newSpace", "spaceVal", Map.of("  spaced  ", "spaceVal")));
//     }

    /**
     * Data for the {@code replaceUnicodeKey} group.
     */
//     static Stream<Arguments> replaceUnicodeKeyProvider() {
//         return // The map contains the German "straße". Upper‑case "STRASSE" is a different logical key.
//         Stream.// The map contains the German "straße". Upper‑case "STRASSE" is a different logical key.
//         of(Arguments.of("STRASSE", "newStreet", null, Map.of("straße", "oldStreet")));
//     }

    /**
     * Data for the {@code replaceOnUnmodifiableMap} group.
     */
//     static Stream<Arguments> replaceOnUnmodifiableMapProvider() {
//         return Stream.of(Arguments.of("foo", "newValue", UnsupportedOperationException.class, Map.of("foo", "old")));
//     }

    /**
     * Returns {@code true} if the supplied method has a {@code void}
     * return type.
     *
     * @param method the method to inspect
     * @return {@code true} if the method returns {@code void}
     */
    public static boolean returnsVoid(Method method) {
        return method != null && void.class.equals(method.getReturnType());
    }

    /**
     * Find a declared method with the given name and parameter types.
     *
     * @param clazz      the class to search
     * @param name       the method name
     * @param paramTypes the parameter types (may be empty)
     * @return the {@link Method} if found, otherwise {@code null}
     */
    public static Method findMethod(Class<?> clazz, String name, Class<?>... paramTypes) {
        try {
            return clazz.getDeclaredMethod(name, paramTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    /**
     * Determine whether a method with the given signature is present
     * on the supplied class.
     *
     * @param clazz      the class to inspect
     * @param name       the method name
     * @param paramTypes the parameter types (may be empty)
     * @return {@code true} if such a method exists
     */
    public static boolean isMethodPresent(Class<?> clazz, String name, Class<?>... paramTypes) {
        return findMethod(clazz, name, paramTypes) != null;
    }

//     @Test
//     void replaceExistingStringKey_ShouldReturnOldValue_AndUpdateEntry() {
//         // given
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("One", "Two");
//         map.put("Three", "Four");
//         // when
//         String old = map.replace("ONE", "NewValue");
//         // then
//         assertEquals("Two", old, "replace should return the previous value");
//         assertEquals("NewValue", map.get("one"), "value should be updated for any case variation");
//         assertEquals("NewValue", map.get("ONE"), "value should be updated for the exact case used in replace");
//         assertEquals(2, map.size(), "size must stay unchanged");
//     }

//     @Test
//     void replaceMissingStringKey_ShouldReturnNull_AndLeaveMapUnchanged() {
//         // given
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("Alpha", 1);
//         map.put("Beta", 2);
//         // when
//         Integer result = map.replace("gamma", 3);
//         // then
//         assertNull(result, "replace on a missing key must return null");
//         assertNull(map.get("gamma"), "map must not contain the new key");
//         assertEquals(2, map.size(), "size must remain unchanged");
//         assertEquals(1, map.get("Alpha"));
//         assertEquals(2, map.get("Beta"));
//     }

//     @Test
//     void replaceWithNullValue_ShouldReturnOldValue_AndStoreNull() {
//         // given
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("Key", "Initial");
//         // when
//         String old = map.replace("kEy", null);
//         // then
//         assertEquals("Initial", old, "replace should return the previous non‑null value");
//         assertNull(map.get("key"), "the value associated with the key must now be null");
//         assertTrue(map.containsKey("Key"), "the key must still be present after replacement with null");
//     }

//     @Test
//     void replaceWithNonStringKey_ShouldBehaveLikeRegularMap() {
//         // given
//         CaseInsensitiveMap<Integer, String> map = new CaseInsensitiveMap<>();
//         map.put(10, "Ten");
//         map.put(20, "Twenty");
//         // when
//         String old = map.replace(10, "NewTen");
//         // then
//         assertEquals("Ten", old);
//         assertEquals("NewTen", map.get(10));
//         assertEquals("Twenty", map.get(20));
//         assertEquals(2, map.size());
//     }

//     @Test
//     void replaceWhenKeyIsNull_ShouldDelegateToBackingMap() {
//         // given
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put(null, "nullValue");
//         // when
//         String old = map.replace(null, "newNullValue");
//         // then
//         assertEquals("nullValue", old);
//         assertEquals("newNullValue", map.get(null));
//     }

//     @SuppressWarnings("unchecked")
//     private <K, V> CaseInsensitiveMap<K, V> createMap(Map<K, V> source) {
//         // The public API does not expose a constructor that takes a source map,
//         // but the class determines the backing map via the protected method
//         // determineBackingMap.  For testing we can simply instantiate the map
//         // and then putAll the source entries – this yields the same behaviour.
//         CaseInsensitiveMap<K, V> ciMap = new CaseInsensitiveMap<>();
//         ciMap.putAll(source);
//         return ciMap;
//     }

//     @ParameterizedTest(name = "[{index}] replaceExistingKey key=''{0}'' value=''{1}''")
//     @MethodSource("replaceExistingKeyProvider")
//     void replaceExistingKey(String key, String newValue, String expectedOld, Map<String, String> initial) {
//         CaseInsensitiveMap<String, String> map = createMap(initial);
//         String old = map.replace(key, newValue);
//         assertEquals(expectedOld, old, "replace should return the previous value");
//         // after replacement the map must contain the new value under any case variation
//         assertEquals(newValue, map.get(key), "new value must be stored for the supplied case");
//         assertEquals(newValue, map.get(key.toLowerCase()), "new value must be accessible with lower‑case key");
//         assertEquals(initial.size(), map.size(), "size must stay unchanged");
//     }

//     @ParameterizedTest(name = "[{index}] replaceWithNullValue key=''{0}''")
//     @MethodSource("replaceWithNullValueProvider")
//     void replaceWithNullValue(String key, String newValue, String expectedOld, Map<String, String> initial) {
//         CaseInsensitiveMap<String, String> map = createMap(initial);
//         String old = map.replace(key, newValue);
//         assertEquals(expectedOld, old, "replace should return the previous value (may be null)");
//         assertEquals(newValue, map.get(key), "new value (or null) must be stored");
//         assertTrue(map.containsKey(key), "key must still be present after replacement");
//     }

//     @Test
//     void replaceWithNullKey_ShouldThrowNullPointerException() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put(null, "any");
//         // According to the specification convertKey does not accept null
//         assertThrows(NullPointerException.class, () -> map.replace(null, "new"), "replace(null, …) must throw NullPointerException");
//     }

//     @ParameterizedTest(name = "[{index}] replaceKeyCaseNormalization key=''{0}''")
//     @MethodSource("replaceKeyCaseNormalizationProvider")
//     void replaceKeyCaseNormalization(String key, String newValue, String expectedOld, Map<String, String> initial) {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         // first put the initial entry using put (case‑preserving)
//         initial.forEach(map::put);
//         String old = map.replace(key, newValue);
//         assertEquals(expectedOld, old, "replace should return expected previous value");
//         assertEquals(newValue, map.get(key), "value must be updated");
//     }

//     @ParameterizedTest(name = "[{index}] replaceNonStringKey key=''{0}''")
//     @MethodSource("replaceNonStringKeyProvider")
//     void replaceNonStringKey(Integer key, String newValue, String expectedOld, Map<Integer, String> initial) {
//         CaseInsensitiveMap<Integer, String> map = createMap(initial);
//         String old = map.replace(key, newValue);
//         assertEquals(expectedOld, old, "replace should behave like a normal map for non‑String keys");
//         assertEquals(newValue, map.get(key), "new value must be stored");
//         assertEquals(initial.size(), map.size(), "size unchanged");
//     }

//     @ParameterizedTest(name = "[{index}] replaceSameValue key=''{0}''")
//     @MethodSource("replaceSameValueProvider")
//     void replaceSameValue(String key, String value, String expectedOld, Map<String, String> initial) {
//         CaseInsensitiveMap<String, String> map = createMap(initial);
//         String old = map.replace(key, value);
//         assertEquals(expectedOld, old, "replace should return the existing value");
//         assertEquals(value, map.get(key), "value remains unchanged");
//         assertEquals(initial.size(), map.size(), "size unchanged");
//     }

//     @ParameterizedTest(name = "[{index}] replaceEmptyKey")
//     @MethodSource("replaceEmptyKeyProvider")
//     void replaceEmptyKey(String key, String newValue, String expectedOld, Map<String, String> initial) {
//         CaseInsensitiveMap<String, String> map = createMap(initial);
//         String old = map.replace(key, newValue);
//         assertEquals(expectedOld, old);
//         assertEquals(newValue, map.get(key));
//     }

//     @ParameterizedTest(name = "[{index}] replaceKeyWithSpaces")
//     @MethodSource("replaceKeyWithSpacesProvider")
//     void replaceKeyWithSpaces(String key, String newValue, String expectedOld, Map<String, String> initial) {
//         CaseInsensitiveMap<String, String> map = createMap(initial);
//         String old = map.replace(key, newValue);
//         assertEquals(expectedOld, old);
//         assertEquals(newValue, map.get(key));
//     }

//     @ParameterizedTest(name = "[{index}] replaceUnicodeKey")
//     @MethodSource("replaceUnicodeKeyProvider")
//     void replaceUnicodeKey(String key, String newValue, String expectedOld, Map<String, String> initial) {
//         CaseInsensitiveMap<String, String> map = createMap(initial);
//         String old = map.replace(key, newValue);
//         assertEquals(expectedOld, old, "different Unicode representations are treated as distinct keys");
//         // map must remain unchanged
//         assertEquals(initial.get("straße"), map.get("straße"));
//         assertNull(map.get(key), "new key should not be added");
//     }

//     @ParameterizedTest(name = "[{index}] replaceOnUnmodifiableMap")
//     @MethodSource("replaceOnUnmodifiableMapProvider")
//     void replaceOnUnmodifiableMap(String key, String newValue, Class<? extends Exception> expectedException, Map<String, String> initial) {
//         // Create an unmodifiable backing map and wrap it in a CaseInsensitiveMap.
//         Map<String, String> unmod = Collections.unmodifiableMap(new HashMap<>(initial));
//         // The public API does not expose a constructor that accepts a backing map,
//         // so we use reflection to inject it.
//         CaseInsensitiveMap<String, String> ciMap = new CaseInsensitiveMap<>();
//         try {
//             java.lang.reflect.Field f = CaseInsensitiveMap.class.getDeclaredField("map");
//             f.setAccessible(true);
//             f.set(ciMap, unmod);
//         } catch (Exception e) {
//             fail("Failed to inject unmodifiable backing map via reflection", e);
//         }
//         assertThrows(expectedException, () -> ciMap.replace(key, newValue), "replace should propagate the exception from the underlying map");
//     }
}