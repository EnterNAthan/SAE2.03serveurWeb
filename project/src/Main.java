import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public class Main {

    public static void main(String[] args) throws IOException {
        Document config = loadConfigFile("project/config.xml");
        int port;
        String root = "www";
        if (args.length != 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 8888;
        }
        if (config != null) {
            Element webconf = config.getDocumentElement();
            port = Integer.parseInt(webconf.getElementsByTagName("port").item(0).getTextContent());
            root = webconf.getElementsByTagName("root").item(0).getTextContent();
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
    public static Document loadConfigFile(String filePath) {
        try {
            // Créer une instance de DocumentBuilderFactory, qui est une classe abstraite
            // utilisée pour créer des objets DocumentBuilder.
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

            // Créer un constructeur de documents à l'aide de la factory.
            // Le DocumentBuilder est utilisé pour analyser les fichiers XML et créer des objets Document.
            DocumentBuilder builder = factory.newDocumentBuilder();

            // Parser le fichier XML spécifié par le chemin filePath.
            // La méthode parse prend un objet File en entrée et renvoie un objet Document représentant le contenu du fichier.
            Document config = builder.parse(new File(filePath));

            // Normaliser le document en supprimant les nœuds vides et en fusionnant les nœuds adjacents.
            // Cela garantit une structure cohérente du document.
            config.getDocumentElement().normalize();

            // Renvoyer le document XML analysé.
            return config;
        } catch (IOException | SAXException | ParserConfigurationException e) {
            // Gérer les exceptions liées à la lecture du fichier ou à la configuration du parser.
            // Afficher les informations d'erreur à l'aide de e.printStackTrace().
            e.printStackTrace();

            // Renvoyer null pour indiquer qu'une erreur s'est produite lors du chargement du fichier.
            return null;
        }
    }


}