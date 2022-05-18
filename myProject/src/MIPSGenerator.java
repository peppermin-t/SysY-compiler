import formats.MBlock;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MIPSGenerator {
    private final MBlock mRoot;
    private BufferedWriter bw;
    private ArrayList<String> dataSeg;
    private ArrayList<String> textSeg;
    
    public MIPSGenerator(MBlock root) {
        try {
            bw = new BufferedWriter(new FileWriter("mips.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.mRoot = root;
        dataSeg = new ArrayList<>();
        textSeg = new ArrayList<>();
    }
    
    public void generate() {
        mRoot.toMIPS(dataSeg, textSeg,0, 0, false);
    }
    
    public void printMIPS() {
        write(".data");
        for (String data : dataSeg) {
            write(data);
        }
        write(".text");
        write("\tli $fp 0x10040000");
        for (String text : textSeg) {
            write(text);
        }
        try {
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void write(String s) {
        boolean debug = false;
        if (debug) {
            System.out.println(s);
        } else {
            try {
                bw.write(s + '\n');
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
