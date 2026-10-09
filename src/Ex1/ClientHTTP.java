package Ex1;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHTTP {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("perdu.com", 80);
            BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true);

            sortie.print("GET / HTTP/1.1\r\n");
            sortie.print("Host: perdu.com\r\n");
            sortie.print("Connection: close\r\n\r\n");
            sortie.flush();

            String ligne;
            while ((ligne = entree.readLine()) != null) {
                System.out.println(ligne);
            }

            entree.close();
            sortie.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
