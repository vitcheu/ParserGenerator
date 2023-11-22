package Generator.LALRGenerator;

import Generator.Generator;
import common.NonTerminal;
import common.Production;
import common.Terminal;
import common.Token;

import java.io.*;
import java.util.*;

public class LRGenerator extends Generator {
    public static final int acc = -2;//分析表中表示接受状态
    public static final int errNo = -1;//表示错误状态
    public static final int syn = -3;//表示可跳过的错误
    private HashMap<Integer, State> collection = new HashMap<>();
    private HashMap<Set<Term>, State> stateMap = new HashMap<>();
    private State startState;
    private Term startTerm;
    private int startStateNum;
    private Production accProduction;
    private static PrintStream err;

    private Reducer reducer=new Reducer();

    public LRGenerator(String path) {
        super(path);
    }


    //传播表项,内容为(项集,项),以唯一定位一个传播和被传播的项
    class tableCell {
        State state;//所属项集
        Term term;//项

        public tableCell(State state, Term term) {
            this.state = state;
            this.term = term;
        }

        public State getState() {
            return state;
        }

        public Term getTerm() {
            return term;
        }

        @Override
        public String toString() {
            return "(S" + state.getId() + "," + term + ")";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            tableCell cell = (tableCell) o;
            return Objects.equals(state, cell.state) && Objects.equals(term, cell.term);
        }

        @Override
        public int hashCode() {
            return Objects.hash(state, term);
        }
    }

