package team.chisel.ctm.client.texture.type;

import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;

import lombok.extern.log4j.Log4j2;
import net.minecraft.util.StringUtils;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import team.chisel.ctm.api.texture.ITextureType;
import team.chisel.ctm.api.texture.TextureType;

/**
 * Registry for all the different texture types
 */
@Log4j2
public class TextureTypeRegistry {

    private static Map<String, ITextureType> map = Maps.newHashMap();

    @SuppressWarnings("unchecked")
    public static void scan() {
        final List<ModFileScanData.AnnotationData> annotations = ModList.get().getAllScanData().stream()
                .map(ModFileScanData::getAnnotations)
                .flatMap(Collection::stream)
                .filter(a -> TextureType.class.getName().equals(a.getAnnotationType().getClassName()))
                .collect(Collectors.toList());
        log.debug("Found {} @TextureType annotations from scan", annotations.size());

        Multimap<ModFileScanData.AnnotationData, String> annots = HashMultimap.create();        
        for (ModFileScanData.AnnotationData single : annotations) {
        	annots.put(single, (String) single.getAnnotationData().get("value"));
        }
        log.debug("Found {} unique texture types", annots.size());
        
        for (Entry<ModFileScanData.AnnotationData, Collection<String>> data : annots.asMap().entrySet()) {
            ITextureType type;
            if (data.getKey().getTargetType() == ElementType.FIELD) {
                try {
                    Class<?> c = Class.forName(data.getKey().getClassType().getClassName());
                    type = (ITextureType) c.getDeclaredField(data.getKey().getMemberName()).get(null);
                } catch (Exception e) {
                    throw new RuntimeException("Exception loading texture type for class: " + data.getKey().getClassType(), e);
                }
            } else if (data.getKey().getTargetType() == ElementType.TYPE) {
	            try {
	                type = ((Class<? extends ITextureType>) Class.forName(data.getKey().getClassType().getClassName())).newInstance();
	            } catch (Exception e) {
	                throw new RuntimeException("Exception loading texture type for class: " + data.getKey().getClassType() + " (on member " + data.getKey().getMemberName() + ")", e);
	            }
            } else {
            	throw new IllegalArgumentException("@TextureType found on invalid element type: " + data.getKey().getTargetType() + " (" + data.getKey().getClassType() + ")");
            }
            for (String name : data.getValue()) {
                if (StringUtils.isNullOrEmpty(name)) {
                    name = data.getKey().getMemberName();
                    name = name.substring(name.lastIndexOf('.') + 1);
                }
                log.debug("Registering scanned texture type: {}", name);
                register(name, type);
            }
        }
    }

    public static void register(String name, ITextureType type){
        String key = name.toLowerCase(Locale.ROOT);
        if (map.containsKey(key) && map.get(key) != type){
            throw new IllegalArgumentException("Render Type with name "+key+" has already been registered!");
        }
        else if (map.get(key) != type){
            map.put(key, type);
        }
    }

    public static ITextureType getType(String name){
        String key = name.toLowerCase(Locale.ROOT);
        return map.get(key);
    }

    public static boolean isValid(String name){
        return getType(name) != null;
    }
}
