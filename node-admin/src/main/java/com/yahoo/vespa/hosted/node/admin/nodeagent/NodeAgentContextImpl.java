// Copyright 2020 Oath Inc. Licensed under the terms of the Apache 2.0 license. See LICENSE in the project root.
package com.yahoo.vespa.hosted.node.admin.nodeagent;

import com.yahoo.config.provision.CloudName;
import com.yahoo.config.provision.SystemName;
import com.yahoo.config.provision.zone.ZoneApi;
import com.yahoo.config.provision.zone.ZoneId;
import com.yahoo.vespa.athenz.api.AthenzIdentity;
import com.yahoo.vespa.athenz.api.AthenzService;
import com.yahoo.vespa.hosted.dockerapi.ContainerName;
import com.yahoo.vespa.hosted.node.admin.configserver.noderepository.Acl;
import com.yahoo.vespa.hosted.node.admin.configserver.noderepository.NodeSpec;
import com.yahoo.vespa.hosted.node.admin.docker.DockerNetworking;

import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.ProviderMismatchException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author freva
 */
public class NodeAgentContextImpl implements NodeAgentContext {

    private final String logPrefix;
    private final NodeSpec node;
    private final Acl acl;
    private final ContainerName containerName;
    private final AthenzIdentity identity;
    private final DockerNetworking dockerNetworking;
    private final ZoneApi zone;
    private final FileSystem fileSystem;
    private final Path root;
    private final Path pathToNodeRootOnHost;
    private final Path pathToVespaHome;
    private final String vespaUser;
    private final String vespaUserOnHost;
    private final double cpuSpeedup;

    public NodeAgentContextImpl(NodeSpec node, Acl acl, AthenzIdentity identity,
                                DockerNetworking dockerNetworking, ZoneApi zone,
                                FileSystem fileSystem,
                                Path pathToContainerStorage, Path pathToVespaHome,
                                String vespaUser, String vespaUserOnHost, double cpuSpeedup) {
        if (cpuSpeedup <= 0)
            throw new IllegalArgumentException("cpuSpeedUp must be positive, was: " + cpuSpeedup);

        this.node = Objects.requireNonNull(node);
        this.acl = Objects.requireNonNull(acl);
        this.containerName = ContainerName.fromHostname(node.hostname());
        this.identity = Objects.requireNonNull(identity);
        this.dockerNetworking = Objects.requireNonNull(dockerNetworking);
        this.zone = Objects.requireNonNull(zone);
        this.fileSystem = fileSystem;
        this.root = fileSystem.getPath("/");
        this.pathToNodeRootOnHost = requireValidPath(pathToContainerStorage).resolve(containerName.asString());
        this.pathToVespaHome = requireValidPath(pathToVespaHome);
        this.logPrefix = containerName.asString() + ": ";
        this.vespaUser = vespaUser;
        this.vespaUserOnHost = vespaUserOnHost;
        this.cpuSpeedup = cpuSpeedup;
    }

    @Override
    public NodeSpec node() {
        return node;
    }

    @Override
    public Acl acl() {
        return acl;
    }

    @Override
    public ContainerName containerName() {
        return containerName;
    }

    @Override
    public AthenzIdentity identity() {
        return identity;
    }

    @Override
    public DockerNetworking dockerNetworking() {
        return dockerNetworking;
    }

    @Override
    public ZoneApi zone() {
        return zone;
    }

    @Override
    public String vespaUser() {
        return vespaUser;
    }

    @Override
    public String vespaUserOnHost() {
        return vespaUserOnHost;
    }

    @Override
    public double unscaledVcpu() {
        return node.vcpu() / cpuSpeedup;
    }

    @Override
    public FileSystem fileSystem() {
        return fileSystem;
    }

    @Override
    public Path pathOnHostFromPathInNode(Path pathInNode) {
        requireValidPath(pathInNode);

        if (! pathInNode.isAbsolute())
            throw new IllegalArgumentException("Expected an absolute path in the container, got: " + pathInNode);

        return pathToNodeRootOnHost.resolve(pathInNode.getRoot().relativize(pathInNode).toString());
    }

    @Override
    public Path pathInNodeFromPathOnHost(Path pathOnHost) {
        requireValidPath(pathOnHost);

        if (! pathOnHost.isAbsolute())
            throw new IllegalArgumentException("Expected an absolute path on the host, got: " + pathOnHost);

        if (!pathOnHost.startsWith(pathToNodeRootOnHost))
            throw new IllegalArgumentException("Path " + pathOnHost + " does not exist in the container");

        return root.resolve(pathToNodeRootOnHost.relativize(pathOnHost).toString());
    }

    @Override
    public Path pathInNodeUnderVespaHome(Path relativePath) {
        requireValidPath(relativePath);

        if (relativePath.isAbsolute())
            throw new IllegalArgumentException("Expected a relative path to the Vespa home, got: " + relativePath);

        return relativePath.getFileSystem().getPath(pathToVespaHome.resolve(relativePath.toString()).toString());
    }

    @Override
    public void recordSystemModification(Logger logger, String message) {
        log(logger, message);
    }

