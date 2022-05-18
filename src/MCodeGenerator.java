import formats.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Objects;

public class MCodeGenerator {
    // 本次递归下降默认已经是语义正确的程序
    private final ArrayList<WordInfo> wordList;
    private int buf;
    private WordInfo sym;
    private int tmpbuf;
    
    // symbol table (only dimen)
    private final LinkedList<HashMap<String, Integer>> blockStackDimen;
    
    private final MBlock root;
    private MBlock curBlock;
    
    private String storeRealVar;
    
    private int curLabelIndex;
    
    public MCodeGenerator(ArrayList<WordInfo> wordList) {
        this.wordList = wordList;
        buf = -1;
        sym = new WordInfo(null, null, -1);
        tmpbuf = 0;
        root = new MBlock(null);
        curBlock = root;
        curLabelIndex = 0;
        blockStackDimen = new LinkedList<>();
    }
    
    public void generate() {
        nextsym();
        blockStackDimen.addFirst(new HashMap<>());
        CompileUnit();
    }
    
    private void CompileUnit() {
        while (!(sym.getType() == WordType.VOIDTK ||
                (sym.getType() == WordType.INTTK &&
                        (getNextType(1) == WordType.MAINTK || getNextType(1) == WordType.IDENFR && getNextType(2) == WordType.LPARENT)))) {
            Decl(true);
        }
        Quaternion q = new Quaternion("CALL", "main", "", "");
        curBlock.addQuaternion(q);
        while (sym.getType() == WordType.VOIDTK ||
                sym.getType() == WordType.INTTK && getNextType(1) == WordType.IDENFR) {
            FuncDef();
        }
        MainFuncDef();
    }
    
    private void Decl(boolean isGlobal) {
        if (sym.getType() == WordType.CONSTTK) {
            ConstDecl(isGlobal);
        } else {
            VarDecl(isGlobal);
        }
    }
    
    private void ConstDecl(boolean isGlobal) {
        nextsym();
        String type = BType();
        ConstDef(type, isGlobal);
        while (sym.getType() == WordType.COMMA) {
            nextsym();
            ConstDef(type, isGlobal);
        }
        nextsym();
    }
    
    private String BType() {
        String name = sym.getToken();
        nextsym();
        return name;
    }
    
    private void ConstDef(String type, boolean isGlobal) {
        String name = sym.getToken();
        int dimen = 0;
        LinkedList<Integer> bounds = new LinkedList<>();
        nextsym();
        while (sym.getType() == WordType.LBRACK) {
            dimen++;
            nextsym();
            bounds.addLast(new Integer(ConstExp()));
            nextsym();
        }
        if (dimen == 0) {
            VarInfo info = new VarInfo(name, type, true, isGlobal);
            curBlock.addVar(info);
        } else if (dimen == 1) {
            VarInfo info = new VarInfo(name, type, true, isGlobal, bounds.getFirst());
            curBlock.addVar(info);
        } else if (dimen == 2) {
            VarInfo info = new VarInfo(name, type, true, isGlobal, bounds.getFirst(), bounds.get(1));
            curBlock.addVar(info);
        }
        bounds.addLast(1);
        bounds.removeFirst();
        blockStackDimen.getFirst().put(name, dimen);
        nextsym();
        ConstInitVal(name, dimen, 0, bounds);
    }
    
    private void ConstInitVal(String name, int dimen, int loc, LinkedList<Integer> bounds) {
        if (sym.getType() == WordType.LPARENT || sym.getType() == WordType.IDENFR ||
                sym.getType() == WordType.INTCON || sym.getType() == WordType.PLUS ||
                sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
            String val = ConstExp();
            if (dimen == 0) {
                curBlock.getName(name).setVal(Integer.parseInt(val));
            } else {
                String newtmp = newtmp();
                Quaternion q1 = new Quaternion("ASSIGN", val, "", newtmp);
                curBlock.addQuaternion(q1);
                Quaternion q2 = new Quaternion("STORE", newtmp, "", name + "#" + loc);
                curBlock.getName(name).setVal(loc, Integer.parseInt(val));
                curBlock.addQuaternion(q2);
            }
        } else {
            nextsym();
            int i = 0;
            int thisBound = bounds.removeFirst();
            if (sym.getType() != WordType.RBRACE) {
                ConstInitVal(name, dimen, loc, bounds);
                while (sym.getType() == WordType.COMMA) {
                    i++;
                    nextsym();
                    ConstInitVal(name, dimen, loc + thisBound * i, bounds);
                }
            }
            bounds.addFirst(thisBound);
            nextsym();
        }
    }
    
