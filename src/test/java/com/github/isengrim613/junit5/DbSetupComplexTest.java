package com.github.isengrim613.junit5;

import com.ninja_squad.dbsetup.bind.Binder;
import com.ninja_squad.dbsetup.bind.BinderConfiguration;
import com.ninja_squad.dbsetup.bind.Binders;
import com.ninja_squad.dbsetup.operation.Operation;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import java.sql.ParameterMetaData;
import java.sql.SQLException;

import static com.github.isengrim613.junit5.TestUtilities.*;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;

@DbSetup
class DbSetupComplexTest {
    @DbSetupSource
    private static final DataSource DATA_SOURCE_1;

    @DbSetupSource(name = "source 2")
    private static final DataSource DATA_SOURCE_2;

    @DbSetupBinderConfiguration(sources = "source 2")
    private static final BinderConfiguration BINDER_CONFIGURATION_2 = new MySimpleBinderConfiguration();

    static {
        HikariConfig config1 = new HikariConfig();
        config1.setJdbcUrl("jdbc:h2:mem:TestTable_1;TRACE_LEVEL_FILE=2;MODE=MYSQL;IGNORECASE=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-database-create.sql';");
        DATA_SOURCE_1 = new HikariDataSource(config1);

        HikariConfig config2 = new HikariConfig();
        config2.setJdbcUrl("jdbc:h2:mem:TestTable_2;TRACE_LEVEL_FILE=2;MODE=MYSQL;IGNORECASE=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-database-create.sql';");
        DATA_SOURCE_2 = new HikariDataSource(config2);
    }

    @DbSetupOperation(order = 1, sources = { "DEFAULT", "source 2" })
    Operation deleteAll = deleteAllFrom("My_Table");

    @DbSetupOperation(order = 2, sources = "DEFAULT")
    Operation insert_1 = insertInto("My_Table")
            .columns("primary_key", "my_value")
            .values(1, "2")
            .build();

    @DbSetupOperation(order = 2, sources = "source 2")
    Operation insert_2 = insertInto("My_Table")
            .columns("primary_key", "my_value")
            .values(11, "22")
            .build();

    @Test
    @DbSetupSkipNext
    void shouldHave1RowAtStart() throws Exception {
        assertDataSourceHasRow(DATA_SOURCE_1, 1, "2");
        assertDataSourceHasRow(DATA_SOURCE_2, 11, "22");
    }

    @Nested
    class Inner {
        @DbSetupOperation(sources = { "DEFAULT", "source 2" })
        Operation insert3 = insertInto("My_Table").columns("primary_key", "my_value").values(2, "3").build();

        @Test
        @DbSetupSkipNext
        void shouldHave2RowsAfter() throws Exception {
            assertDataSourceOnlyHasRows(DATA_SOURCE_1, Pair.of(1, "2"), Pair.of(2, "3"));
            assertDataSourceOnlyHasRows(DATA_SOURCE_2, Pair.of(11, "22"), Pair.of(2, "3"));
        }

        @Test
        void shouldBeAbleToInsert() throws Exception {
            insertRow(DATA_SOURCE_1, 3, "4");
            insertRow(DATA_SOURCE_2, 3, "4");
        }

        @Test
        @DbSetupSkipNext
        void shouldStillHave2RowsAfter() throws Exception {
            shouldHave2RowsAfter();
        }
    }

    private static class MySimpleBinderConfiguration implements BinderConfiguration {

        @Override
        public Binder getBinder(ParameterMetaData metadata, int param) throws SQLException {
            return Binders.defaultBinder();
        }
    }
}