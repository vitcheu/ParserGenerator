package Lexer;

import common.Terminal;

import java.io.*;
import java.util.*;

import static java.lang.Character.*;

/**
 * 该词法分析器输入C--语言的词素流,
 * 输出语法分析所用的词法单元流tokens
 * 主要将数字,字符,字符串规约为相应类型的常量,将标识符统一规约为id
 * 其他如关键字,分隔符,运算符等,保留其词素的值
 * 唯一的例外是逗号,因为语法分析需要读入csv文件,所以特殊地,逗号被表示成值为comma的词法单元
 */
public class Lexer {
    private List<Terminal> tokens;
    private int lineCnt = 0;
    public final static int NORMAL = 0;

    int state;
    private char cur;
    private String multiLineStr;

    private String line;
    private int pos = 0;
    public final static int IN_BLOCK_COOMENT = 2;
    public final static int IN_STRING = 3;
    public static String path="resource/lexer/";
    private PrintWriter err = new PrintWriter(path+"scan error.txt");
    private final static List<String> keyWords;
    static {
        keyWords=new LinkedList<>();
        String filePath="resource/C--/"+"terminal.tsv";
        try (BufferedReader reader=new BufferedReader(new FileReader(filePath))){
            String line;
            while ((line=reader.readLine())!=null){
                Scanner scanner=new Scanner(line);
                while (scanner.hasNext()){
                    keyWords.add(scanner.next());
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
//            = Arrays.asList(new String[]{"int", "float", "char", "unsigned", "signed", "long", "double" ,
//            "if", "else", "do", "return", "for", "while", "until", "goto", "break", "continue", "static", "const",
//            "void", "struct", "typedef", "union", "sizeof", "switch"});

    public Lexer() throws FileNotFoundException {
    }


    public List<Terminal> scan(String filename) {
        tokens = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            state=NORMAL;
            while ((line = reader.readLine()) != null) {
                tokenize(line);
                lineCnt++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tokens;
    }

    /**
     *扫描特定行,解析得到词法词法单元并加入tokens
     */
    private void tokenize(String line) {
        System.out.printf("\n-------第%d行开始,line=%s\n",lineCnt,line);
        pos=0;
        while ((cur=getChar())!= '\n') {
            StringBuilder sb = new StringBuilder();
//            System.out.println("********循环开始,cur="+cur);
            switch (state) {
                case NORMAL:
                    if (isWhitespace(cur)) {
                        if (cur == '\n')
                            break;
                        System.out.println("空白符:"+cur);
                        pos++;
                    }

                    else if (Character.isDigit(cur)) {
                        if (cur == '0') {
                            consume(sb);
                            //小数
                            if (cur == '.') {
                                sb.append(decimal());
                                tokens.add(new Terminal("FLOAT",sb.toString(), lineCnt));
                                System.out.println("小数:"+sb.toString());
                                continue;
                            }
                            //十六进制数
                            else if (cur == 'x' || cur == 'X') {
                                consume(sb);
                                sb.append(hex());
                            }
                            //八进制数
                            else if (cur >= '0' && cur < '8') {
                                sb.append(octal());
                            }
                            sb.append(opt_UL());
                            System.out.println("解析到数字:" + sb);
                            tokens.add(new Terminal("INTEGER", sb.toString(), lineCnt));
                        }
                        //[1-9]
                        else {
                            consume(sb);
                            sb.append(number());
                            if (cur == '.') {
                                consume(sb);
                                sb.append(decimal());
                                tokens.add(new Terminal("FLOAT",sb.toString(), lineCnt));
                                System.out.println("解析到数字:" + sb);
                            } else {
                                String exp = exp();
                                if (!exp.isEmpty()) {
                                    sb.append(exp);
                                    sb.append(opt_DF());
                                    tokens.add(new Terminal("FLOAT", sb.toString(), lineCnt));
                                    System.out.println("解析到数字:" + sb);
                                } else {
                                    sb.append(opt_UL());
                                    tokens.add(new Terminal("INTEGER", sb.toString(), lineCnt));
                                    System.out.println("解析到数字:" + sb);
                                }
                            }
                        }


                    }
                    //标识符
                    else if (Character.isJavaIdentifierStart(cur)) {
                        consume(sb);
                        sb.append(identity());
                        System.out.println("id="+sb);
                        if (keyWords.contains(sb.toString())) {
                            tokens.add(new Terminal(sb.toString(),lineCnt));
                        }else{
                            tokens.add(new Terminal("id",sb.toString(),lineCnt));
                        }

                    } else if (cur == '.') {
                        consume(sb);
                        //成员操作符
                        if (Character.isDigit(cur)||Character.isAlphabetic(cur)||cur=='_') {
                            tokens.add(new Terminal(".",lineCnt));
                            continue;
                        }
                        //省略整数部分的小数
                        if (Character.isDigit(cur)) {
                            sb.append('0');
                            sb.append(decimal());
                        } else {
                            err.println(lineCnt + ": 错误的点号");
                        }

                    }

                    else if (cur == '+' || cur == '-') {
                        char c = cur;
                        consume(sb);
                        //+int或-int
//                        if (Character.isDigit(cur) && cur != '0') {
//                            consume(sb);
//                            sb.append(number());
//                            tokens.add(new Terminal(sb.toString(), tokenKind.INTEGER, lineCnt));
//                        }
                        //++或--
                         if (cur == c) {
                            consume(sb);
                        }
                        //+=或-=
                        else if (cur == '=') {
                            consume(sb);
                        }
                        tokens.add(new Terminal(sb.toString(),lineCnt));
                    }

                    else if(cur=='*'||cur=='/'||cur=='%'||cur=='!'){
                        if(cur=='/'&&lookahead()=='/'){
                            //进入行注释,丢弃行内其他所有字符
                            pos=line.length();
                        }else if(cur=='/'&&lookahead()=='*') {
                            //进入块注释
                            pos+=2;
                            cur=getChar();
                            state=IN_BLOCK_COOMENT;
                        }else {
                            consume(sb);
                            if (cur == '=') {
                                consume(sb);
                            }
                            tokens.add(new Terminal(sb.toString(),lineCnt));
                            System.out.println("****解析到运算符："+sb);
                        }
                    }

                    else if(cur=='&'||cur=='|'||cur=='='){
                        char c=cur;
                        consume(sb);
                        if(cur==c){
                            consume(sb);
                        }
                        tokens.add(new Terminal(sb.toString(),lineCnt));
                    }

                    else if(cur=='<'||cur=='>'){
                        char c=cur;
                        consume(sb);
                        if(cur==c){
                            consume(sb);
                        }else if(cur=='='){
                            consume(sb);
                        }
                        tokens.add(new Terminal(sb.toString(),lineCnt));
                    }

                    else if(cur=='~'||cur=='{'||cur=='}'||cur=='['||cur==']'||cur==';'||cur==','||cur=='?'
                            ||cur==':'||cur=='('||cur==')'){
                        if(cur==','){
                            sb.append("comma");
                            pos++;
                            cur=getChar();
                        }else {
                            consume(sb);
                        }
                        tokens.add(new Terminal(sb.toString(),lineCnt));
                    }

                    //字符串
                    else if(cur=='\"'){
                        pos++;//丢弃\”
                        cur=getChar();
                        multiLineStr=new String();
                        str(sb);
                    }

                    //字符
                    else if(cur=='\''){
                        pos++;
                        cur=getChar();
                        if(cur=='\\'){
                            consume(sb);
                            //八进制数
                            if(cur>='0'&&cur<'7'){
                                for(int i=0;i<3;i++){
                                    if(cur>='0'&&cur<='7')
                                        consume(sb);
                                    else {
                                        err.println("\n期望八进制数,得到:,"+sb+cur);
                                    }
                                }
                            }else{
                                consume(sb);
                            }
                        }else if(cur!='\n'&&cur!='\''){
                            consume(sb);
                        }

                        if(cur=='\''){
                            pos++;
                            cur=getChar();
                        }else{
                            err.println("非法的字符:"+sb+cur);
                            continue;
                        }
                        tokens.add(new Terminal("CHAR",sb.toString(),lineCnt));
                        System.out.println("解析到字符："+sb);
                    }
                    break;

                case IN_BLOCK_COOMENT:
                    while (cur!='\n'&&cur!='*'&&lookahead()!='/'){
                        pos++;
                        cur=getChar();
                    }
                    if(cur=='\n') break;
                    else{
                        state=NORMAL;
                        pos+=2;
                        cur=getChar();
                    }
                    break;

                case IN_STRING:
                    str(sb);
                    break;
            }
        }
    }

    /**
     * 获取当前输入流的字符
     */
    public char getChar() {
        if (pos == line.length()) {
            return '\n';//表示结束
        } else {
            return line.charAt(pos);
        }
    }

    /**
     * 向前看一个字符
     */
    public char lookahead() {
        if (pos+1== line.length()) {
            return '\n';
        } else {
            return line.charAt(pos + 1);
        }
    }

    /**
     * 消耗一个字符
     */
    public void consume(StringBuilder sb) {
        sb.append(getChar());
        pos++;
        cur = getChar();
    }

    /**
     * @return 从当前位置开始扫描的数字串
     * 即[digit]*
     */
    public String number() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(cur)) {
            consume(sb);
        }
        System.out.println("#number获得："+sb);
        return sb.toString();
    }

    /**
     * 扫描指数部分
     */
    public String exp() {
        StringBuilder sb = new StringBuilder();
        //可选的指数部分
        if (cur == 'E' || cur == 'e') {
            consume(sb);
            //读入指数值
            String numStr = number();
            if (numStr.isEmpty()) {
                err.println("\n指数部分为空!");
                sb = new StringBuilder();
            } else {
                sb.append(numStr);
            }
        }
        return sb.toString();
    }

    /**
     * 读入可选的后置D或F
     */
    public String opt_DF() {
        if (cur == 'D' || cur == 'F') {
            pos++;
            return String.valueOf(cur);
        }
        return "";
    }

    public String opt_UL() {
        if (cur == 'U' || cur == 'L') {
            pos++;
            return String.valueOf(cur);
        }
        return "";
    }

    /**
     * 读入小数部分,由数字串,可选的指数部分,可选的D或F标志组成
     */
    public String decimal() {
        StringBuilder sb = new StringBuilder();
        String num1 = number(),
                exp = exp(),
                opt = opt_DF();
        if (num1.isEmpty()) {
            sb.append("0");
        }
        sb.append(num1);
        sb.append(exp);
        sb.append(opt);
        return sb.toString();
    }

    /**
     * 读入八进制数字
     * [0-7]*
     */
    public String octal() {
        StringBuilder sb = new StringBuilder();
        while (cur >= '0' &&cur <= '7') {
            consume(sb);
        }
        return sb.toString();
    }

    /**
     * 读入十六进制数字
     * [0-f]+
     */
    public String hex() {
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(cur) || cur >= 'a' &&
                cur<= 'f') {
            consume(sb);
        }
        return sb.toString();
    }

    /**
     * 读入标识符串
     */
    public String identity() {
        //进入此函数时已经判断nextChar为[1-9]
        StringBuilder sb = new StringBuilder();
        while (Character.isDigit(cur)||Character.isAlphabetic(cur)||cur=='_') {
            consume(sb);
        }
        return sb.toString();
    }

    /**
     *读取多行字符串
     */
    public void str(StringBuilder sb){
        while ((cur=getChar())!='\"'){
            if(isWhitespace(cur)) {
                pos++;
                continue;
            }
            if(cur!='\\'&&cur!='\n'){
               consume(sb);
            }else if(cur=='\\'){
                //多行字符,丢弃'\',暂时保存其他字符
                if(lookahead()=='\n'){
                    multiLineStr+=sb.toString();
                    state=IN_STRING;
                    pos++;
                    break;
                }
                consume(sb);
                //八进制数
                if(cur>='0'&&cur<'7'){
                    for(int i=0;i<3;i++){
                        if(cur>'0'&&cur<'7')
                            consume(sb);
                        else {
                            err.println("\n期望八进制数,得到:,"+sb+cur);
                        }
                    }
                }
            }else{
                err.println("\n此格式的字符串不能跨越多行,需要在行末增加\\!");
            }
        }
        //字符串结束
        if(cur=='\"'){
        pos++;//丢弃\”
        cur=getChar();
        multiLineStr+=sb;
        state=NORMAL;
        tokens.add(new Terminal("STRING",multiLineStr,lineCnt));
        System.out.println("#str解析到字符串："+multiLineStr);
        }
    }


    public static void main(String[] args) throws FileNotFoundException {
        Lexer lexer = new Lexer();
        List<Terminal> tokens = lexer.scan(path+"source.c");
        // 输出词法单元
//        for (Terminal token : tokens) {
//            System.out.println(token.getValue());
//        }
        int cnt=8;
        try(PrintWriter ps=new PrintWriter("resource/C--/"+"input.txt")){
            for (Terminal token : tokens) {
                ps.print(token.getValue()+" ");
                if(++cnt%8==0)
                    ps.println();
            }
        }
    }
}