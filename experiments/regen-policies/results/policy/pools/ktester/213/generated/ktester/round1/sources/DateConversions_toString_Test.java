package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Instant;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.sql.Timestamp;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class DateConversions_toString_Test {

    /**
     * The Converter instance required by the method under test.
     */
    private Converter converter;

    /**
     * A mock Converter used to verify that the method does **not** interact with it.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // The real Converter is not needed for the logic of DateConversions.toString,
        // but we create one to demonstrate a typical usage scenario.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock after each test to avoid cross‑test interference.
        reset(mockConverter);
    }

    /**
     * Parameter source that encodes every scenario described in the supplied JSON.
     * Each {@link Arguments} entry contains:
     * <ul>
     *   <li>the {@code from} object (may be {@code null})</li>
     *   <li>the {@code Converter} to pass (null, a real instance or a mock)</li>
     *   <li>the expected formatted string (null when an exception is expected)</li>
     *   <li>the expected exception class (null when a normal result is expected)</li>
     *   <li>a short description used as the test name</li>
     * </ul>
     */
    static Stream<Arguments> toStringProvider() {
        return Stream.of(// basic functionality -------------------------------------------------
        Arguments.of(new Date(0L), null, "1970-01-01T00:00:00.000Z", null, "Epoch start date should be formatted with exactly three fractional digits"), Arguments.of(new Date(1_234_567_890_123L), null, "2009-02-13T23:31:30.123Z", null, "Typical timestamp with milliseconds part 123 should retain three digits"), Arguments.of(new Date(1_234_567_890_456L), null, "2009-02-13T23:31:30.456Z", null, "Typical timestamp with milliseconds part 456 should retain three digits"), Arguments.of(new Date(-1L), null, "1969-12-31T23:59:59.999Z", null, "Negative epoch (1 ms before 1970) should be formatted correctly with rounding to three decimal places"), Arguments.of(new Timestamp(1_609_459_200_123L), null, "2021-01-01T00:00:00.123Z", null, "SQL Timestamp is a subclass of Date; ensure it is handled identically"), // null input ---------------------------------------------------------
        Arguments.of(null, null, null, NullPointerException.class, "Passing null should result in NPE when casting to Date"), // future date handling ------------------------------------------------
        Arguments.of(new Date(253_402_300_799_000L), null, "9999-12-31T23:59:59.000Z", null, "Maximum representable year before 10000 should be formatted correctly with three fractional digits"), // one‑day offset ------------------------------------------------------
        Arguments.of(new Date(86_400_000L), null, "1970-01-02T00:00:00.000Z", null, "A date exactly one day after the epoch should be rendered with correct date part and zero milliseconds"), // java.sql.Date handling ---------------------------------------------
        Arguments.of(new java.sql.Date(0L), null, "1970-01-01T00:00:00.000Z", null, "java.sql.Date is a subclass of java.util.Date; formatting must be identical to a plain Date at epoch"), // extreme long values (positive) ------------------------------------
        Arguments.of(new Date(Long.MAX_VALUE), null, "292278994-08-17T07:12:55.807Z", null, "Maximum long millisecond value should be converted to the far‑future instant and formatted with three fractional digits"), // extreme long values (negative) ------------------------------------
        Arguments.of(new Date(Long.MIN_VALUE), null, "-292278994-08-17T07:12:55.808Z", null, "Minimum long millisecond value should be converted to the far‑past instant (negative year) and formatted correctly"), // non‑Date input ------------------------------------------------------
        Arguments.of("some string", null, null, ClassCastException.class, "Passing an object that is not a java.util.Date (e.g., a String) should result in a ClassCastException"));
    }

    @Test
    void toString_formatsEpochZeroCorrectly() {
        // GIVEN
        // 1970‑01‑01T00:00:00.000Z
        Date epoch = Date.from(Instant.ofEpochMilli(0));
        // WHEN
        String result = DateConversions.toString(epoch, converter);
        // THEN
        assertEquals("1970-01-01T00:00:00.000Z", result);
    }

    @Test
    void toString_formatsArbitraryInstantCorrectly() {
        // GIVEN
        Instant instant = Instant.parse("2021-03-14T15:09:26.123Z");
        Date date = Date.from(instant);
        String expected = "2021-03-14T15:09:26.123Z";
        // WHEN
        String result = DateConversions.toString(date, converter);
        // THEN
        assertEquals(expected, result);
    }

    @Test
    void toString_ignoresConverterParameter() {
        // GIVEN
        // any non‑null date
        Date now = new Date();
        // WHEN
        String result = DateConversions.toString(now, mockConverter);
        // THEN
        // The method should produce a correctly formatted string regardless of the Converter.
        assertNotNull(result);
        // Verify that the mock Converter was never used.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toString_throwsClassCastExceptionWhenNotADate() {
        // GIVEN
        Object notADate = "2021-01-01";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> DateConversions.toString(notADate, converter));
    }

    /**
     * Executes the matrix defined in {@link #toStringProvider()}.
     * <p>
     * The test asserts either the exact string output or the expected exception.
     * It also confirms that the {@link Converter} argument is never touched
     * (when a mock is supplied) – the method under test deliberately ignores it.
     */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("toStringProvider")
    void toString_parameterised(Object from, Converter conv, String expected, Class<? extends Throwable> expectedException, String description) {
        // Use the mockConverter when the test explicitly wants to verify "no interaction".
        // For all other rows we simply pass the supplied Converter (which may be null or a real instance).
        Converter converterToUse = (conv == null && description.contains("converter")) ? mockConverter : conv;
        if (expectedException != null) {
            assertThrows(expectedException, () -> DateConversions.toString(from, converterToUse), description);
        } else {
            String result = DateConversions.toString(from, converterToUse);
            assertEquals(expected, result, description);
        }
        // If we handed the mock to the method, verify that it was never used.
        if (converterToUse == mockConverter) {
            verifyNoInteractions(mockConverter);
        }
    }
}
