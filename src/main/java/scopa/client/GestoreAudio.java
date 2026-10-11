package scopa.client;

import javax.sound.sampled.*;
import javax.swing.Timer;
import java.io.InputStream;
import java.io.BufferedInputStream;

public class GestoreAudio {
    private static Clip bgmClip;
    private static FloatControl bgmVolumeControl;
    private static boolean musicaAttiva = true;
    private static float volumeMusica = 0.7f;

    private static Clip dropClip;
    private static Clip presaClip;
    private static Clip shuffleClip;

    // Carica e mantiene in memoria il Clip
    private static Clip caricaSuono(String nomeFile) {
        try {
            InputStream audioSrc = GestoreAudio.class.getResourceAsStream("/audio/" + nomeFile);
            if (audioSrc == null) return null;
            InputStream bufferedIn = new BufferedInputStream(audioSrc);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(bufferedIn);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            return clip;
        } catch (Exception e) {
            System.err.println("File audio non trovato o non supportato: /audio/" + nomeFile);
            return null;
        }
    }

    // Precaricamento all'avvio per evitare lag al click
    public static void inizializza() {
        if (dropClip == null) dropClip = caricaSuono("drop.wav");
        if (presaClip == null) presaClip = caricaSuono("presa.wav");
        if (shuffleClip == null) shuffleClip = caricaSuono("shuffle.wav");
    }

    public static void avviaMusicaSottofondo() {
        if (bgmClip == null) {
            bgmClip = caricaSuono("bgm.wav");
            if (bgmClip != null) {
                bgmVolumeControl = (FloatControl) bgmClip.getControl(FloatControl.Type.MASTER_GAIN);
                aggiornaVolume();
            }
        }
        if (bgmClip != null && musicaAttiva) {
            bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
        }
    }

    public static void impostaMusicaAttiva(boolean attiva) {
        musicaAttiva = attiva;
        if (bgmClip != null) {
            if (musicaAttiva) {
                bgmClip.loop(Clip.LOOP_CONTINUOUSLY);
            } else {
                bgmClip.stop();
            }
        }
    }

    public static void impostaVolume(float volume) {
        volumeMusica = Math.max(0.0f, Math.min(1.0f, volume));
        aggiornaVolume();
    }

    private static void aggiornaVolume() {
        if (bgmVolumeControl != null) {
            float range = bgmVolumeControl.getMaximum() - bgmVolumeControl.getMinimum();
            float gain = (range * volumeMusica) + bgmVolumeControl.getMinimum();
            bgmVolumeControl.setValue(gain);
        }
    }

    public static void suonaShuffle(long durataMillisecondi) {
        if (shuffleClip == null) shuffleClip = caricaSuono("shuffle.wav");
        if (shuffleClip != null) {
            shuffleClip.stop();
            shuffleClip.setFramePosition(0);
            shuffleClip.start();

            Timer timerStop = new Timer((int) durataMillisecondi, e -> {
                if (shuffleClip != null && shuffleClip.isRunning()) {
                    shuffleClip.stop();
                }
            });
            timerStop.setRepeats(false);
            timerStop.start();
        }
    }

    public static void suonaCartaGiocata() {
        riproduciIstantaneo(dropClip, "drop.wav");
    }

    public static void suonaPresa() {
        riproduciIstantaneo(presaClip, "presa.wav");
    }

    private static void riproduciIstantaneo(Clip clip, String nomeFile) {
        try {
            if (clip == null) {
                clip = caricaSuono(nomeFile);
            }
            if (clip != null) {
                if (clip.isRunning()) {
                    clip.stop();
                }
                clip.setFramePosition(0); // Riavvolge a zero senza ricaricare da disco
                clip.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}