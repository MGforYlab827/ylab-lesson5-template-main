package io.ylab.intensive.lesson05.messagefilter;

import com.rabbitmq.client.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.TimeoutException;

@Component
public class MessageFilter {
    private ConnectionFactory factory;
    private final String QUEUE_INPUT = "input";
    private final String QUEUE_OUTPUT = "output";
    private Database database;

    @Autowired
    public MessageFilter(ConnectionFactory factory, Database database) {
        this.factory = factory;
        this.database = database;
    }

    public void startMessageFilter() throws IOException, SQLException, TimeoutException {
        database.initTable();
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.queueDeclare(QUEUE_INPUT, true, false, false, null);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
          String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                message = getCensoredMessage(message);
                sendInQueue(message);
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (TimeoutException timeoutException) {
                timeoutException.printStackTrace();
            }
        };
        channel.basicConsume(QUEUE_INPUT, true, deliverCallback, consumerTag -> {});
    }

    private void sendInQueue(String message) throws IOException, TimeoutException {
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare(QUEUE_OUTPUT, true, false, false, null);
            channel.basicPublish("", QUEUE_OUTPUT, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        }
    }

    private String getCensoredMessage(String message) throws SQLException {
        StringBuilder result = new StringBuilder(message);
        String replace = "*";
        String word;
        int position = 0;
        try (Scanner scanner = new Scanner(message)) {
            scanner.useDelimiter("\\.|,|;|\\?|!|\\n|\\s");
            int start, end, repeat;
            while (scanner.hasNext()) {
                word = scanner.next();
                if (!word.isEmpty() && database.isForbiddenWord(word.toLowerCase(Locale.ROOT))) {
                    start = position + 1;
                    end = start + word.length() - 2;
                    repeat = word.length() - 2;
                    result.replace(start, end, replace.repeat(repeat));
                }
                position += word.length() + 1;
            }
        }
        return result.toString();
    }
}
