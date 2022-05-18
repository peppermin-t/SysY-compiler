import formats.WordType;

import java.util.HashMap;

public class Dict {
    private static Dict instance = null;
    private final HashMap<String, WordType> typeHashMap;
    
    private Dict() {
        typeHashMap = new HashMap<>();
        typeHashMap.put("main", WordType.MAINTK);
        typeHashMap.put("const", WordType.CONSTTK);
        typeHashMap.put("int", WordType.INTTK);
        typeHashMap.put("break", WordType.BREAKTK);
        typeHashMap.put("continue", WordType.CONTINUETK);
        typeHashMap.put("if", WordType.IFTK);
        typeHashMap.put("else", WordType.ELSETK);
        typeHashMap.put("while", WordType.WHILETK);
        typeHashMap.put("getint", WordType.GETINTTK);
        typeHashMap.put("printf", WordType.PRINTFTK);
        typeHashMap.put("return", WordType.RETURNTK);
        typeHashMap.put("void", WordType.VOIDTK);
    }
    
    public static Dict getInstance() {
        if (instance == null) {
            instance = new Dict();
        }
        return instance;
    }
    
    public WordType getType(String target) {
        return typeHashMap.get(target);
    }
    
}
