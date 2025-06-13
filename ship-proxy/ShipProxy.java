import java.io.*;
import java.net.*;

public class ShipProxy {
    public static void main(String[] args) throws Exception {
        ServerSocket httpSocket = new ServerSocket(8080);
        Socket offshoreSocket = new Socket("localhost", 9000); // Replace if needed

        System.out.println("Ship Proxy running on port 8080...");

        while (true) {
            Socket clientSocket = httpSocket.accept();

            BufferedReader clientIn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            BufferedWriter clientOut = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));

            BufferedWriter offshoreOut = new BufferedWriter(new OutputStreamWriter(offshoreSocket.getOutputStream()));
            BufferedReader offshoreIn = new BufferedReader(new InputStreamReader(offshoreSocket.getInputStream()));

            StringBuilder requestBuilder = new StringBuilder();
            String line;
            while (!(line = clientIn.readLine()).isEmpty()) {
                requestBuilder.append(line).append("\r\n");
            }
            requestBuilder.append("\r\n");

            offshoreOut.write(requestBuilder.toString());
            offshoreOut.flush();

            StringBuilder responseBuilder = new StringBuilder();
            String resLine;
            while ((resLine = offshoreIn.readLine()) != null) {
                responseBuilder.append(resLine).append("\r\n");
                if (resLine.isEmpty()) break;
            }

            clientOut.write(responseBuilder.toString());
            clientOut.flush();

            clientSocket.close();
        }
    }
}
