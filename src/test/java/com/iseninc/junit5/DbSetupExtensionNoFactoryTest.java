package com.iseninc.junit5;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DbSetupOperationExtension.class)
class DbSetupExtensionNoFactoryTest {
    @Test
    void shouldDoNothingIfNoFactoryIsDefined() {
        assertTrue(true);
    }
}