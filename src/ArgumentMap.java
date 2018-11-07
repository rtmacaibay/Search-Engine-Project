import java.util.HashMap;
import java.util.Map;

/**
 * Parses command-line arguments into flag/value pairs, and stores those pairs
 * in a map for easy access.
 * 
 * @author Robert Macaibay
 */
public class ArgumentMap {

	private final Map<String, String> map;

	/**
	 * Initializes the argument map.
	 */
	public ArgumentMap() {
		map = new HashMap<>();
	}

	/**
	 * Initializes the argument map and parses the specified arguments into
	 * key/value pairs.
	 *
	 * @param args - command line arguments
	 *
	 * @see #parse(String[])
	 */
	public ArgumentMap(String[] args) {
		this();
		parse(args);
	}

	/**
	 * Parses the specified arguments into key/value pairs and adds them to the
	 * argument map.
	 *
	 * @param args - command line arguments
	 */
	public void parse(String[] args) {
		//create a variable to hold a key
		String key = null;
		//iterate through the arguments
		for (String arg: args) {
			//check if the argument is a flag
			if (isFlag(arg)) {
				//save a copy of that flag
				key = arg;
				//put that key into the map (set value as null bc there's no value... yet)
				map.put(key, null);
			}
			
			//check if the argument is a value AND that there is a key to put in
			if (isValue(arg) && key != null) {
				//replace the value for that key with a new value ONLY if the key's value in the map is null
				map.putIfAbsent(key, arg);
				//set key to null
				key = null;
			}
		}
		
		//checks if the "-index" flag has been placed
		if (hasFlag("-index")) {
			//checks if index does not have a value
			if (!hasValue("-index")) {
				//place a default path AKA "index.json"
				map.put("-index", "index.json");
			}
		}
		
		//checks if there's a "-results" flag has been placed
		if (hasFlag("-results")) {
			//checks if the flag does not have a value
			if (!hasValue("-results")) {
				//place a default path AKA "results.json"
				map.put("-results", "results.json");
			}
		}
	}

	/**
	 * checks if the specified argument is a flag
	 * @param arg - argument to test
	 * @return true if the argument is a flag
	 */
	public static boolean isFlag(String arg) {
		//check if the argument is null
		if (arg == null)
			return false;
		//check if the argument is generally empty or consists of just a '-'
		if (arg.trim().length() <= 1)
			return false;
		//check if the argument at least starts with a '-'
		return arg.charAt(0) == '-';
	}

	/**
	 * checks if the specified argument is a value
	 * @param arg - argument to test
	 * @return true if the argument is a value
	 */
	public static boolean isValue(String arg) {
		//check if the argument is null
		if (arg == null)
			return false;
		//check if the argument is empty
		if (arg.trim().equals(""))
			return false;
		//check if the argument does not start with a '-'
		return arg.charAt(0) != '-';
	}

	/**
	 * Returns the number of unique flags stored in the argument map.
	 *
	 * @return number of flags
	 */
	public int numFlags() {
		//return the size of the map
		return map.size();
	}

	/**
	 * Determines whether the specified flag is stored in the argument map.
	 *
	 * @param flag - flag to test
	 *
	 * @return true if the flag is in the argument map
	 */
	public boolean hasFlag(String flag) {
		//check if a flag is in the map
		return map.containsKey(flag);
	}

	/**
	 * Determines whether the specified flag is stored in the argument map and
	 * has a non-null value stored with it.
	 *
	 * @param flag - flag to test
	 *
	 * @return true if the flag is in the argument map and has a non-null value
	 */
	public boolean hasValue(String flag) {
		//check if a flag is in the map but also has a value
		return hasFlag(flag) && map.get(flag) != null;
	}

	/**
	 * Returns the value for the specified flag as a String object.
	 *
	 * @param flag - flag to get value for
	 *
	 * @return value as a String or null if flag or value was not found
	 */
	public String getString(String flag) {
		//check if there is a flag, if so, return that flag's value
		if (hasFlag(flag))
			return map.get(flag);
		else
			return null;
	}

	/**
	 * Returns the value for the specified flag as a String object. If the flag
	 * is missing or the flag does not have a value, returns the specified
	 * default value instead.
	 *
	 * @param flag - flag to get value for
	 * @param defaultValue -  value to return if flag or value is missing
	 * @return value of flag as a String, or the default value if the flag or value is missing
	 */
	public String getString(String flag, String defaultValue) {
		//check if the flag exists AND its value exists, if so, return that. otherwise, return defaultValue
		if (getString(flag) == null)
			return defaultValue;
		else
			return getString(flag);
	}

	/**
	 * Returns the value for the specified flag as an int value. If the flag is
	 * missing or the flag does not have a value, returns the specified default
	 * value instead.
	 *
	 * @param flag - flag to get value for
	 * @param defaultValue - value to return if the flag or value is missing
	 * @return value of flag as an int, or the default value if the flag or value is missing
	 */
	public int getInteger(String flag, int defaultValue) {
		//check if the flag exists AND its value exists, if so, return that. otherwise, return defaultValue
		if (getString(flag) == null)
			return defaultValue;
		else
			return Integer.getInteger(getString(flag), defaultValue);
	}

	/**
	 * Creates String representation of object.
	 * 
	 * @return String output
	 */
	@Override
	public String toString() {
		return map.toString();
	}
}
