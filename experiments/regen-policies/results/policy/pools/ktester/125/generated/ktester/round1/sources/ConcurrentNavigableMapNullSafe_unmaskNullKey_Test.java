package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class ConcurrentNavigableMapNullSafe_unmaskNullKey_Test {

    private TestableConcurrentNavigableMapNullSafe<String, Integer> map;

    /**
     * A concrete subclass that makes the protected methods visible for testing.
     */
    private static class TestableConcurrentNavigableMapNullSafe<K, V> extends ConcurrentNavigableMapNullSafe<K, V> {

        TestableConcurrentNavigableMapNullSafe() {
            super();
        }

        /**
         * Expose {@code maskNullKey} for the tests.
         */
        public Object mask(K key) {
            return super.maskNullKey(key);
        }

        /**
         * Expose {@code unmaskNullKey} for the tests.
         */
        @SuppressWarnings("unchecked")
        public K unmask(Object maskedKey) {
            return super.unmaskNullKey(maskedKey);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        map = new TestableConcurrentNavigableMapNullSafe<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Clear reference for GC; not strictly necessary but keeps the pattern consistent.
        map = null;
    }

    /**
     * Supplies the arguments for {@link #unmaskNullKey_normalBehaviour(Object, Object, String)}.
     */
    private static Stream<Arguments> provideNormalUnmaskCases() throws Exception {
        // Helper map used only to obtain the real sentinel value.
        TestableConcurrentNavigableMapNullSafe<String, Integer> helperMap = new TestableConcurrentNavigableMapNullSafe<>();
        // the true sentinel
        Object sentinel = helperMap.mask(null);
        // Dynamically generated sentinel‑like string (different from the real sentinel)
        String dynamicSentinel = "null_" + UUID.randomUUID().toString();
        // An arbitrary object instance – we keep a reference so the expected value
        // can be compared by identity.
        Object arbitraryObject = new Object();
        return Stream.of(arguments(sentinel, null, "Sentinel string should be unmasked to null"), arguments("someStringKey", "someStringKey", "Normal string key should be returned unchanged"), arguments(123, 123, "Non‑String key (Integer) should be returned unchanged"), arguments(null, null, "Raw null argument should result in null"), arguments(dynamicSentinel, dynamicSentinel, "Different sentinel‑like string should be returned unchanged"), arguments("null_different", "null_different", "String that starts with the sentinel prefix but is not equal should be unchanged"), arguments(arbitraryObject, arbitraryObject, "Object instance that is not the sentinel should be returned unchanged"));
    }

    /**
     * Verify that a non‑sentinel (i.e., a normal) masked key is returned unchanged.
     */
    @Test
    void unmaskNullKey_returnsSameObjectWhenNotSentinel() {
        String original = "regularKey";
        // Masking a non‑null key should return the key itself (no sentinel involved).
        Object masked = map.mask(original);
        // The masked value should be equal to the original key.
        assertEquals(original, masked, "Masking a non‑null key should return the key itself");
        // Unmasking should therefore return the original key unchanged.
        String unmasked = map.unmask(masked);
        assertEquals(original, unmasked, "Unmasking a non‑sentinel key should return the original key");
    }

    /**
     * Verify that a {@code null} key is masked to a unique sentinel string and
     * that {@code unmaskNullKey} correctly translates that sentinel back to {@code null}.
     */
    @Test
    void unmaskNullKey_translatesSentinelBackToNull() {
        // Mask a null key – the implementation should replace it with a unique sentinel.
        Object sentinel = map.mask(null);
        assertNotNull(sentinel, "Sentinel for null key must not be null");
        assertTrue(sentinel instanceof String, "Sentinel should be a String");
        // The sentinel must be distinct from any ordinary key.
        assertNotEquals("null", sentinel, "Sentinel should not be the literal string \"null\"");
        // Unmasking the sentinel must yield null.
        String result = map.unmask(sentinel);
        assertNull(result, "Unmasking the sentinel should produce null");
    }

    /**
     * Ensure that {@code unmaskNullKey} returns {@code null} when the supplied
     * object equals the sentinel, even if the reference is a different {@code String}
     * instance with the same value (the sentinel is unique per class load, but we
     * still verify equality semantics).
     */
    @Test
    void unmaskNullKey_returnsNullWhenEqualToSentinel() {
        Object sentinel = map.mask(null);
        // Create a new String with the same content to test equality handling.
        String sameContent = new String(sentinel.toString());
        // Equality should hold (the sentinel is a String, so equals works).
        assertEquals(sentinel, sameContent, "Sentinel strings with same content must be equal");
        // Unmask using the equal string should still return null.
        assertNull(map.unmask(sameContent), "Unmasking an equal sentinel string must return null");
    }

    /**
     * Confirm that calling {@code unmaskNullKey} with an object that is not the
     * sentinel (including {@code null}) does not throw and simply returns the
     * object cast to the generic type.
     */
    @Test
    void unmaskNullKey_handlesNonSentinelIncludingNullArgument() {
        // Passing a raw {@code null} (not the sentinel) should return {@code null}
        // because the method checks sentinel equality first.
        assertNull(map.unmask(null), "Unmasking a raw null should return null");
        // Passing an unrelated object (e.g., an Integer) should be cast and returned.
        Integer unrelated = 42;
        // The map is typed with String keys, but the method is generic; we suppress warnings.
        @SuppressWarnings("unchecked")
        String result = map.unmask(unrelated);
        assertEquals((String) (Object) unrelated, result, "Unmasking a non‑sentinel, non‑null object should return the object cast to K");
    }

    /**
     * Parameterised test covering the various normal (non‑exception) scenarios
     * described in the JSON test specification.
     */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideNormalUnmaskCases")
    void unmaskNullKey_normalBehaviour(Object maskedKey, Object expected, String description) {
        Object actual = map.unmask(maskedKey);
        assertEquals(expected, actual, description);
    }

    /**
     * When the map is declared with {@code K=Integer}, providing a {@code String}
     * as the masked key causes a {@code ClassCastException} when the result is
     * assigned to an {@code Integer} variable.
     */
    @Test
    void unmaskNullKey_throwsClassCastException_whenKeyTypeMismatch_IntegerMap() {
        TestableConcurrentNavigableMapNullSafe<Integer, String> intMap = new TestableConcurrentNavigableMapNullSafe<>();
        assertThrows(ClassCastException.class, () -> {
            // The cast to Integer happens at the assignment site.
            Integer result = intMap.unmask("someString");
        });
    }

    /**
     * When the map is declared with {@code K=String}, providing a non‑String
     * object (e.g., {@code new Object()}) causes a {@code ClassCastException}
     * on assignment to a {@code String} variable.
     */
    @Test
    void unmaskNullKey_throwsClassCastException_whenKeyTypeMismatch_StringMap() {
        TestableConcurrentNavigableMapNullSafe<String, Integer> strMap = new TestableConcurrentNavigableMapNullSafe<>();
        assertThrows(ClassCastException.class, () -> {
            String result = strMap.unmask(new Object());
        });
    }

    /**
     * When the map is declared with {@code K=List<String>}, providing a
     * {@code HashMap} as the masked key results in a {@code ClassCastException}
     * on assignment to a {@code List<String>} variable.
     */
    @Test
    void unmaskNullKey_throwsClassCastException_whenKeyTypeMismatch_ListMap() {
        TestableConcurrentNavigableMapNullSafe<List<String>, Integer> listMap = new TestableConcurrentNavigableMapNullSafe<>();
        assertThrows(ClassCastException.class, () -> {
            List<String> result = listMap.unmask(new HashMap<>());
        });
    }

    /**
     * When the map is declared with {@code K=Number}, providing a {@code Date}
     * (which does not extend {@code Number}) triggers a {@code ClassCastException}
     * on assignment to a {@code Number} variable.
     */
    @Test
    void unmaskNullKey_throwsClassCastException_whenKeyTypeMismatch_NumberMap() {
        TestableConcurrentNavigableMapNullSafe<Number, String> numberMap = new TestableConcurrentNavigableMapNullSafe<>();
        assertThrows(ClassCastException.class, () -> {
            Number result = numberMap.unmask(new Date());
        });
    }
}
