package org.alex_melan.spacereloaded.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

/**
 * Светящаяся геометрия для анимаций мультиблоков 004 (RenderTypes.lightning — позиция + цвет,
 * аддитивно, без текстуры и освещения): кубы-«ореолы» чуть больше блока и квады окон.
 */
final class GlowGeometry {

    private GlowGeometry() {
    }

    /** Куб [x0..x1]×[y0..y1]×[z0..z1] цвета ARGB — все 6 граней. */
    static void cube(PoseStack.Pose pose, VertexConsumer vc, float x0, float y0, float z0,
                     float x1, float y1, float z1, int argb) {
        // низ, верх
        quad(pose, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, argb);
        quad(pose, vc, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, argb);
        // север, юг
        quad(pose, vc, x0, y0, z0, x0, y1, z0, x1, y1, z0, x1, y0, z0, argb);
        quad(pose, vc, x1, y0, z1, x1, y1, z1, x0, y1, z1, x0, y0, z1, argb);
        // запад, восток
        quad(pose, vc, x0, y0, z1, x0, y1, z1, x0, y1, z0, x0, y0, z0, argb);
        quad(pose, vc, x1, y0, z0, x1, y1, z0, x1, y1, z1, x1, y0, z1, argb);
    }

    static void quad(PoseStack.Pose pose, VertexConsumer vc,
                     float ax, float ay, float az, float bx, float by, float bz,
                     float cx, float cy, float cz, float dx, float dy, float dz, int argb) {
        vc.addVertex(pose, ax, ay, az).setColor(argb);
        vc.addVertex(pose, bx, by, bz).setColor(argb);
        vc.addVertex(pose, cx, cy, cz).setColor(argb);
        vc.addVertex(pose, dx, dy, dz).setColor(argb);
    }

    /** ARGB с альфой 0..1. */
    static int argb(float alpha, int rgb) {
        int a = Math.max(0, Math.min(255, Math.round(alpha * 255)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
