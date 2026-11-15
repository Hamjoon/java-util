package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

@ExtendWith(MockitoExtension.class)
class Converter_getOptions_Test {

    /**
     * The mock {@link ConverterOptions} that will be supplied to the {@link Converter}.
     */
    @Mock
    private ConverterOptions mockOptions;

    /**
     * The {@link Converter} under test.
     */
    private Converter converter;

    @BeforeEach
    void setupBeforeEach() {
        // Configure the mock to return deterministic values for the few getters that
        // are used by the existing test‑suite (time‑zone, zone‑id and charset).
        when(mockOptions.getTimeZone()).thenReturn(TimeZone.getTimeZone("UTC"));
        when(mockOptions.getZoneId()).thenReturn(ZoneId.of("UTC"));
        when(mockOptions.getCharset()).thenReturn(StandardCharsets.UTF_8);
        // Create the Converter with the mocked options.
        converter = new Converter(mockOptions);
    }

    @AfterEach
    void teardownAfterEach() {
        // Reset the mock to avoid cross‑test interference.
        reset(mockOptions);
    }

    @Test
    void getOptions_returnsSameInstanceProvidedToConstructor() {
        // WHEN
        ConverterOptions returned = converter.getOptions();
        // THEN
        assertSame(mockOptions, returned, "Converter#getOptions() should return the exact instance supplied to the constructor");
    }

    @Test
    void getOptions_canBeUsedToRetrieveConfiguredTimeZone() {
        // GIVEN
        TimeZone expected = TimeZone.getTimeZone("UTC");
        // WHEN
        TimeZone actual = converter.getOptions().getTimeZone();
        // THEN
        assertEquals(expected, actual, "The TimeZone obtained from ConverterOptions should match the stubbed value");
        // Verify that the getter was indeed called on the mock.
        verify(mockOptions, times(1)).getTimeZone();
    }

    @Test
    void getOptions_canBeUsedToRetrieveConfiguredZoneId() {
        // GIVEN
        ZoneId expected = ZoneId.of("UTC");
        // WHEN
        ZoneId actual = converter.getOptions().getZoneId();
        // THEN
        assertEquals(expected, actual, "The ZoneId obtained from ConverterOptions should match the stubbed value");
        verify(mockOptions, times(1)).getZoneId();
    }

    @Test
    void getOptions_canBeUsedToRetrieveConfiguredCharset() {
        // GIVEN
        Charset expected = StandardCharsets.UTF_8;
        // WHEN
        Charset actual = converter.getOptions().getCharset();
        // THEN
        assertEquals(expected, actual, "The Charset obtained from ConverterOptions should match the stubbed value");
        verify(mockOptions, times(1)).getCharset();
    }

    @Test
    void getOptions_isNotNullEvenWhenOptionsAreEmpty() {
        // Create a Converter with a real (empty) options instance to ensure non‑null behavior.
        Converter emptyOptionsConverter = new Converter(new ConverterOptions() {
        });
        assertNotNull(emptyOptionsConverter.getOptions(), "Converter#getOptions() must never return null, even with an empty options implementation");
    }

    /**
     * Verify that after performing a conversion the same {@link ConverterOptions}
     * instance is still returned.
     */
    @Test
    void getOptions_returnsSameInstanceAfterConversion() {
        // Use a real (non‑mock) options object so that conversion logic can run.
        ConverterOptions realOptions = new ConverterOptions() {
        };
        Converter realConverter = new Converter(realOptions);
        // Perform a simple conversion that is guaranteed to be supported by the library.
        Integer result = realConverter.convert("123", Integer.class);
        assertEquals(123, result, "Sanity check – conversion should succeed");
        // The options reference must remain unchanged.
        assertSame(realOptions, realConverter.getOptions(), "After a conversion, getOptions() must still return the original instance");
    }

    /**
     * Using reflection, set the private final field {@code options} to {@code null}
     * and verify that {@code getOptions()} returns {@code null}.
     */
    @Test
    void getOptions_returnsNullWhenFieldIsForcefullySetToNull() throws Exception {
        // Create a Converter with a non‑null options instance.
        ConverterOptions dummy = new ConverterOptions() {
        };
        Converter c = new Converter(dummy);
        // Break encapsulation via reflection.
        Field optionsField = Converter.class.getDeclaredField("options");
        optionsField.setAccessible(true);
        // Remove final modifier (works on most JVMs for test purposes).
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(optionsField, optionsField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
        optionsField.set(c, null);
        // Now the getter should return null.
        assertNull(c.getOptions(), "When the internal options field is null, getOptions() must return null");
    }

    /**
     * Multiple successive calls must return the identical reference.
     */
    @Test
    void getOptions_multipleCallsReturnIdenticalInstance() {
        ConverterOptions first = converter.getOptions();
        for (int i = 0; i < 5; i++) {
            assertSame(first, converter.getOptions(), "All successive calls to getOptions() must return the same instance");
        }
    }

    /**
     * Two independently created {@link Converter} objects must not share the same
     * {@link ConverterOptions} instance.
     */
    @Test
    void getOptions_distinctInstancesPerConverter() {
        Converter c1 = new Converter(new ConverterOptions() {
        });
        Converter c2 = new Converter(new ConverterOptions() {
        });
        assertNotSame(c1.getOptions(), c2.getOptions(), "Each Converter should have its own distinct ConverterOptions instance");
    }

    /**
     * Verify that the public no‑arg constructor (if present) creates a non‑null
     * {@link ConverterOptions} instance.
     */
    @Test
    void getOptions_defaultConstructorReturnsNonNull() throws Exception {
        // Attempt to locate a public no‑arg constructor via reflection.
        // If the class does not have one, the test is skipped.
        try {
            Converter noArgConverter = Converter.class.getDeclaredConstructor().newInstance();
            assertNotNull(noArgConverter.getOptions(), "Default constructor must initialise a non‑null ConverterOptions");
        } catch (NoSuchMethodException e) {
            // No default constructor – the requirement is not applicable.
            Assumptions.assumeTrue(false, "Converter does not have a public no‑arg constructor");
        }
    }

    /**
     * Concurrent access test – all threads must see the same {@link ConverterOptions}
     * reference.
     */
    @Test
    void getOptions_concurrentAccessReturnsSameReference() throws Exception {
        final int THREAD_COUNT = 10;
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT);
        try {
            // Use a real Converter (options do not matter for this test).
            Converter concurrentConverter = new Converter(new ConverterOptions() {
            });
            Callable<ConverterOptions> task = concurrentConverter::getOptions;
            List<Future<ConverterOptions>> futures = new ArrayList<>();
            for (int i = 0; i < THREAD_COUNT; i++) {
                futures.add(executor.submit(task));
            }
            // Collect results.
            Set<ConverterOptions> distinctResults = new HashSet<>();
            for (Future<ConverterOptions> f : futures) {
                distinctResults.add(f.get(2, TimeUnit.SECONDS));
            }
            assertEquals(1, distinctResults.size(), "All threads must receive the identical ConverterOptions instance");
        } finally {
            executor.shutdownNow();
        }
    }
}
