import java.io.*;
import java.net.*;
import java.util.concurrent.*;

public class OffshoreProxy {
    public static void main(String[] args) throws IOException {
        ServerSocket server = new ServerSocket(9000);
        System.out.println("Offshore waiting for ship...");
        Socket shipSocket = server.accept();
        System.out.println("Ship connected");

        ExecutorService pool = Executors.newCachedThreadPool();

        DataInputStream shipIn = new DataInputStream(shipSocket.getInputStream());
        DataOutputStream shipOut = new DataOutputStream(shipSocket.getOutputStream());

        while (true) {
            try {
                int len = shipIn.readInt();
                byte[] reqBytes = new byte[len];
                shipIn.readFully(reqBytes);

                // Handle each request in its own thread
                pool.submit(() -> handleRequest(reqBytes, shipIn, shipOut));
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }

    private static void handleRequest(byte[] reqBytes,
                                      DataInputStream shipIn,
                                      DataOutputStream shipOut) {
        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(reqBytes);
            BufferedReader reader = new BufferedReader(new InputStreamReader(bais));
            String line = reader.readLine();

            if (line == null) return;

            boolean isConnect = line.startsWith("CONNECT");
            String targetHost = "";
            int targetPort = isConnect ? 443 : 80;

            String[] parts = line.split(" ");
            if (isConnect) {
                String[] hp = parts[1].split(":");
                targetHost = hp[0];
                targetPort = Integer.parseInt(hp[1]);
            } else {
                String ln;
                while ((ln = reader.readLine()) != null && !ln.isEmpty()) {
                    if (ln.toLowerCase().startsWith("host:")) {
                        String hostLine = ln.substring(5).trim();
                        if (hostLine.contains(":")) {
                            String[] hp = hostLine.split(":");
                            targetHost = hp[0];
                            targetPort = Integer.parseInt(hp[1]);
                        } else {
                            targetHost = hostLine;
                        }
                    }
                }
            }

            Socket target = new Socket(targetHost, targetPort);
            InputStream tin = target.getInputStream();
            OutputStream tout = target.getOutputStream();

            if (isConnect) {
                // Inform ship that tunnel is established
                byte[] respBytes = "HTTP/1.1 200 Connection Established\r\n\r\n".getBytes();
                synchronized (shipOut) {
                    shipOut.writeInt(respBytes.length);
                    shipOut.write(respBytes);
                    shipOut.flush();
                }

                forwardBiDirectional(shipIn, shipOut, tin, tout);
            } else {
                // Forward normal HTTP request
                tout.write(reqBytes);
                tout.flush();

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int read;
                while ((read = tin.read(buf)) != -1) {
                    baos.write(buf, 0, read);
                    if (tin.available() == 0) break;
                }
                byte[] resp = baos.toByteArray();
                synchronized (shipOut) {
                    shipOut.writeInt(resp.length);
                    shipOut.write(resp);
                    shipOut.flush();
                }

                target.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void forwardBiDirectional(DataInputStream shipIn,
                                             DataOutputStream shipOut,
                                             InputStream tin,
                                             OutputStream tout) {
        Thread t1 = new Thread(() -> {
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = shipIn.read(buf)) != -1) {
                    tout.write(buf, 0, n);
                    tout.flush();
                }
            } catch (IOException ignored) {}
        });

        Thread t2 = new Thread(() -> {
            try {
                byte[] buf = new byte[8192];
                int n;
                while ((n = tin.read(buf)) != -1) {
                    shipOut.write(buf, 0, n);
                    shipOut.flush();
                }
            } catch (IOException ignored) {}
        });

        t1.start();
        t2.start();

        try {
            t1.join();
            t2.join();
        } catch (InterruptedException ignored) {}
    }
}
