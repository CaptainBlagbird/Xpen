package com.captainblagbird.xpen;

import android.content.Context;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

public class XpenView extends View
{
	Xpen xpen;
	
	private PointF		posLast;					// Last point of touch
	private PointF		posNow;						// Current point of touch
	private float		radius;						// Radius of circle
	private int			sector;						// First sector on leaving circle
	private int			dir;						// Direction (sign) and number of sectors visited
	private boolean		outsideOfCircle = false;	// State of position, is only true if circle was left during onTouchMove
	private boolean		movedPos = false;			// To check if touch moved or only tapped
	private boolean		uppercase = true;			// Letter case state
	private char[][]	characters = {
							{'a', 's', 'i', 'o'},
							{'r', 'd', 'h', 'u'},
							{'x', 'g', 'j', 'v'},
							{'?', 'ü', ',', 'w'},
							{'n', 'y', 't', 'e'},
							{'m', 'b', 'c', 'l'},
							{'f', 'p', 'z', 'k'},
							{'ä', 'q', '.', 'ö'}};	// Table of the arranged characters
	public XpenView(Context context, AttributeSet attrs)
	{
		super(context,attrs);
	}
	
	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec)
	{
		// Get size without mode
		int measureWidth = View.MeasureSpec.getSize(widthMeasureSpec);
		int measureHeight = View.MeasureSpec.getSize(heightMeasureSpec);
		
		int width = measureWidth;
		int height = measureHeight;
		
		// Get orientation
		if(getResources().getConfiguration().orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE)
		{
			// Switch to button mode
			// TODO
			// Placeholder:
			height = 100;
		}
		else  // Portrait mode
		{
			// Adjust the height to match the aspect ratio 4:3
			height = Math.round(0.75f * width);
		}
		
		// Calculate the diameter with the circle width to image width ratio 260:800,
		// and divide in half to get the radius
		radius = (0.325f * width) / 2;
		
		// Set the new size
		setMeasuredDimension(width, height);
	}
	
	void handleLettercase(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
	{
		// Set uppercase if cursor is at position 0
		if(newSelStart == 0)
		{
			uppercase = true;
			handleBackground();
			return;
		}
		
		// Check if selection moved only 1 to the left/right, else return
		if(Math.abs(newSelStart-oldSelStart) != 1) return;
		
		// Get last character
		char c = xpen.readText(-1).charAt(0);
		
		if((c == '.') || (c == ':') || (c == '!') || (c == '?') || (c == '…'))  // Terminating character
		{
			uppercase = true;
		}
		else if((c == ' ') || (c == '\t') || (c == '\r') || (c == '\n'))  // Space character
		{
			// Don't change letter case
		}
		else  // Any other character
		{
			uppercase = false;
		}
		
		handleBackground();
	}

	/** Gets the distance from the center to point p */
	private double getRadius(PointF p)
	{
		// Get center point
		PointF m = new PointF(getWidth() / 2, getHeight() / 2);
		
		// Get difference of coordinates
		double x = p.x - m.x;
		double y = m.y - p.y;

		// Return distance calculated with Pythagoras
		return Math.sqrt(x*x + y*y);
	}

	/** Gets the angle of point p relative to the center */
	private double getAngle(PointF p)
	{
		// Get center point
		PointF m = new PointF(getWidth() / 2, getHeight() / 2);
		
		// Get difference of coordinates
		double x = p.x - m.x;
		double y = m.y - p.y;

		// Calculate angle with special atan (calculates the correct angle in all quadrants)
		double angle = Math.atan2(y, x);
		// Make all angles positive
		if (angle < 0) angle = Math.PI * 2 + angle;

		return angle;
	}

	/** Modulus calculation (a % b) that supports negative numbers */
	private double mod(double a, double b)
	{
		double result;
		// Calculate result with modulus operator
		result = a % b;
		// Fix zero truncation
		if (result < 0) result += b;
		return result;
	}
	
	/** Get the number of the sector that point p is in
	 *  @return 0: right, 1: top, 2: left, 3: bottom */
	private int getSector(PointF p)
	{
		// Angle to Sector:
		// -> 0.0 ... 4.0
		double a = getAngle(p) / (Math.PI / 2);
		// ->   0 ... 4
		int d = (int)Math.round(a);
		// ->   0 ... 3
		return (int)mod(d, 4);
	}
	
	/** Sets the background image depending on the letter case */
	private void handleBackground()
	{
		if(uppercase)
		{
			findViewById(R.id.keyboard).setBackgroundResource(R.drawable.background_uppercase);
		}
		else
		{
			findViewById(R.id.keyboard).setBackgroundResource(R.drawable.background_lowercase);
		}
	}
	
	public boolean onTouchEvent(MotionEvent e)
	{
		// Touch handler
		// (Without MotionEvent.ACTION_MASK or e.getActionMasked() because we use only 1 Finger anyway
		// and to support the lowest possible required API Level)
		switch(e.getAction())
		{
			case MotionEvent.ACTION_DOWN:
				img_Input_onTouchDown(e);
				break;
	
			case MotionEvent.ACTION_MOVE:
				img_Input_onTouchMove(e);
				break;
			
			case MotionEvent.ACTION_UP:
				img_Input_onTouchUp(e);
				break;
				
			default:
				return false;
		}
		
		return true;
	}
	
	private void img_Input_onTouchDown(MotionEvent e)
	{
		// Get touch point
		posNow = new PointF(e.getX(), e.getY());
		
		if (getRadius(posNow) > radius) return;  // Not inside of circle

		// Remember point
		posLast = posNow;
	}
	
	private void img_Input_onTouchMove(MotionEvent e)
	{
		// Return if posLast is still undefined
		if(posLast == null) return;
		
		if ((getRadius(posLast)-getRadius(posNow))<=10.0) 
		{
			movedPos=false;
		} 
		else 
		{
			movedPos = true;
		}
		
		// Get touch point
		posNow = new PointF(e.getX(), e.getY());
		
		if ((getRadius(posLast) < radius) && (getRadius(posNow) >= radius))  // Leaving circle
		{
			// Update position state variable
			outsideOfCircle = true;
	
			// Reset direction/number variable
			dir = 0;
	
			// Calculate the sector
			sector = getSector(posNow);
		}
		else if ((getRadius(posLast) >= radius) && (getRadius(posNow) < radius))  // Entering circle
		{
			// Update position state variable
			outsideOfCircle = false;
	
			// Check if dir is valid (crossed at least one line)
			if(dir != 0)
			{
				// Calculate array index i with dir
				int i;
				if(dir > 0)
				{
					// dir = 1 ... 4  to  i= 0 ... 3
					i = dir - 1;
				}
				else  // dir < 0
				{
					// dir = -1 ...-4  to  i= 4 ... 7
					i = 3 - dir;
				}
	
				// Send character in correct case
				if(uppercase)
				{
					xpen.sendText("" + Character.toUpperCase(characters[i][sector]));
					uppercase = false;
					handleBackground();
				}
				else
				{
					xpen.sendText("" + characters[i][sector]);
				}
			}
		}
		
		if (outsideOfCircle)
		{
			// Norm angles to 0...PI/2 so we can check with the same values for each line
			double al = getAngle(posLast) % (Math.PI / 2);  // Last angle normed
			double ac = getAngle(posNow) % (Math.PI / 2);  // Current angle normed
	
			// Exclude false hits at crossing of ~2*PI <--> 0 by only looking for smaller changes than PI/3
			// Also excludes false hits due to (PI/2 % PI/2) = 0
			if (Math.abs(al - ac) < Math.PI / 3)
			{
				// Count crossed lines
				// (it's possible to "uncross" a line by going back in the other direction, only the final value is used)
				if ((ac > Math.PI / 4) && (al <= Math.PI / 4)) dir++;  // Crossed line CCW
				if ((ac <= Math.PI / 4) && (al > Math.PI / 4)) dir--;  // Crossed line CW

				// Check if a complete circle was done
				if(Math.abs(dir) > 4)
				{
					// Toggle letter case
					uppercase =! uppercase;
					handleBackground();

					// Norm dir to -4 ... 4 without 0
					int temp = 4;
					if(dir < 0) temp = -4;
					dir = dir % 4;
					if(dir == 0) dir = temp;
				}
			}
		}
	
		// Remember point
		posLast = posNow;
	}

	private void img_Input_onTouchUp(MotionEvent e)
	{
		// Get touch point
		posNow = new PointF(e.getX(), e.getY());
		
		// Check if dir is valid (crossed at least one line)
		if(!outsideOfCircle && (dir != 0))
		{
			// Send space
			xpen.sendText(" ");
		}
		
		else if(!movedPos)  // Not moved -> Tapped
		{
			if(getRadius(posNow) <= radius)  // Tapped inside circle
			{
				// Send space
				xpen.sendText(" ");
			}
			else  // Tapped outside circle
			{
				switch (getSector(posNow))
				{
					case 0:
						// Switch to numbers/special characters
						// TODO
						break;
					case 1:
						// Toggle letter case
						uppercase = !uppercase;
						handleBackground();
						break;
					case 2:
						// Send backspace
						xpen.sendKey(KeyEvent.KEYCODE_DEL);
						break;
					case 3:
						// Send enter
						xpen.sendKey(KeyEvent.KEYCODE_ENTER);
						break;
				}
			}
		}
		
		// Reset position state variable
		outsideOfCircle = false;
		// Reset moved state variable
		movedPos = false;
		// Reset direction/number variable
		dir = 0;
	}
	
	public void setIME(Xpen _xpen)
	{
		xpen = _xpen;
	}
}
