# ParserGenerator
俗称编译器的编译器,也就是语法分析器生成器,包括LL(1)型文法和LALR文法

输入:以CSV格式描述的文法,每一行代表一个产生式,其中第一个单词表示产生式头,为非终结符,其余部分表示产生式体,为终结符或非终结符或表示空产生式的"ε";

输出:语法分析表,指导语法分析器对于特定输入符号和栈信息进行相应的分析动作;所解析的终结符和非终结符集;所有语法分析状态的描述.

本程序还可以对输入的非终结符流进行语法分析.

其中的输入输出文件均储存于工作目录下的resource/目录,产生语法分析表时,需要此目录下的grammar.csv文件;而进行语法分析时,需要input.txt文件;
