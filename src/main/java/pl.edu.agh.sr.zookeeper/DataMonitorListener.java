package pl.edu.agh.sr.zookeeper;

interface DataMonitorListener {

    void exists(byte data[]);

    void closing(int rc);
}
