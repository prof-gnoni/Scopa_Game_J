package scopa.shared;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CalcolatorePuntiTest {

    private static List<Carta> tutteLeCarte() {
        List<Carta> carte = new ArrayList<>();
        for (Seme seme : Seme.values()) {
            for (int valore = 1; valore <= 10; valore++) {
                carte.add(new Carta(valore, seme));
            }
        }
        return carte;
    }

    private static List<Carta> carteDeiSemi(Seme... semi) {
        List<Carta> carte = new ArrayList<>();
        for (Seme seme : semi) {
            for (int valore = 1; valore <= 10; valore++) {
                carte.add(new Carta(valore, seme));
            }
        }
        return carte;
    }

    @Test
    void chiPrendeTuttoFaQuattroPuntiPiuLeScope() {
        Giocatore umano = new Giocatore("Tu");
        Giocatore bot = new Giocatore("Computer");
        umano.aggiungiPrese(tutteLeCarte());
        umano.aggiungiScopa(new Carta(3, Seme.COPPE));
        umano.aggiungiScopa(new Carta(5, Seme.SPADE));

        CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, true);

        // carte + denari + settebello + primiera + 2 scope
        assertEquals(6, umano.getPuntiGlobali());
        assertEquals(0, bot.getPuntiGlobali());
    }

    @Test
    void carteVentiAVentiEPrimieraSenzaSemiSonoPatte() {
        Giocatore umano = new Giocatore("Tu");
        Giocatore bot = new Giocatore("Computer");
        umano.aggiungiPrese(carteDeiSemi(Seme.BASTONI, Seme.COPPE));
        bot.aggiungiPrese(carteDeiSemi(Seme.DENARI, Seme.SPADE));

        CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, true);

        // Al bot: denari (10) e settebello. Carte 20-20 e primiera 0-0 patte.
        assertEquals(0, umano.getPuntiGlobali());
        assertEquals(2, bot.getPuntiGlobali());
    }

    @Test
    void quattroSetteBattonoLaPrimieraDelleAltreCarte() {
        Giocatore umano = new Giocatore("Tu");
        Giocatore bot = new Giocatore("Computer");
        for (Carta c : tutteLeCarte()) {
            if (c.getValore() == 7) umano.aggiungiPrese(java.util.Collections.singletonList(c));
            else bot.aggiungiPrese(java.util.Collections.singletonList(c));
        }

        CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, true);

        // Umano: settebello + primiera (84 contro 72). Bot: carte (36) + denari (9).
        assertEquals(2, umano.getPuntiGlobali());
        assertEquals(2, bot.getPuntiGlobali());
    }

    @Test
    void cinqueDenariACiascunoSonoPatta() {
        Giocatore umano = new Giocatore("Tu");
        Giocatore bot = new Giocatore("Computer");
        for (Carta c : tutteLeCarte()) {
            // Umano: denari 1-5, coppe 1-5 e tutti i bastoni (20 carte)
            boolean basso = c.getValore() <= 5;
            boolean aUmano = c.getSeme() == Seme.BASTONI
                    || (c.getSeme() == Seme.DENARI && basso)
                    || (c.getSeme() == Seme.COPPE && basso);
            if (aUmano) umano.aggiungiPrese(java.util.Collections.singletonList(c));
            else bot.aggiungiPrese(java.util.Collections.singletonList(c));
        }

        String resoconto = CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, true);

        assertTrue(resoconto.contains("Denari: Patta"));
    }

    @Test
    void settebelloNonPresoDaNessunoNonVieneAssegnato() {
        Giocatore umano = new Giocatore("Tu");
        Giocatore bot = new Giocatore("Computer");
        // Il settebello e altre carte sono rimasti a terra: nessuno li ha presi
        for (Carta c : carteDeiSemi(Seme.BASTONI)) umano.aggiungiPrese(java.util.Collections.singletonList(c));
        for (Carta c : carteDeiSemi(Seme.COPPE)) bot.aggiungiPrese(java.util.Collections.singletonList(c));

        String resoconto = CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, true);

        assertEquals(0, umano.getPuntiGlobali());
        assertEquals(0, bot.getPuntiGlobali());
        assertTrue(resoconto.contains("Settebello: non assegnato"));
    }

    @Test
    void settebelloPresoDalComputerVaAlComputer() {
        Giocatore umano = new Giocatore("Tu");
        Giocatore bot = new Giocatore("Computer");
        bot.aggiungiPrese(java.util.Collections.singletonList(new Carta(7, Seme.DENARI)));

        String resoconto = CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, true);

        assertTrue(resoconto.contains("Settebello: +1 al Computer"));
        assertEquals(0, umano.getPuntiGlobali());
    }

    @Test
    void ilResocontoMostraIlConteggioRealeDelleCarteInCasoDiPatta() {
        Giocatore umano = new Giocatore("Tu");
        Giocatore bot = new Giocatore("Computer");
        for (Carta c : carteDeiSemi(Seme.BASTONI)) umano.aggiungiPrese(java.util.Collections.singletonList(c));
        for (Carta c : carteDeiSemi(Seme.COPPE)) bot.aggiungiPrese(java.util.Collections.singletonList(c));

        String resoconto = CalcolatorePunti.calcolaEGeneraResoconto(umano, bot, true);

        assertTrue(resoconto.contains("Carte: Patta (10 a 10)"));
        assertFalse(resoconto.contains("20 a 20"));
    }

    @Test
    void cartePrimieraSceglieLaMiglioreDiOgniSeme() {
        List<Carta> prese = new ArrayList<>();
        prese.add(new Carta(1, Seme.COPPE));
        prese.add(new Carta(6, Seme.COPPE));
        prese.add(new Carta(10, Seme.DENARI));
        prese.add(new Carta(7, Seme.DENARI));

        List<Carta> primiera = CalcolatorePunti.getCartePrimiera(prese);

        assertEquals(2, primiera.size());
        assertEquals(6, primiera.get(0).getValore());
        assertEquals(7, primiera.get(1).getValore());
    }
}
