import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    //permet d'executer une commande bash
    public static String execCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        Process process = processBuilder.start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder resu = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            resu.append(line);
        }
        process.waitFor();
        return resu.toString();
    }

    //permet de conter le nombre de processus
    public static String getProcessCount() {
        String processCount = "Error while getting process count";
        try {
            processCount = Utils.execCommand("ps -e | wc -l");
            // formate les 1000 en 1 000
            processCount = String.format("%,d", Integer.parseInt(processCount));
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            Logger.logError("Error while getting process count: " + e.getMessage());
        }
        return processCount.replace(" ", " ");
    }

    //permet de recuperer la memoire libre
    public static String getFreeMemory() {
        String freeMemory = "Error while getting free memory";
        try {
            freeMemory = Utils.execCommand("free -m | grep Mem | awk '{print $4}'");
            freeMemory = String.format("%,d", Integer.parseInt(freeMemory));
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
            Logger.logError("Error while getting free memory: " + e.getMessage());
        }
        return freeMemory.replace(" ", " ");
    }

    //permet de recuperer le disque libre
    public static String getFreeDisk() {
        String freeDisk = "Error while getting free disk";
        try {
            freeDisk =  String.format("%,d", (int) (Files.getFileStore(Path.of("/")).getUsableSpace() / 1024 / 1024));
        } catch (IOException e) {
            e.printStackTrace();
            Logger.logError("Error while getting free disk: " + e.getMessage());
        }
        return freeDisk.replace(" ", " ");
    }

    //permet de verifier si la requete est bien formee
    public static boolean badRequestVerification(String path, Socket clientSocket, DataOutputStream writer) throws IOException {
        if (!path.contains("command=") || !path.contains("?")) {
            writer.writeBytes("HTTP/1.1 400 Bad Request\r\n");
            writer.writeBytes("\r\n");
            writer.flush();
            Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 400);
            return true;
        }
        return false;
    }
}
