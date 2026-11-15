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
class AtomicBooleanConversions_toBoolean_Test {

    @Mock
    private Converter // Converter is not used by the method but must be supplied
    mockConverter;

    private AutoCloseable mocks;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations for this test instance
        mocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Release Mockito resources
        mocks.close();
    }

    private static Stream<Arguments> realAtomicBooleanProvider() {
        Converter nullConverter = null;
        // ordinary mock – never called
        Converter mockConverter = mock(Converter.class);
        // would throw if invoked
        Converter throwingConverter = mock(Converter.class);
        // No stubbing needed; the method never calls any Converter method.
        return Stream.of(Arguments.of(new AtomicBoolean(true), nullConverter, true), Arguments.of(new AtomicBoolean(false), nullConverter, false), Arguments.of(new AtomicBoolean(true), mockConverter, true), Arguments.of(new AtomicBoolean(false), throwingConverter, false));
    }

    @Test
    void toBoolean_returnsTrueWhenAtomicBooleanIsTrue() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        boolean result = AtomicBooleanConversions.toBoolean(source, mockConverter);
        // then
        assertTrue(result, "Expected true when source AtomicBoolean holds true");
    }

    @Test
    void toBoolean_returnsFalseWhenAtomicBooleanIsFalse() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        boolean result = AtomicBooleanConversions.toBoolean(source, mockConverter);
        // then
        assertFalse(result, "Expected false when source AtomicBoolean holds false");
    }

    @Test
    void toBoolean_throwsClassCastExceptionWhenSourceIsNotAtomicBoolean() {
        // given
        Object notAtomic = "not an atomic boolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBoolean(notAtomic, mockConverter), "Expected ClassCastException when source is not an AtomicBoolean");
    }

    @Test
    void toBoolean_throwsNullPointerExceptionWhenSourceIsNull() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBoolean(nullSource, mockConverter), "Expected NullPointerException when source is null");
    }

    @ParameterizedTest(name = "[{index}] from={0}, converter={1}")
    @MethodSource("realAtomicBooleanProvider")
    void toBoolean_realAtomicBoolean_behaviour(AtomicBoolean from, Converter converter, boolean expected) {
        // when
        boolean result = AtomicBooleanConversions.toBoolean(from, converter);
        // then
        assertEquals(expected, result, () -> "Expected " + expected + " for AtomicBoolean value " + from.get());
        // The method must never touch the converter
        if (converter != null) {
            verifyZeroInteractions(converter);
        }
    }

    @Test
    void toBoolean_mockAtomicBoolean_returnsTrue() {
        // given
        AtomicBoolean mockAtomic = mock(AtomicBoolean.class);
        when(mockAtomic.get()).thenReturn(true);
        // when
        boolean result = AtomicBooleanConversions.toBoolean(mockAtomic, mockConverter);
        // then
        assertTrue(result, "Mocked AtomicBoolean returning true should yield true");
        verify(mockAtomic).get();
        verifyZeroInteractions(mockConverter);
    }

    @Test
    void toBoolean_mockAtomicBoolean_returnsFalse() {
        // given
        AtomicBoolean mockAtomic = mock(AtomicBoolean.class);
        when(mockAtomic.get()).thenReturn(false);
        // when
        boolean result = AtomicBooleanConversions.toBoolean(mockAtomic, mockConverter);
        // then
        assertFalse(result, "Mocked AtomicBoolean returning false should yield false");
        verify(mockAtomic).get();
        verifyZeroInteractions(mockConverter);
    }

    @Test
    void toBoolean_mockAtomicBoolean_propagatesIllegalStateException() {
        // given
        AtomicBoolean mockAtomic = mock(AtomicBoolean.class);
        when(mockAtomic.get()).thenThrow(new IllegalStateException("boom"));
        // when / then
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> AtomicBooleanConversions.toBoolean(mockAtomic, mockConverter), "Exception from mock get() should be propagated");
        assertEquals("boom", ex.getMessage());
        verify(mockAtomic).get();
        verifyZeroInteractions(mockConverter);
    }

    @Test
    void toBoolean_mockAtomicBoolean_propagatesIllegalArgumentException() {
        // given
        AtomicBoolean mockAtomic = mock(AtomicBoolean.class);
        when(mockAtomic.get()).thenThrow(new IllegalArgumentException("bad arg"));
        // when / then
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> AtomicBooleanConversions.toBoolean(mockAtomic, mockConverter));
        assertEquals("bad arg", ex.getMessage());
        verify(mockAtomic).get();
        verifyZeroInteractions(mockConverter);
    }

    @Test
    void toBoolean_mockAtomicBoolean_propagatesNullPointerException() {
        // given
        AtomicBoolean mockAtomic = mock(AtomicBoolean.class);
        when(mockAtomic.get()).thenThrow(new NullPointerException("null inside get"));
        // when / then
        NullPointerException ex = assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBoolean(mockAtomic, mockConverter));
        assertEquals("null inside get", ex.getMessage());
        verify(mockAtomic).get();
        verifyZeroInteractions(mockConverter);
    }

    @Test
    void toBoolean_nonNullConverter_isNeverInvoked() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // spy to detect any accidental call
        Converter spyConverter = spy(Converter.class);
        // when
        boolean result = AtomicBooleanConversions.toBoolean(source, spyConverter);
        // then
        assertTrue(result, "Result should reflect the AtomicBoolean value");
        verifyZeroInteractions(spyConverter);
    }
}
