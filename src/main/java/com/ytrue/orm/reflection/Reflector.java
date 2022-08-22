package com.ytrue.orm.reflection;

import com.ytrue.orm.reflection.invoker.GetFieldInvoker;
import com.ytrue.orm.reflection.invoker.Invoker;
import com.ytrue.orm.reflection.invoker.MethodInvoker;
import com.ytrue.orm.reflection.invoker.SetFieldInvoker;
import com.ytrue.orm.reflection.property.PropertyNamer;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author ytrue
 * @date 2022/8/19 21:02
 * @description 反射器，属性 get/set 的映射器
 */
public class Reflector {

    /**
     * 是否开启类缓存
     */
    private static boolean classCacheEnabled = true;

    /**
     * 空String数组
     */
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * 线程安全的缓存
     */
    private static final Map<Class<?>, Reflector> REFLECTOR_MAP = new ConcurrentHashMap<>();

    /**
     * class
     */
    private Class<?> type;

    /**
     * get 属性列表
     */
    private String[] readablePropertyNames = EMPTY_STRING_ARRAY;

    /**
     * set 属性列表
     */
    private String[] writeablePropertyNames = EMPTY_STRING_ARRAY;

    /**
     * set 方法列表
     */
    private Map<String, Invoker> setMethods = new HashMap<>();

    /**
     * get 方法列表
     */
    private Map<String, Invoker> getMethods = new HashMap<>();

    /**
     * set 类型列表
     */
    private Map<String, Class<?>> setTypes = new HashMap<>();

    /**
     * get 类型列表
     */
    private Map<String, Class<?>> getTypes = new HashMap<>();

    /**
     * 构造函数
     */
    private Constructor<?> defaultConstructor;

    /**
     * 不区分大小写的属性映射
     */
    private Map<String, String> caseInsensitivePropertyMap = new HashMap<>();

    public Reflector(Class<?> clazz) {
        this.type = clazz;
        // 加入构造函数
        addDefaultConstructor(clazz);
        // 加入 getter
        addGetMethods(clazz);
        // 加入 setter
        addSetMethods(clazz);
        // 加入字段
        addFields(clazz);

        readablePropertyNames = getMethods.keySet().toArray(new String[getMethods.keySet().size()]);
        writeablePropertyNames = setMethods.keySet().toArray(new String[setMethods.keySet().size()]);

        for (String propName : readablePropertyNames) {
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
        for (String propName : writeablePropertyNames) {
            caseInsensitivePropertyMap.put(propName.toUpperCase(Locale.ENGLISH), propName);
        }
    }


    /**
     * 添加字段
     *
     * @param clazz
     */
    private void addFields(Class<?> clazz) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            if (canAccessPrivateMethods()) {
                try {
                    field.setAccessible(true);
                } catch (Exception e) {
                    // Ignored. This is only a final precaution, nothing we can do.
                }
            }
            if (field.isAccessible()) {
                // 判断 set 方法列表 是否包含
                if (!setMethods.containsKey(field.getName())) {
                    // issue #379 - removed the check for final because JDK 1.5 allows
                    // modification of final fields through reflection (JSR-133). (JGB)
                    // pr #16 - final static can only be set by the classloader
                    // 获取字段的修饰符
                    int modifiers = field.getModifiers();
                    // 如果不是Final 和 Static修饰的就可以添加
                    if (!(Modifier.isFinal(modifiers) && Modifier.isStatic(modifiers))) {
                        addSetField(field);
                    }
                }
                // 判断 get 方法列表 是否包含
                if (!getMethods.containsKey(field.getName())) {
                    addGetField(field);
                }
            }
        }

