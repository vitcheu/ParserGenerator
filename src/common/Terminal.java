package common;

public class Terminal extends Token{
    private static int id=-3;
    private tokenKind kind;
    private String lxrValue;
    private int lineNum;
    public static final Terminal AT=new Terminal("@");
    public static final Terminal SHARP=new Terminal("#");
    public static final Terminal EPSILON=new Terminal("ε");
    public static final Terminal EOF=new Terminal("$");
    public Terminal(String value){
        super(value);
        super.setTid(id++);
    }

    public Terminal(String value,int lineNum){
        super(value);
        this.lineNum=lineNum;
    }

    public Terminal(String value, tokenKind kind, int lineNum) {
        super(value);
        this.kind = kind;
        this.lineNum = lineNum;
    }

    public Terminal(String value, String lxrValue, int lineNum) {
        super(value);
        this.lxrValue = lxrValue;
        this.lineNum = lineNum;
    }


    public tokenKind getKind() {
        return kind;
    }

    public int getLineNum() {
        return lineNum;
    }

    public String getLxrValue() {
        return lxrValue;
    }

    @Override
    public boolean isTerminal() {
        return true;
    }

//    @Override
//    public String toString() {
//        return "Terminal{" +getValue()+","+
//                "kind=" + kind +
//                ", lineNum=" + lineNum +
//                '}';
//    }
}
