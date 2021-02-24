package com.infinitescript.napster.server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * The socket listener for server.
 * 服务器的套接字监听器
 * 
 * @author Haozhe Xie
 */
public class SocketListener {
	/**
	 * The private constructor for singleton pattern.
	 */
	protected SocketListener() { }
	
	public static SocketListener getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Receive the message from client.
	 * 从客户端接收消息
	 * @throws IOException
	 */
	public void accept() throws IOException {
		//ServerSocket等待客户端的请求，一旦获得一个连接请求，就创建一个Socket示例来与客户端进行通信。
		ServerSocket listener = new ServerSocket(PORT);//监听端口，针对这个端口创建Socket
		try {
			// Listen to incoming sockets
			while ( true ) {
				//accept（），返回一个Socket，有客户端发送连接，建立连接；拿到Socket后就可以读客户端发送来的数据
				new SessionGateway(listener.accept()).start();
			}
		} finally {
			listener.close();
		}
	}

	/**
	 * The port used for receiving commands from clients.
	 */
	private static final int PORT = 7777;
	
	/**
	 * The unique server instance.
	 */
	private static final SocketListener INSTANCE = new SocketListener();
}
