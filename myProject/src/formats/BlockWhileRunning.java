package formats;

import java.util.HashMap;

public class BlockWhileRunning {
    private int index;
    private final HashMap<String, Integer> varOffsets;
    private final HashMap<String, Integer> addrVarOffsets;
    
    public BlockWhileRunning(int varStartIndex) {
        index = varStartIndex == 0 ? 4 + 64 : varStartIndex;
        this.varOffsets = new HashMap<>();
        this.addrVarOffsets = new HashMap<>();
    }
    
    public int pushVar(String name, boolean needStoreAddr) {
        String[] tmp = name.split("#");
        int size = tmp.length == 1 ? 4 : 4 * Integer.parseInt(tmp[1]);
        if (needStoreAddr) {
            addrVarOffsets.put(tmp[0], index + size - 4);
        } else {
            varOffsets.put(tmp[0], index + size - 4);
        }
        index += size;
        return size;
    }
    
    public boolean getStoreIsAddr(String name) {
        return addrVarOffsets.containsKey(name);
    }
    
    public int getOffset(String name) {
        if (varOffsets.containsKey(name)) {
            return varOffsets.get(name);
        } else {
            return addrVarOffsets.get(name);
        }
    }
    
    public boolean contains(String name) {
        return varOffsets.containsKey(name) || addrVarOffsets.containsKey(name);
    }
    
    public int getIndex() {
        return index;
    }
}
