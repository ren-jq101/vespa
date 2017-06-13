// Copyright 2016 Yahoo Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.config.server.application;

import com.yahoo.config.model.api.FileDistribution;
import com.yahoo.config.model.api.HostInfo;
import com.yahoo.config.model.api.Model;
import com.yahoo.config.model.api.PortInfo;
import com.yahoo.config.model.api.ServiceInfo;
import com.yahoo.config.provision.ClusterSpec;
import com.yahoo.config.provision.ProvisionInfo;
import com.yahoo.vespa.config.ConfigKey;
import com.yahoo.vespa.config.ConfigPayload;
import com.yahoo.vespa.config.buildergen.ConfigDefinition;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

// Model with two services, one that does not have a state port
class MockModel implements Model {
    private final Collection<HostInfo> hosts;

    static MockModel createContainer(String hostname, int statePort) {
        ServiceInfo container = createServiceInfo(hostname, "container", "container",
                ClusterSpec.Type.container, statePort, "state");
        ServiceInfo serviceNoStatePort = createServiceInfo(hostname, "logserver", "logserver",
                ClusterSpec.Type.admin, 1234, "logtp");
        HostInfo hostInfo = new HostInfo(hostname, Arrays.asList(container, serviceNoStatePort));
        return new MockModel(Collections.singleton(hostInfo));
    }

    static MockModel createClusterController(String hostname, int statePort) {
        ServiceInfo container = createServiceInfo(
                hostname,
                "foo", // name
                "container-clustercontroller", // type
                ClusterSpec.Type.container,
                statePort,
                "state http external query");
        ServiceInfo serviceNoStatePort = createServiceInfo(hostname, "storagenode", "storagenode",
                ClusterSpec.Type.content, 1234, "rpc");
        HostInfo hostInfo = new HostInfo(hostname, Arrays.asList(container, serviceNoStatePort));

        return new MockModel(Collections.singleton(hostInfo));
    }

    static private ServiceInfo createServiceInfo(
            String hostname,
            String name,
            String type,
            ClusterSpec.Type clusterType,
            int port,
            String portTags) {
        PortInfo portInfo = new PortInfo(port, Arrays.stream(portTags.split(" ")).collect(Collectors.toSet()));
        Map<String, String> properties = new HashMap<>();
        properties.put("clustername", "default");
        properties.put("clustertype", clusterType.name());
        return new ServiceInfo(name, type, Collections.singleton(portInfo), properties, "", hostname);
    }

    MockModel(Collection<HostInfo> hosts) {
        this.hosts = hosts;
    }

    @Override
    public ConfigPayload getConfig(ConfigKey<?> configKey, ConfigDefinition targetDef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<ConfigKey<?>> allConfigsProduced() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<HostInfo> getHosts() {
        return hosts;
    }

    @Override
    public Set<String> allConfigIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void distributeFiles(FileDistribution fileDistribution) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<ProvisionInfo> getProvisionInfo() {
        throw new UnsupportedOperationException();
    }
}
