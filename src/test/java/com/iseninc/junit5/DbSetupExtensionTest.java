package com.iseninc.junit5;

import com.iseninc.junit5.datasource.DbSetupExtension;
import com.iseninc.junit5.datasource.DbSetupOperation;
import com.iseninc.junit5.datasource.DbSetupSkip;
import com.iseninc.junit5.datasource.DbSetupSourceFactory;
import com.ninja_squad.dbsetup.operation.Operation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.sql.DataSource;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(DbSetupExtension.class)
class DbSetupExtensionTest {
    private static DataSource mockDataSource;
    private static Operation mockOperation1;
    private static Operation mockOperation2;

    @BeforeAll
    static void setupAll() {
        mockDataSource = mock(DataSource.class, RETURNS_DEEP_STUBS);
        mockOperation1 = mock(Operation.class, RETURNS_DEEP_STUBS);
        mockOperation2 = mock(Operation.class, RETURNS_DEEP_STUBS);
    }

    @DbSetupSourceFactory
    static DataSource getMockDataSource() {
        return mockDataSource;
    }

    @DbSetupOperation
    static Operation setupOperation1() {
        return mockOperation1;
    }

    @DbSetupOperation
    static Operation setupOperation2() {
        return mockOperation2;
    }

    @Test
    void shouldRunAllOperations() throws Exception {
        verify(mockDataSource).getConnection();
        verify(mockOperation1).execute(any(), any());
        verify(mockOperation2).execute(any(), any());
    }

    @Test
    @DbSetupSkip
    void shouldNotRunOperationWhenSkipAnnotationIsPresent() throws Exception {
        verifyZeroInteractions(mockDataSource);
        verifyZeroInteractions(mockOperation1);
        verifyZeroInteractions(mockOperation2);
    }
}