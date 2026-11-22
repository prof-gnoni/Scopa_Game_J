package scopa.client;

import javax.sound.sampled.*;
import javax.swing.Timer;
import java.io.InputStream;
import java.io.BufferedInputStream;

public class GestoreAudio {
    private static Clip bgmClip;
    private static boolean musicaAttiva = true;
    private static float volumeMusica = 0.3f;

    private static Clip dropClip;
    private static Clip presaClip;
    private static Clip shuffleClip;
    private static boolean effettiAttivi = true;
    private static float volumeEffetti = 1.0f;

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
        if (dropClip == null) dropClip = caricaEffetto("drop.wav");
        if (presaClip == null) presaClip = caricaEffetto("presa.wav");
        if (shuffleClip == null) shuffleClip = caricaEffetto("shuffle.wav");
    }

    private static Clip caricaEffetto(String nomeFile) {
        Clip clip = caricaSuono(nomeFile);
        applicaVolume(clip, volumeEffetti);
        return clip;
    }

    public static void impostaEffettiAttivi(boolean attivi) {
        effettiAttivi = attivi;
        if (!effettiAttivi) {
            fermaEffetto(dropClip);
            fermaEffetto(presaClip);
            fermaEffetto(shuffleClip);
        }
    }

    public static void impostaVolumeEffetti(float volume) {
        volumeEffetti = Math.max(0.0f, Math.min(1.0f, volume));
        applicaVolume(dropClip, volumeEffetti);
        applicaVolume(presaClip, volumeEffetti);
        applicaVolume(shuffleClip, volumeEffetti);
    }

    // Scala logaritmica, uguale per musica ed effetti:
    // 100% = volume originale del file (0 dB), 0% = silenzio
    private static void applicaVolume(Clip clip, float volume) {
        if (clip == null || !clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float db = volume <= 0.0f
                ? gain.getMinimum()
                : (float) (20.0 * Math.log10(volume));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
    }

    private static void fermaEffetto(Clip clip) {
        if (clip != null && clip.isRunning()) clip.stop();
    }

    public static void avviaMusicaSottofondo() {
        if (bgmClip == null) {
            bgmClip = caricaSuono("bgm.wav");
            applicaVolume(bgmClip, volumeMusica);
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
        applicaVolume(bgmClip, volumeMusica);
    }

    public static void suonaShuffle(long durataMillisecondi) {
        if (!effettiAttivi) return;
        if (shuffleClip == null) shuffleClip = caricaEffetto("shuffle.wav");
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
        if (dropClip == null) dropClip = caricaEffetto("drop.wav");
        riproduciIstantaneo(dropClip);
    }

    public static void suonaPresa() {
        if (presaClip == null) presaClip = caricaEffetto("presa.wav");
        riproduciIstantaneo(presaClip);
    }

    private static void riproduciIstantaneo(Clip clip) {
        if (!effettiAttivi) return;
        try {
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