package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import java.time.LocalDate;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateTimeConversions_toMap_Test {

    /**
     * The mock Converter is not used by the method under test, but it must be supplied.
     */
    @Mock
    private Converter mockConverter;

    private static Stream<Arguments> provideSuccessCases() {
        return Stream.of(Arguments.of("Normal case", LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000), "2023-05-17T14:30:45.123"), Arguments.of("Minimum year without nanoseconds", LocalDateTime.of(-999_999_999, 1, 1, 0, 0, 0, 0), "-999999999-01-01T00:00"), Arguments.of("Minimum year with maximum nanoseconds", LocalDateTime.of(-999_999_999, 12, 31, 23, 59, 59, 999_999_999), "-999999999-12-31T23:59:59.999999999"), Arguments.of("Maximum year without nanoseconds", LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59, 0), "999999999-12-31T23:59:59"), Arguments.of("Maximum year with maximum nanoseconds", LocalDateTime.of(999_999_999, 12, 31, 23, 59, 59, 999_999_999), "999999999-12-31T23:59:59.999999999"));
    }

    @Test
    void toMap_returnsMapContainingLocalDateTimeString() {
        // GIVEN
        LocalDateTime source = LocalDateTime.of(2023, 5, 10, 15, 30, 45, 123_456_789);
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(source, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.LOCAL_DATE_TIME), "Map should contain the key defined in MapConversions.LOCAL_DATE_TIME");
        assertEquals(source.toString(), result.get(MapConversions.LOCAL_DATE_TIME), "The value stored in the map must be the string representation of the LocalDateTime");
    }

    @Test
    void toMap_throwsClassCastExceptionWhenSourceIsNotLocalDateTime() {
        // GIVEN
        Object notALocalDateTime = "2023-01-01T00:00:00";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMap(notALocalDateTime, mockConverter), "Passing a non‑LocalDateTime object should result in a ClassCastException");
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideSuccessCases")
    void toMap_success(String description, LocalDateTime source, String expectedString) {
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(source, mockConverter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.LOCAL_DATE_TIME), "Result map must contain the key defined in MapConversions.LOCAL_DATE_TIME");
        assertEquals(expectedString, result.get(MapConversions.LOCAL_DATE_TIME), "Stored value must match the expected LocalDateTime string");
    }

    @Test
    void toMap_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> LocalDateTimeConversions.toMap(null, mockConverter), "Casting null to LocalDateTime and calling toString() should throw NPE");
    }

    @Test
    void toMap_invalidType_string_throwsClassCastException() {
        Object notLocalDateTime = "2023-05-17T14:30:45";
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMap(notLocalDateTime, mockConverter), "A String is not a LocalDateTime and should cause ClassCastException");
    }

    @Test
    void toMap_invalidType_localDate_throwsClassCastException() {
        Object notLocalDateTime = LocalDate.of(2023, 5, 17);
        assertThrows(ClassCastException.class, () -> LocalDateTimeConversions.toMap(notLocalDateTime, mockConverter), "A LocalDate (different temporal type) should cause ClassCastException");
    }

    @Test
    void toMap_converterIgnored() {
        // GIVEN a valid LocalDateTime and a Converter implementation that would fail if invoked
        LocalDateTime source = LocalDateTime.of(2023, 5, 17, 14, 30, 45, 123_000_000);
        Converter evilConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new AssertionError("Converter should not be invoked");
            }
        };
        // WHEN
        Map<?, ?> result = LocalDateTimeConversions.toMap(source, evilConverter);
        // THEN
        assertNotNull(result, "Result map must not be null");
        assertEquals(1, result.size(), "Result map must contain exactly one entry");
        assertEquals(source.toString(), result.get(MapConversions.LOCAL_DATE_TIME), "The map value must be the string representation of the supplied LocalDateTime");
    }
}
