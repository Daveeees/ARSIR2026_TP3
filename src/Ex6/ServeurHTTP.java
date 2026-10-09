package Ex6;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServeurHTTP {

    private static final int PORT = 6666;
    private static final String NOM_SERVEUR = "ServeurHTTP-ARSIR/1.0";
    private static final String DOSSIER_SITE = "site1";

    private static final Map<Integer, String> CODES = new HashMap<>();

    static {
        CODES.put(200, "OK");
        CODES.put(400, "Bad Request");
        CODES.put(404, "Not Found");
        CODES.put(405, "Method Not Allowed");
        CODES.put(418, "I'm a teapot");
        CODES.put(500, "Internal Server Error");
        CODES.put(502, "Bad Gateway");
    }

    private static final Pattern REGEX_REQUETE = Pattern.compile("^([A-Za-z]+)\\s+(\\S+)\\s+HTTP/(\\d+(?:\\.\\d+)?)$");
    private static final Pattern REGEX_HEXA = Pattern.compile("%([0-9a-fA-F]{2})");

    public static void main(String[] args) {
        try {
            ServerSocket socketServeur = new ServerSocket(PORT);
            System.out.println("Serveur HTTP (Exo 6) démarré sur le port " + PORT + "...");

            while (true) {
                Socket socketClient = socketServeur.accept();
                new Thread(() -> gererClient(socketClient)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String recevoirRequete(BufferedReader entree) throws IOException {
        StringBuilder sb = new StringBuilder();
        String ligne;
        while ((ligne = entree.readLine()) != null) {
            if (ligne.isEmpty()) break;
            sb.append(ligne).append("\r\n");
        }
        return sb.toString();
    }

    public static int verifierRequete(String requete) {
        if (requete == null || requete.isEmpty()) return 400;

        String[] lignes = requete.split("\r?\n");
        String[] elements = lignes[0].split(" ");

        if (elements.length != 3) return 400;
        if (!elements[2].matches("HTTP/1\\.[01]")) return 400;
        if (elements[1].isEmpty()) return 400;
        if (!elements[0].matches("[A-Z]+")) return 400;

        if (!elements[0].equals("GET")) return 405;

        if (elements[2].equals("HTTP/1.1")) {
            boolean hostPresent = false;

            for (String ligne : lignes) {
                if (ligne.matches("(?i)Host:\\s*\\S+.*")) hostPresent = true;
            }

            if (!hostPresent) return 400;
        }

        return 200;
    }


    public static File construireCheminFichier(String requete, String repertoireRacine) {
        if (requete == null || requete.trim().isEmpty()) return null;

        String premiereLigne = requete.split("\r?\n")[0].trim();
        Matcher matcher = REGEX_REQUETE.matcher(premiereLigne);
        if (!matcher.matches()) return null;

        String uri = matcher.group(2);

        if (uri.startsWith("http://") || uri.startsWith("https://")) {
            int idx = uri.indexOf('/', uri.indexOf("//") + 2);
            uri = (idx != -1) ? uri.substring(idx) : "/";
        }

        int idxInterrogation = uri.indexOf('?');
        if (idxInterrogation != -1) {
            uri = uri.substring(0, idxInterrogation);
        }

        Matcher matcherHexa = REGEX_HEXA.matcher(uri);
        StringBuilder sb = new StringBuilder();
        while (matcherHexa.find()) {
            char ascii = (char) Integer.parseInt(matcherHexa.group(1), 16);
            matcherHexa.appendReplacement(sb, Matcher.quoteReplacement(String.valueOf(ascii)));
        }
        matcherHexa.appendTail(sb);
        uri = sb.toString();

        File racine = new File(repertoireRacine);
        if (!racine.exists()) {
            File alt = new File("..", repertoireRacine);
            if (alt.exists()) racine = alt;
        }

        File cible = (uri.equals("/") || uri.isEmpty()) ? new File(racine, "index.html") : new File(racine, uri.startsWith("/") ? uri.substring(1) : uri);

        if (cible.exists() && cible.isDirectory()) {
            cible = new File(cible, "index.html");
        }

        return cible;
    }

    public static String genererEnTete(int code) {
        return genererEnTete(code, -1, "text/html");
    }

    public static String genererEnTete(int code, long taille) {
        return genererEnTete(code, taille, "text/html");
    }

    public static String genererEnTete(int code, long taille, String contentType) {
        String message = CODES.getOrDefault(code, "Internal Server Error");
        if (!CODES.containsKey(code)) code = 500;

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.FRENCH);
        String date = sdf.format(new Date());

        StringBuilder sb = new StringBuilder();
        sb.append("HTTP/1.1 ").append(code).append(" ").append(message).append("\r\n");
        sb.append("Date: ").append(date).append("\r\n");
        sb.append("Server: ").append(NOM_SERVEUR).append("\r\n");
        sb.append("Connection: close\r\n");
        if (taille >= 0) sb.append("Content-Length: ").append(taille).append("\r\n");
        sb.append("Content-Type: ").append(contentType).append("\r\n\r\n");
        return sb.toString();
    }

    public static String genererReponseErreur(int code) {
        String message = CODES.getOrDefault(code, "Internal Server Error");
        if (!CODES.containsKey(code)) code = 500;

        String corps = "<!DOCTYPE html><html><head><title>Erreur " + code + "</title></head>"
                + "<body><h1>Erreur " + code + " : " + message + "</h1></body></html>\r\n";

        byte[] octets = corps.getBytes(StandardCharsets.UTF_8);
        return genererEnTete(code, octets.length, "text/html; charset=UTF-8") + corps + "\r\n";
    }

    public static void genererReponseSucces(OutputStream sortie, File fichier) throws IOException {
        String contentType = determinerContentType(fichier.getName());
        String enTete = genererEnTete(200, fichier.length(), contentType);
        sortie.write(enTete.getBytes(StandardCharsets.UTF_8));

        try (FileInputStream fis = new FileInputStream(fichier)) {
            byte[] tampon = new byte[4096];
            int lu;
            while ((lu = fis.read(tampon)) != -1) {
                sortie.write(tampon, 0, lu);
            }
        }
        sortie.flush();
    }

    private static String determinerContentType(String nom) {
        String min = nom.toLowerCase();
        if (min.endsWith(".html") || min.endsWith(".htm")) return "text/html; charset=UTF-8";
        if (min.endsWith(".css")) return "text/css; charset=UTF-8";
        if (min.endsWith(".js")) return "application/javascript";
        if (min.endsWith(".png")) return "image/png";
        if (min.endsWith(".jpg") || min.endsWith(".jpeg")) return "image/jpeg";
        return "text/html; charset=UTF-8";
    }

    private static void gererClient(Socket socketClient) {
        try {
            BufferedReader entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            OutputStream sortie = socketClient.getOutputStream();

            String requete = recevoirRequete(entree);
            if (requete.isEmpty()) {
                socketClient.close();
                return;
            }

            int code = verifierRequete(requete);
            if (code != 200) {
                sortie.write(genererReponseErreur(code).getBytes(StandardCharsets.UTF_8));
                sortie.flush();
                socketClient.close();
                return;
            }

            File fichier = construireCheminFichier(requete, DOSSIER_SITE);
            if (fichier == null || !fichier.exists() || fichier.isDirectory()) {
                sortie.write(genererReponseErreur(404).getBytes(StandardCharsets.UTF_8));
                sortie.flush();
            } else {
                genererReponseSucces(sortie, fichier);
            }

            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
