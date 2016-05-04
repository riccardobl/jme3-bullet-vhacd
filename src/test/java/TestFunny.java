import java.io.File;
import java.util.HashMap;
import java.util.Map;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.vhacd.VHACDCollisionShapeFactory;
import com.jme3.bullet.vhacd.cache.PersistentByBuffersCaching;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.DirectionalLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Spatial;
import com.jme3.system.AppSettings;

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
public class TestFunny extends SimpleApplication implements ActionListener{
    VHACDCollisionShapeFactory vhacd_factory;
    BulletAppState bullet;
    Map<String,Mesh> cache=new HashMap<String,Mesh>();

    
	void makeScene(){
		Geometry g=load("models/bowlAlmost/bowlAlmost.obj",new ColorRGBA(.22f,.14f,.06f,1f),100f,new Vector3f(0,-4f,0),new Vector3f(6,6,6));
		rootNode.attachChild(g);
		bullet.getPhysicsSpace().add(g);
		
		Geometry g2=load("models/ihavenoidea/ihavenoidea.obj",new ColorRGBA(.53f,.80f,.01f,1f),400f,new Vector3f(0,-4f,80f),new Vector3f(1.5f,1.5f,1.5f));
		rootNode.attachChild(g2);
		bullet.getPhysicsSpace().add(g2);
		
		// Preload projectile 
		load("models/torus/torus.obj",new ColorRGBA(),0f,new Vector3f(),new Vector3f());
	}
	
	
    private Geometry load(String path,ColorRGBA c,float mass,Vector3f location,Vector3f scale){
		Spatial s=assetManager.loadModel(path);
		Mesh m=cache.get(path);
		Geometry geo;
		if(m!=null){
			System.out.println("Load "+path+" from cache.");
			geo=new Geometry("",m);
		}else{
			geo= Commons.getGeom(s);	
			cache.put(path,geo.getMesh());
		}
		geo.setLocalScale(scale);
		geo.setLocalTranslation(location);
		Material mat=new Material(assetManager,"Common/MatDefs/Light/Lighting.j3md");
		mat.setColor("Diffuse",c);
		mat.setBoolean("UseMaterialColors",true);
		geo.setMaterial(mat);
		
		CollisionShape cs=vhacd_factory.create(geo);
		RigidBodyControl rb=new RigidBodyControl(cs,mass);
		geo.addControl(rb);
		return geo;
    }

	@Override
	public void simpleInitApp() {
		
		System.out.println("Initialize");

		Commons.initApp(this);
			
		bullet=stateManager.getState(BulletAppState.class);
		bullet.setSpeed(2.f);

		System.out.println("Initialize input");
		inputManager.addMapping("debug",new KeyTrigger(KeyInput.KEY_B));
		inputManager.addMapping("fire!!!!!",new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
		inputManager.addListener(this,"fire!!!!!","debug");

		DirectionalLight dl=new DirectionalLight(new Vector3f(0f, -1f, 0),new ColorRGBA(.72f,.97f,1f,1f).mult(1.4f));
		rootNode.addLight(dl);
		dl=new DirectionalLight(new Vector3f(0f, 0f, -1),new ColorRGBA(.72f,.97f,1f,1f).mult(.4f));
		rootNode.addLight(dl);
		dl=new DirectionalLight(new Vector3f(0f, 0f, 1),new ColorRGBA(.72f,.97f,1f,1f).mult(.4f));
		rootNode.addLight(dl);
		dl=new DirectionalLight(new Vector3f(-1f, 0f, 0),new ColorRGBA(.72f,.97f,1f,1f).mult(.4f));
		rootNode.addLight(dl);
		dl=new DirectionalLight(new Vector3f(1f, 0f, 0),new ColorRGBA(.72f,.97f,1f,1f).mult(.4f));
		rootNode.addLight(dl);
		System.out.println("Create floor");
		Commons.makeFloor(this);
		System.out.println("Create scene");
		
		// ##################################################################
		VHACDParameters params=new VHACDParameters();
		params.setMaxVerticesPerHull(16);
		vhacd_factory=new VHACDCollisionShapeFactory(params);		
		new File("cache").mkdir();
    	vhacd_factory.cachingQueue().add(new PersistentByBuffersCaching("cache"));
		makeScene();		
		// ##################################################################
		
		System.out.println("Ready!");

	}


	boolean debug;
	@Override
	public void onAction(String name, boolean isPressed, float tpf) {
		if(!isPressed)return;
		System.out.println(name);
		switch(name){
			case "debug":
				bullet.setDebugEnabled(debug=!debug);
				break;
			case "fire!!!!!":
				Vector3f dir=cam.getRotation().mult(new Vector3f(0,0,1));			
				Geometry g=load("models/torus/torus.obj",new ColorRGBA(.43f,.52f,.86f,1f),20f,cam.getLocation().add(dir),new Vector3f(6,6,6));
				bullet.getPhysicsSpace().add(g);
				RigidBodyControl rb=g.getControl(RigidBodyControl.class);
				rb.applyImpulse(dir.mult(1339f),new Vector3f());
				rootNode.attachChild(g);
				break;
		}
	}
	
	
	public static void main(String[] args) {
		AppSettings settings=new AppSettings(true);
		 settings.setTitle("VHACD Test");

		 TestFunny app=new TestFunny();
		 app.setSettings(settings);
		 app.start();
	}
    
}
