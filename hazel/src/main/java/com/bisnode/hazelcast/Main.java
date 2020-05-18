package com.bisnode.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.MulticastConfig;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.config.SSLConfig;
import com.hazelcast.config.TcpIpConfig;
import com.hazelcast.core.DistributedObject;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.Member;
import com.hazelcast.spi.properties.GroupProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static spark.Spark.awaitInitialization;
import static spark.Spark.port;
import static spark.Spark.post;

public class Main {
    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static HazelcastInstance hazelcastInstance = null;

    private static final int PORT = 5701;

    public static void main(String[] args) {

        List<String> hazelcastEndpoints = new ArrayList<>();

        hazelcastInstance = runHazelcast(configureHazelcast(hazelcastEndpoints));

        while (!hazelcastInstance.getPartitionService().isLocalMemberSafe()) {
            log.info("Waiting for safe cluster state");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        runWebServer();
    }

    private static Config configureHazelcast(List<String> nodes) {
        final Config cfg = new Config();

        cfg.setInstanceName(UUID.randomUUID().toString());

        final NetworkConfig netCfg = new NetworkConfig();
        netCfg.setPortAutoIncrement(false);
        netCfg.setPort(PORT);

        final MulticastConfig mcCfg = new MulticastConfig().setEnabled(false);

        final TcpIpConfig tcpCfg = new TcpIpConfig().setEnabled(true);
        nodes.forEach(tcpCfg::addMember);

        final JoinConfig joinCfg = new JoinConfig();
        joinCfg.setMulticastConfig(mcCfg);
        joinCfg.setTcpIpConfig(tcpCfg);
        netCfg.setJoin(joinCfg);

        netCfg.setSSLConfig(new SSLConfig().setEnabled(false));

        cfg.setNetworkConfig(netCfg);

        cfg.setProperty(GroupProperty.MAX_NO_HEARTBEAT_SECONDS.getName(), "20");
        cfg.setProperty(GroupProperty.LOGGING_TYPE.getName(), "slf4j");

        cfg.setProperty(GroupProperty.MEMCACHE_ENABLED.getName(), "true");

        cfg.setProperty(GroupProperty.SHUTDOWNHOOK_ENABLED.getName(), "false");

        return cfg;
    }

    private static HazelcastInstance runHazelcast(Config cfg) {
        final HazelcastInstance hazelcastInstance = Hazelcast.newHazelcastInstance(cfg);

        while (!hazelcastInstance.getLifecycleService().isRunning()) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                // ignore
            }
        }

        log.info("Hazelcast instance started");

        final ExecutorService executorService = Executors.newSingleThreadExecutor();
        executorService.submit(() -> logInfoLoop(hazelcastInstance));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutdown hook called");
            executorService.shutdown();
            hazelcastInstance.getLifecycleService().shutdown();
            log.info("Shutdown hook done");
        }));

        return hazelcastInstance;
    }

    private static void runWebServer() {
        port(8082);
        post("/shutdown", (req, res) -> {
            log.info("Shutdown called");
            if (hazelcastInstance != null) {
                hazelcastInstance.getPartitionService().forceLocalMemberToBeSafe(5, TimeUnit.SECONDS);
                hazelcastInstance.getLifecycleService().shutdown();
                while (hazelcastInstance.getLifecycleService().isRunning()) {
                    Thread.sleep(100);
                }
                log.info("Shutdown done");
            }
            return "OK";
        });

        awaitInitialization();

        log.info("Web server started on port 8082");
    }


    private static void logInfoLoop(HazelcastInstance hazelcastInstance) {
        String prev = "";
        while (true) {
            String output = getClusterInfo(hazelcastInstance);

            if (!output.equals(prev)) {
                prev = output;
                String[] ss = output.split("\n");
                for (String s : ss) {
                    log.info(s);
                }
            }

            try {
                Thread.sleep(30000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private static String getClusterInfo(HazelcastInstance hazelcastInstance) {
        Set<Member> members = hazelcastInstance.getCluster().getMembers();

        StringBuilder output = new StringBuilder(String.format("Number of members: %d\n", members.size()));
        for (Member member : members) {
            output.append(String.format("Member: %s %s\n", member.getUuid(), member.getAddress().getHost()));
        }

        output.append(String.format("Cluster state: %s\n", hazelcastInstance.getCluster().getClusterState().name()));
        output.append(String.format("Cluster safe: %s\n", hazelcastInstance.getPartitionService().isClusterSafe()));
        output.append(String.format("Local member safe: %s\n", hazelcastInstance.getPartitionService().isLocalMemberSafe()));
        output.append(String.format("Number of clients: %d\n", hazelcastInstance.getClientService().getConnectedClients().size()));

        Collection<DistributedObject> distributedObjects = hazelcastInstance.getDistributedObjects();
        output.append(String.format("Number of collections: %d\n", distributedObjects.size()));
        for (DistributedObject dio : distributedObjects) {
            output.append(String.format("Name: %s Key: %s Svc: %s\n", dio.getName(), dio.getPartitionKey(), dio.getServiceName()));

            try {
                Method sizeMethod = dio.getClass().getMethod("size");
                Object size = sizeMethod.invoke(dio);
                output.append(String.format("Size: %s\n", size.toString()));
            } catch (NoSuchMethodException e) {
                // ignore
            } catch (InvocationTargetException | IllegalAccessException e) {
                log.warn("Failed to get size of collection", e);
            }
        }

        return output.toString();
    }

}
