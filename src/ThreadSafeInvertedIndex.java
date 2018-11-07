import java.util.List;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * A copy of an InvertedIndex that allows for threaded work
 * 
 * @author Robert Macaibay
 */
public class ThreadSafeInvertedIndex extends InvertedIndex {
	//private lock to prevent concurrency problems
	private ReadWriteLock lock;

	/**
	 * default constructor
	 */
	public ThreadSafeInvertedIndex() {
		super();
		lock = new ReadWriteLock();
	}

	/**
	 * Adds a specific word to a specific path and position... safely
	 * @param word - word found
	 * @param path - path to html/htm file
	 * @param pos - position of word in that file
	 */
	public void add(String word, String path, int pos) {
		lock.lockReadWrite();
		super.add(word, path, pos);
		lock.unlockReadWrite();
	}
	
	/**
	 * Adds the array of words at once, assuming the first word in the array is
	 * at position 1... safely
	 *
	 * @param words - array of words to add
	 * @param path - path to html/htm file
	 */
	public void addAll(String[] words, String path) {
		addAll(words, path, 1);
	}
	
	/**
	 * Adds the array of words at once, assuming the first word in the array is
	 * at the provided starting position
	 *
	 * @param words - array of words to add
	 * @param path - path to html/htm file
	 * @param start - starting position
	 */
	public void addAll(String[] words, String path, int start) {
		lock.lockReadWrite();
		for (int i = start; i <= words.length; i++)
			super.add(words[i - 1], path, i);
		lock.unlockReadWrite();
	}

	/**
	 * Returns a copy of the words in this index as a sorted list... safely
	 * @return sorted list of paths
	 */
	public List<String> copyWords() {
		lock.lockReadOnly();
		try {
			return super.copyWords();
		} finally {
			lock.unlockReadOnly();
		}
	}

	/**
	 * Gets the PathIndex map of a certain word... safely
	 * @param word - specified word to find
	 * @return Mapping of paths and positions for a word
	 */
	public PathIndex get(String word) {
		lock.lockReadOnly();
		try {
			return super.get(word);
		} finally {
			lock.unlockReadOnly();
		}
	}
	
	/**
	 * Gets the ReadWriteLock associated for this index. Used for the search tasks explicitly
	 * @return the ReadWriteLock
	 */
	private ReadWriteLock getLock() {
		return lock;
	}

	/**
	 * Executes an exact search
	 * 
	 * @author Robert Macaibay
	 */
	public static class ExactSearchTask implements Runnable {
		private InvertedIndex index;
		private TreeMap<String, List<SearchResult>> map;
		private List<String> queries;
		private String word;
		private ReadWriteLock lock;

		public ExactSearchTask(InvertedIndex index, TreeMap<String, List<SearchResult>> map, List<String> queries) {
			this.index = index;
			this.map = map;
			this.queries = queries;
			this.word = queries.stream().collect(Collectors.joining(" "));
			this.lock = ((ThreadSafeInvertedIndex)index).getLock();
		}

		@Override
		public void run() {
			lock.lockReadOnly();
			//gets a list of results for a particular line query
			List<SearchResult> results = index.exactSearch(queries);
			lock.unlockReadOnly();

			//save it into the full map
			synchronized (map) {
				map.put(word, results);
			}
		}
	}

	/**
	 * Executes a partial search
	 * 
	 * @author Robert Macaibay
	 */
	public static class PartialSearchTask implements Runnable {
		private InvertedIndex index;
		private TreeMap<String, List<SearchResult>> map;
		private List<String> queries;
		private String word;
		private ReadWriteLock lock;

		public PartialSearchTask(InvertedIndex index, TreeMap<String, List<SearchResult>> map, List<String> queries) {
			this.index = index;
			this.map = map;
			this.queries = queries;
			this.word = queries.stream().collect(Collectors.joining(" "));;
			this.lock = ((ThreadSafeInvertedIndex)index).getLock();
		}

		@Override
		public void run() {
			lock.lockReadOnly();
			//gets a list of results for a particular line query
			List<SearchResult> results = index.partialSearch(queries);
			lock.unlockReadOnly();

			//save it into the full map
			synchronized (map) {
				map.put(word, results);
			}
		}
	}
}
