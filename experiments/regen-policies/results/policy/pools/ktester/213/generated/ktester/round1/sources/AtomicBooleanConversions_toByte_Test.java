package com.cedarsoftware.util.convert;

import com.cedarsoftware.util.convert.CommonValues;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toByte_Test {

    @Mock
    private Converter // Converter is not used by toByte, but we provide a mock
    mockConverter;

    @Test
    void toByte_whenAtomicBooleanTrue_returnsByteOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Byte result = AtomicBooleanConversions.toByte(source, mockConverter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "AtomicBoolean true should be converted to BYTE_ONE");
    }

    @Test
    void toByte_whenAtomicBooleanFalse_returnsByteZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        Byte result = AtomicBooleanConversions.toByte(source, mockConverter);
        // then
        assertEquals(CommonValues.BYTE_ZERO, result, "AtomicBoolean false should be converted to BYTE_ZERO");
    }

    @Test
    void toByte_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object source = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toByte(source, mockConverter), "Passing null as source should throw NullPointerException");
    }

    @Test
    void toByte_whenSourceIsNotAtomicBoolean_throwsClassCastException() {
        // given
        Object source = "some string";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toByte(source, mockConverter), "A non‑AtomicBoolean source must cause a ClassCastException");
    }

    @Test
    void toByte_withExplicitNullConverter_returnsByteOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        Converter nullConverter = null;
        // when
        Byte result = AtomicBooleanConversions.toByte(source, nullConverter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Result must be BYTE_ONE even when converter argument is null");
    }

    @Test
    void toByte_withNonNullMockConverter_returnsByteOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // mockConverter is already a non‑null mock injected by Mockito
        // when
        Byte result = AtomicBooleanConversions.toByte(source, mockConverter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Result must be BYTE_ONE when a non‑null converter (mock) is supplied");
    }

    @Test
    void toByte_whenConverterThrows_ifNotUsed_stillReturnsByteOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        Converter throwingConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Should not be called");
            }
        };
        // when
        Byte result = AtomicBooleanConversions.toByte(source, throwingConverter);
        // then
        assertEquals(CommonValues.BYTE_ONE, result, "Method must ignore the converter and still return BYTE_ONE");
    }

    @Test
    void toByte_whenConverterReturnsNull_ifNotUsed_stillReturnsByteZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        Converter nullReturningConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                return null;
            }
        };
        // when
        Byte result = AtomicBooleanConversions.toByte(source, nullReturningConverter);
        // then
        assertEquals(CommonValues.BYTE_ZERO, result, "Method must ignore the converter and still return BYTE_ZERO");
    }

    @Test
    void toByte_resultIsByteObject_andHasCorrectValue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        Byte result = AtomicBooleanConversions.toByte(source, mockConverter);
        // then
        assertNotNull(result, "Result must not be null");
        assertTrue(result instanceof Byte, "Result should be an instance of java.lang.Byte");
        assertEquals((byte) 1, result.byteValue(), "Byte value should be 1 for a true AtomicBoolean");
    }
}
