package com.cedarsoftware.util.convert;

import java.nio.ByteBuffer;
import java.util.*;
import java.lang.reflect.Field;
import java.time.*;
import java.net.URL;
// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
// @RunWith(MockitoJUnitRunner.class)
class Converter_isConversionSupportedFor_Test {

    private Converter converter;

//     @BeforeEach
    void setupBeforeEach() {
        // A fresh Converter for each test guarantees isolation.
        converter = new Converter(defaultOptions());
    }

//     @AfterEach
    void teardownAfterEach() {
        // Help GC – not strictly required but keeps the pattern symmetric.
        converter = null;
    }

    /**
     * Create a plain {@link ConverterOptions} implementation – all defaults.
     */
    private static ConverterOptions defaultOptions() {
        return new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }
        };
    }

    /**
     * Retrieves the private static {@code UNSUPPORTED} placeholder instance.
     *
     * @return the {@link Convert} used by Converter to denote an unsupported conversion
     * @throws Exception if reflection fails
     */
    private static Convert<?> getUnsupportedPlaceholder() throws Exception {
        Field f = Converter.class.getDeclaredField("UNSUPPORTED");
        f.setAccessible(true);
        return (Convert<?>) f.get(null);
    }

//     @Test
//     void shouldSupportStringToInteger() {
//         assertTrue(converter.isConversionSupportedFor(String.class, Integer.class));
//     }

//     @Test
//     void shouldSupportPrimitiveToWrapper() {
//         assertTrue(converter.isConversionSupportedFor(int.class, Integer.class));
//         assertTrue(converter.isConversionSupportedFor(boolean.class, Boolean.class));
//     }

//     @Test
//     void shouldSupportDateAndTimeConversions() {
//         assertTrue(converter.isConversionSupportedFor(Date.class, LocalDate.class));
//         assertTrue(converter.isConversionSupportedFor(LocalDate.class, Date.class));
//     }

//     @Test
//     void shouldSupportListToSetConversion() {
//         assertTrue(converter.isConversionSupportedFor(List.class, Set.class));
//     }

//     @Test
//     void shouldSupportMapToListConversion() {
//         assertTrue(converter.isConversionSupportedFor(Map.class, List.class));
//     }

//     @Test
//     void shouldSupportArrayToCollectionConversion() {
//         assertTrue(converter.isConversionSupportedFor(String[].class, List.class));
//     }

//     @Test
//     void shouldSupportObjectArrayToObjectArray() {
//         assertTrue(converter.isConversionSupportedFor(Object[].class, Object[].class));
//     }

//     @Test
//     void shouldSupportStringArrayToObjectArray() {
//         assertTrue(converter.isConversionSupportedFor(String[].class, Object[].class));
//     }

//     @Test
//     void shouldSupportStringArrayToStringArrayViaComponentCheck() {
//         assertTrue(converter.isConversionSupportedFor(String[].class, String[].class));
//     }

//     @Test
//     void shouldRejectArrayToArrayWhenComponentConversionIsMissing() {
//         assertFalse(converter.isConversionSupportedFor(ByteBuffer[].class, Integer[].class));
//     }

//     @Test
//     void shouldRejectUnsupportedConversion() {
//         assertFalse(converter.isConversionSupportedFor(String.class, Thread.class));
//     }

//     @Test
//     void shouldRejectCollectionConversionWhenTargetIsNotACollectionOrArray() {
//         assertFalse(converter.isConversionSupportedFor(List.class, Integer.class));
//     }

