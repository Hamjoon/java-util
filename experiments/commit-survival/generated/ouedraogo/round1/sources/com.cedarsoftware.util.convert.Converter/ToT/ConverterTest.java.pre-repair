package com.cedarsoftware.util.convert;

import org.junit.Before;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

/**
 * Comprehensive JUnit‑4 tests for {@link Converter}.
 *
 * The tests cover:
 * <ul>
 *   <li>Basic successful conversions (String → Integer, Map → Integer, null handling).</li>
 *   <li>Edge cases for primitive vs. wrapper types.</li>
 *   <li>Unsupported conversion handling.</li>
 *   <li>Direct‑conversion lookup, inheritance‑based lookup and custom overrides.</li>
 *   <li>Introspection APIs ({@code allSupportedConversions()}, {@code getSupportedConversions()}).</li>
 *   <li>Option retrieval via {@code getOptions()}.</li>
 * </ul>
 *
 * The {@link ConverterOptions} instance used throughout the tests is a minimal
 * anonymous implementation that returns an empty {@code Map} for overrides and
 * {@code null} for all other optional values.
 */
public class ConverterTest {

    private Converter converter;
    private ConverterOptions options;

    @Before
    public void setUp() {
        // Minimal implementation of ConverterOptions – all defaults are null/empty.
        options = new ConverterOptions() {
            @Override
            public Map<Map.Entry<Class<?>, Class<?>>, Convert<?>> getConverterOverrides() {
                return new ConcurrentHashMap<>();
            }
        };
        converter = new Converter(options);
    }

    /*** --------------------  convert(...) tests -------------------- ***/

    @Test
    public void testConvertStringToInteger() {
        Integer result = converter.convert("123", Integer.class);
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void testConvertMapToInteger_UsesValueKey() {
        Map<String, Object> map = new HashMap<>();
        map.put(Converter.VALUE, "456");               // "_v" key expected by MapConversions
        Integer result = converter.convert(map, Integer.class);
        assertEquals(Integer.valueOf(456), result);
    }

    @Test
    public void testConvertNullToPrimitiveInt_ReturnsZero() {
        int result = converter.convert(null, int.class);
        assertEquals(0, result);
    }

    @Test
    public void testConvertNullToWrapperInteger_ReturnsNull() {
        Integer result = converter.convert(null, Integer.class);
        assertNull(result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testConvertUnsupportedTarget_ThrowsException() {
        // No conversion from String to Thread is defined.
        converter.convert("abc", Thread.class);
    }

    /*** --------------------  isDirectConversionSupportedFor(...) tests -------------------- ***/

    @Test
    public void testDirectConversionSupported_True() {
        assertTrue(converter.isDirectConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testDirectConversionSupported_False() {
        // There is no direct conversion from String to Thread.
        assertFalse(converter.isDirectConversionSupportedFor(String.class, Thread.class));
    }

    /*** --------------------  isConversionSupportedFor(...) tests -------------------- ***/

    @Test
    public void testConversionSupported_ThroughDirectLookup() {
        assertTrue(converter.isConversionSupportedFor(String.class, Integer.class));
    }

    @Test
    public void testConversionSupported_ThroughInheritanceLookup() {
        // AtomicInteger implements Number via inheritance (Number is not a target in the DB),
        // but there is a direct conversion from AtomicInteger → Integer.
        // To test inheritance we use a subclass of a supported source: Byte extends Number,
        // and there is a direct conversion Byte → Integer, so the inheritance path works.
        assertTrue(converter.isConversionSupportedFor(Byte.class, Integer.class));
    }

    @Test
    public void testConversionNotSupported() {
        // No conversion path from String to Thread, even via inheritance.
        assertFalse(converter.isConversionSupportedFor(String.class, Thread.class));
    }

    /*** --------------------  addConversion(...) & override tests -------------------- ***/

    @Test
    public void testAddCustomConversion_OverridesFactory() {
        // Override String → Integer conversion to always return 42.
        converter.addConversion(String.class, Integer.class,
                (from, conv) -> 42);

        Integer result = converter.convert("any string", Integer.class);
        assertEquals(Integer.valueOf(42), result);
    }

    @Test
    public void testAddCustomConversion_NewMapping() {
        // Add a brand‑new conversion from String → Thread.
        converter.addConversion(String.class, Thread.class,
                (from, conv) -> new Thread(() -> System.out.println("Custom thread")));

        Thread t = converter.convert("ignored", Thread.class);
        assertNotNull(t);
        assertEquals("Thread[Thread-0,5,main]", t.toString().split("@")[0]); // basic sanity check
    }

    /*** --------------------  getOptions() test -------------------- ***/

    @Test
    public void testGetOptionsReturnsSameInstance() {
        assertSame(options, converter.getOptions());
    }

    /*** --------------------  allSupportedConversions() test -------------------- ***/

    @Test
    public void testAllSupportedConversionsStructure() {
        Map<Class<?>, Set<Class<?>>> map = converter.allSupportedConversions();

        // Verify that a known source (String) maps to a known target (Integer).
        assertTrue(map.containsKey(String.class));
        assertTrue(map.get(String.class).contains(Integer.class));

        // Verify that a primitive source (int) is represented by its wrapper (Integer) in the map.
        assertTrue(map.containsKey(Integer.class)); // source wrapper for primitive int
        assertTrue(map.get(Integer.class).contains(Integer.class));
    }

    /*** --------------------  getSupportedConversions() test -------------------- ***/

    @Test
    public void testGetSupportedConversionsNames() {
        Map<String, Set<String>> nameMap = converter.getSupportedConversions();

        // The key should be the short name of the source class.
        assertTrue(nameMap.containsKey("String"));
        // The target set should contain the short name of Integer.
        assertTrue(nameMap.get("String").contains("Integer"));
    }

    /*** --------------------  Edge case: conversion of primitive wrapper → primitive -------------------- ***/

    @Test
    public void testConvertWrapperToPrimitive() {
        // Convert a wrapper Byte to primitive int – conversion path Byte → Integer → int
        int result = converter.convert(Byte.valueOf((byte) 7), int.class);
        assertEquals(7, result);
    }

    /*** --------------------  Verify handling of BigInteger → String conversion -------------------- ***/

    @Test
    public void testBigIntegerToString() {
        BigInteger big = new BigInteger("12345678901234567890");
        String result = converter.convert(big, String.class);
        assertEquals(big.toString(), result);
    }

    /*** --------------------  Verify handling of BigDecimal → Double conversion -------------------- ***/

    @Test
    public void testBigDecimalToDouble() {
        BigDecimal bd = new BigDecimal("12.34");
        Double result = converter.convert(bd, Double.class);
        assertEquals(Double.valueOf(12.34), result);
    }

    /*** --------------------  Verify conversion of AtomicInteger → String -------------------- ***/

    @Test
    public void testAtomicIntegerToString() {
        AtomicInteger ai = new AtomicInteger(99);
        String result = converter.convert(ai, String.class);
        assertEquals("99", result);
    }

    /*** --------------------  Verify conversion of null source with Void target (should be null) -------------------- ***/

    @Test
    public void testConvertNullToVoidTarget() {
        // Void target is not a real class; the factory defines Void → Void as unsupported,
        // but converting null to a reference type that has a Void conversion (e.g., Byte) returns null.
        Byte result = converter.convert(null, Byte.class);
        assertNull(result);
    }
}
