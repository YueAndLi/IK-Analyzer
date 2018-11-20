/**
 * IK 中文分词  版本 5.0
 * IK Analyzer release 5.0
 * <p>
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * 源代码由林良益(linliangyi2005@gmail.com)提供
 * 版权声明 2012，乌龙茶工作室
 * provided by Linliangyi and copyright 2012 by Oolong studio
 */
package org.wltea.analyzer.sample;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.ArrayUtil;
import org.wltea.analyzer.lucene.IKAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;


/**
 * 使用IKAnalyzer进行Lucene索引和查询的演示
 * <p>
 * 以下是结合Lucene6.2.1 API的写法
 */
public class LuceneIndexAndSearchDemo {


    public void init(String IQL, int cPage, int psize) {
        if (LUCENE_QUERY_MAXNUM == 0) {
            String para = null;
            try {
                para = "1000";
                LUCENE_QUERY_MAXNUM = Integer.valueOf(para);
            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("系统参数LUCENE_QUERY_MAXNUM错误:" + para);
            }
        }
        iql = IQL.trim();
        if (iql.indexOf("WHERE(") >= 0) {
            String where;
            where = iql.substring(iql.indexOf("WHERE(") + "WHERE(".length(), iql.indexOf(")"));
            String[] whereArray = where.split("=");
            if (whereArray.length < 2) {
                throw new RuntimeException("where错误");
            }
            title = whereArray[0];
            value = whereArray[1];

        }
        if (iql.indexOf("ORDER_BY(") >= 0) {
            String iqlpart = iql.substring(iql.indexOf("ORDER_BY("));
            group = iqlpart.substring(iqlpart.indexOf("ORDER_BY(") + "ORDER_BY(".length(), iqlpart.indexOf(")"));
        }

        this.currentPage = cPage;
        pageSize = psize;
    }

    private String iql;
    private String title;
    private String value;
    // 排序语句
    private String group;
    // 分页语句
    private int currentPage;
    private int pageSize;
    private static int LUCENE_QUERY_MAXNUM;

    public String[] getLimitTitle() {
        return title.trim().toUpperCase().split(",");
    }

    public String getLimitValue() {
        return value.replaceAll("\\(", "").replaceAll("\\)", "");
    }

    public int getMaxTopNum() {
        return LUCENE_QUERY_MAXNUM;
    }

    public Sort getSortTitle() {
        // ORDER_BY(dddd:int ASC,dddd:string ASC)
        // 注意被排序的字段必须被存储索引
        Sort sort = new Sort();
        if (group == null || group.trim().length() <= 0) {
            return sort;
        }
        String[] sortstr = group.split(",");
        SortField[] fields = new SortField[sortstr.length];
        try {
            for (int i = 0; i < sortstr.length; i++) {
                String sortString = sortstr[i];
                String[] para = sortString.trim().split(" ");
                String[] para2 = para[0].trim().split(":");
                String title = para2[0].trim().toUpperCase();
                String type = para2[1].trim().toUpperCase();
                String sortType = para[1].trim().toUpperCase();
                SortField.Type typeInt = null;
                if (title == null || title.length() <= 0 || type == null || type.length() <= 0 || sortType == null
                        || sortType.length() <= 0) {
                    throw new RuntimeException("排序语句错误" + group);
                }
                if (type.equals("DOUBLE")) {
                    typeInt = SortField.Type.DOUBLE;
                }
                if (type.equals("STRING")) {
                    typeInt = SortField.Type.STRING;
                }
                if (type.equals("LONG")) {
                    typeInt = SortField.Type.LONG;
                }

                fields[i] = new SortField(title, typeInt, sortType.toUpperCase().equals("DESC"));
            }
            sort.setSort(fields);
        } catch (Exception e) {
            throw new RuntimeException("排序语句错误" + group + "/" + e);
        }
        return sort;
    }

