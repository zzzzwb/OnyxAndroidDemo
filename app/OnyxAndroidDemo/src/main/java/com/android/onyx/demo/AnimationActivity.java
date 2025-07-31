package com.android.onyx.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.onyx.android.demo.R;
import com.onyx.android.demo.databinding.ActivityAnimationBinding;

public class AnimationActivity extends Activity implements View.OnClickListener, PageCurlView.OnPageTurnListener {

    private static final String TAG = AnimationActivity.class.getSimpleName();
    private ActivityAnimationBinding binding;
    
    // 页面内容相关变量
    private Bitmap currentPageBitmap;
    private Bitmap nextPageBitmap;
    private Paint paint = new Paint();
    
    // 对齐相关变量
    private boolean useAlignment = false;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_animation);
        initView();
        initListener();
    }
    
    private void initView() {
        // 初始化页面内容
        binding.textAlignmentStatus.setText("当前状态：未对齐");
    }
    
    private void initListener() {
        binding.buttonStartAnimation.setOnClickListener(this);
        binding.buttonToggleAlignment.setOnClickListener(this);
        
        // 设置页面翻转监听器
        binding.pageCurlView.setOnPageTurnListener(this);
        
        // 初始化页面内容
        binding.pageCurlView.post(new Runnable() {
            @Override
            public void run() {
                initPageBitmaps();
                setPageBitmaps();
            }
        });
    }
    
    private void initPageBitmaps() {
        int width = binding.pageCurlView.getWidth();
        int height = binding.pageCurlView.getHeight();
        
        // 创建当前页和下一页的位图
        currentPageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        nextPageBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        
        // 绘制当前页内容（模拟书籍页面）
        Canvas currentCanvas = new Canvas(currentPageBitmap);
        currentCanvas.drawColor(Color.WHITE);
        Paint textPaint = new Paint();
        textPaint.setColor(Color.BLACK);
        textPaint.setTextSize(40);
        currentCanvas.drawText("第一页内容", 100, 100, textPaint);
        
        // 绘制标题
        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.BLACK);
        titlePaint.setTextSize(60);
        titlePaint.setFakeBoldText(true);
        currentCanvas.drawText("电子书翻页效果演示", width/2 - 300, 200, titlePaint);
        
        // 绘制正文内容
        textPaint.setTextSize(30);
        String content = "这是一个模拟电子书翻页效果的演示。\n\n" +
                "通过自定义的PageCurlView实现了垂直边缘的书籍翻页动画，" +
                "包括垂直边缘和阴影效果，使得翻页体验更加清晰流畅。\n\n" +
                "您可以点击'开始翻页动画'按钮来查看自动翻页效果，" +
                "也可以直接从屏幕右侧边缘滑动来手动翻页。";
        
        // 简单的文本换行处理
        String[] lines = content.split("\\n");
        int y = 300;
        for (String line : lines) {
            if (line.isEmpty()) {
                y += 40; // 空行增加行距
            } else {
                // 简单的自动换行处理
                int charsPerLine = 30; // 每行大约30个字符
                for (int i = 0; i < line.length(); i += charsPerLine) {
                    int end = Math.min(i + charsPerLine, line.length());
                    currentCanvas.drawText(line.substring(i, end), 100, y, textPaint);
                    y += 50; // 行距
                }
            }
        }
        
        // 绘制一些装饰元素
        Paint decorPaint = new Paint();
        decorPaint.setStyle(Paint.Style.STROKE);
        decorPaint.setStrokeWidth(2);
        decorPaint.setColor(Color.GRAY);
        currentCanvas.drawLine(50, height - 100, width - 50, height - 100, decorPaint);
        currentCanvas.drawText("第 1 页", width/2 - 50, height - 50, textPaint);
        
        // 绘制下一页内容
        Canvas nextCanvas = new Canvas(nextPageBitmap);
        nextCanvas.drawColor(Color.WHITE);
        
        // 绘制标题
        nextCanvas.drawText("第二页内容", 100, 100, textPaint);
        
        // 绘制一些图形内容
        Paint shapePaint = new Paint();
        shapePaint.setStyle(Paint.Style.STROKE);
        shapePaint.setStrokeWidth(5);
        shapePaint.setColor(Color.BLACK);
        
        // 绘制一些图形
        nextCanvas.drawCircle(width/2, height/2 - 100, 150, shapePaint);
        
        // 填充圆形
        Paint fillPaint = new Paint();
        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(Color.LTGRAY);
        nextCanvas.drawCircle(width/2, height/2 - 100, 150, fillPaint);
        
        // 在圆上绘制文字
        textPaint.setTextSize(40);
        textPaint.setColor(Color.BLACK);
        nextCanvas.drawText("翻页效果", width/2 - 80, height/2 - 90, textPaint);
        
        // 绘制说明文字
        textPaint.setTextSize(30);
        nextCanvas.drawText("您可以继续翻页或者尝试手动从屏幕边缘滑动", 100, height/2 + 100, textPaint);
        nextCanvas.drawText("来体验更真实的翻页效果", 100, height/2 + 150, textPaint);
        
        // 页脚
        nextCanvas.drawLine(50, height - 100, width - 50, height - 100, decorPaint);
        nextCanvas.drawText("第 2 页", width/2 - 50, height - 50, textPaint);
    }
    
    private void setPageBitmaps() {
        // 将位图设置到PageCurlView中
        binding.pageCurlView.setPageBitmaps(currentPageBitmap, nextPageBitmap);
    }
    
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_start_animation) {
            // 启动自动翻页动画
            binding.pageCurlView.startPageTurnAnimation();
            Toast.makeText(this, "开始翻页动画", Toast.LENGTH_SHORT).show();
        } else if (v.getId() == R.id.button_toggle_alignment) {
            useAlignment = !useAlignment;
            binding.textAlignmentStatus.setText(useAlignment ? "当前状态：使用64位对齐" : "当前状态：未对齐");
            Toast.makeText(this, "此设置在新的翻页动画中不再影响视觉效果", Toast.LENGTH_SHORT).show();
        }
    }
    
    @Override
    public void onPageTurned() {
        // 页面翻转完成后的回调
        Toast.makeText(this, "页面已翻转", Toast.LENGTH_SHORT).show();
        
        // 创建新的下一页内容（这里简单地交换当前页和下一页）
        Bitmap temp = currentPageBitmap;
        currentPageBitmap = nextPageBitmap;
        nextPageBitmap = temp;
        
        // 更新PageCurlView中的页面内容
        binding.pageCurlView.setPageBitmaps(currentPageBitmap, nextPageBitmap);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 释放位图资源
        if (currentPageBitmap != null && !currentPageBitmap.isRecycled()) {
            currentPageBitmap.recycle();
            currentPageBitmap = null;
        }
        if (nextPageBitmap != null && !nextPageBitmap.isRecycled()) {
            nextPageBitmap.recycle();
            nextPageBitmap = null;
        }
    }
}
