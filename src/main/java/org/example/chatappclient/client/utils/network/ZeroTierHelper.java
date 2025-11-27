package org.example.chatappclient.client.utils.network;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class để làm việc với ZeroTier - Improved version
 */
public class ZeroTierHelper {

    /**
     * Lấy tất cả IP ZeroTier trên máy
     */
    public static List<String> getZeroTierIPs() {
        List<String> ztIPs = new ArrayList<>();

        try {
            String ip = getIPFromCLI();
            if (ip != null) {
                ztIPs.add(ip);
                return ztIPs;
            }

            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();

            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }

                String ifaceName = iface.getName().toLowerCase();
                String displayName = iface.getDisplayName().toLowerCase();

                if (ifaceName.contains("zt") || ifaceName.contains("zerotier") ||
                        displayName.contains("zerotier") || displayName.contains("zt")) {

                    Enumeration<InetAddress> addresses = iface.getInetAddresses();

                    while (addresses.hasMoreElements()) {
                        InetAddress addr = addresses.nextElement();
                        if (addr.getAddress().length == 4 && !addr.isLoopbackAddress()) {
                            ztIPs.add(addr.getHostAddress());
                        }
                    }
                }
            }

        } catch (Exception e) {
        }

        return ztIPs;
    }

    /**
     * Lấy IP từ zerotier-cli (chính xác nhất)
     */
    private static String getIPFromCLI() {
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
            while ((line = reader.readLine()) != null) {
                // Tìm IP trong output (format: xxx.xxx.xxx.xxx hoặc xxx.xxx.xxx.xxx/xx)
                String[] parts = line.split("\\s+");
                for (String part : parts) {
                    if (part.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}(/\\d{1,2})?")) {
                        String ip = part.split("/")[0];
                        if (isValidIP(ip)) {
                            reader.close();
                            process.waitFor();
                            return ip;
                        }
                    }
                }
            }

            reader.close();
            process.waitFor();

        } catch (Exception e) {
            // Không in lỗi
        }

        return null;
    }

    /**
     * Validate IP address
     */
    private static boolean isValidIP(String ip) {
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
     * Lấy IP ZeroTier đầu tiên tìm thấy
     */
    public static String getZeroTierIP() {
        List<String> ips = getZeroTierIPs();
        return ips.isEmpty() ? null : ips.get(0);
    }

    /**
     * Kiểm tra ZeroTier đã cài đặt chưa
     */
    public static boolean isZeroTierInstalled() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("zerotier-cli.bat", "-v");
            } else {
                pb = new ProcessBuilder("zerotier-cli", "-v");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String line = reader.readLine();
            reader.close();
            process.waitFor();

            return line != null && !line.isEmpty();

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Kiểm tra đã join network chưa
     */
    public static boolean isJoinedNetwork(String networkId) {
        if (networkId == null || networkId.trim().isEmpty()) {
            return false;
        }

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
     * Join ZeroTier network
     */
    public static boolean joinNetwork(String networkId) {
        if (networkId == null || networkId.trim().isEmpty()) {
            System.err.println("❌ Network ID không hợp lệ");
            return false;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            System.out.println("Đang join ZeroTier network: " + networkId);

            if (os.contains("win")) {
                pb = new ProcessBuilder("zerotier-cli.bat", "join", networkId);
            } else {
                pb = new ProcessBuilder("zerotier-cli", "join", networkId);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Đã join network thành công!");
                return true;
            } else {
                System.err.println("❌ Join network thất bại!");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi join network: " + e.getMessage());
            return false;
        }
    }

    /**
     * Leave ZeroTier network
     */
    public static boolean leaveNetwork(String networkId) {
        if (networkId == null || networkId.trim().isEmpty()) {
            System.err.println("❌ Network ID không hợp lệ");
            return false;
        }

        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            System.out.println("Đang leave ZeroTier network: " + networkId);

            if (os.contains("win")) {
                pb = new ProcessBuilder("zerotier-cli.bat", "leave", networkId);
            } else {
                pb = new ProcessBuilder("zerotier-cli", "leave", networkId);
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            int exitCode = process.waitFor();

            if (exitCode == 0) {
                System.out.println("✅ Đã leave network thành công!");
                return true;
            } else {
                System.err.println("❌ Leave network thất bại!");
                return false;
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi leave network: " + e.getMessage());
            return false;
        }
    }

    /**
     * Lấy thông tin ZeroTier networks
     */
    public static void printNetworkInfo() {
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
                System.out.println("(Không có output hoặc ZeroTier chưa được cài đặt)");
            }

            System.out.println("=======================================\n");

            reader.close();
            process.waitFor();

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy thông tin network: " + e.getMessage());
        }
    }

    /**
     * Kiểm tra IP có phải ZeroTier không
     */
    public static boolean isZeroTierIP(String ip) {
        List<String> ztIPs = getZeroTierIPs();
        return ztIPs.contains(ip);
    }

    /**
     * Lấy status của ZeroTier
     */
    public static String getZeroTierStatus() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                pb = new ProcessBuilder("zerotier-cli.bat", "info");
            } else {
                pb = new ProcessBuilder("zerotier-cli", "info");
            }

            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            StringBuilder status = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                status.append(line).append("\n");
            }

            reader.close();
            process.waitFor();

            return status.toString().trim();

        } catch (Exception e) {
            return "Không thể lấy status: " + e.getMessage();
        }
    }
}