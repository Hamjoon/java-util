package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.Supplier;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import org.mockito.Mockito;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_toString_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    class SleepingTreeMap<K, V> extends TreeMap<K, V> {

        @Override
        public String toString() {
            // Trigger iteration over entrySet which will detect concurrent mods.
            Iterator<Entry<K, V>> it = entrySet().iterator();
            StringBuilder sb = new StringBuilder("{");
            while (it.hasNext()) {
                Entry<K, V> e = it.next();
                sb.append(e.getKey()).append("=").append(e.getValue());
                if (it.hasNext())
                    sb.append(", ");
                // Small pause after first element to allow other thread to modify.
                if (size() > 1) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            sb.append("}");
            return sb.toString();
        }
    }

    class SleepingTreeMap<K, V> extends TreeMap<K, V> {

        @Override
        public String toString() {
            // Trigger iteration over entrySet which will detect concurrent mods.
            Iterator<Entry<K, V>> it = entrySet().iterator();
            StringBuilder sb = new StringBuilder("{");
            while (it.hasNext()) {
                Entry<K, V> e = it.next();
                sb.append(e.getKey()).append("=").append(e.getValue());
                if (it.hasNext())
                    sb.append(", ");
                // Small pause after first element to allow other thread to modify.
                if (size() > 1) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
            sb.append("}");
            return sb.toString();
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // The map starts unsealed.
        sealed = false;
        sealedSupplier = () -> sealed;
        // Use the same concrete implementation that the production code expects.
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("one", 1);
        backingMap.put("two", 2);
        // include a null entry to exercise edge‑cases
        backingMap.put(null, null);
        // Create the SealableNavigableMap that wraps the backing map.
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the sealed flag so each test starts from a clean state.
        sealed = false;
    }

    /**
     * Helper to create a SealableNavigableMap with a supplied underlying map.
     */
    private <K, V> SealableNavigableMap<K, V> wrap(NavigableMap<K, V> map) {
        // sealedSupplier is still valid because it only reads the boolean flag.
        @SuppressWarnings("unchecked")
        Supplier<Boolean> supplier = (Supplier<Boolean>) sealedSupplier;
        return new SealableNavigableMap<>(map, supplier);
    }

    @Test
    void toStringMatchesUnderlyingMapInitially() {
        // The delegate map and the sealable wrapper should produce identical strings.
        assertEquals(backingMap.toString(), sealableMap.toString(), "toString() should delegate to the underlying map's toString()");
    }

    @Test
    void toStringReflectsMutationsWhenUnsealed() {
        // Mutate the map while it is unsealed.
        sealableMap.put("three", 3);
        sealableMap.put("four", 4);
        // The string representation must contain the newly added entries.
        // backingMap has been updated via the wrapper
        String expected = backingMap.toString();
        assertEquals(expected, sealableMap.toString(), "toString() should reflect mutations performed while the map is unsealed");
    }

    @Test
    void toStringRemainsStableWhenSealed() {
        // Add entries before sealing.
        sealableMap.put("alpha", 10);
        sealableMap.put("beta", 20);
        // Capture the representation before sealing.
        String beforeSeal = sealableMap.toString();
        // Seal the map – further mutations are prohibited.
        sealed = true;
        assertTrue(sealedSupplier.get(), "Supplier should now report a sealed state");
        // Attempting to modify should throw, but the string representation must stay the same.
        assertThrows(UnsupportedOperationException.class, () -> sealableMap.put("gamma", 30), "Mutations while sealed must be rejected");
        assertEquals(beforeSeal, sealableMap.toString(), "toString() should not change after a failed mutation while sealed");
    }

    @Test
    void toStringHandlesNullKeyAndValue() {
        // The map already contains a null key/value pair from setUp().
        String representation = sealableMap.toString();
        // Verify that the string contains the literal "null=null" fragment.
        assertTrue(representation.contains("null=null"), "String representation should include the null entry");
    }

    /* ---------- Empty map ---------- */
    @Test
    void toStringEmptyMap() {
        NavigableMap<String, String> empty = new TreeMap<>();
        SealableNavigableMap<String, String> wrapper = wrap(empty);
        assertEquals("{}", wrapper.toString(), "Empty map should produce \"{}\"");
    }

    /* ---------- Single entry map ---------- */
    @Test
    void toStringSingleEntryMap() {
        NavigableMap<String, String> single = new TreeMap<>();
        single.put("apple", "red");
        SealableNavigableMap<String, String> wrapper = wrap(single);
        assertEquals("{apple=red}", wrapper.toString(), "Single entry map must match underlying representation");
    }

    /* ---------- Multiple ordered entries ---------- */
    @Test
    void toStringMultipleOrderedEntries() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        SealableNavigableMap<Integer, String> wrapper = wrap(map);
        assertEquals("{1=one, 2=two, 3=three}", wrapper.toString(), "Ordered entries must appear in natural key order");
    }

    /* ---------- Map with null key/value (custom comparator) ---------- */
    @Test
    void toStringMapWithNullKeyAndValueCustomComparator() {
        Comparator<String> nullFriendly = Comparator.nullsFirst(String::compareTo);
        NavigableMap<String, String> map = new TreeMap<>(nullFriendly);
        map.put(null, null);
        map.put("a", "alpha");
        SealableNavigableMap<String, String> wrapper = wrap(map);
        assertEquals("{null=null, a=alpha}", wrapper.toString(), "Null key/value must be represented when comparator permits nulls");
    }

    /* ---------- Underlying map modified after construction ---------- */
    @Test
    void toStringUnderlyingMapModifiedAfterConstruction() {
        NavigableMap<String, String> mutable = new TreeMap<>();
        mutable.put("cat", "meow");
        mutable.put("dog", "bark");
        SealableNavigableMap<String, String> wrapper = wrap(mutable);
        // mutate after wrapper creation
        mutable.put("elephant", "trumpet");
        mutable.remove("cat");
        // re‑add to keep expected order
        mutable.put("cat", "meow");
        assertEquals("{cat=meow, dog=bark, elephant=trumpet}", wrapper.toString(), "toString() must reflect current state of underlying map");
    }

    /* ---------- Underlying map with overridden toString() (mock) ---------- */
    @Test
    void toStringUnderlyingMapWithMockedToString() {
        @SuppressWarnings("unchecked")
        NavigableMap<String, String> mockMap = mock(NavigableMap.class);
        when(mockMap.toString()).thenReturn("CUSTOM_MAP_STRING");
        SealableNavigableMap<String, String> wrapper = wrap(mockMap);
        assertEquals("CUSTOM_MAP_STRING", wrapper.toString(), "Wrapper should forward the mocked toString() result unchanged");
    }

    /* ---------- Duplicate values map ---------- */
    @Test
    void toStringDuplicateValuesMap() {
        NavigableMap<String, String> map = new TreeMap<>();
        map.put("apple", "red");
        map.put("banana", "yellow");
        map.put("cherry", "red");
        SealableNavigableMap<String, String> wrapper = wrap(map);
        assertEquals("{apple=red, banana=yellow, cherry=red}", wrapper.toString(), "Duplicate values must be displayed for each key");
    }

    /* ---------- Custom comparator reverse order ---------- */
    @Test
    void toStringCustomComparatorReverseOrder() {
        NavigableMap<Integer, String> map = new TreeMap<>(Comparator.reverseOrder());
        map.put(1, "one");
        map.put(2, "two");
        map.put(3, "three");
        SealableNavigableMap<Integer, String> wrapper = wrap(map);
        assertEquals("{3=three, 2=two, 1=one}", wrapper.toString(), "Reverse comparator should affect ordering in toString()");
    }

    /* ---------- Descending map view ---------- */
    @Test
    void toStringDescendingMapView() {
        NavigableMap<Integer, String> base = new TreeMap<>();
        base.put(1, "one");
        base.put(2, "two");
        NavigableMap<Integer, String> descending = base.descendingMap();
        SealableNavigableMap<Integer, String> wrapper = wrap(descending);
        assertEquals("{2=two, 1=one}", wrapper.toString(), "Descending view must preserve its own ordering");
    }

    /* ---------- Null values only ---------- */
    @Test
    void toStringNullValuesOnly() {
        NavigableMap<String, String> map = new TreeMap<>();
        map.put("a", null);
        map.put("b", null);
        SealableNavigableMap<String, String> wrapper = wrap(map);
        assertEquals("{a=null, b=null}", wrapper.toString(), "Null values should appear as \"null\" in the representation");
    }

    /* ---------- Multiple entries many (0‑9) ---------- */
    @Test
    void toStringMultipleEntriesMany() {
        NavigableMap<Integer, String> map = new TreeMap<>();
        String[] words = { "zero", "one", "two", "three", "four", "five", "six", "seven", "eight", "nine" };
        for (int i = 0; i < 10; i++) {
            map.put(i, words[i]);
        }
        SealableNavigableMap<Integer, String> wrapper = wrap(map);
        assertEquals("{0=zero, 1=one, 2=two, 3=three, 4=four, 5=five, 6=six, 7=seven, 8=eight, 9=nine}", wrapper.toString(), "Large map must preserve natural ordering and format");
    }

    /* ---------- Map cleared after construction ---------- */
    @Test
    void toStringMapClearedAfterConstruction() {
        NavigableMap<String, Integer> map = new TreeMap<>();
        map.put("x", 1);
        map.put("y", 2);
        SealableNavigableMap<String, Integer> wrapper = wrap(map);
        // clear through wrapper
        wrapper.clear();
        assertEquals("{}", wrapper.toString(), "After clear() the representation must be empty");
    }

    /* ---------- Special characters in keys/values ---------- */
    @Test
    void toStringSpecialCharactersInKeysValues() {
        NavigableMap<String, String> map = new TreeMap<>();
        map.put("a,b", "c}");
        SealableNavigableMap<String, String> wrapper = wrap(map);
        assertEquals("{a,b=c}}", wrapper.toString(), "Special characters must be shown verbatim");
    }

    /* ---------- Null underlying map ---------- */
    @Test
    void toStringNullUnderlyingMap() {
        // Directly construct with a null map – expect NPE when toString() is invoked.
        SealableNavigableMap<String, String> wrapper = new SealableNavigableMap<>(null, sealedSupplier);
        assertThrows(NullPointerException.class, wrapper::toString, "Calling toString() on a wrapper with null navMap should throw NPE");
    }

    /* ---------- Underlying map toString throws RuntimeException ---------- */
    @Test
    void toStringUnderlyingMapToStringThrowsRuntimeException() {
        NavigableMap<String, String> badMap = new TreeMap<>() {

            @Override
            public String toString() {
                throw new RuntimeException("deliberate");
            }
        };
        SealableNavigableMap<String, String> wrapper = wrap(badMap);
        RuntimeException ex = assertThrows(RuntimeException.class, wrapper::toString, "RuntimeException from underlying map should propagate");
        assertEquals("deliberate", ex.getMessage());
    }

    /* ---------- Concurrent modification during toString ---------- */
    @Test
    void toStringConcurrentModificationDuringToString() throws Exception {
        // Custom map that sleeps inside toString() to give another thread a chance to modify it.
        class SleepingTreeMap<K, V> extends TreeMap<K, V> {

            @Override
            public String toString() {
                // Trigger iteration over entrySet which will detect concurrent mods.
                Iterator<Entry<K, V>> it = entrySet().iterator();
                StringBuilder sb = new StringBuilder("{");
                while (it.hasNext()) {
                    Entry<K, V> e = it.next();
                    sb.append(e.getKey()).append("=").append(e.getValue());
                    if (it.hasNext())
                        sb.append(", ");
                    // Small pause after first element to allow other thread to modify.
                    if (size() > 1) {
                        try {
                            Thread.sleep(10);
                        } catch (InterruptedException ignored) {
                        }
                    }
                }
                sb.append("}");
                return sb.toString();
            }
        }
        SleepingTreeMap<Integer, String> map = new SleepingTreeMap<>();
        map.put(1, "one");
        map.put(2, "two");
        SealableNavigableMap<Integer, String> wrapper = wrap(map);
        ExecutorService exec = Executors.newSingleThreadExecutor();
        Future<?> future = exec.submit(() -> {
            // Wait a moment and then modify the map while toString() is in progress.
            try {
                Thread.sleep(5);
            } catch (InterruptedException ignored) {
            }
            // structural modification
            map.put(3, "three");
        });
        // Expect ConcurrentModificationException to be propagated.
        assertThrows(ConcurrentModificationException.class, wrapper::toString, "Concurrent modification during toString() should be propagated");
        // ensure background thread completed
        future.get();
        exec.shutdownNow();
    }
}
