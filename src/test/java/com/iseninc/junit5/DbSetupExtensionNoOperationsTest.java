package com.iseninc.junit5;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

@ExtendWith(DbSetupOperationExtension.class)
class DbSetupExtensionNoOperationsTest {
    private static DataSource mockDataSource;

    @BeforeAll
    static void setupAll() {
        mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);
    }

    @DbSetupSourceFactory
    static DataSource getMockDataSource() {
        return mockDataSource;
    }

    @Test
    void shouldDoNothingIfNoOperationsAreDefined() {
        assertTrue(true);
    }
}