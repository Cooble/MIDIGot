package cs.cooble;

import java.io.*;

/**
 * Created by Matej on 30.12.2017.
 */
public class FreqDatabase {
    public static final int MAX_LENGTH = 108;
    private static final int[] array = new int[MAX_LENGTH];

    public static void loadFreqDatabase(InputStream file){
        try(BufferedReader br = new BufferedReader(new InputStreamReader(file))) {
            try {
                for(String line; (line = br.readLine()) != null; ) {
                    line=line.trim();
                    String id ="";
                    String freq = "";
                    id=line.substring(0,line.indexOf(' '));
                    freq=line.substring(line.lastIndexOf(' ')+1);
                    int index = Integer.parseInt(id);
                    if(index> MAX_LENGTH -1){
                        continue;
                    }
                    array[index]=(int)Math.round(Double.parseDouble(freq));

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // line is not visible here.
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static void print(){
        System.out.println("Freq Database:");
        System.out.println("==============");
        for (int i = 0; i < array.length; i++) {
            System.out.println(i+": "+array[i]);
        }
    }
    public static int getFrequency(int index){
        if(index>= MAX_LENGTH)
            return -1;
        return array[index];
    }
}
