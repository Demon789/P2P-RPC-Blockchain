package com.infinitescript.napster.client;

import java.io.Serializable;

/**
 * The class is designed for storing information for shared files.
 *  存储共享文件的信息
 * @author Haozhe Xie
 */
public class SharedFile implements Serializable {
	/** 实现序列化接口
	 * Default constructor for FastJSON.
	 */
	public SharedFile() { }

	/**
	 * The constructor of this class.
	 * 单个文件
	 * @param fileName  the file name of the file
	 * @param sharer    the nick name who share the file
	 * @param checksum  the checksum of the file
	 * @param size      the size in Byte of the file
	 */
	public SharedFile(String fileName, String sharer, String checksum, long size) {
		this.fileName = fileName;
		this.sharer = sharer;
		this.checksum = checksum;
		this.size = size;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public String getSharer() {
		return sharer;
	}

	public void setSharer(String sharer) {
		this.sharer = sharer;
	}

	public String getChecksum() {
		return checksum;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("SharedFile [FileName=%s, Sharer=%s, Checksum=%s, Size=%d Byte]", //设置显示格式
				new Object[] { fileName, sharer, checksum, size });
	}

	/**
	 * The name of the file.
	 */
	private String fileName;

	/**
	 * The nick name who share the file.
	 */
	private String sharer;

	/**
	 * The checksum of the file.
	 */
	private String checksum;

	/**
	 * The size in Byte of the file.
	 */
	private long size;
	
	/**
	 * The unique ID for serializing.
	 * 序列化的时候，被序列化的类要有一个唯一标记，判定为同一个对象（哈希得到）
	 */
	private static final long serialVersionUID = -4459827249944645125L;
}