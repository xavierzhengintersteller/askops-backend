package com.scb.tb.askopt_backend.dto.podman;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
public class ContainerDetailDTO {
    @JsonProperty("Id")
    private String id;

    @JsonProperty("Created")
    private LocalDateTime created;

    @JsonProperty("Path")
    private String path;

    @JsonProperty("Args")
    private List<String> args;

    @JsonProperty("State")
    private ContainerState state;

    @JsonProperty("Image")
    private String image;

    @JsonProperty("ResolvConfPath")
    private String resolvConfPath;

    @JsonProperty("HostnamePath")
    private String hostnamePath;

    @JsonProperty("HostsPath")
    private String hostsPath;

    @JsonProperty("LogPath")
    private String logPath;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("RestartCount")
    private Integer restartCount;

    @JsonProperty("Driver")
    private String driver;

    @JsonProperty("Platform")
    private String platform;

    @JsonProperty("MountLabel")
    private String mountLabel;

    @JsonProperty("ProcessLabel")
    private String processLabel;

    @JsonProperty("AppArmorProfile")
    private String appArmorProfile;

    @JsonProperty("ExecIDs")
    private List<String> execIDs;

    @JsonProperty("HostConfig")
    private ContainerHostConfig hostConfig;

    @JsonProperty("GraphDriver")
    private ContainerGraphDriver graphDriver;

    @JsonProperty("SizeRootFs")
    private Long sizeRootFs;

    @JsonProperty("Mounts")
    private List<ContainerMount> mounts;

    @JsonProperty("Config")
    private ContainerConfig config;

    @JsonProperty("NetworkSettings")
    private ContainerNetworkSettings networkSettings;

    // 内部嵌套类：State 容器运行状态
    @Data
    public static class ContainerState {
        @JsonProperty("Status")
        private String status;
        @JsonProperty("Running")
        private Boolean running;
        @JsonProperty("Paused")
        private Boolean paused;
        @JsonProperty("Restarting")
        private Boolean restarting;
        @JsonProperty("OOMKilled")
        private Boolean oomKilled;
        @JsonProperty("Dead")
        private Boolean dead;
        @JsonProperty("Pid")
        private Integer pid;
        @JsonProperty("ExitCode")
        private Integer exitCode;
        @JsonProperty("Error")
        private String error;
        @JsonProperty("StartedAt")
        private LocalDateTime startedAt;
        @JsonProperty("FinishedAt")
        private LocalDateTime finishedAt;
    }

    // 内部嵌套类：挂载信息
    @Data
    public static class ContainerMount {
        @JsonProperty("Type")
        private String type;
        @JsonProperty("Source")
        private String source;
        @JsonProperty("Destination")
        private String destination;
        @JsonProperty("Mode")
        private String mode;
        @JsonProperty("RW")
        private Boolean rw;
        @JsonProperty("Propagation")
        private String propagation;
    }

    // 内部嵌套类：HostConfig 主机配置
    @Data
    public static class ContainerHostConfig {
        @JsonProperty("Binds")
        private List<String> binds;
        @JsonProperty("ContainerIDFile")
        private String containerIDFile;
        @JsonProperty("LogConfig")
        private LogConfig logConfig;
        @JsonProperty("NetworkMode")
        private String networkMode;
        @JsonProperty("PortBindings")
        private Map<String, List<PortBind>> portBindings;
        @JsonProperty("RestartPolicy")
        private RestartPolicy restartPolicy;
        @JsonProperty("AutoRemove")
        private Boolean autoRemove;
        @JsonProperty("VolumeDriver")
        private String volumeDriver;
        @JsonProperty("CapAdd")
        private List<String> capAdd;
        @JsonProperty("CapDrop")
        private List<String> capDrop;
        @JsonProperty("IpcMode")
        private String ipcMode;
        @JsonProperty("Privileged")
        private Boolean privileged;
        @JsonProperty("PublishAllPorts")
        private Boolean publishAllPorts;
        @JsonProperty("ReadonlyRootfs")
        private Boolean readonlyRootfs;
        @JsonProperty("ShmSize")
        private Long shmSize;
        @JsonProperty("Runtime")
        private String runtime;
        @JsonProperty("ConsoleSize")
        private List<Integer> consoleSize;
        @JsonProperty("Memory")
        private Long memory;
        @JsonProperty("NanoCpus")
        private Long nanoCpus;
        @JsonProperty("OomScoreAdj")
        private Integer oomScoreAdj;
        @JsonProperty("PidMode")
        private String pidMode;
        @JsonProperty("PidsLimit")
        private Integer pidsLimit;
    }

