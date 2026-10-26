"""Run inside Blender to audit the current scene and write a JSON report.
Usage: blender -b input.blend --python blender/audit_nova_scene.py -- --report /tmp/nova-audit.json
For a GLB, import it first in a .blend file or use Blender's Python console to run this script.
This script reports structure; it does not certify animation quality or deformation.
"""
import bpy, json, os, sys

args = sys.argv[sys.argv.index('--') + 1:] if '--' in sys.argv else []
report_path = os.path.abspath('nova_scene_audit.json')
if '--report' in args:
    report_path = os.path.abspath(args[args.index('--report') + 1])
meshes = [o for o in bpy.context.scene.objects if o.type == 'MESH']
armatures = [o for o in bpy.context.scene.objects if o.type == 'ARMATURE']
shape_keys = {}
for obj in meshes:
    if obj.data.shape_keys:
        shape_keys[obj.name] = [k.name for k in obj.data.shape_keys.key_blocks]
actions = sorted({a.name for a in bpy.data.actions})
report = {
  'blend_file': bpy.data.filepath or None,
  'mesh_objects': [{'name': o.name, 'vertices': len(o.data.vertices), 'polygons': len(o.data.polygons),
                    'armature_modifiers': [m.object.name for m in o.modifiers if m.type == 'ARMATURE' and m.object],
                    'vertex_groups': [g.name for g in o.vertex_groups]} for o in meshes],
  'armatures': [{'name': a.name, 'bones': [b.name for b in a.data.bones]} for a in armatures],
  'shape_keys_by_mesh': shape_keys,
  'actions': actions,
  'checks': {
    'has_mesh': bool(meshes), 'has_armature': bool(armatures), 'has_actions': bool(actions),
    'has_any_shape_keys': bool(shape_keys),
    'has_expected_nova_clips': all(n in actions for n in ['Nova_Idle','Nova_Listening','Nova_Thinking','Nova_Greeting','Nova_Speaking']),
    'has_expected_visemes': any(set(['jawOpen','viseme_A','viseme_O','viseme_E','viseme_M']).issubset(set(names)) for names in shape_keys.values())
  },
  'limitations': ['Structural audit only; does not verify animation motion, skin deformation, facial appearance, GLB export, or Filament playback.']
}
os.makedirs(os.path.dirname(report_path) or '.', exist_ok=True)
with open(report_path, 'w', encoding='utf-8') as f: json.dump(report, f, indent=2)
print(json.dumps(report, indent=2))
print('NOVA_AUDIT_REPORT=' + report_path)
