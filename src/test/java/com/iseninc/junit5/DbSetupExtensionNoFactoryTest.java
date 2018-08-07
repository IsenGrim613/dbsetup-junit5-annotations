package com.iseninc.junit5;

import com.iseninc.junit5.datasource.DbSetupExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(DbSetupExtension.class)
class DbSetupExtensionNoFactoryTest {
    @Test
    void shouldDoNothingIfNoFactoryIsDefined() {
        assertTrue(true);
    }
}