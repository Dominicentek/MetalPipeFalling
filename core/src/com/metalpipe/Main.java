package com.metalpipe;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.loaders.ModelLoader;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.environment.DirectionalLight;
import com.badlogic.gdx.graphics.g3d.loader.ObjLoader;
import com.badlogic.gdx.graphics.g3d.utils.CameraInputController;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.bullet.Bullet;
import com.badlogic.gdx.physics.bullet.collision.*;
import com.badlogic.gdx.physics.bullet.dynamics.*;
import com.badlogic.gdx.physics.bullet.linearmath.btMotionState;
import com.badlogic.gdx.utils.Array;

import java.util.Scanner;

public class Main extends ApplicationAdapter {
	public static PerspectiveCamera cam;
	public static CameraInputController camController;
	public static ModelBatch modelBatch;
	public static Array<ModelInstance> instances;
	public static Environment environment;
	public static ModelInstance box;
	public static ModelInstance pipe;
	public static btCollisionShape boxShape;
	public static btCollisionShape pipeShape;
	public static btRigidBody boxObject;
	public static btRigidBody pipeObject;
	public static btCollisionConfiguration collisionConfig;
	public static btDispatcher dispatcher;
	public static btBroadphaseInterface broadphase;
	public static btDynamicsWorld dynamicsWorld;
	public static btConstraintSolver constraintSolver;
	public static boolean playedSound = false;
	public static Sound sound;
	public void create() {
		Bullet.init();
		modelBatch = new ModelBatch();
		environment = new Environment();
		environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
		environment.add(new DirectionalLight().set(0.8f, 0.8f, 0.8f, -1f, -0.8f, -0.2f));
		cam = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.position.set(3f, 7f, 10f);
		cam.lookAt(0, 4f, 0);
		cam.update();
		camController = new CameraInputController(cam);
		Gdx.input.setInputProcessor(camController);
		instances = new Array<>();
		ModelBuilder builder = new ModelBuilder();
		builder.begin();
		builder.node().id = "box";
		builder.part("box", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal, new Material(ColorAttribute.createDiffuse(Color.RED))).box(5f, 1f, 5f);
		builder.node().id = "pipe";
		MeshPartBuilder part = builder.part("pipe", GL20.GL_TRIANGLES, VertexAttributes.Usage.Position | VertexAttributes.Usage.Normal | VertexAttributes.Usage.TextureCoordinates, new Material(ColorAttribute.createDiffuse(Color.GRAY)));
		Array<Mesh> meshes = new ObjLoader().loadModel(Gdx.files.internal("pipe.obj")).meshes;
		for (Mesh mesh : meshes) {
			part.addMesh(mesh);
		}
		Model model = builder.end();
		box = new ModelInstance(model, "box");
		pipe = new ModelInstance(model, "pipe");
		pipe.transform.setFromEulerAngles(MathUtils.random(360f), MathUtils.random(360f), MathUtils.random(360f));
		pipe.transform.trn(0, 9, 0);
		instances = new Array<>();
		instances.add(box);
		instances.add(pipe);
		boxShape = new btBoxShape(new Vector3(2.5f, 0.5f, 2.5f));
		pipeShape = new btCylinderShape(new Vector3(0.2f, 2f, 0.2f));
		Vector3 pipeLocalInertia = new Vector3();
		pipeShape.calculateLocalInertia(1f, pipeLocalInertia);
		boxObject = new btRigidBody(0f, null, boxShape, new Vector3(0, 0, 0));
		boxObject.setCollisionShape(boxShape);
		boxObject.setWorldTransform(box.transform);
		pipeObject = new btRigidBody(1f, null, pipeShape, pipeLocalInertia);
		pipeObject.setCollisionShape(pipeShape);
		pipeObject.setWorldTransform(pipe.transform);
		collisionConfig = new btDefaultCollisionConfiguration();
		dispatcher = new btCollisionDispatcher(collisionConfig);
		broadphase = new btDbvtBroadphase();
		constraintSolver = new btSequentialImpulseConstraintSolver();
		dynamicsWorld = new btDiscreteDynamicsWorld(dispatcher, broadphase, constraintSolver, collisionConfig);
		dynamicsWorld.setGravity(new Vector3(0, -10f, 0));
		boxObject.setCollisionFlags(boxObject.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_KINEMATIC_OBJECT);
		boxObject.setContactCallbackFlag(1 << 8);
		boxObject.setContactCallbackFilter(0);
		boxObject.setActivationState(Collision.DISABLE_DEACTIVATION);
		pipeObject.proceedToTransform(pipe.transform);
		pipeObject.setCollisionFlags(pipeObject.getCollisionFlags() | btCollisionObject.CollisionFlags.CF_CUSTOM_MATERIAL_CALLBACK);
		pipeObject.setContactCallbackFlag(1 << 9);
		pipeObject.setContactCallbackFilter(1 << 8);
		pipeObject.setMotionState(new MotionState(pipe.transform));
		dynamicsWorld.addRigidBody(boxObject);
		dynamicsWorld.addRigidBody(pipeObject);
		sound = Gdx.audio.newSound(Gdx.files.internal("sound.wav"));
		new ContactListener() {
			public boolean onContactAdded (int userValue0, int partId0, int index0, boolean match0, int userValue1, int partId1, int index1, boolean match1) {
				if (!playedSound) {
					playedSound = true;
					sound.play();
					new Thread(() -> {
						try {
							Thread.sleep(2500);
							System.exit(0);
						}
						catch (Exception e) {
							e.printStackTrace();
						}
					}).start();
				}
				return true;
			}
		};
	}
	public void render() {
		float delta = Math.min(1f / 30f, Gdx.graphics.getDeltaTime());
		dynamicsWorld.stepSimulation(delta, 5, 1f / 60f);
		camController.update();
		Gdx.gl.glClearColor(0.3f, 0.3f, 0.3f, 1.f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
		modelBatch.begin(cam);
		modelBatch.render(instances, environment);
		modelBatch.end();
	}
	public static class MotionState extends btMotionState {
		public Matrix4 transform;
		public MotionState(Matrix4 transform) {
			this.transform = transform;
		}
		public void getWorldTransform(Matrix4 worldTrans) {
			worldTrans.set(transform);
		}
		public void setWorldTransform(Matrix4 worldTrans) {
			transform.set(worldTrans);
		}
	}
}
