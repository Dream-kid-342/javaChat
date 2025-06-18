package javaChat;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.nio.charset.StandardCharsets;

// Java HTTP Chat Server
// Serves a web-based chat UI, handles messaging, user tracking, and supports voice input.
// Start the server, open the provided link in a browser, and chat in real time.

public class ChatHttpServer {
    private static final List<String> messages = new CopyOnWriteArrayList<>();
    private static final Set<String> activeUsers = ConcurrentHashMap.newKeySet();

    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Run server on (1) localhost or (2) network? [1/2]: ");
        String choice = scanner.nextLine().trim();
        InetSocketAddress address;
        String link;
        if ("2".equals(choice)) {
            System.out.print("Enter IP address to bind (e.g., 192.168.1.100): ");
            String ip = scanner.nextLine().trim();
            System.out.print("Enter port number (e.g., 8081): ");
            int port = Integer.parseInt(scanner.nextLine().trim());
            address = new InetSocketAddress(ip, port);
            link = "http://" + ip + ":" + port + "/";
        } else {
            address = new InetSocketAddress("localhost", 8081);
            link = "http://localhost:8081/";
        }
        HttpServer server = HttpServer.create(address, 0);
        server.createContext("/", new PageHandler());
        server.createContext("/send", new SendHandler());
        server.createContext("/messages", new MessagesHandler());
        server.createContext("/users", new UsersHandler());
        server.setExecutor(Executors.newCachedThreadPool());
        System.out.println("Chat server started at " + link);
        server.start();
    }

    static class PageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String html = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="UTF-8">
                <title>Java HTTP Chat</title>
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <link href="https://fonts.googleapis.com/css?family=Roboto:400,500&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --main-bg: #111b21;
                        --main-fg: #ece5dd;
                        --container-bg: #222e35;
                        --header-bg: #075e54;
                        --mine-bg: #25d366;
                        --mine-fg: #111b21;
                        --theirs-bg: #2a3942;
                        --theirs-fg: #ece5dd;
                        --sidebar-bg: #202c33;
                        --sidebar-title: #25d366;
                        --send-bg: #25d366;
                        --send-fg: #111b21;
                        --voice-bg: #075e54;
                    }
                    body.theme-dark {
                        --main-bg: #111b21;
                        --main-fg: #ece5dd;
                        --container-bg: #222e35;
                        --header-bg: #075e54;
                        --mine-bg: #25d366;
                        --mine-fg: #111b21;
                        --theirs-bg: #2a3942;
                        --theirs-fg: #ece5dd;
                        --sidebar-bg: #202c33;
                        --sidebar-title: #25d366;
                        --send-bg: #25d366;
                        --send-fg: #111b21;
                        --voice-bg: #075e54;
                    }
                    body.theme-light {
                        --main-bg: #f7f7f7;
                        --main-fg: #222e35;
                        --container-bg: #fff;
                        --header-bg: #e0e0e0;
                        --mine-bg: #d2f8d2;
                        --mine-fg: #222e35;
                        --theirs-bg: #e9e9eb;
                        --theirs-fg: #222e35;
                        --sidebar-bg: #f3f3f3;
                        --sidebar-title: #43b581;
                        --send-bg: #43b581;
                        --send-fg: #fff;
                        --voice-bg: #4f8cff;
                    }
                    html, body {
                        height: 100%%;
                        margin: 0;
                        padding: 0;
                        box-sizing: border-box;
                    }
                    body {
                        font-family: 'Roboto', 'Segoe UI', Arial, sans-serif;
                        background: var(--main-bg);
                        color: var(--main-fg);
                        min-height: 100vh;
                        margin: 0;
                        transition: background 0.3s, color 0.3s;
                    }
                    #container {
                        display: flex;
                        flex-direction: row;
                        max-width: 950px;
                        margin: 0 auto;
                        background: var(--container-bg);
                        border-radius: 12px;
                        box-shadow: 0 4px 32px #000a;
                        min-height: 90vh;
                        height: 90vh;
                        margin-top: 2vh;
                        margin-bottom: 2vh;
                        overflow: hidden;
                    }
                    #main {
                        flex: 3;
                        display: flex;
                        flex-direction: column;
                        min-width: 0;
                        background: var(--container-bg);
                    }
                    #header {
                        background: var(--header-bg);
                        padding: 18px;
                        border-radius: 12px 0 0 0;
                        font-size: 1.5em;
                        text-align: left;
                        letter-spacing: 2px;
                        color: #fff;
                        font-weight: 500;
                        box-shadow: 0 2px 8px #0004;
                        display: flex;
                        align-items: center;
                        justify-content: space-between;
                    }
                    #settings-btn {
                        background: none;
                        border: none;
                        color: #fff;
                        font-size: 1.3em;
                        cursor: pointer;
                        margin-left: 8px;
                        padding: 4px 8px;
                        border-radius: 6px;
                        transition: background 0.2s;
                    }
                    #settings-btn:hover {
                        background: rgba(255,255,255,0.08);
                    }
                    #settings-modal {
                        display: none;
                        position: fixed;
                        top: 0; left: 0; right: 0; bottom: 0;
                        background: rgba(0,0,0,0.35);
                        z-index: 1000;
                        align-items: center;
                        justify-content: center;
                    }
                    #settings-modal.active {
                        display: flex;
                    }
                    #settings-content {
                        background: var(--container-bg);
                        color: var(--main-fg);
                        padding: 28px 32px 24px 32px;
                        border-radius: 14px;
                        box-shadow: 0 4px 32px #000a;
                        min-width: 240px;
                        min-height: 120px;
                        display: flex;
                        flex-direction: column;
                        align-items: flex-start;
                    }
                    #settings-content label {
                        font-size: 1.1em;
                        margin-bottom: 10px;
                    }
                    #theme-select {
                        font-size: 1em;
                        padding: 6px 12px;
                        border-radius: 6px;
                        border: 1px solid #ccc;
                        margin-bottom: 18px;
                    }
                    #close-settings {
                        align-self: flex-end;
                        background: none;
                        border: none;
                        color: var(--main-fg);
                        font-size: 1.2em;
                        cursor: pointer;
                        margin-top: -10px;
                        margin-right: -10px;
                    }
                    #chat {
                        width: 100%%;
                        flex: 1 1 auto;
                        border: none;
                        overflow-y: auto;
                        background: var(--main-bg);
                        padding: 24px 16px 8px 16px;
                        box-sizing: border-box;
                        display: flex;
                        flex-direction: column;
                        gap: 10px;
                        scrollbar-width: thin;
                        scrollbar-color: var(--mine-bg) var(--main-bg);
                    }
                    #chat::-webkit-scrollbar {
                        width: 8px;
                    }
                    #chat::-webkit-scrollbar-thumb {
                        background: var(--mine-bg);
                        border-radius: 4px;
                    }
                    .bubble-row {
                        display: flex;
                        width: 100%%;
                    }
                    .bubble {
                        display: inline-block;
                        padding: 12px 18px;
                        border-radius: 18px;
                        margin: 2px 0;
                        max-width: 75%%;
                        font-size: 1.08em;
                        word-break: break-word;
                        box-shadow: 0 2px 8px #0002;
                        transition: background 0.2s;
                        position: relative;
                    }
                    .mine {
                        background: var(--mine-bg);
                        color: var(--mine-fg);
                        margin-left: auto;
                        text-align: right;
                        border-bottom-right-radius: 4px;
                    }
                    .mine:after {
                        content: "";
                        position: absolute;
                        right: -8px;
                        bottom: 0;
                        width: 0;
                        height: 0;
                        border-top: 12px solid var(--mine-bg);
                        border-left: 12px solid transparent;
                    }
                    .theirs {
                        background: var(--theirs-bg);
                        color: var(--theirs-fg);
                        margin-right: auto;
                        text-align: left;
                        border-bottom-left-radius: 4px;
                    }
                    .theirs:after {
                        content: "";
                        position: absolute;
                        left: -8px;
                        bottom: 0;
                        width: 0;
                        height: 0;
                        border-top: 12px solid var(--theirs-bg);
                        border-right: 12px solid transparent;
                    }
                    .username {
                        font-size: 0.85em;
                        opacity: 0.7;
                        margin-bottom: 2px;
                        font-weight: 500;
                    }
                    #inputbar {
                        display: flex;
                        gap: 10px;
                        padding: 14px 20px 0 20px;
                        background: var(--container-bg);
                        border-bottom-left-radius: 12px;
                    }
                    #user {
                        flex: 1;
                        max-width: 120px;
                        border-radius: 8px;
                        border: none;
                        padding: 10px;
                        background: var(--theirs-bg);
                        color: var(--theirs-fg);
                        font-size: 1em;
                        font-family: inherit;
                    }
                    #msg {
                        flex: 4;
                        border-radius: 8px;
                        border: none;
                        padding: 10px;
                        background: var(--theirs-bg);
                        color: var(--theirs-fg);
                        font-size: 1em;
                        font-family: inherit;
                    }
                    #send {
                        flex: 0;
                        border-radius: 8px;
                        border: none;
                        padding: 10px 22px;
                        background: var(--send-bg);
                        color: var(--send-fg);
                        font-weight: bold;
                        font-size: 1em;
                        cursor: pointer;
                        transition: background 0.2s;
                    }
                    #send:hover {
                        background: #20ba5a;
                    }
                    #voice {
                        flex: 0;
                        border-radius: 8px;
                        border: none;
                        padding: 10px 14px;
                        background: var(--voice-bg);
                        color: #fff;
                        font-weight: bold;
                        cursor: pointer;
                        margin-left: 2px;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        transition: background 0.2s;
                    }
                    #voice.listening {
                        background: #faa61a;
                        color: #23272a;
                    }
                    #sidebar {
                        flex: 1;
                        background: var(--sidebar-bg);
                        border-radius: 0 12px 12px 0;
                        padding: 24px 18px;
                        min-width: 140px;
                        max-width: 220px;
                        display: flex;
                        flex-direction: column;
                        align-items: stretch;
                        box-shadow: -2px 0 8px #0002;
                    }
                    #sidebar h3 {
                        margin-top: 0;
                        font-size: 1.15em;
                        color: var(--sidebar-title);
                        font-weight: 500;
                        letter-spacing: 1px;
                        margin-bottom: 12px;
                    }
                    #userlist {
                        list-style: none;
                        padding: 0;
                        margin: 0;
                        flex: 1 1 auto;
                        overflow-y: auto;
                    }
                    #userlist li {
                        padding: 8px 0;
                        border-bottom: 1px solid var(--theirs-bg);
                        font-size: 1.05em;
                        word-break: break-all;
                        color: #b9bbbe;
                    }
                    @media (max-width: 700px) {
                        #container {
                            flex-direction: column;
                            min-height: 98vh;
                            height: 98vh;
                            max-width: 98vw;
                            margin: 1vw;
                            border-radius: 8px;
                        }
                        #sidebar {
                            border-radius: 0 0 8px 8px;
                            min-width: 0;
                            max-width: 100%%;
                            margin-top: 8px;
                            padding: 14px;
                            box-shadow: none;
                        }
                        #main {
                            border-radius: 8px 8px 0 0;
                        }
                    }
                    @media (max-width: 500px) {
                        #container {
                            margin: 0;
                            min-height: 100vh;
                            height: 100vh;
                            max-width: 100vw;
                            border-radius: 0;
                        }
                        #header {
                            font-size: 1em;
                            padding: 10px;
                            border-radius: 0;
                        }
                        #inputbar {
                            flex-direction: column;
                            gap: 8px;
                            padding: 10px 6px 0 6px;
                            border-radius: 0 0 8px 8px;
                        }
                        #user, #msg, #send, #voice {
                            width: 100%%;
                            max-width: 100%%;
                            min-width: 0;
                            margin: 0;
                            font-size: 1em;
                        }
                        #chat {
                            padding: 8px 2px 2px 2px;
                        }
                    }
                </style>
            </head>
            <body class="theme-dark">
                <div id="container">
                    <div id="main">
                        <div id="header">
                            Java HTTP Chat
                            <button id="settings-btn" title="Settings">
                                <svg xmlns="http://www.w3.org/2000/svg" width="22" height="22" fill="currentColor" viewBox="0 0 24 24">
                                    <path d="M19.14,12.94a7.07,7.07,0,0,0,.05-1,7.07,7.07,0,0,0-.05-1l2.11-1.65a.5.5,0,0,0,.12-.64l-2-3.46a.5.5,0,0,0-.61-.22l-2.49,1a7,7,0,0,0-1.73-1l-.38-2.65A.5.5,0,0,0,13,2H11a.5.5,0,0,0-.5.42l-.38,2.65a7,7,0,0,0-1.73,1l-2.49-1a.5.5,0,0,0-.61.22l-2,3.46a.5.5,0,0,0,.12.64L4.86,10a7.07,7.07,0,0,0-.05,1,7.07,7.07,0,0,0,.05,1l-2.11,1.65a.5.5,0,0,0-.12.64l2,3.46a.5.5,0,0,0,.61.22l2.49-1a7,7,0,0,0,1.73,1l.38,2.65A.5.5,0,0,0,11,22h2a.5.5,0,0,0,.5-.42l.38-2.65a7,7,0,0,0,1.73-1l2.49,1a.5.5,0,0,0,.61-.22l2-3.46a.5.5,0,0,0-.12-.64ZM12,15.5A3.5,3.5,0,1,1,15.5,12,3.5,3.5,0,0,1,12,15.5Z"/>
                                </svg>
                            </button>
                        </div>
                        <div id="chat"></div>
                        <div id="inputbar">
                            <input id="user" placeholder="Username" autocomplete="off" />
                            <input id="msg" placeholder="Type a message..." autocomplete="off" />
                            <button id="voice" title="Voice input (speech to text)">
                                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" style="vertical-align:middle;" viewBox="0 0 24 24" fill="currentColor">
                                    <path d="M12 15c1.654 0 3-1.346 3-3V6c0-1.654-1.346-3-3-3s-3 1.346-3 3v6c0 1.654 1.346 3 3 3zm5-3c0 2.757-2.243 5-5 5s-5-2.243-5-5H5c0 3.519 2.613 6.432 6 6.92V22h2v-2.08c3.387-.488 6-3.401 6-6.92h-2z"/>
                                </svg>
                            </button>
                            <button id="send">Send</button>
                        </div>
                    </div>
                    <div id="sidebar">
                        <h3>Active Users</h3>
                        <ul id="userlist"></ul>
                    </div>
                </div>
                <div id="settings-modal">
                    <div id="settings-content">
                        <button id="close-settings" title="Close">&times;</button>
                        <label for="theme-select">Theme:</label>
                        <select id="theme-select">
                            <option value="theme-dark">Dark (WhatsApp)</option>
                            <option value="theme-light">Light</option>
                        </select>
                    </div>
                </div>
                <script>
                    let since = 0;
                    let userInput = document.getElementById('user');
                    let chatDiv = document.getElementById('chat');
                    let msgInput = document.getElementById('msg');
                    let sendBtn = document.getElementById('send');
                    let voiceBtn = document.getElementById('voice');
                    let userList = document.getElementById('userlist');
                    let myUser = localStorage.getItem('chat_user') || "";
                    if (myUser) userInput.value = myUser;
                    userInput.addEventListener('change', function() {
                        localStorage.setItem('chat_user', userInput.value.trim());
                        updateUser();
                    });
                    function escapeHtml(text) {
                        return text.replace(/[&<>"']/g, function(m) {
                            return {'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[m];
                        });
                    }
                    function addMessageBubble(msg) {
                        let myName = userInput.value.trim() || "anon";
                        let isMine = msg.startsWith(myName + ": ");
                        let row = document.createElement('div');
                        row.className = 'bubble-row';
                        let bubble = document.createElement('div');
                        bubble.className = 'bubble ' + (isMine ? 'mine' : 'theirs');
                        let content = msg;
                        if (isMine) {
                            content = msg.substring((myName + ": ").length);
                        } else {
                            let idx = msg.indexOf(": ");
                            if (idx > 0) {
                                let uname = msg.substring(0, idx);
                                let text = msg.substring(idx + 2);
                                let unameDiv = document.createElement('div');
                                unameDiv.className = 'username';
                                unameDiv.textContent = uname;
                                bubble.appendChild(unameDiv);
                                content = text;
                            }
                        }
                        let textDiv = document.createElement('div');
                        textDiv.textContent = content;
                        bubble.appendChild(textDiv);
                        row.appendChild(bubble);
                        chatDiv.appendChild(row);
                        chatDiv.scrollTop = chatDiv.scrollHeight;
                    }
                    function poll() {
                        fetch('/messages?since=' + since)
                            .then(r => r.json())
                            .then(data => {
                                data.messages.forEach(m => addMessageBubble(m));
                                since = data.next;
                            })
                            .catch(()=>{});
                    }
                    setInterval(poll, 1000);
                    sendBtn.onclick = function() {
                        let user = userInput.value.trim() || "anon";
                        let msg = msgInput.value.trim();
                        if (!msg) return;
                        fetch('/send?user=' + encodeURIComponent(user) + '&msg=' + encodeURIComponent(msg), {method:'POST'})
                            .then(()=>{ msgInput.value=''; updateUser(); });
                    };
                    msgInput.addEventListener('keydown', function(e) {
                        if (e.key === 'Enter') sendBtn.onclick();
                    });
                    // Voice input (Web Speech API)
                    if ('webkitSpeechRecognition' in window || 'SpeechRecognition' in window) {
                        let SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
                        let recognition = new SpeechRecognition();
                        recognition.continuous = false;
                        recognition.interimResults = false;
                        recognition.lang = 'en-US';
                        recognition.onresult = function(event) {
                            let transcript = event.results[0][0].transcript;
                            msgInput.value += transcript;
                        };
                        recognition.onend = function() {
                            voiceBtn.classList.remove('listening');
                        };
                        voiceBtn.onclick = function() {
                            recognition.start();
                            voiceBtn.classList.add('listening');
                        };
                    } else {
                        voiceBtn.disabled = true;
                        voiceBtn.title = "Voice input not supported in this browser";
                    }
                    // Active users
                    function updateUser() {
                        let user = userInput.value.trim() || "anon";
                        fetch('/users?user=' + encodeURIComponent(user), {method:'POST'});
                    }
                    function pollUsers() {
                        fetch('/users')
                            .then(r => r.json())
                            .then(data => {
                                userList.innerHTML = "";
                                data.users.forEach(u => {
                                    let li = document.createElement('li');
                                    li.textContent = u;
                                    userList.appendChild(li);
                                });
                            });
                    }
                    setInterval(pollUsers, 2000);
                    window.onload = function() {
                        updateUser();
                        pollUsers();
                    };
                    // Settings modal logic
                    const settingsBtn = document.getElementById('settings-btn');
                    const settingsModal = document.getElementById('settings-modal');
                    const closeSettings = document.getElementById('close-settings');
                    const themeSelect = document.getElementById('theme-select');
                    function setTheme(theme) {
                        document.body.classList.remove('theme-dark', 'theme-light');
                        document.body.classList.add(theme);
                        localStorage.setItem('chat_theme', theme);
                    }
                    settingsBtn.onclick = () => { settingsModal.classList.add('active'); };
                    closeSettings.onclick = () => { settingsModal.classList.remove('active'); };
                    themeSelect.onchange = () => setTheme(themeSelect.value);
                    // Load theme from localStorage
                    const savedTheme = localStorage.getItem('chat_theme') || 'theme-dark';
                    setTheme(savedTheme);
                    themeSelect.value = savedTheme;
                    // Close modal on outside click
                    settingsModal.onclick = (e) => { if (e.target === settingsModal) settingsModal.classList.remove('active'); };
                </script>
            </body>
            </html>
            """;
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=UTF-8");
            exchange.sendResponseHeaders(200, html.getBytes().length);
            OutputStream os = exchange.getResponseBody();
            os.write(html.getBytes());
            os.close();
        }
    }

    static class SendHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String user = params.getOrDefault("user", "anon");
            String msg = params.getOrDefault("msg", "");
            if (!msg.isEmpty()) {
                messages.add(user + ": " + msg);
                activeUsers.add(user);
            }
            String response = "OK";
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class MessagesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            int since = 0;
            try { since = Integer.parseInt(params.getOrDefault("since", "0")); } catch (Exception ignored) {}
            List<String> newMessages = messages.subList(Math.min(since, messages.size()), messages.size());
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < newMessages.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(newMessages.get(i).replace("\"", "\\\"")).append("\"");
            }
            json.append("]");
            String response = "{\"messages\":" + json + ",\"next\":" + messages.size() + "}";
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length());
            OutputStream os = exchange.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    static class UsersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (exchange.getRequestMethod().equalsIgnoreCase("POST")) {
                Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
                String user = params.getOrDefault("user", "anon");
                if (!user.isEmpty()) {
                    activeUsers.add(user);
                }
                exchange.sendResponseHeaders(200, 2);
                OutputStream os = exchange.getResponseBody();
                os.write("OK".getBytes());
                os.close();
            } else {
                // GET: return list of users as JSON
                StringBuilder json = new StringBuilder("[");
                boolean first = true;
                for (String user : activeUsers) {
                    if (!first) json.append(",");
                    json.append("\"").append(user.replace("\"", "\\\"")).append("\"");
                    first = false;
                }
                json.append("]");
                String response = "{\"users\":" + json + "}";
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }
        }
    }

    // Utility method to convert query string to map
    private static Map<String, String> queryToMap(String query) {
        Map<String, String> map = new HashMap<>();
        if (query != null) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                if (idx > 0) {
                    String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
                    String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
                    map.put(key, value);
                }
            }
        }
        return map;
    }
}
