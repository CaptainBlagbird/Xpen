package com.captainblagbird.xpen;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;
import android.view.HapticFeedbackConstants;

public class Xpen extends InputMethodService
{
	XpenView xpenView;
	
	@Override
	public View onCreateInputView()
	{
		xpenView = (XpenView) getLayoutInflater().inflate(R.layout.input, null);

		xpenView.setIME(this);
		xpenView.setHapticFeedbackEnabled(true);
		return xpenView;
	}
	
	@Override
	public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candidatesStart, int candidatesEnd)
	{
		super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
		// Pass it to the view
		xpenView.handleLettercase(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candidatesStart, candidatesEnd);
	}
	
	/** Helper to commit text to input */
	public void sendText(String str)
	{
		getCurrentInputConnection().commitText(str,1);
		xpenView.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP,HapticFeedbackConstants.FLAG_IGNORE_GLOBAL_SETTING);
	}
	
	/** Helper to send a special key to input */
	public void sendKey(int keyEventCode)
	{
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}
	
	/** Helper to read n characters relative to cursor position (n can be negative to read before cursor) */
	public String readText(int n)
	{		
		String returnString = "";
		
		if(n > 0)
		{
			returnString = getCurrentInputConnection().getTextAfterCursor(n, 0).toString();
		}
		else if(n < 0)
		{
			returnString = getCurrentInputConnection().getTextBeforeCursor(-n, 0).toString();
		}
		
		return returnString;
	}
}
