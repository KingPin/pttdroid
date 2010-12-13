/*
 * Copyright (C) 2010 The pttdroid Open Source Project
 * 
 * This file is part of pttdroid (http://www.code.google.com/p/pttdroid/)
 * 
 * pttdroid is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This source code is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this source code; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package ro.ui.pttdroid.codecs;

public class Speex {
	
	static {
		System.loadLibrary("speex_jni");
	}
	
	private static final int[] encodedSizes = {6, 10, 15, 20, 20, 28, 28, 38, 38, 46, 62};
	
	public static int getEncodedSize(int quality) {
		return encodedSizes[quality];
	}

	public static native void open(int quality);
    public static native int decode(byte[] in, int length, short[] out);
    public static native int encode(short[] in, byte[] out);
    public static native void close();	
	
}
