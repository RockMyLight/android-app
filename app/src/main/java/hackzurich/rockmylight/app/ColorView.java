package hackzurich.rockmylight.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by kmisiunas on 15-10-03.
 */
public class ColorView extends View {

    private int color;

    public ColorView(Context context, AttributeSet attrs) {
        super(context, attrs);
        color = Color.rgb(0, 0, 0);

    }



    public void setColor(int c) { color = c;}

    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Paint paint = new Paint();
        paint.setColor(color);
        canvas.drawRect(new Rect(0,0, getWidth(), getHeight()) , paint);
    }


}
