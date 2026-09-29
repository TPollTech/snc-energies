package com.snc.energies.client.vehicle;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * One-time mesh bake of the golden JSON. Every cuboid keeps its six full-texture
 * faces, authored ZYX rotation, material, and parent pivot. Only group transforms
 * change at render time; no cuboids, JSON, or textures are rebuilt per vehicle.
 */
final class TractorRig {
    private static final float RAD = (float) (Math.PI / 180.0);
    private static final Identifier MODEL = Identifier.fromNamespaceAndPath("snc_energies", "vehicle/tractor-model.json");
    private final List<Node> roots;

    private TractorRig(List<Node> roots) {
        this.roots = List.copyOf(roots);
    }

    static TractorRig load(ResourceManager resources) {
        try (Reader reader = resources.openAsReader(MODEL)) {
            JsonObject data = JsonParser.parseReader(reader).getAsJsonObject();
            float units = data.get("units_per_block").getAsFloat();
            if (units != 16) throw new IllegalArgumentException("SNC 75 model units must be 16 per block");
            Map<String, Node> nodes = new LinkedHashMap<>();
            for (JsonElement element : data.getAsJsonArray("groups")) {
                JsonObject group = element.getAsJsonObject();
                String name = group.get("name").getAsString();
                nodes.put(name, new Node(name, vector(group, "origin"), vector(group, "rotation")));
            }
            List<Node> roots = new ArrayList<>();
            for (JsonElement element : data.getAsJsonArray("groups")) {
                JsonObject group = element.getAsJsonObject();
                Node node = nodes.get(group.get("name").getAsString());
                JsonElement parentName = group.get("parent");
                if (parentName == null || parentName.isJsonNull()) {
                    roots.add(node);
                    node.offset.set(node.origin).div(units);
                } else {
                    Node parent = nodes.get(parentName.getAsString());
                    if (parent == null) throw new IllegalArgumentException("Missing tractor parent " + parentName);
                    parent.children.add(node);
                    node.offset.set(node.origin).sub(parent.origin).div(units);
                }
            }
            for (JsonElement element : data.getAsJsonArray("cubes")) {
                JsonObject cube = element.getAsJsonObject();
                Node node = nodes.get(cube.get("group").getAsString());
                if (node == null) throw new IllegalArgumentException("Missing tractor cube group");
                String material = cube.get("material").getAsString();
                List<Vertex> vertices = node.baking.computeIfAbsent(material, ignored -> new ArrayList<>());
                bakeCube(vertices, cube, node.origin, units);
            }
            nodes.values().forEach(Node::finishBake);
            return new TractorRig(roots);
        } catch (IOException | RuntimeException exception) {
            throw new IllegalStateException("Unable to bake approved SNC 75 model " + MODEL, exception);
        }
    }

    void submit(TractorRenderState state, PoseStack poses, SubmitNodeCollector collector) {
        for (Node node : roots) node.submit(state, poses, collector);
    }

    private static Vector3f vector(JsonObject object, String key) {
        JsonArray array = object.getAsJsonArray(key);
        return array == null ? new Vector3f() : new Vector3f(
            array.get(0).getAsFloat(), array.get(1).getAsFloat(), array.get(2).getAsFloat());
    }

    private static Quaternionf rotation(Vector3f degrees) {
        return new Quaternionf().rotationZYX(degrees.z * RAD, degrees.y * RAD, degrees.x * RAD);
    }

    private static void bakeCube(List<Vertex> vertices, JsonObject cube, Vector3f groupOrigin, float units) {
        Vector3f low = vector(cube, "from");
        Vector3f high = vector(cube, "to");
        Vector3f center = new Vector3f(low).add(high).mul(0.5F);
        Vector3f pivot = cube.has("origin") ? vector(cube, "origin") : center;
        Quaternionf orientation = rotation(vector(cube, "rotation"));
        float x0 = low.x, y0 = low.y, z0 = low.z;
        float x1 = high.x, y1 = high.y, z1 = high.z;
        // Same face layout and full-face UVs as the preview's Three.BoxGeometry.
        face(vertices, new float[][] {{x1,y1,z1},{x1,y1,z0},{x1,y0,z0},{x1,y0,z1}}, 1,0,0, pivot,groupOrigin,orientation,units);
        face(vertices, new float[][] {{x0,y1,z0},{x0,y1,z1},{x0,y0,z1},{x0,y0,z0}}, -1,0,0, pivot,groupOrigin,orientation,units);
        face(vertices, new float[][] {{x0,y1,z0},{x1,y1,z0},{x1,y1,z1},{x0,y1,z1}}, 0,1,0, pivot,groupOrigin,orientation,units);
        face(vertices, new float[][] {{x0,y0,z1},{x1,y0,z1},{x1,y0,z0},{x0,y0,z0}}, 0,-1,0, pivot,groupOrigin,orientation,units);
        face(vertices, new float[][] {{x0,y1,z1},{x1,y1,z1},{x1,y0,z1},{x0,y0,z1}}, 0,0,1, pivot,groupOrigin,orientation,units);
        face(vertices, new float[][] {{x1,y1,z0},{x0,y1,z0},{x0,y0,z0},{x1,y0,z0}}, 0,0,-1, pivot,groupOrigin,orientation,units);
    }

