package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.NoteColumns;

public class TrashNotesActivity extends Activity {
    private static final String TAG = "TrashNotesActivity";
    private ListView mTrashListView;
    private TextView mEmptyView;
    private NotesListAdapter mAdapter;
    private ContentResolver mContentResolver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.trash_note_list);

        // 初始化视图和数据
        mTrashListView = (ListView) findViewById(R.id.trash_notes_list);
        mEmptyView = (TextView) findViewById(R.id.empty_trash_view);
        mContentResolver = getContentResolver();

        initTrashNotesList();
    }

    private void initTrashNotesList() {
        // 初始化适配器，只传入 Context 参数
        mAdapter = new NotesListAdapter(this);
        mTrashListView.setAdapter(mAdapter);
        mTrashListView.setEmptyView(mEmptyView);

        // 加载数据
        loadTrashNotes();
    }

    private void loadTrashNotes() {
        Cursor cursor = mContentResolver.query(
                Notes.CONTENT_NOTE_URI,
                null,
                NoteColumns.PARENT_ID + "=" + Notes.ID_TRASH_FOLER,
                null,
                NoteColumns.MODIFIED_DATE + " DESC"
        );
        mAdapter.changeCursor(cursor);
    }

    private void showRestoreTrashDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.menu_restore_trash)
                .setMessage(R.string.alert_restore_all_notes)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        restoreTrash();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void restoreTrash() {
        try {
            // 准备更新的数据
            ContentValues values = new ContentValues();
            values.put(NoteColumns.PARENT_ID, Notes.ID_ROOT_FOLDER);
            values.put(NoteColumns.TYPE, Notes.TYPE_NOTE);
            values.put(NoteColumns.LOCAL_MODIFIED, System.currentTimeMillis());

            // 更新回收站中的所有便签
            int count = mContentResolver.update(
                    Notes.CONTENT_NOTE_URI,
                    values,
                    NoteColumns.PARENT_ID + "=? AND " + NoteColumns.TYPE + "!=?",
                    new String[] { String.valueOf(Notes.ID_TRASH_FOLER), String.valueOf(Notes.TYPE_SYSTEM) }
            );

            if (count > 0) {
                Toast.makeText(this, R.string.alert_restore_all_success, Toast.LENGTH_SHORT).show();
                // 重新加载数据
                loadTrashNotes();
            } else {
                Toast.makeText(this, R.string.error_restore_all, Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error restoring trash: " + e.getMessage());
            Toast.makeText(this, R.string.error_restore_all, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (mAdapter != null && mAdapter.getCursor() != null) {
            mAdapter.getCursor().close();
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.trash_note_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_empty_trash:
                showEmptyTrashDialog();
                return true;
            case R.id.menu_restore:
                showRestoreTrashDialog();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showEmptyTrashDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.empty_trash_title)
                .setMessage(R.string.empty_trash_message)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        emptyTrash();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void emptyTrash() {
        int count = mContentResolver.delete(
                Notes.CONTENT_NOTE_URI,
                NoteColumns.PARENT_ID + "=" + Notes.ID_TRASH_FOLER,
                null
        );

        if (count > 0) {
            Toast.makeText(this, R.string.empty_trash_success, Toast.LENGTH_SHORT).show();
            mAdapter.changeCursor(null);
        }
    }
}