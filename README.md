# Custom HTTP Server

This project is a simple HTTP server implemented in Java. It is designed to handle basic HTTP requests and responses, supporting features such as gzip compression, file handling, and multi-threaded client connections.

## Features

- **Multi-threaded Server**: Each client connection is handled in a separate thread.
- **HTTP Methods**: Supports `GET` and `POST` requests.
- **GZIP Compression**: Compresses responses using GZIP if the client supports it.
- **File Handling**: Allows reading and writing files in a specified directory.
- **Custom Endpoints**:
  - `/echo/<message>`: Echoes the message back to the client.
  - `/user-agent`: Returns the `User-Agent` header sent by the client.
  - `/files/<filename>`: Handles file operations (read/write).

## Requirements

- Java 8 or higher
- Maven (for building the project)

## Getting Started

### Build the Project

1. Clone the repository:
   ```bash
   git clone <repository-url>
   ```
2. Navigate to the project directory:
   ```bash
   cd custom-http-server
   ```
3. Run the server (listening on port 4221):
   ```bash
   ./your_program.sh
   ```

### Test the Server

You can test the server using tools like `curl` or a web browser. Here are some examples:

- **Echo Endpoint**:
  ```bash
  curl http://localhost:4221/echo/hello
  ```
- **User-Agent Endpoint**:
  ```bash
  curl -H "User-Agent: MyBrowser" http://localhost:4221/user-agent
  ```
- **File Operations**:
  - Read a file:
    ```bash
    curl http://localhost:4221/files/example.txt
    ```
  - Write to a file:
    ```bash
    curl -X POST -d "File content" http://localhost:4221/files/example.txt
    ```

## Project Structure

- `src/main/java/Main.java`: The main server implementation.
- `pom.xml`: Maven configuration file.
- `target/`: Contains the compiled JAR file and other build artifacts.