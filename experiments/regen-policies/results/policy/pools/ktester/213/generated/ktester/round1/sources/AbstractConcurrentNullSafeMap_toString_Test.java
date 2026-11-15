package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbstractConcurrentNullSafeMap_toString_Test {

    private TestMap map;

    /**
     * Simple concrete subclass used only for testing.
     */
    private static final class TestMap extends AbstractConcurrentNullSafeMap<String, String> {

        TestMap(ConcurrentMap<Object, Object> internal) {
            super(internal);
        }
    }

    /**
     * Helper class whose {@code toString()} throws the supplied exception.
     */
    private static final class BadToString {

        private final RuntimeException toThrow;

        BadToString(RuntimeException toThrow) {
            this.toThrow = toThrow;
        }

        @Override
        public String toString() {
            throw toThrow;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use a fresh ConcurrentHashMap for each test to guarantee isolation.
        ConcurrentMap<Object, Object> internal = new ConcurrentHashMap<>();
        map = new TestMap(internal);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly clear the map to aid garbage collection; not strictly necessary.
        map.clear();
    }

    /**
     * Supplies the arguments for {@link #toString_NormalCases}.
     */
    private static Stream<Arguments> normalCases() {
        return Stream.of(// 1. Empty map
        Arguments.of("Empty map", (Consumer<TestMap>) m -> {
        }, /* no entries */
        Set.of("{}"), true), // 2. Single non‑null entry
        Arguments.of("Single non‑null entry", (Consumer<TestMap>) m -> m.put("one", "1"), Set.of("{one=1}"), true), // 3. Multiple non‑null entries (order not guaranteed)
        Arguments.of("Multiple non‑null entries", (Consumer<TestMap>) m -> {
            m.put("one", "1");
            m.put("two", "2");
            m.put("three", "3");
        }, Set.of("one=1", "two=2", "three=3"), false), // 4. Null key
        Arguments.of("Null key with non‑null value", (Consumer<TestMap>) m -> m.put(null, "42"), Set.of("null=42"), false), // 5. Null value
        Arguments.of("Non‑null key with null value", (Consumer<TestMap>) m -> m.put("answer", null), Set.of("answer=null"), false), // 6. Both key and value null
        Arguments.of("Both key and value are null", (Consumer<TestMap>) m -> m.put(null, null), Set.of("null=null"), false), // 7. Special characters in value (newline, tab, carriage‑return)
        Arguments.of("Key/value with control characters", (Consumer<TestMap>) m -> m.put("keyWithSpecialChars", "val\n\t\r"), Set.of("{keyWithSpecialChars=val\n\t\r}"), true));
    }

    @Test
    void toString_EmptyMap_ReturnsEmptyBraces() {
        assertEquals("{}", map.toString(), "An empty map should be represented by {}");
    }

    @Test
    void toString_NonNullEntries_ReturnsStandardRepresentation() {
        map.put("key1", "value1");
        map.put("key2", "value2");
        String result = map.toString();
        // The order of entries in a ConcurrentHashMap is not guaranteed,
        // so we verify that both expected key/value pairs appear.
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in braces");
        assertTrue(result.contains("key1=value1"), "Result must contain first entry");
        assertTrue(result.contains("key2=value2"), "Result must contain second entry");
    }

    @Test
    void toString_NullKeyAndNullValue_AreRenderedAsLiteralNull() {
        map.put(null, null);
        map.put("a", "b");
        String result = map.toString();
        // Expect the null key/value to appear as the literal string "null".
        assertTrue(result.contains("null=null"), "Null key/value should be rendered as \"null=null\"");
        assertTrue(result.contains("a=b"), "Other entries should be rendered normally");
    }

    @Test
    void toString_MixedNullAndNonNullEntries_AllRenderedCorrectly() {
        map.put(null, "valueForNullKey");
        map.put("keyForNullValue", null);
        map.put("regularKey", "regularValue");
        String result = map.toString();
        assertTrue(result.contains("null=valueForNullKey"), "Null key should be rendered with its associated value");
        assertTrue(result.contains("keyForNullValue=null"), "Null value should be rendered as literal null");
        assertTrue(result.contains("regularKey=regularValue"), "Regular entry should be rendered normally");
    }

    /**
     * Parameterized test that covers all normal scenarios described in the JSON
     * test specification.  For most cases we only need to verify that the
     * resulting string is wrapped in braces and contains the expected
     * {@code key=value} fragments.  The special‑character case requires an
     * exact match because the representation includes control characters.
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("normalCases")
    void toString_NormalCases(String description, Consumer<TestMap> mapSetup, Set<String> expectedFragments, boolean exactMatch) {
        // Arrange
        mapSetup.accept(map);
        // Act
        String result = map.toString();
        // Assert
        if (exactMatch) {
            // For the special‑character case we compare the whole string.
            assertEquals(expectedFragments.iterator().next(), result, "Exact string representation does not match for: " + description);
        } else {
            // General checks: braces and presence of each fragment.
            assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in braces for: " + description);
            for (String fragment : expectedFragments) {
                assertTrue(result.contains(fragment), "Result must contain \"" + fragment + "\" for: " + description);
            }
        }
    }

    @Test
    void toString_KeyToStringThrows_IllegalStateExceptionPropagates() {
        // Arrange: a key whose toString throws IllegalStateException
        BadToString badKey = new BadToString(new IllegalStateException("bad key"));
        // ensure map is not empty
        map.put((String) null, "value");
        // Directly insert the bad key into the internal map via reflection
        // because the public API prevents putting a non‑String key.
        try {
            var field = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentMap<Object, Object> internal = (ConcurrentMap<Object, Object>) field.get(map);
            internal.put(badKey, "value");
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        // Act & Assert
        assertThrows(IllegalStateException.class, map::toString, "IllegalStateException from key.toString() must propagate");
    }

    @Test
    void toString_ValueToStringThrows_IllegalArgumentExceptionPropagates() {
        // Arrange: a value whose toString throws IllegalArgumentException
        BadToString badValue = new BadToString(new IllegalArgumentException("bad value"));
        // ensure map is not empty
        map.put("key", (String) null);
        // Insert the bad value via reflection.
        try {
            var field = AbstractConcurrentNullSafeMap.class.getDeclaredField("internalMap");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            ConcurrentMap<Object, Object> internal = (ConcurrentMap<Object, Object>) field.get(map);
            internal.put("key", badValue);
        } catch (Exception e) {
            fail("Reflection setup failed: " + e);
        }
        // Act & Assert
        assertThrows(IllegalArgumentException.class, map::toString, "IllegalArgumentException from value.toString() must propagate");
    }

    @Test
    void toString_IteratorThrows_RuntimeExceptionPropagates() {
        // Arrange: mock a ConcurrentMap whose entrySet iterator throws.
        @SuppressWarnings("unchecked")
        ConcurrentMap<Object, Object> faultyMap = mock(ConcurrentMap.class);
        Set<Map.Entry<Object, Object>> faultySet = mock(Set.class);
        Iterator<Map.Entry<Object, Object>> faultyIter = mock(Iterator.class);
        when(faultyMap.entrySet()).thenReturn(faultySet);
        when(faultySet.iterator()).thenReturn(faultyIter);
        when(faultyIter.hasNext()).thenThrow(new RuntimeException("iterator failure"));
        // Use the mocked map in a new TestMap instance.
        TestMap faultyTestMap = new TestMap(faultyMap);
        // Act & Assert
        RuntimeException ex = assertThrows(RuntimeException.class, faultyTestMap::toString, "RuntimeException from iterator must propagate");
        assertEquals("iterator failure", ex.getMessage());
    }
}
