package formats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class MBlock extends MCode {
    private final HashMap<String, VarInfo> params;
    private final HashMap<String, VarInfo> vars;
    private final ArrayList<MCode> mcodes;
    
    private final MBlock father;
    // for MIPS
    private BlockWhileRunning block;
    private final HashMap<String, String> labelContentMap;
    
    private final HashMap<String, String> tmpRegMap;
    private final LinkedList<String> freeRegs;
    
    public MBlock(MBlock father) {
        params = new HashMap<>();
        vars = new HashMap<>();
        mcodes = new ArrayList<>();
        this.father = father;
        // block = new BlockWhileRunning();
        labelContentMap = new HashMap<>();
        if (father != null) {
            father.addMBlock(this);
        }
        tmpRegMap = new HashMap<>();
        freeRegs = new LinkedList<>();
        for (int i = 0; i < 8; i++) {
            freeRegs.addLast("$t" + i);
        }
        for (int i = 0; i < 8; i++) {
            freeRegs.addLast("$s" + i);
        }
    }
    
    private void addMBlock(MBlock mBlock) {
        mcodes.add(mBlock);
    }
    
    public MBlock getFather() {
        return father;
    }
    
    public VarInfo getName(String name) {
        return (params.get(name) != null) ? params.get(name) : vars.get(name);
    }
    
    public void addParam(VarInfo info) {
        vars.put(info.getName(), info);
        Quaternion quaternion;
        // 注意：这里PARA后面的xx#yy，暂时啥也没传，只传名字。
        if (info.getDimen() == 1) {
            quaternion = new Quaternion("PARA", "arr_" + info.getType(), info.getName(), "");
        } else if (info.getDimen() == 2) {
            quaternion = new Quaternion("PARA", "arr_" + info.getType(), info.getName(), "");
        } else {
            quaternion = new Quaternion("PARA", info.getType(), info.getName(), "");
        }
        mcodes.add(quaternion);
    }
    
    public void addVar(VarInfo info) {
        vars.put(info.getName(), info);
        // 常量只需要存在于编译程序中，无需在生成代码中体现
        // 相当于，生成中间代码后，const已经没用了
        if (!info.getIsConst()) {
            Quaternion quaternion;
            if (info.getDimen() == 1) {
                quaternion = new Quaternion("DECL", (info.getIsGlobal() ? "g_" : "") + info.getType(), info.getName() + "#" + info.getBound1(), "");
            } else if (info.getDimen() == 2) {
                quaternion = new Quaternion("DECL", (info.getIsGlobal() ? "g_" : "") + info.getType(), info.getName() + "#" + (info.getBound1() * info.getBound2()), "");
            } else {
                quaternion = new Quaternion("DECL", (info.getIsGlobal() ? "g_" : "") + info.getType(), info.getName(), "");
            }
            mcodes.add(quaternion);
        } else {
            Quaternion quaternion;
            if (info.getDimen() == 1) {
                quaternion = new Quaternion("DECL", (info.getIsGlobal() ? "gc_" : "") + info.getType(), info.getName() + "#" + info.getBound1(), "");
                mcodes.add(quaternion);
            } else if (info.getDimen() == 2) {
                quaternion = new Quaternion("DECL", (info.getIsGlobal() ? "gc_" : "") + info.getType(), info.getName() + "#" + (info.getBound1() * info.getBound2()), "");
                mcodes.add(quaternion);
            } /*else {
                quaternion = new Quaternion("DECL", (info.getIsGlobal() ? "g_" : "") + info.getType(), info.getName(), "");
            }*/
            
        }
    }
    
    public void addQuaternion(Quaternion q) {
        mcodes.add(q);
    }
    
    public void addSonQuaternion(Quaternion q) {
        ((MBlock) mcodes.get(mcodes.size() - 1)).addQuaternion(q);
    }
    
    public int toMIPS(ArrayList<String> dataSeg, ArrayList<String> textSeg, int baseLabelIndex, int varIndexInsideBlock, boolean inMain) {
        this.block = new BlockWhileRunning(varIndexInsideBlock);
        int curLabelIndex = baseLabelIndex;
        boolean pushed = false;
        boolean nextIsFunc = false;
        for (int j= 0; j < mcodes.size(); j++) {
            MCode mcode = mcodes.get(j);
            if (mcode instanceof Quaternion) {
                Quaternion q = (Quaternion) mcode;
                String op = q.getOp();
                String op1 = getReg(q.getOp1());
                String op2 = getReg(q.getOp2());
                String res = getReg(q.getRes());
                nextIsFunc = op.equals("FUNC");
                String name;
                String offset;
                switch (op) {
                    case "DECL":
                        // var
                        if (op1.charAt(0) == 'g') {
                            block.pushVar(op2, false);
                            String[] tmp = op2.split("#");
                            int size = tmp.length == 1 ? 4 : Integer.parseInt(tmp[1]) * 4;
                            labelContentMap.put(op2, String.valueOf(size));
                            dataSeg.add("\t" + op2.split("#")[0] + ": .space " + labelContentMap.get(op2));
                        } else {
                            int size = block.pushVar(op2, false);
                            textSeg.add("\tsubu $sp $sp " + size);
                        }
                        break;
                    case "FUNC":
                        textSeg.add(op2 + ":");
                        if (!Objects.equals(op2, "main")) {
                            textSeg.add("\tlw $t9 -4($fp)");
                            textSeg.add("\tsw $ra 0($t9)");
                        } else {
                            inMain = true;
                        }
                        break;
                    case "PARA":
                        // var
                        block.pushVar(op2, op1.charAt(0) == 'a');
                        break;
                    case "ASSIGN":
                        // only -> reg
                        if (op1.equals("RET")) {
                            // RET -> reg
                            textSeg.add("\tmove " + res + " $v0");
                        } else if (op1.charAt(0) == '$') {
                            // reg -> reg
                            textSeg.add("\tmove " + res + " " + op1);
                            releaseReg(q.getOp1());
                        } else {
                            // num -> reg
                            textSeg.add("\tli " + res + " " + op1);
                        }
                        break;
                    // 问题：
                    // 存地址的内存单元，a#1中1是给地址加载出来之后的偏移量用的（getOffset加载的是地址的基地址，不能直接解析）；
                    // 存数组本身的内存单元，a#1中1是给getOffset直接使用的（可以返回叠加偏移量的整体offset）
                    // 考虑：要不要都在外面解析？
                    // 最终选择：都在外面解析。
                    
                    case "LOAD":
                        // var -> reg
                        name = parseName(op1);
                        offset = parseNormalOffset(op1);
                        if (getIsGlobal(name)) {
                            // global var
                            textSeg.add("\tla $t9 " + name);
                            if (offset.charAt(0) == '$') {
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu $t9 $t9 " + offset);
                                textSeg.add("\tlw " + res + " 0($t9)");
                            } else {
                                textSeg.add("\tlw " + res + " " + offset + "($t9)");
                            }
                        } else if (getStoreIsAddr(name)) {
                            int aoffset = getVarOffset(name);
                            String newReg = newReg();
                            textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                            textSeg.add("\tlw " + newReg + " " + aoffset + "($t9)"); // newReg存的是倒数组基地址
                            if (offset.charAt(0) == '$') {
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu " + newReg + " " + newReg + " " + offset);
                                textSeg.add("\tlw " + res + " 0(" + newReg + ")");
                            } else {
                                textSeg.add("\tlw " + res + " " + offset + "(" + newReg + ")");
                            }
                        } else {
                            if (offset.charAt(0) == '$') {
                                int aoffset = getVarOffset(name);
                                textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu $t9 $t9 " + offset);
                                textSeg.add("\tlw " + res + " " + aoffset + "($t9)");
                            } else {
                                int aoffset = getVarOffset(name) + Integer.parseInt(offset);
                                textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                                textSeg.add("\tlw " + res + " " + aoffset + "($t9)");
                            }
                        }
                        if (offset.charAt(0) == '$') {
                            releaseReg(op1.split("#")[1]);
                        }
                        break;
                    case "STORE":
                        // reg -> var
                        name = parseName(res);
                        offset = parseNormalOffset(res);
                        if (getIsGlobal(name)) {
                            // global var
                            textSeg.add("\tla $t9 " + name);
                            if (offset.charAt(0) == '$') {
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu $t9 $t9 " + offset);
                                textSeg.add("\tsw " + op1 + " 0($t9)");
                            } else {
                                textSeg.add("\tsw " + op1 + " " + offset + "($t9)");
                            }
                        } else if (getStoreIsAddr(name)) {
                            int soffset = getVarOffset(name);
                            String newReg = newReg();
                            textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                            textSeg.add("\tlw " + newReg + " " + soffset + "($t9)");
                            if (offset.charAt(0) == '$') {
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu " + newReg + " " + newReg + " " + offset);
                                textSeg.add("\tsw " + op1 + " 0(" + newReg + ")");
                            } else {
                                textSeg.add("\tsw " + op1 + " " + offset + "(" + newReg + ")");
                            }
                        } else {
                            if (offset.charAt(0) == '$') {
                                int soffset = getVarOffset(name);
                                textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu $t9 $t9 " + offset);
                                textSeg.add("\tsw " + op1 + " " + soffset + "($t9)");
                            } else {
                                int sOffset = getVarOffset(name) + Integer.parseInt(offset);
                                textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                                textSeg.add("\tsw " + op1 + " " + sOffset + "($t9)");
                            }
                        }
                        releaseReg(q.getOp1());
                        if (offset.charAt(0) == '$') {
                            releaseReg(res.split("#")[1]);
                        }
                        break;
                    case "LADDR":
                        // var -> reg
                        name = parseName(op1);
                        offset = parseNormalOffset(op1);
                        if (getIsGlobal(name)) {
                            // global var
                            textSeg.add("\tla $t9 " + name);
                            if (offset.charAt(0) == '$') {
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu " + res + " $t9 " + offset);
                            } else {
                                textSeg.add("\taddi " + res + " $t9 " + offset);
                            }
                        } else if (getStoreIsAddr(name)) {
                            // 问题：未考虑传入数组地址为全局变量的问题（需要地址增）
                            int loffset = getVarOffset(name);
                            textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                            textSeg.add("\tlw " + res + " " + loffset + "($t9)"); // res存储的是目标数组基地址
                            if (offset.charAt(0) == '$') {
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu " + res + " " + res + " " + offset);
                            } else {
                                textSeg.add("\taddi " + res + " " + res + " " + offset);
                            }
                        } else {
                            if (offset.charAt(0) == '$') {
                                int loffset = getVarOffset(name);
                                textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                                textSeg.add("\tsll " + offset + " " + offset + " " + 2);
                                textSeg.add("\taddu $t9 $t9 " + offset);
                                textSeg.add("\taddi " + res + " $t9 " + loffset);
                            } else {
                                int loffset = getVarOffset(name) + Integer.parseInt(offset);
                                textSeg.add("\tlw $t9 " + (-4) + "($fp)");
                                textSeg.add("\taddi " + res + " $t9 " + loffset); // loffset有符号
                            }
                        }
                        if (offset.charAt(0) == '$') {
                            releaseReg(op1.split("#")[1]);
                        }
                        break;
                    case "PUSH":
                        // only reg / num
                        if (!pushed) {
                            textSeg.add("\tsw $sp 0($fp)");
                            textSeg.add("\tsubu $sp $sp 68"); // 留出ra、寄存器位置
                        }
                        pushed = true;
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tsw " + op1 + " 0($sp)");
                            releaseReg(q.getOp1());
                        } else {
                            String newReg = newReg();
                            textSeg.add("\tli " + newReg + " " + op1);
                            textSeg.add("\tsw " + newReg + " 0($sp)");
                        }
                        textSeg.add("\tsubu $sp $sp 4");
                        break;
                    case "CALL":
                        if (!Objects.equals(op1, "main")) {
                            if (!pushed) { // no param unpushed
                                textSeg.add("\tsw $sp 0($fp)");
                                textSeg.add("\tsubu $sp $sp 68"); // 留出ra、寄存器位置
                            }
                            textSeg.add("\taddu $fp $fp 4");
                            pushed = false;
    
                            textSeg.add("\tlw $t9 -4($fp)");
                            // 压栈所有寄存器
                            for (int i = 0; i < 8; i++) {
                                textSeg.add("\tsw $t" + i + " -" + (4 + i * 8) + "($t9)");
                                textSeg.add("\tsw $s" + i + " -" + (8 + i * 8) + "($t9)");
                            }
                        } else {
                            textSeg.add("\tsw $sp 0($fp)");
                            textSeg.add("\tsubu $sp $sp 68"); // 留出ra、寄存器位置 ?
                            textSeg.add("\taddu $fp $fp 4");
                        }
                        textSeg.add("\tjal " + op1);
                        break;
                    case "RETURN":
                        // only reg / num / <empty>
                        if (inMain) {
                            textSeg.add("\tli $v0 10");
                            textSeg.add("\tsyscall");
                            // 最后一个一定是int main，不用更新inMain了
                        } else {
                            if (!op1.isEmpty()) {
                                if (op1.charAt(0) == '$') {
                                    textSeg.add("\tmove $v0 " + op1);
                                    releaseReg(q.getOp1());
                                } else {
                                    textSeg.add("\tli $v0 " + op1);
                                }
                            }
                            // 恢复现场
                            textSeg.add("\tlw $t9 -4($fp)");
                            textSeg.add("\tlw $ra 0($t9)");
                            for (int i = 0; i < 8; i++) {
                                textSeg.add("\tlw $t" + i + " -" + (4 + i * 8) + "($t9)");
                                textSeg.add("\tlw $s" + i + " -" + (8 + i * 8) + "($t9)");
                            }
                            textSeg.add("\tsubu $fp $fp 4");
                            textSeg.add("\tlw $sp 0($fp)");
                            textSeg.add("\tjr $ra");
                        }
                        break;
                    case "PRINTF":
                        textSeg.add("\tli $v0 1");
                        // only reg / num
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tmove $a0 " + op1);
                            releaseReg(q.getOp1());
                        } else {
                            textSeg.add("\tli $a0 " + op1);
                        }
                        textSeg.add("\tsyscall");
                        break;
                    case "PRINTS":
                        String label = newLabel(curLabelIndex++);
                        labelContentMap.put(label, op1);
                        dataSeg.add("\t" + label + ": .asciiz \"" + labelContentMap.get(label) + "\"");
                        textSeg.add("\tli $v0 4");
                        textSeg.add("\tla $a0 " + label);
                        textSeg.add("\tsyscall");
                        break;
                    case "SCANF":
                        // only reg
                        textSeg.add("\tli $v0 5");
                        textSeg.add("\tsyscall");
                        textSeg.add("\tmove " + op1 + " $v0");
                        // cant release
                        break;
                    case "NOT":
                        // reg -> reg
                        textSeg.add("\tseq " + res + " " + op1 + " " + 0);
                        releaseReg(q.getOp1());
                        break;
                    case "ADD":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\taddu " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            textSeg.add("\taddi " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "SUB":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tsubu " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            String newReg = newReg();
                            textSeg.add("\tli " + newReg + " " + op1);
                            textSeg.add("\tsubu " + res + " " + newReg + " " + op2);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "MUL":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tmul " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            textSeg.add("\tmul " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "DIV":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tdiv " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            String newReg = newReg();
                            textSeg.add("\tli " + newReg + " " + op1);
                            textSeg.add("\tdiv " + res + " " + newReg + " " + op2);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "MOD":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            if (op2.charAt(0) == '$') {
                                textSeg.add("\tdiv " + op1 + " " + op2);
                                releaseReg(q.getOp1());
                                releaseReg(q.getOp2());
                            } else {
                                String newReg = newReg();
                                textSeg.add("li " + newReg + " " + op2);
                                textSeg.add("\tdiv " + op1 + " " + newReg);
                                releaseReg(q.getOp1());
                            }
                        } else {
                            String newReg = newReg();
                            textSeg.add("\tli " + newReg + " " + op1);
                            textSeg.add("\tdiv " + newReg + " " + op2);
                            releaseReg(q.getOp2());
                        }
                        textSeg.add("\tmfhi " + res);
                        break;
                    case "LABEL":
                        textSeg.add(op1 + ":");
                        break;
                    case "SLT":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            if (op2.charAt(0) == '$') {
                                textSeg.add("\tslt " + res + " " + op1 + " " + op2);
                                releaseReg(q.getOp1());
                                releaseReg(q.getOp2());
                            } else {
                                textSeg.add("\tslti " + res + " " + op1 + " " + op2);
                                releaseReg(q.getOp1());
                            }
                        } else {
                            textSeg.add("\tsgt " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "SGT":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tsgt " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            textSeg.add("\tslti " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "SLE":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tsle " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            textSeg.add("\tsge " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "SGE":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tsge " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            textSeg.add("\tsle " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "SEQ":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tseq " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            textSeg.add("\tseq " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "SNE":
                        // reg / num + reg / num -> reg
                        if (op1.charAt(0) == '$') {
                            textSeg.add("\tsne " + res + " " + op1 + " " + op2);
                            releaseReg(q.getOp1());
                            if (op2.charAt(0) == '$') {
                                releaseReg(q.getOp2());
                            }
                        } else {
                            textSeg.add("\tsne " + res + " " + op2 + " " + op1);
                            releaseReg(q.getOp2());
                        }
                        break;
                    case "BEQ":
                        // reg + num -> label
                        textSeg.add("\tbeq " + op1 + " " + op2 + " " + res);
                        if (j != mcodes.size() - 1 && mcodes.get(j+1) instanceof Quaternion &&
                                Objects.equals(((Quaternion) mcodes.get(j + 1)).getOp(), "BEQ") &&
                                Objects.equals(((Quaternion) mcodes.get(j + 1)).getOp1(), ((Quaternion) mcodes.get(j)).getOp1())) {
                        } else {
                            releaseReg(q.getOp1());
                        }
                        break;
                    case "JUMP":
                        // label
                        textSeg.add("\tj " + op1);
                        break;
                    default:
                        break;
                }
            } else {
                int varIndexToPush = nextIsFunc ? 0 : block.getIndex();
                curLabelIndex = ((MBlock) mcode).toMIPS(dataSeg, textSeg,curLabelIndex, varIndexToPush, inMain);
                nextIsFunc = false;
            }
        }
        return curLabelIndex; // return global labelIndex
    }
    
    private String parseNormalOffset(String op1) {
        String[] tmp = op1.split("#");
        if (tmp.length > 1 && tmp[1].charAt(0) == '$') {
            return getReg(tmp[1]); // 下标索引是寄存器
        } else {
            return String.valueOf(tmp.length == 1 ? 0 : 4 * Integer.parseInt(tmp[1]));
        }
    }
    
    private String parseName(String op1) {
        String[] tmp = op1.split("#");
        return tmp[0];
    }
    
    private String newLabel(int curLabelIndex) {
        return "label_" + (curLabelIndex);
    }
    
    private String newReg() {
        return "$t8";
    }
    
    private String getReg(String tmp) {
        if (!tmp.isEmpty() && tmp.charAt(0) == '$') {
            String reg;
            if (tmpRegMap.get(tmp) == null) {
                // allocate new Reg
                reg = getFreeReg(freeRegs);
                tmpRegMap.put(tmp, reg);
                return reg;
            } else {
                // return allocated Reg
                reg = tmpRegMap.get(tmp);
                return reg;
            }
        } else {
            return tmp;
        }
    }
    
    private String getFreeReg(LinkedList<String> freeRegs) {
        return freeRegs.removeFirst();
    }
    
    private void releaseReg(String tmp) {
        String reg = tmpRegMap.get(tmp);
        tmpRegMap.remove(tmp);
        freeRegs.addLast(reg);
        
    }
    
    private boolean getStoreIsAddr(String name) {
        // getStoreIsAddr返回所索引的名字是否（对应位置存储的是地址）
        if (block.contains(name)) {
            return block.getStoreIsAddr(name);
        } else {
            return father.getStoreIsAddr(name);
        }
    }
    
    private int getVarOffset(String name) {
        // getOffset无论是数组还是变量，均返回基地址
        // 注意：传入的只有名字。没有#下标！
        if (block.contains(name)) {
            return - block.getOffset(name);
        } else {
            return father.getVarOffset(name);
        }
    }
    
    private boolean getIsGlobal(String name) {
        if (block.contains(name)) {
            return father == null;
        } else {
            return father.getIsGlobal(name);
        }
    }
    
    public void print() {
        for (MCode mcode : mcodes) {
            mcode.print();
        }
    }
}
