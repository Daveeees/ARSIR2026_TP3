package Ex3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientHTTP {

    public static void main(String[] args) {
        try {
            Socket socket = new Socket("localhost", 6666);
            PrintWriter sortie = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader entree = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            sortie.print("GET /index.html HTTP/1.1\r\n");
            sortie.print("Host: localhost:6666\r\n");
            sortie.print("Connection: close\r\n\r\n");
            sortie.flush();

            sortie.close();
            entree.close();
            socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
