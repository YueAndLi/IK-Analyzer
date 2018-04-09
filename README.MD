本来打算用在自己的网站搜索中的，所以特意般了过来改造了下，但我找到一个感觉更好的项目：

- [HanLP](http://hanlp.linrunsoft.com/index.html)

IK就这么放这里吧

#IK-Analyzer 中文分词

基于lucene 7.2.1

使用maven依赖

分词演示

    org.wltea.analyzer.sample.IKAnalzyerDemo

查询演示

    org.wltea.analyzer.sample.LuceneIndexAndSearchDemo
    
Maven打包

    1. 通过mvn clean 清理target
    2. 通过mvn package来构建jar包
    3. /Users/huiyu/ideaProject/myCoding/IK-Analyzer/target/IK-Analyzer-1.0-SNAPSHOT.jar
    
使用说明：

    1. 通过Maven打包;
    2. 将IK-Analyzer-1.0-SNAPSHOT.jar方法使用的项目lib中，
    3、test的resouces中的三个文件也要放入到使用的项目resource中
    4、hotword.dic为要分的词，stopword.dic为停用词
    