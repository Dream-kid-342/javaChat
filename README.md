# Java HTTP Chat Project

## Context

This project demonstrates a WhatsApp-style chat application using Java's built-in HTTP server.

It consists of:

- **ChatHttpServer.java**  
  A web-based chat server. It serves an HTML/JavaScript chat UI, manages messages and active users, supports voice input (speech-to-text), user authentication, profile pictures, and theme switching (dark/light).  
  Start the server, open the provided link in your browser, and chat in real time with others.

- **ChatHttpClient.java**  
  (Optional) A terminal-based Java client for the chat server. It connects to the server, sends messages, and polls for new messages in real time.  
  Useful for testing or for environments without a browser.

## Features

- **Web-based chat UI:** Real-time chat from any modern browser.
- **WhatsApp-like design:** Responsive, professional UI with chat bubbles and sidebar.
- **Active users list:** See who is currently active in the chat, with profile pictures.
- **User authentication:** Users must sign up and log in before chatting.
- **Profile management:** Users set a display name and upload a profile picture during signup.
- **Profile pictures:** Shown in the user list and next to each message.
- **Voice input:** Use your voice to compose messages (if your browser supports the Web Speech API).
- **Theme switching:** Choose between dark (WhatsApp style) and improved light theme.
- **Customizable server binding:** Choose to run the server on localhost or any network IP and port.
- **Multiple clients:** Supports multiple users and devices simultaneously.
- **Terminal client (optional):** Send and receive messages from the command line.
- **No external dependencies:** Uses only Java standard library and built-in HTTP server.

## How to Use

1. **Compile the server:**
   ```
   javac ChatHttpServer.java
   ```

2. **Start the server:**
   ```
   java ChatHttpServer
   ```
   - When you start the server, you will be prompted:
     - `Run server on (1) localhost or (2) network? [1/2]:`
     - If you choose `1`, the server will only be accessible from your own computer at `http://localhost:8081/`.
     - If you choose `2`, you will be asked to enter an IP address and port.  
       - Enter your local network IP (e.g., `192.168.1.100`) and a port (e.g., `8081`).  
       - The server will then be accessible from other devices on your network using the link shown (e.g., `http://192.168.1.100:8081/`).
       - To allow access from the internet, you must configure port forwarding on your router and ensure your firewall allows incoming connections.

3. **Open the chat UI:**
   - Visit the link shown in the terminal (e.g., `http://localhost:8081/` or `http://<your-ip>:<port>/`).
   - Sign up for a new account, upload a profile picture, and log in to start chatting.

4. **(Optional) Use the Java client:**
   ```
   javac ChatHttpClient.java
   java ChatHttpClient
   ```

## How It Works

- The server uses Java's built-in HTTP server to serve a chat web page and handle chat/message/user/auth/profile requests.
- When started, the server asks if you want to run on localhost (only your computer) or on a network IP (accessible to others).
- All chat messages and user activity are managed in memory on the server.
- User accounts and profile pictures are stored in a `users.txt` file.
- The web UI polls the server for new messages and active users, and sends messages via HTTP requests.
- Voice input is supported in browsers that implement the Web Speech API.
- Users can switch between dark and light themes using the settings button.

## How to Find Your IP Address

You can find your local IP address using the command line on any operating system:

### Windows

Open Command Prompt and run:
```
ipconfig
```
Look for the `IPv4 Address` under your active network adapter.

### Linux

Open a terminal and run:
```
hostname -I
```
or
```
ip addr show
```
Look for the IP address under your network interface (often `eth0` or `wlan0`).

### macOS

Open Terminal and run:
```
ifconfig
```
Look for the `inet` value under your active interface (usually `en0` for Wi-Fi).

## Notes

- For network access, ensure your firewall and router allow incoming connections on the chosen port.
- This project is for demonstration/learning purposes and is not secure for public deployment.
- Passwords are stored in plain text for demonstration only. Do not use real credentials.
