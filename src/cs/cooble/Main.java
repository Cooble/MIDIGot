package cs.cooble;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.sound.midi.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Scanner;
import java.util.function.BiConsumer;

/**
 * This cute piece of software converts one voice midi files to special binary form which can be read by arduino
 * using https://github.com/Cooble/SSTCInterrupter
 *
 * It will make (MIDI_FILE_SIZE * 3) byte files which can be put on sd card for arduino to read it
 */
public class Main extends Application {
    public static final int MIDI_FILE_SIZE = 100;//in notes!

    public static String TARGET_FOLDER = "";
    public static String TARGET_NAME = "";
    private static File srcFile = null;

    public static final int NOTE_ON = 0x90;
    public static final int NOTE_OFF = 0x80;

    private static final String I_SRC_FILE = "Source file";
    private static final String I_TARGET_FOLDER = "Target folder";
    private static final String I_TARGET_NAME = "Target name";
    private static final String I_CUTOFF_FREQUENCY = "Cutoff frequency";

    private static final int ZERO_FREQUENCY_NOTE = 84;

    private static int CUT_OFF_FREQUENCY = 1000;

    private static int transpose;


    public static void main(String[] args) {
        launch(args);
    }

    public static void main2(String[] args) throws Exception {
        Scanner scanner = new Scanner(System.in);
        Settings settings = loadSettings();
        CUT_OFF_FREQUENCY = Integer.parseInt(settings.getValue(I_CUTOFF_FREQUENCY));
        while (true) {
            System.out.println();
            System.out.println("============================================================================");
            System.out.println("\nWelcome to MIDIGot2");
            System.out.println("\n*Note: use midiGot.conf to set variables");
            settings.getMap().forEach((s, s2) -> System.out.println(s + " -> " + s2));
            if (transpose != 0)
                System.out.println("Transpose -> " + transpose);
            System.out.println();

            srcFile = new File(settings.getValue(I_SRC_FILE));
            if (!srcFile.exists()) {
                System.err.println(I_SRC_FILE + " is not specified");
            }
            TARGET_FOLDER = settings.getValue(I_TARGET_FOLDER);
            if (TARGET_FOLDER == null) {
                System.err.println(I_TARGET_FOLDER + " is not specified");
            }
            TARGET_NAME = settings.getValue(I_TARGET_NAME);
            if (TARGET_NAME == null) {
                System.err.println(I_TARGET_NAME + " is not specified");
            }

            FreqDatabase.loadFreqDatabase(Main.class.getResourceAsStream("midiToFrequency.txt"));
            Sequence sequence = null;

            SongList.open();
            if (srcFile != null && srcFile.exists()) {
                sequence = MidiSystem.getSequence(srcFile);
                long offTick = 0;
                for (Track track : sequence.getTracks()) {
                    long noteStartTime = 0;
                    for (int i = 0; i < track.size(); i++) {
                        MidiEvent event = track.get(i);
                        MidiMessage message = event.getMessage();
                        if (message instanceof ShortMessage) {
                            ShortMessage sm = (ShortMessage) message;
                            if (sm.getCommand() == NOTE_ON) {
                                if (event.getTick() > offTick + 1) {
                                    SongList.addNote(ZERO_FREQUENCY_NOTE, (int) (event.getTick() - offTick - 1));
                                }
                                noteStartTime = event.getTick();
                            } else if (sm.getCommand() == NOTE_OFF) {
                                offTick = event.getTick();
                                SongList.addNote(sm.getData1(), (int) (event.getTick() - noteStartTime));
                            }
                        }
                    }
                }
            }

            SongList preList = SongList.bake();
            int cut = 0;
            if (transpose != 0)
                cut = transpose(settings, transpose);
            SongList list = SongList.bake();


            System.out.println(
                    "\n0) Exit" +
                            "\n1) Set input" +
                            "\n2) Set output" +
                            "\n3) Set output name" +
                            "\n4) Transpose notes" +
                            "\n5) Run");
            System.out.println();
            if (sequence != null) {
                System.out.println("=== SPECS ======================");
                System.out.println("Duration: " + toNormalTime(getDurationInMillis(sequence.getTickLength(), sequence)));
                System.out.println("Notes: " + list.getLength());
                int highest = getHighestFrequency(list);
                int highestOld = getHighestFrequency(preList);
                int lowest = getLowestFrequency(list);
                int lowestOld = getLowestFrequency(preList);
                System.out.println("Max input freq: " + highest + " Hz" + (highest != highestOld ? ("  (orig: " + highestOld + " Hz)") : ""));
                System.out.println("Min input freq: " + lowest + " Hz " + (lowest != lowestOld ? ("  (orig: " + lowestOld + " Hz)") : ""));
                System.out.println("Longest note: " + getLongestNote(list) + " ms");
                if (cut != 0)
                    System.out.println("Cut notes: " + cut);

                if (getNotesOver(list, CUT_OFF_FREQUENCY) != 0) {
                    System.out.println();
                    System.out.println();
                    System.out.println("=========================ERROR================================================================================");
                    System.out.println("-> Notes over " + CUT_OFF_FREQUENCY + ": " + getNotesOver(list, CUT_OFF_FREQUENCY));
                    System.out.println("-> Notes over " + CUT_OFF_FREQUENCY + " ratio: " + 100 * (double) getNotesOver(list, CUT_OFF_FREQUENCY) / list.getLength() + "%");
                    System.out.println("-> Please lower frequencies to not be greater than " + CUT_OFF_FREQUENCY + " Hz");

                }
                System.out.println();
            }
            System.out.println("\nEnter number! ");

            String input = scanner.next();
            switch (input) {
                case "0":
                    System.out.println("Exiting...");
                    settings.saveFile();
                    System.exit(0);
                    return;
                case "1":
                    System.out.println("Choose input!");
                    FileChooser chooser = new FileChooser();
                    if (settings.getValue(I_SRC_FILE) != null) {
                        chooser.setInitialDirectory(new File(settings.getValue(I_SRC_FILE)).getParentFile());

                    }
                    File newInput = chooser.showOpenDialog(null);
                    if (newInput == null)
                        System.out.println("Invalid file!");
                    else {
                        settings.setValue(I_SRC_FILE, newInput.getAbsolutePath());
                        String sss = newInput.getName().substring(0, newInput.getName().lastIndexOf('.')).trim();
                        if (sss.indexOf(' ') != -1)
                            sss = sss.substring(sss.lastIndexOf(' ') + 1);
                        settings.setValue(I_TARGET_NAME, sss.toLowerCase());
                        transpose = 0;
                        System.out.println("Success");
                    }
                    break;
                case "2":
                    System.out.println("Choose output folder!");
                    DirectoryChooser chooser1 = new DirectoryChooser();
                    if (settings.getValue(I_TARGET_FOLDER) != null) {
                        chooser1.setInitialDirectory(new File(settings.getValue(I_TARGET_FOLDER)).getParentFile());
                    }
                    File newInput1 = chooser1.showDialog(null);
                    if (newInput1 == null)
                        System.out.println("Invalid file!");
                    else {
                        settings.setValue(I_TARGET_FOLDER, newInput1.getAbsolutePath());
                        System.out.println("Success");
                    }
                    break;
                case "3":
                    System.out.println("Choose output name!");
                    String s = scanner.nextLine();
                    s = scanner.nextLine();
                    if (s == null || s.equals(""))
                        System.out.println("Invalid name!");
                    else {
                        settings.setValue(I_TARGET_NAME, s);
                        System.out.println("Success");

                    }
                    break;
                case "4":
                    System.out.println("Enter transpose number (like -12 o 6)");
                    String number = scanner.nextLine();
                    number = scanner.nextLine();
                    try {
                        transpose = Integer.parseInt(number);
                        System.out.println("Success");
                    } catch (Exception e) {
                        System.out.println("Invalid number!");
                    }
                    break;
                case "5":
                    if (getNotesOver(list, CUT_OFF_FREQUENCY) != 0) {
                        System.out.println("Error notes are over frequency!");
                    } else {

                        if (srcFile != null && srcFile.exists() && new File(settings.getValue(I_TARGET_FOLDER)).exists()) {
                            System.out.println("Running...");
                            saveIt(sequence, list, settings);
                        } else System.out.println("No files specified!");
                    }
            }
            System.out.println("Enter anything to continue.");
            scanner.nextLine();
            scanner.nextLine();
            for (int i = 0; i < 30; i++) {
                System.out.println();
            }
        }
    }

