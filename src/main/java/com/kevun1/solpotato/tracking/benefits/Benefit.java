package com.kevun1.solpotato.tracking.benefits;

import com.kevun1.solpotato.SOLPotato;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.DoubleNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.nbt.StringNBT;

import java.util.Objects;

public abstract class Benefit {
    protected final String benefitType;
    protected final String name;
    protected final double value;
    protected boolean invalid = false;

    public Benefit(String benefitType, String name, double value) {
        this.benefitType = benefitType;
        this.name = name;
        this.value = value;
    }

    public abstract void applyTo(PlayerEntity player);

    public abstract void removeFrom(PlayerEntity player);

    @Override
    public String toString() {
        return "[" + benefitType + ", " + name + ", " + value + "]";
    }

    public boolean isInvalid() {
        return invalid;
    }

    protected void markInvalid() {
        SOLPotato.LOGGER.warn("Invalid attribute specified in config: " + name);
        invalid = true;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return benefitType;
    }

    public double getValue() {
        return value;
    }

    public abstract CompoundNBT serializeNBT();
}