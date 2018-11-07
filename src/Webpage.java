import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletHandler;
import org.eclipse.jetty.servlet.ServletHolder;

public class Webpage {

	public static final int PORT = 8080;

	public static void main(String[] args) throws Exception {
		Server server = new Server(PORT);

		ServletHandler handler = new ServletHandler();
		handler.addServletWithMapping(new ServletHolder(new EngineServlet(handler)), "/engine");

		server.setHandler(handler);
		server.start();
		server.join();
		
	}
}