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
	
	private PointF P_last;							// Last point of touch
	private PointF P_now;							// Current point of touch
	private float Radius;							// Radius of circle
	private int Sector;								// First sector on leaving circle
	private int Dir;								// Direction (sign) and number of sectors visited
	private boolean Left_circle = false;			// State of position, is only true if circle was left during onTouchMove
	private boolean Moved = false;					// To check if touch moved or only tapped
	private boolean Uppercase = true;				// Letter case state
	private char[][] Ascii = {{'a', 's', 'i', 'o'},
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
		// Adjust the height to match the aspect ratio 4:3
		int height = Math.round(0.75f * getWidth());
		setMeasuredDimension(widthMeasureSpec, height);
		
		// Calculate the diameter with the circle width to image width ratio 260:800,
		// and divide in half to get the radius
		Radius = ((float)0.325 * getWidth()) / 2;
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

	/** Modulus calculation that supports negative numbers */
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
		if(Uppercase)
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
		P_now = new PointF(e.getX(), e.getY());
		
		if (getRadius(P_now) > Radius) return; // Not inside of circle

		// Remember point
		P_last = P_now;
	}
	
	private void img_Input_onTouchMove(MotionEvent e)
	{
		Moved = true;
		
		// Get touch point
		P_now = new PointF(e.getX(), e.getY());
		
		if ((getRadius(P_last) < Radius) && (getRadius(P_now) >= Radius)) // Leaving circle
		{
			// Update position state variable
			Left_circle = true;
	
			// Reset direction/number variable
			Dir = 0;
	
			// Calculate the sector
			Sector = getSector(P_now);
		}
		else if ((getRadius(P_last) >= Radius) && (getRadius(P_now) < Radius)) // Entering circle
		{
			// Update position state variable
			Left_circle = false;
	
			// Check if Dir is valid (crossed 1 ... 4 lines)
			if ((Dir != 0) && (Dir <= 4) && (Dir >= -4))
			{
				// Calculate array index i with Dir
				int i;
				if (Dir > 0)
				{
					// Dir=  1 ... 4  to  i= 0 ... 3
					i = Dir - 1;
				}
				else // Dir < 0
				{
					// Dir= -1 ...-4  to  i= 4 ... 7
					i = 3 - Dir;
				}
				
				// Set uppercase for the first character of a field
				if(xpen.ReadText(-2).length() < 1)
				{
					Uppercase = true;
					handleBackground();
				}
				
				// Send character in correct case
				if(Uppercase)
				{
					xpen.SendText("" + Character.toUpperCase(Ascii[i][Sector]));
					Uppercase = false;
					handleBackground();
				}
				else
				{
					xpen.SendText("" + Ascii[i][Sector]);
				}
			}
		}
		
		if (Left_circle)
		{
			// Norm angles to 0...PI/2 so we can check with the same values for each line
			double al = getAngle(P_last) % (Math.PI / 2); // Last angle normed
			double ac = getAngle(P_now) % (Math.PI / 2); // Current angle normed
	
			// Exclude false hits at crossing of ~2*PI <--> 0 by only looking for smaller changes than PI/3
			// Also excludes false hits due to (PI/2 % PI/2) = 0
			if (Math.abs(al - ac) < Math.PI / 3)
			{
				// Count crossed lines
				// (it's possible to "uncross" a line by going back in the other direction, only the final value is used)
				if ((ac > Math.PI / 4) && (al <= Math.PI / 4)) Dir++; // Crossed line CCW
				if ((ac <= Math.PI / 4) && (al > Math.PI / 4)) Dir--; // Crossed line CW
			}
		}
	
		// Remember point
		P_last = P_now;
	}
	
	private void img_Input_onTouchUp(MotionEvent e)
	{
		// Get touch point
		P_now = new PointF(e.getX(), e.getY());
		
		// Check if Dir is valid (crossed 1 ... 4 lines)
		if (!Left_circle && (Dir != 0) && (Dir <= 4) && (Dir >= -4))
		{
			// Send space
			xpen.SendText(" ");

			// Set uppercase after ". "
			if(xpen.ReadText(-2).equals(". "))
			{
				Uppercase = true;
				handleBackground();
			}
		}
		else if(!Moved && (getRadius(P_now) > Radius)) // Touch outside and not moved -> Tapped outside
		{
			switch (getSector(P_now))
			{
				case 0:
					// Switch to numbers/special characters
					//-
					break;
				case 1:
					// Toggle letter case
					Uppercase = !Uppercase;
					handleBackground();
					break;
				case 2:
					// Send backspace
					xpen.SendKey(KeyEvent.KEYCODE_DEL);
					break;
				case 3:
					// Send enter
					xpen.SendKey(KeyEvent.KEYCODE_ENTER);
					break;
			}
		}
		
		// Reset position state variable
		Left_circle = false;
		// Reset moved state variable
		Moved = false;
		// Reset direction/number variable
		Dir = 0;
	}
	
	public void setIME(Xpen _xpen)
	{
		xpen = _xpen;
	}
}