package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;

@ExtendWith(MockitoExtension.class)
class CompactMap_hashCode_Test {

    /**
     * Simple concrete subclass that lets us control the internal state.
     */
    private static class TestCompactMap<K, V> extends CompactMap<K, V> {

        private final int compactSize;

        TestCompactMap(int compactSize) {
            this.compactSize = compactSize;
        }

        @Override
        protected int compactSize() {
            return compactSize;
        }

        @Override
        protected Map<K, V> getNewMap() {
            // Use a plain HashMap as the backing map.
            return new HashMap<>();
        }

        @Override
        protected int computeKeyHashCode(Object key) {
            // Use the standard Objects.hashCode implementation – this is what the
            // production code does for non‑null keys. It also safely handles null.
            return Objects.hashCode(key);
        }

        @Override
        protected int computeValueHashCode(Object value) {
            // Same as for keys – rely on Objects.hashCode.
            return Objects.hashCode(value);
        }
    }

    class ThrowingMap<K, V> extends AbstractMap<K, V> {

        private final Set<Entry<K, V>> entries = new HashSet<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return entries;
        }

        @Override
        public int hashCode() {
            throw new RuntimeException("forced map hashCode failure");
        }
    }

    class ThrowingCompactMap<K, V> extends TestCompactMap<K, V> {

        private final Map<K, V> throwingMap = new ThrowingMap<>();

        ThrowingCompactMap(int compactSize) {
            super(compactSize);
        }

        @Override
        protected Map<K, V> getNewMap() {
            return throwingMap;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad key hashCode");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad value hashCode");
        }
    }

    class ThrowingMap<K, V> extends AbstractMap<K, V> {

        private final Set<Entry<K, V>> entries = new HashSet<>();

        @Override
        public Set<Entry<K, V>> entrySet() {
            return entries;
        }

        @Override
        public int hashCode() {
            throw new RuntimeException("forced map hashCode failure");
        }
    }

    class ThrowingCompactMap<K, V> extends TestCompactMap<K, V> {

        private final Map<K, V> throwingMap = new ThrowingMap<>();

        ThrowingCompactMap(int compactSize) {
            super(compactSize);
        }

        @Override
        protected Map<K, V> getNewMap() {
            return throwingMap;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad key hashCode");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new RuntimeException("bad value hashCode");
        }
    }

    /**
     * Helper to set the private {@code val} field via reflection.
     */
    private static void setInternalVal(CompactMap<?, ?> map, Object newVal) throws Exception {
        Field f = CompactMap.class.getDeclaredField("val");
        f.setAccessible(true);
        f.set(map, newVal);
    }

    @Test
    void hashCodeEmptyMap() {
        // compactSize = 5 (any value >=2 works for the empty case)
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        assertEquals(0, map.hashCode(), "Empty CompactMap must return hash code 0");
    }

    @Test
    void hashCodeSingleEntry() {
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        map.put("key", 42);
        int expected = Objects.hashCode("key") ^ Objects.hashCode(42);
        assertEquals(expected, map.hashCode(), "Hash code for a single entry should be keyHash ^ valueHash");
    }

    @Test
    void hashCodeCompactArray() {
        // compactSize = 5 → map stays in the Object[] representation for 3 entries.
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // Expected hash code is the sum of (keyHash ^ valueHash) for each entry.
        int expected = (Objects.hashCode("a") ^ Objects.hashCode(1)) + (Objects.hashCode("b") ^ Objects.hashCode(2)) + (Objects.hashCode("c") ^ Objects.hashCode(3));
        assertEquals(expected, map.hashCode(), "Hash code for compact array representation should sum per‑entry XORs");
    }

    @Test
    void hashCodeBackingMap() {
        // compactSize = 2 → inserting 3 entries forces a transition to a backing Map.
        CompactMap<String, Integer> map = new TestCompactMap<>(2);
        map.put("x", 10);
        map.put("y", 20);
        // triggers backing map
        map.put("z", 30);
        // When val is a Map, CompactMap delegates to that map's hashCode().
        // same contract as Map.hashCode()
        int expected = map.entrySet().hashCode();
        assertEquals(expected, map.hashCode(), "When using a backing map, CompactMap.hashCode() must equal the map's hashCode");
    }

    @Test
    void hashCodeConsistencyAfterMutations() {
        // Verify that hashCode changes appropriately after modifications.
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        assertEquals(0, map.hashCode());
        map.put("k1", 100);
        int afterFirst = Objects.hashCode("k1") ^ Objects.hashCode(100);
        assertEquals(afterFirst, map.hashCode());
        map.put("k2", 200);
        int afterSecond = afterFirst + (Objects.hashCode("k2") ^ Objects.hashCode(200));
        assertEquals(afterSecond, map.hashCode());
        map.remove("k1");
        int afterRemove = Objects.hashCode("k2") ^ Objects.hashCode(200);
        assertEquals(afterRemove, map.hashCode(), "Hash code must reflect removal of an entry");
    }

    @Test
    void hashCodeCorruptedObjectArray_oddLength() throws Exception {
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        // Force an odd‑length Object[] as internal storage.
        // length 3 (odd)
        Object[] corrupted = new Object[] { "k1", 1, "k2" };
        setInternalVal(map, corrupted);
        // The implementation will try to read i+1 for the last iteration → AIOOBE.
        assertThrows(ArrayIndexOutOfBoundsException.class, map::hashCode, "Odd‑length internal Object[] should cause ArrayIndexOutOfBoundsException");
    }

    @Test
    void hashCodeUnderlyingMapHashCodeThrows() throws Exception {
        // Custom map that throws from its hashCode().
        class ThrowingMap<K, V> extends AbstractMap<K, V> {

            private final Set<Entry<K, V>> entries = new HashSet<>();

            @Override
            public Set<Entry<K, V>> entrySet() {
                return entries;
            }

            @Override
            public int hashCode() {
                throw new RuntimeException("forced map hashCode failure");
            }
        }
        // Subclass CompactMap to inject the throwing map as the backing storage.
        class ThrowingCompactMap<K, V> extends TestCompactMap<K, V> {

            private final Map<K, V> throwingMap = new ThrowingMap<>();

            ThrowingCompactMap(int compactSize) {
                super(compactSize);
            }

            @Override
            protected Map<K, V> getNewMap() {
                return throwingMap;
            }
        }
        // compactSize = 2 ensures we transition to the backing map after 3 puts.
        CompactMap<String, Integer> map = new ThrowingCompactMap<>(2);
        map.put("a", 1);
        map.put("b", 2);
        // now val is the ThrowingMap
        map.put("c", 3);
        RuntimeException ex = assertThrows(RuntimeException.class, map::hashCode, "hashCode() should propagate RuntimeException from backing map");
        assertEquals("forced map hashCode failure", ex.getMessage());
    }

    @Test
    void hashCodeNullInternalVal_NullPointerException() throws Exception {
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        // Directly set the internal val to null.
        setInternalVal(map, null);
        // The method falls through to the single‑entry branch and dereferences
        // internal state, resulting in NPE.
        assertThrows(NullPointerException.class, map::hashCode, "Null internal val should cause NullPointerException");
    }

    @Test
    void hashCodeKeyObjectThrowsDuringHashCode() {
        class BadKey {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad key hashCode");
            }
        }
        CompactMap<Object, Integer> map = new TestCompactMap<>(5);
        map.put(new BadKey(), 123);
        RuntimeException ex = assertThrows(RuntimeException.class, map::hashCode, "Exception from key.hashCode() must propagate");
        assertEquals("bad key hashCode", ex.getMessage());
    }

    @Test
    void hashCodeValueObjectThrowsDuringHashCode() {
        class BadValue {

            @Override
            public int hashCode() {
                throw new RuntimeException("bad value hashCode");
            }
        }
        CompactMap<String, Object> map = new TestCompactMap<>(5);
        map.put("goodKey", new BadValue());
        RuntimeException ex = assertThrows(RuntimeException.class, map::hashCode, "Exception from value.hashCode() must propagate");
        assertEquals("bad value hashCode", ex.getMessage());
    }

    @Test
    void hashCodeConsistencyWithEquals() {
        CompactMap<String, String> map1 = new TestCompactMap<>(5);
        CompactMap<String, String> map2 = new TestCompactMap<>(5);
        map1.put("x", "y");
        map1.put("p", "q");
        map2.put("p", "q");
        map2.put("x", "y");
        assertTrue(map1.equals(map2), "Maps with identical entries must be equal");
        assertEquals(map1.hashCode(), map2.hashCode(), "Equal maps must have identical hash codes");
    }

    @Test
    void hashCodeObjectArrayWithNulls() {
        // This reproduces the JSON case: single entry where both key and value are null.
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        map.put(null, null);
        // In the compact array representation the keyHash and valueHash are both 0.
        int expected = 0;
        assertEquals(expected, map.hashCode(), "Hash code should be 0 when both key and value are null");
    }

    @Test
    void hashCodeObjectArrayTwoEntriesMatchesPrecomputed() {
        // Re‑creates the JSON example where expected hashCode is 160.
        CompactMap<String, String> map = new TestCompactMap<>(5);
        map.put("a", "1");
        map.put("b", "2");
        int expected = (Objects.hashCode("a") ^ Objects.hashCode("1")) + (Objects.hashCode("b") ^ Objects.hashCode("2"));
        assertEquals(160, expected, "Pre‑computed expected value should be 160");
        assertEquals(expected, map.hashCode(), "Hash code for two entries must match the pre‑computed sum");
    }

    @Test
    void hashCodeAfterRemovalBackToEmpty() {
        CompactMap<String, Integer> map = new TestCompactMap<>(5);
        map.put("only", 99);
        assertNotEquals(0, map.hashCode(), "Hash code should be non‑zero after insertion");
        map.remove("only");
        assertEquals(0, map.hashCode(), "After removing the sole entry, hashCode must return 0 (EMPTY_MAP)");
    }
}
