package com.cedarsoftware.util;

import java.util.Map;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_lastEntry_Test {

    private NavigableMap<String, Integer> underlyingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        sealed = false;
        sealedSupplier = () -> sealed;
        // Populate a map with a known ordering.
        underlyingMap = new ConcurrentNavigableMapNullSafe<>();
        underlyingMap.put("a", 1);
        underlyingMap.put("b", 2);
        underlyingMap.put("c", 3);
        sealableMap = new SealableNavigableMap<>(underlyingMap, sealedSupplier);
    }

    /**
     * Supplies test data for the basic behavior group.
     *
     * Each array element contains:
     *   0 – description (used as test name)
     *   1 – a pre‑populated NavigableMap (may be empty)
     *   2 – expected entry (null if lastEntry should return null)
     */
    private static Object[][] behaviorCases() {
        return new Object[][] { { "Empty underlying map returns null", new TreeMap<String, String>(), null }, { "Single entry map returns that entry", mapOf("A", "alpha"), entryOf("A", "alpha") }, { "Multiple entries with natural ordering returns greatest key 'C'", mapOf("B", "beta", "A", "alpha", "C", "gamma"), entryOf("C", "gamma") }, { "Reverse comparator makes 'A' the highest key", mapWithComparator((k1, k2) -> k2.compareTo(k1), "B", "beta", "A", "alpha", "C", "gamma"), entryOf("A", "alpha") }, { "After removing highest key (3) the new highest is 2", mapOf("1", 10, "2", 20, "3", 30), // after removal in the test body
        entryOf("2", 20) }, { "Live view: modifying entry value updates underlying map", mapOf("X", "xvalue", "Y", "yvalue"), // expected after setValue in test body
        entryOf("Y", "newValue") } };
    }

    private static <K, V> Map.Entry<K, V> entryOf(K key, V value) {
        return Collections.singletonMap(key, value).entrySet().iterator().next();
    }

    @Test
    void testLastEntryReturnsHighestKey() {
        Map.Entry<String, Integer> last = sealableMap.lastEntry();
        assertNotNull(last, "lastEntry should not be null when map has entries");
        assertEquals("c", last.getKey(), "Highest key should be 'c'");
        assertEquals(Integer.valueOf(3), last.getValue(), "Value for key 'c' should be 3");
    }

    @Test
    void testLastEntryUpdatesWhenHigherKeyAdded() {
        sealableMap.put("d", 4);
        Map.Entry<String, Integer> last = sealableMap.lastEntry();
        assertNotNull(last);
        assertEquals("d", last.getKey(), "After adding a higher key, lastEntry should reflect it");
        assertEquals(Integer.valueOf(4), last.getValue());
    }

    @Test
    void testLastEntryWhenMapIsEmpty() {
        // Create an empty SealableNavigableMap (different instance from the one set up above)
        SealableNavigableMap<String, Integer> emptyMap = new SealableNavigableMap<>(sealedSupplier);
        assertNull(emptyMap.lastEntry(), "lastEntry should return null for an empty map");
    }

    @Test
    void testLastEntrySetValueWhenSealedThrows() {
        Map.Entry<String, Integer> last = sealableMap.lastEntry();
        assertNotNull(last);
        // make the map sealed
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> last.setValue(10), "Modifying an entry's value while sealed must throw");
    }

    @Test
    void testLastEntrySetValueWhenUnsealedSucceeds() {
        Map.Entry<String, Integer> last = sealableMap.lastEntry();
        assertNotNull(last);
        // Map is unsealed by default
        last.setValue(30);
        assertEquals(Integer.valueOf(30), sealableMap.get("c"), "Changing the entry's value should update the underlying map when unsealed");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("behaviorCases")
    void testLastEntryBehavior(String description, NavigableMap<String, ?> initialMap, Map.Entry<String, ?> expectedAfterOperations) {
        @SuppressWarnings("unchecked")
        NavigableMap<String, Object> map = (NavigableMap<String, Object>) initialMap;
        // Build the SealableNavigableMap with the supplied map.
        SealableNavigableMap<String, Object> mapUnderTest = new SealableNavigableMap<>(map, () -> false);
        // Special handling for cases that require extra operations before the assertion.
        switch(description) {
            case "After removing highest key (3) the new highest is 2":
                // remove the entry with key "3"
                mapUnderTest.remove("3");
                break;
            case "Live view: modifying entry value updates underlying map":
                // obtain the entry, modify its value, then fall through to assertion
                Map.Entry<String, Object> entry = mapUnderTest.lastEntry();
                assertNotNull(entry, "Entry should not be null before modification");
                entry.setValue("newValue");
                break;
            default:
        }
        Map.Entry<String, ?> actual = mapUnderTest.lastEntry();
        if (expectedAfterOperations == null) {
            assertNull(actual, description);
        } else {
            assertNotNull(actual, description);
            assertEquals(expectedAfterOperations.getKey(), actual.getKey(), description + " – key");
            assertEquals(expectedAfterOperations.getValue(), actual.getValue(), description + " – value");
        }
    }

    @Test
    void testLastEntryWithNegativeZeroPositiveIntegerKeys() {
        NavigableMap<Integer, Integer> map = new TreeMap<>();
        map.put(-10, -100);
        map.put(0, 0);
        map.put(10, 100);
        SealableNavigableMap<Integer, Integer> snMap = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<Integer, Integer> last = snMap.lastEntry();
        assertNotNull(last);
        assertEquals(Integer.valueOf(10), last.getKey());
        assertEquals(Integer.valueOf(100), last.getValue());
    }

    @Test
    void testLastEntryWithExplicitNaturalComparator() {
        Comparator<String> natural = (k1, k2) -> k1.compareTo(k2);
        NavigableMap<String, Integer> map = new TreeMap<>(natural);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        SealableNavigableMap<String, Integer> snMap = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<String, Integer> last = snMap.lastEntry();
        assertNotNull(last);
        assertEquals("c", last.getKey());
        assertEquals(Integer.valueOf(3), last.getValue());
    }

    @Test
    void testLastEntryWithReversedComparator() {
        Comparator<String> reversed = (k1, k2) -> k2.compareTo(k1);
        NavigableMap<String, Integer> map = new TreeMap<>(reversed);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        SealableNavigableMap<String, Integer> snMap = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<String, Integer> last = snMap.lastEntry();
        assertNotNull(last);
        assertEquals("a", last.getKey());
        assertEquals(Integer.valueOf(1), last.getValue());
    }

    @Test
    void testLastEntryAfterRemovingTwoHighestKeys() {
        NavigableMap<String, String> map = new TreeMap<>();
        map.put("key1", "v1");
        map.put("key2", "v2");
        map.put("key3", "v3");
        SealableNavigableMap<String, String> snMap = new SealableNavigableMap<>(map, () -> false);
        snMap.remove("key3");
        snMap.remove("key2");
        Map.Entry<String, String> last = snMap.lastEntry();
        assertNotNull(last);
        assertEquals("key1", last.getKey());
        assertEquals("v1", last.getValue());
    }

    @Test
    void testLastEntryMutableViewUpdatesMap() {
        NavigableMap<String, String> map = new TreeMap<>();
        map.put("A", "alpha");
        map.put("B", "beta");
        SealableNavigableMap<String, String> snMap = new SealableNavigableMap<>(map, () -> false);
        // should be "B"
        Map.Entry<String, String> entry = snMap.lastEntry();
        assertNotNull(entry);
        entry.setValue("newBeta");
        assertEquals("newBeta", snMap.get("B"));
        assertEquals("newBeta", map.get("B"));
    }

    @Test
    void testLastEntryWithNullValue() {
        NavigableMap<String, String> map = new TreeMap<>();
        map.put("nullKey", null);
        SealableNavigableMap<String, String> snMap = new SealableNavigableMap<>(map, () -> false);
        Map.Entry<String, String> last = snMap.lastEntry();
        assertNotNull(last);
        assertEquals("nullKey", last.getKey());
        assertNull(last.getValue());
    }

    @Test
    void testLastEntryReflectsEntryAddedAfterConstruction() {
        // start empty
        SealableNavigableMap<String, String> snMap = new SealableNavigableMap<>(() -> false);
        snMap.put("Z", "zeta");
        Map.Entry<String, String> last = snMap.lastEntry();
        assertNotNull(last);
        assertEquals("Z", last.getKey());
        assertEquals("zeta", last.getValue());
    }

    @Test
    void testLastEntryReturnsNullAfterClear() {
        SealableNavigableMap<String, Integer> snMap = new SealableNavigableMap<>(underlyingMap, () -> false);
        snMap.clear();
        assertNull(snMap.lastEntry(), "lastEntry should be null after the map is cleared");
    }

    @Test
    void testLastEntryWithNullUnderlyingMapThrowsNPE() throws Exception {
        // Construct via reflection to inject a null navMap.
        SealableNavigableMap<String, Integer> snMap = new SealableNavigableMap<>(sealedSupplier);
        Field navMapField = SealableNavigableMap.class.getDeclaredField("navMap");
        navMapField.setAccessible(true);
        // force null internal map
        navMapField.set(snMap, null);
        assertThrows(NullPointerException.class, snMap::lastEntry, "Calling lastEntry when internal navMap is null should throw NPE");
    }

    @Test
    void testLastEntryWithIncomparableKeysThrowsClassCastException() {
        // TreeMap without comparator will try to compare Integer and String.
        NavigableMap<Object, Integer> map = new TreeMap<>();
        map.put(1, 100);
        // heterogeneous key
        map.put("b", 200);
        SealableNavigableMap<Object, Integer> snMap = new SealableNavigableMap<>(map, () -> false);
        assertThrows(ClassCastException.class, snMap::lastEntry, "Calling lastEntry on a map with non‑comparable heterogeneous keys should throw ClassCastException");
    }

    @Test
    void testLastEntryPropagatesRuntimeExceptionFromUnderlyingMap(@Mock NavigableMap<String, Integer> mockMap) {
        when(mockMap.lastEntry()).thenThrow(new RuntimeException("forced"));
        SealableNavigableMap<String, Integer> snMap = new SealableNavigableMap<>(mockMap, () -> false);
        RuntimeException thrown = assertThrows(RuntimeException.class, snMap::lastEntry, "RuntimeException from underlying map should propagate unchanged");
        assertEquals("forced", thrown.getMessage());
    }

    @SafeVarargs
    private static <K, V> NavigableMap<K, V> mapOf(Object... keyVals) {
        NavigableMap<K, V> map = new TreeMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K) keyVals[i];
            @SuppressWarnings("unchecked")
            V v = (V) keyVals[i + 1];
            map.put(k, v);
        }
        return map;
    }

    @SafeVarargs
    private static <K, V> NavigableMap<K, V> mapWithComparator(Comparator<? super K> comp, Object... keyVals) {
        NavigableMap<K, V> map = new TreeMap<>(comp);
        for (int i = 0; i < keyVals.length; i += 2) {
            @SuppressWarnings("unchecked")
            K k = (K) keyVals[i];
            @SuppressWarnings("unchecked")
            V v = (V) keyVals[i + 1];
            map.put(k, v);
        }
        return map;
    }
}
