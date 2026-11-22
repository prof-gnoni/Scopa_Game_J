package scopa.shared;

import java.util.ArrayList;
import java.util.List;

/**
 * Codifica delle carte nei messaggi di rete.
 * Una carta è "SEME,VALORE" (es. "DENARI,7"); una lista è separata da ";".
 */
public final class Protocollo {

    private Protocollo() {
    }

    public static String codificaCarta(Carta c) {
        return c.getSeme().name() + "," + c.getValore();
    }

    public static Carta decodificaCarta(String testo) {
        String[] parti = testo.trim().split(",");
        if (parti.length != 2) throw new IllegalArgumentException("Carta non valida: " + testo);
        int valore = Integer.parseInt(parti[1].trim());
        if (valore < 1 || valore > 10) throw new IllegalArgumentException("Valore non valido: " + testo);
        return new Carta(valore, Seme.valueOf(parti[0].trim()));
    }

    public static String codificaCarte(List<Carta> carte) {
        StringBuilder sb = new StringBuilder();
        for (Carta c : carte) {
            if (sb.length() > 0) sb.append(";");
            sb.append(codificaCarta(c));
        }
        return sb.toString();
    }

    public static List<Carta> decodificaCarte(String testo) {
        List<Carta> carte = new ArrayList<>();
        for (String parte : testo.split(";")) {
            if (!parte.trim().isEmpty()) carte.add(decodificaCarta(parte));
        }
        return carte;
    }
}