    /**
     * @param transpose
     * @return number of notes which have been cut to the lowest/highest possible frequency
     */
    private static int transpose(Settings settings, int transpose) {
        int cut = 0;
        List<Integer> list = SongList.getNoteList();
        for (int i = 0; i < list.size(); i++) {
            int note = list.get(i);
            if (note != ZERO_FREQUENCY_NOTE) {
                note += transpose;
                if (note < 0) {
                    note = 0;
                    System.out.println("zlobiva nota" + list.get(i));
                    cut++;
                }
              /*  else if (FreqDatabase.getFrequency(note) > Integer.parseInt(settings.getValue(I_CUTOFF_FREQUENCY))) {
                    note = Integer.parseInt(settings.getValue(I_CUTOFF_FREQUENCY));
                    System.out.println("zlobiva nota high "+list.get(i));
                    cut++;
                }*/
            }
            list.set(i, note);
        }
        return cut;

    }

    private static void saveIt(Sequence sequence, SongList list, Settings settings) throws IOException {
        removeOldFiles(TARGET_FOLDER + "/" + TARGET_NAME);

        int piece = 0;
        int noteIndex = 0;
        int bytes = 0;
        FileOutputStream out = new FileOutputStream(settings.getValue(I_TARGET_FOLDER) + "/" + settings.getValue(I_TARGET_NAME) + piece + ".txt");
        for (int note = 0; note < list.getLength(); note++) {
            noteIndex++;
            if (noteIndex > MIDI_FILE_SIZE) {
                bytes = 0;
                piece++;
                noteIndex = 0;
                out.close();
                out = new FileOutputStream(settings.getValue(I_TARGET_FOLDER) + "/" + settings.getValue(I_TARGET_NAME) + piece + ".txt");

            }
            //out.write(list.getNote(note));
            // int duration = list.getDuration(note);
            out.write(toNoteBytes(list.getNote(note), getDurationInMillis(list.getDuration(note), sequence)));
            bytes += 3;
        }
        if (bytes < MIDI_FILE_SIZE * 3) {
            out.write(new byte[MIDI_FILE_SIZE * 3 - bytes]);//fill other with zero
        }
        out.close();

        System.out.println("Saved " + (piece + 1) + " files to: " + new File(settings.getValue(I_TARGET_FOLDER) + "/" + settings.getValue(I_TARGET_NAME)).getAbsolutePath());
    }

