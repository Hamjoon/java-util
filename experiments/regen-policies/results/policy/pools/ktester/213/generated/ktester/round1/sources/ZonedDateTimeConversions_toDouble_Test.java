package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class ZonedDateTimeConversions_toDouble_Test {

    /**
     * Mock for the static methods of {@link InstantConversions}.
     */
    private static MockedStatic<InstantConversions> instantConversionsMock;

    /**
     * A real {@link Converter} instance used for the delegation call.
     */
    private static Converter converter;

    @BeforeAll
    static void setupBeforeAll() {
        // Initialise the static mock for InstantConversions.
        instantConversionsMock = mockStatic(InstantConversions.class);
        // Create a default Converter (options can be empty).
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Reset the mock before each test to avoid cross‑test interference.
        instantConversionsMock.reset();
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static mock.
        instantConversionsMock.close();
    }

    /**
     * Provides a set of valid {@link ZonedDateTime} instances together with
     * the {@link Converter} to be used (may be {@code null}) and the expected
     * double value (epoch seconds with fractional nanoseconds).
     */
    private static Stream<Arguments> provideValidZonedDateTimes() {
        return Stream.of(// Standard UTC ZonedDateTime with nanoseconds.
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 12, 30, 15, 123_000_000, ZoneOffset.UTC), new Converter(new ConverterOptions()), 1679747415.123), // Same instant expressed with a +02:00 offset; converter is null.
        Arguments.of(ZonedDateTime.of(2023, 3, 25, 14, 30, 15, 123_000_000, ZoneOffset.ofHours(2)), null, 1679747415.123), // Date before the Unix epoch (negative epoch seconds).
        Arguments.of(ZonedDateTime.of(1960, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC), new Converter(new ConverterOptions()), -315619200.0), // Very large future instant (close to Long.MAX_VALUE milliseconds).
        Arguments.of(ZonedDateTime.ofInstant(Instant.ofEpochSecond(Long.MAX_VALUE / 1000), ZoneOffset.UTC), new Converter(new ConverterOptions()), 9.223372036854776E18));
    }

    @Test
    void testToDouble_DelegatesToInstantConversions() {
        // GIVEN
        ZonedDateTime zdt = ZonedDateTime.of(2023, 5, 10, 12, 30, 45, 0, ZoneOffset.UTC);
        Instant instant = zdt.toInstant();
        double expected = 12345.678;
        // Mock the static call that ZonedDateTimeConversions delegates to.
        instantConversionsMock.when(() -> InstantConversions.toDouble(eq(instant), eq(converter))).thenReturn(expected);
        // WHEN
        double result = ZonedDateTimeConversions.toDouble(zdt, converter);
        // THEN
        assertEquals(expected, result, "The returned double should match the mocked value.");
        instantConversionsMock.verify(() -> InstantConversions.toDouble(eq(instant), eq(converter)));
    }

    @Test
    void testToDouble_NullInput_ThrowsNullPointerException() {
        // GIVEN a null source object
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> ZonedDateTimeConversions.toDouble(null, converter), "Passing null should result in a NullPointerException.");
    }

    @Test
    void testToDouble_InvalidType_ThrowsClassCastException() {
        // GIVEN an object that is not a ZonedDateTime
        Object notZdt = "I am not a ZonedDateTime";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> ZonedDateTimeConversions.toDouble(notZdt, converter), "Passing an object of the wrong type should cause a ClassCastException.");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("provideValidZonedDateTimes")
    void testToDouble_ValidZonedDateTime(ZonedDateTime from, Converter conv, double expected) {
        // GIVEN
        Instant instant = from.toInstant();
        // Mock the static delegation to return the pre‑computed expected value.
        instantConversionsMock.when(() -> InstantConversions.toDouble(eq(instant), same(conv))).thenReturn(expected);
        // WHEN
        double result = ZonedDateTimeConversions.toDouble(from, conv);
        // THEN
        assertEquals(expected, result, 1e-12, () -> "Expected double value does not match for ZonedDateTime: " + from);
        instantConversionsMock.verify(() -> InstantConversions.toDouble(eq(instant), same(conv)));
    }
}
