package com.shutart.onefilefs.test.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

import com.shutart.filesys.domain.FSConstans;
import com.shutart.filesys.domain.IFile;
import com.shutart.filesys.domain.IFileSystem;

public final class MemoryFileSystem implements IFileSystem {
	
	private final FileAttrs getDefaultAttrs(){return new FileAttrs(false, FSConstans.START_LAST_MODIF_VAL);}
	
	private final Map<String, IFile> fFiles = new HashMap<String, IFile>();
	private final Map<IFile, List<Byte>> fBytesOfFiles= new HashMap<IFile, List<Byte>>(); 
	private final Map<IFile, FileAttrs> fAttrsOfFiles= new HashMap<IFile, FileAttrs>(); 

	private final static class FileAttrs{

		private boolean isReadOnly;
		private long lastModified;
		
		FileAttrs(boolean isReadOnly, long lastModified) {
			this.isReadOnly = isReadOnly;
			this.lastModified = lastModified;
		}

		boolean isReadOnly() {
			return isReadOnly;
		}

		void setReadOnly(boolean isReadOnly) {
			this.isReadOnly = isReadOnly;
		}

		long getLastModified() {
			return lastModified;
		}

		void setLastModified(long time) {
			this.lastModified = time;
		}
		
	}
//	private final FSPage[] pages = new FSPage[count];
//	private final Index index;
//	private final boolean[] freePages = new boolean[count];

	@Override
	public void clear() {
		fFiles.clear();
		fAttrsOfFiles.clear();
		fBytesOfFiles.clear();
	}

	@Override
	public boolean deleteFile(String fileName) {
		return deleteFile(fFiles.get(fileName));
	}

	@Override
	public boolean deleteFile(IFile file) {
		IFile test = fFiles.remove(file.getName());
		if (test == null)
			return false;
		fBytesOfFiles.remove(file);
		fAttrsOfFiles.remove(file);
		return true;
	}

	@Override
	public char getSeparator() {
		return FSConstans.SEPARATOR;
	}

	@Override
	public long length(IFile file) {
		if (!exists(file))
			return 0;
		return bytesOf(file).size();
	}

	@Override
	public boolean isWritable(IFile file) {
		if (!exists(file))
			return false;
		return !attrsOf(file).isReadOnly();
	}


	@Override
	public boolean setWritable(IFile file, boolean isWritable) {
		if (!exists(file))
			return false;
		attrsOf(file).setReadOnly(!isWritable);
//		attrsOf(file).setLastModified(System.currentTimeMillis());
		return true;
	}

	@Override
	public long lastModified(IFile file) {
		if(!exists(file))
			return FSConstans.START_LAST_MODIF_VAL;
		return attrsOf(file).getLastModified();
	}

	@Override
	public boolean setLastModified(IFile file, long time) {
		if(!exists(file))
			return false;
		attrsOf(file).setLastModified(time);
		return true;
	}

	@Override
	public OutputStream getNewOutputStream(final IFile file, boolean append) throws FileNotFoundException {
		if (!exists(file))
			initFile(file);
		if (!isWritable(file))
			throw new FileNotFoundException();
		if (append) {
			return new OutputStream() {

				@Override
				public void write(int b) throws IOException {
					bytesOf(file).add((byte) b);
					attrsOf(file).setLastModified(System.currentTimeMillis());
				}
			};
		} else {
			return new OutputStream() {
				private int index;
				private boolean firstOperation = true;

				@Override
				public void write(int b) throws IOException {
					if (firstOperation){
						firstOperation = false;
						bytesOf(file).clear();
					}
					if (bytesOf(file).size()<index)
						bytesOf(file).set(index, (byte) b);
					else
						bytesOf(file).add((byte)b);
					index++;
					attrsOf(file).setLastModified(System.currentTimeMillis());
				}
			};
		}
	}

	@Override
	public InputStream getNewInputStream(IFile file, int startByteIndex) throws FileNotFoundException {
		if (!exists(file))
			throw new FileNotFoundException();
		return new MyInputStream(startByteIndex,bytesOf(file));
	}
	
	private List<Byte> bytesOf(IFile file){
		List<Byte> rez = fBytesOfFiles.get(file);
		if (rez == null){
//			initFile(file);
//			rez = fBytesOfFiles.get(file);
			throw new IllegalStateException();
		}
		return rez;
	}
	private FileAttrs attrsOf(IFile file) {
		FileAttrs rez = fAttrsOfFiles.get(file);
		if (rez == null){
//			initFile(file);
//			rez = fAttrsOfFiles.get(file);
			throw new IllegalStateException();
		}
		return rez;
	}
	
	@Override
	public boolean initFile(IFile file) {
		if (exists(file))
			return false;
		fFiles.put(file.getName(), file);
		fBytesOfFiles.put(file, new ArrayList<Byte>());
		FileAttrs attrs =  getDefaultAttrs();
		fAttrsOfFiles.put(file,attrs );
		attrs.setLastModified(System.currentTimeMillis());
		return true;
	}

	@Override
	public boolean initFile(String fileName) {
		throw new UnsupportedOperationException();
	}
	private static final class MyInputStream extends InputStream{
		
		private int index;
		private final List<Byte> bytes;

		MyInputStream(long startByteIndex, List<Byte> bytes){
			index = (int) startByteIndex;
			this.bytes = bytes;
		}

		@Override
		public int read() throws IOException {
			int rez = -1;
			if (index < bytes.size())
				rez = bytes.get(index++);
			return rez ;
		}
		
	}

	@Override
	public boolean exists(IFile file) {
//		return fBytesOfFiles.get(file)!=null;
		return fFiles.values().contains(file);
	}

	private static final byte[] NULL_BYTE_ARRAY = {};
	@Override
	public byte[] getBytes(IFile file, int fromPosition, int length) {
		if (! exists(file) || fromPosition > file.length())
			return NULL_BYTE_ARRAY;
		List<Byte> bytes = bytesOf(file);
		byte[] rez = new byte[Math.min(length, (int) file.length() - fromPosition)];
		for (int i = 0, j = fromPosition; i < rez.length; i++, j++) {
			rez[i] = (byte) bytes.get(j);
		}
		return rez;
	}

	@Override
	public boolean setBytes(IFile file, int fromPosition, byte[] bytes,
			int startByte, int length) {
		if (! file.exists())
			initFile(file);
		if (fromPosition > file.length())
			return false;
		length = Math.min(length, bytes.length - startByte);
		if (fromPosition < 0 || startByte < 0 || length < 0
				|| startByte + length > bytes.length)
			throw new IndexOutOfBoundsException("startWritePosition="
					+ fromPosition + " from=" + startByte + " length="
					+ length);
		List<Byte> listBytes = bytesOf(file);
		for (int writeIndex = fromPosition, i = startByte; i < startByte + length; writeIndex++, i++) {
			if (writeIndex < listBytes.size())
				listBytes.set(writeIndex, bytes[i]);
			else
				listBytes.add(bytes[i]);
		}
		return true;
	}



}
