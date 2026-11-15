package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import static org.mockito.Mockito.*;
import com.cedarsoftware.util.convert.ConversionException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2String_Test {

    /**
     * Holds the original singleton instance so we can restore it after each test.
     */
    private static Converter originalInstance;

    /**
     * Mocked Converter that will replace the static singleton during tests.
     */
    @Mock
    private Converter mockConverter;

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original instance created by the library.
        originalInstance = Converter.instance;
    }

    @BeforeEach
    void setupBeforeEach() {
        // By default replace the static singleton with a Mockito mock.
        Converter.instance = mockConverter;
    }

    @AfterEach
    void teardownAfterEach() {
        // Restore the original singleton so other tests (or later runs) are not affected.
        Converter.instance = originalInstance;
    }

    private static Stream<Arguments> provideSuccessfulConversions() {
        return Stream.of(// String input – should be returned unchanged
        Arguments.of("String input unchanged", "Hello World", "Hello World"), // Empty string
        Arguments.of("Empty string unchanged", "", ""), // Integer inputs
        Arguments.of("Integer 42", 42, "42"), Arguments.of("Integer -123", -123, "-123"), Arguments.of("Integer 0", 0, "0"), // Long input
        Arguments.of("Long 1234567890123L", 1234567890123L, "1234567890123"), // Double inputs
        Arguments.of("Double 3.1415", 3.1415, "3.1415"), Arguments.of("Double NaN", Double.NaN, "NaN"), Arguments.of("Double POSITIVE_INFINITY", Double.POSITIVE_INFINITY, "Infinity"), Arguments.of("Double NEGATIVE_INFINITY", Double.NEGATIVE_INFINITY, "-Infinity"), // Boolean inputs
        Arguments.of("Boolean true", true, "true"), Arguments.of("Boolean false", false, "false"), // Character input
        Arguments.of("Character 'Z'", 'Z', "Z"), // BigDecimal input
        Arguments.of("BigDecimal 12345.6789", new BigDecimal("12345.6789"), "12345.6789"), // BigInteger input
        Arguments.of("BigInteger 9876543210", new BigInteger("9876543210"), "9876543210"), // Atomic types
        Arguments.of("AtomicInteger 7", new AtomicInteger(7), "7"), Arguments.of("AtomicLong 123456789L", new AtomicLong(123456789L), "123456789"), Arguments.of("AtomicBoolean true", new AtomicBoolean(true), "true"), // Timestamp input (epoch)
        Arguments.of("Timestamp epoch", new Timestamp(0L), "1970-01-01 00:00:00.0"), // Custom object with overridden toString()
        Arguments.of("Custom object with toString()", new Object() {

            @Override
            public String toString() {
                return "CustomObject";
            }
        }, "CustomObject"), // Plain Object – we only check the prefix because hashcode varies
        Arguments.of("Plain Object default toString()", new Object(), "java.lang.Object@"));
    }

    @Test
    void convert2String_givenNull_returnsEmptyString() {
        // GIVEN
        Object input = null;
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals("", result, "convert2String(null) should return an empty string");
        // No interaction with the mockConverter should have occurred.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void convert2String_givenNonNull_delegatesToInstanceConvert() {
        // GIVEN
        Object input = new Object();
        String expected = "mocked conversion";
        when(mockConverter.convert(input, String.class)).thenReturn(expected);
        // WHEN
        String result = Converter.convert2String(input);
        // THEN
        assertEquals(expected, result, "convert2String should delegate to instance.convert");
        verify(mockConverter, times(1)).convert(input, String.class);
    }

    /* --------------------------------------------------------------
     *  Parameterized tests that exercise the real conversion logic.
     *  For these tests we replace the mock with the original singleton.
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("provideSuccessfulConversions")
    void convert2String_successfulConversions(String description, Object input, String expected) {
        // Use the real converter for real conversion behaviour.
        Converter.instance = originalInstance;
        String result = Converter.convert2String(input);
        assertEquals(expected, result, description);
    }

    @Test
    void convert2String_plainObject_defaultToStringPrefix() {
        // Use the real converter for this test.
        Converter.instance = originalInstance;
        Object input = new Object();
        String result = Converter.convert2String(input);
        assertTrue(result.startsWith("java.lang.Object@"), "Default Object.toString() should start with 'java.lang.Object@' but was: " + result);
    }

    @Test
    void convert2String_unsupportedType_throwsConversionException() {
        // Use the real converter – the library should throw a ConversionException for Thread.
        Converter.instance = originalInstance;
        Thread unsupported = new Thread();
        assertThrows(ConversionException.class, () -> Converter.convert2String(unsupported), "Converting an unsupported type should throw ConversionException");
    }
}
