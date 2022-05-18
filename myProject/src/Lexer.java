import formats.WordInfo;
import formats.WordType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class Lexer {
    private int buf;
    private int line;
    private final ArrayList<WordInfo> wordList;
    
    public Lexer() {
        buf = 0;
        line = 1;
        wordList = new ArrayList<>();
    }
    
    public void dispose(String source) {
        while (true) {
            int flag = getWord(source);
            if (flag == -1) {
                break;
            }
            
        }
    }
    
    private int getWord(String source) {
        StringBuilder tk = new StringBuilder();
        String token;
        WordType type;
        
        while (!overFlowed(source) && isEmptyChar(source.charAt(buf))) {
            if (source.charAt(buf) == '\n')
                line++;
            buf++;
        }
        if (overFlowed(source)) {
            return -1;
        }
        
        if (Character.isLetter(source.charAt(buf)) || source.charAt(buf) == '_') {
            while ((Character.isLetterOrDigit(source.charAt(buf)) || source.charAt(buf) == '_') && !overFlowed(source)) {
                tk.append(source.charAt(buf++));
            }
            token = tk.toString();
            
            if ((type = Dict.getInstance().getType(token)) != null) {
                // 为关键字
                wordList.add(new WordInfo(token, type, line));
            } else {
                // 为ident
                wordList.add(new WordInfo(token, WordType.IDENFR, line));
            }
        } else if (Character.isDigit(source.charAt(buf))) {
            // 整型常数
            while (Character.isDigit(source.charAt(buf)) && !overFlowed(source)) {
                tk.append(source.charAt(buf++));
            }
            token = tk.toString();
            wordList.add(new WordInfo(token, WordType.INTCON, line));
        } else if (source.charAt(buf) == '"') {
            // 字符串常量
            tk.append(source.charAt(buf++));
            while (source.charAt(buf) != '"') {
                tk.append(source.charAt(buf++));
                if (overFlowed(source)) {
                    // error();
                }
            }
            tk.append(source.charAt(buf++));
            token = tk.toString();
            wordList.add(new WordInfo(token, WordType.STRCON, line));
        } else if (source.charAt(buf) == '!') {
            buf++;
            if (!overFlowed(source) && source.charAt(buf) == '=') {
                buf++;
                wordList.add(new WordInfo("!=", WordType.NEQ, line));
            } else {
                wordList.add(new WordInfo("!", WordType.NOT, line));
            }
        } else if (source.charAt(buf) == '&') {
            buf++;
            if (overFlowed(source) || source.charAt(buf) != '&') {
                // error();
            } else {
                buf++;
                wordList.add(new WordInfo("&&", WordType.AND, line));
            }
        } else if (source.charAt(buf) == '|') {
            buf++;
            if (overFlowed(source) || source.charAt(buf) != '|') {
                // error();
            } else {
                buf++;
                wordList.add(new WordInfo("||", WordType.OR, line));
            }
        } else if (source.charAt(buf) == '+') {
            buf++;
            wordList.add(new WordInfo("+", WordType.PLUS, line));
        } else if (source.charAt(buf) == '-') {
            buf++;
            wordList.add(new WordInfo("-", WordType.MINU, line));
        } else if (source.charAt(buf) == '*') {
            buf++;
            wordList.add(new WordInfo("*", WordType.MULT, line));
        } else if (source.charAt(buf) == '/') {
            buf++;
            if (overFlowed(source) || source.charAt(buf) != '/' && source.charAt(buf) != '*') {
                wordList.add(new WordInfo("/", WordType.DIV, line));
            } else if (source.charAt(buf) == '/') {
                buf++;
                while (!overFlowed(source) && source.charAt(buf) != '\n') {
                    buf++;
                }
            } else if (source.charAt(buf) == '*') {
                do {
                    do {
                        buf++;
                        if (overFlowed(source)) {
                            // error();
                        } else if (source.charAt(buf) == '\n') {
                            line++;
                        }
                    } while (source.charAt(buf) != '*');
                    do {
                        buf++;
                        if (overFlowed(source)) {
                            // error();
                        } else if (source.charAt(buf) == '/') {
                            buf++;
                            return 0;
                        }
                    } while (source.charAt(buf) == '*');
                } while (source.charAt(buf) != '*');
            }
        } else if (source.charAt(buf) == '%') {
            buf++;
            wordList.add(new WordInfo("%", WordType.MOD, line));
        } else if (source.charAt(buf) == '<') {
            buf++;
            if (!overFlowed(source) && source.charAt(buf) == '=') {
                buf++;
                wordList.add(new WordInfo("<=", WordType.LEQ, line));
            } else {
                wordList.add(new WordInfo("<", WordType.LSS, line));
            }
        } else if (source.charAt(buf) == '>') {
            buf++;
            if (!overFlowed(source) && source.charAt(buf) == '=') {
                buf++;
                wordList.add(new WordInfo(">=", WordType.GEQ, line));
            } else {
                wordList.add(new WordInfo(">", WordType.GRE, line));
            }
        } else if (source.charAt(buf) == '=') {
            buf++;
            if (!overFlowed(source) && source.charAt(buf) == '=') {
                buf++;
                wordList.add(new WordInfo("==", WordType.EQL, line));
            } else {
                wordList.add(new WordInfo("=", WordType.ASSIGN, line));
            }
        } else if (source.charAt(buf) == ';') {
            buf++;
            wordList.add(new WordInfo(";", WordType.SEMICN, line));
        } else if (source.charAt(buf) == ',') {
            buf++;
            wordList.add(new WordInfo(",", WordType.COMMA, line));
        } else if (source.charAt(buf) == '(') {
            buf++;
            wordList.add(new WordInfo("(", WordType.LPARENT, line));
        } else if (source.charAt(buf) == ')') {
            buf++;
            wordList.add(new WordInfo(")", WordType.RPARENT, line));
        } else if (source.charAt(buf) == '[') {
            buf++;
            wordList.add(new WordInfo("[", WordType.LBRACK, line));
        } else if (source.charAt(buf) == ']') {
            buf++;
            wordList.add(new WordInfo("]", WordType.RBRACK, line));
        } else if (source.charAt(buf) == '{') {
            buf++;
            wordList.add(new WordInfo("{", WordType.LBRACE, line));
        } else if (source.charAt(buf) == '}') {
            buf++;
            wordList.add(new WordInfo("}", WordType.RBRACE, line));
        } else {
            // error();
        }
        
        return 0;
    }
    
    public void printTokenList() throws IOException {
        FileWriter fw = new FileWriter("out_1.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        for (WordInfo wordInfo : wordList) {
            bw.write(wordInfo.getType().toString() + " " + wordInfo.getToken() + "\n");
        }
        bw.flush();
        bw.close();
        fw.close();
    }
    
    public ArrayList<WordInfo> getWordList() {
        return wordList;
    }
    
    private boolean isEmptyChar(char c) {
        return c == ' ' || c == '\n' || c == '\r' || c == '\t';
    }
    
    private boolean overFlowed(String s) {
        return buf >= s.length();
    }
}
