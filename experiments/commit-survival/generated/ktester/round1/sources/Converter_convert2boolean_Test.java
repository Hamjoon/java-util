package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.Mockito;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.mockito.junit.jupiter.MockitoExtension;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class Converter_convert2boolean_Test {

    /**
     * Holds the original {@code Converter.instance} so we can restore it later.
     */
    private static Converter originalInstance;

    /**
     * Mock that will replace {@code Converter.instance} during the tests.
     */
    @Mock
    private Converter mockInstance;

    /**
     * Save the original singleton before any test runs.
     */
    @BeforeAll
    static void setupBeforeAll() throws Exception {
        // Grab the current singleton instance via reflection (the field is public,
        // but we keep the reflective approach in case its visibility changes).
        Field instanceField = Converter.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        originalInstance = (Converter) instanceField.get(null);
    }

    /**
     * Replace the singleton with a fresh mock before each test.
     */
    @BeforeEach
    void setupBeforeEach() throws Exception {
        // Reset the mock to a clean state.
        Mockito.reset(mockInstance);
        // Inject the mock into the static field.
        Field instanceField = Converter.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, mockInstance);
    }

    /**
     * Restore the original singleton after all tests have finished.
     */
    @AfterAll
    static void teardownAfterAll() throws Exception {
        Field instanceField = Converter.class.getDeclaredField("instance");
        instanceField.setAccessible(true);
        instanceField.set(null, originalInstance);
    }

    /**
     * Supplies all test cases defined in the JSON matrix.
     */
    static Stream<Arguments> provideConvert2booleanCases() {
        return Stream.of(// ------------------------------------------------------------------
        // Null Input – already covered by a dedicated test, omitted here.
        // ------------------------------------------------------------------
        // ---------------------- Non‑Null Input -------------------------
        arguments("Boolean true should be converted to true", Boolean.TRUE, true), arguments("Boolean false should be converted to false", Boolean.FALSE, false), arguments("String \"true\" (case‑insensitive) should be interpreted as true", "true", true), arguments("String \"false\" should be interpreted as false", "false", false), arguments("Numeric 1 should be converted to true", 1, true), arguments("Numeric 0 should be converted to false", 0, false), arguments("Unsupported string \"yes\" should cause exception", "yes", IllegalArgumentException.class), // ----------------------- Numeric Types ------------------------
        arguments("Int 42 -> true", 42, true), arguments("Int -7 -> true", -7, true), arguments("Double 0.0 -> false", 0.0, false), arguments("Double 3.14 -> true", 3.14, true), arguments("BigInteger non‑zero -> true", new BigInteger("12345678901234567890"), true), arguments("BigInteger zero -> false", BigInteger.ZERO, false), arguments("BigDecimal non‑zero -> true", new BigDecimal("123.45"), true), arguments("BigDecimal zero -> false", new BigDecimal("0.00"), false), arguments("Byte 127 -> true", (byte) 127, true), arguments("Short 0 -> false", (short) 0, false), // ---------------------- String Variations ----------------------
        arguments("String \"TRUE\" upper case -> true", "TRUE", true), arguments("String \"TrUe\" mixed case -> true", "TrUe", true), arguments("String \" false \" with whitespace -> false", " false ", false), arguments("Empty string should cause exception", "", IllegalArgumentException.class), arguments("String \"no\" unsupported -> exception", "no", IllegalArgumentException.class), arguments("String \"1\" numeric string -> exception", "1", IllegalArgumentException.class), // -------------------------- Atomic Types -----------------------
        arguments("AtomicBoolean true -> true", new AtomicBoolean(true), true), arguments("AtomicBoolean false -> false", new AtomicBoolean(false), false), // ----------------------- Unsupported Types --------------------
        arguments("Arbitrary Object -> exception", new Object(), IllegalArgumentException.class), arguments("Array int[] -> exception", new int[] { 1, 2, 3 }, IllegalArgumentException.class), arguments("LocalDate now -> exception", LocalDate.now(), IllegalArgumentException.class), arguments("EnumSample.VALUE -> exception", SampleEnum.VALUE, IllegalArgumentException.class), arguments("Character 'T' -> exception", 'T', IllegalArgumentException.class), // -------------------- Whitespace String -----------------------
        arguments("String only whitespace -> exception", "   ", IllegalArgumentException.class), // ------------------- Unrecognized Tokens ----------------------
        arguments("String \"maybe\" -> exception", "maybe", IllegalArgumentException.class), // --------------- Unsupported Atomic Numeric Types -------------
        arguments("AtomicInteger(5) -> exception", new AtomicInteger(5), IllegalArgumentException.class), arguments("AtomicLong(0L) -> exception", new AtomicLong(0L), IllegalArgumentException.class), // ------------------- Unsupported Array Types ------------------
        arguments("int[]{1,2,3} -> exception", new int[] { 1, 2, 3 }, IllegalArgumentException.class), arguments("String[]{\"true\",\"false\"} -> exception", new String[] { "true", "false" }, IllegalArgumentException.class), // ------------------- Unsupported Collection Types -------------
        arguments("List.of(true,false) -> exception", List.of(true, false), IllegalArgumentException.class), arguments("Map.of(\"key\", true) -> exception", Map.of("key", true), IllegalArgumentException.class), // ------------------- Unsupported Date/Time Types ---------------
        arguments("java.util.Date -> exception", new Date(), IllegalArgumentException.class), arguments("LocalDateTime.now() -> exception", LocalDateTime.now(), IllegalArgumentException.class), arguments("Timestamp now -> exception", Timestamp.valueOf(LocalDateTime.now()), IllegalArgumentException.class), arguments("ZonedDateTime.now() -> exception", ZonedDateTime.now(), IllegalArgumentException.class), // ------------------- Unsupported BigNumber Objects ------------
        arguments("new BigInteger(\"123\") object -> exception", new BigInteger("123"), IllegalArgumentException.class), arguments("new BigDecimal(\"0.0\") object -> exception", new BigDecimal("0.0"), IllegalArgumentException.class));
    }

    /**
     * {@code convert2boolean(null)} must return {@code false} without delegating.
     */
    @Test
    void convert2boolean_NullInput_ReturnsFalse() {
        // Act
        boolean result = Converter.convert2boolean(null);
        // Assert
        assertFalse(result, "convert2boolean(null) should return false");
        // Verify that the mock was never called – the method should short‑circuit.
        verifyNoInteractions(mockInstance);
    }

    /**
     * {@code convert2boolean(Object)} should delegate to {@code instance.convert(...)}.
     */
    @Test
    void convert2boolean_NonNullInput_DelegatesToInstanceConvert() {
        // Arrange
        String source = "true";
        // Simulate the underlying conversion returning true.
        when(mockInstance.convert(source, boolean.class)).thenReturn(true);
        // Act
        boolean result = Converter.convert2boolean(source);
        // Assert
        assertTrue(result, "The delegated conversion should return true");
        // Verify that the mock was called exactly once with the expected arguments.
        verify(mockInstance, times(1)).convert(source, boolean.class);
    }

    /**
     * Verify that a false conversion result is propagated correctly.
     */
    @Test
    void convert2boolean_NonNullInput_DelegatesAndReturnsFalse() {
        // Arrange
        Integer source = 0;
        when(mockInstance.convert(source, boolean.class)).thenReturn(false);
        // Act
        boolean result = Converter.convert2boolean(source);
        // Assert
        assertFalse(result, "The delegated conversion should return false");
        verify(mockInstance, times(1)).convert(source, boolean.class);
    }

    @ParameterizedTest(name = "{index}: {0}")
    @MethodSource("provideConvert2booleanCases")
    void convert2boolean_VariousScenarios(String description, Object input, Object expected) {
        // --------------------------------------------------------------
        // 1️⃣  Expected a concrete boolean value → mock returns it.
        // --------------------------------------------------------------
        if (expected instanceof Boolean) {
            when(mockInstance.convert(eq(input), eq(boolean.class))).thenReturn((Boolean) expected);
            boolean result = Converter.convert2boolean(input);
            assertEquals(expected, result, description);
            verify(mockInstance, times(1)).convert(input, boolean.class);
            return;
        }
        // --------------------------------------------------------------
        // 2️⃣  Expected an exception → mock throws it and we assert.
        // --------------------------------------------------------------
        if (expected instanceof Class) {
            @SuppressWarnings("unchecked")
            Class<? extends Throwable> exClass = (Class<? extends Throwable>) expected;
            when(mockInstance.convert(eq(input), eq(boolean.class))).thenThrow(new IllegalArgumentException("Conversion not supported"));
            assertThrows(exClass, () -> Converter.convert2boolean(input), description);
            verify(mockInstance, times(1)).convert(input, boolean.class);
            return;
        }
        fail("Test data error: expected must be Boolean or Exception class");
    }
}