    private void VarDecl(boolean isGlobal) {
        String type = BType();
        VarDef(type, isGlobal);
        while (sym.getType() == WordType.COMMA) {
            nextsym();
            VarDef(type, isGlobal);
        }
        nextsym();
    }
    
    private void VarDef(String type, boolean isGlobal) {
        String name = sym.getToken();
        int dimen = 0;
        LinkedList<Integer> bounds = new LinkedList<>();
        nextsym();
        while (sym.getType() == WordType.LBRACK) {
            dimen++;
            nextsym();
            bounds.addLast(new Integer(ConstExp()));
            nextsym();
        }
        if (dimen == 0) {
            VarInfo info = new VarInfo(name, type, false, isGlobal);
            curBlock.addVar(info);
        } else if (dimen == 1) {
            VarInfo info = new VarInfo(name, type, false, isGlobal, bounds.getFirst());
            curBlock.addVar(info);
        } else if (dimen == 2) {
            VarInfo info = new VarInfo(name, type, false, isGlobal, bounds.getFirst(), bounds.get(1));
            curBlock.addVar(info);
        }
        bounds.addLast(1);
        bounds.removeFirst();
        blockStackDimen.getFirst().put(name, dimen);
        if (sym.getType() == WordType.ASSIGN) {
            nextsym();
            InitVal(name, dimen, 0, bounds);
        }
    }
    
    private void InitVal(String name, int dimen, int loc, LinkedList<Integer> bounds) {
        if (sym.getType() == WordType.LBRACE) {
            nextsym();
            int i = 0;
            int thisBound = bounds.removeFirst();
            if (sym.getType() != WordType.RBRACE) {
                InitVal(name, dimen, loc, bounds);
                while (sym.getType() == WordType.COMMA) {
                    i++;
                    nextsym();
                    InitVal(name, dimen, loc + thisBound * i, bounds);
                }
            }
            bounds.addFirst(thisBound);
            nextsym();
        } else {
            String val = Exp(false, true, false);
            String newtmp = newtmp();
            Quaternion q1, q2;
            q1 = new Quaternion("ASSIGN", val, "", newtmp);
            curBlock.addQuaternion(q1);
            if (dimen == 0) {
                q2 = new Quaternion("STORE", newtmp, "", name);
            } else {
                q2 = new Quaternion("STORE", newtmp, "", name + "#" + loc);
            }
            curBlock.addQuaternion(q2);
        }
    }
    
    private void FuncDef() {
        String type = FuncType();
        String name = sym.getToken();
        ArrayList<VarInfo> infos;
        Quaternion q = new Quaternion("FUNC", type, name, "");
        curBlock.addQuaternion(q);
        nextsym();
        nextsym();
        if (sym.getType() == WordType.INTTK) {
            infos = FuncFParams();
        } else {
            infos = new ArrayList<>();
        }
        nextsym();
        boolean hasReturn = Block(infos, "", "");
        if (!hasReturn) {
            Quaternion q1 = new Quaternion("RETURN", "", "", "");
            curBlock.addSonQuaternion(q1);
        }
    }
    
    private void MainFuncDef() {
        nextsym();
        Quaternion q = new Quaternion("FUNC", "int", "main", "");
        curBlock.addQuaternion(q);
        nextsym();
        nextsym();
        nextsym();
        Block(new ArrayList<>(), "", "");
    }
    
    private String FuncType() {
        String type = sym.getToken();
        nextsym();
        return type;
    }
    
    private ArrayList<VarInfo> FuncFParams() {
        ArrayList<VarInfo> infos = new ArrayList<>();
        infos.add(FuncFParam());
        while (sym.getType() == WordType.COMMA) {
            nextsym();
            infos.add(FuncFParam());
        }
        return infos;
    }
    
    private VarInfo FuncFParam() {
        // 注意：这里的VarInfo，是虚假的VarInfo，因为虚参不代表实际传入的东西是啥
        String type = BType();
        String name = sym.getToken();
        int dimen = 0;
        int bound1 = 0;
        int bound2 = 0;
        nextsym();
        if (sym.getType() == WordType.LBRACK) {
            dimen++;
            nextsym();
            nextsym();
            while (sym.getType() == WordType.LBRACK) {
                dimen++;
                nextsym();
                bound2 = Integer.parseInt(ConstExp());
                nextsym();
            }
        }
        if (dimen == 0) {
            return new VarInfo(name, type, false, false); // noticable
        } else if (dimen == 1) {
            return new VarInfo(name, type, false, false, bound1);
        } else {
            return new VarInfo(name, type, false, false, bound1, bound2);
        }
    }
    
