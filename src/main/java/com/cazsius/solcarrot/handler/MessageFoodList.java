package com.cazsius.solcarrot.handler;

import com.cazsius.solcarrot.capability.FoodCapability;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

public class MessageFoodList implements IMessage
{
	FoodCapability food;
	
    public MessageFoodList(FoodCapability food) 
    {
        this.food=food;
    }
    
    public MessageFoodList() 
    {
        this.food=new FoodCapability();
    }

	@Override
	public void fromBytes(ByteBuf buf) {
		PacketBuffer pb = new PacketBuffer(buf);
		while (pb.isReadable())
		{
			food.addFood(Item.getItemById(pb.readInt()));
		}
	}

	@Override
	public void toBytes(ByteBuf buf) 
	{
		PacketBuffer pb = new PacketBuffer(buf);
		for (int i : food.getIDs()) 
		{
			pb.writeInt(i);
		}
	}
	
    public static class Handler implements IMessageHandler<MessageFoodList, IMessage> {
        @Override
        public IMessage onMessage(MessageFoodList message, MessageContext ctx) {
            // Always use a construct like this to actually handle your message. This ensures that
            // your 'handle' code is run on the main Minecraft thread. 'onMessage' itself
            // is called on the networking thread so it is not safe to do a lot of things
            // here.
            FMLCommonHandler.instance().getWorldThread(ctx.netHandler).addScheduledTask(() -> handle(message, ctx));
            return null;
        }

        private void handle(MessageFoodList message, MessageContext ctx) 
        {
            EntityPlayer player = Minecraft.getMinecraft().player;
        	System.out.println("Is Remote: "+player.world.isRemote);
            FoodCapability food = player.getCapability(FoodCapability.FOOD_CAPABILITY, null);
        	System.out.println("List: "+food.getIDs());
            food.clearFood();
            food.copyFoods(message.food);
        }
    }
}