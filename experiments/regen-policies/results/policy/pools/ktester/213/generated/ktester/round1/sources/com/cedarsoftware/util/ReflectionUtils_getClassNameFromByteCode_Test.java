package com.cedarsoftware.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

// import org.junit.After;
// import org.junit.Before;
// import org.junit.Test;
// import static org.junit.Assert.*;
class ReflectionUtils_getClassNameFromByteCode_Test {

    /**
     * Byte‑code of a simple inner class used for the happy‑path test.
     */
    private byte[] validClassBytes;

    /**
     * Simple class whose byte‑code is read in {@link #setupBeforeEach()}.
     */
    static class SampleClass {
        // No members – the content is irrelevant for the test.
    }

    //     @BeforeEach
    void setupBeforeEach() throws IOException {
        // The inner class file name is based on the current outer class name.
        String resourceName = "ReflectionUtilsClassNameFromByteCodeSpec$SampleClass.class";
        try (InputStream is = getClass().getResourceAsStream(resourceName)) {
            // If the resource cannot be found we would normally fail the test,
            // but for this utility class we simply let a NullPointerException surface.
            validClassBytes = is.readAllBytes();
        }
    }

    //     @AfterEach
    void teardownAfterEach() {
        validClassBytes = null;
    }

    /* --------------------------------------------------------------
     *  Helper – Base64 decoder
     * -------------------------------------------------------------- */
    private static byte[] decode(String base64) {
        return Base64.getDecoder().decode(base64);
    }

    /**
     * Demonstrates how {@link ReflectionUtils#getClassNameFromByteCode(byte[])} can be
     * invoked with a real class file.
     *
     * @throws IOException if the class file cannot be read
     */
    void exampleUsage() throws IOException {
        // Load the SampleClass byte‑code.
        setupBeforeEach();
        // Extract the class name.
        String className = ReflectionUtils.getClassNameFromByteCode(validClassBytes);
        // For demonstration purposes we simply print the result.
        System.out.println("Extracted class name: " + className);
    }

    /**
     * Decodes a Base64‑encoded class file that represents {@code com.example.Test}.
     * This method is retained as a reference for developers who need handcrafted
     * byte‑code samples.
     *
     * @return the decoded byte array
     */
    static byte[] sampleByteCode() {
        return decode("yv76vgAAADQABwAFAABjb20vZXhhbXBsZS9UZXN0AAEAAA==");
    }
}
