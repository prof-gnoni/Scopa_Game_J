package scopa.server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerScopa {
    private static final Logger LOG = Logger.getLogger(ServerScopa.class.getName());
    public static final int PORTA_PREDEFINITA = 5555;

    private final int porta;
    private final List<GestoreClient> inAttesa = new ArrayList<>();
    private final List<GestoreClient> connessi = new ArrayList<>();
    private ServerSocket serverSocket;
    private Thread threadAccettazione;
    private volatile boolean attivo = false;

    public ServerScopa(int porta) {
        this.porta = porta;
    }

    // Avvio da solo (server dedicato): resta acceso finché non si chiude il processo
    public static void main(String[] args) throws Exception {
        ServerScopa server = new ServerScopa(PORTA_PREDEFINITA);
        server.avvia();
        server.threadAccettazione.join();
    }

    /**
     * Apre la porta e accetta i giocatori in un thread in background.
     * Il thread è daemon: quando il gioco si chiude, il server si spegne con lui.
     *
     * @throws BindException se la porta è già in uso
     */
    public synchronized void avvia() throws IOException {
        if (attivo) return;
        serverSocket = new ServerSocket(porta);
        attivo = true;
        threadAccettazione = new Thread(this::accettaConnessioni, "ServerScopa-" + porta);
        threadAccettazione.setDaemon(true);
        threadAccettazione.start();
        visualizza("=== SERVER SCOPA AVVIATO SULLA PORTA " + porta + " ===");
    }

    public synchronized void ferma() {
        if (!attivo) return;
        attivo = false;
        try {
            serverSocket.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Chiusura della porta " + porta, e);
        }
        for (GestoreClient c : connessi) c.chiudi();
        connessi.clear();
        inAttesa.clear();
        visualizza("=== SERVER SCOPA FERMATO ===");
    }

    public boolean isAttivo() {
        return attivo;
    }

    public int getPorta() {
        return porta;
    }

    // Indirizzi IPv4 di questo computer sulla rete (da comunicare all'avversario)
    public static List<String> indirizziLocali() {
        List<String> risultato = new ArrayList<>();
        try {
            for (NetworkInterface ni : Collections.list(NetworkInterface.getNetworkInterfaces())) {
                if (!ni.isUp() || ni.isLoopback() || ni.isVirtual()) continue;
                for (InetAddress indirizzo : Collections.list(ni.getInetAddresses())) {
                    if (indirizzo instanceof Inet4Address && !indirizzo.isLinkLocalAddress()) {
                        risultato.add(indirizzo.getHostAddress());
                    }
                }
            }
        } catch (SocketException e) {
            LOG.log(Level.WARNING, "Impossibile leggere le interfacce di rete", e);
        }
        return risultato;
    }

    private void accettaConnessioni() {
        while (attivo) {
            try {
                Socket socket = serverSocket.accept();
                visualizza("Nuovo giocatore connesso da: " + socket.getInetAddress());
                GestoreClient client = new GestoreClient(socket, this);
                Thread t = new Thread(client, "GestoreClient-" + socket.getInetAddress());
                t.setDaemon(true);
                t.start();
                gestisciAccoppiamento(client);
            } catch (IOException e) {
                // accept() viene interrotto anche da ferma(): in quel caso non è un errore
                if (attivo) LOG.log(Level.SEVERE, "Errore del server sulla porta " + porta, e);
            }
        }
    }

    // Unico metodo della classe che stampa su console
    private void visualizza(String messaggio) {
        System.out.println(messaggio);
    }

    // Un giocatore uscito prima di essere accoppiato non deve restare in attesa
    synchronized void rimuoviDaAttesa(GestoreClient client) {
        inAttesa.remove(client);
        connessi.remove(client);
    }

    private synchronized void gestisciAccoppiamento(GestoreClient client) {
        connessi.add(client);
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
    private static final Logger LOG = Logger.getLogger(GestoreClient.class.getName());

    private final Socket socket;
    private final ServerScopa server;
    private PrintWriter out;
    private BufferedReader in;
    private volatile StanzaPartita stanza;
    private volatile boolean connesso = true;

    public GestoreClient(Socket socket, ServerScopa server) {
        this.socket = socket;
        this.server = server;
        try {
            this.out = new PrintWriter(socket.getOutputStream(), true);
            this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Impossibile aprire i flussi per il client " + socket.getInetAddress(), e);
        }
    }

    // Unico metodo della classe che stampa su console
    private void visualizza() {
        System.out.println("Giocatore " + getIndirizzo() + (connesso ? " connesso" : " disconnesso"));
    }

    public String getIndirizzo() {
        return String.valueOf(socket.getInetAddress());
    }

    public void setStanza(StanzaPartita stanza) {
        this.stanza = stanza;
    }

    public void inviaMessaggio(String msg) {
        if (out != null) out.println(msg);
    }

    public void chiudi() {
        try {
            socket.close();
        } catch (IOException e) {
            LOG.log(Level.FINE, "Chiusura del client " + socket.getInetAddress(), e);
        }
    }

    @Override
    public void run() {
        try {
            String linea;
            while (in != null && (linea = in.readLine()) != null) {
                if (stanza != null) {
                    stanza.riceviComando(this, linea);
                }
            }
        } catch (IOException e) {
            LOG.log(Level.FINE, "Connessione interrotta con " + getIndirizzo(), e);
        } finally {
            // Sia chiusura normale sia errore: il giocatore non c'è più
            connesso = false;
            visualizza();
            if (stanza != null) {
                stanza.gestisciDisconnessione(this);
            } else {
                server.rimuoviDaAttesa(this);
            }
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

    // Unico metodo della classe che stampa su console
    private void visualizza() {
        System.out.println("Partita online: " + p1.getIndirizzo() + " (Giocatore 1) contro "
                + p2.getIndirizzo() + " (Giocatore 2)");
    }

    public void avvia() {
        p1.inviaMessaggio("START|1|Sei il Giocatore 1");
        p2.inviaMessaggio("START|2|Sei il Giocatore 2");
        visualizza();
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