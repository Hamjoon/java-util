package com.cedarsoftware.util;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class MapUtilities_getOrThrow_Test {

    static Stream<Arguments> nullChecksProvider() {
        return Stream.of(Arguments.of("Map argument is null", null, "anyKey", new IllegalArgumentException("missing"), NullPointerException.class, "Map parameter cannot be null"), Arguments.of("Throwable argument is null", Collections.singletonMap("k", "v"), "k", null, NullPointerException.class, "Throwable object cannot be null"));
    }

    static Stream<Arguments> successfulRetrievalProvider() {
        ???;
    }

    static Stream<Arguments> keyNotFoundProvider() {
        return Stream.of(Arguments.of("Empty map – missing key", Collections.emptyMap(), "any", new IllegalArgumentException("key missing"), IllegalArgumentException.class, "key missing"), Arguments.of("TreeMap (SortedMap) – missing key", new TreeMap<Integer, String>() {

            {
                put(1, "one");
            }
        }, 2, new IndexOutOfBoundsException("no such key"), IndexOutOfBoundsException.class, "no such key"), Arguments.of("HashMap with other entries – missing key", new HashMap<String, String>() {

            {
                put("present", "value");
            }
        }, "absent", new UnsupportedOperationException("unsupported"), UnsupportedOperationException.class, "unsupported"));
    }

    static Stream<Arguments> edgeCasesProvider() {
        return Stream.of(Arguments.of("Heterogeneous keys – exact match with Integer key", new HashMap<Object, Object>() {

            {
                put(123, "intKey");
                put("123", "stringKey");
            }
        }, 123, new IllegalStateException("missing"), "intKey"), Arguments.of("Complex object value (List)", new HashMap<String, List<Integer>>() {

            {
                put("list", Arrays.asList(1, 2, 3));
            }
        }, "list", new IllegalArgumentException("absent"), Arrays.asList(1, 2, 3)), Arguments.of("Key present with null value", new HashMap<String, String>() {

            {
                put("nullValue", null);
            }
        }, "nullValue", new IllegalStateException("missing"), null), Arguments.of("Null key absent – throws supplied throwable", new HashMap<String, String>() {

            {
                put("k", "v");
            }
        }, null, new IllegalArgumentException("null key not allowed"), new IllegalArgumentException("null key not allowed")));
    }

    @Test
    void testGetOrThrowReturnsExistingValue() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("key", Boolean.TRUE);
        Object result = MapUtilities.getOrThrow(map, "key", new RuntimeException("should not be thrown"));
        assertTrue(result instanceof Boolean);
        assertTrue((Boolean) result);
    }

    @Test
    void testGetOrThrowReturnsNullWhenValueIsNull() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("nullKey", null);
        Object result = MapUtilities.getOrThrow(map, "nullKey", new RuntimeException("should not be thrown"));
        assertNull(result);
    }

    @Test
    void testGetOrThrowThrowsProvidedThrowableWhenKeyMissing() {
        Map<String, Object> map = new HashMap<>();
        map.put("present", "value");
        RuntimeException thrown = new RuntimeException("missing key");
        RuntimeException caught = assertThrows(RuntimeException.class, () -> MapUtilities.getOrThrow(map, "absent", thrown));
        assertEquals("missing key", caught.getMessage());
        // Ensure the same instance is propagated (not a new wrapper)
        assertSame(thrown, caught);
    }

    @Test
    void testGetOrThrowNullMapThrowsNPE() {
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(null, "any", new RuntimeException("ignored")));
        assertEquals("Map parameter cannot be null", npe.getMessage());
    }

    @Test
    void testGetOrThrowNullThrowableThrowsNPE() {
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        NullPointerException npe = assertThrows(NullPointerException.class, () -> MapUtilities.getOrThrow(map, "key", null));
        assertEquals("Throwable object cannot be null", npe.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("nullChecksProvider")
    void testNullChecks(String description, Map<?, ?> map, Object key, Throwable throwable, Class<? extends Throwable> expectedExceptionClass, String expectedMessage) {
        Throwable thrown = assertThrows(expectedExceptionClass, () -> MapUtilities.getOrThrow(map, key, throwable));
        assertEquals(expectedMessage, thrown.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("successfulRetrievalProvider")
    void testSuccessfulRetrieval(String description, Map<?, ?> map, Object key, Throwable throwable, Object expectedValue) throws Throwable {
        Object result = MapUtilities.getOrThrow(map, key, throwable);
        if (expectedValue instanceof List) {
            assertEquals(expectedValue, result);
        } else {
            assertEquals(expectedValue, result);
        }
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("keyNotFoundProvider")
    void testKeyNotFoundThrows(String description, Map<?, ?> map, Object key, Throwable throwable, Class<? extends Throwable> expectedExceptionClass, String expectedMessage) {
        Throwable thrown = assertThrows(expectedExceptionClass, () -> MapUtilities.getOrThrow(map, key, throwable));
        assertSame(throwable, thrown);
        assertEquals(expectedMessage, thrown.getMessage());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("edgeCasesProvider")
    void testEdgeCases(String description, Map<?, ?> map, Object key, Throwable throwable, Object expectedOutcome) throws Throwable {
        if (expectedOutcome instanceof Throwable) {
            Throwable expectedThrowable = (Throwable) expectedOutcome;
            Throwable caught = assertThrows(expectedThrowable.getClass(), () -> MapUtilities.getOrThrow(map, key, throwable));
            assertSame(throwable, caught);
            assertEquals(expectedThrowable.getMessage(), caught.getMessage());
        } else {
            Object result = MapUtilities.getOrThrow(map, key, throwable);
            if (expectedOutcome instanceof List) {
                assertEquals(expectedOutcome, result);
            } else {
                assertEquals(expectedOutcome, result);
            }
        }
    }
}
