package com.infinitescript.napster.client;

import java.io.*;
import java.net.*;
import java.util.Hashtable;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * File server for sending shared files to others.
 * 文件服务器，用于将共享文件发送给他人
 * @author Haozhe Xie
 */
public class FileServer {
	private FileServer() { }

	public static FileServer getInstance() {
		return INSTANCE;
	}

	/**
	 * Receive the message from client.
	 * 接收来自客户端的消息
	 * @throws IOException
	 */
	public void accept() throws IOException {
		//commandListenerTask线程，run（）
		Runnable commandListenerTask = () -> {
			DatagramSocket commandSocket = null;
			try {
				//构造数据报套接字并将其绑定到本地主机上的指定端口
				commandSocket = new DatagramSocket(COMMAND_PORT);
				byte[] inputDataBuffer = new byte[BUFFER_SIZE];
				byte[] outputDataBuffer = new byte[BUFFER_SIZE];

				while ( true ) {
					//构造一个packet接收长度为.length的数据包
					DatagramPacket inputPacket = new DatagramPacket(inputDataBuffer, inputDataBuffer.length);
					//从该socket接收数据报；此方法会阻塞，直到接收到数据报
					commandSocket.receive(inputPacket);
					//getData返回Datagram Packet对象里封装的字节数组
					String command = new String(inputPacket.getData());
					LOGGER.debug("Received new message: " + command);

					if ( command.startsWith("GET ") ) {
						//substring()截取字符串4到36
						String checksum = command.substring(4, 36);
						String ipAddress = inputPacket.getAddress().toString().substring(1);
						int port = inputPacket.getPort();

						if ( sharedFiles.containsKey(checksum) ) {//映射是否包含指定的映射键
							outputDataBuffer = "ACCEPT".getBytes();//getBytes（）得到字节数组
							sendDatagramPacket(commandSocket, outputDataBuffer, ipAddress, port);//发送数据包（指定端口）
							Thread.sleep(1000); // Wait for open socket for receiving files 等待打开套接字接收文件
							sendFileStream(checksum, ipAddress);
						} else {
							outputDataBuffer = "ERROR".getBytes();
							sendDatagramPacket(commandSocket, outputDataBuffer, ipAddress, port);
						}
					}
				}
			} catch ( Exception ex ) {
				LOGGER.catching(ex);
			} finally {
				if ( commandSocket != null ) {
					commandSocket.close();
				}
			}
		};
		Thread commandListenerThread = new Thread(commandListenerTask);
		//守护线程一般用来进行一些后台任务，程序退出时线程结束
		commandListenerThread.setDaemon(true);
		commandListenerThread.start();
	}
    /*
    * 发送数据包
    * */
	private void sendDatagramPacket(DatagramSocket socket, byte[] outputDataBuffer, String ipAddress, int port)
			throws IOException {
		InetAddress inetAddress = InetAddress.getByName(ipAddress);//得到主机地址

		DatagramPacket outputPacket = new DatagramPacket(outputDataBuffer, outputDataBuffer.length, inetAddress, port);//将发送的信息打包
		socket.send(outputPacket);//发送数据包
	}

	/**
	 * Stop receiving file stream.
	 * 停止接收文件流
	 */
	public void close() {
		closeSocket(commandListener);
	}

	/**
	 * Send file stream to the receiver.
	 * 发送文件流到接收者
	 * @param checksum  the checksum of the file
	 * @param ipAddress the IP address of the receiver
	 */
	private void sendFileStream(String checksum, String ipAddress) {
		String filePath = sharedFiles.get(checksum);//获得checksum对应的值——文件绝对路径
		Socket socket = null;
		//读取
		DataInputStream fileInputStream = null;
		//写入
		DataOutputStream fileOutputStream = null;

		try {
			//创建流套接字并将其连接到指定主机上的指定端口号
			socket = new Socket(ipAddress, FILE_STREAM_PORT);
			//上载文件，读取
			fileInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(filePath)));
			//获取文件，写入到输入流中
			fileOutputStream = new DataOutputStream(socket.getOutputStream());
            //读取字节
			byte[] fileBuffer = new byte[BUFFER_SIZE];
			while ( true ) {
				if ( fileInputStream == null ) {
					return;
				}
				//read()此方法阻塞，直到输入数据可用、检测到文件结束或抛出异常。
				int bytesRead = fileInputStream.read(fileBuffer);

				if ( bytesRead == -1 ) {
					break;
				}
				fileOutputStream.write(fileBuffer, 0, bytesRead);//从0到bytesRead写入数据
			}
			//刷新此数据输出流。这将强制将任何缓冲的输出字节写入流。
			fileOutputStream.flush();
		} catch ( IOException ex ) {
			LOGGER.catching(ex);
		} finally {
			// Close Socket and DataStream  关闭套接字和数据流
			try {
				if ( fileInputStream != null ) {
					fileInputStream.close();
				}
				if ( fileOutputStream != null ) {
					fileOutputStream.close();
				}
				if ( socket != null ) {
					socket.close();
				}
			} catch ( IOException ex ) {
				LOGGER.catching(ex);
			}
		}
	}

	/**
	 * Register new file to the file server for sharing.
	 *将新文件注册到文件服务器以便共享
	 * @param checksum the checksum of the file
	 * @param filePath the absolute path of the file
	 */
	//put()增加新的文件映射，如果该映射先前包含了该键的映射，则旧值将被指定的值替换
	public void shareNewFile(String checksum, String filePath) {//添加新的共享文件，储存键值对文件校验和，文件绝对路径
		sharedFiles.put(checksum, filePath);
	}

	/**
	 * Remove a shared file from the file server because it is no longer shared.
	 * 从文件服务器删除共享文件，因为它不再是共享的。
	 * @param checksum the checksum of the file
	 */
	//remove()移除
	public void unshareFile(String checksum) {//找到该文件校验和，移除
		sharedFiles.remove(checksum);
	}

	/**
	 * Check if the shared file requested is available.
	 * 检查请求的共享文件是否可用
	 * @param checksum - the checksum of the file
	 * @return whether the shared file is available
	 */
	public boolean contains(String checksum) {//对应该文件校验和
		return sharedFiles.containsKey(checksum);//sharedFiles是否有checksum对应的文件绝对路径，boolean
	}

	/**
	 * Close socket for the server.
	 * 关闭服务器的socket
	 * @param socket the server socket to close
	 */
	private void closeSocket(ServerSocket socket) {
		try {
			if ( socket != null ) {
				socket.close();
			}
		} catch ( IOException ex ) {
			LOGGER.catching(ex);
		}
	}

	/**
	 * The map is used for storing shared files.
	 *
	 * The key stands for the checksum of the file.
	 * The value stands for the absolute path of the file.
	 * 创建哈希表，存储键值对（文件校验和，文件绝对路径）
	 */
	private static Map<String, String>  sharedFiles = new Hashtable<>();

	/**
	 * The server socket used for receiving commands.
	 * 用于接收命令的服务器套接字
	 */
	private ServerSocket commandListener;

	/**
	 * The port used for receiving commands.
	 * 接收命令的端口
	 */
	private static final int COMMAND_PORT = 7701;

	/**
	 * The port used for sending file stream.
	 * 用于发送文件流的端口
	 */
	private static final int FILE_STREAM_PORT = 7702;

	/**
	 * The buffer size of the file stream.
	 * 文件流的缓冲区大小
	 */
	private static final int BUFFER_SIZE = 4096;

	/**
	 * The unique instance of Napster client.
	 * Napster客户端的独特实例
	 */
	public static final FileServer INSTANCE = new FileServer();

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger(FileServer.class);
}
