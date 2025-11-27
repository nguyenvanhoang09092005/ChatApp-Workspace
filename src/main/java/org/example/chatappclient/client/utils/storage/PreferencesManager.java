package org.example.chatappclient.client.utils.storage;


import org.example.chatappclient.client.config.Constants;
import java.util.prefs.Preferences;
import java.util.Base64;

public class PreferencesManager {
    private static PreferencesManager instance;
    private Preferences preferences;

    private PreferencesManager() {
        preferences = Preferences.userNodeForPackage(PreferencesManager.class);
    }

    public static synchronized PreferencesManager getInstance() {
        if (instance == null) {
            instance = new PreferencesManager();
        }
        return instance;
    }

    // ==================== CREDENTIALS ====================

    /**
     * Save login credentials
     */
    public void saveCredentials(String username, String password) {
        preferences.put(Constants.PREF_SAVED_USERNAME, username);
        // Simple encoding (not secure, just obfuscation)
        String encodedPassword = Base64.getEncoder().encodeToString(password.getBytes());
        preferences.put(Constants.PREF_SAVED_PASSWORD, encodedPassword);
        preferences.putBoolean(Constants.PREF_REMEMBER_ME, true);
    }

    /**
     * Get saved username
     */
    public String getSavedUsername() {
        return preferences.get(Constants.PREF_SAVED_USERNAME, "");
    }

    /**
     * Get saved password
     */
    public String getSavedPassword() {
        String encodedPassword = preferences.get(Constants.PREF_SAVED_PASSWORD, "");
        if (encodedPassword.isEmpty()) {
            return "";
        }
        try {
            return new String(Base64.getDecoder().decode(encodedPassword));
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Check if remember me is enabled
     */
    public boolean isRememberMeEnabled() {
        return preferences.getBoolean(Constants.PREF_REMEMBER_ME, false);
    }

    /**
     * Clear saved credentials
     */
    public void clearCredentials() {
        preferences.remove(Constants.PREF_SAVED_USERNAME);
        preferences.remove(Constants.PREF_SAVED_PASSWORD);
        preferences.putBoolean(Constants.PREF_REMEMBER_ME, false);
    }

    // ==================== THEME ====================

    /**
     * Save theme preference
     */
    public void saveTheme(String theme) {
        preferences.put(Constants.PREF_THEME, theme);
    }

    /**
     * Get saved theme
     */
    public String getTheme() {
        return preferences.get(Constants.PREF_THEME, Constants.THEME_LIGHT);
    }

    /**
     * Check if dark theme is enabled
     */
    public boolean isDarkTheme() {
        return Constants.THEME_DARK.equals(getTheme());
    }

    // ==================== LANGUAGE ====================

    /**
     * Save language preference
     */
    public void saveLanguage(String language) {
        preferences.put(Constants.PREF_LANGUAGE, language);
    }

    /**
     * Get saved language
     */
    public String getLanguage() {
        return preferences.get(Constants.PREF_LANGUAGE, "vi");
    }

    // ==================== NOTIFICATIONS ====================

    /**
     * Save notification preference
     */
    public void setNotificationsEnabled(boolean enabled) {
        preferences.putBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, enabled);
    }

    /**
     * Check if notifications are enabled
     */
    public boolean isNotificationsEnabled() {
        return preferences.getBoolean(Constants.PREF_NOTIFICATIONS_ENABLED, true);
    }

    /**
     * Save sound preference
     */
    public void setSoundEnabled(boolean enabled) {
        preferences.putBoolean(Constants.PREF_SOUND_ENABLED, enabled);
    }

    /**
     * Check if sound is enabled
     */
    public boolean isSoundEnabled() {
        return preferences.getBoolean(Constants.PREF_SOUND_ENABLED, true);
    }

    // ==================== AUTO DOWNLOAD ====================

    /**
     * Save auto download images preference
     */
    public void setAutoDownloadImages(boolean enabled) {
        preferences.putBoolean(Constants.PREF_AUTO_DOWNLOAD_IMAGES, enabled);
    }

    /**
     * Check if auto download images is enabled
     */
    public boolean isAutoDownloadImages() {
        return preferences.getBoolean(Constants.PREF_AUTO_DOWNLOAD_IMAGES, true);
    }

    // ==================== LAST CONVERSATION ====================

    /**
     * Save last opened conversation ID
     */
    public void saveLastConversationId(String conversationId) {
        preferences.put(Constants.PREF_LAST_CONVERSATION_ID, conversationId);
    }

    /**
     * Get last opened conversation ID
     */
    public String getLastConversationId() {
        return preferences.get(Constants.PREF_LAST_CONVERSATION_ID, "");
    }

    // ==================== GENERIC METHODS ====================

    /**
     * Save string value
     */
    public void putString(String key, String value) {
        preferences.put(key, value);
    }

    /**
     * Get string value
     */
    public String getString(String key, String defaultValue) {
        return preferences.get(key, defaultValue);
    }

    /**
     * Save boolean value
     */
    public void putBoolean(String key, boolean value) {
        preferences.putBoolean(key, value);
    }

    /**
     * Get boolean value
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return preferences.getBoolean(key, defaultValue);
    }

    /**
     * Save int value
     */
    public void putInt(String key, int value) {
        preferences.putInt(key, value);
    }

    /**
     * Get int value
     */
    public int getInt(String key, int defaultValue) {
        return preferences.getInt(key, defaultValue);
    }

    /**
     * Save long value
     */
    public void putLong(String key, long value) {
        preferences.putLong(key, value);
    }

    /**
     * Get long value
     */
    public long getLong(String key, long defaultValue) {
        return preferences.getLong(key, defaultValue);
    }

    /**
     * Remove preference
     */
    public void remove(String key) {
        preferences.remove(key);
    }

    /**
     * Clear all preferences
     */
    public void clearAll() {
        try {
            preferences.clear();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}