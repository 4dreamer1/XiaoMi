/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.tool;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;


public class BackupUtils {
    private static final String TAG = "BackupUtils";
    // 单例模式相关变量
    private static BackupUtils sInstance;

    /**
     * 获取BackupUtils的单例实例。
     *
     * @param context 上下文对象，用于访问应用全局功能。
     * @return 返回BackupUtils的单例实例。
     */
    public static synchronized BackupUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new BackupUtils(context);
        }
        return sInstance;
    }

    /**
     * 定义备份或恢复状态的标志。
     */
    // 当前，SD卡未挂载
    public static final int STATE_SD_CARD_UNMOUONTED = 0;
    // 备份文件不存在
    public static final int STATE_BACKUP_FILE_NOT_EXIST = 1;
    // 数据格式不正确，可能被其他程序更改
    public static final int STATE_DATA_DESTROIED = 2;
    // 运行时异常导致恢复或备份失败
    public static final int STATE_SYSTEM_ERROR = 3;
    // 备份或恢复成功
    public static final int STATE_SUCCESS = 4;

    private TextExport mTextExport;

    /**
     * BackupUtils的私有构造函数。
     *
     * @param context 上下文对象，用于初始化文本导出功能。
     */
    private BackupUtils(Context context) {
        mTextExport = new TextExport(context);
    }

    /**
     * 检查外部存储是否可用。
     *
     * @return 如果外部存储可用返回true，否则返回false。
     */
    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    /**
     * 导出数据到文本文件。
     *
     * @return 返回导出操作的状态码，详见STATE_*常量。
     */
    public int exportToText() {
        return mTextExport.exportToText();
    }

    /**
     * 获取导出的文本文件名。
     *
     * @return 返回导出文本文件的名称。
     */
    public String getExportedTextFileName() {
        return mTextExport.mFileName;
    }

    /**
     * 获取导出的文本文件目录。
     *
     * @return 返回导出文本文件所在的目录。
     */
    public String getExportedTextFileDir() {
        return mTextExport.mFileDirectory;
    }

    /**
     * 内部类TextExport，用于执行文本导出操作。
     */
    private static class TextExport {
        // 查询笔记时需要的列
        private static final String[] NOTE_PROJECTION = {
                NoteColumns.ID,
                NoteColumns.MODIFIED_DATE,
                NoteColumns.SNIPPET,
                NoteColumns.TYPE
        };

        // 笔记列的索引
        private static final int NOTE_COLUMN_ID = 0;
        private static final int NOTE_COLUMN_MODIFIED_DATE = 1;
        private static final int NOTE_COLUMN_SNIPPET = 2;

        // 查询数据时需要的列
        private static final String[] DATA_PROJECTION = {
                DataColumns.CONTENT,
                DataColumns.MIME_TYPE,
                DataColumns.DATA1,
                DataColumns.DATA2,
                DataColumns.DATA3,
                DataColumns.DATA4,
        };


        // 定义数据列的内容索引
        private static final int DATA_COLUMN_CONTENT = 0;

        // 定义数据列的MIME类型索引
        private static final int DATA_COLUMN_MIME_TYPE = 1;

        // 定义数据列的呼叫日期索引
        private static final int DATA_COLUMN_CALL_DATE = 2;

        // 定义数据列的电话号码索引
        private static final int DATA_COLUMN_PHONE_NUMBER = 4;

        // 用于导出笔记的文本格式数组
        private final String[] TEXT_FORMAT;
        // 定义文本格式的索引：文件夹名称
        private static final int FORMAT_FOLDER_NAME = 0;
        // 定义文本格式的索引：笔记日期
        private static final int FORMAT_NOTE_DATE = 1;
        // 定义文本格式的索引：笔记内容
        private static final int FORMAT_NOTE_CONTENT = 2;

        // 上下文对象，用于访问资源和内容解析器
        private Context mContext;
        // 文件名
        private String mFileName;
        // 文件目录
        private String mFileDirectory;

        /**
         * 构造函数
         *
         * @param context 上下文对象，通常是一个Activity或者Application对象
         */
        public TextExport(Context context) {
            // 初始化文本格式数组
            TEXT_FORMAT = context.getResources().getStringArray(R.array.format_for_exported_note);
            mContext = context;
            mFileName = "";
            mFileDirectory = "";
        }

        /**
         * 根据索引获取文本格式
         *
         * @param id 索引ID
         * @return 返回对应索引的文本格式
         */
        private String getFormat(int id) {
            return TEXT_FORMAT[id];
        }

        /**
         * 将指定文件夹的笔记导出为文本
         *
         * @param folderId 文件夹ID
         * @param ps       打印流，用于写入导出的文本内容
         */
        private void exportFolderToText(String folderId, PrintStream ps) {
            // 查询属于该文件夹的笔记
            Cursor notesCursor = mContext.getContentResolver().query(Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION, NoteColumns.PARENT_ID + "=?", new String[]{
                            folderId
                    }, null);

            if (notesCursor != null) {
                if (notesCursor.moveToFirst()) {
                    do {
                        // 打印笔记的最后修改日期
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                notesCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // 导出该笔记的内容到文本
                        String noteId = notesCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (notesCursor.moveToNext());
                }
                notesCursor.close();
            }
        }


        /**
         * 将指定id的笔记导出到打印流中
         *
         * @param noteId 笔记的id
         * @param ps     打印流，用于输出笔记内容
         */
        private void exportNoteToText(String noteId, PrintStream ps) {
            // 查询指定id的笔记数据
            Cursor dataCursor = mContext.getContentResolver().query(Notes.CONTENT_DATA_URI,
                    DATA_PROJECTION, DataColumns.NOTE_ID + "=?", new String[]{
                            noteId
                    }, null);

            if (dataCursor != null) {
                if (dataCursor.moveToFirst()) {
                    do {
                        String mimeType = dataCursor.getString(DATA_COLUMN_MIME_TYPE);
                        if (DataConstants.CALL_NOTE.equals(mimeType)) {
                            // 处理通话记录类型的笔记
                            String phoneNumber = dataCursor.getString(DATA_COLUMN_PHONE_NUMBER);
                            long callDate = dataCursor.getLong(DATA_COLUMN_CALL_DATE);
                            String location = dataCursor.getString(DATA_COLUMN_CONTENT);

                            // 打印电话号码、通话时间、附件位置
                            if (!TextUtils.isEmpty(phoneNumber)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        phoneNumber));
                            }
                            ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT), DateFormat
                                    .format(mContext.getString(R.string.format_datetime_mdhm),
                                            callDate)));
                            if (!TextUtils.isEmpty(location)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        location));
                            }
                        } else if (DataConstants.NOTE.equals(mimeType)) {
                            // 处理普通笔记类型
                            String content = dataCursor.getString(DATA_COLUMN_CONTENT);
                            if (!TextUtils.isEmpty(content)) {
                                ps.println(String.format(getFormat(FORMAT_NOTE_CONTENT),
                                        content));
                            }
                        }
                    } while (dataCursor.moveToNext());
                }
                dataCursor.close();
            }
            // 在每个笔记内容之间打印一行分隔符
            try {
                ps.write(new byte[]{
                        Character.LINE_SEPARATOR, Character.LETTER_NUMBER
                });
            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
        }

        /**
         * 将所有笔记以文本格式导出
         *
         * @return 导出操作的状态码，成功返回STATE_SUCCESS，否则返回其他错误状态码
         */
        public int exportToText() {
            // 检查外部存储器是否可用
            if (!externalStorageAvailable()) {
                Log.d(TAG, "Media was not mounted");
                return STATE_SD_CARD_UNMOUONTED;
            }

            // 获取用于导出的打印流
            PrintStream ps = getExportToTextPrintStream();
            if (ps == null) {
                Log.e(TAG, "get print stream error");
                return STATE_SYSTEM_ERROR;
            }

            // 首先导出文件夹及其包含的笔记
            Cursor folderCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                            + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                            + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER, null, null);

            if (folderCursor != null) {
                if (folderCursor.moveToFirst()) {
                    do {
                        // 打印文件夹名称
                        String folderName = "";
                        if (folderCursor.getLong(NOTE_COLUMN_ID) == Notes.ID_CALL_RECORD_FOLDER) {
                            folderName = mContext.getString(R.string.call_record_folder_name);
                        } else {
                            folderName = folderCursor.getString(NOTE_COLUMN_SNIPPET);
                        }
                        if (!TextUtils.isEmpty(folderName)) {
                            ps.println(String.format(getFormat(FORMAT_FOLDER_NAME), folderName));
                        }
                        String folderId = folderCursor.getString(NOTE_COLUMN_ID);
                        // 导出文件夹中的笔记
                        exportFolderToText(folderId, ps);
                    } while (folderCursor.moveToNext());
                }
                folderCursor.close();
            }

            // 导出根文件夹中的笔记
            Cursor noteCursor = mContext.getContentResolver().query(
                    Notes.CONTENT_NOTE_URI,
                    NOTE_PROJECTION,
                    NoteColumns.TYPE + "=" + +Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID
                            + "=0", null, null);

            if (noteCursor != null) {
                if (noteCursor.moveToFirst()) {
                    do {
                        // 打印笔记的修改时间
                        ps.println(String.format(getFormat(FORMAT_NOTE_DATE), DateFormat.format(
                                mContext.getString(R.string.format_datetime_mdhm),
                                noteCursor.getLong(NOTE_COLUMN_MODIFIED_DATE))));
                        // 导出笔记内容
                        String noteId = noteCursor.getString(NOTE_COLUMN_ID);
                        exportNoteToText(noteId, ps);
                    } while (noteCursor.moveToNext());
                }
                noteCursor.close();
            }
            ps.close();

            return STATE_SUCCESS;
        }


        /**
         * 获取用于导出文本的PrintStream对象
         * 该方法会在SD卡上创建一个文件并返回对应的打印流
         *
         * @return PrintStream对象，如果创建失败则返回null
         */
        private PrintStream getExportToTextPrintStream() {
            // 在SD卡上生成文件，使用预定义的路径和文件名格式
            File file = generateFileMountedOnSDcard(mContext, R.string.file_path,
                    R.string.file_name_txt_format);
            if (file == null) {
                Log.e(TAG, "create file to exported failed");
                return null;
            }
            
            // 保存生成的文件信息，用于后续处理
            mFileName = file.getName();
            mFileDirectory = mContext.getString(R.string.file_path);

            PrintStream ps = null;
            // 使用try-with-resources确保FileOutputStream会被自动关闭
            try (FileOutputStream fos = new FileOutputStream(file)) {
                // 创建PrintStream，包装FileOutputStream提供更高级的打印功能
                ps = new PrintStream(fos);
                return ps;
            } catch (FileNotFoundException e) {
                // 文件未找到（例如：权限问题、存储空间已满等）
                Log.e(TAG, "File not found: " + e.getMessage());
                closeQuietly(ps);
                return null;
            } catch (NullPointerException e) {
                // 空指针异常（例如：file对象为null）
                Log.e(TAG, "Null pointer exception: " + e.getMessage());
                closeQuietly(ps);
                return null;
            } catch (IOException e) {
                // IO异常（例如：文件操作过程中的错误）
                Log.e(TAG, "IO exception: " + e.getMessage());
                closeQuietly(ps);
                return null;
            }
        }

        /**
         * 安全关闭PrintStream的辅助方法
         * 
         * @param ps 要关闭的PrintStream对象
         */
        private void closeQuietly(PrintStream ps) {
            if (ps != null) {
                try {
                    ps.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing PrintStream: " + e.getMessage());
                }
            }
        }

    }

    /**
     * 在SD卡上生成用于存储导入数据的文本文件
     *
     * @param context             上下文对象，用于访问应用的资源和内容提供者
     * @param filePathResId       路径字符串资源ID，指定文件存储的路径
     * @param fileNameFormatResId 文件名格式字符串资源ID，用于生成带有日期的文件名
     * @return 返回创建的文件对象，如果创建失败则返回null
     */
    public static File generateFileMountedOnSDcard(Context context, int filePathResId, int fileNameFormatResId) {
        // 检查是否有写外部存储的权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED) {
            // 权限未授予，弹出请求权限对话框
            ActivityCompat.requestPermissions((Activity) context,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            return null;
        }

        try {
            // 获取应用专属的外部存储目录
            File filedir;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                filedir = context.getExternalFilesDir(null);
            } else {
                filedir = new File(Environment.getExternalStorageDirectory(), sanitizePath(context.getString(filePathResId)));
            }

            if (filedir == null) {
                Log.e(TAG, "Failed to get external storage directory");
                return null;
            }

            // 创建文件目录
            if (!filedir.exists() && !filedir.mkdirs()) {
                Log.e(TAG, "Failed to create directory");
                return null;
            }

            // 生成安全的文件名
            String dateStr = DateFormat.format(context.getString(R.string.format_date_ymd), 
                System.currentTimeMillis()).toString();
            String fileName = sanitizeFileName(context.getString(fileNameFormatResId, dateStr));

            // 验证文件路径的规范性
            File file = new File(filedir, fileName);
            String canonicalPath = file.getCanonicalPath();
            String dirCanonicalPath = filedir.getCanonicalPath();
            
            // 确保文件路径在预期目录内
            if (!canonicalPath.startsWith(dirCanonicalPath)) {
                Log.e(TAG, "Security violation: attempted path traversal");
                return null;
            }

            // 创建文件，使用原子操作
            if (!file.exists() && !file.createNewFile()) {
                Log.e(TAG, "Failed to create file");
                return null;
            }

            // 设置适当的文件权限并验证
            boolean readableSet = file.setReadable(true, true);
            boolean writableSet = file.setWritable(true, true);
            boolean executableSet = file.setExecutable(false);

            if (!readableSet || !writableSet || !executableSet) {
                Log.w(TAG, "Failed to set one or more file permissions: " +
                          "readable=" + readableSet +
                          ", writable=" + writableSet +
                          ", executable=" + executableSet);
                // 根据你的安全要求，你可以选择:
                // 1. 继续使用文件（当前实现）
                // 2. 删除文件并返回null
                // 3. 抛出异常
            }

            return file;

        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Error creating file: " + e.getMessage());
            return null;
        }
    }

    /**
     * 清理文件名中的不安全字符
     */
    private static String sanitizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        // 移除或替换不安全的字符
        return fileName.replaceAll("[\\\\/:*?\"<>|]", "_")
                      .replaceAll("\\s+", "_")
                      .trim();
    }

    /**
     * 清理路径中的不安全字符
     */
    private static String sanitizePath(String path) {
        if (path == null) {
            return null;
        }
        // 移除或替换不安全的字符，保留路径分隔符
        return path.replaceAll("[*?\"<>|]", "_")
                  .replaceAll("\\s+", "_")
                  .trim();
    }

}


