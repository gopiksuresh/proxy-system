import java.io.*;
import java.net.*;

public class OffshoreProxy {
    public static void main(String[] args) throws Exception {
        ServerSocket serverSocket = new ServerSocket(9000);
        System.out.println("Offshore proxy waiting for ship connection...");

        Socket shipSocket = serverSocket.accept();
        System.out.println("Connected to ship.");

        BufferedReader in = new BufferedReader(new InputStreamReader(shipSocket.getInputStream()));
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(shipSocket.getOutputStream()));

        while (true) {
            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while (!(line = in.readLine()).isEmpty()) {
                requestBuilder.append(line).append("\r\n");
            }
            requestBuilder.append("\r\n");

            // Extract host
            String host = "example.com";
            for (String l : requestBuilder.toString().split("\r\n")) {
                if (l.toLowerCase().startsWith("host:")) {
                    host = l.split(":")[1].trim();
                }
            }

            Socket targetSocket = new Socket(host, 80);
            BufferedWriter targetOut = new BufferedWriter(new OutputStreamWriter(targetSocket.getOutputStream()));
            BufferedReader targetIn = new BufferedReader(new InputStreamReader(targetSocket.getInputStream()));

            targetOut.write(requestBuilder.toString());
            targetOut.flush();

            StringBuilder responseBuilder = new StringBuilder();
            String resLine;
            while ((resLine = targetIn.readLine()) != null) {
                responseBuilder.append(resLine).append("\r\n");
                if (resLine.isEmpty()) break;
            }

            out.write(responseBuilder.toString());
            out.flush();

            targetSocket.close();
        }
    }
}
