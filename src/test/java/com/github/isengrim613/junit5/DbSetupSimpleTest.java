package com.github.isengrim613.junit5;

import com.ninja_squad.dbsetup.operation.Operation;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Optional;
import java.util.stream.Stream;

import static com.github.isengrim613.junit5.TestUtilities.assertDataSourceHasRows;
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
        Operation insert3 = sequenceOf(
                deleteAllFrom("My_Table"),
                insertInto("My_Table")
                        .columns("primary_key", "my_value")
                        .values(2, "3")
                        .build());

        @Test
        void shouldHaveRowsFromOperation_1() throws Exception {
            assertDataSourceHasRows(DATA_SOURCE_1, Pair.of(2, "3"));
        }

        @Test
        void shouldHaveRowsFromOperation_2() throws Exception {
            assertDataSourceHasRows(DATA_SOURCE_1, Pair.of(2, "3"));
        }
    }
}