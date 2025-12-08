import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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
        InputStream rawIn = clientSocket.getInputStream(); // read one byte at a time
        PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true); // convert text to byte

        // read request line
        String requestLine = readLine(rawIn);
        System.out.println("Received request: " + requestLine);

        if (requestLine == null || requestLine.isEmpty()) {
            clientSocket.close();
            return;
        }

        // read headers
        String headerLine;
        String userAgent = "";
        String acceptEncoding = "";
        int contentLength = 0;
        while ((headerLine = readLine(rawIn)) != null && !headerLine.isEmpty()) {
          if (headerLine.toLowerCase().startsWith("user-agent:")) {
            userAgent = headerLine.substring("user-agent:".length()).trim();
          }  
          
          if (headerLine.toLowerCase().startsWith("content-length:")) {
            contentLength = Integer.parseInt(headerLine.split(":")[1].trim());
          }

          if (headerLine.toLowerCase().startsWith("accept-encoding:")) {
            String[] encodings = headerLine.substring("accept-encoding:".length()).trim().split(",");
            acceptEncoding = Arrays.stream(encodings)
                                   .map(String::trim)
                                   .anyMatch(enc -> enc.equalsIgnoreCase("gzip"))
                                   ? "gzip"
                                   : "identity";
          }
        }

        // parse path
        String[] status = requestLine.split("\\s+");
        String type = status[0], path = status[1];
        boolean valid = (path.equals("/") || 
                         path.startsWith("/echo/") || 
                         path.startsWith("/user-agent") ||
                         path.startsWith("/files/"));
        String OK = "HTTP/1.1 200 OK" + CRLF, NF = "HTTP/1.1 404 Not Found" + CRLF, CR = "HTTP/1.1 201 Created" + CRLF;

        // read request body
        byte[] bodyBytes = rawIn.readNBytes(contentLength);

        // response construction
        String statusLine = valid ? OK : NF;
        String headers = "";
        String body = "";

        // possible headers
        String contentTypeText = "Content-Type: text/plain" + CRLF;
        String contentTypeOctet = "Content-Type: application/octet-stream" + CRLF;
        String contentEncodingText = acceptEncoding.equals("gzip") ? "Content-Encoding: gzip" + CRLF : "";

        if (path.startsWith("/echo/")) {
            String[] parts = path.split("/");
            body = parts.length > 2 ? parts[2] : "";
            headers = contentTypeText + contentEncodingText + "Content-Length: " + body.length() + CRLF.repeat(2);
        } else if (path.startsWith("/user-agent")) {
            body = userAgent;
            headers = contentTypeText + "Content-Length: " + body.length() + CRLF.repeat(2);
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
                headers = contentTypeOctet + "Content-Length: " + body.length() + CRLF.repeat(2);
              }
            } else if (type.equals("POST")) {
              Files.createFile(p);
              String content = new String(bodyBytes, StandardCharsets.UTF_8);
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

    /**
    * Reads a line from the InputStream until CRLF (\r\n).
    * Returns the line **without CRLF**, or null if the stream ends.
    */
    public static String readLine(InputStream in) throws IOException {
      ByteArrayOutputStream buffer = new ByteArrayOutputStream(); // temporarily stores all the bytes read from input stream
      int prev = -1, curr; // track previous and current bytes

      while ((curr = in.read()) != -1) { // read one byte at a time
          if (prev == '\r' && curr == '\n') { // end of a line
              byte[] lineBytes = buffer.toByteArray();
              // return after CRLF is reached
              return new String(lineBytes, 0, lineBytes.length - 1, StandardCharsets.UTF_8); // exclude the last byte (\r)
          }

          // never collects \r or \n
          buffer.write(curr);
          prev = curr;
      }

      // End of stream
      if (buffer.size() == 0) return null;
      return buffer.toString(StandardCharsets.UTF_8);
    }

    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }
}
