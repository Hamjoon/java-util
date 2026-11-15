package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toNull_Test {

    /**
     * Mocked {@link Converter} that will be passed to the method under test.
     */
    @Mock
    private Converter mockConverter;

    /* --------------------------------------------------------------
     *  Simple POJO used in the custom‑object test cases
     * -------------------------------------------------------------- */
    private static final class MyPojo {

        private final String name;

        private final int value;

        MyPojo(String name, int value) {
            this.name = name;
            this.value = value;
        }
        // getters, equals, hashCode omitted – not required for the tests.
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not interacted with during the test.
        // This asserts that {@code VoidConversions.toNull} does not delegate to the converter.
        verifyNoInteractions(mockConverter);
    }

    static Stream<Arguments> basicBehaviorProvider() {
        // Dummy converter – a simple mock with no behaviour.
        Converter dummy = mock(Converter.class);
        return Stream.of(Arguments.of(null, null, "Both arguments are null; the method must still return null."), Arguments.of(new Object(), null, "Non‑null source object with a null Converter; the method should ignore the inputs and return null."), Arguments.of("sample string", dummy, "Both arguments are non‑null (a String and a dummy Converter); the method must still return null."));
    }

    static Stream<Arguments> nullSourceWithNonNullConverterProvider() {
        Converter dummy = mock(Converter.class);
        return Stream.of(Arguments.of(null, dummy, "Source object is null while a valid Converter instance is supplied; method must still return null and must not invoke the converter."));
    }

    static Stream<Arguments> primitiveWrapperSourceProvider() {
        Converter dummy = mock(Converter.class);
        return Stream.of(Arguments.of(Integer.valueOf(42), null, "Source is a boxed primitive (Integer) and converter is null; method must ignore the value and return null."), Arguments.of(Boolean.TRUE, dummy, "Source is a Boolean wrapper and a non‑null converter is supplied; method must still return null."));
    }

    static Stream<Arguments> arraySourceProvider() {
        Converter dummy = mock(Converter.class);
        return Stream.of(Arguments.of(new int[] { 1, 2, 3 }, null, "Source is an int array with a null converter; method must return null."), Arguments.of(new String[] { "a", "b" }, dummy, "Source is a String array and a dummy converter is supplied; method must still return null."));
    }

    static Stream<Arguments> collectionSourceProvider() {
        Converter dummy = mock(Converter.class);
        return Stream.of(Arguments.of(java.util.Collections.emptyList(), null, "Source is an empty List and converter is null; method must return null."), Arguments.of(java.util.Arrays.asList(1, 2, 3), dummy, "Source is a populated List with a non‑null converter; method must still return null."));
    }

    static Stream<Arguments> customObjectSourceProvider() {
        Converter dummy = mock(Converter.class);
        return Stream.of(Arguments.of(new MyPojo("test", 123), null, "Source is a user‑defined POJO and converter is null; method must return null."), Arguments.of(new MyPojo("test", 123), dummy, "Source is a POJO with a dummy converter; method must still return null."));
    }

    static Stream<Arguments> converterWithSideEffectProvider() {
        // Throwing converter – will throw if its convert method is ever invoked.
        Converter throwing = mock(Converter.class);
        doThrow(new RuntimeException("Converter should not be called")).when(throwing).convert(any(), any());
        // Counting converter – we only care that it is never invoked.
        Converter counting = mock(Converter.class);
        return Stream.of(Arguments.of("any", throwing, "Converter throws an exception if its convert method is called; toNull must not call the converter and must simply return null."), Arguments.of(new Object(), counting, "Converter records how many times it is invoked; after calling toNull the count must remain zero, confirming that the method does not interact with the converter."));
    }

    @Test
    void toNull_returnsNull_whenFromIsNull() {
        Object result = VoidConversions.toNull(null, mockConverter);
        assertNull(result, "toNull should return null when the source object is null");
    }

    @Test
    void toNull_returnsNull_whenFromIsNonNull() {
        String someObject = "any value";
        Object result = VoidConversions.toNull(someObject, mockConverter);
        assertNull(result, "toNull should always return null regardless of the source object");
    }

    @Test
    void toNull_ignoresConverterBehaviour() {
        // Configure the mock to throw if it is ever called – the method must not invoke it.
        doThrow(new AssertionError("Converter should not be called")).when(mockConverter).convert(any(), any());
        // Call the method with arbitrary arguments.
        Object result = VoidConversions.toNull(new Object(), mockConverter);
        assertNull(result, "toNull must still return null even if the converter would fail");
        // No need for additional verification; {@code teardownAfterEach} already checks for interactions.
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("basicBehaviorProvider")
    void toNull_basicBehavior(Object from, Converter converter, String description) {
        Object result = VoidConversions.toNull(from, converter);
        assertNull(result, "toNull must always return null");
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("nullSourceWithNonNullConverterProvider")
    void toNull_nullSourceWithNonNullConverter(Object from, Converter converter, String description) {
        Object result = VoidConversions.toNull(from, converter);
        assertNull(result);
        verifyNoInteractions(converter);
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("primitiveWrapperSourceProvider")
    void toNull_primitiveWrapperSource(Object from, Converter converter, String description) {
        Object result = VoidConversions.toNull(from, converter);
        assertNull(result);
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("arraySourceProvider")
    void toNull_arraySource(Object from, Converter converter, String description) {
        Object result = VoidConversions.toNull(from, converter);
        assertNull(result);
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("collectionSourceProvider")
    void toNull_collectionSource(Object from, Converter converter, String description) {
        Object result = VoidConversions.toNull(from, converter);
        assertNull(result);
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("customObjectSourceProvider")
    void toNull_customObjectSource(Object from, Converter converter, String description) {
        Object result = VoidConversions.toNull(from, converter);
        assertNull(result);
        if (converter != null) {
            verifyNoInteractions(converter);
        }
    }

    @ParameterizedTest(name = "{2}")
    @MethodSource("converterWithSideEffectProvider")
    void toNull_converterWithSideEffect(Object from, Converter converter, String description) {
        Object result = VoidConversions.toNull(from, converter);
        assertNull(result);
        // The converter must not have been called – verify zero interactions.
        verifyNoInteractions(converter);
    }
}
