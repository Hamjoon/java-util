package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.ConcurrentNavigableMap;
import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.Field;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_lowerKey_Test {

    /**
     * Map under test – created fresh for each test method.
     */
    private ConcurrentNavigableMapNullSafe<String, Integer> map;

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor (natural ordering) unless a test overrides it.
        map = new ConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and ensure no cross‑test contamination.
        map.clear();
        map = null;
    }

    @Test
    void lowerKey_normalKeys() {
        // given
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // when / then
        assertNull(map.lowerKey("a"), "There is no key lower than the smallest key");
        assertEquals("a", map.lowerKey("b"));
        assertEquals("b", map.lowerKey("c"));
    }

    @Test
    void lowerKey_keyNotPresent() {
        // given
        map.put("a", 1);
        map.put("c", 3);
        map.put("e", 5);
        // when the exact key is missing, lowerKey should act as if the key were inserted
        // and return the greatest key that is strictly less.
        assertEquals("c", map.lowerKey("d"));
        assertEquals("a", map.lowerKey("b"));
        // still the smallest existing key
        assertNull(map.lowerKey("a"));
    }

    @Test
    void lowerKey_withNullKey() {
        // given – null is allowed and considered the greatest element in the ascending view
        map.put("a", 1);
        map.put("b", 2);
        map.put(null, 99);
        // when / then
        assertEquals("b", map.lowerKey(null), "lowerKey(null) should return the greatest non‑null key");
        assertNull(map.lowerKey("a"), "The smallest key has no lower key, even when null is present");
        assertEquals("a", map.lowerKey("b"));
    }

    @Test
    void lowerKey_withCustomComparator() {
        // given – a map that orders strings in reverse (descending natural order)
        ConcurrentNavigableMapNullSafe<String, Integer> reverseMap = new ConcurrentNavigableMapNullSafe<>(Collections.reverseOrder());
        reverseMap.put("a", 1);
        reverseMap.put("b", 2);
        reverseMap.put("c", 3);
        // null is still treated as the greatest element in the *ascending* view,
        // which for a reverse‑ordered map means it is the *smallest* element.
        reverseMap.put(null, 99);
        // when / then – note the reversed ordering
        assertEquals("c", reverseMap.lowerKey("b"), "With reverse order, the key lower than 'b' is the next greater natural key");
        assertEquals("b", reverseMap.lowerKey("a"));
        assertEquals(null, reverseMap.lowerKey(null), "In a reverse‑ordered map, null is the smallest element, so there is no lower key");
        assertEquals("c", reverseMap.lowerKey("b"));
    }

    @Test
    void lowerKey_onEmptyMap() {
        // given – an empty map (no entries at all)
        // when / then
        assertNull(map.lowerKey("anyKey"));
        assertNull(map.lowerKey(null));
    }

    @Test
    void lowerKey_emptyMap_nullKey() {
        // JSON case: empty map, lowerKey(null) → null (already covered by onEmptyMap)
        assertNull(map.lowerKey(null), "Empty map should return null for lowerKey(null)");
    }

    @Test
    void lowerKey_mapContainsOnlyNonNullKeys_nullKey() {
        // JSON case: map with only "a","b" – lowerKey(null) → null
        map.put("a", 1);
        map.put("b", 2);
        assertNull(map.lowerKey(null), "When only non‑null keys exist, lowerKey(null) must be null");
    }

    @Test
    void lowerKey_mapContainsOnlyNullKey() {
        // JSON case: map contains only a null key – lowerKey(null) → null
        map.put(null, 0);
        assertNull(map.lowerKey(null), "Only a null key present – there is no lower key");
    }

    @Test
    void lowerKey_singleEntry() {
        // JSON case: map has a single entry "a" – lowerKey("a") → null
        map.put("a", 1);
        assertNull(map.lowerKey("a"));
    }

    @Test
    void lowerKey_greatestKey() {
        // JSON case: map "a","b","c" – lowerKey("c") → "b"
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        assertEquals("b", map.lowerKey("c"));
    }

    @Test
    void lowerKey_customComparator_byLength() {
        // JSON case: comparator orders by length (shorter first)
        Comparator<String> lengthAsc = Comparator.comparingInt(String::length);
        ConcurrentNavigableMapNullSafe<String, Integer> lengthMap = new ConcurrentNavigableMapNullSafe<>(lengthAsc);
        // length 1
        lengthMap.put("a", 1);
        // length 3
        lengthMap.put("ccc", 3);
        // key "bb" length 2 → lower key should be the greatest key with length < 2 i.e. "a"
        assertEquals("a", lengthMap.lowerKey("bb"));
    }

    @Test
    void lowerKey_customComparator_byLengthDesc() {
        // JSON case: comparator orders by length descending (longer first)
        Comparator<String> lengthDesc = Comparator.comparingInt(String::length).reversed();
        ConcurrentNavigableMapNullSafe<String, Integer> lengthDescMap = new ConcurrentNavigableMapNullSafe<>(lengthDesc);
        // length 1
        lengthDescMap.put("a", 1);
        // length 2
        lengthDescMap.put("bb", 2);
        // length 3
        lengthDescMap.put("ccc", 3);
        // In this ordering, keys less than "bb" are those with greater length → "ccc"
        assertEquals("ccc", lengthDescMap.lowerKey("bb"));
    }

    @Test
    void lowerKey_subMapView() {
        // JSON case: subMap view from "b" (inclusive) to "d" (exclusive)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        map.put("d", 4);
        ConcurrentNavigableMap<String, Integer> sub = map.subMap("b", true, "d", false);
        assertEquals("b", sub.lowerKey("c"), "Within subMap, lowerKey('c') should be 'b'");
    }

    @Test
    void lowerKey_tailMapView() {
        // JSON case: tailMap starting at "b" (inclusive)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        ConcurrentNavigableMap<String, Integer> tail = map.tailMap("b", true);
        assertNull(tail.lowerKey("b"), "In tailMap view, there is no key lower than the first key");
    }

    @Test
    void lowerKey_headMapView() {
        // JSON case: headMap up to "c" (exclusive)
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        ConcurrentNavigableMap<String, Integer> head = map.headMap("c", false);
        assertEquals("a", head.lowerKey("b"), "In headMap view, lowerKey('b') should be 'a'");
    }

    @Test
    void lowerKey_crossTypeComparator_toString() {
        // JSON case: comparator compares via toString, map holds "10" and "2", key is Integer 5
        Comparator<Object> toStringCmp = Comparator.comparing(Object::toString);
        ConcurrentNavigableMapNullSafe<Object, Integer> toStringMap = new ConcurrentNavigableMapNullSafe<>(toStringCmp);
        toStringMap.put("10", 10);
        toStringMap.put("2", 2);
        // "5".compareTo("10") > 0? Actually lexical: "5" > "2" and "5" < "10"? "5" > "10" because '5' > '1'.
        // The greatest key less than "5" is "2".
        assertEquals("2", toStringMap.lowerKey(5), "lowerKey(5) should return key \"2\" in lexical order");
    }

    @Test
    void lowerKey_classCastException_wrongKeyType() {
        // JSON case: map typed with String, passing Integer should cause ClassCastException
        map.put("a", 1);
        map.put("b", 2);
        assertThrows(ClassCastException.class, () -> map.lowerKey(42), "Passing an Integer to a String‑typed map must throw ClassCastException");
    }

    @Test
    void lowerKey_classCastException_maskedNullWithIncompatibleComparator() {
        // JSON case: comparator casts to String, lowerKey(null) triggers sentinel cast → CCE
        Comparator<Object> castToString = (o1, o2) -> ((String) o1).compareTo((String) o2);
        ConcurrentNavigableMapNullSafe<String, Integer> castMap = new ConcurrentNavigableMapNullSafe<>(castToString);
        castMap.put("a", 1);
        // The sentinel used for null is not a String → ClassCastException
        assertThrows(ClassCastException.class, () -> castMap.lowerKey(null), "Comparator that expects String should throw CCE when sentinel is passed");
    }

    @Test
    void lowerKey_illegalArgumentException_fromComparator() {
        // JSON case: comparator throws IllegalArgumentException for key "bad"
        Comparator<String> badKeyComparator = (k1, k2) -> {
            if ("bad".equals(k1) || "bad".equals(k2)) {
                throw new IllegalArgumentException("bad key");
            }
            return k1.compareTo(k2);
        };
        ConcurrentNavigableMapNullSafe<String, Integer> badMap = new ConcurrentNavigableMapNullSafe<>(badKeyComparator);
        badMap.put("good", 1);
        assertThrows(IllegalArgumentException.class, () -> badMap.lowerKey("bad"), "Comparator‑thrown IllegalArgumentException must propagate");
    }

    @Test
    void lowerKey_nullPointerException_internalMapNull() throws Exception {
        // JSON case: internalMap field set to null via reflection → NPE on lowerKey
        map.put("a", 1);
        // Access the protected field declared in AbstractConcurrentNullSafeMap
        Field internalMapField = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        // sabotage the internal map
        internalMapField.set(map, null);
        assertThrows(NullPointerException.class, () -> map.lowerKey("any"), "If internalMap is null, lowerKey should throw NullPointerException");
    }
}
