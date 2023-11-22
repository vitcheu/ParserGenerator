package Generator.LL1Generator;

import Generator.Generator;
import common.NonTerminal;
import common.Production;
import common.Terminal;
import common.Token;

import java.io.*;
import java.util.*;

public class LLGenerator extends Generator {
    public LLGenerator(String path) {
        super(path);
    }

//    public static void main(String[] args) {
//        LLGenerator g = new LLGenerator();
////        g.readToken();
//
//
//        //对输入的源程序进行语法分析
//        g.analysis("input.txt");
//
//    }

    @Override
    public void generate() {
        //改造文法,使其尽量适合自顶向下的递归分析方法
        eliminateEpsilonProduction();
        eliminateLeftRecursion();
        extractLeftFactor();
        printAllProductions();

        //计算First和Follow集合
        calculateFirstSet();
        calculateFollowSet();

        //构造语法分析表
        fillTable();
    }

    /**
     * 消除ε产生式
     */
    public void eliminateEpsilonProduction() {
        //找到所有有空产生式的非终结符
        boolean updated = true;
        HashMap<NonTerminal, Boolean> hasEpsilon = new HashMap<>();
        while (updated) {
            updated = false;//用于标记本轮遍历所有非终结符过程中,hasEpsilon的内容是否更新,若无,表明其内容已收敛到正确的值
            for (NonTerminal Nt : nonTerminals) {
                Boolean b2 = hasEpsilon.get(Nt);
                //若已经标记,跳过下面的处理
                if (b2 != null && b2.booleanValue()) continue;

                boolean Ntflag = false;//用于标记本轮遍历非终结符Nt的过程中,能否发现空产生式
                for (Production p : Nt.getProductions()) {
                    if (p.getRight().isEmpty()) {//有直接的空产生式
                        Ntflag = true;
                        break;
                    }
                    //遍历产生式体,判断是否能间接产生空串
                    boolean b = true;
                    for (Token t : p.getRight()) {
                        if (t.isTerminal()) {//产生式体有终结符,该产生式不可能为空
                            b = false;
                            break;
                        } else {//该非终结符可能没有空产生式
                            Boolean b1 = hasEpsilon.get((NonTerminal) t);
                            if (b1 == null || b1 != null && b1.equals(Boolean.FALSE)) {
                                b = false;
                                break;
                            }
                        }
                    }
                    if (b) {
                        //该产生式必定能产生空串
                        Ntflag = true;
                        break;
                    }
                }
                if (Ntflag) {
                    hasEpsilon.put(Nt, true);
                    updated = true;
                    System.out.println("找到一个能产生空串的非终结符:" + Nt);
                } else {
                    hasEpsilon.put(Nt, false);
                }
                System.out.println("hasEpsilon: " + hasEpsilon);
            }
        }

        /**
         * 转换含有能产生空串的终结符的产生式
         */
        HashMap<NonTerminal, Boolean> modified = new HashMap<>();
        for (NonTerminal nt : hasEpsilon.keySet()) {
            if (hasEpsilon.get(nt)) {
                modified.put(nt, false);
            }
        }

        for (NonTerminal nt : modified.keySet()) {
            //转换所有含有nt的产生式
            for (NonTerminal nonTerminal : nonTerminals) {
                List<Production> productions = nonTerminal.getProductions();
                Production removePro = null;
                int proSize = productions.size();
                //遍历终结符nonterminal的产生式
                for (int i = 0; i < proSize; i++) {
                    Production p = productions.get(i);
                    if (nonTerminal.equals(nt) && p.getRight().size() == 0) {
                        removePro = p;//删除nt的空产生式
                        continue;
                    }
                    LinkedList<Integer> idxes = new LinkedList<>();
                    List<Production> newProList = new LinkedList<>();//该产生式转换后的产生式集合
                    //遍历产生式体
                    for (int j = 0; j < p.getRight().size(); j++) {
                        Token t = p.getRight().get(j);
                        if (!t.isTerminal() && t.equals(nt)) {
                            idxes.add(j);
                        }
                    }
                    if (idxes.isEmpty()) continue;

                    //生成新的产生式
                    int n = (int) (Math.pow(2, idxes.size()));
                    for (int k = 1; k < n; k++) {
                        int[] bitmap = getBitmap(k, idxes.size());
                        List<Integer> selector = select(idxes, bitmap);
                        Production newPro = replaceWithEpsilon(p, selector);
                        if (!newProList.contains(newPro)) {
                            newProList.add(newPro);
                        }
                    }

                    for (Production np : newProList) {
                        nonTerminal.addProduction(np);
                    }
                }
                if (removePro != null) {
                    nonTerminal.removeProduction(removePro);
                }
            }
        }
        printAllProductions();
    }

