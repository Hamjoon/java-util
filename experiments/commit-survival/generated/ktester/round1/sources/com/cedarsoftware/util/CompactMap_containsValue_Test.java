package com.cedarsoftware.util;

import org.mockito.Mock;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import static org.mockito.Mockito.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import static org.junit.Assert.*;

@ExtendWith(MockitoExtension.class)
@RunWith(MockitoJUnitRunner.class)
class CompactMap_containsValue_Test {

    private CompactMap<String, Object> map;

    // cached reflective access to the private field
    private static Field valField;

    class Wrapper {

        final int v;

        Wrapper(int v) {
            this.v = v;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Wrapper) && ((Wrapper) o).v == v;
        }

        @Override
        public int hashCode() {
            return v;
        }
    }

    class EqualsTrue {

        @Override
        public boolean equals(Object o) {
            return true;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals failed");
        }
    }

    class Wrapper {

        final int v;

        Wrapper(int v) {
            this.v = v;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Wrapper) && ((Wrapper) o).v == v;
        }

        @Override
        public int hashCode() {
            return v;
        }
    }

    class EqualsTrue {

        @Override
        public boolean equals(Object o) {
            return true;
        }

        @Override
        public int hashCode() {
            return 1;
        }
    }

    class BadEquals {

        @Override
        public boolean equals(Object o) {
            throw new RuntimeException("equals failed");
        }
    }

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

    @Test
    void emptyMapShouldReturnFalseForAnyValue() {
        assertFalse(map.containsValue(null));
        assertFalse(map.containsValue(1));
    }

    @Test
    void singleEntryMapShouldDetectExistingValue() {
        map.put("one", 42);
        assertTrue(map.containsValue(42));
        assertFalse(map.containsValue(99));
    }

    @Test
    void singleEntryMapShouldHandleNullValue() {
        map.put("nullKey", null);
        assertTrue(map.containsValue(null));
        assertFalse(map.containsValue(0));
    }

    @Test
    void twoEntriesShouldBeStoredInArrayAndBeDetectable() {
        map.put("first", 1);
        map.put("second", 2);
        assertTrue(map.containsValue(1));
        assertTrue(map.containsValue(2));
        assertFalse(map.containsValue(3));
    }

    @Test
    void manyEntriesShouldSwitchToBackingMapAndDetectValues() {
        // default compactSize is 80
        int total = 85;
        for (int i = 0; i < total; i++) {
            map.put("k" + i, i);
        }
        assertTrue(map.containsValue(0));
        assertTrue(map.containsValue(84));
        assertFalse(map.containsValue(999));
        assertEquals(total, map.size());
    }

    @Test
    void containsValueShouldUseObjectsEqualsSemantics() {
        class Wrapper {

            final int v;

            Wrapper(int v) {
                this.v = v;
            }

            @Override
            public boolean equals(Object o) {
                return (o instanceof Wrapper) && ((Wrapper) o).v == v;
            }

            @Override
            public int hashCode() {
                return v;
            }
        }
        Wrapper w1 = new Wrapper(10);
        Wrapper w2 = new Wrapper(10);
        CompactMap<String, Wrapper> customMap = new CompactMap<>();
        customMap.put("key", w1);
        assertTrue(customMap.containsValue(w1));
        assertTrue(customMap.containsValue(w2));
        assertFalse(customMap.containsValue(new Wrapper(11)));
    }

    @Test
    void arrayStorageValuePresent() throws Exception {
        Object[] arr = new Object[] { "k1", "value1", "k2", "value2" };
        valField.set(map, arr);
        assertTrue(map.containsValue("value2"));
    }

    @Test
    void arrayStorageValueAbsent() throws Exception {
        Object[] arr = new Object[] { "k1", "value1", "k2", "value2" };
        valField.set(map, arr);
        assertFalse(map.containsValue("missing"));
    }

    @Test
    void arrayStorageNullValuePresent() throws Exception {
        Object[] arr = new Object[] { "k1", null, "k2", "value2" };
        valField.set(map, arr);
        assertTrue(map.containsValue(null));
    }

    @Test
    void mapStorageValuePresent() throws Exception {
        Map<String, Object> backing = new HashMap<>();
        backing.put("a", 42);
        valField.set(map, backing);
        assertTrue(map.containsValue(42));
    }

    @Test
    void mapStorageValueAbsent() throws Exception {
        Map<String, Object> backing = new HashMap<>();
        backing.put("a", 42);
        valField.set(map, backing);
        assertFalse(map.containsValue(99));
    }

    @Test
    void emptyMapAlwaysReturnsFalse() throws Exception {
        // Fresh map already points to EMPTY_MAP sentinel.
        assertFalse(map.containsValue("anything"));
    }

    @Test
    void singleEntryReferenceEqualityMatches() throws Exception {
        String stored = "singleValue";
        valField.set(map, stored);
        assertTrue(map.containsValue(stored));
    }

    @Test
    void singleEntryEqualButDifferentReferenceFails() throws Exception {
        String stored = "singleValue";
        valField.set(map, stored);
        String differentRef = new String("singleValue");
        assertFalse(map.containsValue(differentRef));
    }

    @Test
    void singleEntryNullValueSearchNullFailsWhenStoredNonNull() throws Exception {
        String stored = "notNull";
        valField.set(map, stored);
        assertFalse(map.containsValue(null));
    }

    @Test
    void singleEntryStoredNullValueSearchNullSucceeds() throws Exception {
        map.put("k", null);
        assertTrue(map.containsValue(null));
    }

    @Test
    void singleEntryStoredNullValueSearchNonNullFails() throws Exception {
        map.put("k", null);
        assertFalse(map.containsValue("nonNull"));
    }

    @Test
    void arrayStorageEmptyInternalArray() throws Exception {
        Object[] empty = new Object[0];
        valField.set(map, empty);
        assertFalse(map.containsValue("anything"));
    }

    @Test
    void arrayStorageNullValueAbsent() throws Exception {
        Object[] arr = new Object[] { "k1", "v1", "k2", "v2" };
        valField.set(map, arr);
        assertFalse(map.containsValue(null));
    }

    @Test
    void mapStorageEmptyUnderlyingMap() throws Exception {
        Map<String, Object> emptyMap = new HashMap<>();
        valField.set(map, emptyMap);
        assertFalse(map.containsValue("irrelevant"));
    }

    @Test
    void mapStorageNullValuePresent() throws Exception {
        Map<String, Object> backing = new HashMap<>();
        backing.put("k", null);
        valField.set(map, backing);
        assertTrue(map.containsValue(null));
    }

    @Test
    void arrayStorageCustomObjectEquality() throws Exception {
        class EqualsTrue {

            @Override
            public boolean equals(Object o) {
                return true;
            }

            @Override
            public int hashCode() {
                return 1;
            }
        }
        EqualsTrue stored = new EqualsTrue();
        Object[] arr = new Object[] { "k", stored };
        valField.set(map, arr);
        assertTrue(map.containsValue(new EqualsTrue()));
    }

    @Test
    void singleEntryDifferentIntegerInstanceSameValueFails() throws Exception {
        Integer stored = Integer.valueOf(128);
        valField.set(map, stored);
        Integer other = new Integer(128);
        assertFalse(map.containsValue(other));
    }

    @Test
    void arrayStorageMalformedOddLengthArrayThrows() throws Exception {
        Object[] malformed = new Object[] { "k1", "v1", "lonelyKey" };
        valField.set(map, malformed);
        try {
            map.containsValue("any");
            fail("Expected ArrayIndexOutOfBoundsException");
        } catch (ArrayIndexOutOfBoundsException ignored) {
        }
    }

    @Test
    void internalValNullThrowsNullPointerException() throws Exception {
        valField.set(map, null);
        try {
            map.containsValue("anything");
            fail("Expected NullPointerException");
        } catch (NullPointerException ignored) {
        }
    }

    @Test
    void mapStorageUnderlyingMapThrowsException() throws Exception {
        @SuppressWarnings("unchecked")
        Map<String, Object> badMap = mock(Map.class);
        when(badMap.containsValue(any())).thenThrow(new IllegalStateException("boom"));
        valField.set(map, badMap);
        try {
            map.containsValue("trigger");
            fail("Expected IllegalStateException");
        } catch (IllegalStateException ex) {
            assertEquals("boom", ex.getMessage());
        }
    }

    @Test
    void arrayStorageEntryEqualsThrowsRuntimeException() throws Exception {
        class BadEquals {

            @Override
            public boolean equals(Object o) {
                throw new RuntimeException("equals failed");
            }
        }
        BadEquals bad = new BadEquals();
        Object[] arr = new Object[] { "k", bad };
        valField.set(map, arr);
        try {
            map.containsValue("any");
            fail("Expected RuntimeException");
        } catch (RuntimeException ex) {
            assertEquals("equals failed", ex.getMessage());
        }
    }
}
