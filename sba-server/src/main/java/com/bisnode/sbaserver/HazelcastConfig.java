package com.bisnode.sbaserver;

import com.hazelcast.client.config.ClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {

    @Bean
    public ClientConfig hcConfig(@Value("${hazelcast.address}") String hazelcastAddress) {
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getNetworkConfig().addAddress(hazelcastAddress);
        return clientConfig;
    }

}
