package com.cedarsoftware.util;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.lang.reflect.Field;
import java.security.Permission;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

class SystemUtilities_getExternalVariable_Test {

    private static final String TEST_KEY = "test.external.var";

    // Keep a snapshot of the original environment to restore after each test
    private static Map<String, String> originalEnv;

    /**
     * Denies any attempt to read a system property.
     */
    private static class DenyPropertyReadSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPropertyAccess(String key) {
            throw new SecurityException("Property read denied for key: " + key);
        }
    }

    /**
     * Denies any attempt to read environment variables.
     */
    private static class DenyEnvReadSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkExec(String cmd) {
            // Not relevant
        }

        @Override
        public void checkRead(String file) {
            // Not relevant
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No-op
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }
    }

    /**
     * Denies access to environment variables.
     */
    private static class DenyEnvReadSecurityManager extends SecurityManager {

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything except getenv permission
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op for most permissions
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // No‑op
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm) {
            // Allow everything else
        }

        @Override
        public void checkPermission(Permission perm, Object context) {
            // Allow everything else
        }
    }

    @BeforeAll
    static void setupBeforeAll() {
        // Preserve the original environment map (read‑only view)
        originalEnv = new HashMap<>(System.getenv());
    }

    @BeforeEach
    void setupBeforeEach() {
        // Ensure a clean state for system properties
        System.clearProperty(TEST_KEY);
        // Ensure the test key is removed from the environment (if previously added)
        clearEnv(TEST_KEY);
    }

    @AfterEach
    void teardownAfterEach() {
        // Clean up system properties and any environment modifications
        System.clearProperty(TEST_KEY);
        clearEnv(TEST_KEY);
    }

    @AfterAll
    static void teardownAfterAll() {
        // Restore the original environment (best‑effort, may not affect the real OS env)
        setEnvMap(originalEnv);
    }

    /**
     * Group: Property precedence over environment variable
     */
    static Stream<Arguments> propertyPrecedenceProvider() {
        return Stream.of(// Property set, env also set – property wins
        Arguments.of("TEST_VAR", "propertyValue", "envValue", true), // Property set, env missing – property wins
        Arguments.of("TEST_VAR", "propertyValue", null, false));
    }

    /**
     * Group: Fallback to environment variable when property is empty
     */
    static Stream<Arguments> fallbackToEnvProvider() {
        return Stream.of(Arguments.of("TEST_VAR", "", "envValue"));
    }

    /**
     * Inserts or updates a single environment variable using reflection.
     *
     * @param key   variable name
     * @param value variable value
     */
    private static void setEnv(String key, String value) {
        try {
            Map<String, String> newEnv = new HashMap<>(System.getenv());
            newEnv.put(key, value);
            setEnvMap(newEnv);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set env variable via reflection", e);
        }
    }

    /**
     * Removes a single environment variable using reflection.
     *
     * @param key variable name to remove
     */
    private static void clearEnv(String key) {
        try {
            Map<String, String> newEnv = new HashMap<>(System.getenv());
            newEnv.remove(key);
            setEnvMap(newEnv);
        } catch (Exception e) {
            throw new RuntimeException("Failed to clear env variable via reflection", e);
        }
    }

    @Test
    void getExternalVariable_returnsSystemPropertyWhenSet() {
        // given
        String expected = "propertyValue";
        System.setProperty(TEST_KEY, expected);
        // when
        String actual = SystemUtilities.getExternalVariable(TEST_KEY);
        // then
        assertEquals(expected, actual, "Should return the value from System.getProperty");
    }

    @Test
    void getExternalVariable_fallsBackToEnvWhenPropertyMissing() {
        // given
        // Ensure the property is not set
        System.clearProperty(TEST_KEY);
        // The test environment may or may not have the variable set.
        // We retrieve the current value (could be null) and use it as the expected result.
        String expected = System.getenv(TEST_KEY);
        // when
        String actual = SystemUtilities.getExternalVariable(TEST_KEY);
        // then
        assertEquals(expected, actual, "Should return the value from System.getenv when property is absent");
    }

    @Test
    void getExternalVariable_returnsNullWhenNeitherPropertyNorEnvSet() {
        // given
        // Choose a key that is highly unlikely to exist in either properties or environment
        String unknownKey = "non.existent.key.12345";
        System.clearProperty(unknownKey);
        assertNull(System.getenv(unknownKey), "Pre‑condition: env variable must be absent for the test");
        // when
        String actual = SystemUtilities.getExternalVariable(unknownKey);
        // then
        assertNull(actual, "Should return null when both System.getProperty and System.getenv are empty");
    }

    @Test
    void getExternalVariable_treatsEmptyStringAsAbsent() {
        // given
        System.setProperty(TEST_KEY, "");
        // when
        String actual = SystemUtilities.getExternalVariable(TEST_KEY);
        // then
        // An empty string is considered empty by StringUtilities, so the method should fall back
        // to the environment variable (which is likely null in the test environment).
        assertNull(actual, "Empty property value should be treated as absent and result in null");
    }

    @ParameterizedTest(name = "[{index}] var={0}, prop={1}, env={2}")
    @MethodSource("propertyPrecedenceProvider")
    void getExternalVariable_propertyPrecedence(String var, String propertyValue, String envValue, boolean envExists) throws Exception {
        // arrange
        System.setProperty(var, propertyValue);
        if (envExists) {
            setEnv(var, envValue);
        } else {
            clearEnv(var);
        }
        // act
        String actual = SystemUtilities.getExternalVariable(var);
        // assert
        assertEquals(propertyValue, actual, "System property should take precedence over environment variable");
    }

    @ParameterizedTest(name = "[{index}] var={0}, prop=empty, env={2}")
    @MethodSource("fallbackToEnvProvider")
    void getExternalVariable_fallbackToEnvWhenPropertyEmpty(String var, String emptyProperty, String envValue) throws Exception {
        // arrange
        System.setProperty(var, emptyProperty);
        setEnv(var, envValue);
        // act
        String actual = SystemUtilities.getExternalVariable(var);
        // assert
        assertEquals(envValue, actual, "When property is empty, method should fall back to environment variable");
    }

    /**
     * Group: Null argument handling
     */
    @Test
    void getExternalVariable_nullArgument_throwsNPE() {
        assertThrows(NullPointerException.class, () -> SystemUtilities.getExternalVariable(null), "Calling with null should throw NullPointerException");
    }

    /**
     * Group: SecurityException when reading system property
     */
    @Test
    void getExternalVariable_securityExceptionOnPropertyRead() {
        // Install a SecurityManager that denies property reads
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new DenyPropertyReadSecurityManager());
        try {
            assertThrows(SecurityException.class, () -> SystemUtilities.getExternalVariable("SECURE_VAR"), "SecurityException should propagate when property access is denied");
        } finally {
            System.setSecurityManager(original);
        }
    }

    /**
     * Group: SecurityException when reading environment variable
     */
    @Test
    void getExternalVariable_securityExceptionOnEnvRead() {
        // Ensure property is absent so the code reaches getenv()
        System.clearProperty("SECURE_ENV");
        // Install a SecurityManager that denies getenv access
        SecurityManager original = System.getSecurityManager();
        System.setSecurityManager(new DenyEnvReadSecurityManager());
        try {
            assertThrows(SecurityException.class, () -> SystemUtilities.getExternalVariable("SECURE_ENV"), "SecurityException should propagate when getenv access is denied");
        } finally {
            System.setSecurityManager(original);
        }
    }

    /**
     * Replaces the internal unmodifiable environment map with the supplied map.
     *
     * @param newEnv the map that should become the new environment
     * @throws Exception if reflection fails
     */
    @SuppressWarnings("unchecked")
    private static void setEnvMap(Map<String, String> newEnv) throws Exception {
        // For Java 9+ the environment map is stored in the class ProcessEnvironment
        Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
        // The variable that holds the environment map
        Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
        theEnvironmentField.setAccessible(true);
        Map<String, String> env = (Map<String, String>) theEnvironmentField.get(null);
        env.clear();
        env.putAll(newEnv);
        // For Windows, there is also theCaseInsensitiveEnvironment field
        try {
            Field ciEnvField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            ciEnvField.setAccessible(true);
            Map<String, String> ciEnv = (Map<String, String>) ciEnvField.get(null);
            ciEnv.clear();
            ciEnv.putAll(newEnv);
        } catch (NoSuchFieldException ignored) {
            // Not a Windows JVM – ignore
        }
        // For Java 8 and earlier, the unmodifiable map is cached in Collections
        Field unmodifiableMapField = Collections.class.getDeclaredField("unmodifiableMap");
        unmodifiableMapField.setAccessible(true);
        // No need to modify this field directly; the above changes affect the underlying map.
    }
}
