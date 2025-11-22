package utils;

import config.ServerConfig;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Date;
import java.util.Properties;

/**
 * Lớp tiện ích để gửi email sử dụng Jakarta Mail 2.0.1
 */
public class EmailUtil {

    // Cấu hình SMTP lấy từ ServerConfig
    private static final String SMTP_HOST = ServerConfig.getEmailHost();
    private static final String SMTP_PORT = ServerConfig.getEmailPort();
    private static final String SMTP_USERNAME = ServerConfig.getEmailUsername();
    private static final String SMTP_PASSWORD = ServerConfig.getEmailPassword();
    private static final String FROM_EMAIL = ServerConfig.getEmailUsername();
    private static final String FROM_NAME = ServerConfig.getEmailFromName();

    /**
     * Tạo và trả về phiên làm việc (Session) với SMTP server, có xác thực
     */
    private static Session getMailSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST); // máy chủ SMTP
        props.put("mail.smtp.port", SMTP_PORT); // cổng SMTP
        props.put("mail.smtp.auth", "true"); // bật xác thực
        props.put("mail.smtp.starttls.enable", "true"); // bật STARTTLS
        props.put("mail.smtp.ssl.protocols", "TLSv1.2"); // phiên bản TLS
        props.put("mail.smtp.ssl.trust", SMTP_HOST); // tin cậy host

        // Tạo Session với xác thực username/password
        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(SMTP_USERNAME, SMTP_PASSWORD);
            }
        });
    }

    /**
     * Hàm gửi email chung (HTML)
     * @param toEmail: email người nhận
     * @param subject: tiêu đề email
     * @param htmlContent: nội dung email dạng HTML
     * @return true nếu gửi thành công, false nếu thất bại
     */
    public static boolean sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            Session session = getMailSession();
            MimeMessage message = new MimeMessage(session);

            message.setFrom(new InternetAddress(FROM_EMAIL, FROM_NAME)); // người gửi
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail)); // người nhận
            message.setSubject(subject); // tiêu đề
            message.setSentDate(new Date()); // ngày gửi
            message.setContent(htmlContent, "text/html; charset=utf-8"); // nội dung HTML

            Transport.send(message); // gửi email
            System.out.println("✓ Email đã được gửi tới: " + toEmail);
            return true;

        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            System.err.println("❌ Gửi email thất bại: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gửi email xác thực (Verification Code)
     */
    public static boolean sendVerificationCode(String toEmail, String code, String username) {
        String subject = "Mã Xác Thực Email";
        String htmlContent = buildVerificationEmailHTML(username, code);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email chào mừng sau khi xác thực thành công
     */
    public static boolean sendWelcomeEmail(String toEmail, String username) {
        String subject = "Chào mừng đến với ChatApp!";
        String htmlContent = buildWelcomeEmailHTML(username);
        return sendEmail(toEmail, subject, htmlContent);
    }

    /**
     * Gửi email yêu cầu đặt lại mật khẩu
     */
    public static boolean sendPasswordResetEmail(String toEmail, String token, String username) {
        String subject = "Yêu cầu đặt lại mật khẩu";
        String htmlContent = buildPasswordResetEmailHTML(username, token);
        return sendEmail(toEmail, subject, htmlContent);
    }

    // ------------------- Hàm xây dựng nội dung HTML -------------------

    /**
     * Xây dựng HTML email xác thực
     */
    private static String buildVerificationEmailHTML(String username, String code) {
        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; line-height: 1.6; color: #333; }" +
                ".container { max-width: 600px; margin: 0 auto; padding: 20px; }" +
                ".header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; border-radius: 10px 10px 0 0; }" +
                ".content { background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; }" +
                ".code-box { background: white; border: 2px dashed #667eea; padding: 20px; margin: 20px 0; text-align: center; border-radius: 8px; }" +
                ".code { font-size: 32px; font-weight: bold; color: #667eea; letter-spacing: 5px; }" +
                ".footer { text-align: center; margin-top: 20px; color: #777; font-size: 12px; }" +
                ".warning { background: #fff3cd; border-left: 4px solid #ffc107; padding: 10px; margin: 20px 0; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<div class='header'><h1>Xác Thực Email</h1></div>" +
                "<div class='content'>" +
                "<h2>Xin chào " + username + ",</h2>" +
                "<p>Cảm ơn bạn đã đăng ký ChatApp! Mã xác thực của bạn là:</p>" +
                "<div class='code-box'><div class='code'>" + code + "</div></div>" +
                "<p>Mã này sẽ hết hạn trong <strong>15 phút</strong>.</p>" +
                "<div class='warning'><strong>⚠️ Lưu ý bảo mật:</strong> Nếu bạn không yêu cầu mã này, hãy bỏ qua email này.</div>" +
                "<p>Thân mến,<br>Đội ngũ ChatApp</p>" +
                "</div>" +
                "<div class='footer'><p>Đây là email tự động. Vui lòng không trả lời.</p></div>" +
                "</div>" +
                "</body></html>";
    }

    /**
     * Xây dựng HTML email chào mừng
     */
    private static String buildWelcomeEmailHTML(String username) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>body{font-family:Arial,sans-serif;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}" +
                ".header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;padding:30px;text-align:center;border-radius:10px 10px 0 0}" +
                ".content{background:#f9f9f9;padding:30px;border-radius:0 0 10px 10px}.feature-list{list-style:none;padding:0}" +
                ".feature-list li{padding:10px 0;padding-left:30px;position:relative}.feature-list li:before{content:'✓';position:absolute;left:0;color:#667eea;font-weight:bold;font-size:18px}" +
                ".footer{text-align:center;margin-top:20px;color:#777;font-size:12px}</style></head><body>" +
                "<div class='container'><div class='header'><h1>🎉 Chào mừng đến với ChatApp!</h1></div>" +
                "<div class='content'><h2>Xin chào " + username + ",</h2>" +
                "<p>Email của bạn đã được xác thực thành công! Bây giờ bạn có thể bắt đầu trò chuyện với bạn bè.</p>" +
                "<h3>Bạn có thể làm gì:</h3><ul class='feature-list'>" +
                "<li>Gửi tin nhắn nhanh đến liên hệ</li><li>Tạo và tham gia nhóm chat</li><li>Chia sẻ file và media</li>" +
                "<li>Tùy chỉnh hồ sơ cá nhân</li><li>Kết nối mọi lúc, mọi nơi</li></ul>" +
                "<p>Thân mến,<br>Đội ngũ ChatApp</p></div>" +
                "<div class='footer'><p>Đây là email tự động. Vui lòng không trả lời.</p></div></div></body></html>";
    }

    /**
     * Xây dựng HTML email đặt lại mật khẩu
     */
    private static String buildPasswordResetEmailHTML(String username, String token) {
        String resetLink = "http://localhost:3000/reset-password?token=" + token;
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'><meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<style>body{font-family:Arial,sans-serif;color:#333}.container{max-width:600px;margin:0 auto;padding:20px}" +
                ".header{background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);color:white;padding:30px;text-align:center;border-radius:10px 10px 0 0}" +
                ".content{background:#f9f9f9;padding:30px;border-radius:0 0 10px 10px}.button{display:inline-block;background:#667eea;color:white;padding:15px 40px;text-decoration:none;border-radius:5px;margin:20px 0;font-weight:bold}" +
                ".token-box{background:white;border:1px solid #ddd;padding:15px;margin:20px 0;border-radius:5px;word-break:break-all;font-family:monospace;font-size:14px}" +
                ".warning{background:#fff3cd;border-left:4px solid #ffc107;padding:15px;margin:20px 0}" +
                ".danger{background:#f8d7da;border-left:4px solid #dc3545;padding:15px;margin:20px 0}" +
                ".footer{text-align:center;margin-top:20px;color:#777;font-size:12px}</style></head><body>" +
                "<div class='container'><div class='header'><h1>🔐 Yêu cầu đặt lại mật khẩu</h1></div>" +
                "<div class='content'><h2>Xin chào " + username + ",</h2>" +
                "<p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu của bạn. Nhấn nút bên dưới để tạo mật khẩu mới:</p>" +
                "<div style='text-align:center'><a href='" + resetLink + "' class='button'>Đặt lại mật khẩu</a></div>" +
                "<p>Hoặc sao chép đường dẫn này vào trình duyệt:</p><div class='token-box'>" + resetLink + "</div>" +
                "<div class='warning'><strong>⏰ Lưu ý:</strong> Liên kết sẽ hết hạn trong <strong>1 giờ</strong>.</div>" +
                "<div class='danger'><strong>⚠️ Cảnh báo bảo mật:</strong> Nếu bạn không yêu cầu, hãy bỏ qua email này và đảm bảo tài khoản an toàn.</div>" +
                "<p>Thân mến,<br>Đội ngũ ChatApp</p></div>" +
                "<div class='footer'><p>Đây là email tự động. Vui lòng không trả lời.</p></div></div></body></html>";
    }
}