    /**
     * 模拟：
     * 创建一个单条记录的索引，并对其进行搜索
     *
     * @param args
     */
    public static void main(String[] args) {
        //Lucene Document的域名
        String fieldName = "text";
        //检索内容
        String text = "IK Analyzer是一个结合词典分词和文法分词的中文分词开源工具包。它使用了全新的正向迭代最细粒度切分算法。";

        //实例化IKAnalyzer分词器
        Analyzer analyzer = new IKAnalyzer(true);

        Directory directory = null;
        IndexWriter iwriter = null;
        IndexReader ireader = null;
        IndexSearcher isearcher = null;
        try {
            //建立内存索引对象
            directory = new RAMDirectory();

            //配置IndexWriterConfig
            IndexWriterConfig iwConfig = new IndexWriterConfig(analyzer);
            iwConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
            iwriter = new IndexWriter(directory, iwConfig);
            //写入索引
            Document doc = new Document();
            doc.add(new StringField("ID", "10000", Field.Store.YES));
            doc.add(new TextField(fieldName, text, Field.Store.YES));
            iwriter.addDocument(doc);
            iwriter.close();


            IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
            IndexWriter fswriter = new IndexWriter(FSDirectory.open(Paths.get("D:\\luceneIndex")), iwc);
            fswriter.addIndexes(new Directory[]{directory});
            fswriter.close();


            //搜索过程**********************************
            //实例化搜索器
            ireader = DirectoryReader.open(directory);
            isearcher = new IndexSearcher(ireader);

            String keyword = "中文";
            //使用QueryParser查询分析器构造Query对象
            QueryParser qp = new QueryParser(fieldName, analyzer);
            qp.setDefaultOperator(QueryParser.AND_OPERATOR);
            Query query = qp.parse(keyword);
            System.out.println("Query = " + query);

            //搜索相似度最高的5条记录
            TopDocs topDocs = isearcher.search(query, 5);
            System.out.println("命中：" + topDocs.totalHits);
            //输出结果
            ScoreDoc[] scoreDocs = topDocs.scoreDocs;
            for (int i = 0; i < topDocs.totalHits; i++) {
                Document targetDoc = isearcher.doc(scoreDocs[i].doc);
                System.out.println("内容：" + highLightText(targetDoc, query, new String[]{"ID", fieldName}));
            }

        } catch (CorruptIndexException e) {
            e.printStackTrace();
        } catch (LockObtainFailedException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        } finally {
            if (ireader != null) {
                try {
                    ireader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (directory != null) {
                try {
                    directory.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public List<File> initdir(String path) {

        List<File> list = new ArrayList<>();

        File file = new File(path);
        if (file.exists() && file.isDirectory()) {
            list.add(file);
            for (File tempFile : file.listFiles()) {
                if (tempFile.isDirectory()) {
                    list.add(tempFile);
                }
            }
        }
        return list;
    }

    public static Map<String, Object> highLightText(Document doc, Query query, String[] fields) {
        Map<String, Object> map = new HashMap<String, Object>();
        SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter(
                "<font color\\=red>",
                "</font>");
        QueryScorer queryScorer = new QueryScorer(query);
        Highlighter highlighter = new Highlighter(simpleHTMLFormatter, queryScorer);
        highlighter.setTextFragmenter(new SimpleFragmenter(20));
        String result = "";
        for (IndexableField field : doc.getFields()) {
            String title = field.name();
            String value = doc.get(field.name());
            try {
                if(ArrayUtils.contains(fields,title)){
                    String highText = highlighter.getBestFragment(new IKAnalyzer(), title, value);
                    result += highText;
                    if(highText != null){
                        value = highText;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InvalidTokenOffsetsException e) {
                e.printStackTrace();
            }
            map.put(title, value);
        }
        return map;
    }

    public IRResult queryByMultiIndex(String Iql, int currentPage, int pageSize, List<File> indexFiles) {

        IRResult irResult = IRResult.getInstance(currentPage);
        IndexSearcher searcher = null;
        Directory directory = null;
        long startTime = new Date().getTime();

        try {
            System.out.println("EXCUTE-IQL:" + Iql + " at " + indexFiles);
            init(Iql, currentPage, pageSize);

            // 查询构造
            QueryParser mqp = new MultiFieldQueryParser(getLimitTitle(), new IKAnalyzer());
            Query qp = mqp.parse(getLimitValue());
            // 开始排序----------------------------------------------------
            Sort sort = getSortTitle();
            // 索引文件集合
            IndexReader[] readers = new IndexReader[indexFiles.size()];

            for (int i = 0; i < indexFiles.size(); i++) {
                if (!indexFiles.get(i).isDirectory()) {
                    // 打开后立即关闭相当于初始化索引目录。
                    indexFiles.get(i).mkdirs();
                }
                directory = FSDirectory.open(Paths.get(indexFiles.get(i).getAbsolutePath()));

                IndexReader reade = DirectoryReader.open(directory);
                readers[i] = reade;
            }
            MultiReader mr = new MultiReader(readers);
            searcher = new IndexSearcher(mr);
            TopFieldDocs search = searcher.search(qp, Integer
                    .valueOf(getMaxTopNum()), sort);
            ScoreDoc[] sco = search.scoreDocs;
            int totleSize = sco.length;
            // 将结果截取用户需要的
            sco = subDoc(sco);
            List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
            for (int i = 0; i < sco.length; i++) {
                Document targetDoc = searcher.doc(sco[i].doc);
                list.add(highLightText(targetDoc, qp, getLimitTitle()));
            }
            irResult = IRResult.getInstance(currentPage);
            irResult.setPageSize(pageSize);
            irResult.setTotleSize(totleSize);
            irResult.setResultList(list);

        } catch (Exception e) {
            e.printStackTrace();
        }

        long endTime = new Date().getTime();
        System.out.println("共检索到记录" + irResult.getTotleSize() + "条，用时"
                + (endTime - startTime) + "毫秒");
        irResult.setRuntime(endTime - startTime);
        irResult.setTotalPage((irResult.getTotleSize() - 1) / irResult.getPageSize()
                + 1);
        return irResult;
    }

    public ScoreDoc[] subDoc(ScoreDoc[] allDoc) {
        int curentSize = allDoc.length - ((currentPage - 1) * pageSize);
        if (curentSize <= 0) {
            curentSize = 0;
        }
        if (curentSize > pageSize) {
            curentSize = pageSize;
        }
        ScoreDoc[] newScore = new ScoreDoc[curentSize];
        int m = 0;
        for (int i = ((currentPage - 1) * pageSize); i < ((currentPage - 1) * pageSize) + curentSize; i++) {
            if (allDoc.length < i) {
                break;
            }
            newScore[m] = allDoc[i];
            m++;
        }
        return newScore;
    }



}
