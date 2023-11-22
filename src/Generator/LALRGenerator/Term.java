package Generator.LALRGenerator;

import common.NonTerminal;
import common.Production;
import common.Terminal;
import common.Token;

import java.util.*;

/**
 * LR(0)项,向前看符号由另外的过程填充
 */
public class Term {
    private static int id = 0;
    private Production production;
    private int pos;
    private Set<Terminal> lookup=new HashSet<>();
    private int tid;

    public Term(Production production) {
        this.production = production;
        this.pos = 0;
    }

    public Term(Production production, int pos) {
        this.production = production;
        this.pos = pos;
    }

    public Term(Production production, Set<Terminal> lookupSet){
        this.production = production;
        this.pos = 0;
        this.lookup.addAll(lookupSet);
    }

    /**
     * 对非终结符号A,构造A的所有产生式对应的初始项
     */
    public static List<Term> createTerms(NonTerminal A) {
        List<Term> result = new LinkedList<>();
        for (Production p : A.getProductions()) {
            result.add(new Term(p));
        }
        return result;
    }

    public static List<Term> createTerms(NonTerminal A, Set<Terminal> firstSet) {
        List<Term> result = new LinkedList<>();
        for (Production p : A.getProductions()) {
            Term t=new Term(p,firstSet);
//            System.out.println("得到非内核项:"+t+",lookup:"+ t.lookup);
            result.add(t);
        }
        return result;
    }

    public Production getProduction() {
        return production;
    }

    public int getPos() {
        return pos;
    }

    public Set<Terminal> getLookup() {
        return lookup;
    }

    public void setLookup(Set<Terminal> lookup) {
        this.lookup = lookup;
    }

    /**
     * 加入向前看符号的集合
     * @return 如果向前看集合更新,返回true,否则返回false
     */
    public boolean addAllLookup(Set<Terminal> lookup){
        int oriSize=this.lookup.size();
        this.lookup.addAll(lookup);
        int curSize=this.lookup.size();
        return curSize!=oriSize;
    }

    /**
     * 加入向前看符号
     * @return 如果向前看集合更新,返回true,否则返回false
     */
    public  boolean addLookup(Terminal terminal){
        if(lookup.contains(terminal)){
            return false;
        }
        lookup.add(terminal);
        return true;
    }

    public int getTid() {
        return tid;
    }

    /**
     * 展示LR0项的内容(不包括向前看符号)
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("[");
        sb.append(production.getLeft() + "->");
        for (int i = 0; i < production.getRight().size(); i++) {
            if (i == pos) {
                sb.append("•");
            }
            sb.append(production.getRight().get(i) + " ");
        }
        if (isReduced()) {
            sb.replace(sb.length() - 1, sb.length(), "•,"+((lookup==null)?"":lookup)+"]");
        } else {
            sb.replace(sb.length() - 1, sb.length(), ","+((lookup==null)?"":lookup)+"]");
        }
        return sb.toString();
    }

    /**
     *判断t1是否为t2的后续
     */
    public static boolean isSuccesor(Term t1,Term t2){
        if(t2.isReduced())
            return false;
        if(t1.getProduction().equals(t2.production)&&
           t2.getPos()+1==t1.getPos()){
            return true;
        }
        return false;
    }

    public Term getSuccesor(){
        if(isReduced()) return null;
        else return new Term(production,pos+1);
    }

    public String toLR0String(){
        StringBuilder sb = new StringBuilder("[");
        sb.append(production.getLeft() + "->");
        for (int i = 0; i < production.getRight().size(); i++) {
            if (i == pos) {
                sb.append("•");
            }
            sb.append(production.getRight().get(i) + " ");
        }
        if (isReduced()) {
            sb.replace(sb.length() - 1, sb.length(), "•]");
        } else {
            sb.replace(sb.length() - 1, sb.length(), "]");
        }
        return sb.toString();
    }


    /**
     * @return 该项是否是一个规约项
     */
    public boolean isReduced() {
        return pos >= production.getRight().size();
    }

    /**
     * @return 当前期待接受的文法符号, 即圆点后面的第一个符号
     */
    public Token getCurrentToken() {
        if (isReduced()) {
            return null;
        }
        return production.getRight().get(pos);
    }

    public boolean equals(Object o) {
        Term t=(Term) o;
        return this.hashCode()==t.hashCode();
    }

    public void setTid(int tid) {
        this.tid = tid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(production, pos);
    }
}