    /**
     * 将整数i转换为二进制位的数组
     */
    private int[] getBitmap(int i, int len) {
        int[] result = new int[len];
        int remain, q = 1;
        len--;
        while (q != 0) {
            q = i / 2;
            remain = i % 2;
            result[len--] = remain;
        }
        return result;
    }

    private List<Integer> select(List<Integer> source, int[] bitmap) {
        List<Integer> result = new LinkedList<>();
        for (int i = 0; i < source.size(); i++) {
            if (bitmap[i] == 1) {
                result.add(source.get(i));
            }
        }
        return result;
    }


    /**
     * @param selector 被空串替换的位置
     * @param p        产生式
     * @return 将idx位置的非终结符删去的新产生式
     */
    public Production replaceWithEpsilon(Production p, List<Integer> selector) {
        List<Token> lside = new LinkedList<>(),
                rside = new LinkedList<>(),
                newRight = new LinkedList<>();
        for (int i = 0; i < p.getRight().size(); i++) {
            if (!selector.contains(i)) {
                newRight.add(p.getRight().get(i));
            }
        }
        System.out.println("selector:" + selector);
        Production newP = new Production(p.getLeft(), newRight);
        return newP;
    }

    public void eliminateLeftRecursion() {
        int n = nonTerminals.size();
        for (int i = 0; i < n; i++) {
            NonTerminal A = nonTerminals.get(i);
            System.out.println("Ai= " + A);
            List<Production> pA = A.getProductions();

            for (int j = 0; j < i; j++) {
                NonTerminal B = nonTerminals.get(j);
                System.out.println("Aj= " + B);
                int pNum = pA.size();
                LinkedList<Production> removeProd = new LinkedList<>();//记录待删除的产生式
                for (int k = 0; k < pNum; k++) {
                    Production p = pA.get(k);
                    if (p.getHeader() != null && p.getHeader().equals(B)) {
                        //用B的所有产生式替换A的产生式右部的第一个B
                        System.out.println("用" + B + "的产生式替换产生式\"" + p + "\"的一部分");
                        removeProd.add(p);
                        A.replaceRight(p, B);
                    }
                }
                for (Production p : removeProd) {
                    A.removeProduction(p);
                }
                System.out.println("替换后," + A + "的产生式为:" + A.getProductions());
            }
            eliminateLeftRecursion(A);
        }
        System.out.print("消除递归后,");
        printAllProductions();
    }

