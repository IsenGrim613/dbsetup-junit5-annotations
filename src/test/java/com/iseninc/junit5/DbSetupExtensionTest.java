package com.iseninc.junit5;

import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DbSetup
class DbSetupExtensionTest {
    private static DataSource mockDataSource;

    @BeforeAll
    static void setupAll() {
        mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);
    }

    @DbSetupSourceFactory
    static DataSource getMockDataSource() {
        return mockDataSource;
    }

    private Operation mockOperation1 = mock(Operation.class, RETURNS_DEEP_STUBS);

    @DbSetupOperation
    Operation setupOperation1() {
        return mockOperation1;
    }

    @Test
    void shouldRunAllOperations() throws Exception {
        verify(mockOperation1).execute(any(), any());
    }

    @Test
    @DbSetupSkip
    void shouldNotRunOperationWhenSkipAnnotationIsPresent() throws Exception {
        verifyZeroInteractions(mockOperation1);
    }

    @Nested
    class WithInnerClass {
        private Operation mockOperation2 = mock(Operation.class, RETURNS_DEEP_STUBS);

        @DbSetupOperation
        Operation setupOperation2() {
            return mockOperation2;
        }

        @Test
        void shouldRunAllOperations() throws Exception {
            verify(mockOperation1).execute(any(), any());
            verify(mockOperation2).execute(any(), any());
        }
    }
}