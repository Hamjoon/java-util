package com.cedarsoftware.util.convert;

import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toByte_Test {

    /**
     * Mocked Converter – not used by the method but required by the signature.
     */
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise mocks for each test.
        MockitoAnnotations.openMocks(this);
    }

    private static Stream<Arguments> booleanProvider() {
        return Stream.of(Arguments.of(Boolean.TRUE, Byte.valueOf((byte) 1)), Arguments.of(Boolean.FALSE, Byte.valueOf((byte) 0)));
    }

    @Test
    void toByte_returnsOneWhenAtomicBooleanIsTrue() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // WHEN
        Byte result = AtomicBooleanConversions.toByte(source, mockConverter);
        // THEN
        assertEquals(Byte.valueOf((byte) 1), result, "Expected Byte value 1 when AtomicBoolean is true");
    }

    @Test
    void toByte_returnsZeroWhenAtomicBooleanIsFalse() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        // WHEN
        Byte result = AtomicBooleanConversions.toByte(source, mockConverter);
        // THEN
        assertEquals(Byte.valueOf((byte) 0), result, "Expected Byte value 0 when AtomicBoolean is false");
    }

    @Test
    void toByte_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // GIVEN
        Object notAtomicBoolean = "not an atomic boolean";
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toByte(notAtomicBoolean, mockConverter), "Expected ClassCastException when source is not an AtomicBoolean");
    }

    @Test
    void toByte_throwsNullPointerExceptionWhenFromIsNull() {
        // The cast itself succeeds (null -> null) but the subsequent call to
        // b.get() triggers a NullPointerException.
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toByte(null, mockConverter), "Expected NullPointerException when 'from' argument is null");
    }

    @Test
    void toByte_ignoresNonNullConverter() {
        // Converter that would explode if its convert method were ever called.
        Converter evilConverter = new Converter() {

            @Override
            public Object convert(Object src, Class<?> dest) {
                throw new RuntimeException("Should not be called");
            }
        };
        AtomicBoolean source = new AtomicBoolean(true);
        Byte result = AtomicBooleanConversions.toByte(source, evilConverter);
        assertEquals(Byte.valueOf((byte) 1), result, "Method should ignore the supplied Converter and still return 1");
    }

    @Test
    void toByte_returnsZeroWhenAtomicBooleanWasMutatedBeforeCall() {
        // GIVEN an AtomicBoolean that starts true but is flipped to false
        AtomicBoolean mutable = new AtomicBoolean(true);
        // mutate before conversion
        mutable.set(false);
        // WHEN
        Byte result = AtomicBooleanConversions.toByte(mutable, mockConverter);
        // THEN
        assertEquals(Byte.valueOf((byte) 0), result, "Method must reflect the current state of the AtomicBoolean");
    }

    @Test
    void toByte_returnsByteWrapperInstance() {
        AtomicBoolean source = new AtomicBoolean(false);
        Byte result = AtomicBooleanConversions.toByte(source, null);
        // Byte.valueOf caches values in the range -128..127, so we can safely use
        // assertSame to confirm the exact cached instance.
        assertSame(Byte.valueOf((byte) 0), result, "Returned instance should be exactly Byte.valueOf((byte)0)");
    }

    @ParameterizedTest(name = "AtomicBoolean({0}) → expected Byte {1}")
    @MethodSource("booleanProvider")
    void toByte_parameterised(Boolean boolValue, Byte expected) {
        AtomicBoolean source = new AtomicBoolean(boolValue);
        Byte result = AtomicBooleanConversions.toByte(source, null);
        assertEquals(expected, result);
    }
}
