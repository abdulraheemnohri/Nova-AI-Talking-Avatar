"""Build a lightweight, stylized Nova prototype and export animated GLB.
Run with Blender 3.6 LTS or Blender 4.x:
  blender -b --python blender/build_nova_prototype.py

This is a procedural prototype for pipeline validation, not a final photoreal Meshy character.
It creates separate rigid-weighted mesh parts, a humanoid armature and simple animation actions.
Facial blendshapes, realistic hands, cloth simulation and production skinning are not included.
"""
import bpy, math, os
from mathutils import Vector

OUT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'assets', 'models', 'nova_prototype.glb'))
os.makedirs(os.path.dirname(OUT), exist_ok=True)

# Reset scene
bpy.ops.object.select_all(action='SELECT'); bpy.ops.object.delete(use_global=False)

def mat(name, color, metallic=0.0, roughness=0.45):
    m=bpy.data.materials.new(name); m.diffuse_color=(*color,1); m.use_nodes=True
    bs=m.node_tree.nodes.get('Principled BSDF'); bs.inputs['Base Color'].default_value=(*color,1)
    bs.inputs['Metallic'].default_value=metallic; bs.inputs['Roughness'].default_value=roughness
    return m

ivory=mat('Nova | ceramic ivory',(0.78,0.84,0.88),0.18,0.28)
charcoal=mat('Nova | graphite',(0.055,0.075,0.095),0.28,0.3)
cyan=mat('Nova | cyan light',(0.02,0.72,0.95),0.35,0.22)
skin=mat('Nova | warm neutral',(0.62,0.42,0.32),0.0,0.5)

# Skeleton built in edit mode; coordinates use Z-up.
arm_data=bpy.data.armatures.new('NovaRig')
rig=bpy.data.objects.new('Nova_Rig',arm_data)
bpy.context.collection.objects.link(rig)
bpy.context.view_layer.objects.active=rig
rig.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
bones={
 'root':((0,0,0),(0,0,0.85),None),
 'spine':((0,0,0.85),(0,0,1.75),'root'),
 'neck':((0,0,1.75),(0,0,2.05),'spine'),
 'head':((0,0,2.05),(0,0,2.85),'neck'),
 'upper_arm.L':((0,0,1.68),(0.52,0,1.58),'spine'),
 'forearm.L':((0.52,0,1.58),(0.88,0,1.38),'upper_arm.L'),
 'hand.L':((0.88,0,1.38),(1.02,0,1.34),'forearm.L'),
 'upper_arm.R':((0,0,1.68),(-0.52,0,1.58),'spine'),
 'forearm.R':((-0.52,0,1.58),(-0.88,0,1.38),'upper_arm.R'),
 'hand.R':((-0.88,0,1.38),(-1.02,0,1.34),'forearm.R'),
 'thigh.L':((0.18,0,0.9),(0.22,0,0.48),'root'),
 'shin.L':((0.22,0,0.48),(0.22,0,0.12),'thigh.L'),
 'foot.L':((0.22,0,0.12),(0.22,-0.22,0.07),'shin.L'),
 'thigh.R':((-0.18,0,0.9),(-0.22,0,0.48),'root'),
 'shin.R':((-0.22,0,0.48),(-0.22,0,0.12),'thigh.R'),
 'foot.R':((-0.22,0,0.12),(-0.22,-0.22,0.07),'shin.R')}
for name,(h,t,parent) in bones.items():
    b=arm_data.edit_bones.new(name); b.head=h; b.tail=t
    if parent: b.parent=arm_data.edit_bones[parent]
bpy.ops.object.mode_set(mode='OBJECT')
rig.select_set(False)

def bind(obj, bone):
    # Rigid one-bone weighting is for the prototype only.
    bpy.context.view_layer.objects.active=obj
    obj.select_set(True)
    bpy.ops.object.transform_apply(location=False, rotation=True, scale=True)
    obj.select_set(False)
    vg=obj.vertex_groups.new(name=bone)
    vg.add(list(range(len(obj.data.vertices))),1.0,'REPLACE')
    mod=obj.modifiers.new('Nova Armature','ARMATURE'); mod.object=rig
    obj.parent=rig
    return obj

def sphere(name, loc, scale, material, bone):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=16, ring_count=10, location=loc)
    o=bpy.context.object; o.name=name; o.scale=scale
    bpy.ops.object.transform_apply(location=False, rotation=False, scale=True)
    o.data.materials.append(material); bind(o,bone)
    return o

def cylinder_between(name, a, b, radius, material, bone):
    a,b=Vector(a),Vector(b); d=b-a
    bpy.ops.mesh.primitive_cylinder_add(vertices=12, radius=radius, depth=d.length, location=(a+b)/2)
    o=bpy.context.object; o.name=name; o.data.materials.append(material)
    o.rotation_mode='QUATERNION'; o.rotation_quaternion=d.to_track_quat('Z','Y')
    bind(o,bone)
    return o

