package its_meow.openscreens.client.renderer.tileentity;

import java.util.Set;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GLContext;

import com.google.common.collect.Sets;

import its_meow.openscreens.OpenScreens;
import its_meow.openscreens.common.tileentity.TileEntityFlatScreen;
import li.cil.oc.Settings;
import li.cil.oc.integration.util.Wrench;
import li.cil.oc.util.RenderState;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;

public class RenderFlatScreen extends TileEntitySpecialRenderer<TileEntityFlatScreen> {

    public double maxRenderDistanceSq = Settings.get().maxScreenTextRenderDistance() * Settings.get().maxScreenTextRenderDistance();

    public double fadeDistanceSq = Settings.get().screenTextFadeStartDistance() * Settings.get().screenTextFadeStartDistance();

    public double fadeRatio = 1.0D / (maxRenderDistanceSq - fadeDistanceSq);

    public TileEntityFlatScreen screen = null;

    public Set<Block> screens = Sets.newHashSet(OpenScreens.FLAT_SCREEN_BLOCK);

    public boolean canUseBlendColor = GLContext.getCapabilities().OpenGL14;

    public static final ResourceLocation UP_ARROW = new ResourceLocation(Settings.resourceDomain(), "textures/blocks/overlay/screen_up_indicator");

    @Override
    public void render(TileEntityFlatScreen te, double x, double y, double z, float partialTicks, int destroyStage, float a) {
        RenderState.checkError(this.getClass().getName() + ".render: entering (aka: wasntme)");

        this.screen = te;
        if (!screen.isOrigin()) {
            return;
        }

        double distance = playerDistanceSq() / Math.min(screen.width(), screen.height());
        if (distance > maxRenderDistanceSq) {
            return;
        }

        RenderState.checkError(this.getClass().getName() + ".render: checks");

        RenderState.pushAttrib();

        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 0xFF, 0xFF);
        RenderState.disableEntityLighting();
        RenderState.makeItBlend();
        GlStateManager.color(1, 1, 1, 1);

        GlStateManager.pushMatrix();

        GlStateManager.translate(x + 0.5, y + 0.5, z + 0.5);

        RenderState.checkError(this.getClass().getName() + ".render: setup");

        drawOverlay();

        RenderState.checkError(this.getClass().getName() + ".render: overlay");

        if (distance > fadeDistanceSq) {
            float alpha = (float) Math.max(0, 1 - ((distance - fadeDistanceSq) * fadeRatio));
            if (canUseBlendColor) {
                GL14.glBlendColor(0, 0, 0, alpha);
                GlStateManager.blendFunc(GL11.GL_CONSTANT_ALPHA, GL11.GL_ONE);
            }
        }

        RenderState.checkError(this.getClass().getName() + ".render: fade");

        if (screen.buffer().isRenderingEnabled()) {
            draw();
        }

        RenderState.disableBlend();
        RenderState.enableEntityLighting();

        GlStateManager.popMatrix();
        RenderState.popAttrib();

