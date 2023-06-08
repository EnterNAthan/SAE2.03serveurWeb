import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPOutputStream;

public class Main {

    private static final List<String> acceptedAddresses = new ArrayList<>();
    private static final List<String> rejectedAddresses = new ArrayList<>();


    public static void main(String[] args) {

        Document config = loadConfigFile("project/config.xml"); // ~/etc/myweb/myweb.conf pour le .deb
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

        assert config != null;
        Logger.init(config.getDocumentElement());


        ServerSocket serverSocket = null;
        System.out.println("url : http://localhost:" + port + "/");

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                if (serverSocket == null || serverSocket.isClosed())
                    serverSocket = new ServerSocket(port);

                Socket clientSocket = serverSocket.accept();
                BufferedReader reader = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                String path = reader.readLine().split(" ")[1];
                System.out.println(path);

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
                                Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 404);
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
                            Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 200);

                        } else {
                            String fileExtension = "";
                            try {
                                fileExtension = file.getName().split("\\.")[1];
                            } catch (ArrayIndexOutOfBoundsException ex) {
                                Logger.logError("File doesn't have an extension");
                            }
                            System.out.println(fileExtension);

                            ContentType contentType = ContentType.of(fileExtension);
                            System.out.println(contentType);

                            byte[] bytes = Files.readAllBytes(file.toPath());
                            FileInputStream fileInputStream = new FileInputStream(file);
                            //noinspection ResultOfMethodCallIgnored
                            fileInputStream.read(bytes);
                            fileInputStream.close();

                            writer.writeBytes("HTTP/1.1 200 OK\r\n");
                            if (contentType == null)
                                writer.writeBytes("Content-Type: text/html\r\n");
                            else {
                                writer.writeBytes(contentType.getHeader(fileExtension));
                                writer.writeBytes("Content-Encoding: gzip\r\n");
                            }
                            writer.writeBytes("\r\n");
                            if (contentType != null) {
                                FileInputStream fis = new FileInputStream(file);
                                GZIPOutputStream gzipOS = new GZIPOutputStream(writer);
                                byte[] buffer = new byte[1024];
                                int len;
                                while((len=fis.read(buffer)) != -1){
                                    gzipOS.write(buffer, 0, len);
                                }
                                gzipOS.close();
                                fis.close();
                            } else {
                                if (fileExtension.equalsIgnoreCase("html"))
                                    writer.writeBytes(Interpreter.formatHTMLPage(file));
                                else writer.write(bytes);
                            }
                            writer.flush();
                            Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 404);

                        }
                    } else {
                        if (path.equalsIgnoreCase("/status")) {
                            writer.writeBytes("HTTP/1.1 200 OK\r\n");
                            String body = Status.buildStatusPage();
                            System.out.println(body);
                            writer.writeBytes("Content-Length: " + body.length() + "\r\n");
                            writer.writeBytes("Content-Type: text/html\r\n");
                            writer.writeBytes("\r\n");
                            writer.writeBytes(body);
                            writer.writeBytes("\r\n");
                            writer.flush();
                            Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 200);

                        } else if (path.startsWith("/bash?")) {
                            if (Utils.badRequestVerification(path, clientSocket, writer)) continue;

                            String commandParam = URLDecoder.decode(path.split("\\?")[1].split("=")[1], StandardCharsets.UTF_8);
                            writer.writeBytes("HTTP/1.1 200 OK\r\n");
                            String body = Utils.execCommand(commandParam);
                            writer.writeBytes("Content-Length: " + body.length() + "\r\n");
                            writer.writeBytes("Content-Type: text/html\r\n");
                            writer.writeBytes("\r\n");
                            writer.writeBytes(body);
                            writer.writeBytes("\r\n");
                            writer.flush();
                            Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 200);

                        } else if (path.startsWith("/py?")) {
                            if (Utils.badRequestVerification(path, clientSocket, writer)) continue;

                            String commandParam = URLDecoder.decode(path.split("\\?")[1].split("=")[1], StandardCharsets.UTF_8);
                            writer.writeBytes("HTTP/1.1 200 OK\r\n");
                            String body = Interpreter.interpretPython(commandParam);
                            writer.writeBytes("Content-Length: " + body.length() + "\r\n");
                            writer.writeBytes("Content-Type: text/html\r\n");
                            writer.writeBytes("\r\n");
                            writer.writeBytes(body);
                            writer.writeBytes("\r\n");
                            writer.flush();
                            Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 200);
                        } else {
                            writer.writeBytes("HTTP/1.1 404 Not Found\r\n");
                            writer.writeBytes("\r\n");
                            Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 404);

                        }

                    }
                } else {
                    writer.writeBytes("HTTP/1.1 403 Forbidden\r\n");
                    writer.writeBytes("\r\n");
                    Logger.logAccess(clientSocket.getInetAddress().getHostAddress(), true, path, 403);


                }

                reader.close();
                clientSocket.close();
            } catch (NullPointerException ignored){
                //arrive que avec chrome car le Keep-Alive est activé et forcé on dirait du coup le reader.readLine() est null...
            } catch (Exception e) {
                e.printStackTrace();
                Logger.logError(e.getMessage());
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
            Logger.logError(e.getMessage());

            // Renvoyer null pour indiquer qu'une erreur s'est produite lors du chargement du fichier.
            return null;
        }
    }



    /*nous avons decider de faire avec des ip local car ce n'etais pas indiquer explicitement dans le sujet */
    public static boolean isIpAddressAllowed(String ipAddress) {
        for (String accepted : acceptedAddresses) {
            if (ipAddress.equals(accepted)) {
                return true;
            }
        }

        for (String rejected : rejectedAddresses) {
            if (ipAddress.equals(rejected)) {
                return false;
            }
        }
        return true; // Autoriser par défaut si l'adresse IP n'est pas explicitement acceptée ou refusée
    }
}