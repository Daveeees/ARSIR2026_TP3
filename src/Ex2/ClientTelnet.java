package Ex2;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientTelnet {

    public static void main(String[] args) {
        envoyerRequete("GET /\r\n");
        envoyerRequete("GET / HTTP/1.1\r\n\r\n");
        envoyerRequete("GET / HTTP/1.1\r\nHost: perdu.com\r\n\r\n");
    }

    private static void envoyerRequete(String requete) {
        System.out.println("=== Envoi : " + requete.replace("\r", "\\r").replace("\n", "\\n") + " ===");
        try {
            Socket socket = new Socket("perdu.com", 80);
            PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            sortie.print(requete);
            sortie.flush();

            String ligne;
            int count = 0;
            while ((ligne = entree.readLine()) != null) {
                System.out.println(ligne);
                count++;
                if (count > 10) {
                    System.out.println("...");
                    break;
                }
            }

            entree.close();
            sortie.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println();
    }
}