    private boolean Block(ArrayList<VarInfo> paras, String while_begin, String while_end) {
        boolean hasReturn = false;
        blockStackDimen.addFirst(new HashMap<>());
        curBlock = new MBlock(curBlock);
        for (VarInfo para : paras) {
            curBlock.addParam(para);
            blockStackDimen.getFirst().put(para.getName(), para.getDimen());
        }
        nextsym();
        while (sym.getType() != WordType.RBRACE) {
            hasReturn = BlockItem(while_begin, while_end);
        }
        blockStackDimen.removeFirst();
        curBlock = curBlock.getFather();
        nextsym();
        return hasReturn;
    }
    
    private boolean BlockItem(String while_begin, String while_end) {
        boolean hasReturn = false;
        if (sym.getType() == WordType.INTTK || sym.getType() == WordType.CONSTTK) {
            Decl(false);
        } else {
            hasReturn = Stmt(while_begin, while_end);
        }
        return hasReturn;
    }
    
    private boolean Stmt(String while_begin, String while_end) {
        boolean hasReturn = false;
        if (sym.getType() == WordType.SEMICN) {
            nextsym();
        } else if (sym.getType() == WordType.IFTK) {
            String labelStart = genBranchLoopLabel();
            String labelEnd = genBranchLoopLabel();
            nextsym();
            nextsym();
            Cond(labelStart, labelEnd);
            nextsym();
            Quaternion q_s = new Quaternion("LABEL", labelStart, "", "");
            curBlock.addQuaternion(q_s);
            Stmt(while_begin, while_end);
            
            if (sym.getType() == WordType.ELSETK) {
                String elseLabelEnd = genBranchLoopLabel();
                Quaternion q1 = new Quaternion("JUMP", elseLabelEnd, "", "");
                curBlock.addQuaternion(q1);
                Quaternion q2 = new Quaternion("LABEL", labelEnd, "", "");
                curBlock.addQuaternion(q2);
                nextsym();
                Stmt(while_begin, while_end);
                Quaternion q3 = new Quaternion("LABEL", elseLabelEnd, "", "");
                curBlock.addQuaternion(q3);
            } else {
                Quaternion q_e = new Quaternion("LABEL", labelEnd, "", "");
                curBlock.addQuaternion(q_e);
            }
        } else if (sym.getType() == WordType.WHILETK) {
            String label_before_cond = genBranchLoopLabel();
            String label_start = genBranchLoopLabel();
            String label_end = genBranchLoopLabel();
            nextsym();
            nextsym();
            Quaternion q_bc = new Quaternion("LABEL", label_before_cond, "", "");
            curBlock.addQuaternion(q_bc);
            Cond(label_start, label_end);
            nextsym();
            Quaternion q_s = new Quaternion("LABEL", label_start, "", "");
            curBlock.addQuaternion(q_s);
            Stmt(label_before_cond, label_end);
            Quaternion q_jump = new Quaternion("JUMP", label_before_cond, "", "");
            curBlock.addQuaternion(q_jump);
            Quaternion q_e = new Quaternion("LABEL", label_end, "", "");
            curBlock.addQuaternion(q_e);
        } else if (sym.getType() == WordType.BREAKTK) {
            nextsym();
            nextsym();
            Quaternion q_e = new Quaternion("JUMP", while_end, "", "");
            curBlock.addQuaternion(q_e);
        } else if (sym.getType() == WordType.CONTINUETK) {
            nextsym();
            nextsym();
            Quaternion q_e = new Quaternion("JUMP", while_begin, "", "");
            curBlock.addQuaternion(q_e);
        } else if (sym.getType() == WordType.RETURNTK) {
            hasReturn = true;
            String name = "";
            nextsym();
            if (sym.getType() == WordType.LPARENT || sym.getType() == WordType.IDENFR ||
                    sym.getType() == WordType.INTCON || sym.getType() == WordType.PLUS ||
                    sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
                name = Exp(false, true, false);
            }
            Quaternion q = new Quaternion("RETURN", name, "", "");
            curBlock.addQuaternion(q);
            nextsym();
        } else if (sym.getType() == WordType.PRINTFTK) {
            nextsym();
            nextsym();
            String tmp = sym.getToken().replaceAll("\"", "");
            nextsym();
            int i;
            while (sym.getType() == WordType.COMMA) {
                nextsym();
                i = tmp.indexOf("%d");
                if (!tmp.substring(0, i).isEmpty()) {
                    Quaternion q1 = new Quaternion("PRINTS", tmp.substring(0, i), "", "");
                    curBlock.addQuaternion(q1);
                }
                String name = Exp(false, true, false);
                Quaternion q2 = new Quaternion("PRINTF", name, "", "");
                curBlock.addQuaternion(q2);
                tmp = tmp.substring(i + 2);
            }
            if (!tmp.isEmpty()) {
                Quaternion q1 = new Quaternion("PRINTS", tmp, "", "");
                curBlock.addQuaternion(q1);
            }
            nextsym();
            nextsym();
        } else if (sym.getType() == WordType.LBRACE) {
            hasReturn = Block(new ArrayList<>(), while_begin, while_end);
        } else {
            if (sym.getType() == WordType.IDENFR) {
                if (getNextType(1) == WordType.LPARENT) {
                    Exp(false, false, false);
                } else {
                    if (isLVal()) {
                        // LVal情况
                        String lname = LVal(false, true, false);
                        nextsym();
                        Quaternion q, qstore;
                        if (sym.getType() == WordType.GETINTTK) {
                            nextsym();
                            nextsym();
                            nextsym();
                            q = new Quaternion("SCANF", lname, "", "");
                        } else {
                            String name = Exp(false, true, false);
                            q = new Quaternion("ASSIGN", name, "", lname);
                        }
                        curBlock.addQuaternion(q);
                        qstore = new Quaternion("STORE", lname, "", storeRealVar);
                        curBlock.addQuaternion(qstore);
                    } else {
                        Exp(false, false, false);
                    }
                }
            } else {
                Exp(false, false, false);
            }
            nextsym();
        }
        return hasReturn;
    }
    
