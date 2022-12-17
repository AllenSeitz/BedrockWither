# BedrockWither
A Minecraft mod which adds (my approximation of) the Bedrock Edition Wither to Java Edition.

The official website for this mod is located on CurseForge at [TODO].

Check out another one of my mods, https://www.curseforge.com/minecraft/mc-mods/dimensional-dungeons.

## What's the difference?

Refer to this chart for a simple list of differences:

![Comparison Table](/wiki/comparison.png)

In other words the Bedrock Wither has twice as much HP but no way to heal itself. Its hitbox is slightly different, although despite being tweaked it is still too small (in my opinion) just like Java Edition. And finally the Bedrock Wither is only capable of firing skulls at a single target where as the Java Edition Wither can target 3 entities independently with its three heads.

## Is the Bedrock Wither harder?

Only because of Bedrock Edition differences and bugs. If anything the Bedrock Edition Wither should be a bit easier and more importantly, more fun to fight and more fair. It's not just a simple gear check like it is in Java Edition.

Here's a list of reasons why players find the Bedrock Wither to be harder than Java Edition:
- Bedrock Edition bows work differently.
- Bedrock Edition does not have rapid saturation healing! You can't tank the Wither with a bunch of food.
- Prior to April 2022, the Protection enchantment did not reduce the damage over time dealt by the wither effect! In Java Edition the protection enchantment always reduced the damage dealt by the wither effect. With Protection 4 on multiple pieces this damage can be trivialized.
- The Bedrock Wither breaks a lot more blocks and drops them as items, which can cause lag.
- The Bedrock Wither gains a dash attack in phase 2 as well as some Wither Skeleton adds.

## How does the Bedrock Wither fight work exactly?
Upon being summoned the Bedrock Wither will explode just like the Java Wither. Unlike the Java Wither however the Bedrock Wither will begin to rapidly fly around. It will pick a random spot about 5 blocks in the air and 12 blocks away from its target, hover, and fire a volley of skulls. This volley will always consist of 3 black wither skulls and 1 blue wither skull. These wither skulls have the same properties as Java Edition except that they can't heal the wither on a successful kill. Additionally the blue skulls can be reflected like ghast fireballs! Phase 1 of the fight repeats these two steps with the Bedrock Wither switching targets as necessary. However as the wither loses health the delay between shots and the "time that it sits still and lets you shoot it" becomes shorter.

Phase 2 of the fight begins at 50% health, just like Java Edition. Also just like Java Edition the Bedrock Wither gains "wither armor" and becomes immune to projectiles in this phase. But that's where the similarities end. First and most importantly this phase change is marked by a **power 8** explosion! A power 8 explosion is extremely powerful. For reference, both withers explode with power 7 when summoned normally, and an End Crystal has explosion power 6. This explosion also summons 4 wither skeletons because why not.

During phase 2 the Bedrock Wither has the following priorities:
- Pick a target.
- Dash towards it.
- Fire a volley of 3 black skulls and a blue skull, similar to phase 1.
- Pause for a short amount of time.
- Fire another volley of 4 skulls like before.
- Repeat!

The dash attack that the Bedrock Wither gains during this phase is particularly dangerous. It does high contact damage (7.5 hearts!) and knockback similar to the Ender Dragon. It can be difficult to dodge this attack since you're forced to be in melee range to deal damage and the Bedrock Wither is probably tearing up the terrain.

Finally, unlike the Java Wither which dies instantly and uncerimoniously, the Bedrock Wither has a fancy delayed death animation and yet another power 7 explosion before finally dropping the Nether Star. (Which in this mod is implemented with a loot table! Not hardcoded!)

## Technical Things
This mod implements a new entity whose id is bwither:bedrock_wither. It does not make any changes to the Java Wither. This mod also makes no core mods and uses no mixins. Being able to reflect the blue skulls is implemented by also implenting a new projectile entity: bwither:bwither_skull.

My version of the Bedrock Wither has the exact same appearance as the Java Wither. It even loads the same texture. If you use a texture pack to reskin the Wither then my Bedrock Wither will use the same texture.

This mod was created based on information from [the unofficial Minecraft wiki](https://minecraft.fandom.com/wiki/Wither), my own playthroughs of Bedrock Edition, and public videos of other players fighting the Bedrock Wither. No information about the Bedrock Wither was reverse engineered or datamined. My implementation of the Bedrock Wither is not 'official'. All of the numerical parameters with regards to how long the AI should pause, the gaps between skull volleys, the distance and speed that it flies, the hover altitude, the dash attack distance, and more are all my own work.
