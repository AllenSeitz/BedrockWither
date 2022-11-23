package com.catastrophe573.bedrockwither.renderer;

import com.catastrophe573.bedrockwither.entity.EntityBedrockWither;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EnergySwirlLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
// based on net.minecraft.client.renderer.entity.layers.WitherArmorLayer
public class LayerBedrockWitherArmor extends EnergySwirlLayer<EntityBedrockWither, ModelBedrockWither<EntityBedrockWither>>
{
	private static final ResourceLocation WITHER_ARMOR_LOCATION = new ResourceLocation("textures/entity/wither/wither_armor.png");
	private final ModelBedrockWither<EntityBedrockWither> model;

	public LayerBedrockWitherArmor(RenderLayerParent<EntityBedrockWither, ModelBedrockWither<EntityBedrockWither>> model, EntityModelSet modelSet)
	{
		super(model);
		this.model = new ModelBedrockWither<>(modelSet.bakeLayer(ModelLayers.WITHER_ARMOR));
	}

	protected float xOffset(float angle)
	{
		return Mth.cos(angle * 0.02F) * 3.0F;
	}

	public ResourceLocation getTextureLocation()
	{
		return WITHER_ARMOR_LOCATION;
	}

	protected EntityModel<EntityBedrockWither> model()
	{
		return this.model;
	}
}
