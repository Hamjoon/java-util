package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_firstKey_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // By default the map is unsealed
        sealed = false;
        sealedSupplier = () -> sealed;
        // Populate a backing map with a known ordering (natural order)
        backingMap = new TreeMap<>();
        backingMap.put("b", 2);
        backingMap.put("a", 1);
        backingMap.put("c", 3);
        // Create the SealableNavigableMap instance under test
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        backingMap = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    @Test
    void testFirstKeyReturnsLowestKeyWhenUnsealed() {
        assertFalse(sealedSupplier.get(), "Map should start unsealed.");
        assertEquals("a", sealableMap.firstKey(), "firstKey() should return the smallest key according to natural ordering.");
    }

    @Test
    void testFirstKeyReturnsLowestKeyWhenSealed() {
        // switch to sealed state
        sealed = true;
        assertTrue(sealedSupplier.get(), "Map should be sealed.");
        // The read‑only view must still expose the correct first key
        assertEquals("a", sealableMap.firstKey(), "firstKey() must work irrespective of the sealed state.");
    }

    @Test
    void testFirstKeyThrowsWhenMapIsEmpty() {
        // Create an empty SealableNavigableMap
        SealableNavigableMap<String, Integer> emptyMap = new SealableNavigableMap<>(new TreeMap<>(), sealedSupplier);
        assertThrows(NoSuchElementException.class, emptyMap::firstKey, "firstKey() should throw NoSuchElementException for an empty map.");
    }

    @Test
    @DisplayName("firstKey_normal – natural ordering returns the smallest key")
    void testFirstKeyNormal() {
        // backingMap already contains keys "a","b","c"
        assertEquals("a", sealableMap.firstKey(), "With natural ordering the smallest key should be \"a\".");
    }

    @Test
    @DisplayName("firstKey_customComparator – reverse order comparator")
    void testFirstKeyCustomComparator() {
        // descending order
        Comparator<String> reverse = Comparator.reverseOrder();
        NavigableMap<String, Integer> reverseMap = new TreeMap<>(reverse);
        reverseMap.put("b", 2);
        reverseMap.put("a", 1);
        reverseMap.put("c", 3);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(reverseMap, sealedSupplier);
        assertEquals("c", map.firstKey(), "With a reverse comparator the \"lowest\" element is \"c\".");
    }

    @Test
    @DisplayName("firstKey_dynamicUpdate – map mutated after wrapper creation")
    void testFirstKeyDynamicUpdate() {
        NavigableMap<Integer, String> mutable = new TreeMap<>();
        mutable.put(5, "five");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(mutable, sealedSupplier);
        // mutate underlying map
        mutable.put(1, "one");
        assertEquals(1, map.firstKey(), "firstKey() must reflect updates made to the underlying map.");
    }

    @Test
    @DisplayName("firstKey_singleElement – map with a single entry")
    void testFirstKeySingleElement() {
        NavigableMap<Integer, String> single = new TreeMap<>();
        single.put(42, "answer");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(single, sealedSupplier);
        assertEquals(42, map.firstKey(), "When only one entry exists, firstKey() should return that key.");
    }

    @Test
    @DisplayName("firstKey_afterRemoval – smallest key after removing the previous first")
    void testFirstKeyAfterRemoval() {
        NavigableMap<Integer, String> mapUnder = new TreeMap<>();
        mapUnder.put(2, "two");
        mapUnder.put(3, "three");
        SealableNavigableMap<Integer, String> map = new SealableNavigableMap<>(mapUnder, sealedSupplier);
        // remove the current first key (2)
        mapUnder.remove(2);
        assertEquals(3, map.firstKey(), "After removing the original first key, firstKey() should return the next smallest key.");
    }

    @Test
    @DisplayName("firstKey_allKeysEqualComparator – comparator that treats all keys as equal")
    void testFirstKeyAllKeysEqualComparator() {
        Comparator<String> allEqual = (o1, o2) -> 0;
        NavigableMap<String, Integer> eqMap = new TreeMap<>(allEqual);
        eqMap.put("first", 1);
        eqMap.put("middle", 2);
        // this entry overwrites the previous ones
        eqMap.put("last", 3);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(eqMap, sealedSupplier);
        assertEquals("last", map.firstKey(), "When comparator treats all keys as equal, the last inserted key survives and becomes the firstKey.");
    }

    @Test
    @DisplayName("firstKey_nullKeyAllowed – map that permits a null key")
    void testFirstKeyNullKeyAllowed() {
        Comparator<String> nullsFirst = Comparator.nullsFirst(Comparator.naturalOrder());
        NavigableMap<String, Integer> nullMap = new TreeMap<>(nullsFirst);
        nullMap.put(null, 0);
        nullMap.put("a", 1);
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(nullMap, sealedSupplier);
        assertNull(map.firstKey(), "When a null key is present and the comparator treats null as lowest, firstKey() should return null.");
    }

    @Test
    @DisplayName("firstKey_incomparableKeys – heterogeneous keys cause ClassCastException")
    void testFirstKeyIncomparableKeys() {
        // Use a raw TreeMap to bypass compile‑time generics
        @SuppressWarnings("unchecked")
        NavigableMap // natural ordering
        rawMap = new TreeMap();
        // Integer key
        rawMap.put(1, "one");
        // The next put will throw ClassCastException because String cannot be compared to Integer
        assertThrows(ClassCastException.class, () -> {
            // attempt to insert heterogeneous key
            rawMap.put("a", "string");
            // If insertion succeeded (unlikely), calling firstKey would also trigger the exception
            new SealableNavigableMap<>(rawMap, sealedSupplier).firstKey();
        }, "Mixing incomparable key types should result in ClassCastException.");
    }

    @Test
    @DisplayName("firstKey_nullUnderlyingMap – delegate map is null")
    void testFirstKeyNullUnderlyingMap() {
        SealableNavigableMap<String, Integer> map = new SealableNavigableMap<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, map::firstKey, "If the underlying NavigableMap is null, firstKey() must throw NullPointerException.");
    }
}
