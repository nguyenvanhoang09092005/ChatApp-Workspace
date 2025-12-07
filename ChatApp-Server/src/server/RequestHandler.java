package server;

public interface RequestHandler {
    String handleRequest(String request, ClientHandler client);
}