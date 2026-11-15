package com.cedarsoftware.util;

import java.lang.reflect.Method;

// @ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_merge_Test {

    private CaseInsensitiveMap<String, String> map;

//     @BeforeEach
    void setupBeforeEach() {
        map = new CaseInsensitiveMap<>();
    }

//     @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared after each test to avoid cross‑test contamination
        map.clear();
    }

    /**
     * Determines whether the given method returns {@code void}.
     *
     * @param method the method to inspect; never {@code null}
     * @return {@code true} if the method's return type is {@code void},
     *         {@code false} otherwise
     */
    public static boolean returnsVoid(Method method) {
        if (method == null) {
            throw new IllegalArgumentException("Method must not be null");
        }
        return method.getReturnType() == void.class;
    }

    /**
     * When the key is not present, {@code merge} should simply add the entry and return the supplied value.
     */
//     @Test
//     void mergeAddsNewEntryWhenKeyAbsent() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         String returned = map.merge("NewKey", "Initial", (oldVal, newVal) -> {
//             fail("Remapping function must not be invoked when the key is absent");
//             return null;
//         });
//         assertEquals("Initial", returned, "Returned value should be the supplied value");
//         assertEquals("Initial", map.get("newkey"), "Map should contain the new entry (case‑insensitive)");
//         assertEquals(1, map.size(), "Map size should be 1 after adding a new entry");
//     }

    /**
     * When the key is already present, the remapping function must be invoked with the
     * existing value and the supplied value, and its result becomes the new mapping.
     */
//     @Test
//     void mergeUsesRemappingFunctionWhenKeyPresent() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("ExistingKey", "OldValue");
//         BiFunction<String, String, String> remapper = (oldVal, newVal) -> oldVal + "+" + newVal;
//         String result = map.merge("existingkey", "NewValue", remapper);
//         assertEquals("OldValue+NewValue", result, "Result should be the value returned by the remapping function");
//         assertEquals("OldValue+NewValue", map.get("EXISTINGKEY"), "Map should store the value returned by the remapping function");
//         assertEquals(1, map.size(), "Map size should remain 1 after merging an existing key");
//     }

    /**
     * Verify that the remapping function receives only the old and new values (no key)
     * and that the method works case‑insensitively.
     */
//     @Test
//     void mergeInvokesRemappingFunctionWithCorrectValuesAndIgnoresKeyCase(@Mock BiFunction<String, String, String> mockRemapper) {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         // Arrange
//         map.put("CaseKey", "First");
//         when(mockRemapper.apply("First", "Second")).thenReturn("Combined");
//         // Act
//         String returned = map.merge("cAsEkEy", "Second", mockRemapper);
//         // Assert
//         assertEquals("Combined", returned, "Returned value must be whatever the remapping function returns");
//         assertEquals("Combined", map.get("casekey"), "Stored value must be the result of the remapping function");
//         verify(mockRemapper, times(1)).apply("First", "Second");
//         verifyNoMoreInteractions(mockRemapper);
//     }

    /**
     * If the remapping function returns {@code null}, the entry should be removed from the map.
     */
//     @Test
//     void mergeRemappingFunctionReturnsNullRemovesEntry() {
//         CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
//         map.put("ToRemove", "Value");
//         String result = map.merge("toremove", "Ignored", (oldVal, newVal) -> null);
//         assertNull(result, "Result should be null when the remapping function returns null");
//         assertFalse(map.containsKey("ToRemove"), "Key should be removed from the map when the remapping function returns null");
//         assertEquals(0, map.size(), "Map should be empty after removal");
//     }

    /**
     * Basic functionality – key absent, null remapping function **
     */
//     @Test
//     void mergeAddsNewEntryWhenKeyAbsentAndRemappingFunctionIsNull() {
//         // key "apple" does not exist, remappingFunction == null (allowed when key absent)
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         Integer returned = map.merge("apple", 10, null);
//         assertEquals(10, returned);
//         assertEquals(10, map.get("APPLE"));
//         assertEquals(1, map.size());
//     }

    /**
     * Basic functionality – key present, sum remapping **
     */
//     @Test
//     void mergeAddsWhenKeyPresentAndRemappingAddsValues() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("banana", 10);
//         Integer result = map.merge("banana", 5, (oldV, newV) -> oldV + newV);
//         assertEquals(15, result);
//         assertEquals(15, map.get("BANANA"));
//         assertEquals(1, map.size());
//     }

    /**
     * Basic functionality – remapping returns null (removal) **
     */
