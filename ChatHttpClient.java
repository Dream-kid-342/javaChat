package javaChat;
import java.io.*;
import java.net.*;
import java.util.*;

public class ChatHttpClient {
    public static void main(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Enter your username: ");
        String user = scanner.nextLine().trim();
        int lastMsg = 0;

        // Start a thread to poll for new messages
        Thread poller = new Thread(() -> {
            int since = 0;
            while (true) {
                try {
                    URL url = new URL("http://localhost:8081/messages?since=" + since);
                    HttpURLConnection con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                    StringBuilder resp = new StringBuilder();
                    String line;
                    while ((line = in.readLine()) != null) resp.append(line);
                    in.close();
                    String json = resp.toString();
                    // Parse messages and next index
                    int idx1 = json.indexOf("[");
                    int idx2 = json.indexOf("]");
                    if (idx1 != -1 && idx2 != -1 && idx2 > idx1) {
                        String arr = json.substring(idx1 + 1, idx2);
                        if (!arr.trim().isEmpty()) {
                            for (String msg : arr.split("\",\"")) {
                                msg = msg.replaceAll("^\"|\"$", "").replace("\\\"", "\"");
                                System.out.println(msg);
                            }
                        }
                    }
                    int nextIdx = json.lastIndexOf("\"next\":");
                    if (nextIdx != -1) {
                        String nextVal = json.substring(nextIdx + 7).replaceAll("[^0-9]", "");
                        if (!nextVal.isEmpty()) since = Integer.parseInt(nextVal);
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    // Ignore errors, retry
                    try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                }
            }
        });
        poller.setDaemon(true);
        poller.start();

        // Main loop: send messages
        while (true) {
            String msg = scanner.nextLine();
            if (msg.trim().isEmpty()) continue;
            try {
                String urlStr = "http://localhost:8081/send?user=" + URLEncoder.encode(user, "UTF-8")
                        + "&msg=" + URLEncoder.encode(msg, "UTF-8");
                URL url = new URL(urlStr);
                HttpURLConnection con = (HttpURLConnection) url.openConnection();
                con.setRequestMethod("POST");
                con.setDoOutput(true);
                con.getOutputStream().write(0); // Send empty body
                con.getInputStream().close();
            } catch (Exception e) {
                System.out.println("Failed to send message: " + e.getMessage());
            }
        }
    }
}
