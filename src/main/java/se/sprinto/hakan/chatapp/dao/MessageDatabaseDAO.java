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

public class MessageDatabaseDAO implements MessageDAO {
    private static final Logger logger = LoggerFactory.getLogger(MessageDatabaseDAO.class);
    private final DataSource dataSource;


    public MessageDatabaseDAO() {
        dataSource = DatabaseUtil.getInstance().getDataSource();
    }

    /**
     * Constructor for testing
     * @param dataSource is to conect database
     */
    public MessageDatabaseDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void saveMessage(Message message) {
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

    private Message mapMessage(ResultSet rs) throws SQLException {
        //int messageId = rs.getInt("message_id"); There is no interface for messageId in Message class
        String text = rs.getString("text");
        LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
        int userId = rs.getInt("user_id");

        return new Message(userId, text, timestamp);
    }
}
