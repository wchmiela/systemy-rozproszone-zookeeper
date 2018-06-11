package pl.edu.agh.sr.zookeeper;

import org.apache.zookeeper.AsyncCallback.StatCallback;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.KeeperException.Code;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Arrays;

class DataMonitor implements StatCallback, Watcher {

    private final ZooKeeper zooKeeper;
    private final String znode;
    private final Watcher chainedWatcher;
    private final DataMonitorListener listener;
    boolean dead;
    private byte[] prevData;

    private static int childrenCount;

    DataMonitor(ZooKeeper zooKeeper, String znode, Watcher chainedWatcher, DataMonitorListener listener) {
        this.zooKeeper = zooKeeper;
        this.znode = znode;
        this.chainedWatcher = chainedWatcher;
        this.listener = listener;

        zooKeeper.exists(znode, true, this, null);
    }

    @Override
    public void processResult(int rc, String path, Object ctx, Stat stat) {
        boolean exists;
        switch (rc) {
            case Code.Ok:
                exists = true;
                break;
            case Code.NoNode:
                exists = false;
                break;
            case Code.SessionExpired:
            case Code.NoAuth:
                dead = true;
                listener.closing(rc);
                return;
            default:
                // Retry errors
                zooKeeper.exists(znode, true, this, null);
                return;
        }

        byte b[] = null;
        if (exists) {
            try {
                b = zooKeeper.getData(znode, false, null);
            } catch (KeeperException e) {
                // We don't need to worry about recovering now. The watch
                // callbacks will kick off any exception handling
                e.printStackTrace();
            } catch (InterruptedException e) {
                return;
            }
        }
        if ((b == null && b != prevData)
                || (b != null && !Arrays.equals(prevData, b))) {
            listener.exists(b);
            prevData = b;

            try {
                if (zooKeeper.exists(znode, false) == null) {
                    return;
                }
            } catch (KeeperException | InterruptedException e) {
                System.out.println("exists error : " + e.getMessage());
            }

            try {
                zooKeeper.getChildren(znode, this);
            } catch (KeeperException | InterruptedException e) {
                System.out.println("getChildren error: " + e.getMessage());
            }
        }
    }

    @Override
    public void process(WatchedEvent event) {
        String path = event.getPath();
        switch (event.getType()) {
            case None:
                // We are are being told that the state of the
                // connection has changed
                switch (event.getState()) {
                    case SyncConnected:
                        // In this particular example we don't need to do anything
                        // here - watches are automatically re-registered with
                        // server and any watches triggered while the client was
                        // disconnected will be delivered (in order of course)
                        break;
                    case Expired:
                        // It's all over
                        dead = true;
                        listener.closing(Code.SessionExpired);
                        break;
                }
                break;
            case NodeChildrenChanged:
                try {
                    if (zooKeeper.exists(znode, false) == null) {
                        return;
                    }
                } catch (KeeperException | InterruptedException e) {
                    System.out.println("exists error : " + e.getMessage());
                }

                childrenCount = 0;
                countChildren(znode);

                System.out.println(String.format("Children count: %d", childrenCount));

                try {
                    zooKeeper.getChildren(znode, this);
                } catch (KeeperException | InterruptedException e) {
                    System.out.println("getChildren error: " + e.getMessage());
                }

                break;
            default:
                if (path != null && path.equals(znode)) {
                    // Something has changed on the node, let's find out
                    zooKeeper.exists(znode, true, this, null);
                }
                break;
        }

        if (chainedWatcher != null) {
            chainedWatcher.process(event);
        }
    }

    private void countChildren(String znode) {
        try {
            if (zooKeeper.exists(znode, false) == null) {
                return;
            }
        } catch (KeeperException | InterruptedException e) {
            System.out.println("exists error : " + e.getMessage());
        }

        try {
            childrenCount += zooKeeper.getChildren(znode, false).size();
        } catch (KeeperException | InterruptedException e) {
            System.out.println("getChildren error: " + e.getMessage());
        }

        try {
            zooKeeper.getChildren(znode, false)
                    .stream()
                    .map(child -> String.format("%s/%s", znode, child))
                    .forEachOrdered(this::countChildren);
        } catch (KeeperException | InterruptedException e) {
            System.out.println("getChildren error: " + e.getMessage());
        }
    }
}
