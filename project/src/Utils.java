import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.NumberFormat;

public class Utils {
    public static String execCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder("bash", "-c", command);
        Process process = processBuilder.start();

        // Read the output of the command
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        StringBuilder resu = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            resu.append(line);
        }
        process.waitFor();
        return resu.toString();
    }

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
}
