package scopa.shared;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Mazzo {
    private final List<Carta> carte;

    public Mazzo() {
        carte = new ArrayList<>();
        // Un doppio ciclo crea tutte le 40 carte in un colpo solo
        for (Seme seme : Seme.values()) {
            for (int valore = 1; valore <= 10; valore++) {
                carte.add(new Carta(valore, seme));
            }
        }
    }

    // Mazzo con un ordine già deciso (es. ricevuto dall'altro giocatore in rete)
    public Mazzo(List<Carta> carteInOrdine) {
        carte = new ArrayList<>(carteInOrdine);
    }

    // Copia dell'ordine attuale delle carte (l'ultima è la prossima pescata)
    public List<Carta> getCarte() {
        return new ArrayList<>(carte);
    }

    // Mescola il mazzo
    public void mescola() {
        Collections.shuffle(carte);
    }

    // Pesca la prima carta disponibile
    public Carta pesca() {
        if (carte.isEmpty()) {
            return null; // Il mazzo è finito
        }
        return carte.remove(carte.size() - 1);
    }

    // Utile per sapere quante carte mancano alla fine
    public int carteRimanenti() {
        return carte.size();
    }
}