    /**
     * 消除A的产生式的直接左递归
     */
    public void eliminateLeftRecursion(NonTerminal A) {
        /*
         * A的产生式有如下形式:A->Aα|β
         * α是所有递归式的右边剩余部分
         * β是无递归产生式的右边
         * 则A生成的句型的形式为:βα+
         * α和β都是文法串的列表,当列表长度大于1时,考虑用新的非终结符号产生α或β
         * */
        int tmp = 0;
        List<List<Token>> alphaList = new ArrayList<>();//有递归的产生式式的右边其余部分
        List<List<Token>> betaList = new ArrayList<>();//无递归的产生式的右边
        //找到有直接左递归的产生式
        A.fillAlphaAndBetaList(alphaList, betaList);
        A.removeAllProduction();

        NonTerminal A1 = null, A2 = null, A3 = null;
        System.out.println("alphaList: " + alphaList);
        System.out.println("betaList " + betaList);
        //生成A1,A2
        if (!betaList.isEmpty()) {//如果β为空,不生成终结符A1,A2
            //生成A1
            if (betaList.size() > 1) {
                A1 = new NonTerminal(A.getValue() + "1");
                add(A1);
                for (List<Token> beta : betaList) {
                    Production np = new Production(A1, beta);
                    A1.addProduction(beta);
                }
            }
            //生成A2
            A2 = new NonTerminal(A.getValue() + "2");
            add(A2);
        }
        //生成A3
        if (alphaList.size() > 1) {
            A3 = new NonTerminal(A.getValue() + "3");
            add(A3);
            for (List<Token> alpha : alphaList) {
                A3.addProduction(alpha);
            }
        }

        //增加A和A2的产生式,A的产生式可能为A->A1A2或者A->A3A
        if (betaList.isEmpty()) {//A->A3A,不存在A2
            List<Token> Ar = new ArrayList<>();
            if (A3 == null) {
                Ar.addAll(alphaList.get(0));
            } else {
                Ar.add(A3);
            }
            Ar.add(A);
            A.addProduction(Ar);
        } else {
            List<Token> Ar = new ArrayList<>(), Ar2 = new ArrayList<>();
            if (A1 == null) {
                Ar.addAll(betaList.get(0));
            } else {
                Ar.add(A1);
            }
            Ar.add(A2);
            A.addProduction(Ar);

            if (A3 == null) {
                Ar2.addAll(alphaList.get(0));
            } else {
                Ar2.add(A3);
            }
            Ar2.add(A2);
            A2.addProduction(Ar2);
            A2.addProduction(new ArrayList<>());

        }
    }


    /**
     * 提取产生式之间的公共左因子
     */
    public void extractLeftFactor() {
        int nsize = nonTerminals.size();
        for (int i = 0; i < nsize; i++) {
            NonTerminal nt = nonTerminals.get(i);
            extractLeftFactor(nt);
        }
    }

    public void extractLeftFactor(NonTerminal nt) {
        System.out.println("\n******************\n******提取" + nt + "的产生式的公因子******");
        List<Production> productions = nt.getProductions();
        boolean[] visited = new boolean[productions.size()];
        Arrays.fill(visited, false);
        int psize = productions.size();
        List<Production> removedList = new LinkedList<>();
        for (int i = 0; i < psize; i++) {
            if (!visited[i]) {
                visited[i] = true;
//                System.out.println("visited of "+nt+":"+Arrays.toString(visited));
                Production pi = productions.get(i);
                System.out.println("寻找等价类[" + pi + "]");
                if (pi.getRight().size() == 0)//空串,直接跳过
                    continue;

                List<Production> commonPro = new LinkedList<>();
                commonPro.add(pi);
                Token ti = pi.getRight().get(0);

                //找到和pi有公共左因子的产生式
                for (int j = 0; j < psize; j++) {
                    Production pj = productions.get(j);
                    if (pj.getRight().size() == 0)
                        continue;
                    Token tj = pj.getRight().get(0);
                    if (j != i && !visited[j] &&
                            tj.equals(ti)) {
                        commonPro.add(pj);
                        System.out.println("等价类中加入" + pj);
                        visited[j] = true;
                        removedList.add(pj);
                    }
                }
                if (commonPro.size() == 1) {//没有其他产生式与pi有相同前缀
                    System.out.println("没有其他产生式与\"" + pi + "\"有相同前缀");
                    continue;
                }
                removedList.add(pi);
                //寻找commonList中的最长公共前缀α
                List<Token> alpha = findLCP(commonPro);
                System.out.println("找到最长前缀:" + alpha);
                //生成新的产生式
                NonTerminal newNt = nt.createTemp();
                List<Token> newRight = new LinkedList<>();
                newRight.addAll(alpha);
                newRight.add(newNt);
                System.out.print("生成新产生式#");
                nt.addProduction(newRight);//加入形式为A->αA1的产生式
                //加入新的非终结符
                add(newNt);
                //为新的非终结符构造产生式
                for (int j = 0; j < commonPro.size(); j++) {
                    Production pj = commonPro.get(j);
                    List<Token> otherpart = pj.subList(alpha.size(), pj.getRight().size());//其余部分
                    newNt.addProduction(otherpart);
                }

                //递归地对新产生的非终结符提取公因式
                extractLeftFactor(newNt);
            }
        }

        //删除原产生式
        for (Production p : removedList) {
            System.out.print("删除原产生式#");
            nt.removeProduction(p);
        }
        System.out.println("递归过程$" + nt + "结束\n**************************");
    }

