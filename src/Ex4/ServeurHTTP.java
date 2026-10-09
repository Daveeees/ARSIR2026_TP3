package Ex4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ServeurHTTP {

    private static final int PORT = 6666;
    private static final Pattern REGEX_REQUETE = Pattern.compile("^([A-Za-z]+)\\s+(\\S+)\\s+HTTP/(\\d+(?:\\.\\d+)?)$");

    public static void main(String[] args) {
        try (ServerSocket socketServeur = new ServerSocket(PORT)) {
            System.out.println("Serveur HTTP (Exo 4) démarré sur le port " + PORT + "...");

            while (true) {
                Socket socketClient = socketServeur.accept();
                new Thread(() -> gererClient(socketClient)).start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Q1 : Recevoir la requête HTTP et la stocker sous forme de String
    public static String recevoirRequete(BufferedReader entree) throws IOException {
        StringBuilder sb = new StringBuilder();
        String ligne;

        while ((ligne = entree.readLine()) != null) {
            if (ligne.isEmpty()) break;
            sb.append(ligne).append("\r\n");
        }

        return sb.toString();
    }

    // Q2 : Vérifier la requête HTTP et retourner 400, 405 ou 200
    public static int verifierRequete(String requete) {
        if (requete == null || requete.trim().isEmpty()) return 400;

        String[] lignes = requete.split("\r?\n");
        Matcher matcher = REGEX_REQUETE.matcher(lignes[0]);

        if (!matcher.matches()) return 400;

        String methode = matcher.group(1);
        String version = matcher.group(3);

        if (!methode.equals("GET")) return 405;

        if (version.equals("1.1")) {
            boolean hostPresent = false;

            for (int i = 1; i < lignes.length; i++) {
                if (lignes[i].matches("(?i)Host:\\s*\\S+.*")) {
                    hostPresent = true;
                }
            }

            if (!hostPresent) return 400;
        }

        return 200;
    }

    // Gérer le client : recevoir, vérifier et répondre
    private static void gererClient(Socket socketClient) {
        try (
                Socket client = socketClient;
                BufferedReader entree = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter sortie = new PrintWriter(client.getOutputStream(), true)
        ) {
            String requete = recevoirRequete(entree);
            int code = verifierRequete(requete);

            System.out.println("Requête reçue :\n" + requete);
            System.out.println("Code d'évaluation : " + code);

            String description = switch (code) {
                case 200 -> "OK";
                case 400 -> "Bad Request";
                case 405 -> "Method Not Allowed";
                default -> "Error";
            };

            String corps = "Code HTTP : " + code + "\n";
            byte[] contenu = corps.getBytes(java.nio.charset.StandardCharsets.UTF_8);

            sortie.print("HTTP/1.1 " + code + " " + description + "\r\n");
            sortie.print("Content-Type: text/plain; charset=UTF-8\r\n");
            sortie.print("Content-Length: " + contenu.length + "\r\n");
            if (code == 405) sortie.print("Allow: GET\r\n");
            sortie.print("Connection: close\r\n");
            sortie.print("\r\n");
            sortie.print(corps);
            sortie.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}