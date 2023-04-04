package io.ylab.intensive.lesson05.eventsourcing.db;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.DeliverCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

/**
 * 1:id удалить Person с id
 * 2:personId:firstName:lastName:middleName сохранить Person
 */
@Component
public class DataProcessing {
    private ConnectionFactory factory;
    private DataSource dataSource;
    private final String queueName = "PERSON_QUEUE";
    private final String SQL_DELETE_PERSON_WITH_ID = "DELETE FROM person WHERE person_id = ?";
    private  final String SQL_INSERT_PERSON = "INSERT INTO person VALUES (?, ?, ?, ?);";
    private final String SQL_UPDATE_PERSON = "UPDATE person SET first_name = ?, last_name = ?, middle_name = ? WHERE person_id = ?";
    private final String SQL_HAS_PERSON = "SELECT * FROM person WHERE person_id = ?";
    @Autowired
    public DataProcessing (ConnectionFactory factory, DataSource dataSource) {
        this.factory = factory;
        this.dataSource = dataSource;
    }

    public void waitMessage() {
        try {
            Connection connection = factory.newConnection();
            Channel channel = connection.createChannel();
            channel.queueDeclare(queueName, true, false, false, null);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                Scanner scanner = new Scanner(new String(delivery.getBody(), StandardCharsets.UTF_8));
                scanner.useDelimiter("\\s*:\\s*");
                int value = scanner.nextInt();
                if (value == 1) {
                    deletePersonFromDb(scanner.nextLong());
                } else if (value == 2) {
                    insertPerson(scanner.nextLong(), scanner.next(), scanner.next(), scanner.next());
                }
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (IOException ioException) {
            ioException.printStackTrace();
        } catch (TimeoutException timeoutException) {
            timeoutException.printStackTrace();
        }
    }

    private void deletePersonFromDb(Long person_id) {
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement preparedStatement = connection.prepareStatement(SQL_DELETE_PERSON_WITH_ID)) {
            preparedStatement.setLong(1, person_id);
            if (preparedStatement.executeUpdate() == 0) {
                System.out.println("Попытка удаления. Данные не найдены.");
            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }

    private void insertPerson(Long person_id, String first_name, String last_name, String middle_name) {
        try (java.sql.Connection connection = dataSource.getConnection();
             PreparedStatement statementInsert = connection.prepareStatement(SQL_INSERT_PERSON);
             PreparedStatement statementUpdate = connection.prepareStatement(SQL_UPDATE_PERSON);
             PreparedStatement statementHasPerson = connection.prepareStatement(SQL_HAS_PERSON)) {
            statementHasPerson.setLong(1, person_id);
            ResultSet resultSet = statementHasPerson.executeQuery();
            if (resultSet.next()) {
                statementUpdate.setString(1, first_name);
                statementUpdate.setString(2, last_name);
                statementUpdate.setString(3, middle_name);
                statementUpdate.setLong(4, person_id);
                statementUpdate.executeUpdate();
            } else {
                statementInsert.setLong(1, person_id);
                statementInsert.setString(2, first_name);
                statementInsert.setString(3, last_name);
                statementInsert.setString(4, middle_name);
                statementInsert.executeUpdate();
            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        }
    }


}