    private List<Token> findLCP(List<Production> commonPro) {
        int k = 1;
        Production p = commonPro.get(0);

        //纵向查找
        for (; k < p.getRight().size(); k++) {
            Token pt = p.getRight().get(k);
            boolean found = false;
            for (int j = 1; j < commonPro.size(); j++) {
                Production pj = commonPro.get(j);
                if (k >= pj.getRight().size()) {
                    found = true;
                    break;
                }
                Token tj = pj.getRight().get(k);
                if (!tj.equals(pt)) {
                    found = true;
                    break;
                }
            }
            if (found) break;
        }
        System.out.println("@sublist:k=" + k);
        return p.subList(0, k);
    }

    /**
     * 用栈对输入流进行语法分析,构造语法分析树
     */
    public void analysis(String sourceFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFile))) {
            /* 初始化语法分析栈 */
            List<Token> analysisStack = new LinkedList<>();
            //加入结束符和开始符号
            analysisStack.add(Terminal.EOF);
            analysisStack.add(start);
            //记录分析过程所选择的产生式
            List<Production> selectedProduciton = new LinkedList<>();

            String line;
            int lineCnt = -1;
            System.out.println();

            /*开始分析过程*/
            System.out.println("*********语法分析开始*********");
//            System.out.println("栈的内容\t输入流符号\t动作");
            System.out.printf("%-56s %-15s %-18s \n", "栈的内容", "输入流符号", "动作");
            while ((line = reader.readLine()) != null) {
                lineCnt++;
                StringTokenizer tokenizer = new StringTokenizer(line);

                Token a = tokenMap.get(tokenizer.nextToken());
                while (!analysisStack.isEmpty()) {
                    Token top = analysisStack.get(analysisStack.size() - 1);
                    System.out.printf("%-60s %-15s ", analysisStack, a);

                    if (a == null) {
                        err.println("错误的输入符号:" + a);
                        throw new RuntimeException();
                    }
                    if (a.equals(top)) {
                        System.out.printf("%-18s \n", "匹配" + a);
                        analysisStack.remove(analysisStack.size() - 1);
                        //读入下一个符号
                        //如果当前行读完,跳出循环,读入下一行
                        if (!tokenizer.hasMoreTokens()) {
                            break;
                        }
                        a = tokenMap.get(tokenizer.nextToken());
                    } else if (top.isTerminal()) {//栈顶符号为错误的终结符
                        err.println("#" + lineCnt + " 匹配错误!expected:" + top + ",given:" + a);
                        throw new RuntimeException();
                    } else {
                        int p = analysisTable[top.getTid()][a.getTid()];
                        if (p == DEFAULT) {
                            err.println("#" + lineCnt + "错误! " + top + "不能根据" + a + "选择任何产生式!");
                            throw new RuntimeException();
                        } else {//栈顶符号为非终结符
                            Production pro = productionMap.get(p);
                            System.out.printf("%-18s \n", pro.toNotapString());
                            selectedProduciton.add(pro);
                            //弹出栈顶符号
                            analysisStack.remove(analysisStack.size() - 1);
                            if (pro.getRight().size() == 0) {
                                continue;
                            }
                            //将选择的产生式体压入栈,并且最左符号位于栈顶
                            for (int i = pro.getRight().size() - 1; i >= 0; i--) {
                                Token t = pro.getRight().get(i);
                                analysisStack.add(t);
                            }
                        }
                    }
                }
                if (analysisStack.isEmpty()) {
                    System.out.println("*****语法分析过程结束******");
//                    System.out.println("选择的产生式依次为:");
//                    for (common.Production p : selectedProduciton) {
//                        System.out.println(p);
//                    }
                    showSyntaxTree(selectedProduciton);
                    break;
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 输出语法分析树,本方法为递归方法的驱动
     *
     * @param productions 语法分析过程中依次选择扩展的产生式
     */
    public void showSyntaxTree(List<Production> productions) {
        try (PrintStream p = new PrintStream(new FileOutputStream(path + "Syntax Analysis Result.txt"))) {
            showSyntaxTree(productions, start, 0, 0, p);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 递归过程,输出非终结符nt的编号为num的产生式的内容
     *
     * @param productions 语法分析中选择的产生式
     * @param nt          待输出内容的非终结符
     * @param num         下一条可用产生式的索引
     * @param level       递归层次,用于输出层次化的制表符
     * @param printStream 输出流
     * @return 本次递归所使用的最后一条产生式的索引
     */
    private int showSyntaxTree(List<Production> productions, NonTerminal nt, int num, int level, PrintStream printStream) {
        Production p = productions.get(num);
        List<Token> right = p.getRight();
        //输出产生式头
        printTap(level, printStream);
        printStream.println("<" + p.getLeft() + ">");

        //输出产生式体
        for (int i = 0; i < right.size(); i++) {
            Token token = right.get(i);
            if (token.isTerminal()) {
                printTap(level + 1, printStream);
                printStream.println("[" + token + "]");
            } else {
                num = showSyntaxTree(productions, (NonTerminal) token, num + 1, level + 1, printStream);
            }
        }

        //再次输出产生式头
        printTap(level, printStream);
        printStream.println("<" + p.getLeft() + ">");
        return num;
    }

    private void printTap(int n, PrintStream p) {
        for (int i = 0; i < n; i++) {
            p.print("\t");
        }
    }

    public void fillTable() {
        super.iniTable();
        fillProductionMap();
        for (NonTerminal nt : nonTerminals) {
            int cnt = 0;//能推导出空串的产生式的个数
            System.out.println("\n******填充第" + nt.getTid() + "行(" + nt + "):********");
            for (Production p : nt.getProductions()) {
                Set<Terminal> firstSet = calculateFirstSet(p.getRight());
                System.out.println("得到firstSet:" + firstSet);
                for (Terminal tm : firstSet) {
                    if (!tm.equals(Terminal.EPSILON)) {
                        writeToTable(nt, tm, p);
                    }
                }
                if (firstSet.contains(Terminal.EPSILON)) {
                    System.out.println("该产生式能推导出空");
                    cnt++;
                    if (cnt > 1) {
                        System.err.println(nt + "有两个及以上的产生式能推导出空串!\n停止为产生式" + p + "分配表项");
                        continue;
                    }
                    Set<Terminal> followSet = nt.getFollowSet();
                    System.out.println("得到followSet" + followSet);
                    for (Terminal t : followSet) {
                        writeToTable(nt, t, p);
                    }
                }
            }
        }
        //输出至文件
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(path + "analysisTable.csv"))) {
            System.out.println("nonterminalMap:" + nonterminalMap);
            bw.write("" + tsvSplit);
            //写入头部的列名
            for (int i = 0; i < analysisTable[0].length; i++) {
                bw.write(terminals.get(i) + tsvSplit);
            }
            bw.write("\n");
            //写入正文
            for (int i = 0; i < analysisTable.length; i++) {
                //写入非终结符作为行名
                bw.write(nonterminalMap.get(i) + tsvSplit);
                for (int j = 0; j < analysisTable[0].length; j++) {
                    if (analysisTable[i][j] == DEFAULT) {
                        bw.write("error" + tsvSplit);
                    } else {
                        bw.write(productionMap.get(analysisTable[i][j]).toCsvString() + tsvSplit);
                    }
                }
                bw.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