# Simplified stylized body components.
sphere('Pelvis',(0,0,0.83),(0.31,0.2,0.24),charcoal,'root')
sphere('Torso',(0,0,1.35),(0.36,0.22,0.55),ivory,'spine')
sphere('ChestCore',(0,-0.205,1.48),(0.19,0.035,0.24),charcoal,'spine')
sphere('CoreLight',(0,-0.24,1.52),(0.07,0.025,0.12),cyan,'spine')
cylinder_between('Neck',(0,0,1.72),(0,0,2.1),0.105,charcoal,'neck')
sphere('Head',(0,0,2.34),(0.27,0.23,0.38),skin,'head')
sphere('HairCap',(0,0.015,2.58),(0.28,0.235,0.19),charcoal,'head')
sphere('Eye.L',(0.105,-0.211,2.39),(0.045,0.025,0.025),cyan,'head')
sphere('Eye.R',(-0.105,-0.211,2.39),(0.045,0.025,0.025),cyan,'head')
mouth_obj=sphere('Mouth',(0,-0.222,2.26),(0.055,0.014,0.012),charcoal,'head')
mouth_obj.shape_key_add(name='Basis', from_mix=False)
def mouth_key(name, sx, sy, sz, offset_z=0.0):
    key=mouth_obj.shape_key_add(name=name, from_mix=False)
    for i, v in enumerate(mouth_obj.data.vertices):
        co=v.co.copy()
        key.data[i].co=(co.x*sx, co.y*sy, co.z*sz + offset_z)
    return key
mouth_key('jawOpen',1.0,1.0,2.8)
mouth_key('viseme_A',1.0,1.0,3.4)
mouth_key('viseme_O',0.72,1.0,2.5)
mouth_key('viseme_E',1.25,1.0,1.4)
mouth_key('viseme_M',0.75,1.0,0.45)

for side,sgn in [('L',1),('R',-1)]:
    cylinder_between('UpperArm.'+side,(0.27*sgn,0,1.66),(0.52*sgn,0,1.58),0.105,ivory,'upper_arm.'+side)
    sphere('Shoulder.'+side,(0.29*sgn,0,1.66),(0.14,0.15,0.14),charcoal,'upper_arm.'+side)
    cylinder_between('Forearm.'+side,(0.52*sgn,0,1.58),(0.88*sgn,0,1.38),0.078,ivory,'forearm.'+side)
    sphere('Hand.'+side,(0.96*sgn,0,1.36),(0.11,0.075,0.06),skin,'hand.'+side)
    cylinder_between('Thigh.'+side,(0.18*sgn,0,0.89),(0.22*sgn,0,0.48),0.14,charcoal,'thigh.'+side)
    cylinder_between('Shin.'+side,(0.22*sgn,0,0.48),(0.22*sgn,0,0.12),0.095,ivory,'shin.'+side)
    sphere('Boot.'+side,(0.22*sgn,-0.1,0.08),(0.13,0.23,0.08),charcoal,'foot.'+side)

def make_action(name, frames):
    bpy.context.view_layer.objects.active=rig
    rig.select_set(True); bpy.ops.object.mode_set(mode='POSE')
    for pb in rig.pose.bones: pb.rotation_mode='XYZ'
    bpy.ops.object.mode_set(mode='OBJECT')
    act=bpy.data.actions.new(name)
    rig.animation_data_create(); rig.animation_data.action=act
    for frame, pose in frames:
        bpy.context.scene.frame_set(frame)
        for pb in rig.pose.bones: pb.rotation_euler=(0.0,0.0,0.0)
        for bone_name, rot in pose.items():
            pb=rig.pose.bones.get(bone_name)
            if pb: pb.rotation_euler=rot
        for pb in rig.pose.bones:
            pb.keyframe_insert(data_path='rotation_euler',frame=frame,group=pb.name)
    act.use_fake_user=True
    return act

zero=(0.0,0.0,0.0)
make_action('Nova_Idle',[(1,{'spine':zero,'head':zero}),(24,{'spine':(0.015,0,0),'head':(0,-0.025,0)}),(48,{'spine':zero,'head':zero})])
make_action('Nova_Listening',[(1,{'head':zero}),(18,{'head':(0.04,0.02,0.04)}),(36,{'head':(0.015,0,0)}),(48,{'head':zero})])
make_action('Nova_Thinking',[(1,{'head':zero,'upper_arm.L':zero}),(16,{'head':(0.08,0,0.12),'upper_arm.L':(0.05,0,0.15)}),(32,{'head':(0.05,0,0.08),'upper_arm.L':(0.02,0,0.08)}),(48,{'head':zero,'upper_arm.L':zero})])
make_action('Nova_Greeting',[(1,{'upper_arm.R':zero,'forearm.R':zero}),(12,{'upper_arm.R':(0,0,-0.5),'forearm.R':(0.1,0,0.3)}),(22,{'upper_arm.R':(0,0,-0.45),'forearm.R':(-0.1,0,0.45)}),(32,{'upper_arm.R':(0,0,-0.5),'forearm.R':(0.1,0,0.3)}),(44,{'upper_arm.R':zero,'forearm.R':zero})])
make_action('Nova_Speaking',[(1,{'head':zero,'spine':zero}),(8,{'head':(0.015,0,0.025),'spine':(0.01,0,0)}),(16,{'head':(-0.01,0,-0.02),'spine':zero}),(24,{'head':zero,'spine':zero})])

# Export the rig, meshes, morph targets and actions. Blender exporter options vary by version.
bpy.ops.object.select_all(action='SELECT')
try:
    bpy.ops.export_scene.gltf(filepath=OUT, export_format='GLB', use_selection=True,
        export_animations=True, export_animation_mode='ACTIONS', export_nla_strips=True,
        export_skins=True, export_morph=True, export_materials='EXPORT', export_apply=True)
except TypeError:
    bpy.ops.export_scene.gltf(filepath=OUT, export_format='GLB', use_selection=True, export_animations=True)
print('NOVA_GLB_EXPORT=' + OUT)
print('NOTE: prototype only; validate clips and deformation in target Blender/Filament versions.')
