package com.walkersclub.chengalpattu;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/** Shares files in cache/shared with the camera (selfies) and the share sheet (CSV export). */
public class FilesProvider extends ContentProvider {
    static final String AUTHORITY = "com.walkersclub.chengalpattu.files";

    static File file(Context ctx, String name) {
        if (name == null || name.contains("/") || name.contains("..")) throw new IllegalArgumentException("bad name");
        File dir = new File(ctx.getCacheDir(), "shared");
        dir.mkdirs();
        return new File(dir, name);
    }

    static Uri uri(String name) { return Uri.parse("content://" + AUTHORITY + "/" + name); }

    @Override public boolean onCreate() { return true; }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        return ParcelFileDescriptor.open(file(getContext(), uri.getLastPathSegment()), ParcelFileDescriptor.parseMode(mode));
    }

    @Override
    public String getType(Uri uri) {
        String n = String.valueOf(uri.getLastPathSegment());
        return n.endsWith(".csv") ? "text/csv" : n.endsWith(".jpg") ? "image/jpeg" : "application/octet-stream";
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sortOrder) {
        File f = file(getContext(), uri.getLastPathSegment());
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }

    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] args) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] args) { return 0; }
}
