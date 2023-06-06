import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Main {

    private static final List<String> acceptedAddresses = new ArrayList<>();
    private static final List<String> rejectedAddresses = new ArrayList<>();


    public static void main(String[] args) {

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

            // Récupérer les adresses IP acceptées et refusées dans une NodeList qui est une liste de noeuds XML.
            NodeList acceptNodes = webconf.getElementsByTagName("accept");
            for (int i = 0; i < acceptNodes.getLength(); i++) {
                acceptedAddresses.add(acceptNodes.item(i).getTextContent());
            }

            NodeList rejectNodes = webconf.getElementsByTagName("reject");
            for (int i = 0; i < rejectNodes.getLength(); i++) {
                rejectedAddresses.add(rejectNodes.item(i).getTextContent());
            }

        }


        ServerSocket serverSocket = null;
        System.out.println("url : http://localhost:" + port + "/");
        String line;

        while (true) {
            try {
                if (serverSocket == null || serverSocket.isClosed())
                    serverSocket = new ServerSocket(port);

                Socket clientSocket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String path = reader.readLine().split(" ")[1];
                System.out.println(path);

                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    //System.out.println(line);
                }
                DataOutputStream writer = new DataOutputStream(clientSocket.getOutputStream());

                File file = new File(root + path);
                if (isIpAddressAllowed(clientSocket.getInetAddress().getHostAddress())) {
                    if (file.exists()) {
                        if (file.isDirectory()) {
                            File[] files = file.listFiles();

                            if (files == null) { // si aucun fichier dans le dossier
                                writer.writeBytes("HTTP/1.1 404 Not Found\r\n");
                                writer.writeBytes("\r\n");
                                writer.flush();
                                continue;
                            }

                            writer.writeBytes("HTTP/1.1 200 OK\r\n");
                            writer.writeBytes("Content-Type: text/html\r\n");
                            writer.writeBytes("\r\n");

                            writer.writeBytes("<html><body>");

                            if (path.equals("/"))
                                path = "";
                            for (File f : files) {
                                writer.writeBytes("<a href='");
                                writer.writeBytes(path + "/" + f.getName());
                                writer.writeBytes("'>");
                                writer.writeBytes(f.getName());
                                if (f.isDirectory())
                                    writer.writeBytes("/");
                                writer.writeBytes("</a><br>");
                            }
                            writer.writeBytes("</body></html>");

                            writer.flush();
                        } else {
                            String fileExtension = "";
                            try {
                                fileExtension = file.getName().split("\\.")[1];
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                System.err.println("No ext");
                            }
                            System.out.println(fileExtension);

                            ContentType contentType = ContentType.of(fileExtension);
                            System.out.println(contentType);

                            byte[] bytes = Files.readAllBytes(file.toPath());
                            FileInputStream fileInputStream = new FileInputStream(file);
                            fileInputStream.read(bytes);
                            fileInputStream.close();

                            writer.writeBytes("HTTP/1.1 200 OK\r\n");
                            if (contentType == null)
                                writer.writeBytes("Content-Type: text/html\r\n");
                            else {
                                writer.writeBytes(contentType.getHeader(fileExtension));

                                //writer.writeBytes("Content-Length: " + Base64.getEncoder().encodeToString(bytes).length() + "\r\n");
                                //writer.writeBytes("Content-Transfer-Encoding: base64\r\n");
                            }
                            writer.writeBytes("\r\n");
                            if (contentType != null) {
                                //writer.writeBytes(Base64.getEncoder().encodeToString(bytes));
                                writer.write(bytes);
                            } else writer.write(bytes);
                            writer.flush();
                        }
                    } else {
                        writer.writeBytes("HTTP/1.1 404 Not Found\r\n");
                        writer.writeBytes("\r\n");

                    }
                } else {
                    writer.writeBytes("HTTP/1.1 403 Forbidden\r\n");
                    writer.writeBytes("\r\n");

                }

                reader.close();
                clientSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Fatal Error: " + e.getMessage());
                e.printStackTrace();
            }
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


    public static boolean isIpAddressAllowed(String ipAddress) {
        for (String accepted : acceptedAddresses) {
            if (ipAddress.matches(accepted)) {
                return true;
            }
        }

        for (String rejected : rejectedAddresses) {
            if (ipAddress.matches(rejected)) {
                return false;
            }
        }
        return true; // Autoriser par défaut si l'adresse IP n'est pas explicitement acceptée ou refusée
    }
}