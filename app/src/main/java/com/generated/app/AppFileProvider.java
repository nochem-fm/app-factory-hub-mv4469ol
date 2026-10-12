package com.generated.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

public class AppFileProvider extends ContentProvider {

    public static final String AUTHORITY = "com.app.factory.fm.fileprovider";
    public static final String FALLBACK_AUTHORITY = "com.generated.app.fileprovider";

    @Override
    public boolean onCreate() {
        return true;
    }

    public static Uri getUriForFile(Context context, File file) {
        String authority = AUTHORITY;
        try {
            String path = file.getCanonicalPath();
            return new Uri.Builder()
                    .scheme("content")
                    .authority(authority)
                    .path(path)
                    .build();
        } catch (IOException e) {
            return new Uri.Builder()
                    .scheme("content")
                    .authority(authority)
                    .path(file.getAbsolutePath())
                    .build();
        }
    }

    private File getFileForUri(Uri uri) throws FileNotFoundException {
        String path = uri.getPath();
        if (path == null) {
            throw new FileNotFoundException("Invalid URI path: " + uri);
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new FileNotFoundException("File not found: " + path);
        }
        return file;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File file = getFileForUri(uri);
        int fileMode;
        if ("r".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_READ_ONLY;
        } else if ("w".equals(mode) || "wt".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_TRUNCATE;
        } else if ("wa".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_WRITE_ONLY | ParcelFileDescriptor.MODE_CREATE | ParcelFileDescriptor.MODE_APPEND;
        } else if ("rw".equals(mode)) {
            fileMode = ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE;
        } else {
            fileMode = ParcelFileDescriptor.MODE_READ_ONLY;
        }
        return ParcelFileDescriptor.open(file, fileMode);
    }

    @Override
    public String getType(Uri uri) {
        String path = uri.getPath();
        if (path != null) {
            int lastDot = path.lastIndexOf('.');
            if (lastDot >= 0) {
                String extension = path.substring(lastDot + 1).toLowerCase();
                if ("apk".equals(extension)) {
                    return "application/vnd.android.package-archive";
                }
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (mime != null) {
                    return mime;
                }
            }
        }
        return "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        try {
            File file = getFileForUri(uri);
            if (projection == null) {
                projection = new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE};
            }
            MatrixCursor cursor = new MatrixCursor(projection, 1);
            MatrixCursor.RowBuilder row = cursor.newRow();
            for (String col : projection) {
                if (OpenableColumns.DISPLAY_NAME.equals(col)) {
                    row.add(file.getName());
                } else if (OpenableColumns.SIZE.equals(col)) {
                    row.add(file.length());
                } else {
                    row.add(null);
                }
            }
            return cursor;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("Insert not supported");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        try {
            File file = getFileForUri(uri);
            return file.delete() ? 1 : 0;
        } catch (FileNotFoundException e) {
            return 0;
        }
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("Update not supported");
    }
}
