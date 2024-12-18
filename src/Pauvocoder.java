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
        displayWaveform(inputWav);

        // Pause dans la console
        waitForNextStep(scanner, "Étape 1 : Appuyez sur Entrée pour continuer...");

        // Étape 2 : Resample test
        double[] newPitchWav = resample(inputWav, freqScale);
        StdAudio.save(outPutFile + "Resampled.wav", newPitchWav);
        System.out.println("Waveform resamplée.");
        displayWaveform(newPitchWav);

        waitForNextStep(scanner, "Étape 2 : Appuyez sur Entrée pour continuer...");

        // Étape 3 : Dilatation simple
        double[] outputWav = vocodeSimple(newPitchWav, 1.0 / freqScale);
        StdAudio.save(outPutFile + "Simple.wav", outputWav);
        System.out.println("Waveform dilatée (simple).");

        waitForNextStep(scanner, "Étape 3 : Appuyez sur Entrée pour continuer...");

        // Étape 4 : Dilatation avec overlapping
        outputWav = vocodeSimpleOver(newPitchWav, 1.0 / freqScale);
        StdAudio.save(outPutFile + "SimpleOver.wav", outputWav);
        System.out.println("Waveform dilatée avec overlapping.");

        waitForNextStep(scanner, "Étape 4 : Appuyez sur Entrée pour continuer...");

        // Étape 5 : Dilatation avec overlapping et cross-corrélation
        outputWav = vocodeSimpleOverCross(newPitchWav, 1.0 / freqScale);
        StdAudio.save(outPutFile + "SimpleOverCross.wav", outputWav);
        System.out.println("Waveform dilatée avec overlapping et cross-corrélation.");

        waitForNextStep(scanner, "Étape 5 : Appuyez sur Entrée pour continuer...");

        // Étape 6 : Ajout d'un écho
        outputWav = echo(outputWav, 100, 0.7);
        StdAudio.save(outPutFile + "SimpleOverCrossEcho.wav", outputWav);
        System.out.println("Écho ajouté à la waveform.");

        waitForNextStep(scanner, "Étape 6 : Appuyez sur Entrée pour afficher le résultat...");

        // Étape 7 : Affichage final
        displayWaveform(outputWav);
        System.out.println("Affichage terminé.");
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
     *
     * @param inputWav
     * @param freqScale
     * @return resampled wav
     */
    public static double[] resample(double[] inputWav, double freqScale) {
        int newLength = (int) (inputWav.length / freqScale);
        double[] resampled = new double[newLength];

        for (int i = 0; i < newLength; i++) {
            double originalIndex = i * freqScale;
            int index = (int) originalIndex;
            double fraction = originalIndex - index;

            if (index + 1 < inputWav.length) {
                resampled[i] = inputWav[index] + fraction * (inputWav[index + 1] - inputWav[index]);
            } else {
                resampled[i] = inputWav[index]; // Dernier échantillon
            }
        }

        return resampled;
    }


    /**
     * Simple dilatation, without any overlapping
     *
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimple(double[] inputWav, double dilatation) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Simple dilatation, with overlapping
     *
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOver(double[] inputWav, double dilatation) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Simple dilatation, with overlapping and maximum cross correlation search
     *
     * @param inputWav
     * @param dilatation factor
     * @return dilated wav
     */
    public static double[] vocodeSimpleOverCross(double[] inputWav, double dilatation) {
        throw new UnsupportedOperationException("Not implemented yet");
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
        int n = 50_000;
        System.out.println(n);
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
