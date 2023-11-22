package common;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Production {
    private static int id=0;
    private NonTerminal left;
    private List<Token> right;
    private  int pid;

    public Production(NonTerminal left,List<Token> right){
        this.left=left;
        this.right=right;
        this.pid=id++;
    }

    public Production(NonTerminal left,String[] right){
        this.left=left;
        this.right=new LinkedList<>();
        for(String str:right){
            Token t=new Token(str);
            this.right.add(t);
        }
    }

    public void extractLeftFactor(int pos, int len,int id) {
        NonTerminal lt=this.left;
        NonTerminal factor=new NonTerminal(lt.getValue()+id);
        List<Token> fRight=new LinkedList<>();
        List<Token> otherPart=getRight().subList(pos+len,getRight().size());

        //更新该产生式
        List<Token> newRight=new LinkedList<>();
        newRight.add(factor);
        newRight.addAll(otherPart);
        right=newRight;
    }


    public String[] getRightStr(){
        StringBuilder stringBuilder=new StringBuilder();
        for(Token t:right){
            stringBuilder.append(t.toString()+" ");
        }
        String[] result=stringBuilder.toString().split(" ");
        return result;
    }

    public List<Token> subList(int begin,int end){
        List<Token> result=new LinkedList<>();
        if(end<begin) throw new IndexOutOfBoundsException("end<begin!end="+end+",begin="+begin);
        for(int i=begin;i<end;i++){
            result.add(right.get(i));
        }
        return result;
    }

    public boolean isEpsilon(){
        return right.size()==0;
    }

    public Token getHeader(){
        if(right.size()>=1)
        return right.get(0);
        else return null;//为空产生式
    }

    public List<Token> getAlpha(){
        return new ArrayList<>(right.subList(1,right.size()));
    }


    public NonTerminal getLeft() {
        return left;
    }

    public List<Token> getRight() {
        return right;
    }

    @Override
    public String toString() {
        StringBuilder sb=new StringBuilder(left+"\t->\t");
        for(Token t:right){
            sb.append(t.toString()+"\t");
        }
        return sb.toString();
    }

    public int getPid() {
        return pid;
    }
    public String toCsvString(){
        StringBuilder sb=new StringBuilder(left+"\t->\t");
        for(Token t:right){
            sb.append(t.toString()+" ");
        }
        return sb.toString();
    }
    public String toNotapString(){
        StringBuilder sb=new StringBuilder(left+"-> ");
        for(Token t:right){
            sb.append(t.toString()+" ");
        }
        return sb.toString();
    }


}
