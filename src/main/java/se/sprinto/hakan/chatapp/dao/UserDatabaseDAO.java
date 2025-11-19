package se.sprinto.hakan.chatapp.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.sprinto.hakan.chatapp.model.Message;
import se.sprinto.hakan.chatapp.model.User;
import se.sprinto.hakan.chatapp.util.DatabaseUtil;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import org.mindrot.jbcrypt.BCrypt;

/**
 * The UserDatabaseDAO class is an implementation of the UserDAO interface. It interacts with
 * a relational database to perform operations such as inserting a new user to the database and
 * finding a user by user id. It has a Data source to access the database. It has two constructors
 *  * for the product and testing version.
 */
public class UserDatabaseDAO implements UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDatabaseDAO.class);
    private static final int DUPLICATE_ENTRY = 1062;
    private final DataSource dataSource;

    public UserDatabaseDAO() {
        dataSource = DatabaseUtil.getInstance().getDataSource();
    }

    /**
     * Constructor for testing.
     * @param dataSource is to connect User database.
     */
    public UserDatabaseDAO(DataSource dataSource) {
         this.dataSource = dataSource;
    }

    /**
     * Authorizes username and password by finding a user that has the same username and password.
     * @param username The username of the user
     * @param password The password of the user
     * @return a User object if it found the user from the database, otherwise returns null.
     */
    @Override
    public User login(String username, String password) {
        User authorizedUser = null;
        String sql = """
                SELECT u.user_id, u.username, u.password, m.message_id, m.text, m.timestamp
                FROM users u
                LEFT JOIN messages m ON u.user_id = m.user_id
                WHERE u.username = ?
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (authorizedUser == null) { // If it is the first row
                        authorizedUser = validateUser(rs, password);
                        if (authorizedUser == null) { // If the password is wrong, authorizedUser will be null
                            logger.warn("Wrong password.");
                            break;
                        }
                    }
                    int message_id = rs.getInt("message_id");
                    if (!rs.wasNull()) {
                        String text = rs.getString("text");
                        LocalDateTime timestamp = rs.getTimestamp("timestamp").toLocalDateTime();
                        authorizedUser.addMessage(new Message(authorizedUser.getId(), text, timestamp));
                    }
                }
                if (authorizedUser == null) {
                    logger.warn("Wrong username or password.");
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to login.", e);
        }
        return authorizedUser;
    }

    /**
     * Inserts a new user to the database.
     * @param user The user to register
     * @return User object with a generated ID if it's successfully inserted, otherwise it
     * returns null.
     * @throws IllegalArgumentException will be thrown if The user object is invalid
     */
    @Override
    public User register(User user) {
        if (user == null || user.getUsername() == null || user.getPassword() == null) {
            throw new IllegalArgumentException("Username and password must exist");
        }
        String insertSql = "INSERT INTO users (username, password) VALUES(?, ?)";
        try (Connection con = dataSource.getConnection();
             PreparedStatement preparedStmtForInsert = con.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStmtForInsert.setString(1, user.getUsername());
            String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
            preparedStmtForInsert.setString(2, hashedPassword);
            int insertedRows = preparedStmtForInsert.executeUpdate();
            if (insertedRows == 0) {
                logger.warn("No user inserted with username= {}", user.getUsername());
                user = null;
            } else {
                logger.info("Successfully inserted user with username= {}", user.getUsername());
                user.setPassword(hashedPassword);
                try (ResultSet generatedKeys = preparedStmtForInsert.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setId(generatedKeys.getInt(1));
                    }
                }
            }
        } catch (SQLException e) {
            if (e.getErrorCode() == DUPLICATE_ENTRY) {
                logger.error("The username already exists.", e);
            }
            logger.error("Failed to insert user to database.", e);
        }
        return user;
    }

    /**
     * Validates password and Maps the ResultSet object to a User object.
     * @param rs ResultSet of SQL command.
     * @param rawPassword The plain password from user input.
     * @return a User object from the ResultSet.
     * @throws SQLException It throws if something went wrong with JDBC functions.
     */
    private User validateUser(ResultSet rs, String rawPassword) throws SQLException {
        int user_id = rs.getInt("user_id");
        String username = rs.getString("username");
        String storedHash = rs.getString("password");

        return BCrypt.checkpw(rawPassword, storedHash) ? new User(user_id, username, storedHash) : null;
    }
}
