/**
 * Creates an object that holds information about a search result
 * 
 * @author Robert Macaibay
 */
public class SearchResult implements Comparable<SearchResult> {
	//private class variables
	private int frequency;
	private int pos;
	private String path;
	
	/**
	 * constructs a search result object
	 * @param frequency - amount of times a query came up in a file
	 * @param pos - first instance the query came up
	 * @param path - path to the file
	 */
	public SearchResult(int frequency, int pos, String path) {
		this.setFrequency(frequency);
		this.setPos(pos);
		this.setPath(path);
	}
	
	/**
	 * compares one search result to another
	 * @param other - another search result to compare to
	 * @return the position where the object would be compared to the other search result
	 */
	@Override
	public int compareTo(SearchResult other) {
		if (frequency == other.getFrequency()) {
			if (pos == other.getPos()) {
				//natural order by the path names
				return path.toString().compareTo(other.getPath().toString());
			}
			//order by the lowest first position found
			return pos - other.getPos();
		}
		//order by highest frequency
		return other.getFrequency() - frequency;
	}

	public int getPos() {
		return pos;
	}

	public void setPos(int pos) {
		this.pos = pos;
	}

	public int getFrequency() {
		return frequency;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
}
