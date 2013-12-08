package ro.ui.pttdroid.util;


public class Log 
{

	/**
	 * 
	 * @param c
	 * @param e
	 */
	public static void error(Class<? extends Object> c, Exception e)
	{
		android.util.Log.e(c.getCanonicalName(), e.toString());
	}
	
}
