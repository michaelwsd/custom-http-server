import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class Main {
    private static final int PORT = 4221;
    private static final String CRLF = "\r\n";

    public static void main(String[] args) {
        clearScreen();

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            System.out.println("Server listening on port " + PORT);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Accepted new connection");
                new Thread(() -> handleClient(clientSocket, args)).start(); // each client is ran in a new thread
            }

        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public static void handleClient(Socket clientSocket, String[] args) {
      try (clientSocket) { // automatically closes socket
        // input and output
        InputStream rawIn = clientSocket.getInputStream();
        BufferedReader in = new BufferedReader(new InputStreamReader(rawIn)); // convert byte to text
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); // convert text to byte

        // read request line
        String requestLine = in.readLine();
        System.out.println("Received request: " + requestLine);

        if (requestLine == null || requestLine.isEmpty()) {
            clientSocket.close();
            return;
        }

        // read headers
        String headerLine;
        String userAgent = "";
        int contentLength = 0;
        while ((headerLine = in.readLine()) != null && !headerLine.isEmpty()) {
          if (headerLine.toLowerCase().startsWith("user-agent:")) {
            userAgent = headerLine.substring("user-agent:".length()).trim();
          }  
          if (headerLine.toLowerCase().startsWith("content-length:")) {
            contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
          }
        }

        System.out.println(contentLength);

        // parse path
        String[] status = requestLine.split("\\s+");
        String type = status[0], path = status[1];
        boolean valid = (path.equals("/") || 
                         path.startsWith("/echo/") || 
                         path.startsWith("/user-agent") ||
                         path.startsWith("/files/"));
        String OK = "HTTP/1.1 200 OK" + CRLF, NF = "HTTP/1.1 404 Not Found" + CRLF, CR = "HTTP/1.1 201 Created" + CRLF;

        System.out.println("ran 1");
        byte[] bodyBytes = rawIn.readNBytes(contentLength);

        System.out.println("ran 2");

        // response construction
        String statusLine = valid ? OK : NF;
        String headers = "Content-Type: text/plain" + CRLF;
        String body = "";

        if (path.startsWith("/echo/")) {
            String[] parts = path.split("/");
            body = parts.length > 2 ? parts[2] : "";
            headers = "Content-Type: text/plain" + CRLF + "Content-Length: " + body.length() + CRLF.repeat(2);
        } else if (path.startsWith("/user-agent")) {
            body = userAgent;
            headers = "Content-Type: text/plain" + CRLF + "Content-Length: " + body.length() + CRLF.repeat(2);
        } else if (path.startsWith("/files/")) {
            String dir = "";

            for (int i = 0; i < args.length; i++) {
                if (args[i].equals("--directory") && i + 1 < args.length) {
                    dir = args[i + 1];
                }
            }

            String fileName = path.substring("/files/".length());

            Path p = Path.of(dir, fileName);

            if (type.equals("GET")) {
              if (!Files.exists(p) || fileName.isEmpty()) {
                headers = CRLF;
                statusLine = NF;
              } else {
                body = Files.readString(p);
                headers = "Content-Type: application/octet-stream" + CRLF + "Content-Length: " + body.length() + CRLF.repeat(2);
              }
            } else if (type.equals("POST")) {
              Files.createFile(p);
              String content = new String(bodyBytes, StandardCharsets.UTF_8);
              System.out.println(content);
              Files.writeString(p, content);

              // send status 
              headers = CRLF;
              statusLine = CR;
            }

        } else {
            headers = CRLF; 
        }

        // build response
        String response = statusLine + headers + body;

        // send response
        out.write(response);
        out.flush();
        System.out.println("Sent response:\n" + response);

      } catch (IOException e) {
          System.out.println("Client connection error: " + e.getMessage());
      }
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
