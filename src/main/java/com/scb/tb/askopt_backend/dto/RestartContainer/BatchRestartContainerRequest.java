    package com.scb.tb.askopt_backend.dto.RestartContainer;

    import lombok.Data;

    import java.util.List;

    @Data
    public class BatchRestartContainerRequest {
        private List<ContainerRestartItem> containerItems;

    }