package Generator;

import Generator.LALRGenerator.LRGenerator;
import common.NonTerminal;
import common.Production;
import common.Terminal;
import common.Token;

import java.io.*;
import java.util.*;

public class Generator {
    protected List<Terminal> terminals = new ArrayList<>();
    protected List<NonTerminal> nonTerminals = new ArrayList<>();
    protected HashMap<String, Token> tokenMap = new HashMap<>();
    protected HashMap<Integer, NonTerminal> nonterminalMap = new HashMap<>();
    protected HashMap<Integer, Terminal> terminalMap = new HashMap<>();
    protected HashMap<Integer, Production> productionMap = new HashMap<>();
    protected int[][] analysisTable;//语法分析表
    public static final int DEFAULT = Integer.MIN_VALUE;
    protected NonTerminal start;
    protected final String path;
    public final String workingDir
//            ="";
            ="D:/Code/CmmCompiler/";
    public final String tsvSplit = "\t";
    public final String comma=",";
    public PrintStream err = System.err;

    public Generator(String path) {
        this.path=workingDir+"resource/"+path;
    }


    public static void main(String[] args) {
        Generator generator = new LRGenerator("parser/");

        //构造语法分析表
        generator.readGrammar("grammar.csv");
        generator.generate();
        generator.printAllProductionsToFile();
        //读取词法分析器产生的词法单元流,进行语法分析
//        generator.analysis("input.txt");
    }

    /**
     *添加文法符号t
     */
    public void add(Token t) {
        tokenMap.put(t.getValue(), t);
        if (t.isTerminal()) {
            terminals.add((Terminal) t);
            terminalMap.put(t.getTid(), (Terminal) t);
        } else {
            nonTerminals.add((NonTerminal) t);
            nonterminalMap.put(t.getTid(), (NonTerminal) t);
        }
    }