        // 处理父类的，递归
        if (clazz.getSuperclass() != null) {
            addFields(clazz.getSuperclass());
        }
    }


    private void addSetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            setMethods.put(field.getName(), new SetFieldInvoker(field));
            setTypes.put(field.getName(), field.getType());
        }
    }

    private void addGetField(Field field) {
        if (isValidPropertyName(field.getName())) {
            getMethods.put(field.getName(), new GetFieldInvoker(field));
            getTypes.put(field.getName(), field.getType());
        }
    }


    /**
     * 添加setter方法
     *
     * @param clazz
     */
    private void addSetMethods(Class<?> clazz) {
        Map<String, List<Method>> conflictingSetters = new HashMap<>();
        Method[] methods = getClassMethods(clazz);
        for (Method method : methods) {
            String name = method.getName();
            if (name.startsWith("set") && name.length() > 3) {
                if (method.getParameterTypes().length == 1) {
                    name = PropertyNamer.methodToProperty(name);
                    addMethodConflict(conflictingSetters, name, method);
                }
            }
        }
        resolveSetterConflicts(conflictingSetters);
    }

    /**
     * 解析 conflictingSetters
     *
     * @param conflictingSetters
     */
    private void resolveSetterConflicts(Map<String, List<Method>> conflictingSetters) {
        for (String propName : conflictingSetters.keySet()) {
            List<Method> setters = conflictingSetters.get(propName);
            Method firstMethod = setters.get(0);
            if (setters.size() == 1) {
                addSetMethod(propName, firstMethod);
            } else {
                Class<?> expectedType = getTypes.get(propName);
                if (expectedType == null) {
                    throw new RuntimeException("Illegal overloaded setter method with ambiguous type for property "
                            + propName + " in class " + firstMethod.getDeclaringClass() + ".  This breaks the JavaBeans " +
                            "specification and can cause unpredicatble results.");
                } else {
                    Iterator<Method> methods = setters.iterator();
                    Method setter = null;
                    while (methods.hasNext()) {
                        Method method = methods.next();
                        if (method.getParameterTypes().length == 1
                                && expectedType.equals(method.getParameterTypes()[0])) {
                            setter = method;
                            break;
                        }
                    }
                    if (setter == null) {
                        throw new RuntimeException("Illegal overloaded setter method with ambiguous type for property "
                                + propName + " in class " + firstMethod.getDeclaringClass() + ".  This breaks the JavaBeans " +
                                "specification and can cause unpredicatble results.");
                    }
                    addSetMethod(propName, setter);
                }
            }
        }
    }

    private void addSetMethod(String name, Method method) {
        if (isValidPropertyName(name)) {
            setMethods.put(name, new MethodInvoker(method));
            setTypes.put(name, method.getParameterTypes()[0]);
        }
    }


    /**
     * 添加 getter
     *
     * @param clazz
     */
    private void addGetMethods(Class<?> clazz) {
        Map<String, List<Method>> conflictingGetters = new HashMap<>();

        // 获取这个类的所有的方法，包含实现的接口和继承类的
        Method[] methods = getClassMethods(clazz);

        for (Method method : methods) {
            // 获取方法名称
            String name = method.getName();

            // 判断是不是标准的get方法
            if (name.startsWith(PropertyNamer.GET) && name.length() > 3) {
                if (method.getParameterTypes().length == 0) {
                    name = PropertyNamer.methodToProperty(name);
                    addMethodConflict(conflictingGetters, name, method);
                }
            } else
                // 判断是不是标准的is方法
                if (name.startsWith(PropertyNamer.IS) && name.length() > 2) {
                    if (method.getParameterTypes().length == 0) {
                        name = PropertyNamer.methodToProperty(name);
                        addMethodConflict(conflictingGetters, name, method);
                    }
                }
        }
        resolveGetterConflicts(conflictingGetters);
    }

    /**
     * 解析 conflictingGetters
     *
     * @param conflictingGetters
     */
    private void resolveGetterConflicts(Map<String, List<Method>> conflictingGetters) {
        for (String propName : conflictingGetters.keySet()) {
            // 获取value内容
            List<Method> getters = conflictingGetters.get(propName);

            Iterator<Method> iterator = getters.iterator();
            // 获取list的第一个元素
            Method firstMethod = iterator.next();
            if (getters.size() == 1) {
                addGetMethod(propName, firstMethod);

                // 下面基本是不会发生的，这里参考mybatis做的校验而已
            } else {
                Method getter = firstMethod;
                Class<?> getterType = firstMethod.getReturnType();
                while (iterator.hasNext()) {
                    Method method = iterator.next();
                    Class<?> methodType = method.getReturnType();
                    // 下面是校验非法重载的，这个基本不会发生的
                    if (methodType.equals(getterType)) {
                        throw new RuntimeException("Illegal overloaded getter method with ambiguous type for property "
                                + propName + " in class " + firstMethod.getDeclaringClass()
                                + ".  This breaks the JavaBeans " + "specification and can cause unpredicatble results.");
                    } else if (methodType.isAssignableFrom(getterType)) {
                        // OK getter type is descendant
                    } else if (getterType.isAssignableFrom(methodType)) {
                        getter = method;
                        getterType = methodType;
                    } else {
                        throw new RuntimeException("Illegal overloaded getter method with ambiguous type for property "
                                + propName + " in class " + firstMethod.getDeclaringClass()
                                + ".  This breaks the JavaBeans " + "specification and can cause unpredicatble results.");
                    }
                }
                addGetMethod(propName, getter);
            }
        }
    }

    private void addGetMethod(String name, Method method) {
        if (isValidPropertyName(name)) {
            // 添加方法
            getMethods.put(name, new MethodInvoker(method));
            // 添加返回类型
            getTypes.put(name, method.getReturnType());
        }
    }

    /**
     * 校验一个属性名
     *
     * @param name
     * @return
     */
    private boolean isValidPropertyName(String name) {
        return !(name.startsWith("$") || "serialVersionUID".equals(name) || "class".equals(name));
    }

    /**
     * 添加方法冲突
     *
     * @param conflictingMethods
     * @param name
     * @param method
     */
    private void addMethodConflict(Map<String, List<Method>> conflictingMethods, String name, Method method) {
        // 判断一个map中是否存在这个key，就会返回 value
        // 如果不存在 则创建一个满足value要求的数据结构放到value中，就行这个里的 创建 new ArrayList<>()
        List<Method> list = conflictingMethods.computeIfAbsent(name, k -> new ArrayList<>());
        list.add(method);
    }

    /**
     * 获取类的所有方法，包含实现的接口，继承的父类的方法
     *
     * @param clazz
     * @return
     */
    private Method[] getClassMethods(Class<?> clazz) {
        Map<String, Method> uniqueMethods = new HashMap<>();
        Class<?> currentClass = clazz;

        while (currentClass != null) {
            addUniqueMethods(uniqueMethods, currentClass.getDeclaredMethods());

            // 我们还需要寻找接口方法——因为类可能是抽象的
            // 获取这个类的接口
            Class<?>[] interfaces = currentClass.getInterfaces();
            for (Class<?> anInterface : interfaces) {
                // 继续重复
                addUniqueMethods(uniqueMethods, anInterface.getMethods());
            }
            // 获取当前类的父类，再次继续
            currentClass = currentClass.getSuperclass();
        }

        // 获取所有的value
        Collection<Method> methods = uniqueMethods.values();

        // 转换成数组返回
        return methods.toArray(new Method[methods.size()]);
    }

    /**
     * 把methods的方法 存到 uniqueMethods中 格式 ["int#getName"=>xxx]
     *
     * @param uniqueMethods
     * @param methods
     */
    private void addUniqueMethods(Map<String, Method> uniqueMethods, Method[] methods) {
        for (Method currentMethod : methods) {

            /*
            什么是桥接方法:
                JDK 1.5 引入泛型后，为了使Java的泛型方法生成的字节码和 1.5 版本前的字节码相兼容，
                由编译器自动生成的方法，这个就是桥接方法。
                可以通过Method.isBridge()方法来判断一个方法是否是桥接方法，
                在字节码中桥接方法会被标记为ACC_BRIDGE和ACC_SYNTHETIC，
                其中ACC_BRIDGE用于说明这个方法是由编译生成的桥接方法，
                ACC_SYNTHETIC说明这个方法是由编译器生成，并且不会在源代码中出现

            什么时候会生成桥接方法:
                就是说一个子类在继承（或实现）一个父类（或接口）的泛型方法时，
                在子类中明确指定了泛型类型，那么在编译时编译器会自动生成桥接方法
             */
            if (!currentMethod.isBridge()) {
                //取得签名, 格式==>返回参数#方法名称:方法的参数类型
                String signature = getSignature(currentMethod);

                // check to see if the method is already known
                // if it is known, then an extended class must have
                // overridden a method
                // 如果uniqueMethods没有key
                if (!uniqueMethods.containsKey(signature)) {
                    if (canAccessPrivateMethods()) {
                        try {
                            currentMethod.setAccessible(true);
                        } catch (Exception e) {
                            // Ignored. This is only a final precaution, nothing we can do.
                        }
                    }

                    // 加入map中
                    uniqueMethods.put(signature, currentMethod);
                }
            }
        }
    }

    /**
     * 获取签名 格式==>返回参数#方法名称:方法的参数类型
     *
     * @param method
     * @return
     */
    private String getSignature(Method method) {
        StringBuilder sb = new StringBuilder();
        // 获取方法的返回类型
        Class<?> returnType = method.getReturnType();
        // 如果返回类型不为空，那么这里是 int#
        if (returnType != null) {
            sb.append(returnType.getName()).append('#');
        }
        // 这里是 int#getAge
        sb.append(method.getName());
        // 获取这个方法的所有参数类型
        Class<?>[] parameters = method.getParameterTypes();
        for (int i = 0; i < parameters.length; i++) {
            if (i == 0) {
                sb.append(':');
            } else {
                sb.append(',');
            }
            sb.append(parameters[i].getName());
        }
        // int#getAge:java.lang.String,java.lang.Integer
        return sb.toString();
    }

    /**
     * 添加默认的构造方法, 简单理解就是把无参数构造赋值个给defaultConstructor
     *
     * @param clazz
     */
    private void addDefaultConstructor(Class<?> clazz) {

        // 获取本类所有的构造方法
        Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();

        for (Constructor<?> constructor : declaredConstructors) {
            // 获取无参数构造
            if (constructor.getParameterTypes().length == 0) {
                // 可以访问私有方法
                if (canAccessPrivateMethods()) {
                    try {
                        // 设置权限
                        constructor.setAccessible(true);
                    } catch (Exception ignore) {
                        // Ignored. This is only a final precaution, nothing we can do
                    }
                }
                if (constructor.isAccessible()) {
                    this.defaultConstructor = constructor;
                }
            }
        }

    }


    /**
     * 判断私有方法是否能够访问
     *
     * @return
     */
    private static boolean canAccessPrivateMethods() {
        try {
            SecurityManager securityManager = System.getSecurityManager();
            if (null != securityManager) {
                securityManager.checkPermission(new ReflectPermission("suppressAccessChecks"));
            }
        } catch (SecurityException e) {
            return false;
        }
        return true;
    }

    /**
     * 获取type
     *
     * @return
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * 获取默认构造
     *
     * @return
     */
    public Constructor<?> getDefaultConstructor() {
        if (defaultConstructor != null) {
            return defaultConstructor;
        } else {
            throw new RuntimeException("There is no default constructor for " + type);
        }
    }

    /**
     * 判断是否有默认构造
     *
     * @return
     */
    public boolean hasDefaultConstructor() {
        return defaultConstructor != null;
    }


    public Class<?> getSetterType(String propertyName) {
        Class<?> clazz = setTypes.get(propertyName);
        if (clazz == null) {
            throw new RuntimeException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    public Invoker getGetInvoker(String propertyName) {
        Invoker method = getMethods.get(propertyName);
        if (method == null) {
            throw new RuntimeException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    public Invoker getSetInvoker(String propertyName) {
        Invoker method = setMethods.get(propertyName);
        if (method == null) {
            throw new RuntimeException("There is no setter for property named '" + propertyName + "' in '" + type + "'");
        }
        return method;
    }

    public Class<?> getGetterType(String propertyName) {
        Class<?> clazz = getTypes.get(propertyName);
        if (clazz == null) {
            throw new RuntimeException("There is no getter for property named '" + propertyName + "' in '" + type + "'");
        }
        return clazz;
    }

    public String[] getGetablePropertyNames() {
        return readablePropertyNames;
    }

    public String[] getSetablePropertyNames() {
        return writeablePropertyNames;
    }

    public boolean hasSetter(String propertyName) {
        return setMethods.keySet().contains(propertyName);
    }

    public boolean hasGetter(String propertyName) {
        return getMethods.keySet().contains(propertyName);
    }

    public String findPropertyName(String name) {
        return caseInsensitivePropertyMap.get(name.toUpperCase(Locale.ENGLISH));
    }


    /**
     * 得到某个类的反射器，是静态方法，而且要缓存，又要多线程，所以REFLECTOR_MAP是一个ConcurrentHashMap
     *
     * @param clazz
     * @return
     */
    public static Reflector forClass(Class<?> clazz) {
        if (classCacheEnabled) {
            // synchronized (clazz) removed see issue #461
            // 对于每个类来说，我们假设它是不会变的，这样可以考虑将这个类的信息(构造函数，getter,setter,字段)加入缓存，以提高速度
            Reflector cached = REFLECTOR_MAP.get(clazz);
            if (cached == null) {
                cached = new Reflector(clazz);
                REFLECTOR_MAP.put(clazz, cached);
            }
            return cached;
        } else {
            return new Reflector(clazz);
        }
    }

    public static void setClassCacheEnabled(boolean classCacheEnabled) {
        Reflector.classCacheEnabled = classCacheEnabled;
    }

    public static boolean isClassCacheEnabled() {
        return classCacheEnabled;
    }


}
