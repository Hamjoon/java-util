package com.cedarsoftware.util.convert;

import java.sql.Timestamp;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDate;
// import org.junit.*;
import java.util.stream.Collectors;

// @ExtendWith(MockitoExtension.class)
class Converter_isSimpleTypeConversionSupported_Test {

    private static Converter converter;

    /**
     * The private static cache method used by {@code Converter}.
     */
    private static Method cacheConverterMethod;

    /**
     * The private static field that holds the {@code UNSUPPORTED} sentinel.
     */
    private static Object UNSUPPORTED_SENTINEL;

    /**
     * Functional interface used for conversion lambdas.
     */
    @FunctionalInterface
    private interface Convert<T> {

        T convert(Object from, Converter conv);
    }

    /**
     * Simple holder for parameterised test data.
     */
    private static final class TestCase {

        final Class<?> src;

        final Class<?> tgt;

        final boolean expected;

        final String description;

        TestCase(Class<?> src, Class<?> tgt, boolean expected, String description) {
            this.src = src;
            this.tgt = tgt;
            this.expected = expected;
            this.description = description;
        }
    }

    private static final class UnsupportedTarget {
    }

//     @BeforeAll
    static void setupBeforeAll() {
        try {
            // The real Converter is needed for its built‑in conversion tables.
            converter = new Converter(new ConverterOptions() {
            });
            // Obtain private static members via reflection once.
            Class<?> clazz = Converter.class;
            cacheConverterMethod = clazz.getDeclaredMethod("cacheConverter", Class.class, Class.class, Convert.class);
            cacheConverterMethod.setAccessible(true);
            Field unsupportedField = clazz.getDeclaredField("UNSUPPORTED");
            unsupportedField.setAccessible(true);
            UNSUPPORTED_SENTINEL = unsupportedField.get(null);
        } catch (NoSuchMethodException | NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Helper that inserts a cached conversion (or the UNSUPPORTED sentinel) for a given pair.
     */
    private static void putCacheEntry(Class<?> src, Class<?> tgt, Convert<?> conversion) {
        try {
            cacheConverterMethod.invoke(null, src, tgt, conversion);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

//     private static Stream<Arguments> cachedConverterProvider() {
//         // 1️⃣ cached successful conversion – any non‑null Convert is considered valid
//         Convert<Object> dummy = (o, c) -> null;
//         putCacheEntry(String.class, Integer.class, dummy);
//         // 2️⃣ cached UNSUPPORTED conversion
//         putCacheEntry(String.class, UnsupportedTarget.class, (Convert<?>) UNSUPPORTED_SENTINEL);
//         return Arrays.asList(new TestCase(String.class, Integer.class, true, "A cached successful converter should be returned without further lookup."), new TestCase(String.class, UnsupportedTarget.class, false, "A cached UNSUPPORTED entry must cause an immediate false result."));
//     }

//     private static Stream<Arguments> directSupportedProvider() {
//         return Arrays.asList(new TestCase(String.class, Integer.class, true, "String → Integer is a built‑in conversion."), new TestCase(Integer.class, String.class, true, "Integer → String is also built‑in."));
//     }

//     private static Stream<Arguments> directNotSupportedProvider() {
//         return Arrays.asList(new TestCase(String.class, UUID.class, false, "String → UUID has no direct or inherited converter."), new TestCase(Boolean.class, LocalDate.class, false, "Boolean → LocalDate is not a supported simple conversion."));
//     }

//     private static Stream<Arguments> numberSpecialCaseProvider() {
//         return Arrays.asList(new TestCase(Number.class, Long.class, true, "Number source delegates to Long → Long which is trivially supported."), new TestCase(Number.class, UUID.class, false, "Number → UUID fails after delegating to Long → UUID."));
//     }

//     private static Stream<Arguments> collectionOrArrayRejectionProvider() {
//         return Arrays.asList(new TestCase(String[].class, Integer.class, false, "Source is an array – should be rejected immediately."), new TestCase(List.class, Set.class, false, "Both source and target are collections – should be rejected."), new TestCase(String.class, Collection.class, false, "Target is a collection interface – should be rejected."));
//     }

//     private static Stream<Arguments> inheritedSupportedProvider() {
//         return Arrays.asList(new TestCase(ArrayList.class, String.class, true, "ArrayList inherits a conversion to String via its List super‑type."), new TestCase(HashMap.class, String.class, true, "Map subclasses gain conversion to String through inheritance."));
//     }

//     private static Stream<Arguments> inheritedNotSupportedProvider() {
//         return Collections.singletonList(new TestCase(ConcurrentHashMap.class, UUID.class, false, "No direct nor inherited conversion exists for this pair."));
//     }

//     private static Stream<Arguments> nullArgumentsProvider() {
//         return Arrays.asList(new TestCase(null, Integer.class, false, "Null source class must cause NPE."), new TestCase(String.class, null, false, "Null target class must cause NPE."), new TestCase(null, null, false, "Both arguments null must cause NPE."));
//     }

//     @Test
//     void simpleSupportedConversion_StringToInteger_ReturnsTrue() {
//         Assert.assertTrue(converter.isSimpleTypeConversionSupported(String.class, Integer.class), "String → Integer should be supported");
//     }

//     @Test
//     void unsupportedConversion_StringToThread_ReturnsFalse() {
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(String.class, Thread.class), "String → Thread is not a supported conversion");
//     }

//     @Test
//     void collectionSource_ReturnsFalse() {
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(List.class, String.class), "Collection source types must be rejected");
//     }

//     @Test
//     void arraySource_ReturnsFalse() {
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(int[].class, String.class), "Array source types must be rejected");
//     }

//     @Test
//     void collectionTarget_ReturnsFalse() {
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(String.class, List.class), "Collection target types must be rejected");
//     }

//     @Test
//     void numberSource_DelegatesViaLong_ReturnsTrue() {
//         // Number → Integer is expected to be resolved via Long → Integer conversion.
//         Assert.assertTrue(converter.isSimpleTypeConversionSupported(Number.class, Integer.class), "Number source should delegate to Long conversion and be supported");
//     }

//     @Test
//     void directConversion_TimestampToDate_ReturnsTrue() {
//         // Timestamp extends Date; a direct conversion exists in the primary DB.
//         Assert.assertTrue(converter.isSimpleTypeConversionSupported(Timestamp.class, java.util.Date.class), "Timestamp → Date should be directly supported");
//     }

//     @Test
//     void inheritedConversion_SubclassToSuperclass_ReturnsTrue() {
//         // java.sql.Timestamp → java.util.Calendar is not a direct conversion,
//         // but Timestamp is a Date, and Date → Calendar is supported via inheritance.
//         Assert.assertTrue(converter.isSimpleTypeConversionSupported(Timestamp.class, java.util.Calendar.class), "Timestamp → Calendar should be resolved via inheritance");
//     }

//     @Test
//     void cachedUnsupportedConversion_ReturnsFalseTwice() {
//         Class<?> src = String.class;
//         // unlikely to be supported
//         Class<?> tgt = java.util.concurrent.BlockingQueue.class;
//         // First call – should compute and cache the failure.
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(src, tgt), "First call should report unsupported conversion");
//         // Second call – should hit the cache and still be false.
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(src, tgt), "Second call should return the cached unsupported result");
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("cachedConverterProvider")
//     void cachedConverterTests(Class<?> src, Class<?> tgt, boolean expected, String description) {
//         for (TestCase tc : cachedConverterProvider()) {
//             boolean actual = converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//             Assert.assertEquals(tc.expected, actual, tc.description);
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("directSupportedProvider")
//     void directSupportedTests(Class<?> src, Class<?> tgt, boolean expected, String description) {
//         for (TestCase tc : directSupportedProvider()) {
//             boolean actual = converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//             Assert.assertEquals(tc.expected, actual, tc.description);
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("directNotSupportedProvider")
//     void directNotSupportedTests(Class<?> src, Class<?> tgt, boolean expected, String description) {
//         for (TestCase tc : directNotSupportedProvider()) {
//             boolean actual = converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//             Assert.assertEquals(tc.expected, actual, tc.description);
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("numberSpecialCaseProvider")
//     void numberSpecialCaseTests(Class<?> src, Class<?> tgt, boolean expected, String description) {
//         for (TestCase tc : numberSpecialCaseProvider()) {
//             boolean actual = converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//             Assert.assertEquals(tc.expected, actual, tc.description);
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("collectionOrArrayRejectionProvider")
//     void collectionOrArrayRejectionTests(Class<?> src, Class<?> tgt, boolean expected, String description) {
//         for (TestCase tc : collectionOrArrayRejectionProvider()) {
//             boolean actual = converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//             Assert.assertEquals(tc.expected, actual, tc.description);
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("inheritedSupportedProvider")
//     void inheritedSupportedTests(Class<?> src, Class<?> tgt, boolean expected, String description) {
//         for (TestCase tc : inheritedSupportedProvider()) {
//             boolean actual = converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//             Assert.assertEquals(tc.expected, actual, tc.description);
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("inheritedNotSupportedProvider")
//     void inheritedNotSupportedTests(Class<?> src, Class<?> tgt, boolean expected, String description) {
//         for (TestCase tc : inheritedNotSupportedProvider()) {
//             boolean actual = converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//             Assert.assertEquals(tc.expected, actual, tc.description);
//         }
//     }

//     @ParameterizedTest(name = "[{index}] {3}")
//     @MethodSource("nullArgumentsProvider")
//     void nullArgumentTests(Class<?> src, Class<?> tgt, Class<? extends Throwable> expectedException, String description) {
//         for (TestCase tc : nullArgumentsProvider()) {
//             try {
//                 converter.isSimpleTypeConversionSupported(tc.src, tc.tgt);
//                 Assert.fail("Expected NullPointerException for: " + tc.description);
//             } catch (NullPointerException expected) {
//                 // expected
//             } catch (Throwable t) {
//                 Assert.fail("Expected NullPointerException but got " + t);
//             }
//         }
//     }

//     @Test
//     void cachedFailureReuse() {
//         Class<?> src = String.class;
//         // already cached as UNSUPPORTED in @BeforeClass
//         Class<?> tgt = UnsupportedTarget.class;
//         // First call – already cached, should be false.
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(src, tgt));
//         // Second call – must still be false (cache hit).
//         Assert.assertFalse(converter.isSimpleTypeConversionSupported(src, tgt));
//     }
}