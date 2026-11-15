package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import java.nio.charset.Charset;
import java.time.ZoneId;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;

@ExtendWith(MockitoExtension.class)
class Converter_getSupportedConversions_Test {

    /**
     * The instance under test.
     */
    private Converter converter;

    class AClass {
    }

    class BClass {
    }

    class AClass {
    }

    class BClass {
    }

    @BeforeEach
    void setupBeforeEach() {
        // Create a fresh Converter for each test to avoid cross‑test contamination.
        converter = new Converter(defaultOptions());
    }

    @AfterEach
    void teardownAfterEach() {
        // Help GC and make sure no state leaks between tests.
        converter = null;
    }

    /**
     * Minimal {@link ConverterOptions} implementation required for the {@link Converter} constructor.
     */
    private static ConverterOptions defaultOptions() {
        return new ConverterOptions() {

            @Override
            public <T> T getCustomOption(String name) {
                return null;
            }

            @Override
            public Charset getCharset() {
                return null;
            }

            @Override
            public ZoneId getZoneId() {
                return null;
            }

            @Override
            public Character trueChar() {
                return null;
            }

            @Override
            public Character falseChar() {
                return null;
            }
        };
    }

    /**
     * Verify that the map returned by {@code getSupportedConversions()} is populated and contains a known
     * built‑in conversion (String → Integer).
     */
    @Test
    void builtInConversionsAreReported() {
        Map<String, Set<String>> supported = converter.getSupportedConversions();
        assertNotNull(supported, "The returned map must not be null");
        assertFalse(supported.isEmpty(), "The map should contain at least one conversion");
        // The built‑in conversion from String to Integer is part of the library.
        // The keys and values are stored as class names (full canonical names).
        String source = String.class.getName();
        String target = Integer.class.getName();
        assertTrue(supported.containsKey(source), "Supported conversions should contain the source type " + source);
        assertTrue(supported.get(source).contains(target), "Conversion from String to Integer should be listed for source " + source);
    }

    /**
     * Verify that a user‑defined conversion added via {@link Converter#addConversion(Class, Class, Convert)}
     * appears in the result of {@code getSupportedConversions()}.
     */
    @Test
    void userDefinedConversionIsReported() {
        // Define a trivial conversion from Boolean to String.
        Convert<Boolean> boolToString = (source) -> source.toString();
        // Register the conversion.
        converter.addConversion(Boolean.class, String.class, boolToString);
        Map<String, Set<String>> supported = converter.getSupportedConversions();
        assertNotNull(supported, "The returned map must not be null after adding a user conversion");
        assertTrue(supported.containsKey(Boolean.class.getName()), "User conversion source type should be present");
        assertTrue(supported.get(Boolean.class.getName()).contains(String.class.getName()), "User conversion target type should be present");
    }

    /**
     * Verify that a freshly created {@link Converter} (no user conversions)
     * returns a non‑null, non‑empty map that only contains the built‑in entries.
     */
    @Test
    void testGetSupportedConversions_NoUserConversions() {
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertNotNull(map, "Map must never be null");
        assertFalse(map.isEmpty(), "Map should contain built‑in conversions");
        // Pick a class that is definitely not part of the built‑in set.
        String bogusSource = "com.cedarsoftware.util.convert.DoesNotExist";
        assertFalse(map.containsKey(bogusSource), "Map must not contain entries for user‑defined sources that were never added");
    }

    /**
     * Add a user conversion that already exists as a built‑in conversion
     * (String → Integer) and verify that the entry is present.
     * The map stores fully‑qualified class names.
     */
    @Test
    void testGetSupportedConversions_SingleUserConversion() {
        // trivial conversion
        Convert<String> identity = (src) -> src;
        converter.addConversion(String.class, Integer.class, identity);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        assertTrue(map.containsKey(String.class.getName()), "Map must contain the source key for java.lang.String");
        Set<String> targets = map.get(String.class.getName());
        assertNotNull(targets, "Target set must not be null");
        assertTrue(targets.contains(Integer.class.getName()), "Target set must contain java.lang.Integer");
        // The built‑in conversion already exists, so the size is at least 1.
        assertTrue(targets.size() >= 1, "There must be at least one target for String");
    }

