package scopa.shared;

import java.io.Serializable;

// Implementa Serializable per viaggiare nei Socket di rete
public class Carta implements Serializable {
    private static final long serialVersionUID = 1L; // Buona pratica per la serializzazione

    private final int valore;
    private final Seme seme;

    public Carta(int valore, Seme seme) {
        this.valore = valore;
        this.seme = seme;
    }

    public int getValore() { return valore; }
    public Seme getSeme() { return seme; }

    // Genera stringhe come "bastoni_01.png"
    public String getNomeImmagine() {
        return seme.getNomeFile() + "_" + String.format("%02d", valore) + ".png";
    }

    @Override
    public String toString() {
        return valore + " di " + seme;
    }
}