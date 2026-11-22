package scopa.client;

import scopa.server.ServerScopa;

import javax.swing.*;
import java.io.IOException;
import java.net.BindException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FinestraPrincipale extends JFrame {

    private static final Logger LOG = Logger.getLogger(FinestraPrincipale.class.getName());

    private final TavoloVerdePanel tavolo;

    // Server avviato da questa finestra quando si sceglie "Ospita partita"
    private ServerScopa serverLocale;

    public FinestraPrincipale() {
        setTitle("La Scopa - Online & Singleplayer");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Alla chiusura del gioco (finestra o System.exit) si spegne anche il server ospitato
        Runtime.getRuntime().addShutdownHook(new Thread(this::fermaServerLocale, "ChiusuraServerLocale"));

        // --- MENU AUDIO E CONTROLLO VOLUME ---
        JMenuBar menuBar = new JMenuBar();
        JMenu menuAudio = new JMenu("Impostazioni Audio");

        // Spunta On/Off Musica
        JCheckBoxMenuItem cbMusica = new JCheckBoxMenuItem("Musica di Sottofondo", true);
        cbMusica.addActionListener(e -> GestoreAudio.impostaMusicaAttiva(cbMusica.isSelected()));

        // Slider Volume
        JMenu menuVolume = new JMenu("Volume Musica");
        JSlider sliderVolume = creaSliderVolume(60);
        sliderVolume.addChangeListener(e -> {
            float vol = sliderVolume.getValue() / 100f;
            GestoreAudio.impostaVolume(vol);
        });
        menuVolume.add(sliderVolume);

        menuAudio.add(cbMusica);
        menuAudio.add(menuVolume);

        // Spunta On/Off Effetti Sonori
        JCheckBoxMenuItem cbEffetti = new JCheckBoxMenuItem("Effetti Sonori", true);
        cbEffetti.addActionListener(e -> GestoreAudio.impostaEffettiAttivi(cbEffetti.isSelected()));

        // Slider Volume Effetti
        JMenu menuVolumeEffetti = new JMenu("Volume Effetti");
        JSlider sliderVolumeEffetti = creaSliderVolume(100);
        sliderVolumeEffetti.addChangeListener(e ->
                GestoreAudio.impostaVolumeEffetti(sliderVolumeEffetti.getValue() / 100f));
        menuVolumeEffetti.add(sliderVolumeEffetti);

        menuAudio.addSeparator();
        menuAudio.add(cbEffetti);
        menuAudio.add(menuVolumeEffetti);
        menuBar.add(menuAudio);
        setJMenuBar(menuBar);

        // Avvia la musica di sottofondo
        GestoreAudio.avviaMusicaSottofondo();

        tavolo = new TavoloVerdePanel();
        add(tavolo);

        // Avvia il flusso principale delle scelte
        avviaFlussoGioco();
    }

    private void avviaFlussoGioco() {
        // --- MENU DI SELEZIONE MODALITÀ DI GIOCO ---
        String[] modalitaIniziale = {"Giocatore Singolo (vs Computer)", "Multiplayer Online (vs Umano)"};

        int sceltaModalita = FinestraDialogo.scegliOpzione(this,
                "BENVENUTO A LA SCOPA!\n\nCome vuoi giocare?",
                modalitaIniziale);

        if (sceltaModalita == 1) {
            // === MODALITÀ MULTIPLAYER ONLINE ===
            String[] opzioniOnline = {"Ospita partita (avvia il server qui)", "Unisciti a una partita (inserisci l'IP)"};

            int sceltaOnline = FinestraDialogo.scegliOpzione(this,
                    "MULTIPLAYER ONLINE\n\nVuoi ospitare la partita o unirti a quella di un amico?",
                    opzioniOnline);

            if (sceltaOnline == 0) {
                ospitaPartita();
            } else if (sceltaOnline == 1) {
                uniscitiAPartita();
            } else {
                avviaFlussoGioco();
            }

        } else if (sceltaModalita == 0) {
            // === MODALITÀ LOCALE (CONTRO BOT) ===
            String[] opzioniRegole = {"Partita Veloce (Mano Secca)", "Classica (A 11 Punti)"};

            int sceltaRegole = FinestraDialogo.scegliOpzione(this,
                    "Scegli le regole della partita contro il Computer:",
                    opzioniRegole);

            // Se l'utente preme Annulla
            if (sceltaRegole == -1) {
                avviaFlussoGioco();
                return;
            }

            boolean partitaVeloce = (sceltaRegole == 0);

            PartitaLocale partita = new PartitaLocale(tavolo, partitaVeloce);
            partita.inizia();
        } else {
            // Se chiude la finestra senza selezionare nulla
            System.exit(0);
        }
    }

    private void ospitaPartita() {
        if (serverLocale == null || !serverLocale.isAttivo()) {
            ServerScopa server = new ServerScopa(ServerScopa.PORTA_PREDEFINITA);
            try {
                server.avvia();
                serverLocale = server;
            } catch (BindException e) {
                int scelta = FinestraDialogo.scegliOpzione(this,
                        "La porta " + ServerScopa.PORTA_PREDEFINITA + " è già in uso su questo computer.\n"
                                + "Forse un server Scopa è già acceso\n(o un'altra finestra del gioco sta ospitando).",
                        new String[]{"Collegati al server già acceso", "Torna al menu"});
                if (scelta == 0) {
                    collegatiAlServer("localhost");
                } else {
                    avviaFlussoGioco();
                }
                return;
            } catch (IOException e) {
                LOG.log(Level.WARNING, "Impossibile avviare il server locale", e);
                FinestraDialogo.mostraMessaggio(this, "Impossibile avviare il server:\n" + e.getMessage());
                avviaFlussoGioco();
                return;
            }
        }

        List<String> indirizzi = ServerScopa.indirizziLocali();
        String testoIndirizzi = indirizzi.isEmpty()
                ? "Nessuna rete trovata: l'avversario può collegarsi\nsolo da questo computer (localhost)."
                : "Comunica al tuo avversario questo indirizzo:\n" + String.join("\n", indirizzi);

        FinestraDialogo.mostraMessaggio(this,
                "SERVER AVVIATO\n\n" + testoIndirizzi
                        + "\n\nPorta: " + serverLocale.getPorta()
                        + "\nIl server si spegne quando chiudi il gioco.");

        collegatiAlServer("localhost");
    }

    private void uniscitiAPartita() {
        String ipServer = JOptionPane.showInputDialog(this,
                "Inserisci l'IP o l'indirizzo del Server:",
                "Connessione Multiplayer",
                JOptionPane.QUESTION_MESSAGE);

        // Se l'utente clicca Annulla o chiude la finestra della richiesta IP
        if (ipServer == null) {
            avviaFlussoGioco(); // Torna indietro al menu principale
            return;
        }

        if (ipServer.trim().isEmpty()) {
            ipServer = "localhost"; // Default per test locali se lascia vuoto
        }

        collegatiAlServer(ipServer.trim());
    }

    private void collegatiAlServer(String ipServer) {
        // A fine partita online (o se la connessione cade) si torna al menu principale
        PartitaRete partitaOnline = new PartitaRete(tavolo, ipServer, ServerScopa.PORTA_PREDEFINITA,
                () -> SwingUtilities.invokeLater(this::avviaFlussoGioco));
        partitaOnline.connetti();
    }

    private void fermaServerLocale() {
        if (serverLocale != null) serverLocale.ferma();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FinestraPrincipale frame = new FinestraPrincipale();
            frame.setVisible(true);
        });
    }

    private static JSlider creaSliderVolume(int valoreIniziale) {
        JSlider slider = new JSlider(0, 100, valoreIniziale);
        slider.setMajorTickSpacing(25);
        slider.setMinorTickSpacing(5);
        slider.setPaintTicks(true);
        slider.setPaintLabels(true);
        return slider;
    }
}