    /**
     * Add two different targets for the same source (String → Integer, String → Long)
     * and verify that both appear in the target set without duplication.
     */
    @Test
    void testGetSupportedConversions_MultipleTargetsSameSource() {
        Convert<String> toInt = (src) -> Integer.valueOf(src);
        Convert<String> toLong = (src) -> Long.valueOf(src);
        converter.addConversion(String.class, Integer.class, toInt);
        converter.addConversion(String.class, Long.class, toLong);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        Set<String> targets = map.get(String.class.getName());
        assertNotNull(targets, "Target set for String must exist");
        assertEquals(2, targets.size(), "String source should map to exactly two distinct targets");
        assertTrue(targets.contains(Integer.class.getName()));
        assertTrue(targets.contains(Long.class.getName()));
    }

    /**
     * Verify that distinct source classes are represented as separate keys
     * and that the underlying {@link TreeMap} orders them case‑insensitively.
     */
    @Test
    void testGetSupportedConversions_DistinctSources() {
        // Two dummy source classes.
        class AClass {
        }
        class BClass {
        }
        Convert<AClass> aToString = (src) -> "a";
        Convert<BClass> bToString = (src) -> "b";
        converter.addConversion(AClass.class, String.class, aToString);
        converter.addConversion(BClass.class, String.class, bToString);
        Map<String, Set<String>> map = converter.getSupportedConversions();
        // Expected keys in case‑insensitive order.
        List<String> expectedOrder = List.of(AClass.class.getName(), BClass.class.getName());
        List<String> actualOrder = new ArrayList<>(map.keySet());
        assertEquals(expectedOrder, actualOrder, "TreeMap should keep keys in natural (case‑insensitive) order");
        assertTrue(map.get(AClass.class.getName()).contains(String.class.getName()));
        assertTrue(map.get(BClass.class.getName()).contains(String.class.getName()));
    }

    /**
     * Ensure the map returned by {@code getSupportedConversions()} is a defensive copy.
     * Mutating the returned map must not affect subsequent calls.
     */
    @Test
    void testGetSupportedConversions_Immutability() {
        Map<String, Set<String>> first = converter.getSupportedConversions();
        // Attempt to corrupt the map – this should not affect the Converter's internal state.
        first.put("X", Set.of("Y"));
        Map<String, Set<String>> second = converter.getSupportedConversions();
        assertFalse(second.containsKey("X"), "Subsequent call must not see the artificial entry added to the previous map");
        assertNotSame(first, second, "Each call should return a new map instance");
    }

    /**
     * After adding user conversions, creating a brand‑new {@link Converter}
     * instance must yield a map that only contains the static built‑in conversions.
     */
    @Test
    void testGetSupportedConversions_EmptyWhenAllUserConversionsRemoved() {
        // Add a couple of user conversions.
        Convert<String> toInt = (src) -> Integer.valueOf(src);
        Convert<String> toLong = (src) -> Long.valueOf(src);
        converter.addConversion(String.class, Integer.class, toInt);
        converter.addConversion(String.class, Long.class, toLong);
        Map<String, Set<String>> withUser = converter.getSupportedConversions();
        assertTrue(withUser.get(String.class.getName()).contains(Integer.class.getName()));
        assertTrue(withUser.get(String.class.getName()).contains(Long.class.getName()));
        // New instance – no user DB entries.
        Converter fresh = new Converter(defaultOptions());
        Map<String, Set<String>> onlyBuiltIn = fresh.getSupportedConversions();
        // The fresh map must not contain the user‑added targets.
        Set<String> freshTargets = onlyBuiltIn.get(String.class.getName());
        assertNotNull(freshTargets, "String key must still exist because of built‑in conversions");
        assertFalse(freshTargets.contains(Integer.class.getName()), "Fresh instance must not contain user‑added Integer target");
        assertFalse(freshTargets.contains(Long.class.getName()), "Fresh instance must not contain user‑added Long target");
    }

    /**
     * Adding a conversion with a {@code null} conversion function must throw
     * a {@link NullPointerException}. The exception is expected at the time of
     * registration (the library validates the argument).
     */
    @Test
    void testGetSupportedConversions_NullConversionFunction() {
        assertThrows(NullPointerException.class, () -> converter.addConversion(String.class, Integer.class, null), "Adding a conversion with a null function should raise NPE");
    }
}
