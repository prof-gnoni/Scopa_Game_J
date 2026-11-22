package scopa.client;

import java.io.*;
import java.net.*;

public class ClientRete {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;
    private final ListenerRete listener;
    private volatile boolean inAscolto = true;

    public interface ListenerRete {
        void messaggioRicevuto(String messaggio);
        void disconnesso();
    }

    public ClientRete(String ip, int porta, ListenerRete listener) throws IOException {
        this.listener = listener;
        this.socket = new Socket(ip, porta);
        this.out = new PrintWriter(socket.getOutputStream(), true);
        this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // Thread dedicato all'ascolto dei messaggi dal Server
        Thread ascolto = new Thread(this::ascoltaServer, "ClientRete-ascolto");
        ascolto.setDaemon(true);
        ascolto.start();
    }

    private void ascoltaServer() {
        try {
            String linea;
            while (inAscolto && (linea = in.readLine()) != null) {
                if (listener != null) {
                    listener.messaggioRicevuto(linea);
                }
            }
        } catch (IOException e) {
            // Gestito sotto: la connessione è comunque persa
        }
        // Fine del flusso (server chiuso) o errore: avvisa, a meno che non abbiamo chiuso noi
        if (inAscolto && listener != null) listener.disconnesso();
    }

    public void inviaComando(String comando) {
        if (out != null) {
            out.println(comando);
        }
    }

    public void chiudi() {
        inAscolto = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {}
    }
}