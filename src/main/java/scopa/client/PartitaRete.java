package scopa.client;

import scopa.shared.CalcolatorePunti;
import scopa.shared.Carta;
import scopa.shared.Giocatore;
import scopa.shared.Mazzo;
import scopa.shared.Protocollo;
import scopa.shared.RegoleScopa;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Partita online tra due giocatori.
 * <p>
 * Il Giocatore 1 (chi si collega per primo, di solito chi ospita) fa da mazziere:
 * sceglie le regole, mescola e manda all'altro l'ordine delle 40 carte.
 * Da lì entrambi i client distribuiscono le stesse carte nello stesso modo
 * e si scambiano solo le mosse, quindi i due tavoli restano identici.
 * <p>
 * Messaggi tra i client (inoltrati dal server come "MOSSA|..."):
 * <ul>
 *   <li>MAZZO|VELOCE o CLASSICA|id di chi inizia|NUOVA o CONTINUA|carte in ordine</li>
 *   <li>PLAY|carta giocata[|PRESE|carte prese]</li>
 *   <li>RIGIOCA|SI o NO</li>
 * </ul>
 */
public class PartitaRete implements ClientRete.ListenerRete {

    private static final Logger LOG = Logger.getLogger(PartitaRete.class.getName());

    private static final String REGOLA_VELOCE = "VELOCE";
    private static final String REGOLA_CLASSICA = "CLASSICA";

    private final TavoloVerdePanel vistaTavolo;
    private final String ipServer;
    private final int portaServer;
    private final Runnable tornaAlMenu;

    private final Giocatore umano = new Giocatore("Tu");
    private final Giocatore avversario = new Giocatore("Avversario");
    private final List<Carta> carteAterra = new ArrayList<>();

    // I messaggi arrivati mentre è in corso un'animazione o una finestra modale
    // aspettano qui e vengono applicati in ordine appena il gioco è libero
    private final Deque<String> codaMessaggi = new ArrayDeque<>();

    private ClientRete client;
    private Mazzo mazzo;
    private int mioIdGiocatore = 0; // 1 o 2 assegnato dal Server
    private boolean partitaVeloce;
    private boolean mioTurno = false;
    private boolean animazioneInCorso = false;
    private boolean dialogoAperto = false;
    private boolean partitaChiusa = false;
    private Giocatore ultimoAPrendere = null;
    private int idChiIniziaMano = 1;
    private boolean ioRigioco = false;
    private boolean avversarioRigioca = false;
    private JDialog dialogoAttesa;

    public PartitaRete(TavoloVerdePanel vistaTavolo, String ipServer, int portaServer, Runnable tornaAlMenu) {
        this.vistaTavolo = vistaTavolo;
        this.ipServer = ipServer;
        this.portaServer = portaServer;
        this.tornaAlMenu = tornaAlMenu;

        GestoreAudio.inizializza();

        this.vistaTavolo.setListener(cartaCliccata -> {
            if (occupato() || !mioTurno || partitaChiusa || !umano.getMano().contains(cartaCliccata)) return;

            List<List<Carta>> opzioniPossibili = RegoleScopa.trovaPresePossibili(cartaCliccata, carteAterra);
            List<Carta> presaScelta = new ArrayList<>();

            if (opzioniPossibili.size() == 1) {
                presaScelta = opzioniPossibili.get(0);
            } else if (opzioniPossibili.size() > 1) {
                presaScelta = conDialogo(() -> faiScegliereAlGiocatore(opzioniPossibili));
                if (presaScelta == null) return;
            }

            giocaMossa(umano, cartaCliccata, presaScelta);
        });
        aggiornaGrafica();
    }

    public void connetti() {
        try {
            this.client = new ClientRete(ipServer, portaServer, this);
            apriAttesa("Connessione al server " + ipServer + " riuscita...");
        } catch (IOException e) {
            LOG.log(Level.INFO, "Connessione fallita a " + ipServer + ":" + portaServer, e);
            partitaChiusa = true;
            FinestraDialogo.mostraMessaggio(vistaTavolo,
                    "Impossibile connettersi al server " + ipServer + ":" + portaServer
                            + "\n\nControlla l'indirizzo e che l'altro giocatore\nabbia scelto \"Ospita partita\".");
            tornaAlMenu.run();
        }
    }

    // ===================== MESSAGGI DI RETE =====================

    @Override
    public void messaggioRicevuto(String messaggio) {
        SwingUtilities.invokeLater(() -> {
            codaMessaggi.add(messaggio);
            elaboraCoda();
        });
    }

