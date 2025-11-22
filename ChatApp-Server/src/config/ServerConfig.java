package config;

import java.io.*;
import java.util.Properties;

public class ServerConfig {

    private static Properties serverProps;
    private static Properties dbProps;

    private static final String SERVER_CONFIG = "resources/config/server.properties";
    private static final String DB_CONFIG = "resources/config/database.properties";

    static {
        loadConfigs();
    }

    /**
     * Tải tất cả file cấu hình
     */
    private static void loadConfigs() {
        serverProps = loadProperties(SERVER_CONFIG);
        dbProps = loadProperties(DB_CONFIG);
    }

    /**
     * Tải properties từ file
     */
    private static Properties loadProperties(String filePath) {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream(filePath)) {
            props.load(input);
        } catch (IOException e) {
            System.err.println("Lỗi tải config từ " + filePath + ": " + e.getMessage());
        }
        return props;
    }

    // ==================== CẤU HÌNH SERVER ====================

    public static int getServerPort() {
        return Integer.parseInt(serverProps.getProperty("server.port", "8888"));
    }

    public static int getMaxClients() {
        return Integer.parseInt(serverProps.getProperty("server.max_clients", "1000"));
    }

    public static int getServerTimeout() {
        return Integer.parseInt(serverProps.getProperty("server.timeout", "300000"));
    }

    // ==================== CẤU HÌNH ZEROTIER ====================

    public static boolean isZeroTierEnabled() {
        return Boolean.parseBoolean(serverProps.getProperty("zerotier.enabled", "false"));
    }

    public static String getZeroTierNetworkId() {
        return serverProps.getProperty("zerotier.network_id", "");
    }

    public static String getZeroTierNetworkName() {
        return serverProps.getProperty("zerotier.network_name", "ChatAppNetwork");
    }

    public static long getZeroTierCheckInterval() {
        return Long.parseLong(serverProps.getProperty("zerotier.check_interval", "30000"));
    }

    // ==================== CẤU HÌNH EMAIL ====================

    public static String getEmailHost() {
        return serverProps.getProperty("email.smtp.host", "smtp.gmail.com");
    }

    public static String getEmailPort() {
        return serverProps.getProperty("email.smtp.port", "587");
    }

    public static String getEmailUsername() {
        return serverProps.getProperty("email.smtp.username", "");
    }

    public static String getEmailPassword() {
        return serverProps.getProperty("email.smtp.password", "");
    }

    public static String getEmailFromName() {
        return serverProps.getProperty("email.from.name", "ChatApp");
    }

    // ==================== CẤU HÌNH BẢO MẬT ====================

    public static String getJWTSecret() {
        return serverProps.getProperty("jwt.secret", "default-secret-change-me");
    }

    public static long getSessionTimeout() {
        return Long.parseLong(serverProps.getProperty("session.timeout", "3600000"));
    }

    public static long getPasswordResetExpiry() {
        return Long.parseLong(serverProps.getProperty("password.reset.expiry", "3600000"));
    }

    public static long getVerificationCodeExpiry() {
        return Long.parseLong(serverProps.getProperty("verification.code.expiry", "600000"));
    }

    // ==================== THÔNG TIN ỨNG DỤNG ====================

    public static String getAppName() {
        return serverProps.getProperty("app.name", "ChatApp");
    }

    public static String getAppVersion() {
        return serverProps.getProperty("app.version", "1.0.0");
    }

    // ==================== CẤU HÌNH DATABASE ====================

    public static String getDBHost() {
        return dbProps.getProperty("db.host", "localhost");
    }

    public static String getDBPort() {
        return dbProps.getProperty("db.port", "3307");
    }

    public static String getDBName() {
        return dbProps.getProperty("db.name", "chatapp");
    }

    public static String getDBUsername() {
        return dbProps.getProperty("db.username", "root");
    }

    public static String getDBPassword() {
        return dbProps.getProperty("db.password", "");
    }

    public static String getDBUrl() {
        return String.format(
                "jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&characterEncoding=utf8",
                getDBHost(), getDBPort(), getDBName()
        );
    }

    public static int getDBPoolMinSize() {
        return Integer.parseInt(dbProps.getProperty("db.pool.minSize", "5"));
    }

    public static int getDBPoolMaxSize() {
        return Integer.parseInt(dbProps.getProperty("db.pool.maxSize", "20"));
    }

    public static int getDBPoolTimeout() {
        return Integer.parseInt(dbProps.getProperty("db.pool.timeout", "30000"));
    }

    // ==================== TẢI LẠI CẤU HÌNH ====================

    public static void reloadConfigs() {
        loadConfigs();
        System.out.println("Đã tải lại cấu hình");
    }

    // ==================== IN CẤU HÌNH ====================

    public static void printConfig() {
        System.out.println("\n========== CẤU HÌNH SERVER ==========");
        System.out.println("Tên ứng dụng: " + getAppName());
        System.out.println("Phiên bản: " + getAppVersion());
        System.out.println("Port Server: " + getServerPort());
        System.out.println("Tối đa Clients: " + getMaxClients());
        System.out.println("\n========== CẤU HÌNH ZEROTIER ==========");
        System.out.println("Đã bật: " + isZeroTierEnabled());
        if (isZeroTierEnabled()) {
            System.out.println("Network ID: " + getZeroTierNetworkId());
            System.out.println("Tên Network: " + getZeroTierNetworkName());
            System.out.println("Kiểm tra mỗi: " + (getZeroTierCheckInterval()/1000) + "s");
        }
        System.out.println("\n========== CẤU HÌNH DATABASE ==========");
        System.out.println("Host: " + getDBHost() + ":" + getDBPort());
        System.out.println("Database: " + getDBName());
        System.out.println("Username: " + getDBUsername());
        System.out.println("==========================================\n");
    }
}