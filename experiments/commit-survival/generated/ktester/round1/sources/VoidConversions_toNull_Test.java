package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoidConversions_toNull_Test {

    @Mock
    private Converter // Mockito will provide a mock instance
    mockConverter;

    /**
     * A benign converter that simply returns the source object.
     */
    private static class DummyConverter implements Converter {

        @Override
        public Object convert(Object source, Class<?> targetType) {
            // never called by toNull
            return source;
        }
    }

    /**
     * A converter that throws on any conversion attempt.
     */
    private static class FaultyConverter implements Converter {

        @Override
        public Object convert(Object source, Class<?> targetType) {
            throw new RuntimeException("FaultyConverter should never be invoked");
        }
    }

    /**
     * A converter that records whether its {@code convert} method was called.
     * Used to assert that {@code VoidConversions.toNull} does not interact with
     * the supplied converter.
     */
    private static class RecordingConverter implements Converter {

        private boolean called = false;

        @Override
        public Object convert(Object source, Class<?> targetType) {
            called = true;
            return null;
        }

        boolean wasCalled() {
            return called;
        }
    }

    /**
     * Simple POJO used as a source object in one of the scenarios.
     */
    private static class CustomObject {

        private final String value;

        CustomObject(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "CustomObject{" + "value='" + value + '\'' + '}';
        }
    }

    /* -----------------------------------------------------------
     *  Test data provider – mirrors the JSON test cases.
     * ----------------------------------------------------------- */
    private static Stream<Arguments> provideToNullCases() {
        return Stream.of(// ---- group: VoidConversions.toNull behavior ----
        Arguments.of(null, null, "Both arguments are null – the method should still return null without throwing."), Arguments.of("sample string", null, "Non‑null source object with a null Converter – verify the method ignores the inputs and returns null."), Arguments.of(null, new DummyConverter(), "Null source object with a valid Converter instance – method must still return null."), Arguments.of(12345, new DummyConverter(), "Both arguments are non‑null – confirm that the method does not depend on their values and always returns null."), // ---- group: VoidConversions.toNull additional cases ----
        Arguments.of(Boolean.TRUE, new DummyConverter(), "Boolean source object with a valid Converter – method must ignore the value and still return null."), Arguments.of(new int[] { 1, 2, 3 }, new DummyConverter(), "Array source object with a valid Converter – verify that the method does not attempt conversion and returns null."), Arguments.of(new Object(), new DummyConverter(), "Arbitrary object as source with a valid Converter – ensure the method consistently returns null."), Arguments.of(null, new FaultyConverter(), "Null source with a Converter whose methods throw exceptions – toNull must not invoke the converter and must still return null without propagating the exception."), Arguments.of("any string", new FaultyConverter(), "Non‑null source with a Converter that throws on any call – toNull should ignore the converter entirely and return null."), Arguments.of(new CustomObject("test"), new RecordingConverter(), "Custom object source with a Converter that records whether it was called – toNull must not call the converter, confirming no side‑effects."), Arguments.of(123L, (Converter) (src, targetType) -> {
            throw new RuntimeException("Should not be called");
        }, "Long source with a lambda‑based Converter that throws if invoked – toNull must bypass the lambda and return null."));
    }

    /* -----------------------------------------------------------
     *  Helper that checks a converter was never used.
     * ----------------------------------------------------------- */
    private void verifyNoInvocation(Converter converter) {
        if (converter == null) {
            // nothing to verify
            return;
        }
        // Mockito mock – verify zero interactions.
        if (mockingDetails(converter).isMock()) {
            verifyNoInteractions(converter);
            return;
        }
        // RecordingConverter – it records a flag when its method is called.
        if (converter instanceof RecordingConverter) {
            assertFalse(((RecordingConverter) converter).wasCalled(), "RecordingConverter should not have been invoked by toNull()");
            return;
        }
        // For other concrete implementations (DummyConverter, FaultyConverter, lambda)
        // we cannot use Mockito verification, but we can be confident that
        // toNull does not call any method because the test would have failed
        // with an exception if it had.
    }

    @Test
    void toNull_returnsNull_whenSourceIsNull_andConverterIsMocked() {
        Object result = VoidConversions.toNull(null, mockConverter);
        assertNull(result, "toNull should return null when the source object is null");
    }

    @Test
    void toNull_returnsNull_whenSourceIsNonNull_andConverterIsMocked() {
        // any non‑null object
        Object source = new Object();
        Object result = VoidConversions.toNull(source, mockConverter);
        assertNull(result, "toNull should return null regardless of the source object");
    }

    @Test
    void toNull_returnsNull_whenConverterIsNull() {
        Object source = "some string";
        Object result = VoidConversions.toNull(source, null);
        assertNull(result, "toNull should return null even when the converter argument is null");
    }

    /* -----------------------------------------------------------
     *  Parameterised test covering all JSON‑specified cases.
     * ----------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("provideToNullCases")
    void toNull_alwaysReturnsNull_andNeverInvokesConverter(Object from, Converter converter, String description) {
        // Act
        Object result = VoidConversions.toNull(from, converter);
        // Assert – the method must always return null.
        assertNull(result, "toNull should return null for: " + description);
        // Verify that the converter (if any) was never used.
        verifyNoInvocation(converter);
    }
}
