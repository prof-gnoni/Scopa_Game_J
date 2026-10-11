package scopa.client;

import scopa.shared.Carta;
import scopa.shared.Giocatore;
import scopa.shared.Mazzo;
import scopa.shared.RegoleScopa;
import scopa.shared.IntelligenzaBot;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class PartitaLocale {
    private final TavoloVerdePanel vistaTavolo;
    private final Giocatore umano;
    private final Giocatore bot;

    private Mazzo mazzo;
    private final List<Carta> carteAterra;
    private boolean turnoUmano;

    private boolean animazioneInCorso = false;
    private Giocatore ultimoAPrendere = null;
    private final boolean partitaVeloce;
    private boolean umanoIniziaMatch;

    public PartitaLocale(TavoloVerdePanel vistaTavolo, boolean partitaVeloce) {
        this.vistaTavolo = vistaTavolo;
        this.partitaVeloce = partitaVeloce;
        this.umano = new Giocatore("Tu");
        this.bot = new Giocatore("Computer");
        this.mazzo = new Mazzo();
        this.carteAterra = new ArrayList<>();

        // Inizializza l'audio in memoria per avere zero latenza
        GestoreAudio.inizializza();

        this.umanoIniziaMatch = Math.random() < 0.5;

        this.vistaTavolo.setListener(cartaCliccata -> {
            if (!animazioneInCorso && turnoUmano && umano.getMano().contains(cartaCliccata)) {
                List<List<Carta>> opzioniPossibili = RegoleScopa.trovaPresePossibili(cartaCliccata, carteAterra);
                List<Carta> presaScelta = new ArrayList<>();

                if (opzioniPossibili.size() == 1) {
                    presaScelta = opzioniPossibili.get(0);
                } else if (opzioniPossibili.size() > 1) {
                    presaScelta = faiScegliereAlGiocatore(opzioniPossibili);
                    if (presaScelta == null) return;
                }

                giocaTurno(umano, cartaCliccata, presaScelta);
            }
        });
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

    public void inizia() {
        GestoreAudio.suonaShuffle(1500);

        mazzo.mescola();
        carteAterra.clear();
        for (int i = 0; i < 4; i++) carteAterra.add(mazzo.pesca());

        turnoUmano = umanoIniziaMatch;

        String messaggioInizio = umanoIniziaMatch ? "Tocca a TE!" : "Tocca al COMPUTER!";

        FinestraDialogo.mostraMessaggioTemporizzato(vistaTavolo, "Carte Mescolate.\n\n" + messaggioInizio, 1500);

        for (int i = 0; i < 3; i++) {
            umano.aggiungiCartaInMano(mazzo.pesca());
            bot.aggiungiCartaInMano(mazzo.pesca());
        }
        aggiornaGrafica();

        if (!turnoUmano) {
            faiPensareIlBot();
        }
    }

    private void giocaTurno(Giocatore giocatore, Carta cartaGiocata, List<Carta> prese) {
        animazioneInCorso = true;
        giocatore.giocaCarta(cartaGiocata);

        // 1. LA CARTA VIENE APPOGGIATA E SUONA IL DROP NELLO STESSO ISTANTE
        carteAterra.add(cartaGiocata);
        vistaTavolo.impostaEvidenza(cartaGiocata, prese);
        aggiornaGrafica();
        GestoreAudio.suonaCartaGiocata();

        // 2. PAUSA VISIVA PER MOSTRARE LA CARTA E LE CARTE SELEZIONATE
        int pausaPrese = prese.isEmpty() ? 500 : 600;

        Timer timerPausa = new Timer(pausaPrese, e -> {
            vistaTavolo.impostaEvidenza(null, null);

            if (!prese.isEmpty()) {
                // 3. NEL MOMENTO ESATTO IN CUI LE CARTE VENGONO RIMOSE DAL TAVOLO SUONA LA PRESA
                carteAterra.removeAll(prese);
                carteAterra.remove(cartaGiocata);
                giocatore.aggiungiPrese(prese);
                giocatore.aggiungiPrese(java.util.Collections.singletonList(cartaGiocata));
                ultimoAPrendere = giocatore;

                aggiornaGrafica();
                GestoreAudio.suonaPresa();

                if (carteAterra.isEmpty()) {
                    if (mazzo.carteRimanenti() == 0 && umano.getMano().isEmpty() && bot.getMano().isEmpty()) {
                        System.out.println("Ultima mano, niente scopa!");
                    } else {
                        giocatore.aggiungiScopa(cartaGiocata);

                        String messaggioScopa = (giocatore == umano)
                                ? "🧹 Hai fatto SCOPA!"
                                : "🧹 Il " + giocatore.getNome() + " ha fatto SCOPA!";

                        FinestraDialogo.mostraMessaggioTemporizzato(vistaTavolo, messaggioScopa, 1000);
                    }
                }
            } else {
                aggiornaGrafica();
            }

            animazioneInCorso = false;
            controllaFineMano();
        });
        timerPausa.setRepeats(false);
        timerPausa.start();
    }

    private void controllaFineMano() {
        turnoUmano = !turnoUmano;

        if (umano.getMano().isEmpty() && bot.getMano().isEmpty()) {
            distribuisciCarte();
            return;
        }

        if (!turnoUmano) {
            faiPensareIlBot();
        }
    }

    private void distribuisciCarte() {
        if (mazzo.carteRimanenti() == 0) {
            if (!carteAterra.isEmpty() && ultimoAPrendere != null) {
                ultimoAPrendere.aggiungiPrese(carteAterra);
                carteAterra.clear();
                aggiornaGrafica();
            }

            String resoconto = scopa.shared.CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, partitaVeloce);

            boolean partitaFinita = partitaVeloce || (umano.getPuntiGlobali() >= 11 || bot.getPuntiGlobali() >= 11);
            boolean pareggio = umano.getPuntiGlobali() == bot.getPuntiGlobali();

            if (partitaFinita && !pareggio) {
                String vincitore = umano.getPuntiGlobali() > bot.getPuntiGlobali() ? "HAI VINTO LA PARTITA!" : "IL COMPUTER HA VINTO LA PARTITA!";
                resoconto += "\n\n🏆 " + vincitore + " 🏆\nVuoi giocare un'altra partita?";
            } else {
                if (partitaFinita && pareggio) {
                    resoconto += "\n\nPAREGGIO ASSOLUTO!\nSi va a oltranza per lo scarto finale!";
                }
                partitaFinita = false;
            }

            boolean continua = FinestraDialogo.mostraResocontoVisuale(vistaTavolo, resoconto, umano, bot, partitaFinita && !pareggio);

            if (partitaFinita && !pareggio) {
                if (continua) {
                    umano.resettaTutto();
                    bot.resettaTutto();
                    umanoIniziaMatch = Math.random() < 0.5;
                    nuovaMano();
                } else {
                    System.exit(0);
                }
            } else {
                umanoIniziaMatch = !umanoIniziaMatch;
                nuovaMano();
            }
            return;
        }

        for (int i = 0; i < 3; i++) {
            umano.aggiungiCartaInMano(mazzo.pesca());
            bot.aggiungiCartaInMano(mazzo.pesca());
        }
        aggiornaGrafica();

        if (!turnoUmano) {
            faiPensareIlBot();
        }
    }

    private void nuovaMano() {
        umano.preparaNuovaMano();
        bot.preparaNuovaMano();
        carteAterra.clear();
        ultimoAPrendere = null;
        animazioneInCorso = false;

        this.mazzo = new Mazzo();
        inizia();
    }

    private void faiPensareIlBot() {
        Timer timerPensiero = new Timer(800, e -> {
            IntelligenzaBot.Mossa mossaMigliore = IntelligenzaBot.scegliMossaMigliore(bot, umano, carteAterra, mazzo.carteRimanenti());
            giocaTurno(bot, mossaMigliore.cartaGiocata, mossaMigliore.presa);
        });
        timerPensiero.setRepeats(false);
        timerPensiero.start();
    }

    private void aggiornaGrafica() {
        vistaTavolo.setCarteTavolo(carteAterra);
        vistaTavolo.setManoGiocatore(umano.getMano());
        vistaTavolo.setStatoMazzoEAvversario(bot.getMano().size(), mazzo.carteRimanenti());
        vistaTavolo.setStatoPrese(umano.getPrese().size(), umano.getCarteScopa(), bot.getPrese().size(), bot.getCarteScopa());
    }
}