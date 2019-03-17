package cs.cooble;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by Matej on 31.12.2017.
 */
public class Try {
    public static final String TARGET_FOLDER="C:\\Users\\Matej\\Desktop\\sstcMusic";
    public static final String TARGET_NAME="try";

    public static void main(String[] args){
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(TARGET_FOLDER+"/"+TARGET_NAME+".txt");
            out.write(131);
            out.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        FileInputStream in = null;
        try {
            in = new FileInputStream(TARGET_FOLDER+"/"+TARGET_NAME+".txt");
            System.out.println(in.read());
            in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
