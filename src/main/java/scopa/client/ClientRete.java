package scopa.client;

import java.io.*;
import java.net.*;

public class ClientRete {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private ListenerRete listener;
    private boolean inAscolto = true;

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
        new Thread(this::ascoltaServer).start();
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
            if (listener != null) listener.disconnesso();
        }
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