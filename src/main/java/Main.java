import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
    private static final int PORT = 4221;
    private static final String CRLF = "\r\n";

    public static void main(String[] args) {
        clearScreen();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server listening on port " + PORT);

            while (true) {
                try (Socket clientSocket = serverSocket.accept()) {
                    System.out.println("Accepted new connection");

                    // input and output
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(clientSocket.getInputStream()) // convert byte to text
                    );
                    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); // convert text to byte

                    // read request line
                    String requestLine = in.readLine();
                    System.out.println("Received request: " + requestLine);

                    if (requestLine == null || requestLine.isEmpty()) {
                        clientSocket.close();
                        continue;
                    }

                    // read headers
                    String headerLine;
                    String userAgent = "";
                    while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                      if (headerLine.toLowerCase().startsWith("user-agent:")) {
                        userAgent = headerLine.substring("user-agent:".length()).trim();
                      }  
                    }

                    // parse path
                    String path = requestLine.split("\\s+")[1];
                    boolean valid = path.equals("/") || path.startsWith("/echo/") || path.startsWith("/user-agent");

                    // response construction
                    String statusLine = (valid ? "HTTP/1.1 200 OK" : "HTTP/1.1 404 Not Found") + CRLF;
                    String headers = "";
                    String body = "";

                    if (path.startsWith("/echo/")) {
                        String[] parts = path.split("/");
                        body = parts.length > 2 ? parts[2] : "";
                        headers = "Content-Type: text/plain" + CRLF +
                                  "Content-Length: " + body.length() + CRLF + CRLF;
                    } else if (path.startsWith("/user-agent")) {
                        body = userAgent;
                        headers = "Content-Type: text/plain" + CRLF +
                                  "Content-Length: " + body.length() + CRLF + CRLF;
                    } else {
                        headers = CRLF; 
                    }

                    // build response
                    String response = statusLine + headers + body;

                    // send response
                    out.println(response);
                    System.out.println("Sent response:\n" + response);

                } catch (IOException e) {
                    System.out.println("Client connection error: " + e.getMessage());
                }
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
