package server.handlers;

import database.dao.MessageDAO;
import database.dao.ConversationDAO;
import database.dao.UserDAO;
import models.Message;
import models.Conversation;
import models.User;
import protocol.Protocol;
import server.ClientHandler;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.io.*;
import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

public class FileHandler {

    private final ClientHandler clientHandler;
    private static final String UPLOAD_DIR = "uploads/";

    // Giới hạn kích thước file theo loại
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024;
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024;
    private static final long MAX_AUDIO_SIZE = 20 * 1024 * 1024;
    private static final long MAX_DOCUMENT_SIZE = 50 * 1024 * 1024;
    private static final long MAX_ARCHIVE_SIZE = 100 * 1024 * 1024;

    // Cloudinary instance
    private static Cloudinary cloudinary;

    static {
        try {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "dnkjhbw9m",
                    "api_key", "791292363868727",
                    "api_secret", "_5aBOAaLNCUabVPcyZMxwH-j1yY"
            ));
            System.out.println("✓ Cloudinary đã được khởi tạo");
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi khởi tạo Cloudinary: " + e.getMessage());
        }
    }

    public FileHandler(ClientHandler clientHandler) {
        this.clientHandler = clientHandler;
        initializeUploadDirectory();
    }

    private void initializeUploadDirectory() {
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                Files.createDirectories(uploadPath.resolve("videos"));
                Files.createDirectories(uploadPath.resolve("audio"));
                Files.createDirectories(uploadPath.resolve("documents"));
                Files.createDirectories(uploadPath.resolve("archives"));
            }
        } catch (IOException e) {
            System.err.println("Lỗi tạo thư mục uploads: " + e.getMessage());
        }
    }

    public void handle(String command, String[] parts) {
        switch (command) {
            case Protocol.FILE_UPLOAD:
                handleFileUpload(parts);
                break;
            case Protocol.FILE_DOWNLOAD:
                handleFileDownload(parts);
                break;
            case Protocol.FILE_DELETE:
                handleFileDelete(parts);
                break;
            case Protocol.FILE_GET_INFO:
                handleFileInfo(parts);
                break;
            default:
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Lệnh file không xác định"
                ));
        }
    }

    // ==================== UPLOAD FILE ====================

    /**
     * FIXED: Broadcast MESSAGE_RECEIVE cho TẤT CẢ members (bao gồm người gửi)
     */
    private void handleFileUpload(String[] parts) {
        if (parts.length < 6) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Dữ liệu upload không hợp lệ"
            ));
            return;
        }

        String conversationId = parts[1];
        String senderId = parts[2];
        String fileName = parts[3];
        String fileType = parts[4];
        long fileSize = Long.parseLong(parts[5]);

        // Kiểm tra quyền truy cập
        Conversation conversation = ConversationDAO.findById(conversationId);
        if (conversation == null) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_NOT_FOUND,
                    "Không tìm thấy cuộc trò chuyện"
            ));
            return;
        }

        if (!conversation.hasMember(senderId)) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.FORBIDDEN,
                    "Bạn không phải thành viên"
            ));
            return;
        }

        // Kiểm tra kích thước
        if (!validateFileSize(fileType, fileSize)) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Kích thước file vượt quá giới hạn"
            ));
            return;
        }

        try {
            // Đọc file data
            byte[] fileData = readFileData(fileSize);

            String fileUrl;

            // Upload ảnh lên Cloudinary
            if (fileType.equalsIgnoreCase("image")) {
                fileUrl = uploadImageToCloudinary(fileData, fileName);
                if (fileUrl == null) {
                    clientHandler.sendMessage(Protocol.buildErrorResponse(
                            Protocol.ERR_SERVER_ERROR,
                            "Lỗi upload ảnh lên Cloudinary"
                    ));
                    return;
                }
                System.out.println("✓ Ảnh đã upload lên Cloudinary: " + fileUrl);
            }
            // Lưu file khác vào local
            else {
                String fileId = UUID.randomUUID().toString();
                String extension = getFileExtension(fileName);
                String storedFileName = fileId + extension;
                String subDir = getSubDirectory(fileType);
                Path filePath = Paths.get(UPLOAD_DIR, subDir, storedFileName);

                Files.write(filePath, fileData);
                fileUrl = generateLocalFileUrl(subDir, storedFileName);
                System.out.println("✓ File đã lưu local: " + filePath);
            }

            // Lấy thông tin người gửi
            User sender = UserDAO.findById(senderId);
            if (sender == null) {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Không tìm thấy người gửi"
                ));
                return;
            }

            // Tạo message
            Message message = new Message(conversationId, senderId, "");
            message.setMessageType(getMessageType(fileType));
            message.setMediaUrl(fileUrl);
            message.setFileName(fileName);
            message.setFileSize(fileSize);
            message.setSenderName(sender.getDisplayName());
            message.setSenderAvatar(sender.getAvatarUrl());

            // Lưu vào database
            if (MessageDAO.createMessage(message)) {

                // Gửi SUCCESS response cho người gửi
                clientHandler.sendMessage(Protocol.buildSuccessResponse(
                        "Upload file thành công",
                        fileUrl
                ));

                // ===== QUAN TRỌNG: BROADCAST CHO TẤT CẢ (bao gồm người gửi) =====
                broadcastFileMessageToAll(conversation, message);

                System.out.println("✓ File đã upload và broadcast: " + fileName);
            } else {
                // Xóa file nếu lưu message thất bại
                if (!fileType.equalsIgnoreCase("image")) {
                    Path filePath = getFilePathFromUrl(fileUrl);
                    Files.deleteIfExists(filePath);
                }
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_DATABASE_ERROR,
                        "Lỗi lưu message file"
                ));
            }

        } catch (IOException e) {
            System.err.println("Lỗi upload file: " + e.getMessage());
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Upload file thất bại: " + e.getMessage()
            ));
        }
    }

    // ==================== CLOUDINARY ====================

    private String uploadImageToCloudinary(byte[] imageData, String fileName) {
        if (cloudinary == null) {
            System.err.println("⚠️ Cloudinary chưa được cấu hình");
            return null;
        }

        try {
            String publicId = "chat_images/" + UUID.randomUUID().toString();

            Map uploadResult = cloudinary.uploader().upload(imageData, ObjectUtils.asMap(
                    "public_id", publicId,
                    "folder", "chat_images",
                    "resource_type", "image",
                    "overwrite", false,
                    "use_filename", false
            ));

            String imageUrl = (String) uploadResult.get("secure_url");
            System.out.println("✓ Upload Cloudinary thành công: " + imageUrl);
            return imageUrl;

        } catch (Exception e) {
            System.err.println("⚠️ Lỗi upload lên Cloudinary: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    // ==================== DOWNLOAD FILE ====================

    private void handleFileDownload(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Yêu cầu download không hợp lệ"
            ));
            return;
        }

        String fileUrl = parts[1];

        // Nếu là Cloudinary URL
        if (fileUrl.contains("cloudinary.com")) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "File là ảnh từ Cloudinary, download trực tiếp từ URL",
                    fileUrl
            ));
            return;
        }

        // Download file local
        try {
            Path filePath = getFilePathFromUrl(fileUrl);

            if (!Files.exists(filePath)) {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_NOT_FOUND,
                        "Không tìm thấy file"
                ));
                return;
            }

            byte[] fileData = Files.readAllBytes(filePath);

            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "File sẵn sàng download",
                    String.valueOf(fileData.length)
            ));

            sendFileData(fileData);

        } catch (IOException e) {
            System.err.println("Lỗi download file: " + e.getMessage());
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Download file thất bại: " + e.getMessage()
            ));
        }
    }

    // ==================== DELETE FILE ====================

    private void handleFileDelete(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Yêu cầu xóa không hợp lệ"
            ));
            return;
        }

        String fileUrl = parts[1];

        if (fileUrl.contains("cloudinary.com")) {
            boolean deleted = deleteImageFromCloudinary(fileUrl);
            if (deleted) {
                clientHandler.sendMessage(Protocol.buildSuccessResponse("Đã xóa ảnh"));
            } else {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_SERVER_ERROR,
                        "Lỗi xóa ảnh"
                ));
            }
            return;
        }

        try {
            Path filePath = getFilePathFromUrl(fileUrl);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                clientHandler.sendMessage(Protocol.buildSuccessResponse("Đã xóa file"));
            } else {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_NOT_FOUND,
                        "Không tìm thấy file"
                ));
            }

        } catch (IOException e) {
            System.err.println("Lỗi xóa file: " + e.getMessage());
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Xóa file thất bại"
            ));
        }
    }

    private boolean deleteImageFromCloudinary(String imageUrl) {
        if (cloudinary == null) return false;

        try {
            String publicId = extractPublicIdFromUrl(imageUrl);
            if (publicId != null) {
                Map result = cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
                return "ok".equals(result.get("result"));
            }
            return false;
        } catch (Exception e) {
            System.err.println("⚠️ Lỗi xóa ảnh: " + e.getMessage());
            return false;
        }
    }

    private String extractPublicIdFromUrl(String url) {
        try {
            String[] parts = url.split("/upload/");
            if (parts.length < 2) return null;

            String afterUpload = parts[1];
            afterUpload = afterUpload.replaceFirst("v\\d+/", "");

            int lastDot = afterUpload.lastIndexOf('.');
            if (lastDot > 0) {
                afterUpload = afterUpload.substring(0, lastDot);
            }

            return afterUpload;
        } catch (Exception e) {
            return null;
        }
    }

    // ==================== FILE INFO ====================

    private void handleFileInfo(String[] parts) {
        if (parts.length < 2) {
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Yêu cầu không hợp lệ"
            ));
            return;
        }

        String fileUrl = parts[1];

        if (fileUrl.contains("cloudinary.com")) {
            clientHandler.sendMessage(Protocol.buildSuccessResponse(
                    "File từ Cloudinary",
                    "0",
                    "image/*"
            ));
            return;
        }

        try {
            Path filePath = getFilePathFromUrl(fileUrl);

            if (Files.exists(filePath)) {
                long size = Files.size(filePath);
                String mimeType = Files.probeContentType(filePath);

                clientHandler.sendMessage(Protocol.buildSuccessResponse(
                        "Đã lấy thông tin file",
                        String.valueOf(size),
                        mimeType != null ? mimeType : "application/octet-stream"
                ));
            } else {
                clientHandler.sendMessage(Protocol.buildErrorResponse(
                        Protocol.ERR_NOT_FOUND,
                        "Không tìm thấy file"
                ));
            }

        } catch (IOException e) {
            System.err.println("Lỗi lấy thông tin file: " + e.getMessage());
            clientHandler.sendMessage(Protocol.buildErrorResponse(
                    Protocol.ERR_SERVER_ERROR,
                    "Lỗi lấy thông tin file"
            ));
        }
    }

    // ==================== HELPER METHODS ====================

    private boolean validateFileSize(String fileType, long fileSize) {
        return switch (fileType.toLowerCase()) {
            case "image" -> fileSize <= MAX_IMAGE_SIZE;
            case "video" -> fileSize <= MAX_VIDEO_SIZE;
            case "audio" -> fileSize <= MAX_AUDIO_SIZE;
            case "document" -> fileSize <= MAX_DOCUMENT_SIZE;
            case "archive" -> fileSize <= MAX_ARCHIVE_SIZE;
            default -> fileSize <= MAX_DOCUMENT_SIZE;
        };
    }

    private String getSubDirectory(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "video" -> "videos";
            case "audio" -> "audio";
            case "document" -> "documents";
            case "archive" -> "archives";
            default -> "files";
        };
    }

    private String getMessageType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "image" -> "image";
            case "video" -> "video";
            case "audio" -> "audio";
            default -> "file";
        };
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(dotIndex) : "";
    }

    private String generateLocalFileUrl(String subDir, String fileName) {
        return "http://localhost:8080/uploads/" + subDir + "/" + fileName;
    }

    private Path getFilePathFromUrl(String fileUrl) {
        String relativePath = fileUrl.substring(fileUrl.indexOf("/uploads/") + 9);
        return Paths.get(UPLOAD_DIR, relativePath);
    }

    private byte[] readFileData(long fileSize) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        InputStream in = clientHandler.getRawInputStream();
        byte[] buffer = new byte[8192];
        long totalRead = 0;

        System.out.println("→ Đang đọc file data: " + fileSize + " bytes");

        while (totalRead < fileSize) {
            int toRead = (int) Math.min(buffer.length, fileSize - totalRead);
            int read = in.read(buffer, 0, toRead);

            if (read == -1) {
                throw new IOException("Client ngắt kết nối. Đã nhận " + totalRead + "/" + fileSize + " bytes");
            }

            baos.write(buffer, 0, read);
            totalRead += read;
        }

        System.out.println("✓ Đã đọc xong " + totalRead + " bytes");
        return baos.toByteArray();
    }

    private void sendFileData(byte[] data) throws IOException {
        OutputStream out = clientHandler.getRawOutputStream();
        out.write(data);
        out.flush();
    }

    /**
     * FIXED: Broadcast MESSAGE_RECEIVE cho TẤT CẢ members (bao gồm người gửi)
     * Điều này đảm bảo người gửi cũng nhận được message và hiển thị đồng bộ
     */
    private void broadcastFileMessageToAll(Conversation conversation, Message message) {
        String broadcastMsg = Protocol.buildRequest(
                Protocol.MESSAGE_RECEIVE,
                message.getMessageId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getContent() != null ? message.getContent() : "",
                message.getMessageType(),
                message.getMediaUrl() != null ? message.getMediaUrl() : "",
                message.getSenderName(),
                message.getSenderAvatar() != null ? message.getSenderAvatar() : "",
                message.getFileName() != null ? message.getFileName() : "",
                String.valueOf(message.getFileSize())
        );

        System.out.println("→ Broadcasting MESSAGE_RECEIVE to all members:");

        // Broadcast cho TẤT CẢ members (KHÔNG loại trừ người gửi)
        for (String memberId : conversation.getMemberIds()) {
            ClientHandler handler = clientHandler.getServer().getClientHandler(memberId);
            if (handler != null) {
                handler.sendMessage(broadcastMsg);
                System.out.println("   ✓ Sent to: " + memberId);
            }
        }
    }
}