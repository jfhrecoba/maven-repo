package tuan.hadoop.conf;

import java.io.IOException;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.OutputFormat;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

/**
 * A typical setting of one Hadoop job
 * @author tuan
 */
public class JobConfig extends Configured {

	public static enum Version {
		HADOOP_1,
		HADOOP_2
	}
	
	private Version version = Version.HADOOP_2;
	
	private boolean removeOutputDirectory = false;
	
	private String mapperSize = "-Xmx1024m";
	
	private String compressType = null;
	
	public void markOutputForDeletion() {
		removeOutputDirectory = true;
	}
	
	public void setMapperSize(String mapSize) {
		mapperSize = mapSize;
	}
	
	/**
	 * Compress type: gz, bz2, lz4
	 * @param type
	 */
	public void setCompress(String type) {
		compressType = type;
	}
	
	@SuppressWarnings({ "rawtypes", "deprecation" })
	public <JOB, INFILE extends InputFormat, OUTFILE extends OutputFormat,
			KEYIN, VALUEIN, KEYOUT, VALUEOUT, 
			MAPPER extends Mapper, REDUCER extends Reducer>
			Job setup(
				String jobName,	Class<JOB> jobClass, 
				String inpath, String outpath,
				Class<INFILE> inputFormatClass,
				Class<OUTFILE> outputFormatClass,
				Class<KEYIN> mapKeyOutClass,
				Class<VALUEIN> mapValOutClass,
				Class<KEYOUT> keyOutClass,
				Class<VALUEOUT> valOutClass,
				Class<MAPPER> mapClass,
				Class<REDUCER> reduceClass,
				int reduceNo) throws IOException {
		
		// Hadoop 2.0
		Job job;
		if (version == Version.HADOOP_2) {
			job = Job.getInstance(getConf());
			job.setJobName(jobName);
		}

		// Hadoop 1.x
		else {
			job = new Job(getConf(), jobName);
		}
		
		// Common configurations
		job.setJarByClass(jobClass);
		
		job.getConfiguration().setBoolean(
				"mapreduce.map.tasks.speculative.execution", false);
		job.getConfiguration().setBoolean(
				"mapreduce.reduce.tasks.speculative.execution", false);
		
		
		// Option: Java heap space
		job.getConfiguration().set("mapreduce.child.java.opts", mapperSize);
		job.getConfiguration().set("mapred.child.java.opts", mapperSize);

		
		// Option: compress output
		if (compressType != null) {
			job.getConfiguration().setBoolean("mapreduce.output.fileoutputformat.compress", true);
			job.getConfiguration().setBoolean("mapred.output.compress", true);
			
			if ("bz2".equals(compressType)) {
				getConf().set("mapreduce.output.fileoutputformat.compress.codec", 
						"org.apache.hadoop.io.compress.BZip2Codec");
				getConf().set("mapred.output.compression.codec", 
						"org.apache.hadoop.io.compress.BZip2Codec");
			}			
			else if ("gz".equals(compressType)) {
				getConf().set("mapreduce.output.fileoutputformat.compress.codec", 
						"org.apache.hadoop.io.compress.GzipCodec");
				getConf().set("mapred.output.compression.codec", 
						"org.apache.hadoop.io.compress.GzipCodec");
			}
			else if ("lz4".equals(compressType)) {
				getConf().set("mapreduce.output.fileoutputformat.compress.codec", 
						"org.apache.hadoop.io.compress.Lz4Codec");
				getConf().set("mapred.output.compression.codec", 
						"org.apache.hadoop.io.compress.Lz4Codec");
			}
			else throw new RuntimeException("Unknown compress codec: " + compressType);
		}
		
		
		job.setNumReduceTasks(reduceNo);

		Path ip = new Path(inpath);
		Path op = new Path(outpath);
		
		if (removeOutputDirectory) {
			FileSystem fs = FileSystem.get(getConf());
			fs.delete(op, true);
		}
		
		FileInputFormat.setInputPaths(job, ip);
		FileOutputFormat.setOutputPath(job, op);
		
		job.setInputFormatClass(inputFormatClass);
		job.setOutputFormatClass(outputFormatClass);
				
		job.setMapOutputKeyClass(mapKeyOutClass);
		job.setMapOutputValueClass(mapValOutClass);
		
		job.setOutputKeyClass(keyOutClass);
		job.setOutputValueClass(valOutClass);
		
		job.setMapperClass(mapClass);
		job.setReducerClass(reduceClass);

		return job;
	}
	
	public void setVersion(Version v) {
		this.version = v;
	}
}