    private String Exp(boolean fakeRun, boolean retValUsed, boolean isRealParam) {
        return AddExp(fakeRun, retValUsed, isRealParam);
    }
    
    private void Cond(String label_s, String label_e) {
        LOrExp(label_s, label_e);
    }
    
    private String LVal(boolean fakeRun, boolean atLeft, boolean isRealParam) {
        if (isRealParam) {
            String res = sym.getToken();
            int bound = searchBound(res);
            LinkedList<String> indexes = new LinkedList<>();
            // indexes.size是实参名后的括号数
            nextsym();
            while (sym.getType() == WordType.LBRACK) {
                nextsym();
                indexes.addLast(Exp(fakeRun, true, false));
                nextsym();
            }
            if (!fakeRun) {
                if (indexes.size() == 2) {
                    // minus 2 dimen
                    // 实参2维，虚参0维，res赋为值
                    String newtmp1;
                    if (calcable(indexes.getFirst()) && calcable(String.valueOf(bound))) {
                        int op1 = calc(indexes.removeFirst());
                        int op2 = calc(String.valueOf(bound));
                        newtmp1 = String.valueOf(op1 * op2);
                    } else {
                        newtmp1 = newtmp();
                        Quaternion q1 = new Quaternion("MUL", indexes.removeFirst(), String.valueOf(bound), newtmp1);
                        curBlock.addQuaternion(q1);
                    }
                    String newtmp2;
                    if (calcable(indexes.getFirst()) && calcable(newtmp1)) {
                        int op1 = calc(indexes.getFirst());
                        int op2 = calc(newtmp1);
                        newtmp2 = String.valueOf(op1 + op2);
                    } else {
                        newtmp2 = newtmp();
                        Quaternion q2 = new Quaternion("ADD", newtmp1, indexes.getFirst(), newtmp2);
                        curBlock.addQuaternion(q2);
                    }
                    res = res + "#" + newtmp2;
                } else if (indexes.size() == 1) {
                    // minus 1 dimen
                    if (getVarDimen(res) == 2) {
                        // 实参2维、虚参1维，res赋为地址
                        // TODO
                        String newtmp1;
                        if (calcable(indexes.getFirst()) && calcable(String.valueOf(bound))) {
                            int op1 = calc(indexes.removeFirst());
                            int op2 = calc(String.valueOf(bound));
                            newtmp1 = String.valueOf(op1 * op2);
                        } else {
                            newtmp1 = newtmp();
                            Quaternion q1 = new Quaternion("MUL", indexes.removeFirst(), String.valueOf(bound), newtmp1);
                            curBlock.addQuaternion(q1);
                        }
                        res = res + "#" + newtmp1;
                        // 二维数组引用行起始地址赋给寄存器
                        String newtmp2 = newtmp();
                        Quaternion q = new Quaternion("LADDR", res, "", newtmp2);
                        curBlock.addQuaternion(q);
                        // 结果返回存有地址的寄存器名
                        res = newtmp2;
                    } else {
                        // 实参1维、虚参0维度
                        res = res + "#" + indexes.getFirst();
                    }
                } else {
                    // minus 0 dimen
                    int dimen = getVarDimen(res);
                    if (dimen == 2 || dimen == 1) {
                        // 实参2维、虚参2维，res赋为地址
                        // 实参1维、虚参1维，res赋为地址
                        // TODO
                        res = res + "#" + 0;
                        String newtmp = newtmp();
                        Quaternion q = new Quaternion("LADDR", res, "", newtmp);
                        curBlock.addQuaternion(q);
                        res = newtmp;
                    } else {
                        // 实参0维、虚参0维
                    }
                }
                if (calcable(res)) {
                    res = String.valueOf(calc(res));
                }
                
                // gen Load or Store (only for var & array_var)
                if (res.charAt(0) != '$' && !res.matches("[+|-]?[0-9]+")) {
                    String newtmp = newtmp();
                    if (!atLeft) {
                        // gen Load
                        Quaternion q = new Quaternion("LOAD", res, "", newtmp);
                        curBlock.addQuaternion(q);
                    } else {
                        // gen Store
                        storeRealVar = res;
                    }
                    res = newtmp;
                }
            }
            return res;
        } else {
            String res = sym.getToken();
            int bound = searchBound(res);
            LinkedList<String> indexes = new LinkedList<>();
            nextsym();
            while (sym.getType() == WordType.LBRACK) {
                nextsym();
                indexes.addLast(Exp(fakeRun, true, false));
                nextsym();
            }
            if (!fakeRun) {
                if (bound != -1) {
                    // 2 dimen
                    String newtmp1;
                    if (calcable(indexes.getFirst()) && calcable(String.valueOf(bound))) {
                        int op1 = calc(indexes.removeFirst());
                        int op2 = calc(String.valueOf(bound));
                        newtmp1 = String.valueOf(op1 * op2);
                    } else {
                        newtmp1 = newtmp();
                        Quaternion q1 = new Quaternion("MUL", indexes.removeFirst(), String.valueOf(bound), newtmp1);
                        curBlock.addQuaternion(q1);
                    }
                    String newtmp2;
                    if (calcable(indexes.getFirst()) && calcable(newtmp1)) {
                        int op1 = calc(indexes.getFirst());
                        int op2 = calc(newtmp1);
                        newtmp2 = String.valueOf(op1 + op2);
                    } else {
                        newtmp2 = newtmp();
                        Quaternion q2 = new Quaternion("ADD", newtmp1, indexes.getFirst(), newtmp2);
                        curBlock.addQuaternion(q2);
                    }
                    res = res + "#" + newtmp2;
                } else if (!indexes.isEmpty()) {
                    // 1 dimen
                    res = res + "#" + indexes.getFirst();
                } else {
                    // 0 dimen
                }
                if (calcable(res)) {
                    res = String.valueOf(calc(res));
                }
                // gen Load or Store
                if (res.charAt(0) != '$' && !res.matches("[+|-]?[0-9]+")) {
                    String newtmp = newtmp();
                    if (!atLeft) {
                        // gen Load
                        Quaternion q = new Quaternion("LOAD", res, "", newtmp);
                        curBlock.addQuaternion(q);
                    } else {
                        // gen Store
                        storeRealVar = res;
                    }
                    res = newtmp;
                }
            }
            return res;
        }
    }
    
