package team.creative.solonion.common.benefit;

import java.util.HashMap;
import java.util.Map.Entry;
import java.util.function.Function;

import javax.annotation.Nullable;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.Holder;
import net.minecraft.core.Holder.Reference;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import team.creative.creativecore.common.config.gui.IGuiConfigParent;
import team.creative.creativecore.common.config.premade.registry.RegistryObjectConfig;
import team.creative.creativecore.common.gui.GuiParent;
import team.creative.creativecore.common.gui.controls.simple.GuiStateButtonMapped;
import team.creative.creativecore.common.util.text.TextMapBuilder;
import team.creative.solonion.common.SOLOnion;
import team.creative.solonion.common.mod.FirstAidManager;

public class BenefitAttribute extends Benefit<Attribute> {
    
    public final Operation operation;
    
    public BenefitAttribute(ResourceLocation location, double value, Operation op) {
        super(new RegistryObjectConfig<>(BuiltInRegistries.ATTRIBUTE, location), value);
        this.operation = op;
    }
    
    public BenefitAttribute(Holder<Attribute> holder, double value, Operation op) {
        this(holder.unwrapKey().get().location(), value);
    }
    
    public BenefitAttribute(ResourceLocation location, double value) {
        this(location, value, Operation.ADD_VALUE);
    }
    
    public BenefitAttribute(Holder<Attribute> holder, double value) {
        this(holder, value, Operation.ADD_VALUE);
    }
    
    public BenefitAttribute(CompoundTag nbt) {
        super(BuiltInRegistries.ATTRIBUTE, nbt);
        operation = Operation.BY_ID.apply(nbt.getInt("op"));
    }
    
    @Override
    public CompoundTag save() {
        CompoundTag nbt = super.save();
        nbt.putInt("op", operation.ordinal());
        return nbt;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj) && obj instanceof BenefitAttribute att)
            return att.operation == operation;
        return false;
    }
    
    public static class BenefitTypeAttribute extends BenefitType<BenefitAttribute, Object2DoubleMap<AttributeHolder>, HashMap<AttributeHolder, AttributeModifier>> {
        
        public BenefitTypeAttribute(Function<CompoundTag, BenefitAttribute> factory) {
            super(factory);
        }
        
        @Override
        public Registry registry() {
            return BuiltInRegistries.ATTRIBUTE;
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        @Environment(EnvType.CLIENT)
        public void createControls(GuiParent parent, IGuiConfigParent configParent) {
            parent.add(new GuiStateButtonMapped<Operation>("operation", new TextMapBuilder<Operation>().addComponent(Operation.values(), x -> Component.translatable(
                "config.solonion." + x.getSerializedName()))));
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        @Environment(EnvType.CLIENT)
        public void loadValue(BenefitAttribute value, GuiParent parent, IGuiConfigParent configParent) {
            GuiStateButtonMapped<Operation> op = parent.get("operation");
            op.select(value.operation);
        }
        
        @Override
        @OnlyIn(Dist.CLIENT)
        @Environment(EnvType.CLIENT)
        public BenefitAttribute saveValue(ResourceLocation location, double value, GuiParent parent, IGuiConfigParent configParent) {
            GuiStateButtonMapped<Operation> op = parent.get("operation");
            return new BenefitAttribute(location, value, op.getSelected());
        }
        
        @Override
        public Object2DoubleMap<AttributeHolder> createStack() {
            return new Object2DoubleArrayMap<>();
        }
        
        @Override
        public void addToStack(BenefitAttribute value, Object2DoubleMap<AttributeHolder> stack) {
            stack.compute(new AttributeHolder(value.property.getHolder(), value.operation), (x, y) -> y != null ? Math.max(y, value.value) : value.value);
        }
        
        @Override
        public boolean isEmpty(Object2DoubleMap<AttributeHolder> stack) {
            return stack.isEmpty();
        }
        
        @Override
        public HashMap<AttributeHolder, AttributeModifier> createApplied() {
            return new HashMap<>();
        }
        
        @Override
        public void clearApplied(HashMap<AttributeHolder, AttributeModifier> applied) {
            applied.clear();
        }
        
        @Override
        public Tag saveApplied(HashMap<AttributeHolder, AttributeModifier> applied) {
            ListTag list = new ListTag();
            for (Entry<AttributeHolder, AttributeModifier> entry : applied.entrySet()) {
                CompoundTag tag = new CompoundTag();
                tag.putString("att", entry.getKey().attribute.getRegisteredName());
                tag.putInt("op", entry.getKey().operation.ordinal());
                tag.put("mod", entry.getValue().save());
                list.add(tag);
            }
            return list;
        }
        
        @Override
        public void loadApplied(HashMap<AttributeHolder, AttributeModifier> applied, Tag nbt) {
            if (nbt instanceof ListTag list)
                for (int i = 0; i < list.size(); i++) {
                    CompoundTag tag = list.getCompound(i);
                    Reference<Attribute> att = BuiltInRegistries.ATTRIBUTE.getHolder(ResourceLocation.parse(tag.getString("att"))).get();
                    if (att != null)
                        applied.put(new AttributeHolder(att, Operation.BY_ID.apply(tag.getInt("op"))), AttributeModifier.load(tag.getCompound("mod")));
                }
        }
        
        @Override
        public boolean apply(Player player, HashMap<AttributeHolder, AttributeModifier> applied, @Nullable Object2DoubleMap<AttributeHolder> stack) {
            if (!applied.isEmpty()) {
                for (Entry<AttributeHolder, AttributeModifier> entry : applied.entrySet())
                    player.getAttribute(entry.getKey().attribute).removeModifier(entry.getValue().id());
                applied.clear();
            }
            
            if (stack != null)
                for (var entry : stack.object2DoubleEntrySet()) {
                    var location = ResourceLocation.tryBuild(SOLOnion.MODID, entry.getKey().operation.toString().toLowerCase());
                    var att = player.getAttribute(entry.getKey().attribute);
                    att.removeModifier(location); // make sure modifier does not exist already
                    var modi = new AttributeModifier(location, entry.getDoubleValue(), entry.getKey().operation);
                    if (att != null) {
                        float oldMax = player.getMaxHealth();
                        
                        att.addPermanentModifier(modi);
                        applied.put(entry.getKey(), modi);
                        if (entry.getKey().attribute == Attributes.MAX_HEALTH && !FirstAidManager.INSTALLED) {
                            // increase current health proportionally
                            float newHealth = player.getHealth() * player.getMaxHealth() / oldMax;
                            player.setHealth(newHealth);
                        }
                    }
                }
            return applied.isEmpty();
        }
    }
    
    private static record AttributeHolder(Holder<Attribute> attribute, Operation operation) {}
    
}