//     @Test
//     void shouldRejectWhenBothSourceAndTargetAreArraysAndTargetComponentIsIncompatible() {
//         assertFalse(converter.isConversionSupportedFor(ByteBuffer[].class, LocalDate[].class));
//     }

    /**
     * Inserts a mapping into the internal conversion cache used by {@link Converter}.
     *
     * @param source      source class
     * @param target      target class
     * @param conversion  the {@link Convert} instance to store (may be the UNSUPPORTED placeholder)
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static void putIntoCache(Class<?> source, Class<?> target, Convert<?> conversion) throws Exception {
        // The cache is a static Map; locate it via reflection.
        Field cacheField = null;
        for (Field f : Converter.class.getDeclaredFields()) {
            if (Map.class.isAssignableFrom(f.getType())) {
                // Heuristic: the first Map field is the conversion cache.
                cacheField = f;
                break;
            }
        }
        if (cacheField == null) {
            throw new IllegalStateException("Unable to locate the conversion cache field in Converter");
        }
        cacheField.setAccessible(true);
        Map<Object, Object> cache = (Map<Object, Object>) cacheField.get(null);
        Object pair = Converter.pair(source, target);
        cache.put(pair, conversion);
    }

//     @Test
//     void cacheHitSupportedShouldReturnTrueWithoutFurtherLookup() throws Exception {
//         // Arrange – place a dummy conversion into the cache.
//         Convert<Object> dummy = (from, conv) -> from;
//         putIntoCache(String.class, Integer.class, dummy);
//         // Act – call the method; it should hit the cache and return true.
//         boolean result = converter.isConversionSupportedFor(String.class, Integer.class);
//         // Assert
//         assertTrue(result);
//     }

//     @Test
//     void cacheHitUnsupportedShouldReturnFalse() throws Exception {
//         // Arrange – put the internal UNSUPPORTED placeholder into the cache.
//         Convert<?> unsupported = getUnsupportedPlaceholder();
//         putIntoCache(Boolean.class, LocalDate.class, unsupported);
//         // Act
//         boolean result = converter.isConversionSupportedFor(Boolean.class, LocalDate.class);
//         // Assert
//         assertFalse(result);
//     }

//     @Test
//     void directDbConversionShouldBeDetectedAndCached() {
//         // Ensure no prior conversion exists.
//         assertFalse(converter.isConversionSupportedFor(Long.class, String.class));
//         // Register a custom conversion (simulates a built‑in DB entry).
//         Convert<Object> longToString = (from, conv) -> String.valueOf(from);
//         Converter.addConversion(Long.class, String.class, longToString);
//         // Now the method should find the conversion via the DB path.
//         assertTrue(converter.isConversionSupportedFor(Long.class, String.class));
//     }

//     @Test
//     void collectionConversionNonArrayShouldBeSupported() {
//         assertTrue(converter.isConversionSupportedFor(List.class, Set.class));
//     }

//     @Test
//     void arrayConversionTargetComponentObjectShouldBeSupported() {
//         assertTrue(converter.isConversionSupportedFor(int[].class, Object[].class));
//     }

//     @Test
//     void arrayConversionRecursiveSupportedShouldReturnTrue() {
//         // String → CharSequence is supported (String implements CharSequence)
//         assertTrue(converter.isConversionSupportedFor(String[].class, CharSequence[].class));
//     }

//     @Test
//     void arrayConversionRecursiveUnsupportedShouldReturnFalse() {
//         // String → Integer has no conversion, so the array conversion must fail.
//         assertFalse(converter.isConversionSupportedFor(String[].class, Integer[].class));
//     }

//     @Test
//     void inheritedConversionShouldBeDetected() {
//         // ArrayList is a subclass of List; inheritance conversion should succeed.
//         assertTrue(converter.isConversionSupportedFor(ArrayList.class, List.class));
//     }

//     @Test
//     void noConversionPossibleShouldReturnFalse() {
//         assertFalse(converter.isConversionSupportedFor(Thread.class, URL.class));
//     }

//     @Test
//     void arraySourceToCollectionTargetShouldBeSupported() {
//         assertTrue(converter.isConversionSupportedFor(String[].class, List.class));
//     }

//     @Test
//     void collectionSourceToArrayTargetShouldBeSupported() {
//         assertTrue(converter.isConversionSupportedFor(Set.class, Integer[].class));
//     }

//     @Test
//     void arrayToArraySameComponentShouldBeSupported() {
//         assertTrue(converter.isConversionSupportedFor(CharSequence[].class, CharSequence[].class));
//     }

//     @Test
//     void arrayToArrayPrimitiveComponentUnsupportedShouldBeFalse() {
//         // No built‑in conversion from int to long (primitive widening is not handled here)
//         assertFalse(converter.isConversionSupportedFor(int[].class, long[].class));
//     }

//     @Test
//     void nullSourceClassShouldThrowNPE() {
//         try {
//             converter.isConversionSupportedFor(null, String.class);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void nullTargetClassShouldThrowNPE() {
//         try {
//             converter.isConversionSupportedFor(Integer.class, null);
//             fail("Expected NullPointerException");
//         } catch (NullPointerException ignored) {
//         }
//     }

//     @Test
//     void wrapperToPrimitiveConversionShouldBeSupported() {
//         assertTrue(converter.isConversionSupportedFor(Integer.class, int.class));
//     }

//     @Test
//     void identityConversionShouldBeSupported() {
//         assertTrue(converter.isConversionSupportedFor(LocalDateTime.class, LocalDateTime.class));
//     }
}