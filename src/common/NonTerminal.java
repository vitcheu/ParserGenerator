package common;

import java.util.*;

public class NonTerminal extends Token {
    public static int nid=4;
    private static int id=0;

    private List<Production> productions=new LinkedList<>();
    private Set<Terminal> firstSet=new HashSet<>();
    private Set<Terminal> followSet=new HashSet<>();
    private boolean finished=false;

    public NonTerminal(String value){
        super(value);
        super.setTid(id++);
    }

    @Override
    public boolean isTerminal() {
        return false;
    }

    public NonTerminal createTemp(){
        return new NonTerminal(getValue()+id++);
    }

    public void addFirst(Terminal t){
            firstSet.add(t);
    }

    public void addFollow(Terminal t){
            followSet.add(t);
    }
    public void addFollows(HashSet<Terminal> set){
        System.out.println("Follow("+this+")增加集合:"+set);
        for(Terminal t:set){
            followSet.add(t);
        }
    }

    public void setFirstSet(Set<Terminal> firstSet) {
        this.firstSet = firstSet;
    }

    public void setFollowSet(Set<Terminal> followSet) {
        this.followSet = followSet;
    }

    public void removeProduction(Production p){
        if(productions.contains(p)){
            productions.remove(p);
            System.out.println("删除产生式: "+p);
        }
    }

    public void addProduction(Production p){
        productions.add(p);
        System.out.println("生成新的产生式:"+p);
    }

    public void addProduction(List<Token> right){
        Production newP=new Production(this,right);
        productions.add(newP);
        System.out.println("生成新的产生式:"+newP);
    }

    public void fillAlphaAndBetaList(List<List<Token>>alphaList,List<List<Token>> betaList){
        //找到有直接左递归的产生式
        for (int i = 0; i <getProductions().size(); i++) {
            Production p = getProductions().get(i);
            if (p.getHeader() != null && p.getHeader().equals(this)) {//该产生式存在左递归
                System.out.println("找到一条递归式: " + p);
                alphaList.add(p.getAlpha());
            } else {
                betaList.add(p.getRight());
            }
        }
        if (alphaList.isEmpty()) {//无直接递归
            System.out.println(this + "无直接递归产生式");
        }
    }


   /**
    * @param p 被替换的产生式
    * @parm  B 用来选择替换的产生式的左侧终结符
    */
    public void replaceRight(Production p, NonTerminal B){
        List<Token> otherPart=new ArrayList<>(p.getRight().subList(1,p.getRight().size()));//产生式剩余部分

        for(Production pb:B.getProductions()){
            List<Token> newRight=new ArrayList<>();
            newRight.addAll(pb.getRight());//产生式的被替换部分
            newRight.addAll(otherPart);
            addProduction(new Production(this,newRight));
        }
//        removeProduction(p);
    }
    public void removeAllProduction(){
        productions=new LinkedList<>();
    }


    public Set<Terminal> getFollowSet() {
        return followSet;
    }

    public Set<Terminal> getFirstSet() {
        return firstSet;
    }

    public List<Production> getProductions() {
        return productions;
    }


    public boolean isFinished() {
        return finished;
    }

    public void setFinished(boolean finished) {
        this.finished = finished;
    }

}
