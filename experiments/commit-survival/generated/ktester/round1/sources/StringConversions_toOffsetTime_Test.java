package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toOffsetTime_Test {

    /**
     * a mock Converter – the real conversion logic inside {@code StringConversions}
     *  does not depend on any behaviour of the Converter for the scenarios we test.
     */
    @Mock
    Converter mockConverter;

    /**
     * A mock Converter that throws on any method invocation – used to simulate a converter‑induced failure.
     */
    private Converter mockThrowingConverter;

    @BeforeEach
    void setUp() {
        // Any interaction with this mock will throw a RuntimeException, simulating a failing converter.
        mockThrowingConverter = Mockito.mock(Converter.class, invocation -> {
            throw new RuntimeException("converter failure");
        });
    }

    private static Stream<Arguments> edgeCaseProvider() {
        return Stream.of(Arguments.of("Fractional seconds are preserved", "12:30:45.123+02:00", OffsetTime.of(12, 30, 45, 123_000_000, ZoneOffset.ofHours(2))), Arguments.of("Offset with hour and minute components", "12:30:45+05:30", OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHoursMinutes(5, 30))), Arguments.of("Explicit zero offset '+00:00' maps to UTC", "12:30:45+00:00", OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC)));
    }

    @Test
    void shouldParseValidOffsetTimeString() {
        // given
        String source = "12:34:56+02:00";
        // when
        OffsetTime result = StringConversions.toOffsetTime(source, mockConverter);
        // then
        OffsetTime expected = OffsetTime.of(12, 34, 56, 0, ZoneOffset.ofHours(2));
        assertEquals(expected, result);
    }

    @Test
    void shouldFallBackToOffsetDateTimeWhenTimeParseFails() {
        // given – a full date‑time string; OffsetTime.parse will fail,
        // but StringConversions.toOffsetDateTime can handle ISO_OFFSET_DATE_TIME.
        String source = "2023-03-25T10:15:30+01:00";
        // when
        OffsetTime result = StringConversions.toOffsetTime(source, mockConverter);
        // then
        OffsetTime expected = OffsetTime.of(10, 15, 30, 0, ZoneOffset.ofHours(1));
        assertEquals(expected, result);
    }

    @Test
    void shouldReturnNullWhenBothParsersReturnNull() {
        // given – a null input leads to a null string; the first parse throws,
        // the fallback to OffsetDateTime also returns null, so the method should return null.
        Object source = null;
        // when
        OffsetTime result = StringConversions.toOffsetTime(source, mockConverter);
        // then
        assertNull(result);
    }

    @Test
    void shouldThrowIllegalArgumentExceptionForUnparseableString() {
        // given
        String source = "invalid-time";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(source, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to parse"));
        assertTrue(ex.getMessage().contains(source));
    }

    @Test
    void shouldParseZuluOffsetViaFallback() {
        // given – the 'Z' designator is not accepted by ISO_OFFSET_TIME, so we fall back.
        String source = "2023-04-05T12:30:45Z";
        // when
        OffsetTime result = StringConversions.toOffsetTime(source, mockConverter);
        // then
        OffsetTime expected = OffsetTime.of(12, 30, 45, 0, ZoneOffset.UTC);
        assertEquals(expected, result);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenInputIsNull() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> StringConversions.toOffsetTime(source, mockConverter));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenStringIsEmpty() {
        // given
        String source = "";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(source, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to parse"));
    }

    @Test
    void shouldThrowIllegalArgumentExceptionWhenStringHasWhitespace() {
        // given
        String source = " 12:30:45+02:00 ";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(source, mockConverter));
        assertTrue(ex.getMessage().contains("Unable to parse"));
    }

    @Test
    void shouldThrowClassCastExceptionWhenFromIsNotAString() {
        // given – an Integer is not a String.
        Integer source = 12345;
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toOffsetTime(source, mockConverter));
    }

    @Test
    void shouldThrowClassCastExceptionWhenFromIsStringBuilder() {
        // given – a StringBuilder is not a String.
        StringBuilder source = new StringBuilder("12:30:45+02:00");
        // when / then
        assertThrows(ClassCastException.class, () -> StringConversions.toOffsetTime(source, mockConverter));
    }

    @Test
    void shouldIgnoreNonNullConverterWhenParsingSucceeds() {
        // given – a normal mock converter that is not used.
        String source = "12:30:45+02:00";
        // when
        OffsetTime result = StringConversions.toOffsetTime(source, mockConverter);
        // then
        OffsetTime expected = OffsetTime.of(12, 30, 45, 0, ZoneOffset.ofHours(2));
        assertEquals(expected, result);
    }

    @Test
    void shouldWrapParseExceptionWhenConverterThrowsDuringFallback() {
        // given – a string that forces the fallback to toOffsetDateTime,
        // and a converter that throws on any call.
        String source = "2023-04-05T12:30:45+02:00";
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> StringConversions.toOffsetTime(source, mockThrowingConverter));
        // The cause should be the original DateTimeParseException from the first parse attempt.
        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof DateTimeParseException);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("edgeCaseProvider")
    void edgeCases(String description, String source, OffsetTime expected) {
        // when
        OffsetTime result = StringConversions.toOffsetTime(source, mockConverter);
        // then
        assertEquals(expected, result, description);
    }
}
