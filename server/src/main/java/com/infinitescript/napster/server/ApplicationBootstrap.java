package com.infinitescript.napster.server;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The entrance of the application.
 * 
 * @author Haozhe Xie
 */
public class ApplicationBootstrap {
	public static void main(String[] args) {
		//服务器的套接字监听器
		SocketListener listener = SocketListener.getInstance();//创建唯一对象（单例）
		
		try {
			LOGGER.info("Server is running...");
			listener.accept();//创建好服务器套接字等待客户请求
		} catch (IOException e) {
			LOGGER.catching(e);
		}
	}
	
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger(ApplicationBootstrap.class);
}
