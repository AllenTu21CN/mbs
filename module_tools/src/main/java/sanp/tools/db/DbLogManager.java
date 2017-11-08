package sanp.tools.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import java.util.ArrayList;
import java.util.List;

import sanp.tools.utils.DBManager;
import sanp.tools.utils.LogManager;

/**
 * Created by Tom on 2017/3/10.
 */

public class DbLogManager {
    private SQLiteDatabase db;
    private Context mContext;

    public DbLogManager(Context mContext) {
        this.mContext = mContext;
    }

    public boolean insertInfo(OperateLogInfo info) {
        db = DBManager.getAppDatabase();
        if (db == null) {
            LogManager.e("数据库调用失败");
            return false;
        }

        String operateContent = info.getOperateContent();  // 操作日志内容
        String operateTime = info.getOperateTime(); //  操作日志时间
        String sql = "insert into logs (operate_content,operate_time)"
                + " values ('"
                + operateContent
                + "',"
                + "'"
                + operateTime
                + "')";
        try {
            LogManager.i("插入：" + sql);
            db.execSQL(sql);
            db.close();
            return true;
        } catch (SQLiteException e) {
            LogManager.e("插入异常" + e.getMessage());
        }
        return false;
    }

    public List<OperateLogInfo> selectorInfo(int nowPage, int nowNum) {
        List<OperateLogInfo> infoList = new ArrayList<>();
        db = DBManager.getAppDatabase();
        if (nowPage == 0) {
            nowPage = 1;
        }
        int selectMaxId = nowNum * nowPage;
        int selectMinId = selectMaxId - nowNum;
        String sql = "select * from logs where id < selectMaxId and id > selectMinId";
        Cursor cursor = db.rawQuery(sql, null);
        if (cursor == null) {
            LogManager.e("cursor is null");
            return null;
        }
        while (cursor.moveToNext()) {
            OperateLogInfo info = new OperateLogInfo();
            String operate_content = cursor.getString(cursor
                    .getColumnIndex("operate_content"));
            String operate_time = cursor.getString(cursor
                    .getColumnIndex("operate_time"));
            info.setOperateContent(operate_content);
            info.setOperateTime(operate_time);
            infoList.add(info);
        }
        return infoList;
    }
}
