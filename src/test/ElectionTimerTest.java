package test;

import registry.Registry;
import server.Server;

/**
 * Created by joe on 05/11/16.
 */
public class ElectionTimerTest {

    public static void main (String[] args) {

        Registry.SERVERS.put("Ted", new Server("Ted"));
        Registry.SERVERS.put("Bob", new Server("Bob"));
        Registry.SERVERS.put("Frank", new Server("Frank"));

    }
}
