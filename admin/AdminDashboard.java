
package admin;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

/**
 * Simple admin dashboard client that connects to AdminHTTPServer
 * and requests GET /results using a lightweight HTTP-like request over a plain socket.
 */
public class AdminDashboard {
	public static void main(String[] args) {
		String host = "localhost";
		int port = 8080;

		if (args.length >= 1) host = args[0];
		if (args.length >= 2) {
			try { port = Integer.parseInt(args[1]); } catch (NumberFormatException ignored) {}
		}

		System.out.println("Admin Dashboard - simple client to fetch /results from " + host + ":" + port);
		System.out.println("Type ENTER to fetch results or type 'exit' to quit.");

		try (Scanner scanner = new Scanner(System.in)) {
			while (true) {
				System.out.print("admin> ");
				String line = scanner.nextLine().trim();
				if (line.equalsIgnoreCase("exit")) break;

				// Send a single GET /results request and print response
				fetchResults(host, port);
			}
		}
	}

	private static void fetchResults(String host, int port) {
		try (Socket socket = new Socket(host, port)) {
			socket.setSoTimeout(5000);
			OutputStream out = socket.getOutputStream();
			InputStream in = socket.getInputStream();

			String request = "GET /results HTTP/1.1\r\nHost: " + host + "\r\n\r\n";
			out.write(request.getBytes(StandardCharsets.UTF_8));
			out.flush();

			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));

			// Read status line
			String statusLine = reader.readLine();
			if (statusLine == null) {
				System.out.println("No response from server.");
				return;
			}
			System.out.println(statusLine);

			// Read headers
			String header;
			int contentLength = -1;
			while ((header = reader.readLine()) != null && !header.isEmpty()) {
				System.out.println(header);
				String lower = header.toLowerCase();
				if (lower.startsWith("content-length:")) {
					try { contentLength = Integer.parseInt(header.split(":",2)[1].trim()); } catch (Exception ignored) {}
				}
			}

			// Read body
			StringBuilder body = new StringBuilder();
			if (contentLength >= 0) {
				char[] buf = new char[contentLength];
				int read = 0;
				while (read < contentLength) {
					int r = reader.read(buf, read, contentLength - read);
					if (r == -1) break;
					read += r;
				}
				body.append(buf, 0, Math.max(0, read));
			} else {
				// Read until EOF
				String l;
				while ((l = reader.readLine()) != null) {
					body.append(l).append('\n');
				}
			}

			System.out.println("\n--- Results ---\n" + body.toString());

		} catch (IOException e) {
			System.err.println("Failed to fetch results: " + e.getMessage());
		}
	}
}