    private int searchBound(String name) {
        // 仅在变量存在、且为二维时返回bound；否则返回-1
        MBlock ptr = curBlock;
        while (ptr != null) {
            VarInfo info = ptr.getName(name);
            if (info != null) {
                return info.getDimen() == 2 ? info.getBound2() : -1;
            }
            ptr = ptr.getFather();
        }
        return -1;
    }
    
    private String PrimaryExp(boolean fakeRun, boolean retValUsed, boolean isRealParam) {
        String res;
        if (sym.getType() == WordType.LPARENT) {
            nextsym();
            res = Exp(fakeRun, retValUsed, false);
            nextsym();
        } else if (sym.getType() == WordType.IDENFR) {
            res = LVal(fakeRun, false, isRealParam);
        } else {
            res = Number();
        }
        return res;
    }
    
    private String Number() {
        String res = sym.getToken();
        nextsym();
        return res;
    }
    
    private String UnaryExp(boolean fakeRun, boolean retValUsed, boolean isRealParam) {
        String res = null;
        if (sym.getType() == WordType.PLUS || sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
            String op = UnaryOp();
            String op2 = UnaryExp(fakeRun, retValUsed, isRealParam);
            if (!fakeRun) {
                if (calcable(op2)) {
                    switch (op) {
                        case "+":
                            res = String.valueOf(calc(op2));
                            break;
                        case "-":
                            res = String.valueOf(-calc(op2));
                            break;
                        case "!":
                            res = calc(op2) == 0 ? "1" : "0";
                            break;
                        default:
                            break;
                    }
                } else {
                    res = newtmp();
                    Quaternion q;
                    if (Objects.equals(op, "+")) {
                        q = new Quaternion("ADD", "0", op2, res);
                    } else if (Objects.equals(op, "-")) {
                        q = new Quaternion("SUB", "0", op2, res);
                    } else {
                        q = new Quaternion("NOT", op2, "", res);
                    }
                    curBlock.addQuaternion(q);
                }
            }
        } else if (sym.getType() == WordType.IDENFR && getNextType(1) == WordType.LPARENT) {
            ArrayList<String> rparams = new ArrayList<>();
            String name = sym.getToken();
            nextsym();
            nextsym();
            if (sym.getType() == WordType.LPARENT || sym.getType() == WordType.IDENFR ||
                    sym.getType() == WordType.INTCON || sym.getType() == WordType.PLUS ||
                    sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
                rparams = FuncRParams();
            }
            for (String param : rparams) {
                Quaternion q = new Quaternion("PUSH", param, "", "");
                curBlock.addQuaternion(q);
            }
            Quaternion q1 = new Quaternion("CALL", name, "", "");
            curBlock.addQuaternion(q1);
            nextsym();
            if (retValUsed) {
                String newtmp = newtmp();
                Quaternion q2 = new Quaternion("ASSIGN", "RET", "", newtmp);
                curBlock.addQuaternion(q2);
                res = newtmp;
            }
        } else {
            res = PrimaryExp(fakeRun, retValUsed, isRealParam);
        }
        return res;
    }
    
