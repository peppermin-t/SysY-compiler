package formats;

import java.util.ArrayList;

public class VarInfo {
    private final String name;
    private final String type;
    private final boolean isConst;
    // private boolean constArrNeedDisplay;
    private final int dimen;
    private final int bound1;
    private final int bound2;
    private ArrayList<Integer> vals;
    private boolean isGlobal;
    
    public VarInfo(String name, String type, boolean isConst, boolean isGlobal) {
        this.name = name;
        this.type = type;
        this.isConst = isConst;
        this.dimen = 0;
        this.bound1 = -1;
        this.bound2 = -1; // for 0 dimen
        this.isGlobal = isGlobal;
        if (isConst) {
            vals = new ArrayList<>();
        }
    }
    
    public VarInfo(String name, String type, boolean isConst, boolean isGlobal, int bound1) {
        this.name = name;
        this.type = type;
        this.isConst = isConst;
        this.dimen = 1;
        this.bound1 = bound1;
        this.bound2 = -1;
        this.isGlobal = isGlobal;
        if (isConst) {
            vals = new ArrayList<>();
        }
    }
    
    public VarInfo(String name, String type, boolean isConst, boolean isGlobal, int bound1, int bound2) {
        this.name = name;
        this.type = type;
        this.isConst = isConst;
        this.dimen = 2;
        this.bound1 = bound1;
        this.bound2 = bound2;
        this.isGlobal = isGlobal;
        if (isConst) {
            vals = new ArrayList<>();
        }
    }
    
    public void setVal(int val) {
        if (isConst) {
            this.vals.add(0, val);
        }
    }
    
    public void setVal(int loc, int val) {
        if (isConst) {
            for (int i = vals.size(); i != loc; i++) {
                this.vals.add(i, 0);
            }
            this.vals.add(loc, val);
        }
    }
    
    public int getVal() {
        return vals.isEmpty() ? 0 : vals.get(0);
    }
    
    public int getVal(int i1) {
        return i1 >= vals.size() ? 0 : vals.get(i1);
    }
    
    public String getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
    public int getDimen() {
        return dimen;
    }
    
    public int getBound1() {
        return bound1;
    }
    
    public int getBound2() {
        return bound2;
    }
    
    public boolean getIsConst() {
        return isConst;
    }
    
    public boolean getIsGlobal() {
        return isGlobal;
    }
}
