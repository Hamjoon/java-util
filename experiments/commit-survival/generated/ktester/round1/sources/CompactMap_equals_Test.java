package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

// kept from the template; not used but harmless
@ExtendWith(MockitoExtension.class)
class CompactMap_equals_Test {

    /**
     * A CompactMap with a tiny {@code compactSize()} to force the array storage quickly.
     */
    private static class TinyCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // array representation will be used for size 2‑2
            return 2;
        }

        @Override
        protected Map<K, V> getNewMap() {
            // delegate map when size > 2
            return new HashMap<>();
        }
    }

    /**
     * A CompactMap that switches to a delegate map after only 5 entries.
     */
    private static class TinyDelegateCompactMap<K, V> extends CompactMap<K, V> {

        @Override
        protected int compactSize() {
            // array representation up to 5 entries
            return 5;
        }

        @Override
        protected Map<K, V> getNewMap() {
            // delegate map for size > 5
            return new HashMap<>();
        }
    }

    /**
     * Provides a stream of arguments for the {@link #equalsParameterizedTest} method.
     * Each argument contains:
     * <ul>
     *   <li>a description (used as the test name)</li>
     *   <li>a {@link CompactMap} instance under test</li>
     *   <li>the object to compare with</li>
     *   <li>the expected boolean result</li>
     * </ul>
     */
    private static Stream<Arguments> equalsTestCases() {
        // 1. same instance
        CompactMap<String, Integer> sameInstance = new CompactMap<>();
        sameInstance.put("x", 1);
        // 2. null
        CompactMap<String, Integer> mapForNull = new CompactMap<>();
        // 3. different type
        CompactMap<String, Integer> mapForOtherType = new CompactMap<>();
        // 4. empty CompactMap vs empty HashMap
        CompactMap<String, Integer> emptyCm = new CompactMap<>();
        Map<String, Integer> emptyHash = new HashMap<>();
        // 5. empty CompactMap vs non‑empty map
        CompactMap<String, Integer> emptyVsNonEmpty = new CompactMap<>();
        Map<String, Integer> nonEmpty = Map.of("a", 1);
        // 6. single entry equal
        CompactMap<String, String> singleEqual = new CompactMap<>();
        singleEqual.put("key", "value");
        // 7. same key, different value
        CompactMap<String, String> singleDiffVal = new CompactMap<>();
        singleDiffVal.put("key", "value");
        // 8. array representation (size 2) equal
        TinyCompactMap<String, Integer> arrayEqual = new TinyCompactMap<>();
        arrayEqual.put("a", 1);
        arrayEqual.put("b", 2);
        // 9. two‑entry map with a null value (array mode)
        TinyCompactMap<String, String> arrayNull = new TinyCompactMap<>();
        arrayNull.put("k1", null);
        arrayNull.put("k2", "v2");
        // 10. two‑entry mismatch in value
        TinyCompactMap<String, Integer> arrayMismatch = new TinyCompactMap<>();
        arrayMismatch.put("k1", 1);
        arrayMismatch.put("k2", 2);
        // 11. delegate representation (size > compactSize) equal
        TinyDelegateCompactMap<String, Integer> delegateEqual = new TinyDelegateCompactMap<>();
        for (int i = 0; i < 10; i++) {
            delegateEqual.put("k" + i, i);
        }
        // 12. delegate representation with extra entry
        TinyDelegateCompactMap<String, Integer> delegateExtra = new TinyDelegateCompactMap<>();
        for (int i = 0; i < 10; i++) {
            delegateExtra.put("k" + i, i);
        }
        // 13. TreeMap equality
        CompactMap<String, Integer> treeMapEqual = new CompactMap<>();
        treeMapEqual.put("a", 1);
        treeMapEqual.put("b", 2);
        // 14. size mismatch (2 vs 3 entries)
        CompactMap<String, Integer> sizeMismatch = new CompactMap<>();
        sizeMismatch.put("a", 1);
        sizeMismatch.put("b", 2);
        return Stream.of(// description, CompactMap under test, object to compare, expected result
        Arguments.of("The map should be equal to itself (reference equality shortcut).", sameInstance, sameInstance, true), Arguments.of("Passing null must return false because null is not a Map.", mapForNull, null, false), Arguments.of("An object of a different type (String) must not be considered equal.", mapForOtherType, "some string", false), Arguments.of("Two empty maps (CompactMap and a regular HashMap) are equal.", emptyCm, emptyHash, true), Arguments.of("An empty CompactMap must not be equal to a non‑empty map.", emptyVsNonEmpty, nonEmpty, false), Arguments.of("A CompactMap with a single entry equals another map containing the same entry.", singleEqual, Map.of("key", "value"), true), Arguments.of("Same key but different value – equality must fail.", singleDiffVal, Map.of("key", "other"), false), Arguments.of("CompactMap with two entries (size 2 → internal Object[] representation) equals a map with the same entries, order independent.", arrayEqual, Map.of("a", 1, "b", 2), true), Arguments.of("Two‑entry map where one value is null – the null handling branch must return true when both sides have null.", arrayNull, Map.of("k1", null, "k2", "v2"), true), Arguments.of("Two‑entry CompactMap compared with a map that differs in one value – equality must be false.", arrayMismatch, Map.of("k1", 1, "k2", 99), false), Arguments.of("CompactMap with more than the compactSize threshold (internal Map representation) equals another map with the same 10 entries.", delegateEqual, new HashMap<>(delegateEqual), true), Arguments.of("CompactMap with 10 entries compared to a map that has an extra entry – size mismatch must cause false.", delegateExtra, new HashMap<String, Integer>() {

            {
                for (int i = 0; i < 10; i++) put("k" + i, i);
                put("extra", 999);
            }
        }, false), Arguments.of("Equality must work across different Map implementations (CompactMap vs TreeMap) when entries are identical.", treeMapEqual, new TreeMap<>(Map.of("a", 1, "b", 2)), true), Arguments.of("CompactMap with two entries compared to a map with three entries – size check must return false.", sizeMismatch, Map.of("a", 1, "b", 2, "c", 3), false));
    }

    @Test
    void equalsSameInstance_isTrue() {
        CompactMap<String, Integer> map = new CompactMap<>();
        map.put("a", 1);
        assertTrue(map.equals(map), "A map must be equal to itself");
    }

    @Test
    void equalsNull_isFalse() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertFalse(map.equals(null), "A map must not be equal to null");
    }

    @Test
    void equalsDifferentType_isFalse() {
        CompactMap<String, Integer> map = new CompactMap<>();
        assertFalse(map.equals("not a map"), "A map must not be equal to an object of another type");
    }

    @Test
    void emptyCompactMapEqualsEmptyCompactMap_isTrue() {
        CompactMap<String, Integer> a = new CompactMap<>();
        CompactMap<String, Integer> b = new CompactMap<>();
        assertTrue(a.equals(b), "Two empty CompactMaps must be equal");
        assertTrue(b.equals(a), "Symmetry for empty CompactMaps");
    }

    @Test
    void emptyCompactMapEqualsEmptyHashMap_isTrue() {
        CompactMap<String, Integer> cm = new CompactMap<>();
        Map<String, Integer> hm = new HashMap<>();
        assertTrue(cm.equals(hm), "Empty CompactMap must equal empty HashMap");
        assertTrue(hm.equals(cm), "HashMap's equals should also consider CompactMap equal");
    }

    @Test
    void singleEntryEqual_sameKeyAndValue_isTrue() {
        CompactMap<String, Integer> a = new CompactMap<>();
        CompactMap<String, Integer> b = new CompactMap<>();
        a.put("k", 42);
        b.put("k", 42);
        assertTrue(a.equals(b), "Maps with identical single entry must be equal");
    }

    @Test
    void singleEntryEqual_differentValue_isFalse() {
        CompactMap<String, Integer> a = new CompactMap<>();
        CompactMap<String, Integer> b = new CompactMap<>();
        a.put("k", 1);
        b.put("k", 2);
        assertFalse(a.equals(b), "Maps with same key but different values must not be equal");
    }

    @Test
    void singleEntryEqual_nullValue_isTrue() {
        CompactMap<String, Integer> a = new CompactMap<>();
        CompactMap<String, Integer> b = new CompactMap<>();
        a.put("k", null);
        b.put("k", null);
        assertTrue(a.equals(b), "Both maps contain null value for same key → should be equal");
    }

    @Test
    void singleEntryEqual_oneNullOneNonNull_isFalse() {
        CompactMap<String, Integer> a = new CompactMap<>();
        CompactMap<String, Integer> b = new CompactMap<>();
        a.put("k", null);
        b.put("k", 5);
        assertFalse(a.equals(b), "Null vs non‑null value for same key must not be equal");
    }

    @Test
    void arrayRepresentationEqualsStandardMap_isTrue() {
        TinyCompactMap<String, Integer> cm = new TinyCompactMap<>();
        cm.put("a", 1);
        // size == compactSize (2) → still array representation
        cm.put("b", 2);
        Map<String, Integer> reference = new HashMap<>();
        reference.put("a", 1);
        reference.put("b", 2);
        assertTrue(cm.equals(reference), "CompactMap in array mode must equal a regular Map with same entries");
    }

    @Test
    void arrayRepresentationMismatchedKey_isFalse() {
        TinyCompactMap<String, Integer> cm = new TinyCompactMap<>();
        cm.put("a", 1);
        cm.put("b", 2);
        Map<String, Integer> other = new HashMap<>();
        other.put("a", 1);
        // different key
        other.put("c", 2);
        assertFalse(cm.equals(other), "Different keys should make equality fail");
    }

    @Test
    void delegateMapRepresentationEqualsStandardMap_isTrue() {
        // Use the regular CompactMap (default compactSize = 80) and insert >80 entries.
        CompactMap<Integer, Integer> cm = new CompactMap<>();
        Map<Integer, Integer> reference = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            cm.put(i, i * 10);
            reference.put(i, i * 10);
        }
        assertTrue(cm.equals(reference), "When CompactMap delegates to a Map, equality must match the delegate");
    }

    @Test
    void delegateMapRepresentationMismatchedSize_isFalse() {
        CompactMap<Integer, Integer> cm = new CompactMap<>();
        Map<Integer, Integer> reference = new HashMap<>();
        for (int i = 0; i < 100; i++) {
            cm.put(i, i);
            reference.put(i, i);
        }
        // Remove one entry from the reference map → size differs
        reference.remove(99);
        assertFalse(cm.equals(reference), "Different sizes must cause equality to be false");
    }

    @Test
    void symmetryWithStandardMap_isMaintained() {
        CompactMap<String, String> cm = new CompactMap<>();
        cm.put("x", "y");
        Map<String, String> hm = new HashMap<>();
        hm.put("x", "y");
        assertTrue(cm.equals(hm), "CompactMap should equal HashMap");
        assertTrue(hm.equals(cm), "HashMap should also consider CompactMap equal");
    }

    @Test
    void transitivityAcrossThreeMaps_isMaintained() {
        CompactMap<String, String> a = new CompactMap<>();
        CompactMap<String, String> b = new CompactMap<>();
        Map<String, String> c = new HashMap<>();
        a.put("k1", "v1");
        a.put("k2", "v2");
        b.putAll(a);
        c.putAll(a);
        assertTrue(a.equals(b));
        assertTrue(b.equals(c));
        assertTrue(a.equals(c), "Equality should be transitive across CompactMap and regular Map");
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("equalsTestCases")
    void equalsParameterizedTest(String description, CompactMap<?, ?> compactMap, Object other, boolean expected) {
        assertEquals(expected, compactMap.equals(other), description);
    }
}
