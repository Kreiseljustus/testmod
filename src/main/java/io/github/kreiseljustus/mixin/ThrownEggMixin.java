package io.github.kreiseljustus.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ThrownEgg;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
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

        if(result.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHitResult = (EntityHitResult) result;

            Entity entity = entityHitResult.getEntity();

            if(entity instanceof Player && entity.equals(player)) {
                egg.discard();
                player.addItem(egg.getItem());
            } else {
                entity.hurt(egg.damageSources().thrown(egg, egg.getOwner()), 5.25f);
            }
        }

        if(player == null) {egg.discard();ci.cancel();return;}

        if(egg.getItem().has(DataComponents.ENCHANTMENTS)) {

        }

        Vec3 direction = new Vec3(player.getX() - egg.getX(), player.getY() - egg.getY(), player.getZ() - egg.getZ());

        direction = direction.normalize();

        egg.setDeltaMovement(direction.scale(0.5).x, 0.4, direction.scale(0.5).z);



        for(int i = 0; i < 5; i++) {
            for(int k = 0; k < 5; k++) {
                FallingBlockEntity block = FallingBlockEntity.fall(level, new BlockPos((int) egg.getX(), (int) egg.getY(), (int) egg.getZ()), Blocks.STONE.defaultBlockState());
                block.setNoGravity(false);
                block.setDeltaMovement(random.nextDouble() * 0.6, random.nextDouble() * 5.5, random.nextDouble() * 0.6);
                level.addFreshEntity(block);
            }
        }

        
        //egg.discard();

        level.addParticle(ParticleTypes.CLOUD, egg.getX(), egg.getY(), egg.getZ(),0, 0.1, 0);

        ci.cancel();
    }
}
