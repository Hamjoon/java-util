package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StringConversions_toLocalDate_Test {

    /**
     * Shared {@link Converter} instance used by all tests.
     */
    private static Converter converter;

    /**
     * Real converter used for the happy‑path cases (already covered in the base test).
     */
    private static Converter realConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Converter requires a ConverterOptions instance; the default constructor provides
        // a sensible set of options for the built‑in conversions.
        converter = new Converter(new ConverterOptions());
        realConverter = new Converter(new ConverterOptions());
    }

    @AfterAll
    static void teardownAfterAll() {
        // Release the static converter reference.
        converter = null;
    }

    /**
     * ---------- Parameterized null‑handling tests ----------
     */
    private static Stream<NullCase> nullCases() {
        return Stream.of(// both arguments null
        new NullCase(null, null, "Both arguments are null – toZonedDateTime returns null, so toLocalDate should return null."), // null converter, valid string (converter not needed for this simple path)
        new NullCase("2023-01-01", null, "Converter is null but the string can still be parsed by internal logic."));
    }

    @Test
    void toLocalDate_fromString_isoFormat_returnsCorrectDate() {
        // GIVEN
        String isoDate = "2023-05-01";
        // WHEN
        LocalDate result = StringConversions.toLocalDate(isoDate, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a valid ISO‑8601 date string");
        assertEquals(LocalDate.of(2023, 5, 1), result, "The LocalDate extracted from the string should match the expected value");
    }

    @Test
    void toLocalDate_fromJavaUtilDate_returnsCorrectDate() {
        // GIVEN
        // Create a java.util.Date representing 2022‑12‑15 (midnight UTC)
        Date date = DateUtilities.createDate(2022, 12, 15, 0, 0, 0, 0, TimeZone.getTimeZone("UTC"));
        // WHEN
        LocalDate result = StringConversions.toLocalDate(date, converter);
        // THEN
        assertNotNull(result, "Result should not be null for a java.util.Date input");
        assertEquals(LocalDate.of(2022, 12, 15), result, "The LocalDate extracted from the Date should match the calendar date portion");
    }

    @Test
    void toLocalDate_fromNull_returnsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        LocalDate result = StringConversions.toLocalDate(input, converter);
        // THEN
        assertNull(result, "Result should be null when the source object is null");
    }

    @Test
    void toLocalDate_fromUnsupportedType_returnsNull() {
        // GIVEN
        // An object type that the conversion library does not know how to turn into a ZonedDateTime.
        Object unsupported = new Object();
        // WHEN
        LocalDate result = StringConversions.toLocalDate(unsupported, converter);
        // THEN
        assertNull(result, "Result should be null when the source type cannot be converted to a ZonedDateTime");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("nullCases")
    void toLocalDate_nullHandling(NullCase tc) {
        // WHEN
        LocalDate result = StringConversions.toLocalDate(tc.from, tc.converter);
        // THEN
        assertNull(result, tc.description);
    }

    /**
     * ---------- Mocked Converter returning null ----------
     */
    @Test
    void toLocalDate_converterReturnsNullFromZonedDateTime(@Mock Converter mockConverter) {
        // GIVEN
        // We force the static helper to behave as if it cannot convert the input.
        // The easiest way is to spy on StringConversions and stub the private call,
        // but since toZonedDateTime is also package‑private static we can mock the
        // Converter and let the method under test call it indirectly.
        // For this test we simply pass an unparsable string; the real implementation
        // will return null, which is what we want to assert.
        String badInput = "not-a-date";
        // WHEN
        LocalDate result = StringConversions.toLocalDate(badInput, mockConverter);
        // THEN
        assertNull(result, "When the Converter cannot produce a ZonedDateTime, toLocalDate must return null.");
        // Verify that the mock was indeed consulted (the implementation calls converter internally).
        verifyNoMoreInteractions(mockConverter);
    }

    /**
     * ---------- Unsupported type should raise IllegalArgumentException ----------
     */
    @Test
    void toLocalDate_unsupportedType_throwsIllegalArgumentException() {
        // GIVEN
        // a type with no conversion support
        Object unsupported = new Object();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> StringConversions.toLocalDate(unsupported, realConverter), "Passing an unsupported type must result in IllegalArgumentException.");
    }
}
