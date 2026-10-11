package scopa.shared;

import java.util.ArrayList;
import java.util.List;

public class CalcolatorePunti {

    private static final int[] VALORI_PRIMIERA = { 0, 16, 12, 13, 14, 15, 18, 21, 10, 10, 10 };

    public static String calcolaEGeneraResoconto(Giocatore umano, Giocatore bot, boolean partitaVeloce) {
        StringBuilder report = new StringBuilder();
        int puntiManoUmano = umano.getScope();
        int puntiManoBot = bot.getScope();

        report.append("--- RIASSUNTO DELLA MANO ---\n\n");
        if (puntiManoUmano > 0 || puntiManoBot > 0) {
            report.append("Scope: Tu ").append(puntiManoUmano).append(" - ").append(puntiManoBot).append(" Computer\n");
        }

        if (umano.getPrese().size() > 20) {
            puntiManoUmano++;
            report.append("Carte (").append(umano.getPrese().size()).append("): +1 a Te\n");
        } else if (bot.getPrese().size() > 20) {
            puntiManoBot++;
            report.append("Carte (").append(bot.getPrese().size()).append("): +1 al Computer\n");
        } else {
            report.append("Carte: Patta (20 a 20)\n");
        }

        int denariUmano = contaDenari(umano.getPrese());
        int denariBot = contaDenari(bot.getPrese());
        if (denariUmano > 5) {
            puntiManoUmano++;
            report.append("Denari (").append(denariUmano).append("): +1 a Te\n");
        } else if (denariBot > 5) {
            puntiManoBot++;
            report.append("Denari (").append(denariBot).append("): +1 al Computer\n");
        } else {
            report.append("Denari: Patta\n");
        }

        if (haSettebello(umano.getPrese())) {
            puntiManoUmano++;
            report.append("Settebello: +1 a Te\n");
        } else {
            puntiManoBot++;
            report.append("Settebello: +1 al Computer\n");
        }

        int primieraUmano = calcolaValorePrimiera(umano.getPrese());
        int primieraBot = calcolaValorePrimiera(bot.getPrese());
        if (primieraUmano > primieraBot) {
            puntiManoUmano++;
            report.append("Primiera (").append(primieraUmano).append(" a ").append(primieraBot).append("): +1 a Te\n");
        } else if (primieraBot > primieraUmano) {
            puntiManoBot++;
            report.append("Primiera (").append(primieraBot).append(" a ").append(primieraUmano).append("): +1 al Computer\n");
        } else {
            report.append("Primiera: Patta\n");
        }

        umano.aggiungiPuntiGlobali(puntiManoUmano);
        bot.aggiungiPuntiGlobali(puntiManoBot);

        report.append("\n========================\n");
        if (partitaVeloce) {
            report.append("PUNTEGGIO FINALE (Mano Secca)\n");
        } else {
            report.append("PUNTEGGIO TOTALE (Vince chi arriva a 11)\n");
        }
        report.append("TU: ").append(umano.getPuntiGlobali()).append("\n");
        report.append("COMPUTER: ").append(bot.getPuntiGlobali()).append("\n");

        return report.toString();
    }

    private static int contaDenari(List<Carta> carte) {
        int count = 0;
        for (Carta c : carte) { if (c.getSeme() == Seme.DENARI) count++; }
        return count;
    }

    private static boolean haSettebello(List<Carta> carte) {
        for (Carta c : carte) { if (c.getValore() == 7 && c.getSeme() == Seme.DENARI) return true; }
        return false;
    }

    private static int calcolaValorePrimiera(List<Carta> carte) {
        int maxCoppe = 0, maxDenari = 0, maxBastoni = 0, maxSpade = 0;
        for (Carta c : carte) {
            int valorePrimiera = VALORI_PRIMIERA[c.getValore()];
            switch (c.getSeme()) {
                case COPPE: maxCoppe = Math.max(maxCoppe, valorePrimiera); break;
                case DENARI: maxDenari = Math.max(maxDenari, valorePrimiera); break;
                case BASTONI: maxBastoni = Math.max(maxBastoni, valorePrimiera); break;
                case SPADE: maxSpade = Math.max(maxSpade, valorePrimiera); break;
            }
        }
        if (maxCoppe == 0 || maxDenari == 0 || maxBastoni == 0 || maxSpade == 0) return 0;
        return maxCoppe + maxDenari + maxBastoni + maxSpade;
    }

    // --- NUOVI METODI: ESTRAGONO LE CARTE FISICHE PER LA GRAFICA ---

    public static List<Carta> getCartePrimiera(List<Carta> carte) {
        Carta maxCoppe = null, maxDenari = null, maxBastoni = null, maxSpade = null;
        int vC = 0, vD = 0, vB = 0, vS = 0;
        for (Carta c : carte) {
            int val = VALORI_PRIMIERA[c.getValore()];
            switch (c.getSeme()) {
                case COPPE: if(val > vC) { vC = val; maxCoppe = c; } break;
                case DENARI: if(val > vD) { vD = val; maxDenari = c; } break;
                case BASTONI: if(val > vB) { vB = val; maxBastoni = c; } break;
                case SPADE: if(val > vS) { vS = val; maxSpade = c; } break;
            }
        }
        List<Carta> prim = new ArrayList<>();
        if(maxCoppe != null) prim.add(maxCoppe);
        if(maxDenari != null) prim.add(maxDenari);
        if(maxBastoni != null) prim.add(maxBastoni);
        if(maxSpade != null) prim.add(maxSpade);
        return prim;
    }

    public static List<Carta> getCarteDenari(List<Carta> carte) {
        List<Carta> denari = new ArrayList<>();
        for (Carta c : carte) {
            if (c.getSeme() == Seme.DENARI) denari.add(c);
        }
        return denari;
    }

    public static List<Carta> getSettebelloList(List<Carta> carte) {
        List<Carta> list = new ArrayList<>();
        for (Carta c : carte) {
            if (c.getValore() == 7 && c.getSeme() == Seme.DENARI) {
                list.add(c);
                break;
            }
        }
        return list;
    }
}