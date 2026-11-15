package com.cedarsoftware.util.convert;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.junit.jupiter.api.Assertions.*;
import com.cedarsoftware.util.CompactLinkedMap;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import java.net.URI;
import java.net.URL;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Stream;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class MapConversions_toStringBuilder_Test {

    /**
     * The {@link Converter} used in the tests.
     */
    private Converter converter;

    /**
     * Real {@link Converter} used for the majority of tests (behaves as a no‑op).
     */
    private Converter realConverter;

    /**
     * A {@link Converter} that converts {@link LocalDate} and {@link Instant}
     *  to their ISO‑8601 {@code String} representation, delegating everything
     *  else to the normal conversion logic.
     */
    private static class IsoStringConverter extends Converter {

        IsoStringConverter() {
            super(new ConverterOptions());
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            if (from instanceof LocalDate && toClass == String.class) {
                // e.g. 2023-05-20
                return (T) from.toString();
            }
            if (from instanceof Instant && toClass == String.class) {
                // e.g. 2021-01-01T00:00:00Z
                return (T) ((Instant) from).toString();
            }
            return super.convert(from, toClass);
        }
    }

    /**
     * A {@link Converter} that always throws {@link IllegalArgumentException}.
     */
    private static class ExceptionThrowingConverter extends Converter {

        ExceptionThrowingConverter() {
            super(new ConverterOptions());
        }

        @Override
        public <T> T convert(Object from, Class<T> toClass) {
            throw new IllegalArgumentException("forced exception");
        }
    }

    /**
     * Simple POJO used for the bean‑to‑map test.
     */
    public static class Person {

        private final String name;

        private final int age;

        private final boolean active;

        public Person(String name, int age, boolean active) {
            this.name = name;
            this.age = age;
            this.active = active;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public boolean isActive() {
            return active;
        }
    }

    /**
     * Create a real {@link Converter} with default options before each test.
     */
    @BeforeEach
    void setupBeforeEach() {
        // Converter requires a ConverterOptions instance; the default constructor provides a sensible default.
        converter = new Converter(new ConverterOptions());
        realConverter = new Converter(new ConverterOptions());
    }

    /**
     * No special clean‑up is required after each test.
     */
    @AfterEach
    void teardownAfterEach() {
        // Explicitly null out the reference to help the GC and to make the intent clear.
        converter = null;
    }

    static Stream<Arguments> atomicValues() {
        return Stream.of(Arguments.of(new AtomicBoolean(true), "true"), Arguments.of(new AtomicInteger(123), "123"), Arguments.of(new AtomicLong(9876543210L), "9876543210"));
    }

    /**
     * Verify that {@code toStringBuilder} returns a {@link StringBuilder}
     * containing the string representation of a simple map that uses the
     * {@code "value"} key – the convention used by {@link MapConversions}
     * for scalar conversions.
     */
    @Test
    void toStringBuilderReturnsBuilderWithValueFromMap() {
        // GIVEN – a map with a single entry that the conversion logic recognises
        CompactLinkedMap<String, Object> map = new CompactLinkedMap<>();
        map.put(MapConversions.VALUE, "hello world");
        // WHEN – the method under test is invoked
        StringBuilder sb = MapConversions.toStringBuilder(map, converter);
        // THEN – a non‑null StringBuilder is returned and it contains the expected text
        assertNotNull(sb, "The returned StringBuilder should not be null");
        String result = sb.toString();
        assertTrue(result.contains("hello world"), () -> "Resulting string should contain the original value; got: " + result);
    }

    /**
     * Verify that {@code toStringBuilder} works with a plain {@link String}
     * (i.e., when the source object is not a map). The implementation should
     * treat the object as a scalar and produce a builder containing its
     * {@code toString()} value.
     */
    @Test
    void toStringBuilderHandlesPlainString() {
        // GIVEN – a plain string value
        String source = "just a string";
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(sb, "The returned StringBuilder should not be null");
        assertEquals(source, sb.toString(), "When the source is a plain String, the builder should contain exactly that string");
    }

    /**
     * Verify that {@code toStringBuilder} returns an empty builder when the
     * source is {@code null}. The contract of the conversion utilities is to
     * return {@code null} for most reference types, but for a {@link StringBuilder}
     * the implementation creates an empty instance.
     */
    @Test
    void toStringBuilderReturnsEmptyBuilderForNull() {
        // GIVEN – a null source
        Object source = null;
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(source, converter);
        // THEN
        assertNotNull(sb, "A non‑null StringBuilder should be returned for a null source");
        assertEquals(0, sb.length(), "The builder should be empty when the source is null");
    }

    @Test
    void mapInputConversion() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 123);
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(map, realConverter);
        // THEN
        assertNotNull(sb);
        String result = sb.toString();
        // order of entries is not guaranteed – check both key/value pairs are present
        assertTrue(result.contains("key1=value1"));
        assertTrue(result.contains("key2=123"));
        assertTrue(result.startsWith("{") && result.endsWith("}"));
    }

    @Test
    void pojoConversion() {
        // GIVEN
        Person p = new Person("John Doe", 30, true);
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(p, realConverter);
        // THEN
        assertNotNull(sb);
        String result = sb.toString();
        assertTrue(result.contains("name=John Doe"));
        assertTrue(result.contains("age=30"));
        assertTrue(result.contains("active=true"));
        assertTrue(result.startsWith("{") && result.endsWith("}"));
    }

    @Test
    void nullInputHandling() {
        // GIVEN
        Object source = null;
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(source, realConverter);
        // THEN – the implementation creates an empty builder (no NPE)
        assertNotNull(sb);
        assertEquals(0, sb.length(), "A null source should result in an empty StringBuilder");
    }

    @Test
    void converterTransformsValues_LocalDate() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("date", LocalDate.of(2023, 5, 20));
        Converter isoConverter = new IsoStringConverter();
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(map, isoConverter);
        // THEN
        assertNotNull(sb);
        String result = sb.toString();
        assertTrue(result.contains("date=2023-05-20"), "LocalDate should be converted to its ISO‑8601 string");
    }

    @Test
    void converterThrowsRuntimeException() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("bad", "value");
        Converter throwing = new ExceptionThrowingConverter();
        // WHEN / THEN
        assertThrows(IllegalArgumentException.class, () -> MapConversions.toStringBuilder(map, throwing), "The exception from the converter must propagate");
    }

    @Test
    void collectionInputConversion_List() {
        // GIVEN
        List<String> list = Arrays.asList("a", "b", "c");
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(list, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("[a, b, c]", sb.toString(), "A List should be rendered using the typical list syntax");
    }

    @Test
    void arrayInputConversion_intArray() {
        // GIVEN
        int[] arr = { 1, 2, 3 };
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(arr, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("[1, 2, 3]", sb.toString(), "An int[] should be rendered like a list");
    }

    @Test
    void emptyMapHandling() {
        // GIVEN
        Map<String, Object> empty = new HashMap<>();
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(empty, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("{}", sb.toString(), "An empty map must be rendered as {}");
    }

    @Test
    void emptyCollectionHandling_Set() {
        // GIVEN
        Set<String> emptySet = new HashSet<>();
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(emptySet, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("[]", sb.toString(), "An empty Set should be rendered as []");
    }

    @Test
    void mapWithNullValues() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("a", null);
        map.put("b", 42);
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(map, realConverter);
        // THEN
        assertNotNull(sb);
        String result = sb.toString();
        assertTrue(result.contains("a=null"));
        assertTrue(result.contains("b=42"));
        assertTrue(result.startsWith("{") && result.endsWith("}"));
    }

    @Test
    void nestedMapConversion() {
        // GIVEN
        Map<String, Object> inner = new HashMap<>();
        inner.put("inner", "value");
        Map<String, Object> outer = new HashMap<>();
        outer.put("outer", inner);
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(outer, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("{outer={inner=value}}", sb.toString(), "Nested maps must be rendered recursively");
    }

    @ParameterizedTest
    @MethodSource("atomicValues")
    void atomicTypeConversion(Object atomic, String expected) {
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(atomic, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals(expected, sb.toString(), () -> "Atomic conversion failed for " + atomic.getClass().getSimpleName());
    }

    @Test
    void instantConversionWithCustomConverter() {
        // GIVEN
        // 2021‑01‑01T00:00:00Z
        Instant instant = Instant.ofEpochMilli(1609459200000L);
        Converter isoConverter = new IsoStringConverter();
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(instant, isoConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("2021-01-01T00:00:00Z", sb.toString(), "Instant should be converted to its ISO‑8601 string");
    }

    @Test
    void zoneIdConversionWithoutCustomConverter() {
        // GIVEN
        ZoneId zone = ZoneId.of("Europe/Paris");
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(zone, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("Europe/Paris", sb.toString(), "ZoneId should be rendered using its toString value");
    }

    @Test
    void localeSimpleConversion() {
        // GIVEN
        Locale locale = Locale.US;
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(locale, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("en_US", sb.toString(), "Locale.US should be rendered as en_US");
    }

    @Test
    void localeWithVariantConversion() {
        // GIVEN
        Locale locale = new Locale("fr", "CA", "POSIX");
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(locale, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("fr_CA_POSIX", sb.toString(), "Locale with variant should be rendered correctly");
    }

    @Test
    void uuidConversion() {
        // GIVEN
        UUID uuid = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(uuid, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("123e4567-e89b-12d3-a456-426614174000", sb.toString(), "UUID should be rendered using its standard string form");
    }

    @Test
    void urlConversion() throws Exception {
        // GIVEN
        URL url = new URL("https://example.com/path?query=1");
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(url, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("https://example.com/path?query=1", sb.toString(), "URL should be rendered using its external form");
    }

    @Test
    void uriConversion() {
        // GIVEN
        URI uri = URI.create("mailto:user@example.com");
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(uri, realConverter);
        // THEN
        assertNotNull(sb);
        assertEquals("mailto:user@example.com", sb.toString(), "URI should be rendered using its string representation");
    }

    @Test
    void unsupportedTypeFallsBackToToString() {
        // GIVEN
        Object obj = new Object();
        // WHEN
        StringBuilder sb = MapConversions.toStringBuilder(obj, realConverter);
        // THEN
        assertNotNull(sb);
        String result = sb.toString();
        // The exact hash code is nondeterministic; we only verify that the result
        // starts with the class name and contains an '@' character.
        assertTrue(result.startsWith("java.lang.Object@"), "Fallback should use Object's toString implementation");
    }

    @Test
    void nullConverterThrowsNPE() {
        // GIVEN
        Map<String, Object> map = new HashMap<>();
        map.put("key", "value");
        // WHEN / THEN
        assertThrows(NullPointerException.class, () -> MapConversions.toStringBuilder(map, null), "Passing a null Converter must result in NPE");
    }
}
