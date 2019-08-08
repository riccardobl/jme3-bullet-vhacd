package vhacd;

import java.io.File;
import java.util.List;

import com.jme3.app.SimpleApplication;
import com.jme3.asset.AssetManager;
import com.jme3.asset.plugins.FileLocator;
import com.jme3.asset.plugins.UrlLocator;
import com.jme3.bullet.BulletAppState;
import com.jme3.bullet.BulletAppState.ThreadingType;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.bullet.collision.shapes.MeshCollisionShape;
import com.jme3.bullet.control.RigidBodyControl;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Box;

public class Commons{

    public static Geometry getGeom(Spatial s){
    	if(s instanceof Geometry)return (Geometry)s;
    	else if(s instanceof Node){
    		List<Spatial> children=((Node)s).getChildren();
    		for(Spatial child:children){
    			Geometry g=getGeom(child);
    			if(g!=null)return g;
    		}
    	}
    	return null;
    }
    
    public static void loadTestData(AssetManager am){
		System.out.println("Assets will be loaded from https://github.com/riccardobl/TestData/raw/master/assets");
		System.out.println("It might take some time, depending on your internet connection.");
        am.registerLocator("https://github.com/riccardobl/TestData/raw/master/assets/",UrlLocator.class);
    }

	public static void initApp(SimpleApplication simpleapp) {
		loadTestData(simpleapp.getAssetManager());
		
		simpleapp.getFlyByCamera().setMoveSpeed(200f);
		simpleapp.setPauseOnLostFocus(false);		
		BulletAppState bullet=new BulletAppState();
		bullet.setThreadingType(ThreadingType.PARALLEL);

		simpleapp.getStateManager().attach(bullet);
	}
	

    public static void makeFloor(SimpleApplication app) {    	
        Box box=new Box(160,.2f,160);
        Geometry floor=new Geometry("the Floor",box);
        floor.setLocalTranslation(0,-4f,0);
        Material mat1=new Material(app.getAssetManager(),"Common/MatDefs/Light/Lighting.j3md");
        mat1.setColor("Diffuse",new ColorRGBA(.54f,.68f,.16f,1f));
        mat1.setBoolean("UseMaterialColors",true);
        floor.setMaterial(mat1);
        
        CollisionShape floorcs=new MeshCollisionShape(floor.getMesh());
		RigidBodyControl floor_phy=new RigidBodyControl(floorcs,0);
		floor.addControl(floor_phy);
		floor_phy.setPhysicsLocation(new Vector3f(0,-30,0));		
		app.getStateManager().getState(BulletAppState.class).getPhysicsSpace().add(floor_phy);		
		app.getRootNode().attachChild(floor);			
    }
}
