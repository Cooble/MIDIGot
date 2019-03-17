package cs.cooble;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Matej on 30.12.2017.
 */
public class SongList {
    private static ArrayList<Integer> durationsList = new ArrayList<>();
    private static ArrayList<Integer> notesList = new ArrayList<>();

    public static void open(){
        durationsList.clear();
        notesList.clear();
    }
    public static void addNote(int note,int duration){
        durationsList.add(duration);
        notesList.add(note);
    }

    public static int getPreLength(){
        return durationsList.size();
    }
    public static int getPreNote(int index){
        return notesList.get(index);
    }
    public static int getPreDuration(int index){
        return durationsList.get(index);
    }

    public static SongList bake(){
        SongList out = new SongList();
        out.notes =new int[notesList.size()];
        out.durations=new int[notesList.size()];
        for (int i = 0; i < notesList.size(); i++) {
            out.notes[i]= notesList.get(i);
            out.durations[i]=durationsList.get(i);
        }
        return out;
    }

    private SongList(){}


    int[] durations;
    int[] notes;

    public static List<Integer> getNoteList() {
        return notesList;
    }

    public int getLength(){
        return durations.length;
    }
    public int getNote(int index){
        return notes[index];

    }
    public int getDuration(int index){
        return durations[index];
    }

}
