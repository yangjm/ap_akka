package com.aggrepoint.utils.akka;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import com.aggrepoint.utils.ThreadContext;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;

@Configuration
@ComponentScan({ "com.aggrepoint.utils.akka" })
public class AkkaConfig {
	private static final String THREAD_CONTEXT_DYNAMIC_CONFIG_KEY = AkkaConfig.class + ".dynamic.config";

	@Value("${akka.system.name}")
	private String akkaSystemName;
	@Value("${akka.conf}")
	private String conf;

	/**
	 * 从命令行参数中提取akka配置
	 */
	public static void processArgs(String[] args) {
		StringBuffer sbCfg = new StringBuffer();

		for (String str : args) {
			if (str.startsWith("-port")) {
				sbCfg.append("akka.remote.netty.tcp.port=" + str.substring(5) + "\n");
			} else if (str.startsWith("-host")) {
				sbCfg.append("akka.remote.netty.tcp.hostname=" + str.substring(5) + "\n");
			}
		}

		if (sbCfg.length() > 0) // 用命令行参数值覆盖配置文件参数值
			ThreadContext.setAttribute(THREAD_CONTEXT_DYNAMIC_CONFIG_KEY, sbCfg.toString());
	}

	@Bean
	ActorSystem actorSystem() {
		Config config = ConfigFactory.parseString(conf.replaceAll(";", "\n"))
				.withFallback(ConfigFactory.load("application"));

		String cfg = (String) ThreadContext.getAttribute(THREAD_CONTEXT_DYNAMIC_CONFIG_KEY);

		return ActorSystem.create(akkaSystemName,
				cfg == null ? config : ConfigFactory.parseString(cfg).withFallback(config));
	}
}
