# Simple HTTP Server

A basic HTTP/1.0 server with HTTP/1.1 support, implementing core functionality as specified.

## Features

- HTTP/1.0 GET request handling
- Multi-threaded request processing
- Basic HTTP/1.1 support (persistent connections)
- Static file serving
- Basic error handling

## Requirements

- JDK 17+
- Maven 3.8+

## Quick Start

1. Clone the repository:

   ```bash
   git clone https://github.com/yourusername/simple-http-server.git
   cd simple-http-server
   ```

2. Build the project:

   ```bash
   mvn clean package
   ```

3. Run the server:

   ```bash
   java -jar target/simple-http-server.jar [port]
   ```
   Default port is 8080 if not specified.

## Usage

The server will start listening for HTTP requests on the specified port. Static files are served from the `public` directory in the project root.

Example requests:
```bash
curl http://localhost:8080/index.html
curl http://localhost:8080/images/photo.jpg
```

## Configuration

Server configuration can be modified in `config.properties`:
- `port`: Server port (default: 8080)
- `webroot`: Static files directory (default: ./public)
- `threads`: Number of worker threads (default: 10)




"# Project-4" 
"# Project-4" 
