package io.ylab.intensive.lesson05.eventsourcing.api;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeoutException;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import io.ylab.intensive.lesson05.eventsourcing.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Тут пишем реализацию
 * 1:id удалить Person с id
 * 2:personId:firstName:lastName:middleName сохранить Person
 */
@Component
public class PersonApiImpl implements PersonApi {

  private ConnectionFactory factory;
  private DataSource dataSource;
  private final String queueName = "PERSON_QUEUE";
  private final String SQL_FIND_PERSON = "SELECT * FROM person WHERE person_id = ?";
  private final String SQL_FIND_ALL_PERSON = "SELECT * FROM person";

  @Autowired
  public PersonApiImpl(ConnectionFactory factory, DataSource dataSource) {
    this.factory = factory;
    this.dataSource = dataSource;
  }
  @Override
  public void deletePerson(Long personId) {
    addQueue("1:" + personId);
  }

  @Override
  public void savePerson(Long personId, String firstName, String lastName, String middleName) {
    addQueue("2:" + personId + ":" + firstName + ":" + lastName+":" + middleName);
  }

  @Override
  public Person findPerson(Long personId) {
    try (java.sql.Connection connection = dataSource.getConnection();
         PreparedStatement preparedStatement = connection.prepareStatement(SQL_FIND_PERSON)) {
      preparedStatement.setLong(1, personId);
      ResultSet resultSet = preparedStatement.executeQuery();
      while (resultSet.next()) {
        Person person = new Person(resultSet.getLong("person_id"), resultSet.getString("first_name"),
                resultSet.getString("last_name"), resultSet.getString("middle_name"));
        return person;
      }
    } catch (SQLException sqlException) {
      sqlException.printStackTrace();
    }
    return null;
  }

  @Override
  public List<Person> findAll() {
    List<Person> list = null;
    try (java.sql.Connection connection = dataSource.getConnection();
         Statement statement = connection.createStatement()) {
      ResultSet resultSet = statement.executeQuery(SQL_FIND_ALL_PERSON);
      list = new ArrayList<>();
      while (resultSet.next()) {
        list.add(new Person(resultSet.getLong("person_id"), resultSet.getString("first_name"),
                resultSet.getString("last_name"), resultSet.getString("middle_name")));
      }
    } catch (SQLException sqlException) {
      sqlException.printStackTrace();
    }
    return list;
  }

  private void addQueue(String message) {
    try (Connection connection = factory.newConnection();
         Channel channel = connection.createChannel()) {
      channel.queueDeclare(queueName, true, false, false, null);
      channel.basicPublish("", queueName, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
    } catch (IOException ioException) {
      ioException.printStackTrace();
    } catch (TimeoutException timeoutException) {
      timeoutException.printStackTrace();
    }
  }
}
