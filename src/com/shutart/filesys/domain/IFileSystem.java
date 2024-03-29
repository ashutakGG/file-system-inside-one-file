package com.shutart.filesys.domain;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

public interface IFileSystem {

	void clear();

	boolean deleteFile(String fileName);

	boolean deleteFile(IFile file);

	char getSeparator();

	long length(IFile file);

	boolean isWritable(IFile file);
	boolean setWritable(IFile file, boolean isWritable);

	long lastModified(IFile file);
	boolean setLastModified(IFile file, long time);

	OutputStream getNewOutputStream(IFile file, boolean append) throws FileNotFoundException;

	InputStream getNewInputStream(IFile file, int startByteIndex) throws FileNotFoundException ;


	boolean exists(IFile file);

	boolean initFile(IFile file);

	boolean initFile(String fileName);

	byte[] getBytes(IFile file, int fromPosition, int length);

	boolean setBytes(IFile file, int fromPosition, byte[] bytes,
			int startByte, int length);

	
}
