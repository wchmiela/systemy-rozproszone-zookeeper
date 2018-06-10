package pl.edu.agh.sr.zookeeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Application {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("USAGE: Application port apps-to-run");
            System.exit(1);
        }

        String hostPort = args[0];
        String znode = Consts.NODE_NAME;
        String exec[] = new String[args.length - 1];
        System.arraycopy(args, 1, exec, 0, exec.length);

        Executor executor = null;

        try {
            executor = new Executor(hostPort, znode, exec);
        } catch (Exception e) {
            System.out.println("Executor error: " + e.getMessage());
            System.exit(1);
        }

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(executor);

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Options: ");
        System.out.println("\tprint tree");
        System.out.println("\t_ - exit");

        System.out.print("Read option: ");

        try {

            while (true) {
                String option = bufferedReader.readLine();

                if (option.toLowerCase().equals("print tree")) {
                    executor.printTree(znode);
                } else {
                    executorService.shutdown();
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("BufferedReader error " + e.getMessage());
        }
    }
}
