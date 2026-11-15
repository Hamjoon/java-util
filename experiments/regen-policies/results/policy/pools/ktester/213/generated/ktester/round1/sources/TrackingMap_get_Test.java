package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.lang.reflect.Field;
import java.util.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_get_Test {

    private static Map<String, String> sharedBackingMap;

    private TrackingMap<String, String> trackingMap;

    @BeforeAll
    static void setupBeforeAll() {
        // shared immutable data that can be reused across tests
        sharedBackingMap = new HashMap<>();
        sharedBackingMap.put("alpha", "A");
        sharedBackingMap.put("beta", "B");
        sharedBackingMap.put("gamma", null);
    }

    @BeforeEach
    void setupBeforeEach() {
        // each test gets a fresh TrackingMap with a copy of the shared backing map
        Map<String, String> mapCopy = new HashMap<>(sharedBackingMap);
        trackingMap = new TrackingMap<>(mapCopy);
    }

    @AfterEach
    void teardownAfterEach() {
        // clear references to help GC and ensure no cross‑test contamination
        trackingMap = null;
    }

    @AfterAll
    static void teardownAfterAll() {
        sharedBackingMap = null;
    }

    @Test
    void getExistingKeyIsReturnedAndRecorded() {
        // given
        assertNull(trackingMap.keysUsed());
        // when
        String value = trackingMap.get("alpha");
        // then
        assertEquals("A", value, "get should return the correct value");
        assertTrue(trackingMap.keysUsed().contains("alpha"), "Accessed key must be recorded");
        assertEquals(1, trackingMap.keysUsed().size(), "Only the accessed key should be recorded");
    }

    @Test
    void getNonExistingKeyReturnsNullAndIsRecorded() {
        // when
        // not present in the map
        String value = trackingMap.get("delta");
        // then
        assertNull(value, "get on a missing key should return null");
        assertTrue(trackingMap.keysUsed().contains("delta"), "Missing key access must still be recorded");
        assertEquals(1, trackingMap.keysUsed().size());
    }

    @Test
    void getNullValueCountsAsAccess() {
        // when
        // key exists with null value
        String value = trackingMap.get("gamma");
        // then
        assertNull(value, "Value is null as stored");
        assertTrue(trackingMap.keysUsed().contains("gamma"));
        assertEquals(1, trackingMap.keysUsed().size());
    }

    @Test
    void multipleGetsRecordEachKeyOnlyOnce() {
        // when
        trackingMap.get("alpha");
        trackingMap.get("beta");
        // duplicate access
        trackingMap.get("alpha");
        // then
        assertTrue(trackingMap.keysUsed().contains("alpha"));
        assertTrue(trackingMap.keysUsed().contains("beta"));
        assertEquals(2, trackingMap.keysUsed().size(), "Duplicate accesses should not create duplicate entries");
    }

    @Test
    void getDoesNotAffectOtherMapOperations() {
        // given
        trackingMap.put("delta", "D");
        assertEquals(4, trackingMap.size());
        // when
        trackingMap.get("delta");
        // then
        assertEquals(4, trackingMap.size(), "get should not modify the map size");
        assertEquals("D", trackingMap.getWrappedMap().get("delta"));
    }

    @Test
    void getAfterExpungeUnusedKeepsTrackedEntries() {
        // given
        // mark as used
        trackingMap.get("alpha");
        // should remove beta and gamma
        trackingMap.expungeUnused();
        // when
        String remaining = trackingMap.get("alpha");
        // then
        assertEquals("A", remaining);
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.keysUsed().contains("alpha"));
    }

    @Test
    void getWithCaseInsensitiveBackingMapRespectsCase() {
        // using a CaseInsensitiveMap as backing store
        TrackingMap<String, String> ciMap = new TrackingMap<>(new CaseInsensitiveMap<>());
        ciMap.put("First", "one");
        ciMap.put("Second", "two");
        // when
        String v = ciMap.get("fIrSt");
        // then
        assertEquals("one", v);
        assertTrue(ciMap.keysUsed().contains("fIrSt"));
    }

    /**
     * Normal case – key present, returns value and records the key.
     */
    @Test
    void get_presentKey_returnsValueAndRecordsKey() {
        String result = trackingMap.get("alpha");
        assertEquals("A", result);
        assertTrue(trackingMap.keysUsed().contains("alpha"));
    }

    /**
     * Normal case – key absent, returns null but still records the key.
     */
    @Test
    void get_absentKey_returnsNullAndRecordsKey() {
        String result = trackingMap.get("betaX");
        assertNull(result);
        assertTrue(trackingMap.keysUsed().contains("betaX"));
    }

    /**
     * Null key on a HashMap (null‑permitting).  The key should be added to the
     * read‑keys set even though the value is null.
     */
    @Test
    void get_nullKeyOnHashMap_isRecorded() {
        // HashMap permits null keys – the shared map does not contain a null key.
        String result = trackingMap.get(null);
        assertNull(result);
        assertTrue(trackingMap.keysUsed().contains(null));
    }

    /**
     * Passing a key of the wrong generic type (Integer into a String‑key map)
     * should cause a {@link ClassCastException} before the key is added to the
     * read‑keys set.
     */
    @Test
    void get_wrongKeyType_throwsClassCastException() {
        // The map is declared as TrackingMap<String, String>
        Executable call = () -> trackingMap.get(12345);
        assertThrows(ClassCastException.class, call);
        // read‑keys set must remain untouched
        assertNull(trackingMap.keysUsed());
    }

    /**
     * TrackingMap instantiated with a generic type that accepts the supplied
     * key (Number) – the cast succeeds and the key is recorded.
     */
    @Test
    void get_subclassKeyCompatibleWithGeneric_success() {
        TrackingMap<Number, String> numberMap = new TrackingMap<>(new HashMap<>());
        numberMap.put(42, "answer");
        // Integer is a subclass of Number
        String value = numberMap.get(42);
        assertEquals("answer", value);
        assertTrue(numberMap.keysUsed().contains(42));
    }

    /**
     * After a {@code put} operation, {@code get} should retrieve the newly added
     * value and record the key.
     */
    @Test
    void get_afterPut_returnsNewValueAndRecordsKey() {
        trackingMap.put("epsilon", "valueEpsilon");
        String result = trackingMap.get("epsilon");
        assertEquals("valueEpsilon", result);
        assertTrue(trackingMap.keysUsed().contains("epsilon"));
    }

    /**
     * After a {@code remove} operation, {@code get} must return {@code null}
     * (the entry no longer exists) but still record the attempted access.
     */
    @Test
    void get_afterRemove_returnsNullAndRecordsKey() {
        trackingMap.put("zeta", "valueZeta");
        trackingMap.remove("zeta");
        String result = trackingMap.get("zeta");
        assertNull(result);
        assertTrue(trackingMap.keysUsed().contains("zeta"));
    }

    /**
     * After a {@code clear} operation, the map is empty; {@code get} returns
     * {@code null} and records the key.
     */
    @Test
    void get_afterClear_returnsNullAndRecordsKey() {
        trackingMap.clear();
        String result = trackingMap.get("eta");
        assertNull(result);
        assertTrue(trackingMap.keysUsed().contains("eta"));
    }

    /**
     * After replacing the backing map via {@code setWrappedMap}, {@code get}
     * should read from the new map.
     */
    @Test
    void get_afterSetWrappedMap_readsFromNewMap() {
        Map<String, String> newMap = new HashMap<>();
        newMap.put("theta", "newValue");
        trackingMap.setWrappedMap(newMap);
        String result = trackingMap.get("theta");
        assertEquals("newValue", result);
        assertTrue(trackingMap.keysUsed().contains("theta"));
    }

    /**
     * Duplicate calls to {@code get} for the same key must not create duplicate
     * entries in the read‑keys set.
     */
    @Test
    void get_duplicateCalls_idempotentReadKeys() {
        trackingMap.get("delta");
        trackingMap.get("delta");
        assertTrue(trackingMap.keysUsed().contains("delta"));
        assertEquals(1, trackingMap.keysUsed().size());
    }

    /**
     * Initial state – no prior reads.  After a single absent {@code get},
     * the read‑keys set should contain exactly that key.
     */
    @Test
    void get_initialState_absentKey_recordsExactlyOneKey() {
        assertNull(trackingMap.keysUsed());
        String result = trackingMap.get("iota");
        assertNull(result);
        assertTrue(trackingMap.keysUsed().contains("iota"));
        assertEquals(1, trackingMap.keysUsed().size());
    }

    /**
     * Constructing a {@code TrackingMap} with a {@code null} internal map
     * should cause a {@link NullPointerException} when {@code get} is called.
     */
    @Test
    void get_internalMapIsNull_throwsNullPointerException() {
        TrackingMap<String, String> nullMap = new TrackingMap<>(null);
        Executable call = () -> nullMap.get("anyKey");
        assertThrows(NullPointerException.class, call);
    }

    /**
     * If the {@code readKeys} field is {@code null}, {@code get} will throw a
     * {@link NullPointerException} after retrieving the value from the backing
     * map.
     */
    @Test
    void get_readKeysIsNull_throwsNullPointerException() throws Exception {
        // Use reflection to null‑out the private final readKeys field.
        Field readKeysField = TrackingMap.class.getDeclaredField("readKeys");
        readKeysField.setAccessible(true);
        readKeysField.set(trackingMap, null);
        Executable call = () -> trackingMap.get("alpha");
        assertThrows(NullPointerException.class, call);
    }

    /**
     * Underlying {@code Hashtable} does not allow {@code null} keys; the call
     * to {@code get(null)} should propagate the {@link NullPointerException}
     * thrown by the backing map.
     */
    @Test
    void get_nullKeyOnHashtable_throwsNullPointerException() {
        Hashtable<String, String> ht = new Hashtable<>();
        ht.put("a", "A");
        TrackingMap<String, String> htTracking = new TrackingMap<>(ht);
        Executable call = () -> htTracking.get(null);
        assertThrows(NullPointerException.class, call);
    }

    /**
     * A {@code TreeMap<Integer, String>} (which expects Integer keys) wrapped
     * in a raw {@code TrackingMap} will throw {@link ClassCastException}
     * when a non‑Integer key is supplied.
     */
    @Test
    @SuppressWarnings({ "unchecked", "rawtypes" })
    void get_treeMapWithIncompatibleKey_throwsClassCastException() {
        TreeMap<Integer, String> treeMap = new TreeMap<>();
        treeMap.put(1, "one");
        // raw type to bypass generic compile‑time checking
        TrackingMap rawTracking = new TrackingMap(treeMap);
        Executable call = () -> rawTracking.get("stringKey");
        assertThrows(ClassCastException.class, call);
    }

    /**
     * A custom {@code Map} implementation that deliberately throws an
     * {@link IllegalStateException} from its {@code get} method.  The exception
     * must propagate unchanged through {@code TrackingMap#get}.
     */
    @Test
    void get_customMapThrowsIllegalStateException_propagates() {
        @SuppressWarnings("unchecked")
        Map<String, String> explodingMap = mock(Map.class);
        when(explodingMap.get(any())).thenThrow(new IllegalStateException("boom"));
        TrackingMap<String, String> tm = new TrackingMap<>(explodingMap);
        Executable call = () -> tm.get("anyKey");
        IllegalStateException ex = assertThrows(IllegalStateException.class, call);
        assertEquals("boom", ex.getMessage());
    }

    /**
     * Parameterized test covering the normal and edge cases defined in the
     * first JSON group (present, absent, null key, wrong type).
     */
    @ParameterizedTest(name = "[{index}] key={0} expected={1}")
    @CsvSource({ // key,            expectedReturn,   expectException
    "'alpha',          'A',              ''", // absent key -> null
    "'betaX',          '',               ''", // empty string (present? not in map) -> null
    "'',               '',               ''", // integer key simulated via special token
    "'null',           '',               'ClassCastException'" })
    void parameterized_getVariousScenarios(String key, String expectedReturn, String expectException) {
        // Convert the special token "null" to an actual null reference for the test
        Object actualKey = "null".equals(key) ? null : key;
        if (!expectException.isEmpty()) {
            // Expecting an exception (e.g., ClassCastException)
            Executable call = () -> trackingMap.get(actualKey);
            assertThrows(ClassCastException.class, call);
        } else {
            // Normal execution path
            String result = trackingMap.get(actualKey);
            if (expectedReturn.isEmpty()) {
                assertNull(result);
            } else {
                assertEquals(expectedReturn, result);
            }
            // The key (including null) must be recorded
            assertTrue(trackingMap.keysUsed().contains(actualKey));
        }
    }
}
