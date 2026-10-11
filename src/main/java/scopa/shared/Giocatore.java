package scopa.shared;

import java.util.ArrayList;
import java.util.List;

public class Giocatore {
    private String nome;
    private List<Carta> mano;
    private List<Carta> prese;

    // NUOVO: Salviamo fisicamente le carte che hanno fatto scopa per poterle disegnare!
    private List<Carta> carteScopa;
    private int puntiGlobali;

    public Giocatore(String nome) {
        this.nome = nome;
        this.mano = new ArrayList<>();
        this.prese = new ArrayList<>();
        this.carteScopa = new ArrayList<>();
        this.puntiGlobali = 0;
    }

    public String getNome() { return nome; }
    public List<Carta> getMano() { return mano; }
    public List<Carta> getPrese() { return prese; }

    // Restituisce direttamente la quantità di carte scopa
    public int getScope() { return carteScopa.size(); }

    public List<Carta> getCarteScopa() { return carteScopa; }
    public int getPuntiGlobali() { return puntiGlobali; }

    public void aggiungiCartaInMano(Carta carta) { mano.add(carta); }
    public void giocaCarta(Carta carta) { mano.remove(carta); }
    public void aggiungiPrese(List<Carta> cartePrese) { prese.addAll(cartePrese); }

    // NUOVO: Ora passiamo la carta esatta quando facciamo scopa
    public void aggiungiScopa(Carta cartaCheHaFattoScopa) {
        carteScopa.add(cartaCheHaFattoScopa);
    }

    public void aggiungiPuntiGlobali(int punti) { puntiGlobali += punti; }

    public void preparaNuovaMano() {
        mano.clear();
        prese.clear();
        carteScopa.clear(); // Svuota le scope fisiche
    }

    public void resettaTutto() {
        preparaNuovaMano();
        puntiGlobali = 0;
    }
}