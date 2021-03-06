//package net.gnu.explorer;
//
//import android.app.NotificationManager;
//import android.app.PendingIntent;
//import android.app.Service;
//import android.content.BroadcastReceiver;
//import android.content.Context;
//import android.content.Intent;
//import android.content.IntentFilter;
//import android.os.AsyncTask;
//import android.os.Binder;
//import android.os.Bundle;
//import android.os.IBinder;
//import android.preference.PreferenceManager;
//import android.support.v4.app.NotificationCompat;
//import android.text.format.Formatter;
//
//import net.gnu.explorer.R;
//import com.amaze.filemanager.filesystem.BaseFile;
//import com.amaze.filemanager.filesystem.FileUtil;
//import com.amaze.filemanager.utils.DataPackage;
//import com.amaze.filemanager.utils.files.Futils;
//import com.amaze.filemanager.utils.files.GenericCopyUtil;
//import com.amaze.filemanager.utils.PreferenceUtils;
//import com.amaze.filemanager.utils.ProgressHandler;
//import com.amaze.filemanager.utils.ServiceWatcherUtil;
//
//import java.io.BufferedInputStream;
//import java.io.BufferedOutputStream;
//import java.io.File;
//import java.io.FileInputStream;
//import java.io.IOException;
//import java.io.OutputStream;
//import java.util.ArrayList;
//import java.util.zip.ZipEntry;
//import java.util.zip.ZipOutputStream;
//import net.gnu.explorer.ExplorerActivity;
//import java.util.List;
//import android.util.Log;
//import net.gnu.explorer.ZipTaskService.DoWork;
//import java.util.Arrays;
//import java.text.SimpleDateFormat;
//import net.gnu.p7zip.Andro7za;
//import net.gnu.zpaq.Zpaq;
//import java.util.Calendar;
//import net.gnu.p7zip.UpdateProgress;
//import java.util.regex.Pattern;
//import java.util.regex.Matcher;
//
//public class ZipTaskService extends Service {
//
//    NotificationManager mNotifyManager;
//    NotificationCompat.Builder mBuilder;
//    String mZipPath;
//    Context ctx;
//    ProgressListener progressListener;
//    long totalBytes = 0L;
//    private final IBinder mBinder = new LocalBinder();
//    private ProgressHandler progressHandler;
//    private ArrayList<DataPackage> dataPackages = new ArrayList<>();
//
//
//
//	public static final String KEY_COMPRESS_PASSWORD = "zip_password";
//    public static final String KEY_COMPRESS_LEVEL = "zip_level";
//    public static final String KEY_COMPRESS_VOLUME = "zip_volume";
//	public static final String KEY_COMPRESS_TYPE = "zip_type";
//    public static final String KEY_COMPRESS_EXCLUDES = "zip_excludes";
//
//	public static final String KEY_COMPRESS_SEPARATE_ARCHIVE = "zip_createSeparateArchives";
//    public static final String KEY_COMPRESS_MASK = "zip_archiveNameMask";
//    public static final String KEY_COMPRESS_COMMAND = "zip_cmd";
//	public static final String KEY_COMPRESS_OTHER_ARGS = "zip_otherArgs";
//
//    public static final String KEY_COMPRESS_PATH = "zip_path";
//    public static final String KEY_COMPRESS_FILES = "zip_files";
//    public static final String KEY_COMPRESS_BROADCAST_CANCEL = "zip_cancel";
//
//	private String TAG = "ZipTaskService";
//	private Andro7za andro7za;
//	private Zpaq zpaq;
//
//    @Override
//    public void onCreate() {
//        ctx = getApplicationContext();
//        registerReceiver(receiver1, new IntentFilter(KEY_COMPRESS_BROADCAST_CANCEL));
//		andro7za = new Andro7za(ctx);
//		zpaq = new Zpaq(ctx);
//	}
//
//    @Override
//    public int onStartCommand(Intent intent, int flags, final int startId) {
//        Bundle b = intent.getExtras();//new Bundle();
//		Log.d(TAG, "onStartCommand " + b);
//
//		ZipTaskService.DoWork doWork = new DoWork();
//
//        doWork.archive = intent.getStringExtra(KEY_COMPRESS_PATH);
//		doWork.lf = intent.getStringExtra(KEY_COMPRESS_FILES);
//		doWork.password = intent.getStringExtra(KEY_COMPRESS_PASSWORD);
//		doWork.level = intent.getStringExtra(KEY_COMPRESS_LEVEL);
//		doWork.type = intent.getStringExtra(KEY_COMPRESS_TYPE);
//		doWork.volume = intent.getStringExtra(KEY_COMPRESS_VOLUME);
//		doWork.excludes = intent.getStringExtra(KEY_COMPRESS_EXCLUDES);
//		doWork.createSeparateArchives = intent.getBooleanExtra(KEY_COMPRESS_SEPARATE_ARCHIVE, false);
//		doWork.archiveNameMask = intent.getStringExtra(KEY_COMPRESS_MASK);
//		doWork.cmd = intent.getStringExtra(KEY_COMPRESS_COMMAND);
//		doWork.otherArgs = intent.getStringArrayListExtra(KEY_COMPRESS_OTHER_ARGS);
//		doWork.id = startId;
//
//		mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
//
//
//        //ArrayList<BaseFile> baseFiles = intent.getParcelableArrayListExtra(KEY_COMPRESS_FILES);
//
////        File zipFile = new File(path);
////        mZipPath = PreferenceManager.getDefaultSharedPreferences(this)
////			.getString(PreferenceUtils.KEY_PATH_COMPRESS, path);
////        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
////        if (!mZipPath.equals(path)) {
////            mZipPath.concat(mZipPath.endsWith("/") ? (zipFile.getName()) : ("/" + zipFile.getName()));
////        }
////
////        if (!zipFile.exists()) {
////            try {
////                zipFile.createNewFile();
////            } catch (IOException e) {
////                // TODO Auto-generated catch block
////                e.printStackTrace();
////            }
////        }
//
//        mBuilder = new NotificationCompat.Builder(this);
//        Intent notificationIntent = new Intent(this, ExplorerActivity.class);
//        notificationIntent.putExtra(ExplorerActivity.KEY_INTENT_PROCESS_VIEWER, true);
//        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
//        mBuilder.setContentIntent(pendingIntent);
//        mBuilder.setContentTitle(getResources().getString(R.string.compressing))
//			.setSmallIcon(R.drawable.ic_zip_box_white_36dp);
//        startForeground(Integer.parseInt("789" + startId), mBuilder.build());
//        //b.putInt("id", startId);
//        //b.putParcelableArrayList(KEY_COMPRESS_FILES, baseFiles);
//        //b.putString(KEY_COMPRESS_PATH, mZipPath);
//        doWork.execute(b);
//        // If we get killed, after returning from here, restart
//        return START_STICKY;
//    }
//
//    public class LocalBinder extends Binder {
//        public ZipTaskService getService() {
//            // Return this instance of LocalService so clients can call public methods
//            return ZipTaskService.this;
//        }
//    }
//
//    public void setProgressListener(ProgressListener progressListener) {
//        this.progressListener = progressListener;
//    }
//
//    public interface ProgressListener {
//        void onUpdate(DataPackage dataPackage);
//
//        void refresh();
//    }
//
//    public class DoWork extends AsyncTask<Bundle, Void, Integer> implements UpdateProgress {
//
//        ZipOutputStream zos;
//
//		int id;
//		//ArrayList<BaseFile> baseFiles;
//        //String zipPath;
//        private String lf = null;
//		private String archive;
//		private String password;
//		private String level = "";
//		private String type = "";
//		private String volume = "";
//		private String excludes = "";
//
//		private boolean createSeparateArchives = false;
//		private String archiveNameMask = "";
//		private String cmd = "";
//		private List<String> otherArgs;
//		ServiceWatcherUtil watcherUtil;
//		int fileProgress = 0;
//
//        public DoWork() {
//        }
//
//		Pattern addPattern = Pattern.compile("[+U]\\s(.+)");
//		@Override
//		public void updateProgress(String...progress) {
//			if (progress != null && progress.length > 0 
//				&& progress[0] != null && progress[0].trim().length() > 0) {
//				Matcher matcher = addPattern.matcher(progress[0]);
//				if (matcher.matches()) {
//					progressHandler.setFileName(matcher.group(1));
//					progressHandler.setSourceFilesProcessed(++fileProgress);
//					ServiceWatcherUtil.POSITION += 1;
//				}
//			}
//		}
//
////        public ArrayList<File> toFileArray(ArrayList<BaseFile> a) {
////            ArrayList<File> b = new ArrayList<>();
////            for (int i = 0; i < a.size(); i++) {
////                b.add(new File(a.get(i).getPath()));
////            }
////            return b;
////        }
//
//        protected Integer doInBackground(Bundle... bundle) {
//            //final int id = p1[0].getInt("id");
//            //ArrayList<BaseFile> baseFiles = p1[0].getParcelableArrayList(KEY_COMPRESS_FILES);
//			final List<String> fList = Arrays.asList(lf.split("\\|+\\s*"));
//
//            // setting up service watchers and initial data packages
//            // finding total size on background thread (this is necessary condition for SMB!)
//            final long[] folderSize = net.gnu.util.FileUtil.getFolderSize(fList, null);
//			totalBytes = folderSize[0];//Futils.getTotalBytes(baseFiles, c);
//            progressHandler = new ProgressHandler((int)folderSize[1], totalBytes);//baseFiles.size()
//            progressHandler.setProgressListener(new ProgressHandler.ProgressListener() {
//					@Override
//					public void onProgressed(String fileName, int sourceFiles, int sourceProgress,
//											 long totalSize, long writtenSize, int speed) {
//						publishResults(id, fileName, sourceFiles, sourceProgress,
//									   totalSize, writtenSize, speed, false);
//					}
//				});
//
//            DataPackage intent1 = new DataPackage();
//            intent1.setName(fList.get(0));//baseFiles.getName()
//            intent1.setSourceFiles((int)folderSize[1]);//baseFiles.size());
//            intent1.setSourceProgress(0);
//            intent1.setTotal(folderSize[1]);//totalBytes);
//            intent1.setByteProgress(0);
//            intent1.setSpeedRaw(0);
//            intent1.setMove(false);
//            intent1.setCompleted(false);
//            putDataPackage(intent1);
//
//            //zipPath = p1[0].getString(KEY_COMPRESS_PATH);
//            //execute(toFileArray(baseFiles), archive);//zipPath);
//
//			watcherUtil = new ServiceWatcherUtil(progressHandler, folderSize[1]);//totalBytes);
//            watcherUtil.watch();
//
//			final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat(archiveNameMask);
//			if (createSeparateArchives) {
//				final List<String> sList = new ArrayList<>(1);
//				if (archiveNameMask != null && archiveNameMask.length() > 0) {
//					for (String st : fList) {
//						try {
//							sList.clear();
//							sList.add(st);
//							File file = new File(st);
//							if (!type.equals("zpaq")) {
//								int ret = andro7za.compress(
//									cmd,
//									file.getParent() + "/" + file.getName() + "_" + TIME_FORMAT.format(Calendar.getInstance().getTimeInMillis()) + "." + type,
//									password, 
//									level, 
//									volume, 
//									sList,
//									excludes,
//									new ArrayList<String>(otherArgs),
//									this);
////								if (ret == 1) {
////									publishProgress("Warning");
////									return "Warning";
////								} else if (ret == 2) {
////									publishProgress("Fatal error");
////									return "Fatal error";
////								} else if (ret == 7) {
////									publishProgress("Command line error");
////									return "Command line error";
////								} else if (ret == 8) {
////									publishProgress("Not enough memory for operation");
////									return "Not enough memory for operation";
////								} else if (ret == 255) {
////									publishProgress("User stopped the process");
////									return "User stopped the process";
////								}
//							} else {
//								int ret = zpaq.compress(
//									file.getParent() + "/" + file.getName() + "_" + TIME_FORMAT.format(Calendar.getInstance().getTimeInMillis()) + "." + type,
//									password, 
//									level, 
//									st,
//									excludes,
//									new ArrayList<String>(otherArgs),
//									this);
//							}
//						} catch (Throwable t) {
//							t.printStackTrace();
//						}
//					}
//				} else {//!archiveNameMask
//					for (String st : fList) {
//						try {
//							sList.clear();
//							sList.add(st);
//							File file = new File(st);
//							if (!type.equals("zpaq")) {
//								int ret = andro7za.compress(
//									cmd,
//									file.getParent() + "/" + file.getName() + "." + type,
//									password, 
//									level, 
//									volume, 
//									sList,
//									excludes,
//									new ArrayList<String>(otherArgs),
//									this);
////								if (ret == 1) {
////									publishProgress("Warning");
////									return "Warning";
////								} else if (ret == 2) {
////									publishProgress("Fatal error");
////									return "Fatal error";
////								} else if (ret == 7) {
////									publishProgress("Command line error");
////									return "Command line error";
////								} else if (ret == 8) {
////									publishProgress("Not enough memory for operation");
////									return "Not enough memory for operation";
////								} else if (ret == 255) {
////									publishProgress("User stopped the process");
////									return "User stopped the process";
////								}
//							} else {
//								int ret = zpaq.compress(
//									file.getParent() + "/" + file.getName() + "." + type,
//									password, 
//									level, 
//									st,
//									excludes,
//									new ArrayList<String>(otherArgs),
//									this);
//							}
//						} catch (Throwable t) {
//							t.printStackTrace();
//						}
//					}
//				}
//			} else { //!createSeparateArchives
//				try {
//					int lastIndexOf = archive.lastIndexOf(".");
//					if (!type.equals("zpaq")) {
//						int ret = andro7za.compress(
//							cmd,
//							archiveNameMask.length() == 0 ? archive : archive.substring(0, lastIndexOf) + "_" + TIME_FORMAT.format(Calendar.getInstance().getTimeInMillis()) + archive.substring(lastIndexOf),
//							password, 
//							level, 
//							volume, 
//							fList,
//							excludes,
//							otherArgs,
//							this);
////					if (ret == 1) {
////						publishProgress("Warning");
////						return "Warning";
////					} else if (ret == 2) {
////						publishProgress("Fatal error");
////						return "Fatal error";
////					} else if (ret == 7) {
////						publishProgress("Command line error");
////						return "Command line error";
////					} else if (ret == 8) {
////						publishProgress("Not enough memory for operation");
////						return "Not enough memory for operation";
////					} else if (ret == 255) {
////						publishProgress("User stopped the process");
////						return "User stopped the process";
////					}
//					} else {
//						int ret = zpaq.compress(
//							archiveNameMask.length() == 0 ? archive : archive.substring(0, lastIndexOf) + "_" + TIME_FORMAT.format(Calendar.getInstance().getTimeInMillis()) + archive.substring(lastIndexOf),
//							password, 
//							level, 
//							lf,
//							excludes,
//							otherArgs,
//							this);
//					}
//				} catch (Throwable t) {
//					t.printStackTrace();
//				}
//			}
//
//            return id;
//        }
//
//        @Override
//        public void onPostExecute(Integer b) {
//
//            watcherUtil.stopWatch();
//            Intent intent = new Intent("loadlist");
//            sendBroadcast(intent);
//            stopSelf();
//        }
//
////        public void execute(ArrayList<File> baseFiles, String zipPath) {
////
////            OutputStream out;
////            File zipDirectory = new File(zipPath);
////            watcherUtil = new ServiceWatcherUtil(progressHandler, totalBytes);
////            watcherUtil.watch();
////
////            try {
////                out = FileUtil.getOutputStream(zipDirectory, ctx, totalBytes);
////                zos = new ZipOutputStream(new BufferedOutputStream(out));
////
////                int fileProgress = 0;
////                for (File file : baseFiles) {
////                    if (!progressHandler.getCancelled()) {
////
////                        progressHandler.setFileName(file.getName());
////                        progressHandler.setSourceFilesProcessed(++fileProgress);
////                        compressFile(file, "");
////                    } else return;
////                }
////            } catch (Exception e) {
////            } finally {
////
////                try {
////                    zos.flush();
////                    zos.close();
////                } catch (IOException e) {
////                    e.printStackTrace();
////                }
////            }
////        }
//
//        private void compressFile(File file, String path) throws IOException, NullPointerException {
//
//            if (!file.isDirectory()) {
//                if (progressHandler.getCancelled()) return;
//
//                byte[] buf = new byte[GenericCopyUtil.DEFAULT_BUFFER_SIZE];
//                int len;
//                BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
//                zos.putNextEntry(new ZipEntry(path + "/" + file.getName()));
//                while ((len = in.read(buf)) > 0) {
//
//                    zos.write(buf, 0, len);
//                    ServiceWatcherUtil.POSITION += len;
//                }
//                in.close();
//                return;
//            }
//            if (file.list() == null) {
//                return;
//            }
//            for (File currentFile : file.listFiles()) {
//
//                compressFile(currentFile, path + File.separator + file.getName());
//
//            }
//        }
//    }
//
//    private void publishResults(int id, String fileName, int sourceFiles, int sourceProgress,
//                                long total, long done, int speed, boolean isCompleted) {
//        if (!progressHandler.getCancelled()) {
//            float progressPercent = ((float) done / total) * 100;
//            mBuilder.setProgress(100, Math.round(progressPercent), false);
//            mBuilder.setOngoing(true);
//            int title = R.string.compressing;
//            mBuilder.setContentTitle(ctx.getResources().getString(title));
//            mBuilder.setContentText(new File(fileName).getName() + " " +
//									Formatter.formatFileSize(ctx, done) + "/" + Formatter.formatFileSize(ctx, total));
//            int id1 = Integer.parseInt("789" + id);
//            mNotifyManager.notify(id1, mBuilder.build());
//            if (done == total || total == 0) {
//                mBuilder.setContentTitle(getString(R.string.compression_complete));
//                mBuilder.setContentText("");
//                mBuilder.setProgress(100, 100, false);
//                mBuilder.setOngoing(false);
//                mNotifyManager.notify(id1, mBuilder.build());
//                publishCompletedResult(id1);
//                isCompleted = true;
//            }
//
//            DataPackage intent = new DataPackage();
//            intent.setName(fileName);
//            intent.setSourceFiles(sourceFiles);
//            intent.setSourceProgress(sourceProgress);
//            intent.setTotal(total);
//            intent.setByteProgress(done);
//            intent.setSpeedRaw(speed);
//            intent.setMove(false);
//            intent.setCompleted(isCompleted);
//
//            putDataPackage(intent);
//            if (progressListener != null) {
//                progressListener.onUpdate(intent);
//                if (isCompleted) progressListener.refresh();
//            }
//        } else {
//            publishCompletedResult(Integer.parseInt("789" + id));
//        }
//    }
//
//    public void publishCompletedResult(int id1) {
//        try {
//            mNotifyManager.cancel(id1);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Class used for the client Binder.  Because we know this service always
//     * runs in the same process as its clients, we don't need to deal with IPC.
//     */
//
//    private BroadcastReceiver receiver1 = new BroadcastReceiver() {
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            progressHandler.setCancelled(true);
//        }
//    };
//
//    @Override
//    public IBinder onBind(Intent arg0) {
//        // TODO Auto-generated method stub
//        return mBinder;
//    }
//
//    @Override
//    public void onDestroy() {
//        this.unregisterReceiver(receiver1);
//    }
//
//    /**
//     * Returns the {@link #dataPackages} list which contains
//     * data to be transferred to {@link com.amaze.filemanager.fragments.ProcessViewer}
//     * Method call is synchronized so as to avoid modifying the list
//     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
//     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
//     *
//     * @return
//     */
//    public synchronized DataPackage getDataPackage(int index) {
//        return this.dataPackages.get(index);
//    }
//
//    public synchronized int getDataPackageSize() {
//        return this.dataPackages.size();
//    }
//
//    /**
//     * Puts a {@link DataPackage} into a list
//     * Method call is synchronized so as to avoid modifying the list
//     * by {@link ServiceWatcherUtil#handlerThread} while {@link MainActivity#runOnUiThread(Runnable)}
//     * is executing the callbacks in {@link com.amaze.filemanager.fragments.ProcessViewer}
//     *
//     * @param dataPackage
//     */
//    private synchronized void putDataPackage(DataPackage dataPackage) {
//        this.dataPackages.add(dataPackage);
//    }
//
//}
//
