import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {
  private static int PORT = 4221;

  public static void main(String[] args) {
    clearScreen();

    try {
      System.out.println("Server listening on port " + PORT);

      // create a new server, bind to port and start listening for connections
      ServerSocket serverSocket = new ServerSocket(PORT);
    
      // if the program restarts quickly, allow it to bind to the same port again
      serverSocket.setReuseAddress(true);
      
      // pause the program until a client connects
      Socket clientSocket = serverSocket.accept(); 
      System.out.println("Accepted new connection");

      // read from client (convert bytes -> characters)
      InputStream clientInputBytes = clientSocket.getInputStream(); // get input as raw byte stream from client
      InputStreamReader reader = new InputStreamReader(clientInputBytes); // turn raw bytes into characters (i.e. 71 = 'G' in ascii)
      BufferedReader in = new BufferedReader(reader); // allows to read lines

      // write to client (convert characters -> bytes)
      OutputStream clientOutputBytes = clientSocket.getOutputStream(); // how program sends data to client
      PrintWriter out = new PrintWriter(clientOutputBytes, true); // true to auto flush (receive immediately after print called)

      String requestMessage, responseMessage;

      // read from socket
      requestMessage = in.readLine();
      System.out.println("The received message from the client: " + requestMessage);

      // parse path
      String path = requestMessage.split("\\s+")[1];
      String RESPONSE = path.equals("/") ? "200 OK" : "404 Not Found"; 

      // writing to socket
      responseMessage = "HTTP/1.1 " + RESPONSE + "\r\n\r\n";
      out.println(responseMessage);
      System.out.println("Message sent to the client: " + responseMessage);

      clientSocket.close();

    } catch (IOException e) {
      System.out.println("IOException: " + e.getMessage());
    }
  }

  public static void clearScreen() {
    System.out.print("\033[H\033[2J");
    System.out.flush(); 
  }
}
