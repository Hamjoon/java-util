package com.cedarsoftware.util.convert;

import org.junit.Test;
import static org.junit.Assert.*;

public class StringBuilderConversionsTest {

    // -----------------------------------------------------------------
    // Basic functionality test
    // -----------------------------------------------------------------
    @Test
    public void basic_toString_returnsObjectToString() {
        Converter dummy = new Converter(new ConverterOptions());
        String result = StringBuilderConversions.toString(123, dummy);
        assertEquals("123", result);
    }

    // -----------------------------------------------------------------
    // Edge‑case / exception tests
    // -----------------------------------------------------------------
    @Test(expected = NullPointerException.class)
    public void edge_nullFrom_throwsNPE() {
        Converter dummy = new Converter(new ConverterOptions());
        StringBuilderConversions.toString(null, dummy);
    }

    @Test
    public void edge_customObjectToString() {
        Converter dummy = new Converter(new ConverterOptions());
        class Foo {
            @Override public String toString() { return "foo‑value"; }
        }
        String result = StringBuilderConversions.toString(new Foo(), dummy);
        assertEquals("foo‑value", result);
    }

    @Test
    public void edge_emptyStringFromObject() {
        Converter dummy = new Converter(new ConverterOptions());
        Object empty = new Object() {
            @Override public String toString() { return ""; }
        };
        String result = StringBuilderConversions.toString(empty, dummy);
        assertEquals("", result);
    }

    @Test
    public void edge_nullConverter_isIgnored() {
        String result = StringBuilderConversions.toString("test‑null‑converter", null);
        assertEquals("test‑null‑converter", result);
    }

    @Test
    public void edge_converterInstance_isIgnored() {
        Converter real = new Converter(new ConverterOptions());
        String result = StringBuilderConversions.toString("test‑real‑converter", real);
        assertEquals("test‑real‑converter", result);
    }

    @Test
    public void edge_mutableStringBuilderSnapshot() {
        Converter dummy = new Converter(new ConverterOptions());
        StringBuilder sb = new StringBuilder("initial");
        String result = StringBuilderConversions.toString(sb, dummy);
        // mutate after the conversion call
        sb.append("-changed");
        assertEquals("initial", result);
    }
}
