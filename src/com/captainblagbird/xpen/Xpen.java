package com.captainblagbird.xpen;

import android.inputmethodservice.InputMethodService;
import android.view.KeyEvent;
import android.view.View;

public class Xpen extends InputMethodService
{
	XpenView xpenView;
	
	@Override
	public View onCreateInputView()
	{
		xpenView = (XpenView) getLayoutInflater().inflate(R.layout.input, null);
		xpenView.setIME(this);
		return xpenView;
	}
	
	/** Helper to commit text to input */
	public void SendText(String s)
	{
		getCurrentInputConnection().commitText(s,1);
	}
	
	/** Helper to send a special key to input */
	public void SendKey(int keyEventCode)
	{
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyEventCode));
		getCurrentInputConnection().sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyEventCode));
	}
	
	/** Helper to read n characters relative to cursor position (n can be negative to read before cursor) */
	public String ReadText(int n)
	{		
		String s = "";
		
		if(n > 0)
		{
			s = getCurrentInputConnection().getTextAfterCursor(n, 0).toString();
		}
		else if(n < 0)
		{
			s = getCurrentInputConnection().getTextBeforeCursor(-n, 0).toString();
		}
		
		return s;
	}
}