        RenderState.checkError(this.getClass().getName() + ".render: leaving");
    }

    public void transform() {
        switch(screen.yaw()) {
        case WEST: GlStateManager.rotate(-90, 0, 1, 0); break;
        case NORTH: GlStateManager.rotate(180, 0, 1, 0); break;
        case EAST: GlStateManager.rotate(90, 0, 1, 0); break;
        default: break;
        }
        
        switch(screen.pitch()) {
        case DOWN: GlStateManager.rotate(90, 1, 0, 0); break;//GlStateManager.translate(0, screen.height() / 2 , -screen.height() + 1); break;
        case UP: GlStateManager.rotate(-90, 1, 0, 0); break;//GlStateManager.translate(0, 0, screen.height()); break;
        default: break;
        }



        // Fit area to screen (bottom left = bottom left).
        GlStateManager.translate(-0.5f, -0.5f, 0.5f);

        GlStateManager.translate(0, screen.height(), 0);




        // Flip text upside down.
        GlStateManager.scale(1, -1, 1);

    }

    private void drawOverlay() {
        if (screen.facing() == EnumFacing.UP || screen.facing() == EnumFacing.DOWN) {
            // Show up vector overlay when holding same screen block.
            ItemStack stack = Minecraft.getMinecraft().player.getHeldItemMainhand();
            if (!stack.isEmpty()) {
                if (Wrench.holdsApplicableWrench(Minecraft.getMinecraft().player, screen.getPos()) || screens.contains(Block.getBlockFromItem(stack.getItem()))) {
                    GlStateManager.pushMatrix();
                    transform();
                    GlStateManager.depthMask(false);
                    GlStateManager.translate(screen.width() / 2f - 0.5f, screen.height() / 2f - 0.5f, 0.05f);

                    Tessellator t = Tessellator.getInstance();
                    BufferBuilder r = t.getBuffer();

                    r.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
                    this.bindTexture(UP_ARROW);

                    TextureAtlasSprite icon = Minecraft.getMinecraft().getTextureMapBlocks().getAtlasSprite(UP_ARROW.toString());

                    r.pos(0, 1, 0).tex(icon.getMinU(), icon.getMaxV()).endVertex();
                    r.pos(1, 1, 0).tex(icon.getMaxU(), icon.getMaxV()).endVertex();
                    r.pos(1, 0, 0).tex(icon.getMaxU(), icon.getMinV()).endVertex();
                    r.pos(0, 0, 0).tex(icon.getMinU(), icon.getMinV()).endVertex();

                    t.draw();

                    GlStateManager.depthMask(true);
                    GlStateManager.popMatrix();
                }
            }
        }
    }

    private void draw() {
        RenderState.checkError(this.getClass().getName() + ".draw: entering (aka: wasntme)");

        float sx = screen.width();
        float sy = screen.height();
        float tw = sx * 16f;
        float th = sy * 16f;

        transform();

        // Offset from border.
        GlStateManager.translate(sx * 2.25f / tw, sy * 2.25f / th, 0);

        // Inner size (minus borders).
        float isx = sx - (4.5f / 16);
        float isy = sy - (4.5f / 16);

        // Scale based on actual buffer size.
        float sizeX = screen.buffer().renderWidth();
        float sizeY = screen.buffer().renderHeight();
        float scaleX = isx / sizeX;
        float scaleY = isy / sizeY;
        if (scaleX > scaleY) {
            GlStateManager.translate(sizeX * 0.5f * (scaleX - scaleY), 0, 0);
            GlStateManager.scale(scaleY, scaleY, 1);
        } else {
            GlStateManager.translate(0, sizeY * 0.5f * (scaleY - scaleX), 0);
            GlStateManager.scale(scaleX, scaleX, 1);
        }

        // Slightly offset the text so it doesn't clip into the screen.
        GlStateManager.translate(0, 0, -0.94 + 0.01);

        RenderState.checkError(this.getClass().getName() + ".draw: setup");

        // Render the actual text.
        screen.buffer().renderText();

        RenderState.checkError(this.getClass().getName() + ".draw: text");
    }

    public double playerDistanceSq() {
        EntityPlayer player = Minecraft.getMinecraft().player;
        AxisAlignedBB bounds = screen.getRenderBoundingBox();

        double px = player.posX;
        double py = player.posY;
        double pz = player.posZ;

        double ex = bounds.maxX - bounds.minX;
        double ey = bounds.maxY - bounds.minY;
        double ez = bounds.maxZ - bounds.minZ;
        double cx = bounds.minX + ex * 0.5;
        double cy = bounds.minY + ey * 0.5;
        double cz = bounds.minZ + ez * 0.5;
        double dx = px - cx;
        double dy = py - cy;
        double dz = pz - cz;

        return fixDiff(dx, ex) + fixDiff(dy, ey) + fixDiff(dz, ez);
    }

    private static double fixDiff(double a, double b) {
        if (a < -b) {
            double d = a + b;
            return d * d;
        } else if (a > b) {
            double d = a - b;
            return d * d;
        } else {
            return 0D;
        }
    }

}