    // 端口绑定子内部类
    @Data
    public static class PortBind {
        @JsonProperty("HostIp")
        private String hostIp;
        @JsonProperty("HostPort")
        private String hostPort;
    }

    // 日志配置
    @Data
    public static class LogConfig {
        @JsonProperty("Type")
        private String type;
        @JsonProperty("Config")
        private Map<String, Object> config;
    }

    // 重启策略
    @Data
    public static class RestartPolicy {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("MaximumRetryCount")
        private Integer maximumRetryCount;
    }

    // 存储驱动信息
    @Data
    public static class ContainerGraphDriver {
        @JsonProperty("Name")
        private String name;
        @JsonProperty("Data")
        private Map<String, String> data;
    }

    // 容器基础配置 Config
    @Data
    public static class ContainerConfig {
        @JsonProperty("Hostname")
        private String hostname;
        @JsonProperty("Domainname")
        private String domainname;
        @JsonProperty("User")
        private String user;
        @JsonProperty("AttachStdin")
        private Boolean attachStdin;
        @JsonProperty("AttachStdout")
        private Boolean attachStdout;
        @JsonProperty("AttachStderr")
        private Boolean attachStderr;
        @JsonProperty("ExposedPorts")
        private Map<String, Object> exposedPorts;
        @JsonProperty("Tty")
        private Boolean tty;
        @JsonProperty("OpenStdin")
        private Boolean openStdin;
        @JsonProperty("StdinOnce")
        private Boolean stdinOnce;
        @JsonProperty("Env")
        private List<String> env;
        @JsonProperty("Cmd")
        private List<String> cmd;
        @JsonProperty("Image")
        private String image;
        @JsonProperty("Volumes")
        private Map<String, Object> volumes;
        @JsonProperty("WorkingDir")
        private String workingDir;
        @JsonProperty("Entrypoint")
        private List<String> entrypoint;
        @JsonProperty("Labels")
        private Map<String, String> labels;
        @JsonProperty("StopSignal")
        private String stopSignal;
        @JsonProperty("StopTimeout")
        private Integer stopTimeout;
    }

    // 网络总配置
    @Data
    public static class ContainerNetworkSettings {
        @JsonProperty("Bridge")
        private String bridge;
        @JsonProperty("SandboxID")
        private String sandboxID;
        @JsonProperty("HairpinMode")
        private Boolean hairpinMode;
        @JsonProperty("Ports")
        private Map<String, List<PortBind>> ports;
        @JsonProperty("SandboxKey")
        private String sandboxKey;
        @JsonProperty("Gateway")
        private String gateway;
        @JsonProperty("IPAddress")
        private String ipAddress;
        @JsonProperty("MacAddress")
        private String macAddress;
        @JsonProperty("Networks")
        private Map<String, NetworkDetail> networks;
    }

    // 单网络详情
    @Data
    public static class NetworkDetail {
        @JsonProperty("NetworkID")
        private String networkID;
        @JsonProperty("Gateway")
        private String gateway;
        @JsonProperty("IPAddress")
        private String ipAddress;
        @JsonProperty("IPPrefixLen")
        private Integer ipPrefixLen;
        @JsonProperty("MacAddress")
        private String macAddress;
    }
}