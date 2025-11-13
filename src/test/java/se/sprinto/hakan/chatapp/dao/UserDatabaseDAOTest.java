package se.sprinto.hakan.chatapp.dao;

import org.junit.jupiter.api.*;
import se.sprinto.hakan.chatapp.model.User;
import se.sprinto.hakan.chatapp.util.DatabaseUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class UserDatabaseDAOTest {
    private static DataSource testDataSource;
    private static UserDatabaseDAO userDatabaseDAO;

    @BeforeAll
    static void setUserDatabaseDAO() throws SQLException {
        testDataSource = DatabaseUtil.getInstance("test").getDataSource();

        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("""
            CREATE TABLE users (
                user_id INT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL,
                password VARCHAR(50) NOT NULL)
            """);

            stmt.execute("""
            CREATE TABLE messages (
                message_id INT AUTO_INCREMENT PRIMARY KEY,
                text VARCHAR(1000) NOT NULL,
                timestamp TIMESTAMP NOT NULL,
                user_id INT NOT NULL,
                FOREIGN KEY (user_id) REFERENCES users(user_id))
            """);

        }
    }
    @BeforeEach
    void setUp() throws SQLException{
        userDatabaseDAO = new UserDatabaseDAO(testDataSource);
        try(Connection con = testDataSource.getConnection();
        Statement stmt = con.createStatement()) {
            stmt.execute("INSERT INTO users (username, password) VALUES ('Yuki', 'pass')");
            stmt.execute("INSERT INTO users (username, password) VALUES ('Emil', 'pass')");
        }
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Clean data before every test
        try (Connection con = testDataSource.getConnection();
             Statement stmt = con.createStatement()) {
            stmt.execute("DELETE FROM messages");
            stmt.execute("DELETE FROM users");
            stmt.execute("ALTER TABLE messages ALTER COLUMN message_id RESTART WITH 1");
            stmt.execute("ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1");
        }
    }

    @Test
    @DisplayName("Login with correct username and password")
    void loginSuccusses() {
        // Arrange
        // Behöver man göra arrange här istället med setUp()?

        // Act
        User authorizedUser = userDatabaseDAO.login("Yuki", "pass");

        // Assert
        assertNotNull(authorizedUser);
        assertNotEquals(0, authorizedUser.getId());
        assertEquals("Yuki", authorizedUser.getUsername());
        assertEquals("pass", authorizedUser.getPassword());
    }

    @Test
    @DisplayName("Login with wrong username and password")
    void loginFailWithWrongNameAndPass() {
        // Arrange
        // Behöver man göra arrange här istället med setUp()?

        // Act
        User authorizedUser = userDatabaseDAO.login("David", "1234");

        // Assert
        assertNull(authorizedUser);
    }

    @Test
    @DisplayName("Login with correct username and wrong password")
    void loginFailWithCorrectNameAndWrongPass() {
        // Arrange
        // Behöver man göra arrange här istället med setUp()?

        // Act
        User authorizedUser = userDatabaseDAO.login("Yuki", "1234");

        // Assert
        assertNull(authorizedUser);
    }

    @Test
    @DisplayName("Register with a valid user object")
    void registerSuccusses() {
        // Arrange
        User targetUser = new User("Karin", "pass");

        // Act
        User registeredUser = userDatabaseDAO.register(targetUser);

        // Assert
        assertNotNull(registeredUser);
        assertEquals("Karin", registeredUser.getUsername());
        assertEquals("pass", registeredUser.getPassword());
        assertNotEquals(0, registeredUser.getId());
    }

    @Test
    @DisplayName("Register with a invalid user object that its username is null")
    void registerFailWithUserWithoutName() {
        // Arrange
        User targetUser = new User();

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> userDatabaseDAO.register(targetUser));
    }
}