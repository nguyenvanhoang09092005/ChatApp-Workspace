package org.example.chatappclient.client.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class AppConfig {
    private static AppConfig instance;
    private Properties properties;

    // Cấu hình Server
    private String serverHost;
    private int serverPort;
    private int connectionTimeout;
    private int readTimeout;

    // Cấu hình ZeroTier
    private boolean useZeroTier;
    private String zeroTierNetworkId;
    private String zeroTierNetworkName;

    // Cài đặt ứng dụng
    private String appName;
    private String appVersion;
    private boolean autoReconnect;
    private int maxReconnectAttempts;
    private int reconnectDelay;

    // Cài đặt giao diện
    private String defaultTheme;
    private boolean enableNotifications;
    private boolean enableSound;
    private String language;

    // Cài đặt cache
    private boolean enableCache;
    private int cacheSize;
    private long cacheExpiry;

    private AppConfig() {
        properties = new Properties();
        loadConfiguration();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    /**
     * Tải cấu hình từ file (không có debug)
     */
    private void loadConfiguration() {
        try (InputStream input = getClass().getClassLoader()
                .getResourceAsStream("config/client.properties")) {

            if (input != null) {
                properties.load(input);
            } else {
                loadDefaultConfiguration();
            }

            parseConfiguration();

        } catch (IOException e) {
            loadDefaultConfiguration();
        }
    }

    /**
     * Tải cấu hình mặc định
     */
    private void loadDefaultConfiguration() {
        properties.setProperty("server.host", "10.149.108.45");
        properties.setProperty("server.port", "8888");
        properties.setProperty("server.connection.timeout", "30000");
        properties.setProperty("server.read.timeout", "30000");

        properties.setProperty("zerotier.enabled", "true");
        properties.setProperty("zerotier.network_id", "8d1c312afae2a81b");
        properties.setProperty("zerotier.network_name", "ChatApp");

        properties.setProperty("app.name", "ChatApp");
        properties.setProperty("app.version", "1.0.0");
        properties.setProperty("app.auto.reconnect", "true");
        properties.setProperty("app.max.reconnect.attempts", "5");
        properties.setProperty("app.reconnect.delay", "5000");

        properties.setProperty("ui.theme", "light");
        properties.setProperty("ui.notifications.enabled", "true");
        properties.setProperty("ui.sound.enabled", "true");
        properties.setProperty("ui.language", "vi");

        properties.setProperty("cache.enabled", "true");
        properties.setProperty("cache.size", "100");
        properties.setProperty("cache.expiry", "3600000");
    }

    /**
     * Phân tích cấu hình properties
     */
    private void parseConfiguration() {
        serverHost = properties.getProperty("server.host", "10.149.108.45");
        serverPort = getIntProperty("server.port", 8888);
        connectionTimeout = getIntProperty("server.connection.timeout", 30000);
        readTimeout = getIntProperty("server.read.timeout", 30000);

        useZeroTier = getBooleanProperty("zerotier.enabled", true);
        zeroTierNetworkId = properties.getProperty("zerotier.network_id", "8d1c312afae2a81b");
        zeroTierNetworkName = properties.getProperty("zerotier.network_name", "ChatApp");

        appName = properties.getProperty("app.name", "ChatApp");
        appVersion = properties.getProperty("app.version", "1.0.0");
        autoReconnect = getBooleanProperty("app.auto.reconnect", true);
        maxReconnectAttempts = getIntProperty("app.max.reconnect.attempts", 5);
        reconnectDelay = getIntProperty("app.reconnect.delay", 5000);

        defaultTheme = properties.getProperty("ui.theme", "light");
        enableNotifications = getBooleanProperty("ui.notifications.enabled", true);
        enableSound = getBooleanProperty("ui.sound.enabled", true);
        language = properties.getProperty("ui.language", "vi");

        enableCache = getBooleanProperty("cache.enabled", true);
        cacheSize = getIntProperty("cache.size", 100);
        cacheExpiry = getLongProperty("cache.expiry", 3600000L);
    }

    private int getIntProperty(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private long getLongProperty(String key, long defaultValue) {
        try {
            return Long.parseLong(properties.getProperty(key, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private boolean getBooleanProperty(String key, boolean defaultValue) {
        return Boolean.parseBoolean(properties.getProperty(key, String.valueOf(defaultValue)));
    }

    // ==================== GETTERS ====================

    public String getServerHost() { return serverHost; }
    public int getServerPort() { return serverPort; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public boolean isUseZeroTier() { return useZeroTier; }
    public String getZeroTierNetworkId() { return zeroTierNetworkId; }
    public String getZeroTierNetworkName() { return zeroTierNetworkName; }
    public String getAppName() { return appName; }
    public String getAppVersion() { return appVersion; }
    public boolean isAutoReconnect() { return autoReconnect; }
    public int getMaxReconnectAttempts() { return maxReconnectAttempts; }
    public int getReconnectDelay() { return reconnectDelay; }
    public String getDefaultTheme() { return defaultTheme; }
    public boolean isEnableNotifications() { return enableNotifications; }
    public boolean isEnableSound() { return enableSound; }
    public String getLanguage() { return language; }
    public boolean isEnableCache() { return enableCache; }
    public int getCacheSize() { return cacheSize; }
    public long getCacheExpiry() { return cacheExpiry; }

    public String getProperty(String key) { return properties.getProperty(key); }
    public String getProperty(String key, String defaultValue) { return properties.getProperty(key, defaultValue); }
}
