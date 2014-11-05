/**
 * 
 */
package edu.umd.cloud9.collection.wikipedia;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.MapFile;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Mapper.Context;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.MapFileOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.apache.log4j.Logger;

import edu.umd.cloud9.io.map.HMapIIW;
import edu.umd.cloud9.io.map.HMapSIW;
import edu.umd.cloud9.io.pair.PairOfInts;
import tuan.hadoop.conf.JobConfig;

/**
 * Invert the Anchor text map generated by Jimmy Lin's ExtractAnchorWikipediaAnchorText
 * 
 * Here outputs are keyed by anchor text
 * @author tuan
 *
 */
public class InverseAnchorTextMap extends JobConfig implements Tool {

	private static final Logger LOG = Logger.getLogger(InverseAnchorTextMap.class);
		
	private static class MyMapper extends Mapper<IntWritable, HMapSIW, Text, PairOfInts> {
		
		private final Text keyOut = new Text();
		private final PairOfInts valOut = new PairOfInts();

		@Override
		protected void map(IntWritable mapKey, HMapSIW mapVal, Context context)
				throws IOException, InterruptedException {
			int entityId = mapKey.get();
			Set<String> anchors = mapVal.keySet();				
			for (String anchor : anchors) {
				keyOut.set(anchor);
				valOut.set(entityId, mapVal.get(anchor));		
				context.write(keyOut, valOut);
			}
		}
	}
	
	private static class MyReducer extends Reducer<Text, PairOfInts, Text, HMapIIW> {
		
		private HMapIIW outVal = new HMapIIW();

		@Override
		protected void reduce(Text k, Iterable<PairOfInts> vs, Context context)
				throws IOException, InterruptedException {
			outVal.clear();
			for (PairOfInts e : vs) {
				outVal.put(e.getKey(), e.getValue());
			}
			context.write(k, outVal);
		}
	}
	
	@Override

	// First argument is the input path
	// Second argument is the output path
	public int run(String[] args) throws Exception {
		Job job = setup(SequenceFileInputFormat.class, MapFileOutputFormat.class,
				Text.class, PairOfInts.class, 
				Text.class, HMapIIW.class,
				MyMapper.class, MyReducer.class, args);
		
		try {
		job.waitForCompletion(true);
		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}
		
		return 0;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			ToolRunner.run(new InverseAnchorTextMap(), args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}