    @Override
    public void disconnesso() {
        messaggioRicevuto("DISCONNECT|Connessione persa con il server.");
    }

    private boolean occupato() {
        return animazioneInCorso || dialogoAperto;
    }

    private void elaboraCoda() {
        while (!partitaChiusa && !occupato() && !codaMessaggi.isEmpty()) {
            String messaggio = codaMessaggi.poll();
            try {
                gestisciMessaggio(messaggio);
            } catch (RuntimeException e) {
                LOG.log(Level.WARNING, "Messaggio di rete non valido: " + messaggio, e);
            }
        }
    }

    private void gestisciMessaggio(String messaggio) {
        String[] parti = messaggio.split("\\|", -1);
        switch (parti[0]) {
            case "WAIT":
                apriAttesa("In attesa di un altro giocatore...\n\nComunica il tuo indirizzo all'avversario.");
                break;

            case "START":
                mioIdGiocatore = Integer.parseInt(parti[1]);
                chiudiAttesa();
                if (mioIdGiocatore == 1) {
                    scegliRegoleEIniziaPartita();
                } else {
                    apriAttesa("Avversario trovato! Sei il Giocatore 2.\n\nL'avversario sta scegliendo le regole...");
                }
                break;

            case "MOSSA":
                gestisciMossaAvversario(Arrays.copyOfRange(parti, 1, parti.length));
                break;

            case "DISCONNECT":
                terminaPartita(parti.length > 1 ? parti[1] : "L'avversario si è disconnesso.");
                break;

            default:
                LOG.warning("Messaggio sconosciuto dal server: " + messaggio);
        }
    }

    private void gestisciMossaAvversario(String[] parti) {
        switch (parti[0]) {
            case "MAZZO": {
                partitaVeloce = REGOLA_VELOCE.equals(parti[1]);
                int idChiInizia = Integer.parseInt(parti[2]);
                boolean nuovaPartita = "NUOVA".equals(parti[3]);
                List<Carta> ordine = Protocollo.decodificaCarte(parti[4]);
                if (ordine.size() != 40) throw new IllegalArgumentException("Mazzo incompleto: " + ordine.size());
                chiudiAttesa();
                iniziaMano(idChiInizia, ordine, nuovaPartita);
                break;
            }
            case "PLAY": {
                Carta carta = Protocollo.decodificaCarta(parti[1]);
                List<Carta> prese = (parti.length > 3 && "PRESE".equals(parti[2]))
                        ? Protocollo.decodificaCarte(parti[3])
                        : new ArrayList<>();
                if (mioTurno || !avversario.getMano().contains(carta) || !new HashSet<>(carteAterra).containsAll(prese)) {
                    throw new IllegalStateException("Mossa dell'avversario non coerente con il tavolo");
                }
                giocaMossa(avversario, carta, prese);
                break;
            }
            case "RIGIOCA":
                avversarioRigioca = "SI".equals(parti[1]);
                if (avversarioRigioca) {
                    verificaRivincita();
                } else {
                    terminaPartita("L'avversario non vuole giocare un'altra partita.");
                }
                break;

            default:
                LOG.warning("Mossa sconosciuta: " + String.join("|", parti));
        }
    }

    // ===================== SVOLGIMENTO DELLA PARTITA =====================

    private void scegliRegoleEIniziaPartita() {
        String[] opzioniRegole = {"Partita Veloce (Mano Secca)", "Classica (A 11 Punti)"};
        int scelta = conDialogo(() -> FinestraDialogo.scegliOpzione(vistaTavolo,
                "Avversario trovato! Sei il Giocatore 1.\n\nScegli le regole della partita:",
                opzioniRegole));
        partitaVeloce = (scelta != 1);
        distribuisciComeMazziere(Math.random() < 0.5 ? 1 : 2, true);
    }

    // Solo il Giocatore 1: mescola, manda l'ordine del mazzo all'altro e inizia
    private void distribuisciComeMazziere(int idChiInizia, boolean nuovaPartita) {
        Mazzo nuovoMazzo = new Mazzo();
        nuovoMazzo.mescola();
        List<Carta> ordine = nuovoMazzo.getCarte();

        client.inviaComando("MAZZO|" + (partitaVeloce ? REGOLA_VELOCE : REGOLA_CLASSICA)
                + "|" + idChiInizia
                + "|" + (nuovaPartita ? "NUOVA" : "CONTINUA")
                + "|" + Protocollo.codificaCarte(ordine));

        iniziaMano(idChiInizia, ordine, nuovaPartita);
    }

