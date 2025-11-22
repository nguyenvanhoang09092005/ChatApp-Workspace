package org.example.chatappclient.client.config;

public final class Constants {

    // ==================== APPLICATION INFO ====================
    public static final String APP_NAME = "ChatApp";
    public static final String APP_VERSION = "1.0.0";
    public static final String APP_AUTHOR = "Your Name";

    // ==================== VALIDATION ====================
    public static final int MIN_USERNAME_LENGTH = 3;
    public static final int MAX_USERNAME_LENGTH = 20;
    public static final int MIN_PASSWORD_LENGTH = 6;
    public static final int MAX_PASSWORD_LENGTH = 50;
    public static final int MIN_DISPLAY_NAME_LENGTH = 1;
    public static final int MAX_DISPLAY_NAME_LENGTH = 50;
    public static final int MAX_STATUS_MESSAGE_LENGTH = 100;
    public static final int MAX_MESSAGE_LENGTH = 5000;

    // ==================== PREFERENCES KEYS ====================
    public static final String PREF_REMEMBER_ME = "remember_me";
    public static final String PREF_SAVED_USERNAME = "saved_username";
    public static final String PREF_SAVED_PASSWORD = "saved_password";
    public static final String PREF_THEME = "theme";
    public static final String PREF_LANGUAGE = "language";
    public static final String PREF_NOTIFICATIONS_ENABLED = "notifications_enabled";
    public static final String PREF_SOUND_ENABLED = "sound_enabled";
    public static final String PREF_AUTO_DOWNLOAD_IMAGES = "auto_download_images";
    public static final String PREF_LAST_CONVERSATION_ID = "last_conversation_id";

    // ==================== REGEX PATTERNS ====================
    public static final String EMAIL_PATTERN = "^[A-Za-z0-9+_.-]+@(.+)$";
    public static final String PHONE_PATTERN = "^[0-9]{10,11}$";
    public static final String USERNAME_PATTERN = "^[a-zA-Z0-9_]{3,20}$";

    // ==================== FILE UPLOAD LIMITS ====================
    public static final long MAX_FILE_SIZE = 50 * 1024 * 1024;  // 50MB
    public static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final long MAX_AVATAR_SIZE = 5 * 1024 * 1024; // 5MB
    public static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    public static final String[] ALLOWED_IMAGE_TYPES = {".jpg", ".jpeg", ".png", ".gif"};
    public static final String[] ALLOWED_FILE_TYPES = {".pdf", ".doc", ".docx", ".xls", ".xlsx", ".txt", ".zip", ".rar"};

    // ==================== UI SETTINGS ====================
    public static final int WINDOW_MIN_WIDTH = 1000;
    public static final int WINDOW_MIN_HEIGHT = 600;
    public static final int WINDOW_DEFAULT_WIDTH = 1400;
    public static final int WINDOW_DEFAULT_HEIGHT = 800;

    public static final int AVATAR_SIZE = 40;
    public static final int AVATAR_SIZE_LARGE = 80;
    public static final int AVATAR_SIZE_SMALL = 30;

    // ==================== COLORS ====================
    public static final String PRIMARY_COLOR = "#2196F3";
    public static final String SECONDARY_COLOR = "#FFC107";
    public static final String SUCCESS_COLOR = "#4CAF50";
    public static final String ERROR_COLOR = "#F44336";
    public static final String WARNING_COLOR = "#FF9800";
    public static final String INFO_COLOR = "#2196F3";

    public static final String LIGHT_BG = "#FFFFFF";
    public static final String DARK_BG = "#303030";
    public static final String LIGHT_TEXT = "#000000";
    public static final String DARK_TEXT = "#FFFFFF";

    // ==================== THEMES ====================
    public static final String THEME_LIGHT = "light";
    public static final String THEME_DARK = "dark";

    // ==================== PATHS ====================
    public static final String RESOURCES_PATH = "/resources/";
    public static final String CSS_PATH = RESOURCES_PATH + "css/";
    public static final String IMAGES_PATH = RESOURCES_PATH + "images/";
    public static final String ICONS_PATH = IMAGES_PATH + "icons/";
    public static final String SOUNDS_PATH = RESOURCES_PATH + "sounds/";
    public static final String FONTS_PATH = RESOURCES_PATH + "fonts/";

    // ==================== DEFAULT FILES ====================
    public static final String DEFAULT_AVATAR = "default-avatar.png";
    public static final String APP_LOGO = "logo.png";
    public static final String CHAT_BACKGROUND = "chat-bg.png";

    public static final String DEFAULT_AVATAR_URL = "https://ui-avatars.com/api/?background=0084ff&color=fff&name=";

    // ==================== SERVER CONFIG ====================
    public static final String SERVER_HOST = "localhost";
    public static final int SERVER_PORT = 8888;
    public static final int REQUEST_TIMEOUT = 10000; // 10 seconds

    // ==================== PAGINATION ====================
    public static final int MESSAGES_PAGE_SIZE = 50;
    public static final int CONVERSATIONS_PAGE_SIZE = 30;
    public static final int SEARCH_RESULTS_LIMIT = 20;

