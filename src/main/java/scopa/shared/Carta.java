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

    // Due carte sono uguali se hanno stesso valore e stesso seme
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Carta)) return false;
        Carta altra = (Carta) o;
        return valore == altra.valore && seme == altra.seme;
    }

    @Override
    public int hashCode() {
        return 31 * valore + (seme != null ? seme.hashCode() : 0);
    }

    @Override
    public String toString() {
        return valore + " di " + seme;
    }
}