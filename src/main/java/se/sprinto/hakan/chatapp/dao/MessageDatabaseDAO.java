package se.sprinto.hakan.chatapp.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sprinto.hakan.chatapp.model.Message;
import se.sprinto.hakan.chatapp.util.DatabaseUtil;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * The MessageDatabaseDAO class is an implementation of the MessageDAO interface. It interacts with
 * a relational database to perform operations such as inserting a new message to the database and
 * finding messages by user id. It has a Data source to access the database. It has two constructors
 * for the product and testing version.
 */
public class MessageDatabaseDAO implements MessageDAO {
    private static final Logger logger = LoggerFactory.getLogger(MessageDatabaseDAO.class);
    private final DataSource dataSource;


    public MessageDatabaseDAO() {
        dataSource = DatabaseUtil.getInstance().getDataSource();
    }

    /**
     * Constructor for testing
     * @param dataSource The DataSource to access the database
     */
    public MessageDatabaseDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Inserts a message to the database.
     * @param message The message to insert to the database.
     * @throws IllegalArgumentException will be thrown if Message is null.
     */
    @Override
    public void saveMessage(Message message) {
        if (message == null) {
            throw new IllegalArgumentException("Message cannot be null.");
        }
        String sql = """
                INSERT INTO messages (text, user_id, timestamp) VALUES (?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, message.getText());
            ps.setInt(2, message.getUserId());
            ps.setTimestamp(3, Timestamp.valueOf(message.getTimestamp()));
            logger.info("Saving message with text= {}, user_id= {}, timestamp= {}", message.getText(), message.getUserId(), message.getTimestamp());
            int insertedRows = ps.executeUpdate();
            if (insertedRows == 0) {
                logger.warn("No messages inserted.");
            } else {
                logger.info("Successfully inserted message");
            }
        } catch (SQLException e) {
            logger.error("Failed to insert new message. message text= {}", message.getText());
        }
    }

    /**
     * Finds messages from the database by user id.
     * @param userId The user id that messages have.
     * @return a list of messages that matched with the ID.
     */
    @Override
    public List<Message> getMessagesByUserId(int userId) {
        List<Message> messages = new ArrayList<>();
        String sql = """
                SELECT m.message_id, m.text, m.timestamp, m.user_id
                FROM messages m JOIN users u ON m.user_id = u.user_id
                WHERE m.user_id = ?
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapMessage(rs));
                }
            }

        } catch (SQLException e) {
            logger.error("Failed to find messages by user_id= {}", userId);
        }
        return messages;
    }

    /**
     * Maps a Message object from ResultSet.
     * @param rs The ResultSet of a SQL command
     * @return A Message object from the ResultSet.
     * @throws SQLException will be thrown if something went wrong with JDBC functions.
     */
    private Message mapMessage(ResultSet rs) throws SQLException {
        //int messageId = rs.getInt("message_id"); There is no interface for messageId in Message class
        String text = rs.getString("text");
        LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
        int userId = rs.getInt("user_id");

        return new Message(userId, text, timestamp);
    }
}
