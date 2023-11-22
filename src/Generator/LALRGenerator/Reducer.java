package Generator.LALRGenerator;

import common.Production;


//在这个类里统一放置规约时的动作代码
public class Reducer {

    public Reducer(){
        ;
    }

    //根据产生式选择规约的动作
    public void reduce(Production p){
        switch (p.getPid()){
            case 0:{
                System.out.println("程序结束!");
            }
            case 1:{

            }
            case 2:{

            }
            case 3:
            {

            }
            //.......
            default:{
//                System.out.println("归约到产生式:"+p);
            }
        }
    }
}