    private String UnaryOp() {
        String op = sym.getToken();
        nextsym();
        return op;
    }
    
    private ArrayList<String> FuncRParams() {
        ArrayList<String> rparams = new ArrayList<>(); // 要收集起来最后PUSH！
        String name = Exp(false, true, true);
        rparams.add(name);
        while (sym.getType() == WordType.COMMA) {
            nextsym();
            name = Exp(false, true, true);
            rparams.add(name);
        }
        return rparams;
    }
    
    private String MulExp(boolean fakeRun, boolean retValUsed, boolean isRealParam) {
        String res = UnaryExp(fakeRun, retValUsed, isRealParam);
        while (sym.getType() == WordType.MULT || sym.getType() == WordType.DIV || sym.getType() == WordType.MOD) {
            String op = sym.getType() == WordType.MULT ? "MUL" :
                    sym.getType() == WordType.DIV ? "DIV" :
                            "MOD";
            nextsym();
            String name = UnaryExp(fakeRun, retValUsed, isRealParam);
            if (!fakeRun) {
                if (calcable(res) && calcable(name)) {
                    int op1 = calc(res);
                    int op2 = calc(name);
                    switch (op) {
                        case "MUL":
                            res = String.valueOf(op1 * op2);
                            break;
                        case "DIV":
                            res = String.valueOf(op1 / op2);
                            break; // div 0 exception
                        case "MOD":
                            res = String.valueOf(op1 % op2);
                            break;
                        default:
                            break;
                    }
                } else {
                    String newtmp = newtmp();
                    Quaternion q = new Quaternion(op, res, name, newtmp);
                    curBlock.addQuaternion(q);
                    res = newtmp;
                }
            }
        }
        return res;
    }
    
