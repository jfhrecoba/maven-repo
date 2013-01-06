/**
 * 
 */
package tuan.ir.core;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.Serializable;

import javax.annotation.Nullable;

/**
 * This is a generic document in high-dimensional space. 
 * Every document is uniquely identified by one key of String type (key
 * cannot be null) 
 * @author tuan
 *
 */
public class Document implements Comparable<Document>, Serializable {

	/**
	 * Automatically generated serialID 
	 */
	private static final long serialVersionUID = 2280379642789656553L;
	
	protected String key; 
	protected Features features;
	
	/** Once we have calculated the hash code of a document, we cache it for 
	 * subsequent references */
	private int hashcode;
	
	public String key() {
		return key;
	}	
		
	/** Get the document's feature */
	public Features features() {
		return features;
	}
	
	public Document(String key, Features features) {
		this.key = key;
		this.features = features;
	}
	
	@Override
	/** sort documents by some indicator (e.g. signature length). CONVENTION: 
	 * If a document is null, then always return Integer.MIN_VALUE (all non-null
	 * documents are sorted before null document) */
	public int compareTo(@Nullable Document doc) {
		if (doc == null) {
			return Integer.MIN_VALUE;
		} 
		String docKey = checkNotNull(doc.key(), ErrorMessage.DOC_KEY_NULL);
		Features docFeatures = checkNotNull(doc.features(), 
				ErrorMessage.FEATURES_NULL_EXPLAINED.toString(), docKey);
		
		// TODO: We do not check non-null conditions of the current object to 
		// speed up the computation. Probably this leads to some bugs
		//key = checkNotNull(key, ErrorMessage.DOC_KEY_NULL);
		// features = checkNotNull(features, 
		//		ErrorMessage.FEATURES_NULL_EXPLAINED.toString(), docKey);
		
		int docDim = docFeatures.size();
		int thisDim = features.size();
		
		if (docDim > thisDim) return 1;
		else if (docDim == thisDim) return 0;
		else return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof Document)) {
			return false;
		}
		Document doc = (Document)obj;
		if (key == null) return false;
		return key.equals(doc.key);
	}
	
	@Override
	public int hashCode() {
		if (hashcode != -1) {
			return hashcode;
		}		
		// in order to get hashcode of a document, the document must have 
		// non-null feature sets
		features = checkNotNull(features, ErrorMessage.FEATURES_NULL);
		hashcode = features.hashCode();
		return hashcode;
	}
}
