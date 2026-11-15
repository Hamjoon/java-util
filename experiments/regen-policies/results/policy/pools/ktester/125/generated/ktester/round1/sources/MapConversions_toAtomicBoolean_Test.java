package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.util.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
class MapConversions_toAtomicBoolean_Test {

    /**
     * Converter used for all test cases.
     */
    private Converter converter;

    /**
     * Simple domain class used for custom conversion tests.
     */
    static class MyCustomFlag {

        private final String flag;

        MyCustomFlag(String flag) {
            this.flag = flag;
        }

        String getFlag() {
            return flag;
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Use default options – they contain the built‑in conversions required for the tests.
        ConverterOptions options = new ConverterOptions();
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Release the reference; helps the GC when many tests run.
        converter = null;
    }

    private static Stream<Arguments> directScalarProvider() {
        return Stream.of(Arguments.of("AtomicBoolean true should be returned as true", new AtomicBoolean(true), new AtomicBoolean(true)), Arguments.of("AtomicBoolean false should be returned as false", new AtomicBoolean(false), new AtomicBoolean(false)), Arguments.of("Boolean.TRUE → true", Boolean.TRUE, new AtomicBoolean(true)), Arguments.of("Boolean.FALSE → false", Boolean.FALSE, new AtomicBoolean(false)), Arguments.of("String \"true\" (case‑insensitive) → true", "true", new AtomicBoolean(true)), Arguments.of("String \"FALSE\" (case‑insensitive) → false", "FALSE", new AtomicBoolean(false)), Arguments.of("Number 1 → true", 1, new AtomicBoolean(true)), Arguments.of("Number 0 → false", 0, new AtomicBoolean(false)));
    }

    private static Stream<Arguments> mapBasedProvider() {
        return Stream.of(Arguments.of("Map with key \"value\" and Boolean true → true", Map.of("value", true), new AtomicBoolean(true)), Arguments.of("Map with key \"_v\" and Boolean false → false", Map.of("_v", false), new AtomicBoolean(false)), Arguments.of("Map with key \"value\" as case‑insensitive string \"TRUE\" → true", new HashMap<String, Object>() {

            {
                put("value", "TRUE");
                put("extra", 123);
            }
        }, new AtomicBoolean(true)), Arguments.of("Map with key \"value\" and numeric zero → false", Map.of("value", 0), new AtomicBoolean(false)), Arguments.of("Map with key \"value\" and non‑zero numeric → true", Map.of("value", 42), new AtomicBoolean(true)), Arguments.of("Map without recognised keys should yield null", Map.of("unknown", true), null));
    }

    private static Stream<Arguments> unsupportedScalarProvider() {
        return Stream.of(Arguments.of("String not representing a boolean literal → exception", "notABoolean"), Arguments.of("Arbitrary object → exception", new Object()));
    }

    private static Stream<Arguments> mapValueExceptionProvider() {
        return Stream.of(Arguments.of("Map with key \"value\" holding a List → exception", Map.of("value", Arrays.asList(true, false))), Arguments.of("Map with key \"_v\" holding null → exception", Map.of("_v", null)), Arguments.of("Source object is a List (not a map) → exception", // The method expects an Object; we pass a List directly.
        // The provider wraps it in a map to keep the signature uniform.
        // The test will cast the argument to Object.
        (Map<String, Object>) (Object) List.of(true)));
    }

    @Test
    void toAtomicBoolean_WithVKeyBooleanValue_ReturnsTrue() {
        // given
        Map<String, Object> map = new HashMap<>();
        // MapConversions.V == "_v"
        map.put("_v", true);
        // when
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.get(), "AtomicBoolean should contain true");
    }

    @Test
    void toAtomicBoolean_WithValueKeyStringTrue_ReturnsTrue() {
        // given
        Map<String, Object> map = new HashMap<>();
        // many conversions fall back to the "value" key
        map.put("value", "true");
        // when
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertTrue(result.get(), "AtomicBoolean should contain true");
    }

