import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Interpreter {
    public static String interpretPython(String code) {
        try {
            return Utils.execCommand("python3 -c \"" + code + "\"");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            Logger.logError("Error while interpreting python code: " + e.getMessage() + "\nCode: " + code);
        }
        return "Error while interpreting python code";
    }

    public static String formatHTMLPage(File file) throws IOException, InterruptedException {
        String html = new String(Files.readAllBytes(file.toPath()));
        while (html.contains("<code interpreteur=\"")) {
            //La date est <code interpreteur="/bin/bash">date</code>
            String interpreter = html.split("<code interpreteur=\"")[1].split("\">")[0];
            String code = html.split("<code interpreteur=\"")[1].split("\">")[1].split("</code>")[0];

            String result = Utils.execCommand(interpreter + " -c \"" + code + "\"");

            html = html.replace("<code interpreteur=\"" + interpreter + "\">" + code + "</code>", result);
        }

        return html;
    }
}
