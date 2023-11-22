package Generator.LALRGenerator;

import Generator.Generator;
import com.sun.org.apache.bcel.internal.generic.GOTO;
import com.sun.org.apache.bcel.internal.generic.GOTO_W;
import common.NonTerminal;
import common.Terminal;
import common.Token;

import java.util.*;

public class State {
    private static int setID = 0;
    private Token symbol;
    private boolean reduced;
    private Set<Term> core;
    private int id;

    public State(Set<Term> core) {
        this.core = core;
        id = setID++;
    }

    public State(Set<Term> core, Token symbol) {
        this.core = core;
        id = setID++;
        this.symbol = symbol;
    }

    public Token getSymbol() {
        return symbol;
    }

    public boolean isReduced() {
        return reduced;
    }

    public Set<Term> getCore() {
        return core;
    }

    public int getId() {
        return id;
    }

    /**
     * 根据内核集,计算该项集的LR(0)闭包
     */
    public Set<Term> Closure() {
        Set<Term> set = new HashSet<>();
        set.addAll(core);
        Queue<Term> Q = new LinkedList<>();
        Q.addAll(core);
        while (!Q.isEmpty()) {
            Term term = Q.poll();
            if (term.isReduced()) {
                if (!core.contains(term)) {
                    core.add(term);
                }
                continue;
            }

            Token token = term.getCurrentToken();
            if (!token.isTerminal()) {
                List<Term> newTerms = Term.createTerms((NonTerminal) token);
                for (Term t : newTerms) {
                    //如果项是新得到的,加入Q以在下一轮循环中处理
                    if (!set.contains(t)) {
                        Q.add(t);
                    }
                    set.add(t);
                }
            }
        }
        return set;
    }

    /**
     * 计算LR(1)项的闭包
     */
    public static List<Term> LR1Closure(State s, Term term, Terminal lookup) {
        LinkedList<Term> list = new LinkedList<>();
        if (term.isReduced()) return list;

        //添加特殊向前看符号#
        term.addLookup(lookup);
        list.add(term);

        //Q记录待计算的项
        Queue<Term> Q = new LinkedList<>();
        Q.add(term);
        while (!Q.isEmpty()) {
            Term cur = Q.poll();
            Token token = cur.getCurrentToken();
            if (cur.isReduced()) {
                continue;
            }
            if (!token.isTerminal()) {
                //获取向前看符号,为token之后的产生式的First集合
                List<Token> rightpart = new LinkedList<>(cur.getProduction().getRight().subList(cur.getPos() + 1,
                        cur.getProduction().getRight().size()));
                //用于判断产生的新项是否含有当前项的向前看集合
                rightpart.add(Terminal.AT);
                Set<Terminal> firstSet = Generator.calculateFirstSet(rightpart);

                //如果有一个"@"符号,则需要将其替换为当前项的向前看集合
                if (firstSet.contains(Terminal.AT)) {
                    firstSet.remove(Terminal.AT);
                    firstSet.addAll(cur.getLookup());
                }
                System.out.println("***得到集合:"+firstSet);

                //生成非内核项
                List<Term> newTerms = Term.createTerms((NonTerminal) token, firstSet);
                for (Term t : newTerms) {
                    //如果t是一个空产生式的项,则该项存在于内核中
                    //此时需要将得到的向前看符号由新产生的项传递至内核的相同项
//                    System.out.println("    \tt="+t);
                    if (t.isReduced()) {
                        System.out.println("\n$$$$$$$t=" + t + "$$$$$$$$");
                        Term coreTerm=s.getTerm(t);
                        if(coreTerm==null) throw new RuntimeException();
                        coreTerm.addAllLookup(t.getLookup());
                        System.out.println("coreTerm:" + coreTerm + "中加入向前看符号::" + t.getLookup());
                        if(coreTerm.getLookup().contains(Terminal.SHARP)&&!list.contains(coreTerm)){
                            list.add(coreTerm);
                        }
                        continue;
                    }

                    //如果项是新得到的,加入Q以在下一轮循环中处理
                    if (!list.contains(t)) {
//                        System.out.println("Q和list加入" + t);
                        Q.add(t);
                        list.add(t);
                    }
                    //该项和list中某一元素在LR(0)项上是相等的,但可能向前看符号不同
                    else {
                        for (Term term1 : list) {
                            if (term1.equals(t) &&
                                    term1.addAllLookup(t.getLookup()) && !Q.contains(term1)) {//向前看符号有所更新,应该加入Q继续生成新项
//                                System.out.println(term1 + "增加向前看符号");
                                Q.add(term1);
                            }
                        }
                    }
                }
            }
        }

        return list;
    }


    //找到真内核
    public void findRealCore(Set<Term> set) {
        Queue<Term> Q = new LinkedList<>();
        Q.addAll(set);
        Set<Term> expanded = new HashSet<>();
        expanded.addAll(set);
        while (!Q.isEmpty()) {
            Term term = Q.poll();

            //如果该项是规约项,则不能产生新项
            if (term.isReduced()) {
                //如果是空产生式的规约项,需要加入内核
                if (term.getProduction().isEpsilon() && !set.contains(term)) {
//                    System.out.println("this: " + this);
//                    System.out.println("*********内核:" + set + "增加项:" + term + "***********");
                    set.add(term);
                }
                continue;
            }

            Token token = term.getCurrentToken();
            if (!token.isTerminal()) {
                List<Term> newTerms = Term.createTerms((NonTerminal) token);
                for (Term t : newTerms) {
                    //如果项是新得到的,加入Q以在下一轮循环中处理
                    if (!expanded.contains(t)) {
                        expanded.add(t);
                        Q.add(t);
                    }
                }
            }
        }
//        System.out.println("得到真内核:"+set);
    }

    /**
     * 跳转函数,对于输入文法符号x,给出接受x后的项集的内核
     */
    public Set<Term> Goto(Token x) {
        Set<Term> set = new HashSet<>();
        Set<Term> closureSet = Closure();
        for (Term term : closureSet) {
            Token curToken = term.getCurrentToken();
            if (curToken != null && curToken.equals(x)) {
                Term newTerm = new Term(term.getProduction(), term.getPos() + 1);
                set.add(newTerm);
            }
        }
//        System.out.println("得到假内核:"+set);
//        System.out.println("GOTO("+this+","+x+")= "+set);
        //找到真内核
        //因为内核中可能还有形如A->•的规约项,需要从Closure过程中找出来
        findRealCore(set);
        return set;
    }


    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("#" + id + " $" + getSymbol() + "{");
        for (Term t : core) {
            sb.append(t + ",");
        }
        sb.deleteCharAt(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }

    //返回圆点后的文法集合
    public Set<Token> getDotFollowedToken() {
        Set<Token> result = new HashSet<>();
        Set<Term> clsSet = Closure();
        for (Term t : clsSet) {
            if (t.isReduced()) continue;
            result.add(t.getCurrentToken());
        }
        return result;
    }

    /**
     * 返回与t在LR(0)项上相同的项,不存在则返回null
     */
    public Term getTerm(Term t) {
        for (Term term1 : Closure()) {
            if (term1.equals(t)) {
                return term1;
            }
        }
        return null;
    }

    public void setReduced(boolean reduced) {
        this.reduced = reduced;
    }
}
