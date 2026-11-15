package com.cedarsoftware.util.convert;

import java.util.*;
import java.util.stream.Stream;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
// import org.junit.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
// import static org.junit.Assert.*;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;

// @ExtendWith(MockitoExtension.class)
// @ExtendWith(MockitoExtension.class)
class MapConversions_toThrowable_Test {

    private static Converter converter;

    /**
     * Simple {@code Exception} subclass used in the tests.
     */
    public static class CustomException extends Exception {

        public CustomException() {
            super();
        }

        public CustomException(String message) {
            super(message);
        }

        public CustomException(String message, Throwable cause) {
            super(message, cause);
        }

        public CustomException(Throwable cause) {
            super(cause);
        }
    }

    /**
     * RuntimeException subclass with a public int field.
     */
    public static class CustomExceptionWithField extends RuntimeException {

        public int code;

        public CustomExceptionWithField() {
            super();
        }

        public CustomExceptionWithField(String message) {
            super(message);
        }
    }

    /**
     * Exception that only has an int‑arg constructor – used to provoke constructor‑mismatch failures.
     */
    public static class NoStringConstructorException extends Exception {

        public NoStringConstructorException(int i) {
            super("int:" + i);
        }
    }

    /**
     * Exception with a final field – populateFields should fail when trying to set it.
     */
    public static class FinalFieldException extends Exception {

        public final int finalValue = 0;

        public FinalFieldException() {
            super();
        }
    }

    //     @BeforeAll
    static void setupBeforeAll() {
        // Use a real Converter – it does not need any special configuration for these tests.
        converter = new Converter(new ConverterOptions() {
        });
    }

    /* ------------------------------------------------------------
     *  Utility method for concise map creation
     * ------------------------------------------------------------ */
    private static Map<String, Object> mapOf(Object... keyVals) {
        Map<String, Object> m = new HashMap<>();
        for (int i = 0; i < keyVals.length; i += 2) {
            m.put(String.valueOf(keyVals[i]), keyVals[i + 1]);
        }
        return m;
    }

    /* ------------------------------------------------------------
     *  Test data for the class‑selection logic
     * ------------------------------------------------------------ */
    private static List<Object[]> classSelectionData() {
        return Arrays.asList(new Object[][] { // No CLASS entry → target RuntimeException
        // CLASS is a subclass of target → subclass chosen
        { mapOf("message", "simple"), RuntimeException.class, RuntimeException.class, "simple", null }, // CLASS is a superclass of target → target kept
        { mapOf("class", IllegalArgumentException.class.getName(), "message", "bad"), RuntimeException.class, IllegalArgumentException.class, "bad", null }, // CLASS is unrelated (IOException vs RuntimeException) → target kept
        { mapOf("class", Exception.class.getName(), "message", "generic"), RuntimeException.class, RuntimeException.class, "generic", null }, // CLASS equals target → that class is used
        { mapOf("class", java.io.IOException.class.getName(), "message", "io error"), RuntimeException.class, RuntimeException.class, "io error", null }, // Invalid CLASS name → fallback to target
        { mapOf("class", java.io.IOException.class.getName(), "message", "io error"), java.io.IOException.class, java.io.IOException.class, "io error", null }, { mapOf("class", "com.nonexistent.FooBar", "message", "oops"), IllegalArgumentException.class, IllegalArgumentException.class, "oops", null } });
    }

    /* ------------------------------------------------------------
     *  Data provider for class‑selection tests
     * ------------------------------------------------------------ */
    private static Stream<Arguments> classSelectionProvider() {
        return Stream.of(// No CLASS entry → target RuntimeException
        Arguments.of(mapOf("message", "simple"), RuntimeException.class, RuntimeException.class, "simple", null), // CLASS is a subclass of target → subclass chosen
        Arguments.of(mapOf("class", IllegalArgumentException.class.getName(), "message", "bad"), RuntimeException.class, IllegalArgumentException.class, "bad", null), // CLASS is a superclass of target → target kept
        Arguments.of(mapOf("class", Exception.class.getName(), "message", "generic"), RuntimeException.class, RuntimeException.class, "generic", null), // CLASS is unrelated (IOException vs RuntimeException) → target kept
        Arguments.of(mapOf("class", java.io.IOException.class.getName(), "message", "io error"), RuntimeException.class, RuntimeException.class, "io error", null), // CLASS equals target → that class is used
        Arguments.of(mapOf("class", java.io.IOException.class.getName(), "message", "io error"), java.io.IOException.class, java.io.IOException.class, "io error", null), // Invalid CLASS name → fallback to target
        Arguments.of(mapOf("class", "com.nonexistent.FooBar", "message", "oops"), IllegalArgumentException.class, IllegalArgumentException.class, "oops", null));
    }

