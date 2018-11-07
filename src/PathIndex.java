import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Data structure to store strings and their positions.
 * 
 * @author Robert Macaibay
 */
public class PathIndex {

	/**
	 * Stores a mapping of paths to the positions the paths were found.
	 */
	protected TreeMap<String, TreeSet<Integer>> index;

	/**
	 * Initializes the index.
	 */
	public PathIndex() {
		index = new TreeMap<>();
	}
	
	/**
	 * Initializes the index mapping but also adds a "starting" mapping
	 * @param path - path to add initially
	 * @param position - position to add initially
	 */
	public PathIndex(String path, int position) {
		index = new TreeMap<>();
		add(path, position);
	}

	/**
	 * Adds the path and the position it was found to the index.
	 *
	 * @param path - path to clean and add to index
	 * @param position - position path was found
	 */
	public void add(String path, int position) {
		//create a new set for the positions
		TreeSet<Integer> set = new TreeSet<>();
		
		//if we do have an existing TreeSet for a particular path
		//make that reference to it
		if (index.get(path) != null)
			set = index.get(path);
		
		//update TreeSet
		set.add(position);
		//update mapping
		index.put(path, set);
	}

	/**
	 * Returns the number of times a path was found (i.e. the number of
	 * positions associated with a path in the index).
	 *
	 * @param path - path to look for
	 * @return number of times the path was found
	 */
	public int count(String path) {
		TreeSet<Integer> set = index.get(path);
		if (set == null)
			return 0;
		return set.size();
	}

	/**
	 * Returns the number of paths stored in the index.
	 *
	 * @return number of paths
	 */
	public int paths() {
		return index.size();
	}

	/**
	 * Tests whether the index contains the specified path.
	 *
	 * @param path - path to look for
	 * @return true if the path is stored in the index
	 */
	public boolean contains(String path) {
		return index.containsKey(path);
	}

	/**
	 * Returns a copy of the paths in this index as a sorted list.
	 *
	 * @return sorted list of paths
	 */
	public List<String> copyPaths() {
		List<String> out = new ArrayList<String>();
		
		//grab list of paths and add them to output
		for (String path : index.keySet())
			out.add(path);
		return out;
	}

	/**
	 * Returns a copy of the positions for a specific path.
	 *
	 * @param path - to find in index
	 * @return sorted list of positions for that path
	 */
	public List<Integer> copyPositions(String path) {
		//grab set for a particular file path
		TreeSet<Integer> set = index.get(path);
		List<Integer> out = new ArrayList<Integer>();
		
		//grab list of positions and add them to output
		for (Integer pos : set)
			out.add(pos);
		return out;
	}

	/**
	 * Returns a string representation of this index.
	 */
	@Override
	public String toString() {
		return index.toString();
	}
}
