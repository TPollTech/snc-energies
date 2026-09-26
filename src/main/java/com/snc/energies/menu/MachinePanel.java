package com.snc.energies.menu;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Bundled layout shared by server slot registration, client rendering and preview. */
public record MachinePanel(String id, String[] subtitle, String accent, List<List<Object>> rects,
                           List<Position> slots, List<Bar> bars, int[] button) {
    public static final int WIDTH=256, HEIGHT=238;
    public record Position(int index,int x,int y,String role) {}
    public record Bar(String source,int x,int y,int w,int h,String color,boolean vertical) {}
    private static final Map<String,MachinePanel> PANELS=load();
    private static Map<String,MachinePanel> load(){
        try(var stream=MachinePanel.class.getResourceAsStream("/assets/snc_energies/machine_panels.json")){
            if(stream==null)throw new IllegalStateException("Missing machine panels");
            return new Gson().fromJson(new InputStreamReader(stream,StandardCharsets.UTF_8),
                new TypeToken<Map<String,MachinePanel>>(){}.getType());
        }catch(java.io.IOException error){throw new IllegalStateException(error);}
    }
    public static MachinePanel of(String id){
        var panel=PANELS.get(id);if(panel==null)throw new IllegalArgumentException("Unknown machine panel "+id);return panel;
    }
    public Position position(int index){return slots.stream().filter(p->p.index()==index).findFirst().orElse(new Position(index,-1000,-1000,""));}
    public static int x(String id,int index){return of(id).position(index).x();}
    public static int y(String id,int index){return of(id).position(index).y();}
}
