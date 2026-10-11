package scopa.client;

import scopa.shared.Carta;
import scopa.shared.Giocatore;
import scopa.shared.CalcolatorePunti;

import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FinestraDialogo {

    private static final Map<String, ImageIcon> cacheIcone = new HashMap<>();

    public static void mostraMessaggio(Component parent, String messaggio) {
        JDialog dialog = creaDialogBase(parent);
        dialog.add(creaPannelloTesto(messaggio), BorderLayout.CENTER);
        JPanel panelBottoni = new JPanel(new FlowLayout());
        panelBottoni.setOpaque(false);
        JButton btnOk = creaBottone("OK");
        btnOk.addActionListener(e -> dialog.dispose());
        panelBottoni.add(btnOk);
        dialog.add(panelBottoni, BorderLayout.SOUTH);
        mostra(dialog);
    }

    public static void mostraMessaggioTemporizzato(Component parent, String messaggio, int millisecondi) {
        JDialog dialog = creaDialogBase(parent);
        dialog.add(creaPannelloTesto(messaggio), BorderLayout.CENTER);
        JPanel panelVuoto = new JPanel();
        panelVuoto.setOpaque(false);
        panelVuoto.setPreferredSize(new Dimension(10, 30));
        dialog.add(panelVuoto, BorderLayout.SOUTH);
        Timer timer = new Timer(millisecondi, e -> dialog.dispose());
        timer.setRepeats(false);
        timer.start();
        mostra(dialog);
    }

    public static int scegliOpzione(Component parent, String messaggio, String[] opzioni) {
        JDialog dialog = creaDialogBase(parent);
        dialog.add(creaPannelloTesto(messaggio), BorderLayout.CENTER);
        final int[] risultato = {-1};
        JPanel panelBottoni = new JPanel(new GridLayout(opzioni.length, 1, 0, 10));
        panelBottoni.setBorder(BorderFactory.createEmptyBorder(10, 30, 20, 30));
        panelBottoni.setOpaque(false);
        for (int i = 0; i < opzioni.length; i++) {
            final int index = i;
            JButton btn = creaBottone(opzioni[i]);
            btn.addActionListener(e -> { risultato[0] = index; dialog.dispose(); });
            panelBottoni.add(btn);
        }
        dialog.add(panelBottoni, BorderLayout.SOUTH);
        mostra(dialog);
        return risultato[0];
    }

    public static boolean mostraDomanda(Component parent, String messaggio) {
        JDialog dialog = creaDialogBase(parent);
        dialog.add(creaPannelloTesto(messaggio), BorderLayout.CENTER);

        final boolean[] risultato = {false};

        JPanel panelBottoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelBottoni.setOpaque(false);

        JButton btnSi = creaBottone("SÌ, GIOCA ANCORA");
        btnSi.addActionListener(e -> { risultato[0] = true; dialog.dispose(); });

        JButton btnNo = creaBottone("NO, ESCI");
        btnNo.setBackground(new Color(178, 34, 34));
        btnNo.addActionListener(e -> { risultato[0] = false; dialog.dispose(); });

        panelBottoni.add(btnSi);
        panelBottoni.add(btnNo);
        dialog.add(panelBottoni, BorderLayout.SOUTH);

        mostra(dialog);
        return risultato[0];
    }

    public static boolean mostraResocontoVisuale(Component parent, String resocontoTesto, Giocatore umano, Giocatore bot, boolean partitaFinita) {
        JDialog dialog = creaDialogBase(parent);

        JPanel mainPanel = new JPanel(new BorderLayout(0, 10));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        mainPanel.add(creaPannelloTesto(resocontoTesto), BorderLayout.NORTH);

        JPanel panelCarte = new JPanel();
        panelCarte.setLayout(new BoxLayout(panelCarte, BoxLayout.Y_AXIS));
        panelCarte.setOpaque(false);

        JPanel header = new JPanel(new BorderLayout(10, 0));
        header.setOpaque(false);
        JLabel lblTu = new JLabel("LE TUE CARTE", SwingConstants.CENTER);
        lblTu.setForeground(Color.WHITE); lblTu.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTu.setPreferredSize(new Dimension(350, 30));
        JLabel lblCat = new JLabel("", SwingConstants.CENTER);
        lblCat.setPreferredSize(new Dimension(120, 30));
        JLabel lblBot = new JLabel("CARTE AVVERSARIO", SwingConstants.CENTER);
        lblBot.setForeground(Color.WHITE); lblBot.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblBot.setPreferredSize(new Dimension(350, 30));
        header.add(lblTu, BorderLayout.WEST);
        header.add(lblCat, BorderLayout.CENTER);
        header.add(lblBot, BorderLayout.EAST);
        panelCarte.add(header);
        panelCarte.add(Box.createRigidArea(new Dimension(0, 5)));

        panelCarte.add(creaRigaCarte("SCOPE", umano.getCarteScopa(), bot.getCarteScopa()));
        panelCarte.add(creaRigaCarte("SETTEBELLO", CalcolatorePunti.getSettebelloList(umano.getPrese()), CalcolatorePunti.getSettebelloList(bot.getPrese())));
        panelCarte.add(creaRigaCarte("DENARI", CalcolatorePunti.getCarteDenari(umano.getPrese()), CalcolatorePunti.getCarteDenari(bot.getPrese())));
        panelCarte.add(creaRigaCarte("PRIMIERA", CalcolatorePunti.getCartePrimiera(umano.getPrese()), CalcolatorePunti.getCartePrimiera(bot.getPrese())));

        mainPanel.add(panelCarte, BorderLayout.CENTER);
        dialog.add(mainPanel, BorderLayout.CENTER);

        final boolean[] risultato = {false};
        JPanel panelBottoni = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panelBottoni.setOpaque(false);

        if (partitaFinita) {
            JButton btnSi = creaBottone("SÌ, GIOCA ANCORA");
            btnSi.addActionListener(e -> { risultato[0] = true; dialog.dispose(); });
            JButton btnNo = creaBottone("NO, ESCI");
            btnNo.setBackground(new Color(178, 34, 34));
            btnNo.addActionListener(e -> { risultato[0] = false; dialog.dispose(); });
            panelBottoni.add(btnSi);
            panelBottoni.add(btnNo);
        } else {
            JButton btnOk = creaBottone("PROSSIMA MANO");
            btnOk.addActionListener(e -> { risultato[0] = true; dialog.dispose(); });
            panelBottoni.add(btnOk);
        }

        dialog.add(panelBottoni, BorderLayout.SOUTH);

        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);

        return risultato[0];
    }

    private static JPanel creaRigaCarte(String titolo, List<Carta> carteUmano, List<Carta> carteBot) {
        JPanel riga = new JPanel(new BorderLayout(10, 0));
        riga.setOpaque(false);
        riga.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));

        PannelloCarteVisuali pUmano = new PannelloCarteVisuali(carteUmano, true);
        PannelloCarteVisuali pBot = new PannelloCarteVisuali(carteBot, false);

        JLabel lblTitolo = new JLabel(titolo, SwingConstants.CENTER);
        lblTitolo.setForeground(new Color(218, 165, 32));
        lblTitolo.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblTitolo.setPreferredSize(new Dimension(120, 75));

        riga.add(pUmano, BorderLayout.WEST);
        riga.add(lblTitolo, BorderLayout.CENTER);
        riga.add(pBot, BorderLayout.EAST);

        return riga;
    }

    private static class PannelloCarteVisuali extends JPanel {
        private final List<Carta> carte;
        private final boolean allineaDestra;

        public PannelloCarteVisuali(List<Carta> carte, boolean allineaDestra) {
            this.carte = carte;
            this.allineaDestra = allineaDestra;
            setOpaque(false);
            setPreferredSize(new Dimension(350, 75));
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (carte.isEmpty()) {
                g.setColor(new Color(255, 255, 255, 70));
                g.setFont(new Font("SansSerif", Font.BOLD, 22));
                FontMetrics fm = g.getFontMetrics();
                String t = "-";
                int tx = allineaDestra ? getWidth() - fm.stringWidth(t) - 20 : 20;
                g.drawString(t, tx, (getHeight() + fm.getAscent()) / 2 - 5);
                return;
            }

            int larghezza = 45;
            int gap = 20;
            int maxSpazio = getWidth() - larghezza;
            if (carte.size() > 1 && (carte.size() - 1) * gap > maxSpazio) {
                gap = maxSpazio / (carte.size() - 1);
            }

            int ingombroTotale = larghezza + (carte.size() - 1) * gap;
            int startX = allineaDestra ? getWidth() - ingombroTotale : 0;

            for (int i = 0; i < carte.size(); i++) {
                int x = startX + (i * gap);
                ImageIcon icon = getIconaCarta(carte.get(i));
                if (icon != null) {
                    icon.paintIcon(this, g, x, 2);
                }
            }
        }
    }

    private static ImageIcon getIconaCarta(Carta c) {
        if (cacheIcone.containsKey(c.getNomeImmagine())) return cacheIcone.get(c.getNomeImmagine());
        try {
            java.io.InputStream is = FinestraDialogo.class.getResourceAsStream("/carte/" + c.getNomeImmagine());
            if (is != null) {
                BufferedImage img = javax.imageio.ImageIO.read(is);
                Image scaled = img.getScaledInstance(45, 70, Image.SCALE_SMOOTH);

                BufferedImage rounded = new BufferedImage(45, 70, BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2 = rounded.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new java.awt.geom.RoundRectangle2D.Float(0, 0, 45, 70, 8, 8));
                g2.drawImage(scaled, 0, 0, null);
                g2.setClip(null);
                g2.setColor(new Color(0, 0, 0, 100));
                g2.drawRoundRect(0, 0, 44, 69, 8, 8);
                g2.dispose();

                ImageIcon icon = new ImageIcon(rounded);
                cacheIcone.put(c.getNomeImmagine(), icon);
                return icon;
            }
        } catch (Exception e) {}
        return null;
    }

    private static JDialog creaDialogBase(Component parent) {
        Window window = SwingUtilities.getWindowAncestor(parent);
        JDialog dialog = new JDialog(window, Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setUndecorated(true);
        dialog.getContentPane().setBackground(new Color(43, 43, 43));
        dialog.getRootPane().setBorder(new LineBorder(new Color(218, 165, 32), 4, true));
        dialog.setLayout(new BorderLayout());
        return dialog;
    }

    private static JPanel creaPannelloTesto(String messaggio) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(25, 35, 10, 35));
        JTextPane textPane = new JTextPane();
        textPane.setText(messaggio);
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setForeground(Color.WHITE);
        textPane.setFont(new Font("SansSerif", Font.BOLD, 16));
        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);
        panel.add(textPane, BorderLayout.CENTER);
        return panel;
    }

    private static JButton creaBottone(String testo) {
        JButton btn = new JButton(testo);
        btn.setFont(new Font("SansSerif", Font.BOLD, 14));
        btn.setBackground(new Color(34, 139, 34));
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(new Color(218, 165, 32), 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));
        return btn;
    }

    // IL METODO MANCANTE CHE HO DIMENTICATO PRIMA!
    private static void mostra(JDialog dialog) {
        dialog.pack();
        if(dialog.getWidth() < 450) {
            dialog.setSize(450, dialog.getHeight());
        }
        dialog.setLocationRelativeTo(dialog.getParent());
        dialog.setVisible(true);
    }
}