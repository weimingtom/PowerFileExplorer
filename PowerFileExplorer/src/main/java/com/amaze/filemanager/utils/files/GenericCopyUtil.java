package com.amaze.filemanager.utils.files;

import android.content.ContentResolver;
import android.content.Context;
import android.support.v4.provider.DocumentFile;
import android.util.Log;

import com.amaze.filemanager.filesystem.BaseFile;
import com.amaze.filemanager.filesystem.FileUtil;
import com.amaze.filemanager.filesystem.HFile;
import com.amaze.filemanager.filesystem.RootHelper;
import com.amaze.filemanager.utils.AppConfig;
import com.amaze.filemanager.utils.DataUtils;
import com.amaze.filemanager.utils.OTGUtil;
import com.amaze.filemanager.utils.OpenMode;
import com.amaze.filemanager.utils.ServiceWatcherUtil;
import com.amaze.filemanager.utils.cloud.CloudUtil;
import com.cloudrail.si.interfaces.CloudStorage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import com.amaze.filemanager.utils.ProgressHandler;
import com.amaze.filemanager.services.DeleteTask;
import java.util.ArrayList;

/**
 * Created by vishal on 26/10/16.
 *
 * Base class to handle file copy.
 */

public class GenericCopyUtil {
	private ProgressHandler progressHandler;
    private BaseFile mSourceFile;
    private HFile mTargetFile;
    private Context mContext;   // context needed to find the DocumentFile in otg/sd card
    private DataUtils dataUtils = DataUtils.getInstance();
    public static final String PATH_FILE_DESCRIPTOR = "/proc/self/fd/";

    public static final int DEFAULT_BUFFER_SIZE = 65536;
	public static final String TAG = "GenericCopyUtil";
	
    public GenericCopyUtil(Context context) {
        this.mContext = context;
    }

