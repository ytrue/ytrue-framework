namespace Spring.Objects.Factory;

/// <summary>
/// 定义一个用于管理和访问对象的工厂接口。提供对象获取、别名管理和类型匹配等功能。
/// </summary>
public interface IObjectFactory
{
    /// <summary>
    /// 确定对象名称是否区分大小写。
    /// </summary>
    /// <returns>如果区分大小写，则返回 true；否则返回 false。</returns>
    bool isCaseSensitive();

    /// <summary>
    /// 确定指定名称的对象是否是单例。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <returns>如果是单例对象，则返回 true；否则返回 false。</returns>
    bool isSingleton(string name);

    /// <summary>
    /// 确定指定名称的对象是否是原型（每次请求都返回一个新的实例）。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <returns>如果是原型对象，则返回 true；否则返回 false。</returns>
    bool IsPrototype(string name);

    /// <summary>
    /// 获取指定名称对象的别名列表。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <returns>别名的只读列表。</returns>
    IReadOnlyList<string> GetAliases(string name);

    /// <summary>
    /// 根据名称获取对象实例。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <returns>对应的对象实例。</returns>
    object this[string name] { get; }

    /// <summary>
    /// 获取指定类型的对象。
    /// </summary>
    /// <typeparam name="T">对象的类型。</typeparam>
    /// <returns>指定类型的对象实例。</returns>
    T GetObject<T>();

    /// <summary>
    /// 根据名称获取对象实例。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <returns>对应的对象实例。</returns>
    object GetObject(string name);

    /// <summary>
    /// 根据名称获取指定类型的对象实例。
    /// </summary>
    /// <typeparam name="T">对象的类型。</typeparam>
    /// <param name="name">对象的名称。</param>
    /// <returns>指定类型的对象实例。</returns>
    T GetObject<T>(string name);

    /// <summary>
    /// 根据名称和构造函数参数获取对象实例。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <param name="arguments">构造函数参数数组。</param>
    /// <returns>对应的对象实例。</returns>
    object GetObject(string name, object[] arguments);

    /// <summary>
    /// 根据名称和构造函数参数获取指定类型的对象实例。
    /// </summary>
    /// <typeparam name="T">对象的类型。</typeparam>
    /// <param name="name">对象的名称。</param>
    /// <param name="arguments">构造函数参数数组。</param>
    /// <returns>指定类型的对象实例。</returns>
    T GetObject<T>(string name, object[] arguments);

    /// <summary>
    /// 根据名称、类型和构造函数参数获取对象实例。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <param name="requiredType">对象的所需类型。</param>
    /// <param name="arguments">构造函数参数数组。</param>
    /// <returns>对应类型的对象实例。</returns>
    object GetObject(string name, Type requiredType, object[] arguments);

    /// <summary>
    /// 根据名称和类型获取对象实例。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <param name="requiredType">对象的所需类型。</param>
    /// <returns>对应类型的对象实例。</returns>
    object GetObject(string name, Type requiredType);

    /// <summary>
    /// 根据名称获取对象的类型。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <returns>对象的类型。</returns>
    Type GetType(string name);

    /// <summary>
    /// 判断指定名称的对象是否与目标类型匹配。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <param name="targetType">目标类型。</param>
    /// <returns>如果类型匹配，则返回 true；否则返回 false。</returns>
    bool IsTypeMatch(string name, Type targetType);

    /// <summary>
    /// 判断指定名称的对象是否与泛型类型匹配。
    /// </summary>
    /// <typeparam name="T">目标类型。</typeparam>
    /// <param name="name">对象的名称。</param>
    /// <returns>如果类型匹配，则返回 true；否则返回 false。</returns>
    bool IsTypeMatch<T>(string name);

    /// <summary>
    /// 根据名称、类型和构造函数参数创建一个新的对象实例。
    /// </summary>
    /// <param name="name">对象的名称。</param>
    /// <param name="requiredType">对象的所需类型。</param>
    /// <param name="arguments">构造函数参数数组。</param>
    /// <returns>新创建的对象实例。</returns>
    object CreateObject(string name, Type requiredType, object[] arguments);

    /// <summary>
    /// 根据名称和构造函数参数创建一个新的指定类型的对象实例。
    /// </summary>
    /// <typeparam name="T">对象的类型。</typeparam>
    /// <param name="name">对象的名称。</param>
    /// <param name="arguments">构造函数参数数组。</param>
    /// <returns>新创建的指定类型的对象实例。</returns>
    T CreateObject<T>(string name, object[] arguments);

    /// <summary>
    /// 配置现有对象的属性并返回配置后的实例。
    /// </summary>
    /// <param name="target">目标对象。</param>
    /// <param name="name">配置对象的名称。</param>
    /// <returns>配置后的对象实例。</returns>
    object ConfigureObject(object target, string name);
}