    private static void face(List<Vertex> vertices, float[][] corners, float nx, float ny, float nz,
                             Vector3f pivot, Vector3f groupOrigin, Quaternionf orientation, float units) {
        Vector3f normal = new Vector3f(nx, ny, nz).rotate(orientation);
        // Native entity quads use counter-clockwise front faces. Three's grid
        // lists its corners in the reverse order, so walk 0,3,2,1 here.
        int[] winding = {0, 3, 2, 1};
        for (int corner : winding) {
            float[] value = corners[corner];
            Vector3f position = new Vector3f(value[0], value[1], value[2])
                .sub(pivot).rotate(orientation).add(pivot).sub(groupOrigin).div(units);
            float u = corner == 1 || corner == 2 ? 1 : 0;
            float v = corner >= 2 ? 1 : 0;
            vertices.add(new Vertex(position.x, position.y, position.z, u, v, normal.x, normal.y, normal.z));
        }
    }

    private record Vertex(float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        void emit(PoseStack.Pose pose, VertexConsumer buffer, int light, int color) {
            buffer.addVertex(pose, x, y, z).setColor(color).setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
        }
    }

    private record Mesh(RenderType renderType, RenderType outlineType, Vertex[] vertices) {
        void submit(PoseStack poses, SubmitNodeCollector collector, int light, int outline) {
            collector.submitCustomGeometry(poses, renderType, (pose, buffer) -> {
                for (Vertex vertex : vertices) vertex.emit(pose, buffer, light, -1);
            });
            if (outline != 0) {
                collector.submitCustomGeometry(poses, outlineType, (pose, buffer) -> {
                    for (Vertex vertex : vertices) vertex.emit(pose, buffer, light, outline);
                });
            }
        }
    }

    private static final class Node {
        private final String name;
        private final Vector3f origin;
        private final Vector3f rest;
        private final Quaternionf restRotation;
        private final Vector3f offset = new Vector3f();
        private final List<Node> children = new ArrayList<>();
        private Map<String, List<Vertex>> baking = new LinkedHashMap<>();
        private List<Mesh> meshes;

        private Node(String name, Vector3f origin, Vector3f rest) {
            this.name = name;
            this.origin = origin;
            this.rest = rest;
            this.restRotation = rotation(rest);
        }

        private void finishBake() {
            List<Mesh> result = new ArrayList<>();
            baking.forEach((material, vertices) -> {
                Identifier texture = Identifier.fromNamespaceAndPath("snc_energies", "textures/entity/tractor/" + material + ".png");
                result.add(new Mesh(RenderTypes.entitySolid(texture), RenderTypes.outline(texture), vertices.toArray(Vertex[]::new)));
            });
            meshes = List.copyOf(result);
            baking = null;
        }

        private void submit(TractorRenderState state, PoseStack poses, SubmitNodeCollector collector) {
            if (name.equals("planter") && !state.planter) return;
            poses.pushPose();
            poses.translate(offset.x, offset.y, offset.z);
            float x = rest.x, y = rest.y, z = rest.z;
            // Match the studio's Euler offsets and physical rolling radii. The
            // entity reports signed rear-wheel degrees (0.70-block radius).
            switch (name) {
                case "rear_left_wheel", "rear_right_wheel" -> x -= state.wheelRotation;
                case "front_left_wheel", "front_right_wheel" -> x -= state.wheelRotation * (11.2F / 8.3F);
                case "planter_left_wheel", "planter_right_wheel" -> {
                    if (state.working && !state.planterRaised) x -= state.wheelRotation * (11.2F / 5.4F);
                }
                case "row_1_press_wheel", "row_2_press_wheel", "row_3_press_wheel" -> {
                    if (state.working && !state.planterRaised) x -= state.wheelRotation * (11.2F / 3.8F);
                }
                case "front_left_steering", "front_right_steering" -> y -= state.steering;
                case "steering_wheel" -> z += state.steering * 4;
                case "planter" -> { if (state.planterRaised) x -= 23; }
                default -> { }
            }
            if (x == rest.x && y == rest.y && z == rest.z) poses.rotate(restRotation);
            else poses.rotate(new Quaternionf().rotationZYX(z * RAD, y * RAD, x * RAD));
            for (Mesh mesh : meshes) mesh.submit(poses, collector, state.lightCoords, state.outlineColor);
            for (Node child : children) child.submit(state, poses, collector);
            poses.popPose();
        }
    }
}
