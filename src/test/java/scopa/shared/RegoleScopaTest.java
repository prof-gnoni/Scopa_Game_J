package scopa.shared;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RegoleScopaTest {

    @Test
    void laCartaSingolaHaPrecedenzaSulleSomme() {
        Carta sette = new Carta(7, Seme.BASTONI);
        List<Carta> tavolo = Arrays.asList(new Carta(3, Seme.COPPE), new Carta(4, Seme.SPADE), sette);

        List<List<Carta>> opzioni = RegoleScopa.trovaPresePossibili(new Carta(7, Seme.DENARI), tavolo);

        assertEquals(1, opzioni.size());
        assertEquals(Collections.singletonList(sette), opzioni.get(0));
    }

    @Test
    void piuCarteUgualiATerraDannoPiuOpzioniSingole() {
        Carta cinqueCoppe = new Carta(5, Seme.COPPE);
        Carta cinqueSpade = new Carta(5, Seme.SPADE);
        List<Carta> tavolo = Arrays.asList(cinqueCoppe, new Carta(2, Seme.BASTONI), cinqueSpade);

        List<List<Carta>> opzioni = RegoleScopa.trovaPresePossibili(new Carta(5, Seme.DENARI), tavolo);

        assertEquals(2, opzioni.size());
        assertEquals(Collections.singletonList(cinqueCoppe), opzioni.get(0));
        assertEquals(Collections.singletonList(cinqueSpade), opzioni.get(1));
    }

    @Test
    void trovaTutteLeCombinazioniPerSomma() {
        Carta tre = new Carta(3, Seme.COPPE);
        Carta quattro = new Carta(4, Seme.SPADE);
        Carta due = new Carta(2, Seme.BASTONI);
        Carta cinque = new Carta(5, Seme.DENARI);
        List<Carta> tavolo = Arrays.asList(tre, quattro, due, cinque);

        List<List<Carta>> opzioni = RegoleScopa.trovaPresePossibili(new Carta(7, Seme.COPPE), tavolo);

        assertEquals(2, opzioni.size());
        assertTrue(opzioni.contains(Arrays.asList(tre, quattro)));
        assertTrue(opzioni.contains(Arrays.asList(due, cinque)));
    }

    @Test
    void sommaDiTreCarte() {
        Carta asso = new Carta(1, Seme.COPPE);
        Carta due = new Carta(2, Seme.SPADE);
        Carta tre = new Carta(3, Seme.BASTONI);
        List<Carta> tavolo = Arrays.asList(asso, due, tre);

        List<List<Carta>> opzioni = RegoleScopa.trovaPresePossibili(new Carta(6, Seme.DENARI), tavolo);

        assertEquals(Collections.singletonList(Arrays.asList(asso, due, tre)), opzioni);
    }

    @Test
    void nessunaPresaPossibile() {
        List<Carta> tavolo = Arrays.asList(new Carta(1, Seme.COPPE), new Carta(2, Seme.SPADE));

        assertTrue(RegoleScopa.trovaPresePossibili(new Carta(10, Seme.DENARI), tavolo).isEmpty());
    }

    @Test
    void tavoloVuotoNessunaPresa() {
        assertTrue(RegoleScopa.trovaPresePossibili(new Carta(4, Seme.DENARI), new ArrayList<>()).isEmpty());
    }
}
