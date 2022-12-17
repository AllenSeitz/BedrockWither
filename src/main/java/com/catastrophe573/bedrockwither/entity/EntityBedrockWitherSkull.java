package com.catastrophe573.bedrockwither.entity;

import com.catastrophe573.bedrockwither.BedrockWither;

import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.WitherSkull;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

// this class exists to keep the BedrockWither from healing 5 HP per kill
// also to make the blue skulls 'reflectable' like Ghast fireballs, which is exclusive to Bedrock Edition
public class EntityBedrockWitherSkull extends WitherSkull
{
	public EntityBedrockWitherSkull(EntityType<? extends EntityBedrockWitherSkull> type, Level worldIn)
	{
		super((EntityType<? extends EntityBedrockWitherSkull>) type, worldIn);
	}

	public EntityBedrockWitherSkull(Level level, LivingEntity owner, double xvel, double yvel, double zvel)
	{
		// Entity
		this(BedrockWither.BEDROCK_WITHER_SKULL.get(), level);

		// AbstractHurtingProjectile
		this.moveTo(owner.getX(), owner.getY(), owner.getZ(), this.getYRot(), this.getXRot());
		this.reapplyPosition();
		double d0 = Math.sqrt(xvel * xvel + yvel * yvel + zvel * zvel);
		if (d0 != 0.0D)
		{
			this.xPower = xvel / d0 * 0.1D;
			this.yPower = yvel / d0 * 0.1D;
			this.zPower = zvel / d0 * 0.1D;
		}

		this.setOwner(owner);
		this.setRot(owner.getYRot(), owner.getXRot());
	}

	@Override
	protected float getInertia()
	{
		return this.isDangerous() ? 0.73F : super.getInertia();
	}

	// Called when the arrow hits an entity
	// this is copied from the vanilla WitherSkull class, except the +5 HP on kill was intentionally commented out
	protected void onHitEntity(EntityHitResult pResult)
	{
		// super.onHitEntity(pResult); // do NOT do this!

		if (!this.level.isClientSide)
		{
			Entity entity = pResult.getEntity();
			Entity entity1 = this.getOwner();
			boolean flag;
			if (entity1 instanceof LivingEntity)
			{
				LivingEntity livingentity = (LivingEntity) entity1;

				float damage = 12.0f;
				if (this.level.getDifficulty() == Difficulty.EASY)
				{
					damage = 5;
				}
				else if (this.level.getDifficulty() == Difficulty.NORMAL)
				{
					damage = 8;
				}

				flag = entity.hurt(DamageSource.witherSkull(this, livingentity), damage);

				if (flag)
				{
					if (entity.isAlive())
					{
						this.doEnchantDamageEffects(livingentity, entity);
					}
					else
					{
						// this line is intentionally commented out
						// livingentity.heal(5.0F);
					}
				}
			}
			else
			{
				flag = entity.hurt(DamageSource.MAGIC, 5.0F);
			}

			// inflict wither
			if (flag && entity instanceof LivingEntity)
			{
				int i = 0;
				if (this.level.getDifficulty() == Difficulty.NORMAL)
				{
					i = 10;
				}
				else if (this.level.getDifficulty() == Difficulty.HARD)
				{
					i = 40;
				}

				if (i > 0)
				{
					((LivingEntity) entity).addEffect(new MobEffectInstance(MobEffects.WITHER, 20 * i, 1), this.getEffectSource());
				}
			}
		}
	}

	// Returns true if other Entities should be prevented from moving through this Entity.
	// also allows for reflecting projectiles
	public boolean isPickable()
	{
		return true;
	}

	public float getPickRadius()
	{
		return 1.0F;
	}

	@Override
	// this override is how the blue skulls are reflectable while the black ones aren't
	public boolean hurt(DamageSource pSource, float pAmount)
	{
		if (!this.isDangerous())
		{
			return false;
		}
		else
		{
			// implement the reflection
			this.markHurt();
			Entity entity = pSource.getEntity();
			if (entity != null)
			{
				if (!this.level.isClientSide)
				{
					Vec3 reflectAngle = entity.getLookAngle();

					Vec3 currentVel = this.getDeltaMovement();
					double xvel = currentVel.x();
					double yvel = currentVel.y();
					double zvel = currentVel.z();
					double mag = Math.sqrt((xvel * xvel) + (yvel * yvel) + (zvel * zvel)) + 0.1F;

					// reflecting this projectile via conventional means is glitchy, so instead spawn a new one with a new owner
					EntityBedrockWitherSkull witherskull = new EntityBedrockWitherSkull(this.level, (LivingEntity) entity, reflectAngle.x * mag, reflectAngle.y * mag, reflectAngle.z * mag);
					witherskull.setDangerous(true);

					witherskull.setPosRaw(this.getX(), this.getY(), this.getZ());
					this.level.addFreshEntity(witherskull);
					this.discard();
				}
				return true;
			}
		}
		return false;
	}
}
