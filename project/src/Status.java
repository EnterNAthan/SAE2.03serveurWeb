import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Status {
    public static String buildStatusPage() throws IOException {
        String freeMemory = Utils.getFreeMemory();
        System.out.println(freeMemory);
        String freeDisk = Utils.getFreeDisk();
        String processCount = Utils.getProcessCount().replace(" ", "&nbsp;");


        return """
                <html>
                    <head>
                        <title>Status</title>
                    </head>
                    <body>
                        <h1>Status</h1>
                        <p>Free memory: <strong>%s Mb</strong></p>
                        <p>Free disk: <strong>%s Mb</strong></p>
                        <p>Process count: <strong>%s</strong></p>
                    </body>
                </html>
                """.formatted(freeMemory, freeDisk, processCount);
    }
}
