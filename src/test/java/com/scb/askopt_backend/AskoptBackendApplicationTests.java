package com.scb.askopt_backend;

import com.scb.askopt_backend.config.Hmac.GoAgentClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class AskoptBackendApplicationTests {

	// 注入封装好的带HMAC签名客户端
	@Autowired
	private GoAgentClient goAgentClient;

	/**
	 * 获取容器列表 GET 请求
	 */
	@Test
	void getContainers() {
		String url = "http://localhost:8090/api/podman/containers";
		// 调用封装get方法，自动携带HMAC签名头
		String result = goAgentClient.get(url, String.class);

		// 校验返回不为空
		assertNotNull(result);
		System.out.println("容器列表响应：");
		System.out.println(result);
	}

	/**
	 * 重启容器 POST 请求
	 */
	@Test
	void restartContainer() {
		String containerName = "grafana";
		String url = "http://localhost:8090/api/podman/containers/" + containerName + "/restart";
		// post无请求体传null，内部会序列化为空字符串参与签名
		String result = goAgentClient.post(url, null, String.class);

		assertNotNull(result);
		System.out.println("重启容器响应：");
		System.out.println(result);
	}
}