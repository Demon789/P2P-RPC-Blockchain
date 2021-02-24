package com.infinitescript.napster.client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import com.alibaba.fastjson.JSON;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Client {
	private Client() { }
	
	public static Client getInstance() {
		return INSTANCE;
	}
	
	/**
	 * Connect to server.
	 * 连接到服务器
	 * @param ipAddress the IP address of the server.
	 * @param nickName  the nick name of the user
	 * @throws Exception 
	 */
	public void connect(String ipAddress, String nickName) 
			throws Exception {
		this.ipAddress = ipAddress;
		this.nickName = nickName;
		//创建流套接字并将其连接到指定主机上的指定端口号。
		this.socket = new Socket(ipAddress, PORT);
		this.inputStreamReader = new BufferedReader(
                new InputStreamReader(socket.getInputStream()));
		this.outputStreamWriter = new PrintWriter(socket.getOutputStream(), true);
		
		// Say hello to server
		outputStreamWriter.println("CONNECT " + nickName);
		
		// Receive ACK from server
		//读取服务端响应
		//按行读取
		String ackMessage = inputStreamReader.readLine();
		if ( ackMessage.equals("ACCEPT") ) {
			LOGGER.info("Connected to server.");
		} else {
			LOGGER.warn("Server closed socket for unknown reason.");
			throw new Exception("Server closed socket for unknown reason.");
		}
	}

	/**
	 * Get the list of shared files right now from Napster server.
	 * 马上从Napster服务器获取共享文件列表。
	 * @return the list of shared files
	 */
	public List<SharedFile> getSharedFiles() {
		// Send LIST command to server for querying shared files
		//发送LIST命令到服务器查询共享文件
		outputStreamWriter.println("LIST");

		// Receive response from server
		List<SharedFile> sharedFiles = new ArrayList<>();
		try {
			//按行读取
			String response = inputStreamReader.readLine();

			if ( !response.equals("ERROR") ) {
				//将json数据的格式转化为数组格式
				sharedFiles = JSON.parseArray(response, SharedFile.class);
			}
		} catch ( IOException ex ) {
			LOGGER.catching(ex);
		}
		return sharedFiles;
	}

	/**
	 * Share a new file to Napster server.
	 * 共享一个新文件到Napster服务器
	 * @param sharedFile the file to share
	 * @return whethe r the share operation is successful
	 */
	public boolean shareNewFile(SharedFile sharedFile) {
		// Send share command to server
		//发送共享命令到服务器
		//toJSON类似toString,转换为JSON格式
		outputStreamWriter.println("ADD " + JSON.toJSON(sharedFile));

		// Receive response from server
		try {
			String response = inputStreamReader.readLine();
			LOGGER.debug("[ShareFile] Received message from server: " + response);

			if ( response.equals("OK") ) {
				return true;
			}
		} catch ( IOException ex ) {
			LOGGER.catching(ex);
		}
		return false;
	}

	/**
	 * Unshare a file from Napster server.
	 * @param fileName the file name of the file
	 * @param checksum the checksum of the file
	 * @return whether the unshare operation is successful
	 */
	public boolean unshareFile(String fileName, String checksum) {
		// Send unshare command to server
		outputStreamWriter.println("DELETE " + JSON.toJSON(((Supplier<Map<String, String>>)(() -> {
			Map<String, String> map = new HashMap();
			map.put("fileName", fileName);
			map.put("checksum", checksum);
			//Collections.unmodifiableMap()方法会返回一个“只读”的map，如果向转化后的map中put数据会报throw new UnsupportedOperationException()错误
			//防止更改
			return Collections.unmodifiableMap(map);
		})).get()));

		// Receive response from server
		try {
			String response = inputStreamReader.readLine();

			if ( response.equals("OK") ) {
				return true;
			}
		} catch ( IOException ex ) {
			LOGGER.catching(ex);
		}
		return false;
	}

	/**
	 * Get the IP of the sharer who share a specific file
	 * 获取共享特定文件的共享者的IP
	 * @param checksum the checksum of the file
	 * @return the IP of the sharer or N/a if the file is not available
	 */
	public String getFileSharerIp(String checksum) {
		// Send share command to server
		outputStreamWriter.println("REQUEST " + checksum);

		// Receive response from server
		try {
			String response = inputStreamReader.readLine();

			if ( !response.equals("ERROR") ) {
				return response;
			}
		} catch ( IOException ex ) {
			LOGGER.catching(ex);
		}
		return "N/a";
	}
	
	/**
	 * Close socket for client.
	 * 关闭客户端套接字
	 */
	public void disconnect() {
		// Say goodbye to Napster server
		outputStreamWriter.println("QUIT");
		
		// CLose Socket
		try {
			inputStreamReader.close();
			outputStreamWriter.close();
			socket.close();
			
			LOGGER.info("Disconnected from server.");
		} catch (IOException ex) {
			LOGGER.catching(ex);
		}
	}
	
	/**
	 * The ip address of the server.
	 */
	private String ipAddress;
	
	/**
	 * The nick name of the user.
	 */
	private String nickName;
	
	/**
	 * The socket used for communicating with server.
	 */
	private Socket socket;
	
	/**
	 * The reader used for reading input stream. 
	 */
	private BufferedReader inputStreamReader;
	
	/**
	 * The writer used for writing output stream.
	 */
	private PrintWriter outputStreamWriter;
	
	/**
	 * The port of Napster server.
	 */
	private static final int PORT = 7777;
	
	/**
	 * The unique instance of Napster client.
	 */
	public static final Client INSTANCE = new Client();

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger(Client.class);
}
