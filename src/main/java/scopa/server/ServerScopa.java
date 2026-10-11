package scopa.server;

import java.io.*;
import java.net.*;
import java.util.*;

public class ServerScopa {
    private static final int PORTA = 5555;
    private static final List<GestoreClient> inAttesa = new ArrayList<>();

    public static void main(String[] args) {
        System.out.println("=== SERVER SCOPA AVVIATO SULLA PORTA " + PORTA + " ===");
        try (ServerSocket serverSocket = new ServerSocket(PORTA)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Nuovo giocatore connesso da: " + socket.getInetAddress());
                GestoreClient client = new GestoreClient(socket);
                new Thread(client).start();
                gestisciAccoppiamento(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static synchronized void gestisciAccoppiamento(GestoreClient client) {
        inAttesa.add(client);
        if (inAttesa.size() >= 2) {
            GestoreClient p1 = inAttesa.remove(0);
            GestoreClient p2 = inAttesa.remove(0);
            StanzaPartita stanza = new StanzaPartita(p1, p2);
            stanza.avvia();
        } else {
            client.inviaMessaggio("WAIT|In attesa di un altro giocatore...");
        }
    }
}

class GestoreClient implements Runnable {
    private final Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private StanzaPartita stanza;

    public GestoreClient(Socket socket) {
        this.socket = socket;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setStanza(StanzaPartita stanza) {
        this.stanza = stanza;
    }

    public void inviaMessaggio(String msg) {
        if (out != null) out.println(msg);
    }

    @Override
    public void run() {
        try {
            String linea;
            while ((linea = in.readLine()) != null) {
                if (stanza != null) {
                    stanza.riceviComando(this, linea);
                }
            }
        } catch (IOException e) {
            System.out.println("Giocatore disconnesso.");
        } finally {
            if (stanza != null) stanza.gestisciDisconnessione(this);
        }
    }
}

class StanzaPartita {
    private final GestoreClient p1;
    private final GestoreClient p2;

    public StanzaPartita(GestoreClient p1, GestoreClient p2) {
        this.p1 = p1;
        this.p2 = p2;
        p1.setStanza(this);
        p2.setStanza(this);
    }

    public void avvia() {
        p1.inviaMessaggio("START|1|Sei il Giocatore 1");
        p2.inviaMessaggio("START|2|Sei il Giocatore 2");
        System.out.println("Nuova partita avviata tra due giocatori online!");
    }

    public void riceviComando(GestoreClient mittente, String comando) {
        GestoreClient destinatario = (mittente == p1) ? p2 : p1;
        // Inoltra la mossa/stato all'altro client
        destinatario.inviaMessaggio("MOSSA|" + comando);
    }

    public void gestisciDisconnessione(GestoreClient uscito) {
        GestoreClient rimasto = (uscito == p1) ? p2 : p1;
        rimasto.inviaMessaggio("DISCONNECT|L'avversario si è disconnesso.");
    }
}