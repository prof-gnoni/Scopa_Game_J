package scopa.client;

import scopa.shared.Carta;
import scopa.shared.Giocatore;
import scopa.shared.Mazzo;
import scopa.shared.RegoleScopa;

import javax.swing.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PartitaRete implements ClientRete.ListenerRete {

    private final TavoloVerdePanel vistaTavolo;
    private final Giocatore umano;
    private final Giocatore avversario;

    private ClientRete client;
    private final String ipServer;
    private final int portaServer;

    private int mioIdGiocatore = 0; // 1 o 2 assegnato dal Server
    private boolean mioTurno = false;
    private boolean animazioneInCorso = false;

    private final List<Carta> carteAterra = new ArrayList<>();
    private Mazzo mazzo;

    public PartitaRete(TavoloVerdePanel vistaTavolo, String ipServer, int portaServer) {
        this.vistaTavolo = vistaTavolo;
        this.ipServer = ipServer;
        this.portaServer = portaServer;
        this.umano = new Giocatore("Tu");
        this.avversario = new Giocatore("Avversario");

        GestoreAudio.inizializza();

        this.vistaTavolo.setListener(cartaCliccata -> {
            if (!animazioneInCorso && mioTurno && umano.getMano().contains(cartaCliccata)) {
                List<List<Carta>> opzioniPossibili = RegoleScopa.trovaPresePossibili(cartaCliccata, carteAterra);
                List<Carta> presaScelta = new ArrayList<>();

                if (opzioniPossibili.size() == 1) {
                    presaScelta = opzioniPossibili.get(0);
                } else if (opzioniPossibili.size() > 1) {
                    presaScelta = faiScegliereAlGiocatore(opzioniPossibili);
                    if (presaScelta == null) return;
                }

                eseguiTurnoMio(cartaCliccata, presaScelta);
            }
        });
    }

    public void connetti() {
        try {
            this.client = new ClientRete(ipServer, portaServer, this);
        } catch (IOException e) {
            FinestraDialogo.mostraMessaggio(vistaTavolo, "Impossibile connettersi al server " + ipServer + ":" + portaServer);
        }
    }

    private List<Carta> faiScegliereAlGiocatore(List<List<Carta>> opzioni) {
        String[] opzioniTesto = new String[opzioni.size()];
        for (int i = 0; i < opzioni.size(); i++) {
            List<Carta> opzione = opzioni.get(i);
            StringBuilder sb = new StringBuilder();
            for (Carta c : opzione) {
                sb.append(c.getValore()).append(" di ").append(c.getSeme()).append(", ");
            }
            opzioniTesto[i] = sb.substring(0, sb.length() - 2);
        }

        int scelta = FinestraDialogo.scegliOpzione(vistaTavolo,
                "Hai più possibilità di presa.\nQuali carte vuoi raccogliere?",
                opzioniTesto);

        if (scelta >= 0) return opzioni.get(scelta);
        return null;
    }

    private void eseguiTurnoMio(Carta cartaGiocata, List<Carta> prese) {
        mioTurno = false;
        animazioneInCorso = true;

        umano.giocaCarta(cartaGiocata);
        carteAterra.add(cartaGiocata);
        vistaTavolo.impostaEvidenza(cartaGiocata, prese);
        aggiornaGrafica();
        GestoreAudio.suonaCartaGiocata();

        // Invia la mossa all'avversario tramite il Server
        StringBuilder comando = new StringBuilder("PLAY|")
                .append(cartaGiocata.getSeme()).append(",").append(cartaGiocata.getValore());

        if (!prese.isEmpty()) {
            comando.append("|PRESE|");
            for (Carta p : prese) {
                comando.append(p.getSeme()).append(",").append(p.getValore()).append(";");
            }
        }
        client.inviaComando(comando.toString());

        Timer timerPausa = new Timer(prese.isEmpty() ? 500 : 600, e -> {
            vistaTavolo.impostaEvidenza(null, null);
            if (!prese.isEmpty()) {
                carteAterra.removeAll(prese);
                carteAterra.remove(cartaGiocata);
                umano.aggiungiPrese(prese);
                umano.aggiungiPrese(java.util.Collections.singletonList(cartaGiocata));
                aggiornaGrafica();
                GestoreAudio.suonaPresa();
            } else {
                aggiornaGrafica();
            }
            animazioneInCorso = false;
        });
        timerPausa.setRepeats(false);
        timerPausa.start();
    }

    @Override
    public void messaggioRicevuto(String messaggio) {
        SwingUtilities.invokeLater(() -> {
            String[] parti = messaggio.split("\\|");
            switch (parti[0]) {
                case "WAIT":
                    FinestraDialogo.mostraMessaggioTemporizzato(vistaTavolo, parti[1], 2000);
                    break;

                case "START":
                    this.mioIdGiocatore = Integer.parseInt(parti[1]);
                    this.mioTurno = (mioIdGiocatore == 1);
                    FinestraDialogo.mostraMessaggioTemporizzato(vistaTavolo,
                            "Partita Online Avviata!\nSei il Giocatore " + mioIdGiocatore, 1500);
                    GestoreAudio.suonaShuffle(1500);
                    break;

                case "DISCONNECT":
                    FinestraDialogo.mostraMessaggio(vistaTavolo, parti[1]);
                    break;
            }
        });
    }

    @Override
    public void disconnesso() {
        SwingUtilities.invokeLater(() ->
                FinestraDialogo.mostraMessaggio(vistaTavolo, "Connessione persa con il Server.")
        );
    }

    private void aggiornaGrafica() {
        vistaTavolo.setCarteTavolo(carteAterra);
        vistaTavolo.setManoGiocatore(umano.getMano());
        vistaTavolo.setStatoMazzoEAvversario(avversario.getMano().size(), mazzo != null ? mazzo.carteRimanenti() : 0);
        vistaTavolo.setStatoPrese(umano.getPrese().size(), umano.getCarteScopa(), avversario.getPrese().size(), avversario.getCarteScopa());
    }
}