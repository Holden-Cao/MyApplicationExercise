package com.example.administrator.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.provider.MediaStore.Images.Media;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.administrator.myapplication.R;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends AppCompatActivity {

    private String cameraPath;
    private Button btn;
    private String imgFileName;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private static final int REQUEST_CALL_PHONE = 100;
    private static final String TAG = "testPermission";

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn = (Button) findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inspectPermission();
            }
        });
    }

    //检查权限问题
    private void inspectPermission() {
        // 如果SDK版本大于或等于23才需要检查权限，否则直接拨弹出图库
        if (Build.VERSION.SDK_INT >= 23) {
            // 检查权限是否允许
            if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.e("TAG", "没有权限");
                // 没有权限，考虑是否需要解释为什么需要这个权限
                /*申请权限的解释，该方法在用户上一次已经拒绝过你的这个权限申请时使用。
                * 也就是说，用户已经拒绝一次了，你又弹个授权框，你需要给用户一个解释，为什么要授权*/
                if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    Log.e("TAG", "没有权限,用户上次已经拒绝该权限，解释为什么需要这个权限");
                    // Show an expanation to the user *asynchronously* -- don't block this thread
                    //waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    Toast.makeText(MainActivity.this, "需要权限才能上传图片哦", Toast.LENGTH_SHORT).show();

                } else {
                    Log.e("TAG", "没有权限，申请权限");
                    // 申请权限
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                            REQUEST_CALL_PHONE);
                }
            } else {
                Log.e(TAG, "有权限，执行相关操作");
                // 有权限，执行相关操作
                toPic();
            }
            //当是6.0以下版本时直接执行弹出拍照图库窗口
        }else{
            toPic();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CALL_PHONE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "权限同意");
                toPic();
            } else {
                Log.e(TAG, "权限拒绝");
                Toast.makeText(this, "权限拒绝", Toast.LENGTH_SHORT).show();
            }
            return;
        }
    }


    Uri photoUri;

    //去图库的方法
    private void toPic() {
        // 写一个去图库选图的Intent
        Intent intent1 = new Intent(Intent.ACTION_PICK);
        intent1.setDataAndType(Media.EXTERNAL_CONTENT_URI,
                "image/*");
        // 写一个打开系统拍照程序的intent
        Intent intent2 = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File file = new File(
                Environment
                        .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                System.currentTimeMillis() + ".jpg");
        cameraPath = file.getAbsolutePath();
        photoUri = Uri.fromFile(file);
        intent2.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

        // Intent选择器
        Intent intent = Intent.createChooser(intent1, "选择头像...");
        intent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                new Intent[]{intent2});
        startActivityForResult(intent, 100);
    }

    //拿到图片的路径
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (resultCode == MainActivity.this.RESULT_OK && requestCode == 100) {
            // 作为头像的图片在设备上的本地路径是什么（/sdcard/XXX/XXXX.jpg）
            String filePath = "";
            if (data != null) {
                // 图片是用户从图库选择得到的
                // uri代表用户选取的图片在MediaStroe中存储的位置
                Uri uri = data.getData();
                try {
                    Bitmap bm = getBitmapFormUri(MainActivity.this, uri);
                    if (bm != null) {
                        String img = imgToBase64(filePath, bm, "256");
                        Log.i("hxl", "img========================================" + img);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                // 利用ContentResolver查找uri所对应图片在设备上的真实路径
                Cursor cursor = MainActivity.this.getContentResolver().query(uri,
                        new String[]{Media.DATA}, null, null, null);
                cursor.moveToNext();
                filePath = cursor.getString(0);
                Log.i("hxl", "filePath========" + filePath);
                imgFileName = filePath.substring(filePath.lastIndexOf("/")+1);
                Log.i("hxl", "imgFileName========" + imgFileName);
                cursor.close();
            } else {
                // 图片是用户拍照得到的
                Uri uriImageData;
                Bitmap bitmap;
                //判断data是否是null，为空的话就用备用uri代替
                Uri uri = null;
                if (data != null && data.getData() != null) {
                    uri = data.getData();
                }
                if (uri == null) {
                    if (photoUri != null) {
                        uri = photoUri;

                    }
                }
                Log.i("hxl", "uri========================================" + uri);
                if (uri != null) {
                    Log.i("hxl", "uri========================================" + uri);
                    try {
                        // 利用ContentResolver查找uri所对应图片在设备上的真实路径

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                //5.1可用
                Log.i("hxl", "cameraPath========================================" + cameraPath);
                if(cameraPath!=null){
                    imgFileName = cameraPath.substring(cameraPath.lastIndexOf("/")+1);
                    Log.i("hxl", "imgFileName========" + imgFileName);
                    bitmap = BitmapFactory.decodeFile(cameraPath);
                    Log.i("hxl", "bitmap========================================"+bitmap);
                    if (bitmap != null) {
                        String img = imgToBase64(filePath, bitmap, "256");
                        Log.i("hxl", "img========================================" + img);
                    }
                }
            }

        }
    }


    /**
     * @param imgPath
     * @param bitmap
     * @param imgFormat 图片格式
     * @return
     */
    public static String imgToBase64(String imgPath, Bitmap bitmap, String imgFormat) {
        if (imgPath != null && imgPath.length() > 0) {
            bitmap = readBitmap(imgPath);
        }
        Log.i("hxl", "bitmap==" + bitmap);
        if (bitmap == null) {
            Log.i("hxl", "bitmap is null----------------");
        } else {
            ByteArrayOutputStream out = null;
            try {
                out = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

                out.flush();
                out.close();

                byte[] imgBytes = out.toByteArray();
                return Base64.encodeToString(imgBytes, Base64.DEFAULT);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                return null;
            } finally {
                try {
                    out.flush();
                    out.close();
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        return null;
    }


    private static Bitmap readBitmap(String imgPath) {
        try {
            Log.i("hxl", "imgPath===============" + imgPath);
            return BitmapFactory.decodeFile(imgPath);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            return null;
        }

    }


    /**
     * 通过uri获取图片并进行压缩
     *
     * @param uri
     */
    public static Bitmap getBitmapFormUri(Activity ac, Uri uri) throws FileNotFoundException, IOException {
        InputStream input = ac.getContentResolver().openInputStream(uri);
        BitmapFactory.Options onlyBoundsOptions = new BitmapFactory.Options();
        onlyBoundsOptions.inJustDecodeBounds = true;
        onlyBoundsOptions.inDither = true;//optional
        onlyBoundsOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        BitmapFactory.decodeStream(input, null, onlyBoundsOptions);
        input.close();
        int originalWidth = onlyBoundsOptions.outWidth;
        int originalHeight = onlyBoundsOptions.outHeight;
        if ((originalWidth == -1) || (originalHeight == -1))
            return null;
        //图片分辨率以480x800为标准
        float hh = 800f;//这里设置高度为800f
        float ww = 480f;//这里设置宽度为480f
        //缩放比。由于是固定比例缩放，只用高或者宽其中一个数据进行计算即可
        int be = 1;//be=1表示不缩放
        if (originalWidth > originalHeight && originalWidth > ww) {//如果宽度大的话根据宽度固定大小缩放
            be = (int) (originalWidth / ww);
        } else if (originalWidth < originalHeight && originalHeight > hh) {//如果高度高的话根据宽度固定大小缩放
            be = (int) (originalHeight / hh);
        }
        if (be <= 0)
            be = 1;
        //比例压缩
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inSampleSize = be;//设置缩放比例
        bitmapOptions.inDither = true;//optional
        bitmapOptions.inPreferredConfig = Bitmap.Config.ARGB_8888;//optional
        input = ac.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, bitmapOptions);
        input.close();

        return compressImage(bitmap);//再进行质量压缩
    }


    /**
     * 质量压缩方法
     *
     * @param image
     * @return
     */
    public static Bitmap compressImage(Bitmap image) {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.JPEG, 100, baos);//质量压缩方法，这里100表示不压缩，把压缩后的数据存放到baos中
        int options = 100;
        while (baos.toByteArray().length / 1024 > 100) {  //循环判断如果压缩后图片是否大于100kb,大于继续压缩
            baos.reset();//重置baos即清空baos
            //第一个参数 ：图片格式 ，第二个参数： 图片质量，100为最高，0为最差  ，第三个参数：保存压缩后的数据的流
            image.compress(Bitmap.CompressFormat.JPEG, options, baos);//这里压缩options%，把压缩后的数据存放到baos中
            options -= 10;//每次都减少10
        }
        ByteArrayInputStream isBm = new ByteArrayInputStream(baos.toByteArray());//把压缩后的数据baos存放到ByteArrayInputStream中
        Bitmap bitmap = BitmapFactory.decodeStream(isBm, null, null);//把ByteArrayInputStream数据生成图片
        return bitmap;
    }

    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
//    public com.google.android.gms.appindexing.Action getIndexApiAction() {
//        com.google.android.gms.appindexing.Thing object = new com.google.android.gms.appindexing.Thing.Builder()
//                .setName("Main Page") // TODO: Define a title for the content shown.
//                // TODO: Make sure this auto-generated URL is correct.
//                .setUrl(Uri.parse("http://[ENTER-YOUR-URL-HERE]"))
//                .build();
//        return new com.google.android.gms.appindexing.Action.Builder(com.google.android.gms.appindexing.Action.TYPE_VIEW)
//                .setObject(object)
//                .setActionStatus(com.google.android.gms.appindexing.Action.STATUS_TYPE_COMPLETED)
//                .build();
//    }

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }
}
