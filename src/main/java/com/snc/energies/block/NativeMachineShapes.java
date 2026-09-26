package com.snc.energies.block;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import com.google.gson.JsonParser;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Collision uses the same bounded cuboids as the shipped native model. */
public final class NativeMachineShapes {
    private static final ConcurrentHashMap<String,VoxelShape> CACHE=new ConcurrentHashMap<>();
    private NativeMachineShapes(){}
    public static VoxelShape shape(String model,int part,Direction facing){
        return CACHE.computeIfAbsent(model+":"+part+":"+facing,key->load(model,part,facing));
    }
    private static VoxelShape load(String model,int part,Direction facing){
        String path="/assets/snc_energies/models/block/"+model+"_"+part+".json";
        try(var input=NativeMachineShapes.class.getResourceAsStream(path)){
            if(input==null)throw new IllegalStateException("Missing machine geometry: "+path);
            var json=JsonParser.parseReader(new InputStreamReader(input,StandardCharsets.UTF_8)).getAsJsonObject();
            VoxelShape result=Shapes.empty();
            for(var entry:json.getAsJsonArray("elements")){
                var cube=entry.getAsJsonObject();var from=cube.getAsJsonArray("from");var to=cube.getAsJsonArray("to");
                double x=from.get(0).getAsDouble(),y=from.get(1).getAsDouble(),z=from.get(2).getAsDouble();
                double xx=to.get(0).getAsDouble(),yy=to.get(1).getAsDouble(),zz=to.get(2).getAsDouble();
                double a=x,c=z,aa=xx,cc=zz;
                switch(facing){
                    case EAST->{a=16-zz;aa=16-z;c=x;cc=xx;}
                    case SOUTH->{a=16-xx;aa=16-x;c=16-zz;cc=16-z;}
                    case WEST->{a=z;aa=zz;c=16-xx;cc=16-x;}
                    default->{}
                }
                result=Shapes.or(result,Shapes.box(a/16,y/16,c/16,aa/16,yy/16,cc/16));
            }
            return result.optimize();
        }catch(java.io.IOException error){throw new IllegalStateException("Cannot read machine geometry "+path,error);}
    }
}
