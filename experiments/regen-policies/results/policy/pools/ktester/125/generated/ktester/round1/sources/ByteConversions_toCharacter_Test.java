package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class ByteConversions_toCharacter_Test {

    /**
     * Mocked {@link Converter} – the method under test should never call any of its methods.
     */
    @Mock
    private Converter mockConverter;

    /**
     * Auto‑closeable resource for Mockito’s open mocks.
     */
    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance.
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Verify that the mock was not used and release resources.
        // (If the method under test mistakenly uses the converter, the verification will fail.)
        verifyNoInteractions(mockConverter);
        mocks.close();
    }

    /**
     * Provides a stream of arguments for successful conversion tests.
     * Each argument consists of:
     *   - the {@code from} object (a {@link Byte})
     *   - the expected {@code char} result after conversion.
     */
    private static Stream<Arguments> successfulConversionProvider() {
        return Stream.of(// Normal conversions
        // 65 → 'A'
        Arguments.of(Byte.valueOf((byte) 65), 'A'), // -1 → 0xFFFF
        Arguments.of(Byte.valueOf((byte) -1), '\uFFFF'), // Boundary values
        // 0 → NUL
        Arguments.of(Byte.valueOf((byte) 0), '\u0000'), // 127 → DELETE
        Arguments.of(Byte.valueOf(Byte.MAX_VALUE), '\u007F'), // -128 → 0xFF80
        Arguments.of(Byte.valueOf(Byte.MIN_VALUE), '\uFF80'), // Converter argument presence (non‑null converter is ignored)
        // 42 → '*'
        Arguments.of(Byte.valueOf((byte) 42), '*'));
    }

    @Test
    void toCharacter_withPositiveByte_returnsCorrespondingChar() {
        // ASCII 'A'
        Byte input = Byte.valueOf((byte) 65);
        Character result = ByteConversions.toCharacter(input, mockConverter);
        assertEquals('A', result);
    }

    @Test
    void toCharacter_withZeroByte_returnsNullChar() {
        Byte input = Byte.valueOf((byte) 0);
        Character result = ByteConversions.toCharacter(input, mockConverter);
        assertEquals('\u0000', result);
    }

    @Test
    void toCharacter_withNegativeByte_returnsCorrespondingChar() {
        // Byte -1 corresponds to char 0xFFFF (65535) when widened to int then cast to char.
        Byte input = Byte.valueOf((byte) -1);
        Character result = ByteConversions.toCharacter(input, mockConverter);
        // The cast (char) (-1) yields 0xFFFF, which is the Unicode replacement character.
        assertEquals('\uFFFF', result);
    }

    @Test
    void toCharacter_withNullInput_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ByteConversions.toCharacter(null, mockConverter));
    }

    /* -------------------------------------------------------------
     *  Successful conversion test cases (parameterised)
     * ------------------------------------------------------------- */
    @ParameterizedTest(name = "[{index}] from={0} → expected=''{1}''")
    @MethodSource("successfulConversionProvider")
    void toCharacter_successfulConversions(Object from, char expectedChar) {
        // The method under test does not use the Converter, so we can safely pass the mock.
        Character result = ByteConversions.toCharacter(from, mockConverter);
        assertEquals(expectedChar, result, () -> "Expected character '" + expectedChar + "' but got '" + result + "'");
    }

    /* -------------------------------------------------------------
     *  Exception handling test cases
     * ------------------------------------------------------------- */
    @Test
    void toCharacter_nullFrom_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ByteConversions.toCharacter(null, mockConverter), "Passing null as the 'from' argument should throw NullPointerException");
    }

    @Test
    void toCharacter_nonByteFrom_throwsClassCastException() {
        Integer notAByte = Integer.valueOf(10);
        assertThrows(ClassCastException.class, () -> ByteConversions.toCharacter(notAByte, mockConverter), "Passing a non‑Byte object should result in a ClassCastException");
    }
}
