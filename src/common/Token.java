package common;

public class Token {
    private String value;
    private   int  tid;
    public Token(String value){
        this.value=value;
    }

    public int getTid() {
        return tid;
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    public String getValue() {
        return value;
    }

    public boolean isTerminal(){return true;}

    public boolean equals(Token t) {
        return  value.equals(t.value);
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object obj) {
        Token t=(Token) obj;
        return this.value.equals(t.getValue());
    }
}
