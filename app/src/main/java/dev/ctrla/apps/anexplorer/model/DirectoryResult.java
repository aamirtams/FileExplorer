package dev.ctrla.apps.anexplorer.model;

import android.content.ContentProviderClient;
import android.database.Cursor;

import java.io.Closeable;

import dev.ctrla.apps.anexplorer.libcore.io.IoUtils;
import dev.ctrla.apps.anexplorer.misc.ContentProviderClientCompat;

import static dev.ctrla.apps.anexplorer.BaseActivity.State.MODE_UNKNOWN;
import static dev.ctrla.apps.anexplorer.BaseActivity.State.SORT_ORDER_UNKNOWN;

public class DirectoryResult implements Closeable {
	public ContentProviderClient client;
    public Cursor cursor;
    public Exception exception;

    public int mode = MODE_UNKNOWN;
    public int sortOrder = SORT_ORDER_UNKNOWN;

    @Override
    public void close() {
        IoUtils.closeQuietly(cursor);
        ContentProviderClientCompat.releaseQuietly(client);
        cursor = null;
        client = null;
    }
}