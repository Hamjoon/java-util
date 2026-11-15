package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_convert2AtomicBoolean_Test {

    /**
     * Simple holder for parameterized test arguments.
     */
    private static class ArgumentsHolder {

        final Object input;

        final boolean expected;

        final String description;

        ArgumentsHolder(Object input, boolean expected, String description) {
            this.input = input;
            this.expected = expected;
            this.description = description;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    /**
     * Object whose {@code toString()} throws a RuntimeException.
     */
    private static class ExceptionThrowingToString {

        @Override
        public String toString() {
            throw new RuntimeException("Intentional toString failure");
        }
    }

    /**
     * Number subclass that throws an ArithmeticException from {@code intValue()}.
     */
    private static class ExceptionThrowingNumber extends Number {

        @Override
        public int intValue() {
            throw new ArithmeticException("Intentional intValue failure");
        }

        @Override
        public long longValue() {
            return 0;
        }

        @Override
        public float floatValue() {
            return 0;
        }

        @Override
        public double doubleValue() {
            return 0;
        }
    }

    private static Stream<ArgumentsHolder> stringProvider() {
        return Stream.of(new ArgumentsHolder("TRUE", true, "Upper‑case representation of true"), new ArgumentsHolder("yes", true, "Common truthy word \"yes\""), new ArgumentsHolder("no", false, "Common falsy word \"no\""), new ArgumentsHolder("randomString", false, "Unrecognised string defaults to false"));
    }

    private static Stream<ArgumentsHolder> numericProvider() {
        return Stream.of(new ArgumentsHolder(1, true, "Integer 1 → true"), new ArgumentsHolder(-42, true, "Negative integer → true"), new ArgumentsHolder(0, false, "Zero integer → false"), new ArgumentsHolder(0.0, false, "Zero double → false"), new ArgumentsHolder(3.14, true, "Non‑zero double → true"), new ArgumentsHolder(0.0e0, false, "Scientific‑notation zero → false"));
    }

    @Test
    @DisplayName("convert2AtomicBoolean returns false AtomicBoolean for null input")
    void testNullInput() {
        AtomicBoolean result = Converter.convert2AtomicBoolean(null);
        assertNotNull(result, "Result should never be null");
        assertFalse(result.get(), "AtomicBoolean should be false for null input");
        // Ensure a new instance is created each call
        AtomicBoolean secondResult = Converter.convert2AtomicBoolean(null);
        assertNotSame(result, secondResult, "Each call with null should return a new instance");
    }

    @Test
    @DisplayName("convert2AtomicBoolean correctly converts Boolean true")
    void testBooleanTrue() {
        AtomicBoolean result = Converter.convert2AtomicBoolean(Boolean.TRUE);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source is Boolean.TRUE");
    }

    @Test
    @DisplayName("convert2AtomicBoolean correctly converts Boolean false")
    void testBooleanFalse() {
        AtomicBoolean result = Converter.convert2AtomicBoolean(Boolean.FALSE);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is Boolean.FALSE");
    }

    @Test
    @DisplayName("convert2AtomicBoolean correctly converts String \"true\" (case‑insensitive)")
    void testStringTrue() {
        AtomicBoolean result = Converter.convert2AtomicBoolean("true");
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source is \"true\"");
    }

    @Test
    @DisplayName("convert2AtomicBoolean correctly converts String \"false\" (case‑insensitive)")
    void testStringFalse() {
        AtomicBoolean result = Converter.convert2AtomicBoolean("false");
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is \"false\"");
    }

    @Test
    @DisplayName("convert2AtomicBoolean converts numeric 1 to true")
    void testNumericOne() {
        AtomicBoolean result = Converter.convert2AtomicBoolean(1);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean should be true when source is numeric 1");
    }

    @Test
    @DisplayName("convert2AtomicBoolean converts numeric 0 to false")
    void testNumericZero() {
        AtomicBoolean result = Converter.convert2AtomicBoolean(0);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean should be false when source is numeric 0");
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("stringProvider")
    @DisplayName("convert2AtomicBoolean String variations")
    void testStringVariations(ArgumentsHolder holder) {
        AtomicBoolean result = Converter.convert2AtomicBoolean(holder.input);
        assertNotNull(result);
        assertEquals(holder.expected, result.get(), () -> "String \"" + holder.input + "\" should be interpreted as " + holder.expected);
    }

    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("numericProvider")
    @DisplayName("convert2AtomicBoolean Numeric variations")
    void testNumericVariations(ArgumentsHolder holder) {
        AtomicBoolean result = Converter.convert2AtomicBoolean(holder.input);
        assertNotNull(result);
        assertEquals(holder.expected, result.get(), () -> "Numeric input " + holder.input + " should be interpreted as " + holder.expected);
    }

    @Test
    @DisplayName("convert2AtomicBoolean converts AtomicBoolean instances")
    void testAtomicBooleanInput() {
        AtomicBoolean src = new AtomicBoolean(true);
        AtomicBoolean result = Converter.convert2AtomicBoolean(src);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicBoolean(true) should stay true");
        src = new AtomicBoolean(false);
        result = Converter.convert2AtomicBoolean(src);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicBoolean(false) should stay false");
    }

    @Test
    @DisplayName("convert2AtomicBoolean converts AtomicInteger values")
    void testAtomicIntegerInput() {
        AtomicInteger src = new AtomicInteger(0);
        AtomicBoolean result = Converter.convert2AtomicBoolean(src);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicInteger(0) → false");
        src = new AtomicInteger(5);
        result = Converter.convert2AtomicBoolean(src);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicInteger(5) → true");
    }

    @Test
    @DisplayName("convert2AtomicBoolean converts AtomicLong values")
    void testAtomicLongInput() {
        AtomicLong src = new AtomicLong(0L);
        AtomicBoolean result = Converter.convert2AtomicBoolean(src);
        assertNotNull(result);
        assertFalse(result.get(), "AtomicLong(0L) → false");
        src = new AtomicLong(123L);
        result = Converter.convert2AtomicBoolean(src);
        assertNotNull(result);
        assertTrue(result.get(), "AtomicLong(123L) → true");
    }

    @Test
    @DisplayName("convert2AtomicBoolean empty collection → false")
    void testEmptyCollection() {
        List<Integer> empty = Collections.emptyList();
        AtomicBoolean result = Converter.convert2AtomicBoolean(empty);
        assertNotNull(result);
        assertFalse(result.get(), "Empty collection should be false");
    }

    @Test
    @DisplayName("convert2AtomicBoolean non‑empty collection → true")
    void testNonEmptyCollection() {
        List<Integer> list = Arrays.asList(1, 2, 3);
        AtomicBoolean result = Converter.convert2AtomicBoolean(list);
        assertNotNull(result);
        assertTrue(result.get(), "Non‑empty collection should be true");
    }

    @Test
    @DisplayName("convert2AtomicBoolean with generic Object → false")
    void testUnsupportedObject() {
        Object obj = new Object();
        AtomicBoolean result = Converter.convert2AtomicBoolean(obj);
        assertNotNull(result);
        assertFalse(result.get(), "Unknown object type should default to false");
    }

    @Test
    @DisplayName("convert2AtomicBoolean with LocalDate → false")
    void testUnsupportedLocalDate() {
        LocalDate date = LocalDate.now();
        AtomicBoolean result = Converter.convert2AtomicBoolean(date);
        assertNotNull(result);
        assertFalse(result.get(), "LocalDate should default to false");
    }

    @Test
    @DisplayName("convert2AtomicBoolean propagates RuntimeException from toString()")
    void testExceptionFromToString() {
        ExceptionThrowingToString obj = new ExceptionThrowingToString();
        assertThrows(RuntimeException.class, () -> Converter.convert2AtomicBoolean(obj), "RuntimeException from toString() should be propagated");
    }

    @Test
    @DisplayName("convert2AtomicBoolean propagates ArithmeticException from Number.intValue()")
    void testExceptionFromNumberIntValue() {
        ExceptionThrowingNumber num = new ExceptionThrowingNumber();
        assertThrows(ArithmeticException.class, () -> Converter.convert2AtomicBoolean(num), "ArithmeticException from intValue() should be propagated");
    }

    @Test
    @DisplayName("convert2AtomicBoolean primitive array input → IllegalArgumentException")
    void testArrayInputIllegalArgument() {
        int[] arr = { 1, 2, 3 };
        assertThrows(IllegalArgumentException.class, () -> Converter.convert2AtomicBoolean(arr), "Primitive array should cause IllegalArgumentException");
    }

    @Test
    @DisplayName("convert2AtomicBoolean incompatible object (Thread) → ClassCastException")
    void testThreadInputClassCast() {
        Thread thread = new Thread();
        assertThrows(ClassCastException.class, () -> Converter.convert2AtomicBoolean(thread), "Thread input should cause ClassCastException");
    }

    @Test
    @DisplayName("convert2AtomicBoolean unsupported ZonedDateTime → UnsupportedOperationException")
    void testZonedDateTimeUnsupported() {
        ZonedDateTime zdt = ZonedDateTime.now();
        assertThrows(UnsupportedOperationException.class, () -> Converter.convert2AtomicBoolean(zdt), "ZonedDateTime without a registered converter should throw UnsupportedOperationException");
    }
}
