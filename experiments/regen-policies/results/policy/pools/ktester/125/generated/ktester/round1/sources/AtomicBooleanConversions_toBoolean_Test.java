package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AtomicBooleanConversions_toBoolean_Test {

    @Mock
    private Converter // not used by the method, but required by signature
    mockConverter;

    // for manual init/close if needed
    private AutoCloseable mocksCloseable;

    @BeforeEach
    void setupBeforeEach() {
        // Initialise Mockito annotations (the @Mock field)
        mocksCloseable = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void teardownAfterEach() throws Exception {
        // Verify that the mockConverter was never interacted with
        verifyNoInteractions(mockConverter);
        // Close the Mockito openMocks resource
        mocksCloseable.close();
    }

    @Test
    void toBoolean_returnsTrueWhenAtomicBooleanIsTrue() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // WHEN
        boolean result = AtomicBooleanConversions.toBoolean(source, mockConverter);
        // THEN
        assertTrue(result, "Expected true when source AtomicBoolean holds true");
    }

    @Test
    void toBoolean_returnsFalseWhenAtomicBooleanIsFalse() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(false);
        // WHEN
        boolean result = AtomicBooleanConversions.toBoolean(source, mockConverter);
        // THEN
        assertFalse(result, "Expected false when source AtomicBoolean holds false");
    }

    @Test
    void toBoolean_nullSource_throwsNullPointerException() {
        // GIVEN
        Object source = null;
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> AtomicBooleanConversions.toBoolean(source, mockConverter), "Expected NPE when source is null");
    }

    @Test
    void toBoolean_wrongType_throwsClassCastException() {
        // GIVEN
        // not an AtomicBoolean
        Object source = Boolean.TRUE;
        // WHEN / THEN
        assertThrows(ClassCastException.class, () -> AtomicBooleanConversions.toBoolean(source, mockConverter), "Expected ClassCastException when source is not an AtomicBoolean");
    }

    @Test
    void toBoolean_withNonNullConverter_ignoresConverterAndReturnsTrue() {
        // GIVEN
        AtomicBoolean source = new AtomicBoolean(true);
        // Create a dummy Converter instance – it can be a mock because the method never calls it.
        Converter dummyConverter = mock(Converter.class);
        // WHEN
        boolean result = AtomicBooleanConversions.toBoolean(source, dummyConverter);
        // THEN
        assertTrue(result, "Method should return true regardless of the converter instance");
        // Verify that the dummy converter was also not interacted with.
        verifyNoInteractions(dummyConverter);
    }
}
