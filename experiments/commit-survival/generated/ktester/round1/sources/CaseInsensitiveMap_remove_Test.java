package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

class CaseInsensitiveMap_remove_Test {

    private CaseInsensitiveMap<Object, String> map;

    /**
     * Simple key class with deliberately broken {@code equals}/{@code hashCode}.
     */
    static final class BadKey {

        final int id;

        BadKey(int id) {
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            return false;
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public String toString() {
            return "BadKey(" + id + ')';
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use the default constructor (LinkedHashMap backing) and populate with a mix of keys.
        map = new CaseInsensitiveMap<>();
        map.put("Alpha", "first");
        map.put("Beta", "second");
        // non‑String key
        map.put(42, "answer");
        // Unicode string
        map.put("γ", "gamma");
        // Default map (LinkedHashMap) – populated with a generic set of entries.
        map = new CaseInsensitiveMap<>();
        map.put("Alpha", "first");
        map.put("Beta", "second");
        // non‑String key
        map.put(42, "answer");
        // Unicode string
        map.put("γ", "gamma");
        map.put("", "emptyValue");
        map.put("   ", "spaceValue");
        // 😀 emoji
        map.put("\uD83D\uDE00", "emojiValue");
        // German sharp‑s example
        map.put("straße", "value1");
        map.put(new StringBuilder("hello"), "mutableValue");
        map.put(new BadKey(1), "bad");
        map.put(true, "boolTrue");
        // duplicate key for CharSequence test
        map.put(new StringBuilder("hello"), "world");
        // Additional entries required for the non‑String tests above.
        map.put(123, "numeric");
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear the map to ensure no cross‑test contamination.
        map.clear();
        map.clear();
    }

    /**
     * Replaces the internal {@code map} field with the supplied instance.
     */
    private static void setBackingMap(CaseInsensitiveMap<?, ?> target, Map<?, ?> backing) {
        try {
            Field f = CaseInsensitiveMap.class.getDeclaredField("map");
            f.setAccessible(true);
            f.set(target, backing);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper to create a fresh map with a specific backing implementation.
     */
    private static CaseInsensitiveMap<Object, String> createMapWithBacking(Map<Object, String> backing) {
        CaseInsensitiveMap<Object, String> ciMap = new CaseInsensitiveMap<>();
        setBackingMap(ciMap, backing);
        return ciMap;
    }

    static Stream<Arguments> caseInsensitiveStringProvider() {
        // First removal succeeds, second returns null.
        return Stream.of(Arguments.of("HeLLo", "world"), Arguments.of("HELLO", null));
    }

    static Stream<Arguments> nonStringKeyProvider() {
        return Stream.of(// will be added in test setup
        Arguments.of(123, "numeric"), // Boolean false not present
        Arguments.of(false, null));
    }

    @Test
    void remove_StringKey_IgnoresCaseAndReturnsValue() {
        // Act: remove using a different case representation.
        // lower‑case version of "Alpha"
        String removed = map.remove("alpha");
        // Assert: the value is returned and the entry is gone.
        assertEquals("first", removed, "remove should return the previous value");
        assertFalse(map.containsKey("Alpha"), "original key should no longer be present");
        assertFalse(map.containsKey("alpha"), "case‑insensitive view should also be absent");
        assertEquals(3, map.size(), "size should be decremented");
    }

    @Test
    void remove_StringKey_IgnoresCaseWhenKeyHasUnicodeCharacters() {
        // Act: remove using a different case (Unicode characters have no case, but we still test the path).
        // Greek capital Gamma, different Unicode code point
        String removed = map.remove("Γ");
        // Since the map treats the key as a String, it will wrap it in CaseInsensitiveString.
        // The internal comparison is case‑insensitive; for characters without case, equality holds.
        assertEquals("gamma", removed, "remove should return the value for the Unicode key");
        assertFalse(map.containsKey("γ"), "Unicode key should be removed");
        assertEquals(3, map.size());
    }

    @Test
    void remove_NonStringKey_DelegatesToUnderlyingMap() {
        // Act: remove the integer key.
        String removed = map.remove(42);
        // Assert: value is returned and key is gone.
        assertEquals("answer", removed, "remove should return the previous value for non‑String key");
        assertFalse(map.containsKey(42), "integer key should be removed");
        assertEquals(3, map.size());
    }

    @Test
    void remove_NonExistingKey_ReturnsNull() {
        // Act: attempt to remove a key that is not present.
        String removed = map.remove("nonexistent");
        // Assert: null is returned and map is unchanged.
        assertNull(removed, "remove should return null for absent key");
        assertEquals(4, map.size(), "size should remain unchanged");
    }

    @Test
    void remove_NullKey_DelegatesToUnderlyingMapBehavior() {
        // The underlying LinkedHashMap permits null keys, so we test that path.
        map.put(null, "nullValue");
        assertEquals(5, map.size());
        // Act: remove the null key.
        String removed = map.remove(null);
        // Assert: value is returned and key is gone.
        assertEquals("nullValue", removed);
        assertFalse(map.containsKey(null));
        assertEquals(4, map.size());
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] remove String key {0} → expected {1}")
    @MethodSource("caseInsensitiveStringProvider")
    void remove_StringKey_CaseInsensitive(String key, String expected) {
        String removed = map.remove(key);
        if (expected == null) {
            assertNull(removed, "Expected null when key is absent");
        } else {
            assertEquals(expected, removed, "Returned value should match expected");
        }
    }

    // -----------------------------------------------------------------
    @ParameterizedTest(name = "[{index}] remove non‑String key {0} → expected {1}")
    @MethodSource("nonStringKeyProvider")
    void remove_NonStringKey_Delegated(Object key, String expected) {
        String removed = map.remove(key);
        if (expected == null) {
            assertNull(removed);
        } else {
            assertEquals(expected, removed);
        }
    }

    // -----------------------------------------------------------------
    @Test
    void remove_NullKey_AllowedMap() {
        map.put(null, "nullValue");
        // sanity check
        assertEquals(13, map.size());
        String removed = map.remove(null);
        assertEquals("nullValue", removed);
        assertFalse(map.containsKey(null));
        assertEquals(12, map.size());
        // second removal → null
        assertNull(map.remove(null));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_StringKey_NotPresent() {
        assertNull(map.remove("missing"));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_StringKey_GermanSharpS() {
        // First removal with capital ß (ß has no uppercase, but "STRASSE" is a common variant)
        String removed = map.remove("Straße");
        assertEquals("value1", removed);
        assertFalse(map.containsKey("straße"));
        // Second attempt with another variant should yield null
        assertNull(map.remove("STRASSE"));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_CharSequence_NotString() {
        StringBuilder sbKey = new StringBuilder("hello");
        // The map already contains this exact object (added in @BeforeEach)
        String removed = map.remove(sbKey);
        assertEquals("world", removed);
        assertFalse(map.containsKey(sbKey));
        // Second removal → null
        assertNull(map.remove(sbKey));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_MutableKey_AfterMutation() {
        StringBuilder mutableKey = new StringBuilder("hello");
        map.put(mutableKey, "mutableValue");
        // Mutate the content – the map uses object identity, not the content.
        mutableKey.append("X");
        String removed = map.remove(mutableKey);
        assertEquals("mutableValue", removed);
        assertFalse(map.containsKey(mutableKey));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_BadKey_InconsistentEqualsHashCode() {
        BadKey bad = new BadKey(1);
        // The map already contains this exact instance (added in @BeforeEach)
        String removed = map.remove(bad);
        assertEquals("bad", removed);
        assertFalse(map.containsKey(bad));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_EmptyStringKey_Twice() {
        assertEquals("emptyValue", map.remove(""));
        assertNull(map.remove(""));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_WhitespaceStringKey() {
        assertEquals("spaceValue", map.remove("   "));
        assertNull(map.remove("   "));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_UnicodeSurrogatePairKey() {
        assertEquals("emojiValue", map.remove("\uD83D\uDE00"));
        assertNull(map.remove("\uD83D\uDE00"));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_BooleanKeys() {
        assertEquals("boolTrue", map.remove(true));
        assertNull(map.remove(false));
    }

    // -----------------------------------------------------------------
    @Test
    void remove_NullKey_OnConcurrentHashMap_ThrowsNPE() {
        // Build a CaseInsensitiveMap that wraps a ConcurrentHashMap (no null keys allowed)
        CaseInsensitiveMap<Object, String> concurrentMap = createMapWithBacking(new ConcurrentHashMap<>());
        // normal entry
        concurrentMap.put("someKey", "someValue");
        assertThrows(NullPointerException.class, () -> concurrentMap.remove(null), "ConcurrentHashMap.remove(null) should throw NPE");
    }

    // -----------------------------------------------------------------
    @Test
    @Disabled("String is final; cannot create a subclass. Test omitted.")
    void remove_StringSubclass_NotApplicable() {
        // Placeholder to document the scenario.
    }
}