    private static void removeOldFiles(String path) {
        File folder = new File(path).getParentFile();
        File[] list = folder.listFiles();
        for (File f : list) {
            if (f.getAbsolutePath().contains(new File(path).getAbsolutePath())) {
                // System.out.println("removing "+f.getAbsolutePath());
                f.delete();
            }
        }

    }

    private static String toNormalTime(int millis) {
        double m = millis / 1000.0;
        int minutes = (int) (m / 60);
        int sec = (int) (m % 60);
        return minutes + " min  " + sec + " sec";

    }

    private static int getLowestFrequency(SongList list) {
        int lowestFreq = CUT_OFF_FREQUENCY;
        for (int i = 0; i < list.getLength(); i++) {
            int note = list.getNote(i);
            if (note != ZERO_FREQUENCY_NOTE) {
                if (FreqDatabase.getFrequency(list.getNote(i)) != 0)
                    lowestFreq = Math.min(FreqDatabase.getFrequency(list.getNote(i)), lowestFreq);
            }
        }
        return lowestFreq;

    }

    private static int getHighestFrequency(SongList list) {
        int lowestFreq = 0;
        for (int i = 0; i < list.getLength(); i++)
            if (list.getNote(i) != ZERO_FREQUENCY_NOTE) {
                lowestFreq = Math.max(FreqDatabase.getFrequency(list.getNote(i)), lowestFreq);
            }
        return lowestFreq;
    }

    private static int getNotesOver(SongList list, int limit) {
        int number = 0;
        for (int i = 0; i < list.getLength(); i++) {
            if (FreqDatabase.getFrequency(list.getNote(i)) > limit)
                number++;
        }
        return number;
    }

    private static int getLongestNote(SongList list) {
        int longestNote = 0;
        for (int i = 0; i < list.getLength(); i++)
            longestNote = Math.max(longestNote, list.getDuration(i));
        return longestNote;
    }

    public static int getDurationInMillis(long ticks, Sequence sequence) {
        return (int) (((double) ticks / (double) sequence.getTickLength()) * (sequence.getMicrosecondLength() / 1000));
    }

    public static byte[] intToByteArray(int value) {
        return new byte[]{
                (byte) (value >>> 8),
                (byte) value};
    }

    public static byte[] toNoteBytes(int note, int duration) {

        int uno = ((duration >> 8) & 0b00001111);
        int f = ((note >> 8) & 0b00001111);
        f = (f << 4 & 0b11110000);
        uno |= f;

        //flip
        int sub = uno;
        uno >>= 4;
        uno &= 0b00001111;
        sub <<= 4;
        sub &= 0b11110000;
        uno |= sub;

        byte duo = (byte) (duration & 255);
        byte tres = (byte) (note & 255);

        return new byte[]{(byte) uno, duo, tres};


    }

    private static void println(byte b) {
        String s1 = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
        System.out.println(s1); // 10000001
    }

    public static Settings loadSettings() throws IOException {
        File conf = new File("midiGot.conf");
        Settings settings = new Settings(conf);
        settings.addAttribute(I_SRC_FILE, srcFile == null ? "" : srcFile.getAbsolutePath());
        settings.addAttribute(I_TARGET_FOLDER, TARGET_FOLDER);
        settings.addAttribute(I_TARGET_NAME, TARGET_NAME);
        settings.addAttribute(I_TARGET_NAME, TARGET_NAME);
        settings.addAttribute(I_CUTOFF_FREQUENCY, CUT_OFF_FREQUENCY + "");
        settings.loadFile();
        return settings;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        main2(null);
    }
}
