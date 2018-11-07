import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

public class LinkParser {
	
	public static final String version = "HTTP/1.1";
	
	public static enum HTTP {
		OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE, CONNECT
	};

	// https://developer.mozilla.org/en-US/docs/Web/HTML/Element/a
	// https://docs.oracle.com/javase/tutorial/networking/urls/creatingUrls.html
	// https://developer.mozilla.org/en-US/docs/Learn/Common_questions/What_is_a_URL

	/**
	 * Removes the fragment component of a URL (if present), and properly
	 * encodes the query string (if necessary).
	 *
	 * @param url
	 *            url to clean
	 * @return cleaned url (or original url if any issues occurred)
	 */
	public static URL clean(URL url) {
		try {
			return new URI(url.getProtocol(), url.getUserInfo(), url.getHost(), url.getPort(), url.getPath(),
					url.getQuery(), null).toURL();
		}
		catch (MalformedURLException | URISyntaxException e) {
			return url;
		}
	}

	/**
	 * Fetches the HTML (without any HTTP headers) for the provided URL. Will
	 * return null if the link does not point to a HTML page.
	 *
	 * @param url
	 *            url to fetch HTML from
	 * @return HTML as a String or null if the link was not HTML
	 */
	public static String fetchHtml(URL url) {
		String request = craftHttpRequest(url, HTTP.GET);
		List<String> lines = fetchLines(url, request);
		
		int start = 0;
		int end = lines.size();
		
		while (!lines.get(start).trim().isEmpty() && start < end)
			start++;
		
		Map<String, String> fields = parseHeaders(lines.subList(0, start + 1));
		String type = fields.get("Content-Type");
		
		if (type != null && type.toLowerCase().contains("html"))
			return String.join(System.lineSeparator(), lines.subList(start + 1, end));
		
		return null;
	}
	
	/**
	 * Will connect to the web server and fetch the URL using the HTTP request
	 * provided. It would be more efficient to operate on each line as returned
	 * instead of storing the entire result as a list.
	 *
	 * @param url - url to fetch
	 * @param request - full HTTP request
	 *
	 * @return the lines read from the web server
	 *
	 * @throws IOException
	 * @throws UnknownHostException
	 */
	public static List<String> fetchLines(URL url, String request) {
		List<String> lines = new ArrayList<String>();
		int port = url.getPort() < 0 ? 80 : url.getPort();
		
		if (!url.toString().contains("https://")) {
			try (
					Socket socket = new Socket(url.getHost(), port);
					BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
					PrintWriter writer = new PrintWriter(socket.getOutputStream());
					) {
				writer.println(request);
				writer.flush();

				String line = new String("");

				while ((line = reader.readLine()) != null)
					lines.add(line);
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			try {
				HttpsURLConnection con = (HttpsURLConnection)url.openConnection();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));) {
					String line = new String("");
					
					while ((line = reader.readLine()) != null)
						lines.add(line);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		return lines;
	}

	/**
	 * Returns a list of all the HTTP(S) links found in the href attribute of the
	 * anchor tags in the provided HTML. The links will be converted to absolute
	 * using the base URL and cleaned (removing fragments and encoding special
	 * characters as necessary).
	 *
	 * @param base - base url used to convert relative links to absolute3
	 * @param html - raw html associated with the base url
	 * @return cleaned list of all http(s) links in the order they were found
	 */
	public static ArrayList<URL> listLinks(URL base, String html) {
		ArrayList<URL> links = new ArrayList<URL>();
		
		String[] splitHtml = html.split("<");
		
		try {
			for (String tag : splitHtml) {
				if (tag.indexOf(">") == -1)
					continue;

				tag = tag.substring(0, tag.indexOf(">")).replaceAll("\n", "").replaceAll(" ", "");
				
				if (!tag.toLowerCase().contains("href=") || tag.toLowerCase().indexOf("a") != 0)
					continue;

				int firstPos = tag.toLowerCase().indexOf("href=\"") + 6;
				int secondPos = tag.indexOf("\"", firstPos + 1); 

				URL temp = null;

				temp = clean(new URL(base, tag.substring(firstPos, secondPos)));
				
				if (temp.toString().contains("mailto"))
					continue;

				if (!links.contains(temp))
					links.add(temp);
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		return links;
	}
	
	/**
	 * Crafts a minimal HTTP/1.1 request for the provided method.
	 *
	 * @param url - url to fetch
	 * @param type - HTTP method to use
	 *
	 * @return HTTP/1.1 request
	 *
	 * @see {@link HTTP}
	 */
	private static String craftHttpRequest(URL url, HTTP type) {
		String host = url.getHost();
		String resource = url.getFile().isEmpty() ? "/" : url.getFile();
		
		return String.format("%s %s %s\r\n" + "Host: %s\r\n" + "Connection: close\r\n"
							+ "\r\n", type.name(), resource, version, host);
	}
	
	/**
	 * Helper method that parses HTTP headers into a map where the key is the
	 * field name and the value is the field value. The status code will be
	 * stored under the key "Status".
	 *
	 * @param headers - HTTP/1.1 header lines
	 * @return field names mapped to values if the headers are properly formatted
	 */
	private static Map<String, String> parseHeaders(List<String> headers) {
		Map<String, String> fields = new HashMap<>();
		
		if (headers.size() > 0 && headers.get(0).startsWith(version)) {
			fields.put("Status", headers.get(0).substring(version.length()).trim());
			
			for (String line : headers.subList(1, headers.size())) {
				String[] pair = line.split(":", 2);
				
				if (pair.length == 2)
					fields.put(pair[0].trim(), pair[1].trim());
			}
		}
		
		return fields;
	}
	
	/**
	 * Executes a link parser task for threaded work
	 * 
	 * @author Robert Macaibay
	 */
	public static class LinkParseTask implements Runnable {
		
		private ArrayList<URL> links;
		private WorkQueue queue;
		private URL url;
		private URL base;
		private int max;
		private String html;
		
		public LinkParseTask(ArrayList<URL> links, WorkQueue queue, URL url, URL base, int max) {
			this.links = links;
			this.queue = queue;
			this.url = url;
			this.base = base;
			this.max = max;
			this.html = new String("");
		}

		@Override
		public void run() {
			if (!links.contains(url))
				links.add(url);
			
			html = fetchHtml(url);
			
			if (html == null)
				return;
			
			String[] splitHtml = html.split("<");
			
			try {
				int addedAt = 0;
				synchronized (links) {
					addedAt = links.size();
				}
				for (String tag : splitHtml) {
					synchronized (links) {
						if (links.size() == max)
							break;
					}
					
					if (tag.indexOf(">") == -1)
						continue;

					tag = tag.substring(0, tag.indexOf(">")).replaceAll("\n", "").replaceAll(" ", "");
					
					if (!tag.toLowerCase().contains("href=") || tag.toLowerCase().indexOf("a") != 0)
						continue;
					
					int firstPos = tag.toLowerCase().indexOf("href=\"") + 6;
					int secondPos = tag.indexOf("\"", firstPos + 1); 
					
					if (firstPos < 0 || secondPos < 0)
						continue;

					URL temp = null;

					temp = clean(new URL(base, tag.substring(firstPos, secondPos)));
					
					if (temp.toString().contains("mailto"))
						continue;
					
					synchronized (links) {
						if (!links.contains(temp))
							links.add(temp);
					}
				}
				
				for (int i = addedAt; i < links.size() && links.size() < max; i++) {
					queue.execute(new LinkParseTask(links, queue, links.get(i), base, max));
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			}
		}
	}
}
