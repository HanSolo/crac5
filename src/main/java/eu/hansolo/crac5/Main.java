package eu.hansolo.crac5;

import org.crac.Context;
import org.crac.Resource;
import org.crac.CheckpointException;
import org.crac.Core;
import org.crac.RestoreException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ScheduledFuture;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;


/**
 * The Main class will run through a loop from 1 - 100_000
 * and will calculate if the number is a prime. Before it calculates the value
 * it will check the primeCache if the number is in the cache and if yes it will
 * return the value from the cache instead of calculation it.
 * The loop that runs from 1 - 100_000 will be called every 5 seconds.
 * It will take around 10-15 runs before the cache is fully loaded with values
 * because the cache will also remove values that have not been read within a given
 * period of time.
 * If you create the checkpoint after 10-15 runs and restore it after some time
 * you should see that the access times are still fast because the cache was also
 * restored.
 * To make that work correctly you will find some code in the afterRestore() method
 * in the GenericCache that takes the time between the checkpoint and the restore into
 * account.
 *
 * Log compilation:
 * java -Xmx256m -XX:MaxRAMPercentage=75 -XX:+UseParallelGC -XX:CRaCCheckpointTo=/home/hansolo/crac-files -XX:+UnlockDiagnosticVMOptions -XX:+LogCompilation -jar build/libs/crac5-17.0.0.jar
 * This log file can be analyzed by using JITWatch
 */
public class Main implements Resource {
    public  static final int                         DEFAULT_INTERVAL = 2;
    private static final Random                      RND              = new Random();
    private static final String                      CRAC_FILES       = "/opt/crac-files";
    private static final DateTimeFormatter           FORMATTER        = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private        final Path                        cracFilesFolder  = Paths.get(CRAC_FILES);
    private        final GenericCache<Long, Boolean> primeCache;
    private              boolean                     createCheckpoint = false;
    private              int                         counter;
    private              Runnable                    task;
    private              ScheduledExecutorService    executorService;
    private              ScheduledFuture<?>          future;
    private              long                        start;


    // ******************** Constructor ***************************************
    public Main(final Runtime runtime) {
        if (!Files.exists(cracFilesFolder)) {
            try {
                System.out.println("Creating " + CRAC_FILES);
                Files.createDirectory(cracFilesFolder);
            } catch (IOException e) {
                System.out.println("Error creating " + CRAC_FILES + ". " + e);
            }
        }

        try {
            createCheckpoint = isEmpty(cracFilesFolder);
        } catch (IOException e) {
            System.out.println("Error checking crac-files folder");
        }

        runtime.addShutdownHook(new Thread(() -> System.out.println("App stopped in shutdown hook")));

        final long initialCleanDelay = PropertyManager.INSTANCE.getLong(Constants.INITIAL_CACHE_CLEAN_DELAY, 50);
        final long cacheTimeout      = PropertyManager.INSTANCE.getLong(Constants.CACHE_TIMEOUT, 12);

        primeCache      = new GenericCache<>(initialCleanDelay, cacheTimeout);
        counter         = 1;
        task            = () -> {
            if (Thread.interrupted()) {
                System.out.println("Current thread interrupted");
                try {
                    executorService.shutdown();
                    System.out.println("Current thread interrupted, shutdown executor service");
                } catch (Exception e) {
                    Thread.currentThread().interrupt();
                }
            } else {
                checkForPrimes();
            }
        };
        executorService = Executors.newSingleThreadScheduledExecutor();
        start           = System.nanoTime();

        // Register this class as resource in the global context of CRaC
        System.out.println("Register Resource: Main");
        Core.getGlobalContext().register(Main.this);

        final long interval = PropertyManager.INSTANCE.getLong(Constants.INTERVAL, 5);
        future = executorService.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);
    }


    // ******************** Methods *******************************************
    @Override public void beforeCheckpoint(Context<? extends Resource> context) throws Exception {
        System.out.println("Application warmup time: " + ((System.nanoTime() - start) / 1_000_000_000) + " sec");

        System.out.println("beforeCheckpoint() called in Main");
        // Free resources or stop services
        if (!executorService.isTerminated()) {
            if (!future.isCancelled()) { future.cancel(true); }
            executorService.shutdown();
            executorService.awaitTermination(5, TimeUnit.SECONDS);
            if (!executorService.isTerminated()) { executorService.shutdownNow(); }
        }
        executorService = null;
    }

    @Override public void afterRestore(Context<? extends Resource> context) throws Exception {
        System.out.println("afterRestore() called in Main");
        // Restore resources or re-start services
        final long interval = PropertyManager.INSTANCE.getLong(Constants.INTERVAL, 5);
        executorService = Executors.newSingleThreadScheduledExecutor();
        executorService.scheduleAtFixedRate(task, 0, interval, TimeUnit.SECONDS);
    }

    private void checkForPrimes() {
        long start = System.nanoTime();
        for (long i = 1 ; i <= 100_000 ; i++) {
            isPrime(RND.nextInt(100_000));
        }
        System.out.println(counter + ". Run: " + ((System.nanoTime() - start) / 1_000_000 + " ms (" + primeCache.size() + " elements cached, " + String.format(Locale.US, "%.1f%%", primeCache.size() / 1_000.0) + ")"));

        // Create checkpoint after iteration 17
        if (createCheckpoint) {
            if (10 == counter) {
                checkpoint();
                executorService.shutdown();
            }
        }
        counter++;
    }

    private boolean isPrime(final long number) {
        if (number < 1) { return false; }
        if (primeCache.containsKey(number)) { return primeCache.get(number).get(); }
        boolean isPrime = true;
        for (long n = number ; n > 0 ; n--) {
            if (n != number && n != 1 && number % n == 0) {
                isPrime = false;
                break;
            }
        }
        primeCache.put(number, isPrime);
        return isPrime;
    }

    private void checkpoint() {
        /*
        try {
            System.out.println("Creating checkpoint...");
            Core.checkpointRestore();
            System.out.println("Checkpoint created: " + (new File(CRAC_FILES).listFiles().length > 0));
        } catch (CheckpointException | RestoreException e) {
            System.out.println("Error creating checkpoint: " + e);
        }
        */

        try {
            System.out.println("Create checkpoint using JCMD");
            final String         jcmd           = new StringBuilder().append("jcmd").append(" ").append(ProcessHandle.current().pid()).append(" ").append("JDK.checkpoint").toString();
            final String[]       checkpointJcmd = { "/bin/sh", "-c", jcmd };
            final ProcessBuilder processBuilder = new ProcessBuilder(checkpointJcmd);
            System.out.println("CMD to execute: /bin/sh -c " + jcmd);
            processBuilder.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isEmpty(final Path path) throws IOException {
        if (null == path || !Files.exists(path)) { return false; }
        if (Files.isDirectory(path)) {
            try (Stream<Path> entries = Files.list(path)) {
                return !entries.findFirst().isPresent();
            }
        }
        return false;
    }

    public static void main(String[] args) {
        long currentTime = System.currentTimeMillis();
        long vmStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        System.out.println("JVM startup time: " + (currentTime - vmStartTime) + "ms");

        Runtime runtime = Runtime.getRuntime();
        System.out.println(FORMATTER.format(LocalDateTime.now()) + " Starting application");
        System.out.println("Running on CRaC (PID " + ProcessHandle.current().pid() + ")");
        System.out.println("First run will take up to 30 seconds...");
        new Main(runtime);

        try {
            while (true) { Thread.sleep(1000); }
        } catch (InterruptedException e) { }
    }
}