    @Test
    void toAtomicBoolean_WithValueKeyStringFalse_ReturnsFalse() {
        // given
        Map<String, Object> map = new HashMap<>();
        map.put("value", "false");
        // when
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, converter);
        // then
        assertNotNull(result, "Result should not be null");
        assertFalse(result.get(), "AtomicBoolean should contain false");
    }

    @Test
    void toAtomicBoolean_WithNullSource_ReturnsNull() {
        // when
        AtomicBoolean result = MapConversions.toAtomicBoolean(null, converter);
        // then
        assertNull(result, "Converting a null source should yield null");
    }

    /* --------------------------------------------------------------
     *  Direct scalar conversions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("directScalarProvider")
    void toAtomicBoolean_DirectScalarConversions(String description, Object from, AtomicBoolean expected) {
        // when
        AtomicBoolean result = MapConversions.toAtomicBoolean(from, converter);
        // then
        assertNotNull(result, description);
        assertEquals(expected.get(), result.get(), description);
    }

    /* --------------------------------------------------------------
     *  Map‑based conversions
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapBasedProvider")
    void toAtomicBoolean_MapBasedConversions(String description, Map<String, Object> map, AtomicBoolean expected) {
        // when
        AtomicBoolean result = MapConversions.toAtomicBoolean(map, converter);
        // then
        assertNotNull(result, description);
        assertEquals(expected.get(), result.get(), description);
    }

    /* --------------------------------------------------------------
     *  Null and empty inputs
     * -------------------------------------------------------------- */
    @Test
    void toAtomicBoolean_NullSource_ReturnsNull() {
        AtomicBoolean result = MapConversions.toAtomicBoolean(null, converter);
        assertNull(result, "A null source should result in null");
    }

    @Test
    void toAtomicBoolean_EmptyMap_ReturnsNull() {
        AtomicBoolean result = MapConversions.toAtomicBoolean(Collections.emptyMap(), converter);
        assertNull(result, "An empty map should result in null");
    }

    /* --------------------------------------------------------------
     *  Unsupported scalar types – should throw IllegalArgumentException
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("unsupportedScalarProvider")
    void toAtomicBoolean_UnsupportedScalars_ThrowException(String description, Object from) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicBoolean(from, converter), description);
    }

    /* --------------------------------------------------------------
     *  Map value type exceptions – unsupported value types inside a map
     * -------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource("mapValueExceptionProvider")
    void toAtomicBoolean_MapValueTypeExceptions_ThrowException(String description, Map<String, Object> map) {
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicBoolean(map, converter), description);
    }

    @Test
    void toAtomicBoolean_CustomConverter_TrueFlag() {
        // Arrange – a mock Converter that knows how to turn MyCustomFlag into Boolean
        Converter customConverter = Mockito.mock(Converter.class);
        MyCustomFlag source = new MyCustomFlag("Y");
        Mockito.when(customConverter.convert(eq(source), eq(Boolean.class))).thenReturn(Boolean.TRUE);
        // Act
        AtomicBoolean result = MapConversions.toAtomicBoolean(source, customConverter);
        // Assert
        assertNotNull(result, "Result should not be null for custom conversion");
        assertTrue(result.get(), "Custom flag \"Y\" should be converted to true");
    }

    @Test
    void toAtomicBoolean_CustomConverter_FalseFlag() {
        // Arrange – a mock Converter that knows how to turn MyCustomFlag into Boolean
        Converter customConverter = Mockito.mock(Converter.class);
        MyCustomFlag source = new MyCustomFlag("N");
        Mockito.when(customConverter.convert(eq(source), eq(Boolean.class))).thenReturn(Boolean.FALSE);
        // Act
        AtomicBoolean result = MapConversions.toAtomicBoolean(source, customConverter);
        // Assert
        assertNotNull(result, "Result should not be null for custom conversion");
        assertFalse(result.get(), "Custom flag \"N\" should be converted to false");
    }

    /* --------------------------------------------------------------
     *  Helper to keep the map‑value‑exception test signature clean.
     * -------------------------------------------------------------- */
    @TestFactory
    Stream<DynamicTest> mapValueExceptionDynamicTests() {
        // The provider above includes a special case where the source object is a List,
        // not a map. We need to handle that separately because the generic type is Object.
        return Stream.of(DynamicTest.dynamicTest("Map with key \"value\" holding a List → exception", () -> assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicBoolean(Map.of("value", Arrays.asList(true, false)), converter))), DynamicTest.dynamicTest("Map with key \"_v\" holding null → exception", () -> assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicBoolean(Map.of("_v", null), converter))), DynamicTest.dynamicTest("Source object is a List → exception", () -> assertThrows(IllegalArgumentException.class, () -> MapConversions.toAtomicBoolean(List.of(true), converter))));
    }
}
