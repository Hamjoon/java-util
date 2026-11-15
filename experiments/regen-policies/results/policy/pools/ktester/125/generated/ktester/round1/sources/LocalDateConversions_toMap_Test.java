package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class LocalDateConversions_toMap_Test {

    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // The Converter has a public constructor that requires ConverterOptions.
        // Using the default options is sufficient for this test.
        converter = new Converter(new ConverterOptions());
    }

    /**
     * Supplies invalid inputs for the parameterized test above. **
     */
    private static Stream<Arguments> invalidTypeProvider() {
        return Stream.of(// case: a plain String that is not a LocalDate
        Arguments.of("Providing a String should result in ClassCastException", "not-a-date"), // case: a LocalDateTime instance (any non‑LocalDate type)
        Arguments.of("Providing a LocalDateTime should result in ClassCastException", LocalDateTime.now()));
    }

    @Test
    void toMap_returnsMapWithDateString() {
        // GIVEN
        LocalDate date = LocalDate.of(2023, 3, 15);
        // WHEN
        Map<String, Object> result = LocalDateConversions.toMap(date, converter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey(MapConversions.DATE), "Map should contain the key defined by MapConversions.DATE");
        assertEquals(date.toString(), result.get(MapConversions.DATE), "Value should be the ISO‑8601 string representation of the LocalDate");
    }

    @Test
    void toMap_nullSource_throwsNullPointerException() {
        // GIVEN
        Object nullSource = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> LocalDateConversions.toMap(nullSource, converter), "Calling toMap with a null source should throw NullPointerException");
    }

    /**
     * Invalid type – the method should throw ClassCastException for non‑LocalDate inputs. **
     */
    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("invalidTypeProvider")
    void toMap_invalidSource_throwsClassCastException(String description, Object invalidSource) {
        // GIVEN
        // (converter may be null – the method does not use it)
        Converter nullConverter = null;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> LocalDateConversions.toMap(invalidSource, nullConverter), description);
    }
}
