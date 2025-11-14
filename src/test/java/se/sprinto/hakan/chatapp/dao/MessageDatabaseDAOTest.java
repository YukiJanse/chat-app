package se.sprinto.hakan.chatapp.dao;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.*;
import se.sprinto.hakan.chatapp.model.Message;
import se.sprinto.hakan.chatapp.util.DatabaseUtil;

import javax.sql.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MessageDatabaseDAOTest {
    private static DataSource testDataSource;
    private static MessageDatabaseDAO messageDatabaseDAO;

    @BeforeAll
    static void setUpDataSource() throws SQLException{
        testDataSource = DatabaseUtil.getInstance("test").getDataSource();

        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("""
                CREATE TABLE users (
                user_id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL,
                password VARCHAR(50) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE messages (
                message_id INT AUTO_INCREMENT PRIMARY KEY,
                text VARCHAR(1000) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                user_id INT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(user_id)
                )
            """);
        }
    }

    @BeforeEach
    void setUp() throws SQLException{
        messageDatabaseDAO = new MessageDatabaseDAO(testDataSource);
        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("INSERT INTO users (user_id, username, password) VALUES (1, 'Yuki', 'pass')");

        }
    }

    @AfterEach
    void clean() throws SQLException {
        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("DELETE FROM messages");
            stmt.execute("DELETE FROM users");
            stmt.execute("ALTER TABLE messages ALTER COLUMN message_id RESTART WITH 1");
            stmt.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");
        }
    }

    @AfterAll
    static void tearDown() {
        ((HikariDataSource)testDataSource).close();
    }

    @Test
    @DisplayName("Save a message successfully")
    void saveMessageSuccess() throws SQLException{
        // Arrange
        Message message = new Message(1, "test", LocalDateTime.now());

        // Act
        messageDatabaseDAO.saveMessage(message);

        // Assert
        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM messages WHERE message_id = 1");

            assertTrue(rs.next());
            assertEquals(1, rs.getInt("user_id"));
            assertEquals("test", rs.getString("text"));
        }
    }

    @Test
    @DisplayName("Saving a message that is null")
    void saveMessageWithNull() {
        // Arrange
        Message message = null;

        // Act Assert
        assertThrows(IllegalArgumentException.class,() ->messageDatabaseDAO.saveMessage(message));
    }

    @Test
    @DisplayName("Saving a message with null values")
    void saveMessageWithWrongNullValues() {
        // Arrange
        Message message = new Message(1, null, null);

        // Act Assert
        assertThrows(IllegalArgumentException.class,() ->messageDatabaseDAO.saveMessage(message));
    }

    @Test
    @DisplayName("Saving a message with not existing user")
    void saveMessageWithWrongUserId() throws SQLException{
        // Arrange
        Message message = new Message(2, "test", LocalDateTime.now());

        // Act
        messageDatabaseDAO.saveMessage(message);

        // Assert
        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            ResultSet rs = stmt.executeQuery("SELECT * FROM messages WHERE message_id = 1");

            assertFalse(rs.next());
        }
    }

    @Test
    @DisplayName("Get messages Successfully")
    void getMessagesSuccess() throws SQLException{
        // Arrange
        String sql = "INSERT INTO messages (text, timestamp, user_id) Values (?, ?, ?)";
        try (Connection con = testDataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            Timestamp timestamp = java.sql.Timestamp.valueOf(LocalDateTime.now());

            ps.setString(1, "test");
            ps.setTimestamp(2, timestamp);
            ps.setInt(3, 1);
            ps.execute();

            ps.setString(1, "hej");
            ps.setTimestamp(2, timestamp);
            ps.setInt(3, 1);
            ps.execute();
        }

        // Act
        List<Message> messages = messageDatabaseDAO.getMessagesByUserId(1);

        // Assert
        assertEquals(2, messages.size());
        assertEquals("test", messages.get(0).getText());
        assertEquals("hej", messages.get(1).getText());
    }

    @Test
    @DisplayName("Get messages with wrong id")
    void getMessagesWithWrongUserId() throws SQLException{
        // Arrange
        String sql = "INSERT INTO messages (text, timestamp, user_id) Values (?, ?, ?)";
        try (Connection con = testDataSource.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            Timestamp timestamp = java.sql.Timestamp.valueOf(LocalDateTime.now());

            ps.setString(1, "test");
            ps.setTimestamp(2, timestamp);
            ps.setInt(3, 1);
            ps.execute();

            ps.setString(1, "hej");
            ps.setTimestamp(2, timestamp);
            ps.setInt(3, 1);
            ps.execute();
        }

        // Act
        List<Message> messages = messageDatabaseDAO.getMessagesByUserId(2);

        // Assert
        assertEquals(0, messages.size());
    }

    @Test
    @DisplayName("Get messages but no user exists")
    void getMessagesButNoMessageExists() {
        // Act
        List<Message> messages = messageDatabaseDAO.getMessagesByUserId(2);

        // Assert
        assertEquals(0, messages.size());
    }
}