package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class TrackingMap_containsValue_Test {

    private TrackingMap<String, String> trackingMap;

    private Map<String, String> backingMap;

    /**
     * Equality based solely on logical id
     */
    static class CustomEqualsObject {

        final int id;

        CustomEqualsObject(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof CustomEqualsObject))
                return false;
            return id == ((CustomEqualsObject) o).id;
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    /**
     * Mutable holder – uses default Object.equals (identity)
     */
    static class MutableHolder {

        String content;

        MutableHolder(String content) {
            this.content = content;
        }
    }

    /**
     * Value whose equals method deliberately throws
     */
    static class EvilEquals {

        @Override
        public boolean equals(Object obj) {
            throw new IllegalStateException("Deliberate exception from equals()");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a simple HashMap as the backing map for deterministic behaviour
        backingMap = new HashMap<>();
        trackingMap = new TrackingMap<>(backingMap);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear references to help GC; not strictly required
        trackingMap = null;
        backingMap = null;
    }

    // ---------- Parameter source for normal/edge cases ----------
    static Stream<Arguments> provideContainsValueNormalCases() {
        return Stream.of(// group: containsValue tests
        Arguments.of("Map contains the queried value (Integer 1) among its entries; should return true.", List.of(Map.entry("k1", 1), Map.entry("k2", 2)), 1, true), Arguments.of("Map does not contain the queried value (Integer 3); should return false.", List.of(Map.entry("k1", 1), Map.entry("k2", 2)), 3, false), Arguments.of("Map has a key mapped to null; containsValue(null) must return true.", List.of(Map.entry("nullKey", null), Map.entry("nonNull", "value")), null, true), Arguments.of("Empty map; any value query must return false.", Collections.emptyList(), "any", false), Arguments.of("Map contains duplicate values (both keys map to Integer 5); containsValue should return true.", List.of(Map.entry("a", 5), Map.entry("b", 5)), 5, true), Arguments.of("Map contains a Long value 5L but the query is an Integer 5; equals() differs, so should return false.", List.of(Map.entry("longKey", 5L)), 5, false), // group: containsValue edge cases
        Arguments.of("Map contains the Boolean value true; containsValue(true) must return true.", List.of(Map.entry("bool", true)), true, true), Arguments.of("Map contains Double.NaN; Map.containsValue uses Double.equals which treats two NaN values as equal, so containsValue(Double.NaN) should return true.", List.of(Map.entry("nan", Double.NaN)), Double.NaN, true), Arguments.of("Map stores an instance of a custom class whose equals() method returns true when compared with another instance having the same logical id.", List.of(Map.entry("custom", new CustomEqualsObject(42))), new CustomEqualsObject(42), true), Arguments.of("Map has an entry where key and value are the same String \"key\"; containsValue(\"key\") must return true.", List.of(Map.entry("key", "key")), "key", true), Arguments.of("Map contains multiple entries, some with null values; containsValue(null) should return true even when there are other non‑null values.", List.of(Map.entry("n1", null), Map.entry("n2", "notNull"), Map.entry("n3", null)), null, true), Arguments.of("Map holds an Integer value 5; query with a Short value 5 (different type) must return false because Integer.equals(Short) is false.", List.of(Map.entry("int", 5)), (short) 5, false), Arguments.of("After putting a new value for an existing key (replacing \"oldValue\" with \"newValue\"), containsValue(\"oldValue\") must return false.", List.of(Map.entry("k", "oldValue")), // after replacement we will replace before query
        "oldValue", // after replacement we will replace before query
        false, // flag indicating we need to replace before checking
        true), Arguments.of("Following the replacement in the previous case, containsValue(\"newValue\") must return true.", List.of(Map.entry("k", "oldValue")), "newValue", true, // replace before checking
        true), Arguments.of("After removing the entry that mapped to \"removedValue\", containsValue(\"removedValue\") must return false.", List.of(Map.entry("k", "removedValue")), "removedValue", false, // remove before checking
        false), Arguments.of("Map stores a mutable object (e.g., a holder) by reference. Even if the internal state of the object changes after insertion, containsValue still returns true because equality is based on object identity (default equals).", List.of(Map.entry("holder", new MutableHolder("initial"))), new MutableHolder("changed"), true, // mutate after insertion, then query the same instance (original)
        false));
    }

    @Test
    void containsValueTrueDoesNotCountAsAccess() {
        trackingMap.put("a", "apple");
        trackingMap.put("b", "banana");
        trackingMap.put("c", "cherry");
        // containsValue should return true
        assertTrue(trackingMap.containsValue("banana"));
        // Since containsValue does not count as an access, expungeUnused should remove all entries
        trackingMap.expungeUnused();
        assertEquals(0, trackingMap.size(), "All entries should be removed because no key was accessed");
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    void containsValueFalseDoesNotCountAsAccess() {
        trackingMap.put("x", "xray");
        trackingMap.put("y", "yellow");
        // containsValue should return false
        assertFalse(trackingMap.containsValue("zulu"));
        // No key access happened, so expungeUnused should clear the map
        trackingMap.expungeUnused();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    void containsValueWithNullValueDoesNotCountAsAccess() {
        trackingMap.put("nullKey", null);
        trackingMap.put("nonNullKey", "value");
        // containsValue should correctly identify the null value
        assertTrue(trackingMap.containsValue(null));
        // Still no key access, so expungeUnused should clear everything
        trackingMap.expungeUnused();
        assertEquals(0, trackingMap.size());
        assertTrue(trackingMap.isEmpty());
    }

    @Test
    void containsValueDoesNotAffectKeysUsed() {
        trackingMap.put("k1", "v1");
        trackingMap.put("k2", "v2");
        // Call containsValue – it must not register any key usage
        trackingMap.containsValue("v1");
        // Verify that keysUsed remains empty (no key was accessed)
        assertTrue(trackingMap.keysUsed().isEmpty(), "keysUsed should be empty because containsValue must not count as a key access");
    }

    @Test
    void containsValueAfterKeyAccessKeepsAccessedEntry() {
        trackingMap.put("alpha", "A");
        trackingMap.put("beta", "B");
        // Access one key via get – this should be recorded
        assertEquals("A", trackingMap.get("alpha"));
        // Now call containsValue – it should not affect the recorded access
        assertTrue(trackingMap.containsValue("B"));
        // Expunge unused entries – only the accessed key should remain
        trackingMap.expungeUnused();
        assertEquals(1, trackingMap.size());
        assertTrue(trackingMap.containsKey("alpha"));
        assertFalse(trackingMap.containsKey("beta"));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideContainsValueNormalCases")
    void parameterisedContainsValueTests(String description, List<Map.Entry<String, Object>> entries, Object queryValue, boolean expectedResult) throws Exception {
        // Populate the map with the supplied entries
        for (Map.Entry<String, Object> e : entries) {
            trackingMap.put(e.getKey(), e.getValue());
        }
        // Special handling for replacement/removal/mutation cases encoded via description
        if (description.contains("replacing")) {
            // Replace the existing key's value before querying
            trackingMap.put("k", "newValue");
        } else if (description.contains("removing")) {
            trackingMap.remove("k");
        } else if (description.contains("mutable object")) {
            // Mutate the holder after insertion
            MutableHolder holder = (MutableHolder) trackingMap.get("holder");
            holder.content = "changed";
            // Query using the original instance (still stored in the map)
            queryValue = holder;
        }
        // Perform the actual containsValue call and assert
        assertEquals(expectedResult, trackingMap.containsValue(queryValue), description);
    }

    // ---------- Exception scenario tests ----------
    @Test
    void containsValueThrowsWhenInternalMapIsNull() throws Exception {
        // Create a TrackingMap with a null backing map via reflection
        TrackingMap<String, Object> mapWithNull = new TrackingMap<>(null);
        // Force internalMap to null using reflection if constructor does not already set it
        Field internalMapField = TrackingMap.class.getDeclaredField("internalMap");
        internalMapField.setAccessible(true);
        internalMapField.set(mapWithNull, null);
        assertThrows(NullPointerException.class, () -> mapWithNull.containsValue(1), "Calling containsValue on a TrackingMap with a null internal map should throw NPE");
    }

    @Test
    void containsValuePropagatesExceptionFromValueEquals() {
        // Insert a value whose equals throws IllegalStateException
        EvilEquals evil = new EvilEquals();
        trackingMap.put("evil", evil);
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> trackingMap.containsValue("any"), "containsValue should propagate IllegalStateException thrown by value.equals()");
        assertEquals("Deliberate exception from equals()", ex.getMessage());
    }
}
