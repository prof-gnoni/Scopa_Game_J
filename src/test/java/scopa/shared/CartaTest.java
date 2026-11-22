package scopa.shared;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CartaTest {

    @Test
    void carteConStessoValoreESemeSonoUguali() {
        Carta a = new Carta(7, Seme.DENARI);
        Carta b = new Carta(7, Seme.DENARI);

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    void carteDiverseNonSonoUguali() {
        Carta setteDenari = new Carta(7, Seme.DENARI);

        assertNotEquals(setteDenari, new Carta(7, Seme.COPPE));
        assertNotEquals(setteDenari, new Carta(6, Seme.DENARI));
        assertNotEquals(setteDenari, null);
        assertNotEquals(setteDenari, "7 di DENARI");
    }

    @Test
    void unaCopiaDellaCartaVieneTrovataERimossaDallaLista() {
        // Simula una carta ricostruita (ad esempio da rete): istanza diversa, stessi dati
        List<Carta> mano = new ArrayList<>(Arrays.asList(new Carta(3, Seme.SPADE), new Carta(7, Seme.DENARI)));
        Carta copia = new Carta(7, Seme.DENARI);

        assertTrue(mano.contains(copia));
        assertTrue(mano.remove(copia));
        assertEquals(1, mano.size());
    }

    @Test
    void ilMazzoContieneQuarantaCarteTutteDiverse() {
        Mazzo mazzo = new Mazzo();
        Set<Carta> distinte = new HashSet<>();
        while (mazzo.carteRimanenti() > 0) {
            distinte.add(mazzo.pesca());
        }

        assertEquals(40, distinte.size());
    }
}