//     @Test
//     void mergeRemappingReturnsNullRemovesEntry() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("banana", 20);
//         Integer result = map.merge("banana", 3, (oldV, newV) -> null);
//         assertNull(result);
//         assertFalse(map.containsKey("banana"));
//         assertEquals(0, map.size());
//     }

    /**
     * Basic functionality – remapping throws exception **
     */
//     @Test
//     void mergeRemappingThrowsExceptionPropagatesAndLeavesMapUnchanged() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("cherry", 7);
//         IllegalStateException ex = assertThrows(IllegalStateException.class, () -> map.merge("cherry", 2, (oldV, newV) -> {
//             throw new IllegalStateException("boom");
//         }));
//         assertEquals("boom", ex.getMessage());
//         // map must stay unchanged
//         assertEquals(7, map.get("CHERRY"));
//         assertEquals(1, map.size());
//     }

    /**
     * Basic functionality – null key allowed by default backing map **
     */
//     @Test
//     void mergeWithNullKeyOnDefaultBackingMap() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         Integer result = map.merge(null, 1, null);
//         assertEquals(1, result);
//         assertTrue(map.containsKey(null));
//         assertEquals(1, map.get(null));
//     }

    /**
     * Basic functionality – null value for missing key **
     */
//     @Test
//     void mergeNullValueForMissingKeyStoresNull() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         Integer result = map.merge("date", null, null);
//         assertNull(result);
//         assertTrue(map.containsKey("date"));
//         assertNull(map.get("date"));
//     }

    /**
     * Basic functionality – existing null value, remapping supplies new **
     */
//     @Test
//     void mergeWhenExistingValueIsNullAndRemappingSuppliesValue() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("date", null);
//         Integer result = map.merge("date", 5, (oldV, newV) -> oldV == null ? newV : oldV + newV);
//         assertEquals(5, result);
//         assertEquals(5, map.get("date"));
//     }

    /**
     * Case‑insensitivity – numeric addition **
     */
//     @Test
//     void mergeCaseInsensitiveStringKeyNumericAddition() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("Key", 20);
//         Integer result = map.merge("key", 5, (oldV, newV) -> oldV + newV);
//         assertEquals(25, result);
//         assertEquals(25, map.get("KEY"));
//     }

    /**
     * Case‑insensitivity – string concatenation **
     */
//     @Test
//     void mergeCaseInsensitiveStringKeyStringConcat() {
//         CaseInsensitiveMap<String, String> strMap = new CaseInsensitiveMap<>();
//         strMap.put("test", "old");
//         String result = strMap.merge("TeSt", "new", (oldV, newV) -> oldV + newV);
//         assertEquals("oldnew", result);
//         assertEquals("oldnew", strMap.get("TEST"));
//     }

    /**
     * Null remappingFunction should throw NPE when key exists **
     */
//     @Test
//     void mergeNullRemappingFunctionWithExistingKeyThrowsNPE() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         map.put("alpha", 3);
//         assertThrows(NullPointerException.class, () -> map.merge("alpha", 4, null));
//         // original entry unchanged
//         assertEquals(3, map.get("alpha"));
//     }

    /**
     * Null remappingFunction should throw NPE when key absent **
     */
//     @Test
//     void mergeNullRemappingFunctionWithAbsentKeyThrowsNPE() {
//         CaseInsensitiveMap<String, Integer> map = new CaseInsensitiveMap<>();
//         assertThrows(NullPointerException.class, () -> map.merge("beta", 7, null));
//         assertFalse(map.containsKey("beta"));
//     }

    /**
     * Non‑String key handling – first insertion with null remapper **
     */
//     @Test
//     void mergeNonStringKeyFirstInsert() {
//         CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
//         String result = intMap.merge(42, "first", null);
//         assertEquals("first", result);
//         assertEquals("first", intMap.get(42));
//     }

    /**
     * Non‑String key handling – subsequent merge uses remapper **
     */
//     @Test
//     void mergeNonStringKeySubsequentUsesRemapper() {
//         CaseInsensitiveMap<Integer, String> intMap = new CaseInsensitiveMap<>();
//         intMap.put(42, "first");
//         String result = intMap.merge(42, "second", (oldV, newV) -> oldV + ":" + newV);
//         assertEquals("first:second", result);
//         assertEquals("first:second", intMap.get(42));
//     }

    /**
     * Remapping function returns unchanged old value **
     */
