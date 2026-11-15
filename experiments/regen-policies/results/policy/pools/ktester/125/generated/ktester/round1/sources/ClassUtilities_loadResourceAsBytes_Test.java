package com.cedarsoftware.util;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
// import org.junit.Test;
// import static org.junit.Assert.*;

// @ExtendWith(MockitoExtension.class)
class ClassUtilities_loadResourceAsBytes_Test {

    /**
     * Simple holder for the parameterised test cases.
     */
    private static final class SuccessfulReadCase {

        final String resourceName;

        /**
         * If non‑null the exact byte content is asserted; otherwise only length checks are performed.
         */
        final byte[] expectedContent;

        final String description;

        SuccessfulReadCase(String resourceName, byte[] expectedContent, String description) {
            this.resourceName = resourceName;
            this.expectedContent = expectedContent;
            this.description = description;
        }

        @Override
        public String toString() {
            return description + " [" + resourceName + "]";
        }
    }

    private static Stream<SuccessfulReadCase> successfulReadProvider() {
        return Stream.of(new SuccessfulReadCase("testdata/sample.txt", "Hello World".getBytes(StandardCharsets.UTF_8), "plain text without leading slash"), new SuccessfulReadCase("/testdata/sample.txt", "Hello World".getBytes(StandardCharsets.UTF_8), "plain text with leading slash"), new SuccessfulReadCase("testdata/empty.txt", new byte[0], "empty file"), new SuccessfulReadCase("testdata/binary.bin", null, "binary file – only length asserted (≥1)"), new SuccessfulReadCase("testdata/large.bin", null, "large file – only length asserted (≥50000)"));
    }

    /**
     * Ensures that a {@code null} argument results in a {@link NullPointerException}
     * with the expected message.
     */
//     @Test
//     @DisplayName("loadResourceAsBytes(null) throws NullPointerException")
//     void testLoadResourceNullArgument() {
//         NullPointerException ex = assertThrows(NullPointerException.class, () -> ClassUtilities.loadResourceAsBytes(null));
//         assertEquals("resourceName cannot be null", ex.getMessage(), "Exception message should indicate that the resource name cannot be null.");
//     }
}