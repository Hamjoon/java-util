package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class TrackingMap_hashCode_Test {

    @Mock
    private Map<String, Integer> // mocked internal map
    mockMap;

    // object under test
    private TrackingMap<String, Integer> trackingMap;

    class ConstantHash {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("bad key hash");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad value hash");
        }
    }

    class ConstantHash {

        @Override
        public int hashCode() {
            return 42;
        }
    }

    class BadKey {

        @Override
        public int hashCode() {
            throw new IllegalArgumentException("bad key hash");
        }
    }

    class BadValue {

        @Override
        public int hashCode() {
            throw new IllegalStateException("bad value hash");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh TrackingMap that wraps the mocked map before each test
        trackingMap = new TrackingMap<>(mockMap);
    }

    @Test
    void hashCode_delegatesToInternalMap_whenUsingMock() {
        // GIVEN – the mocked internal map returns a deterministic hash code
        when(mockMap.hashCode()).thenReturn(42_424);
        // WHEN
        int result = trackingMap.hashCode();
        // THEN
        assertEquals(42_424, result, "TrackingMap.hashCode() should return the hash code of the wrapped map");
        // ensure delegation happened exactly once
        verify(mockMap, times(1)).hashCode();
    }

    @Test
    void hashCode_matchesUnderlyingHashMap_forRealMap() {
        // GIVEN – a real backing map with some entries
        Map<String, Integer> realMap = new HashMap<>();
        realMap.put("one", 1);
        realMap.put("two", 2);
        TrackingMap<String, Integer> realTrackingMap = new TrackingMap<>(realMap);
        // WHEN
        int trackingHash = realTrackingMap.hashCode();
        int backingHash = realMap.hashCode();
        // THEN
        assertEquals(backingHash, trackingHash, "TrackingMap.hashCode() must be identical to the hashCode of the wrapped map");
    }

    @Test
    void hashCode_reflectsChangesInUnderlyingMap() {
        // GIVEN – a mutable backing map
        Map<String, Integer> mutableMap = new HashMap<>();
        mutableMap.put("alpha", 100);
        TrackingMap<String, Integer> mutableTrackingMap = new TrackingMap<>(mutableMap);
        // Capture hash code after initial state
        int hashBefore = mutableTrackingMap.hashCode();
        // WHEN – mutate the underlying map
        mutableMap.put("beta", 200);
        int hashAfter = mutableTrackingMap.hashCode();
        // THEN – hash codes must differ because the underlying map changed
        assertNotEquals(hashBefore, hashAfter, "TrackingMap.hashCode() should change when the wrapped map's contents change");
    }

    @Test
    void hashCode_emptyUnderlyingMap_returnsZero() {
        // GIVEN an empty HashMap wrapped by TrackingMap
        Map<String, Integer> emptyMap = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(emptyMap);
        // WHEN
        int hash = tm.hashCode();
        // THEN HashMap's hashCode for an empty map is defined as 0
        assertEquals(0, hash, "Hash code of an empty map should be 0");
    }

    @Test
    void hashCode_nonEmptyHashMap_returnsExpectedValue() {
        // GIVEN a HashMap with two entries {"a"=1, "b"=2}
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        // WHEN
        int hash = tm.hashCode();
        // THEN – compute expected value manually (("a".hashCode() ^ 1) + ("b".hashCode() ^ 2))
        int expected = ("a".hashCode() ^ 1) + ("b".hashCode() ^ 2);
        assertEquals(expected, hash, "Hash code should equal the sum of entry hash codes (192 for given data)");
    }

    @Test
    void hashCode_mapWithNullKey_returnsEntryHash() {
        // GIVEN a map containing a single entry {null="value"}
        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        // WHEN
        int hash = tm.hashCode();
        // THEN entry hash = 0 ^ "value".hashCode()
        int expected = 0 ^ "value".hashCode();
        assertEquals(expected, hash, "Hash code with null key should equal value's hashCode");
    }

    @Test
    void hashCode_mapWithNullValue_returnsEntryHash() {
        // GIVEN a map containing a single entry {"key"=null}
        Map<String, String> map = new HashMap<>();
        map.put("key", null);
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        // WHEN
        int hash = tm.hashCode();
        // THEN entry hash = "key".hashCode() ^ 0
        int expected = "key".hashCode() ^ 0;
        assertEquals(expected, hash, "Hash code with null value should equal key's hashCode");
    }

    @Test
    void hashCode_mapWithBothKeyAndValueNull_returnsZero() {
        // GIVEN a map containing a single entry {null=null}
        Map<String, String> map = new HashMap<>();
        map.put(null, null);
        TrackingMap<String, String> tm = new TrackingMap<>(map);
        // WHEN
        int hash = tm.hashCode();
        // THEN entry hash = 0 ^ 0 = 0
        assertEquals(0, hash, "Hash code with both key and value null should be 0");
    }

    @Test
    void hashCode_mapWithCustomObjectsConstantHash_returnsZero() {
        // GIVEN a map with custom objects whose hashCode() always returns 42
        class ConstantHash {

            @Override
            public int hashCode() {
                return 42;
            }
        }
        ConstantHash key = new ConstantHash();
        ConstantHash value = new ConstantHash();
        Map<ConstantHash, ConstantHash> map = new HashMap<>();
        map.put(key, value);
        TrackingMap<ConstantHash, ConstantHash> tm = new TrackingMap<>(map);
        // WHEN
        int hash = tm.hashCode();
        // THEN entry hash = 42 ^ 42 = 0, map hash = 0
        assertEquals(0, hash, "Map with entries whose key/value hashCodes cancel out should have hash 0");
    }

    @Test
    void hashCode_LinkedHashMapImplementation_returnsSameAsHashMap() {
        // GIVEN a LinkedHashMap with entries {"a"=1, "b"=2}
        Map<String, Integer> linked = new LinkedHashMap<>();
        linked.put("a", 1);
        linked.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(linked);
        // WHEN
        int hash = tm.hashCode();
        // THEN map hashCode is independent of iteration order, same as HashMap case
        int expected = ("a".hashCode() ^ 1) + ("b".hashCode() ^ 2);
        assertEquals(expected, hash, "LinkedHashMap hashCode should match HashMap hashCode for same entries");
    }

    @Test
    void hashCode_singleEntryAfterPut_returnsCorrectHash() {
        // GIVEN an empty map, then put a single entry
        Map<String, Integer> map = new HashMap<>();
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        tm.put("a", 1);
        // WHEN
        int hash = tm.hashCode();
        // THEN entry hash = "a".hashCode() ^ 1
        int expected = "a".hashCode() ^ 1;
        assertEquals(expected, hash, "Hash code after a single put should equal entry hash");
    }

    @Test
    void hashCode_afterRemovalOfEntry_returnsRemainingHash() {
        // GIVEN map with {"a"=1, "b"=2}
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        map.put("b", 2);
        TrackingMap<String, Integer> tm = new TrackingMap<>(map);
        // WHEN remove "b"
        tm.remove("b");
        int hash = tm.hashCode();
        // THEN only entry {"a"=1} remains
        int expected = "a".hashCode() ^ 1;
        assertEquals(expected, hash, "Hash code after removal should reflect remaining entries");
    }

    @Test
    void hashCode_internalMapIsNull_throwsNullPointerException() throws Exception {
        // GIVEN a TrackingMap instance with its internalMap set to null via reflection
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        Field f = TrackingMap.class.getDeclaredField("internalMap");
        f.setAccessible(true);
        f.set(tm, null);
        // WHEN / THEN
        assertThrows(NullPointerException.class, tm::hashCode, "Calling hashCode when internalMap is null should throw NPE");
    }

    @Test
    void hashCode_internalMapCustomImplementationThrowsIllegalStateException() {
        // GIVEN a custom Map whose hashCode() always throws IllegalStateException
        Map<String, Integer> badMap = new AbstractMap<>() {

            @Override
            public Set<Entry<String, Integer>> entrySet() {
                return Collections.emptySet();
            }

            @Override
            public int hashCode() {
                throw new IllegalStateException("bad hashCode");
            }
        };
        TrackingMap<String, Integer> tm = new TrackingMap<>(new HashMap<>());
        tm.setWrappedMap(badMap);
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, tm::hashCode, "IllegalStateException from underlying map should propagate");
        assertEquals("bad hashCode", ex.getMessage());
    }

    @Test
    void hashCode_keyWithFaultyHashCode_throwsIllegalArgumentException() {
        // GIVEN a key whose hashCode throws IllegalArgumentException
        class BadKey {

            @Override
            public int hashCode() {
                throw new IllegalArgumentException("bad key hash");
            }
        }
        Map<BadKey, Integer> map = new HashMap<>();
        map.put(new BadKey(), 1);
        TrackingMap<BadKey, Integer> tm = new TrackingMap<>(map);
        // WHEN / THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, tm::hashCode, "Exception from key.hashCode() should propagate");
        assertEquals("bad key hash", ex.getMessage());
    }

    @Test
    void hashCode_valueWithFaultyHashCode_throwsIllegalStateException() {
        // GIVEN a value whose hashCode throws IllegalStateException
        class BadValue {

            @Override
            public int hashCode() {
                throw new IllegalStateException("bad value hash");
            }
        }
        Map<String, BadValue> map = new HashMap<>();
        map.put("key", new BadValue());
        TrackingMap<String, BadValue> tm = new TrackingMap<>(map);
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, tm::hashCode, "Exception from value.hashCode() should propagate");
        assertEquals("bad value hash", ex.getMessage());
    }
}
