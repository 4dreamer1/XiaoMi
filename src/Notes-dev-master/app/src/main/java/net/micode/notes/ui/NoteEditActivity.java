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

package net.micode.notes.ui;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.appwidget.AppWidgetManager;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ShortcutManager;
import android.content.pm.ShortcutInfo;
import android.content.SharedPreferences;
import android.graphics.drawable.Icon;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.TextNote;
import net.micode.notes.model.WorkingNote;
import net.micode.notes.model.WorkingNote.NoteSettingChangedListener;
import net.micode.notes.tool.DataUtils;
import net.micode.notes.tool.ResourceParser;
import net.micode.notes.tool.ResourceParser.TextAppearanceResources;
import net.micode.notes.ui.DateTimePickerDialog.OnDateTimeSetListener;
import net.micode.notes.ui.NoteEditText.OnTextViewChangeListener;
import net.micode.notes.widget.NoteWidgetProvider_2x;
import net.micode.notes.widget.NoteWidgetProvider_4x;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Stack;

/**
 * 笔记编辑活动类
 * 该类负责处理笔记的编辑、保存、删除等核心功能。
 * 
 * 主要功能：
 * 1. 提供笔记编辑界面
 * 2. 处理笔记的保存和更新
 * 3. 管理笔记的删除和移动到回收站
 * 4. 处理笔记的分享和导出
 * 5. 管理笔记的提醒设置
 * 
 * @author MiCode
 * @version 1.0
 */
public class NoteEditActivity extends Activity implements OnClickListener,
        NoteSettingChangedListener, OnTextViewChangeListener {

    private EditText mNoteEditor; // 用于编辑文本的 EditText

    /**
     * 头部视图的ViewHolder类，用于存储头部视图中的UI组件引用。
     */
    private static class HeadViewHolder {
        public TextView tvModified; // 显示修改日期的文本视图
        public ImageView ivAlertIcon; // 提示图标图像视图
        public TextView tvAlertDate; // 显示提醒日期的文本视图
        public ImageView ibSetBgColor; // 设置背景颜色的图像视图
        private EditText editText;
        private TextView textView;
    }

    /**
     * 背景选择按钮与其对应选中状态图标的映射。
     */
    private static final Map<Integer, Integer> sBgSelectorBtnsMap = new HashMap<Integer, Integer>();

    static {
        // 初始化背景选择按钮映射
        sBgSelectorBtnsMap.put(R.id.iv_bg_yellow, ResourceParser.YELLOW);
        sBgSelectorBtnsMap.put(R.id.iv_bg_red, ResourceParser.RED);
        sBgSelectorBtnsMap.put(R.id.iv_bg_blue, ResourceParser.BLUE);
        sBgSelectorBtnsMap.put(R.id.iv_bg_green, ResourceParser.GREEN);
        sBgSelectorBtnsMap.put(R.id.iv_bg_white, ResourceParser.WHITE);
    }

    /**
     * 背景选择按钮选中状态与其对应图标的映射。
     */
    private static final Map<Integer, Integer> sBgSelectorSelectionMap = new HashMap<Integer, Integer>();

    static {
        // 初始化背景选择按钮选中状态映射
        sBgSelectorSelectionMap.put(ResourceParser.YELLOW, R.id.iv_bg_yellow_select);
        sBgSelectorSelectionMap.put(ResourceParser.RED, R.id.iv_bg_red_select);
        sBgSelectorSelectionMap.put(ResourceParser.BLUE, R.id.iv_bg_blue_select);
        sBgSelectorSelectionMap.put(ResourceParser.GREEN, R.id.iv_bg_green_select);
        sBgSelectorSelectionMap.put(ResourceParser.WHITE, R.id.iv_bg_white_select);
    }

    /**
     * 字号选择按钮与其对应字体大小的映射。
     */
    private static final Map<Integer, Integer> sFontSizeBtnsMap = new HashMap<Integer, Integer>();

    static {
        // 初始化字号选择按钮映射
        sFontSizeBtnsMap.put(R.id.ll_font_large, ResourceParser.TEXT_LARGE);
        sFontSizeBtnsMap.put(R.id.ll_font_small, ResourceParser.TEXT_SMALL);
        sFontSizeBtnsMap.put(R.id.ll_font_normal, ResourceParser.TEXT_MEDIUM);
        sFontSizeBtnsMap.put(R.id.ll_font_super, ResourceParser.TEXT_SUPER);
    }

    /**
     * 字号选择按钮选中状态与其对应图标的映射。
     */
    private static final Map<Integer, Integer> sFontSelectorSelectionMap = new HashMap<Integer, Integer>();

    static {
        // 初始化字号选择按钮选中状态映射
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_LARGE, R.id.iv_large_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SMALL, R.id.iv_small_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_MEDIUM, R.id.iv_medium_select);
        sFontSelectorSelectionMap.put(ResourceParser.TEXT_SUPER, R.id.iv_super_select);
    }

    private static final String TAG = "NoteEditActivity"; // 日志标签

    private HeadViewHolder mNoteHeaderHolder; // 头部视图的ViewHolder

    private View mHeadViewPanel; // 头部视图面板

    private View mNoteBgColorSelector; // 笔记背景颜色选择器

    private View mFontSizeSelector; // 字号选择器

    private View mNoteEditorPanel; // 笔记编辑器面板

    private WorkingNote mWorkingNote; // 当前正在编辑的笔记

    private SharedPreferences mSharedPrefs; // 共享偏好设置
    private int mFontSizeId; // 当前选中的字体大小资源ID

    private static final String PREFERENCE_FONT_SIZE = "pref_font_size"; // 字体大小偏好设置键

    private static final String PREFERENCE_FONT_TYPE = "PREFERENCE_FONT_TYPE";

    private static final int MAX_TIME_OF_RVOKE_TIME=100;


    private static final int SHORTCUT_ICON_TITLE_MAX_LEN = 10; // 快捷图标标题的最大长度

    public static final String TAG_CHECKED = String.valueOf('\u221A'); // 标记为已检查的字符串
    public static final String TAG_UNCHECKED = String.valueOf('\u25A1'); // 标记为未检查的字符串

    private LinearLayout mEditTextList; // 编辑文本列表

    private String mUserQuery; // 用户查询字符串

