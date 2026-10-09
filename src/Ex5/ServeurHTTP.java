package Ex5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
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

    private static final int PORT = 8080;
    private static final String NOM_SERVEUR = "ServeurHTTP-ARSIR/1.0";
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

    public static void main(String[] args) {
        try {
            ServerSocket socketServeur = new ServerSocket(PORT);
            System.out.println("Serveur HTTP (Exo 5) démarré sur le port " + PORT + "...");

            while (true) {
                Socket socketClient = socketServeur.accept();
                new Thread(() -> gererClient(socketClient)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String genererEnTete(int code) {
        return genererEnTete(code, -1);
    }

    public static String genererEnTete(int code, long tailleContenu) {
        String message = CODES.getOrDefault(code, "Internal Server Error");
        if (!CODES.containsKey(code)) {
            code = 500;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.FRENCH);
        String date = sdf.format(new Date());

        StringBuilder enTete = new StringBuilder();
        enTete.append("HTTP/1.1 ").append(code).append(" ").append(message).append("\r\n");
        enTete.append("Date: ").append(date).append("\r\n");
        enTete.append("Server: ").append(NOM_SERVEUR).append("\r\n");
        enTete.append("Connection: close\r\n");
        if (tailleContenu >= 0) {
            enTete.append("Content-Length: ").append(tailleContenu).append("\r\n");
        }
        enTete.append("Content-Type: text/html\r\n");
        enTete.append("\r\n");

        return enTete.toString();
    }

    public static String genererReponseErreur(int code) {
        String message = CODES.getOrDefault(code, "Internal Server Error");
        if (!CODES.containsKey(code)) {
            code = 500;
        }

        String corps = "<!DOCTYPE html><html><head><title>Erreur " + code + "</title></head>"
                + "<body><h1>Erreur " + code + " : " + message + "</h1></body></html>\r\n";

        byte[] octets = corps.getBytes(StandardCharsets.UTF_8);
        return genererEnTete(code, octets.length) + corps + "\r\n";
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

    private static void gererClient(Socket socketClient) {
        try {
            BufferedReader entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            PrintWriter sortie = new PrintWriter(socketClient.getOutputStream(), true);

            String requete = recevoirRequete(entree);
            int code = verifierRequete(requete);

            System.out.println("Requête reçue :\n" + requete);
            System.out.println("Code renvoyé : " + code + " " + CODES.get(code));

            if (code != 200) {
                sortie.print(genererReponseErreur(code));
            } else {
                String corps = "<html><body><h1>200 OK</h1></body></html>\r\n";
                sortie.print(genererEnTete(200, corps.getBytes(StandardCharsets.UTF_8).length));
                sortie.print(corps + "\r\n");
            }
            sortie.flush();

            entree.close();
            sortie.close();
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