    // ==================== TIMEOUTS ====================
    public static final int SOCKET_TIMEOUT = 30000; // 30 seconds
    public static final int CONNECTION_TIMEOUT = 30000; // 30 seconds
    public static final int READ_TIMEOUT = 30000; // 30 seconds
    public static final int RECONNECT_DELAY_MS = 5000;
    public static final int MAX_RECONNECT_ATTEMPTS = 5;
    public static final int TYPING_INDICATOR_TIMEOUT = 3000; // 3 seconds

    // ==================== DATE TIME FORMATS ====================
    public static final String DATE_FORMAT = "dd/MM/yyyy";
    public static final String TIME_FORMAT = "HH:mm";
    public static final String DATETIME_FORMAT = "dd/MM/yyyy HH:mm";
    public static final String FULL_DATETIME_FORMAT = "dd/MM/yyyy HH:mm:ss";

    // ==================== ERROR MESSAGES ====================
    public static final String ERROR_NETWORK = "Lỗi kết nối mạng. Vui lòng kiểm tra kết nối của bạn.";
    public static final String ERROR_SERVER = "Lỗi server. Vui lòng thử lại sau.";
    public static final String ERROR_TIMEOUT = "Hết thời gian chờ. Vui lòng thử lại.";
    public static final String ERROR_INVALID_CREDENTIALS = "Tên đăng nhập hoặc mật khẩu không đúng.";
    public static final String ERROR_USERNAME_EXISTS = "Tên đăng nhập đã tồn tại.";
    public static final String ERROR_EMAIL_EXISTS = "Email đã được sử dụng.";
    public static final String ERROR_INVALID_EMAIL = "Email không hợp lệ.";
    public static final String ERROR_INVALID_PHONE = "Số điện thoại không hợp lệ.";
    public static final String ERROR_WEAK_PASSWORD = "Mật khẩu quá yếu. Vui lòng sử dụng mật khẩu mạnh hơn.";
    public static final String ERROR_PASSWORD_MISMATCH = "Mật khẩu xác nhận không khớp.";
    public static final String ERROR_REQUIRED_FIELD = "Vui lòng điền đầy đủ thông tin.";
    public static final String ERROR_FILE_TOO_LARGE = "File quá lớn. Kích thước tối đa: ";
    public static final String ERROR_INVALID_FILE_TYPE = "Loại file không được hỗ trợ.";

    // ==================== SUCCESS MESSAGES ====================
    public static final String SUCCESS_LOGIN = "Đăng nhập thành công!";
    public static final String SUCCESS_REGISTER = "Đăng ký thành công!";
    public static final String SUCCESS_LOGOUT = "Đăng xuất thành công!";
    public static final String SUCCESS_PASSWORD_RESET = "Mật khẩu đã được đặt lại. Vui lòng kiểm tra email.";
    public static final String SUCCESS_EMAIL_VERIFIED = "Email đã được xác thực thành công!";
    public static final String SUCCESS_PROFILE_UPDATED = "Cập nhật thông tin thành công!";
    public static final String SUCCESS_PASSWORD_CHANGED = "Đổi mật khẩu thành công!";
    public static final String SUCCESS_MESSAGE_SENT = "Tin nhắn đã được gửi!";

    // ==================== INFO MESSAGES ====================
    public static final String INFO_LOADING = "Đang tải...";
    public static final String INFO_CONNECTING = "Đang kết nối...";
    public static final String INFO_RECONNECTING = "Đang kết nối lại...";
    public static final String INFO_DISCONNECTED = "Mất kết nối với server.";
    public static final String INFO_NO_MESSAGES = "Chưa có tin nhắn nào.";
    public static final String INFO_NO_CONTACTS = "Chưa có liên hệ nào.";
    public static final String INFO_TYPING = "đang nhập...";

    // ==================== DIALOG TITLES ====================
    public static final String DIALOG_ERROR = "Lỗi";
    public static final String DIALOG_SUCCESS = "Thành công";
    public static final String DIALOG_WARNING = "Cảnh báo";
    public static final String DIALOG_CONFIRM = "Xác nhận";
    public static final String DIALOG_INFO = "Thông tin";

    // ==================== BUTTON LABELS ====================
    public static final String BTN_LOGIN = "Đăng nhập";
    public static final String BTN_REGISTER = "Đăng ký";
    public static final String BTN_LOGOUT = "Đăng xuất";
    public static final String BTN_SEND = "Gửi";
    public static final String BTN_CANCEL = "Hủy";
    public static final String BTN_OK = "OK";
    public static final String BTN_YES = "Có";
    public static final String BTN_NO = "Không";
    public static final String BTN_SAVE = "Lưu";
    public static final String BTN_DELETE = "Xóa";
    public static final String BTN_EDIT = "Sửa";
    public static final String BTN_SEARCH = "Tìm kiếm";
    public static final String BTN_UPLOAD = "Tải lên";
    public static final String BTN_DOWNLOAD = "Tải xuống";

    // Private constructor to prevent instantiation
    private Constants() {
        throw new AssertionError("Cannot instantiate Constants class");
    }
}
