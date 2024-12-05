package com.dsamine.mavenproject1;

import java.io.*;
import java.net.*;

/**
 * CentraleChatClient est un client qui se connecte au serveur de chat, envoie et reçoit des messages,
 * et gère la connexion et la reconnexion au serveur.
 */
public class CentraleChatClient {
    private BufferedReader in;
    private PrintWriter out;
    private Socket socket;

    /**
     * Méthode principale pour démarrer le client et se connecter au serveur.
     * @param args Arguments de la ligne de commande (non utilisés).
     */
    public static void main(String[] args) {
        CentraleChatClient client = new CentraleChatClient();
        client.start();
    }

    /**
     * Démarre le client et gère la connexion au serveur, ainsi que la lecture et l'envoi de messages.
     * Le client tente de se reconnecter en cas de perte de connexion.
     */
    public void start() {
        while (true) {
            try {
                connectToServer();

                // Lire les entrées de l'utilisateur et envoyer les messages au serveur
                BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in));
                String userInput;
                while ((userInput = consoleInput.readLine()) != null) {
                    out.println(userInput);
                    if (userInput.equalsIgnoreCase("/exit")) {
                        System.out.println("Déconnecté du serveur. Tapez '/login' pour vous reconnecter.");
                        break;
                    }
                }

                // Quitter la boucle uniquement si l'utilisateur se déconnecte explicitement
                break;
            } catch (ConnectException e) {
                System.err.println("Impossible de se connecter au serveur. Assurez-vous que le serveur est en fonctionnement.");
                waitBeforeReconnect();
            } catch (IOException e) {
                System.err.println("Connexion perdue. Tentative de reconnexion...");
                waitBeforeReconnect();
            } finally {
                cleanup();
            }
        }
    }

    /**
     * Se connecte au serveur en établissant une connexion Socket avec l'adresse "localhost" et le port 12345.
     * Démarre également un thread pour écouter les messages du serveur.
     * @throws IOException Si une erreur de connexion se produit.
     */
    private void connectToServer() throws IOException {
        socket = new Socket("localhost", 9643);
        in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        out = new PrintWriter(socket.getOutputStream(), true);

        // Démarre un thread pour écouter les messages du serveur
        new Thread(new ServerListener()).start();
        System.out.println("Connecté au serveur.");
    }

    /**
     * Attend 5 secondes avant de tenter de se reconnecter en cas de perte de connexion.
     */
    private void waitBeforeReconnect() {
        try {
            System.out.println("Tentative de reconnexion dans 5 secondes...");
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Effectue un nettoyage en fermant la connexion Socket si elle est encore ouverte.
     */
    private void cleanup() {
        if (socket != null && !socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * La classe ServerListener écoute les messages envoyés par le serveur et les affiche à l'écran.
     */
    private class ServerListener implements Runnable {
        /**
         * Lit les messages envoyés par le serveur et les affiche dans la console.
         */
        @Override
        public void run() {
            try {
                String serverMessage;
                while ((serverMessage = in.readLine()) != null) {
                    System.out.println(serverMessage);
                }
            } catch (IOException e) {
                System.out.println("Déconnecté du serveur. Tapez '/login' pour vous reconnecter.");
            }
        }
    }
}
