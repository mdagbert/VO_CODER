import java.util.Arrays;
import java.util.Scanner;
import static java.lang.System.exit;
import static java.lang.System.in;

public class Pauvocoder {

    // Processing SEQUENCE size (100 msec with 44100Hz samplerate)
    final static int SEQUENCE = StdAudio.SAMPLE_RATE / 10;

    // Overlapping size (20 msec)
    final static int OVERLAP = SEQUENCE / 5;
    // Best OVERLAP offset seeking window (15 msec)
    final static int SEEK_WINDOW = 3 * OVERLAP / 4;

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("usage: pauvocoder <input.wav> <freqScale>\n");
            exit(1);
        }

        String wavInFile = args[0];
        double freqScale = Double.parseDouble(args[1]);
        String outPutFile = wavInFile.split("\\.")[0] + "_" + freqScale + "_";

        Scanner scanner = new Scanner(System.in);

        // Étape 1 : Lecture et affichage du fichier original
        double[] inputWav = StdAudio.read(wavInFile);
        System.out.println("Waveform originale chargée.");
        joue(inputWav);

        // Pause dans la console
        waitForNextStep(scanner, "Étape 1 : Appuyez sur Entrée pour continuer...");

        // Étape 2 : Resample test
        double[] newPitchWav = resample(inputWav, freqScale);
        StdAudio.save(outPutFile + "Resampled.wav", newPitchWav);
        System.out.println("Waveform resamplée.");
        joue(newPitchWav);

        waitForNextStep(scanner, "Étape 2 : Appuyez sur Entrée pour continuer...");

        // Étape 3 : Dilatation simple
        double[] outputWav = vocodeSimple(newPitchWav, 1.0/freqScale);
        StdAudio.save(outPutFile + "Simple.wav", outputWav);
        System.out.println("Waveform dilatée (simple).");
        joue(outputWav);

        waitForNextStep(scanner, "Étape 3 : Appuyez sur Entrée pour continuer...");

        // Étape 4 : Dilatation avec overlapping
        outputWav = vocodeSimpleOver(newPitchWav, 1.0 / freqScale);
        StdAudio.save(outPutFile + "SimpleOver.wav", outputWav);
        System.out.println("Waveform dilatée avec overlapping.");
        joue(outputWav);

        waitForNextStep(scanner, "Étape 4 : Appuyez sur Entrée pour continuer...");

        // Étape 5 : Dilatation avec overlapping et cross-corrélation
//        outputWav =
        vocodeSimpleOverCross(newPitchWav, 1.0 / freqScale);
//        StdAudio.save(outPutFile + "SimpleOverCross.wav", outputWav);
        System.out.println("Waveform dilatée avec overlapping et cross-corrélation.");
