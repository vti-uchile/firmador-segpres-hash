package cl.uchile.fea;

import java.io.IOException;
import java.security.Security;
import java.util.logging.Level;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.bridge.SLF4JBridgeHandler;

import cl.uchile.fea.grpc.SignerServer;
import co.elastic.apm.attach.ElasticApmAttacher;

/**
 * The application class.
 */
public class App {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);

    /**
     * Checks if the value of the environement is null or empty.
     * @param name The environment name
     */
    private static void checkEnv(String name) {
        String value = System.getenv(name);
        if (value == null || value.isEmpty()) {
            LOGGER.error("Got null or empty value for environment {}", name);

            System.exit(1);
        }
    }

    /**
     * Main method.  This comment makes the linter happy.
     * @param args The arguments
     * @throws IOException if unable to bind
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        SLF4JBridgeHandler.removeHandlersForRootLogger();
        SLF4JBridgeHandler.install();

        java.util.logging.Logger.getLogger("").setLevel(Level.FINEST);

        // https://www.elastic.co/guide/en/apm/agent/java/current/setup-attach-api.html
        ElasticApmAttacher.attach();

        // check the environment variables
        checkEnv("SEGPRES_API_TOKEN_KEY");
        checkEnv("SEGPRES_SECRET");
        checkEnv("SEGPRES_BASE_URL");

        if (Security.getProvider("BC") == null) {
            Security.addProvider(new BouncyCastleProvider());
        }

        //PdfReader.unethicalreading = true; // PdfReader not opened with owner password

        int nThreads = 5;
        try {
            nThreads = Utils.getEnv("APP_THREADS", 1, 20, nThreads); // between 1 and 20 threads
        } catch (NumberFormatException e) {
            LOGGER.warn("Unable to get threads ({}), using {}", e.getMessage(), nThreads);
        }

        int size = 4 * 1024 * 1024; // 4MB
        try {
            size = Utils.getEnv("APP_MAX_INBOUND_MESSAGE_SIZE", 1*1024*1024, 100*1024*1024, size); // between 1MB and 100MB
        } catch (NumberFormatException e) {
            LOGGER.warn("Unable to get maximum inbound message size ({}), using {} bytes", e.getMessage(), size);
        }

        SignerServer server = new SignerServer(8080, nThreads, size);
        server.start();
        server.blockUntilShutdown();
    }
}
