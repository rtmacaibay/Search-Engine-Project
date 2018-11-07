import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

/**
 * HTMLFinder imports the specified file or iterates through a specified path, checking for html/htm files.
 * HTMLFinder also checks for html/htm files in sub-directories of the specified paths.
 * 
 * @author Robert Macaibay
 */
public class HTMLFinder {
	/**
	 * Helper method which calls a recursive function to find html/htm files
	 * Also skips over that step if the path is a path to a single file
	 * @param path - path to a file or directory
	 * @return a File array of html/htm files ONLY
	 */
	public static Path[] findHtml(String path) throws IOException {
		//output ArrayList
		ArrayList<Path> output = new ArrayList<Path>();
		
		//create File object of the String type path
		Path p = Paths.get(path);
		
		//check if the path is a directory or not.
		//if it is a directory, call our recursive function to iterate through the directory
		if (Files.isDirectory(p))
			findHtml(Files.newDirectoryStream(p), output);
		
		//checks if its a regular file
		if (Files.isRegularFile(p)) {
			//grab filename AKA path toString
			String fileName = p.toString().toLowerCase();

			//check if filename ends with html or htm
			if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
				output.add(p);
		}	
		
		//convert our ArrayList of Files to an array
		return output.toArray(new Path[output.size()]);
	}
	
		
	/**
	 * Helper method which calls a recursive function to find html/htm files
	 * Also skips over that step if the path is a path to a single file
	 * @param path - path to a file or directory
	 * @return a File array of html/htm files ONLY
	 */
	public static void findHtmlThreaded(String path, WorkQueue queue, InvertedIndex index) throws IOException{
		//create File object of the String type path
		Path p = Paths.get(path);

		//check if the path is a directory or not.
		//if it is a directory, call our recursive function to iterate through the directory
		if (Files.isDirectory(p))
			findHtmlThreaded(Files.newDirectoryStream(p), queue, index);
		
		//checks if its a regular file
		if (Files.isRegularFile(p)) {
			//grab filename AKA path toString
			String fileName = p.toString().toLowerCase();

			//check if filename ends with html or htm
			if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
				queue.execute(new InvertedIndexBuilder.BuildTask(index, p));
		}
	}
	
	/**
	 * Recursive function that iterates through a directory and any sub-directories
	 * Primarily looks for html/htm files
	 * @param files - list/array of files/paths found in a directory
	 * @param output - output ArrayList that holds only html/htm file paths
	 */
	private static void findHtml(DirectoryStream<Path> ds, ArrayList<Path> output) throws IOException {
		//iterates through the stream
		for (Path p : ds) {
			//checks if directory to call itself
			if (Files.isDirectory(p))
				findHtml(Files.newDirectoryStream(p), output);
			
			//checks if its a regular file
			if (Files.isRegularFile(p)) {
				//grab filename AKA path toString
				String fileName = p.toString().toLowerCase();
				
				//check if filename ends with html or htm
				if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
					output.add(p);
			}
		}
	}
	
	/**
	 * Recursive function that iterates through a directory and any sub-directories
	 * Primarily looks for html/htm files
	 * @param files - list/array of files/paths found in a directory
	 * @param output - output ArrayList that holds only html/htm file paths
	 */
	private static void findHtmlThreaded(DirectoryStream<Path> ds, WorkQueue queue, InvertedIndex index) throws IOException {
		//iterates through the stream
		for (Path p : ds) {
			//checks if directory to call itself
			if (Files.isDirectory(p))
				findHtmlThreaded(Files.newDirectoryStream(p), queue, index);

			//checks if its a regular file
			if (Files.isRegularFile(p)) {
				//grab filename AKA path toString
				String fileName = p.toString().toLowerCase();

				//check if filename ends with html or htm
				if (fileName.endsWith(".html") || fileName.endsWith(".htm"))
					queue.execute(new InvertedIndexBuilder.BuildTask(index, p));
			}
		}
	}
}
