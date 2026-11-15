package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.convert.Convert;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class Converter_addConversion_2_Test {

    /**
     * A conversion function that turns a {@link String} into a {@link CustomNumber}.
     */
    private static final Convert<CustomNumber> STRING_TO_CUSTOM = (from, conv) -> {
        // The source is guaranteed to be a String because we register it for that source type.
        String s = (String) from;
        return new CustomNumber(Integer.parseInt(s));
    };

    /* --------------------------------------------------------------------- */
    static class SourceA {
    }

    static class TargetA {
    }

    static class SourceB {
    }

    static class TargetB {
    }

    static class SourceC {
    }

    static class TargetC {
    }

    static class SourceD {
    }

    static class TargetD {
    }

    /* --------------------------------------------------------------------- */
    static class CustomNumber {

        final int value;

        CustomNumber(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            return (o instanceof CustomNumber) && ((CustomNumber) o).value == value;
        }

        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state before each test – the static Converter instance holds
        // all registered conversions, so we add our custom conversion here.
        Converter.addConversion(String.class, CustomNumber.class, STRING_TO_CUSTOM);
    }

    private static Stream<Arguments> invalidArgumentProvider() {
        return Stream.of(// both null
        Arguments.of(null, null, null, NullPointerException.class, "Both source and target classes are null; should throw NPE"), // source null
        Arguments.of(null, String.class, (Convert<?>) (o, c) -> o.toString(), NullPointerException.class, "Source class is null; should throw NPE"), // target null
        Arguments.of(Integer.class, null, (Convert<?>) (o, c) -> o.toString(), NullPointerException.class, "Target class is null; should throw NPE"), // conversion function null
        Arguments.of(Integer.class, String.class, null, NullPointerException.class, "Conversion function is null; should throw NPE"), // identity conversion (source == target)
        Arguments.of(String.class, String.class, (Convert<?>) (o, c) -> o, IllegalArgumentException.class, "Source and target are identical; should throw IllegalArgumentException"));
    }

    private static Stream<Arguments> successfulAdditionProvider() {
        return Stream.of(Arguments.of(Integer.class, String.class, (Convert<String>) (o, c) -> "int:" + o, "Register Integer→String conversion"), Arguments.of(Double.class, Long.class, (Convert<Long>) (o, c) -> ((Double) o).longValue(), "Register Double→Long conversion"));
    }

    @Test
    void addConversion_registersAndIsUsableViaConvert() {
        // given – a string that can be parsed to an integer
        String source = "42";
        // when – we convert using the static API
        CustomNumber result = Converter.convert(source, CustomNumber.class);
        // then – the custom conversion logic was applied
        assertNotNull(result, "Result should not be null");
        assertEquals(new CustomNumber(42), result, "Custom conversion should produce the expected value");
    }

    @Test
    void addConversion_isReportedAsSupported() {
        // given – source and target classes that we just registered
        Class<?> source = String.class;
        Class<?> target = CustomNumber.class;
        // when – we query the supported‑conversion map
        Map<String, Set<String>> supported = Converter.getSupportedConversions();
        // then – the map should contain an entry for the source class pointing to the target class
        assertTrue(supported.containsKey(source.getName()), "Supported conversions should contain the source class name");
        assertTrue(supported.get(source.getName()).contains(target.getName()), "Supported conversions should list the target class name for the source");
        // also check the convenience method
        assertTrue(Converter.isConversionSupportedFor(source, target), "isConversionSupportedFor should return true for the newly added conversion");
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("invalidArgumentProvider")
    void addConversion_invalidArguments(Class<?> source, Class<?> target, Convert<?> conversion, Class<? extends Throwable> expectedException, String description) {
        assertThrows(expectedException, () -> Converter.addConversion(source, target, conversion), description);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {4}")
    @MethodSource("successfulAdditionProvider")
    void addConversion_successfulAddition(Class<?> source, Class<?> target, Convert<?> conversion, String description) {
        // Ensure there is no prior registration for this pair
        // (using unique helper classes guarantees isolation).
        Convert<?> previous = Converter.addConversion(source, target, conversion);
        assertNull(previous, description + " – expected no previous conversion");
        // Verify that the conversion is now reported as supported.
        assertTrue(Converter.isConversionSupportedFor(source, target), description + " – conversion should be reported as supported");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_overwriteExisting_ReturnsPrevious() {
        // First registration
        Convert<String> first = (from, conv) -> "first:" + from;
        Convert<?> previous1 = Converter.addConversion(SourceA.class, TargetA.class, first);
        assertNull(previous1, "First registration should return null");
        // Second registration – should return the first instance
        Convert<String> second = (from, conv) -> "second:" + from;
        Convert<?> previous2 = Converter.addConversion(SourceA.class, TargetA.class, second);
        assertNotNull(previous2, "Overwriting should return the previous conversion");
        assertSame(first, previous2, "Returned conversion should be the original one");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_supportVerificationAfterAddition() {
        Convert<Boolean> conversion = (from, conv) -> ((Float) from) > 0;
        Converter.addConversion(Float.class, Boolean.class, conversion);
        assertTrue(Converter.isConversionSupportedFor(Float.class, Boolean.class), "Conversion should be reported as supported after registration");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_primitiveSource_isAccepted() {
        Convert<String> intToString = (from, conv) -> Integer.toString((int) from);
        Convert<?> previous = Converter.addConversion(int.class, String.class, intToString);
        assertNull(previous, "Primitive source registration should return null");
        assertTrue(Converter.isConversionSupportedFor(int.class, String.class), "Primitive source conversion should be reported as supported");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_interfaceSource_isAccepted() {
        Convert<Integer> csToInt = (from, conv) -> ((CharSequence) from).length();
        Convert<?> previous = Converter.addConversion(CharSequence.class, Integer.class, csToInt);
        assertNull(previous, "Interface source registration should return null");
        assertTrue(Converter.isConversionSupportedFor(CharSequence.class, Integer.class), "Interface source conversion should be reported as supported");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_arraySource_isAccepted() {
        Convert<List<String>> arrayToList = (from, conv) -> Arrays.asList((String[]) from);
        Convert<?> previous = Converter.addConversion(String[].class, List.class, arrayToList);
        assertNull(previous, "Array source registration should return null");
        assertTrue(Converter.isConversionSupportedFor(String[].class, List.class), "Array source conversion should be reported as supported");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_functionThrows_isStoredWithoutExecution() {
        Convert<String> throwing = (from, conv) -> {
            throw new RuntimeException("forced");
        };
        // The registration itself must not trigger the exception.
        assertDoesNotThrow(() -> Converter.addConversion(Long.class, String.class, throwing), "addConversion should store the lambda without invoking it");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_statefulFunction_isAccepted() {
        AtomicInteger counter = new AtomicInteger(0);
        Convert<Integer> stateful = (from, conv) -> counter.incrementAndGet();
        Convert<?> previous = Converter.addConversion(String.class, Integer.class, stateful);
        assertNull(previous, "Stateful conversion registration should return null");
        // Verify that the conversion works as expected when used.
        Integer result = Converter.convert("any", Integer.class);
        assertEquals(1, result, "First conversion should return 1");
        Integer second = Converter.convert("any", Integer.class);
        assertEquals(2, second, "Second conversion should return 2");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_primitiveTarget_isRejected() {
        Convert<Integer> conversion = (from, conv) -> Integer.parseInt((String) from);
        assertThrows(IllegalArgumentException.class, () -> Converter.addConversion(String.class, int.class, conversion), "Primitive target types are not allowed and should throw IllegalArgumentException");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_primitiveToPrimitive_isRejected() {
        Convert<Long> conversion = (from, conv) -> ((Number) from).longValue();
        assertThrows(IllegalArgumentException.class, () -> Converter.addConversion(int.class, long.class, conversion), "Primitive-to-primitive conversions should be rejected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_invalidSourceVoid_isRejected() {
        Convert<String> conversion = (from, conv) -> "void";
        assertThrows(IllegalArgumentException.class, () -> Converter.addConversion(void.class, String.class, conversion), "void.class as source should be rejected");
    }

    /* --------------------------------------------------------------------- */
    @Test
    void addConversion_primitiveArraySource_isRejected() {
        Convert<List<Integer>> conversion = (from, conv) -> {
            int[] arr = (int[]) from;
            List<Integer> list = new ArrayList<>();
            for (int i : arr) list.add(i);
            return list;
        };
        assertThrows(IllegalArgumentException.class, () -> Converter.addConversion(int[].class, List.class, conversion), "Primitive array sources should be rejected");
    }
}
