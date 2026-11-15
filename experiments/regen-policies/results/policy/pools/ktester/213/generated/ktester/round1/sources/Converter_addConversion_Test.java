package com.cedarsoftware.util.convert;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.ParameterizedTest;
import java.util.stream.Stream;

class Converter_addConversion_Test {

    /* --------------------------------------------------------------------- */
    private static class SourceA {
    }

    private static class TargetA {
    }

    private static class SourceB {
    }

    private static class TargetB {
    }

    @BeforeEach
    void setUp() throws Exception {
        clearUserDb();
    }

    /**
     * Utility that wipes the internal {@code USER_DB} map via reflection.
     * This is required because the map is static and would otherwise retain
     * entries from previous test methods.
     */
    private static void clearUserDb() throws Exception {
        Field f = Converter.class.getDeclaredField("USER_DB");
        f.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<?, ?> map = (Map<?, ?>) f.get(null);
        if (map instanceof ConcurrentHashMap) {
            ((ConcurrentHashMap<?, ?>) map).clear();
        }
    }

    private static Stream<Arguments> addConversionRetrievalProvider() {
        Convert<Object> first = (from, conv) -> Integer.valueOf((String) from);
        Convert<Object> second = (from, conv) -> Integer.valueOf(((String) from).length());
        return Stream.of(Arguments.of("First registration of String → Integer returns null", String.class, Integer.class, first, null), Arguments.of("Second registration of same pair returns the first conversion", String.class, Integer.class, second, first));
    }

    private static Stream<Arguments> primitiveNormalizationProvider() {
        Convert<Object> first = (from, conv) -> Integer.valueOf(((Number) from).intValue());
        Convert<Object> second = (from, conv) -> ((Number) from).intValue();
        return Stream.of(Arguments.of("Register conversion with primitive int source, Integer target – first time returns null", int.class, Integer.class, first, null), Arguments.of("Second registration for same normalized pair returns the first conversion", int.class, Integer.class, second, first));
    }

    /**
     * Helper data for the “addConversionRetrieval” group.
     */
    private static List<Object[]> addConversionRetrievalData() {
        Convert<Object> first = (from, conv) -> Integer.valueOf((String) from);
        Convert<Object> second = (from, conv) -> Integer.valueOf(((String) from).length());
        return Arrays.asList(new Object[][] { { "First registration of String → Integer returns null", String.class, Integer.class, first, null }, { "Second registration of same pair returns the first conversion", String.class, Integer.class, second, first } });
    }

    /**
     * Helper data for the “primitiveNormalization” group.
     */
    private static List<Object[]> primitiveNormalizationData() {
        Convert<Object> first = (from, conv) -> Integer.valueOf(((Number) from).intValue());
        Convert<Object> second = (from, conv) -> ((Number) from).intValue();
        return Arrays.asList(new Object[][] { { "Register conversion with primitive int source, Integer target – first time returns null", int.class, Integer.class, first, null }, { "Second registration for same normalized pair returns the first conversion", int.class, Integer.class, second, first } });
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testAddConversionReturnsNullWhenNoPrevious() {
        Convert<Object> conversion = (from, conv) -> "dummy";
        Convert<?> previous = Converter.addConversion(SourceA.class, TargetA.class, conversion);
        Assertions.assertNull(previous, "First registration should return null");
    }

    @Test
    void testAddConversionReplacesExisting() {
        Convert<Object> first = (from, conv) -> "first";
        Convert<Object> second = (from, conv) -> "second";
        Convert<Object> third = (from, conv) -> "third";
        // first registration – expect null
        Assertions.assertNull(Converter.addConversion(SourceB.class, TargetB.class, first));
        // second registration – should return the first conversion
        Convert<?> returnedSecond = Converter.addConversion(SourceB.class, TargetB.class, second);
        Assertions.assertSame(first, returnedSecond, "Second registration should return the first conversion");
        // third registration – should return the second conversion
        Convert<?> returnedThird = Converter.addConversion(SourceB.class, TargetB.class, third);
        Assertions.assertSame(second, returnedThird, "Third registration should return the second conversion");
    }

    @Test
    void testPrimitiveTypesAreWrapped() {
        Convert<Object> intToLong = (from, conv) -> ((Integer) from).longValue();
        // register using primitive types
        Convert<?> first = Converter.addConversion(int.class, long.class, intToLong);
        Assertions.assertNull(first, "First registration with primitives should return null");
        // register again using wrapper types – should retrieve the previous conversion
        Convert<?> second = Converter.addConversion(Integer.class, Long.class, (from, conv) -> 0L);
        Assertions.assertSame(intToLong, second, "Wrapper registration should return the previously stored conversion");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("addConversionRetrievalProvider")
    void testAddConversionRetrieval(String description, Class<?> source, Class<?> target, Convert<?> conversionToAdd, Convert<?> expectedPrevious) {
        Convert<?> previous = Converter.addConversion(source, target, conversionToAdd);
        Assertions.assertSame(expectedPrevious, previous, description + " – unexpected previous conversion");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("primitiveNormalizationProvider")
    void testPrimitiveNormalization(String description, Class<?> source, Class<?> target, Convert<?> conversionToAdd, Convert<?> expectedPrevious) {
        Convert<?> previous = Converter.addConversion(source, target, conversionToAdd);
        Assertions.assertSame(expectedPrevious, previous, description + " – unexpected previous conversion");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void testNullConversionMethodIsAccepted() {
        // first registration with a null conversion method
        Convert<?> previous = Converter.addConversion(String.class, Boolean.class, null);
        Assertions.assertNull(previous, "First registration with null conversion should return null");
        // second registration – the stored null should be returned
        Convert<?> secondPrev = Converter.addConversion(String.class, Boolean.class, (from, conv) -> Boolean.TRUE);
        Assertions.assertNull(secondPrev, "Second registration should return the previously stored null");
    }

    @Test
    void testIndependentPairs() {
        Convert<Object> sToD = (from, conv) -> Double.valueOf((String) from);
        Convert<Object> iToS = (from, conv) -> from.toString();
        Convert<Object> sToDSecond = (from, conv) -> Double.valueOf(((String) from).length());
        // Pair 1 – first registration
        Assertions.assertNull(Converter.addConversion(String.class, Double.class, sToD));
        // Pair 2 – first registration
        Assertions.assertNull(Converter.addConversion(Integer.class, String.class, iToS));
        // Pair 1 – second registration should return the first conversion
        Convert<?> previous = Converter.addConversion(String.class, Double.class, sToDSecond);
        Assertions.assertSame(sToD, previous, "Second registration for first pair should return the original conversion");
    }

    @Test
    void testNullSourceClassThrowsNPE() {
        Assertions.assertThrows(NullPointerException.class, () -> Converter.addConversion(null, String.class, (from, conv) -> "ignored"), "Adding a conversion with a null source class must throw NullPointerException");
    }
}
