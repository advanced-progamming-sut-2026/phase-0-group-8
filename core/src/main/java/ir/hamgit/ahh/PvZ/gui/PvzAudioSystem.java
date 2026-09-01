package ir.hamgit.ahh.PvZ.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.audio.AudioDevice;
import ir.hamgit.ahh.PvZ.model.User;
import ir.hamgit.ahh.PvZ.model.enums.ChapterType;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;

 







final class PvzAudioSystem implements AutoCloseable {
    private static final int SAMPLE_RATE = 22_050;
    private static final int BUFFER_SIZE = 1024;
    private static final double TWO_PI = Math.PI * 2.0;

    private enum Mode { SILENT, MENU, GAME }

    private final AudioDevice device;
    private final Thread mixerThread;
    private final ConcurrentLinkedQueue<Voice> queuedVoices = new ConcurrentLinkedQueue<>();
    private volatile boolean running;
    private volatile boolean muted;
    private volatile float musicVolume = .70f;
    private volatile float effectsVolume = .80f;
    private volatile Mode mode = Mode.MENU;
    private volatile int worldIndex;
    private long musicSample;

    PvzAudioSystem() {
        AudioDevice created = null;
        try {
            created = Gdx.audio.newAudioDevice(SAMPLE_RATE, true);
        } catch (RuntimeException ignored) {
            
            running = false;
        }
        device = created;
        Thread createdThread = null;
        if (device != null) {
            running = true;
            createdThread = new Thread(this::mixLoop, "pvz-audio-mixer");
            createdThread.setDaemon(true);
            createdThread.start();
        }
        mixerThread = createdThread;
    }

    void apply(User user) {
        if (user == null) {
            return;
        }
        musicVolume = user.getMusicVolume() / 100f;
        effectsVolume = user.getSoundEffectsVolume() / 100f;
        muted = user.isAudioMuted();
    }

    void playMenuMusic() {
        mode = Mode.MENU;
    }

    void playGameMusic(ChapterType chapter) {
        worldIndex = chapter == null ? 0 : chapter.ordinal();
        mode = Mode.GAME;
    }

    void silenceMusic() {
        mode = Mode.SILENT;
    }

    void playCue(String cue) {
        if (!running || muted || effectsVolume <= 0f || cue == null) {
            return;
        }
        String normalized = cue.toLowerCase(Locale.ROOT);
        Voice voice = switch (normalized) {
            case "click" -> new Voice(720, .055f, .18f, Wave.SQUARE);
            case "plant" -> new Voice(440, .12f, .24f, Wave.SINE);
            case "collect", "pickup" -> new Voice(880, .16f, .22f, Wave.SINE);
            case "wave" -> new Voice(220, .34f, .28f, Wave.SAW);
            case "impact", "hit" -> new Voice(145, .07f, .20f, Wave.NOISE);
            case "explosion", "mower" -> new Voice(72, .38f, .36f, Wave.NOISE);
            case "special" -> new Voice(520, .24f, .24f, Wave.SAW);
            case "win" -> new Voice(660, .65f, .28f, Wave.ARPEGGIO);
            case "lose" -> new Voice(185, .65f, .25f, Wave.FALL);
            default -> new Voice(330, .09f, .16f, Wave.SINE);
        };
        queuedVoices.offer(voice);
    }

    private void mixLoop() {
        short[] samples = new short[BUFFER_SIZE];
        List<Voice> active = new ArrayList<>();
        try {
            while (running) {
                Voice next;
                while ((next = queuedVoices.poll()) != null) {
                    active.add(next);
                }
                for (int i = 0; i < samples.length; i++) {
                    double mixed = muted ? 0.0 : musicSample() * musicVolume;
                    if (!muted) {
                        for (Voice voice : active) {
                            mixed += voice.sample() * effectsVolume;
                        }
                    }
                    mixed = Math.max(-.92, Math.min(.92, mixed));
                    samples[i] = (short) Math.round(mixed * Short.MAX_VALUE);
                    musicSample++;
                }
                for (Iterator<Voice> it = active.iterator(); it.hasNext();) {
                    if (it.next().finished()) {
                        it.remove();
                    }
                }
                device.writeSamples(samples, 0, samples.length);
            }
        } catch (RuntimeException ignored) {
            running = false;
        }
    }

