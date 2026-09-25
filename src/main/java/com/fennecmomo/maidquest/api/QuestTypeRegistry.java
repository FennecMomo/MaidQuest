package com.fennecmomo.maidquest.api;

import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// 委托类型注册表。
//
// 内置类型在 MaidQuest 启动时注册；第三方模组可调用 register(...)，
// 或在 RegisterQuestTypesEvent 里注册，或对自有包调用 discover(...) 扫描 @RegQuestType。
public final class QuestTypeRegistry
{
    private static final Logger LOG = LoggerFactory.getLogger("maidquest:types");
    private static final Map<Identifier, IQuestType> TYPES = new LinkedHashMap<>();

    private QuestTypeRegistry()
    {
    }

    // ======== 注册 ========

    public static void register(IQuestType type)
    {
        if (TYPES.putIfAbsent(type.id(), type) != null)
        {
            throw new IllegalStateException("Duplicate quest type: " + type.id());
        }
    }

    @Nullable
    public static IQuestType get(Identifier id)
    {
        return TYPES.get(id);
    }

    public static IQuestType getOrThrow(Identifier id)
    {
        IQuestType type = TYPES.get(id);
        if (type == null)
        {
            throw new IllegalStateException("Unknown quest type: " + id);
        }
        return type;
    }

    public static List<IQuestType> getAll()
    {
        return List.copyOf(TYPES.values());
    }

    public static List<IQuestType> getEnabled()
    {
        return TYPES.values().stream().filter(IQuestType::isEnabled).toList();
    }

    public static int size()
    {
        return TYPES.size();
    }

    // ======== 自动发现 ========

    // 扫描指定包下的 @RegQuestType 实现类并注册。
    public static void discover(String packageName)
    {
        String path = packageName.replace('.', '/');
        try
        {
            Enumeration<URL> resources = QuestTypeRegistry.class.getClassLoader().getResources(path);
            while (resources.hasMoreElements())
            {
                scanUrl(resources.nextElement(), packageName);
            }
        }
        catch (IOException e)
        {
            LOG.warn("Quest type discovery failed for package {}", packageName, e);
        }
    }

    private static void scanUrl(URL url, String packageName)
    {
        try
        {
            URI uri = url.toURI();
            if (!"file".equals(uri.getScheme()))
            {
                return;
            }
            File[] files = new File(uri).listFiles();
            if (files == null)
            {
                return;
            }
            for (File file : files)
            {
                String name = file.getName();
                if (name.endsWith(".class"))
                {
                    discoverClass(packageName + "." + name.substring(0, name.length() - ".class".length()));
                }
                else if (file.isDirectory())
                {
                    scanUrl(file.toURI().toURL(), packageName + "." + name);
                }
            }
        }
        catch (URISyntaxException | IOException e)
        {
            LOG.warn("Quest type discovery failed for {}", url, e);
        }
    }

    private static void discoverClass(String className)
    {
        try
        {
            Class<?> clazz = Class.forName(className);
            if (Modifier.isAbstract(clazz.getModifiers()) || Modifier.isInterface(clazz.getModifiers()))
            {
                return;
            }
            if (!IQuestType.class.isAssignableFrom(clazz) || !clazz.isAnnotationPresent(RegQuestType.class))
            {
                return;
            }
            IQuestType type = (IQuestType) clazz.getDeclaredConstructor().newInstance();
            if (get(type.id()) == null)
            {
                register(type);
            }
        }
        catch (ReflectiveOperationException e)
        {
            LOG.warn("Failed to register quest type {}", className, e);
        }
    }
}
