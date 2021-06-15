package me.gleep.oreganized.tools;

import me.gleep.oreganized.util.RegistryHandler;
import net.minecraft.block.BlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.Color;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class STSBase extends SwordItem {
    public static final int MAX_TINT_DURABILITY = 150;
    private final boolean immuneToFire;
    private boolean shouldDisplayTint;

    public STSBase(IItemTier tier, int attackDamageIn, float attackSpeedIn) {
        super(tier, attackDamageIn, attackSpeedIn, new Item.Properties().group(ItemGroup.COMBAT).maxStackSize(1));
        this.immuneToFire = tier == ItemTier.NETHERITE;
    }

    @Override
    public boolean hitEntity(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        long random =  Math.round(Math.random() * 100.0F);

        if (random <= 35) {
            target.addPotionEffect(this.getSilverShine());
        }

        this.spawnParticles(target);

        this.decreaseDurabilty(stack, attacker);
        return super.hitEntity(stack, target, attacker);
    }

    @Override
    public boolean onBlockDestroyed(ItemStack stack, World worldIn, BlockState state, BlockPos pos, LivingEntity entityLiving) {
        this.decreaseDurabilty(stack, entityLiving);
        return super.onBlockDestroyed(stack, worldIn, state, pos, entityLiving);
    }

    @Override
    public boolean isImmuneToFire() {
        return this.immuneToFire;
    }

    @Override
    public void inventoryTick(ItemStack stack, World worldIn, Entity entityIn, int itemSlot, boolean isSelected) {
        super.inventoryTick(stack, worldIn, entityIn, itemSlot, isSelected);
        if (entityIn instanceof PlayerEntity) {
            PlayerEntity pl = (PlayerEntity) entityIn;
            this.shouldDisplayTint = pl.isCrouching();
        }
    }

    public void decreaseDurabilty(ItemStack stack, LivingEntity entityLiving) {
        if (!(entityLiving instanceof PlayerEntity)) return;
        PlayerEntity pl = (PlayerEntity) entityLiving;
        if (pl.isCreative()) return;
        int durability = stack.getOrCreateTag().getInt("TintedDamage");
        if (durability == 0) {
            durability = MAX_TINT_DURABILITY;
            stack.getOrCreateTag().putInt("TintedDamage", durability);
        }

        if (durability - 1 < 1) {
            if (!stack.isEmpty()) {
                if (!pl.isSilent()) {
                    pl.world.playSound(pl.getPosX(), pl.getPosY(), pl.getPosZ(), SoundEvents.ENTITY_ITEM_BREAK, pl.getSoundCategory(), 0.8F, 0.8F + pl.world.rand.nextFloat() * 0.4F, false);
                }

                for (int i = 0; i < 5; ++i) {
                    Vector3d vector3d = new Vector3d(((double)pl.getRNG().nextFloat() - 0.5D) * 0.1D, Math.random() * 0.1D + 0.1D, 0.0D);
                    vector3d = vector3d.rotatePitch(-pl.rotationPitch * ((float)Math.PI / 180F));
                    vector3d = vector3d.rotateYaw(-pl.rotationYaw * ((float)Math.PI / 180F));
                    double d0 = (double)(-pl.getRNG().nextFloat()) * 0.6D - 0.3D;
                    Vector3d vector3d1 = new Vector3d(((double)pl.getRNG().nextFloat() - 0.5D) * 0.3D, d0, 0.6D);
                    vector3d1 = vector3d1.rotatePitch(-pl.rotationPitch * ((float)Math.PI / 180F));
                    vector3d1 = vector3d1.rotateYaw(-pl.rotationYaw * ((float)Math.PI / 180F));
                    vector3d1 = vector3d1.add(pl.getPosX(), pl.getPosYEye(), pl.getPosZ());
                    if (pl.world instanceof ServerWorld) //Forge: Fix MC-2518 spawnParticle is nooped on server, need to use server specific variant
                        ((ServerWorld)pl.world).spawnParticle(new ItemParticleData(ParticleTypes.ITEM, stack), vector3d1.x, vector3d1.y, vector3d1.z, 1, vector3d.x, vector3d.y + 0.05D, vector3d.z, 0.0D);
                    else
                        pl.world.addParticle(new ItemParticleData(ParticleTypes.ITEM, stack), vector3d1.x, vector3d1.y, vector3d1.z, vector3d.x, vector3d.y + 0.05D, vector3d.z);
                }

                ItemStack newSword = ItemStack.EMPTY;
                if (this.getTier().equals(ItemTier.DIAMOND)) {
                    newSword = new ItemStack(Items.DIAMOND_SWORD, 1);
                } else if (this.getTier().equals(ItemTier.GOLD)) {
                    newSword = new ItemStack(Items.GOLDEN_SWORD, 1);
                } else if (this.getTier().equals(ItemTier.NETHERITE)) {
                    newSword = new ItemStack(Items.NETHERITE_SWORD, 1);
                }

                newSword.setTag(stack.getTag());
                newSword.getOrCreateTag().remove("TintedDamage");
                pl.setHeldItem(pl.getActiveHand(), newSword);
            }
        } else {
            durability--;
            stack.getOrCreateTag().putInt("TintedDamage", durability);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void spawnParticles(LivingEntity entity) {
        for(int i = 0; i < 5; ++i) {
            double d0 = entity.world.rand.nextGaussian() * 0.02D;
            double d1 = entity.world.rand.nextGaussian() * 0.02D;
            double d2 = entity.world.rand.nextGaussian() * 0.02D;
            entity.world.addParticle(RegistryHandler.DAWN_SHINE_PARTICLE.get(), entity.getPosXRandom(1.0D), entity.getPosYRandom() + 1.0D, entity.getPosZRandom(1.0D), d0, d1, d2);
        }
    }

    /**
     *
     * @return 0.0 for 100% (no damage / full bar), 1.0 for 0% (fully damaged / empty bar)
     */
    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        if (this.shouldDisplayTint) {
            return this.getSilverDurabilityForDisplay(stack);
        }
        return (double) stack.getDamage() / (double) stack.getMaxDamage();
    }

    public double getSilverDurabilityForDisplay(ItemStack stack) {
        return stack.getOrCreateTag().getInt("TintedDamage") < 1 ? 0D : (double)  (MAX_TINT_DURABILITY - stack.getOrCreateTag().getInt("TintedDamage")) / (double) MAX_TINT_DURABILITY;
    }

    @Override
    public int getRGBDurabilityForDisplay(ItemStack stack) {
        if (this.shouldDisplayTint) {
            return MathHelper.hsvToRGB(200F / 360F, Math.max(0.0F, (float) this.getDurabilityForDisplay(stack)), 0.94F);
        }
        return MathHelper.hsvToRGB(Math.max(0.0F, (float) (1.0F - this.getDurabilityForDisplay(stack))) / 3.0F, 1.0F, 1.0F);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void addInformation(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (stack.getOrCreateTag().getInt("TintedDamage") > 0) {
            ITextComponent text = ITextComponent.getTextComponentOrEmpty("Tint Durability: " + stack.getOrCreateTag().getInt("TintedDamage") + "/" + MAX_TINT_DURABILITY);
            text.getStyle().setColor(Color.fromInt(0xE1EBF0));
            tooltip.add(text);
        }
        super.addInformation(stack, worldIn, tooltip, flagIn);
    }

    public EffectInstance getSilverShine() {
        return new EffectInstance(RegistryHandler.DAWN_SHINE.get(), 400, 1);
    }
}
