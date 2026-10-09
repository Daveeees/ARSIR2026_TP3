package Ex3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class ServeurHTTP {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            ServerSocket socketServeur = new ServerSocket(PORT);
            System.out.println("Serveur HTTP démarré sur le port " + PORT + "...");

            while (true) {
                Socket socketClient = socketServeur.accept();
                System.out.println("Connexion : " + socketClient.getInetAddress());

                Thread thread = new Thread(() -> gererClient(socketClient));
                thread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void gererClient(Socket socketClient) {
        try {
            BufferedReader entree = new BufferedReader(new InputStreamReader(socketClient.getInputStream()));

            String ligne;
            while ((ligne = entree.readLine()) != null) {
                if (ligne.isEmpty()) {
                    break;
                }
                System.out.println(ligne);
            }

            entree.close();
            socketClient.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
