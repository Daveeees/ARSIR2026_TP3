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
        try {
            ServerSocket socketServeur = new ServerSocket(PORT);
            System.out.println("Serveur HTTP (Exo 4) démarré sur le port " + PORT + "...");

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
        if (requete == null || requete.trim().isEmpty()) return 400;

        String premiereLigne = requete.split("\r?\n")[0].trim();
        Matcher matcher = REGEX_REQUETE.matcher(premiereLigne);

        if (!matcher.matches()) return 400;
        if (!matcher.group(1).equalsIgnoreCase("GET")) return 405;

        return 200;
    }

    private static void gererClient(Socket socketClient) {
        try {
            BufferedReader entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));
            PrintWriter sortie = new PrintWriter(socketClient.getOutputStream(), true);

            String requete = recevoirRequete(entree);
            int code = verifierRequete(requete);

            System.out.println("Requête reçue :\n" + requete);
            System.out.println("Code d'évaluation : " + code);

            sortie.println("HTTP/1.1 " + code);
            sortie.println("Content-Type: text/plain; charset=UTF-8");
            sortie.println("Connection: close");
            sortie.println();
            sortie.println("Code HTTP : " + code);

            entree.close();
            sortie.close();
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
