package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderConversionsTest {

    /*** Basic functionality ***/
    @Test
    public void testToString_basic() {
        StringBuilder sb = new StringBuilder("hello");
        String result = StringBuilderConversions.toString(sb, null);
        assertEquals("hello", result);
    }

    /*** Edge case: null `from` argument ***/
    @Test(expected = NullPointerException.class)
    public void testToString_nullFrom() {
        StringBuilderConversions.toString(null, null);
    }

    /*** Edge case: `toString()` returns null ***/
    @Test
    public void testToString_toStringReturnsNull() {
        Object obj = new Object() {
            @Override public String toString() { return null; }
        };
        String result = StringBuilderConversions.toString(obj, null);
        assertNull(result);
    }

    /*** Edge case: null `converter` (method ignores it) ***/
    @Test
    public void testToString_nullConverter() {
        Integer value = 42;
        String result = StringBuilderConversions.toString(value, null);
        assertEquals("42", result);
    }

    /*** Additional typical use – different object types ***/
    @Test
    public void testToString_variousObjects() {
        assertEquals("true", StringBuilderConversions.toString(Boolean.TRUE, null));
        assertEquals("3.14", StringBuilderConversions.toString(3.14, null));
        assertEquals("[a, b]", StringBuilderConversions.toString(java.util.Arrays.asList("a", "b"), null));
    }
}
