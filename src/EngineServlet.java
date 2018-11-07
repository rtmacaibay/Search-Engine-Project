import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;

// More XSS Prevention:
// https://www.owasp.org/index.php/XSS_(Cross_Site_Scripting)_Prevention_Cheat_Sheet

// Apache Comments:
// http://commons.apache.org/proper/commons-lang/download_lang.cgi

@SuppressWarnings({ "serial" })
public class EngineServlet extends HttpServlet {
	private static final String TITLE = "Project 4";
	private static Logger log = Log.getRootLogger();
	public static final int PORT = 8080;
	private ServletHandler handler;
	private TreeMap<String, RefreshValue> refresh;

	private ConcurrentLinkedQueue<String> queries;

	public EngineServlet() {
		super();
		this.queries = new ConcurrentLinkedQueue<>();
		this.handler = null;
		this.refresh = new TreeMap<>();
	}
	
	public EngineServlet(ServletHandler handler) {
		super();
		this.queries = new ConcurrentLinkedQueue<>();
		this.handler = handler;
		this.refresh = new TreeMap<>();
	}

	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		log.info("EngineServlet ID " + this.hashCode() + " handling GET request.");

		PrintWriter out = response.getWriter();
		out.printf("<html>%n%n");
		out.printf("<head>");
		out.printf("<link rel=\"shortcut icon\" type=\"image/png\" href=\"https://i.imgur.com/VgMmZui.jpg\" />");
		out.printf("<title>%s</title></head>%n", TITLE);
		out.printf("<body>%n");
		out.printf("<style>%n");
		out.printf(".content { max-width: 550px; margin: 0 auto; margin-top: 25px; text-align: center; }%n");
		out.printf("</style>");
		out.printf("<style>%n");
		out.printf(".image { display: block; margin-left: auto; margin-right: auto; }%n");
		out.printf("</style>");
		
		out.printf("<h1 class=\"content\">Bomas the Search Engine</h1>%n%n");

		// Keep in mind multiple threads may access at once
		for (String query : queries) {
			out.printf("<p class=\"content\">%s</p>%n%n", query);
		}

		printForm(request, response);

		out.printf("<p class=\"content\">This request was handled by thread %s.</p>%n", Thread.currentThread().getName());
		out.printf("<p class=\"content\">Your search was handled by the Bomas search engine, created by Robert Macaibay.</p>%n");
		out.printf("<img class=\"image\" src=\"%s\" alt=\"bomas the search engine\">%n", "https://i.imgur.com/VgMmZui.jpg");
		
		out.printf("%n</body>%n");
		out.printf("</html>%n");

