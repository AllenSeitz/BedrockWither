package com.catastrophe573.bedrockwither;

import com.catastrophe573.bedrockwither.entity.EntityBedrockWither;

import net.minecraft.core.BlockPos;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ForgeEvents
{
	// TODO: on kill event for wither roses, and remove wither roses from EntityBedrockWitherSkull

	// this was somewhat copied from LivingEntity::createWitherRose(), which is surprisingly called for every entity that dies
	@SubscribeEvent
	public void livingDeathEvent(LivingDeathEvent event)
	{
		Entity dead = event.getEntity();
		DamageSource source = event.getSource();
		
		if (dead == null || dead.level.isClientSide())
		{
			return;
		}

		boolean plantedFlower = false;

		if (source.getEntity() instanceof EntityBedrockWither)
		{
			if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(dead.getLevel(), source.getEntity()))
			{
				BlockPos blockpos = dead.blockPosition();
				BlockState newRose = Blocks.WITHER_ROSE.defaultBlockState();
				if (dead.level.isEmptyBlock(blockpos) && newRose.canSurvive(dead.level, blockpos))
				{
					dead.level.setBlock(blockpos, newRose, 3);
					plantedFlower = true;
				}
			}

			// drop it as an item instead
			if (!plantedFlower)
			{
				ItemEntity itementity = new ItemEntity(dead.level, dead.getX(), dead.getY(), dead.getZ(), new ItemStack(Items.WITHER_ROSE));
				dead.level.addFreshEntity(itementity);
			}
		}
	}
}
