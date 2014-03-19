/* Copyright 2011 Ionut Ursuleanu
 
This file is part of pttdroid.
 
pttdroid is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.
 
pttdroid is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.
 
You should have received a copy of the GNU General Public License
along with pttdroid.  If not, see <http://www.gnu.org/licenses/>. */

package ro.ui.pttdroid.codecs;

public class Speex 
{
	
	static 
	{
		System.loadLibrary("speex_jni");
	}
	
	private static final int[] encodedSizes = {6, 10, 15, 20, 20, 28, 28, 38, 38, 46, 62};
	
	/**
	 * 
	 * @param quality
	 * @return
	 */
	public static int getEncodedSize(int quality) 
	{
		return encodedSizes[quality];
	}

	/**
	 * 
	 * @param quality
	 */
	public static native void open(int quality);
	
	/**
	 * 
	 * @param in
	 * @param length
	 * @param out
	 * @return
	 */
    public static native int decode(byte[] in, int length, short[] out);
    
    /**
     * 
     * @param in
     * @param out
     * @return
     */
    public static native int encode(short[] in, byte[] out);
    
    /**
     * 
     */
    public static native void close();	
	
}
