package Exceptions;

import formats.ExceptionType;

import java.io.BufferedWriter;
import java.io.IOException;

public class CompilerException {
    private final boolean debug;
    private final int line;
    private final ExceptionType type;
    
    public CompilerException(int line, ExceptionType type) {
        debug = false;
        this.line = line;
        this.type = type;
    }
    
    public void printMessage(BufferedWriter bw) {
        String printType;
        switch (type) {
            case ILLEGALCHAR:
                printType = "a";
                break;
            case IDENTDOUBLED:
                printType = "b";
                break;
            case IDENTUNDEFINED:
                printType = "c";
                break;
            case PARAMSCNTUNMATCHED:
                printType = "d";
                break;
            case PARAMSTYPEUNMATCHED:
                printType = "e";
                break;
            case RETURNVARINVOIDFUNC:
                printType = "f";
                break;
            case RETURNMISSINGINNONVOIDFUNC:
                printType = "g";
                break;
            case CONSTVARALTERED:
                printType = "h";
                break;
            case SEMICOLONMISSING:
                printType = "i";
                break;
            case RPARENTMISSING:
                printType = "j";
                break;
            case RBRACKMISSING:
                printType = "k";
                break;
            case PRINTFEXPCNTUNMATCHED:
                printType = "l";
                break;
            case BREAKCONTINUEINNONLOOP:
                printType = "m";
                break;
            default:
                printType = "unknown";
        }
        try {
            if (debug) {
                System.out.println(line + " " + printType);
            } else {
                bw.write(line + " " + printType + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
