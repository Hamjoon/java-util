package com.cedarsoftware.util;

// @ExtendWith(MockitoExtension.class)
class CompactSet_isDefaultCompactSet_Test {

    /**
     * The underlying {@link CompactMap}.  It is deliberately package‑private so that the test
     * suite can replace it with a mock via reflection.
     */
//     CompactMap<E, Object> map = new CompactMap<>();

    /**
     * Custom subclass of {@link HashMap} to test strict class equality check.
     */
//     private static class MyHashMap<K, V> extends HashMap<K, V> {
    }

    /**
     * Replaces the private {@code map} field of the given {@link CompactSet}
     * with the supplied mock. This allows us to construct a {@code CompactSet}
     * with a custom {@link CompactMap} without relying on a public constructor.
     */
//     private static <E> void injectMockMap(CompactSet<E> set, CompactMap<E, Object> mockMap) {
//         try {
//             Field mapField = CompactSet.class.getDeclaredField("map");
//             mapField.setAccessible(true);
//             mapField.set(set, mockMap);
//         } catch (Exception e) {
//             throw new RuntimeException("Failed to inject mock map", e);
//         }
//     }

//     private void orderingCaseMismatchHelper(String ordering) {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn(ordering);
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Ordering case‑mismatch should cause isDefaultCompactSet() to return false");
//     }

    /**
     * Returns {@code true} if this {@code CompactSet} was constructed with the default
     * configuration.  The default configuration is defined by {@link CompactMap} as:
     * <ul>
     *   <li>compactSize == {@link CompactMap#DEFAULT_COMPACT_SIZE}</li>
     *   <li>case‑insensitive == {@code false}</li>
     *   <li>ordering == {@code "unordered"}</li>
     *   <li>single‑value key == {@link CompactMap#DEFAULT_SINGLE_KEY}</li>
     *   <li>backing map is exactly a {@link java.util.HashMap}</li>
     * </ul>
     *
     * @return {@code true} if the set uses the default configuration; {@code false} otherwise
     * @throws NullPointerException if the internal {@code map} reference or any of its
     *                               configuration values are {@code null}
     */
//     public boolean isDefaultCompactSet() {
//         // Defensive null‑check – the test suite explicitly verifies that a NPE is thrown
//         // when the internal map reference is {@code null}.
//         if (map == null) {
//             throw new NullPointerException("CompactSet.map is null");
//         }
//         // 1. compact size must be the default
//         if (map.compactSize() != CompactMap.DEFAULT_COMPACT_SIZE) {
//             return false;
//         }
//         // 2. must be case‑sensitive (i.e. not case‑insensitive)
//         if (map.isCaseInsensitive()) {
//             return false;
//         }
//         // 3. ordering must be exactly "unordered" (case‑sensitive, no surrounding whitespace)
//         String ordering = map.getOrdering();
//         if (!"unordered".equals(ordering)) {
//             return false;
//         }
//         // 4. the single‑value key must be the default constant
//         String singleKey = map.getSingleValueKey();
//         if (singleKey == null || !CompactMap.DEFAULT_SINGLE_KEY.equals(singleKey)) {
//             return false;
//         }
//         // 5. the backing map must be a plain HashMap – not a subclass, not an unmodifiable view,
//         //    and not {@code null}.
//         Map<E, Object> backing = map.getNewMap();
//         if (backing == null || backing.getClass() != HashMap.class) {
//             return false;
//         }
//         // All checks passed – this is the default configuration.
//         return true;
//     }

    /**
     * Returns the number of elements in this set.  Delegates to the underlying
     * {@link CompactMap}.
     *
     * @return the size of the set
     */
    public int size() {
        return map.size();
    }

//     @Test
//     void defaultCompactSetIsRecognisedAsDefault() {
//         CompactSet<String> set = new CompactSet<>();
//         assertTrue(set.isDefaultCompactSet(), "The default CompactSet should be recognised as the default configuration");
//     }

//     @Test
//     void compactSizeDiffers_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         // not the default 50
//         when(mockMap.compactSize()).thenReturn(10);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "A CompactSet with a non‑default compactSize should not be considered default");
//     }

//     @Test
//     void caseInsensitiveReturnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         // should be false for default
//         when(mockMap.isCaseInsensitive()).thenReturn(true);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "A CompactSet that is case‑insensitive should not be considered default");
//     }

//     @Test
//     void orderingDiffers_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         // not "unordered"
//         when(mockMap.getOrdering()).thenReturn("sorted");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "A CompactSet with ordering other than \"unordered\" should not be considered default");
//     }

