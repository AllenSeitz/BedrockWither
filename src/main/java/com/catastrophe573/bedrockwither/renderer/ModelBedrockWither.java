package com.catastrophe573.bedrockwither.renderer;

import com.catastrophe573.bedrockwither.entity.EntityBedrockWither;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
// this class was based on the vanilla WitherBossModel
// net.minecraft.client.model.WitherBossModel
public class ModelBedrockWither<T extends EntityBedrockWither> extends HierarchicalModel<T>
{
	private static final String RIBCAGE = "ribcage";
	private static final String TAIL = "tail";
	private static final String CENTER_HEAD = "center_head";
	private static final String RIGHT_HEAD = "right_head";
	private static final String LEFT_HEAD = "left_head";
	private static final float RIBCAGE_X_ROT_OFFSET = 0.065F;
	private static final float TAIL_X_ROT_OFFSET = 0.265F;
	private final ModelPart root;
	private final ModelPart centerHead;
	private final ModelPart rightHead;
	private final ModelPart leftHead;
	private final ModelPart ribcage;
	private final ModelPart tail;

	public ModelBedrockWither(ModelPart pRoot)
	{
		this.root = pRoot;
		this.ribcage = pRoot.getChild(RIBCAGE);
		this.tail = pRoot.getChild(TAIL);
		this.centerHead = pRoot.getChild(CENTER_HEAD);
		this.rightHead = pRoot.getChild(RIGHT_HEAD);
		this.leftHead = pRoot.getChild(LEFT_HEAD);
	}

	public static LayerDefinition createBodyLayer(CubeDeformation pCubeDeformation)
	{
		MeshDefinition meshdefinition = new MeshDefinition();
		PartDefinition partdefinition = meshdefinition.getRoot();
		partdefinition.addOrReplaceChild("shoulders", CubeListBuilder.create().texOffs(0, 16).addBox(-10.0F, 3.9F, -0.5F, 20.0F, 3.0F, 3.0F, pCubeDeformation), PartPose.ZERO);
		// float f = 0.20420352F;
		partdefinition.addOrReplaceChild(RIBCAGE, CubeListBuilder.create().texOffs(0, 22).addBox(0.0F, 0.0F, 0.0F, 3.0F, 10.0F, 3.0F, pCubeDeformation).texOffs(24, 22).addBox(-4.0F, 1.5F, 0.5F, 11.0F, 2.0F, 2.0F, pCubeDeformation).texOffs(24, 22).addBox(-4.0F, 4.0F, 0.5F, 11.0F, 2.0F, 2.0F, pCubeDeformation).texOffs(24, 22).addBox(-4.0F, 6.5F, 0.5F, 11.0F, 2.0F, 2.0F, pCubeDeformation), PartPose.offsetAndRotation(-2.0F, 6.9F, -0.5F, 0.20420352F, 0.0F, 0.0F));
		partdefinition.addOrReplaceChild(TAIL, CubeListBuilder.create().texOffs(12, 22).addBox(0.0F, 0.0F, 0.0F, 3.0F, 6.0F, 3.0F, pCubeDeformation), PartPose.offsetAndRotation(-2.0F, 6.9F + Mth.cos(0.20420352F) * 10.0F, -0.5F + Mth.sin(0.20420352F) * 10.0F, 0.83252203F, 0.0F, 0.0F));
		partdefinition.addOrReplaceChild(CENTER_HEAD, CubeListBuilder.create().texOffs(0, 0).addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, pCubeDeformation), PartPose.ZERO);
		CubeListBuilder cubelistbuilder = CubeListBuilder.create().texOffs(32, 0).addBox(-4.0F, -4.0F, -4.0F, 6.0F, 6.0F, 6.0F, pCubeDeformation);
		partdefinition.addOrReplaceChild(RIGHT_HEAD, cubelistbuilder, PartPose.offset(-8.0F, 4.0F, 0.0F));
		partdefinition.addOrReplaceChild(LEFT_HEAD, cubelistbuilder, PartPose.offset(10.0F, 4.0F, 0.0F));
		return LayerDefinition.create(meshdefinition, 64, 64);
	}

	public ModelPart root()
	{
		return this.root;
	}

	// Sets this entity's model rotation angles
	public void setupAnim(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pAgeInTicks, float pNetHeadYaw, float pHeadPitch)
	{
		float f = Mth.cos(pAgeInTicks * 0.1F);
		this.ribcage.xRot = (RIBCAGE_X_ROT_OFFSET + 0.05F * f) * (float) Math.PI;
		this.tail.setPos(-2.0F, 6.9F + Mth.cos(this.ribcage.xRot) * 10.0F, -0.5F + Mth.sin(this.ribcage.xRot) * 10.0F);
		this.tail.xRot = (TAIL_X_ROT_OFFSET + 0.1F * f) * (float) Math.PI;
		this.centerHead.yRot = pNetHeadYaw * ((float) Math.PI / 180F);
		this.centerHead.xRot = pHeadPitch * ((float) Math.PI / 180F);
	}

	public void prepareMobModel(T pEntity, float pLimbSwing, float pLimbSwingAmount, float pPartialTick)
	{
		setupHeadRotation(pEntity, this.rightHead, 0);
		setupHeadRotation(pEntity, this.leftHead, 1);

		setupBodyRotation(pEntity, this.root);
	}

	// unused on the bwither since there is just one target
	private static <T extends EntityBedrockWither> void setupHeadRotation(T pWither, ModelPart pPart, int headIndex)
	{
		pPart.yRot = (pWither.getHeadYRot(headIndex) - pWither.yBodyRot) * ((float) Math.PI / 180F);
		pPart.xRot = pWither.getHeadXRot(headIndex) * ((float) Math.PI / 180F);
	}

	// rotates the entire body during a dash attack
	// changes made during this function are permanent and have side effects
	private static <T extends EntityBedrockWither> void setupBodyRotation(T pWither, ModelPart pRoot)
	{
		if (pWither.isDashing() && pWither.isPowered())
		{
			pRoot.xRot = 90 * ((float) Math.PI / 180F);
			pRoot.y = 15;
		}
		else
		{
			pRoot.xRot = 0 * ((float) Math.PI / 180F);
			pRoot.y = 0;
		}
	}
}