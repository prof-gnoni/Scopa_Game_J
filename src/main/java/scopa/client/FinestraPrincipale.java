package scopa.client;

import javax.swing.*;

public class FinestraPrincipale extends JFrame {

    private final TavoloVerdePanel tavolo;

    public FinestraPrincipale() {
        setTitle("La Scopa - Online & Singleplayer");
        setSize(1024, 768);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // --- MENU AUDIO E CONTROLLO VOLUME ---
        JMenuBar menuBar = new JMenuBar();
        JMenu menuAudio = new JMenu("Impostazioni Audio");

        // Spunta On/Off Musica
        JCheckBoxMenuItem cbMusica = new JCheckBoxMenuItem("Musica di Sottofondo", true);
        cbMusica.addActionListener(e -> GestoreAudio.impostaMusicaAttiva(cbMusica.isSelected()));

        // Slider Volume
        JMenu menuVolume = new JMenu("Volume Musica");
        JSlider sliderVolume = new JSlider(0, 100, 70);
        sliderVolume.addChangeListener(e -> {
            float vol = sliderVolume.getValue() / 100f;
            GestoreAudio.impostaVolume(vol);
        });
        menuVolume.add(sliderVolume);

        menuAudio.add(cbMusica);
        menuAudio.add(menuVolume);
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

            // Istanzia e connette la partita via rete
            PartitaRete partitaOnline = new PartitaRete(tavolo, ipServer, 5555);
            partitaOnline.connetti();

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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FinestraPrincipale frame = new FinestraPrincipale();
            frame.setVisible(true);
        });
    }
}