//     @Test
//     void singleKeyDiffers_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         // not the default
//         when(mockMap.getSingleValueKey()).thenReturn("differentKey");
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "A CompactSet with a non‑default single‑value key should not be considered default");
//     }

//     @Test
//     void backingMapNotHashMap_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         // not HashMap
//         when(mockMap.getNewMap()).thenReturn(new TreeMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "A CompactSet whose backing map is not a HashMap should not be considered default");
//     }

//     @Test
//     void multipleNonDefaultSettings_firstCheckCompactSize_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         // first check fails
//         when(mockMap.compactSize()).thenReturn(30);
//         when(mockMap.isCaseInsensitive()).thenReturn(true);
//         when(mockMap.getOrdering()).thenReturn("ordered");
//         when(mockMap.getSingleValueKey()).thenReturn("uid");
//         when(mockMap.getNewMap()).thenReturn(new TreeMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Method should stop at the first failing check (compactSize) and return false");
//     }

//     @Test
//     void multipleNonDefaultSettings_secondCheckCaseInsensitive_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         // second check fails
//         when(mockMap.isCaseInsensitive()).thenReturn(true);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Method should fail at the second check (case‑insensitivity) and return false");
//     }

//     @Test
//     void nullOrdering_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         // null ordering
//         when(mockMap.getOrdering()).thenReturn(null);
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Null ordering should cause isDefaultCompactSet() to return false");
//     }

//     @ParameterizedTest(name = "ordering=\"{0}\" (case mismatch) should return false")
//     @ValueSource(strings = { "Unordered", "UNORDERED", "unordered " })
//     void orderingCaseMismatch_returnsFalse(String ordering) {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn(ordering);
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Ordering case‑mismatch should cause isDefaultCompactSet() to return false");
//     }

//     @Test
//     void nullSingleValueKey_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         // null key
//         when(mockMap.getSingleValueKey()).thenReturn(null);
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Null single‑value key should cause isDefaultCompactSet() to return false");
//     }

//     @Test
//     void emptyStringSingleValueKey_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         // empty string
//         when(mockMap.getSingleValueKey()).thenReturn("");
//         when(mockMap.getNewMap()).thenReturn(new HashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Empty string single‑value key should cause isDefaultCompactSet() to return false");
//     }

//     @Test
//     void backingMapSubclassOfHashMap_returnsFalse() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(new MyHashMap<>());
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Backing map that is a subclass of HashMap should not be considered default");
//     }

//     @Test
//     void backingMapUnmodifiableView_returnsFalse() {
//         Map<String, Object> unmodifiable = Collections.unmodifiableMap(new HashMap<>());
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         when(mockMap.getNewMap()).thenReturn(unmodifiable);
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         assertFalse(set.isDefaultCompactSet(), "Unmodifiable view of a HashMap should cause isDefaultCompactSet() to return false");
//     }

//     @Test
//     void backingMapReturnsNull_throwsNullPointerException() {
//         CompactMap<String, Object> mockMap = mock(CompactMap.class);
//         when(mockMap.compactSize()).thenReturn(CompactMap.DEFAULT_COMPACT_SIZE);
//         when(mockMap.isCaseInsensitive()).thenReturn(false);
//         when(mockMap.getOrdering()).thenReturn("unordered");
//         when(mockMap.getSingleValueKey()).thenReturn(CompactMap.DEFAULT_SINGLE_KEY);
//         // malformed map
//         when(mockMap.getNewMap()).thenReturn(null);
//         CompactSet<String> set = new CompactSet<>();
//         injectMockMap(set, mockMap);
//         try {
//             set.isDefaultCompactSet();
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//             // expected
//         }
//     }

//     @Test
//     void nullCompactMapReference_throwsNullPointerException() throws Exception {
//         CompactSet<String> set = new CompactSet<>();
//         // Force the private 'map' field to null via reflection
//         Field mapField = CompactSet.class.getDeclaredField("map");
//         mapField.setAccessible(true);
//         mapField.set(set, null);
//         try {
//             set.isDefaultCompactSet();
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//             // expected
//         }
//     }

//     @Test
//     public void orderingCaseMismatch_UnorderedCapitalized_returnsFalse() {
//         orderingCaseMismatchHelper("Unordered");
//     }

//     @Test
//     public void orderingCaseMismatch_UnorderedUppercase_returnsFalse() {
//         orderingCaseMismatchHelper("UNORDERED");
//     }

//     @Test
//     public void orderingCaseMismatch_UnorderedTrailingSpace_returnsFalse() {
//         orderingCaseMismatchHelper("unordered ");
//     }
}