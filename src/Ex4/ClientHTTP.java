package Ex4;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHTTP {

    public static void main(String[] args) {
        tester("GET /index.html HTTP/1.1\r\nHost: localhost:6666\r\n\r\n");
        tester("POST /login HTTP/1.1\r\nHost: localhost:6666\r\n\r\n");
        tester("REQUETE_INVALIDE\r\n\r\n");
    }

    private static void tester(String requete) {
        try (Socket socket = new Socket("localhost", 6666);
             PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            sortie.print(requete);
            sortie.flush();

            String ligne;
            while ((ligne = entree.readLine()) != null) {
                System.out.println(ligne);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("---------------------------------");
    }
}
