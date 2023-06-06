import org.w3c.dom.Element;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;

@SuppressWarnings("ResultOfMethodCallIgnored")
public class Logger {
    private static File logAccessFile, logErrorFile;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
    public static void init(Element webconf) {
        // création des fichier des log s'il sotn pas crée
        String logAccesPath = webconf.getElementsByTagName("accesslog").item(0).getTextContent();
        String logErrorPath = webconf.getElementsByTagName("errorlog").item(0).getTextContent();
        logAccessFile = new File(logAccesPath);
        if (!logAccessFile.exists()) {
            try {
                new File(logAccesPath.substring(0, logAccesPath.lastIndexOf("/"))).mkdirs();
                logAccessFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        logErrorFile = new File(webconf.getElementsByTagName("errorlog").item(0).getTextContent());
        if (!logErrorFile.exists()) {
            try {
                new File(logErrorPath.substring(0, logErrorPath.lastIndexOf("/"))).mkdirs();
                logErrorFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logAccess(String ip, boolean authorized, String requestPath, int status) {
        try {
            FileWriter fw = new FileWriter(logAccessFile, true);
            fw.write("[" + dateFormat.format(System.currentTimeMillis()) + "]: " + ip + " got " + (authorized ? "Authorized" : "Refused") + " for " + requestPath + " with code " + status + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void logError(String message) {
        try {
            FileWriter fw = new FileWriter(logErrorFile, true);
            fw.write("[" + dateFormat.format(System.currentTimeMillis()) + "]: " + message + "\n");
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
