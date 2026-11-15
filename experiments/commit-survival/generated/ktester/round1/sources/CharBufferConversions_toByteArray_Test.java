package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.ByteBuffer;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class CharBufferConversions_toByteArray_Test {

    /**
     * A real {@code Converter} instance – the tests do not depend on its internal conversion logic.
     */
    private static Converter converter;

    /**
     * Mocked static scope for {@link CharBufferConversions}.
     */
    private MockedStatic<CharBufferConversions> charBufferConversionsMock;

    /**
     * Mocked static scope for {@link ByteBufferConversions}.
     */
    private MockedStatic<ByteBufferConversions> byteBufferConversionsMock;

    /**
     * Simple holder for the arguments of the parameterized valid‑conversion test.
     */
    private static final class ArgumentsHolder {

        final Object source;

        final byte[] expected;

        final String description;

        ArgumentsHolder(Object source, byte[] expected, String description) {
            this.source = source;
            this.expected = expected;
            this.description = description;
        }

        Object getSource() {
            return source;
        }

        byte[] getExpected() {
            return expected;
        }

        String getDescription() {
            return description;
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // The Converter constructor requires a ConverterOptions instance.
        // Using the default options (null) is sufficient for these tests.
        converter = new Converter(new ConverterOptions());
    }

    @BeforeEach
    void setupBeforeEach() {
        // CALLS_REAL_METHODS allows us to mock only the methods we explicitly stub.
        charBufferConversionsMock = Mockito.mockStatic(CharBufferConversions.class, CALLS_REAL_METHODS);
        byteBufferConversionsMock = Mockito.mockStatic(ByteBufferConversions.class);
    }

    @AfterEach
    void teardownAfterEach() {
        // Close the static mocks – this restores the original static behaviour.
        charBufferConversionsMock.close();
        byteBufferConversionsMock.close();
    }

    private static Stream<ArgumentsHolder> validConversionProvider() {
        return Stream.of(new ArgumentsHolder("Hello", new byte[] { 72, 101, 108, 108, 111 }, "Convert a plain Java String to a byte array using the default converter (UTF-8)."), new ArgumentsHolder(new StringBuilder("World"), new byte[] { 87, 111, 114, 108, 100 }, "Convert a StringBuilder instance to a byte array; verifies delegation through toCharBuffer."), new ArgumentsHolder(new StringBuffer("Buffer"), new byte[] { 66, 117, 102, 102, 101, 114 }, "Convert a StringBuffer instance to a byte array."), new ArgumentsHolder(CharBuffer.wrap("CharBuf"), new byte[] { 67, 104, 97, 114, 66, 117, 102 }, "Directly convert a CharBuffer to a byte array."), new ArgumentsHolder(new char[] { 'a', 'b', 'c' }, new byte[] { 97, 98, 99 }, "Convert a primitive char array to a byte array."));
    }

    @Test
    void toByteArray_delegatesToByteBufferAndByteBufferConversions() {
        // GIVEN
        String source = "sample";
        ByteBuffer dummyByteBuffer = ByteBuffer.wrap(new byte[] { 0x01, 0x02, 0x03 });
        byte[] expectedResult = new byte[] { 0x0A, 0x0B, 0x0C };
        // Mock the intermediate conversion: CharBufferConversions.toByteBuffer(...)
        charBufferConversionsMock.when(() -> CharBufferConversions.toByteBuffer(eq(source), eq(converter))).thenReturn(dummyByteBuffer);
        // Mock the final conversion: ByteBufferConversions.toByteArray(...)
        byteBufferConversionsMock.when(() -> ByteBufferConversions.toByteArray(eq(dummyByteBuffer), eq(converter))).thenReturn(expectedResult);
        // WHEN
        byte[] actual = CharBufferConversions.toByteArray(source, converter);
        // THEN
        assertArrayEquals(expectedResult, actual, "The byte array returned by CharBufferConversions.toByteArray should match the value produced by the mocked ByteBufferConversions.");
        // Verify that the static methods were invoked exactly once with the expected arguments.
        charBufferConversionsMock.verify(() -> CharBufferConversions.toByteBuffer(eq(source), eq(converter)), times(1));
        byteBufferConversionsMock.verify(() -> ByteBufferConversions.toByteArray(eq(dummyByteBuffer), eq(converter)), times(1));
    }

    @Test
    void toByteArray_returnsNullWhenIntermediateByteBufferIsNull() {
        // GIVEN
        // any non‑null object
        Object source = new Object();
        // Simulate that the conversion to ByteBuffer yields null.
        charBufferConversionsMock.when(() -> CharBufferConversions.toByteBuffer(eq(source), eq(converter))).thenReturn(null);
        // Simulate that ByteBufferConversions.toByteArray also returns null when given a null ByteBuffer.
        byteBufferConversionsMock.when(() -> ByteBufferConversions.toByteArray(isNull(), eq(converter))).thenReturn(null);
        // WHEN
        byte[] actual = CharBufferConversions.toByteArray(source, converter);
        // THEN
        assertNull(actual, "When the intermediate ByteBuffer is null, the result should be null.");
        charBufferConversionsMock.verify(() -> CharBufferConversions.toByteBuffer(eq(source), eq(converter)), times(1));
        byteBufferConversionsMock.verify(() -> ByteBufferConversions.toByteArray(isNull(), eq(converter)), times(1));
    }

    @Test
    void toByteArray_returnsNullWhenSourceIsNull() {
        // GIVEN
        Object source = null;
        // The implementation is expected to propagate nulls; we mock the static calls accordingly.
        charBufferConversionsMock.when(() -> CharBufferConversions.toByteBuffer(isNull(), eq(converter))).thenReturn(null);
        byteBufferConversionsMock.when(() -> ByteBufferConversions.toByteArray(isNull(), eq(converter))).thenReturn(null);
        // WHEN
        byte[] actual = CharBufferConversions.toByteArray(source, converter);
        // THEN
        assertNull(actual, "When the source object is null, the result should be null.");
        charBufferConversionsMock.verify(() -> CharBufferConversions.toByteBuffer(isNull(), eq(converter)), times(1));
        byteBufferConversionsMock.verify(() -> ByteBufferConversions.toByteArray(isNull(), eq(converter)), times(1));
    }

    @ParameterizedTest(name = "[{index}] {2}")
    @MethodSource("validConversionProvider")
    void toByteArray_validConversions(Object source, byte[] expectedBytes, String description) {
        // GIVEN – a ByteBuffer that already contains the expected bytes
        ByteBuffer dummyByteBuffer = ByteBuffer.wrap(expectedBytes);
        // Mock the intermediate conversion
        charBufferConversionsMock.when(() -> CharBufferConversions.toByteBuffer(eq(source), eq(converter))).thenReturn(dummyByteBuffer);
        // Mock the final conversion – simply return the same byte array we expect
        byteBufferConversionsMock.when(() -> ByteBufferConversions.toByteArray(eq(dummyByteBuffer), eq(converter))).thenReturn(expectedBytes);
        // WHEN
        byte[] actual = CharBufferConversions.toByteArray(source, converter);
        // THEN
        assertArrayEquals(expectedBytes, actual, "The returned byte array must match the bytes produced by the mocked ByteBufferConversions.");
        // Verify delegation happened exactly once
        charBufferConversionsMock.verify(() -> CharBufferConversions.toByteBuffer(eq(source), eq(converter)), times(1));
        byteBufferConversionsMock.verify(() -> ByteBufferConversions.toByteArray(eq(dummyByteBuffer), eq(converter)), times(1));
    }

    @Test
    void toByteArray_nullConverter_throwsNullPointerException() {
        // GIVEN
        Object source = "sample";
        Converter nullConverter = null;
        // The real implementation will forward the null converter to the static
        // methods; we let the call happen without stubbing – it should raise NPE.
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteArray(source, nullConverter), "Passing a null Converter must result in NullPointerException.");
    }

    @Test
    void toByteArray_nullFrom_nullConverter_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        Converter nullConverter = null;
        assertThrows(NullPointerException.class, () -> CharBufferConversions.toByteArray(source, nullConverter), "Both arguments null should still cause a NullPointerException.");
    }

    @Test
    void toByteArray_unsupportedSourceType_throwsIllegalArgumentException() {
        // GIVEN
        // Integer is not a supported source type
        Object unsupported = 12345;
        // Simulate the internal toByteBuffer throwing IllegalArgumentException
        charBufferConversionsMock.when(() -> CharBufferConversions.toByteBuffer(eq(unsupported), eq(converter))).thenThrow(new IllegalArgumentException("Unsupported source type"));
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> CharBufferConversions.toByteArray(unsupported, converter), "An unsupported source type must cause IllegalArgumentException.");
        charBufferConversionsMock.verify(() -> CharBufferConversions.toByteBuffer(eq(unsupported), eq(converter)), times(1));
    }

    @Test
    void toByteArray_converterThrows_propagatesException() {
        // GIVEN
        Object source = "trigger";
        // Create a Converter that throws during conversion
        Converter explodingConverter = new Converter(new ConverterOptions()) {

            @Override
            public ByteBuffer toByteBuffer(Object o, Converter c) {
                throw new IllegalStateException("converter failure");
            }
        };
        // Mock the static call to delegate to the exploding converter
        charBufferConversionsMock.when(() -> CharBufferConversions.toByteBuffer(eq(source), eq(explodingConverter))).// let the real method (which uses the converter) run
        thenCallRealMethod();
        // The real toByteBuffer will invoke the overridden method and throw
        assertThrows(IllegalStateException.class, () -> CharBufferConversions.toByteArray(source, explodingConverter), "Exception thrown by a custom Converter must propagate unchanged.");
    }
}
