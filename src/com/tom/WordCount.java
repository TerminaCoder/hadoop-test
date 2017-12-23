package com.tom;  
  
import java.io.File;  
import java.io.IOException;  
import java.text.SimpleDateFormat;  
import java.util.Date;  
import java.util.StringTokenizer;  
  
import org.apache.hadoop.conf.Configuration;  
import org.apache.hadoop.fs.Path;  
import org.apache.hadoop.io.IntWritable;  
import org.apache.hadoop.io.Text;  
import org.apache.hadoop.mapred.JobConf;  
import org.apache.hadoop.mapreduce.Job;  
import org.apache.hadoop.mapreduce.Mapper;  
import org.apache.hadoop.mapreduce.Reducer;  
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;  
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;  
   
public class WordCount {  
   
    /** 
     * 用户自定义map函数，对以<key, value>为输入的结果文件进行处理 
     * Map过程需要继承org.apache.hadoop.mapreduce包中Mapper类，并重写其map方法。 
     * 通过在map方法中添加两句把key值和value值输出到控制台的代码 
     * ，可以发现map方法中value值存储的是文本文件中的一行（以回车符为行结束标记），而key值为该行的首字母相对于文本文件的首地址的偏移量。 
     * 然后StringTokenizer类将每一行拆分成为一个个的单词 
     * ，并将<word,1>作为map方法的结果输出，其余的工作都交有MapReduce框架处理。 每行数据调用一次 Tokenizer：单词分词器 
     */  
    public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable> {  
        private final static IntWritable one = new IntWritable(1);  
        private Text word = new Text();  
   
        /* 
         * 重写Mapper类中的map方法 
         */  
        public void map(Object key, Text value, Context context)  
                throws IOException, InterruptedException {  
            StringTokenizer itr = new StringTokenizer(value.toString());  
            //System.out.println(value.toString());  
            while (itr.hasMoreTokens()) {  
                word.set(itr.nextToken());// 获取下个字段的值并写入文件  
                context.write(word, one);  
            }  
        }  
    }  
   
    /** 
     * 用户自定义reduce函数，如果有多个热度测，则每个reduce处理自己对应的map结果数据 
     * Reduce过程需要继承org.apache.hadoop.mapreduce包中Reducer类，并重写其reduce方法。 
     * Map过程输出<key,values>中key为单个单词，而values是对应单词的计数值所组成的列表，Map的输出就是Reduce的输入， 
     * 所以reduce方法只要遍历values并求和，即可得到某个单词的总次数。 
     */  
    public static class IntSumReducer extends  Reducer<Text, IntWritable, Text, IntWritable> {  
        private IntWritable result = new IntWritable();  
        public void reduce(Text key, Iterable<IntWritable> values,  
                Context context) throws IOException, InterruptedException {  
            int sum = 0;  
            for (IntWritable val : values) {  
                sum += val.get();  
            }  
            result.set(sum);  
            context.write(key, result);  
        }  
    }  
   
    public static void main(String[] args) throws Exception {  
   
        /** 
         * 环境变量配置 
         */ 
    	//用户一定要和电脑的用户名称一样才可以运行hadoop   	
    	//每次运行Hadoop需要删除Hadoop的tmp文件
        File jarFile = EJob.createTempJar("bin");  
        ClassLoader classLoader = EJob.getClassLoader();  
        Thread.currentThread().setContextClassLoader(classLoader);  
   
        /** 
         * 连接hadoop集群配置 
         */  
        Configuration conf = new Configuration(true);  
        conf.set("fs.default.name", "hdfs://localhost:9000");  
        conf.set("hadoop.job.user", "Google");  
        conf.set("mapreduce.framework.name", "yarn");  
        conf.set("mapreduce.jobtracker.address", "localhost:50020");  
        conf.set("yarn.resourcemanager.hostname", "localhost");  
        conf.set("yarn.resourcemanager.admin.address", "localhost:8033");  
        conf.set("yarn.resourcemanager.address", "localhost:8032");  
        conf.set("yarn.resourcemanager.resource-tracker.address", "localhost:8036");  
        conf.set("yarn.resourcemanager.scheduler.address", "localhost:8030");  
   
        String[] otherArgs = new String[2];  
        otherArgs[0] = "hdfs://localhost:9000/user/Google/input";//计算原文件目录，需要提前创建并在里面存入文件  
        System.out.println(otherArgs[0]);
        String time = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());  
        otherArgs[1] = "hdfs://localhost:9000/user/Google/output" + time;//计算后的计算结果存储目录，输出目录不能相同，每次程序执行的结果目录不能相同，所以添加时间标签  
   
        /* 
         * setJobName()方法命名这个Job。对Job进行合理的命名有助于更快地找到Job， 
         * 以便在JobTracker和Tasktracker的页面中对其进行监视 
         */  
        Job job = Job.getInstance(conf);  
        job.setJobName("word count");  
        job.setJarByClass(WordCount.class);  
   
        ((JobConf) job.getConfiguration()).setJar(jarFile.toString());//环境变量调用，添加此句则可在eclipse中直接提交mapreduce任务，如果将该java文件打成jar包，需要将该句注释掉，否则在执行时反而找不到环境变量  
   
        // job.setMaxMapAttempts(100);//设置最大试图产生底map数量，该命令不一定会设置该任务运行过车中的map数量  
        // job.setNumReduceTasks(5);//设置reduce数量，即最后生成文件的数量  
   
        /* 
         * Job处理的Map（拆分）、Combiner（中间结果合并）以及Reduce（合并）的相关处理类。 
         * 这里用Reduce类来进行Map产生的中间结果合并，避免给网络数据传输产生压力。 
         */  
        job.setMapperClass(TokenizerMapper.class);// 执行用户自定义map函数  
        job.setCombinerClass(IntSumReducer.class);// 对用户自定义map函数的数据处理结果进行合并，可以减少带宽消耗  
        job.setReducerClass(IntSumReducer.class);// 执行用户自定义reduce函数  
   
        /* 
         * 接着设置Job输出结果<key,value>的中key和value数据类型，因为结果是<单词,个数>， 
         * 所以key设置为"Text"类型，相当于Java中String类型 
         * 。Value设置为"IntWritable"，相当于Java中的int类型。 
         */  
        job.setOutputKeyClass(Text.class);  
        
        job.setOutputValueClass(IntWritable.class);  
   
        /* 
         * 加载输入文件夹或文件路径，即输入数据的路径 
         * 将输入的文件数据分割成一个个的split，并将这些split分拆成<key,value>对作为后面用户自定义map函数的输入 
         * 其中，每个split文件的大小尽量小于hdfs的文件块大小 
         * （默认64M），否则该split会从其它机器获取超过hdfs块大小的剩余部分数据，这样就会产生网络带宽造成计算速度影响 
         * 默认使用TextInputFormat类型，即输入数据形式为文本类型数据文件 
         */  
        System.out.println("Job start!");  
        FileInputFormat.addInputPath(job, new Path(otherArgs[0]));  
   
        /* 
         * 设置输出文件路径 默认使用TextOutputFormat类型，即输出数据形式为文本类型文件，字段间默认以制表符隔开 
         */  
        FileOutputFormat.setOutputPath(job, new Path(otherArgs[1]));  
   
        /* 
         * 开始运行上面的设置和算法 
         */  
        if (job.waitForCompletion(true)) {  
            System.out.println("ok!");  
        } else {  
            System.out.println("error!");  
            System.exit(0);  
        }  
    }  
}
