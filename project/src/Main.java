import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

    public static void main(String[] args) throws IOException {
        int port;
        String root = "www";
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8888;
        }

        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("url : http://localhost:" + port + "/");
        String line;

        while (true) {
            Socket clientSocket = serverSocket.accept();
            BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String firstLine = reader.readLine();
            String[] lString = firstLine.split(" ");

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
            }
            DataOutputStream writer = new DataOutputStream(clientSocket.getOutputStream());

            File file = new File(root + lString[1]);

            if (file.exists()) {
                if (file.isDirectory()) {
                    File[] files = file.listFiles();
                    String html = "<html><body>";
                    for (File f : files) {
                        html += "<a href='" + f.getName() + "'>" + f.getName() + "</a><br>";
                    }
                    html += "</body></html>";
                    writer.writeBytes("HTTP/1.1 200 OK\r\n");
                    writer.writeBytes("Content-Type: text/html\r\n");
                    writer.writeBytes("\r\n");
                    writer.writeBytes(html);
                    writer.flush();
                } else {
                    byte[] bytes = new byte[(int) file.length()];
                    FileInputStream fileInputStream = new FileInputStream(file);
                    fileInputStream.read(bytes);
                    fileInputStream.close();

                    writer.writeBytes("HTTP/1.1 200 OK\r\n");

                    writer.writeBytes("Content-Type: text/html\r\n");
                    writer.writeBytes("\r\n");
                    writer.write(bytes);
                    writer.flush();
                }


            } else {
                writer.writeBytes("HTTP/1.1 404 Not Found\r\n");
                writer.writeBytes("\r\n");

            }
            reader.close();
            clientSocket.close();
        }

    }
}