    @Override
    public void log(Logger logger, Level level, String message) {
        logger.log(level, logPrefix + message);
    }

    @Override
    public void log(Logger logger, Level level, String message, Throwable throwable) {
        logger.log(level, logPrefix + message, throwable);
    }

    @Override
    public Path rewritePathInNodeForWantedDockerImage(Path path) {
        requireValidPath(path);

        if (!node().wantedDockerImage().get().repository().endsWith("/vespa/ci")) return path;

        Path originalVespaHome = pathInNodeUnderVespaHome("");
        if (!path.startsWith(originalVespaHome)) return path;

        return fileSystem.getPath("/home/y").resolve(originalVespaHome.relativize(path));
    }

    @Override
    public String toString() {
        return "NodeAgentContextImpl{" +
                "node=" + node +
                ", acl=" + acl +
                ", containerName=" + containerName +
                ", identity=" + identity +
                ", dockerNetworking=" + dockerNetworking +
                ", zone=" + zone +
                ", pathToNodeRootOnHost=" + pathToNodeRootOnHost +
                ", pathToVespaHome=" + pathToVespaHome +
                ", vespaUser='" + vespaUser + '\'' +
                ", vespaUserOnHost='" + vespaUserOnHost + '\'' +
                '}';
    }

    private Path requireValidPath(Path path) {
        Objects.requireNonNull(path);

        Objects.requireNonNull(fileSystem); // to allow this method to be used in constructor.
        if (!path.getFileSystem().provider().equals(fileSystem.provider())) {
            throw new ProviderMismatchException("Expected file system provider " + fileSystem.provider() +
                    " but " + path + " had " + path.getFileSystem().provider());
        }

        return path;
    }

    /** For testing only! */
    public static class Builder {
        private NodeSpec.Builder nodeSpecBuilder;
        private Acl acl;
        private AthenzIdentity identity;
        private DockerNetworking dockerNetworking;
        private ZoneApi zone;
        private String vespaUser;
        private String vespaUserOnHost;
        private FileSystem fileSystem = FileSystems.getDefault();
        private double cpuSpeedUp = 1;

        public Builder(NodeSpec node) {
            this.nodeSpecBuilder = new NodeSpec.Builder(node);
        }

        /**
         * Creates a NodeAgentContext.Builder with a NodeSpec that has the given hostname and some
         * reasonable values for the remaining required NodeSpec fields. Use {@link #Builder(NodeSpec)}
         * if you want to control the entire NodeSpec.
         */
        public Builder(String hostname) {
            this.nodeSpecBuilder = NodeSpec.Builder.testSpec(hostname);
        }

        public Builder nodeSpecBuilder(Function<NodeSpec.Builder, NodeSpec.Builder> nodeSpecBuilderModifier) {
            this.nodeSpecBuilder = nodeSpecBuilderModifier.apply(nodeSpecBuilder);
            return this;
        }

        public Builder acl(Acl acl) {
            this.acl = acl;
            return this;
        }

        public Builder identity(AthenzIdentity identity) {
            this.identity = identity;
            return this;
        }

        public Builder dockerNetworking(DockerNetworking dockerNetworking) {
            this.dockerNetworking = dockerNetworking;
            return this;
        }

        public Builder zone(ZoneApi zone) {
            this.zone = zone;
            return this;
        }

        public Builder vespaUser(String vespaUser) {
            this.vespaUser = vespaUser;
            return this;
        }

        public Builder vespaUserOnHost(String vespaUserOnHost) {
            this.vespaUserOnHost = vespaUserOnHost;
            return this;
        }

        /** Sets the file system to use for paths. */
        public Builder fileSystem(FileSystem fileSystem) {
            this.fileSystem = fileSystem;
            return this;
        }

        public Builder cpuSpeedUp(double cpuSpeedUp) {
            this.cpuSpeedUp = cpuSpeedUp;
            return this;
        }

        public NodeAgentContextImpl build() {
            return new NodeAgentContextImpl(
                    nodeSpecBuilder.build(),
                    Optional.ofNullable(acl).orElse(Acl.EMPTY),
                    Optional.ofNullable(identity).orElseGet(() -> new AthenzService("domain", "service")),
                    Optional.ofNullable(dockerNetworking).orElse(DockerNetworking.HOST_NETWORK),
                    Optional.ofNullable(zone).orElseGet(() -> new ZoneApi() {
                        @Override
                        public SystemName getSystemName() {
                            return SystemName.defaultSystem();
                        }

                        @Override
                        public ZoneId getId() {
                            return ZoneId.defaultId();
                        }

                        @Override
                        public CloudName getCloudName() {
                            return CloudName.defaultName();
                        }

                        @Override
                        public String getCloudNativeRegionName() {
                            return getId().region().value();
                        }
                    }),
                    fileSystem,
                    fileSystem.getPath("/home/docker"),
                    fileSystem.getPath("/opt/vespa"),
                    Optional.ofNullable(vespaUser).orElse("vespa"),
                    Optional.ofNullable(vespaUserOnHost).orElse("container_vespa"),
                    cpuSpeedUp);
        }
    }
}
