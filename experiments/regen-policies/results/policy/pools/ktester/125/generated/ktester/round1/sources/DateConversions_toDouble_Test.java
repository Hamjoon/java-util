package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Date;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@ExtendWith(MockitoExtension.class)
class DateConversions_toDouble_Test {

    /**
     * A real {@link Converter} instance – the method under test does not use it, but we need a non‑null value.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter constructor requires a ConverterOptions instance.
        converter = new Converter(new ConverterOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure each test starts with a clean state.
        converter = null;
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> validDatesProvider() {
        return Stream.of(arguments("Epoch start (milliseconds = 0) should be converted to 0.0 seconds.", new Date(0L), 0.0), arguments("Typical recent timestamp (2020‑09‑13) yields whole‑second double value.", new Date(1_600_000_000_000L), 1_600_000_000.0));
    }

    private static Stream<org.junit.jupiter.params.provider.Arguments> invalidTypeProvider() {
        return Stream.of(arguments("Providing a non‑Date object (String) should trigger a ClassCastException during the cast to Date.", "2020-01-01"), arguments("Providing an Integer instead of a Date should also result in a ClassCastException.", 12345));
    }

    @Test
    void toDouble_WithDate_ReturnsSeconds() {
        // GIVEN
        // arbitrary timestamp
        long epochMillis = 1_234_567_890_123L;
        Date date = new Date(epochMillis);
        double expectedSeconds = epochMillis / 1000.0;
        // WHEN
        double actualSeconds = DateConversions.toDouble(date, converter);
        // THEN
        assertEquals(expectedSeconds, actualSeconds, 1e-9, "The double value should represent epoch milliseconds divided by 1000.0");
    }

    @Test
    void toDouble_NullInput_ThrowsNullPointerException() {
        // GIVEN
        Object nullObject = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> DateConversions.toDouble(nullObject, converter), "Passing null as the source should result in NullPointerException");
    }

    @Test
    void toDouble_DoesNotInteractWithConverter() {
        // GIVEN
        // Use a spy to verify that the converter is never called.
        Converter spyConverter = org.mockito.Mockito.spy(converter);
        Date date = new Date(0L);
        // WHEN
        DateConversions.toDouble(date, spyConverter);
        // THEN
        // The method should not call any method on the Converter instance.
        org.mockito.Mockito.verifyNoInteractions(spyConverter);
    }

    /**
     * Valid {@link Date} instances and the expected double values.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("validDatesProvider")
    void toDouble_ValidDates(String description, Date from, Double expected) {
        // The converter argument is deliberately set to {@code null} because the method does not use it.
        double actual = DateConversions.toDouble(from, null);
        assertEquals(expected, actual, 1e-9, description);
    }

    /**
     * Source objects that are not {@link Date}s – expecting a {@link ClassCastException}.
     */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("invalidTypeProvider")
    void toDouble_InvalidType(String description, Object from) {
        assertThrows(ClassCastException.class, () -> DateConversions.toDouble(from, null), description);
    }

    /**
     * {@code null} as the source object – expecting a {@link NullPointerException}.
     */
    @Test
    void toDouble_NullFrom_ThrowsNullPointerException() {
        // The JSON test case expects a NullPointerException when the source is null.
        assertThrows(NullPointerException.class, () -> DateConversions.toDouble(null, null), "Passing null as the 'from' argument should cause a NullPointerException");
    }
}