//        joue(outputWav);

        waitForNextStep(scanner, "Étape 5 : Appuyez sur Entrée pour continuer...");

        // Étape 6 : Ajout d'un écho
        outputWav = echo(outputWav, 100, 0.7);
        StdAudio.save(outPutFile + "SimpleOverCrossEcho.wav", outputWav);
        System.out.println("Écho ajouté à la waveform.");
        joue(outputWav);

        System.out.println("Affichage terminé.");
    }


    public static void joue(double[] input) {
        int displayWindow = 5000; // Number of sample to show
        int sampleRate = StdAudio.SAMPLE_RATE;

        // Display and playing in real time
        for (int i = 0; i < input.length; i++) {
            // Play the sample
            StdAudio.play(input[i]);

            // Show the played sample
            if (i % displayWindow == 0 || i == input.length - 1) {
                int startIndex = Math.max(0, i - displayWindow / 2);
                int endIndex = Math.min(input.length, i + displayWindow / 2);
                double[] wavToDisplay = new double[endIndex - startIndex];
                System.arraycopy(input, startIndex, wavToDisplay, 0, wavToDisplay.length);

                displayWaveform(wavToDisplay);
            }
        }
    }

    /**
     * Pause l'exécution jusqu'à ce que l'utilisateur appuie sur Entrée.
     *
     * @param scanner Instance de Scanner pour lire l'entrée.
     * @param message Message à afficher pour indiquer la pause.
     */
    private static void waitForNextStep(Scanner scanner, String message) {
        System.out.println(message);
        scanner.nextLine();
    }

    /**
     * Resample inputWav with freqScale
     * @apiNote We didn't understood
     * @param inputWav
     * @param freqScale
     * @return resampled wav
     */
    public static double[] resample(double[] inputWav, double freqScale) {
        if (freqScale <= 0) {
            throw new IllegalArgumentException("freqScale must be greater than 0");
        }

        if (freqScale == 1) {
            // No resampling needed, return the input directly
            return inputWav.clone();
        }

        // New length and the return array
        int newLength = (int) (inputWav.length / freqScale);
        double[] resampled = new double[newLength];

        for (int i = 0; i < newLength; i++) {
            // We separate the integer and fraction of i * freqScale
            double originalIndex = i * freqScale;
            int index = (int) originalIndex;
            double fraction = originalIndex - index;

            // Then we apply the formula
            if (index + 1 < inputWav.length) {
                // Get the middle point between f(index) and f(index+1) and multiply the fraction by it
                resampled[i] = inputWav[index] + fraction * (inputWav[index + 1] - inputWav[index]);
            } else {
                resampled[i] = inputWav[index]; // Last sample doesn't have a next index
            }
        }

        return resampled;
    }


    /**
     * Simple dilatation, without any overlapping
     *
     * @param input the array of the song
     * @param freqScale dialatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimple(double[] input, double freqScale) {
        if (freqScale <= 0) {
            throw new IllegalArgumentException("freqScale must be greater than 0");
        }

        int inputLength = input.length;

        // Dynamically calculate seqLength to preserve original signal duration after resampling
        int seqLength = (int) (1024 * freqScale); // Base length (1024, 2^10) adjusted by freqScale
        int hopSize = (int) (seqLength * freqScale);

        // Estimate output length
        int numSequences = (inputLength - seqLength) / hopSize + 1;
        int outputLength = numSequences * seqLength;
        double[] output = new double[outputLength];

        // Then proceed to cut the input with seq
        for (int seq = 0; seq < numSequences; seq++) {
            int inputStart = seq * hopSize;
            int outputStart = seq * seqLength;

            for (int i = 0; i < seqLength; i++) {
                if (inputStart + i < inputLength && outputStart + i < outputLength) {
                    output[outputStart + i] = input[inputStart + i];
                }
            }
        }

        return output;
    }


    /**
     * Simple dilatation, with overlapping
     *
     * @apiNote This function is essentially the same as the precedent, but instead of using "square" we use a smooth windows, also called a hann window, the hann window is not used for that, but we can use the same formula since we want to separate some part of the audio ( observation )
     * @param input
     * @param freqScale factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOver(double[] input, double freqScale) {
        if (freqScale <= 0) {
            throw new IllegalArgumentException("freqScale must be greater than 0");
        }

        int inputLength = input.length;

        // Dynamically calculate seqLength to preserve original signal duration after resampling
        int seqLength = (int) (1024 * freqScale); // Base length (1024, 2^10) adjusted by freqScale
        int hopSize = (int) (seqLength * freqScale);
        int oLap = seqLength / 4; // Overlap is set to 25% of the sequence length

        // Estimate output length
        int numSequences = (inputLength - seqLength) / hopSize + 1;
        int outputLength = numSequences * seqLength;
        double[] output = new double[outputLength];

        // Window function to create smooth transitions (Hann window), found the formula on it's wikipedia page
        double[] window = new double[seqLength];
        for (int i = 0; i < seqLength; i++) {
            if (i < oLap) {
                window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (2 * oLap)));
            } else if (i >= seqLength - oLap) {
                window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * (seqLength - i - 1) / (2 * oLap)));
            } else {
                window[i] = 1.0;
            }
        }

        // Then proceed to cut the input with seq with the window
        for (int seq = 0; seq < numSequences; seq++) {
            int inputStart = seq * hopSize;
            int outputStart = seq * (seqLength - oLap);

            for (int i = 0; i < seqLength; i++) {
                if (inputStart + i < inputLength && outputStart + i < outputLength) {
                    output[outputStart + i] += input[inputStart + i] * window[i];
                }
            }
        }

        return output;
    }

    /**
     * Simple dilatation, with overlapping and maximum cross correlation search
     *
     * @param input
     * @param freqScale factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOverCross(double[] input, double freqScale) {
        System.out.println("Could not implement this function :(");
        return input.clone();
    }

    /**
     * Add an echo to the wav
     *
     * @param wav
     * @param delay in msec
     * @param gain
     * @return wav with echo
     */
    public static double[] echo(double[] wav, double delay, double gain) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Display the waveform
     *
     * @param wav
     */
    public static void displayWaveform(double[] wav) {
        int n = wav.length;
        StdDraw.enableDoubleBuffering();
        // Configurer StdDraw
        StdDraw.setCanvasSize(800, 400);
        StdDraw.setXscale(0, n);
        StdDraw.setYscale(-1.1, 1.1);
        StdDraw.clear(StdDraw.WHITE);
        StdDraw.setPenColor(StdDraw.BLUE);

        // Tracer le signal audio
        for (int i = 0; i < n - 1; i++) {
            StdDraw.line(i, wav[i], i + 1, wav[i + 1]);
        }

        // Ajouter des axes
        StdDraw.setPenColor(StdDraw.BLACK);
        StdDraw.line(0, 0, n, 0); // Axe X
        StdDraw.line(0, -1, 0, 1); // Axe Y
        StdDraw.show();
    }


}
