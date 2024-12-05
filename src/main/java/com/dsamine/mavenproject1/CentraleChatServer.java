package com.dsamine.mavenproject1;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * CentraleChatServer est un serveur de chat qui gère les connexions des clients, l'authentification des utilisateurs,
 * la diffusion de messages aux clients et la gestion des sessions des clients.
 */
public class CentraleChatServer {
    private static Set<ClientHandler> clientHandlers = ConcurrentHashMap.newKeySet();
    private static Map<String, String> userCredentials = new HashMap<>();
    private static Set<String> loggedInUsers = ConcurrentHashMap.newKeySet();

    /**
     * Méthode principale pour démarrer le serveur de chat et écouter les connexions entrantes des clients.
     * @param args Arguments de la ligne de commande (non utilisés).
     */
    public static void main(String[] args) {
        int port = 9643;
        loadUserCredentials();

        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Le serveur de chat fonctionne sur le port " + port + "...");
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nouveau client connecté.");
                ClientHandler handler = new ClientHandler(socket);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Charge les identifiants des utilisateurs depuis un fichier nommé "users_credentials.txt" pour authentifier les utilisateurs.
     */
    private static void loadUserCredentials() {
        try (InputStream inputStream = CentraleChatServer.class.getClassLoader().getResourceAsStream("users_credentials.txt")) {
            if (inputStream != null) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] credentials = line.split(" ");
                        if (credentials.length == 2) {
                            userCredentials.put(credentials[0], credentials[1]);
                        }
                    }
                }
            } else {
                System.out.println("Fichier non trouvé : users_credentials.txt");
            }
        } catch (IOException e) {
            System.out.println("Erreur lors du chargement des identifiants des utilisateurs : " + e.getMessage());
        }
    }

    /**
     * Ajoute un nouveau gestionnaire de client à l'ensemble des gestionnaires de clients.
     * @param handler Le gestionnaire de client à ajouter.
     */
    public static void addClientHandler(ClientHandler handler) {
        clientHandlers.add(handler);
    }

    /**
     * Authentifie un utilisateur en comparant le mot de passe fourni avec les identifiants stockés.
     * @param username Le nom d'utilisateur à authentifier.
     * @param password Le mot de passe à authentifier.
     * @return true si l'authentification est réussie, false sinon.
     */
    public static boolean authenticate(String username, String password) {
        return password.equals(userCredentials.get(username));
    }

    /**
     * Vérifie si un utilisateur est déjà connecté.
     * @param username Le nom d'utilisateur à vérifier.
     * @return true si l'utilisateur est connecté, false sinon.
     */
    public static boolean isUserLoggedIn(String username) {
        return loggedInUsers.contains(username);
    }

    /**
     * Diffuse un message à tous les clients, sauf l'expéditeur.
     * @param message Le message à envoyer.
     * @param sender Le gestionnaire de client qui envoie le message.
     */
    public static void broadcastMessage(String message, ClientHandler sender) {
        clientHandlers.stream()
            .filter(client -> client != sender)
            .forEach(client -> client.sendMessage(message));
    }

    /**
     * Supprime un client de l'ensemble des gestionnaires de clients et déconnecte l'utilisateur.
     * @param clientHandler Le gestionnaire de client à supprimer.
     */
    public static void removeClient(ClientHandler clientHandler) {
        clientHandlers.remove(clientHandler);
        if (clientHandler.getUsername() != null) {
            loggedInUsers.remove(clientHandler.getUsername());
        }
    }

    /**
     * ClientHandler est responsable de la gestion de la session d'un client, y compris l'authentification,
     * la gestion des messages et la déconnexion du client.
     */
    static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private String username;

        /**
         * Construit un gestionnaire de client pour une connexion de socket donnée.
         * @param socket Le socket représentant la connexion du client.
         */
        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        /**
         * Envoie un message au client.
         * @param message Le message à envoyer.
         */
        public void sendMessage(String message) {
            if (out != null) {
                out.println(message);
            }
        }

        /**
         * Récupère le nom d'utilisateur associé au client.
         * @return Le nom d'utilisateur.
         */
        public String getUsername() {
            return username;
        }

        /**
         * Gère la connexion du client, y compris l'authentification, la lecture des messages,
         * et la déconnexion du client.
         */
        @Override
        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
                out = new PrintWriter(socket.getOutputStream(), true);

                out.println("Entrez le nom d'utilisateur :");
                username = in.readLine();

                if (isUserLoggedIn(username)) {
                    out.println("L'utilisateur est déjà connecté. Connexion refusée.");
                    socket.close();
                    return;
                }

                out.println("Entrez le mot de passe :");
                String password = in.readLine();

                if (!CentraleChatServer.authenticate(username, password)) {
                    out.println("Échec de l'authentification. Connexion fermée.");
                    socket.close();
                    return;
                }

                // Ajoute l'utilisateur à la liste des utilisateurs connectés après une authentification réussie
                loggedInUsers.add(username);
                CentraleChatServer.addClientHandler(this);

                out.println("Bienvenue dans le chat, " + username + " !");
                CentraleChatServer.broadcastMessage(username + " a rejoint le chat.", this);

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equalsIgnoreCase("/exit")) {
                        break;
                    }
                    CentraleChatServer.broadcastMessage(username + ": " + message, this);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                CentraleChatServer.removeClient(this);
                CentraleChatServer.broadcastMessage(username + " a quitté le chat.", this);
                System.out.println("L'utilisateur " + username + " s'est déconnecté de la session.");
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
