package com.cedarsoftware.util;

import java.util.function.Supplier;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class SealableNavigableMap_higherEntry_Test {

    private NavigableMap<String, Integer> backingMap;

    private SealableNavigableMap<String, Integer> sealableMap;

    private Supplier<Boolean> sealedSupplier;

    private boolean sealed;

    @BeforeEach
    void setupBeforeEach() {
        // start unsealed for each test
        sealed = false;
        sealedSupplier = () -> sealed;
        // use the same concrete map implementation that the production class expects
        backingMap = new ConcurrentNavigableMapNullSafe<>();
        backingMap.put("a", 1);
        backingMap.put("b", 2);
        backingMap.put("c", 3);
        sealableMap = new SealableNavigableMap<>(backingMap, sealedSupplier);
        // default: map is un‑sealed; each test will initialise its own backing map
        sealed = false;
        sealedSupplier = () -> sealed;
    }

    @AfterEach
    void teardownAfterEach() {
        // clear references – not strictly required but keeps the pattern consistent
        backingMap = null;
        sealableMap = null;
        sealedSupplier = null;
    }

    /**
     * Helper that creates a {@link SealableNavigableMap} wrapping the supplied
     * backing map.  The unchecked cast is safe for test purposes.
     */
    private void initSealableMap(NavigableMap<?, ?> map) {
        @SuppressWarnings("unchecked")
        NavigableMap<Object, Object> m = (NavigableMap<Object, Object>) map;
        this.backingMap = m;
        this.sealableMap = new SealableNavigableMap<>(m, sealedSupplier);
    }

    /* -------------------------------------------------------------
     *  BASIC FORWARD BEHAVIOUR
     * ------------------------------------------------------------- */
    static Stream<Arguments> basicForwardProvider() {
        return Stream.of(// key 1 → expect entry (2, "b")
        Arguments.of(1, new AbstractMap.SimpleEntry<>(2, "b")), // greatest key 3 → null
        Arguments.of(3, null), // null key on a natural‑ordering map → NPE
        Arguments.of(null, NullPointerException.class), // wrong type (String) on an Integer‑keyed map → CCE
        Arguments.of("foo", ClassCastException.class), // empty map case – any key returns null
        Arguments.of(42, null));
    }

    /* -------------------------------------------------------------
     *  CUSTOM COMPARATOR (REVERSE ORDER)
     * ------------------------------------------------------------- */
    static Stream<Arguments> reverseComparatorProvider() {
        return Stream.of(Arguments.of("b", new AbstractMap.SimpleEntry<>("a", 1)), Arguments.of("c", null));
    }

    /* -------------------------------------------------------------
     *  NON‑EXISTENT INTERMEDIATE KEY
     * ------------------------------------------------------------- */
    static Stream<Arguments> intermediateKeyProvider() {
        return Stream.of(Arguments.of(2, new AbstractMap.SimpleEntry<>(3, "c")), Arguments.of(4, new AbstractMap.SimpleEntry<>(5, "e")), Arguments.of(0, new AbstractMap.SimpleEntry<>(1, "a")));
    }

    /* -------------------------------------------------------------
     *  SINGLE ENTRY MAP
     * ------------------------------------------------------------- */
    static Stream<Arguments> singleEntryProvider() {
        return Stream.of(Arguments.of(5, new AbstractMap.SimpleEntry<>(10, "single")), Arguments.of(10, null));
    }

    /* -------------------------------------------------------------
     *  NULLS‑FIRST COMPARATOR
     * ------------------------------------------------------------- */
    static Stream<Arguments> nullsFirstProvider() {
        return Stream.of(Arguments.of(null, new AbstractMap.SimpleEntry<>(1, "one")), Arguments.of(-1, new AbstractMap.SimpleEntry<>(1, "one")));
    }

    /* -------------------------------------------------------------
     *  COMPARATOR EXCEPTION PROPAGATION
     * ------------------------------------------------------------- */
    static Stream<Arguments> comparatorExceptionProvider() {
        return Stream.of(Arguments.of(5, IllegalArgumentException.class), Arguments.of(null, NullPointerException.class), Arguments.of(11, RuntimeException.class));
    }

    @Test
    void higherEntryReturnsNextHigherKey() {
        Map.Entry<String, Integer> entry = sealableMap.higherEntry("a");
        assertNotNull(entry, "higherEntry should return a non‑null entry for key \"a\"");
        assertEquals("b", entry.getKey(), "The next higher key after \"a\" should be \"b\"");
        assertEquals(Integer.valueOf(2), entry.getValue(), "Value associated with \"b\" should be 2");
    }

    @Test
    void higherEntryReturnsNullWhenNoHigherKey() {
        // "c" is the greatest key in the map
        Map.Entry<String, Integer> entry = sealableMap.higherEntry("c");
        assertNull(entry, "higherEntry should return null when there is no higher key");
    }

    @Test
    void higherEntryReflectsDynamicChanges() {
        // initially, higherEntry("b") -> "c"
        Map.Entry<String, Integer> before = sealableMap.higherEntry("b");
        assertNotNull(before);
        assertEquals("c", before.getKey());
        // add a new key that is higher than "b" but lower than "c"
        sealableMap.put("bb", 22);
        Map.Entry<String, Integer> after = sealableMap.higherEntry("b");
        assertNotNull(after);
        // natural ordering: "bb" comes after "b" and before "c"
        assertEquals("bb", after.getKey(), "higherEntry should reflect newly added keys");
        assertEquals(Integer.valueOf(22), after.getValue());
    }

    @Test
    void higherEntryEntryIsImmutableWhenSealed() {
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        map.put(2, "b");
        initSealableMap(map);
        Map.Entry<Integer, String> entry = sealableMap.higherEntry(1);
        assertNotNull(entry);
        // seal the map
        sealed = true;
        assertThrows(UnsupportedOperationException.class, () -> entry.setValue("new"), "Modifying a returned entry must fail when the map is sealed");
    }

    @Test
    void higherEntryWorksWhenMapIsSealedForReadOnlyAccess() {
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        map.put(2, "b");
        // map is sealed, but reads are allowed
        sealed = true;
        initSealableMap(map);
        Map.Entry<Integer, String> entry = sealableMap.higherEntry(1);
        assertNotNull(entry);
        assertEquals(2, entry.getKey());
        assertEquals("b", entry.getValue());
    }

    @ParameterizedTest(name = "higherEntry basic – key={0}")
    @MethodSource("basicForwardProvider")
    void testHigherEntryBasic(Object key, Object expected) {
        // -----------------------------------------------------------------
        // Build the appropriate backing map for each case
        // -----------------------------------------------------------------
        if (expected instanceof Class) {
            // For exception‑expecting cases we can use a plain TreeMap
            initSealableMap(new TreeMap<Integer, String>());
        } else if (key instanceof Integer && ((Integer) key) == 42) {
            // Empty map scenario
            initSealableMap(new TreeMap<Integer, String>());
        } else if (key == null) {
            // Null key on natural ordering → TreeMap will throw NPE
            initSealableMap(new TreeMap<Integer, String>());
        } else if (key instanceof String) {
            // Wrong key type on Integer map → ClassCastException from TreeMap
            initSealableMap(new TreeMap<Integer, String>());
        } else {
            // Normal map {1→"a", 2→"b", 3→"c"}
            TreeMap<Integer, String> map = new TreeMap<>();
            map.put(1, "a");
            map.put(2, "b");
            map.put(3, "c");
            initSealableMap(map);
        }
        // -----------------------------------------------------------------
        // Execute and verify
        // -----------------------------------------------------------------
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> ex = (Class<? extends Throwable>) expected;
            assertThrows(ex, () -> sealableMap.higherEntry(key));
        } else if (expected == null) {
            assertNull(sealableMap.higherEntry(key));
        } else {
            @SuppressWarnings("unchecked")
            Map.Entry<Object, Object> expectedEntry = (Map.Entry<Object, Object>) expected;
            Map.Entry<Object, Object> actual = sealableMap.higherEntry(key);
            assertNotNull(actual, "higherEntry should not be null");
            assertEquals(expectedEntry.getKey(), actual.getKey(), "Returned key does not match expected");
            assertEquals(expectedEntry.getValue(), actual.getValue(), "Returned value does not match expected");
        }
    }

    /* -------------------------------------------------------------
     *  LIVE VIEW SEMANTICS
     * ------------------------------------------------------------- */
    @Test
    void higherEntryLiveViewUpdatesUnderlyingMap() {
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        initSealableMap(map);
        Map.Entry<Integer, String> entry = sealableMap.higherEntry(1);
        assertNotNull(entry, "Entry for key 1 must exist");
        // live view mutation
        entry.setValue("newValue");
        // Verify the underlying map reflects the mutation
        assertEquals("newValue", map.get(2));
        // Subsequent call must see the updated value
        Map.Entry<Integer, String> after = sealableMap.higherEntry(1);
        assertEquals("newValue", after.getValue());
    }

    @ParameterizedTest(name = "reverse comparator – key={0}")
    @MethodSource("reverseComparatorProvider")
    void testHigherEntryReverseComparator(String key, Object expected) {
        Comparator<String> reverse = Comparator.reverseOrder();
        TreeMap<String, Integer> map = new TreeMap<>(reverse);
        map.put("c", 3);
        map.put("b", 2);
        map.put("a", 1);
        initSealableMap(map);
        if (expected == null) {
            assertNull(sealableMap.higherEntry(key));
        } else {
            @SuppressWarnings("unchecked")
            Map.Entry<String, Integer> exp = (Map.Entry<String, Integer>) expected;
            Map.Entry<String, Integer> actual = sealableMap.higherEntry(key);
            assertNotNull(actual);
            assertEquals(exp.getKey(), actual.getKey());
            assertEquals(exp.getValue(), actual.getValue());
        }
    }

    @ParameterizedTest(name = "intermediate key – key={0}")
    @MethodSource("intermediateKeyProvider")
    void testHigherEntryIntermediateKey(Integer key, Map.Entry<Integer, String> expected) {
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        map.put(3, "c");
        map.put(5, "e");
        initSealableMap(map);
        Map.Entry<Integer, String> actual = sealableMap.higherEntry(key);
        assertNotNull(actual);
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getValue(), actual.getValue());
    }

    @ParameterizedTest(name = "single entry map – key={0}")
    @MethodSource("singleEntryProvider")
    void testHigherEntrySingleEntry(Integer key, Object expected) {
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(10, "single");
        initSealableMap(map);
        if (expected == null) {
            assertNull(sealableMap.higherEntry(key));
        } else {
            @SuppressWarnings("unchecked")
            Map.Entry<Integer, String> exp = (Map.Entry<Integer, String>) expected;
            Map.Entry<Integer, String> actual = sealableMap.higherEntry(key);
            assertNotNull(actual);
            assertEquals(exp.getKey(), actual.getKey());
            assertEquals(exp.getValue(), actual.getValue());
        }
    }

    @ParameterizedTest(name = "nullsFirst comparator – key={0}")
    @MethodSource("nullsFirstProvider")
    void testHigherEntryNullsFirst(Object key, Map.Entry<Integer, String> expected) {
        Comparator<Integer> cmp = Comparator.nullsFirst(Integer::compareTo);
        TreeMap<Integer, String> map = new TreeMap<>(cmp);
        map.put(null, "nullVal");
        map.put(1, "one");
        initSealableMap(map);
        Map.Entry<Integer, String> actual = sealableMap.higherEntry(key);
        assertNotNull(actual);
        assertEquals(expected.getKey(), actual.getKey());
        assertEquals(expected.getValue(), actual.getValue());
    }

    /* -------------------------------------------------------------
     *  AFTER REMOVAL OF HIGHER ENTRY
     * ------------------------------------------------------------- */
    @Test
    void higherEntryAfterRemovalOfHigherEntry() {
        TreeMap<Integer, String> map = new TreeMap<>();
        map.put(1, "a");
        map.put(2, "b");
        map.put(3, "c");
        initSealableMap(map);
        Map.Entry<Integer, String> entry = sealableMap.higherEntry(1);
        assertNotNull(entry);
        assertEquals(2, entry.getKey());
        // Remove the entry we just retrieved
        map.remove(entry.getKey());
        Map.Entry<Integer, String> after = sealableMap.higherEntry(1);
        assertNotNull(after);
        assertEquals(3, after.getKey());
        assertEquals("c", after.getValue());
    }

    @ParameterizedTest(name = "comparator exception – key={0}")
    @MethodSource("comparatorExceptionProvider")
    void testHigherEntryComparatorExceptions(Object key, Class<? extends Throwable> expectedException) {
        Comparator<Object> throwingComparator = (o1, o2) -> {
            if (o1 == null)
                throw new NullPointerException();
            if (o1 instanceof Integer) {
                int i = (Integer) o1;
                if (i == 5)
                    throw new IllegalArgumentException();
                if (i > 10)
                    throw new RuntimeException();
            }
            // fallback to natural ordering for other values
            @SuppressWarnings("unchecked")
            Comparable<Object> c1 = (Comparable<Object>) o1;
            return c1.compareTo(o2);
        };
        TreeMap<Object, String> map = new TreeMap<>(throwingComparator);
        map.put(5, "five");
        map.put(6, "six");
        map.put(11, "eleven");
        initSealableMap(map);
        assertThrows(expectedException, () -> sealableMap.higherEntry(key));
    }
}
