package org.example.chatappclient.client.services;

import org.example.chatappclient.client.SocketClient;
import org.example.chatappclient.client.models.User;
import org.example.chatappclient.client.protocol.Protocol;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service xử lý user profile, tìm kiếm và cache local để tránh gọi server quá nhiều
 */
public class UserService {

    private static volatile UserService instance;
    private final SocketClient socketClient;

    // ==================== CACHE LOCAL ====================
    private final Map<String, User> userCache = new ConcurrentHashMap<>();

    private UserService() {
        socketClient = SocketClient.getInstance();
    }

    public static UserService getInstance() {
        if (instance == null) {
            synchronized (UserService.class) {
                if (instance == null) {
                    instance = new UserService();
                }
            }
        }
        return instance;
    }

    // ==================== CACHE HANDLERS ====================

    /**
     * Lấy user từ cache, trả về null nếu chưa có
     */
    public User getUser(String userId) {
        return userCache.get(userId);
    }

    /**
     * Lưu user vào cache
     */
    public void cacheUser(User user) {
        if (user != null && user.getUserId() != null) {
            userCache.put(user.getUserId(), user);
        }
    }

    // ==================== USER OPERATIONS ====================

    /**
     * Lấy profile từ server và cache vào local
     */
    public User getProfile(String userId) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_GET_PROFILE, userId);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        User user = parseUser(Protocol.getData(response));
        cacheUser(user); // lưu vào cache
        return user;
    }

    /**
     * Cập nhật profile và cache lại
     */
    public User updateProfile(String userId, String displayName, String bio) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_UPDATE_PROFILE, userId, displayName, bio);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        User user = parseUser(Protocol.getData(response));
        cacheUser(user);
        return user;
    }

    /**
     * Cập nhật avatar và cache lại
     */
    public String updateAvatar(String userId, String avatarUrl) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_UPDATE_AVATAR, userId, avatarUrl);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        // Cập nhật avatar vào cache nếu có
        User cached = userCache.get(userId);
        if (cached != null) cached.setAvatarUrl(avatarUrl);

        return Protocol.getData(response);
    }

    /**
     * Đổi mật khẩu
     */
    public void changePassword(String userId, String oldPassword, String newPassword) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_CHANGE_PASSWORD, userId, oldPassword, newPassword);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));
    }

    /**
     * Tìm kiếm user theo query (tên, email, số điện thoại)
     * Lưu tất cả kết quả vào cache
     */
    public List<User> searchUsers(String query) throws Exception {
        String request = Protocol.buildRequest(Protocol.USER_SEARCH, query);
        String response = socketClient.sendRequest(request, 10000);

        if (response == null) throw new Exception("Server không phản hồi");
        if (!Protocol.isSuccess(response)) throw new Exception(Protocol.getErrorMessage(response));

        List<User> users = parseUsers(Protocol.getData(response));
        for (User u : users) cacheUser(u);
        return users;
    }

    /**
     * Tìm user theo email hoặc số điện thoại
     */
    public User findUserByEmailOrPhone(String query) throws Exception {
        List<User> users = searchUsers(query);
        if (users.isEmpty()) return null;
        return users.get(0);
    }

    /**
     * Cập nhật trạng thái online của user
     */
    public void updateOnlineStatus(String userId, String status) {
        String request = Protocol.buildRequest(Protocol.USER_UPDATE_STATUS, userId, status);
        socketClient.sendMessage(request);

        // Cập nhật cache nếu có
        User cached = userCache.get(userId);
        if (cached != null) cached.setStatus(status);
    }

    // ==================== PARSING ====================

    private List<User> parseUsers(String data) {
        List<User> list = new ArrayList<>();
        if (data == null || data.isEmpty()) return list;

        for (String userData : Protocol.parseDataList(data)) {
            User user = parseUser(userData);
            if (user != null) list.add(user);
        }
        return list;
    }

    private User parseUser(String data) {
        if (data == null || data.isEmpty()) return null;
        String[] f = Protocol.parseFields(data);
        if (f.length < 3) return null;

        User u = new User();
        u.setUserId(f[0]);
        u.setUsername(f[1]);
        u.setEmail(f[2]);
        if (f.length > 3) u.setDisplayName(f[3]);
        if (f.length > 4) u.setAvatarUrl(f[4]);
        if (f.length > 5) u.setPhone(f[5]);
        if (f.length > 6) u.setBio(f[6]);
        if (f.length > 7) u.setStatus(f[7]);
        if (f.length > 8) u.setOnline(Boolean.parseBoolean(f[8]));
        return u;
    }
}
