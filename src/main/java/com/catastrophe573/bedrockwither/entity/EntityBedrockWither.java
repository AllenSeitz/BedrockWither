package com.catastrophe573.bedrockwither.entity;

import com.catastrophe573.bedrockwither.BedrockWither;
import com.google.common.collect.ImmutableList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.BossEvent;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PowerableMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.targeting.TargetingConditions;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.monster.RangedAttackMob;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class EntityBedrockWither extends Monster implements PowerableMob, RangedAttackMob
{
	// the Bedrock Wither only has one target (changing the head array to have just one item was the easiest change)
	private static final EntityDataAccessor<Integer> DATA_TARGET_A = SynchedEntityData.defineId(EntityBedrockWither.class, EntityDataSerializers.INT);
	private static final List<EntityDataAccessor<Integer>> DATA_TARGETS = ImmutableList.of(DATA_TARGET_A);

	private static final EntityDataAccessor<Integer> DATA_ID_INV = SynchedEntityData.defineId(EntityBedrockWither.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ID_PHASECHANGE = SynchedEntityData.defineId(EntityBedrockWither.class, EntityDataSerializers.INT);

	private static final EntityDataAccessor<Integer> DATA_ID_DASH_TARGET_X = SynchedEntityData.defineId(EntityBedrockWither.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ID_DASH_TARGET_Y = SynchedEntityData.defineId(EntityBedrockWither.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ID_DASH_TARGET_Z = SynchedEntityData.defineId(EntityBedrockWither.class, EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_ID_IS_DASHING = SynchedEntityData.defineId(EntityBedrockWither.class, EntityDataSerializers.INT);

	private static final int INVULNERABLE_TICKS = 220;
	private final float[] xRotHeads = new float[2];
	private final float[] yRotHeads = new float[2];
	private final float[] xRotOHeads = new float[2];
	private final float[] yRotOHeads = new float[2];
	private final int[] nextHeadUpdate = new int[2];
	private final int[] idleHeadUpdates = new int[2];
	private int destroyBlocksTick;

	private final ServerBossEvent bossEvent = (ServerBossEvent) (new ServerBossEvent(this.getDisplayName(), BossEvent.BossBarColor.PINK, BossEvent.BossBarOverlay.PROGRESS)).setDarkenScreen(true);

	DamageSource savedDamageSource = null;
	private int[] skullVolleyUpdates = new int[] { 0, 0, 0, 0 };
	int skullVolleyPostDelay = 0;
	int volleysCompleted = 0; // fires one volley before moving in the first phase, two in the second
	int ticksSpentDashing = 0; // for detecting when it is stuck, and when to give up

	private boolean freshlySpawned = true; // set to false on the first update tick

	// this seems to decide who the wither chooses for valid targets
	private static final Predicate<LivingEntity> LIVING_ENTITY_SELECTOR = (entity) ->
	{
		return entity.getMobType() != MobType.UNDEAD && entity.attackable();
	};

	// this also seems important for choosing targets
	private static final TargetingConditions TARGETING_CONDITIONS = TargetingConditions.forCombat().range(40.0D).selector(LIVING_ENTITY_SELECTOR);

	public EntityBedrockWither(EntityType<? extends EntityBedrockWither> type, Level worldIn)
	{
		super(type, worldIn);
		this.moveControl = new FlyingMoveControl(this, 10, false);
		this.setHealth(this.getMaxHealth());
		this.xpReward = 50;

		this.setNoGravity(true); // apparently does nothing in Forge?
		AttributeInstance gravity = this.getAttributes().getInstance(net.minecraftforge.common.ForgeMod.ENTITY_GRAVITY.get());
		gravity.setBaseValue(0);
	}

	protected PathNavigation createNavigation(Level pLevel)
	{
		FlyingPathNavigation flyingpathnavigation = new FlyingPathNavigation(this, pLevel);
		flyingpathnavigation.setCanOpenDoors(false);
		flyingpathnavigation.setCanFloat(true);
		flyingpathnavigation.setCanPassDoors(true);
		return flyingpathnavigation;
	}

	protected void registerGoals()
	{
		this.goalSelector.addGoal(0, new EntityBedrockWither.WitherDoNothingGoal());
		// this.goalSelector.addGoal(2, new RangedAttackGoal(this, 1.0D, 40, 20.0F));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomFlyingGoal(this, 1.0D));
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

		// manual targeting is working better
		//this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
		//this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 0, false, false, LIVING_ENTITY_SELECTOR));
	}

	protected void defineSynchedData()
	{
		super.defineSynchedData();
		this.entityData.define(DATA_TARGET_A, 0);
		this.entityData.define(DATA_ID_INV, 0);
		this.entityData.define(DATA_ID_PHASECHANGE, 0);

		this.entityData.define(DATA_ID_DASH_TARGET_X, 0);
		this.entityData.define(DATA_ID_DASH_TARGET_Y, 0);
		this.entityData.define(DATA_ID_DASH_TARGET_Z, 0);
		this.entityData.define(DATA_ID_IS_DASHING, 0);
	}

	public void addAdditionalSaveData(CompoundTag pCompound)
	{
		super.addAdditionalSaveData(pCompound);
		pCompound.putInt("Invul", this.getInvulnerableTicks());
		pCompound.putInt("PhaseChange", this.getPhaseChange());

		if (getDashTarget() != null)
		{
			pCompound.putInt("DashTargetX", this.getDashTarget().getX());
			pCompound.putInt("DashTargetY", this.getDashTarget().getY());
			pCompound.putInt("DashTargetZ", this.getDashTarget().getZ());
		}
		pCompound.putInt("IsDash", this.isDashing() ? 1 : 0);
	}

	public void readAdditionalSaveData(CompoundTag pCompound)
	{
		super.readAdditionalSaveData(pCompound);
		this.setInvulnerableTicks(pCompound.getInt("Invul"));
		this.setPhaseChange(pCompound.getInt("PhaseChange"));

		BlockPos target = new BlockPos(pCompound.getInt("DashTargetX"), pCompound.getInt("DashTargetY"), pCompound.getInt("DashTargetZ"));
		this.setDashTarget(target);
		this.setIsDashing(pCompound.getInt("IsDash") == 1 ? true : false);

		if (this.hasCustomName())
		{
			this.bossEvent.setName(this.getDisplayName());
		}
	}

	public void setCustomName(@Nullable Component pName)
	{
		super.setCustomName(pName);
		this.bossEvent.setName(this.getDisplayName());
	}

	protected SoundEvent getAmbientSound()
	{
		return SoundEvents.WITHER_AMBIENT;
	}

	protected SoundEvent getHurtSound(DamageSource pDamageSource)
	{
		return SoundEvents.WITHER_HURT;
	}

	protected SoundEvent getDeathSound()
	{
		return SoundEvents.WITHER_DEATH;
	}

	// Called frequently so the entity can update its state every tick as required. For example, zombies and skeletons use this to react to sunlight and start to burn.
	// this is only called on the server?
	public void aiStep()
	{
		// run once, after spawning. For some reason all entities seem to spawn at 0,0 and then move later?
		if (freshlySpawned)
		{
			setDashTarget(this.blockPosition());
			freshlySpawned = false;
		}

		// appears to implement the slow downwards falling? This is unused because I set the Forge gravity to 0
		Vec3 deltaMovement = this.getDeltaMovement().multiply(1.0D, 0.6D, 1.0D);

		if (!this.level.isClientSide && getDashTarget() != null)
		{
			// handle movement on the y-axis
			double deltaY = deltaMovement.y;
			if (this.getY() < getDashTarget().getY())
			{
				deltaY = Math.max(0.0D, deltaY);
				deltaY += 0.3D - deltaY * (double) 0.6F;
			}
			deltaMovement = new Vec3(deltaMovement.x, deltaY, deltaMovement.z);

			// handle movement on the x/z plane
			Vec3 tempDelta = new Vec3(getDashTarget().getX() - this.getX(), getDashTarget().getY() - this.getY(), getDashTarget().getZ() - this.getZ());
			if (tempDelta.horizontalDistanceSqr() > 2.0D && ticksSpentDashing < 160)
			{
				Vec3 vec32 = tempDelta.normalize();
				deltaMovement = deltaMovement.add(vec32.x * 0.3D - deltaMovement.x * 0.6D, vec32.y * 0.3D - deltaMovement.y * 0.6D, vec32.z * 0.3D - deltaMovement.z * 0.6D);
			}
			else
			{
				// arrived at destination, begin volley
				setDashTarget(null);
				setIsDashing(false);
				volleysCompleted = 0;
				deltaMovement = Vec3.ZERO;
				if (ticksSpentDashing >= 160)
				{
					BedrockWither.logInfo("Destination: gave up because we took long. Probably stuck on bedrock or something.");
				}
				else
				{
					BedrockWither.logInfo("Destination: arrived!");
				}
				ticksSpentDashing = 0;
			}
		}

		// deltaMovement was recalculated but does not necessarily change each update
		this.setDeltaMovement(deltaMovement);
		if (deltaMovement.horizontalDistanceSqr() > 0.05D)
		{
			this.setYRot((float) Mth.atan2(deltaMovement.z, deltaMovement.x) * (180F / (float) Math.PI) - 90.0F);
		}

		// this implements the dash attack, which I partially copied from the EnderDragon
		if (!this.level.isClientSide && this.hurtTime == 0 && getPhaseChange() == 2 && isDashing())
		{
			this.dashKnockback(this.level.getEntities(this, this.getBoundingBox(), EntitySelector.NO_CREATIVE_OR_SPECTATOR));
		}

		super.aiStep();

		for (int i = 0; i < 2; ++i)
		{
			this.yRotOHeads[i] = this.yRotHeads[i];
			this.xRotOHeads[i] = this.xRotHeads[i];
		}

		// implement head turning (all 3 heads face the same target) - this was copied from WitherBoss
		for (int j = 0; j < 2; ++j)
		{
			int k = this.getWitherTarget();
			Entity entity1 = null;
			if (k > 0)
			{
				entity1 = this.level.getEntity(k);
			}

			if (entity1 != null)
			{
				double d9 = this.getHeadX(j + 1);
				double d1 = this.getHeadY(j + 1);
				double d3 = this.getHeadZ(j + 1);
				double d4 = entity1.getX() - d9;
				double d5 = entity1.getEyeY() - d1;
				double d6 = entity1.getZ() - d3;
				double d7 = Math.sqrt(d4 * d4 + d6 * d6);
				float f = (float) (Mth.atan2(d6, d4) * (double) (180F / (float) Math.PI)) - 90.0F;
				float f1 = (float) (-(Mth.atan2(d5, d7) * (double) (180F / (float) Math.PI)));
				this.xRotHeads[j] = this.rotlerp(this.xRotHeads[j], f1, 40.0F);
				this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], f, 10.0F);
			}
			else
			{
				this.yRotHeads[j] = this.rotlerp(this.yRotHeads[j], this.yBodyRot, 10.0F);
			}
		}

		// particles for phase 2 when it has wither armor
		for (int l = 0; l < 3; ++l)
		{
			double d8 = this.getHeadX(l);
			double d10 = this.getHeadY(l);
			double d2 = this.getHeadZ(l);
			this.level.addParticle(ParticleTypes.SMOKE, d8 + this.random.nextGaussian() * (double) 0.3F, d10 + this.random.nextGaussian() * (double) 0.3F, d2 + this.random.nextGaussian() * (double) 0.3F, 0.0D, 0.0D, 0.0D);
			if (this.isPowered() && this.level.random.nextInt(4) == 0)
			{
				this.level.addParticle(ParticleTypes.ENTITY_EFFECT, d8 + this.random.nextGaussian() * (double) 0.3F, d10 + this.random.nextGaussian() * (double) 0.3F, d2 + this.random.nextGaussian() * (double) 0.3F, (double) 0.7F, (double) 0.7F, 0.5D);
			}
		}

		// particle effects to demonstrate the the wither is invulnerable during phase changes
		if (this.getInvulnerableTicks() > 0)
		{
			for (int i1 = 0; i1 < 3; ++i1)
			{
				this.level.addParticle(ParticleTypes.ENTITY_EFFECT, this.getX() + this.random.nextGaussian(), this.getY() + (double) (this.random.nextFloat() * 3.3F), this.getZ() + this.random.nextGaussian(), (double) 0.7F, (double) 0.7F, (double) 0.9F);
			}
		}
	}

	protected void customServerAiStep()
	{
		// phase change logic
		if (this.getInvulnerableTicks() > 0)
		{
			int invulnTicksRemaining = this.getInvulnerableTicks() - 1;
			this.setInvulnerableTicks(invulnTicksRemaining);

			// phase 0 - heal / fill HP bar, spawn in
			if (this.getPhaseChange() == 0)
			{
				this.bossEvent.setProgress(1.0F - (float) invulnTicksRemaining / INVULNERABLE_TICKS);

				// explode with power 7 when the invulnerable ticks reach 0
				if (invulnTicksRemaining <= 0)
				{
					// this is the birth explosion that starts phase 1
					Explosion.BlockInteraction explosion$blockinteraction = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this) ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE;
					this.level.explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, explosion$blockinteraction);
					this.setPhaseChange(1);
					if (!this.isSilent())
					{
						this.level.globalLevelEvent(1023, this.blockPosition(), 0);
					}
					BedrockWither.logInfo("FINISHED INITIAL SPAWN IN");
				}

				// fill the HP bar during the spawn animation
				if (this.tickCount % 10 == 0)
				{
					this.heal(10.0F);
				}
			}
			// phase 1 - short delay, then explode and summon 4 wither skeletons
			else if (this.getPhaseChange() == 1)
			{
				// explode with power 8 when the invulnerable ticks reach 0
				if (invulnTicksRemaining <= 0)
				{
					// this is explosion that starts phase 2
					Explosion.BlockInteraction explosion$blockinteraction = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this) ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE;
					this.level.explode(this, this.getX(), this.getEyeY(), this.getZ(), 8.0F, false, explosion$blockinteraction);
					this.setPhaseChange(2);

					// spawn 4 wither skeletons in a predictable, tight formation (which usually fall into the crater but sure whatever)
					WitherSkeleton nw_add = EntityType.WITHER_SKELETON.create(this.level);
					WitherSkeleton sw_add = EntityType.WITHER_SKELETON.create(this.level);
					WitherSkeleton ne_add = EntityType.WITHER_SKELETON.create(this.level);
					WitherSkeleton se_add = EntityType.WITHER_SKELETON.create(this.level);
					nw_add.finalizeSpawn((ServerLevel) this.level, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null, (CompoundTag) null);
					sw_add.finalizeSpawn((ServerLevel) this.level, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null, (CompoundTag) null);
					ne_add.finalizeSpawn((ServerLevel) this.level, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null, (CompoundTag) null);
					se_add.finalizeSpawn((ServerLevel) this.level, level.getCurrentDifficultyAt(this.blockPosition()), MobSpawnType.MOB_SUMMONED, (SpawnGroupData) null, (CompoundTag) null);
					LivingEntity target = (LivingEntity) this.level.getEntity(this.getWitherTarget()); // can be null, no big deal
					nw_add.setTarget(target);
					sw_add.setTarget(target);
					ne_add.setTarget(target);
					se_add.setTarget(target);
					nw_add.invulnerableTime = 60;
					sw_add.invulnerableTime = 60;
					ne_add.invulnerableTime = 60;
					se_add.invulnerableTime = 60;
					nw_add.setPos(this.getX() - 2.0D, this.getY() + 2.0D, this.getZ() - 2.0D);
					sw_add.setPos(this.getX() + 2.0D, this.getY() + 2.0D, this.getZ() - 2.0D);
					ne_add.setPos(this.getX() - 2.0D, this.getY() + 2.0D, this.getZ() + 2.0D);
					se_add.setPos(this.getX() + 2.0D, this.getY() + 2.0D, this.getZ() + 2.0D);
					level.addFreshEntity(nw_add);
					level.addFreshEntity(sw_add);
					level.addFreshEntity(ne_add);
					level.addFreshEntity(se_add);
					level.broadcastEntityEvent(this, (byte) 18); // I don't know, I copied this from vanilla
					BedrockWither.logInfo("CREATED WITHER SKELETONS");
				}
			}
		}
		else
		{
			super.customServerAiStep();

			// implement the delayed shots 2-4 of the volley that is started inside the next code block
			int targetID = this.getWitherTarget();
			for (int i = 0; i < 4; i++)
			{
				if (skullVolleyUpdates[i] > 0 && this.tickCount >= skullVolleyUpdates[i])
				{
					skullVolleyUpdates[i] = 0;

					BedrockWither.logInfo("Volley: " + i);
					LivingEntity targetEntity = (LivingEntity) this.level.getEntity(targetID);
					if (targetEntity != null && this.canAttack(targetEntity) && !(this.distanceToSqr(targetEntity) > 900.0D))
					{
						this.performRangedAttack(0, targetEntity, i == 3); // the final shot is blue
					}
				}
			}
			if (skullVolleyPostDelay > 0 && this.tickCount >= skullVolleyPostDelay)
			{
				skullVolleyPostDelay = 0;
				volleysCompleted++;
				BedrockWither.logInfo("Post-delay complete.");
			}

			// head targeting logic, mostly unchanged from vanilla, except now for only a single head instead of 3
			if (this.tickCount >= this.nextHeadUpdate[0])
			{
				// this.nextHeadUpdate[i - 1] = this.tickCount + 10 + this.random.nextInt(10); // original logic
				this.nextHeadUpdate[0] = this.tickCount + 5 + this.random.nextInt(3);

				// fire random blue skulls on normal and hard difficulty when enough time has passed with no target
				if (this.level.getDifficulty() == Difficulty.NORMAL || this.level.getDifficulty() == Difficulty.HARD)
				{
					if (!isDashing() && getWitherTarget() == 0)
					{
						this.idleHeadUpdates[0] = this.idleHeadUpdates[0] + 1;
						if (idleHeadUpdates[0] > 15)
						{
							// float f = 10.0F;
							// float f1 = 5.0F;
							double d0 = Mth.nextDouble(this.random, this.getX() - 10.0D, this.getX() + 10.0D);
							double d1 = Mth.nextDouble(this.random, this.getY() - 5.0D, this.getY() + 5.0D);
							double d2 = Mth.nextDouble(this.random, this.getZ() - 10.0D, this.getZ() + 10.0D);
							this.performRangedAttack(0, d0, d1, d2, true);
							this.idleHeadUpdates[0] = 0;
						}
					}
				}

				// if a target exists then begin a volley of 4 shots, else pick a target
				if (targetID > 0 && isOkayToStartVolley())
				{
					LivingEntity targetEntity = (LivingEntity) this.level.getEntity(targetID);
					if (targetEntity != null && this.canAttack(targetEntity) && this.distanceToSqr(targetEntity) < 900.0D)
					{
						// this.nextHeadUpdate[0] = this.tickCount + 40 + this.random.nextInt(20); // original code, needs to decrease with lost HP
						this.nextHeadUpdate[0] = this.tickCount + getSkullVolleyDelay() * 12; // returns a value depending on health remaining in each phase
						this.idleHeadUpdates[0] = 0;
						int baseDelay = getSkullVolleyDelay() * 2;
						skullVolleyUpdates[0] = this.tickCount + baseDelay + (getSkullVolleyDelay());
						skullVolleyUpdates[1] = this.tickCount + baseDelay + (getSkullVolleyDelay() * 2);
						skullVolleyUpdates[2] = this.tickCount + baseDelay + (getSkullVolleyDelay() * 3);
						skullVolleyUpdates[3] = this.tickCount + baseDelay + (getSkullVolleyDelay() * 4);
						skullVolleyPostDelay = this.tickCount + baseDelay + (getSkullVolleyDelay() * 8);

						// shoot the random blue skulls since the previously fired skulls hit a wall, apparently
						if (!this.hasLineOfSight(targetEntity))
						{
							this.idleHeadUpdates[0] = 15;
						}
					}
					else
					{
						this.setWitherTarget(0);
						volleysCompleted = 2; // to ensure the first thing it does with regards to the next target, is to move
					}
				}
				else
				{
					// made the area to search for targets a little larger
					List<LivingEntity> list = this.level.getNearbyEntities(LivingEntity.class, TARGETING_CONDITIONS, this, this.getBoundingBox().inflate(30.0D, 20.0D, 30.0D));
					if (!list.isEmpty())
					{
						LivingEntity livingentity1 = list.get(this.random.nextInt(list.size()));
						this.setWitherTarget(livingentity1.getId());
						volleysCompleted = 2; // to ensure the first thing it does with regards to the next target, is to move
					}
					else
					{
						this.setWitherTarget(0); // no target
						volleysCompleted = 2; // to ensure the first thing it does with regards to the next target, is to move
					}
				}
			}

			if (isDashing())
			{
				ticksSpentDashing++;
			}

			// only update movement positions if there is a target
			if (getWitherTarget() > 0)
			{
				// The Wither is supposed to dash to a certain point, then stop and fire shots, then dash again. Standing still for a few seconds each time. (Both phases, except two volleys per dash in phase 2.)
				if (isOkayToStartDash())
				{
					Entity target = this.level.getEntity(this.getWitherTarget());
					if (target != null)
					{
						if (!isPowered())
						{
							// pick a random position about 10-15 blocks away from the entity, and start the volley
							double angle = this.random.nextDouble(0, 360.D);
							double distance = this.random.nextDouble(10.0D, 15.0D);
							double xoff = Math.cos(angle) * distance;
							double zoff = Math.sin(angle) * distance;

							BlockPos orbitPoint = new BlockPos(target.getX() + xoff, target.getY() + 5, target.getZ() + zoff);
							setDashTarget(orbitPoint);
							setIsDashing(true);
							BedrockWither.logInfo("Destination Phase 1: set - " + orbitPoint.getX() + ", " + orbitPoint.getY() + ", " + orbitPoint.getZ());
						}
						else
						{
							// during phase 2 dash directly towards the target, about 12 blocks each time
							dashAtTarget(target.getX(), target.getY(), target.getZ());
							setIsDashing(true);
							BedrockWither.logInfo("Destination Phase 2: start - " + Math.floor(this.getX()) + ", " + Math.floor(this.getY()) + ", " + Math.floor(this.getZ()));
							BedrockWither.logInfo("Destination Phase 2: set   - " + Math.floor(target.getX()) + ", " + Math.floor(target.getY()) + ", " + Math.floor(target.getZ()));
						}
					}
				}
			}

			// on some ticks (after taking damage OR MOVING) break any blocks inside this wither's hitbox
			// in phase 2, always break blocks
			if (this.destroyBlocksTick > 0 || isDashing())
			{
				if (destroyBlocksTick > 0)
				{
					--this.destroyBlocksTick;
				}

				if ((this.destroyBlocksTick == 0 || isDashing()) && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this))
				{
					int j1 = Mth.floor(this.getY());
					int i2 = Mth.floor(this.getX());
					int j2 = Mth.floor(this.getZ());
					boolean brokeAnyBlocks = false;

					for (int j = -1; j <= 1; ++j)
					{
						for (int k2 = -1; k2 <= 1; ++k2)
						{
							for (int k = 0; k <= 3; ++k)
							{
								int l2 = i2 + j;
								int l = j1 + k;
								int i1 = j2 + k2;
								BlockPos blockpos = new BlockPos(l2, l, i1);
								BlockState blockstate = this.level.getBlockState(blockpos);
								if (canDestroy(blockstate) && net.minecraftforge.event.ForgeEventFactory.onEntityDestroyBlock(this, blockpos, blockstate))
								{
									brokeAnyBlocks = this.level.destroyBlock(blockpos, true, this) || brokeAnyBlocks;
								}
							}
						}
					}

					// play the block breaking sound effect
					if (brokeAnyBlocks)
					{
						this.level.levelEvent((Player) null, 1022, this.blockPosition(), 0);
					}
				}
			}

			// heal 1 HP/sec, implement HP regeneration
			// if (this.tickCount % 20 == 0)
			// {
			// this.heal(1.0F);
			// }

			// check for phase changes
			if (this.isPowered() && this.getPhaseChange() == 0 || this.getPhaseChange() == 1) // phase 0 can be skipped with certain summoning methods
			{
				this.setPhaseChange(1);
				this.setInvulnerableTicks(INVULNERABLE_TICKS / 2); // get ready for the wither skeletons!
			}

			this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
		}
	}

	// called from inside the dash attack to implement the effects
	private void dashKnockback(List<Entity> pEntities)
	{
		double hitCenterX = (this.getBoundingBox().minX + this.getBoundingBox().maxX) / 2.0D;
		double hitCenterZ = (this.getBoundingBox().minZ + this.getBoundingBox().maxZ) / 2.0D;

		for (Entity entity : pEntities)
		{
			if (entity instanceof LivingEntity)
			{
				double hitResultX = entity.getX() - hitCenterX;
				double hitResultZ = entity.getZ() - hitCenterZ;
				double exitVector = Math.max(hitResultX * hitResultX + hitResultZ * hitResultZ, 0.1D);
				double knockbackAmount = 4.0D;
				entity.push(hitResultX / exitVector * knockbackAmount, (double) 0.2F, hitResultZ / exitVector * knockbackAmount);

				entity.hurt(DamageSource.mobAttack(this), 15.0F); // the dash attack does 15 (7.5 hearts) of damage!
			}
		}
	}

	protected boolean isOkayToStartVolley()
	{
		int phase = getPhaseChange();
		if (phase < 2)
		{
			return getDashTarget() == null && volleysCompleted < 1;
		}
		else if (phase == 2)
		{
			return getDashTarget() == null && volleysCompleted < 2;
		}

		return false; // phase 0 is spawning in and phase 3 is dying
	}

	protected boolean isOkayToStartDash()
	{
		int phase = getPhaseChange();
		if (phase < 2)
		{
			return getDashTarget() == null && volleysCompleted >= 1;
		}
		else if (phase == 2)
		{
			return getDashTarget() == null && volleysCompleted >= 2;
		}

		return false; // phase 0 is spawning in and phase 3 is dying
	}

	protected int getNextActionDelay()
	{
		float percent = this.getHealth() / this.getMaxHealth();

		if (percent > 0.8f)
		{
			return 40;
		}
		else if (percent > 0.7f)
		{
			return 30;
		}
		else if (percent > 0.6f)
		{
			return 25;
		}
		else if (percent > 0.5f)
		{
			return 20;
		}
		else if (percent > 0.3f)
		{
			return 40;
		}
		else if (percent > 0.2f)
		{
			return 30;
		}
		else if (percent > 0.1f)
		{
			return 25;
		}
		return 20;
	}

	protected int getSkullVolleyDelay()
	{
		float percent = this.getHealth() / this.getMaxHealth();

		if (percent > 0.8f)
		{
			return 11;
		}
		else if (percent > 0.7f)
		{
			return 9;
		}
		else if (percent > 0.6f)
		{
			return 7;
		}
		else if (percent > 0.5f)
		{
			return 5;
		}
		else if (percent > 0.3f)
		{
			return 11;
		}
		else if (percent > 0.2f)
		{
			return 9;
		}
		else if (percent > 0.1f)
		{
			return 7;
		}
		return 5;
	}

	// BlockState.canEntityDestroy() is useless because it has hardcoded checks for WitherBoss
	// so I have to check for this tag myself
	public static boolean canDestroy(BlockState pBlock)
	{
		return !pBlock.isAir() && !pBlock.is(BlockTags.WITHER_IMMUNE);
	}

	// Initializes this Wither's explosion sequence and makes it invulnerable. Called immediately after spawning.
	public void startPhaseZero()
	{
		this.setInvulnerableTicks(INVULNERABLE_TICKS);
		this.bossEvent.setProgress(0.0F);
		this.setHealth(this.getMaxHealth() / 3.0F);
	}

	@Override
	// implement the death animation (and explosion)
	protected void tickDeath()
	{
		this.bossEvent.setProgress(0);

		if (this.level.isClientSide())
		{
			return; // don't run this on the client, no need
		}

		if (this.getPhaseChange() < 3)
		{
			// is this the first tick since the Wither's HP dropped to 0?
			if (this.getInvulnerableTicks() == 0)
			{
				this.setInvulnerableTicks(INVULNERABLE_TICKS / 2); // get ready for the final explosion
			}
			else
			{
				int invulnTicksRemaining = this.getInvulnerableTicks() - 1;
				this.setInvulnerableTicks(invulnTicksRemaining);
				if (invulnTicksRemaining <= 0)
				{
					Explosion.BlockInteraction explosion$blockinteraction = net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.level, this) ? Explosion.BlockInteraction.DESTROY : Explosion.BlockInteraction.NONE;
					this.level.explode(this, this.getX(), this.getEyeY(), this.getZ(), 7.0F, false, explosion$blockinteraction);
					this.setPhaseChange(3);
					BedrockWither.logInfo("FINAL BWITHER EXPLOSION");

					// this causes the item drop, statistics, etc
					if (savedDamageSource != null)
					{
						super.die(savedDamageSource);
					}
					else
					{
						// this could happen if you shut down the server during the death animation or something
						// but it would only matter if you added conditions to the loot table anyway
						super.die(DamageSource.GENERIC);
					}
				}
			}
		}
		else
		{
			super.tickDeath(); // allow a natural death one second after the final explosion
		}
	}

	@Override
	public void makeStuckInBlock(BlockState pState, Vec3 pMotionMultiplier)
	{
	}

	// Add the given player to the list of players tracking this entity. For instance, a player may track a boss in order to view its associated boss bar.
	@Override
	public void startSeenByPlayer(ServerPlayer pPlayer)
	{
		super.startSeenByPlayer(pPlayer);
		this.bossEvent.addPlayer(pPlayer);
	}

	// Removes the given player from the list of players tracking this entity. See {@link Entity#addTrackingPlayer} for more information on tracking.
	@Override
	public void stopSeenByPlayer(ServerPlayer pPlayer)
	{
		super.stopSeenByPlayer(pPlayer);
		this.bossEvent.removePlayer(pPlayer);
	}

	private double getHeadX(int pHead)
	{
		if (pHead <= 0)
		{
			return this.getX();
		}
		else
		{
			float f = (this.yBodyRot + (float) (180 * (pHead - 1))) * ((float) Math.PI / 180F);
			float f1 = Mth.cos(f);
			return this.getX() + (double) f1 * 1.3D;
		}
	}

	private double getHeadY(int pHead)
	{
		return pHead <= 0 ? this.getY() + 3.0D : this.getY() + 2.2D;
	}

	private double getHeadZ(int pHead)
	{
		if (pHead <= 0)
		{
			return this.getZ();
		}
		else
		{
			float f = (this.yBodyRot + (float) (180 * (pHead - 1))) * ((float) Math.PI / 180F);
			float f1 = Mth.sin(f);
			return this.getZ() + (double) f1 * 1.3D;
		}
	}

	private float rotlerp(float p_31443_, float p_31444_, float p_31445_)
	{
		float f = Mth.wrapDegrees(p_31444_ - p_31443_);
		if (f > p_31445_)
		{
			f = p_31445_;
		}

		if (f < -p_31445_)
		{
			f = -p_31445_;
		}

		return p_31443_ + f;
	}

	private void performRangedAttack(int pHead, LivingEntity pTarget, boolean makeItBlue)
	{
		this.performRangedAttack(pHead, pTarget.getX(), pTarget.getY() + (double) pTarget.getEyeHeight() * 0.5D, pTarget.getZ(), makeItBlue);
	}

	// Launches a Wither skull toward (pX, pY, pZ)
	private void performRangedAttack(int pHead, double pX, double pY, double pZ, boolean makeItBlue)
	{
		if (!this.isSilent())
		{
			this.level.levelEvent((Player) null, 1024, this.blockPosition(), 0);
		}

		double d0 = this.getHeadX(pHead);
		double d1 = this.getHeadY(pHead);
		double d2 = this.getHeadZ(pHead);
		double d3 = pX - d0;
		double d4 = pY - d1;
		double d5 = pZ - d2;
		EntityBedrockWitherSkull witherskull = new EntityBedrockWitherSkull(this.level, this, d3, d4, d5);
		witherskull.setOwner(this);
		if (makeItBlue)
		{
			witherskull.setDangerous(true);
		}

		witherskull.setPosRaw(d0, d1, d2);
		this.level.addFreshEntity(witherskull);
	}

	// this is required by the parent class, but should never be used
	@Override
	public void performRangedAttack(LivingEntity pTarget, float pDistanceFactor)
	{
		BedrockWither.logInfo("BWITHER: parent version of PerformRangedAttack() called unexpectedly?");
		this.performRangedAttack(0, pTarget, false);
	}

	// does the second phase dash attack
	protected void dashAtTarget(double pX, double pY, double pZ)
	{
		double headX = this.getX();
		double headY = this.getY();
		double headZ = this.getZ();

		// get the vector from the center head to the target
		Vec3 targetVector = new Vec3(pX - headX, pY - headY, pZ - headZ);

		// the dash attack always goes exactly 12 blocks, even if it would overshoot or not go far enough
		targetVector = targetVector.normalize();
		targetVector = targetVector.scale(12.0D);

		// save start and end points of dash
		BlockPos newTarget = this.blockPosition();
		this.setDashTarget(newTarget.offset(targetVector.x, targetVector.y, targetVector.z));
	}

	// Called when the entity is attacked.
	public boolean hurt(DamageSource pSource, float pAmount)
	{
		if (this.isInvulnerableTo(pSource))
		{
			return false;
		}
		else if (pSource != DamageSource.DROWN && !(pSource.getEntity() instanceof EntityBedrockWither))
		{
			if (this.getInvulnerableTicks() > 0 && pSource != DamageSource.OUT_OF_WORLD)
			{
				return false;
			}
			else
			{
				// after the phase change, become immune to arrows
				if (this.isPowered())
				{
					Entity entity = pSource.getDirectEntity();
					if (entity instanceof AbstractArrow)
					{
						return false;
					}
				}

				Entity entity1 = pSource.getEntity();
				if (entity1 != null && !(entity1 instanceof Player) && entity1 instanceof LivingEntity && ((LivingEntity) entity1).getMobType() == this.getMobType())
				{
					return false;
				}
				else
				{
					// set a timer to destroy blocks after taking damage, but only one second later
					if (this.destroyBlocksTick <= 0)
					{
						this.destroyBlocksTick = 20;
					}

					for (int i = 0; i < this.idleHeadUpdates.length; ++i)
					{
						this.idleHeadUpdates[i] += 3;
					}

					// this entity is now the target!
					if ( pSource != null && pSource.getEntity() != null )
					{
						setWitherTarget(pSource.getEntity().getId());
					}
					
					return super.hurt(pSource, pAmount);
				}
			}
		}
		else
		{
			return false;
		}
	}

	protected void dropCustomDeathLoot(DamageSource pSource, int pLooting, boolean pRecentlyHit)
	{
		// use a regular loot table
	}

	@Override
	public void die(DamageSource pCause)
	{
		// intentionally do nothing now, but then promise to kill super.die() later when the dramatic, delayed death animation is done
		savedDamageSource = pCause;
	}

	// Makes the entity despawn if requirements are reached
	public void checkDespawn()
	{
		if (this.level.getDifficulty() == Difficulty.PEACEFUL && this.shouldDespawnInPeaceful())
		{
			this.discard();
		}
		else
		{
			this.noActionTime = 0;
		}
	}

	public boolean causeFallDamage(float pFallDistance, float pMultiplier, DamageSource pSource)
	{
		return false;
	}

	// immune to all potion effects apparently
	public boolean addEffect(MobEffectInstance pEffectInstance, @Nullable Entity p_182398_)
	{
		return false;
	}

	public static AttributeSupplier.Builder createAttributes()
	{
		return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, 600.0D).add(Attributes.MOVEMENT_SPEED, (double) 0.6F).add(Attributes.FLYING_SPEED, (double) 0.6F).add(Attributes.FOLLOW_RANGE, 40.0D).add(Attributes.ARMOR, 4.0D);
	}

	public float getHeadYRot(int pHead)
	{
		return this.yRotHeads[pHead];
	}

	public float getHeadXRot(int pHead)
	{
		return this.xRotHeads[pHead];
	}

	public int getInvulnerableTicks()
	{
		return this.entityData.get(DATA_ID_INV);
	}

	public void setInvulnerableTicks(int pTime)
	{
		this.entityData.set(DATA_ID_INV, pTime);
	}

	// the Bedrock Wither only has one target
	public int getWitherTarget()
	{
		return this.entityData.get(DATA_TARGETS.get(0));
	}

	// the Bedrock Wither only has one target
	public void setWitherTarget(int pNewId)
	{
		this.entityData.set(DATA_TARGETS.get(0), pNewId);
	}

	public int getPhaseChange()
	{
		return this.entityData.get(DATA_ID_PHASECHANGE);
	}

	public void setPhaseChange(int newPhase)
	{
		this.entityData.set(DATA_ID_PHASECHANGE, newPhase);
	}

	public BlockPos getDashTarget()
	{
		int x = this.entityData.get(DATA_ID_DASH_TARGET_X);
		int y = this.entityData.get(DATA_ID_DASH_TARGET_Y);
		int z = this.entityData.get(DATA_ID_DASH_TARGET_Z);

		if (y == -9999)
		{
			return null; // intentional and safe
		}

		return new BlockPos(x, y, z);
	}

	public void setDashTarget(BlockPos target)
	{
		if (target == null)
		{
			this.entityData.set(DATA_ID_DASH_TARGET_X, 0);
			this.entityData.set(DATA_ID_DASH_TARGET_Y, -9999); // means the getter should return null
			this.entityData.set(DATA_ID_DASH_TARGET_Z, 0);
		}
		else
		{
			this.entityData.set(DATA_ID_DASH_TARGET_X, target.getX());
			this.entityData.set(DATA_ID_DASH_TARGET_Y, target.getY());
			this.entityData.set(DATA_ID_DASH_TARGET_Z, target.getZ());
		}
	}

	public boolean isDashing()
	{
		return this.entityData.get(DATA_ID_IS_DASHING) == 1;
	}

	// parameter is ticks, same as invuln timer
	public void setIsDashing(boolean state)
	{
		this.entityData.set(DATA_ID_IS_DASHING, state ? 1 : 0);
	}

	public boolean isPowered()
	{
		return this.getHealth() <= this.getMaxHealth() / 2.0F;
	}

	public MobType getMobType()
	{
		return MobType.UNDEAD;
	}

	protected boolean canRide(Entity pEntity)
	{
		return false;
	}

	// Returns false if this Entity can't move between dimensions. True if it can.
	public boolean canChangeDimensions()
	{
		return false;
	}

	public boolean canBeAffected(MobEffectInstance pPotioneffect)
	{
		return pPotioneffect.getEffect() == MobEffects.WITHER ? false : super.canBeAffected(pPotioneffect);
	}

	class WitherDoNothingGoal extends Goal
	{
		public WitherDoNothingGoal()
		{
			this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.JUMP, Goal.Flag.LOOK));
		}

		// Returns whether execution should begin. You can also read and cache any state necessary for execution in this method as well.
		public boolean canUse()
		{
			return EntityBedrockWither.this.getInvulnerableTicks() > 0;
		}
	}
}
