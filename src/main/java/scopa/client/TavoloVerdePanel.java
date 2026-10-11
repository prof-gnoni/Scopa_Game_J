package scopa.client;

import scopa.shared.Carta;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TavoloVerdePanel extends JPanel {

    private final Map<String, BufferedImage> cacheImmagini = new HashMap<>();

    private List<Carta> carteTavolo = new ArrayList<>();
    private List<Carta> manoGiocatore = new ArrayList<>();

    private int numeroCarteAvversario = 0;
    private int carteRimanentiMazzo = 0;

    private int preseUmano = 0;
    private List<Carta> scopeUmano = new ArrayList<>();
    private int preseBot = 0;
    private List<Carta> scopeBot = new ArrayList<>();

    private Carta cartaEvidenziata = null;
    private List<Carta> preseEvidenziate = new ArrayList<>();

    private final List<Rectangle> hitboxMano = new ArrayList<>();

    public interface ListenerCarta {
        void cartaCliccata(Carta c);
    }
    private ListenerCarta listener;

    public void setListener(ListenerCarta listener) {
        this.listener = listener;
    }

    public void setStatoMazzoEAvversario(int numeroCarteAvv, int rimanentiMazzo) {
        this.numeroCarteAvversario = numeroCarteAvv;
        this.carteRimanentiMazzo = rimanentiMazzo;
        repaint();
    }

    public void setStatoPrese(int numPreseUmano, List<Carta> scopeU, int numPreseBot, List<Carta> scopeB) {
        this.preseUmano = numPreseUmano;
        this.scopeUmano = scopeU;
        this.preseBot = numPreseBot;
        this.scopeBot = scopeB;
        repaint();
    }

    public void impostaEvidenza(Carta cartaGiocata, List<Carta> prese) {
        this.cartaEvidenziata = cartaGiocata;
        this.preseEvidenziate = (prese != null) ? prese : new ArrayList<>();
        repaint();
    }

    public TavoloVerdePanel() {
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                gestisciClick(e.getPoint());
            }
        });
    }

    public void setCarteTavolo(List<Carta> carte) {
        this.carteTavolo = carte;
        repaint();
    }

    public void setManoGiocatore(List<Carta> carte) {
        this.manoGiocatore = carte;
        repaint();
    }

    private void gestisciClick(Point puntoCliccato) {
        if (listener == null) return;
        for (int i = hitboxMano.size() - 1; i >= 0; i--) {
            if (hitboxMano.get(i).contains(puntoCliccato)) {
                Carta cartaCliccata = manoGiocatore.get(i);
                listener.cartaCliccata(cartaCliccata);
                break;
            }
        }
    }

    private BufferedImage getImmagineCarta(String nomeImmagine) {
        if (!cacheImmagini.containsKey(nomeImmagine)) {
            try {
                InputStream is = getClass().getResourceAsStream("/carte/" + nomeImmagine);
                if (is != null) cacheImmagini.put(nomeImmagine, ImageIO.read(is));
            } catch (Exception e) { e.printStackTrace(); }
        }
        return cacheImmagini.get(nomeImmagine);
    }

    private void disegnaCartaArrotondata(Graphics2D g2d, BufferedImage img, int x, int y, int larghezza, int altezza) {
        int arc = 15;

        Shape clipSalvata = g2d.getClip();
        RoundRectangle2D cartaShape = new RoundRectangle2D.Float(x, y, larghezza, altezza, arc, arc);

        g2d.clip(cartaShape);
        g2d.drawImage(img, x, y, larghezza, altezza, null);

        g2d.setClip(clipSalvata);

        g2d.setColor(new Color(0, 0, 0, 80));
        g2d.setStroke(new BasicStroke(1));
        g2d.draw(cartaShape);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();

        Point2D center = new Point2D.Float(width / 2.0f, height / 2.0f);
        float radius = Math.max(width, height);
        float[] dist = {0.0f, 1.0f};

        // --- MODIFICA EFFETTUATA QUI: Colori resi "verde pastello rilassante" (basato su ricerca scientifica) ---
        Color[] colors = {new Color(146, 210, 147), new Color(85, 160, 85)};

        RadialGradientPaint p = new RadialGradientPaint(center, radius, dist, colors);
        g2d.setPaint(p);
        g2d.fillRect(0, 0, width, height);

        int altezzaCarta = getHeight() / 4;
        int larghezzaCarta = (int) (altezzaCarta * 0.66);

        int spazioVuotoTotale = height - (3 * altezzaCarta);
        int gapY = spazioVuotoTotale / 4;

        int yAvversario = gapY;
        int yTavolo = (gapY * 2) + altezzaCarta;
        int yMano = (gapY * 3) + (altezzaCarta * 2);

        int ingombroLaterale = larghezzaCarta + 80;

        int maxSpazioTavolo = width - (ingombroLaterale * 2);
        if (maxSpazioTavolo < larghezzaCarta) {
            maxSpazioTavolo = larghezzaCarta;
        }

        int gapTavolo = 20;
        int larghezzaPrevistaTavolo = (carteTavolo.size() * larghezzaCarta) + (Math.max(0, carteTavolo.size() - 1) * gapTavolo);

        if (larghezzaPrevistaTavolo > maxSpazioTavolo && carteTavolo.size() > 1) {
            gapTavolo = (maxSpazioTavolo - (carteTavolo.size() * larghezzaCarta)) / (carteTavolo.size() - 1);
        }

        int larghezzaTotaleTavolo = (carteTavolo.size() * larghezzaCarta) + (Math.max(0, carteTavolo.size() - 1) * gapTavolo);
        int startXTavolo = (width - larghezzaTotaleTavolo) / 2;

        for (int i = 0; i < carteTavolo.size(); i++) {
            Carta c = carteTavolo.get(i);
            BufferedImage img = getImmagineCarta(c.getNomeImmagine());
            if (img != null) {
                int x = startXTavolo + (i * (larghezzaCarta + gapTavolo));

                disegnaCartaArrotondata(g2d, img, x, yTavolo, larghezzaCarta, altezzaCarta);

                if (c.equals(cartaEvidenziata)) {
                    g2d.setColor(new Color(255, 215, 0, 200));
                    g2d.setStroke(new BasicStroke(5));
                    g2d.drawRoundRect(x, yTavolo, larghezzaCarta, altezzaCarta, 15, 15);
                } else if (preseEvidenziate.contains(c)) {
                    g2d.setColor(new Color(0, 191, 255, 200));
                    g2d.setStroke(new BasicStroke(5));
                    g2d.drawRoundRect(x, yTavolo, larghezzaCarta, altezzaCarta, 15, 15);
                }
            }
        }

        hitboxMano.clear();
        int gapStandard = 20;
        int larghezzaTotaleMano = (manoGiocatore.size() * larghezzaCarta) + (Math.max(0, manoGiocatore.size() - 1) * gapStandard);
        int startXMano = (width - larghezzaTotaleMano) / 2;

        for (int i = 0; i < manoGiocatore.size(); i++) {
            Carta c = manoGiocatore.get(i);
            BufferedImage img = getImmagineCarta(c.getNomeImmagine());
            if (img != null) {
                int x = startXMano + (i * (larghezzaCarta + gapStandard));
                disegnaCartaArrotondata(g2d, img, x, yMano, larghezzaCarta, altezzaCarta);
                hitboxMano.add(new Rectangle(x, yMano, larghezzaCarta, altezzaCarta));
            }
        }

        BufferedImage imgRetro = getImmagineCarta("carte_napoletane_retro.jpg");
        if (imgRetro != null) {
            int larghezzaTotaleAvversario = (numeroCarteAvversario * larghezzaCarta) + (Math.max(0, numeroCarteAvversario - 1) * gapStandard);
            int startXAvversario = (width - larghezzaTotaleAvversario) / 2;

            for (int i = 0; i < numeroCarteAvversario; i++) {
                int x = startXAvversario + (i * (larghezzaCarta + gapStandard));
                disegnaCartaArrotondata(g2d, imgRetro, x, yAvversario, larghezzaCarta, altezzaCarta);
            }

            if (carteRimanentiMazzo > 0) {
                int xMazzo = width - larghezzaCarta - 40;
                disegnaCartaArrotondata(g2d, imgRetro, xMazzo, yTavolo, larghezzaCarta, altezzaCarta);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Arial", Font.BOLD, 18));
                String testoMazzo = carteRimanentiMazzo + " carte";
                FontMetrics fm = g2d.getFontMetrics();
                int testoX = xMazzo + (larghezzaCarta - fm.stringWidth(testoMazzo)) / 2;
                g2d.drawString(testoMazzo, testoX, yTavolo - 10);
            }

            disegnaMazzettoPrese(g2d, 40, yAvversario, larghezzaCarta, altezzaCarta, preseBot, scopeBot, imgRetro);
            disegnaMazzettoPrese(g2d, 40, yMano, larghezzaCarta, altezzaCarta, preseUmano, scopeUmano, imgRetro);
        }
    }

    private void disegnaMazzettoPrese(Graphics2D g2d, int startX, int startY, int larghezzaCarta, int altezzaCarta, int numPrese, List<Carta> scope, BufferedImage imgRetro) {
        if (numPrese == 0) return;

        double percentualeSpuntare = 0.40;
        int stepSpuntare = (int) (larghezzaCarta * percentualeSpuntare);

        for (int i = scope.size() - 1; i >= 0; i--) {
            Carta c = scope.get(i);
            BufferedImage img = getImmagineCarta(c.getNomeImmagine());
            if (img != null) {
                int xScopa = startX + (stepSpuntare * (i + 1));
                disegnaCartaArrotondata(g2d, img, xScopa, startY, larghezzaCarta, altezzaCarta);
            }
        }

        disegnaCartaArrotondata(g2d, imgRetro, startX, startY, larghezzaCarta, altezzaCarta);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 16));
        String testo = numPrese + " prese";
        FontMetrics fm = g2d.getFontMetrics();
        g2d.drawString(testo, startX + (larghezzaCarta - fm.stringWidth(testo)) / 2, startY - 10);
    }
}