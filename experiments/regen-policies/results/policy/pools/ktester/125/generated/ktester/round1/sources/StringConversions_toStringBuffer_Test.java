package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class StringConversions_toStringBuffer_Test {

    /**
     * Mocked Converter – the method under test does **not** use it, but we verify that.
     */
    @Mock
    private Converter mockConverter;

    class Custom {

        @Override
        public String toString() {
            return "custom-value";
        }
    }

    class NullStringObject {

        @Override
        public String toString() {
            return null;
        }
    }

    class ExceptionObject {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    class ErrorObject {

        @Override
        public String toString() {
            throw new AssertionError("assertion failure");
        }
    }

    class OOMObject {

        @Override
        public String toString() {
            throw new OutOfMemoryError("simulated OOM");
        }
    }

    class NullStringObject {

        @Override
        public String toString() {
            return null;
        }
    }

    class ExceptionObject {

        @Override
        public String toString() {
            throw new IllegalStateException("boom");
        }
    }

    class ErrorObject {

        @Override
        public String toString() {
            throw new AssertionError("assertion failure");
        }
    }

    class OOMObject {

        @Override
        public String toString() {
            throw new OutOfMemoryError("simulated OOM");
        }
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure the mock is clean before each test.
        reset(mockConverter);
    }

    private static Stream<Arguments> primitiveWrapperProvider() {
        return Stream.of(// Integer
        Arguments.of(123, "123"), // Boolean
        Arguments.of(true, "true"), // String (already covered but kept for completeness)
        Arguments.of("hello world", "hello world"));
    }

    @Test
    void toStringBuffer_returnsNullWhenInputIsNull() {
        // GIVEN
        Object input = null;
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNull(result, "Expected null when the source object is null");
        // The converter must not be interacted with.
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_wrapsStringRepresentation() {
        // GIVEN
        Object input = "hello world";
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result, "Result should not be null for non‑null input");
        assertEquals("hello world", result.toString(), "StringBuffer should contain the object's toString() value");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_usesObjectToStringForCustomType() {
        // GIVEN
        class Custom {

            @Override
            public String toString() {
                return "custom-value";
            }
        }
        Object input = new Custom();
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals("custom-value", result.toString(), "StringBuffer must reflect the custom object's toString() output");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_doesNotCallConverterMethods() {
        // GIVEN
        // any non‑null value
        Object input = 12345;
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, mockConverter);
        // THEN
        assertEquals("12345", result.toString());
        // Explicit verification that the converter was never used.
        verifyNoInteractions(mockConverter);
    }

    @ParameterizedTest(name = "[{index}] from={0} → expected=\"{1}\"")
    @MethodSource("primitiveWrapperProvider")
    void toStringBuffer_handlesPrimitiveWrappers(Object from, String expected) {
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(from, mockConverter);
        // THEN
        assertNotNull(result);
        assertEquals(expected, result.toString(), "StringBuffer should contain the textual representation of the input");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_converterArgumentIgnored() {
        // GIVEN
        Object input = 456;
        // a non‑null converter
        Converter dummy = mock(Converter.class);
        // WHEN
        StringBuffer result = StringConversions.toStringBuffer(input, dummy);
        // THEN
        assertEquals("456", result.toString());
        // The dummy converter must not have been touched.
        verifyNoInteractions(dummy);
    }

    @Test
    void toStringBuffer_toStringReturnsNull_throwsNullPointerException() {
        // GIVEN
        class NullStringObject {

            @Override
            public String toString() {
                return null;
            }
        }
        Object input = new NullStringObject();
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> StringConversions.toStringBuffer(input, mockConverter), "StringBuffer constructor should throw NPE when toString() returns null");
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_toStringThrowsRuntimeException_propagates() {
        // GIVEN
        class ExceptionObject {

            @Override
            public String toString() {
                throw new IllegalStateException("boom");
            }
        }
        Object input = new ExceptionObject();
        // WHEN / THEN
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> StringConversions.toStringBuffer(input, mockConverter), "Runtime exception from toString() must propagate");
        assertEquals("boom", ex.getMessage());
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_toStringThrowsError_propagates() {
        // GIVEN
        class ErrorObject {

            @Override
            public String toString() {
                throw new AssertionError("assertion failure");
            }
        }
        Object input = new ErrorObject();
        // WHEN / THEN
        AssertionError err = assertThrows(AssertionError.class, () -> StringConversions.toStringBuffer(input, mockConverter), "Error from toString() must propagate unchanged");
        assertEquals("assertion failure", err.getMessage());
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toStringBuffer_toStringThrowsOutOfMemoryError_propagates() {
        // GIVEN
        class OOMObject {

            @Override
            public String toString() {
                throw new OutOfMemoryError("simulated OOM");
            }
        }
        Object input = new OOMObject();
        // WHEN / THEN
        OutOfMemoryError err = assertThrows(OutOfMemoryError.class, () -> StringConversions.toStringBuffer(input, mockConverter), "OutOfMemoryError from toString() must propagate unchanged");
        assertEquals("simulated OOM", err.getMessage());
        verifyNoInteractions(mockConverter);
    }
}
