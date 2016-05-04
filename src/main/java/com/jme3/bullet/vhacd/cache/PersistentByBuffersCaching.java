package com.jme3.bullet.vhacd.cache;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.export.binary.BinaryExporter;
import com.jme3.export.binary.BinaryImporter;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.VertexBuffer.Type;

import vhacd.VHACDParameters;
/**
Copyright (c) 2016, Riccardo Balbo
All rights reserved.

Redistribution and use in source and binary forms, with or without 
modification, are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice, 
this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice, 
this list of conditions and the following disclaimer in the documentation 
and/or other materials provided with the distribution.

3. Neither the name of the copyright holder nor the names of its contributors 
may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, 
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY 
OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE 
USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
**/
public class PersistentByBuffersCaching implements Caching{
	private static final Logger _LOGGER=Logger.getLogger(PersistentByBuffersCaching.class.getName());
	private static final Level _LOGGING_LEVEL=Level.FINE;
	protected final ArrayList<Entry> _CACHE=new ArrayList<Entry> ();
	protected long TOTAL_MEMORY_USAGE;
	protected long MAX_MEMORY_USAGE=32*1024*1024; // Default 32 MB
	protected final BinaryExporter _EXPORTER = BinaryExporter.getInstance();
	protected final BinaryImporter _IMPORTER = BinaryImporter.getInstance();
	protected final File _OUTPUT_PATH;

	protected class Entry{
		public String hash;
		public long size;
		public CompoundCollisionShape shape;
	}
	
	public PersistentByBuffersCaching(String out_path){
		_OUTPUT_PATH=new File(out_path.replace("/",File.separator));
	}
	
	public void setMaxMemoryInMB(int mb){
		MAX_MEMORY_USAGE=mb*1024*1024;
	}
	
	public int getMaxMemoryInMB(){
		return (int)(MAX_MEMORY_USAGE/1024/1024);
	}
	
	public long getCurrentMemoryInB(){
		return TOTAL_MEMORY_USAGE;
	}
	
	@Override
	public void save(Mesh m, CompoundCollisionShape shape, VHACDParameters p) {		
		try{
			
			String hash=getHash(m,p);
	
			ByteArrayOutputStream bao=new ByteArrayOutputStream();
			_EXPORTER.save(shape,bao);
			byte bytes[]=bao.toByteArray();
			bao.close();
			
			Entry e=new Entry();
			e.hash=hash;
			e.size=bytes.length;
			e.shape=shape;
		
			File f=new File(_OUTPUT_PATH,hash+".jme3_vhacd_cache");
			if(!f.exists()){
				FileOutputStream fos=new FileOutputStream(f);
				fos.write(bytes);
				fos.close();
				addToCache(e);			
			}

			
		}catch(Exception e){
			e.printStackTrace();
		}
	}

	protected void removeFromCache(int index,Entry e){
		if(index==-1){
			_CACHE.remove(e);
		}else{
			_CACHE.remove(index);
		}
		TOTAL_MEMORY_USAGE-=e.size;
		_LOGGER.log(_LOGGING_LEVEL,"Total cache size {0} bytes / max {1} bytes",new Object[]{TOTAL_MEMORY_USAGE,MAX_MEMORY_USAGE});
	}
	
	protected void addToCache(Entry e) {
		TOTAL_MEMORY_USAGE+=e.size;
		_CACHE.add(e);
		_LOGGER.log(_LOGGING_LEVEL,"Total cache size {0} bytes / max {1} bytes",new Object[]{TOTAL_MEMORY_USAGE,MAX_MEMORY_USAGE});
	}

	protected String getHash(Mesh m, VHACDParameters p) throws Exception {
		ByteArrayOutputStream bao=new ByteArrayOutputStream();
			
		VertexBuffer p_b=m.getBuffer(Type.Position);
		VertexBuffer i_b=m.getBuffer(Type.Index);

		p.toOutputStream(bao);
		_EXPORTER.save(p_b,bao);
		_EXPORTER.save(i_b,bao);			
		byte bytes[]=bao.toByteArray();
		bao.close();
		
		
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(bytes);
		byte[] digest = md.digest();
		BigInteger bigInt = new BigInteger(1,digest);
		String hash = bigInt.toString(16);
		while(hash.length() < 32 )hash = "0"+hash;	
		return hash;
	}

	@Override
	public CompoundCollisionShape load(Mesh m, VHACDParameters p) {
		CompoundCollisionShape out=null;

		try{
			String hash=getHash(m,p);
			for(int i=0;i<_CACHE.size();i++){
				Entry e=_CACHE.get(i);
				if(out==null&&e.hash.equals(hash)){
					out=e.shape; 
					// Move entry to the end of the queue 
					_CACHE.remove(i);
					_CACHE.add(e); 
					break;
				}
			}
			if(out==null)out=loadFromDisk(hash);				
			
		}catch(Exception e){
			e.printStackTrace();
		}
		return out;
	}

	protected CompoundCollisionShape loadFromDisk(String hash) throws IOException {
		File f=new File(_OUTPUT_PATH,hash+".jme3_vhacd_cache");
		if(f.exists()){
			Entry e=new Entry();
			e.hash=hash;
			e.size=f.length();
			e.shape=(CompoundCollisionShape)_IMPORTER.load(f);

			if(e.size>MAX_MEMORY_USAGE){
				_LOGGER.log(_LOGGING_LEVEL,"Not enough max cache memory. Max {0} bytes / required {1} ",new Object[]{MAX_MEMORY_USAGE,e.size});
				return e.shape;
			}
			
			if(TOTAL_MEMORY_USAGE>0){
				for(int i=0;i<_CACHE.size()&&(MAX_MEMORY_USAGE-TOTAL_MEMORY_USAGE)<e.size;i++){
					_LOGGER.log(_LOGGING_LEVEL,"Not enough free cache memory. Total cache size {0} bytes / max {1} bytes / required free {2} bytes",new Object[]{TOTAL_MEMORY_USAGE,MAX_MEMORY_USAGE,e.size});
					Entry old_e=_CACHE.get(0);
					_LOGGER.log(_LOGGING_LEVEL,"Free {0} bytes",old_e.size);
					removeFromCache(0,old_e);
				}
			}

			addToCache(e);
			return e.shape;
		}				
		return null;
	}

}
