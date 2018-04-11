package com.cs.divideadtest;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class DL1Arhive {

	public class DL1ArhiveException extends Exception {
		public DL1ArhiveException() {
			super();
		}

		public DL1ArhiveException(String detailMessage) {
			super(detailMessage);
		}

		private static final long serialVersionUID = -7364418566739567674L;
	}

	class DL1ArhiveHeader {
		byte m_bSign[] = new byte[6];
		short m_sReserved1;
		short m_sEntryCount;
		int m_iFileTableOffset;
		short m_sReserved2;
	}

	class DL1ArhiveEntry {
		String m_sFileName;
		int m_iFileSize;
		int m_iFileOffset;
	}

	class PackedFileHeader {
		byte[] m_baSign = new byte[2];
		int m_iPackedSize;
		int m_iUnpackedSize;
	}

	private boolean m_bHeaderOk = false;
	private boolean m_bFileTableOk = false;
	private int m_iFileTableSize;
	private RandomAccessFile m_rafArhiveFile = null;

	DL1ArhiveHeader m_dahArhiveHeader = null;
	List<DL1ArhiveEntry> m_lArhiveEntries = null;

	private ByteBuffer m_baBuffer;
	private PackedFileHeader m_pfhFileHeader;

	public DL1Arhive(File fFile) {
		try {
			m_rafArhiveFile = new RandomAccessFile(fFile, "r");

			LoadArhiveHeader();
			LoadArhiveFileTable();
		} catch (IOException eE) {
			Dispose();
			throw new RuntimeException("Error loading arhive: "
					+ fFile.getAbsolutePath() + "\n" + eE.getMessage());
		} catch (DL1ArhiveException eE) {
			Dispose();
			throw new RuntimeException("Error loading arhive: "
					+ fFile.getAbsolutePath() + "\n" + eE.getMessage());
		}
	}

	public DL1ArhiveHeader GetArhiveHeader() throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		return m_dahArhiveHeader;
	}

	public List<DL1ArhiveEntry> GetArhiveEntries() throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		return m_lArhiveEntries;
	}

	public DL1ArhiveEntry GetArhiveEntry(int iIndex) throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		return m_lArhiveEntries.get(iIndex);
	}

	public byte[] LoadFile(String sFileName) throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		int iIdx;
		DL1ArhiveEntry daeEntry;
		byte[] baBuffer;

		iIdx = FindFileIndex(sFileName);
		if (iIdx < 0) {
			throw new DL1ArhiveException("File " + sFileName + "not found\n");
		}

		daeEntry = GetArhiveEntry(iIdx);
		baBuffer = new byte[daeEntry.m_iFileSize];
		try {
			m_rafArhiveFile.seek(daeEntry.m_iFileOffset);
			m_rafArhiveFile.read(baBuffer, 0, daeEntry.m_iFileSize);
		} catch (IOException eE) {
			Dispose();
			throw new DL1ArhiveException("Error loading file: " + sFileName
					+ " from arhive\n" + eE.getMessage());
		}

		return baBuffer;
	}

	public FileDescriptor GetArhiveFD() throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		try {
			return m_rafArhiveFile.getFD();
		} catch (IOException eE) {
			throw new DL1ArhiveException(
					"Error getting arhive file descriptor\n");
		}
	}

	public int GetFileOffsetInArhive(String sFileName)
			throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		int iIdx;

		iIdx = FindFileIndex(sFileName);
		if (iIdx < 0) {
			throw new DL1ArhiveException("File " + sFileName + "not found\n");
		}

		return GetArhiveEntry(iIdx).m_iFileOffset;
	}

	public int GetFileSize(String sFileName) throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		int iIdx;

		iIdx = FindFileIndex(sFileName);
		if (iIdx < 0) {
			throw new DL1ArhiveException("File " + sFileName + "not found\n");
		}

		return GetArhiveEntry(iIdx).m_iFileSize;
	}

	public byte[] UnpackFile(byte[] baCompressed) {
		if (baCompressed.length <= 10) {
			return null;
		}

		int iDstBufPos;
		int iBytesUnpacked;
		int iOffsetInWin;
		int iIdx;
		int iReadOffsetInWin;
		byte bFlagByte;
		byte bData;
		byte bRepeatCount;
		byte[] baSlideWinBuf;
		byte[] baUnpacked;
		
		m_baBuffer = ByteBuffer.wrap(baCompressed);
		m_baBuffer.order(ByteOrder.LITTLE_ENDIAN);

		m_pfhFileHeader = LoadPackedFileHeader();
		if (m_pfhFileHeader == null) {
			return null;
		}
		if (m_pfhFileHeader.m_baSign[0] != 'L'
				|| m_pfhFileHeader.m_baSign[1] != 'Z'
				|| m_pfhFileHeader.m_iPackedSize > m_baBuffer.capacity()) {
			return null;
		}

		baUnpacked = new byte[m_pfhFileHeader.m_iUnpackedSize];
		baSlideWinBuf = new byte[0x1000];
		for (iIdx = 0; iIdx < baSlideWinBuf.length; iIdx++) {
			baSlideWinBuf[iIdx] = 0;
		}
		iDstBufPos = 0;
		iBytesUnpacked = 0;
		iOffsetInWin = 0x0FEE;
		
		do {
			bFlagByte = m_baBuffer.get();

			for (iIdx = 0; iIdx < 8 && iBytesUnpacked < m_pfhFileHeader.m_iUnpackedSize; iIdx++) {
				if ((bFlagByte & 1) != 0) {
					bData = m_baBuffer.get();
					baUnpacked[iDstBufPos] = bData;
					baSlideWinBuf[iOffsetInWin] = bData;

					iDstBufPos++;
					iBytesUnpacked++;
					iOffsetInWin = (iOffsetInWin + 1) & 0x0FFF;
				} else {
					iReadOffsetInWin = m_baBuffer.getShort();
					bRepeatCount = (byte) (((iReadOffsetInWin >>> 8) & 0x0F) + 3);
					iReadOffsetInWin = ((iReadOffsetInWin >>> 4) & 0x0F00)
							| (iReadOffsetInWin & 0x00FF);

					for (; bRepeatCount > 0; bRepeatCount--) {
						baUnpacked[iDstBufPos] = baSlideWinBuf[iReadOffsetInWin];
						baSlideWinBuf[iOffsetInWin] = baSlideWinBuf[iReadOffsetInWin];

						iDstBufPos++;
						iBytesUnpacked++;
						iOffsetInWin = (iOffsetInWin + 1) & 0x0FFF;
						iReadOffsetInWin = (iReadOffsetInWin + 1) & 0x0FFF;
					}
				}
				bFlagByte = (byte)(bFlagByte >>> 1);
			}

		} while (iBytesUnpacked < m_pfhFileHeader.m_iUnpackedSize && m_baBuffer.position() < m_baBuffer.capacity());

		return baUnpacked;
	}

	public void Dispose() {
		m_bHeaderOk = false;
		m_bFileTableOk = false;
		m_lArhiveEntries = null;
		m_dahArhiveHeader = null;
		m_lArhiveEntries = null;

		try {
			if (m_rafArhiveFile != null) {
				m_rafArhiveFile.close();
			}
		} catch (IOException eE) {
			// no problem if error occurs here
		}
	}

	private void LoadArhiveHeader() throws IOException, DL1ArhiveException {
		int iIdx;
		ByteBuffer bbByteBuffer;

		if (m_rafArhiveFile == null || m_rafArhiveFile.length() <= 16) {
			throw new DL1ArhiveException("File is too small!\n");
		}

		bbByteBuffer = ByteBuffer.allocate(16);
		bbByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		m_dahArhiveHeader = new DL1ArhiveHeader();

		m_rafArhiveFile.read(bbByteBuffer.array(), 0, 16);

		for (iIdx = 0; iIdx < 6; iIdx++) {
			m_dahArhiveHeader.m_bSign[iIdx] = bbByteBuffer.get();
		}
		m_dahArhiveHeader.m_sReserved1 = bbByteBuffer.getShort();
		m_dahArhiveHeader.m_sEntryCount = bbByteBuffer.getShort();
		m_dahArhiveHeader.m_iFileTableOffset = bbByteBuffer.getInt();
		m_dahArhiveHeader.m_sReserved2 = bbByteBuffer.getShort();
		m_iFileTableSize = m_dahArhiveHeader.m_sEntryCount * 16;

		if (m_dahArhiveHeader.m_bSign[0] == 'D'
				&& m_dahArhiveHeader.m_bSign[1] == 'L'
				&& m_dahArhiveHeader.m_bSign[2] == '1'
				&& m_rafArhiveFile.length() >= (m_dahArhiveHeader.m_iFileTableOffset + m_iFileTableSize)) {
			m_bHeaderOk = true;
		} else {
			throw new DL1ArhiveException("Arhive is corrupt!\n");
		}
	}

	private void LoadArhiveFileTable() throws IOException, DL1ArhiveException {
		int iIdx;
		int iCharIdx;
		int iCurrentOffset;
		int iOffsetInBuffer;
		DL1ArhiveEntry daeEntry;
		ByteBuffer bbByteBuffer;

		if (!m_bHeaderOk) {
			throw new DL1ArhiveException("Arhive header is not initialized!\n");
		}

		iCurrentOffset = 16;
		iOffsetInBuffer = 0;
		m_lArhiveEntries = new ArrayList<DL1ArhiveEntry>();
		bbByteBuffer = ByteBuffer.allocate(m_iFileTableSize);
		bbByteBuffer.order(ByteOrder.LITTLE_ENDIAN);

		m_rafArhiveFile.seek(m_dahArhiveHeader.m_iFileTableOffset);
		if (m_rafArhiveFile.read(bbByteBuffer.array(), 0, m_iFileTableSize) != m_iFileTableSize) {
			throw new DL1ArhiveException("File is too small!\n");
		}

		for (iIdx = 0; iIdx < m_dahArhiveHeader.m_sEntryCount; iIdx++) {
			daeEntry = new DL1ArhiveEntry();

			for (iCharIdx = 0; iCharIdx < 12
					&& bbByteBuffer.get(iOffsetInBuffer + iCharIdx) != 0; iCharIdx++) {

			}
			try {
				daeEntry.m_sFileName = new String(bbByteBuffer.array(),
						iOffsetInBuffer, iCharIdx, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new DL1ArhiveException("Error decoding entry name!\n");
			}
			iOffsetInBuffer += 12;

			daeEntry.m_iFileSize = bbByteBuffer.getInt(iOffsetInBuffer);
			iOffsetInBuffer += 4;

			daeEntry.m_iFileOffset = iCurrentOffset;
			iCurrentOffset += daeEntry.m_iFileSize;

			if ((daeEntry.m_iFileOffset + daeEntry.m_iFileSize) > m_rafArhiveFile
					.length()) {
				throw new DL1ArhiveException(
						String.format(
								"Arhive entry %06d points outside file!\nName: %s; size: %d; offset: %d\n",
								iIdx, daeEntry.m_sFileName,
								daeEntry.m_iFileSize, daeEntry.m_iFileOffset));
			}

			m_lArhiveEntries.add(daeEntry);
		}
		m_bFileTableOk = true;
	}

	private int FindFileIndex(String sFileName) throws DL1ArhiveException {
		if (!m_bHeaderOk || !m_bFileTableOk) {
			throw new DL1ArhiveException("Arhive is not loaded!\n");
		}

		int iIdx;

		for (iIdx = 0; iIdx < m_dahArhiveHeader.m_sEntryCount; iIdx++) {
			if (sFileName.compareToIgnoreCase(GetArhiveEntry(iIdx).m_sFileName) == 0) {
				return iIdx;
			}
		}

		return -1;
	}

	private PackedFileHeader LoadPackedFileHeader() {
		if (m_baBuffer == null) {
			return null;
		}

		PackedFileHeader pfhHeader;

		pfhHeader = new PackedFileHeader();
		m_baBuffer.position(0);
		pfhHeader.m_baSign[0] = m_baBuffer.get();
		pfhHeader.m_baSign[1] = m_baBuffer.get();
		pfhHeader.m_iPackedSize = m_baBuffer.getInt();
		pfhHeader.m_iUnpackedSize = m_baBuffer.getInt();

		return pfhHeader;
	}
}