    @Test
    void toThrowable_usesClassFromMap_andSetsMessageAndCause() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, CustomException.class.getName());
        map.put(MapConversions.MESSAGE, "main message");
        map.put(MapConversions.CAUSE, IllegalArgumentException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "cause message");
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        Assertions.assertNotNull(result, "Result should never be null");
        Assertions.assertTrue(result instanceof CustomException, "Expected a CustomException instance");
        Assertions.assertEquals("main message", result.getMessage(), "Message should be taken from the map");
        Assertions.assertNotNull(result.getCause(), "Cause should have been set");
        Assertions.assertTrue(result.getCause() instanceof IllegalArgumentException, "Cause should be an IllegalArgumentException");
        Assertions.assertEquals("cause message", result.getCause().getMessage(), "Cause message should be taken from the map");
        Assertions.assertEquals(0, result.getStackTrace().length, "Stack trace must be cleared");
    }

    @Test
    void toThrowable_withoutClassKey_usesTargetClass() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MESSAGE, "only message");
        Throwable result = MapConversions.toThrowable(map, converter, IllegalArgumentException.class);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof IllegalArgumentException, "When no class key is present, the target class should be used");
        Assertions.assertEquals("only message", result.getMessage(), "Message should be taken from the map");
        Assertions.assertNull(result.getCause(), "No cause was supplied, so cause must be null");
        Assertions.assertEquals(0, result.getStackTrace().length, "Stack trace must be cleared");
    }

    @Test
    void toThrowable_withDetailMessageKey_usedWhenMessageAbsent() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, CustomException.class.getName());
        map.put(MapConversions.DETAIL_MESSAGE, "detail only");
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result instanceof CustomException);
        Assertions.assertEquals("detail only", result.getMessage(), "When MESSAGE is missing, DETAIL_MESSAGE should be used");
        Assertions.assertNull(result.getCause());
        Assertions.assertEquals(0, result.getStackTrace().length);
    }

    @ParameterizedTest
    @MethodSource("classSelectionProvider")
    void toThrowable_classSelection(Map<String, Object> map, Class<?> target, Class<?> expectedClass, String expectedMessage, Throwable expectedCause) {
        Throwable result = MapConversions.toThrowable(map, converter, target);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(expectedClass, result.getClass(), "Expected exception class based on class selection rules");
        Assertions.assertEquals(expectedMessage, result.getMessage(), "Message should match the one supplied in the map");
        Assertions.assertEquals(expectedCause, result.getCause(), "Cause should match expected (null in these cases)");
        Assertions.assertEquals(0, result.getStackTrace().length, "Stack trace must be cleared");
    }

    @Test
    void toThrowable_causeWithStringThrowableConstructor() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        map.put(MapConversions.MESSAGE, "outer");
        map.put(MapConversions.CAUSE, IllegalArgumentException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "inner");
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        Assertions.assertTrue(result instanceof RuntimeException);
        Assertions.assertEquals("outer", result.getMessage());
        Assertions.assertNotNull(result.getCause());
        Assertions.assertTrue(result.getCause() instanceof IllegalArgumentException);
        Assertions.assertEquals("inner", result.getCause().getMessage());
        Assertions.assertEquals(0, result.getStackTrace().length);
    }

    @Test
    void toThrowable_causeWithoutStringThrowableConstructor() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        map.put(MapConversions.MESSAGE, "outer");
        map.put(MapConversions.CAUSE, NullPointerException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "inner");
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        Assertions.assertTrue(result instanceof RuntimeException);
        Assertions.assertEquals("outer", result.getMessage());
        Assertions.assertNotNull(result.getCause());
        Assertions.assertTrue(result.getCause() instanceof NullPointerException);
        // NullPointerException only has (String) ctor, so cause is attached via initCause
        Assertions.assertEquals("inner", result.getCause().getMessage());
        Assertions.assertEquals(0, result.getStackTrace().length);
    }

    @Test
    void toThrowable_detailMessageUsedWhenMessageAbsent() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, IllegalArgumentException.class.getName());
        map.put(MapConversions.DETAIL_MESSAGE, "detail only");
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        Assertions.assertTrue(result instanceof IllegalArgumentException);
        Assertions.assertEquals("detail only", result.getMessage());
        Assertions.assertNull(result.getCause());
        Assertions.assertEquals(0, result.getStackTrace().length);
    }

    @Test
    void toThrowable_noMessageOrDetailMessageUsesNoArgConstructor() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        Assertions.assertTrue(result instanceof RuntimeException);
        Assertions.assertNull(result.getMessage(), "Message should be null when no keys are supplied");
        Assertions.assertNull(result.getCause());
        Assertions.assertEquals(0, result.getStackTrace().length);
    }

    @Test
    void toThrowable_populatePublicFieldFromMap() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, CustomExceptionWithField.class.getName());
        map.put(MapConversions.MESSAGE, "custom");
        map.put("code", 42);
        Throwable result = MapConversions.toThrowable(map, converter, Exception.class);
        Assertions.assertTrue(result instanceof CustomExceptionWithField);
        CustomExceptionWithField ex = (CustomExceptionWithField) result;
        Assertions.assertEquals(42, ex.code);
        Assertions.assertEquals("custom", ex.getMessage());
        Assertions.assertEquals(0, ex.getStackTrace().length);
    }

    @Test
    void toThrowable_stackTraceIsCleared() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        map.put(MapConversions.MESSAGE, "stackless");
        Throwable result = MapConversions.toThrowable(map, converter, RuntimeException.class);
        Assertions.assertEquals(0, result.getStackTrace().length);
    }

    @Test
    void toThrowable_invalidFromTypeThrowsClassCastException() {
        Assertions.assertThrows(ClassCastException.class, () -> MapConversions.toThrowable("just a string", converter, RuntimeException.class));
    }

    @Test
    void toThrowable_invalidClassValueWrapped() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, 123);
        map.put(MapConversions.MESSAGE, "test");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, RuntimeException.class));
        Assertions.assertTrue(ex.getCause() instanceof ClassCastException);
    }

    @Test
    void toThrowable_invalidCauseValueWrapped() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        map.put(MapConversions.CAUSE, 456);
        map.put(MapConversions.CAUSE_MESSAGE, "inner");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, RuntimeException.class));
        Assertions.assertTrue(ex.getCause() instanceof ClassCastException);
    }

    @Test
    void toThrowable_nullTargetWrapped() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.MESSAGE, "test");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, null));
        Assertions.assertTrue(ex.getCause() instanceof NullPointerException);
    }

    @Test
    void toThrowable_nullConverterWrapped() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        map.put(MapConversions.MESSAGE, "test");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, null, RuntimeException.class));
        Assertions.assertTrue(ex.getCause() instanceof NullPointerException);
    }

    @Test
    void toThrowable_mainExceptionMissingStringConstructorWrapped() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, NoStringConstructorException.class.getName());
        map.put(MapConversions.MESSAGE, "msg");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, Exception.class));
        Assertions.assertNotNull(ex.getCause());
    }

    @Test
    void toThrowable_causeClassMissingStringConstructorWrapped() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, RuntimeException.class.getName());
        map.put(MapConversions.MESSAGE, "outer");
        map.put(MapConversions.CAUSE, NoStringConstructorException.class.getName());
        map.put(MapConversions.CAUSE_MESSAGE, "inner");
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, RuntimeException.class));
        Assertions.assertNotNull(ex.getCause());
    }

    @Test
    void toThrowable_populateFinalFieldWrapped() {
        Map<String, Object> map = new HashMap<>();
        map.put(MapConversions.CLASS, FinalFieldException.class.getName());
        map.put(MapConversions.MESSAGE, "test");
        map.put("finalValue", 99);
        IllegalArgumentException ex = Assertions.assertThrows(IllegalArgumentException.class, () -> MapConversions.toThrowable(map, converter, Exception.class));
        Assertions.assertTrue(ex.getCause() instanceof IllegalAccessException);
    }
}