		response.setStatus(HttpServletResponse.SC_OK);
	}

	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		response.setContentType("text/html");
		response.setStatus(HttpServletResponse.SC_OK);

		String action = request.getParameter("action");
		
		if (action.equals("Search")) {
			log.info("EngineServlet ID " + this.hashCode() + " handling SEARCH request.");

			String username = request.getParameter("username");
			String query = request.getParameter("query");
			String url = request.getParameter("url");
			boolean partialSearch = false;
			boolean privateSearch = false;
			String[] buttons = request.getParameterValues("search");
			
			if (buttons != null)
				for (String b : buttons)
					if (b.equals("partial"))
						partialSearch = true;
					else if (b.equals("private"))
						privateSearch = true;
			
			username = username == null || username.equals("") ? "anonymous" : username;
			query = query == null || query.equals("") ? "bird" : query;
			url = url == null || url.equals("") ? "http://vis.cs.ucdavis.edu/~cjbryan/cs212/" : url;

			if (privateSearch)
				username = "PRIVATE";
			
			String broadLink = url.replace("http://", "").replace("https://", "").replace("www", "").replaceAll("\\W", "-").replaceAll("--", "-");
			
			if (broadLink.endsWith("-"))
				broadLink = broadLink.substring(0, broadLink.length() - 1);

			String filename = String.format("index-url-%s.json", broadLink);

			Path output = Paths.get("out", filename);
			Files.deleteIfExists(output);
			Files.createDirectories(output.getParent());

			if (partialSearch) {
				String[] args = {"-url", url, "-queryLine", query, "-index", output.toString(), "-limit", "-servlet"};
				Driver.main(args);
			} else {
				String[] args = {"-url", url, "-queryLine", query, "-index", output.toString(), "-limit", "-servlet", "-exact"};
				Driver.main(args);
			}

			StringBuilder results = new StringBuilder();

			for (String line : Files.readAllLines(output, StandardCharsets.UTF_8))
				results.append(line + "<br>");

			//System.out.println(results);

			String link = "/" + broadLink + "/" + query;
			if (!refresh.containsKey(link))
				refresh.put(link, new RefreshValue());
			else {
				refresh.get(link).setRefreshValue(false);
				response.setStatus(HttpServletResponse.SC_OK);
				response.sendRedirect(request.getServletPath());
				return;
			}
	
			
			ServletHolder sh = new ServletHolder(new ResultServlet(results.toString(), refresh.get(link)));
			String formatted = String.format("%s <a href=\"http://localhost:%d%s\" target=\"_blank\">"
					+ "results link</a><br><font size=\"-2\">[ posted by %s at %s ]</font>", 
					query, PORT, link, username, getDate());
			
			// Keep in mind multiple threads may access at once
			queries.add(formatted);

			// Only keep the latest 5 messages
			if (queries.size() > 10) {
				String first = queries.poll();
				log.info("Removing message: " + first);
			}
			
			handler.addServletWithMapping(sh, link);

		} else {
			queries.clear();
			for (String link : refresh.keySet()) {
				refresh.get(link).setRefreshValue(true);
			}
			log.info("Removing search history");
		}
		
		response.setStatus(HttpServletResponse.SC_OK);
		response.sendRedirect(request.getServletPath());
	}

	private static void printForm(HttpServletRequest request, HttpServletResponse response) throws IOException {

		String tab = "&nbsp;&nbsp;&nbsp;&nbsp;";
		PrintWriter out = response.getWriter();
		out.printf("<form name=\"myForm\" class=\"content\" method=\"post\" action=\"%s\">%n", request.getServletPath());
		out.printf("<table margin=\"auto\" cellspacing=\"0\" cellpadding=\"2\"%n");
		out.printf("<tr>%n");
		out.printf("\t<td nowrap>Name:</td>%n");
		out.printf("\t<td>%n");
		out.printf("\t\t<input type=\"text\" name=\"username\" maxlength=\"50\" size=\"20\">%n");
		out.printf("\t</td>%n");
		out.printf("</tr>%n");
		out.printf("<tr>%n");
		out.printf("\t<td nowrap>URL:</td>%n");
		out.printf("\t<td>%n");
		out.printf("\t\t<input type=\"text\" name=\"url\" maxlength=\"50\" size=\"40\">%n");
		out.printf("\t</td>%n");
		out.printf("</tr>%n");
		out.printf("<tr>%n");
		out.printf("\t<td nowrap>Search Query:</td>%n");
		out.printf("\t<td>%n");
		out.printf("\t\t<input type=\"text\" name=\"query\" maxlength=\"50\" size=\"40\">%n");
		out.printf("\t</td>%n");
		out.printf("</tr>%n");
		out.printf("</table>%n");
		out.printf("<p><input type=\"submit\" name=\"action\" value=\"Search\">");
		out.printf(tab + "<input type=\"submit\" name=\"action\" value=\"Clear History\">");
		out.printf(tab + "<input type=\"checkbox\" name=\"search\" value=\"partial\">Partial Search?");
		out.printf(tab + "<input type=\"checkbox\" name=\"search\" value=\"private\">Private Search?");
		out.printf("</p>\n%n");
		out.printf("</form>\n%n");
	}

	private static String getDate() {
		String format = "hh:mm a 'on' EEEE, MMMM dd yyyy";
		DateFormat formatter = new SimpleDateFormat(format);
		return formatter.format(new Date());
	}
	
	protected static String printResults(String json) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html>%n");
		sb.append("<head><title>Result</title></head>%n");
		sb.append("<body>%n");

		sb.append("<p>");
		sb.append(json);
		sb.append("</p>%n");

		sb.append("</body>%n");
		sb.append("</html>%n");
		
		return sb.toString();
	}
	
	public static class ResultServlet extends HttpServlet {
		private String json;
		private RefreshValue refresh;
		
		public ResultServlet(String json, RefreshValue refresh) throws IOException {
			super();
			this.json = json;
			this.refresh = refresh;
		}
		
		protected void doGet(HttpServletRequest request, HttpServletResponse response)
				throws ServletException, IOException {
			
			response.setContentType("text/html");
			response.setStatus(HttpServletResponse.SC_OK);
			
			log.info("EngineServlet ID " + this.hashCode() + " handling GET request.");
			PrintWriter out = response.getWriter();
			
			if (!refresh.getRefreshValue()) {
				out.printf(printResults(json));
			} else {
				out.printf("<html>%n");
				out.printf("<head>");
				out.printf("<meta http-equiv=\"refresh\""
						+ " content=\"0; url=http://localhost:%d/engine\">", PORT);
				out.printf("<script type=\"text/javascript\">");
				out.printf("window.location.href = \"http://localhost:%d/engine\"", PORT);
				out.printf("</script><title>Page Redirection</title>");
				out.printf("</head>%n");
				out.printf("<body>%n");
				out.printf("<p>If you are not redirected automatically,"
						+ " follow this <a href=\"localhost:%d/engine\">"
						+ "link to the homepage</a>.</p>%n", PORT, json);
				out.printf("</body>%n");
				out.printf("</html>%n");
			}
			
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}
	
	protected static class RefreshValue {
		private boolean refresh;
		
		public RefreshValue() {
			refresh = false;
		}
		
		public void setRefreshValue(boolean b) {
			refresh = b;
		}
		
		public boolean getRefreshValue() {
			return refresh;
		}
	}
}