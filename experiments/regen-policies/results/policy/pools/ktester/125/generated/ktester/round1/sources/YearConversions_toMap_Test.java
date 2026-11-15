package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.CompactMap;
import org.junit.jupiter.api.*;
import java.time.Year;
import java.util.Map;
import static com.cedarsoftware.util.convert.MapConversions.YEAR;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class YearConversions_toMap_Test {

    /**
     * Converter instance used for all tests.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter has a public constructor that requires ConverterOptions.
        // Use default options – no special configuration needed for this test.
        converter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release static resources if any (none for Converter).
        converter = null;
    }

    /**
     * Provides arguments for {@link #testToMapParameterized}.
     *
     * Each argument consists of:
     * <ul>
     *   <li>the {@code Year} (or other object) to pass as {@code from}</li>
     *   <li>a {@link Converter} instance (may be {@code null})</li>
     *   <li>the expected outcome:
     *       <ul>
     *         <li>if a {@link Map} is expected, the map content is supplied</li>
     *         <li>if an exception is expected, the exception class is supplied</li>
     *       </ul>
     *   </li>
     *   <li>a description used only for readability in the test name</li>
     * </ul>
     */
    private static Stream<Arguments> toMapArguments() {
        // dummy converters – they are never used inside the method, but we provide them
        Converter dummy = new Converter(new ConverterOptions());
        Converter throwing = new Converter(new ConverterOptions()) {

            @Override
            public Object convert(Object source, Class<?> targetClass) {
                throw new RuntimeException("Converter should not be called");
            }
        };
        return Stream.of(// Standard positive year, null converter
        Arguments.of(Year.of(2023), null, Map.of(YEAR, 2023), "Standard case – positive year, null converter"), // Negative year (BCE)
        Arguments.of(Year.of(-5), null, Map.of(YEAR, -5), "Edge case – negative year"), // Year zero
        Arguments.of(Year.of(0), null, Map.of(YEAR, 0), "Boundary case – year zero"), // Invalid: null source should cause ClassCastException (the method casts before NPE)
        Arguments.of(null, null, ClassCastException.class, "Invalid input – null source"), // Invalid type: String instead of Year
        Arguments.of("2023", null, ClassCastException.class, "Invalid type – String source"), // Upper bound (Year.MAX_VALUE is 999_999_999)
        Arguments.of(Year.of(999_999_999), null, Map.of(YEAR, 999_999_999), "Maximum bound – Year.MAX_VALUE"), // Lower bound (Year.MIN_VALUE is -999_999_999)
        Arguments.of(Year.of(-999_999_999), null, Map.of(YEAR, -999_999_999), "Minimum bound – Year.MIN_VALUE"), // Non‑null dummy converter
        Arguments.of(Year.of(2023), dummy, Map.of(YEAR, 2023), "Non‑null dummy converter – should be ignored"), // Converter that would throw if used
        Arguments.of(Year.of(2023), throwing, Map.of(YEAR, 2023), "Throwing converter – method must not invoke it"), // Parsing a negative year string via Year.parse("-0045")
        Arguments.of(Year.parse("-0045"), null, Map.of(YEAR, -45), "Parsing negative year string"));
    }

    @Test
    @DisplayName("toMap should return a map containing the YEAR key with the correct integer value")
    void testToMapReturnsMapWithYearValue() {
        // GIVEN
        Year year = Year.of(2023);
        // WHEN
        Map<?, ?> result = YearConversions.toMap(year, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertTrue(result instanceof CompactMap, "Result should be a CompactMap (insertion‑order map)");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(YEAR), "Map must contain the YEAR key");
        assertEquals(year.getValue(), result.get(YEAR), "Map value must match Year.getValue()");
    }

    @Test
    @DisplayName("toMap should throw NullPointerException when the source object is null")
    void testToMapWithNullThrowsNPE() {
        // GIVEN
        Object nullYear = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> YearConversions.toMap(nullYear, converter), "Calling toMap with a null source should throw NPE");
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("toMapArguments")
    void testToMapParameterized(Object from, Converter conv, Object expected, String description) {
        if (expected instanceof Class) {
            // Expected an exception
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            assertThrows(exClass, () -> YearConversions.toMap(from, conv), () -> "Expected " + exClass.getSimpleName() + " for input: " + description);
        } else {
            // Expected a map result
            @SuppressWarnings("unchecked")
            Map<String, Integer> expectedMap = (Map<String, Integer>) expected;
            Map<?, ?> result = YearConversions.toMap(from, conv);
            assertNotNull(result, "Result map must not be null");
            assertTrue(result instanceof CompactMap, "Result should be a CompactMap (insertion‑order map)");
            assertEquals(expectedMap.size(), result.size(), "Result map size should match expected");
            expectedMap.forEach((k, v) -> {
                assertTrue(result.containsKey(k), "Result map must contain key: " + k);
                assertEquals(v, result.get(k), "Value for key '" + k + "' must match");
            });
        }
    }
}
