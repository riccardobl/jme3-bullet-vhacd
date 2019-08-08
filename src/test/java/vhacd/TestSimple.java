package vhacd;

import com.jme3.app.SimpleApplication;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.bullet.vhacd.VHACDCollisionShapeFactory;
import com.jme3.scene.Geometry;
import com.jme3.scene.Spatial;

public class TestSimple extends SimpleApplication{

    
	@Override
	public void simpleInitApp() {       
		Commons.initApp(this);
		getFlyByCamera().setDragToRotate(true);

		
		BulletAppState bullet=stateManager.getState(BulletAppState.class);

		bullet.setDebugEnabled(true);
		
		Spatial m=assetManager.loadModel("models/wasp/wasp.obj");
		Geometry g=Commons.getGeom(m);
		
		VHACDCollisionShapeFactory csf=new VHACDCollisionShapeFactory();
		CollisionShape cs=csf.create(g);
			
		RigidBodyControl	rb=new RigidBodyControl(cs,0);
		bullet.getPhysicsSpace().add(rb);
	
	}

	public static void main(String[] args) {
		new TestSimple().start();
	}
}
