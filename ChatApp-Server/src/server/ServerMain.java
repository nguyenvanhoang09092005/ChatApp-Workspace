package server;

import java.util.Scanner;

public class ServerMain {

    private static ChatServer server;
    private static Thread serverThread;

    public static void main(String[] args) {
        printBanner();

        // Create and start server
        server = new ChatServer();
        serverThread = new Thread(() -> server.start());
        serverThread.start();

        // Handle console commands
        handleCommands();
    }

    /**
     * Print server banner
     */
    private static void printBanner() {
        System.out.println("\n");
        System.out.println("  ╔═══════════════════════════════════════════════╗");
        System.out.println("  ║                                               ║");
        System.out.println("  ║         ChatApp Server v1.0.0                 ║");
        System.out.println("  ║         Real-time Chat Application            ║");
        System.out.println("  ║                                               ║");
        System.out.println("  ╚═══════════════════════════════════════════════╝");
        System.out.println("\n");
    }

    /**
     * Handle server console commands
     */
    private static void handleCommands() {
        Scanner scanner = new Scanner(System.in);
        boolean running = true;

        printHelp();

        while (running) {
            System.out.print("\nserver> ");
            String command = scanner.nextLine().trim().toLowerCase();

            switch (command) {
                case "help":
                case "?":
                    printHelp();
                    break;

                case "status":
                    printStatus();
                    break;

                case "clients":
                    printClients();
                    break;

                case "config":
                    printConfig();
                    break;

                case "reload":
                    reloadConfig();
                    break;

                case "stop":
                case "exit":
                case "quit":
                    running = false;
                    stopServer();
                    break;

                case "clear":
                case "cls":
                    clearScreen();
                    break;

                default:
                    if (!command.isEmpty()) {
                        System.out.println("Unknown command: " + command);
                        System.out.println("Type 'help' for available commands");
                    }
            }
        }

        scanner.close();
    }

    /**
     * Print available commands
     */
    private static void printHelp() {
        System.out.println("\n╔════════════════ Server Commands ════════════════╗");
        System.out.println("║  help     - Show this help message              ║");
        System.out.println("║  status   - Show server status                  ║");
        System.out.println("║  clients  - List connected clients              ║");
        System.out.println("║  config   - Show server configuration           ║");
        System.out.println("║  reload   - Reload configuration                ║");
        System.out.println("║  clear    - Clear console                       ║");
        System.out.println("║  stop     - Stop server and exit                ║");
        System.out.println("╚═════════════════════════════════════════════════╝");
    }

    /**
     * Print server status
     */
    private static void printStatus() {
        if (server != null) {
            System.out.println("\n╔════════════════ Server Status ════════════════╗");
            System.out.println("║  Status: " + (server.isRunning() ? "RUNNING" : "STOPPED"));
            System.out.println("║  Connected Clients: " + server.getConnectedClientsCount());
            System.out.println("║  Thread Status: " + (serverThread.isAlive() ? "ALIVE" : "DEAD"));
            System.out.println("╚═══════════════════════════════════════════════╝");
        }
    }

    /**
     * Print connected clients
     */
    private static void printClients() {
        if (server != null) {
            var clients = server.getConnectedClients();
            System.out.println("\n╔═══════════ Connected Clients (" + clients.size() + ") ═══════════╗");

            if (clients.isEmpty()) {
                System.out.println("║  No clients connected                         ║");
            } else {
                int i = 1;
                for (String userId : clients.keySet()) {
                    System.out.printf("║  %d. User ID: %-34s ║\n", i++, userId);
                }
            }

            System.out.println("╚═══════════════════════════════════════════════╝");
        }
    }

    /**
     * Print server configuration
     */
    private static void printConfig() {
        config.ServerConfig.printConfig();
    }

    /**
     * Reload configuration
     */
    private static void reloadConfig() {
        config.ServerConfig.reloadConfigs();
        System.out.println("✓ Configuration reloaded successfully");
    }

    /**
     * Stop server
     */
    private static void stopServer() {
        System.out.println("\n⚠ Shutting down server...");

        if (server != null) {
            server.stop();
        }

        try {
            if (serverThread != null) {
                serverThread.join(5000);
            }
        } catch (InterruptedException e) {
            System.err.println("Error waiting for server thread: " + e.getMessage());
        }

        System.out.println("✓ Server stopped. Goodbye!");
        System.exit(0);
    }

    /**
     * Clear console screen
     */
    private static void clearScreen() {
        try {
            final String os = System.getProperty("os.name");
            if (os.contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // If clear fails, just print newlines
            for (int i = 0; i < 50; i++) {
                System.out.println();
            }
        }
    }
}