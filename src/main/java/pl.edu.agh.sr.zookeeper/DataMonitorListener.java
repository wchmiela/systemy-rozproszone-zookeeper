package pl.edu.agh.sr.zookeeper;

public interface DataMonitorListener {

    void exists(byte data[]);

    void closing(int rc);
}