//     @Test
//     void mergeRemappingReturnsUnchangedOldValue() {
//         CaseInsensitiveMap<String, Integer> sameMap = new CaseInsensitiveMap<>();
//         sameMap.put("same", 100);
//         Integer result = sameMap.merge("same", 0, (oldV, newV) -> oldV);
//         assertEquals(100, result);
//         assertEquals(100, sameMap.get("same"));
//         assertEquals(1, sameMap.size());
//     }

    /**
     * Both old and new values null, remapping supplies concrete value **
     */
//     @Test
//     void mergeBothNullValuesRemappingSuppliesValue() {
//         CaseInsensitiveMap<String, String> nullMap = new CaseInsensitiveMap<>();
//         nullMap.put("nullKey", null);
//         String result = nullMap.merge("nullKey", null, (oldV, newV) -> "filled");
//         assertEquals("filled", result);
//         assertEquals("filled", nullMap.get("nullKey"));
//     }

    /**
     * Unmodifiable backing map – insertion attempt throws **
     */
//     @Test
//     void mergeOnUnmodifiableBackingMapThrowsUnsupportedOperationException() {
//         Map<String, Integer> unmod = Collections.unmodifiableMap(new java.util.HashMap<>());
//         CaseInsensitiveMap<String, Integer> unmodMap = new CaseInsensitiveMap<>(unmod);
//         assertThrows(UnsupportedOperationException.class, () -> unmodMap.merge("foo", 42, null));
//     }

    /**
     * Unmodifiable backing map – remapping function also throws **
     */
//     @Test
//     void mergeOnUnmodifiableBackingMapWithRemapperThrowsUnsupportedOperationException() {
//         Map<String, Integer> source = new java.util.HashMap<>();
//         source.put("bar", 50);
//         CaseInsensitiveMap<String, Integer> unmodMap = new CaseInsensitiveMap<>(Collections.unmodifiableMap(source));
//         assertThrows(UnsupportedOperationException.class, () -> unmodMap.merge("bar", 100, (oldV, newV) -> oldV + newV));
//     }

    /**
     * ConcurrentHashMap backing – null key throws NPE **
     */
//     @Test
//     void mergeOnConcurrentHashMapWithNullKeyThrowsNPE() {
//         CaseInsensitiveMap<String, Integer> chMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
//         assertThrows(NullPointerException.class, () -> chMap.merge(null, 1, null));
//     }

    /**
     * ConcurrentHashMap backing – null value for missing key throws NPE **
     */
//     @Test
//     void mergeOnConcurrentHashMapWithNullValueForMissingKeyThrowsNPE() {
//         CaseInsensitiveMap<String, Integer> chMap = new CaseInsensitiveMap<>(new ConcurrentHashMap<>());
//         assertThrows(NullPointerException.class, () -> chMap.merge("cKey", null, null));
//     }

    /**
     * ConcurrentHashMap backing – existing key, remapping returns null (removal allowed) **
     */
//     @Test
//     void mergeOnConcurrentHashMapRemappingReturnsNullRemovesEntry() {
//         ConcurrentHashMap<String, Integer> backing = new ConcurrentHashMap<>();
//         backing.put("cKey", 99);
//         CaseInsensitiveMap<String, Integer> chMap = new CaseInsensitiveMap<>(backing);
//         Integer result = chMap.merge("cKey", 0, (oldV, newV) -> null);
//         assertNull(result);
//         assertFalse(chMap.containsKey("cKey"));
//     }

    /**
     * ConcurrentHashMap backing – existing key, new value null triggers NPE before remapper **
     */
//     @Test
//     void mergeOnConcurrentHashMapWithNullNewValueThrowsNPEBeforeRemap() {
//         ConcurrentHashMap<String, Integer> backing = new ConcurrentHashMap<>();
//         backing.put("cKey", 10);
//         CaseInsensitiveMap<String, Integer> chMap = new CaseInsensitiveMap<>(backing);
//         assertThrows(NullPointerException.class, () -> chMap.merge("cKey", null, (oldV, newV) -> 123));
//         // original entry must stay unchanged
//         assertEquals(10, chMap.get("cKey"));
//     }
}