package Ex4;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class ServeurHTTP {

    private static final int PORT = 8080; //6666

    public static void main(String[] args) {
        try (ServerSocket serveur = new ServerSocket(PORT)) {
            System.out.println("Serveur démarré sur le port " + PORT);

            while (true) {
                Socket client = serveur.accept();
                new Thread(() -> gererClient(client)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Q1 : Recevoir la requête et la stocker dans une String
    public static String recevoirRequete(BufferedReader entree) throws IOException {
        StringBuilder requete = new StringBuilder();
        String ligne;

        while ((ligne = entree.readLine()) != null) {
            if (ligne.isEmpty()) break;
            requete.append(ligne).append("\r\n");
        }

        return requete.toString();
    }

    // Q2 : Vérifier la requête et retourner 400, 405 ou 200
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

    private static void gererClient(Socket client) {
        try (
                Socket socket = client;
                BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true)
        ) {
            String requete = recevoirRequete(entree);
            int code = verifierRequete(requete);

            System.out.println("Requête reçue :\n" + requete);
            System.out.println("Code : " + code);

            sortie.println(code);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}