    public void generate(){
        try {
            err = new PrintStream(path + "error message.txt");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
        //构造增广文法
        expand();
        //构造项集族
        createSetCollection();
        createStateMap();
        //传播向前看符号
        fillLookups();
        //填充语法分析表
        fillProductionMap();
        fillTable();
        showAnalysisTable();

    }

    /**
     * 构造原文法的增广文法
     */
    public void expand() {
        NonTerminal oriStart = start;
        NonTerminal newStart = new NonTerminal(start + "'");
        add(newStart);
        List<Token> r = new LinkedList<>();
        r.add(oriStart);
        accProduction = new Production(newStart, r);
        newStart.addProduction(accProduction);
        start = newStart;

    }

    /**
     * 从初始项集开始,构造该文法的所有项集
     */
    private void createSetCollection() {
        //构造初始项集S'->•S
        startTerm = new Term(start.getProductions().get(0));
//        System.out.println("iniTerm=" + startTerm);
        Set<Term> set = new HashSet<>();
        set.add(startTerm);
        startState = new State(set, Terminal.EOF);
        startStateNum = startState.getId();
//        System.out.println("iniSet=" + startState);
        collection.put(startState.getId(), startState);

        //从这个项集开始,计算其他项集,直至没有新的项集加入
        //用队列Q表示新生成的项
        Queue<State> Q = new LinkedList<>();
        Q.add(startState);
        while (!Q.isEmpty()) {
            State top = Q.poll();
//            System.out.println("\n---------当前项集:" + top + "---------");
//            System.out.println("Closure:" + top.Closure());
            for (Token x : top.getDotFollowedToken()) {
                Set<Term> newSet = top.Goto(x);
                int id = getSetID(newSet);
//                System.out.println("id="+id);
                if (id == -1) {
                    State newState = new State(newSet, x);
                    //判断该项集是否可规约
                    boolean canReduced = false;
                    for (Term term : newSet) {
                        if (term.isReduced()) {
                            canReduced = true;
                            break;
                        }
                    }
                    newState.setReduced(canReduced);

                    collection.put(newState.getId(), newState);
                    Q.add(newState);
                    id = newState.getId();
                }
//                System.out.println("#" + top.getId() + " Goto->" + x + "=" + newSet + "#" + id);
            }
//            System.out.println("---------项集:" + top + "结束---------\n");
//            System.out.println("队列Q的大小:" + Q.size());
        }

    }


    public void createStateMap() {
        for (int i : collection.keySet()) {
            State state = collection.get(i);
            state.Closure();
            Set<Term> core = state.getCore();
            stateMap.put(core, state);
        }
    }

    /**
     * 查找与set中的项相同的项集
     *
     * @return 如果存在, 返回项集id, 否则返回-1
     */
    public int getSetID(Set<Term> set) {
        for (Integer i : collection.keySet()) {
            Set<Term> coreset = collection.get(i).getCore();
            if (coreset.toString().equals(set.toString()))
                return i;
        }
        return -1;
    }

    public void printSingleState(PrintStream ps, State state) {
        List<Term> list = new ArrayList<>(state.Closure());
        list.sort((o1, o2) -> o1.getProduction().getLeft().toString().compareTo(o2.getProduction().getLeft().toString()));
        ps.println("\n##############<状态" + state.getId() + ">#############");
        for (Term t : list) {
            if (state.getCore().contains(t)) {
                ps.println(t);
                ps.println();
            }
        }
        ps.println("--------------------------------------------");
        for (Term t : list) {
            if (!state.getCore().contains(t)) {
                ps.println(t);
                ps.println();
            }
        }
        ps.println("\n##############</状态" + state.getId() + ">#############");
    }

    public void printAllState() {
        try (PrintStream ps = new PrintStream(path + "States.txt");
             PrintStream ps2 = new PrintStream(path + "Reduce States.txt")) {
            List<State> reduce_States = new LinkedList<>();

            //找到具有规约项的状态
            for (int i : collection.keySet()) {
                State state = collection.get(i);
                List<Term> list = new ArrayList<>(state.Closure());
                for (Term t : list) {
                    if (t.isReduced() && !reduce_States.contains(state)) {
                        reduce_States.add(state);
                    }
                }
                //输出该状态
                printSingleState(ps, state);
            }

            //输出具有规约项的状态
            ps2.println("共有" + reduce_States.size() + "个规约项\n");
            for (State s : reduce_States) {
                printSingleState(ps2, s);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 对每个LR(0)项集,填充其对应的LR(1)向前看符号
     */
    public void fillLookups() {
        try (PrintWriter ps = new PrintWriter(path + "transfer table.txt")) {
            //初始化传播表,该表有两列,索引列为作为传播方的项;
            //第二列为被传播的项,需要用(项集,项)来定位
            HashMap<tableCell, List<tableCell>> transferTable = new HashMap<>();

            //Q记录已生成待传播其向前看符号的项
            Queue<tableCell> Q = new LinkedList<>();
            //从初始项开始传递
            startTerm.addLookup(Terminal.EOF);
            tableCell startCell = new tableCell(startState, startTerm);
            Q.add(startCell);
            calculateTransfer(Q, transferTable, startCell, ps);

            //开始传播
            while (!Q.isEmpty()) {
                tableCell curCell = Q.poll();
                transfer(Q, transferTable, curCell, ps);
            }
            System.out.println("传播完毕后");
            printAllState();

            //输出传播表
//            for(Term term:transferTable.keySet()){
//                List<tableCell> list=transferTable.get(term);
//                if(list==null||list.isEmpty()) continue;
//                ps.println("\n#######BEGIN#######");
//                ps.printf("****from %-45s  %-80s *******\n",term.toLR0String(),term.getLookup());
//                for(tableCell cell:transferTable.get(term)){
//                    if(cell.term.isReduced()){
//                        Term t=cell.getTerm();
//                        ps.printf("%-5s\t","@"+cell.getState().getId());
//                        ps.printf("%-45s\t",t.toLR0String());
//                        ps.printf("%-25s\n",t.getLookup());
//                    }
//                }
//                ps.println("#######END#######\n");
//            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 根据传播关系传播当前项的向前看符号并更新Q,传播关系的值某项能传播的项的集合
     *
     * @param Q     记录新生成而待传播的项
     * @param table 传播关系,在此方法中根据传播关系传播curTerm的向前看符号
     */
    private void transfer(Queue<tableCell> Q, HashMap<tableCell, List<tableCell>> table, tableCell curCell, PrintWriter ps) {
        //获取被传播的项的列表
        List<tableCell> cellList = table.get(curCell);
        if (table.get(curCell) == null) {
            throw new RuntimeException("传播关系尚未计算完毕!");
        }

        for (tableCell cell : cellList) {
            State srcState = curCell.getState();
            Term curTerm = curCell.getTerm();
            State state = cell.getState();
            Term t = cell.getTerm();

            //该项的向前看内容有所更新,需要传播此更新
            Set<Terminal> oriSet = new HashSet<>(t.getLookup());
            if (t.addAllLookup(curTerm.getLookup())) {
                List<Terminal> newList = calculateUpdatedList(oriSet, t.getLookup());
                ps.printf("\n@%-4s %-85s传播符号%-80s\t->\t@%-4s %-85s\n",
                        srcState.getId(), curTerm.toLR0String(), newList, state.getId(), t.toLR0String());
                ps.println("orgSize=" + oriSet.size() + ",curSize=" + t.getLookup().size());
                //在下一轮传播过程中,沿着传播关系传播该更新
                calculateTransfer(Q, table, cell, ps);
                if (!Q.contains(cell))
                    Q.add(cell);
            }
        }
    }

    /**
     * 计算项curCell中的项所能传播的项,相应地修改Q和table表的内容
     */
    private void calculateTransfer(Queue<tableCell> Q, HashMap<tableCell, List<tableCell>> table, tableCell curCell, PrintWriter ps) {
        //如果传播已经构造,则不用计算
//        ps.println("*********一个递归层************");
//        ps.println("Q:"+Q);
//        ps.println("table:"+table);
//        ps.println("curCell"+curCell);
        if (table.get(curCell) != null) {
//            ps.println(curCell + "的传播关系已计算或正在计算");
            return;
        }

        Term curTerm = curCell.getTerm();
        State curState = curCell.getState();
        //保存原lookup集合
        Set<Terminal> oriLookups = new HashSet<>(curTerm.getLookup());
        //置lookup集合为空
        curTerm.setLookup(new HashSet<>());
        List<Term> closure = State.LR1Closure(curState, curTerm, Terminal.SHARP);
        List<tableCell> transList = new LinkedList<>();
        table.put(curCell, transList);

        for (Term t : closure) {
            //
            if (t.isReduced()) {
                //t为空产生式且向前看符号可由curTerm传播,而t无传播能力
                if (t.getProduction().isEpsilon() && t.getLookup().contains(Terminal.SHARP)) {
                    t.getLookup().remove(Terminal.SHARP);
                    //传播
                    tableCell cell=new tableCell(curState,t);
                    if(!transList.contains(cell))
                        transList.add(cell);
//                    Set<Terminal> oriSet = new HashSet<>(t.getLookup());
//                    if (t.addAllLookup(oriLookups)) {
//                        List<Terminal> newList = calculateUpdatedList(oriSet, t.getLookup());
//                        ps.printf("\n@%-4s %-85s传播符号%-80s\t->\t@%-4s %-85s\n",
//                                curState.getId(), curTerm.toLR0String(),newList, curState.getId(), t.toLR0String());
//                    }
                }
                continue;
            }
            //计算t的后续所属的集合
            Set<Term> termj = curState.Goto(t.getCurrentToken());
            State Ij = stateMap.get(termj);
            Term succesor = findSuccessor(Ij, t);
            //构造被传播项
            tableCell cell = new tableCell(Ij, succesor);
            if (t.getLookup().contains(Terminal.SHARP)) {
                //断定项t的向前看符号由当前项传播而成
                if (!transList.contains(cell))
                    transList.add(cell);
                t.getLookup().remove(Terminal.SHARP);
            }

            //该项有其他的向前看符号,属于自发生成,有传播能力
            if (t.getLookup().size() >= 1) {
                Set<Terminal> oriSet = new HashSet<>(succesor.getLookup());
                //后继项successor加入此向前看集合
                if (succesor.addAllLookup(t.getLookup())) {
                    List<Terminal> newList = calculateUpdatedList(oriSet, succesor.getLookup());
//                    ps.printf("\n@%-4s %-85s传播符号%-80s\t->\t@%-4s %-85s\n",
//                            curState.getId(), t.toLR0String(), newList, Ij.getId(), succesor.toLR0String());
                    //构造传播项
                    tableCell newCell = new tableCell(Ij, succesor);
                    if (table.get(newCell) == null) {
                        calculateTransfer(Q, table, newCell, ps);
                    }
                    //加入Q中以传播此自动生成的向前看集合
                    if (!Q.contains(newCell)) {
                        Q.add(newCell);
//                        ps.println("Q中加入" + newCell);
//                        ps.println("Q.size=" + Q.size());
                    }
                }
            }
        }
        //恢复原lookup集合
        curTerm.setLookup(oriLookups);
    }

    /**
     * 在state中找到属于t的后续的项
     */
    public static Term findSuccessor(State state, Term t) {
        for (Term term : state.Closure()) {
            if (Term.isSuccesor(term, t)) {
                return term;
            }
        }
        return null;
    }

    private List<Terminal> calculateUpdatedList(Set<Terminal> original, Set<Terminal> current) {
        List<Terminal> newList = new LinkedList<>();
        for (Terminal tm : current) {
            if (!original.contains(tm)) {
                newList.add(tm);
            }
        }
        return newList;
    }

    //0至状态数-1表示移入/转移,更大的数表示规约,acc表示接受
    public void fillTable() {
        int numOfTerminal = terminals.size() - 1;//有效终结符数量
        int numOfNonterminal = nonTerminals.size() - 1;//去掉增广文法的开始符号
        int numOfState = collection.keySet().size();

//        for (Terminal t : terminals) {
////            System.out.println(t.getTid() + ":" + t);
//        }
//        for (NonTerminal nt : nonTerminals) {
////            System.out.println(nt.getTid() + ":" + nt);
//        }

        analysisTable = new int[collection.size()][numOfTerminal + numOfNonterminal];
        for (int[] a : analysisTable) {
            Arrays.fill(a, errNo);
        }

        for (int i : collection.keySet()) {
            State state = collection.get(i);
//            System.out.println("state=" + state);
            for (Token t : state.getDotFollowedToken()) {
                State desState = stateMap.get(state.Goto(t));
                int colOffset = (t.isTerminal()) ? 0 : numOfTerminal;
//                System.out.println("t=" + t + ",desState" + desState);
                writeToTable(i, t.getTid() + colOffset, desState.getId());
            }

            //填充规约项
            for (Term term : state.getCore()) {
                if (term.isReduced()) {
                    for (Terminal tml : term.getLookup()) {
                        Production p = term.getProduction();
                        int value = (p.equals(accProduction)) ? acc : p.getPid() + numOfState;
                        writeToTable(i, tml.getTid(), value);
                    }
                }
            }
        }
    }

    private void writeToTable(int r, int c, int value) {
        int oriValue = analysisTable[r][c];
        int numOfTerminals = terminals.size() - 1;
        String lxrvalue = (c < numOfTerminals) ? terminals.get(c).toString()
                : nonTerminals.get(c - numOfTerminals).toString();
        if(lxrvalue==null){
            throw  new RuntimeException("获取文法失败!");
        }

        if (oriValue != errNo) {
            String msg=String.format("\n在[@%-3d,%8s]\t中发生冲突,冲突项为: \t%-30s和\t%-30s\n",
                    r, lxrvalue, translateValue(oriValue), translateValue(value));
            err.println(msg);
//            throw new RuntimeException("该文法有冲突!");
            System.out.println(msg);
            System.out.println("现状态:");
            printSingleState(System.out,collection.get(r));
            Scanner s=new Scanner(System.in);
            System.out.println("\n*******************************************\n请选择冲突的解决方案:保留(N)还是写入新值(Y)?");
            String ans=s.next();
            if(ans.equals("Y")||ans.equals("y")){
                analysisTable[r][c]=value;
            }
            return;
        }
//        System.out.printf("[%d,%d]处写入%d\n", r, c, value);
        analysisTable[r][c] = value;
    }

    public void showAnalysisTable() {
        int numOfTerminal = terminals.size() - 1;//有效终结符数量
        int numOfNonterminal = nonTerminals.size() - 1;//去掉增广文法的开始符号
        int numOfState = collection.keySet().size();

//        System.out.println("nonTerminals:" + nonTerminals);
//        System.out.println("terminals:" + terminals);

        //输出至文件
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + "LALR analysis Table.tsv"));
            BufferedWriter bw1=new BufferedWriter(new FileWriter(path+"raw table.tsv"))) {
            bw.write("State" + tsvSplit);
            //写入头部的列名
            for (int i = 0; i < numOfTerminal; i++) {
                bw.write(terminals.get(i) + tsvSplit);
            }
            for (int i = 0; i < numOfNonterminal; i++) {
                bw.write(nonTerminals.get(i) + tsvSplit);
            }
            bw.write("\n");
            //写入正文
            for (int i = 0; i < numOfState; i++) {
                //写入行名
                bw.write(i + tsvSplit);
                for (int j = 0; j < numOfTerminal + numOfNonterminal; j++) {
                    int value = analysisTable[i][j];
                    bw1.write(value+tsvSplit);
                    if (value == acc) {
                        bw.write("acc" + tsvSplit);
                        continue;
                    }
                    if (value == errNo) {
                        bw.write("" + tsvSplit);
                    } else {
                        if (j < numOfTerminal) {
                            String prefix = (value < numOfState) ? "s" : "r";
                            int offset = (value < numOfState) ? 0 : numOfState;
                            bw.write(prefix + (value - offset) + tsvSplit);
                        } else {
                            bw.write(value + tsvSplit);
                        }
                    }
                }
                bw.write("\n");
                bw1.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public String translateValue(int value) {
        int numOfState = collection.keySet().size();
        if (value < numOfState) {
            return "shift to " + collection.get(value).getId();
        } else {
            Production p=productionMap.get(value - numOfState);
            return "reduced " + p+" (pid="+p.getPid()+")";
        }
    }

    @Override
    public void analysis(String sourceFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(path+sourceFile))) {
            /* 初始化语法分析栈 */
            Stack<Integer> analysisStack = new Stack<>();
            List<Token> stackContent = new LinkedList<>();
            //加入开始状态
            push(analysisStack, stackContent, startStateNum);
            //记录分析过程所选择的产生式
            List<Production> selectedProduciton = new LinkedList<>();

            String line;
            int lineCnt = -1;
            int numOfState = collection.keySet().size();
            int colOffset = terminals.size() - 1;
            boolean finished = false;

            /*开始分析过程*/
            System.out.println("*********语法分析开始*********");
//            System.out.println("栈的内容\t输入流符号\t动作");
            System.out.printf("%-96s %-10s %-15s %-18s \n", "栈的内容", "当前状态", "输入流符号", "动作");
            while ((line = reader.readLine()) != null && !finished) {
                lineCnt++;
                StringTokenizer  tokenizer= new StringTokenizer(line);
                Token a;
                String curWord;
                try {
                    curWord=tokenizer.nextToken();
                    a = tokenMap.get(curWord);
                }catch (NoSuchElementException e){
                    //跳过空行
                    continue;
                }

                while (!finished) {
                    int curState = analysisStack.peek();
                    System.out.printf("%-100s %-10d %-15s ", stackContent, curState, a);

                    if (a == null) {
                        String s="\n错误的输入符号:" + curWord;
                        err.println(s);
                        throw new RuntimeException(s);
                    }
                    int value = analysisTable[curState][a.getTid()];
                    //语法错误
                    if (value == errNo) {
                        err.println("语法错误,行号" + lineCnt);
                        throw new RuntimeException();
                    }
                    //动作为接受
                    if (value == acc) {
                        finished = true;
                        pop(analysisStack, stackContent);
                        System.out.printf("%-18s \n", "接受");
                        break;
                    }
                    //动作为移进
                    if (value < numOfState) {
                        System.out.printf("%-18s \n", "移入" + a);
                        //转到新状态
                        push(analysisStack, stackContent, value);
                        //读入下一个符号
                        //如果当前行读完,跳出循环,读入下一行
                        if (!tokenizer.hasMoreTokens()) {
                            break;
                        }
                        curWord=tokenizer.nextToken();
                        a = tokenMap.get(curWord);
                    }
                    //动作为规约
                    else {
                        Production pro = productionMap.get(value - numOfState);
                        System.out.printf("%-18s \n", "根据" + pro.toNotapString() + "规约");
                        selectedProduciton.add(pro);
                        reduced(analysisStack, stackContent, pro);
                    }
                }
                if (finished) {
                    //还有多余的程序,这里假设后面的输入流不全部为空行
                    if (tokenizer.hasMoreTokens() || reader.readLine() != null) {
                        System.err.println("多余的内容");
                    }
                    System.out.println("*****语法分析过程结束******");
                    System.out.println("选择的产生式依次为:");
                    for (common.Production p : selectedProduciton) {
                        System.out.println(p);
                    }
                    showSyntaxTree(selectedProduciton);
                    break;
                }
            }
        } catch (
                FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 使记录栈的内容的content能跟踪栈的变化,当移进一个终结符号时,content增加该符号
     *
     * @param i 分析栈中压入的状态号
     */
    private void push(Stack<Integer> stack, List<Token> content, int i) {
        stack.push(i);
        content.add(collection.get(i).getSymbol());
    }

    /**
     * 根据p规约,修改栈中的内容
     */
    private void reduced(Stack<Integer> stack, List<Token> content, Production p) {
        int colOffSet = terminals.size() - 1;
        //弹出栈的内容
        for (int i = 0; i < p.getRight().size(); i++) {
            pop(stack, content);
        }
        //执行动作
        reducer.reduce(p);

        //获取规约所得的非终结符
        NonTerminal nt = p.getLeft();
        //转到新状态
        int curState = stack.peek();
        int newState = analysisTable[curState][nt.getTid() + colOffSet];
        push(stack, content, newState);
    }

    /**
     * 语法分析栈的弹出
     */
    private void pop(Stack<Integer> stack, List<Token> content) {
        if (stack.empty()) {
            err.println("语法错误,分析栈已空!");
        }
        stack.pop();
        content.remove(content.size() - 1);
    }
}
