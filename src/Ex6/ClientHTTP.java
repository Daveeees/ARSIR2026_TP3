package Ex6;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHTTP {

    public static void main(String[] args) {
        tester("GET /index.html HTTP/1.1\r\nHost: localhost:6666\r\n\r\n");
        tester("GET /dossier/ HTTP/1.1\r\nHost: localhost:6666\r\n\r\n");
        tester("GET /mon%20document.html HTTP/1.1\r\nHost: localhost:6666\r\n\r\n");
        tester("GET /inexistant.html HTTP/1.1\r\nHost: localhost:6666\r\n\r\n");
    }

    private static void tester(String requete) {
        try (Socket socket = new Socket("localhost", 6666);
             PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            sortie.print(requete);
            sortie.flush();

            String ligne;
            int count = 0;
            while ((ligne = entree.readLine()) != null) {
                System.out.println(ligne);
                if (++count > 10) break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("---------------------------------");
    }
}
