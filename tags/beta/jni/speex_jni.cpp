/*
 * Copyright (C) 2009 The Sipdroid Open Source Project
 * 
 * This file is part of Sipdroid (http://www.sipdroid.org)
 * 
 * Sipdroid is free software; you can redistribute it and/or modify
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
 
#include <jni.h>

#include <speex/speex.h>

const int FRAME_SIZE = 160;

const int CODEC_OPENED = 1;
const int CODEC_CLOSED = 0;

int codec_status = 0;

SpeexBits ebits, dbits;
void *enc_state;
void *dec_state;

jshort in_enc_tmp[FRAME_SIZE], out_dec_tmp[FRAME_SIZE];
jbyte out_enc_tmp[FRAME_SIZE], in_dec_tmp[FRAME_SIZE];		

extern "C"
JNIEXPORT void JNICALL Java_ro_ui_pttdroid_codecs_Speex_open(JNIEnv *env, jobject obj, jint quality) {
    if(codec_status==CODEC_OPENED)
    	return;
    	
    codec_status = CODEC_OPENED;

    speex_bits_init(&ebits);
    speex_bits_init(&dbits);

    enc_state = speex_encoder_init(&speex_nb_mode); 
    dec_state = speex_decoder_init(&speex_nb_mode); 
        
    int tmp = quality;
    speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &tmp);
}

extern "C"
JNIEXPORT jint JNICALL Java_ro_ui_pttdroid_codecs_Speex_encode(JNIEnv *env, jobject obj, jshortArray in, jbyteArray out) {
	if(codec_status==CODEC_CLOSED)
		return (jint)0;
				
	speex_bits_reset(&ebits);
	
	env->GetShortArrayRegion(in, 0, FRAME_SIZE, in_enc_tmp);
	speex_encode_int(enc_state, in_enc_tmp, &ebits);
	
	jint outSize = speex_bits_write(&ebits, (char *)out_enc_tmp, FRAME_SIZE);
	env->SetByteArrayRegion(out, 0, outSize, out_enc_tmp);
	
	return (jint)outSize;	
}

extern "C"
JNIEXPORT jint JNICALL Java_ro_ui_pttdroid_codecs_Speex_decode(JNIEnv *env, jobject obj, jbyteArray in, jint inSize, jshortArray out) {
	if(codec_status==CODEC_CLOSED)
		return (jint)0;
	
	env->GetByteArrayRegion(in, 0, inSize, in_dec_tmp);
	speex_bits_read_from(&dbits, (char *)in_dec_tmp, inSize);
	speex_decode_int(dec_state, &dbits, out_dec_tmp);
	env->SetShortArrayRegion(out, 0, FRAME_SIZE, out_dec_tmp);
	
	return (jint)FRAME_SIZE;
}

extern "C"
JNIEXPORT void JNICALL Java_ro_ui_pttdroid_codecs_Speex_close(JNIEnv *env, jobject obj) {
	if(codec_status==CODEC_CLOSED)
        return;
        
    codec_status = CODEC_CLOSED;

    speex_bits_destroy(&ebits);
    speex_bits_destroy(&dbits);
    speex_decoder_destroy(dec_state); 
    speex_encoder_destroy(enc_state); 
}

