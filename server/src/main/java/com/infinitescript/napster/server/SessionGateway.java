package com.infinitescript.napster.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SessionGateway extends Thread {
	public SessionGateway(Socket socket) {
		this.socket = socket;
		this.fileServer = FileServer.getInstance();
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Thread#run()
	 */
	public void run() {
		try {
			// Decorate the streams so we can send characters
			// and not just bytes.  Ensure output is flushed after every newline.
			//按行读取客户端数据（socket表示客户端与服务器的连接）
			BufferedReader in = new BufferedReader(
					new InputStreamReader(socket.getInputStream()));
			//把数据写到文件输出流里（一次写一行），参数true表示下一次接着上一次写，而不是覆盖
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			
			// Get messages from the client, line by line; return them capitalized
			while ( true ) {
				//用一个字符串来表示读到的每一行
				String command = in.readLine();
				LOGGER.debug("Received new message: " + command);
				
				// Check if the user has logged in
				//用户离开
				if ( command == null || command.equals("QUIT") ) {//比较对象是否一致
					// The user is willing to leave
					String nickName = users.get(socket);//<socket,string>
					LOGGER.info("User leaved " + nickName + ", Current Online Users: " + (users.size() - 1));
					//用户离开
					if ( !fileServer.unshareFiles(socket) ) {
						LOGGER.warn("Failed to unshare files shared by " + socket);
					}
					users.remove(socket);

					// Fix infinite loop after client exit  修复客户端退出后的无限循环
					break;
				} else if ( !users.containsKey(socket) ) {
					if ( !command.startsWith("CONNECT ") ) {
						//向客户发送响应
						out.println("[WARN] Socket is going to close.");
						closeSocket();
					} else {
						String nickName = command.substring(7);
						
						out.println("ACCEPT");
						//新用户加入
						users.put(socket, nickName.trim());
						LOGGER.info("New user joined " + nickName + ", Current Online Users: " + users.size());
					}
				} else {
					// Invoke FileServer for other request  对其他请求调用FileServer
					if ( command.startsWith("ADD ") ) {
						//将Json字符串转化为相应的对象
						SharedFile sharedFile = JSON.parseObject(command.substring(4), SharedFile.class);
						//是否分享新文件
						boolean isFileShared = fileServer.shareNewFile(sharedFile, socket);

						if ( isFileShared ) {
							out.println("OK");
						} else {
							out.println("ERROR");
						}
					} else if ( command.startsWith("DELETE ") ) {
						Map<String, String> sharedFile = JSON.parseObject(command.substring(7), HashMap.class);
						String checksum = sharedFile.get("checksum");
						boolean isFileUnshared = fileServer.unshareFile(checksum, socket);

						if ( isFileUnshared ) {
							out.println("OK");
						} else {
							out.println("ERROR");
						}
					} else if ( command.equals("LIST") ) {
						List<SharedFile> sharedFiles = fileServer.getSharedFiles();
						//将实体对象转化为Json字符串
						out.println(JSON.toJSONString(sharedFiles));
					} else if ( command.startsWith("REQUEST ") ) {
						String checksum = command.substring(8);
						String sharerIp = fileServer.getFileSharerIp(checksum);
						out.println(sharerIp);
					}
				}
			}
		} catch (IOException e) {
			LOGGER.catching(e);
		} finally {
			closeSocket();
		}
	}

	/**
	 * Close socket.
	 */
	private void closeSocket() {
		if ( socket == null ) {
			return;
		}
		
		try {
			socket.close();
			LOGGER.info("Socket has closed for " + socket);
		} catch ( IOException e ) {
			LOGGER.catching(e);
		}
	}
	
	/**
	 * The socket between the server and client.
	 * 服务器和客户端之间的套接字
	 */
	private Socket socket;

	/**
	 * The file server for sharing files.
	 * 用于共享文件的文件服务器
	 */
	private FileServer fileServer;
	
	/**
	 * The map used for storing the nick name for online users.
	 */
	private static Map<Socket, String> users = new Hashtable<Socket, String>();
	
	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger(SessionGateway.class);
}
