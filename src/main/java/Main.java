import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

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

                    // read and ignore headers
                    String headerLine;
                    String userAgent = "";
                    List<String> headerLines = new ArrayList<>();
                    while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
                      headerLines.add(headerLine);
                      if (headerLine.toLowerCase().startsWith("user-agent:")) {
                        userAgent = headerLine.substring("user-agent:".length()).trim();
                      }  
                    }

                    System.out.println(headerLines);

                    // parse path
                    String[] requestParts = requestLine.split("\\s+");
                    String path = requestParts.length > 1 ? requestParts[1] : "/";
                    String statusLine;
                    String headers = "";
                    String body = "";

                    if (path.equals("/")) {
                        statusLine = "HTTP/1.1 200 OK" + CRLF;
                        headers = CRLF; // empty headers
                    } else if (path.startsWith("/echo/")) {
                        String[] parts = path.split("/");
                        body = parts.length > 2 ? parts[2] : "";
                        statusLine = "HTTP/1.1 200 OK" + CRLF;
                        headers = "Content-Type: text/plain" + CRLF +
                                  "Content-Length: " + body.length() + CRLF + CRLF;
                    } else if (path.startsWith("/user-agent")) {
                        body = userAgent;
                        statusLine = "HTTP/1.1 200 OK" + CRLF;
                        headers = "Content-Type: text/plain" + CRLF +
                                  "Content-Length: " + body.length() + CRLF + CRLF;
                    } else {
                        statusLine = "HTTP/1.1 404 Not Found" + CRLF;
                        headers = CRLF; // empty headers
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
