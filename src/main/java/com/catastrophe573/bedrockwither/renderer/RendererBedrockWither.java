package com.catastrophe573.bedrockwither.renderer;

import com.catastrophe573.bedrockwither.entity.EntityBedrockWither;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
// this class was based on the vanilla WitherBossRenderer
// net.minecraft.client.renderer.entity.WitherBossRenderer
public class RendererBedrockWither extends MobRenderer<EntityBedrockWither, ModelBedrockWither<EntityBedrockWither>>
{
	private static final ResourceLocation WITHER_INVULNERABLE_LOCATION = new ResourceLocation("textures/entity/wither/wither_invulnerable.png");
	private static final ResourceLocation WITHER_LOCATION = new ResourceLocation("textures/entity/wither/wither.png");

	public RendererBedrockWither(EntityRendererProvider.Context context)
	{
		super(context, new ModelBedrockWither<EntityBedrockWither>(context.bakeLayer(ModelLayers.WITHER)), 1.0F);
		this.addLayer(new LayerBedrockWitherArmor(this, context.getModelSet()));
	}

	protected int getBlockLightLevel(EntityBedrockWither pEntity, BlockPos pPos)
	{
		return 15;
	}

	// Returns the location of an entity's texture.
	public ResourceLocation getTextureLocation(EntityBedrockWither pEntity)
	{
		int i = pEntity.getInvulnerableTicks();
		return i > 0 && (i > 80 || i / 5 % 2 != 1) ? WITHER_INVULNERABLE_LOCATION : WITHER_LOCATION;
	}

	protected void scale(EntityBedrockWither pLivingEntity, PoseStack pMatrixStack, float pPartialTickTime)
	{
		float f = 2.0F;
		int i = pLivingEntity.getInvulnerableTicks();
		if (i > 0)
		{
			f -= ((float) i - pPartialTickTime) / 220.0F * 0.5F;
		}

		pMatrixStack.scale(f, f, f);
	}
}
