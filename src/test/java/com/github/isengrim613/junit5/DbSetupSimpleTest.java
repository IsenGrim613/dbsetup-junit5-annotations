package com.github.isengrim613.junit5;

import com.ninja_squad.dbsetup.operation.Operation;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;

import static com.github.isengrim613.junit5.TestUtilities.assertDataSourceOnlyHasRows;
import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static com.ninja_squad.dbsetup.Operations.sequenceOf;
import static org.assertj.core.api.Assertions.assertThat;

@DbSetup
class DbSetupSimpleTest {
    @DbSetupSource
    private static final DataSource DATA_SOURCE_1;

    static {
        HikariConfig config1 = new HikariConfig();
        config1.setJdbcUrl("jdbc:h2:mem:TestTable_1;MODE=MYSQL;IGNORECASE=TRUE;INIT=RUNSCRIPT FROM 'classpath:h2-database-create.sql';");
        DATA_SOURCE_1 = new HikariDataSource(config1);
    }

    @Nested
    class Inner {
        @DbSetupOperation
        Operation insert = sequenceOf(
                deleteAllFrom("My_Table"),
                insertInto("My_Table")
                        .columns("primary_key", "my_value")
                        .values(2, "3")
                        .build());

        @Test
        void shouldHaveRowsFromOperation_1() throws Exception {
            assertDataSourceOnlyHasRows(DATA_SOURCE_1, Pair.of(2, "3"));
        }

        @Test
        void shouldHaveRowsFromOperation_2() throws Exception {
            assertDataSourceOnlyHasRows(DATA_SOURCE_1, Pair.of(2, "3"));
        }
    }

    @Nested
    class Inner2 {
        @DbSetupOperation
        Operation insert = sequenceOf(
                deleteAllFrom("My_Table"),
                insertInto("My_Table")
                        .columns("primary_key", "my_value")
                        .values(3, "4")
                        .build());

        @Test
        void shouldHaveRowsFromOperation_1() throws Exception {
            assertDataSourceOnlyHasRows(DATA_SOURCE_1, Pair.of(3, "4"));
        }

        @Test
        void shouldHaveRowsFromOperation_2() throws Exception {
            assertDataSourceOnlyHasRows(DATA_SOURCE_1, Pair.of(3, "4"));
        }
    }
}