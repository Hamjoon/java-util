package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.function.BiFunction;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Function;

@ExtendWith(MockitoExtension.class)
class CaseInsensitiveMap_computeIfPresent_Test {

    private CaseInsensitiveMap<String, String> stringMap;

    private CaseInsensitiveMap<Integer, String> intMap;

    /**
     * Simple POJO used as a non‑String key.
     */
    static final class Person {

        private final String name;

        Person(String name) {
            this.name = Objects.requireNonNull(name);
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof Person) && name.equals(((Person) o).name);
        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }

        @Override
        public String toString() {
            return "Person{" + name + '}';
        }
    }

    /**
     * Minimal wrapper that mimics the library's {@code CaseInsensitiveString}.
     * It stores the original case and provides {@code getOriginal()} for the
     * remapping function verification.
     */
    static final class CaseInsensitiveString {

        private final String original;

        CaseInsensitiveString(String original) {
            this.original = Objects.requireNonNull(original);
        }

        public String getOriginal() {
            return original;
        }

        @Override
        public String toString() {
            return original;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CaseInsensitiveString) && original.equalsIgnoreCase(((CaseInsensitiveString) o).original);
        }

        @Override
        public int hashCode() {
            return original.toLowerCase(Locale.ROOT).hashCode();
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Ensure the cache length is at its default for all tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @BeforeEach
    void setupBeforeEach() {
        stringMap = new CaseInsensitiveMap<>();
        intMap = new CaseInsensitiveMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset any static configuration that could affect other tests
        CaseInsensitiveMap.setMaxCacheLengthString(100);
    }

    @Test
    void computeIfPresent_StringKey_IsCaseInsensitiveAndPassesOriginalKey() {
        // given
        stringMap.put("Key", "Value");
        @SuppressWarnings("unchecked")
        BiFunction<String, String, String> remap = mock(BiFunction.class);
        when(remap.apply(anyString(), anyString())).thenAnswer(invocation -> {
            String passedKey = invocation.getArgument(0);
            String passedValue = invocation.getArgument(1);
            // The original key (with the case used in the call) must be received,
            // not the normalized internal representation.
            assertEquals("kEy", passedKey);
            assertEquals("Value", passedValue);
            return "NewValue";
        });
        // when
        String result = stringMap.computeIfPresent("kEy", remap);
        // then
        assertEquals("NewValue", result);
        // The underlying entry must now hold the new value; any case works for retrieval
        assertEquals("NewValue", stringMap.get("KEY"));
        verify(remap, times(1)).apply(anyString(), anyString());
    }

    @Test
    void computeIfPresent_KeyAbsent_DoesNotInvokeRemappingFunction() {
        // given
        @SuppressWarnings("unchecked")
        BiFunction<String, String, String> remap = mock(BiFunction.class);
        // when
        String result = stringMap.computeIfPresent("missing", remap);
        // then
        assertNull(result);
        verify(remap, never()).apply(any(), any());
    }

    @Test
    void computeIfPresent_NonStringKey_BehavesLikeRegularMap() {
        // given
        intMap.put(1, "One");
        @SuppressWarnings("unchecked")
        BiFunction<Integer, String, String> remap = mock(BiFunction.class);
        when(remap.apply(eq(1), eq("One"))).thenReturn("Uno");
        // when
        String result = intMap.computeIfPresent(1, remap);
        // then
        assertEquals("Uno", result);
        assertEquals("Uno", intMap.get(1));
        verify(remap, times(1)).apply(eq(1), eq("One"));
    }

    // ---------- 1. key is present (case‑insensitive lookup) ----------
    @Test
    void computeIfPresent_KeyPresent_CaseInsensitiveLookup_AppendsSuffix() {
        // Map contains "test" → "value"
        stringMap.put("test", "value");
        // remapping function appends "_updated"
        BiFunction<String, String, String> remap = (k, v) -> v + "_updated";
        String result = stringMap.computeIfPresent("TeSt", remap);
        assertEquals("value_updated", result);
        assertEquals("value_updated", stringMap.get("TEST"));
    }

    @Test
    void computeIfPresent_KeyPresent_RemappingReturnsNull_RemovesEntry() {
        stringMap.put("test", "value");
        BiFunction<String, String, String> remap = (k, v) -> null;
        String result = stringMap.computeIfPresent("test", remap);
        assertNull(result);
        assertFalse(stringMap.containsKey("test"));
    }

    @Test
    void computeIfPresent_KeyPresent_RemappingThrowsException_Propagates() {
        stringMap.put("test", "value");
        BiFunction<String, String, String> remap = (k, v) -> {
            throw new IllegalStateException("boom");
        };
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> stringMap.computeIfPresent("TEST", remap));
        assertEquals("boom", ex.getMessage());
        // original entry must stay unchanged
        assertEquals("value", stringMap.get("test"));
    }

    @Test
    void computeIfPresent_KeyAbsent_RemappingFunctionNeverCalled() {
        @SuppressWarnings("unchecked")
        BiFunction<String, String, String> remap = mock(BiFunction.class);
        // The lambda would fail the test if executed
        when(remap.apply(any(), any())).thenAnswer(invocation -> {
            fail("remappingFunction should not be executed");
            return null;
        });
        String result = stringMap.computeIfPresent("MiXeD", remap);
        assertNull(result);
        verify(remap, never()).apply(any(), any());
    }

    // ---------- 3. non‑String key handling ----------
    @Test
    void computeIfPresent_NonStringKey_IncrementsValue() {
        CaseInsensitiveMap<Integer, Integer> intIntMap = new CaseInsensitiveMap<>();
        intIntMap.put(42, 42);
        BiFunction<Integer, Integer, Integer> remap = (k, v) -> v + 1;
        Integer result = intIntMap.computeIfPresent(42, remap);
        assertEquals(43, result);
        assertEquals(43, intIntMap.get(42));
    }

    // ---------- 4. null key handling ----------
    @Test
    void computeIfPresent_NullKey_ThrowsNullPointerException() {
        @SuppressWarnings("unchecked")
        BiFunction<String, String, String> remap = mock(BiFunction.class);
        assertThrows(NullPointerException.class, () -> stringMap.computeIfPresent(null, remap));
    }

    // ---------- 5. custom object key ----------
    @Test
    void computeIfPresent_CustomObjectKey_AppendsSuffix() {
        CaseInsensitiveMap<Person, String> personMap = new CaseInsensitiveMap<>();
        Person alice = new Person("Alice");
        personMap.put(alice, "value");
        BiFunction<Person, String, String> remap = (k, v) -> v + "_updated";
        String result = personMap.computeIfPresent(new Person("Alice"), remap);
        assertEquals("value_updated", result);
        assertEquals("value_updated", personMap.get(alice));
    }

    @Test
    void computeIfPresent_CustomObjectKey_RemappingReturnsNull_RemovesEntry() {
        CaseInsensitiveMap<Person, String> personMap = new CaseInsensitiveMap<>();
        Person bob = new Person("Bob");
        personMap.put(bob, "value");
        BiFunction<Person, String, String> remap = (k, v) -> null;
        String result = personMap.computeIfPresent(new Person("Bob"), remap);
        assertNull(result);
        assertFalse(personMap.containsKey(bob));
    }

    // ---------- 6. CaseInsensitiveString wrapper ----------
    @Test
    void computeIfPresent_CaseInsensitiveStringWrapper_PassesOriginalWrapper() {
        CaseInsensitiveMap<CaseInsensitiveString, String> map = new CaseInsensitiveMap<>();
        CaseInsensitiveString key = new CaseInsensitiveString("TeSt");
        map.put(key, "value");
        BiFunction<CaseInsensitiveString, String, String> remap = (k, v) -> k.getOriginal() + ":" + v;
        String result = map.computeIfPresent(new CaseInsensitiveString("TeSt"), remap);
        assertEquals("TeSt:value", result);
        // Verify that the stored value has been updated
        assertEquals("TeSt:value", map.get(key));
    }

    // ---------- 7. remapping function receives null value ----------
    @Test
    void computeIfPresent_NullValueEntry_RemappingHandlesNull() {
        stringMap.put("nullValueKey", null);
        BiFunction<String, String, String> remap = (k, v) -> v == null ? "wasNull" : v;
        String result = stringMap.computeIfPresent("nullValueKey", remap);
        assertEquals("wasNull", result);
        assertEquals("wasNull", stringMap.get("nullValueKey"));
    }

    // ---------- 8. concurrent modification during remapping ----------
    @Test
    void computeIfPresent_ConcurrentModification_DoesNotAffectCurrentComputation() {
        stringMap.put("concurrent", "init");
        stringMap.put("otherKey", "other");
        BiFunction<String, String, String> remap = (k, v) -> {
            // remove a different key while computing
            stringMap.remove("otherKey");
            return v + "_changed";
        };
        String result = stringMap.computeIfPresent("concurrent", remap);
        assertEquals("init_changed", result);
        assertEquals("init_changed", stringMap.get("concurrent"));
        assertFalse(stringMap.containsKey("otherKey"));
    }

    // ---------- 9. Boolean value ----------
    @Test
    void computeIfPresent_BooleanValue_Negates() {
        CaseInsensitiveMap<String, Boolean> boolMap = new CaseInsensitiveMap<>();
        boolMap.put("boolKey", false);
        BiFunction<String, Boolean, Boolean> remap = (k, v) -> !v;
        Boolean result = boolMap.computeIfPresent("boolKey", remap);
        assertTrue(result);
        assertTrue(boolMap.get("boolKey"));
    }

    // ---------- 10. checked exception wrapped in RuntimeException ----------
    @Test
    void computeIfPresent_RemappingThrowsRuntimeExceptionWithCause_Propagates() {
        stringMap.put("checked", "value");
        BiFunction<String, String, String> remap = (k, v) -> {
            try {
                throw new java.io.IOException("io");
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
        RuntimeException ex = assertThrows(RuntimeException.class, () -> stringMap.computeIfPresent("checked", remap));
        assertTrue(ex.getCause() instanceof java.io.IOException);
        assertEquals("io", ex.getCause().getMessage());
    }

    // ---------- 11. identity remapping (no change) ----------
    @Test
    void computeIfPresent_IdentityRemap_ReturnsSameValue() {
        stringMap.put("identity", "same");
        BiFunction<String, String, String> remap = (k, v) -> v;
        String result = stringMap.computeIfPresent("identity", remap);
        assertEquals("same", result);
        assertEquals("same", stringMap.get("identity"));
    }

    // ---------- 12. null remappingFunction ----------
    @Test
    void computeIfPresent_NullRemappingFunction_ThrowsNullPointerException() {
        stringMap.put("test", "value");
        assertThrows(NullPointerException.class, () -> stringMap.computeIfPresent("test", null));
    }

    // ---------- 13. unmodifiable backing map ----------
    @Test
    void computeIfPresent_UnmodifiableBackingMap_ThrowsUnsupportedOperationException() throws Exception {
        // Create an unmodifiable map with a single entry
        Map<String, String> base = Collections.unmodifiableMap(new HashMap<>(Map.of("readonly", "old")));
        // Inject it via reflection (no public constructor for custom backing map)
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        Field mapField = CaseInsensitiveMap.class.getDeclaredField("map");
        mapField.setAccessible(true);
        mapField.set(map, base);
        BiFunction<String, String, String> remap = (k, v) -> v + "_new";
        assertThrows(UnsupportedOperationException.class, () -> map.computeIfPresent("readonly", remap));
    }

    // ---------- 14. remapping function returns incompatible type ----------
    @Test
    void computeIfPresent_IncompatibleReturnType_ThrowsClassCastException() {
        CaseInsensitiveMap<Integer, Integer> intIntMap = new CaseInsensitiveMap<>();
        intIntMap.put(1, 10);
        BiFunction<Integer, Integer, Object> remap = (k, v) -> "incompatible";
        assertThrows(ClassCastException.class, () -> intIntMap.computeIfPresent(1, (BiFunction<Integer, Integer, Integer>) remap));
    }
}
