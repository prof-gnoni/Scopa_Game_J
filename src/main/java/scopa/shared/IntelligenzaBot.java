package scopa.shared;

import java.util.ArrayList;
import java.util.List;

public class IntelligenzaBot {

    // Una classe interna per impacchettare una mossa e il suo punteggio
    public static class Mossa {
        public Carta cartaGiocata;
        public List<Carta> presa;
        public int punteggioValutato;

        public Mossa(Carta c, List<Carta> p) {
            this.cartaGiocata = c;
            this.presa = p;
            this.punteggioValutato = 0;
        }
    }

    public static Mossa scegliMossaMigliore(Giocatore bot, Giocatore umano, List<Carta> tavolo, int carteRimanentiMazzo) {
        // Il bot "conta le carte" per dedurre cosa hai in mano (fondamentale nell'ultima mano)
        List<Carta> carteAvversarioDedotte = deduciCarteAvversario(bot, umano, tavolo, carteRimanentiMazzo);

        // Trova tutte le mosse legali possibili
        List<Mossa> mossePossibili = generaTutteLeMosse(bot.getMano(), tavolo);

        Mossa mossaMigliore = null;
        int punteggioMassimo = Integer.MIN_VALUE;

        // Valuta ogni mossa come farebbe un professionista
        for (Mossa mossa : mossePossibili) {
            valutaMossa(mossa, tavolo, bot, carteAvversarioDedotte, carteRimanentiMazzo);

            if (mossa.punteggioValutato > punteggioMassimo) {
                punteggioMassimo = mossa.punteggioValutato;
                mossaMigliore = mossa;
            }
        }

        return mossaMigliore;
    }

    private static List<Mossa> generaTutteLeMosse(List<Carta> mano, List<Carta> tavolo) {
        List<Mossa> mosse = new ArrayList<>();
        for (Carta c : mano) {
            List<List<Carta>> presePossibili = RegoleScopa.trovaPresePossibili(c, tavolo);
            if (presePossibili.isEmpty()) {
                mosse.add(new Mossa(c, new ArrayList<>())); // Mossa a vuoto (butta a terra)
            } else {
                for (List<Carta> presa : presePossibili) {
                    mosse.add(new Mossa(c, presa));
                }
            }
        }
        return mosse;
    }

    private static void valutaMossa(Mossa mossa, List<Carta> tavolo, Giocatore bot, List<Carta> carteAvversario, int carteMazzo) {
        int score = 0;
        boolean isScopa = (mossa.presa.size() == tavolo.size()) && !tavolo.isEmpty();
        boolean ultimaMano = (carteMazzo == 0 && bot.getMano().size() <= 3);

        if (!mossa.presa.isEmpty()) {
            // --- VALUTAZIONE DI UNA PRESA ---

            // La Scopa vale tantissimo, a meno che non sia l'ultima giocata assoluta (che per regola non vale)
            if (isScopa && !(ultimaMano && bot.getMano().size() == 1)) {
                score += 1000;
            }

            // Pesi delle carte catturate
            for (Carta c : mossa.presa) {
                if (c.getValore() == 7 && c.getSeme() == Seme.DENARI) score += 500; // Settebello
                else if (c.getValore() == 7) score += 50; // Primiera forte
                else if (c.getSeme() == Seme.DENARI) score += 30; // Denari
                else if (c.getValore() == 6) score += 20; // Primiera media
            }

            // Pesi della carta giocata (usare un 7 per prendere è un piccolo sacrificio)
            if (mossa.cartaGiocata.getValore() == 7 && mossa.cartaGiocata.getSeme() == Seme.DENARI) score += 500;
            else if (mossa.cartaGiocata.getValore() == 7) score += 50;
            else if (mossa.cartaGiocata.getSeme() == Seme.DENARI) score += 30;

            score += (mossa.presa.size() * 10); // Preferisce prendere più carte fisiche (per il punto Carte)

            // Controlliamo cosa resta sul tavolo DOPO la presa
            int sommaRimanente = 0;
            for (Carta c : tavolo) {
                if (!mossa.presa.contains(c)) sommaRimanente += c.getValore();
            }
            // Lasciare meno di 10 sul tavolo è un assist per la Scopa avversaria
            if (sommaRimanente > 0 && sommaRimanente <= 10) score -= 150;
            if (sommaRimanente == 0 && !isScopa) score -= 150; // Lasciare vuoto è letale

        } else {
            // --- VALUTAZIONE DI UNO SCARTO A TERRA (Nessuna presa) ---
            Carta c = mossa.cartaGiocata;

            // MAI buttare carichi a terra se si può evitare
            if (c.getValore() == 7 && c.getSeme() == Seme.DENARI) score -= 2000; // Tragedia
            else if (c.getValore() == 7) score -= 300;
            else if (c.getSeme() == Seme.DENARI) score -= 150;
            else if (c.getValore() == 6) score -= 100;
            else score += 10; // Scartine o Figure lisce vanno bene

            // Controlliamo cosa succede al tavolo aggiungendo questa carta
            int sommaFutura = 0;
            for (Carta tc : tavolo) sommaFutura += tc.getValore();
            sommaFutura += c.getValore();

            // Evitare accuratamente di preparare tavoli facili da scopare
            if (sommaFutura <= 10) score -= 150;
            else score += 50; // Un tavolo "pesante" (>10) è sicuro
        }

        // --- ENDGAME: IL CONTACARTE ---
        // Se siamo all'ultima mano, il bot sa esattamente cosa ha in mano l'avversario
        if (carteMazzo == 0 && !carteAvversario.isEmpty()) {
            if (mossa.presa.isEmpty()) { // Se stiamo buttando a terra
                for (Carta avversario : carteAvversario) {
                    if (avversario.getValore() == mossa.cartaGiocata.getValore()) {
                        score -= 500; // Penalità enorme: stiamo regalando una presa certa!
                    }
                }
            }
        }

        mossa.punteggioValutato = score;
    }

    private static List<Carta> deduciCarteAvversario(Giocatore bot, Giocatore umano, List<Carta> tavolo, int carteMazzo) {
        List<Carta> nonViste = new ArrayList<>();
        // Il contacarte preciso serve davvero solo quando il mazzo è finito
        if (carteMazzo > 0) return nonViste;

        // Crea un mazzo completo "mentale"
        Mazzo temp = new Mazzo();
        List<Carta> tutte = new ArrayList<>();
        while (temp.carteRimanenti() > 0) tutte.add(temp.pesca());

        // Rimuove tutte le carte che sono già uscite allo scoperto
        for (Carta c : bot.getMano()) rimuoviDaListaMentale(tutte, c);
        for (Carta c : bot.getPrese()) rimuoviDaListaMentale(tutte, c);
        for (Carta c : umano.getPrese()) rimuoviDaListaMentale(tutte, c);
        for (Carta c : tavolo) rimuoviDaListaMentale(tutte, c);

        // Quelle che rimangono per esclusione sono nella mano dell'avversario!
        return tutte;
    }

    private static void rimuoviDaListaMentale(List<Carta> lista, Carta daRimuovere) {
        lista.removeIf(c -> c.getValore() == daRimuovere.getValore() && c.getSeme() == daRimuovere.getSeme());
    }
}