    /**
     * Starts copy of file
     * Supports : {@link File}, {@link jcifs.smb.SmbFile}, {@link DocumentFile}, {@link CloudStorage}
     * @param lowOnMemory defines whether system is running low on memory, in which case we'll switch to
     *                    using streams instead of channel which maps the who buffer in memory.
     *                    TODO: Use buffers even on low memory but don't map the whole file to memory but
     *                          parts of it, and transfer each part instead.
     * @throws IOException
     */
    private void startCopy(boolean lowOnMemory) throws IOException {

        FileInputStream inputStream = null;
        FileOutputStream outputStream = null;
        FileChannel inChannel = null;
        FileChannel outChannel = null;
        BufferedInputStream bufferedInputStream = null;
        BufferedOutputStream bufferedOutputStream = null;

        try {
            // initializing the input channels based on file types
            if (mSourceFile.isOtgFile()) {
                // source is in otg
                ContentResolver contentResolver = mContext.getContentResolver();
                DocumentFile documentSourceFile = OTGUtil.getDocumentFile(mSourceFile.getPath(),
																		  mContext, false);

                bufferedInputStream = new BufferedInputStream(contentResolver
															  .openInputStream(documentSourceFile.getUri()), DEFAULT_BUFFER_SIZE);
            } else if (mSourceFile.isSmb()) {
                // source is in smb
                bufferedInputStream = new BufferedInputStream(mSourceFile.getInputStream(mContext), DEFAULT_BUFFER_SIZE);
            } else if (mSourceFile.isDropBoxFile()) {
                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);
                bufferedInputStream = new BufferedInputStream(cloudStorageDropbox
															  .download(CloudUtil.stripPath(OpenMode.DROPBOX,
																							mSourceFile.getPath())));
            } else if (mSourceFile.isBoxFile()) {
                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);
                bufferedInputStream = new BufferedInputStream(cloudStorageBox
															  .download(CloudUtil.stripPath(OpenMode.BOX,
																							mSourceFile.getPath())));
            } else if (mSourceFile.isGoogleDriveFile()) {
                CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);
                bufferedInputStream = new BufferedInputStream(cloudStorageGdrive
															  .download(CloudUtil.stripPath(OpenMode.GDRIVE,
																							mSourceFile.getPath())));
            } else if (mSourceFile.isOneDriveFile()) {
                CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);
                bufferedInputStream = new BufferedInputStream(cloudStorageOnedrive
															  .download(CloudUtil.stripPath(OpenMode.ONEDRIVE,
																							mSourceFile.getPath())));
            } else {
                // source file is neither smb nor otg; getting a channel from direct file instead of stream
                File file = new File(mSourceFile.getPath());
                if (FileUtil.isReadable(file)) {

                    if (mTargetFile.isOneDriveFile()
						|| mTargetFile.isDropBoxFile()
						|| mTargetFile.isGoogleDriveFile()
						|| mTargetFile.isBoxFile()
						|| lowOnMemory) {
                        // our target is cloud, we need a stream not channel
                        bufferedInputStream = new BufferedInputStream(new FileInputStream(file));
                    } else {
                        inChannel = new RandomAccessFile(file, "r").getChannel();
                    }
                } else {
                    ContentResolver contentResolver = mContext.getContentResolver();
                    DocumentFile documentSourceFile = FileUtil.getDocumentFile(file,
																			   mSourceFile.isDirectory(), mContext);
					Log.d(TAG, file + ", " + mSourceFile);
                    bufferedInputStream = new BufferedInputStream(contentResolver
																  .openInputStream(documentSourceFile.getUri()), DEFAULT_BUFFER_SIZE);
                }
            }

			File targetFile = null;
			DocumentFile documentTargetFile = null;
            // initializing the output channels based on file types
            if (mTargetFile.isOtgFile()) {

                // target in OTG, obtain streams from DocumentFile Uri's

                ContentResolver contentResolver = mContext.getContentResolver();
                documentTargetFile = OTGUtil.getDocumentFile(mTargetFile.getPath(),
															 mContext, true);

                bufferedOutputStream = new BufferedOutputStream(contentResolver
																.openOutputStream(documentTargetFile.getUri()), DEFAULT_BUFFER_SIZE);
            } else if (mTargetFile.isSmb()) {

                bufferedOutputStream = new BufferedOutputStream(mTargetFile.getOutputStream(mContext), DEFAULT_BUFFER_SIZE);
            } else if (mTargetFile.isDropBoxFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageDropbox = dataUtils.getAccount(OpenMode.DROPBOX);

                if (mSourceFile.isDropBoxFile()) {
                    // we're in the same provider, use api method
                    cloudStorageDropbox.copy(CloudUtil.stripPath(OpenMode.DROPBOX, mSourceFile.getPath()),
											 CloudUtil.stripPath(OpenMode.DROPBOX, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageDropbox.upload(CloudUtil.stripPath(OpenMode.DROPBOX, mTargetFile.getPath()),
											   bufferedInputStream, mSourceFile.size, true);
                    return;
                }
            } else if (mTargetFile.isBoxFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageBox = dataUtils.getAccount(OpenMode.BOX);

                if (mSourceFile.isBoxFile()) {
                    // we're in the same provider, use api method
                    cloudStorageBox.copy(CloudUtil.stripPath(OpenMode.BOX, mSourceFile.getPath()),
										 CloudUtil.stripPath(OpenMode.BOX, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageBox.upload(CloudUtil.stripPath(OpenMode.BOX, mTargetFile.getPath()),
										   bufferedInputStream, mSourceFile.size, true);
                    bufferedInputStream.close();
                    return;
                }
            } else if (mTargetFile.isGoogleDriveFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageGdrive = dataUtils.getAccount(OpenMode.GDRIVE);


                if (mSourceFile.isGoogleDriveFile()) {
                    // we're in the same provider, use api method
                    cloudStorageGdrive.copy(CloudUtil.stripPath(OpenMode.GDRIVE, mSourceFile.getPath()),
											CloudUtil.stripPath(OpenMode.GDRIVE, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageGdrive.upload(CloudUtil.stripPath(OpenMode.GDRIVE, mTargetFile.getPath()),
											  bufferedInputStream, mSourceFile.size, true);
                    bufferedInputStream.close();
                    return;
                }
            } else if (mTargetFile.isOneDriveFile()) {
                // API doesn't support output stream, we'll upload the file directly
                CloudStorage cloudStorageOnedrive = dataUtils.getAccount(OpenMode.ONEDRIVE);

                if (mSourceFile.isOneDriveFile()) {
                    // we're in the same provider, use api method
                    cloudStorageOnedrive.copy(CloudUtil.stripPath(OpenMode.ONEDRIVE, mSourceFile.getPath()),
											  CloudUtil.stripPath(OpenMode.ONEDRIVE, mTargetFile.getPath()));
                    return;
                } else {
                    cloudStorageOnedrive.upload(CloudUtil.stripPath(OpenMode.ONEDRIVE, mTargetFile.getPath()),
												bufferedInputStream, mSourceFile.size, true);
                    bufferedInputStream.close();
                    return;
                }
            } else {
                // copying normal file, target not in OTG
                targetFile = new File(mTargetFile.getPath());
                if (FileUtil.isWritable(targetFile)) {

                    if (lowOnMemory) {
                        bufferedOutputStream = new BufferedOutputStream(new FileOutputStream(targetFile));
                    } else {
                        outChannel = new RandomAccessFile(targetFile, "rw").getChannel();
                    }
                } else {
                    ContentResolver contentResolver = mContext.getContentResolver();
                    documentTargetFile = FileUtil.getDocumentFile(targetFile,
																  mTargetFile.isDirectory(), mContext);

                    bufferedOutputStream = new BufferedOutputStream(contentResolver
																	.openOutputStream(documentTargetFile.getUri()), DEFAULT_BUFFER_SIZE);
                }
            }

			boolean success = true;
            if (bufferedInputStream != null) {
                if (bufferedOutputStream != null) 
					success = copyFile(bufferedInputStream, bufferedOutputStream);
                else if (outChannel != null) {
                    success = copyFile(bufferedInputStream, outChannel);
                }
            } else if (inChannel != null) {
                if (bufferedOutputStream != null) 
					success = copyFile(inChannel, bufferedOutputStream);
                else if (outChannel != null)  
					success = copyFile(inChannel, outChannel);
            }
			if (!success) {
				if (documentTargetFile != null) {
					documentTargetFile.delete();
				} else if (targetFile != null) {
					targetFile.delete();
				}
			}
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
            throw e;
        } catch (OutOfMemoryError e) {
            Log.e(TAG, e.getMessage());
            // we ran out of memory to map the whole channel, let's switch to streams
            //AppConfig.toast(mContext, mContext.getResources().getString(R.string.copy_low_memory));
            startCopy(true);
        } finally {
            //try {
				net.gnu.util.FileUtil.close(inChannel, outChannel, inputStream, outputStream, bufferedInputStream, bufferedOutputStream);
//                if (inChannel != null) inChannel.close();
//                if (outChannel != null) outChannel.close();
//                if (inputStream != null) inputStream.close();
//                if (outputStream != null) outputStream.close();
//                if (bufferedInputStream != null) bufferedInputStream.close();
//                if (bufferedOutputStream != null) bufferedOutputStream.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//                // failure in closing stream
//            }
        }
    }

    /**
     * Method exposes this class to initiate copy
     * @param sourceFile the source file, which is to be copied
     * @param targetFile the target file
     */
    public void copy(BaseFile sourceFile, HFile targetFile, ProgressHandler progressHandler) throws IOException {

        this.mSourceFile = sourceFile;
        this.mTargetFile = targetFile;
		this.progressHandler = progressHandler;
        startCopy(false);
    }

    private boolean copyFile(BufferedInputStream bufferedInputStream, FileChannel outChannel)
	throws IOException {

        MappedByteBuffer byteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0,
													 mSourceFile.size);
        int count = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		while ((count = bufferedInputStream.read(buffer)) != -1) {
			if (progressHandler.isCancelled) {
				outChannel.close();
				return false;
			}
//            count = bufferedInputStream.read(buffer);
//            if (count != -1) {
			byteBuffer.put(buffer, 0, count);
			ServiceWatcherUtil.POSITION += count;
//            }
        }
		return true;
    }

    private boolean copyFile(FileChannel inChannel, FileChannel outChannel) throws IOException {

        //MappedByteBuffer inByteBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
        //MappedByteBuffer outByteBuffer = outChannel.map(FileChannel.MapMode.READ_WRITE, 0, inChannel.size());

        ReadableByteChannel inByteChannel = new CustomReadableByteChannel(inChannel);
        outChannel.transferFrom(inByteChannel, 0, mSourceFile.size);
		return true;
    }

    private boolean copyFile(BufferedInputStream bufferedInputStream, BufferedOutputStream bufferedOutputStream)
	throws IOException {
        int count = 0;
        byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
		while ((count = bufferedInputStream.read(buffer)) != -1) {
			if (progressHandler.isCancelled) {
				bufferedOutputStream.close();
				return false;
			}
//          count = bufferedInputStream.read(buffer);
//          if (count != -1) {
			bufferedOutputStream.write(buffer, 0 , count);
			ServiceWatcherUtil.POSITION += count;
//          }
        }
        bufferedOutputStream.flush();
		return true;
    }

    private boolean copyFile(final FileChannel inChannel, final BufferedOutputStream bufferedOutputStream)
	throws IOException {
        final MappedByteBuffer inBuffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, mSourceFile.size);

        int count = -1;
        final byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
        while (inBuffer.hasRemaining() && count != 0) {
			if (progressHandler.isCancelled) {
				bufferedOutputStream.close();
				return false;
			}
            final int tempPosition = inBuffer.position();
            try {
                // try normal way of getting bytes
                final ByteBuffer tempByteBuffer = inBuffer.get(buffer);
                count = tempByteBuffer.position() - tempPosition;
            } catch (BufferUnderflowException exception) {
                exception.printStackTrace();

                // not enough bytes left in the channel to read, iterate over each byte and store
                // in the buffer

                // reset the counter bytes
                count = 0;
                final int length = buffer.length;
				for (int i=0; i < length && inBuffer.hasRemaining(); i++) {
                    buffer[i] = inBuffer.get();
                    count++;
                }
            }

            if (count != -1) {
                bufferedOutputStream.write(buffer, 0, count);
                ServiceWatcherUtil.POSITION = inBuffer.position();
            }

        }
        bufferedOutputStream.flush();
		return true;
    }

    /**
     * Inner class responsible for getting a {@link ReadableByteChannel} from the input channel
     * and to watch over the read progress
     */
    private class CustomReadableByteChannel implements ReadableByteChannel {

        ReadableByteChannel byteChannel;

        CustomReadableByteChannel(ReadableByteChannel byteChannel) {
            this.byteChannel = byteChannel;
        }

        @Override
        public int read(ByteBuffer dst) throws IOException {
            int bytes;
            if (((bytes = byteChannel.read(dst)) > 0)) {

                ServiceWatcherUtil.POSITION += bytes;
                return bytes;

            }
            return 0;
        }

        @Override
        public boolean isOpen() {
            return byteChannel.isOpen();
        }

        @Override
        public void close() throws IOException {

            byteChannel.close();
        }
    }
}
