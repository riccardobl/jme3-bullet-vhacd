package com.jme3.bullet.vhacd;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.LinkedList;

import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.CompoundCollisionShape;
import com.jme3.bullet.collision.shapes.HullCollisionShape;
import com.jme3.bullet.collision.shapes.infos.ChildCollisionShape;
import com.jme3.bullet.vhacd.cache.Caching;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer.Type;

import vhacd.VHACD;
import vhacd.VHACDHull;
import vhacd.VHACDParameters;
import vhacd.VHACDResults;
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
public class VHACDCollisionShapeFactory{
	public static VHACDParameters default_parameters=new VHACDParameters();
	
	protected final LinkedList<Caching> _CACHING_QUEUE=new LinkedList<Caching>();
	protected VHACDParameters PARAMETERS;
	
	public VHACDCollisionShapeFactory(){
		this(default_parameters);
	}
	
	public void setParameters(VHACDParameters p){
		PARAMETERS=p;
	}
	
	public VHACDParameters getParameters(){
		return PARAMETERS;
	}
		
	public VHACDCollisionShapeFactory(VHACDParameters params){
		PARAMETERS=params;
	}
	
	public  LinkedList<Caching> cachingQueue(){
		return _CACHING_QUEUE;
	}

	public CompoundCollisionShape create(Spatial s) {
		CompoundCollisionShape out=new CompoundCollisionShape();
		create(s,PARAMETERS,out);
		return out;
	}

	private void create(Spatial s, VHACDParameters p, CompoundCollisionShape out) {
		if(s instanceof Geometry){
			Geometry geo=(Geometry)s;
			CompoundCollisionShape ccs=create(geo.getMesh(),p);
			for(ChildCollisionShape cc:ccs.getChildren()){
				CollisionShape ccc=cc.shape;
				ccc.setScale(geo.getLocalScale());
				out.addChildShape(ccc,new Vector3f());
			}
		}else if(s instanceof Node){
			Node n=(Node)s;
			Collection<Spatial> cs=n.getChildren();
			for(Spatial c:cs){
				create(c,p,out);
			}
		}
	}


	public synchronized CompoundCollisionShape create(Mesh m, VHACDParameters p) {
		CompoundCollisionShape out=getCached(m,p);
		if(out!=null)return out;
		
		out=new CompoundCollisionShape();

		FloatBuffer vb=(FloatBuffer)m.getBuffer(Type.Position).getData();
		Buffer ib=m.getBuffer(Type.Index).getData();
		vb.rewind();
		ib.rewind();

		float positions[]=new float[vb.limit()];
		int indexes[]=new int[ib.limit()];

		for(int i=0;i<positions.length;i++)	positions[i]=vb.get(i);
		for(int i=0;i<indexes.length;i++){
			if(ib instanceof IntBuffer){
				indexes[i]=(int)((IntBuffer)ib).get(i);
			}else{
				indexes[i]=(int)((ShortBuffer)ib).get(i);
			}
		}

		VHACDResults results=VHACD.compute(positions,indexes,p);
		for(VHACDHull hull:results){
			float points[]=hull.positions;
			HullCollisionShape hc=new HullCollisionShape(points);
			out.addChildShape(hc,new Vector3f());
		}

		for(Caching c:_CACHING_QUEUE)	c.save(m,out,p);		
		return out;
	}
	
	private CompoundCollisionShape getCached(Mesh m, VHACDParameters p) {
		CompoundCollisionShape out=null;
		Caching r=null;
		for(Caching c:_CACHING_QUEUE){
			out=c.load(m,p);
			if(out!=null) {
				r=c;
				break;
			}
		}
		if(out!=null){
			for(Caching c:_CACHING_QUEUE){
				if(c!=r)c.save(m,out,p);
			}
		}
		return out;
	}
}
