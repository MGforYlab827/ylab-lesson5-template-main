package io.ylab.intensive.lesson05.messagefilter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.*;

import java.util.Scanner;

@Component
public class Database {
    private final String TABLE_NAME = "forbiddenwords";
    private final String FILE_NAME = "forbiddenwords.txt";
    private final String SQL_CREATE_TABLE = "CREATE TABLE forbiddenwords (word CHARACTER VARYING(30));";
    private final String SQL_INSERT_DATA = "INSERT INTO forbiddenwords VALUES (?);";
    private final String SQL_DELETE_ALL_DATA = "DELETE FROM forbiddenwords";
    private final String SQL_CONTAIN_WORD = "SELECT * FROM forbiddenwords WHERE word = ?;";
    private DataSource dataSource;

    @Autowired
    public Database(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void initTable() throws SQLException, IOException {
        if (!isExistTable(TABLE_NAME)){
            applyDdl(SQL_CREATE_TABLE);
        } else {
            applyDdl(SQL_DELETE_ALL_DATA);
        }
        insertDataInTable();
    }

    public boolean isForbiddenWord(String word) throws SQLException {
        boolean res = false;
        try (Connection connection = dataSource.getConnection();
            PreparedStatement preparedStatement = connection.prepareStatement(SQL_CONTAIN_WORD)) {
            preparedStatement.setString(1, word);
            res = preparedStatement.executeQuery().next();
        }
        return res;
    }

    private boolean isExistTable(String tableName) throws SQLException {
        boolean isExist = false;
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getTables(null, null, tableName, null)){
            while (resultSet.next()){
                isExist = true;
            }
        }
        return isExist;
    }

    private void applyDdl(String sql) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    private void insertDataInTable() throws SQLException, IOException {
        try (Scanner scanner = new Scanner(new File(FILE_NAME));
             Connection connection = dataSource.getConnection(); PreparedStatement preparedStatement = connection.prepareStatement(SQL_INSERT_DATA)) {
            connection.setAutoCommit(false);
            int count = 0;
            while (scanner.hasNextLine()) {
                preparedStatement.setString(1, scanner.nextLine().toLowerCase());
                preparedStatement.addBatch();
                count++;
                if (count == 50) {
                    preparedStatement.executeBatch();
                    connection.commit();
                }
            }
            if (count != 0) {
                preparedStatement.executeBatch();
                connection.commit();
            }
            connection.setAutoCommit(true);
        }
    }
}
