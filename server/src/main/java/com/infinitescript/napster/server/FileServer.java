package com.infinitescript.napster.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.Socket;
import java.util.*;

/**
 * File Server stored the list of shared files.
 *  存储共享文件列表
 * @author Haozhe Xie
 */
public class FileServer {
	private FileServer() { }

	public static FileServer getInstance() {
		return INSTANCE;
	}

	/**
	 * Get shared files.
	 * @return a list contains shared files
	 * 返回一个包含共享文件的列表
	 */
	public List<SharedFile> getSharedFiles() {
		List<SharedFile> sharedFileList = new ArrayList<>();
       //每个Entry对应一个键值对，entrySet取出键值对，包含getValue与getKey方法
		for ( Map.Entry<String, Map<String, Object>> e : sharedFiles.entrySet() ) {
			sharedFileList.add((SharedFile) e.getValue().get("sharedFile"));
		}
		return sharedFileList;
	}

	/**
	 * Share a new file to Napster server.
	 * @param sharedFile the file to share
	 * @param socket     the socket of the sharer  共享者的socket
	 * @return whether the share operation is successful
	 */
	public boolean shareNewFile(SharedFile sharedFile, Socket socket) {
		String checksum = sharedFile.getChecksum();

		if ( sharedFiles.containsKey(checksum) ) {//已经有该文件，返回false
			return false;
		}

		String ipAddress = socket.getInetAddress().toString();//获得分享者的IP
        //Map<String, Map<String, Object>>形式
		Map<String, Object> meta = new HashMap<>();
		meta.put("ipAddress", ipAddress);
		meta.put("socket", socket);
		meta.put("sharedFile", sharedFile);
		sharedFiles.put(checksum, meta);

		LOGGER.info("File shared at " + socket + ", " + sharedFile);
		return true;
	}

	/**
	 * Unshare a file.
	 * @param checksum the checksum of the file
	 * @param socket   the socket of the sharer
	 * @return whether the unshare operation is successful
	 */
	public boolean unshareFile(String checksum, Socket socket) {
		if ( socket == null || !sharedFiles.containsKey(checksum) ) {
			return false;
		}

		Map<String, Object> sharedFileMeta = sharedFiles.get(checksum);//Map<String, Map<String, Object>>后半部分
		SharedFile sharedFile = (SharedFile) sharedFileMeta.get("sharedFile");//得到sharedFile
		Socket s = (Socket) sharedFileMeta.get("socket");

		if ( !socket.equals(s) ) {
			return false;
		}
		sharedFiles.remove(checksum);
		LOGGER.info("File unshared at " + socket + ", " + sharedFile);
		return true;
	}

	/**
	 * Unshare all files shared by the user when this user log out.
	 * 当该用户注销时，取消该用户共享的所有文件
	 * @param socket the socket of the sharer
	 * @return whether the unshare operation is successful
	 */
	public boolean unshareFiles(Socket socket) {
		if ( socket == null ) {
			return false;
		}
       //遍历
		Iterator<Map.Entry<String, Map<String, Object>>> itr = sharedFiles.entrySet().iterator();
		while ( itr.hasNext() ) {
			Map.Entry<String, Map<String, Object>> e = itr.next();
			Socket s = (Socket) e.getValue().get("socket");
			SharedFile sharedFile = (SharedFile) e.getValue().get("sharedFile");

			if ( socket.equals(s) ) {
				itr.remove();
				LOGGER.info("File unshared at " + socket + ", " + sharedFile);
			}
		}
		return true;
	}

	/**
	 * Get the IP of the sharer who share a specific file
	 * @param checksum the checksum of the file
	 * @return the IP of the sharer or ERROR if the file is not available
	 */
	public String getFileSharerIp(String checksum) {
		if ( !sharedFiles.containsKey(checksum) ) {
			return "ERROR";
		}

		String ipAddress = (String) sharedFiles.get(checksum).get("ipAddress");
		return ipAddress;
	}

	/**
	 * The list of shared files.
	 *
	 * The key stands for the checksum of a shared file.
	 * The value is a HashMap which stores the meta data of the share file.
	 */
	private static final Map<String, Map<String, Object>> sharedFiles = new Hashtable<>();

	/**
	 * The unique instance of File server.
	 */
	private static final FileServer INSTANCE = new FileServer();

	/**
	 * Logger.
	 */
	private static final Logger LOGGER = LogManager.getLogger(FileServer.class);
}
