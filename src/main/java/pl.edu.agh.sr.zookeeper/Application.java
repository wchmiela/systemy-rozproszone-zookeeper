package pl.edu.agh.sr.zookeeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Application {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.err.println("USAGE: Executor hostPort znode filename program [args ...]");
            System.exit(1);
        }

        String hostPort = args[0];
        String znode = Consts.NODE_NAME;
        String filename = args[1];
        String exec[] = new String[args.length - 2];
        System.arraycopy(args, 2, exec, 0, exec.length);


        Executor executor = null;

        try {
            executor = new Executor(hostPort, znode, filename, exec);
        } catch (Exception e) {
            System.out.println("Executor error: " + e.getMessage());
            System.exit(1);
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(executor);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Opcje: ");
        System.out.println("\tprint tree - wyswietl drzewo");
        System.out.println("\texit");

        System.out.print("Wczytaj opcje: ");

        try {

            while (true) {
                String option = bufferedReader.readLine();

                if (option.toLowerCase().equals("print tree")) {
                    executor.printTree(znode);
                } else {
                    executorService.shutdown();
                }
            }
        } catch (IOException e) {
            System.out.println("BufferedReader error " + e.getMessage());
        }
    }
}
