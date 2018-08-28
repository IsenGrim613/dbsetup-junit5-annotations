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

import static com.ninja_squad.dbsetup.Operations.deleteAllFrom;
import static com.ninja_squad.dbsetup.Operations.insertInto;
import static org.assertj.core.api.Assertions.assertThat;

@DbSetup
@SuppressWarnings("unchecked")
class DbSetupTest {
    @DbSetupSource
    private static final DataSource DATA_SOURCE_1;

    @DbSetupSource(name = "source 2")
    private static final DataSource DATA_SOURCE_2;

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
            assertDataSourceHasRows(DATA_SOURCE_1, Pair.of(1, "2"), Pair.of(2, "3"));
            assertDataSourceHasRows(DATA_SOURCE_2, Pair.of(11, "22"), Pair.of(2, "3"));
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

    private static void insertRow(DataSource dataSource, Integer key, String value) throws Exception {
        // arrange
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();

        // act
        boolean result = statement.execute("insert into My_Table VALUES (" + key + ", '" + value + "')");

        // assert
        assertThat(result).isFalse();
    }

    private static void assertDataSourceHasRow(DataSource dataSource, Integer key, String value) throws Exception {
        // arrange
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();

        // act
        boolean result = statement.execute("select my_value from My_Table where primary_key = " + key);
        ResultSet resultSet = statement.getResultSet();

        // assert
        assertThat(result).isTrue();

        assertThat(resultSet.next()).isTrue();
        assertThat(resultSet.getString("my_value")).isEqualTo(value);
    }

    private static void assertDataSourceHasRows(DataSource dataSource, Pair<Integer, String>... rows) throws Exception {
        // arrange
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();

        // act
        boolean result = statement.execute("select primary_key, my_value from My_Table");
        ResultSet resultSet = statement.getResultSet();

        // assert
        assertThat(result).isTrue();

        while(resultSet.next()) {
            int key = resultSet.getInt("primary_key");

            Optional<Pair<Integer, String>> first = Stream.of(rows)
                    .filter(row -> row.getLeft() == key)
                    .findFirst();

            assertThat(first.isPresent()).isTrue();
            assertThat(resultSet.getString("my_value")).isEqualTo(first.get().getRight());
        }
    }

    private static class Pair<L, R> {
        private L left;
        private R right;

        public L getLeft() {
            return left;
        }

        public R getRight() {
            return right;
        }

        private static <L, R> Pair<L, R> of(L left, R right) {
            Pair<L, R> pair = new Pair<>();
            pair.left = left;
            pair.right = right;

            return pair;
        }
    }
}