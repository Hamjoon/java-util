package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

class SystemUtilities_isJavaVersionAtLeast_Test {

    private static int currentMajor;

    private static int currentMinor;

    // saved once for restoration
    private static String originalJavaVersion;

    @BeforeAll
    static void setupBeforeAll() {
        // Capture the real java version – used by the existing tests that rely on the actual JVM.
        String version = System.getProperty("java.version");
        String[] parts = version.split("\\.");
        currentMajor = Integer.parseInt(parts[0]);
        currentMinor = (parts.length > 1) ? Integer.parseInt(parts[1]) : 0;
        // Preserve the original value of the static final field so we can restore it after each test.
        Field f = SystemUtilities.class.getDeclaredField("JAVA_VERSION");
        f.setAccessible(true);
        originalJavaVersion = (String) f.get(null);
    }

    @AfterEach
    void restoreJavaVersion() throws Exception {
        // Reset the mutated field back to its original value after every test method.
        setJavaVersion(originalJavaVersion);
    }

    /* --------------------------------------------------------------------- */
    private static void setJavaVersion(String version) throws Exception {
        Field field = SystemUtilities.class.getDeclaredField("JAVA_VERSION");
        field.setAccessible(true);
        // Remove the final modifier (required for JDK 8+)
        Field modifiers = Field.class.getDeclaredField("modifiers");
        modifiers.setAccessible(true);
        modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);
        field.set(null, version);
    }

    private static Stream<Arguments> validVersionWithMajorMinorProvider() {
        return Stream.of(// major lower
        Arguments.of(10, 5, true), // same major, minor <= actual
        Arguments.of(11, 0, true), // same major, minor > actual
        Arguments.of(11, 1, false));
    }

    private static Stream<Arguments> onlyMajorComponentProvider() {
        return Stream.of(Arguments.of(8, 0, true), Arguments.of(8, 1, false), Arguments.of(7, 5, true), Arguments.of(9, 0, false));
    }

    private static Stream<Arguments> legacyFormatProvider() {
        return Stream.of(Arguments.of(1, 7, true), Arguments.of(1, 8, true), Arguments.of(1, 9, false), Arguments.of(2, 0, false));
    }

    private static Stream<Arguments> invalidVersionProvider() {
        return Stream.of(Arguments.of("invalid"), // trailing dot yields empty second token
        Arguments.of("13."), // empty string
        Arguments.of(""));
    }

    private static Stream<Arguments> extraComponentsProvider() {
        return Stream.of(Arguments.of(9, 2, true), Arguments.of(9, 1, true), Arguments.of(9, 3, false), Arguments.of(8, 5, true), Arguments.of(10, 0, false));
    }

    private static Stream<Arguments> leadingZerosProvider() {
        return Stream.of(Arguments.of(9, 3, true), Arguments.of(9, 4, false), Arguments.of(8, 0, true));
    }

    private static Stream<Arguments> negativeRequestedProvider() {
        return Stream.of(Arguments.of(-1, 0), Arguments.of(11, -1), Arguments.of(-5, -10));
    }

    @Test
    void testCurrentVersionIsAtLeastItself() {
        assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, currentMinor), "Current version should be reported as at least itself");
    }

    @Test
    void testLowerMajorVersionIsAtLeast() {
        // Any lower major version must be considered at least satisfied
        int lowerMajor = currentMajor - 1;
        assertTrue(SystemUtilities.isJavaVersionAtLeast(lowerMajor, 0), "Lower major version should be considered at least");
    }

    @Test
    void testHigherMajorVersionIsNotAtLeast() {
        // Any higher major version must not be satisfied
        int higherMajor = currentMajor + 1;
        assertFalse(SystemUtilities.isJavaVersionAtLeast(higherMajor, 0), "Higher major version should not be considered at least");
    }

    @Test
    void testSameMajorHigherMinorIsAtLeast() {
        // Same major but a higher minor should be true
        // arbitrary increase
        int higherMinor = currentMinor + 5;
        assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, higherMinor), "Same major with higher minor should be considered at least");
    }

    @Test
    void testSameMajorLowerMinorIsNotAtLeast() {
        // Same major but a lower minor should be false (unless currentMinor is 0)
        int lowerMinor = Math.max(0, currentMinor - 1);
        // If currentMinor is already 0, the condition becomes true; handle that case separately
        if (currentMinor == 0) {
            assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, lowerMinor), "When current minor is 0, lower minor (also 0) should be true");
        } else {
            assertFalse(SystemUtilities.isJavaVersionAtLeast(currentMajor, lowerMinor), "Same major with lower minor should not be considered at least");
        }
    }

    @Test
    void testMajorEqualMinorEqualIsAtLeast() {
        // Exact match of major and minor must be true
        assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, currentMinor), "Exact match of major and minor should be true");
    }

    @Test
    void testMajorEqualMinorGreaterIsAtLeast() {
        // When minor is greater than current, should be true
        int greaterMinor = currentMinor + 10;
        assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, greaterMinor), "Same major with a much greater minor should be true");
    }

    @Test
    void testMajorEqualMinorLessIsNotAtLeast() {
        // When minor is less than current, should be false (unless currentMinor is 0)
        int lessMinor = Math.max(0, currentMinor - 2);
        if (currentMinor == 0) {
            // Both are 0, condition is true
            assertTrue(SystemUtilities.isJavaVersionAtLeast(currentMajor, lessMinor), "When current minor is 0, lower minor (also 0) should be true");
        } else {
            assertFalse(SystemUtilities.isJavaVersionAtLeast(currentMajor, lessMinor), "Same major with a lower minor should be false");
        }
    }

    // ---------- Group: Valid version string with major and minor components ----------
    @ParameterizedTest(name = "[{index}] major={0}, minor={1} → {2}")
    @MethodSource("validVersionWithMajorMinorProvider")
    void testValidVersionWithMajorAndMinor(int major, int minor, boolean expected) throws Exception {
        // as described in the JSON group
        setJavaVersion("11.0.2");
        assertEquals(expected, SystemUtilities.isJavaVersionAtLeast(major, minor), "JAVA_VERSION=\"11.0.2\" – expected " + expected);
    }

    // ---------- Group: Version string containing only major component ----------
    @ParameterizedTest(name = "[{index}] major={0}, minor={1} → {2}")
    @MethodSource("onlyMajorComponentProvider")
    void testVersionWithOnlyMajorComponent(int major, int minor, boolean expected) throws Exception {
        setJavaVersion("8");
        assertEquals(expected, SystemUtilities.isJavaVersionAtLeast(major, minor), "JAVA_VERSION=\"8\" – expected " + expected);
    }

    // ---------- Group: Legacy Java version format ("1.x") ----------
    @ParameterizedTest(name = "[{index}] major={0}, minor={1} → {2}")
    @MethodSource("legacyFormatProvider")
    void testLegacyFormatVersion(int major, int minor, boolean expected) throws Exception {
        setJavaVersion("1.8.0_231");
        assertEquals(expected, SystemUtilities.isJavaVersionAtLeast(major, minor), "JAVA_VERSION=\"1.8.0_231\" – expected " + expected);
    }

    // ---------- Group: Invalid version strings causing NumberFormatException ----------
    @ParameterizedTest(name = "[{index}] version=\"{0}\" → NumberFormatException")
    @MethodSource("invalidVersionProvider")
    void testInvalidVersionStrings(String javaVersion) throws Exception {
        setJavaVersion(javaVersion);
        assertThrows(NumberFormatException.class, () -> SystemUtilities.isJavaVersionAtLeast(1, 0), "Expected NumberFormatException for JAVA_VERSION=\"" + javaVersion + "\"");
    }

    // ---------- Group: Version string with extra components ----------
    @ParameterizedTest(name = "[{index}] major={0}, minor={1} → {2}")
    @MethodSource("extraComponentsProvider")
    void testVersionWithExtraComponents(int major, int minor, boolean expected) throws Exception {
        setJavaVersion("9.2.3");
        assertEquals(expected, SystemUtilities.isJavaVersionAtLeast(major, minor), "JAVA_VERSION=\"9.2.3\" – expected " + expected);
    }

    // ---------- Group: Version string with leading zeros ----------
    @ParameterizedTest(name = "[{index}] major={0}, minor={1} → {2}")
    @MethodSource("leadingZerosProvider")
    void testVersionWithLeadingZeros(int major, int minor, boolean expected) throws Exception {
        setJavaVersion("09.03");
        assertEquals(expected, SystemUtilities.isJavaVersionAtLeast(major, minor), "JAVA_VERSION=\"09.03\" – expected " + expected);
    }

    // ---------- Group: Version string with surrounding whitespace ----------
    @Test
    void testVersionWithSurroundingWhitespace() throws Exception {
        // leading/trailing spaces
        setJavaVersion(" 11.0.2 ");
        assertThrows(NumberFormatException.class, () -> SystemUtilities.isJavaVersionAtLeast(11, 0), "Whitespace should cause NumberFormatException");
    }

    // ---------- Group: Negative requested version numbers ----------
    @ParameterizedTest(name = "[{index}] major={0}, minor={1} → true")
    @MethodSource("negativeRequestedProvider")
    void testNegativeRequestedVersions(int major, int minor) throws Exception {
        setJavaVersion("11.0.2");
        assertTrue(SystemUtilities.isJavaVersionAtLeast(major, minor), "Any negative requested version should be satisfied");
    }

    // ---------- Group: Version string with empty major component ----------
    @Test
    void testEmptyMajorComponent() throws Exception {
        setJavaVersion(".1");
        assertThrows(NumberFormatException.class, () -> SystemUtilities.isJavaVersionAtLeast(0, 0), "Empty major component should cause NumberFormatException");
    }

    // ---------- Group: Version string with non‑numeric minor component ----------
    @Test
    void testNonNumericMinorComponent() throws Exception {
        setJavaVersion("11.x");
        assertThrows(NumberFormatException.class, () -> SystemUtilities.isJavaVersionAtLeast(11, 0), "Non‑numeric minor component should cause NumberFormatException");
    }
}
