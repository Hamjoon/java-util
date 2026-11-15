package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.DayOfWeek;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class EnumConversions_toMap_Test {

    /**
     * Mocked {@link Converter} – the method under test does not use it, but we verify that it stays untouched.
     */
    @Mock
    private Converter mockConverter;

    /**
     * A dummy {@link Converter} implementation – its {@code convert} method is never called.
     */
    private static class DummyConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> tgt) {
            return null;
        }
    }

    /**
     * A {@link Converter} that throws if its {@code convert} method is invoked – used to ensure the method truly ignores the converter.
     */
    private static class ThrowingConverter implements Converter {

        @Override
        public Object convert(Object src, Class<?> tgt) {
            throw new RuntimeException("convert should not be called");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // MockitoExtension already creates the mock for us.
        // Ensure the mock has no unexpected interactions before each test.
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the method under test never interacts with the Converter.
        verifyNoInteractions(mockConverter);
    }

    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// from,                converter,               expected name
        Arguments.of(Color.RED, null, "RED"), Arguments.of(Color.GREEN, new DummyConverter(), "GREEN"), Arguments.of(Color.BLUE, new ThrowingConverter(), "BLUE"), Arguments.of(DayOfWeek.MONDAY, null, "MONDAY"), Arguments.of(SampleEnum.FIRST, mockConverter, "FIRST"));
    }

    private static Stream<Arguments> exceptionProvider() {
        return Stream.of(// from,                converter,               expected exception
        Arguments.of(null, null, NullPointerException.class), Arguments.of("not-an-enum", null, ClassCastException.class));
    }

    @Test
    void toMap_returnsMapContainingEnumName() {
        // GIVEN
        SampleEnum enumValue = SampleEnum.FIRST;
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumValue, mockConverter);
        // THEN
        assertNotNull(result, "Resulting map should not be null");
        assertEquals(1, result.size(), "Resulting map should contain exactly one entry");
        assertTrue(result.containsKey("name"), "Resulting map should contain the key 'name'");
        assertEquals(enumValue.name(), result.get("name"), "The 'name' entry should match the enum's name");
    }

    @Test
    void toMap_nullEnum_throwsNullPointerException() {
        // GIVEN
        Object nullEnum = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> EnumConversions.toMap(nullEnum, mockConverter), "Passing a null enum should result in NullPointerException");
    }

    @ParameterizedTest(name = "[{index}] toMap({0}, {1}) → name={2}")
    @MethodSource("successfulConversionProvider")
    void toMap_successfulConversions(Object from, Converter converter, String expectedName) {
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(from, converter);
        // THEN
        assertNotNull(result, "Resulting map must not be null");
        assertEquals(1, result.size(), "Map should contain exactly one entry");
        assertTrue(result.containsKey("name"), "Map must contain the key 'name'");
        assertEquals(expectedName, result.get("name"), "The value for key 'name' must match the enum constant's name");
    }

    @ParameterizedTest(name = "[{index}] toMap({0}, {1}) throws {2}")
    @MethodSource("exceptionProvider")
    void toMap_throwsExpectedExceptions(Object from, Converter converter, Class<? extends Throwable> expectedException) {
        // WHEN / THEN
        assertThrows(expectedException, () -> EnumConversions.toMap(from, converter), () -> "Expected " + expectedException.getSimpleName() + " when converting " + from);
    }

    @Test
    void toMap_returnedMapIsMutable() {
        // GIVEN
        Color enumValue = Color.GREEN;
        Converter converter = new DummyConverter();
        // WHEN
        Map<String, Object> result = EnumConversions.toMap(enumValue, converter);
        // THEN – basic assertions (same as in successful test)
        assertNotNull(result);
        assertEquals("GREEN", result.get("name"));
        // AND – mutability check
        result.put("extra", 42);
        assertEquals(2, result.size(), "Map should be mutable and allow additional entries");
        assertEquals(42, result.get("extra"));
    }
}
