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

public class UserDatabaseDAO implements UserDAO {
    private static final Logger logger = LoggerFactory.getLogger(UserDatabaseDAO.class);
    private DataSource dataSource;

    public UserDatabaseDAO() {
        dataSource = DatabaseUtil.getInstance().getDataSource();
    }

    /**
     * Constructor for testing.
     * @param dataSource is to connect User data base.
     */
    public UserDatabaseDAO(DataSource dataSource) {
         this.dataSource = dataSource;
    }

    @Override
    public User login(String username, String password) {
        User authorizedUser = null;
        String sql = """
                SELECT u.user_id, u.username, u.password, m.message_id, m.text, m.timestamp
                FROM user u
                LEFT JOIN message m ON u.user_id = m.user_id
                WHERE u.username = ? AND u.password = ?
                """;
        try (Connection con = dataSource.getConnection();
        PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    if (authorizedUser == null) {
                        authorizedUser = mapUser(rs);
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

    @Override
    public User register(User user) {
        User registeredUser = null;
        String insertSql = """
                INSERT INTO user (user_id, username, password) VALUES(?, ?, ?)
                """;
        try (Connection con = dataSource.getConnection();
             PreparedStatement preparedStmtForInsert = con.prepareStatement(insertSql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            preparedStmtForInsert.setInt(1, user.getId());
            preparedStmtForInsert.setString(2, user.getUsername());
            preparedStmtForInsert.setString(3, user.getPassword());
            int insertedRows = preparedStmtForInsert.executeUpdate();
            if (insertedRows == 0) {
                logger.warn("No user inserted with username= {}", user.getUsername());
            } else {
                logger.info("Successfully inserted user with username= {}", user.getUsername());
                try (ResultSet generatedKeys = preparedStmtForInsert.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int generatedId = generatedKeys.getInt(1);
                        registeredUser = findRegisteredUser(con, generatedId);
                    }
                }
            }
        } catch (SQLException e) {
            logger.error("Failed to insert user to database.", e);
        }
        return registeredUser;
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(rs.getInt("user_id"), rs.getString("username"), rs.getString("password"));
    }

    private User findRegisteredUser(Connection con, int id) throws SQLException {
        User registeredUser = null;
        String selectSql = "SELECT * FROM user WHERE user_id = ?";
        try (PreparedStatement preparedStmtForSelect = con.prepareStatement(selectSql)) {
            preparedStmtForSelect.setInt(1, id);
            try (ResultSet rs =  preparedStmtForSelect.executeQuery()) {
                if (rs.next()) {
                    registeredUser = mapUser(rs);
                    logger.info("Successfully find registered user");
                } else {
                    logger.warn("Failed to find registered user");
                }
            }
        }
        return registeredUser;
    }
}
