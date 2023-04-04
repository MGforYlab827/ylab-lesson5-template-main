package io.ylab.intensive.lesson05.sqlquerybuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Component
public class SQLQueryBuilderImpl implements  SQLQueryBuilder{
    private DataSource dataSource;
    @Autowired
    public SQLQueryBuilderImpl(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    @Override
    public String queryForTable(String tableName) throws SQLException {
        StringBuilder stringBuilder = new StringBuilder("SELECT ");
        try (Connection connection = dataSource.getConnection();
            ResultSet resultSet = connection.getMetaData().getColumns(null, null, tableName, "%")) {
            while (resultSet.next()) {
                stringBuilder.append(resultSet.getString("COLUMN_NAME"));
                if(!resultSet.isLast()) {
                    stringBuilder.append(", ");
                }
            }
            stringBuilder.append(" FROM " + tableName + ";");
        }
        return stringBuilder.toString();
    }

    @Override
    public List<String> getTables() throws SQLException {
        List<String> list = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             ResultSet resultSet = connection.getMetaData().getTables(null, null, "%", new String[]{
                     "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM"
             })) {
            while (resultSet.next()) {
                list.add(resultSet.getString("Table_NAME"));
            }
        }
        return list;
    }
}