    private void iniziaMano(int idChiInizia, List<Carta> ordine, boolean nuovaPartita) {
        if (nuovaPartita) {
            umano.resettaTutto();
            avversario.resettaTutto();
        } else {
            umano.preparaNuovaMano();
            avversario.preparaNuovaMano();
        }
        ioRigioco = false;
        avversarioRigioca = false;
        idChiIniziaMano = idChiInizia;
        ultimoAPrendere = null;

        mazzo = new Mazzo(ordine);
        carteAterra.clear();
        for (int i = 0; i < 4; i++) carteAterra.add(mazzo.pesca());
        distribuisciTreCarte();

        mioTurno = (idChiInizia == mioIdGiocatore);
        aggiornaGrafica();

        GestoreAudio.suonaShuffle(1500);
        String chiTocca = mioTurno ? "Tocca a TE!" : "Tocca all'AVVERSARIO!";
        conDialogo(() -> {
            FinestraDialogo.mostraMessaggioTemporizzato(vistaTavolo, "Carte Mescolate.\n\n" + chiTocca, 1500);
            return null;
        });
    }

    // Stesso ordine su entrambi i client: prima il Giocatore 1, poi il Giocatore 2
    private void distribuisciTreCarte() {
        Giocatore giocatore1 = (mioIdGiocatore == 1) ? umano : avversario;
        Giocatore giocatore2 = (mioIdGiocatore == 1) ? avversario : umano;
        for (int i = 0; i < 3; i++) {
            giocatore1.aggiungiCartaInMano(mazzo.pesca());
            giocatore2.aggiungiCartaInMano(mazzo.pesca());
        }
    }

    private List<Carta> faiScegliereAlGiocatore(List<List<Carta>> opzioni) {
        int scelta = FinestraDialogo.scegliPresa(vistaTavolo,
                "Hai più possibilità di presa.\nQuali carte vuoi raccogliere?",
                opzioni);

        if (scelta >= 0) return opzioni.get(scelta);
        return null;
    }

    private void giocaMossa(Giocatore giocatore, Carta cartaGiocata, List<Carta> prese) {
        animazioneInCorso = true;

        if (giocatore == umano) {
            mioTurno = false;
            StringBuilder comando = new StringBuilder("PLAY|").append(Protocollo.codificaCarta(cartaGiocata));
            if (!prese.isEmpty()) comando.append("|PRESE|").append(Protocollo.codificaCarte(prese));
            client.inviaComando(comando.toString());
        }

        // 1. LA CARTA VIENE APPOGGIATA E SUONA IL DROP
        giocatore.giocaCarta(cartaGiocata);
        carteAterra.add(cartaGiocata);
        vistaTavolo.impostaEvidenza(cartaGiocata, prese);
        aggiornaGrafica();
        GestoreAudio.suonaCartaGiocata();

        // 2. PAUSA VISIVA, POI LA PRESA
        Timer timerPausa = new Timer(prese.isEmpty() ? 500 : 600, e -> {
            vistaTavolo.impostaEvidenza(null, null);

            if (!prese.isEmpty()) {
                carteAterra.removeAll(prese);
                carteAterra.remove(cartaGiocata);
                giocatore.aggiungiPrese(prese);
                giocatore.aggiungiPrese(Collections.singletonList(cartaGiocata));
                ultimoAPrendere = giocatore;
                aggiornaGrafica();
                GestoreAudio.suonaPresa();

                boolean ultimaGiocata = mazzo.carteRimanenti() == 0
                        && umano.getMano().isEmpty() && avversario.getMano().isEmpty();
                if (carteAterra.isEmpty() && !ultimaGiocata) {
                    giocatore.aggiungiScopa(cartaGiocata);
                    String messaggioScopa = (giocatore == umano)
                            ? "🧹 Hai fatto SCOPA!"
                            : "🧹 L'avversario ha fatto SCOPA!";
                    conDialogo(() -> {
                        FinestraDialogo.mostraMessaggioTemporizzato(vistaTavolo, messaggioScopa, 1000);
                        return null;
                    });
                }
            } else {
                aggiornaGrafica();
            }

            animazioneInCorso = false;
            fineTurno(giocatore);
            SwingUtilities.invokeLater(this::elaboraCoda);
        });
        timerPausa.setRepeats(false);
        timerPausa.start();
    }

    private void fineTurno(Giocatore chiHaGiocato) {
        mioTurno = (chiHaGiocato != umano);

        if (umano.getMano().isEmpty() && avversario.getMano().isEmpty()) {
            if (mazzo.carteRimanenti() > 0) {
                distribuisciTreCarte();
                aggiornaGrafica();
            } else {
                fineMano();
            }
        }
    }

