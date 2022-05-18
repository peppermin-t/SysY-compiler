import Exceptions.CompilerException;
import formats.ExceptionType;
import formats.SymbolAttributes;
import formats.WordInfo;
import formats.WordType;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Parser {
    private final boolean debug;
    private final ArrayList<WordInfo> wordList;
    private int buf;
    private WordInfo symLast;
    private WordInfo sym;
    private final ArrayList<String> parserOutput;
    
    // symbol table related
    private final LinkedList<HashMap<String, SymbolAttributes>> blockStack;
    
    private BufferedWriter bw;
    
    public Parser(ArrayList<WordInfo> wordList) {
        this.wordList = wordList;
        buf = -1;
        debug = false;
        sym = new WordInfo(null, null, -1);
        parserOutput = new ArrayList<>();
        
        blockStack = new LinkedList<>();
        try {
            bw = new BufferedWriter(new FileWriter("error.txt"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void dispose() {
        nextsym();
        blockStack.addFirst(new HashMap<>());
        CompileUnit();
        try {
            bw.flush();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void CompileUnit() {
        while (true) {
            if (sym.getType() == WordType.CONSTTK) {
                Decl();
            } else if (sym.getType() == WordType.VOIDTK) {
                break;
            } else if (sym.getType() == WordType.INTTK) {
                if (getNextType(1) == WordType.MAINTK ||
                        getNextType(1) == WordType.IDENFR && getNextType(2) == WordType.LPARENT) {
                    break;
                } else {
                    Decl();
                }
            } else { /* error();*/ }
        }
        while (true) {
            if (sym.getType() == WordType.VOIDTK) {
                FuncDef();
            } else if (sym.getType() == WordType.INTTK &&
                    getNextType(1) == WordType.IDENFR) {
                FuncDef();
            } else {
                break;
            }
        }
        if (sym.getType() == WordType.INTTK) {
            MainFuncDef();
        } else { /* error();*/ }
        parserOutput.add("<CompUnit>");
    }
    
    private void Decl() {
        // Decl证明有一个条目要插入当前HashMap
        SymbolAttributes tmp = new SymbolAttributes();
        if (sym.getType() == WordType.CONSTTK) {
            ConstDecl(tmp);
        } else if (sym.getType() == WordType.INTTK) {
            VarDecl(tmp);
        } else { /* error();*/ }
    }
    
    private void ConstDecl(SymbolAttributes tmp) {
        if (sym.getType() == WordType.CONSTTK) {
            tmp.setConst(true);
            nextsym();
            if (sym.getType() == WordType.INTTK) {
                BType(tmp);
                if (sym.getType() == WordType.IDENFR) {
                    ConstDef(tmp);
                    while (true) {
                        if (sym.getType() == WordType.COMMA) {
                            nextsym();
                            ConstDef(tmp);
                        } else {
                            break;
                        }
                    }
                    if (sym.getType() == WordType.SEMICN) {
                        nextsym();
                    } else {
                        (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
                    }
                } else { /* error();*/ }
            } else { /* error();*/ }
        } else { /* error();*/ }
        parserOutput.add("<ConstDecl>");
    }
    
    private void BType(SymbolAttributes tmp) {
        if (sym.getType() == WordType.INTTK) {
            tmp.setType("int");
            nextsym();
        } else { /* error();*/ }
    }
    
    private void ConstDef(SymbolAttributes tmp) {
        if (sym.getType() == WordType.IDENFR) {
            tmp.setName(sym.getToken());
            tmp.setIdentLine(sym.getLine());
            
            nextsym();
            while (true) {
                if (sym.getType() == WordType.LBRACK) {
                    nextsym();
                    ConstExp();
                    if (sym.getType() == WordType.RBRACK) {
                        tmp.plusDimension();
                        nextsym();
                    } else {
                        (new CompilerException(symLast.getLine(), ExceptionType.RBRACKMISSING)).printMessage(bw);
                    }
                } else {
                    break;
                }
            }
            if (checkDouble(tmp.getName())) {
                (new CompilerException(tmp.getIdentLine(), ExceptionType.IDENTDOUBLED)).printMessage(bw);
            } else {
                blockStack.getFirst().put(tmp.getName(), tmp.clone()); // 不能放在循环里，否则进不了循环就不行了
            }
            if (sym.getType() == WordType.ASSIGN) {
                nextsym();
                ConstInitVal();
            } else { /* error();*/ }
        } else { /* error();*/ }
        parserOutput.add("<ConstDef>");
    }
    
    /*private String renewName(String name) {
        int newIndex = repeatTime++;
        return "@" + name + "@" + newIndex;
    }*/
    
    private void ConstInitVal() {
        if (sym.getType() == WordType.LPARENT || sym.getType() == WordType.IDENFR ||
                sym.getType() == WordType.INTCON || sym.getType() == WordType.PLUS ||
                sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
            ConstExp();
        } else if (sym.getType() == WordType.LBRACE) {
            nextsym();
            if (sym.getType() == WordType.RBRACE) {
                nextsym();
            } else { // 问题：先判断FIRST符合不符合还是进去再判断？
                // 结论：只有在需要选择分支时才需要提前判断，否则不需要
                ConstInitVal();
                while (true) {
                    if (sym.getType() == WordType.COMMA) {
                        nextsym();
                        ConstInitVal();
                    } else {
                        break;
                    }
                }
                if (sym.getType() == WordType.RBRACE) {
                    nextsym();
                } else { /* error();*/ }
            }
        } else { /* error();*/ }
        parserOutput.add("<ConstInitVal>");
    }
    
    private void VarDecl(SymbolAttributes tmp) {
        tmp.setConst(false);
        BType(tmp);
        VarDef(tmp);
        while (true) {
            if (sym.getType() == WordType.COMMA) {
                nextsym();
                VarDef(tmp);
            } else {
                break;
            }
        }
        if (sym.getType() == WordType.SEMICN) {
            nextsym();
        } else {
            (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
        }
        parserOutput.add("<VarDecl>");
    }
    
    private void VarDef(SymbolAttributes tmp) {
        if (sym.getType() == WordType.IDENFR) {
            tmp.setName(sym.getToken());
            tmp.setIdentLine(sym.getLine());
            
            nextsym();
            while (true) {
                if (sym.getType() == WordType.LBRACK) {
                    nextsym();
                    ConstExp();
                    if (sym.getType() == WordType.RBRACK) {
                        tmp.plusDimension();
                        nextsym();
                    } else {
                        (new CompilerException(symLast.getLine(), ExceptionType.RBRACKMISSING)).printMessage(bw);
                    }
                } else {
                    break;
                }
            }
            if (checkDouble(tmp.getName())) {
                (new CompilerException(tmp.getIdentLine(), ExceptionType.IDENTDOUBLED)).printMessage(bw);
            } else {
                // blockStack里的名字不需要改，搜索时搜原名字；
                // 但sym需要改，变量出现下标累增
                
                blockStack.getFirst().put(tmp.getName(), tmp.clone());
            }
            if (sym.getType() == WordType.ASSIGN) {
                nextsym();
                InitVal();
            }
        } else { /* error();*/ }
        parserOutput.add("<VarDef>");
    }
    
    private void InitVal() {
        if (sym.getType() == WordType.LBRACE) {
            nextsym();
            if (sym.getType() == WordType.RBRACE) {
                nextsym();
            } else {
                InitVal();
                while (true) {
                    if (sym.getType() == WordType.COMMA) {
                        nextsym();
                        InitVal();
                    } else {
                        break;
                    }
                }
                if (sym.getType() == WordType.RBRACE) {
                    nextsym();
                } else { /* error();*/ }
            }
        } else {
            // 问题：通过偷看在外层就判断错误，还是到内层再判断错误？
            Exp(false);
        }
        parserOutput.add("<InitVal>");
    }
    
    private void FuncDef() {
        ArrayList<SymbolAttributes> fparams = null;
        SymbolAttributes tmp = new SymbolAttributes();
        FuncType(tmp);
        if (sym.getType() == WordType.IDENFR) {
            tmp.setIdentLine(sym.getLine());
            tmp.setName("1" + sym.getToken()); // 函数名前加1与变量名区分
            nextsym();
            if (sym.getType() == WordType.LPARENT) {
                nextsym();
                if (sym.getType() == WordType.INTTK) {
                    fparams = FuncFParams();
                    for (SymbolAttributes var : fparams) {
                        tmp.getFormalParams().add(var.getDimension());
                    }
                }
                if (checkDouble(tmp.getName())) {
                    (new CompilerException(tmp.getIdentLine(), ExceptionType.IDENTDOUBLED)).printMessage(bw);
                } else {
                    blockStack.getFirst().put(tmp.getName(), tmp.clone());
                }
                if (sym.getType() == WordType.RPARENT) {
                    nextsym();
                } else {
                    (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
                }
                boolean hasReturn = Block(fparams, tmp.getType());
                if (!hasReturn && !Objects.equals(tmp.getType(), "void")) {
                    (new CompilerException(symLast.getLine(), ExceptionType.RETURNMISSINGINNONVOIDFUNC)).printMessage(bw);
                }
            } else { /* error();*/ }
        } else { /* error();*/ }
        parserOutput.add("<FuncDef>");
    }
    
    private void MainFuncDef() {
        if (sym.getType() == WordType.INTTK) {
            nextsym();
            if (sym.getType() == WordType.MAINTK) {
                SymbolAttributes tmp = new SymbolAttributes();
                tmp.setIdentLine(sym.getLine());
                tmp.setType("int");
                tmp.setName("1main");
                blockStack.getFirst().put(tmp.getName(), tmp);
                nextsym();
                if (sym.getType() == WordType.LPARENT) {
                    nextsym();
                    if (sym.getType() == WordType.RPARENT) {
                        nextsym();
                    } else {
                        (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
                    }
                    boolean hasReturn = Block(new ArrayList<>(), tmp.getType());
                    if (!hasReturn && !Objects.equals(tmp.getType(), "void")) {
                        (new CompilerException(symLast.getLine(), ExceptionType.RETURNMISSINGINNONVOIDFUNC)).printMessage(bw);
                    }
                } else { /* error();*/ }
            } else { /* error();*/ }
        } else { /* error();*/ }
        parserOutput.add("<MainFuncDef>");
    }
    
    private void FuncType(SymbolAttributes tmp) {
        if (sym.getType() == WordType.VOIDTK || sym.getType() == WordType.INTTK) {
            tmp.setType(sym.getToken());
            nextsym();
        } else { /* error();*/ }
        parserOutput.add("<FuncType>");
    }
    
    private ArrayList<SymbolAttributes> FuncFParams() {
        ArrayList<SymbolAttributes> fparams = new ArrayList<>();
        fparams.add(FuncFParam());
        while (true) {
            if (sym.getType() == WordType.COMMA) {
                nextsym();
                fparams.add(FuncFParam());
            } else {
                break;
            }
        }
        parserOutput.add("<FuncFParams>");
        return fparams;
    }
    
    private SymbolAttributes FuncFParam() {
        SymbolAttributes tmp = new SymbolAttributes();
        BType(tmp);
        if (sym.getType() == WordType.IDENFR) {
            tmp.setName(sym.getToken());
            tmp.setIdentLine(sym.getLine());
            
            nextsym();
            if (sym.getType() == WordType.LBRACK) {
                tmp.plusDimension();
                nextsym();
                if (sym.getType() == WordType.RBRACK) {
                    nextsym();
                } else {
                    (new CompilerException(symLast.getLine(), ExceptionType.RBRACKMISSING)).printMessage(bw);
                }
                while (true) {
                    if (sym.getType() == WordType.LBRACK) {
                        tmp.plusDimension();
                        nextsym();
                        ConstExp();
                        if (sym.getType() == WordType.RBRACK) {
                            nextsym();
                        } else {
                            (new CompilerException(symLast.getLine(), ExceptionType.RBRACKMISSING)).printMessage(bw);
                        }
                    } else {
                        break;
                    }
                }
            }
        } else { /* error();*/ }
        parserOutput.add("<FuncFParam>");
        return tmp;
    }
    
    private boolean Block(ArrayList<SymbolAttributes> funcFParams, String funcReturnType) {
        boolean hasReturn = false;
        if (sym.getType() == WordType.LBRACE) {
            blockStack.addFirst(new HashMap<>());
            if (funcFParams != null) {
                for (SymbolAttributes var : funcFParams) {
                    if (checkDouble(var.getName())) {
                        (new CompilerException(var.getIdentLine(), ExceptionType.IDENTDOUBLED)).printMessage(bw);
                    } else {
                        blockStack.getFirst().put(var.getName(), var);
                    }
                } // 若有形参，将形参放在内层
            }
            nextsym();
            while (true) {
                if (sym.getType() == WordType.RBRACE) {
                    blockStack.removeFirst();
                    break;
                }
                hasReturn = BlockItem(funcReturnType);
            }
            nextsym();
            // 这里偷了个懒，因为BlockItem的FIRST太多了，直接判断是不是终结符'}'了，将错误处理留给BlockItem
        } else { /* error();*/ }
        parserOutput.add("<Block>");
        return hasReturn;
    }
    
    private boolean BlockItem(String funcReturnType) {
        boolean hasReturn = false;
        if (sym.getType() == WordType.INTTK || sym.getType() == WordType.CONSTTK) {
            Decl();
        } else {
            hasReturn = Stmt(funcReturnType); // 错误留给Stmt处理
        }
        return hasReturn;
    }
    
    private boolean Stmt(String funcReturnType) {
        boolean hasReturn = false;
        if (sym.getType() == WordType.SEMICN) {
            nextsym();
        } else if (sym.getType() == WordType.IFTK) {
            nextsym();
            if (sym.getType() == WordType.LPARENT) {
                nextsym();
                Cond();
                if (sym.getType() == WordType.RPARENT) {
                    nextsym();
                } else {
                    (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
                }
                Stmt(null);
                if (sym.getType() == WordType.ELSETK) {
                    nextsym();
                    Stmt(null);
                }
            } else { /* error();*/ }
        } else if (sym.getType() == WordType.WHILETK) {
            nextsym();
            if (sym.getType() == WordType.LPARENT) {
                nextsym();
                Cond();
                if (sym.getType() == WordType.RPARENT) {
                    nextsym();
                    SymbolAttributes tmp = new SymbolAttributes();
                    tmp.setName("while");
                    blockStack.getFirst().put(tmp.getName(), tmp);
                    Stmt(null);
                    blockStack.getFirst().remove("while");
                } else {
                    (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
                }
            } else { /* error();*/ }
        } else if (sym.getType() == WordType.BREAKTK || sym.getType() == WordType.CONTINUETK) {
            if (checkDefinition("while") == null) {
                (new CompilerException(sym.getLine(), ExceptionType.BREAKCONTINUEINNONLOOP)).printMessage(bw);
            }
            nextsym();
            if (sym.getType() == WordType.SEMICN) {
                nextsym();
            } else {
                (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
            }
        } else if (sym.getType() == WordType.RETURNTK) {
            hasReturn = true;
            int returnLine = sym.getLine();
            nextsym();
            if (sym.getType() == WordType.LPARENT || sym.getType() == WordType.IDENFR ||
                    sym.getType() == WordType.INTCON || sym.getType() == WordType.PLUS ||
                    sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
                if (Objects.equals(funcReturnType, "void")) {
                    (new CompilerException(returnLine, ExceptionType.RETURNVARINVOIDFUNC)).printMessage(bw);
                }
                Exp(false);
            }
            if (sym.getType() == WordType.SEMICN) {
                nextsym();
            } else {
                (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
            }
        } else if (sym.getType() == WordType.PRINTFTK) {
            int printfLine = sym.getLine();
            nextsym();
            if (sym.getType() == WordType.LPARENT) {
                nextsym();
                if (sym.getType() == WordType.STRCON) {
                    int expCount = 0;
                    int fcharCount = getFCharCount(sym.getToken());
                    boolean hasIllegalChar = hasIllegalChar(sym.getToken());
                    if (hasIllegalChar) {
                        (new CompilerException(sym.getLine(), ExceptionType.ILLEGALCHAR)).printMessage(bw);
                    }
                    nextsym();
                    while (true) {
                        if (sym.getType() == WordType.COMMA) {
                            expCount++;
                            nextsym();
                            Exp(false);
                        } else {
                            break;
                        }
                    }
                    if (expCount != fcharCount) {
                        (new CompilerException(printfLine, ExceptionType.PRINTFEXPCNTUNMATCHED)).printMessage(bw);
                    }
                    if (sym.getType() == WordType.RPARENT) {
                        nextsym();
                    } else {
                        (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
                    }
                    if (sym.getType() == WordType.SEMICN) {
                        nextsym();
                    } else {
                        (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
                    }
                } else { /* error();*/ }
            } else { /* error();*/ }
        } else if (sym.getType() == WordType.LBRACE) {
            Block(null, null); // 由于return只出现现在函数Block最后一句，所以不用考虑嵌套Block
        } else if (sym.getType() == WordType.IDENFR) {
            if (getNextType(1) == WordType.LPARENT) {
                Exp(false);
                if (sym.getType() == WordType.SEMICN) {
                    nextsym();
                } else {
                    (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
                }
            } else if (isLVal()) {
                // LVal情况
                SymbolAttributes tmp = LVal(false);
                if (tmp.getIsConst()) {
                    (new CompilerException(tmp.getIdentLine(), ExceptionType.CONSTVARALTERED)).printMessage(bw);
                }
                if (sym.getType() == WordType.ASSIGN) {
                    nextsym();
                    if (sym.getType() == WordType.GETINTTK) {
                        nextsym();
                        if (sym.getType() == WordType.LPARENT) {
                            nextsym();
                            if (sym.getType() == WordType.RPARENT) {
                                nextsym();
                            } else {
                                (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
                            }
                            if (sym.getType() == WordType.SEMICN) {
                                nextsym();
                            } else {
                                (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
                            }
                        } else { /* error();*/ }
                    } else {
                        Exp(false);
                        if (sym.getType() == WordType.SEMICN) {
                            nextsym();
                        } else {
                            (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
                        }
                    }
                } else { /* error();*/ }
            } else {
                Exp(false);
                if (sym.getType() == WordType.SEMICN) {
                    nextsym();
                } else {
                    (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
                }
            }
        } else {
            Exp(false);
            if (sym.getType() == WordType.SEMICN) {
                nextsym();
            } else {
                (new CompilerException(symLast.getLine(), ExceptionType.SEMICOLONMISSING)).printMessage(bw);
            }
        }
        parserOutput.add("<Stmt>");
        return hasReturn;
    }
    
    private int Exp(boolean fakeRun) {
        int dimension = AddExp(fakeRun);
        parserOutput.add("<Exp>");
        return dimension;
    }
    
    private void Cond() {
        LOrExp();
        parserOutput.add("<Cond>");
    }
    
    private SymbolAttributes LVal(boolean fakeRun) {
        SymbolAttributes rparam = new SymbolAttributes(); // only use for return
        if (sym.getType() == WordType.IDENFR) {
            SymbolAttributes var = checkDefinition(sym.getToken());
            if (var != null) {
                // 已定义，要找最近的，将引用的名字改成那个“版本”的该变量
                // sym.setToken("@" + sym.getToken() + "@" + var.getRenameTime());
                rparam.setIdentLine(sym.getLine());
                rparam.setConst(var.getIsConst());
                rparam.setDimension(var.getDimension());
            } else if (!fakeRun) {
                // 使用前未定义
                (new CompilerException(sym.getLine(), ExceptionType.IDENTUNDEFINED)).printMessage(bw);
            }
            nextsym();
            while (true) {
                if (sym.getType() == WordType.LBRACK) {
                    nextsym();
                    Exp(fakeRun);
                    if (sym.getType() == WordType.RBRACK) {
                        rparam.minusDimension();
                        nextsym();
                    } else if (!fakeRun) {
                        (new CompilerException(symLast.getLine(), ExceptionType.RBRACKMISSING)).printMessage(bw);
                    }
                } else {
                    break;
                }
            }
        } else { /* error();*/ }
        parserOutput.add("<LVal>");
        return rparam;
    }
    
    private int PrimaryExp(boolean fakeRun) {
        SymbolAttributes tmp = new SymbolAttributes();
        if (sym.getType() == WordType.LPARENT) {
            nextsym();
            tmp.setDimension(Exp(fakeRun));
            if (sym.getType() == WordType.RPARENT) {
                nextsym();
            } else if (!fakeRun) {
                (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
            }
        } else if (sym.getType() == WordType.IDENFR) {
            tmp = LVal(fakeRun);
        } else if (sym.getType() == WordType.INTCON) {
            tmp = Number();
        } else { /* error();*/ }
        parserOutput.add("<PrimaryExp>");
        return tmp.getDimension();
    }
    
    private SymbolAttributes Number() {
        SymbolAttributes tmp = new SymbolAttributes();
        if (sym.getType() == WordType.INTCON) {
            tmp.setDimension(0);
            tmp.setConst(true);
            nextsym();
        } else { /* error();*/ }
        parserOutput.add("<Number>");
        return tmp;
    }
    
    private int UnaryExp(boolean fakeRun) {
        int dimension = 0;
        if (sym.getType() == WordType.PLUS || sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
            UnaryOp();
            dimension = UnaryExp(fakeRun);
        } else if (sym.getType() == WordType.IDENFR && getNextType(1) == WordType.LPARENT) {
            int funcIdentLine = sym.getLine();
            SymbolAttributes tmp;
            ArrayList<Integer> fparams = null;
            if ((tmp = checkDefinition("1" + sym.getToken())) != null) {
                fparams = tmp.getFormalParams();
                if (Objects.equals(tmp.getType(), "void")) {
                    dimension = -1;
                }
            } else if (!fakeRun) {
                (new CompilerException(sym.getLine(), ExceptionType.IDENTUNDEFINED)).printMessage(bw);
            }
            nextsym();
            nextsym();
            if (sym.getType() == WordType.LPARENT || sym.getType() == WordType.IDENFR ||
                    sym.getType() == WordType.INTCON || sym.getType() == WordType.PLUS ||
                    sym.getType() == WordType.MINU || sym.getType() == WordType.NOT) {
                FuncRParams(funcIdentLine, fparams);
            } else if (fparams != null && fparams.size() != 0) {
                // 没有实参传入
                (new CompilerException(funcIdentLine, ExceptionType.PARAMSCNTUNMATCHED)).printMessage(bw);
            }
            
            if (sym.getType() == WordType.RPARENT) {
                nextsym();
            } else if (!fakeRun) {
                (new CompilerException(symLast.getLine(), ExceptionType.RPARENTMISSING)).printMessage(bw);
            }
        } else {
            dimension = PrimaryExp(fakeRun);
        }
        parserOutput.add("<UnaryExp>");
        return dimension;
    }
    
    private void UnaryOp() {
        if (sym.getType() == WordType.PLUS) {
            nextsym();
        } else if (sym.getType() == WordType.MINU) {
            nextsym();
        } else if (sym.getType() == WordType.NOT) {
            nextsym();
        } else { /* error();*/ }
        parserOutput.add("<UnaryOp>");
    }
    
    private void FuncRParams(int identLine, ArrayList<Integer> fparams) {
        int rparamCount = 0;
        int dimension = Exp(false);
        if (fparams != null && rparamCount < fparams.size() && dimension != fparams.get(rparamCount)) {
            // fparams为空值表示函数未定义，但仍然需要走完，但不需要报匹配的错了
            (new CompilerException(identLine, ExceptionType.PARAMSTYPEUNMATCHED)).printMessage(bw);
        }
        rparamCount++;
        
        while (true) {
            if (sym.getType() == WordType.COMMA) {
                nextsym();
                dimension = Exp(false);
                if (fparams != null && rparamCount < fparams.size() && dimension != fparams.get(rparamCount)) {
                    (new CompilerException(identLine, ExceptionType.PARAMSTYPEUNMATCHED)).printMessage(bw);
                }
                rparamCount++;
            } else {
                break;
            }
        }
        if (fparams != null && rparamCount != fparams.size()) {
            (new CompilerException(identLine, ExceptionType.PARAMSCNTUNMATCHED)).printMessage(bw);
        }
        parserOutput.add("<FuncRParams>");
    }
    
    private int MulExp(boolean fakeRun) {
        int dimension = UnaryExp(fakeRun);
        parserOutput.add("<MulExp>");
        while (true) {
            if (sym.getType() == WordType.MULT || sym.getType() == WordType.DIV || sym.getType() == WordType.MOD) {
                nextsym();
                UnaryExp(fakeRun);
                parserOutput.add("<MulExp>");
            } else {
                break;
            }
        }
        return dimension;
    }
    
    private int AddExp(boolean fakeRun) {
        int dimension = MulExp(fakeRun);
        parserOutput.add("<AddExp>");
        while (true) {
            if (sym.getType() == WordType.PLUS || sym.getType() == WordType.MINU) {
                nextsym();
                MulExp(fakeRun);
                parserOutput.add("<AddExp>");
            } else {
                break;
            }
        }
        return dimension;
    }
    
    private void RelExp() {
        AddExp(false);
        parserOutput.add("<RelExp>");
        while (true) {
            if (sym.getType() == WordType.LSS || sym.getType() == WordType.GRE ||
                    sym.getType() == WordType.LEQ || sym.getType() == WordType.GEQ) {
                nextsym();
                AddExp(false);
                parserOutput.add("<RelExp>");
            } else {
                break;
            }
        }
    }
    
    private void EqExp() {
        RelExp();
        parserOutput.add("<EqExp>");
        while (true) {
            if (sym.getType() == WordType.EQL || sym.getType() == WordType.NEQ) {
                nextsym();
                RelExp();
                parserOutput.add("<EqExp>");
            } else {
                break;
            }
        }
    }
    
    private void LAndExp() {
        EqExp();
        parserOutput.add("<LAndExp>");
        while (true) {
            if (sym.getType() == WordType.AND) {
                nextsym();
                EqExp();
                parserOutput.add("<LAndExp>");
            } else {
                break;
            }
        }
    }
    
    private void LOrExp() {
        LAndExp();
        parserOutput.add("<LOrExp>");
        while (true) {
            if (sym.getType() == WordType.OR) {
                nextsym();
                LAndExp();
                parserOutput.add("<LOrExp>");
            } else {
                break;
            }
        }
    }
    
    private void ConstExp() {
        AddExp(false);
        parserOutput.add("<ConstExp>");
    }
    
    private void nextsym() {
        if (buf != -1) {
            parserOutput.add(wordList.get(buf).getType().toString() + " " + wordList.get(buf).getToken());
        }
        
        buf++;
        symLast = sym;
        sym = (buf == wordList.size()) ? new WordInfo(null, null, -1) : wordList.get(buf);
    }
    
    private WordType getNextType(int i) {
        return buf == wordList.size() - 1 ? null : wordList.get(buf + i).getType();
    }
    
    private boolean checkDouble(String s) {
        return blockStack.getFirst().containsKey(s);
    }
    
    private SymbolAttributes checkDefinition(String s) {
        for (HashMap<String, SymbolAttributes> tmp : blockStack) {
            if (tmp.containsKey(s)) {
                return tmp.get(s);
            }
        }
        return null;
    }
    
    private int getFCharCount(String s) {
        int cnt = 0, index = s.indexOf("%d");
        while (index != -1) {
            index += 2;
            cnt++;
            index = s.indexOf("%d", index);
        }
        return cnt;
    }
    
    private boolean hasIllegalChar(String s) {
        String s_real = s.substring(1);
        for (int i = 0; i < s_real.length() - 1; i++) {
            if (!isLegalChar(s_real, i)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isLegalChar(String s, int i) {
        char c = s.charAt(i);
        char c_next = i == s.length() - 1 ? 0 : s.charAt(i + 1);
        return Character.isDigit(c) || (int) c == 32 || (int) c == 33 ||
                (int) c >= 40 && (int) c <= 91 ||
                (int) c >= 93 && (int) c <= 126 ||
                c == '\\' && c_next == 'n' ||
                c == '%' && c_next == 'd';
        // 默认FormatString 中不会出现恶意换行或文件终结情况
    }
    
    private boolean isLVal() {
        int curBuf = buf;
        LVal(true);
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
    
    public void printOutput() throws IOException {
        FileWriter fw = new FileWriter("out_1.txt");
        BufferedWriter bw = new BufferedWriter(fw);
        for (String s : parserOutput) {
            if (debug) {
                System.out.println(s);
            } else {
                bw.write(s);
            }
        }
        bw.flush();
        bw.close();
        fw.close();
    }
}
