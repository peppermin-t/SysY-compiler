package formats;

public class WordInfo {
    private final String token;
    private final WordType type;
    private final int line;
    
    public WordInfo(String token, WordType type, int line) {
        this.token = token;
        this.type = type;
        this.line = line;
    }
    
    public String getToken() {
        return token;
    }
    
    public WordType getType() {
        return type;
    }
    
    public int getLine() {
        return line;
    }
    
    /*public void setToken(String token) {
        this.token = token;
    }*/
}