    private void fineMano() {
        mioTurno = false;

        if (!carteAterra.isEmpty() && ultimoAPrendere != null) {
            ultimoAPrendere.aggiungiPrese(new ArrayList<>(carteAterra));
            carteAterra.clear();
            aggiornaGrafica();
        }

        String resoconto = CalcolatorePunti.calcolaEGeneraResoconto(
                umano, avversario, partitaVeloce, "Avversario", "all'Avversario");

        boolean traguardo = partitaVeloce || umano.getPuntiGlobali() >= 11 || avversario.getPuntiGlobali() >= 11;
        boolean pareggio = umano.getPuntiGlobali() == avversario.getPuntiGlobali();
        boolean partitaFinita = traguardo && !pareggio;

        if (partitaFinita) {
            String vincitore = umano.getPuntiGlobali() > avversario.getPuntiGlobali()
                    ? "HAI VINTO LA PARTITA!"
                    : "L'AVVERSARIO HA VINTO LA PARTITA!";
            resoconto += "\n\n🏆 " + vincitore + " 🏆\nVuoi giocare un'altra partita?";
        } else if (traguardo) {
            resoconto += "\n\nPAREGGIO ASSOLUTO!\nSi va a oltranza per lo scarto finale!";
        }

        String testoResoconto = resoconto;
        boolean continua = conDialogo(() -> FinestraDialogo.mostraResocontoVisuale(
                vistaTavolo, testoResoconto, umano, avversario, partitaFinita));

        if (partitaChiusa) return;

        if (partitaFinita) {
            if (!continua) {
                client.inviaComando("RIGIOCA|NO");
                client.chiudi();
                System.exit(0);
            }
            ioRigioco = true;
            client.inviaComando("RIGIOCA|SI");
            apriAttesa("In attesa che l'avversario decida\nse giocare un'altra partita...");
            verificaRivincita();
        } else if (mioIdGiocatore == 1) {
            // Nella mano successiva inizia l'altro giocatore
            distribuisciComeMazziere(idChiIniziaMano == 1 ? 2 : 1, false);
        }
        // Il Giocatore 2 aspetta il nuovo mazzo dal Giocatore 1 (arriva come MAZZO)
    }

    private void verificaRivincita() {
        if (!ioRigioco || !avversarioRigioca) return;
        if (mioIdGiocatore == 1) {
            chiudiAttesa();
            distribuisciComeMazziere(Math.random() < 0.5 ? 1 : 2, true);
        }
        // Il Giocatore 2 chiude l'attesa quando arriva il nuovo mazzo
    }

    private void terminaPartita(String messaggio) {
        if (partitaChiusa) return;
        partitaChiusa = true;
        codaMessaggi.clear();
        chiudiAttesa();
        if (client != null) client.chiudi();
        vistaTavolo.setListener(null);

        mazzo = null;
        carteAterra.clear();
        umano.resettaTutto();
        avversario.resettaTutto();
        vistaTavolo.impostaEvidenza(null, null);
        aggiornaGrafica();

        FinestraDialogo.mostraMessaggio(vistaTavolo, messaggio);
        tornaAlMenu.run();
    }

    // ===================== SUPPORTO =====================

    // Apre una finestra modale: intanto i messaggi di rete restano in coda
    private <T> T conDialogo(Supplier<T> finestra) {
        boolean eraAperto = dialogoAperto;
        dialogoAperto = true;
        try {
            return finestra.get();
        } finally {
            dialogoAperto = eraAperto;
            SwingUtilities.invokeLater(this::elaboraCoda);
        }
    }

    private void apriAttesa(String messaggio) {
        chiudiAttesa();
        dialogoAttesa = FinestraDialogo.apriMessaggioAttesa(vistaTavolo, messaggio);
    }

    private void chiudiAttesa() {
        if (dialogoAttesa != null) {
            dialogoAttesa.dispose();
            dialogoAttesa = null;
        }
    }

    private void aggiornaGrafica() {
        vistaTavolo.setCarteTavolo(carteAterra);
        vistaTavolo.setManoGiocatore(umano.getMano());
        vistaTavolo.setStatoMazzoEAvversario(avversario.getMano().size(), mazzo != null ? mazzo.carteRimanenti() : 0);
        vistaTavolo.setStatoPrese(umano.getPrese().size(), umano.getCarteScopa(), avversario.getPrese().size(), avversario.getCarteScopa());
    }
}
