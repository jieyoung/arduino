package org.test;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

public class WaveDiagramThread {

	private boolean isRecording = false;// 线程控制标记
	private byte[] btBuffer = null; // 蓝牙数据输入流 byte
	/**
	 * X轴缩小的比例
	 */
	public int rateX = 1;
	/**
	 * Y轴缩小的比例
	 */
	public int rateY = 1;
	/**
	 * Y轴基线
	 */
	public int baseLine = 0;
	/**
	 * 字节长度
	 */
	public int bufferlength=0;

	/**
	 * 初始化
	 */
	public WaveDiagramThread(byte[] btBuffer, int rateX, int rateY,
			int baseLine,int bufferlength) {
		this.btBuffer = btBuffer;
		this.rateX = rateX;
		this.rateY = rateY;
		this.baseLine = baseLine;
		this.bufferlength=bufferlength;
	}

	/**
	 * 开始
	 * 
	 * @param recBufSize
	 *            AudioRecord的MinBufferSize
	 */
	public void Start(WaveformView sfv, Paint mPaint, int wait) {
		isRecording = true;
		new DrawThread(sfv, mPaint, wait).start();// 开始绘制线程
	}

	/**
	 * 停止
	 */
	public void Stop() {
		isRecording = false;
	}

	/**
	 * 负责绘制inBuf中的数据
	 * 
	 * @author GV
	 * 
	 */
	class DrawThread extends Thread {

		private int oldX = 0;// 上次绘制的X坐标
		private int oldY = 0;// 上次绘制的Y坐标
		private WaveformView sfv;// 画板
		private int X_index = 0;// 当前画图所在屏幕X轴的坐标
		private Paint mPaint;// 画笔
		private int wait = 50;// 线程等待时间

		public DrawThread(WaveformView sfv, Paint mPaint, int wait) {
			this.sfv = sfv;
			this.mPaint = mPaint;
			this.wait = wait;
		}

		public void run() {
			while (isRecording) {
				try {
					
					Log.e("available", String.valueOf(bufferlength));
					if (bufferlength > 0) {
						SimpleDraw(X_index, btBuffer, rateX, rateY, baseLine);// 把缓冲区数据画出来
						X_index = X_index + (bufferlength/ rateX) - 1;// 这里-1可以减少空隙
						if (X_index > sfv.getHeight()) {
							X_index = 0;
						}
					}
					Thread.sleep(wait);// 延时一定时间缓冲数据
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			}
		}

		/**
		 * 绘制指定区域
		 * 
		 * @param start
		 *            X轴开始的位置(全屏)
		 * @param inputBuf
		 *            缓冲区
		 * @param rateX
		 *            X轴数据缩小的比例
		 * @param rateY
		 *            Y轴数据缩小的比例
		 * @param baseLine
		 *            Y轴基线
		 */
		void SimpleDraw(int start, byte[] btBuffer, int rateX, int rateY,
				int baseLine) {
			if (start == 0)
				oldX = 0;
			// 根据需要缩小X轴比例
			byte[] buffer = new byte[bufferlength / rateX];
			for (int i = 0, ii = 0; i < bufferlength; i++, ii = i * rateX)
				buffer[i] = btBuffer[ii];

			Canvas canvas = sfv.getHolder().lockCanvas(
					new Rect(0, start, sfv.getWidth(), start + bufferlength));// 关键:获取画布
			canvas.drawColor(Color.BLACK);// 清除背景

			for (int i = 0; i < bufferlength; i++) {// 有多少画多少
				int x = (0xFF - (buffer[i] & 0xFF))// 0xFF- 用于翻转，&0xFF用把byte类型的负数取值转为正
			    /rateY + baseLine;// 调节缩小比例，调节基准线
				//java的byte最高位为符号位
				int y = i + start;
				canvas.drawLine(oldX, oldY, x, y, mPaint);
				oldX = x;
				oldY = y;

			}
			sfv.getHolder().unlockCanvasAndPost(canvas);//解锁画布，提交画好的图像
		}
	}
}
