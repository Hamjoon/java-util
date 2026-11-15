package com.cedarsoftware.util;

import com.cedarsoftware.util.convert.Convert;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_addConversion_2_Test {

    /**
     * Simple POJO used as a custom target type for conversion tests.
     */
    private static final class MyNumber {

        private final int value;

        MyNumber(int value) {
            this.value = value;
        }

        int getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof MyNumber))
                return false;
            MyNumber other = (MyNumber) o;
            return this.value == other.value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }

        @Override
        public String toString() {
            return "MyNumber{" + "value=" + value + '}';
        }
    }

    /**
     * Data provider for {@link #addConversion_firstRegistration_returnsNullPrevious}.
     */
    private static Stream<Arguments> firstRegistrationProvider() {
        Convert<Integer> stringToInt = (src, conv) -> Integer.valueOf((String) src);
        Convert<String> intToString = (src, conv) -> String.valueOf(((Integer) src).intValue());
        Convert<Set<?>> listToSet = (src, conv) -> new HashSet<>((List<?>) src);
        Convert<Thread> runnableToThread = (src, conv) -> new Thread((Runnable) src);
        Convert<String> intPrimitiveToString = (src, conv) -> String.valueOf(((Integer) src).intValue());
        Convert<Object> objectToObject = (src, conv) -> src;
        Convert<String> stringToStringReturningNull = (src, conv) -> null;
        Convert<Integer> integerIdentity = (src, conv) -> (Integer) src;
        return Stream.of(Arguments.of(String.class, Integer.class, stringToInt), Arguments.of(int.class, String.class, intPrimitiveToString), Arguments.of(List.class, Set.class, listToSet), Arguments.of(Runnable.class, Thread.class, runnableToThread), Arguments.of(Object.class, Object.class, objectToObject), Arguments.of(String.class, String.class, stringToStringReturningNull), Arguments.of(Integer.class, Integer.class, integerIdentity));
    }

    /**
     * Verify that a custom conversion can be added and is then reported as supported.
     */
    @Test
    void addConversion_registersAndReportsSupport() {
        // GIVEN – a conversion from String to MyNumber that parses the string as an int
        Convert<MyNumber> stringToMyNumber = (source, conv) -> new MyNumber(Integer.parseInt((String) source));
        // WHEN – the conversion is registered
        Convert<?> previous = Converter.addConversion(String.class, MyNumber.class, stringToMyNumber);
        // THEN – the registration should be reported as supported
        assertTrue(Converter.isConversionSupportedFor(String.class, MyNumber.class), "Custom conversion should be reported as supported");
        // AND – the previous conversion (if any) should be null because no conversion existed before
        assertNull(previous, "There should be no previous conversion for this source/target pair");
    }

    /**
     * Verify that the newly added conversion is actually used by {@link Converter#convert(Object, Class)}.
     */
    @Test
    void convert_usesCustomConversion() {
        // GIVEN – register a conversion from String to MyNumber
        Convert<MyNumber> stringToMyNumber = (source, conv) -> new MyNumber(Integer.parseInt((String) source) + 10);
        Converter.addConversion(String.class, MyNumber.class, stringToMyNumber);
        // WHEN – converting a string
        MyNumber result = Converter.convert("7", MyNumber.class);
        // THEN – the result should reflect the custom conversion logic (7 + 10 = 17)
        assertNotNull(result, "Result of conversion must not be null");
        assertEquals(new MyNumber(17), result, "Custom conversion should add 10 to the parsed integer value");
    }

    /**
     * Verify that passing a {@code null} conversion function results in a {@link NullPointerException}.
     */
    @Test
    void addConversion_nullConversion_throwsException() {
        // GIVEN – a valid source and target class
        Class<?> source = String.class;
        Class<?> target = MyNumber.class;
        // WHEN / THEN – adding a null conversion should throw NPE
        assertThrows(NullPointerException.class, () -> Converter.addConversion(source, target, null), "Adding a null conversion function must throw NullPointerException");
    }

    /**
     * Verify that the first registration for a source/target pair returns {@code null}
     * and that the conversion is reported as supported.
     */
    @ParameterizedTest(name = "[{index}] source={0}, target={1}")
    @MethodSource("firstRegistrationProvider")
    void addConversion_firstRegistration_returnsNullPrevious(Class<?> source, Class<?> target, Convert<?> conversion) {
        // WHEN – register the conversion
        Convert<?> previous = Converter.addConversion(source, target, conversion);
        // THEN – previous must be null and the pair must be reported as supported
        assertNull(previous, "First registration should return null previous conversion");
        assertTrue(Converter.isConversionSupportedFor(source, target), () -> "Conversion should be supported after registration for " + source + " -> " + target);
    }

    @Test
    void addConversion_replaceExisting_returnsPreviousConversion() {
        // first registration
        Convert<Integer> first = (src, conv) -> Integer.valueOf((String) src);
        Convert<Integer> second = (src, conv) -> Integer.valueOf(((String) src).length());
        Convert<?> previous = Converter.addConversion(String.class, Integer.class, first);
        assertNull(previous, "First registration should return null");
        // replace
        Convert<?> returned = Converter.addConversion(String.class, Integer.class, second);
        assertNotNull(returned, "Replacing an existing conversion must return the previous Convert");
        // sanity‑check: the previous conversion should still work as originally defined
        assertEquals(42, ((Convert<Integer>) returned).convert("42", null));
    }

    @Test
    void addConversion_replacePrimitiveSource_returnsPreviousConversion() {
        // first registration (int -> String)
        Convert<String> first = (src, conv) -> String.valueOf(((Integer) src).intValue());
        // replacement (different logic)
        Convert<String> second = (src, conv) -> "num:" + ((Integer) src).intValue();
        Convert<?> previous = Converter.addConversion(int.class, String.class, first);
        assertNull(previous, "First registration should return null");
        Convert<?> returned = Converter.addConversion(int.class, String.class, second);
        assertNotNull(returned, "Replacement should return the first Convert instance");
        assertEquals("123", ((Convert<String>) returned).convert(123, null));
    }

    @Test
    void addConversion_replaceSameWrapperClass_returnsPreviousConversion() {
        Convert<Integer> first = (src, conv) -> (Integer) src;
        Convert<Integer> second = (src, conv) -> ((Integer) src) + 1;
        Convert<?> previous = Converter.addConversion(Integer.class, Integer.class, first);
        assertNull(previous, "First registration should return null");
        Convert<?> returned = Converter.addConversion(Integer.class, Integer.class, second);
        assertNotNull(returned, "Replacing same‑type conversion must return the original Convert");
        assertEquals(5, ((Convert<Integer>) returned).convert(5, null));
    }

    @Test
    void addConversion_nullSource_throwsNPE() {
        Convert<Integer> conversion = (src, conv) -> 0;
        assertThrows(NullPointerException.class, () -> Converter.addConversion(null, Integer.class, conversion), "Null source class must cause NullPointerException");
    }

    @Test
    void addConversion_nullTarget_throwsNPE() {
        Convert<Integer> conversion = (src, conv) -> 0;
        assertThrows(NullPointerException.class, () -> Converter.addConversion(String.class, null, conversion), "Null target class must cause NullPointerException");
    }

    @Test
    void addConversion_conversionThrowsDuringConvert_propagatesException() {
        // conversion that always throws IllegalArgumentException
        Convert<Boolean> throwing = (src, conv) -> {
            throw new IllegalArgumentException("invalid");
        };
        Converter.addConversion(String.class, Boolean.class, throwing);
        // The registration itself must succeed; the exception appears when we invoke convert()
        assertThrows(IllegalArgumentException.class, () -> Converter.convert("anything", Boolean.class), "Exception from conversion function must propagate");
    }

    @Test
    void addConversion_conversionThrowsOnNullInput_propagatesExceptionOnConvert() {
        // conversion that throws IllegalStateException when source is null
        Convert<Integer> nullSensitive = (src, conv) -> {
            if (src == null) {
                throw new IllegalStateException("source is null");
            }
            return 0;
        };
        Converter.addConversion(String.class, Integer.class, nullSensitive);
        // convert with null should trigger the IllegalStateException
        assertThrows(IllegalStateException.class, () -> Converter.convert(null, Integer.class), "Conversion function should throw IllegalStateException for null input");
    }

    @Test
    void addConversion_sameSourceAndTargetAllowed() {
        Convert<Object> identity = (src, conv) -> src;
        Converter.addConversion(Object.class, Object.class, identity);
        assertTrue(Converter.isConversionSupportedFor(Object.class, Object.class), "Conversion with identical source/target classes must be supported");
    }

    @Test
    void addConversion_conversionReturnsNull_isRegistered() {
        Convert<String> returnsNull = (src, conv) -> null;
        Converter.addConversion(String.class, String.class, returnsNull);
        assertTrue(Converter.isConversionSupportedFor(String.class, String.class), "Conversion returning null should still be registered");
        assertNull(Converter.convert("any", String.class), "Calling convert should yield null as defined by the conversion function");
    }
}
