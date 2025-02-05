package io.github.kreiseljustus.mixin;

import io.github.kreiseljustus.TestMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.EggItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Random;

@Mixin(ThrownEgg.class)
public class ThrownEggMixin {
    private final static Random random = new Random();

    ResourceKey<Enchantment> RETURN = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(TestMod.MODID, "return"));
    ResourceKey<Enchantment> UNBREGGABLE = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(TestMod.MODID, "unbreggable"));
    ResourceKey<Enchantment> EGGSPLOSIVE = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(TestMod.MODID, "eggsplosive"));
    ResourceKey<Enchantment> LIGHTNING = ResourceKey.create(Registries.ENCHANTMENT, ResourceLocation.fromNamespaceAndPath(TestMod.MODID, "lightning"));

    @Inject(method = "<init>(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void onEggThrown(Level level, LivingEntity owner, ItemStack item, CallbackInfo ci) {
        ThrownEgg egg = (ThrownEgg) (Object) this;

        if(item.has(DataComponents.ENCHANTMENTS)) {
            egg.getItem().set(DataComponents.ENCHANTMENTS, item.get(DataComponents.ENCHANTMENTS));
        }
    }

    @Inject(method = "onHit", at = @At("HEAD"), cancellable = true)
    private void modifyOnHit(HitResult result, CallbackInfo ci) {
        ThrownEgg egg = (ThrownEgg) (Object) this;

        Player player = (Player) egg.getOwner();
        Level level = egg.level();

        int EGGSPLOSIVE_LEVEL = getEnchantmentLevel(egg.getItem(), EGGSPLOSIVE);
        int LIGHTNING_LEVEL = getEnchantmentLevel(egg.getItem(), LIGHTNING);

        if(EGGSPLOSIVE_LEVEL > 0) {
            level.explode(egg, egg.getX(),egg.getY(),egg.getZ(), 2 * EGGSPLOSIVE_LEVEL, Level.ExplosionInteraction.TNT);
            egg.discard();
            ci.cancel();
        }

        if (result.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) result;

            Entity entity = entityHitResult.getEntity();

            if (entity instanceof Player && entity.equals(player)) {
                egg.discard();
                player.addItem(egg.getItem());
            } else {
                entity.hurt(egg.damageSources().thrown(egg, egg.getOwner()), 5.25f);

                if(LIGHTNING_LEVEL > 0) {
                    LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
                    bolt.setPos(egg.getX(),egg.getY(),egg.getZ());
                    level.addFreshEntity(bolt);
                }
            }
        }

        if(getEnchantmentLevel(egg.getItem(), UNBREGGABLE) > 0) {

            if (player == null) {
                egg.discard();
                ci.cancel();
                return;
            }

            int RETURN_LEVEL = getEnchantmentLevel(egg.getItem(), RETURN);
            if (RETURN_LEVEL > 0) {
                Vec3 direction = new Vec3(player.getX() - egg.getX(), player.getY() - egg.getY(), player.getZ() - egg.getZ());

                direction = direction.normalize();

                egg.setDeltaMovement(direction.scale(0.5 * RETURN_LEVEL).x, 0.4, direction.scale(0.5 * RETURN_LEVEL).z);
            } else {
                level.addFreshEntity(new ItemEntity(level, egg.getX(), egg.getY(), egg.getZ(), egg.getItem()));
                egg.discard();
            }

            level.addParticle(ParticleTypes.CLOUD, egg.getX(), egg.getY(), egg.getZ(), 0, 0.1, 0);

            ci.cancel();
        }
    }

    private static int getEnchantmentLevel(ItemStack item, ResourceKey<Enchantment> enchantment) {
        return item
                .getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
                .entrySet()
                .stream()
                .filter(entry -> entry.getKey().getKey().compareTo(enchantment) == 0)
                .findAny()
                .map(entry -> entry.getIntValue())
                .orElse(0);
    }
}
