package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.*;
import static org.mockito.Mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toAtomicInteger_Test {

    // The Converter is not used by the method under test, but it must be supplied.
    @Mock
    private Converter mockConverter;

    @BeforeEach
    void setupBeforeEach() {
        // Mockito will initialise @Mock fields automatically.
        // Ensure the mock has no unexpected interactions.
        clearInvocations(mockConverter);
    }

    @AfterEach
    void teardownAfterEach() {
        // Verify that the mock was not used (the method does not call the converter).
        verifyNoInteractions(mockConverter);
    }

    @Test
    void toAtomicInteger_whenAtomicBooleanTrue_returnsOne() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(1, result.get(), "AtomicBoolean true should map to AtomicInteger 1");
    }

    @Test
    void toAtomicInteger_whenAtomicBooleanFalse_returnsZero() {
        // given
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.get(), "AtomicBoolean false should map to AtomicInteger 0");
    }

    @Test
    void toAtomicInteger_whenSourceIsNotAtomicBoolean_throwsClassCastException() {
        // given
        Object notAnAtomicBoolean = "not an AtomicBoolean";
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicInteger(notAnAtomicBoolean, mockConverter), "Casting a non‑AtomicBoolean should throw ClassCastException");
    }

    @Test
    void toAtomicInteger_whenSourceIsInteger_throwsClassCastException() {
        // given
        Object integerSource = Integer.valueOf(5);
        // when / then
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toAtomicInteger(integerSource, mockConverter), "Casting an Integer to AtomicBoolean should throw ClassCastException");
    }

    @Test
    void toAtomicInteger_whenSourceIsNull_throwsNullPointerException() {
        // given
        Object nullSource = null;
        // when / then
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toAtomicInteger(nullSource, mockConverter), "Passing null as the source should throw NullPointerException");
    }

    @Test
    void toAtomicInteger_instanceIndependence_sourceChangeDoesNotAffectResult() {
        // given
        AtomicBoolean source = new AtomicBoolean(true);
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, mockConverter);
        assertEquals(1, result.get(), "Pre‑condition: result should be 1");
        // when: mutate the original AtomicBoolean
        source.set(false);
        // then: the previously created AtomicInteger must stay unchanged
        assertEquals(1, result.get(), "Changing the original AtomicBoolean after conversion must not affect the returned AtomicInteger");
    }

    @Test
    void toAtomicInteger_converterIsIgnored_evenWhenNonNull() {
        // given: a non‑null (mock) converter – the method should ignore it
        Converter dummyConverter = mock(Converter.class);
        AtomicBoolean source = new AtomicBoolean(false);
        // when
        AtomicInteger result = AtomicBooleanConversions.toAtomicInteger(source, dummyConverter);
        // then
        assertNotNull(result, "Result should not be null");
        assertEquals(0, result.get(), "Converter is ignored; false should map to 0");
        // verify that the dummy converter was not used
        verifyNoInteractions(dummyConverter);
    }
}
