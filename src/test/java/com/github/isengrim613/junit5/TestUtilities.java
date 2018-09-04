package com.github.isengrim613.junit5;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class TestUtilities {
    private TestUtilities() {
        // singleton
    }

    public static void insertRow(DataSource dataSource, Integer key, String value) throws Exception {
        // arrange
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();

        // act
        boolean result = statement.execute("insert into My_Table VALUES (" + key + ", '" + value + "')");

        // assert
        assertThat(result).isFalse();
    }

    public static void assertDataSourceHasRows(DataSource dataSource, Pair<Integer, String>... rows) throws Exception {
        // arrange
        Connection connection = dataSource.getConnection();
        Statement statement = connection.createStatement();

        // act
        boolean result = statement.execute("select primary_key, my_value from My_Table");
        ResultSet resultSet = statement.getResultSet();

        // assert
        assertThat(result).isTrue();

        Set<Pair<Integer, String>> rowsFound = new HashSet<>();
        while(resultSet.next()) {
            int key = resultSet.getInt("primary_key");

            Optional<Pair<Integer, String>> first = Stream.of(rows)
                    .filter(row -> row.getLeft() == key)
                    .findFirst();

            assertThat(first.isPresent()).isTrue();
            assertThat(resultSet.getString("my_value")).isEqualTo(first.get().getRight());

            rowsFound.add(first.get());
        }

        assertThat(rowsFound.size()).isEqualTo(rows.length);
    }

    public static void assertDataSourceHasRow(DataSource dataSource, Integer key, String value) throws Exception {
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
}
