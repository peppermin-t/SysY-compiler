package formats;

import java.util.ArrayList;

public class SymbolAttributes implements Cloneable {
    private int identLine;
    private String name; // real_name(func and var), while(only for while)
    private boolean isConst; // true(only for var), false
    private String type; // int, void(only for func_kind)
    private int dimension; // 0, 1(not for func), 2(not for func)
    private ArrayList<Integer> formalParams; // only recording dimensions of each param
    
    public SymbolAttributes() {
        this.identLine = -1;
        this.isConst = false;
        this.dimension = 0;
        this.formalParams = new ArrayList<>();
    }
    
    public int getIdentLine() {
        return identLine;
    }
    
    public String getName() {
        return name;
    }
    
    public boolean getIsConst() {
        return isConst;
    }
    
    public String getType() {
        return type;
    }
    
    public int getDimension() {
        return dimension;
    }
    
    public ArrayList<Integer> getFormalParams() {
        return formalParams;
    }
    
    public void setIdentLine(int line) {
        this.identLine = line;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public void setConst(boolean aConst) {
        isConst = aConst;
    }
    
    public void setType(String type) {
        this.type = type;
    }
    
    public void setDimension(int dimension) {
        this.dimension = dimension;
    }
    
    public void plusDimension() {
        this.dimension = this.dimension + 1;
    }
    
    public void minusDimension() {
        this.dimension = this.dimension - 1;
    }
    
    public SymbolAttributes clone() {
        try {
            return (SymbolAttributes) super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}
