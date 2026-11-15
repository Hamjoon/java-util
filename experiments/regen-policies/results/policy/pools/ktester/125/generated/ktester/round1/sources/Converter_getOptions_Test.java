package com.cedarsoftware.util.convert;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.TimeZone;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class Converter_getOptions_Test {

    /**
     * The Converter under test.
     */
    private Converter converter;

    /**
     * The options supplied to the Converter.
     */
    private ConverterOptions options;

    @BeforeEach
    void setupBeforeEach() {
        // Create a simple, concrete implementation of ConverterOptions.
        // Only the methods needed by the test suite are overridden.
        options = new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public Charset getCharset() {
                return StandardCharsets.UTF_8;
            }

            @Override
            public ZoneId getZoneId() {
                return ZoneId.of("UTC");
            }

            @Override
            public TimeZone getTimeZone() {
                return TimeZone.getTimeZone("UTC");
            }
        };
        // Instantiate the Converter with the above options.
        converter = new Converter(options);
    }

    @AfterEach
    void teardownAfterEach() {
        // Explicitly drop references – helps the GC in long test suites.
        converter = null;
        options = null;
    }

    /**
     * Verify that {@link Converter#getOptions()} returns the exact instance that
     * was supplied to the constructor.
     */
    @Test
    void getOptionsReturnsSameInstance() {
        ConverterOptions returned = converter.getOptions();
        assertSame(options, returned, "Converter#getOptions() should return the same instance passed to the constructor");
    }

    /**
     * Ensure that the returned {@link ConverterOptions} can be used through the
     * public API – e.g., charset, zone, and time‑zone values are as expected.
     */
    @Test
    void optionsExposeConfiguredValues() {
        ConverterOptions opts = converter.getOptions();
        // Charset
        assertEquals(StandardCharsets.UTF_8, opts.getCharset(), "Charset configured via ConverterOptions should be UTF-8");
        // ZoneId
        assertEquals(ZoneId.of("UTC"), opts.getZoneId(), "ZoneId configured via ConverterOptions should be UTC");
        // TimeZone
        assertEquals(TimeZone.getTimeZone("UTC"), opts.getTimeZone(), "TimeZone configured via ConverterOptions should be UTC");
    }

    /**
     * A regression test that checks the options are not recreated on each call.
     * The same reference must be returned on multiple invocations.
     */
    @Test
    void getOptionsIsConsistentAcrossCalls() {
        ConverterOptions firstCall = converter.getOptions();
        ConverterOptions secondCall = converter.getOptions();
        assertSame(firstCall, secondCall, "Multiple calls to getOptions() should return the identical object");
    }

    /**
     * When a {@link Converter} is created with the no‑arg constructor,
     * {@link Converter#getOptions()} should return a non‑null default
     * {@link ConverterOptions} instance.
     *
     * This test covers the {@code defaultConstructorOptions} group from the
     * supplied JSON test definition.
     */
    @Test
    void defaultConstructorReturnsNonNullOptions() {
        // The fixture's {@code converter} is created with custom options,
        // so we instantiate a fresh Converter using the no‑arg constructor.
        // assumes a public no‑arg ctor exists
        Converter defaultConverter = new Converter();
        ConverterOptions defaultOpts = defaultConverter.getOptions();
        assertNotNull(defaultOpts, "Converter created with the no‑arg constructor should provide a non‑null ConverterOptions");
        // Optional sanity check: the returned object should be an instance of ConverterOptions
        assertTrue(defaultOpts instanceof ConverterOptions, "Returned object should be an instance of ConverterOptions");
    }
}
