package utils;

import config.ServerConfig;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Monitor ZeroTier cho server - Improved version
 */
public class ZeroTierMonitor {

    private Timer monitorTimer;
    private boolean isRunning = false;
    private boolean hasWarned = false; // Tránh warning lặp lại

    /**
     * Bắt đầu monitor ZeroTier
     */
    public void startMonitoring() {
        if (!ServerConfig.isZeroTierEnabled()) {
            System.out.println("ZeroTier đã bị tắt trong cấu hình");
            return;
        }

        if (isRunning) {
            System.out.println("ZeroTier monitor đang chạy");
            return;
        }

        // Kiểm tra ban đầu
        checkZeroTierStatus();

        // Lên lịch kiểm tra định kỳ
        monitorTimer = new Timer(true);
        long interval = ServerConfig.getZeroTierCheckInterval();

        monitorTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                checkZeroTierStatus();
            }
        }, interval, interval);

        isRunning = true;
        System.out.println("ZeroTier monitor đã khởi động (kiểm tra mỗi " + (interval/1000) + " giây)");
    }

    /**
     * Dừng monitoring
     */
    public void stopMonitoring() {
        if (monitorTimer != null) {
            monitorTimer.cancel();
            monitorTimer = null;
        }
        isRunning = false;
        System.out.println("ZeroTier monitor đã dừng");
    }

    /**
     * Kiểm tra trạng thái ZeroTier - Improved
     */
    private void checkZeroTierStatus() {
        String networkId = ServerConfig.getZeroTierNetworkId();

        // Nếu đã có IP ZeroTier thì coi như OK, không cần check join nữa
        String ip = getZeroTierIP();
        if (ip != null) {
            hasWarned = false; // Reset warning flag
            return; // Đã có IP => đã join và authorized
        }

        // Chỉ kiểm tra và warning nếu chưa có IP
        if (!hasWarned) {
            if (!isJoinedNetwork(networkId)) {
                System.err.println("⚠️ Chưa join ZeroTier network: " + networkId);
                System.out.println("ℹ️ Chạy lệnh: zerotier-cli join " + networkId);
            } else {
                // Đã join nhưng chưa có IP => chưa authorized
                System.err.println("⚠️ Không tìm thấy ZeroTier IP");
                System.out.println("ℹ️ Kiểm tra thiết bị đã được authorized trên ZeroTier Central chưa");
            }
            hasWarned = true; // Chỉ warning 1 lần
        }
    }

    /**
     * Lấy tất cả IP ZeroTier
     */
    public List<String> getZeroTierIPs() {
        List<String> ztIPs = new ArrayList<>();

        try {
            // Cách 1: Thử lấy từ zerotier-cli
            String ip = getIPFromCLI();
            if (ip != null) {
                ztIPs.add(ip);
                return ztIPs;
            }

            // Cách 2: Quét tất cả network interfaces
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                // Bỏ qua interface không hoạt động
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }

                String ifaceName = iface.getName().toLowerCase();
                String displayName = iface.getDisplayName().toLowerCase();

                // Kiểm tra tên interface hoặc display name có chứa zerotier không
                if (ifaceName.contains("zt") || ifaceName.contains("zerotier") ||
                        displayName.contains("zerotier") || displayName.contains("zt")) {

                    Enumeration<InetAddress> addresses = iface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        // Chỉ lấy IPv4
                        if (addr.getAddress().length == 4 && !addr.isLoopbackAddress()) {
                            ztIPs.add(addr.getHostAddress());
                        }
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy ZeroTier IP: " + e.getMessage());
        }

        return ztIPs;
    }

    /**
     * Lấy IP từ zerotier-cli command - Improved
     */
    private String getIPFromCLI() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("zerotier-cli.bat", "listnetworks");
            } else {
                pb = new ProcessBuilder("zerotier-cli", "listnetworks");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String networkId = ServerConfig.getZeroTierNetworkId();
            String line;

            while ((line = reader.readLine()) != null) {
                // Format có thể là:
                // 200 listnetworks <nwid> <name> <mac> <status> <type> <dev> <ips>
                // hoặc chỉ đơn giản là dòng có network ID

                if (line.contains(networkId)) {
                    // Tìm IP address trong dòng (format: xxx.xxx.xxx.xxx)
                    String[] parts = line.split("\\s+");
                    for (String part : parts) {
                        // Kiểm tra xem có phải IP không (x.x.x.x hoặc x.x.x.x/xx)
                        if (part.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(/\\d{1,2})?")) {
                            String ip = part.split("/")[0]; // Bỏ subnet mask nếu có
                            // Validate IP
                            if (isValidIP(ip)) {
                                reader.close();
                                return ip;
                            }
                        }
                    }
                }
            }

            reader.close();
            process.waitFor();

        } catch (Exception e) {
            // Không làm gì, sẽ thử cách khác
        }

        return null;
    }

    /**
     * Validate IP address
     */
    private boolean isValidIP(String ip) {
        try {
            String[] parts = ip.split("\\.");
            if (parts.length != 4) return false;

            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Lấy IP ZeroTier đầu tiên
     */
    public String getZeroTierIP() {
        List<String> ips = getZeroTierIPs();
        return ips.isEmpty() ? null : ips.get(0);
    }

    /**
     * Kiểm tra đã join network chưa - Improved
     */
    private boolean isJoinedNetwork(String networkId) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("zerotier-cli.bat", "listnetworks");
            } else {
                pb = new ProcessBuilder("zerotier-cli", "listnetworks");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line;
            boolean found = false;

            while ((line = reader.readLine()) != null) {
                if (line.contains(networkId)) {
                    found = true;
                    break;
                }
            }

            reader.close();
            process.waitFor();

            return found;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * In thông tin ZeroTier - với format rõ ràng hơn
     */
    public void printZeroTierInfo() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("zerotier-cli.bat", "listnetworks");
            } else {
                pb = new ProcessBuilder("zerotier-cli", "listnetworks");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            System.out.println("\n========== ZeroTier Networks ==========");
            String line;
            int lineCount = 0;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
                lineCount++;
            }

            if (lineCount == 0) {
                System.out.println("(Không có output từ zerotier-cli)");
            }

            System.out.println("=======================================\n");

            reader.close();
            process.waitFor();

            // Debug: In tất cả network interfaces
            System.out.println("========== Network Interfaces ==========");
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (iface.isUp() && !iface.isLoopback()) {
                    System.out.println("Interface: " + iface.getName() + " - " + iface.getDisplayName());
                    Enumeration<InetAddress> addresses = iface.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr.getAddress().length == 4) {
                            System.out.println("  IP: " + addr.getHostAddress());
                        }
                    }
                }
            }
            System.out.println("=========================================\n");

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin network: " + e.getMessage());
        }
    }

    /**
     * Lấy IP local
     */
    public static String getLocalIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            return "127.0.0.1";
        }
    }

    /**
     * Kiểm tra đang chạy
     */
    public boolean isRunning() {
        return isRunning;
    }
}