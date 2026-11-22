package scopa.client;

import javax.sound.sampled.*;
import javax.swing.Timer;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

public class GestoreAudio {
    private static final Logger LOG = Logger.getLogger(GestoreAudio.class.getName());

    private static Clip bgmClip;
    private static boolean musicaAttiva = true;
    private static float volumeMusica = 0.6f;

    private static Clip dropClip;
    private static Clip presaClip;
    private static Clip shuffleClip;
    private static boolean effettiAttivi = true;
    private static float volumeEffetti = 1.0f;

    // Bilanciamento: i file audio hanno livelli molto diversi tra loro
    // (bgm.wav è ~20 dB più forte degli effetti). Queste correzioni, misurate
    // sulla parte udibile di ogni file, li portano a un livello coerente:
    // effetti intorno a -30 dBFS, musica più bassa per restare di sottofondo.
    // Java consente al massimo +6 dB di guadagno.
    private static final float CORREZIONE_MUSICA_DB = -14f;
    private static final float CORREZIONE_DROP_DB = -4f;
    private static final float CORREZIONE_PRESA_DB = 6f;
    private static final float CORREZIONE_SHUFFLE_DB = 6f;

    // Ampiezza = volume^3: curva percettiva standard dei controlli di volume.
    // 100% = 0 dB, 50% ≈ -18 dB, 25% ≈ -36 dB, 10% = -60 dB (quasi impercettibile), 0% = muto.
    private static final double ESPONENTE_CURVA = 3.0;

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
            LOG.log(Level.WARNING, "File audio non supportato: /audio/" + nomeFile, e);
            return null;
        }
    }

    // Precaricamento all'avvio per evitare lag al click
    public static void inizializza() {
        if (dropClip == null) dropClip = caricaEffetto("drop.wav", CORREZIONE_DROP_DB);
        if (presaClip == null) presaClip = caricaEffetto("presa.wav", CORREZIONE_PRESA_DB);
        if (shuffleClip == null) shuffleClip = caricaEffetto("shuffle.wav", CORREZIONE_SHUFFLE_DB);
    }

    private static Clip caricaEffetto(String nomeFile, float correzioneDb) {
        Clip clip = caricaSuono(nomeFile);
        applicaVolume(clip, volumeEffetti, correzioneDb);
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
        applicaVolume(dropClip, volumeEffetti, CORREZIONE_DROP_DB);
        applicaVolume(presaClip, volumeEffetti, CORREZIONE_PRESA_DB);
        applicaVolume(shuffleClip, volumeEffetti, CORREZIONE_SHUFFLE_DB);
    }

    // Stessa curva per musica ed effetti, più la correzione di bilanciamento del file
    private static void applicaVolume(Clip clip, float volume, float correzioneDb) {
        if (clip == null) return;
        if (clip.isControlSupported(BooleanControl.Type.MUTE)) {
            ((BooleanControl) clip.getControl(BooleanControl.Type.MUTE)).setValue(volume <= 0.0f);
        }
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        float db = volume <= 0.0f
                ? gain.getMinimum()
                : (float) (20.0 * ESPONENTE_CURVA * Math.log10(volume)) + correzioneDb;
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), db)));
    }

    private static void fermaEffetto(Clip clip) {
        if (clip != null && clip.isRunning()) clip.stop();
    }

    public static void avviaMusicaSottofondo() {
        if (bgmClip == null) {
            bgmClip = caricaSuono("bgm.wav");
            applicaVolume(bgmClip, volumeMusica, CORREZIONE_MUSICA_DB);
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
        applicaVolume(bgmClip, volumeMusica, CORREZIONE_MUSICA_DB);
    }

    public static void suonaShuffle(long durataMillisecondi) {
        if (!effettiAttivi) return;
        if (shuffleClip == null) shuffleClip = caricaEffetto("shuffle.wav", CORREZIONE_SHUFFLE_DB);
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
        if (dropClip == null) dropClip = caricaEffetto("drop.wav", CORREZIONE_DROP_DB);
        riproduciIstantaneo(dropClip);
    }

    public static void suonaPresa() {
        if (presaClip == null) presaClip = caricaEffetto("presa.wav", CORREZIONE_PRESA_DB);
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
            LOG.log(Level.WARNING, "Impossibile riprodurre l'effetto sonoro", e);
        }
    }
}