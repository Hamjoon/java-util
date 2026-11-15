package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.time.*;
import java.util.*;
import java.sql.Timestamp;

// import org.junit.*;
// import static org.junit.Assert.*;
// @ExtendWith(MockitoExtension.class)
class Converter_convert_Test {

    /**
     * The instance under test – created fresh for each test method.
     */
    private Converter converter;

    //     @BeforeEach
    void setupBeforeEach() {
        // Each test gets a clean Converter instance with default options.
        converter = new Converter(defaultOptions());
    }

    //     @AfterEach
    void teardownAfterEach() throws Exception {
        // No per‑test cleanup required.
        converter = null;
        // USER_DB is a private static field; clear it to avoid cross‑test contamination.
        java.lang.reflect.Field userDbField = Converter.class.getDeclaredField("USER_DB");
        userDbField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<Converter.ConversionPair, Convert<?>> userDb = (Map<Converter.ConversionPair, Convert<?>>) userDbField.get(null);
        userDb.clear();
    }

    /**
     * Simple {@link ConverterOptions} implementation that uses the defaults.
     */
    private static ConverterOptions defaultOptions() {
        return new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
    }
}
