package com.jme3.bullet.vhacd.cache;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.scene.Mesh;

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
public class VolatileByMeshCaching implements Caching{
	private final Map<Entry,CompoundCollisionShape> _CACHE=new HashMap<Entry,CompoundCollisionShape> ();
	private class Entry{
		private final VHACDParameters _P;
		private final WeakReference<Mesh> _M ;

		private Entry(Mesh m,VHACDParameters p){
			_M=new WeakReference<Mesh>(m);
			_P=p.clone();
		}
		
		private Boolean process(Entry t,boolean need_result) {
			Entry b=(Entry)t;
			VHACDParameters pA=_P;
			Mesh mA=_M.get();

			if(mA==null){ return null; }
			if(!need_result)return false;
			
			VHACDParameters pB=b._P;
			Mesh mB=b._M.get();
			if(mB==null) return false;

			return mA==mB&&pA.equals(pB);
		}
	}
	
	
	@Override
	public void save(Mesh m, CompoundCollisionShape shape, VHACDParameters p) {
		Entry e=new Entry(m,p);
		for(java.util.Map.Entry<Entry,CompoundCollisionShape> x:_CACHE.entrySet()){
			Boolean r=x.getKey().process(e,true);
			if(r!=null&&r){
				return;
			}
		}
		_CACHE.put(e,shape);
	}

	@Override
	public CompoundCollisionShape load(Mesh m, VHACDParameters p) {
		Entry e=new Entry(m,p);
		Set<java.util.Map.Entry<Entry,CompoundCollisionShape>> e_s=_CACHE.entrySet();
		Iterator<java.util.Map.Entry<Entry,CompoundCollisionShape>> e_s_i=e_s.iterator();
		CompoundCollisionShape out=null;
		while(e_s_i.hasNext()){
			java.util.Map.Entry<Entry,CompoundCollisionShape> entry=e_s_i.next();
			Boolean r=entry.getKey().process(e,out==null);
			if(r==null)e_s_i.remove();
			else if(r){ //keep going because we want to remove GCed keys.
				out=entry.getValue();
			}
		}
		return out;
	}

}
