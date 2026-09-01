package ir.hamgit.ahh.PvZ.server;

import java.nio.file.Path;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;

 
public final class PhaseThreeServerMain {
    private PhaseThreeServerMain() { }

    public static void main(String[] args) throws Exception {
        String host = System.getProperty("pvz.server.host", "127.0.0.1");
        int port = Integer.getInteger("pvz.server.port", 8080);
        Path data = Path.of(System.getProperty("pvz.server.data", "server-data/accounts.dat"));
        for (int index = 0; index < args.length; index++) {
            switch (args[index].toLowerCase(Locale.ROOT)) {
                case "--host" -> host = requireValue(args, ++index, "--host");
                case "--port" -> port = Integer.parseInt(requireValue(args, ++index, "--port"));
                case "--data" -> data = Path.of(requireValue(args, ++index, "--data"));
                default -> throw new IllegalArgumentException("Unknown argument: " + args[index]);
            }
        }
        PhaseThreeServer server = new PhaseThreeServer(host, port, data);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close, "pvz-phase3-shutdown"));
        server.start();
        System.out.println("PvZ Phase 3 server listening on http://" + host + ":" + server.getPort());
        System.out.println("Account database: " + data.toAbsolutePath());
        new CountDownLatch(1).await();
    }

    private static String requireValue(String[] args, int index, String option) {
        if (index >= args.length) throw new IllegalArgumentException(option + " requires a value.");
        return args[index];
    }
}
