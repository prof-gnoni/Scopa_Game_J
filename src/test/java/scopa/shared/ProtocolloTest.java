package scopa.shared;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolloTest {

    @Test
    void codificaEDecodificaUnaCarta() {
        Carta settebello = new Carta(7, Seme.DENARI);
        assertEquals("DENARI,7", Protocollo.codificaCarta(settebello));
        assertEquals(settebello, Protocollo.decodificaCarta("DENARI,7"));
    }

    @Test
    void codificaEDecodificaUnaLista() {
        List<Carta> carte = Arrays.asList(new Carta(3, Seme.COPPE), new Carta(10, Seme.SPADE));
        String testo = Protocollo.codificaCarte(carte);
        assertEquals("COPPE,3;SPADE,10", testo);
        assertEquals(carte, Protocollo.decodificaCarte(testo));
    }

    @Test
    void accettaIlVecchioFormatoConPuntoEVirgolaFinale() {
        assertEquals(Collections.singletonList(new Carta(1, Seme.BASTONI)),
                Protocollo.decodificaCarte("BASTONI,1;"));
    }

    @Test
    void listaVuota() {
        assertEquals("", Protocollo.codificaCarte(Collections.emptyList()));
        assertTrue(Protocollo.decodificaCarte("").isEmpty());
    }

    @Test
    void rifiutaCarteNonValide() {
        assertThrows(IllegalArgumentException.class, () -> Protocollo.decodificaCarta("DENARI,11"));
        assertThrows(IllegalArgumentException.class, () -> Protocollo.decodificaCarta("ORO,3"));
        assertThrows(IllegalArgumentException.class, () -> Protocollo.decodificaCarta("DENARI"));
    }

    @Test
    void ilMazzoInviatoInReteSiPescaNelloStessoOrdine() {
        Mazzo originale = new Mazzo();
        originale.mescola();
        List<Carta> ordine = originale.getCarte();

        Mazzo ricevuto = new Mazzo(Protocollo.decodificaCarte(Protocollo.codificaCarte(ordine)));

        assertEquals(40, ricevuto.carteRimanenti());
        for (int i = 0; i < 40; i++) {
            assertEquals(originale.pesca(), ricevuto.pesca());
        }
        assertNull(ricevuto.pesca());
    }
}