    /**
     *从语法表中读取非终结符,终结符集合和产生式
     */
    public void readGrammar(String grammarFileName) {
        String grammarFile = path +grammarFileName;
        String line;

        String[] row;
        Token token;

        try (BufferedReader br = new BufferedReader(new FileReader(grammarFile));
            PrintWriter p2=new PrintWriter(path+"terminal.tsv");
            PrintWriter p1=new PrintWriter(path+"nonterminal.tsv")) {
            List<List<String>> bodies=new LinkedList<>();
            List<NonTerminal> heads=new LinkedList<>();
            int cnt1=6;
            int cnt2=6;
            //加载非终结符,保存产生式体
            while ((line = br.readLine()) != null) {
                if(line.equals("")||line.trim().startsWith("//")) continue;
                List<String> rightSide = new ArrayList<>();
                row = line.split(comma);
                //保存非终结符
                token=tokenMap.get(row[0].trim());
                if(token==null){
                    token= new NonTerminal(row[0].trim());
                    add(token);
                    p1.print(token+tsvSplit);
                    if(++cnt1%6==0){
                        p1.println();
                    }
                }
                heads.add((NonTerminal) token);

                //获取产生式体
                for(int i=1;i<row.length;i++){
                    rightSide.add(row[i].trim());
                }
                bodies.add(rightSide);
            }
            //获取开始符号
            start=nonTerminals.get(0);

            //加载非终结符和产生式
            add(Terminal.EOF);
            for(int i=0;i<bodies.size();i++) {
                List<Token> rightSide=new LinkedList<>();
                List<String> body=bodies.get(i);
                NonTerminal head=heads.get(i);
                for(String str:body){
                    if(str.trim().equals("ε")||str.equals("")){
                            continue;
                    }
                    token= tokenMap.get(str.trim());
                    if(token==null){
                        token=new Terminal(str.trim());
//                        System.out.println("加入终结符"+token);
                        add(token);
                        p2.print(token+tsvSplit);
                        if(++cnt2%6==0){
                            p2.println();
                        }
                    }
                    rightSide.add(token);
                }
                head.addProduction(rightSide);
            }
            add(Terminal.EPSILON);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    /**
     * 占位方法,在子类中重写
     * 主要完成构造语法分析表所需的各项工作
     */
    public void generate(){}

    /**
     * 初始化语法分析表
     */
    public void iniTable() {
        analysisTable = new int[nonTerminals.size()][terminals.size() - 1];
        for (int[] a : analysisTable) {
            Arrays.fill(a, DEFAULT);
        }
    }

    /**
     * 在分析表[nt,tm]位置写入产生式p的编号
     */
    public void writeToTable(NonTerminal nt, Terminal tm, Production p) {
        int nNum = nt.getTid(), tNum = tm.getTid(), pNum = p.getPid();
        if (analysisTable[nNum][tNum] != DEFAULT) {
            System.err.println("在[" + nt + "," + tm + "]项,产生式:\"" + productionMap.get(analysisTable[nNum][tNum])
                    + "\"和\"" + p + "\"发生冲突,写入失败!");
            return;
        }
        System.out.println("[" + nt + "," + tm + "]写入\t" + p);
        analysisTable[nNum][tNum] = pNum;
    }

    /**
     * 填充语法分析表,留给子类重写
     */
    public void fillTable() {
        ;
    }

    public void fillProductionMap() {
        List<Production> productions = getAllProductions();
        for (Production p : productions) {
            productionMap.put(p.getPid(), p);
        }
        System.out.println("productionMap:" + productionMap);
    }

    public void printAllProductions() {
        System.out.println("产生式列表:");
        List<Production> productions = getAllProductions();
        productions.sort((o1, o2) -> o1.getLeft().getValue().compareTo(o2.getLeft().getValue()));
        for (Production p : productions) {
            System.out.println(p);
        }
    }

    public void printAllProductionsToFile(){
        String productionFile="D:/Code/CmmCompiler/resource/parser/"+"production3.txt";
        List<Production> productions = getAllProductions();
        productions.sort(Comparator.comparing(o -> o.getPid()));
        try(PrintWriter pw=new PrintWriter(productionFile)){
            for(Production p:productions){
                pw.println(String.format("-----------------<%d>------------------",p.getPid()));
                pw.println(p);
                pw.println(String.format("-----------------</%d>------------------\n",p.getPid()));
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 返回所有产生式的列表
     */
    public List<Production> getAllProductions() {
        List<Production> result = new LinkedList<>();
        for (NonTerminal nt : nonTerminals) {
            for (Production p : nt.getProductions()) {
                result.add(p);
            }
        }
        return result;
    }

    /**
     * 计算所有非终结符的First集合
     */
    public void calculateFirstSet() {
        for (NonTerminal nt : nonTerminals) {
            if (!nt.isFinished())
                calculateFirstSet(nt);
        }
        //输出计算结果
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + "firstSet.csv"))) {
            for (NonTerminal nt : nonTerminals) {
                bw.write(nt + tsvSplit);
                for (Terminal t : nt.getFirstSet()) {
                    bw.write(t + tsvSplit);
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     *计算非终结符nt的first集合
     */
    public static Set<Terminal> calculateFirstSet(NonTerminal nt) {
        if (nt.isFinished())
            return nt.getFirstSet();
        System.out.println("\n************计算" + nt + "的first集合*************");
        Set<Terminal> result = new HashSet<>();

        List<List<Token>> alphaList=new ArrayList<>();
        List<List<Token>> betaList=new ArrayList<>();
        nt.fillAlphaAndBetaList(alphaList,betaList);

        boolean b=false;
        for(List<Token> beta:betaList){
            if(beta.size()==0) b=true;
            Set<Terminal>firstSet = calculateFirstSet(beta);
            System.out.println(nt+"的beta集合"+"得到first集合:" + firstSet);
            result.addAll(firstSet);
        }
        //betaList中存在空的产生式体
        if(b){
            for(List<Token> alpha:alphaList){
                Set<Terminal>firstSet = calculateFirstSet(alpha);
                System.out.println(nt+"的alpha集合"+"得到first集合:" + firstSet);
                result.addAll(firstSet);
            }
        }
//        for (Production p : nt.getProductions()) {
//            //此判断为没有消除左递归的LR生成器所用
//            if(!p.isEpsilon()&&p.getHeader().equals(nt)){
//                continue;
//            }
//
//            Set<Terminal> firstSet = calculateFirstSet(p.getRight());
//            System.out.println("从产生式" + p + "得到first集合:" + firstSet);
//            result.addAll(firstSet);
//        }
        System.out.println("\n************计算" + nt + "的first集合结束*************");
        nt.setFinished(true);//标志计算完毕
        nt.setFirstSet(result);
        return result;
    }

    /**
     *计算文法串tokens的first集合
     */
    public static Set<Terminal> calculateFirstSet(List<Token> tokens) {
        System.out.println("计算文法串" + tokens + "的first集合");
        Set<Terminal> result = new HashSet<>();
        if (tokens.size() == 0) {
            result.add(Terminal.EPSILON);
            return result;
        }
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (token.isTerminal()) {
                result.add((Terminal) token);
                return result;
            }
            Set<Terminal> firstSet = calculateFirstSet((NonTerminal) token);
            result.addAll(firstSet);
            if (!firstSet.contains(Terminal.EPSILON)) {
                return result;
            } else result.remove(Terminal.EPSILON);
        }
        result.add(Terminal.EPSILON);
        System.out.println("得到集合" + result);
        return result;
    }


    /**
     * 计算所有非终结符的follow集合
     */
    public void calculateFollowSet() {
        boolean updated = true;
        start.addFollow(Terminal.EOF);
        while (updated) {
            updated = false;
            for (NonTerminal nt : nonTerminals) {
                System.out.println("\n--------------考察" + nt + "的产生式----------");
                List<Production> productions = nt.getProductions();
                for (Production p : productions) {
                    System.out.println("遍历产生式" + p);
                    HashSet<Terminal> follows = new HashSet<>();
                    follows.addAll(nt.getFollowSet());
                    if (p.isEpsilon()) {
                        continue;
                    }
                    for (int i = p.getRight().size() - 1; i >= 0; i--) {
                        Token ti = p.getRight().get(i);
                        if (ti.isTerminal()) {
                            follows = new HashSet<>();
                            follows.add((Terminal) ti);
                            continue;
                        }
                        NonTerminal nti = (NonTerminal) ti;
                        int oriSize = nti.getFollowSet().size();
                        nti.addFollows(follows);
                        if (nti.getFollowSet().size() > oriSize)
                            updated = true;

                        //更新follows
                        Set<Terminal> fset = nti.getFirstSet();
                        if (fset.contains(Terminal.EPSILON)) {
                            follows.addAll(fset);
                            follows.remove(Terminal.EPSILON);
                        } else {
                            follows = (HashSet<Terminal>) fset;
                        }
                    }
                }
                System.out.println("\n----------考察" + nt + "的产生式结束-----------------");
            }
        }

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + "followSet.csv"))) {
            for (NonTerminal nt : nonTerminals) {
                bw.write(nt + tsvSplit);
                for (Terminal t : nt.getFollowSet()) {
                    bw.write(t + tsvSplit);
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 用栈对输入流进行语法分析,构造语法分析树
     */
    public void analysis(String sourceFile) {
        ;
    }

    /**
     * 输出语法分析树
     *
     * @param productions 语法分析过程中依次选择扩展或规约的产生式
     */
    public void showSyntaxTree(List<Production> productions) {
    }


}
