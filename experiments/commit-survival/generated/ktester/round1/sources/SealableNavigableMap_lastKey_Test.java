package com.cedarsoftware.util;

import java.util.NoSuchElementException;
import java.util.NavigableMap;
import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Comparator;
import java.util.TreeMap;
import org.mockito.Mock;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_lastKey_Test {

    /* ---------------------------------------------------------------------- */
    private static final String KEY_ONE = "one";

    private static final String KEY_TWO = "two";

    private static final String KEY_THREE = "three";

    private static final String KEY_Z = "z";

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    class MutableKey implements Comparable<MutableKey> {

        int id;

        MutableKey(int id) {
            this.id = id;
        }

        @Override
        public int compareTo(MutableKey o) {
            return Integer.compare(this.id, o.id);
        }
    }

    class MutableKey implements Comparable<MutableKey> {

        int id;

        MutableKey(int id) {
            this.id = id;
        }

        @Override
        public int compareTo(MutableKey o) {
            return Integer.compare(this.id, o.id);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // start each test un‑sealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // a simple mutable backing map
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put(KEY_ONE, 1);
        backingMap.put(KEY_TWO, 2);
        backingMap.put(KEY_THREE, 3);
        // the object under test
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @Test
    void lastKeyReturnsHighestKeyInNaturalOrder() {
        // "three" is lexicographically highest among the three keys
        assertEquals(KEY_THREE, sealableMap.lastKey(), "lastKey() should return the greatest key according to natural ordering");
    }

    @Test
    void lastKeyReflectsNewlyAddedMaximumKey() {
        sealableMap.put(KEY_Z, 26);
        assertEquals(KEY_Z, sealableMap.lastKey(), "After inserting a new maximum key, lastKey() should return it");
    }

    @Test
    void lastKeyWorksWhenMapIsSealed() {
        // make the map read‑only
        sealed = true;
        // read operations must still succeed
        assertEquals(KEY_THREE, sealableMap.lastKey(), "lastKey() must be callable even when the map is sealed");
    }

    @Test
    void lastKeyOnEmptyMapThrowsNoSuchElementException() {
        // Create a SealableNavigableMap with no backing entries
        SealableNavigableMap<String, Integer> emptyMap = new SealableNavigableMap<>(sealedSupplier);
        assertThrows(NoSuchElementException.class, emptyMap::lastKey, "Calling lastKey() on an empty map should throw NoSuchElementException");
    }

    /**
     * Verify that the key returned is the *same* instance that was stored
     * in the underlying map (reference equality). This matters for mutable
     * key objects.
     */
    @Test
    void lastKeyReturnsExactKeyInstanceStoredInBackingMap() {
        // mutable comparable key
        class MutableKey implements Comparable<MutableKey> {

            int id;

            MutableKey(int id) {
                this.id = id;
            }

            @Override
            public int compareTo(MutableKey o) {
                return Integer.compare(this.id, o.id);
            }
        }
        MutableKey keyA = new MutableKey(1);
        // highest
        MutableKey keyB = new MutableKey(2);
        NavigableMap<MutableKey, String> map = new TreeMap<>();
        map.put(keyA, "a");
        map.put(keyB, "b");
        SealableNavigableMap<MutableKey, String> sm = new SealableNavigableMap<>(map, sealedSupplier);
        assertSame(keyB, sm.lastKey(), "lastKey() must return the exact key instance stored in the backing map");
    }

    /**
     * The underlying map may throw IllegalArgumentException (e.g., a custom
     * implementation). The wrapper should propagate the exception unchanged.
     */
    @Test
    void lastKeyPropagatesIllegalArgumentException(@Mock NavigableMap<String, Integer> mockMap) {
        when(mockMap.lastKey()).thenThrow(new IllegalArgumentException("bad key"));
        SealableNavigableMap<String, Integer> sm = new SealableNavigableMap<>(mockMap, sealedSupplier);
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, sm::lastKey);
        assertEquals("bad key", ex.getMessage());
    }

    /**
     * The underlying map may throw ClassCastException (e.g., incomparable keys).
     * The wrapper should propagate the exception.
     */
    @Test
    void lastKeyPropagatesClassCastException(@Mock NavigableMap<String, Integer> mockMap) {
        when(mockMap.lastKey()).thenThrow(new ClassCastException("cannot compare"));
        SealableNavigableMap<String, Integer> sm = new SealableNavigableMap<>(mockMap, sealedSupplier);
        assertThrows(ClassCastException.class, sm::lastKey);
    }

    /**
     * The underlying map may throw UnsupportedOperationException (read‑only map).
     * The wrapper should propagate the exception.
     */
    @Test
    void lastKeyPropagatesUnsupportedOperationException(@Mock NavigableMap<String, Integer> mockMap) {
        when(mockMap.lastKey()).thenThrow(new UnsupportedOperationException("not supported"));
        SealableNavigableMap<String, Integer> sm = new SealableNavigableMap<>(mockMap, sealedSupplier);
        assertThrows(UnsupportedOperationException.class, sm::lastKey);
    }

    /**
     * TreeMap with a comparator that allows null keys and treats null as the
     * greatest element. The wrapper should return null as the highest key.
     */
    @Test
    void lastKeyReturnsNullWhenNullIsHighestKey() {
        Comparator<String> nullsLast = Comparator.nullsLast(Comparator.naturalOrder());
        TreeMap<String, Integer> map = new TreeMap<>(nullsLast);
        // highest according to comparator
        map.put(null, 0);
        map.put("a", 1);
        map.put("b", 2);
        SealableNavigableMap<String, Integer> sm = new SealableNavigableMap<>(map, sealedSupplier);
        assertNull(sm.lastKey(), "When the underlying map's highest key is null, lastKey() should return null");
    }

    /**
     * If SealableNavigableMap is constructed with a null backing map,
     * any operation that dereferences the delegate should throw NPE.
     */
    @Test
    void lastKeyThrowsNullPointerExceptionWhenBackingMapIsNull() {
        SealableNavigableMap<String, Integer> sm = new SealableNavigableMap<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, sm::lastKey, "Invoking lastKey() on a SealableNavigableMap with a null navMap should throw NPE");
    }
}