    private String AddExp(boolean fakeRun, boolean retValUsed, boolean isRealParam) {
        String res = MulExp(fakeRun, retValUsed, isRealParam);
        while (sym.getType() == WordType.PLUS || sym.getType() == WordType.MINU) {
            String op = sym.getType() == WordType.PLUS ? "ADD" : "SUB";
            nextsym();
            String name = MulExp(fakeRun, retValUsed, isRealParam);
            if (!fakeRun) {
                if (calcable(res) && calcable(name)) {
                    int op1 = calc(res);
                    int op2 = calc(name);
                    switch (op) {
                        case "ADD":
                            res = String.valueOf(op1 + op2);
                            break;
                        case "SUB":
                            res = String.valueOf(op1 - op2);
                            break;
                        default:
                            break;
                    }
                } else {
                    // can't assure op2 is num, op1 is reg (sub)
                    String newtmp = newtmp();
                    Quaternion q = new Quaternion(op, res, name, newtmp);
                    curBlock.addQuaternion(q);
                    res = newtmp;
                }
            }
        }
        return res;
    }
    
    private boolean calcable(String res) {
        String[] tmp = res.split("#");
        if (tmp.length > 1 && tmp[1].charAt(0) == '$') {
            return false; // 数组为常量、但下标为寄存器的形式
        }
        
        if (res.matches("[+|-]?[0-9]+")) {
            return true;
        } else {
            MBlock ptr = curBlock;
            while (ptr != null) {
                String name = tmp[0];
                VarInfo info = ptr.getName(name);
                if (info != null) {
                    if (info.getIsConst()) {
                        return true;
                    } else {
                        break;
                    }
                }
                ptr = ptr.getFather();
            }
        }
        return false;
    }
    
    private int calc(String res) {
        if (res.matches("[+|-]?[0-9]+")) {
            return Integer.parseInt(res);
        } else {
            MBlock ptr = curBlock;
            String[] tmp = res.split("#");
            String name = tmp[0];
            while (ptr != null) {
                VarInfo info = ptr.getName(name);
                if (info != null && info.getIsConst()) {
                    if (info.getDimen() == 0) {
                        return info.getVal();
                    } else {
                        return info.getVal(Integer.parseInt(tmp[1]));
                    }
                }
                ptr = ptr.getFather();
            }
        }
        return -1;
    }
    
    private String RelExp() {
        String res = AddExp(false, true, false);
        while (sym.getType() == WordType.LSS || sym.getType() == WordType.GRE ||
                sym.getType() == WordType.LEQ || sym.getType() == WordType.GEQ) {
            String op = sym.getType() == WordType.LSS ? "SLT" :
                    sym.getType() == WordType.GRE ? "SGT" :
                            sym.getType() == WordType.LEQ ? "SLE" : "SGE";
            nextsym();
            String name = AddExp(false, true, false);
            if (calcable(res) && calcable(name)) {
                int op1 = calc(res);
                int op2 = calc(name);
                switch (op) {
                    case "SLT":
                        res = String.valueOf(op1 < op2 ? 1 : 0);
                        break;
                    case "SGT":
                        res = String.valueOf(op1 > op2 ? 1 : 0);
                        break;
                    case "SLE":
                        res = String.valueOf(op1 <= op2 ? 1 : 0);
                        break;
                    case "SGE":
                        res = String.valueOf(op1 >= op2 ? 1 : 0);
                    default:
                        break;
                }
            } else {
                String newtmp = newtmp();
                Quaternion q = new Quaternion(op, res, name, newtmp);
                curBlock.addQuaternion(q);
                res = newtmp;
            }
        }
        return res;
    }
    
    private String EqExp() {
        String res = RelExp();
        while (sym.getType() == WordType.EQL || sym.getType() == WordType.NEQ) {
            String op = sym.getType() == WordType.EQL ? "SEQ" : "SNE";
            nextsym();
            String name = RelExp();
            if (calcable(res) && calcable(name)) {
                int op1 = calc(res);
                int op2 = calc(name);
                switch (op) {
                    case "SEQ":
                        res = String.valueOf(op1 == op2 ? 1 : 0);
                        break;
                    case "SNE":
                        res = String.valueOf(op1 != op2 ? 1 : 0);
                        break;
                    default:
                        break;
                }
            } else {
                String newtmp = newtmp();
                Quaternion q = new Quaternion(op, res, name, newtmp);
                curBlock.addQuaternion(q);
                res = newtmp;
            }
        }
        return res;
    }
    
