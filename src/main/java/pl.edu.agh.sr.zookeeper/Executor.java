package pl.edu.agh.sr.zookeeper;

import org.apache.zookeeper.*;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Executor implements Runnable, Watcher, DataMonitorListener {

    private final String filename;
    private final String[] exec;
    private final String rootZnode;
    ZooKeeper zooKeeper;
    DataMonitor datamonitor;
    Process child;


    public Executor(String hostPort, String znode, String filename, String[] exec) throws IOException {
        this.filename = filename;
        this.exec = exec;
        this.rootZnode = znode;
        zooKeeper = new ZooKeeper(hostPort, 1000, this);
        datamonitor = new DataMonitor(zooKeeper, znode, null, this);
    }

    @Override
    public void run() {
        try {
            synchronized (this) {
                while (!datamonitor.dead) {
                    wait();
                }
            }
        } catch (InterruptedException e) {
        }
    }

    @Override
    public void process(WatchedEvent event) {
        datamonitor.process(event);
    }

    @Override
    public void exists(byte[] data) {

        if (data == null) {
            if (child != null) {
                System.out.println("Killing process");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                }
            }
            child = null;
        } else {
            if (child != null) {
                System.out.println("Stopping child");
                child.destroy();
                try {
                    child.waitFor();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileOutputStream fos = new FileOutputStream(filename);
                fos.write(data);
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                System.out.println("Starting child");
                child = Runtime.getRuntime().exec(exec);
                new StreamWriter(child.getInputStream(), System.out);
                new StreamWriter(child.getErrorStream(), System.err);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void closing(int rc) {
        synchronized (this) {
            notifyAll();
        }
    }

    void printTree(String znode) {
        try {
            if (zooKeeper.exists(znode, false) == null) {
                System.out.println("Nie ma takiego wezla.");
                return;
            }
        } catch (KeeperException | InterruptedException e) {
            System.out.println("exists error : " + e.getMessage());
        }

        try {
            String[] splitted = znode.split("/");
            int count = splitted.length;
            String prefix = IntStream.range(0, count - 2).mapToObj(i -> "\t").collect(Collectors.joining());

            System.out.println(prefix + splitted[count - 1]);

            zooKeeper.getChildren(znode, false)
                    .stream()
                    .map(child -> String.format("%s/%s", znode, child))
                    .forEachOrdered(this::printTree);
        } catch (KeeperException | InterruptedException e) {
            System.out.println("getChildren error: " + e.getMessage());
        }
    }
}