//    private Pattern mPattern; // 正则表达式模式

    private boolean mIsRvoke = false;  // 是否正在执行撤销操作的标志
    private Stack<SpannableString> mChanged = new Stack<>();  // 用于存储编辑历史的栈

    private long mNoteId;  // 添加便签ID成员变量

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.note_edit); // 设置活动的视图布局

        // 检查实例状态是否被保存，如果未保存且初始化活动状态失败，则结束该活动
        if (savedInstanceState == null && !initActivityState(getIntent())) {
            finish();
            return;
        }
        initResources(); // 初始化资源
        count();

        // 从 Intent 中获取便签 ID
        mNoteId = getIntent().getLongExtra(Notes.INTENT_EXTRA_NOTE_ID, 0);


    }

    /**
     * 当活动被系统销毁后，为了恢复之前的状态，此方法会被调用。
     * 主要用于处理活动重新创建时的数据恢复。
     *
     * @param savedInstanceState 包含之前保存状态的Bundle，如果活动之前保存了状态，这里会传入非空的Bundle。
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // 检查是否有保存的状态并且包含必要的UID信息，用于恢复活动状态
        if (savedInstanceState != null && savedInstanceState.containsKey(Intent.EXTRA_UID)) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.putExtra(Intent.EXTRA_UID, savedInstanceState.getLong(Intent.EXTRA_UID));
            // 使用intent尝试恢复活动状态，如果失败则结束该活动
            if (!initActivityState(intent)) {
                finish();
                return;
            }
            Log.d(TAG, "Restoring from killed activity"); // 日志记录，表示活动状态正在从被杀死的状态恢复
        }
    }


    /**
     * 初始化活动状态，根据传入的Intent确定是查看笔记、新建笔记还是编辑笔记。
     *
     * @param intent 传入的Intent，包含了动作类型和相关数据。
     * @return boolean 返回true表示成功初始化活动状态，false表示初始化失败。
     */
    private boolean initActivityState(Intent intent) {
        mWorkingNote = null;

        // 根据Intent的Action分发处理
        String action = intent.getAction();
        if (TextUtils.equals(Intent.ACTION_VIEW, action)) {
            return handleViewAction(intent);
        } else if (TextUtils.equals(Intent.ACTION_INSERT_OR_EDIT, action)) {
            return handleInsertOrEditAction(intent);
        } else {
            Log.e(TAG, "Intent not specified action, should not support");
            finish();
            return false;
        }
    }

    /**
     * 处理查看笔记的逻辑
     */
    private boolean handleViewAction(Intent intent) {
        long noteId = getNoteIdFromIntent(intent);
        if (!isNoteVisible(noteId)) {
            navigateToNotesList();
            return false;
        }

        mWorkingNote = WorkingNote.load(this, noteId);
        if (mWorkingNote == null) {
            Log.e(TAG, "load note failed with note id " + noteId);
            finish();
            return false;
        }

        // 隐藏软键盘
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN
                        | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        return true;
    }

    /**
     * 处理新建或编辑笔记的逻辑
     */
    private boolean handleInsertOrEditAction(Intent intent) {
        long folderId = intent.getLongExtra(Notes.INTENT_EXTRA_FOLDER_ID, 0);
        int widgetId = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        int widgetType = intent.getIntExtra(Notes.INTENT_EXTRA_WIDGET_TYPE, Notes.TYPE_WIDGET_INVALIDE);
        int bgResId = intent.getIntExtra(Notes.INTENT_EXTRA_BACKGROUND_ID, ResourceParser.getDefaultBgId(this));

        String phoneNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
        long callDate = intent.getLongExtra(Notes.INTENT_EXTRA_CALL_DATE, 0);

        if (callDate != 0 && phoneNumber != null) {
            return handleCallNote(phoneNumber, callDate, folderId, widgetId, widgetType, bgResId);
        } else {
            mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId, widgetType, bgResId);
        }

        // 显示软键盘
        getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
                        | WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        return true;
    }

    /**
     * 处理通话记录笔记的逻辑
     */
    private boolean handleCallNote(String phoneNumber, long callDate, long folderId, int widgetId, int widgetType, int bgResId) {
        if (TextUtils.isEmpty(phoneNumber)) {
            Log.w(TAG, "The call record number is null");
        }

        long noteId = DataUtils.getNoteIdByPhoneNumberAndCallDate(getContentResolver(), phoneNumber, callDate);
        if (noteId > 0) {
            mWorkingNote = WorkingNote.load(this, noteId);
            if (mWorkingNote == null) {
                Log.e(TAG, "load call note failed with note id " + noteId);
                finish();
                return false;
            }
        } else {
            mWorkingNote = WorkingNote.createEmptyNote(this, folderId, widgetId, widgetType, bgResId);
            mWorkingNote.convertToCallNote(phoneNumber, callDate);
        }
        return true;
    }

    /**
     * 从Intent中获取笔记ID
     */
    private long getNoteIdFromIntent(Intent intent) {
        long noteId = intent.getLongExtra(Intent.EXTRA_UID, 0);
        if (intent.hasExtra(SearchManager.EXTRA_DATA_KEY)) {
            noteId = Long.parseLong(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
            mUserQuery = intent.getStringExtra(SearchManager.USER_QUERY);
        }
        return noteId;
    }

    /**
     * 检查笔记是否可见
     */
    private boolean isNoteVisible(long noteId) {
        return DataUtils.visibleInNoteDatabase(getContentResolver(), noteId, Notes.TYPE_NOTE);
    }

    /**
     * 跳转到笔记列表并显示错误提示
     */
    private void navigateToNotesList() {
        Intent jump = new Intent(this, NotesListActivity.class);
        startActivity(jump);
        showToast(R.string.error_note_not_exist);
        finish();
    }


    /**
     * 当Activity恢复到前台时调用。
     * 主要负责初始化笔记界面。
     */
    @Override
    protected void onResume() {
        super.onResume();
        initNoteScreen();   // 初始化笔记界面
    }

    /**
     * 初始化笔记界面的函数。
     * 该函数设置笔记编辑器的外观，根据笔记类型切换到相应的模式，设置背景和头部信息，
     * 以及处理提醒头部的显示。
     */
    private void initNoteScreen() {
        // 设置编辑器的文本外观
        mNoteEditor.setTextAppearance(this, TextAppearanceResources
                .getTexAppearanceResource(mFontSizeId));
        // 恢复背景颜色
        Integer bgColorId = mWorkingNote.getBgColorId();
        Integer viewId = sBgSelectorSelectionMap.get(bgColorId);
        if (viewId != null) {
            findViewById(viewId).setVisibility(View.VISIBLE); // 恢复选择的背景颜色
        }
        // 根据当前笔记的类型，切换到列表模式或高亮查询结果模式
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            switchToListMode(mWorkingNote.getContent());
        } else {
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            mNoteEditor.setSelection(mNoteEditor.getText().length());
        }
        // 隐藏所有背景选择器
        for (Map.Entry<Integer, Integer> entry : sBgSelectorSelectionMap.entrySet()) {
            findViewById(entry.getValue()).setVisibility(View.GONE);
        }
        // 设置标题和编辑区域的背景
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());

        // 设置修改时间
        mNoteHeaderHolder.tvModified.setText(DateUtils.formatDateTime(this,
                mWorkingNote.getModifiedDate(), DateUtils.FORMAT_SHOW_DATE
                        | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_TIME
                        | DateUtils.FORMAT_SHOW_YEAR));

        // 显示提醒头部信息（当前禁用，因DateTimePicker未准备好）
        showAlertHeader();
    }

    /**
     * 显示提醒头部信息的方法。
     * 如果当前笔记设置了提醒，该方法将根据提醒时间显示相对的时间或者过期信息。
     */
    private void showAlertHeader() {
        // 处理提醒显示
        if (mWorkingNote.hasClockAlert()) {
            long time = System.currentTimeMillis();
            // 如果提醒时间已过，显示提醒已过期的信息
            if (time > mWorkingNote.getAlertDate()) {
                mNoteHeaderHolder.tvAlertDate.setText(R.string.note_alert_expired);
            } else {
                // 否则，显示相对时间
                mNoteHeaderHolder.tvAlertDate.setText(DateUtils.getRelativeTimeSpanString(
                        mWorkingNote.getAlertDate(), time, DateUtils.MINUTE_IN_MILLIS));
            }
            // 设置提醒视图可见
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.VISIBLE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.VISIBLE);
        } else {
            // 如果没有设置提醒，隐藏提醒视图
            mNoteHeaderHolder.tvAlertDate.setVisibility(View.GONE);
            mNoteHeaderHolder.ivAlertIcon.setVisibility(View.GONE);
        }
    }

    /**
     * 当Activity接收到新的Intent时调用。
     * 用于根据新的Intent重新初始化Activity状态。
     *
     * @param intent 新的Intent
     */
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        initActivityState(intent);
    }

    /**
     * 保存Activity状态时调用。
     * 用于保存当前编辑的笔记，如果该笔记还未保存到数据库，则首先保存它。
     * 并且保存当前笔记的ID到Bundle中。
     *
     * @param outState 用于保存Activity状态的Bundle
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // 如果当前编辑的笔记不存在于数据库中，即还未保存，先保存它
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }
        // 保存笔记ID
        outState.putLong(Intent.EXTRA_UID, mWorkingNote.getNoteId());
        Log.d(TAG, "Save working note id: " + mWorkingNote.getNoteId() + " onSaveInstanceState");
    }


    /**
     * 分发触摸事件。如果触摸事件不在指定视图范围内，则隐藏该视图。
     *
     * @param ev 触摸事件
     * @return 如果事件被消费则返回true，否则返回false
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 如果背景颜色选择器可见且不在触摸范围内，则隐藏它并消费事件
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mNoteBgColorSelector, ev)) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        }

        // 如果字体大小选择器可见且不在触摸范围内，则隐藏它并消费事件
        if (mFontSizeSelector.getVisibility() == View.VISIBLE
                && !inRangeOfView(mFontSizeSelector, ev)) {
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        // 如果上述条件都不满足，则将事件传递给父类处理
        return super.dispatchTouchEvent(ev);
    }

    /**
     * 判断触摸点是否在指定视图范围内。
     *
     * @param view 视图
     * @param ev   触摸事件
     * @return 如果触摸点在视图范围内则返回true，否则返回false
     */
    private boolean inRangeOfView(View view, MotionEvent ev) {
        int[] location = new int[2];
        view.getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        // 判断触摸点是否在视图外
        if (ev.getX() < x
                || ev.getX() > (x + view.getWidth())
                || ev.getY() < y
                || ev.getY() > (y + view.getHeight())) {
            return false;
        }
        return true;
    }

    /**
     * 初始化资源，包括视图和偏好设置。
     */
    private void initResources() {
        // 初始化头部视图和相关组件
        mHeadViewPanel = findViewById(R.id.note_title);
        mNoteHeaderHolder = new HeadViewHolder();
        mNoteHeaderHolder.tvModified = (TextView) findViewById(R.id.tv_modified_date);
        mNoteHeaderHolder.ivAlertIcon = (ImageView) findViewById(R.id.iv_alert_icon);
        mNoteHeaderHolder.tvAlertDate = (TextView) findViewById(R.id.tv_alert_date);
        mNoteHeaderHolder.ibSetBgColor = (ImageView) findViewById(R.id.btn_set_bg_color);
        mNoteHeaderHolder.editText = (EditText) findViewById(R.id.note_edit_view);
        mNoteHeaderHolder.textView = (TextView) findViewById(R.id.text_num);
        mNoteHeaderHolder.ibSetBgColor.setOnClickListener(this);
        // 初始化编辑器和相关组件
        mNoteEditor = (EditText) findViewById(R.id.note_edit_view);
        mNoteEditorPanel = findViewById(R.id.sv_note_edit);
        mNoteBgColorSelector = findViewById(R.id.note_bg_color_selector);
        // 设置背景选择器按钮点击监听器
        for (int id : sBgSelectorBtnsMap.keySet()) {
            ImageView iv = (ImageView) findViewById(id);
            iv.setOnClickListener(this);
        }

        mFontSizeSelector = findViewById(R.id.font_size_selector);
        // 设置字体大小选择器按钮点击监听器
        for (int id : sFontSizeBtnsMap.keySet()) {
            View view = findViewById(id);
            view.setOnClickListener(this);
        }
        ;
        // 从偏好设置中读取字体大小
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        mFontSizeId = mSharedPrefs.getInt(PREFERENCE_FONT_SIZE, ResourceParser.BG_DEFAULT_FONT_SIZE);
        // 修复存储在偏好设置中的字体大小资源ID的错误
        if (mFontSizeId >= TextAppearanceResources.getResourcesSize()) {
            mFontSizeId = ResourceParser.BG_DEFAULT_FONT_SIZE;
        }
        // 初始化编辑列表
        mEditTextList = (LinearLayout) findViewById(R.id.note_edit_list);

        // 从 SharedPreferences 中读取保存的字体类型
        String savedFontType = mSharedPrefs.getString(PREFERENCE_FONT_TYPE, "默认");

        // 根据保存的字体类型设置字体
        switch (savedFontType) {
            case "默认":
                mNoteEditor.setTypeface(Typeface.DEFAULT);
                break;
            case "行楷":
                mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STXINGKA.TTF"));
                break;
            case "新魏":
                mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STXINWEI.TTF"));
                break;
            case "隶书":
                mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STLITI.TTF"));
                break;
            case "黑体":
                mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STHEITI.TTF"));
                break;
            case "琥珀":
                mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STHUPO.TTF"));
                break;
            default:
                mNoteEditor.setTypeface(Typeface.DEFAULT);
                break;
        }

        mNoteEditor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
            @Override
            public void afterTextChanged(Editable s) {//文本更改后
                if(!mIsRvoke) {
                    saveMyChanged();
                }else {
                    mIsRvoke = false;
                }
            }
        });

        // 初始化撤销按钮
        ImageButton btnRevoke = (ImageButton) findViewById(R.id.btn_revoke);
        
        // 设置工具提示（适用于 API 26 以下的版本）
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) {
            btnRevoke.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    Toast.makeText(NoteEditActivity.this, R.string.revoke_tooltip, Toast.LENGTH_SHORT).show();
                    return true;
                }
            });
        }
        
        btnRevoke.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doRevoke();
            }
        });
    }

    private void saveMyChanged(){
        SpannableString text = new SpannableString(mNoteEditor.getText());//用 getText方法获取每次编辑的内容
        if(mChanged.size() >= MAX_TIME_OF_RVOKE_TIME){//如果栈中的数据大于最大撤销次数，就把第一次修改的内容删除
            mChanged.removeElementAt(0);
        }
        mChanged.add(text);//然后把本次修改的内容加入栈中
    }

    private void doRevoke(){
        int size = mChanged.size();//获取当前栈大小
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);//创建一个alertdialog 窗口
        dialog.setTitle(R.string.tips_of_revoke);//设置 title 信息
        dialog.setCancelable(true);//设置为可取消
        dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {//只需要设置一个 OK 键即可
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        mIsRvoke = true;//把是否已执行撤销的标记设置为 true
        if(size <= 1){//如果栈中元素过少，打印提示信息
            dialog.setMessage(R.string.have_not_input_anything);//提示用户您还没有输入任何信息
            dialog.show();//显示当前 alertdialog
            return;
        }
        else {
            mNoteEditor.setText((CharSequence) mChanged.elementAt(size-2));//在textview 中设置撤销的内容
            mNoteEditor.setSelection(mNoteEditor.length());
            mChanged.removeElementAt(size-1);//删除元素
            if(size == 2){
                dialog.setMessage(R.string.can_not_revoke);//如果只有一次操作，那么提示用户不能再撤销了
                dialog.show();//显示当前 alertdialog
            }
        }
    }


    /**
     * 暂停时保存笔记数据并清除设置状态。
     */
    @Override
    protected void onPause() {
        super.onPause();
        // 保存笔记数据
        if (saveNote()) {
            Log.d(TAG, "Note data was saved with length:" + mWorkingNote.getContent().length());
        }
        // 清除设置状态
        clearSettingState();
    }

    /**
     * 更新小部件显示。
     */
    private void updateWidget() {
        Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        // 根据小部件类型设置对应的类
        if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_2X) {
            intent.setClass(this, NoteWidgetProvider_2x.class);
        } else if (mWorkingNote.getWidgetType() == Notes.TYPE_WIDGET_4X) {
            intent.setClass(this, NoteWidgetProvider_4x.class);
        } else {
            Log.e(TAG, "Unspported widget type");
            return;
        }

        // 设置小部件ID
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{
                mWorkingNote.getWidgetId()
        });

        // 发送广播更新小部件
        sendBroadcast(intent);
        // 设置结果为OK
        setResult(RESULT_OK, intent);
    }


    /**
     * 点击事件的处理函数。
     *
     * @param v 被点击的视图对象。
     */
    public void onClick(View v) {
        int id = v.getId();
        // 如果点击的是设置背景颜色的按钮
        if (id == R.id.btn_set_bg_color) {
            mNoteBgColorSelector.setVisibility(View.VISIBLE);
            Integer bgColorId = mWorkingNote.getBgColorId();
            Integer viewId = sBgSelectorSelectionMap.get(bgColorId);
            if (viewId != null) {
                findViewById(viewId).setVisibility(View.VISIBLE);
            }
        } else if (sBgSelectorBtnsMap.containsKey(id)) {
            // 如果点击的是背景色板上的颜色
            Integer currentBgColorId = mWorkingNote.getBgColorId();
            Integer currentViewId = sBgSelectorSelectionMap.get(currentBgColorId);
            if (currentViewId != null) {
                findViewById(currentViewId).setVisibility(View.GONE); // 隐藏当前选择的背景颜色
            }

            Integer newBgColorId = sBgSelectorBtnsMap.get(id);
            if (newBgColorId != null) {
                mWorkingNote.setBgColorId(newBgColorId); // 更新笔记的背景颜色
            }

            mNoteBgColorSelector.setVisibility(View.GONE); // 隐藏背景颜色选择器
        } else if (sFontSizeBtnsMap.containsKey(id)) {
            // 如果点击的是字体大小按钮
            Integer currentFontSizeId = sFontSelectorSelectionMap.get(mFontSizeId);
            if (currentFontSizeId != null) {
                findViewById(currentFontSizeId).setVisibility(View.GONE); // 隐藏当前选择的字体大小
            }

            Integer newFontSizeId = sFontSizeBtnsMap.get(id);
            if (newFontSizeId != null) {
                mFontSizeId = newFontSizeId; // 更新选择的字体大小
                mSharedPrefs.edit().putInt(PREFERENCE_FONT_SIZE, mFontSizeId).apply(); // 使用apply()保存选择的字体大小到SharedPreferences

                Integer newFontSizeViewId = sFontSelectorSelectionMap.get(mFontSizeId);
                if (newFontSizeViewId != null) {
                    findViewById(newFontSizeViewId).setVisibility(View.VISIBLE); // 设置新选择的字体大小按钮为可见
                }
            }

            // 根据当前笔记是否为清单模式，进行相应的文本更新
            if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
                getWorkingText();
                switchToListMode(mWorkingNote.getContent());
            } else {
                mNoteEditor.setTextAppearance(this,
                        TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
            }
            mFontSizeSelector.setVisibility(View.GONE); // 隐藏字体大小选择器
        }
        // 如果点击的是设置字体类型的按钮
        if (id == R.id.menu_font_size) {
            showSingleAlertDiglog();  // 显示字体类型选择对话框
        }
    }

    /**
     * 按下返回键时的处理函数。
     */
    @Override
    public void onBackPressed() {
        // 尝试清除设置状态，如果成功，则不执行保存笔记操作
        if (clearSettingState()) {
            return;
        }
        // 保存笔记并执行父类的onBackPressed()方法（结束当前Activity）
        saveNote();
        super.onBackPressed();
    }

    /**
     * 尝试清除设置状态（背景颜色选择或字体大小选择）。
     *
     * @return 如果成功清除设置状态，返回true；否则返回false。
     */
    private boolean clearSettingState() {
        // 如果背景颜色选择器可见，则隐藏它并返回true
        if (mNoteBgColorSelector.getVisibility() == View.VISIBLE) {
            mNoteBgColorSelector.setVisibility(View.GONE);
            return true;
        } else if (mFontSizeSelector.getVisibility() == View.VISIBLE) { // 如果字体大小选择器可见，则隐藏它并返回true
            mFontSizeSelector.setVisibility(View.GONE);
            return true;
        }
        return false; // 没有可见的设置状态需要清除，返回false
    }

    /**
     * 当背景颜色发生变化时的处理函数。
     */
    public void onBackgroundColorChanged() {
        // 根据当前笔记的背景颜色，设置对应的色板按钮可见，并更新编辑器及标题栏的背景颜色
        findViewById(sBgSelectorSelectionMap.get(mWorkingNote.getBgColorId())).setVisibility(
                View.VISIBLE);
        mNoteEditorPanel.setBackgroundResource(mWorkingNote.getBgColorResId());
        mHeadViewPanel.setBackgroundResource(mWorkingNote.getTitleBgResId());
    }

    /**
     * 准备选项菜单的函数。
     *
     * @param menu 选项菜单对象。
     * @return 总是返回true。
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        // 如果Activity正在结束，则不进行任何操作
        if (isFinishing()) {
            return true;
        }
        // 尝试清除设置状态
        clearSettingState();
        menu.clear(); // 清除菜单项
        // 根据笔记所属的文件夹，加载不同的菜单资源
        if (mWorkingNote.getFolderId() == Notes.ID_CALL_RECORD_FOLDER) {
            getMenuInflater().inflate(R.menu.call_note_edit, menu);
        } else {
            getMenuInflater().inflate(R.menu.note_edit, menu);
        }
        // 根据当前笔记的清单模式，更新"清单模式"菜单项的标题
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_normal_mode);
        } else {
            menu.findItem(R.id.menu_list_mode).setTitle(R.string.menu_list_mode);
        }
        // 根据笔记是否有提醒，更新"删除提醒"菜单项的可见性
        if (mWorkingNote.hasClockAlert()) {
            menu.findItem(R.id.menu_alert).setVisible(false);
        } else {
            menu.findItem(R.id.menu_delete_remind).setVisible(false);
        }
        return true;
    }


    /**
     * 处理选项菜单项的选择事件。
     *
     * @param item 选中的菜单项
     * @return 总是返回true，表示事件已处理。
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_new_note) {
            // 创建新笔记
            createNewNote();
        } else if (item.getItemId() == R.id.menu_delete) {
            // 显示删除笔记的确认对话框
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(getString(R.string.alert_title_delete));
            builder.setIcon(android.R.drawable.ic_dialog_alert);
            builder.setMessage(getString(R.string.alert_message_delete_note));
            builder.setPositiveButton(android.R.string.ok,
                    (dialog, which) -> {
                        // 确认删除当前笔记并将其移动到回收站
                        moveNoteToTrash();
                        finish();
                    });
            builder.setNegativeButton(android.R.string.cancel, null);
            builder.show();
        } else if (item.getItemId() == R.id.menu_font_size) {
            mFontSizeSelector.setVisibility(View.VISIBLE);
            findViewById(sFontSelectorSelectionMap.get(mFontSizeId)).setVisibility(View.VISIBLE);
        } else if (item.getItemId() == R.id.menu_font_type) {
            // 隐藏字体大小选择器
            mFontSizeSelector.setVisibility(View.GONE);
            // 显示字体编辑器
            showSingleAlertDiglog();
        } else if (item.getItemId() == R.id.menu_list_mode) {
            // 切换笔记的列表模式
            mWorkingNote.setCheckListMode(mWorkingNote.getCheckListMode() == 0 ?
                    TextNote.MODE_CHECK_LIST : 0);
        } else if (item.getItemId() == R.id.menu_share) {
            // 获取当前编辑的笔记内容并分享
            getWorkingText();
            sendTo(this, mWorkingNote.getContent());
        } else if (item.getItemId() == R.id.menu_send_to_desktop) {
            // 将笔记发送到桌面
            sendToDesktop();
        } else if (item.getItemId() == R.id.menu_alert) {
            // 设置提醒
            setReminder();
        } else if (item.getItemId() == R.id.menu_delete_remind) {
            // 删除提醒设置
            mWorkingNote.setAlertDate(0, false);
        } else if (item.getItemId() == MENU_RESTORE) {
            restoreFromTrash();
        } else {
            return super.onOptionsItemSelected(item);
        }
        return true;
    }

    /**
     * 将当前笔记移动到回收站
     * 该方法执行以下操作：
     * 1. 检查笔记是否存在于数据库
     * 2. 将笔记ID添加到待处理集合
     * 3. 调用DataUtils将笔记批量移动到回收站
     * 4. 更新UI和状态
     * 
     * 安全性考虑：
     * - 使用LocalBroadcastManager确保广播仅在应用内部传递
     * - 添加包名限制，防止其他应用接收广播
     * 
     * @throws IllegalStateException 如果笔记ID无效
     */
    private void moveNoteToTrash() {
        if (mWorkingNote.existInDatabase()) {
            HashSet<Long> ids = new HashSet<Long>();
            long id = mWorkingNote.getNoteId();
            if (id != Notes.ID_ROOT_FOLDER) {
                ids.add(id);
            } else {
                Log.d(TAG, "Wrong note id, should not happen");
                return;
            }

            // 将笔记移动到回收站
            if (!DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_TRASH_FOLER)) {
                Log.e(TAG, "Move notes to trash folder error");
                Toast.makeText(this, R.string.error_note_empty_for_trash, 
                        Toast.LENGTH_SHORT).show();
            } else {
                // 移动成功，更新UI和状态
                Toast.makeText(this, R.string.note_moved_to_trash, Toast.LENGTH_SHORT).show();
                
                // 标记笔记为已删除状态
                mWorkingNote.markDeleted(true);
                
                // 使用LocalBroadcastManager发送应用内广播
                // 通知其他组件数据已更改
                Intent intent = new Intent(Intent.ACTION_PROVIDER_CHANGED, Notes.CONTENT_NOTE_URI);
                intent.setPackage(getPackageName());  // 限制接收者为本应用
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
            }
        }
    }


    public void showSingleAlertDiglog() {
        final String[] items = {"默认", "行楷", "新魏", "隶书", "黑体", "琥珀"};  // 添加新的字体选项
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择字体");
        builder.setSingleChoiceItems(items, -1, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (items[which]) {
                    case "默认":
                        mNoteEditor.setTypeface(Typeface.DEFAULT);
                        break;
                    case "行楷":
                        mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STXINGKA.TTF"));
                        break;
                    case "新魏":
                        mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STXINWEI.TTF"));
                        break;
                    case "隶书":
                        mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STLITI.TTF"));
                        break;
                    case "黑体":
                        mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STHEITI.TTF"));
                        break;
                    case "琥珀":
                        mNoteEditor.setTypeface(Typeface.createFromAsset(getAssets(), "Font/STHUPO.TTF"));
                        break;
                }
                // 保存字体选择到 SharedPreferences
                mSharedPrefs.edit().putString(PREFERENCE_FONT_TYPE, items[which]).apply();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.show();
    }





    /**
     * 弹出日期时间选择器，用于设置提醒时间。
     */
    private void setReminder() {
        DateTimePickerDialog d = new DateTimePickerDialog(this, System.currentTimeMillis());
        d.setOnDateTimeSetListener(new OnDateTimeSetListener() {
            public void OnDateTimeSet(AlertDialog dialog, long date) {
                // 用户设定时间后，设置提醒
                mWorkingNote.setAlertDate(date, true);
            }
        });
        d.show();
    }

    /**
     * 分享笔记到支持 {@link Intent#ACTION_SEND} 操作和 {@text/plain} 类型的应用。
     *
     * @param context 上下文
     * @param info    要分享的信息
     */
    private void sendTo(Context context, String info) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_TEXT, info);
        intent.setType("text/plain");
        context.startActivity(intent);
    }

    /**
     * 首先保存当前正在编辑的笔记，然后启动一个新的NoteEditActivity用于创建新笔记。
     */
    private void createNewNote() {
        // 首先保存当前笔记
        saveNote();

        // 安全地开始一个新的NoteEditActivity
        finish();
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_INSERT_OR_EDIT);
        intent.putExtra(Notes.INTENT_EXTRA_FOLDER_ID, mWorkingNote.getFolderId());
        startActivity(intent);
    }


    /**
     * 删除当前正在编辑的笔记。
     * 如果笔记存在于数据库中，并且当前不是同步模式，将直接删除该笔记；
     * 如果处于同步模式，则将笔记移动到回收站文件夹。
     */
    private void deleteCurrentNote() {
        if (mWorkingNote.existInDatabase()) {
            HashSet<Long> ids = new HashSet<Long>();
            long id = mWorkingNote.getNoteId();
            if (id != Notes.ID_ROOT_FOLDER) {
                ids.add(id);
            } else {
                Log.d(TAG, "Wrong note id, should not happen");
            }
            if (!isSyncMode()) {
                // 非同步模式下直接删除笔记
                if (!DataUtils.batchDeleteNotes(getContentResolver(), ids)) {
                    Log.e(TAG, "Delete Note error");
                }
            } else {
                // 同步模式下将笔记移动到回收站
                if (!DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_TRASH_FOLER)) {
                    Log.e(TAG, "Move notes to trash folder error, should not happens");
                }
            }
        }
        mWorkingNote.markDeleted(true);
    }

    /**
     * 判断当前是否为同步模式。
     * 同步模式是指在设置中配置了同步账户名。
     *
     * @return 如果配置了同步账户名返回true，否则返回false。
     */
    private boolean isSyncMode() {
        return NotesPreferenceActivity.getSyncAccountName(this).trim().length() > 0;
    }

    /**
     * 处理时钟提醒变更事件。
     * 首先检查当前笔记是否已保存，未保存则先保存。
     * 如果笔记存在，根据set参数设置或取消提醒。
     * 如果笔记不存在（即无有效ID），记录错误并提示用户输入内容。
     *
     * @param date 提醒的日期时间戳
     * @param set  是否设置提醒
     */
    public void onClockAlertChanged(long date, boolean set) {
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }
        if (mWorkingNote.getNoteId() > 0) {
            Intent intent = new Intent(this, AlarmReceiver.class);
            intent.setData(ContentUris.withAppendedId(Notes.CONTENT_NOTE_URI, mWorkingNote.getNoteId()));
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            AlarmManager alarmManager = ((AlarmManager) getSystemService(ALARM_SERVICE));
            showAlertHeader();
            if (!set) {
                alarmManager.cancel(pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, date, pendingIntent);
            }
        } else {
            Log.e(TAG, "Clock alert setting error");
            showToast(R.string.error_note_empty_for_clock);
        }
    }

    /**
     * 更新小部件显示。
     */
    public void onWidgetChanged() {
        updateWidget();
    }

    /**
     * 当删除某个编辑框中的文本时的处理逻辑。
     * 重新设置后续编辑框的索引，并将删除的文本添加到前一个或当前编辑框中。
     *
     * @param index 被删除文本的编辑框索引
     * @param text  被删除的文本内容
     */
    public void onEditTextDelete(int index, String text) {
        int childCount = mEditTextList.getChildCount();
        if (childCount == 1) {
            return;
        }

        for (int i = index + 1; i < childCount; i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i - 1);
        }

        mEditTextList.removeViewAt(index);
        NoteEditText edit = null;
        if (index == 0) {
            edit = (NoteEditText) mEditTextList.getChildAt(0).findViewById(
                    R.id.et_edit_text);
        } else {
            edit = (NoteEditText) mEditTextList.getChildAt(index - 1).findViewById(
                    R.id.et_edit_text);
        }
        int length = edit.length();
        edit.append(text);
        edit.requestFocus();
        edit.setSelection(length);
    }

    /**
     * 当在编辑框中按下"Enter"键时的处理逻辑。
     * 在列表中添加一个新的编辑框，并重新设置后续编辑框的索引。
     *
     * @param index 当前编辑框的索引
     * @param text  当前编辑框中的文本内容
     */
    public void onEditTextEnter(int index, String text) {
        if (index > mEditTextList.getChildCount()) {
            Log.e(TAG, "Index out of mEditTextList boundrary, should not happen");
        }

        View view = getListItem(text, index);
        mEditTextList.addView(view, index);
        NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        edit.requestFocus();
        edit.setSelection(0);
        for (int i = index + 1; i < mEditTextList.getChildCount(); i++) {
            ((NoteEditText) mEditTextList.getChildAt(i).findViewById(R.id.et_edit_text))
                    .setIndex(i);
        }
    }


    /**
     * 切换到列表模式。
     * 将文本分割成多行，并为每行创建一个列表项，展示在编辑文本列表中。
     *
     * @param text 要转换成列表模式的文本，每行代表一个列表项。
     */
    private void switchToListMode(String text) {
        // 清空当前的视图
        mEditTextList.removeAllViews();
        // 使用换行符分割文本，创建列表项
        String[] items = text.split("\n");
        int index = 0;
        for (String item : items) {
            // 忽略空行
            if (!TextUtils.isEmpty(item)) {
                mEditTextList.addView(getListItem(item, index));
                index++;
            }
        }
        // 添加一个空的列表项作为占位符
        mEditTextList.addView(getListItem("", index));
        // 请求焦点以便于编辑
        mEditTextList.getChildAt(index).findViewById(R.id.et_edit_text).requestFocus();

        // 隐藏编辑器，显示列表
        mNoteEditor.setVisibility(View.GONE);
        mEditTextList.setVisibility(View.VISIBLE);
    }

    /**
     * 高亮显示查询结果。
     * 在给定的文本中，根据用户查询字符串高亮显示匹配的部分。
     *
     * @param fullText  完整的文本。
     * @param userQuery 用户的查询字符串。
     * @return 包含高亮显示的文本的Spannable对象。
     */
    private Spannable getHighlightQueryResult(String fullText, String userQuery) {
        // 如果 fullText 为 null，则使用空字符串
        SpannableString spannable = new SpannableString(fullText == null ? "" : fullText);

        // 如果查询字符串不为空，则进行高亮处理
        if (!TextUtils.isEmpty(userQuery)) {
            Pattern pattern = Pattern.compile(Pattern.quote(userQuery)); // 使用 Pattern.quote 防止特殊字符干扰
            Matcher matcher = pattern.matcher(fullText);

            while (matcher.find()) {
                // 设置高亮背景
                spannable.setSpan(
                        new BackgroundColorSpan(this.getResources().getColor(R.color.user_query_highlight)),
                        matcher.start(),
                        matcher.end(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE // 使用 Spanned 而非 Spannable
                );
            }
        }

        return spannable;
    }

    /**
     * 创建列表项视图。
     * 为列表模式创建并配置一个包含文本编辑框和复选框的视图。
     *
     * @param item  列表项的文本内容。
     * @param index 列表项的索引。
     * @return 配置好的列表项视图。
     */
    private View getListItem(String item, int index) {
        // 加载列表项布局
        View view = LayoutInflater.from(this).inflate(R.layout.note_edit_list_item, null);
        final NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
        // 设置文本样式
        edit.setTextAppearance(this, TextAppearanceResources.getTexAppearanceResource(mFontSizeId));
        CheckBox cb = ((CheckBox) view.findViewById(R.id.cb_edit_item));
        // 复选框的监听器，用于切换文本的划线状态
        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
                }
            }
        });

        // 根据文本前缀设置复选框状态和文本内容
        if (item.startsWith(TAG_CHECKED)) {
            cb.setChecked(true);
            edit.setPaintFlags(edit.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
            item = item.substring(TAG_CHECKED.length(), item.length()).trim();
        } else if (item.startsWith(TAG_UNCHECKED)) {
            cb.setChecked(false);
            edit.setPaintFlags(Paint.ANTI_ALIAS_FLAG | Paint.DEV_KERN_TEXT_FLAG);
            item = item.substring(TAG_UNCHECKED.length(), item.length()).trim();
        }

        // 设置文本变化监听和索引
        edit.setOnTextViewChangeListener(this);
        edit.setIndex(index);
        // 设置带有查询结果高亮的文本
        edit.setText(getHighlightQueryResult(item, mUserQuery));
        return view;
    }

    /**
     * 根据文本内容是否为空，切换复选框的可见性。
     *
     * @param index   列表项索引。
     * @param hasText 列表项是否包含文本。
     */
    public void onTextChange(int index, boolean hasText) {
        if (index >= mEditTextList.getChildCount()) {
            Log.e(TAG, "Wrong index, should not happen");
            return;
        }
        // 根据文本内容决定复选框的可见性
        if (hasText) {
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.VISIBLE);
        } else {
            mEditTextList.getChildAt(index).findViewById(R.id.cb_edit_item).setVisibility(View.GONE);
        }
    }
    private void count(){
        mNoteEditor.addTextChangedListener(new TextWatcher() {
            int currentLength=0;
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mNoteHeaderHolder.textView.setText("字符数：" + currentLength);
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentLength=mNoteHeaderHolder.editText.getText().length();
            }
            @Override
            public void afterTextChanged(Editable s) {
                mNoteHeaderHolder.textView.setText("字符数："+currentLength);
            }
        });
    }
    private String Textchange(String oriText){
        StringBuffer stringBuffer = new StringBuffer(oriText);
        int Flag1 = -1;
        int Flag2 = -1;
        do {//不计入表示图片的字符
            Flag1 = stringBuffer.indexOf("<img");
            Flag2 = stringBuffer.indexOf(">");
            if (Flag1 != -1 && Flag2 != -1) {
                stringBuffer = stringBuffer.replace(Flag1, Flag2+1, "");
            }
        } while (Flag1 != -1 && Flag2 != -1);

        do {//不计入换行字符
            Flag1 = stringBuffer.indexOf("\n");

            if (Flag1 != -1){
                stringBuffer = stringBuffer.replace(Flag1, Flag1+1, "");
            }
        } while (Flag1 != -1);
        do {//不计入空格字符
            Flag1 = stringBuffer.indexOf(" ");

            if (Flag1 != -1) {
                stringBuffer = stringBuffer.replace(Flag1, Flag1+1, "");
            }
        } while (Flag1 != -1);
        return stringBuffer.toString();
    }

    /**
     * 在切换编辑模式和列表模式时更新UI。
     *
     * @param oldMode 旧的编辑模式。
     * @param newMode 新的编辑模式。
     */
    public void onCheckListModeChanged(int oldMode, int newMode) {
        // 切换到列表模式
        if (newMode == TextNote.MODE_CHECK_LIST) {
            switchToListMode(mNoteEditor.getText().toString());
        } else {
            // 切换回编辑模式
            if (!getWorkingText()) {
                mWorkingNote.setWorkingText(mWorkingNote.getContent().replace(TAG_UNCHECKED + " ",
                        ""));
            }
            mNoteEditor.setText(getHighlightQueryResult(mWorkingNote.getContent(), mUserQuery));
            mEditTextList.setVisibility(View.GONE);
            mNoteEditor.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 根据当前列表项的选中状态，构建并返回工作文本。
     *
     * @return 是否有已选中的列表项。
     */
    private boolean getWorkingText() {
        boolean hasChecked = false;
        if (mWorkingNote.getCheckListMode() == TextNote.MODE_CHECK_LIST) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mEditTextList.getChildCount(); i++) {
                View view = mEditTextList.getChildAt(i);
                NoteEditText edit = (NoteEditText) view.findViewById(R.id.et_edit_text);
                // 构建带有选中状态前缀的文本
                if (!TextUtils.isEmpty(edit.getText())) {
                    if (((CheckBox) view.findViewById(R.id.cb_edit_item)).isChecked()) {
                        sb.append(TAG_CHECKED).append(" ").append(edit.getText()).append("\n");
                        hasChecked = true;
                    } else {
                        sb.append(TAG_UNCHECKED).append(" ").append(edit.getText()).append("\n");
                    }
                }
            }
            mWorkingNote.setWorkingText(sb.toString());
        } else {
            mWorkingNote.setWorkingText(mNoteEditor.getText().toString());
        }
        return hasChecked;
    }

    /**
     * 保存笔记。
     * 更新笔记内容并保存。
     *
     * @return 是否成功保存笔记。
     */
    private boolean saveNote() {
        getWorkingText();
        boolean saved = mWorkingNote.saveNote();
        if (saved) {
            // 设置结果为成功，以便外部调用者知道保存操作的状态
            setResult(RESULT_OK);
        }
        return saved;
    }


    /**
     * 将当前编辑的笔记发送到桌面。首先会检查当前编辑的笔记是否已存在于数据库中，
     * 如果不存在，则先保存。如果存在，会创建一个快捷方式放在桌面。
     */
    private void sendToDesktop() {
        // 检查当前编辑的笔记是否存在于数据库，若不存在则先保存
        if (!mWorkingNote.existInDatabase()) {
            saveNote();
        }

        // 如果笔记存在于数据库（有noteId），则创建快捷方式
        if (mWorkingNote.getNoteId() > 0) {
            ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);

            if (shortcutManager != null) {
                ShortcutInfo shortcutInfo = new ShortcutInfo.Builder(this, String.valueOf(mWorkingNote.getNoteId()))
                        .setShortLabel(makeShortcutIconTitle(mWorkingNote.getContent()))
                                .setLongLabel(makeShortcutIconTitle(mWorkingNote.getContent()))
                                        .setIcon(Icon.createWithResource(this, R.drawable.icon_app))
                                        .setIntent(new Intent(this, NoteEditActivity.class)
                                                .setAction(Intent.ACTION_VIEW)
                                                .putExtra(Intent.EXTRA_UID, mWorkingNote.getNoteId()))
                                        .build();

                shortcutManager.setDynamicShortcuts(Collections.singletonList(shortcutInfo));
                showToast(R.string.info_note_enter_desktop);
            }
        } else {
            // 如果用户未输入任何内容，无法创建快捷方式，提醒用户输入内容
            Log.e(TAG, "Send to desktop error");
            showToast(R.string.error_note_empty_for_send_to_desktop);
        }
    }

    /**
     * 根据笔记内容生成快捷方式的标题。移除内容中的已选和未选标签，并确保标题长度不超过上限。
     *
     * @param content 符合快捷方式图标标题要求的笔记内容
     * @return 标题字符串
     */
    private String makeShortcutIconTitle(String content) {
        content = content.replace(TAG_CHECKED, "");
        content = content.replace(TAG_UNCHECKED, "");
        return content.length() > SHORTCUT_ICON_TITLE_MAX_LEN ? content.substring(0,
                SHORTCUT_ICON_TITLE_MAX_LEN) : content;
    }

    /**
     * 显示一个Toast消息。
     *
     * @param resId 资源ID，指向要显示的字符串
     */
    private void showToast(int resId) {
        showToast(resId, Toast.LENGTH_SHORT);
    }

    /**
     * 显示一个指定时长的Toast消息。
     *
     * @param resId    资源ID，指向要显示的字符串
     * @param duration 显示时长，可以是Toast.LENGTH_SHORT或Toast.LENGTH_LONG
     */
    private void showToast(int resId, int duration) {
        Toast.makeText(this, resId, duration).show();
    }


    private static final int MENU_RESTORE = Menu.FIRST + 10;

    private void restoreFromTrash() {
        // 从回收站恢复便签的逻辑
        if (mWorkingNote != null) {
            HashSet<Long> ids = new HashSet<Long>();
            ids.add(mWorkingNote.getNoteId());
            
            // 将便签移回普通文件夹
            if (DataUtils.batchMoveToFolder(getContentResolver(), ids, Notes.ID_ROOT_FOLDER)) {
                Toast.makeText(this, R.string.note_restored, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }
}




