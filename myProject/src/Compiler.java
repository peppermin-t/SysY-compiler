import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Compiler {
    public static void main(String[] args) throws IOException {
        String source = readSource();
        
        Lexer lexer = new Lexer();
        lexer.dispose(source);
        // lexer.printTokenList();
        Parser parser = new Parser(lexer.getWordList());
        parser.dispose();
        // parser.printOutput();
        MCodeGenerator generator1 = new MCodeGenerator(lexer.getWordList());
        generator1.generate();
        generator1.printMCode();
        MIPSGenerator generator2 = new MIPSGenerator(generator1.getMCodeRoot());
        generator2.generate();
        generator2.printMIPS();
    }
    
    private static String readSource() throws IOException {
        FileReader fr = new FileReader("testfile.txt");
        BufferedReader bf = new BufferedReader(fr);
        StringBuilder source = new StringBuilder();
        
        int c;
        while ((c = bf.read()) != -1) {
            source.append(Character.toChars(c)[0]);
        }
        
        bf.close();
        fr.close();
        return source.toString();
    }
}
