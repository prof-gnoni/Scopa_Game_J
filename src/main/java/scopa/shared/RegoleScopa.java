package scopa.shared;

import java.util.ArrayList;
import java.util.List;

public class RegoleScopa {

    // Ora restituisce una lista di opzioni (ogni opzione è a sua volta una lista di carte)
    public static List<List<Carta>> trovaPresePossibili(Carta giocata, List<Carta> tavolo) {
        List<List<Carta>> opzioni = new ArrayList<>();

        // 1. PRECEDENZA ASSOLUTA: Ci sono carte dello stesso valore a terra?
        for (Carta c : tavolo) {
            if (c.getValore() == giocata.getValore()) {
                List<Carta> presaSingola = new ArrayList<>();
                presaSingola.add(c);
                opzioni.add(presaSingola);
            }
        }

        // Se c'è almeno una carta di valore uguale, DEVI prendere quella (non si calcolano somme)
        if (!opzioni.isEmpty()) {
            return opzioni;
        }

        // 2. SOMME: Troviamo tutte le combinazioni possibili
        trovaTutteLeSomme(tavolo, giocata.getValore(), new ArrayList<>(), 0, opzioni);
        return opzioni;
    }

    private static void trovaTutteLeSomme(List<Carta> tavolo, int target, List<Carta> parziale, int index, List<List<Carta>> risultati) {
        int sommaAttuale = 0;
        for (Carta c : parziale) {
            sommaAttuale += c.getValore();
        }

        if (sommaAttuale == target) {
            risultati.add(new ArrayList<>(parziale));
            return;
        }

        if (sommaAttuale > target || index >= tavolo.size()) {
            return;
        }

        // Prova a includere la carta corrente
        parziale.add(tavolo.get(index));
        trovaTutteLeSomme(tavolo, target, parziale, index + 1, risultati);

        // Prova a saltare la carta corrente (Backtracking)
        parziale.remove(parziale.size() - 1);
        trovaTutteLeSomme(tavolo, target, parziale, index + 1, risultati);
    }
}