package lan.chaos.flink.cdc.sync;

import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.StrUtil;
import lombok.var;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigurationManager {
    private static final String DEF_REGION_NAME = "default";
    private static final Map<String, Properties[]> propertyMap = new HashMap<>();
    private static String profile = null;

    private static Properties getPropertiesFromFile(String fileName) {
        Properties pts = new Properties();
        try {
            ClassPathResource source = new ClassPathResource(fileName);
            pts.load(source.getStream());
        } catch (IOException ignored) {

        }
        return pts;
    }

    private static boolean isDefaultRegion(String region) {
        return DEF_REGION_NAME.equals(region);
    }

    private static Properties[] getProperties(String region) {
        if (!propertyMap.containsKey(region)) {
            if (profile == null && !isDefaultRegion(region)) {
                getProperties(DEF_REGION_NAME);
            }
            var propertiesWithProfile = new Properties[2];
            var fileName = (isDefaultRegion(region) ? "" : (region + "/")) + "application.properties";
            propertiesWithProfile[0] = getPropertiesFromFile(fileName);
            if (isDefaultRegion(region) && profile == null) {
                profile = propertiesWithProfile[0].getProperty("flink.executer.profile");
            }
            if (StrUtil.isNotEmpty(profile)) {
                fileName = (isDefaultRegion(region) ? "" : (region + "/")) + "application-" + profile + ".properties";
                propertiesWithProfile[1] = getPropertiesFromFile(fileName);
            }
            propertyMap.put(region, propertiesWithProfile);
        }
        return propertyMap.get(region);
    }

    public static String getProperty(String region, String key) {
        var profileProperties = getProperties(region);
        String property = profileProperties[0].getProperty(key);
        if (StrUtil.isNotEmpty(profile)) {
            var tempProperty = profileProperties[1].getProperty(key);
            //可能在profile里面没有配置，则使用主配置文件里面的
            if (tempProperty != null) {
                property = tempProperty;
            }
        }
        return property;
    }

    public static String getProperty(String key) {
        return getProperty(DEF_REGION_NAME, key);
    }
}
