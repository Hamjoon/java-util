package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

class ZonedDateTimeConversions_toMap_Test {

    private static Converter converter;

    private static Converter defaultConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // The real converter is used for the cases where we do not need special behaviour.
        defaultConverter = new Converter(new ConverterOptions());
    }

    static Stream<ValidCase> validConversionProvider() {
        // 1) ZonedDateTime → String (no custom converter)
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 0, 0, ZoneId.of("UTC"));
        String isoString = "2023-05-17T14:30:00Z[UTC]";
        // 2) Identity converter on a String that already contains a zoned date‑time
        Converter identity = mock(Converter.class);
        when(identity.convert(eq("2023-05-17T14:30:00+02:00[Europe/Paris]"), eq(String.class))).thenAnswer(inv -> inv.getArgument(0));
        // 3) LocalDateTime → ZonedDateTime (UTC) → String via a custom converter
        Converter ldtToZdt = mock(Converter.class);
        LocalDateTime ldt = LocalDateTime.of(2023, 5, 17, 14, 30);
        when(ldtToZdt.convert(eq(ldt), eq(String.class))).thenReturn(// we bypass the intermediate ZonedDateTime step
        isoString);
        return Stream.of(new ValidCase(zdt, null, isoString), new ValidCase("2023-05-17T14:30:00+02:00[Europe/Paris]", identity, "2023-05-17T14:30:00+02:00[Europe/Paris]"), new ValidCase(ldt, ldtToZdt, isoString));
    }

    static Stream<NullCase> nullHandlingProvider() {
        // 1) both source and converter null → NPE
        // 2) source non‑null, converter null → normal conversion (no exception)
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 0, 0, ZoneId.of("UTC"));
        return Stream.of(new NullCase(null, null, NullPointerException.class), // no exception expected
        new NullCase(zdt, null, null));
    }

    static Stream<UnsupportedCase> unsupportedSourceProvider() {
        return Stream.of(new UnsupportedCase(new Object(), null, ClassCastException.class));
    }

    static Stream<ConverterExceptionCase> converterExceptionProvider() {
        // Converter that always throws IllegalArgumentException
        Converter throwing = mock(Converter.class);
        when(throwing.convert(any(), any())).thenThrow(new IllegalArgumentException("forced"));
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 17, 14, 30, 0, 0, ZoneId.of("UTC"));
        return Stream.of(new ConverterExceptionCase(zdt, throwing, IllegalArgumentException.class), new ConverterExceptionCase(null, throwing, IllegalArgumentException.class));
    }

    @Test
    void toMap_WithZonedDateTime_ReturnsMapContainingStringRepresentation() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 12, 34, 56, 0, ZoneId.of("UTC"));
        // WHEN
        Map<String, Object> result = ZonedDateTimeConversions.toMap(zdt, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.ZONED_DATE_TIME), "Map should contain the ZONED_DATE_TIME key");
        // Expected string is whatever the Converter produces for a ZonedDateTime → String conversion.
        // Using the same converter guarantees consistency with the implementation.
        String expected = converter.convert(zdt, String.class);
        assertEquals(expected, result.get(MapConversions.ZONED_DATE_TIME), "Map value should be the string representation of the ZonedDateTime");
    }

    @Test
    void toMap_WithNullSource_ReturnsMapWithNullValue() {
        // GIVEN
        Object source = null;
        // WHEN
        Map<String, Object> result = ZonedDateTimeConversions.toMap(source, defaultConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null even when source is null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.ZONED_DATE_TIME), "Map should contain the ZONED_DATE_TIME key");
        assertNull(result.get(MapConversions.ZONED_DATE_TIME), "Map value should be null when source object is null");
    }

    @ParameterizedTest(name = "[{index}] Valid conversion – from={0}")
    @MethodSource("validConversionProvider")
    void toMap_ValidConversions_ReturnsExpectedMap(ValidCase tc) {
        // GIVEN
        Object from = tc.from();
        // may be null → use defaultConverter
        Converter conv = tc.converter();
        Converter used = conv != null ? conv : defaultConverter;
        String expected = tc.expectedString();
        // WHEN
        Map<String, Object> result = ZonedDateTimeConversions.toMap(from, used);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertEquals(expected, result.get(MapConversions.ZONED_DATE_TIME), "Map value must match the expected string representation");
    }

    @ParameterizedTest(name = "[{index}] Null handling – from={0}")
    @MethodSource("nullHandlingProvider")
    void toMap_NullHandling(NullCase tc) {
        Object from = tc.from();
        // may be null
        Converter conv = tc.converter();
        Class<? extends Throwable> expected = tc.expectedException();
        if (expected != null) {
            // Expect an exception
            assertThrows(expected, () -> ZonedDateTimeConversions.toMap(from, conv), "Expected " + expected.getSimpleName() + " to be thrown");
        } else {
            // Normal behaviour – map should contain the converted string
            Map<String, Object> result = ZonedDateTimeConversions.toMap(from, defaultConverter);
            assertNotNull(result);
            assertEquals(1, result.size());
            assertNotNull(result.get(MapConversions.ZONED_DATE_TIME));
        }
    }

    @ParameterizedTest(name = "[{index}] Unsupported source – {0}")
    @MethodSource("unsupportedSourceProvider")
    void toMap_UnsupportedSourceThrows(UnsupportedCase tc) {
        assertThrows(tc.expectedException(), () -> ZonedDateTimeConversions.toMap(tc.from(), tc.converter()), "Method should propagate the underlying ClassCastException");
    }

    @ParameterizedTest(name = "[{index}] Converter exception – from={0}")
    @MethodSource("converterExceptionProvider")
    void toMap_ConverterThrowsException(ConverterExceptionCase tc) {
        assertThrows(tc.expectedException(), () -> ZonedDateTimeConversions.toMap(tc.from(), tc.converter()), "Method should let the converter‑thrown exception bubble up");
    }
}
