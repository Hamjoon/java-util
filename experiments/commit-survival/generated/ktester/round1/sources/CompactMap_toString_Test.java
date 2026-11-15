package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.AbstractMap;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CompactMap_toString_Test {

    /**
     * Map instance used in the tests.
     */
    private CompactMap<Object, Object> map;

    /* -------------------------------------------------------------
     *  Helper holder class for parameterised test data
     * ------------------------------------------------------------- */
    private static class ArgumentsHolder {

        final String description;

        final Object[][] entries;

        final String expected;

        ArgumentsHolder(String description, Object[][] entries, String expected) {
            this.description = description;
            this.entries = entries;
            this.expected = expected;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh map before each test.
        map = new CompactMap<>();
        map = new CompactMap<>();
    }

    @AfterEach
    void teardownAfterEach() {
        // Ensure the map is cleared so no side‑effects remain.
        if (map != null) {
            map.clear();
        }
        if (map != null) {
            map.clear();
        }
    }

    /* -------------------------------------------------------------
     *  Parameterised tests for simple deterministic scenarios
     * ------------------------------------------------------------- */
    private static Stream<ArgumentsHolder> simpleScenarios() {
        return Stream.of(// Empty map – already covered by the base test, but kept here for completeness
        new ArgumentsHolder("Empty map", new Object[][] {}, "{}"), // Single entry
        new ArgumentsHolder("Single entry", new Object[][] { { "a", 1 } }, "{a=1}"), // Null key and null value
        new ArgumentsHolder("Null key/value", new Object[][] { { null, null } }, "{null=null}"), // Self‑referential key
        new ArgumentsHolder("Self‑referential key", new Object[][] { { "selfKey", "value" } }, null), // Self‑referential value
        new ArgumentsHolder("Self‑referential value", new Object[][] { { "key", "selfValue" } }, null), // Self‑referential both
        new ArgumentsHolder("Self‑referential both", new Object[][] { { "selfBoth", "selfBoth" } }, null), // Mixed key/value types
        new ArgumentsHolder("Mixed types", new Object[][] { { 123, "abc" }, { "xyz", 456 } }, "{123=abc, xyz=456}"));
    }

    @Test
    void toStringReturnsBracesForEmptyMap() {
        assertEquals("{}", map.toString(), "An empty CompactMap should be represented as {}");
    }

    @Test
    void toStringSingleEntry() {
        map.put("key", "value");
        assertEquals("{key=value}", map.toString(), "A map with a single entry should render as {key=value}");
    }

    @Test
    void toStringMultipleEntries() {
        map.put("a", 1);
        map.put("b", 2);
        String result = map.toString();
        // The order of entries is not guaranteed, so we check for the presence of both pairs.
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should start with '{' and end with '}'");
        assertTrue(result.contains("a=1"), "Result should contain the pair a=1");
        assertTrue(result.contains("b=2"), "Result should contain the pair b=2");
        // Ensure there is a comma and a space separating the entries.
        assertTrue(result.contains(", "), "Entries should be separated by \", \"");
    }

    @Test
    void toStringHandlesSelfReferenceAsKeyAndValue() {
        // Put the map itself as a key and as a value.
        map.put(map, "value");
        map.put("key", map);
        String result = map.toString();
        // Expected fragments when the map references itself.
        String expectedKeyFragment = "(this Map)=value";
        String expectedValueFragment = "key=(this Map)";
        assertTrue(result.startsWith("{") && result.endsWith("}"), "Result should be wrapped in braces");
        assertTrue(result.contains(expectedKeyFragment), "Result should contain the self‑reference key fragment: " + expectedKeyFragment);
        assertTrue(result.contains(expectedValueFragment), "Result should contain the self‑reference value fragment: " + expectedValueFragment);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("simpleScenarios")
    void toStringVariousScenarios(ArgumentsHolder holder) {
        // Populate the map according to the test data.
        for (Object[] pair : holder.entries) {
            Object key = pair[0];
            Object value = pair[1];
            // Handle the special self‑reference cases.
            if ("Self‑referential key".equals(holder.description)) {
                map.put(map, value);
                continue;
            }
            if ("Self‑referential value".equals(holder.description)) {
                map.put(key, map);
                continue;
            }
            if ("Self‑referential both".equals(holder.description)) {
                map.put(map, map);
                continue;
            }
            map.put(key, value);
        }
        String result = map.toString();
        // For the self‑reference cases we cannot use the generic expected string,
        // because the map instance is used as key/value.
        switch(holder.description) {
            case "Self‑referential key":
                assertTrue(result.contains("(this Map)=value"), "Self‑reference as key should be rendered as \"(this Map)=value\"");
                break;
            case "Self‑referential value":
                assertTrue(result.contains("key=(this Map)"), "Self‑reference as value should be rendered as \"key=(this Map)\"");
                break;
            case "Self‑referential both":
                assertEquals("{(this Map)=(this Map)}", result, "Both key and value refer to the map itself");
                break;
            default:
                // Normal deterministic comparison.
                assertEquals(holder.expected, result, "Unexpected toString output for scenario: " + holder.description);
        }
    }

    /* -------------------------------------------------------------
     *  Multiple entries – verify insertion order is preserved
     * ------------------------------------------------------------- */
    @Test
    void toStringMultipleEntriesInsertionOrder() {
        map.put("a", 1);
        map.put("b", 2);
        map.put("c", 3);
        // According to the specification, CompactMap should preserve insertion order.
        assertEquals("{a=1, b=2, c=3}", map.toString(), "Multiple entries should be rendered in insertion order separated by \", \"");
    }

    /* -------------------------------------------------------------
     *  Concurrent modification during toString()
     * ------------------------------------------------------------- */
    @Test
    void toStringFailsFastOnConcurrentModification() {
        // Prepare a normal entry to make the iterator non‑empty.
        map.put("first", 1);
        // Mock a Set<Entry> and its Iterator to throw CME after the first element.
        @SuppressWarnings("unchecked")
        Set<Map.Entry<Object, Object>> mockedSet = mock(Set.class);
        @SuppressWarnings("unchecked")
        Iterator<Map.Entry<Object, Object>> mockedIter = mock(Iterator.class);
        // First call: there is an element.
        when(mockedSet.iterator()).thenReturn(mockedIter);
        // first hasNext -> true
        when(mockedIter.hasNext()).thenReturn(true).thenThrow(new ConcurrentModificationException());
        when(mockedIter.next()).thenReturn(new AbstractMap.SimpleEntry<>("first", 1));
        // Create a CompactMap subclass that returns the mocked entry set.
        CompactMap<Object, Object> fragileMap = new CompactMap<>() {

            @Override
            public Set<Entry<Object, Object>> entrySet() {
                return mockedSet;
            }
        };
        // The toString() call should propagate the ConcurrentModificationException.
        assertThrows(ConcurrentModificationException.class, fragileMap::toString, "toString() must fail fast when the underlying iterator detects concurrent modification");
    }
}
