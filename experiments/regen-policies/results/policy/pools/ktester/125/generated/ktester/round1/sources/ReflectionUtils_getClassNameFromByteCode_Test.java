package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Stream;

@ExtendWith(MockitoExtension.class)
class ReflectionUtils_getClassNameFromByteCode_Test {

    /**
     * Simple class used for generating a valid byte‑code array.
     */
    static class Sample {
        // empty – only its .class file matters.
    }

    /**
     * Simple public class – used for the “validSimpleClass” group.
     */
    static class Simple {
        // no members required
    }

    /**
     * Outer/Inner pair – used for the “validInnerClassName” group.
     */
    static class Outer {

        static class Inner {
        }
    }

    static class Inner {
    }

    static class Inner {
    }

    /**
     * Class that defines a static final long constant – used for “classWithLongConstant”.
     */
    static class WithLong {

        static final long VALUE = 0x1122334455667788L;
    }

    /* --------------------------------------------------------------------- */
    private static byte[] classBytes(Class<?> clazz) throws IOException {
        String resourceName = clazz.getSimpleName() + ".class";
        try (InputStream is = clazz.getResourceAsStream(resourceName)) {
            assertNotNull(is, "Unable to locate class file for " + clazz.getName());
            return is.readAllBytes();
        }
    }

