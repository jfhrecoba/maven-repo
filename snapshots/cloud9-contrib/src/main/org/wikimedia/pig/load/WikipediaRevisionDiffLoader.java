/**
 * 
 */
package org.wikimedia.pig.load;

import static org.apache.hadoop.mapreduce.lib.input.FileInputFormat.setInputPaths;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import name.fraser.neil.plaintext.diff_match_patch;
import name.fraser.neil.plaintext.diff_match_patch.Diff;
import name.fraser.neil.plaintext.diff_match_patch.Operation;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.pig.Expression;
import org.apache.pig.LoadFunc;
import org.apache.pig.LoadMetadata;
import org.apache.pig.ResourceSchema;
import org.apache.pig.ResourceStatistics;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.BagFactory;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.wikimedia.hadoop.io.WikipediaRevisionInputFormat;

/**
 * A UDF loader for WikipediaRevisionInputFormat using RevisionPairRecordReader
 * @author tuan
 *
 */
public class WikipediaRevisionDiffLoader extends LoadFunc implements LoadMetadata {

	private static final WikipediaRevisionInputFormat INPUT_FORMAT = new WikipediaRevisionInputFormat();

	protected RecordReader<LongWritable, Text> reader;
	
	// a cached object that defines the output schema of a Wikipedia page. Use volatile to fix
	// the infamous double-checked locking issue, and to make access to this object thread-safe
	protected volatile ResourceSchema schema;
	
	protected diff_match_patch dmp;
	
	protected TupleFactory tuples;
	protected BagFactory bags;
	
	@Override
	public InputFormat getInputFormat() throws IOException {
		return INPUT_FORMAT;
	}

	@Override
	public void prepareToRead(@SuppressWarnings("rawtypes") RecordReader reader, PigSplit split)
			throws IOException {
		this.reader = (RecordReader<LongWritable, Text>)reader;
		this.dmp = new diff_match_patch();
		this.tuples = TupleFactory.getInstance();
		this.bags = BagFactory.getInstance();
	}

	@Override
	public Tuple getNext() throws IOException {
		boolean hasNext;
		try {
			hasNext = reader.nextKeyValue();
			if (hasNext) {
				Text content = reader.getCurrentValue();
				Document doc = Jsoup.parse(content.toString(), "", Parser.xmlParser());
				String id =  doc.select("page > id").first().data();
				String title = doc.select("page > title").first().data();
				Elements elems = doc.select("page > revision");
				List<String> revIds = new ArrayList<>(2);
				List<String> revTexts = new ArrayList<>(2);
				for (Element e : elems) {
					String t = e.attr("beginningofpage");
					if (t == null || t.isEmpty()) {
						String revId = e.getElementsByTag("id").first().data();
						revIds.add(revId);
						String revTxt = e.getElementsByTag("text").first().data();
						revTexts.add(revTxt);
					}
				}
				LinkedList<Diff> diffs;
				if (revTexts.size() > 1) {
					diffs = dmp.diff_main(revTexts.get(0), revTexts.get(1));
				} else {
					diffs = new LinkedList<>();
					diffs.add(new Diff(Operation.INSERT, revTexts.get(0)));
				}
				
				// TODO: Generate the Tuple instance here
				
			}
		} catch (InterruptedException e) {
			throw new IOException(e);
		}
		return null;
	}
	
	@Override
	public void setLocation(String loc, Job job) throws IOException {
		setInputPaths(job, loc);
	}
	
	@Override
	public String[] getPartitionKeys(String loc, Job job) throws IOException {
		setLocation(loc, job);
		return null;
	}

	@Override
	public ResourceSchema getSchema(String arg0, Job arg1) throws IOException {
		ResourceSchema s = schema;
		if (s == null) {
			synchronized (this) {
				s = schema;
				if (s == null) {					
					defineSchema();
				}
			}
		}
		return schema;
	}

	@Override
	public ResourceStatistics getStatistics(String arg0, Job arg1)
			throws IOException {
		return null;
	}

	@Override
	public void setPartitionFilter(Expression arg0) throws IOException {
	}
	
	protected void defineSchema() throws FrontendException {
		// TODO: define schema here
	}
}
