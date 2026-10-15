package kr.sanchaek.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * DB 연결. 모든 DAO는 이걸로 연결을 받습니다.
 *
 * <pre>
 * try (Connection conn = DBUtil.getConnection();
 *      PreparedStatement ps = conn.prepareStatement("SELECT ... WHERE course_id = ?")) {
 *     ps.setLong(1, courseId);
 *     try (ResultSet rs = ps.executeQuery()) { ... }
 * }
 * </pre>
 *
 * try-with-resources(괄호 안에 선언)로 쓰면 다 쓴 연결이 자동으로 닫힙니다.
 */
public final class DBUtil {

    private static volatile boolean driverLoaded = false;

    private DBUtil() {
    }

    public static Connection getConnection() throws SQLException {
        loadDriver();
        return DriverManager.getConnection(
                AppConfig.get("db.url"),
                AppConfig.get("db.user"),
                AppConfig.get("db.password"));
    }

    /**
     * Tomcat 안에서는 WEB-INF/lib 의 드라이버가 자동으로 등록되지 않습니다.
     * 이 줄이 없으면 "No suitable driver found" 오류가 납니다 (실제 테스트로 확인).
     */
    private static void loadDriver() throws SQLException {
        if (driverLoaded) {
            return;
        }
        String driver = AppConfig.get("db.driver", "com.mysql.cj.jdbc.Driver");
        try {
            Class.forName(driver);
            driverLoaded = true;
        } catch (ClassNotFoundException e) {
            throw new SQLException("DB 드라이버(" + driver + ")를 찾지 못했습니다. 이클립스에서 Maven → Update Project 를 해 보세요.", e);
        }
    }
}
