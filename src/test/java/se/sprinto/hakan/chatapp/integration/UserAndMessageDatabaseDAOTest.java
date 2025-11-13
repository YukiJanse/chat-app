package se.sprinto.hakan.chatapp.integration;

import org.junit.jupiter.api.*;
import se.sprinto.hakan.chatapp.dao.MessageDatabaseDAO;
import se.sprinto.hakan.chatapp.dao.UserDatabaseDAO;
import se.sprinto.hakan.chatapp.model.Message;
import se.sprinto.hakan.chatapp.model.User;
import se.sprinto.hakan.chatapp.util.DatabaseUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class UserAndMessageDatabaseDAOTest {
    private static DataSource testDataSource;
    private static UserDatabaseDAO userDatabaseDAO;
    private static MessageDatabaseDAO messageDatabaseDAO;

    @BeforeAll
    static void setUpDatabase() throws SQLException {
        testDataSource = DatabaseUtil.getInstance("test").getDataSource();

        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("""
                CREATE TABLE users(
                    user_id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(50) NOT NULL,
                    password VARCHAR(50) NOT NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE messages(
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
    void setUp() {
        userDatabaseDAO = new UserDatabaseDAO(testDataSource);
        messageDatabaseDAO = new MessageDatabaseDAO(testDataSource);
    }

    @AfterEach
    void clean() throws SQLException{
        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("DELETE FROM messages");
            stmt.execute("DELETE FROM users");
            stmt.execute("ALTER TABLE messages ALTER COLUMN message_id RESTART WITH 1");
            stmt.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");

        }
    }

    @Test
    @DisplayName("Register user and 2 messages")
    void registerUserAndTwoMessages() {
        // Arrange
        User user = new User("Yuki", "pass");

        // Act
        User registeredUser = userDatabaseDAO.register(user);

        // Assert
        assertNotNull(registeredUser);
        assertEquals("Yuki", registeredUser.getUsername());
        assertEquals("pass", registeredUser.getPassword());

        // Arrange
        LocalDateTime sentDateTime = LocalDateTime.now();
        Message message1 = new Message(registeredUser.getId(), "Hej hej", sentDateTime);
        Message message2 = new Message(registeredUser.getId(), "Hur mår du?", sentDateTime);

        // Act
        messageDatabaseDAO.saveMessage(message1);
        messageDatabaseDAO.saveMessage(message2);
        List<Message> messages = messageDatabaseDAO.getMessagesByUserId(registeredUser.getId());

        // Assert
        assertEquals(2, messages.size());
        assertEquals("Hej hej", messages.get(0).getText());
        assertEquals(sentDateTime, messages.get(0).getTimestamp());
        assertEquals(registeredUser.getId(), messages.get(0).getUserId());
        assertEquals("Hur mår du?", messages.get(1).getText());
        assertEquals(sentDateTime, messages.get(1).getTimestamp());
        assertEquals(registeredUser.getId(), messages.get(1).getUserId());

    }
}
