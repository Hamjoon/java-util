package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Additional unit tests for {@link CompactMap#containsValue(Object)} that cover
 * all edge‑cases described in the JSON test specification.
 *
 * The existing tests already verify the normal behaviour of the map; the
 * tests below focus on the internal representation (array, map, single value,
 * malformed state, and exception propagation) by using reflection to force
 * the private {@code val} field into the desired state.
 */
@ExtendWith(MockitoExtension.class)
class CompactMap_containsValue_Test {

    private CompactMap<String, Object> map;
    private static Field valField;   // cached reflective access to the private field

    @BeforeAll
    static void initReflection() throws NoSuchFieldException {
        valField = CompactMap.class.getDeclaredField("val");
        valField.setAccessible(true);
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        map = null;
    }

    /* --------------------------------------------------------------------- */
    /* Existing basic behaviour tests (kept for completeness)               */
    /* --------------------------------------------------------------------- */

    @Test
    void emptyMapShouldReturnFalseForAnyValue() {
        assertFalse(map.containsValue(null), "Empty map must return false for null");
        assertFalse(map.containsValue(1), "Empty map must return false for any non‑null value");
    }

    @Test
    void singleEntryMapShouldDetectExistingValue() {
        map.put("one", 42);
        assertTrue(map.containsValue(42), "Map should contain the value that was put");
        assertFalse(map.containsValue(99), "Map should not contain a value that was not put");
    }

    @Test
    void singleEntryMapShouldHandleNullValue() {
        map.put("nullKey", null);
        assertTrue(map.containsValue(null), "Map should report true for a stored null value");
        assertFalse(map.containsValue(0), "Map should not report true for a different value");
    }

    @Test
    void twoEntriesShouldBeStoredInArrayAndBeDetectable() {
        map.put("first", 1);
        map.put("second", 2);
        assertTrue(map.containsValue(1), "Map should contain first value");
        assertTrue(map.containsValue(2), "Map should contain second value");
        assertFalse(map.containsValue(3), "Map should not contain a value that was never added");
    }

    @Test
    void manyEntriesShouldSwitchToBackingMapAndDetectValues() {
        int total = 85; // default compactSize is 80
        for (int i = 0; i < total; i++) {
            map.put("k" + i, i);
        }
        assertTrue(map.containsValue(0), "Backing map should contain the first value");
        assertTrue(map.containsValue(84), "Backing map should contain the last value");
        assertFalse(map.containsValue(999), "Backing map should not contain a value that was never added");
        assertEquals(total, map.size(), "Map size should match the number of inserted entries");
    }

    @Test
    void containsValueShouldUseObjectsEqualsSemantics() {
        class Wrapper {
            final int v;
            Wrapper(int v) { this.v = v; }
            @Override public boolean equals(Object o) {
                return (o instanceof Wrapper) && ((Wrapper) o).v == v;
            }
            @Override public int hashCode() { return v; }
        }
        Wrapper w1 = new Wrapper(10);
        Wrapper w2 = new Wrapper(10);
        CompactMap<String, Wrapper> customMap = new CompactMap<>();
        customMap.put("key", w1);
        assertTrue(customMap.containsValue(w1), "Exact instance should be found");
        assertTrue(customMap.containsValue(w2), "Equal instance should be found via Objects.equals");
        assertFalse(customMap.containsValue(new Wrapper(11)), "Different value should not be found");
    }

    /* --------------------------------------------------------------------- */
    /* New tests derived from the JSON specification                          */
    /* --------------------------------------------------------------------- */

    @Test
    void arrayStorageValuePresent() throws Exception {
        // Force array storage with two entries: {"k1","value1","k2","value2"}
        Object[] arr = new Object[]{"k1", "value1", "k2", "value2"};
        valField.set(map, arr);
        assertTrue(map.containsValue("value2"), "Value present in second pair should be found");
    }

    @Test
    void arrayStorageValueAbsent() throws Exception {
        Object[] arr = new Object[]{"k1", "value1", "k2", "value2"};
        valField.set(map, arr);
        assertFalse(map.containsValue("missing"), "Absent value must return false");
    }

    @Test
    void arrayStorageNullValuePresent() throws Exception {
        Object[] arr = new Object[]{"k1", null, "k2", "value2"};
        valField.set(map, arr);
        assertTrue(map.containsValue(null), "Null value stored in array must be found");
    }

    @Test
    void mapStorageValuePresent() throws Exception {
        Map<String, Object> backing = new HashMap<>();
        backing.put("a", 42);
        valField.set(map, backing);
        assertTrue(map.containsValue(42), "Delegated map contains the value");
    }

    @Test
    void mapStorageValueAbsent() throws Exception {
        Map<String, Object> backing = new HashMap<>();
        backing.put("a", 42);
        valField.set(map, backing);
        assertFalse(map.containsValue(99), "Delegated map does not contain the value");
    }

    @Test
    void emptyMapAlwaysReturnsFalse() throws Exception {
        // The default EMPTY_MAP sentinel is already set in a fresh map.
        assertFalse(map.containsValue("anything"), "Empty map must always return false");
    }

    @Test
    void singleEntryReferenceEqualityMatches() throws Exception {
        String stored = "singleValue";
        // Directly set the internal val to the stored reference
        valField.set(map, stored);
        assertTrue(map.containsValue(stored), "Same reference should match with ==");
    }

    @Test
    void singleEntryEqualButDifferentReferenceFails() throws Exception {
        String stored = "singleValue";
        valField.set(map, stored);
        String differentRef = new String("singleValue");
        assertFalse(map.containsValue(differentRef), "Different String instance must not match with ==");
    }

    @Test
    void singleEntryNullValueSearchNullFailsWhenStoredNonNull() throws Exception {
        String stored = "notNull";
        valField.set(map, stored);
        assertFalse(map.containsValue(null), "Searching null when stored value is non‑null must be false");
    }

    @Test
    void singleEntryStoredNullValueSearchNullSucceeds() throws Exception {
        // Store a null value via the public API – this puts the map into the
        // single‑entry‑null state.
        map.put("k", null);
        assertTrue(map.containsValue(null), "Stored null value should be found");
    }

    @Test
    void singleEntryStoredNullValueSearchNonNullFails() throws Exception {
        map.put("k", null);
        assertFalse(map.containsValue("nonNull"), "Searching non‑null when stored null must be false");
    }

    @Test
    void arrayStorageEmptyInternalArray() throws Exception {
        Object[] empty = new Object[0];
        valField.set(map, empty);
        assertFalse(map.containsValue("anything"), "Empty internal array must yield false");
    }

    @Test
    void arrayStorageNullValueAbsent() throws Exception {
        Object[] arr = new Object[]{"k1", "v1", "k2", "v2"};
        valField.set(map, arr);
        assertFalse(map.containsValue(null), "Array without null values must return false for null");
    }

    @Test
    void mapStorageEmptyUnderlyingMap() throws Exception {
        Map<String, Object> emptyMap = new HashMap<>();
        valField.set(map, emptyMap);
        assertFalse(map.containsValue("irrelevant"), "Empty backing map must return false");
    }

    @Test
    void mapStorageNullValuePresent() throws Exception {
        Map<String, Object> backing = new HashMap<>();
        backing.put("k", null);
        valField.set(map, backing);
        assertTrue(map.containsValue(null), "Backing map containing null should be found");
    }

    @Test
    void arrayStorageCustomObjectEquality() throws Exception {
        class EqualsTrue {
            @Override public boolean equals(Object o) { return true; }
            @Override public int hashCode() { return 1; }
        }
        EqualsTrue stored = new EqualsTrue();
        Object[] arr = new Object[]{"k", stored};
        valField.set(map, arr);
        assertTrue(map.containsValue(new EqualsTrue()), "Custom equals returning true must be honored");
    }

    @Test
    void singleEntryDifferentIntegerInstanceSameValueFails() throws Exception {
        Integer stored = Integer.valueOf(128);
        valField.set(map, stored);
        Integer other = new Integer(128);
        assertFalse(map.containsValue(other), "Different Integer instance must not match with ==");
    }

    @Test
    void arrayStorageMalformedOddLengthArrayThrows() throws Exception {
        Object[] malformed = new Object[]{"k1", "v1", "lonelyKey"};
        valField.set(map, malformed);
        assertThrows(ArrayIndexOutOfBoundsException.class,
                () -> map.containsValue("any"),
                "Odd‑length array should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void internalValNullThrowsNullPointerException() throws Exception {
        valField.set(map, null);
        assertThrows(NullPointerException.class,
                () -> map.containsValue("anything"),
                "Null internal val must propagate NullPointerException");
    }

    @Test
    void mapStorageUnderlyingMapThrowsException() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> badMap = mock(Map.class);
        when(badMap.containsValue(any())).thenThrow(new IllegalStateException("boom"));
        valField.set(map, badMap);
        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> map.containsValue("trigger"),
                "Exception from underlying map must propagate");
        assertEquals("boom", ex.getMessage());
    }

    @Test
    void arrayStorageEntryEqualsThrowsRuntimeException() throws Exception {
        class BadEquals {
            @Override public boolean equals(Object o) {
                throw new RuntimeException("equals failed");
            }
        }
        BadEquals bad = new BadEquals();
        Object[] arr = new Object[]{"k", bad};
        valField.set(map, arr);
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> map.containsValue("any"),
                "Exception from entry.equals must propagate");
        assertEquals("equals failed", ex.getMessage());
    }
}