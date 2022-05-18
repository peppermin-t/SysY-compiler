package formats;

public class Quaternion extends MCode {
    private final String op;
    private final String op1;
    private final String op2;
    private final String res;
    
    public Quaternion(String op, String op1, String op2, String res) {
        this.op = op;
        this.op1 = op1;
        this.op2 = op2;
        this.res = res;
    }
    
    public void print() {
        System.out.println((op + " " + op1 + " " + op2 + " " + res).trim());
    }
    
    public String getOp() {
        return op;
    }
    
    public String getOp1() {
        return op1;
    }
    
    public String getOp2() {
        return op2;
    }
    
    public String getRes() {
        return res;
    }
}
