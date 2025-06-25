package cl.uchile.fea.grpc;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.grpc.Grpc;
import io.grpc.InsecureServerCredentials;
import io.grpc.Server;
import io.grpc.ServerBuilder;

/**
 * The signer server.
 * @see <a href="https://grpc.io/docs/languages/java/basics/#starting-the-server">Starting the server</a>
 */
public class SignerServer {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SignerServer.class);

    /**
     * The listening port.
     */
    private final int port;
    /**
     * The custom executor.
     */
    private final ExecutorService executor;
    /**
     * The server.
     */
    private final Server server;

    /**
     * Creates a server listening on {@code port}.
     * @param port The listening port
     * @param nThreads The number of threads
     * @param size The maximum inbound message size (in bytes)
     */
    public SignerServer(int port, int nThreads, int size) {
        this(Grpc.newServerBuilderForPort(port, InsecureServerCredentials.create()), port, nThreads, size);
    }

    /**
     * Creates a server using the builder as base.
     * @param serverBuilder The server builder
     * @param port The listening port
     * @param nThreads The number of threads
     * @param size The maximum inbound message size (in bytes)
     */
    public SignerServer(ServerBuilder<?> serverBuilder, int port, int nThreads, int size) {
        this.port = port;

        executor = Executors.newFixedThreadPool(nThreads);
        server = serverBuilder.addService(new SignerService())
            .maxInboundMessageSize(size)
            .executor(executor)
            .build();
    }

    /**
     * Start serving requests.
     * @throws IOException if unable to bind
     */
    public void start() throws IOException {
        if (server != null) {
            server.start();

            LOGGER.info("Server started, listening on port {}", port);

            Runtime.getRuntime().addShutdownHook(new Thread() {

                @Override
                public void run() {
                    // Use stderr here since the logger may have been reset by its JVM shutdown hook.
                    System.err.println("*** shutting down gRPC server since JVM is shutting down");

                    try {
                        SignerServer.this.stop();
                    } catch (InterruptedException e) {
                        e.printStackTrace(System.err);
                    }

                    System.err.println("*** server shut down");
                }
            });
        }
    }

    /**
     * Stop serving requests and shutdown resources.
     * @throws InterruptedException
     */
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }

        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * Await termination on the main thread since the gRPC library uses daemon threads.
     * @throws InterruptedException
     */
    public void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }
}