    /* --------------------------------------------------------------------- */
    private static byte[] compileAndGetBytes(String qualifiedClassName, String source) throws IOException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        assertNotNull(compiler, "JavaCompiler not available – tests must run on a JDK, not a JRE.");
        // In‑memory source file
        SimpleJavaFileObject sourceFile = new SimpleJavaFileObject(URI.create("string:///" + qualifiedClassName.replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE) {

            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return source;
            }
        };
        // Destination directory (temporary)
        Path outputDir = Files.createTempDirectory("compile-test");
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, Collections.singletonList(outputDir.toFile()));
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, null, null, null, Collections.singletonList(sourceFile));
            Boolean result = task.call();
            assertTrue(result, "Compilation failed for " + qualifiedClassName);
        }
        // Read the generated .class file
        String classFilePath = qualifiedClassName.replace('.', '/') + ".class";
        Path classFile = outputDir.resolve(classFilePath);
        byte[] bytes = Files.readAllBytes(classFile);
        // Cleanup temporary directory
        Files.walk(outputDir).sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
        return bytes;
    }

    /**
     * Provides byte arrays for the malformed‑input groups.
     */
    static Stream<Arguments> malformedByteArrays() {
        return Stream.of(// invalidConstantPoolTag – tag 99 (undefined)
        Arguments.of(invalidTagByteArray(), IllegalStateException.class, "Unrecognized constant pool tag: 99"), // emptyByteArray – nothing at all
        Arguments.of(new byte[0], EOFException.class, null), // negativeArraySizeDueToZeroConstantPool – constant_pool_count = 0
        Arguments.of(zeroConstantPoolByteArray(), NegativeArraySizeException.class, null), // arrayIndexOutOfBoundsInsufficientConstantPool – count=1, this_class=1
        Arguments.of(insufficientConstantPoolByteArray(), ArrayIndexOutOfBoundsException.class, null), // nullClassNameString – CLASS entry points to INTEGER entry
        Arguments.of(nullClassNameByteArray(), NullPointerException.class, null), // malformedUtf8String – UTF8 length larger than remaining bytes
        Arguments.of(malformedUtf8ByteArray(), UTFDataFormatException.class, null));
    }

    /**
     * Magic number + minor + major + constant_pool_count=2 (cpcnt=1) + tag 99
     */
    private static byte[] invalidTagByteArray() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)) {
            // magic
            dos.writeInt(0xCAFEBABE);
            // minor
            dos.writeShort(0);
            // major (Java 8)
            dos.writeShort(52);
            // constant_pool_count = 2 (cpcnt = 1)
            dos.writeShort(2);
            // invalid tag
            dos.writeByte(99);
            // dummy bytes to keep stream consistent
            dos.writeShort(0);
            // access flags
            dos.writeShort(0);
            // this_class (points to entry 1)
            dos.writeShort(1);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Header only with constant_pool_count = 0 → cpcnt = -1
     */
    private static byte[] zeroConstantPoolByteArray() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(0xCAFEBABE);
            dos.writeShort(0);
            dos.writeShort(52);
            // constant_pool_count = 0
            dos.writeShort(0);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * constant_pool_count = 1 (cpcnt=0) but this_class = 1 → out‑of‑bounds
     */
    private static byte[] insufficientConstantPoolByteArray() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(0xCAFEBABE);
            dos.writeShort(0);
            dos.writeShort(52);
            // constant_pool_count = 1
            dos.writeShort(1);
            // no constant‑pool entries
            // access flags
            dos.writeShort(0);
            // this_class = 1 (invalid)
            dos.writeShort(1);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * CLASS entry (index 1) points to INTEGER entry (index 2) → strings[1] stays null
     */
    private static byte[] nullClassNameByteArray() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(0xCAFEBABE);
            dos.writeShort(0);
            dos.writeShort(52);
            // constant_pool_count = 3 (cpcnt = 2)
            dos.writeShort(3);
            // entry 1: CONSTANT_CLASS -> string_index = 2
            dos.writeByte(ReflectionUtils.CONSTANT_CLASS);
            dos.writeShort(2);
            // entry 2: CONSTANT_INTEGER (not a UTF8)
            dos.writeByte(ReflectionUtils.CONSTANT_INTEGER);
            dos.writeInt(12345);
            // access flags + this_class = 1 (points to entry 1)
            dos.writeShort(0);
            dos.writeShort(1);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * UTF8 entry with length 10 but only 1 byte of payload
     */
    private static byte[] malformedUtf8ByteArray() {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DataOutputStream dos = new DataOutputStream(baos)) {
            dos.writeInt(0xCAFEBABE);
            dos.writeShort(0);
            dos.writeShort(52);
            // constant_pool_count = 2 (cpcnt = 1)
            dos.writeShort(2);
            // entry 1: CONSTANT_UTF8 with bogus length
            dos.writeByte(ReflectionUtils.CONSTANT_UTF8);
            // length = 10
            dos.writeShort(10);
            // only 1 byte provided
            dos.writeByte('A');
            // access flags + this_class (won't be reached)
            dos.writeShort(0);
            dos.writeShort(1);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    void getClassNameFromByteCode_validClass() throws IOException {
        // GIVEN – a valid byte‑code array for the inner class Sample
        byte[] byteCode = classBytes(Sample.class);
        // WHEN – the utility extracts the class name
        String extractedName = ReflectionUtils.getClassNameFromByteCode(byteCode);
        // THEN – the fully‑qualified name matches the actual class name
        assertEquals(Sample.class.getName(), extractedName);
    }

    @Test
    void getClassNameFromByteCode_invalidByteArray_throwsIOException() {
        // GIVEN – an obviously malformed byte array (too short to contain a valid class file)
        byte[] malformed = new byte[] { 0x00, 0x01, 0x02 };
        // WHEN / THEN – the method should raise an IOException (EOFException is a subclass)
        assertThrows(IOException.class, () -> ReflectionUtils.getClassNameFromByteCode(malformed));
    }

    @Test
    void getClassNameFromByteCode_validSimpleClass() throws IOException {
        byte[] byteCode = classBytes(Simple.class);
        String name = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(Simple.class.getName(), name);
    }

    @Test
    void getClassNameFromByteCode_validInnerClassName() throws IOException {
        byte[] byteCode = classBytes(Outer.Inner.class);
        String name = ReflectionUtils.getClassNameFromByteCode(byteCode);
        // The class file stores the name with '/' which is converted to '.'
        // Inner class separator '$' is preserved.
        assertEquals(Outer.Inner.class.getName(), name);
    }

    @Test
    void getClassNameFromByteCode_classWithLongConstant() throws IOException {
        byte[] byteCode = classBytes(WithLong.class);
        String name = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(WithLong.class.getName(), name);
    }

    @Test
    void getClassNameFromByteCode_classWithUnicodeName() throws Exception {
        // Compile a class in package "café" with name "Test"
        String qualifiedName = "café.Test";
        String source = "package café; public class Test {}";
        byte[] byteCode = compileAndGetBytes(qualifiedName, source);
        String extracted = ReflectionUtils.getClassNameFromByteCode(byteCode);
        assertEquals(qualifiedName, extracted);
    }

    /* --------------------------------------------------------------------- */
    @ParameterizedTest(name = "{index}: {2}")
    @MethodSource("malformedByteArrays")
    void getClassNameFromByteCode_malformedInputs(byte[] byteCode, Class<? extends Throwable> expectedException, String expectedMessage) {
        // Null byte array case is handled separately because the method will NPE before any I/O.
        if (byteCode == null) {
            assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassNameFromByteCode(null));
            return;
        }
        Throwable thrown = assertThrows(expectedException, () -> ReflectionUtils.getClassNameFromByteCode(byteCode));
        if (expectedMessage != null) {
            assertTrue(thrown.getMessage().contains(expectedMessage), () -> "Expected message to contain [" + expectedMessage + "] but was [" + thrown.getMessage() + "]");
        }
    }

    @Test
    void getClassNameFromByteCode_nullByteArray_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> ReflectionUtils.getClassNameFromByteCode(null));
    }
}
