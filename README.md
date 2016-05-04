#JME3 V-HACD Collision Shape Factory

This is a facility that uses [java bindings](https://github.com/riccardobl/v-hacd-java-bindings) for [Khaled Mamou's V-HACD](https://github.com/kmammou/v-hacd) to decompose concave meshes into hull-shapes in jmonkey engine.

#Requirements
java 1.7+

#Installation
##Gradle
```gradle
repositories {
    maven { url "https://jitpack.io" }
}

dependencies {
    compile 'com.github.riccardobl:jme3-bullet-vhacd:-SNAPSHOT'
}
```


##Other managers
https://jitpack.io/#riccardobl/jme3-bullet-vhacd

____
This library relies on vhacd native bindings whose build is currently available only for Linux and Windows.

#Usage
```java
VHACDParameters p=new VHACDParameters();
// p.set.....

VHACDCollisionShapeFactory factory=new VHACDCollisionShapeFactory(p);		
CompoundCollisionShape cs=factory.create(Spatial);
```
The resulting CollisionShape can be used and serialized as a usual. See `TestSimple.java` and `TestFunny.java` for the full example.
___________
The process of building collision shapes could be quite slow, to overcome this you can use one of the following approaches 

##Pregeneration and serialization
The generated shape is a common `CompoundCollisionShape` made of `HullCollisionShape`s this means it can be [saved and loaded with jmonkeyengine](https://wiki.jmonkeyengine.org/doku.php/jme3:advanced:save_and_load).

This method has a further benefit: the shape can be generated by the developer and then loaded withing a project that doesn't need to include this library. 
This comes quite handy when you are targeting platforms that are not supported by this library.

##Caching
The library can also do transparent caching.

To do this, you just need to add an implementation of `Caching` to  the `VHACDCollisionShapeFactory` with
```java
((VHACDCollisionShapeFactory)factory).cachingQueue().add(Caching)
```
Multiple Caching systems can be added to the queue, when one fails to provide the cache, the next one is used.

There are few built-in Caching implementations: 

##VolatileByMeshCaching
This provides a minimal in-memory caching based on mesh objects.

With this, when you call `VHACDCollisionShapeFactory.create` on a mesh or spatial for which the collision shape has already been generated with the same parameters, a cached version may be returned.

The shapes leave the cache when their original mesh has been GCed or when the application is restarted.
```java
((VHACDCollisionShapeFactory)factory).cachingQueue().add(new VolatileByMeshCaching());
```
##PersistentByBuffersCaching
This is a more advanced system that provides persistent caching on memory and disk, based on the content of the the original mesh.

It must be create by passing the path of the cache folder to the constructor
```java
PersistentByBuffersCaching caching=new PersistentByBuffersCaching("app/cache/");
((VHACDCollisionShapeFactory)factory).cachingQueue().add(caching);
```
(The `/` will be automatically replaced with the `File.separator`)

You can optionally set an indicative maximum ammount of memory that can be used for the in-memory caching with:
```java
caching.setMaxMemoryInMB(10);
```
By default the limit is 32MB. A negative number means *unlimited*  and *0* means "no in-memory cache (= cache provided always from the disk)".

The cache will be swapped from disk to memory automatically and will be kept across executions, installations and can also be shipped with the software. 




#License
Everything in this repo, as well as V-HACD and V-HACD-bindings, is released under BSD 3-clause license.


#P.S.
Everything here is half untested and experimental, if you find an issue, please report it :).