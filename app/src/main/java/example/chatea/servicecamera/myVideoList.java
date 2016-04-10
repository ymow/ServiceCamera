package example.chatea.servicecamera;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.LinkedList;

public class MyVideoList extends ActionBarActivity {

    private ListView lv_myVideoList;
    private Intent intent;
    private String videoPath;
    private ArrayList mVideoList;
    private String[] arrayVideoLisy;
    private Cursor mVideoCursor;
    private int video_column_index;
    public static final String TAG = "MyVideoList";

    @SuppressWarnings("deprecation")
    private void init_phone_video_grid() {
        System.gc();
        String[] proj = { MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE };
        mVideoCursor = managedQuery(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                proj, null, null, null);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_video_list);
        lv_myVideoList = (ListView) findViewById(R.id.my_video_listview);
        mVideoList = new ArrayList<String>();
//        printNamesToLogCat(this);
        init_phone_video_grid();
        mVideoListAdapter adapter = new mVideoListAdapter();
        Log.i(TAG, mVideoList.toString());
        lv_myVideoList.setAdapter(adapter);
    }

    public class mVideoListAdapter extends BaseAdapter {

        private Context context;
        LayoutInflater mLayoutInflater;

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return mVideoCursor.getCount();
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            View v = convertView;
            Holder holder;
            String id = null;
            if (v == null) {
                v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.my_video_list_item, null);
                holder = new Holder();

                holder.videoName = (TextView) v.findViewById(R.id.videoname);
                holder.videoThumbnail = (ImageView) v.findViewById(R.id.thumbnail);
                v.setTag(holder);
            } else {
                holder = (Holder) v.getTag();
            }

//            video_column_index = mVideoCursor.getCount()
//                    .get(MediaStore.Video.Media.DISPLAY_NAME);
            mVideoCursor.moveToPosition(position);
            id = mVideoCursor.getString(video_column_index);
            video_column_index = mVideoCursor
                    .getColumnIndexOrThrow(MediaStore.Video.Media.SIZE);
            mVideoCursor.moveToPosition(position);
             id += " Size(KB):" +
             mVideoCursor.getString(video_column_index);
            holder.videoName.setText(id);
//            holder.txtSize.setText(" Size(KB):"
//                    + videocursor.getString(video_column_index));

            String[] proj = { MediaStore.Video.Media._ID,
                    MediaStore.Video.Media.DISPLAY_NAME,
                    MediaStore.Video.Media.DATA };
            @SuppressWarnings("deprecation")
            Cursor cursor = managedQuery(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj,
                    MediaStore.Video.Media.DISPLAY_NAME + "=?",
                    new String[] { id }, null);
            cursor.moveToFirst();
            long ids = cursor.getLong(cursor
                    .getColumnIndex(MediaStore.Video.Media._ID));

            ContentResolver crThumb = getContentResolver();
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1;
            Bitmap curThumb = MediaStore.Video.Thumbnails.getThumbnail(
                    crThumb, ids, MediaStore.Video.Thumbnails.MICRO_KIND,
                    options);
            holder.videoThumbnail.setImageBitmap(curThumb);
            curThumb = null;

//
//            String[] proj = { MediaStore.Video.Media._ID,
//                    MediaStore.Video.Media.DISPLAY_NAME,
//                    MediaStore.Video.Media.DATA };
//            @SuppressWarnings("deprecation")
//            Cursor cursor = managedQuery(
//                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj,
//                    MediaStore.Video.Media.DISPLAY_NAME + "=?",
//                    new String[] { id }, null);
//            cursor.moveToFirst();
//            long ids = cursor.getLong(cursor
//                    .getColumnIndex(MediaStore.Video.Media._ID));
//
//            ContentResolver crThumb = getContentResolver();
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inSampleSize = 1;
//            Bitmap curThumb = MediaStore.Video.Thumbnails.getThumbnail(
//                    crThumb, ids, MediaStore.Video.Thumbnails.MICRO_KIND,
//                    options);
//            holder.thumbImage.setImageBitmap(curThumb);
//            curThumb = null;

//            Bitmap bmThumbnail;
//            bmThumbnail = ThumbnailUtils.createVideoThumbnail(mVideoCursor.get(position).toString(), MediaStore.Video.Thumbnails.MINI_KIND);
//            ContentResolver crThumb = getContentResolver();
//            BitmapFactory.Options options=new BitmapFactory.Options();
//            options.inSampleSize = 1;
//            Bitmap bmThumbnail = MediaStore.Video.Thumbnails.getThumbnail(crThumb, position, MediaStore.Video.Thumbnails.MICRO_KIND, options);
//            holder.videoThumbnail.setImageBitmap(bmThumbnail);
            holder.videoThumbnail.setImageDrawable(getDrawable(R.mipmap.ic_launcher));
            holder.videoName.setText(mVideoList.get(position).toString());

            return v;
        }

        class Holder {
            TextView videoName;
            ImageView videoThumbnail;
            String id = null;
        }
    }

    public void printNamesToLogCat(Context context) {
        Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        String[] projection = { MediaStore.Video.VideoColumns.DATA };
        String[] proj = { MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.SIZE };
        mVideoCursor = context.getContentResolver().query(uri, projection, null, null, null);
//        int vidsCount = 0;
//        if (c != null) {
//            vidsCount = c.getCount();
//            while (c.moveToPosition(po)) {
//                Log.d("VIDEO", c.getString(0));
//                mVideoList.add(c.getString(0));
//            }
//            c.close();
//        }
    }

}
