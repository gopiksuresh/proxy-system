
import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class ShipProxy {
    public static void main(String[] args) throws IOException {
        ServerSocket httpServer = new ServerSocket(8080);
        Socket offshore = new Socket("localhost", 9000);
        DataOutputStream offOut = new DataOutputStream(offshore.getOutputStream());
        DataInputStream offIn = new DataInputStream(offshore.getInputStream());
        ExecutorService pool = Executors.newCachedThreadPool();

        System.out.println("Ship Proxy listening on port 8080");

        while (true) {
            Socket client = httpServer.accept();
            pool.submit(() -> handleClient(client, offOut, offIn));
        }
    }

    private static void handleClient(Socket client, DataOutputStream offOut, DataInputStream offIn) {
        try (Socket sock = client;
             InputStream cin = sock.getInputStream();
             OutputStream cout = sock.getOutputStream()) {

            byte[] req = readHttpRequest(cin);
            synchronized (offOut) {
                offOut.writeInt(req.length);
                offOut.write(req);
                offOut.flush();
            }

            byte[] resp = readFramedResponse(offIn);
            cout.write(resp);
            cout.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] readHttpRequest(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int read;
        while ((read = in.read(buf)) != -1) {
            baos.write(buf, 0, read);
            if (containsEndOfHeaders(baos.toByteArray())) break;
        }
        return baos.toByteArray();
    }

    private static boolean containsEndOfHeaders(byte[] data) {
        String s = new String(data, 0, Math.min(data.length, 8192));
        return s.contains("\r\n\r\n");
    }

    private static byte[] readFramedResponse(DataInputStream in) throws IOException {
        int len = in.readInt();
        byte[] resp = new byte[len];
        in.readFully(resp);
        return resp;
    }
}