    private double musicSample() {
        if (mode == Mode.SILENT) {
            return 0.0;
        }
        int[] menuNotes = {60, 64, 67, 72, 67, 64, 62, 67};
        int[][] gameNotes = {
            {57, 60, 64, 62, 57, 65, 64, 60},
            {50, 55, 57, 62, 57, 55, 53, 50},
            {62, 66, 69, 71, 69, 66, 64, 62},
            {48, 51, 55, 58, 55, 51, 50, 48}
        };
        boolean menu = mode == Mode.MENU;
        int[] notes = menu ? menuNotes : gameNotes[Math.floorMod(worldIndex, gameNotes.length)];
        double stepSeconds = menu ? .42 : .31;
        long stepSamples = Math.max(1L, Math.round(SAMPLE_RATE * stepSeconds));
        int index = (int) ((musicSample / stepSamples) % notes.length);
        double frequency = 440.0 * Math.pow(2.0, (notes[index] - 69) / 12.0);
        double time = musicSample / (double) SAMPLE_RATE;
        double beatPhase = (musicSample % stepSamples) / (double) stepSamples;
        double envelope = Math.min(1.0, beatPhase * 12.0) * Math.max(.12, 1.0 - beatPhase * .72);
        double lead = Math.sin(TWO_PI * frequency * time) * .075 * envelope;
        double bass = Math.sin(TWO_PI * (frequency / (menu ? 2.0 : 4.0)) * time) * .045;
        double pulse = Math.sin(TWO_PI * (menu ? 1.2 : 2.0) * time) * .008;
        return lead + bass + pulse;
    }

    @Override
    public void close() {
        running = false;
        if (mixerThread != null) {
            mixerThread.interrupt();
            try {
                mixerThread.join(250L);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        if (device != null) {
            try {
                device.dispose();
            } catch (RuntimeException ignored) {
                
            }
        }
    }

    private enum Wave { SINE, SQUARE, SAW, NOISE, ARPEGGIO, FALL }

    private static final class Voice {
        private final double baseFrequency;
        private final int durationSamples;
        private final double amplitude;
        private final Wave wave;
        private int sample;
        private int noise = 0x13579BDF;

        private Voice(double frequency, float seconds, double amplitude, Wave wave) {
            baseFrequency = frequency;
            durationSamples = Math.max(1, Math.round(seconds * SAMPLE_RATE));
            this.amplitude = amplitude;
            this.wave = wave;
        }

        private double sample() {
            double progress = sample / (double) durationSamples;
            double envelope = Math.max(0.0, 1.0 - progress);
            double frequency = switch (wave) {
                case ARPEGGIO -> baseFrequency * (sample / (SAMPLE_RATE / 8) % 3 == 0 ? 1.0
                    : sample / (SAMPLE_RATE / 8) % 3 == 1 ? 1.25 : 1.5);
                case FALL -> baseFrequency * (1.4 - progress * .8);
                default -> baseFrequency;
            };
            double phase = TWO_PI * frequency * sample / SAMPLE_RATE;
            double value = switch (wave) {
                case SQUARE -> Math.sin(phase) >= 0 ? 1.0 : -1.0;
                case SAW -> 2.0 * ((frequency * sample / SAMPLE_RATE) % 1.0) - 1.0;
                case NOISE -> nextNoise();
                default -> Math.sin(phase);
            };
            sample++;
            return value * amplitude * envelope;
        }

        private double nextNoise() {
            noise ^= noise << 13;
            noise ^= noise >>> 17;
            noise ^= noise << 5;
            return (noise & 0x7fffffff) / (double) Integer.MAX_VALUE * 2.0 - 1.0;
        }

        private boolean finished() {
            return sample >= durationSamples;
        }
    }
}
