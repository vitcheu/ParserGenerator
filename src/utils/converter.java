package utils;

import common.Token;

import java.io.*;
import java.util.*;

public class converter {
    static final String split = ",";
    static String dir = "BNF/";
    static String path = "resource/" + dir;
    static String name = "grammar2";
    static List<String> nonterminals=new LinkedList<>();
    static  List<String> terminals=new LinkedList<>();
    public static void main(String[] args) {

        try (PrintWriter pw = new PrintWriter(path + name + ".csv");
             BufferedReader bf = new BufferedReader(new FileReader(path + name + ".txt"));
             PrintWriter p2=new PrintWriter(path+"nonterminal2.csv")){
            String line = null;
            String head = null, body;
            String[] symbols;
            int cnt=8;
            while ((line = bf.readLine()) != null) {
                if (line.contains("::=")) {
                    String[] s1 = line.split("::=");
                    head = s1[0].trim();
                    if(!nonterminals.contains(head)){
                        nonterminals.add(head);
                        p2.write(head+split);
                        cnt++;
                        if(cnt%8==0) p2.println();
                    }
                    if (s1[1].contains("|")) {
                        String[] s2 = s1[1].split("\\|");
                        for (String s3 : s2) {
                            writeLine(head, s3.trim(), pw);
                        }
                    } else {
                        body = s1[1].trim();
                        writeLine(head, body, pw);
                    }
                } else if (line.trim().startsWith("|")) {
                    String[] s1 = line.split("\\|");
                    body = s1[1].trim();
                    writeLine(head, body, pw);
                }
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        addTerminal();
    }

    private static void addTerminal() {
        try(BufferedReader bf=new BufferedReader(new FileReader(path+name+".csv"));
            PrintWriter pw=new PrintWriter(path+"terminal2.csv")){
            String line=null;
            int cnt=8;
            while ((line=bf.readLine())!=null){
                if(line.equals("")){
                    continue;
                }
                String[] ss=line.split(split);
//                String token;
                for (String token:ss){
                    if(token.equals(""))
                        continue;
                    if(nonterminals.contains(token)) continue;
                    if(!terminals.contains(token)){
                       terminals.add(token);
                       pw.print(token+split);
                       cnt++;
                       if(cnt%8==0){
                           pw.println();
                       }
                    }
                }
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeLine(String head, String body, PrintWriter pw) {
        pw.print(head + split);
        String[] symbols = body.split("\\s+");
        for (String sym : symbols) {
            pw.print(sym + split);
        }
        pw.println();
    }
}