    private void LAndExp(String all_true_jump, String false_jump) {
        // 先计算EqExp的个数
        int and_count = 1;
        int buf_reserved = buf;
        int parentCount = 1;
        while (true) {
            if (sym.getType() == WordType.LPARENT) {
                parentCount ++;
            } else if (sym.getType() == WordType.RPARENT) {
                parentCount --;
            } else if (sym.getType() == WordType.AND) {
                and_count++;
            }
            if (sym.getType() == WordType.OR || parentCount == 0) {
                break;
            }
            nextsym();
        }
        buf = buf_reserved;
        sym = wordList.get(buf);
        
        int i = 0;
        String left = EqExp();
        if (calcable(left)) {
            if (calc(left) == 0) {
                Quaternion q = new Quaternion("JUMP", false_jump, "", "");
                curBlock.addQuaternion(q);
            }
        } else {
            Quaternion q = new Quaternion("BEQ", left, "0", false_jump);
            curBlock.addQuaternion(q);
        }
        while (sym.getType() == WordType.AND) {
            i++;
            nextsym();
            left = EqExp();
            if (i != and_count - 1) {
                if (calcable(left)) {
                    if (calc(left) == 0) {
                        Quaternion q = new Quaternion("JUMP", false_jump, "", "");
                        curBlock.addQuaternion(q);
                    }
                } else {
                    Quaternion q = new Quaternion("BEQ", left, "0", false_jump);
                    curBlock.addQuaternion(q);
                }
            }
        }
        if (!all_true_jump.isEmpty()) {
            if (calcable(left)) {
                if (calc(left) == 1) {
                    Quaternion q = new Quaternion("JUMP", all_true_jump, "", "");
                    curBlock.addQuaternion(q);
                }
            } else {
                Quaternion q = new Quaternion("BEQ", left, "1", all_true_jump);
                curBlock.addQuaternion(q);
            }
        } else {
            if (calcable(left)) {
                if (calc(left) == 0) {
                    Quaternion q = new Quaternion("JUMP", false_jump, "", "");
                    curBlock.addQuaternion(q);
                }
            } else {
                Quaternion q = new Quaternion("BEQ", left, "0", false_jump);
                curBlock.addQuaternion(q);
            }
        }
    }
    
    private void LOrExp(String l_s, String l_e) {
        // 先计算LAND的个数
        int or_count = 1;
        int buf_reserved = buf;
        int parentCount = 1;
        while (parentCount != 0) {
            if (sym.getType() == WordType.LPARENT) {
                parentCount ++;
            } else if (sym.getType() == WordType.RPARENT) {
                parentCount --;
            } else if (sym.getType() == WordType.OR) {
                or_count ++;
            }
            nextsym();
        }
        buf = buf_reserved;
        sym = wordList.get(buf);
        
        ArrayList<String> label_and_ends = new ArrayList<>();
        for (int i = 0; i < or_count - 1; i++) {
            String label_and_end = genBranchLoopLabel();
            label_and_ends.add(label_and_end);
        }
        label_and_ends.add(l_e);
        
        int i = 0;
        if (i == or_count - 1) {
            LAndExp("", label_and_ends.get(i));
        } else {
            LAndExp(l_s, label_and_ends.get(i));
            Quaternion q = new Quaternion("LABEL", label_and_ends.get(i++), "", "");
            curBlock.addQuaternion(q);
        }
        while (sym.getType() == WordType.OR) {
            nextsym();
            if (i == or_count - 1) {
                LAndExp("", label_and_ends.get(i));
            } else {
                LAndExp(l_s, label_and_ends.get(i));
                Quaternion q = new Quaternion("LABEL", label_and_ends.get(i++), "", "");
                curBlock.addQuaternion(q);
            }
        }
    }
    
    private String ConstExp() {
        return AddExp(false, true, false);
    }
    
    private String newtmp() {
        return "$" + (tmpbuf++);
    }
    
    private void nextsym() {
        buf++;
        sym = (buf == wordList.size()) ? new WordInfo(null, null, -1) : wordList.get(buf);
    }
    
    private WordType getNextType(int i) {
        return buf == wordList.size() - 1 ? null : wordList.get(buf + i).getType();
    }
    
    private String genBranchLoopLabel() {
        return "jumpLabel_" + (curLabelIndex++);
    }
    
    private boolean isLVal() {
        int curBuf = buf;
        LVal(true, true, false);
        if (sym.getType() == WordType.ASSIGN) {
            buf = curBuf;
            sym = wordList.get(buf);
            return true;
        } else {
            buf = curBuf;
            sym = wordList.get(buf);
            return false;
        }
    }
    
    public void printMCode() {
        root.print();
    }
    
    public MBlock getMCodeRoot() {
        return root;
    }
    
    private int getVarDimen(String s) {
        for (HashMap<String, Integer> tmp : blockStackDimen) {
            if (tmp.containsKey(s)) {
                return tmp.get(s);
            }
        }
        return -1;
    }
}
